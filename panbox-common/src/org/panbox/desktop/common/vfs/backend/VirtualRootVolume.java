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
package org.panbox.desktop.common.vfs.backend;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import javax.crypto.SecretKey;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.panbox.core.crypto.AbstractObfuscatorFactory;
import org.panbox.core.crypto.FileObfuscatorFactory;
import org.panbox.core.crypto.Obfuscator;
import org.panbox.core.exception.MissingIVException;
import org.panbox.core.exception.ObfuscationException;
import org.panbox.core.keymgmt.ShareKey;
import org.panbox.core.vfs.backend.VirtualFile;
import org.panbox.desktop.common.vfs.backend.exceptions.SecretKeyNotFoundException;

public class VirtualRootVolume implements IRootVolume {

	private final static Logger logger = Logger
			.getLogger(VirtualRootVolume.class);

	// singleton

	private static VirtualRootVolume instance;

	private VirtualRootVolume() {
	}

	public synchronized static IRootVolume getInstance() {
		if (instance == null) {
			instance = new VirtualRootVolume();
		}
		return instance;
	}

	// root-file and VFS management

	// shares holds a concurrent (multi-threading safe) queue instance, which
	// contains all attached shares.
	private final Queue<VFSShare> shares = new ConcurrentLinkedDeque<VFSShare>();

	private final VirtualFile rootFile = new VirtualFile(File.separator, null) {

		/**
		 * Gets the relative Path for the VirtualRoot-File
		 * 
		 * @return returns java.io.File.seperator
		 */
		@Override
		public String getRelativePath() {
			return File.separator;
		}

		/**
		 * Gets the Path for the VirtualRoot-File
		 * 
		 * @return returns java.io.File.seperator
		 */
		@Override
		public String getPath() {
			return File.separator;
		}

		/**
		 * Lists all directories in the VirtualRootVolume, which are all
		 * attached shares.
		 * 
		 * @return Array of VirtualFile contains a VirtualFile wrapper file,
		 *         that leads directory to the root-file of the attached shares.
		 */
		@Override
		public VirtualFile[] list() {
			List<VirtualFile> files = new ArrayList<VirtualFile>();
			for (final VFSShare share : shares) {
				VirtualFile shareFile = null;
				shareFile = new VirtualFile(share.getShareName(),
						share.getBackend()) {

					@Override
					public VirtualFile[] list() throws IOException {
						return share.getBackend().getFile(File.separator)
								.list(); // forward to VirtualVolume of share
					}

					// TODO What's the difference in paths here?
					@Override
					public String getRelativePath() {
						return share.getShareName();
					}

					@Override
					public String getPath() {
						return share.getShareName();
					}
				};
				files.add(shareFile);
			}
			return files.toArray(new VirtualFile[files.size()]);
		}
	};

	@Override
	public void registerShare(String userid, VFSShare share) {
		registerShare(share);

	}

	@Override
	public boolean removeShare(String userid, String shareName) {
		return removeShare(shareName);
	}

	@Override
	public boolean existsAndChanged(String userid, VFSShare vfsShare) {
		return existsAndChanged(vfsShare);
	}

	/**
	 * Adds a new instance of VFSShare to the list of attached shares.
	 * 
	 * @param share
	 *            VFSShare instance of the newly registered share.
	 */
	public final void registerShare(VFSShare share) {
		shares.add(share);
	}

	/**
	 * Removes the attached share with the share name that has been passed by
	 * parameter. If there was a share with the specified name it will return
	 * true, else false.
	 * 
	 * @param shareName
	 *            Name of the Share that will be removed.
	 * @return True if a share was removed, else false.
	 */
	public final boolean removeShare(String shareName) {
		Iterator<VFSShare> it = shares.iterator();
		while (it.hasNext()) {
			VFSShare cur = it.next();
			if (cur.getShareName().equals(shareName)) {
				// try to remove obfuscator instance for the share
				try {
					AbstractObfuscatorFactory.getFactory(
							FileObfuscatorFactory.class).removeInstance(
							cur.getObfuscator());
				} catch (ClassNotFoundException | InstantiationException
						| IllegalAccessException | ObfuscationException e) {
					logger.error(
							"Unable to remove obfuscator instance for share "
									+ cur.getShareName(), e);
				}
				it.remove();
				return true;
			}
		}
		return false;
	}

	public final boolean existsAndChanged(VFSShare vfsShare) {
		Iterator<VFSShare> it = shares.iterator();
		while (it.hasNext()) {
			VFSShare share = it.next();
			if (share.getShareName().equals(vfsShare.getShareName())) {
				if (FilenameUtils.equalsNormalizedOnSystem(share.getBackend()
						.getRoot().getPath(), vfsShare.getBackend().getRoot()
						.getPath())) {
					return true;
				} else {
					return false;
				}
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.panbox.desktop.common.vfs.backend.IRootVolume#getFreeSpace()
	 */
	@Override
	public long getFreeSpace() {
		return 107374182400L;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.panbox.desktop.common.vfs.backend.IRootVolume#getTotalSpace()
	 */
	@Override
	public long getTotalSpace() {
		return 107374182400L;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.panbox.desktop.common.vfs.backend.IRootVolume#getUsableSpace()
	 */
	@Override
	public long getUsableSpace() {
		return 107374182400L;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.panbox.desktop.common.vfs.backend.IRootVolume#getObfuscationKeyForFile
	 * (java.lang.String)
	 */
	@Override
	public SecretKey getObfuscationKeyForFile(String fileName)
			throws SecretKeyNotFoundException {
		String normalizedFileName = FilenameUtils.normalize(fileName);
		for (VFSShare share : shares) {
			if (share.contains(normalizedFileName)) {
				return share.getObfuscationKey();
			}
		}
		throw new SecretKeyNotFoundException(
				"Could not obtain a share key for the specified file '"
						+ normalizedFileName + "'.");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.panbox.desktop.common.vfs.backend.IRootVolume#getObfuscator(java.
	 * lang.String)
	 */
	@Override
	public Obfuscator getObfuscator(String fileName)
			throws SecretKeyNotFoundException {
		String normalizedFileName = FilenameUtils.normalize(fileName);
		for (VFSShare share : shares) {
			if (share.contains(normalizedFileName)) {
				return share.getObfuscator();
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.panbox.desktop.common.vfs.backend.IRootVolume#getShareKeyForFile(
	 * java.lang.String, int)
	 */
	@Override
	public SecretKey getShareKeyForFile(String fileName, int version)
			throws SecretKeyNotFoundException {
		String normalizedFileName = FilenameUtils.normalize(fileName);
		for (VFSShare share : shares) {
			if (share.contains(normalizedFileName)) {
				return share.getShareKey(version);
			}
		}
		throw new SecretKeyNotFoundException(
				"Could not obtain a share key for the specified file '"
						+ normalizedFileName + "'.");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.panbox.desktop.common.vfs.backend.IRootVolume#getLatestShareKeyForFile
	 * (java.lang.String)
	 */
	@Override
	public ShareKey getLatestShareKeyForFile(String fileName)
			throws SecretKeyNotFoundException {
		String normalizedFileName = FilenameUtils.normalize(fileName);
		for (VFSShare share : shares) {
			if (share.contains(normalizedFileName)) {
				return share.getLatestShareKey();
			}
		}
		throw new SecretKeyNotFoundException(
				"Could not obtain a share key for the specified file '"
						+ normalizedFileName + "'.");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.panbox.desktop.common.vfs.backend.IRootVolume#getFile(java.lang.String
	 * )
	 */
	@Override
	public VirtualFile getFile(final String fileName) throws IOException {
		String normalizedFileName = FilenameUtils.normalize(fileName);
		if (normalizedFileName.equals(File.separator)) {
			return rootFile;
		}
		for (VFSShare share : shares) {
			if (share.contains(normalizedFileName)) {
				return share.getFile(normalizedFileName);
			}
		}
		throw new FileNotFoundException(
				"Could not get the VirtualFile for the specified file '"
						+ normalizedFileName + "'.");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.panbox.desktop.common.vfs.backend.IRootVolume#getRelativePathForFile
	 * (org.panbox.core.vfs.backend.VirtualFile)
	 */
	@Override
	public String getRelativePathForFile(VirtualFile file)
			throws FileNotFoundException {
		if (file.equals(rootFile)) {
			return File.separator;
		}
		for (VFSShare share : shares) {
			String result = _getRelativePath(share, file);
			if (result != null) {
				return result;
			}
		}
		throw new FileNotFoundException(
				"Could not get the Relative Path for the specified file  '"
						+ file.getFileName() + "'.");
	}

	private String _getRelativePath(VFSShare share, VirtualFile file) {
		if (share.contains(file)) {

			if (FilenameUtils.equalsOnSystem(share.getShareName(),
					file.getFileName())
					|| FilenameUtils.equalsOnSystem(share.getShareName(),
							file.getFileName())) {
				return File.separator + share.getShareName();
			} else {
				return share.getRelativePath(file);
			}
		} else {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.panbox.desktop.common.vfs.backend.IRootVolume#deobfuscatePath(org
	 * .panbox.core.vfs.backend.VirtualFile)
	 */
	@Override
	public String deobfuscatePath(VirtualFile file)
			throws FileNotFoundException, ObfuscationException {
		if (file.equals(rootFile)) {
			return File.separator;
		}
		String path = null;
		VFSShare s = null;
		for (VFSShare share : shares) {
			path = _getRelativePath(share, file);
			if (path != null) {
				s = share;
				break;
			}
		}
		if (path == null || s == null) {
			throw new FileNotFoundException(
					"Could not get the Relative Path for the specified file '"
							+ file.getFileName() + "'.");
		}
		path = path.substring(1);
		path = path.substring(path.indexOf(File.separator));
		Obfuscator o = s.getObfuscator();
		SecretKey okey = s.getObfuscationKey();
		String deobfPath = null;
		try {
			deobfPath = o.deObfuscatePath(path, okey);
		} catch (MissingIVException e) {
			// check for conflict
			deobfPath = o.resolveConflictCandidate(path, okey);
			// if we get a proposed new filename back, try renaming file
			if (deobfPath != null) {
				try {
					VirtualFile proposedName = getFile(File.separator
							+ s.getShareName() + deobfPath);
					file.renameTo(proposedName);
				} catch (IOException e1) {
					throw new ObfuscationException(
							"Unable to deobfuscate filename. Conflict resolution was successful, but renaming the conflicting file failed!",
							e1);
				}
			} else {
				throw new ObfuscationException(
						"Unable to deobfuscate filename. Conflict resolution was not successful",
						e);
			}
		}

		return File.separator + s.getShareName() + deobfPath;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.panbox.desktop.common.vfs.backend.IRootVolume#obfuscatePath(java.
	 * lang.String)
	 */
	@Override
	public String obfuscatePath(String fileName, boolean createivs)
			throws FileNotFoundException, ObfuscationException {
		if (fileName.equals(File.separator)) {
			return File.separator;
		}
		VFSShare share = null;
		for (VFSShare s : shares) {
			if (s.contains(fileName)) {
				share = s;
				break;
			}
		}
		if (share != null) {
			String path = fileName.substring(share.getShareName().length() + 1);
			return File.separator
					+ share.getShareName()
					+ share.getObfuscator().obfuscatePath(path,
							share.getObfuscationKey(), createivs);
		}
		throw new FileNotFoundException(
				"None of the mounted shares contains the specified file '"
						+ fileName + "'.");
	}
}
