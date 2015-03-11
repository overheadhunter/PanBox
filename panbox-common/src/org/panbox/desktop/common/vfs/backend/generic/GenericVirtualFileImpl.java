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
package org.panbox.desktop.common.vfs.backend.generic;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Hashtable;

import javax.crypto.SecretKey;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.panbox.core.crypto.io.AESGCMRandomAccessFileCompat;
import org.panbox.core.exception.FileEncryptionException;
import org.panbox.core.exception.FileIntegrityException;
import org.panbox.core.vfs.backend.VirtualFile;
import org.panbox.core.vfs.backend.VirtualVolume;
import org.panbox.desktop.common.ex.PanboxEncryptionException;
import org.panbox.desktop.common.ex.PanboxIntegrityException;
import org.panbox.desktop.common.vfs.backend.VirtualRandomAccessFile;

public class GenericVirtualFileImpl extends VirtualRandomAccessFile {

	protected File file;
	protected AESGCMRandomAccessFileCompat aesRandomAccessFile;

	protected boolean deleteFileOnClose = false;
	
	private final static Logger logger = Logger
			.getLogger(GenericVirtualFileImpl.class);

	/**
	 * stores number of open file handles for each opened file
	 */
	final static Hashtable<AESGCMRandomAccessFileCompat, Integer> filehandleCtrMap = new Hashtable<AESGCMRandomAccessFileCompat, Integer>();

	protected GenericVirtualFileImpl(String fileName, VirtualVolume volume) {
		super(fileName, volume);
		file = new File(fileName);
		try {
			if (!isDirectory()) {
				if (exists()) {
					aesRandomAccessFile = file.canWrite() ? AESGCMRandomAccessFileCompat
							.getInstance(file, true)
							: AESGCMRandomAccessFileCompat.getInstance(file,
									false);
					// System.err.println("getInstance(" + file.getName() + ","
					// + aesRandomAccessFile.writable + ") -> "
					// + aesRandomAccessFile.toString());
				} else {
					aesRandomAccessFile = file.getParentFile().canWrite() ? AESGCMRandomAccessFileCompat
							.getInstance(file, true)
							: AESGCMRandomAccessFileCompat.getInstance(file,
									false);
					// System.err.println("getInstance(" + file.getName() + ","
					// + aesRandomAccessFile.writable + ") -> "
					// + aesRandomAccessFile.toString());
				}
			}
		} catch (IOException | FileEncryptionException e) {
			logger.error("Unable to initialize AES backend", e);
		}
	}

	@Override
	public boolean isDirectory() {
		return file.isDirectory();
	}

	@Override
	public boolean delete() {
		if (file.isDirectory()) {
			try {
				FileUtils.deleteDirectory(file);
				return true;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		} else {
			// TODO: check if this causes problems if file wasn't closed
			return file.delete();
		}
	}

	@Override
	public boolean createNewDirectory() {
		return file.mkdirs();
	}

	@Override
	public boolean exists() {
		return file.exists();
	}

	@Override
	public long length() throws IOException {
		// return aesRandomAccessFile.length();
		return AESGCMRandomAccessFileCompat
				.realToVirtualFileSize(file.length());
	}

	@Override
	public VirtualFile[] list() {
		String[] files = file.list();

		GenericVirtualFileImpl[] fileList = new GenericVirtualFileImpl[files.length];

		for (int i = 0; i < files.length; ++i) {
			fileList[i] = new GenericVirtualFileImpl(
					new File(file, files[i]).getPath(), volume);
		}

		return fileList;
	}

	@Override
	public String getPath() {
		return file.getPath();
	}

	@Override
	public String getRelativePath() {
		return volume.getRelativePathForFile(this);
	}

	@Override
	public boolean renameTo(VirtualFile newFile) {
		if (isDirectory()) {
			return file.renameTo(newFile.getFile());
		} else {
			// use backend rename function
			GenericVirtualFileImpl newDFile = (GenericVirtualFileImpl) newFile;
			if (aesRandomAccessFile.renameTo(newDFile.getFile())) {
				this.file = newDFile.getFile();
				return true;
			} else {
				return false;
			}
		}
	}

	@Override
	public File getFile() {
		return file;
	}

	@Override
	public String toString() {
		return file.getName() + " at " + file.getAbsoluteFile();
	}

	@Override
	public long getCreationTime() {
		try {
			BasicFileAttributes attr = Files.readAttributes(file.toPath(),
					BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
			return attr.creationTime().toMillis();
		} catch (IOException e) {
			logger.error("Error in getCreationTime()", e);
			return 0;
		}
	}

	@Override
	public long getLastAccessTime() {
		try {
			BasicFileAttributes attr = Files.readAttributes(file.toPath(),
					BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
			return attr.lastAccessTime().toMillis();
		} catch (IOException e) {
			logger.error("Error in getLastAccessTime()", e);
			return 0;
		}
	}

	@Override
	public long getLastWriteTime() {
		// return file.lastModified();
		try {
			BasicFileAttributes attr = Files.readAttributes(file.toPath(),
					BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
			return attr.lastModifiedTime().toMillis();
		} catch (IOException e) {
			logger.error("Error in getLastWriteTime()", e);
			return 0;
		}
	}

	@Override
	public void setLastAccessTime(long atime) throws IOException {
		if (atime > 0) {
			FileTime fileTime = FileTime.fromMillis(atime);
			Files.setAttribute(file.toPath(), "lastAccessTime", fileTime);
		}
	}

	@Override
	public void setModifiedTime(long mtime) throws IOException {
		if (mtime > 0) {
			FileTime fileTime = FileTime.fromMillis(mtime);
			Files.setLastModifiedTime(file.toPath(), fileTime);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.panbox.desktop.common.vfs.backend.VirtualFile#seek(long)
	 */
	@Override
	public void seek(long pos) throws IOException, PanboxEncryptionException {
		try {
			aesRandomAccessFile.seek(pos);
		} catch (IOException e) {
			throw new IOException(e.getMessage(), e);
		} catch (FileEncryptionException e) {
			throw new PanboxEncryptionException(e.getMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.panbox.desktop.common.vfs.backend.VirtualFile#getFilePointer()
	 */
	@Override
	public long getFilePointer() throws IOException {
		try {
			return aesRandomAccessFile.getFilePointer();
		} catch (IOException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.panbox.desktop.common.vfs.backend.VirtualFile#skipBytes(int)
	 */
	@Override
	public int skipBytes(int n) throws IOException, PanboxEncryptionException {
		try {
			return aesRandomAccessFile.skipBytes(n);
		} catch (IOException e) {
			throw new IOException(e.getMessage(), e);
		} catch (FileEncryptionException e) {
			throw new PanboxEncryptionException(e.getMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.panbox.desktop.common.vfs.backend.VirtualFile#write(byte[])
	 */
	@Override
	public void write(byte[] b) throws IOException, PanboxEncryptionException {
		try {
			aesRandomAccessFile.write(b);
		} catch (IOException e) {
			throw new IOException(e.getMessage(), e);
		} catch (FileEncryptionException e) {
			throw new PanboxEncryptionException(e.getMessage(), e);
		} catch (FileIntegrityException e) {
			throw new PanboxIntegrityException(e.getMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.panbox.desktop.common.vfs.backend.VirtualFile#write(long,
	 * java.nio.ByteBuffer)
	 */
	@Override
	public int write(long seekpos, ByteBuffer b) throws IOException,
			PanboxEncryptionException {
		try {
			aesRandomAccessFile.seek(seekpos);
			byte[] buf;
			if (b.hasArray()) {
				buf = b.array();
				this.write(buf, 0, buf.length);
			} else {
				buf = new byte[b.remaining()];
				b.get(buf);
				this.write(buf, 0, buf.length);
			}
			return buf.length;
		} catch (FileEncryptionException e) {
			throw new PanboxEncryptionException(e.getMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.panbox.desktop.common.vfs.backend.VirtualFile#write(long,
	 * java.nio.ByteBuffer)
	 */
	@Override
	public int write(long seekpos, byte[] b) throws IOException,
			PanboxEncryptionException {
		try {
			aesRandomAccessFile.seek(seekpos);
			this.write(b, 0, b.length);
			return b.length;
		} catch (FileEncryptionException e) {
			throw new PanboxEncryptionException(e.getMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.panbox.desktop.common.vfs.backend.VirtualFile#write(byte[], int,
	 * int)
	 */
	@Override
	public void write(byte[] b, int off, int len) throws IOException,
			PanboxEncryptionException {
		try {
			aesRandomAccessFile.write(b, off, len);
		} catch (IOException e) {
			throw new IOException(e.getMessage(), e);
		} catch (FileEncryptionException e) {
			throw new PanboxEncryptionException(e.getMessage(), e);
		} catch (FileIntegrityException e) {
			throw new PanboxIntegrityException(e.getMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.panbox.desktop.common.vfs.backend.VirtualFile#write(int)
	 */
	@Override
	public void write(int b) throws IOException, PanboxEncryptionException {
		try {
			aesRandomAccessFile.write(b);
		} catch (IOException e) {
			throw new IOException(e.getMessage(), e);
		} catch (FileEncryptionException e) {
			throw new PanboxEncryptionException(e.getMessage(), e);
		} catch (FileIntegrityException e) {
			throw new PanboxIntegrityException(e.getMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.panbox.desktop.common.vfs.backend.VirtualFile#read(byte[], int,
	 * int)
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException,
			PanboxEncryptionException {
		try {
			return aesRandomAccessFile.read(b, off, len);
		} catch (IOException e) {
			throw new IOException(e.getMessage(), e);
		} catch (FileEncryptionException e) {
			throw new PanboxEncryptionException(e.getMessage(), e);
		} catch (FileIntegrityException e) {
			throw new PanboxIntegrityException(e.getMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.panbox.desktop.common.vfs.backend.VirtualFile#read(byte[])
	 */
	@Override
	public int read(byte[] b) throws IOException, PanboxEncryptionException {
		try {
			return aesRandomAccessFile.read(b);
		} catch (IOException e) {
			throw new IOException(e.getMessage(), e);
		} catch (FileEncryptionException e) {
			throw new PanboxEncryptionException(e.getMessage(), e);
		} catch (FileIntegrityException e) {
			throw new PanboxIntegrityException(e.getMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.panbox.desktop.common.vfs.backend.VirtualFile#read(long,
	 * java.nio.ByteBuffer)
	 */
	@Override
	public int read(long seekpos, ByteBuffer b) throws IOException,
			PanboxEncryptionException {
		byte[] buf = new byte[b.remaining()]; // raf.array() does not
		// work, because backend
		// byte[] is missing!
		try {
			aesRandomAccessFile.seek(seekpos);
			int reallyRead = aesRandomAccessFile.read(buf, 0, buf.length);
			if (reallyRead != -1) {
				// Trim NUL-Bytes from file
				b.put(buf, 0, reallyRead);
				return reallyRead;
			} else {
				// EOF is indicated by -1; returning -1 will be misinterpreted
				// as an ERRNO (i.e. EPERM)
				return 0;
			}
		} catch (FileEncryptionException e) {
			throw new PanboxEncryptionException(e.getMessage(), e);
		} catch (FileIntegrityException e) {
			throw new PanboxIntegrityException(e.getMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.panbox.desktop.common.vfs.backend.VirtualFile#read(long,
	 * java.nio.ByteBuffer)
	 */
	@Override
	public int read(long seekpos, byte[] b) throws IOException,
			PanboxEncryptionException {
		try {
			aesRandomAccessFile.seek(seekpos);
			int reallyRead = aesRandomAccessFile.read(b);
			if (reallyRead == -1) {
				// EOF is indicated by -1; returning -1 will be misinterpreted
				// as an ERRNO (i.e. EPERM)
				return 0;
			} else {
				return reallyRead;
			}
		} catch (FileEncryptionException e) {
			throw new PanboxEncryptionException(e.getMessage(), e);
		} catch (FileIntegrityException e) {
			throw new PanboxIntegrityException(e.getMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.panbox.desktop.common.vfs.backend.VirtualFile#read()
	 */
	@Override
	public int read() throws IOException, PanboxEncryptionException {
		try {
			return aesRandomAccessFile.read();
		} catch (IOException e) {
			throw new IOException(e.getMessage(), e);
		} catch (FileEncryptionException e) {
			throw new PanboxEncryptionException(e.getMessage(), e);
		} catch (FileIntegrityException e) {
			throw new PanboxIntegrityException(e.getMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.panbox.desktop.common.vfs.backend.VirtualFile#open(java.io.File,
	 * boolean)
	 */
	@Override
	public void open() throws IOException, PanboxEncryptionException {
		try {
			// windows verifies file existance by calling create(OPEN_EXISTING)
			// - thus check if file exists at an earlypoint in time
			if (!file.exists()) {
				throw new IOException("File " + file + "does not exist!");
			} else {

				// TODO: support read-only mode, use fileinfo-objects
				// check file handle counter maps and create/increase values
				synchronized (filehandleCtrMap) {
					Integer fhctr = filehandleCtrMap.get(aesRandomAccessFile);
					// System.err.println("open(" + file.getName() + ","
					// + aesRandomAccessFile.writable + ", ctr=" + fhctr
					// + ") -> " + aesRandomAccessFile.toString());
					if (fhctr != null) {
						// file has already been opened - only increase counter
						filehandleCtrMap.put(aesRandomAccessFile, ++fhctr);
					} else {
						aesRandomAccessFile.open();
						filehandleCtrMap.put(aesRandomAccessFile, 1);
					}
				}
			}
		} catch (FileEncryptionException e) {
			throw new PanboxEncryptionException(e.getMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.panbox.desktop.common.vfs.backend.VirtualFile#create(int,
	 * javax.crypto.SecretKey, java.io.File)
	 */
	@Override
	public void create(int shareKeyVersion, SecretKey shareKey)
			throws IOException, PanboxEncryptionException {
		try {
			// don't check if file has been opened - multiple create() calls on
			// the same file should cause an exception
			// TODO: check error handling + mapping to error codes
			aesRandomAccessFile.create(shareKeyVersion, shareKey);
			// System.err.println("create(" + file.getName() + ","
			// + aesRandomAccessFile.writable + ") -> "
			// + aesRandomAccessFile.toString());

			// check file handle counter maps and create/increase values
			synchronized (filehandleCtrMap) {
				Integer fhctr = filehandleCtrMap.get(aesRandomAccessFile);
				if (fhctr == null) {
					filehandleCtrMap.put(aesRandomAccessFile, 1);
				}
			}

		} catch (IOException e) {
			throw new IOException(e.getMessage(), e);
		} catch (FileEncryptionException e) {
			throw new PanboxEncryptionException(e.getMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.panbox.desktop.common.vfs.backend.VirtualFile#flush()
	 */
	@Override
	public void flush() throws IOException {
		aesRandomAccessFile.flush();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.panbox.desktop.common.vfs.backend.VirtualFile#close()
	 */
	@Override
	public void close() throws IOException {
		if (!file.exists()) {
			throw new IOException("File " + file + "does not exist!");
		} else {
			synchronized (filehandleCtrMap) {
				Integer fhctr = filehandleCtrMap.get(aesRandomAccessFile);
				if ((fhctr == null) || (fhctr < 1)) {
					throw new IOException(
							"Invalid value of file handle counter!");
				} else {
					if (fhctr == 1) {
						// last reference, close file and remove entry
						aesRandomAccessFile.close();
						// System.err.println("close(" + file.getName() + ","
						// + aesRandomAccessFile.writable + ") -> "
						// + aesRandomAccessFile.toString());
						filehandleCtrMap.remove(aesRandomAccessFile);
						
						if(deleteFileOnClose) {
							delete();
						}
					} else if (fhctr > 1) {
						// decrease counter value, but don't close file
						filehandleCtrMap.put(aesRandomAccessFile, --fhctr);
					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.panbox.desktop.common.vfs.backend.VirtualFile#setDeleteOnClose()
	 */
	@Override
	public void setDeleteOnClose() {
		deleteFileOnClose = true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.panbox.desktop.common.vfs.backend.VirtualFile#getShareKeyVersion()
	 */
	@Override
	public int getShareKeyVersion() throws PanboxEncryptionException,
			IOException {
		try {
			return aesRandomAccessFile.getShareKeyVersion();
		} catch (FileEncryptionException e) {
			throw new PanboxEncryptionException(e.getMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.panbox.desktop.common.vfs.backend.VirtualFile#initWithShareKey(javax
	 * .crypto .SecretKey)
	 */
	@Override
	public void initWithShareKey(SecretKey shareKey)
			throws PanboxEncryptionException, IOException {
		try {
			aesRandomAccessFile.initWithShareKey(shareKey);
		} catch (FileEncryptionException e) {
			throw new PanboxEncryptionException(e.getMessage(), e);
		} catch (FileIntegrityException e) {
			throw new PanboxIntegrityException(e.getMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.panbox.desktop.common.vfs.backend.VirtualFile#setLength(long)
	 */
	@Override
	public void setLength(long newLength) throws IOException,
			PanboxEncryptionException {
		try {
			aesRandomAccessFile.setLength(newLength);
		} catch (FileEncryptionException e) {
			throw new PanboxEncryptionException(e.getMessage(), e);
		} catch (FileIntegrityException e) {
			throw new PanboxIntegrityException(e.getMessage(), e);
		}
	}

	@Override
	public boolean isOpened() {
		return aesRandomAccessFile.isOpen();
	}

	@Override
	public boolean isSymbolic() {
		return Files.isSymbolicLink(getFile().toPath());
	}

	@Override
	public void setAttr(long attr) throws IOException {
		super.setAttr(attr);
	}

	@Override
	public boolean canExecute() {
		return file.canExecute();
	}

	@Override
	public boolean canRead() {
		return file.canRead();
	}

	@Override
	public boolean canWrite() {
		return file.canWrite();
	}
}
