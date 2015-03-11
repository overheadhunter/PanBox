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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.SecretKey;

import org.apache.commons.io.FilenameUtils;
import org.panbox.core.crypto.Obfuscator;
import org.panbox.core.exception.MissingIVException;
import org.panbox.core.exception.ObfuscationException;
import org.panbox.core.keymgmt.ShareKey;
import org.panbox.core.vfs.backend.VirtualFile;
import org.panbox.desktop.common.vfs.backend.exceptions.SecretKeyNotFoundException;

public class VirtualRootMultiuserVolume implements IRootVolume {

	// singleton

	private static VirtualRootMultiuserVolume instance;

	private VirtualRootMultiuserVolume() {
	}

	public synchronized static IRootVolume getInstance() {
		if (instance == null) {
			instance = new VirtualRootMultiuserVolume();
		}
		return instance;
	}

	// root-file, user-files and VFS management

	private final Map<String, VirtualMultiuserRootFile> rootFilesPerUser = new ConcurrentHashMap<String, VirtualMultiuserRootFile>();

	private final VirtualMultiuserRootFile generateVirtualFileForUser(
			final String sid) {
		return new VirtualMultiuserRootFile(sid);
	}

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
			for (final Entry<String, VirtualMultiuserRootFile> entry : rootFilesPerUser
					.entrySet()) {
				files.add(entry.getValue());
			}
			return files.toArray(new VirtualFile[files.size()]);
		}
	};

	/**
	 * Adds a new instance of VFSShare to the list of attached shares.
	 * 
	 * @param share
	 *            VFSShare instance of the newly registered share.
	 */
	@Override
	public final synchronized void registerShare(String sid, VFSShare share) {
		if (rootFilesPerUser.get(sid) == null) {
			rootFilesPerUser.put(sid, generateVirtualFileForUser(sid));
		}
		rootFilesPerUser.get(sid).addShare(share);
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
	@Override
	public final synchronized boolean removeShare(String sid, String shareName) {
		if (rootFilesPerUser.get(sid) == null) {
			// ignore request: user has no shares at all!
			return false;
		}
		boolean result = rootFilesPerUser.get(sid).removeShare(shareName);
		if (!rootFilesPerUser.get(sid).hasShares()) {
			rootFilesPerUser.remove(sid);
		}
		return result;
	}

	@Override
	public final boolean existsAndChanged(String sid, VFSShare vfsShare) {
		if (rootFilesPerUser.get(sid) == null) {
			synchronized (this) {
				rootFilesPerUser.put(sid, generateVirtualFileForUser(sid));
				return false;
			}
		}
		return rootFilesPerUser.get(sid).existsAndChanged(vfsShare);
	}

	/**
	 * Since we have more than one VirtualVolume we cannot say anything about
	 * the space! So we will turn of the information on these infos.
	 * 
	 * @return Always 0, since this disables the information on how much free
	 *         space is still left on Windows/Linux.
	 */
	public long getFreeSpace() {
		return 107374182400L;
	}

	/**
	 * Since we have more than one VirtualVolume we cannot say anything about
	 * the space! So we will turn of the information on these infos.
	 * 
	 * @return Always 0, since this disables the information on how much total
	 *         space exists on Windows/Linux.
	 */
	public long getTotalSpace() {
		return 107374182400L;
	}

	/**
	 * Since we have more than one VirtualVolume we cannot say anything about
	 * the space! So we will turn of the information on these infos.
	 * 
	 * @return Always 0, since this disables the information on how much usable
	 *         space exists on Windows/Linux.
	 */
	public long getUsableSpace() {
		return 107374182400L;
	}

	/**
	 * Gets the share key as SecretKey instance for the specified file. This
	 * will iterate over all shares and check which share contains this file. If
	 * the share notifies, that it contains the file, the share key of this
	 * share will be used as files' share key.
	 * 
	 * @param fileName
	 *            Relative path of the file.
	 * @return SecretKey instance of the share key of the share that contains
	 *         the specified file.
	 */
	public synchronized SecretKey getObfuscationKeyForFile(String fileName)
			throws SecretKeyNotFoundException {
		String normalizedFileName = FilenameUtils.normalize(fileName);

		for (VirtualMultiuserRootFile f : rootFilesPerUser.values()) {
			for (VFSShare share : f.getShares()) {
				if (share.contains(normalizedFileName)) {
					return share.getObfuscationKey();
				}
			}
		}
		throw new SecretKeyNotFoundException(
				"Could not obtain a share key for the specified file '"
						+ normalizedFileName + "'.");
	}

	public synchronized Obfuscator getObfuscator(String fileName)
			throws SecretKeyNotFoundException {
		String normalizedFileName = FilenameUtils.normalize(fileName);

		for (VirtualMultiuserRootFile f : rootFilesPerUser.values()) {
			for (VFSShare share : f.getShares()) {
				if (share.contains(normalizedFileName)) {
					return share.getObfuscator();
				}
			}
		}
		return null;
	}

	public synchronized SecretKey getShareKeyForFile(String fileName,
			int version) throws SecretKeyNotFoundException {
		String normalizedFileName = FilenameUtils.normalize(fileName);
		for (VirtualMultiuserRootFile f : rootFilesPerUser.values()) {
			for (VFSShare share : f.getShares()) {
				if (share.contains(normalizedFileName
						.substring(normalizedFileName
								.indexOf(File.separator, 1)))) {
					return share.getShareKey(version);
				}
			}
		}
		throw new SecretKeyNotFoundException(
				"Could not obtain a share key for the specified file '"
						+ normalizedFileName + "'.");
	}

	public synchronized ShareKey getLatestShareKeyForFile(String fileName)
			throws SecretKeyNotFoundException {
		String normalizedFileName = FilenameUtils.normalize(fileName);
		for (VirtualMultiuserRootFile f : rootFilesPerUser.values()) {
			for (VFSShare share : f.getShares()) {
				if (share.contains(normalizedFileName
						.substring(normalizedFileName
								.indexOf(File.separator, 1)))) {
					return share.getLatestShareKey();
				}
			}
		}
		throw new SecretKeyNotFoundException(
				"Could not obtain a share key for the specified file '"
						+ normalizedFileName + "'.");
	}

	/**
	 * Gets a VirtualFile instance of the file that has been specified in the
	 * fileName parameter. This will iterate over all shares and check which
	 * share contains this file. If the share notifies, that it contains the
	 * file, a getFile-call will be placed on the share in order to forward to
	 * request to the share itself.
	 * 
	 * @param fileName
	 *            Relative path of the file.
	 * @return VirtualFile instance of the specified file.
	 * @throws IOException
	 */
	public synchronized VirtualFile getFile(final String fileName)
			throws IOException {
		String normalizedFileName = FilenameUtils.normalize(fileName);
		if (normalizedFileName.equals(File.separator)) {
			return rootFile;
		}
		if (rootFilesPerUser.get(normalizedFileName.substring(1)) != null) {
			// this is a user request!
			return rootFilesPerUser.get(normalizedFileName.substring(1));
		} else {
			// ask the shares!
			for (VirtualMultiuserRootFile f : rootFilesPerUser.values()) {
				for (VFSShare share : f.getShares()) {
					if (share.contains(normalizedFileName)) {
						return share.getFile(normalizedFileName);
					}
				}
			}
		}
		throw new FileNotFoundException(
				"Could not get the VirtualFile for the specified file '"
						+ normalizedFileName + "'.");
	}

	/**
	 * Resolves the filePath of a specified VirtualFile in order to get a
	 * relative path, which can be used in Panbox for Obfuscators, etc.
	 * (starting with /<ShareName>/...
	 * 
	 * @param file
	 *            VirtualFile instance of the file which relative filename
	 *            should be resolved.
	 * @return Relative path of the specified file.
	 */
	public synchronized String getRelativePathForFile(VirtualFile file)
			throws FileNotFoundException {
		if (file.equals(rootFile)) {
			return File.separator;
		}
		for (VirtualFile vf : rootFilesPerUser.values()) {
			if (vf.equals(file)) {
				return vf.getFileName();
			}
		}
		for (VirtualMultiuserRootFile f : rootFilesPerUser.values()) {
			for (VFSShare share : f.getShares()) {
				String result = _getRelativePath(share, file);
				if (result != null) {
					return result;
				}
			}
		}
		throw new FileNotFoundException(
				"Could not get the Relative Path for the specified file  '"
						+ file.getFileName() + "'.");
	}

	private String _getRelativePath(VFSShare share, VirtualFile file) {
		if (share.contains(file)) {

			if (FilenameUtils.equalsOnSystem(share.getShareName(),
					file.getFileName())) {
				return File.separator + share.getShareName();
			} else {
				return share.getRelativePath(file);
			}
		} else {
			return null;
		}
	}

	public synchronized String deobfuscatePath(VirtualFile file)
			throws FileNotFoundException, ObfuscationException {
		if (file.equals(rootFile)) {
			return File.separator;
		}
		for (VirtualFile vf : rootFilesPerUser.values()) {
			if (vf.equals(file)) {
				return vf.getFileName();
			}
		}
		String path = null;
		VFSShare s = null;
		for (VirtualMultiuserRootFile f : rootFilesPerUser.values()) {
			if (path == null) {
				for (VFSShare share : f.getShares()) {
					path = _getRelativePath(share, file);
					if (path != null) {
						s = share;
						break;
					}
				}
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

	public synchronized String obfuscatePath(String fileName, boolean createivs)
			throws FileNotFoundException, ObfuscationException {
		if (fileName.equals(File.separator)) {
			return File.separator;
		}
		for (VirtualFile vf : rootFilesPerUser.values()) {
			if (vf.getFileName().equals(fileName)) {
				return vf.getFileName();
			}
		}
		String sid = null;
		VFSShare share = null;
		for (VirtualMultiuserRootFile f : rootFilesPerUser.values()) {
			if (share == null) {
				for (VFSShare s : f.getShares()) {
					try {
						if (s.contains(fileName.substring(fileName.indexOf(
								File.separator, 1)))) {
							share = s;
							sid = f.getSid();
							break;
						}
					} catch (IndexOutOfBoundsException e) {
						// In case a fileName in root will be checked this
						// implementation. Simply ignore this. File does not
						// exists!
						throw new FileNotFoundException(
								"Could not get the Relative Path for the specified file '"
										+ fileName + "'.");
					}
				}
			}
		}
		if (share != null && sid != null) {
			String path = fileName.substring(sid.length() + 1
					+ share.getShareName().length() + 1);
			return File.separator
					+ share.getShareName()
					+ share.getObfuscator().obfuscatePath(path,
							share.getObfuscationKey(), createivs);
		}
		throw new FileNotFoundException(
				"Could not get the Relative Path for the specified file '"
						+ fileName + "'.");
	}

	// Fallback methods for old method structes without username authentication

	@Override
	public void registerShare(VFSShare share) {
		registerShare("Unknown", share);

	}

	@Override
	public boolean removeShare(String shareName) {
		return removeShare("Unknown", shareName);
	}

	@Override
	public boolean existsAndChanged(VFSShare vfsShare) {
		return existsAndChanged("Unknown", vfsShare);
	}
}
