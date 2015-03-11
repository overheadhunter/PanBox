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
package org.panbox.desktop.common.vfs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import javax.crypto.SecretKey;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.panbox.core.crypto.Obfuscator;
import org.panbox.core.exception.ObfuscationException;
import org.panbox.core.keymgmt.ShareKey;
import org.panbox.core.vfs.backend.VirtualFile;
import org.panbox.desktop.common.ex.PanboxCreateFailedException;
import org.panbox.desktop.common.ex.PanboxDeleteFailedException;
import org.panbox.desktop.common.ex.PanboxEncryptionException;
import org.panbox.desktop.common.ex.PanboxHandleException;
import org.panbox.desktop.common.ex.PanboxRenameFailedException;
import org.panbox.desktop.common.vfs.backend.VirtualRandomAccessFile;
import org.panbox.desktop.common.vfs.backend.exceptions.SecretKeyNotFoundException;

/**
 * @author palige
 * 
 *         Linux-specific implementation of Panbox VFS interface.
 */
public class PanboxFSLinux extends PanboxFS {

	private static final Logger logger = Logger
			.getLogger("org.panbox.desktop.common");

	private final static HashMap<Long, VirtualFileAccessSession> fileInstanceTable = new HashMap<Long, VirtualFileAccessSession>();

	public PanboxFSLinux(PanboxFSAdapter backend) {
		super(backend);
	}

	private class VirtualFileAccessSession {
		final VirtualRandomAccessFile file;
		final boolean readonly;

		/**
		 * @param file
		 * @param readonly
		 */
		public VirtualFileAccessSession(VirtualRandomAccessFile file,
				boolean readonly) {
			this.file = file;
			this.readonly = readonly;
		}
	}

	public synchronized void create(final String path, final long handle,
			final boolean readonly) throws ObfuscationException, IOException,
			PanboxEncryptionException {
		logger.debug("create : " + path + ", handle: " + handle);

		ShareKey shareKey = backingStorage.getLatestShareKeyForFile(path);
		VirtualRandomAccessFile virt = (VirtualRandomAccessFile) getVirtualFileForFileName(
				path, true);
		virt.create(shareKey.version, shareKey.key);
		fileInstanceTable.put(handle, new VirtualFileAccessSession(virt,
				readonly));
		logger.debug("create : VirtualFile(" + path
				+ ").create() was successful.");

	}

	public synchronized void flush(final String path, final long handle)
			throws PanboxHandleException, IOException {
		logger.debug("flush : " + path + ", handle: " + handle);
		VirtualFileAccessSession session = fileInstanceTable.get(handle);
		if (session != null) {
			// note: flush() will also be called upon readonly files
			session.file.flush();
			logger.debug("flush : VirtualFile(" + path
					+ ").flush() was successful.");
		} else {
			throw new PanboxHandleException(
					"No available instance for given handle nr. " + handle);
		}
	}

	public synchronized AbstractFileInfo getattr(final String path,
			boolean alreadyObfuscated, boolean outputObfuscated)
			throws IOException {
		logger.debug("getattr : " + path);
		return getFileInfo(path, alreadyObfuscated, outputObfuscated);
	}

	public synchronized void mkdir(final String path)
			throws PanboxCreateFailedException, SecretKeyNotFoundException,
			FileNotFoundException, ObfuscationException {
		logger.debug("mkdir : " + path);

		if (!getVirtualFileForFileName(path, true).createNewDirectory()) {
			throw new PanboxCreateFailedException(
					"Failed to create directory '" + path + "'.");
		} else {
			logger.debug("mkdir : VirtualFile(" + path
					+ ").createNewDirectory() was successful.");
		}

	}

	public synchronized void open(final String path, final long handle,
			boolean readonly) throws ObfuscationException,
			PanboxEncryptionException, IOException {
		logger.debug("open : " + path + ", handle: " + handle + ", readonly: "
				+ readonly);

		VirtualRandomAccessFile virt = (VirtualRandomAccessFile) getVirtualFileForFileName(path);
		virt.open();
		virt.initWithShareKey(backingStorage.getShareKeyForFile(path,
				virt.getShareKeyVersion()));
		fileInstanceTable.put(handle, new VirtualFileAccessSession(virt,
				readonly));
	}

	public synchronized int read(final String path, final ByteBuffer buffer,
			final long offset, final long size, final long handle)
			throws PanboxHandleException, PanboxEncryptionException,
			IOException {
		VirtualFileAccessSession session = fileInstanceTable.get(handle);
		if (session != null) {
			logger.debug("read, File : " + path + ", Obf. path: "
					+ session.file + ", buffersize: " + buffer.remaining()
					+ ", offset: " + offset);
			return session.file.read(offset, buffer);
		} else {
			throw new PanboxHandleException(
					"No available instance for given handle nr. " + handle);
		}
	}

	public synchronized int read(final String path, final byte[] buffer,
			final long offset, final long size, final long handle)
			throws PanboxHandleException, PanboxEncryptionException,
			IOException {
		VirtualFileAccessSession session = fileInstanceTable.get(handle);
		if (session != null) {
			logger.debug("read, File : " + path + ", Obf. path: "
					+ session.file + ", buffersize: " + buffer.length
					+ ", offset: " + offset);
			return session.file.read(offset, buffer);
		} else {
			throw new PanboxHandleException(
					"No available instance for given handle nr. " + handle);
		}
	}

	public synchronized Collection<String> readdir(final String path)
			throws FileNotFoundException {
		logger.debug("readdir : " + path);
		VirtualFile[] files = null;
		try {
			if (path.equals(File.separator)) {
				files = backingStorage.getFile(path).list();
			} else {
				files = getVirtualFileForFileName(path).list();
			}
		} catch (IOException | ObfuscationException e) {
			files = new VirtualFile[0];
		}

		final ArrayList<String> list = new ArrayList<String>();
		for (final VirtualFile s : files) {
			// This code is for making files and folders staring with '.'
			// invisible!
			// This is mainly used for the .panbox Folder
			String nameOfFile = s.getFileName().substring(
					s.getFileName().lastIndexOf(File.separator) + 1);
			logger.trace("listDirectory : nameOfFile: " + nameOfFile);
			if (nameOfFile.startsWith(".")) {
				// ignore file, it started with .
				logger.trace("listDirectory : Ignoring file starting with '.': "
						+ nameOfFile + " (" + path + ")");
				continue;
			}

			list.add(backingStorage.getRelativePathForFile(s));
		}
		return list;
	}

	public synchronized void release(final String path, final long handle)
			throws PanboxHandleException, IOException {
		logger.debug("release : " + path + ", handle: " + handle);
		VirtualFileAccessSession session = fileInstanceTable.get(handle);
		if (session != null) {
			session.file.close();
			fileInstanceTable.remove(handle);
		} else {
			throw new PanboxHandleException(
					"No available instance for given handle nr. " + handle);
		}
	}

	public synchronized void setLastAccessTime(final String path,
			final long ac_nsec) throws IOException {
		logger.debug("setLastAccessTime : " + path);
		try {
			VirtualFile backed = getVirtualFileForFileName(path);
			backed.setLastAccessTime(ac_nsec);
		} catch (ObfuscationException e) {
			throw new IOException("Obfuscation failed!");
		}
	}

	public synchronized void setModifiedTime(String path, long mod_nsec)
			throws IOException, ObfuscationException {
		logger.debug("setModifiedTime : " + path);

		VirtualFile backed = getVirtualFileForFileName(path);
		backed.setModifiedTime(mod_nsec);
	}

	public synchronized void rename(final String oldpath, final String newpath)
			throws PanboxRenameFailedException, ObfuscationException,
			IOException {
		logger.debug("rename : " + oldpath + " -> " + newpath);

		VirtualFile newFile = getVirtualFileForFileName(newpath, true);
		VirtualFile oldFile = getVirtualFileForFileName(oldpath);

		// VirtualFileAccessSession sessionold, sessionnew;
		// sessionold = getSessionforVirtualFile(oldFile);
		// sessionnew = getSessionforVirtualFile(newFile);
		//
		// if ((sessionnew != null) || (sessionold) != null) {
		// throw new IOException(
		// "rename of files in active session not supported!");
		// }

		// oldFile = (sessionold != null) ? sessionold.file : oldFile;
		// newFile = (sessionnew != null) ? sessionnew.file : newFile;
		if (newFile.exists() && !newFile.canWrite()) {
			throw new IOException("Renaming of readonly files is not allowed!");
		} else {
			if (!oldFile.renameTo(newFile)) {
				throw new PanboxRenameFailedException("Operation " + oldpath
						+ ").renameTo(" + newpath + ") was not successful");
			} else {
				logger.debug("rename : VirtualFile(" + oldpath + ").renameTo("
						+ newpath + ") was successful.");
			}
		}
	}

	/**
	 * Convenience method which returns {@link VirtualFile}-instance for given
	 * filename without creating IV sidear file during obfuscation.
	 * 
	 * 
	 * @param filename
	 * @return
	 * @throws SecretKeyNotFoundException
	 * @throws FileNotFoundException
	 * @throws ObfuscationException
	 */
	protected synchronized VirtualFile getVirtualFileForFileName(String filename)
			throws SecretKeyNotFoundException, FileNotFoundException,
			ObfuscationException {
		return getVirtualFileForFileName(filename, false);
	}

	@Override
	protected synchronized VirtualFile getVirtualFileForFileName(
			String fileName, boolean createIV)
			throws SecretKeyNotFoundException, FileNotFoundException,
			ObfuscationException {
		// check if file currently is already referenced within the context of a
		// file access session and, if so, use this instance instead.
		// Reasoning: If operations like rename or truncate are conducted upon
		// a file whose contents are currently also being accessed, having
		// multiple instances may cause inconsistencies
		VirtualFile file = super.getVirtualFileForFileName(fileName, createIV);
		VirtualFileAccessSession session = getSessionforVirtualFile(file);
		return (session != null) ? session.file : file;
	}

	public synchronized void rmdir(final String path) throws IOException,
			ObfuscationException {
		logger.debug("rmdir : " + path);

		if (!getVirtualFileForFileName(path).delete()) {
			// TODO: Clean this up .. Current backend deletion handler
			// throws IOexcetion anyway
			throw new IOException("Deletion failed!");
		} else {
			logger.debug("deleteDirectory : VirtualFile(" + path
					+ ").deleteDirectory() was successful.");
		}

	}

	private synchronized VirtualFileAccessSession getSessionforVirtualFile(
			VirtualFile file) {
		if (file instanceof VirtualRandomAccessFile) {
			VirtualFileAccessSession session = null;
			Collection<VirtualFileAccessSession> col = fileInstanceTable
					.values();
			for (Iterator<VirtualFileAccessSession> iterator = col.iterator(); iterator
					.hasNext();) {
				VirtualFileAccessSession virtualFileAccessSession = (VirtualFileAccessSession) iterator
						.next();
				if (virtualFileAccessSession.file.equals(file)) {
					session = virtualFileAccessSession;
					break;
				}
			}
			return session;
		} else
			return null;
	}

	public synchronized void truncate(final String path, final long length)
			throws IOException, SecretKeyNotFoundException,
			FileNotFoundException, ObfuscationException,
			PanboxEncryptionException {
		logger.debug("truncate : " + path + ", length: " + length);

		VirtualFile vFile = getVirtualFileForFileName(path);
		VirtualFileAccessSession session = getSessionforVirtualFile(vFile);

		if (session == null) {
			VirtualRandomAccessFile virt = (VirtualRandomAccessFile) getVirtualFileForFileName(path);
			virt.open();
			virt.initWithShareKey(backingStorage.getShareKeyForFile(path,
					virt.getShareKeyVersion()));
			virt.setLength(length);
			virt.flush();
			virt.close();
		} else {
			// already opened
			if (session.readonly) {
				throw new IOException("Illegal operation: File " + session.file
						+ " was opened readonly!");
			} else {
				session.file.setLength(length);
			}
		}
	}

	public enum AccessMode {
		EXISTS, READ, WRITE, EXECUTE
	};

	public synchronized boolean access(final String path, final AccessMode mode)
			throws SecretKeyNotFoundException, FileNotFoundException,
			ObfuscationException {
		logger.debug("access : " + path);
		VirtualFile virt = getVirtualFileForFileName(path);
		if (virt instanceof VirtualRandomAccessFile) {
			switch (mode) {
			case READ:
				return virt.canRead();

			case WRITE:
				return virt.canWrite();

			case EXECUTE:
				return virt.canExecute();

			case EXISTS:
				return virt.exists();

			default:
				return true;
			}
		} else {
			return true;
		}
	}

	public synchronized void unlink(final String path)
			throws PanboxDeleteFailedException, SecretKeyNotFoundException,
			FileNotFoundException, ObfuscationException {
		logger.debug("unlink : " + path);

		if (!getVirtualFileForFileName(path).delete()) {
			// TODO: error handling
			throw new PanboxDeleteFailedException("Deletion failed!");
		} else {
			logger.debug("deleteFile : VirtualFile(" + path
					+ ").deleteFile() was successful.");
		}
	}

	public synchronized int write(final String path, final ByteBuffer buffer,
			final long offset, final long size, final long handle)
			throws PanboxHandleException, IOException,
			PanboxEncryptionException {
		VirtualFileAccessSession session = fileInstanceTable.get(handle);
		if (session != null) {
			if (session.readonly) {
				throw new IOException("Illegal operation: File " + session.file
						+ " was opened readonly!");
			} else {
				logger.debug("write, File : " + path + ", Obf. path: "
						+ session.file + ", buffersize: " + buffer.remaining()
						+ ", offset: " + offset);
				return session.file.write(offset, buffer);
			}
		} else {
			throw new PanboxHandleException(
					"No available instance for given handle nr. " + handle);
		}
	}

	public synchronized int write(final String path, final byte[] buffer,
			final long offset, final long size, final long handle)
			throws PanboxHandleException, IOException,
			PanboxEncryptionException {
		VirtualFileAccessSession session = fileInstanceTable.get(handle);
		if (session != null) {
			if (session.readonly) {
				throw new IOException("Illegal operation: File " + session.file
						+ " was opened readonly!");
			} else {
				logger.debug("write, File : " + path + ", Obf. path: "
						+ session.file + ", buffersize: " + buffer.length
						+ ", offset: " + offset);
				return session.file.write(offset, buffer);
			}
		} else {
			throw new PanboxHandleException(
					"No available instance for given handle nr. " + handle);
		}
	}

	public synchronized void chmod(final String path, final long attr)
			throws IOException, ObfuscationException {
		logger.debug("chmod: path=" + path + ", attr=" + attr);
		VirtualFile virt = getVirtualFileForFileName(path);
		// NOTE: Calling chmod() on files which are currently in the process of
		// being encrypted & written should not affect the corresponding file
		// access session, but only come into effect after the fh has been close
		// in the backend
		virt.setAttr(attr);
	}

	public synchronized void symlink(final String target, final String link)
			throws IOException {

		try {
			// TODO: Here, we parse 3 times for the Share that manages the File
			VirtualFile vlink = getVirtualFileForFileName(link, true);
			String fullpath = FilenameUtils.getFullPath(link);
			String sharePath = FilenameUtils.normalize(fullpath.substring(0,
					fullpath.indexOf('/', 1) + 1));

			String shareloc = FilenameUtils.concat(fullpath, target);

			if (FilenameUtils.equals(sharePath, shareloc)
					|| FilenameUtils.directoryContains(sharePath, shareloc)) {

				SecretKey sk = backingStorage
						.getObfuscationKeyForFile(shareloc);
				Obfuscator obfuscator = backingStorage.getObfuscator(shareloc);

				// create obfuscated symlink target
				String[] targetparts = target.split("/");
				StringBuffer res = new StringBuffer();

				if (target.startsWith(File.separator)) {
					res.append(File.separator);
				}

				for (int i = 0; i < targetparts.length; i++) {
					String cur = targetparts[i];
					if (cur.equals(".") || cur.equals("..")) {
						res.append(cur);
					} else {
						// append obfuscated part of path
						res.append(obfuscator.obfuscate(cur, sk, true));
					}
					// append intermediary separators
					if (i < targetparts.length - 1) {
						res.append(File.separator);
					}
				}

				if (target.endsWith(File.separator)) {
					res.append(File.separator);
				}

				String obfuscatedTarget = res.toString();
				Path ptarget = Paths.get(obfuscatedTarget);

				Path plink = Paths.get(vlink.getFileName());
				Files.createSymbolicLink(plink, ptarget);
				logger.debug("symlink, Target : " + obfuscatedTarget
						+ ", Link: " + vlink.getFileName());
			} else {
				throw new IOException(
						"Symlinks outside of shares are not supported.");
			}
		} catch (ObfuscationException e) {
			// logger.error("Could not obfuscate symlink target!", e);
			throw new IOException("Could not obfuscate symlink target!", e);
		}

	}

	public synchronized void readlink(final String path,
			final ByteBuffer buffer, final long size) throws IOException {

		try {
			// TODO: Here, we parse 3 times for the Share that manages the File
			String fullpath = FilenameUtils.getFullPath(path);
			String sharePath = FilenameUtils.normalize(fullpath.substring(0,
					fullpath.indexOf('/', 1) + 1));

			VirtualFile vpath = getVirtualFileForFileName(path);
			String target = Files.readSymbolicLink(vpath.getFile().toPath())
					.toString();

			String shareloc = FilenameUtils.concat(fullpath, target.toString());

			if (FilenameUtils.directoryContains(sharePath, shareloc)
					|| FilenameUtils.equals(sharePath, shareloc)) {

				SecretKey sk = backingStorage
						.getObfuscationKeyForFile(shareloc);
				Obfuscator obfuscator = backingStorage.getObfuscator(shareloc);

				// create deobfuscated symlink target
				String[] targetparts = target.split("/");
				StringBuffer res = new StringBuffer();

				if (target.startsWith(File.separator)) {
					res.append(File.separator);
				}

				for (int i = 0; i < targetparts.length; i++) {
					String cur = targetparts[i];
					if (cur.equals(".") || cur.equals("..")) {
						res.append(cur);
					} else {
						// append obfuscated part of path
						res.append(obfuscator.deObfuscate(cur, sk));
					}
					// append intermediary separators
					if (i < targetparts.length - 1) {
						res.append(File.separator);
					}
				}

				if (target.endsWith(File.separator)) {
					res.append(File.separator);
				}

				byte[] ret = res.toString().getBytes();
				int realsize = Math.min(ret.length, (int) size);
				buffer.put(ret, 0, realsize);
				logger.debug("readline, Link : " + path + ", Target: "
						+ res.toString());
			} else {
				throw new IOException(
						"Symlinks outside of shares are not supported.");
			}
		} catch (ObfuscationException e) {
			throw new IOException("Error deobfuscating symlink contents!", e);
		}
	}

	/**
	 * indicates if there currently exist any ongoing file access sessions
	 * 
	 * @return <code>true</code>, if there exist one or more file access
	 *         sessions
	 */
	public boolean openFileAccessSessions() {
		return (fileInstanceTable.size() > 0);
	}

	public void beforeUnmount(File mountPoint) {
		logger.warn("Panbox is about to shutdown - flush and close all open AES* instances...");
		Collection<VirtualFileAccessSession> coll = fileInstanceTable.values();
		for (Iterator<VirtualFileAccessSession> it = coll.iterator(); it
				.hasNext();) {
			VirtualFileAccessSession session = (VirtualFileAccessSession) it
					.next();
			try {
				session.file.flush();
				session.file.close();
				logger.info("Successfully closed VirtualFile instance for file "
						+ session.file.getFileName());
			} catch (Exception e) {
				logger.error("Error on closing VirtualFile instance for file "
						+ session.file.getFileName());
			}
		}
	}

}
