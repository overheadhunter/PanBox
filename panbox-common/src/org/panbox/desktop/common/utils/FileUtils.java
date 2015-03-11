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
package org.panbox.desktop.common.utils;

import static java.nio.file.FileVisitResult.CONTINUE;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.log4j.Logger;

/**
 * @author palige
 * 
 *         Helper class for FS I/O operations.
 * 
 */
public class FileUtils {

	private final static Logger logger = Logger.getLogger(FileUtils.class);

	private static class FileDeletionVisitor extends SimpleFileVisitor<Path> {

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attr)
				throws IOException {
			if (attr.isSymbolicLink()) {
				logger.debug("Deleting symlink " + file);
				Files.delete(file);
			} else if (attr.isRegularFile()) {
				logger.debug("Deleting regular file " + file);
				Files.delete(file);
			} else {
				logger.info("Skipping other file " + file);
			}
			return CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc)
				throws IOException {
			if (exc == null) {
				try {
					logger.debug("Deleting directory " + dir);
					Files.delete(dir);
				} catch (DirectoryNotEmptyException e) {
					logger.warn("Failed deleting non-empty directory " + dir);
				}
				return CONTINUE;
			} else {
				throw exc;
			}
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) {
			logger.error("Deletion of file " + file + " failed.", exc);
			return CONTINUE;
		}
	}

	/**
	 * Deletes the given directory structure including all regular files,
	 * subdirectories and symlinks.
	 * 
	 * @param dir
	 * @throws IOException
	 */
	public static void deleteDirectoryTree(File dir)
			throws AccessDeniedException, IOException {
		dir.delete();
		Files.walkFileTree(dir.toPath(), new FileDeletionVisitor());
	}

}
