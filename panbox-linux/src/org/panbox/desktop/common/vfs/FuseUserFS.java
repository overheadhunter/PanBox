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
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicLong;

import net.fusejna.DirectoryFiller;
import net.fusejna.ErrorCodes;
import net.fusejna.FlockCommand;
import net.fusejna.FuseException;
import net.fusejna.StructFlock.FlockWrapper;
import net.fusejna.StructFuseFileInfo.FileInfoWrapper;
import net.fusejna.StructFuseFileInfo.FileInfoWrapper.OpenMode;
import net.fusejna.StructStat.StatWrapper;
import net.fusejna.StructStatvfs.StatvfsWrapper;
import net.fusejna.StructTimeBuffer.TimeBufferWrapper;
import net.fusejna.types.TypeMode.ModeWrapper;
import net.fusejna.types.TypeMode.NodeType;
import net.fusejna.util.FuseFilesystemAdapterFull;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.panbox.Settings;
import org.panbox.core.exception.ObfuscationException;
import org.panbox.desktop.common.ex.PanboxCreateFailedException;
import org.panbox.desktop.common.ex.PanboxDeleteFailedException;
import org.panbox.desktop.common.ex.PanboxEncryptionException;
import org.panbox.desktop.common.ex.PanboxHandleException;
import org.panbox.desktop.common.ex.PanboxRenameFailedException;
import org.panbox.desktop.common.vfs.PanboxFSLinux.AccessMode;
import org.panbox.desktop.common.vfs.backend.exceptions.SecretKeyNotFoundException;
import org.panbox.desktop.linux.Utils;

public final class FuseUserFS extends FuseFilesystemAdapterFull implements
		PanboxFSAdapter {

	private static final Logger logger = Logger
			.getLogger("org.panbox.desktop.common");

	private static final ResourceBundle bundle = ResourceBundle.getBundle(
			"org.panbox.desktop.common.gui.Messages", Settings.getInstance()
					.getLocale());

	private static final long fakeBlockSize = 4096L;
	private PanboxFSLinux panboxFS = null;

	public FuseUserFS() {
	}

	private String[] vfsoptions = null;

	public FuseUserFS(String[] vfsoptions) {
		this.vfsoptions = vfsoptions;
	}

	@Override
	protected String[] getOptions() {
		// per default, set some extra fuse mount options to allow to deviate
		// from the default write-buffersize of 4096 bytes for increasing
		// performance
		String[] ret = null;
		if ((vfsoptions == null) || (vfsoptions.length == 0)) {
			ret = new String[] { "-o", "big_writes", "-o", "max_write=4194304" };
			logger.info("Using default FUSE options: " + Arrays.toString(ret));
		} else {
			ret = vfsoptions;
			logger.info("FUSE options: " + Arrays.toString(ret));
		}
		return ret;
	}

	/**
	 * {@link AtomicLong} instance for file handle generation.
	 */
	final AtomicLong handleCtr = new AtomicLong(5);

	// private long setFH(FileInfoWrapper info) {
	// info.fh(handleCtr.incrementAndGet());
	// return info.fh();
	// }

	private final synchronized void log_error(Exception e) {
		logger.error(getMethodName(1) + ": Caught exception "
				+ e.getClass().getSimpleName() + " from PanboxFS.", e);
	}

	public static final int R_OK = 4; /* test for read permission */
	public static final int W_OK = 2; /* test for write permission */
	public static final int X_OK = 1; /* test for execute (search) permission */
	public static final int F_OK = 0; /* test for presence of file */

	@Override
	public int access(String path, int access) {

		try {
			if (!panboxFS.access(path, AccessMode.EXISTS)) {
				return -ErrorCodes.ENOENT();
			} else {
				if ((access & R_OK) == R_OK) {
					if (!panboxFS.access(path, AccessMode.READ)) {
						return -ErrorCodes.EACCES();
					}
				}

				if ((access & W_OK) == W_OK) {
					if (!panboxFS.access(path, AccessMode.WRITE)) {
						return -ErrorCodes.EACCES();
					}
				}

				if ((access & X_OK) == X_OK) {
					if (!panboxFS.access(path, AccessMode.EXECUTE)) {
						return -ErrorCodes.EACCES();
					}
				}
			}
		} catch (FileNotFoundException | ObfuscationException e) {
			return -ErrorCodes.ENOENT();
		}

		return 0;
	}

	/**
	 * Get the method name for a depth in call stack. <br />
	 * Utility function. See <a href=
	 * "http://stackoverflow.com/questions/442747/getting-the-name-of-the-current-executing-method"
	 * >http://stackoverflow.com/questions/442747/getting-the-name-of-the-
	 * current-executing-method</a> for details
	 * 
	 * @param depth
	 *            depth in the call stack (0 means current method, 1 means call
	 *            method, ...)
	 * @return method name
	 */
	public static String getMethodName(final int depth) {
		final StackTraceElement[] ste = Thread.currentThread().getStackTrace();

		return ste[ste.length - 1 - depth].getMethodName();
	}

	@Override
	public int create(final String path, final ModeWrapper mode,
			final FileInfoWrapper info) {
		try {
			if (FilenameUtils.indexOfLastSeparator(path) == 0) {
				// we're in the VFS root
				VFSErrorMessages.showErrorMessage(
						bundle.getString("FuseUserFS.creatingInRootDir"),
						bundle.getString("FuseUserFS.illegalOperation"));
				return -ErrorCodes.ECANCELED();
			} else {
				// preparations
				boolean readonly = (info.openMode() == OpenMode.READONLY);
				long handle = handleCtr.incrementAndGet();
				panboxFS.create(path, handle, readonly);
				// if no exception was thrown, set handle for subsequent session
				info.fh(handle);
			}
		} catch (ObfuscationException e) {
			log_error(e);
			VFSErrorMessages.showErrorMessage(MessageFormat.format(
					bundle.getString("FuseUserFS.fileCouldNotBeObfuscated"),
					path), bundle.getString("error"));
			return -ErrorCodes.EIO();
		} catch (IOException e) {
			log_error(e);
			return -ErrorCodes.EIO();
		} catch (PanboxEncryptionException e) {
			log_error(e);
			VFSErrorMessages.showErrorMessage(MessageFormat.format(
					bundle.getString("FuseUserFS.fileCouldNotBeEncrypted"),
					path), bundle.getString("error"));
			return -ErrorCodes.EIO();
		}
		return 0;
	}

	@Override
	public void destroy() {
		panboxFS._onUnmount();
	}

	@Override
	public int flush(final String path, final FileInfoWrapper info) {
		try {
			panboxFS.flush(path, info.fh());
		} catch (IOException e) {
			log_error(e);
			return -ErrorCodes.EIO();
		} catch (PanboxHandleException e) {
			logger.error(
					"FuseUserFS::flush : Caught exception PanboxHandleException from PanboxFS: ",
					e);
			return -ErrorCodes.EIO();
		}
		return 0;
	}

	final static int uid = Utils.getUid();
	final static int gid = Utils.getGid();

	@Override
	public int getattr(final String path, final StatWrapper stat) {
		try {
			if (!UnconsequentialFiles.isUnconsequential(path)) {

				FileInfo info = (FileInfo) panboxFS.getattr(path, false, false);
				if (info.isSymbolic()) {
					stat.setMode(NodeType.SYMBOLIC_LINK, info.getAttr()).size(
							info.getSize());
				} else {
					stat.setMode(
							info.isDirectory() ? NodeType.DIRECTORY
									: NodeType.FILE, info.getAttr()).size(
							info.getSize());
				}

				// defaults to current uid/gid. on rare occasions, FuseJNA
				// uid/gid resultion seems to fail
				stat.uid(uid).gid(gid);

				stat.setTimes(info.getLastAccessTime(), 0,
						info.getLastWriteTime(), 0, info.getCreationTime(), 0);

			} else {
				return -ErrorCodes.ENOENT();

				// TODO: for dolphin bug with .directory.lock use this
				// return -ErrorCodes.EACCES();

			}
		} catch (SecretKeyNotFoundException e) {
			log_error(e);
			return -ErrorCodes.ENOENT();
		} catch (FileNotFoundException e) {
			// method is being used for file existance checking - omit logging
			// here
			// log_error(e);
			return -ErrorCodes.ENOENT();
		} catch (IOException e) {
			log_error(e);
			return -ErrorCodes.EIO();
		}
		return 0;
	}

	@Override
	protected String getName() {
		return panboxFS.getFilesystemName() + "-" + panboxFS.getVolumeName();
	}

	@Override
	public int mkdir(final String path, final ModeWrapper mode) {
		if (!UnconsequentialFiles.isUnconsequential(path)) {
			try {
				if (FilenameUtils.indexOfLastSeparator(path) == 0) {
					// we're in the VFS root
					VFSErrorMessages.showErrorMessage(
							bundle.getString("FuseUserFS.creatingInRootDir"),
							bundle.getString("FuseUserFS.illegalOperation"));
					return -ErrorCodes.EACCES();
				} else {
					panboxFS.mkdir(path);
				}
			} catch (SecretKeyNotFoundException e) {
				log_error(e);
				return -ErrorCodes.ENOENT();
			} catch (FileNotFoundException e) {
				log_error(e);
				return -ErrorCodes.ENOENT();
			} catch (PanboxCreateFailedException e) {
				log_error(e);
				return -ErrorCodes.EACCES();
			} catch (ObfuscationException e) {
				log_error(e);
				VFSErrorMessages
						.showErrorMessage(
								MessageFormat.format(
										bundle.getString("FuseUserFS.fileCouldNotBeObfuscated"),
										path), bundle.getString("error"));
				return -ErrorCodes.EIO();
			}
		}
		return 0;
	}

	@Override
	public int open(final String path, final FileInfoWrapper info) {
		try {
			boolean readonly = (info.openMode() == OpenMode.READONLY);
			// shareModeDeletable always false for linux as files are deletable
			// after being opened by default
			long handle = handleCtr.incrementAndGet();
			panboxFS.open(path, handle, readonly);
			info.fh(handle);

		} catch (ObfuscationException e) {
			log_error(e);
			VFSErrorMessages.showErrorMessage(MessageFormat.format(
					bundle.getString("FuseUserFS.fileCouldNotBeObfuscated"),
					path), bundle.getString("error"));
			return -ErrorCodes.EIO();
		} catch (PanboxEncryptionException e) {
			log_error(e);
			VFSErrorMessages.showErrorMessage(
					MessageFormat.format(bundle
							.getString("FuseUserFS.decryptionOfFileFailed"),
							path), bundle.getString("error"));
			return -ErrorCodes.EIO();
		} catch (IOException e) {
			log_error(e);
			return -ErrorCodes.EIO();
		}
		return 0;
	}

	@Override
	public int read(final String path, final ByteBuffer buffer,
			final long size, final long offset, final FileInfoWrapper info) {
		try {
			return panboxFS.read(path, buffer, offset, size, info.fh());
		} catch (SecretKeyNotFoundException e) {
			log_error(e);
			return -ErrorCodes.ENOENT();
		} catch (FileNotFoundException e) {
			log_error(e);
			return -ErrorCodes.ENOENT();
		} catch (PanboxEncryptionException e) {
			log_error(e);
			VFSErrorMessages.showErrorMessage(
					MessageFormat.format(bundle
							.getString("FuseUserFS.decryptionOfFileFailed"),
							path), bundle.getString("error"));
			return -ErrorCodes.EIO();
		} catch (IOException e) {
			log_error(e);
			return -ErrorCodes.EIO();
		} catch (PanboxHandleException e) {
			log_error(e);
			return -ErrorCodes.EIO();
		}
	}

	@Override
	public int readdir(final String path, final DirectoryFiller filler) {
		try {
			if (!UnconsequentialFiles.isUnconsequential(path)) {
				Collection<String> files = panboxFS.readdir(path);
				for (Iterator<String> iterator = files.iterator(); iterator
						.hasNext();) {
					String curfile = (String) iterator.next();
					try {
						AbstractFileInfo info = panboxFS.getattr(curfile, true,
								false);
						if (!filler.add(info.fileName)) {
							return -ErrorCodes.ENOMEM();
						}
					} catch (SecretKeyNotFoundException e) {
						log_error(e);
						continue;
					} catch (FileNotFoundException e) {
						log_error(e);
						continue;
					} catch (IOException e) {
						log_error(e);
						continue;
					}
				}
			}
		} catch (SecretKeyNotFoundException e) {
			log_error(e);
			return -ErrorCodes.ENOENT();
		} catch (FileNotFoundException e) {
			log_error(e);
			return -ErrorCodes.ENOENT();
		}
		return 0;
	}

	@Override
	public int release(final String path, final FileInfoWrapper info) {
		try {
			// panboxFS._closeFile(path, info.fh());
			panboxFS.release(path, info.fh());
		} catch (SecretKeyNotFoundException e) {
			log_error(e);
			return -ErrorCodes.ENOENT();
		} catch (FileNotFoundException e) {
			log_error(e);
			return -ErrorCodes.ENOENT();
		} catch (IOException e) {
			log_error(e);
			return -ErrorCodes.EIO();
		} catch (PanboxHandleException e) {
			log_error(e);
			return -ErrorCodes.EIO();
		}
		return 0;
	}

	@Override
	public int utimens(String path, TimeBufferWrapper wrapper) {
		try {
			panboxFS.setLastAccessTime(path,
					FileInfo.unixLong2JavaLong(wrapper.ac_sec()));
			panboxFS.setModifiedTime(path,
					FileInfo.unixLong2JavaLong(wrapper.mod_sec()));
		} catch (IOException e) {
			log_error(e);
			return -ErrorCodes.ENOENT();
		} catch (ObfuscationException e) {
			log_error(e);
			VFSErrorMessages.showErrorMessage(MessageFormat.format(
					bundle.getString("FuseUserFS.fileCouldNotBeObfuscated"),
					path), bundle.getString("error"));
			return -ErrorCodes.ENOENT();
		}
		return 0;
	}

	@Override
	public int rename(final String path, final String newName) {
		try {
			panboxFS.rename(path, newName);
		} catch (SecretKeyNotFoundException e) {
			log_error(e);
			return -ErrorCodes.ENOENT();
		} catch (FileNotFoundException e) {
			log_error(e);
			return -ErrorCodes.ENOENT();
		} catch (PanboxRenameFailedException e) {
			log_error(e);
			return -ErrorCodes.EEXIST();
		} catch (ObfuscationException e) {
			log_error(e);
			VFSErrorMessages.showErrorMessage(MessageFormat.format(
					bundle.getString("FuseUserFS.fileCouldNotBeObfuscated"),
					path), bundle.getString("error"));
			return -ErrorCodes.EIO();
		} catch (IOException e) {
			log_error(e);
			return -ErrorCodes.EACCES();
		}
		return 0;
	}

	@Override
	public int rmdir(final String path) {
		try {
			panboxFS.rmdir(path);
		} catch (SecretKeyNotFoundException e) {
			log_error(e);
			return -ErrorCodes.ENOENT();
		} catch (FileNotFoundException e) {
			log_error(e);
			return -ErrorCodes.ENOENT();
		} catch (IOException e) {
			log_error(e);
			return -ErrorCodes.EIO();
		} catch (ObfuscationException e) {
			log_error(e);
			VFSErrorMessages.showErrorMessage(MessageFormat.format(
					bundle.getString("FuseUserFS.fileCouldNotBeObfuscated"),
					path), bundle.getString("error"));
			return -ErrorCodes.EIO();
		}
		return 0;
	}

	@Override
	public int statfs(final String path, final StatvfsWrapper wrapper) {

		// NOTE: as the panbox VFS is not a real fs on one single physical
		// storage volume, but may reference multiple shares with differing
		// storage backend paths, we are unable to determine a sensible overall
		// number of available/total bytes.

		wrapper.setBlockInfo(panboxFS.getFreeBytes() / fakeBlockSize,
				panboxFS.getUsableBytes() / fakeBlockSize,
				panboxFS.getTotalBytes() / fakeBlockSize).setSizes(
				fakeBlockSize, 0);
		return 0;
	}

	@Override
	public int truncate(final String path, final long offset) {
		try {
			panboxFS.truncate(path, offset);
		} catch (SecretKeyNotFoundException e) {
			log_error(e);
			return -ErrorCodes.ENOENT();
		} catch (FileNotFoundException e) {
			log_error(e);
			return -ErrorCodes.ENOENT();
		} catch (IOException e) {
			log_error(e);
			return -ErrorCodes.EIO();
		} catch (ObfuscationException e) {
			log_error(e);
			VFSErrorMessages.showErrorMessage(MessageFormat.format(
					bundle.getString("FuseUserFS.fileCouldNotBeObfuscated"),
					path), bundle.getString("error"));
			return -ErrorCodes.EIO();
		} catch (PanboxEncryptionException e) {
			log_error(e);
			VFSErrorMessages.showErrorMessage(MessageFormat.format(
					bundle.getString("FuseUserFS.fileCouldNotBeEncrypted"),
					path), bundle.getString("error"));
			return -ErrorCodes.EIO();
		}
		return 0;
	}

	@Override
	public int unlink(final String path) {
		try {
			panboxFS.unlink(path);
		} catch (PanboxDeleteFailedException e) {
			log_error(e);
			return -ErrorCodes.ENOENT();
		} catch (SecretKeyNotFoundException e) {
			log_error(e);
			return -ErrorCodes.ENOENT();
		} catch (FileNotFoundException e) {
			log_error(e);
			return -ErrorCodes.ENOENT();
		} catch (ObfuscationException e) {
			log_error(e);
			VFSErrorMessages.showErrorMessage(MessageFormat.format(
					bundle.getString("FuseUserFS.fileCouldNotBeObfuscated"),
					path), bundle.getString("error"));
			return -ErrorCodes.EIO();
		}
		return 0;
	}

	@Override
	public boolean userfs_mount(final PanboxFS panboxFS, final File mountPoint) {
		return userfs_mount(panboxFS, mountPoint, true);
	}

	public boolean userfs_mount(final PanboxFS panboxFS, final File mountPoint,
			final boolean blocking) {
		if (!(panboxFS instanceof PanboxFSLinux)) {
			logger.fatal("FUSE implementation requires an instance of PanboxFSLinux!");
			return false;
		} else {
			this.panboxFS = (PanboxFSLinux) panboxFS;
			try {
				mount(mountPoint, blocking, panboxFS);
			} catch (final FuseException e) {
				return false;
			}
			return true;
		}
	}

	@Override
	public boolean userfs_unmount(final File mountPoint) {
		try {
			unmount();
		} catch (final Exception e) {
			return false;
		}
		return true;
	}

	@Override
	public int write(final String path, final ByteBuffer buf,
			final long bufSize, final long writeOffset,
			final FileInfoWrapper info) {
		try {
			return panboxFS.write(path, buf, writeOffset, bufSize, info.fh());
		} catch (SecretKeyNotFoundException e) {
			log_error(e);
			return -ErrorCodes.ENOENT();
		} catch (FileNotFoundException e) {
			log_error(e);
			return -ErrorCodes.ENOENT();
		} catch (PanboxEncryptionException e) {
			log_error(e);
			VFSErrorMessages.showErrorMessage(MessageFormat.format(
					bundle.getString("FuseUserFS.fileCouldNotBeEncrypted"),
					path), bundle.getString("error"));
			return -ErrorCodes.EIO();
		} catch (IOException e) {
			log_error(e);
			return -ErrorCodes.ENOENT();
		} catch (PanboxHandleException e) {
			log_error(e);
			return -ErrorCodes.EIO();
		}
	}

	@Override
	public int chmod(final String path, final ModeWrapper mode) {
		try {
			panboxFS.chmod(path, mode.getBits());
			return 0;
		} catch (SecretKeyNotFoundException e) {
			log_error(e);
			return -ErrorCodes.ENOENT();
		} catch (FileNotFoundException e) {
			log_error(e);
			return -ErrorCodes.ENOENT();
		} catch (IOException e) {
			log_error(e);
			return -ErrorCodes.EACCES();
		} catch (ObfuscationException e) {
			log_error(e);
			VFSErrorMessages.showErrorMessage(MessageFormat.format(
					bundle.getString("FuseUserFS.fileCouldNotBeObfuscated"),
					path), bundle.getString("error"));
			return -ErrorCodes.EIO();
		}
	}

	@Override
	public int lock(String path, FileInfoWrapper info, FlockCommand command,
			FlockWrapper flock) {

		// NOTE: libreoffice applications rely on fcntl(2) NOT returning ENOSYS.
		// See
		// http://fuse.sourceforge.net/doxygen/structfuse__operations.html#a1c3fff5cf0c1c2003d117e764b9a76fd

		// TODO: Actual implementation, should any applications break due to
		// missing locking mechanism
		return 0;
	}

	@Override
	public AbstractFileInfo createFileInfo(String fileName, boolean b, long i,
			long creationTime, long lastAccessTime, long lastWriteTime,
			long attr, boolean symbolic) {
		return new FileInfo(fileName, b, i, creationTime, lastAccessTime,
				lastWriteTime, attr, symbolic);
	}

	@Override
	public int symlink(String path, String target) {
		try {
			panboxFS.symlink(path, target);
			return 0;
		} catch (IOException e) {
			log_error(e);
			return -ErrorCodes.EIO();
		}
	}

	@Override
	public int readlink(String path, ByteBuffer buffer, long size) {
		try {
			panboxFS.readlink(path, buffer, size);
			return 0;
		} catch (IOException e) {
			log_error(e);
			return -ErrorCodes.EIO();
		}
	}

	@Override
	public void beforeUnmount(File mountPoint) {
		try {
			super.beforeUnmount(mountPoint);
			panboxFS.beforeUnmount(mountPoint);
		} catch (Exception e) {
			log_error(e);
		}

	}

}
