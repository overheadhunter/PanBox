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

import static net.decasdev.dokan.WinError.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileAlreadyExistsException;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.panbox.desktop.common.ex.PanboxCreateFailedException;
import org.panbox.desktop.common.ex.PanboxDeleteFailedException;
import org.panbox.desktop.common.ex.PanboxEncryptionException;
import org.panbox.desktop.common.vfs.AbstractFileInfo;
import org.panbox.desktop.common.vfs.PanboxFS;
import org.panbox.desktop.common.vfs.PanboxFSAdapter;

import net.decasdev.dokan.ByHandleFileInformation;
import net.decasdev.dokan.Dokan;
import net.decasdev.dokan.DokanDiskFreeSpace;
import net.decasdev.dokan.DokanFileInfo;
import net.decasdev.dokan.DokanOperationException;
import net.decasdev.dokan.DokanOperations;
import net.decasdev.dokan.DokanOptions;
import net.decasdev.dokan.DokanVolumeInformation;
import net.decasdev.dokan.FileFlag;
import net.decasdev.dokan.FileShareMode;
import net.decasdev.dokan.Win32FindData;

public class DokanUserFS implements PanboxFSAdapter, DokanOperations {

	private static final Logger logger = Logger
			.getLogger("org.panbox.desktop.common");

	private final Hashtable<Long, Boolean> isDeletableTable = new Hashtable<Long, Boolean>();

	private PanboxFSWindows panboxFS = null;
	private String mountPoint = null;

	// ==== PanboxFSAdapter methods ====

	@Override
	public boolean userfs_mount(PanboxFS panboxFS, File mountPoint) {
		if (!(panboxFS instanceof PanboxFSWindows)) {
			logger.fatal(getClass().getName()
					+ " : Dokan implementation requires an instance of PanboxFSWindows!");
			return false;
		} else {
			this.panboxFS = (PanboxFSWindows) panboxFS;
			this.mountPoint = mountPoint.getAbsolutePath();
			try {
				Dokan.removeMountPoint(this.mountPoint);
			} catch (final Throwable ex) {
				// If unmount of existing failed exit with error!
				logger.fatal(getClass().getName()
						+ " : userfs_mount : Exception: " + ex.getMessage());
				return false;
			}
			final int result = Dokan.mount(new DokanOptions(this.mountPoint, 0,
					0), this);
			if (result != Dokan.DOKAN_SUCCESS) {
				logger.fatal(getClass().getName() + " : Dokan mount failed: "
						+ Dokan.getErrorString(result));
			}
			return result == Dokan.DOKAN_SUCCESS;
		}
	}

	@Override
	public boolean userfs_unmount(File mountPoint) {
		// Flushes and closes all files
		panboxFS.beforeUnmount(mountPoint);

		boolean unmount = Dokan.unmount(this.mountPoint.charAt(0));
		boolean remove = Dokan.removeMountPoint(mountPoint.getAbsolutePath());
		logger.info(getClass().getName()
				+ " : userfs_unmount, Unmount successful: " + unmount
				+ ", Remove successful: " + remove);
		return (unmount && remove);
	}

	@Override
	public AbstractFileInfo createFileInfo(String fileName,
			boolean isDirectory, long fileSize, long creationTime,
			long lastAccessTime, long lastWriteTime, long attr,
			boolean isSymbolic) {
		return new FileInfo(fileName, isDirectory, fileSize, creationTime,
				lastAccessTime, lastWriteTime);
	}

	// ==== DokanOperations Windows specific methods ====

	@Override
	public void onCleanup(String fileName, DokanFileInfo fileInfo)
			throws DokanOperationException {
		logger.debug(getClass().getName() + " : onCleanup (fileName: "
				+ fileName + ", fileInfo: " + fileInfo + ")");
		try {
			panboxFS.cleanup(fileName, fileInfo);
		} catch (IOException e) {
			logger.error(getClass().getName() + " : onCleanup : Exception: ", e);
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		} catch (Exception e) {
			logger.error(getClass().getName()
					+ " : onCleanup : Unknown Exception: ", e);
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		}

	}

	@Override
	public void onCloseFile(String fileName, DokanFileInfo fileInfo)
			throws DokanOperationException {
		logger.debug(getClass().getName() + " : onCloseFile (fileName: "
				+ fileName + ", fileInfo: " + fileInfo + ")");
		try {
			boolean isDeletable = (isDeletableTable.get(fileInfo.handle) != null)
					&& (isDeletableTable.get(fileInfo.handle));
			panboxFS.closeFile(fileName, fileInfo, isDeletable);
			isDeletableTable.remove(fileInfo.handle);
		} catch (IOException e) {
			logger.error(getClass().getName() + " : onCloseFile : Exception: ",
					e);
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		} catch (Exception e) {
			logger.error(getClass().getName()
					+ " : onCloseFile : Unknown Exception: ", e);
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		}
	}

	@Override
	public void onCreateDirectory(String fileName, DokanFileInfo fileInfo)
			throws DokanOperationException {
		logger.debug(getClass().getName() + " : onCreateDirectory (fileName: "
				+ fileName + ", fileInfo: " + fileInfo + ")");
		try {
			panboxFS.createDirectory(fileName, fileInfo);
		} catch (FileNotFoundException | PanboxCreateFailedException e) {
			logger.error(getClass().getName()
					+ " : onCreateDirectory : Exception: ", e);
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		} catch (Exception e) {
			logger.error(getClass().getName()
					+ " : onCreateDirectory : Unknown Exception: ", e);
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		}
	}

	@Override
	public long onCreateFile(String fileName, int desiredAccess, int shareMode,
			int creationDisposition, int flagsAndAttributes,
			DokanFileInfo fileInfo) throws DokanOperationException {
		logger.debug(getClass().getName() + " : onCreateFile (fileName: "
				+ fileName + ", desiredAccess: " + desiredAccess
				+ ", shareMode: " + shareMode + ", creationDisposition: "
				+ creationDisposition + ", flagsAndAttributes: "
				+ flagsAndAttributes + ", fileInfo: " + fileInfo + ")");
		try {
			// boolean readonly = ((desiredAccess & FileAccess.GENERIC_WRITE) ==
			// 0);

			// Some applications open files event with FILE_SHARE_DELETE in case
			// they don't want to delete it at all!
			// Example: Windows Media Player
			boolean isDeletable = ((shareMode & FileShareMode.FILE_SHARE_DELETE) != 0
					&& (shareMode & FileShareMode.FILE_SHARE_READ) == 0 && (shareMode & FileShareMode.FILE_SHARE_WRITE) == 0);

			// Some applications create temp files with deleteOnClose file
			// attribute. They will be removed once all handles are closed!
			boolean deleteOnClose = ((flagsAndAttributes & FileFlag.FILE_FLAG_DELETE_ON_CLOSE) != 0);

			if (!panboxFS.createFile(fileName, creationDisposition,
					flagsAndAttributes, isDeletable, deleteOnClose, fileInfo)) {
				// exception will be thrown at the end of this method!
			} else {
				isDeletableTable.put(fileInfo.handle, isDeletable);
				return fileInfo.handle;
			}
		} catch (PathNotFoundException e) {
			logger.debug(getClass().getName()
					+ " : onCreateFile : Path was not available for the requested file.");
			throw new DokanOperationException(ERROR_PATH_NOT_FOUND);
		} catch (PanboxCreateFailedException e) {
			// ignore non-existing files in log (otherwise log might be full of
			// spam!)
			if (!e.getMessage().contains(
					"Failed to get VirtualFile for filename")) {
				logger.error(getClass().getName()
						+ " : onCreateFile : Exception: ", e);
			}
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		} catch (Exception e) {
			logger.error(getClass().getName()
					+ " : onCreateFile : Unknown Exception: ", e);
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		}
		throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
	}

	@Override
	public void onDeleteDirectory(String fileName, DokanFileInfo fileInfo)
			throws DokanOperationException {
		logger.debug(getClass().getName() + " : onDeleteDirectory (fileName: "
				+ fileName + ", fileInfo: " + fileInfo + ")");
		try {
			panboxFS.deleteDirectory(fileName, fileInfo);
		} catch (IOException e) {
			logger.error(getClass().getName()
					+ " : onDeleteDirectory : Exception: ", e);
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		} catch (PanboxDeleteFailedException e) {
			logger.error(getClass().getName()
					+ " : onDeleteDirectory : Exception: ", e);
			throw new DokanOperationException(ERROR_BUSY);
		} catch (Exception e) {
			logger.error(getClass().getName()
					+ " : onDeleteDirectory : Unknown Exception: ", e);
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		}

	}

	@Override
	public void onDeleteFile(String fileName, DokanFileInfo fileInfo)
			throws DokanOperationException {
		logger.debug(getClass().getName() + " : onDeleteFile (fileName: "
				+ fileName + ", fileInfo: " + fileInfo + ")");
		try {
			panboxFS.deleteFile(fileName, fileInfo);
		} catch (FileNotFoundException e) {
			logger.error(
					getClass().getName() + " : onDeleteFile : Exception: ", e);
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		} catch (PanboxDeleteFailedException e) {
			logger.error(
					getClass().getName() + " : onDeleteFile : Exception: ", e);
			throw new DokanOperationException(ERROR_BUSY);
		} catch (Exception e) {
			logger.error(getClass().getName()
					+ " : onDeleteFile : Unknown Exception: ", e);
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		}

	}

	@Override
	public Win32FindData[] onFindFiles(String pathName, DokanFileInfo fileInfo)
			throws DokanOperationException {
		logger.debug(getClass().getName() + " : onFindFiles (pathName: "
				+ pathName + ", fileInfo: " + fileInfo + ")");
		try {
			return panboxFS.findFiles(pathName, null, fileInfo);
		} catch (FileNotFoundException e) {
			logger.error(getClass().getName() + " : onFindFiles : Exception: ",
					e);
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		} catch (Exception e) {
			logger.error(getClass().getName()
					+ " : onFindFiles : Unknown Exception: ", e);
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		}
	}

	@Override
	public Win32FindData[] onFindFilesWithPattern(String pathName,
			String searchPattern, DokanFileInfo fileInfo)
			throws DokanOperationException {
		logger.debug(getClass().getName()
				+ " : onFindFilesWithPattern (pathName: " + pathName
				+ ", searchPattern: " + searchPattern + ", fileInfo: "
				+ fileInfo + ")");
		try {
			return panboxFS.findFiles(pathName, searchPattern, fileInfo);
		} catch (FileNotFoundException e) {
			logger.error(getClass().getName()
					+ " : onFindFilesWithPattern : Exception: ", e);
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		} catch (Exception e) {
			logger.error(getClass().getName()
					+ " : onFindFilesWithPattern : Unknown Exception: ", e);
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		}
	}

	@Override
	public void onFlushFileBuffers(String fileName, DokanFileInfo fileInfo)
			throws DokanOperationException {
		logger.debug(getClass().getName() + " : onFlushFileBuffers (fileName: "
				+ fileName + ", fileInfo: " + fileInfo + ")");
		try {
			panboxFS.flushFileBuffers(fileName, fileInfo);
		} catch (IOException e) {
			logger.error(getClass().getName()
					+ " : onFlushFileBuffers : Exception: ", e);
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		} catch (Exception e) {
			logger.error(getClass().getName()
					+ " : onFlushFileBuffers : Unknown Exception: ", e);
			throw new DokanOperationException(ERROR_INVALID_PARAMETER);
		}
	}

	@Override
	public DokanDiskFreeSpace onGetDiskFreeSpace(DokanFileInfo fileInfo)
			throws DokanOperationException {
		logger.debug(getClass().getName() + " : onGetDiskFreeSpace (fileInfo: "
				+ fileInfo + ")");
		return panboxFS.getDiskFreeSpace(fileInfo);
	}

	@Override
	public ByHandleFileInformation onGetFileInformation(String fileName,
			DokanFileInfo fileInfo) throws DokanOperationException {
		logger.debug(getClass().getName()
				+ " : onGetFileInformation (fileName: " + fileName
				+ ", fileInfo: " + fileInfo + ")");
		try {
			return panboxFS
					.getFileInformation(fileName, false, false, fileInfo)
					.toByhandleFileInformation(0);
		} catch (IOException e) {
			logger.error(getClass().getName()
					+ " : onGetFileInformation : Exception: ", e);
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		} catch (Exception e) {
			logger.error(getClass().getName()
					+ " : onGetFileInformation : Unknown Exception: ", e);
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		}
	}

	@Override
	public DokanVolumeInformation onGetVolumeInformation(String volumeName,
			DokanFileInfo fileInfo) throws DokanOperationException {
		logger.debug(getClass().getName()
				+ " : onGetVolumeInformation (volumeName: " + volumeName
				+ ", fileInfo: " + fileInfo + ")");
		return panboxFS.getVolumeInformation(volumeName, fileInfo);
	}

	@Override
	public void onLockFile(String fileName, long byteOffset, long length,
			DokanFileInfo fileInfo) throws DokanOperationException {
		logger.debug(getClass().getName() + " : onLockFile (fileName: "
				+ fileName + ", byteOffset: " + byteOffset + ", length: "
				+ length + ", fileInfo: " + fileInfo + ")");
		panboxFS.lockFile(fileName, byteOffset, length, fileInfo);
	}

	@Override
	public void onMoveFile(String existingFileName, String newFileName,
			boolean replaceExisiting, DokanFileInfo fileInfo)
			throws DokanOperationException {
		logger.debug(getClass().getName() + " : onMoveFile (existingFileName: "
				+ existingFileName + ", newFileName: " + newFileName
				+ ", replaceExisiting: " + replaceExisiting + ", fileInfo: "
				+ fileInfo + ")");
		try {
			panboxFS.moveFile(existingFileName, newFileName, replaceExisiting,
					fileInfo);
		} catch (FileAlreadyExistsException | FileNotFoundException e) {
			logger.error(getClass().getName() + " : onMoveFile : Exception: ",
					e);
			throw new DokanOperationException(ERROR_FILE_EXISTS);
		} catch (IOException e) {
			logger.error(getClass().getName() + " : onMoveFile : Exception: ",
					e);
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		} catch (Exception e) {
			logger.error(getClass().getName()
					+ " : onMoveFile : Unknown Exception: ", e);
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		}
	}

	@Override
	public long onOpenDirectory(String fileName, DokanFileInfo fileInfo)
			throws DokanOperationException {
		logger.debug(getClass().getName() + " : onOpenDirectory (fileName: "
				+ fileName + ", fileInfo: " + fileInfo + ")");
		return panboxFS.openDirectory(fileName, fileInfo);
	}

	@Override
	public int onReadFile(String fileName, ByteBuffer buffer, long offset,
			DokanFileInfo fileInfo) throws DokanOperationException {
		logger.debug(getClass().getName() + " : onReadFile (fileName: "
				+ fileName + ", offset: " + offset + ", fileInfo: " + fileInfo
				+ ")");
		try {
			return panboxFS.readFile(fileName, buffer, offset, fileInfo);
		} catch (IOException | PanboxEncryptionException e) {
			logger.error(getClass().getName() + " : onReadFile : Exception: ",
					e);
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		} catch (Exception e) {
			logger.error(getClass().getName()
					+ " : onReadFile : Unknown Exception: ", e);
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		}
	}

	@Override
	public void onSetEndOfFile(String fileName, long length,
			DokanFileInfo fileInfo) throws DokanOperationException {
		logger.debug(getClass().getName() + " : onSetEndOfFile (fileName: "
				+ fileName + ", length: " + length + ", fileInfo: " + fileInfo
				+ ")");
		try {
			panboxFS.setEndOfFile(fileName, length, fileInfo);
		} catch (IOException | PanboxEncryptionException e) {
			logger.error(getClass().getName()
					+ " : onSetEndOfFile : Exception: ", e);
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		} catch (Exception e) {
			logger.error(getClass().getName()
					+ " : onSetEndOfFile : Unknown Exception: ", e);
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		}
	}

	@Override
	public void onSetFileAttributes(String fileName, int fileAttributes,
			DokanFileInfo fileInfo) throws DokanOperationException {
		logger.debug(getClass().getName()
				+ " : onSetFileAttributes (fileName: " + fileName
				+ ", fileAttributes: " + fileAttributes + ", fileInfo: "
				+ fileInfo + ")");
		panboxFS.setFileAttributes(fileName, fileAttributes, fileInfo);
	}

	@Override
	public void onSetFileTime(String fileName, long creationTime,
			long lastAccessTime, long lastWriteTime, DokanFileInfo fileInfo)
			throws DokanOperationException {
		logger.debug(getClass().getName() + " : onSetFileTime (fileName: "
				+ fileName + ", creationTime: " + creationTime
				+ ", lastAccessTime: " + lastAccessTime + ", lastWriteTime: "
				+ lastWriteTime + ", fileInfo: " + fileInfo + ")");
		panboxFS.setFileTime(fileName, creationTime, lastAccessTime,
				lastWriteTime, fileInfo);
	}

	@Override
	public void onUnlockFile(String fileName, long byteOffset, long length,
			DokanFileInfo fileInfo) throws DokanOperationException {
		logger.debug(getClass().getName() + " : onUnlockFile (fileName: "
				+ fileName + ", byteOffset: " + byteOffset + ", length: "
				+ length + ", fileInfo: " + fileInfo + ")");
		panboxFS.unlockFile(fileName, byteOffset, length, fileInfo);
	}

	@Override
	public void onUnmount(DokanFileInfo fileInfo)
			throws DokanOperationException {
		logger.debug(getClass().getName() + " : onUnmount (fileInfo: "
				+ fileInfo + ")");
		panboxFS.unmount(fileInfo);
	}

	@Override
	public int onWriteFile(String fileName, ByteBuffer buffer, long offset,
			DokanFileInfo fileInfo) throws DokanOperationException {
		logger.debug(getClass().getName() + " : onWriteFile (fileName: "
				+ fileName + ", offset: " + offset + ", fileInfo: " + fileInfo
				+ ")");
		try {
			return panboxFS.writeFile(fileName, buffer, offset, fileInfo);
		} catch (IOException | PanboxEncryptionException e) {
			logger.error(getClass().getName() + " : onWriteFile : Exception: ",
					e);
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		} catch (Exception e) {
			logger.error(getClass().getName()
					+ " : onWriteFile : Unknown Exception: ", e);
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		}
	}

	@Override
	public String getPathToBackendFile(String fileName)
			throws DokanOperationException {
		return null;
	}

}
