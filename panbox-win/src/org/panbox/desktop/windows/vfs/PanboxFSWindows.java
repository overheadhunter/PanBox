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
package org.panbox.desktop.windows.vfs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.decasdev.dokan.CreationDisposition;
import net.decasdev.dokan.DokanDiskFreeSpace;
import net.decasdev.dokan.DokanFileInfo;
import net.decasdev.dokan.DokanVolumeInformation;
import net.decasdev.dokan.Win32FindData;

import org.apache.log4j.Logger;
import org.panbox.core.exception.ObfuscationException;
import org.panbox.core.keymgmt.ShareKey;
import org.panbox.core.vfs.backend.VirtualFile;
import org.panbox.desktop.common.ex.PanboxCreateFailedException;
import org.panbox.desktop.common.ex.PanboxDeleteFailedException;
import org.panbox.desktop.common.ex.PanboxEncryptionException;
import org.panbox.desktop.common.vfs.FileCreationFlags;
import org.panbox.desktop.common.vfs.PanboxFS;
import org.panbox.desktop.common.vfs.PanboxFSAdapter;
import org.panbox.desktop.common.vfs.backend.VirtualRandomAccessFile;
import org.panbox.desktop.common.vfs.backend.exceptions.SecretKeyNotFoundException;

public class PanboxFSWindows extends PanboxFS {

	private static final Logger logger = Logger
			.getLogger("org.panbox.desktop.common");

	private final static ConcurrentHashMap<Long, VirtualRandomAccessFile> fileInstanceTable = new ConcurrentHashMap<Long, VirtualRandomAccessFile>();

	// 1611421524 = P A N B O X in alphabet
	private final static AtomicInteger WINDOWS_DOKAN_VFS_SERIAL_NUMBER = new AtomicInteger(
			1611421524);

	/**
	 * {@link Random}-instance for file handle generation
	 */
	private final Random handlerandom = new Random(System.nanoTime());

	private long setHandle(DokanFileInfo info) {
		if (info.handle == 0) {
			return (info.handle = handlerandom.nextLong());
		} else {
			return info.handle;
		}
	}

	protected synchronized VirtualFile getVirtualFileForFileName(String fileName)
			throws SecretKeyNotFoundException, FileNotFoundException,
			ObfuscationException {
		return super.getVirtualFileForFileName(fileName, true);
	}

	public static FileCreationFlags fromDokan(final int value) {
		switch (value) {
		case CreationDisposition.CREATE_NEW:
			return FileCreationFlags.CREATE_NEW;
		case CreationDisposition.OPEN_ALWAYS:
			return FileCreationFlags.OPEN_ALWAYS;
		case CreationDisposition.OPEN_EXISTING:
			return FileCreationFlags.OPEN_EXISTING;
		case CreationDisposition.CREATE_ALWAYS:
			return FileCreationFlags.CREATE_ALWAYS;
		case CreationDisposition.TRUNCATE_EXISTING:
			return FileCreationFlags.TRUNCATE_EXISTING;
		}
		return null;
	}

	public PanboxFSWindows(PanboxFSAdapter backend) {
		super(backend);
	}

	public synchronized void closeFile(String fileName, DokanFileInfo fileInfo,
			boolean isDeletable) throws IOException {
		logger.debug("PanboxFS : closeFile : " + fileName + ", fileInfo: "
				+ fileInfo);

		if (!isDeletable) {
			VirtualRandomAccessFile v = fileInstanceTable.get(fileInfo.handle);
			if (v != null) {
				logger.debug("PanboxFS : closeFile : Found an instance with the handle number. Will close it now!");
				if (v.isOpened()) {
					v.close();
				}
			} else {
				logger.debug("PanboxFS : closeFile : Could not find instance to be closed. If this is a directory it should be just fine!");
			}
		}
		fileInstanceTable.remove(fileInfo.handle);
	}

	public synchronized void cleanup(String fileName, DokanFileInfo fileInfo)
			throws IOException {
		logger.debug("PanboxFS : cleanup : " + fileName + ", fileInfo: "
				+ fileInfo);

		VirtualRandomAccessFile v = fileInstanceTable.get(fileInfo.handle);
		if (v != null) {
			logger.debug("PanboxFS : cleanup : Found file, will flush it now!");
			if (v.isOpened()) {
				v.flush();
			}
			logger.debug("PanboxFS : cleanup : VirtualFile(" + fileName
					+ ").flush() was successful.");
		} else {
			logger.debug("PanboxFS : cleanup : Could not find instance to be flushed. If this is a directory it should be just fine!");
		}
	}

	public synchronized void createDirectory(String fileName,
			DokanFileInfo fileInfo) throws SecretKeyNotFoundException,
			FileNotFoundException, PanboxCreateFailedException {
		logger.debug("PanboxFS : createDirectory : " + fileName);
		try {
			// Only create directory if it does not exist already!
			if (!getVirtualFileForFileName(fileName, true).exists()) {
				boolean success = getVirtualFileForFileName(fileName, true)
						.createNewDirectory();
				if (!success) {
					throw new PanboxCreateFailedException(
							"Failed to create directory '" + fileName + "'.");
				} else {
					logger.debug("PanboxFS : createDirectory : VirtualFile("
							+ fileName
							+ ").createNewDirectory() was successful.");
				}
			}
		} catch (ObfuscationException e) {
			throw new PanboxCreateFailedException("Obfuscation failed!");
		}
	}

	public synchronized boolean createFile(String fileName,
			int creationDisposition, int flagsAndAttributes,
			boolean shareModeDeletable, boolean deleteOnClose,
			DokanFileInfo fileInfo) throws PanboxCreateFailedException,
			PathNotFoundException {
		final FileCreationFlags disposition = fromDokan(creationDisposition);

		logger.debug("PanboxFS : createFile : fileName: " + fileName
				+ ", disposition: " + disposition + ", flagsAndAttributes: "
				+ flagsAndAttributes + ", shareModeDeletable: "
				+ shareModeDeletable + ", deleteOnClose: " + deleteOnClose
				+ ", fileInfo: " + fileInfo);

		long handle = setHandle(fileInfo);

		try {
			VirtualFile virt = getVirtualFileForFileName(fileName, true);

			if (virt.isDirectory()) {
				// this is a directory!
				fileInfo.isDirectory = true;
				if (!virt.exists() && !disposition.shouldCreate()) {
					return false;
				}
				return true;
			} else {
				// this is a file!
				if (disposition.shouldCreate() || disposition.shouldOpen()) {
					@SuppressWarnings("resource")
					// will be closed in close call!
					VirtualRandomAccessFile file = (VirtualRandomAccessFile) virt;

					if (!file.exists()) {
						if (!getVirtualFileForFileName(
								fileName.substring(0,
										fileName.lastIndexOf("\\")), false)
								.exists()) {
							throw new PathNotFoundException();
						}
						if (!disposition.shouldCreate()) {
							return false;
						}
						try {
							ShareKey shareKey = backingStorage
									.getLatestShareKeyForFile(fileName);
							// -- BEGIN --
							/*
							 * This piece of code is needed since Windows
							 * sometimes still holds file instances after a
							 * delete call was done. Therefore the create()-call
							 * will cause an IOException with the message
							 * "Access denied.". After Windows gave this file
							 * free on file system the create call will work
							 * just fine. We will try this 10 times with a sleep
							 * of 10ms betweeen the files. This is a work-around
							 * for SVN and Git! It might also be needed for
							 * other files or applications that are being
							 * created and deleted in very short times.
							 */
							int maxTries = 10;
							while (maxTries > 0) {
								try {
									file.create(shareKey.version, shareKey.key);
									break;
								} catch (IOException ex) {
									--maxTries;
									logger.debug("PanboxFS : createFile : Exception creating file. Will try it "
											+ maxTries + " more tries.");
									try {
										Thread.sleep(10);
									} catch (InterruptedException e) {
										// ignore
									}
								}
							}
							if (maxTries == 0 && !file.isOpened()) {
								throw new PanboxCreateFailedException(
										"Could not create file. Perhaps access is denied!");
							}
							// -- END --
							if (deleteOnClose) {
								file.setDeleteOnClose();
							}
							logger.debug("PanboxFS : createFile : VirtualFile("
									+ fileName + ").create() was successful.");
						} catch (IOException | PanboxEncryptionException e) {
							throw new PanboxCreateFailedException(
									"Failed to create file '" + fileName
											+ "': ", e);
						}
					} else {
						if (!shareModeDeletable) {
							file.open();
							file.initWithShareKey(backingStorage
									.getShareKeyForFile(fileName,
											file.getShareKeyVersion()));
							if (deleteOnClose) {
								file.setDeleteOnClose();
							}
							logger.debug("PanboxFS : createFile : VirtualFile("
									+ fileName
									+ ").open() and .init() was successful.");
						} else {
							logger.debug("PanboxFS : createFile : VirtualFile("
									+ fileName
									+ ") should be deletable. Therefor it has not been opened!");
							if (System.getProperty("os.version").equals("6.2")
									|| System.getProperty("os.version").equals(
											"6.3")) {
								// Windows 8, 8.1, 2012 Server, 2012 R2 Server
								// workaround!

								// The DeleteFile call is not working in Dokan
								// on Windows 8 so we need to run this
								// workaround here in order to remove the file!
								boolean deleted = file.delete();
								if (deleted) {
									logger.debug("PanboxFS : createFile : Delete workaround successful!");
								} else {
									logger.debug("PanboxFS : createFile : Delete workaround failed!");
								}
								return true;
							}
						}
						logger.debug("PanboxFS : createFile : shouldCreate or shouldOpen for '"
								+ fileName + "' has been successful.");
					}
				} else if (disposition.shouldTruncate()) {
					try {
						@SuppressWarnings("resource")
						// will be closed in close call!
						VirtualRandomAccessFile file = (VirtualRandomAccessFile) virt;

						file.open();
						file.initWithShareKey(backingStorage
								.getShareKeyForFile(fileName,
										file.getShareKeyVersion()));

						file.setLength(0);
					} catch (IOException | PanboxEncryptionException e) {
						throw new PanboxCreateFailedException(
								"Failed to open existing file for truncating '"
										+ fileName + "'.");
					}
				}

				if (disposition.hasToExist()) {
					try {
						getFileInfo(fileName, false, true);
					} catch (IOException ex) {
						logger.debug("PanboxFS : createFile : creation.hasToExist() but did not exist.");
						return false;
					}
				}

				fileInstanceTable.put(handle, (VirtualRandomAccessFile) virt);

				logger.debug("PanboxFS : createFile : handle for file "
						+ fileName + " has been set: " + fileInfo);

				return true;
			}
		} catch (IOException | ObfuscationException | PanboxEncryptionException e) {
			throw new PanboxCreateFailedException(
					"Failed to get VirtualFile for filename '" + fileName
							+ "':", e);
		}
	}

	public synchronized void deleteDirectory(String fileName,
			DokanFileInfo fileInfo) throws IOException,
			PanboxDeleteFailedException {
		logger.debug("PanboxFS : deleteDirectory : " + fileName);
		try {
			// there is no need to do some manual recursion anymore.
			boolean success = getVirtualFileForFileName(fileName).delete();
			if (!success) {
				logger.error("PanboxFS : deleteDirectory : VirtualFile("
						+ fileName + ").deleteDirectory() failed.");
				throw new PanboxDeleteFailedException("VirtualFile(" + fileName
						+ ").deleteDirectory() failed.");
			} else {
				logger.debug("PanboxFS : deleteDirectory : VirtualFile("
						+ fileName + ").deleteDirectory() was successful.");
			}
		} catch (ObfuscationException e) {
			throw new IOException("Obfuscation failed!");
		}
	}

	public synchronized void deleteFile(String fileName, DokanFileInfo fileInfo)
			throws PanboxDeleteFailedException, IOException {
		logger.debug("PanboxFS : deleteFile : " + fileName);

		VirtualRandomAccessFile v = fileInstanceTable.get(fileInfo.handle);
		if (v != null) {
			logger.debug("PanboxFS : deleteFile : Found an instance with the handle number. Will delete it now!");

			while (v.isOpened()) {
				logger.debug("PanboxFS : deleteFile : Instance is still opened! Will close it before deletion.");
				v.close();
			}

			// -- BEGIN --
			/*
			 * This piece of code is needed since Windows sometimes still holds
			 * file instances after a close call was done. Therefore the
			 * delete()-call will cause an IOException with the message
			 * "Access denied.". After Windows gave this file free on file
			 * system the delete call will work just fine. We will try this 10
			 * times with a sleep of 10ms between the files. This is a
			 * work-around for SVN and Git! It might also be needed for other
			 * files or applications that are being created and deleted in very
			 * short times.
			 */
			int maxTries = 10;
			boolean success = false;
			while (maxTries > 0) {
				success = v.delete();
				if (success) {
					break;
				} else {
					--maxTries;
					logger.debug("PanboxFS : deleteFile : Error deleting file. Will try it "
							+ maxTries + " more tries.");
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// ignore
					}
				}
			}
			// -- END --
			if (!success) {
				logger.error("PanboxFS : deleteFile : VirtualFile(" + fileName
						+ ").deleteFile() failed.");
				throw new PanboxDeleteFailedException("VirtualFile(" + fileName
						+ ").deleteFile() failed.");
			} else {
				logger.debug("PanboxFS : deleteFile : VirtualFile(" + fileName
						+ ").deleteFile() was successful.");
			}
		} else {
			logger.error("PanboxFS : closeFile : Could not find instance to be deleted. Ignored request.");
			throw new PanboxDeleteFailedException(
					"Could not find handle for rquest: VirtualFile(" + fileName
							+ ").");
		}
	}

	public synchronized Win32FindData[] findFiles(String fileName,
			String pattern, DokanFileInfo fileInfo)
			throws FileNotFoundException {
		logger.debug("PanboxFS : findFiles : " + fileName);
		VirtualFile[] files = null;
		try {
			if (fileName.equals(File.separator)) {
				files = backingStorage.getFile(fileName).list();
			} else {
				files = getVirtualFileForFileName(fileName).list();
			}
		} catch (IOException | ObfuscationException e) {
			files = new VirtualFile[0];
		}

		final ArrayList<String> list = new ArrayList<String>();
		for (final VirtualFile s : files) {
			// Ignore files starts with .dropbox or are .panbox and desktop.ini
			String nameOfFile = s.getFileName().substring(
					s.getFileName().lastIndexOf(File.separator) + 1);
			logger.trace("PanboxFS : findFiles : nameOfFile: " + nameOfFile);
			if (nameOfFile.toLowerCase().startsWith(".dropbox")
					|| nameOfFile.toLowerCase().equals("desktop.ini")
					|| nameOfFile.toLowerCase().equals(".panbox")) {
				// ignore file
				logger.trace("PanboxFS : findFiles : Ignoring file starting with '.': "
						+ nameOfFile + " (" + fileName + ")");
				continue;
			}

			list.add(backingStorage.getRelativePathForFile(s));
		}
		final List<Win32FindData> data = new ArrayList<Win32FindData>();
		FileInfo info;
		for (final String s : list) {
			try {
				info = getFileInformation(s, true, false, null);
			} catch (SecretKeyNotFoundException e) {
				logger.error(
						"DokanUserFS::onFindFiles(_getFileInfo) : Caught exception SecretKeyNotFoundException from PanboxFS, Ignore file: ",
						e);
				continue;
			} catch (FileNotFoundException e) {
				logger.error(
						"DokanUserFS::onFindFiles(_getFileInfo) : Caught exception FileNotFoundException from PanboxFS, Ignore file: ",
						e);
				continue;
			} catch (IOException e) {
				logger.error(
						"DokanUserFS::onFindFiles(_getFileInfo) : Caught exception IOException from PanboxFS, Ignore file: ",
						e);
				continue;
			}
			data.add(info.toFindData());
		}
		return data.toArray(new Win32FindData[] {});
	}

	public synchronized void flushFileBuffers(String fileName,
			DokanFileInfo fileInfo) throws IOException {
		logger.debug("PanboxFS : flushFileBuffers : Will forward flush to cleanup call!");

		cleanup(fileName, fileInfo);
	}

	public synchronized DokanDiskFreeSpace getDiskFreeSpace(
			DokanFileInfo fileInfo) {
		return new DokanDiskFreeSpace(getFreeBytes(), getUsableBytes(),
				getTotalBytes());
	}

	public synchronized FileInfo getFileInformation(String fileName,
			boolean alreadyObfuscated, boolean outputObfuscated,
			DokanFileInfo fileInfo) throws IOException {
		logger.debug("PanboxFS : getFileInformation : " + fileName
				+ ", alreadyObfuscated: " + alreadyObfuscated
				+ ", outputObfuscated: " + outputObfuscated);

		return (FileInfo) getFileInfo(fileName, alreadyObfuscated,
				outputObfuscated);
	}

	public synchronized DokanVolumeInformation getVolumeInformation(
			String volumeName, DokanFileInfo fileInfo) {
		logger.debug("PanboxFS : getVolumeInformation : " + volumeName
				+ ", fileInfo: " + fileInfo);

		return new DokanVolumeInformation(getVolumeName(), getFilesystemName(),
				50, WINDOWS_DOKAN_VFS_SERIAL_NUMBER.get());
	}

	public synchronized void lockFile(String fileName, long byteOffset,
			long length, DokanFileInfo fileInfo) {
		logger.debug("PanboxFS : lockFile : No implementation executed!");
	}

	public synchronized void moveFile(String existingFileName,
			String newFileName, boolean replaceExisiting, DokanFileInfo fileInfo)
			throws IOException {
		logger.debug("PanboxFS : moveFile : " + existingFileName + " -> "
				+ newFileName);

		try {
			final VirtualFile newFile = getVirtualFileForFileName(newFileName,
					true);
			if (newFile.exists() && !replaceExisiting) {
				throw new FileAlreadyExistsException(newFileName, newFile
						.getFile().getAbsolutePath(),
						"New file exists and should not be replaced.");
			}

			VirtualRandomAccessFile v = fileInstanceTable.get(fileInfo.handle);
			if (v != null) {
				logger.debug("PanboxFS : moveFile : Found an instance with the handle number. Will move it now!");

				while (v.isOpened()) {
					logger.debug("PanboxFS : moveFile : Instance is still opened! Will flush and close it before moving.");
					v.flush();
					v.close();
				}

				// -- BEGIN --
				/*
				 * This piece of code is needed since Windows sometimes still
				 * holds file instances after a close call was done. Therefore
				 * the renameTo()-call will cause an IOException with the
				 * message "Access denied.". After Windows gave this file free
				 * on file system the move call will work just fine. We will try
				 * this 10 times with a sleep of 10ms between the files. This is
				 * a work-around for SVN and Git! It might also be needed for
				 * other files or applications that are being created and
				 * deleted in very short times.
				 */
				int maxTries = 10;
				boolean success = false;
				while (maxTries > 0) {
					success = v.renameTo(newFile);
					if (success) {
						break;
					} else {
						--maxTries;
						logger.debug("PanboxFS : moveFile : Error moving file. Will try it "
								+ maxTries + " more tries.");
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							// ignore
						}
					}
				}
				// -- END --

				if (!success) {
					logger.error("PanboxFS : moveFile : VirtualFile("
							+ existingFileName + ").renameTo(" + newFileName
							+ ") failed.");
				} else {
					logger.debug("PanboxFS : moveFile : VirtualFile("
							+ existingFileName + ").renameTo(" + newFileName
							+ ") was successful.");
				}
			} else {
				logger.debug("PanboxFS : moveFile : Could not find instance to be moved. If this is a directory it should be just fine!");

				boolean success = getVirtualFileForFileName(existingFileName)
						.renameTo(newFile);
				if (!success) {
					logger.error("PanboxFS : moveFile : VirtualFile("
							+ existingFileName + ").renameTo(" + newFileName
							+ ") failed.");
				} else {
					logger.debug("PanboxFS : moveFile : VirtualFile("
							+ existingFileName + ").renameTo(" + newFileName
							+ ") was successful.");
				}
			}
		} catch (ObfuscationException e) {
			throw new IOException("Obfuscation failed!");
		}

	}

	public synchronized long openDirectory(String fileName,
			DokanFileInfo fileInfo) {
		logger.debug("PanboxFS : openDirectory : " + fileName + ", fileInfo: "
				+ fileInfo);

		return fileInfo.handle;
	}

	public synchronized int readFile(String fileName, ByteBuffer buffer,
			long offset, DokanFileInfo fileInfo) throws IOException,
			PanboxEncryptionException {
		logger.debug("PanboxFS : readFile : " + fileName + ", fileInfo: "
				+ fileInfo);

		VirtualRandomAccessFile v = fileInstanceTable.get(fileInfo.handle);
		if (v != null) {
			logger.debug("PanboxFS : readFile : Found an instance with the handle number. Will delete it now!");

			logger.debug("PanboxFS : readFile, File : " + v + ", buffersize: "
					+ buffer.remaining() + ", offset: " + offset);
			try {
				return v.read(offset, buffer);
			} catch (PanboxEncryptionException ex) {
				throw new PanboxEncryptionException(
						"PanboxFS : readFile : Decryption failed with exception: ",
						ex);
			}
		} else {
			logger.error("PanboxFS : readFile : Could not find instance to be read. Ignored request.");
			throw new FileNotFoundException(
					"PanboxFS : readFile : Could not find handle for rquest: VirtualFile("
							+ fileName + ").");
		}
	}

	public synchronized void setEndOfFile(String fileName, long length,
			DokanFileInfo fileInfo) throws IOException,
			PanboxEncryptionException {
		logger.debug("PanboxFS : setEndOfFile : " + fileName);

		// end of file requests are ignored at this point as size will be
		// managed by VirtualRandomAccessFile
	}

	public synchronized void setFileAttributes(String fileName,
			int fileAttributes, DokanFileInfo fileInfo) {
		logger.debug("PanboxFS : setFileAttributes : No implementation executed!");
	}

	public synchronized void setFileTime(String fileName, long creationTime,
			long lastAccessTime, long lastWriteTime, DokanFileInfo fileInfo) {
		logger.debug("PanboxFS : setFileTime : No implementation executed!");
	}

	public synchronized void unlockFile(String fileName, long byteOffset,
			long length, DokanFileInfo fileInfo) {
		logger.debug("PanboxFS : unlockFile : No implementation executed!");
	}

	public synchronized int writeFile(String fileName, ByteBuffer buffer,
			long offset, DokanFileInfo fileInfo) throws IOException,
			PanboxEncryptionException {
		logger.debug("PanboxFS : writeFile : " + fileName + ", fileInfo: "
				+ fileInfo);

		VirtualRandomAccessFile v = fileInstanceTable.get(fileInfo.handle);
		if (v != null) {
			logger.debug("PanboxFS : writeFile : Found an instance with the handle number. Will delete it now!");

			logger.debug("PanboxFS : writeFile : File: " + v + ", buffersize: "
					+ buffer.remaining() + ", offset: " + offset);

			if (!v.canWrite()) {
				logger.error("PanboxFS : writeFile : File instance was opened for readOnly!");
				throw new IllegalAccessError();
			}

			return v.write(offset, buffer);
		} else {
			logger.error("PanboxFS : writeFile : Could not find instance to be written. Ignored request.");
			throw new FileNotFoundException(
					"PanboxFS : writeFile : Could not find handle for request: VirtualFile("
							+ fileName + ").");
		}
	}

	public synchronized void unmount(DokanFileInfo fileInfo) {
		logger.debug("PanboxFS : unmount : No implementation executed!");
	}

	public void beforeUnmount(File mountPoint) {
		logger.info("PanboxFS : beforeUnmount : Panbox Service is about to shutdown - flush and close all open AES* instances...");
		Collection<VirtualRandomAccessFile> coll = fileInstanceTable.values();
		for (Iterator<VirtualRandomAccessFile> it = coll.iterator(); it
				.hasNext();) {
			VirtualRandomAccessFile file = it.next();
			try {
				try {
					file.flush();
				} catch (NullPointerException ex) {
					logger.warn("PanboxFS : beforeUnmount : File '"
							+ file.getFileName()
							+ "' was already flushed. No cache set.");
				}
				if (file.isOpened()) {
					file.close();
				} else {
					logger.debug("PanboxFS : beforeUnmount : File war already closed. No need to close this file.");
				}
				logger.info("PanboxFS : beforeUnmount : Successfully closed VirtualFile instance for file "
						+ file.getFileName());
			} catch (Exception e) {
				logger.error(
						"PanboxFS : beforeUnmount : Error on closing VirtualFile instance for file "
								+ file.getFileName(), e);
			}
		}
	}
}
