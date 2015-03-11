/*
 * 
 *               Panbox - encryption for cloud storage 
 *      Copyright (C) 2014-2015 by Fraunhofer SIT and Sirrix AG 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additonally, third party code may be provided with notices and open source
 * licenses from communities and third parties that govern the use of those
 * portions, and any licenses granted hereunder do not alter any rights and
 * obligations you may have under such open source licenses, however, the
 * disclaimer of warranty and limitation of liability provisions of the GPLv3 
 * will apply to all the product.
 * 
 */
package org.panbox.core.crypto;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Map;

import org.apache.log4j.Logger;
import org.panbox.PanboxConstants;
import org.panbox.core.LimitedHashMap;

public class FileBasedObfuscator extends AbstractObfuscatorIVPool {

	Logger logger = Logger.getLogger(FileBasedObfuscator.class);

	// private static FileBasedObfuscator instance = null;

	// private FileBasedObfuscator() {}

	// public static FileBasedObfuscator getInstance()
	// {
	// if(instance == null)
	// {
	// instance = new FileBasedObfuscator();
	// }
	// return instance;
	// }

	final FileFilter dirFilter = new FileFilter() {

		@Override
		public boolean accept(File pathname) {
			return pathname.isDirectory() && pathname.canRead();
		}
	};

	private final class CleanupIVFilter implements FileFilter {

		String ivfileprefix;

		private CleanupIVFilter(String ivfileprefix) {
			this.ivfileprefix = ivfileprefix;
		}

		@Override
		public boolean accept(File pathname) {
			return (pathname.isFile()
					&& (pathname.getName().length() > IV_SIDECAR_FILE_LEN) && pathname
					.getName().startsWith(ivfileprefix));
		}
	}

	@Override
	public synchronized void fetchIVPool(String absolutePath, String shareName) {
		File path = new File(absolutePath + File.separator
				+ Obfuscator.IV_POOL_PATH);

		File[] subdirs = path.listFiles(dirFilter);

		// String path_ = absolutePath + File.separator +
		// Obfuscator.IV_POOL_PATH;

		LimitedHashMap<String, byte[]> ivs = new LimitedHashMap<String, byte[]>(
				PanboxConstants.OBFUSCATOR_IV_POOL_SIZE);

		for (int i = 0; i < subdirs.length; i++) {
			// char csub = Utils.hexArray[i];
			// Path sub = Paths.get(path_ + File.separator + csub);

			DirectoryStream<Path> stream;
			try {
				stream = Files.newDirectoryStream(subdirs[i].toPath());
				// iterate all subdirectories of IVpool
				for (Path entry : stream) {
					String filename = entry.getName(entry.getNameCount() - 1)
							.toString();
					Map.Entry<String, byte[]> e;
					// check length, if invalid a conflict may have been flagged
					// by the CSP
					if (!ivEntryLengthValid(filename)) {
						e = resolveIVConflict(subdirs[i], filename);
					} else {
						e = splitFilename(filename);
					}
					if (e != null) {
						byte[] old = null;
						if (((old = ivs.put(e.getKey(), e.getValue())) != null)
								&& (!Arrays.equals(old, e.getValue()))) {
							logger.error("Detected duplicate lookup values with differing IVs! No actions taken");
						}
					} else {
						logger.error("Error while reading IV " + filename);
					}
				}
				stream.close();
			} catch (IOException e1) {
				logger.error("Error while reading IV pool", e1);
			}
		}

		// cache it
		this.ivPool = ivs;
	}

	/**
	 * helper method for resolving potential conflicts in IV pool sidecar files
	 * 
	 * @param subdir
	 * @param filename
	 * @return
	 */
	protected synchronized Map.Entry<String, byte[]> resolveIVConflict(File subdir,
			String filename) {
		logger.warn("Invalid sidecar file length detected, trying to resolve problem.");
		Map.Entry<String, byte[]> ret = null;
		if (filename.length() > IV_SIDECAR_FILE_LEN) {
			try {
				String part = filename.substring(0, IV_SIDECAR_FILE_LEN);
				// should throw some exception if parsing fails
				if ((ret = splitFilename(part)) == null) {
					throw new Exception();
				}

				// Otherwise, clean up
				// Check if another, valid file already exists
				if (!Files.exists(Paths.get(subdir.getAbsolutePath()
						+ File.separator + part))) {
					// no, move current
					Files.move(
							Paths.get(subdir.getAbsolutePath() + File.separator
									+ filename),
							Paths.get(subdir.getAbsolutePath() + File.separator
									+ part),
							StandardCopyOption.REPLACE_EXISTING);
				}

				// finally delete any other files with the same
				// prefix and invalid length
				File[] invalidIVs = subdir.listFiles(new CleanupIVFilter(part));
				for (int j = 0; j < invalidIVs.length; j++) {
					Files.delete(invalidIVs[j].toPath());
				}
			} catch (Exception e2) {
				logger.error(
						"Unable to resolve problem for potential sidecar file "
								+ filename, e2);
			}
		} else {
			// nothing we can do
			logger.error("Unable to resolve problem for potential sidecar file "
					+ filename);
		}
		return ret;
	}
}
