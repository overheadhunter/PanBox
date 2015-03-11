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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.panbox.core.crypto.AbstractObfuscatorFactory;
import org.panbox.core.crypto.FileObfuscatorFactory;
import org.panbox.core.exception.ObfuscationException;
import org.panbox.core.vfs.backend.VirtualFile;

public class VirtualMultiuserRootFile extends VirtualFile {

	private static final Logger logger = Logger
			.getLogger(VirtualMultiuserRootFile.class);
	private final String sid;
	private final Queue<VFSShare> userShares;

	VirtualMultiuserRootFile(String sid) {
		super(File.separator + sid, null);
		this.sid = sid;
		this.userShares = new ConcurrentLinkedQueue<>();
	}

	/**
	 * Gets the relative Path for the VirtualRoot-File
	 * 
	 * @return returns java.io.File.seperator
	 */
	@Override
	public String getRelativePath() {
		return File.separator + sid;
	}

	/**
	 * Gets the Path for the VirtualRoot-File
	 * 
	 * @return returns java.io.File.seperator
	 */
	@Override
	public String getPath() {
		return File.separator + sid;
	}

	/**
	 * Lists all directories in the VirtualRootVolume, which are all attached
	 * shares.
	 * 
	 * @return Array of VirtualFile contains a VirtualFile wrapper file, that
	 *         leads directory to the root-file of the attached shares.
	 */
	@Override
	public VirtualFile[] list() {
		List<VirtualFile> files = new ArrayList<VirtualFile>();
		for (final VFSShare share : userShares) {
			VirtualFile shareFile = null;
			shareFile = new VirtualFile(share.getShareName(),
					share.getBackend()) {

				@Override
				public VirtualFile[] list() throws IOException {
					return share.getBackend().getFile(File.separator).list(); // forward
																				// to
																				// VirtualVolume
																				// of
																				// share
				}

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

	public synchronized void addShare(VFSShare share) {
		userShares.add(share);
	}

	public synchronized boolean removeShare(String shareName) {
		Iterator<VFSShare> it = userShares.iterator();
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

	public synchronized boolean hasShares() {
		return !userShares.isEmpty();
	}

	public synchronized boolean existsAndChanged(VFSShare vfsShare) {
		for (VFSShare share : userShares) {
			if (share.getShareName().equals(vfsShare.getShareName())) {
				if (FilenameUtils.equalsNormalizedOnSystem(share.getBackend()
						.getRoot().getPath(), vfsShare.getBackend().getRoot()
						.getPath())) {
					return true; // exists and changed!
				} else {
					return false; // exists but didn't change
				}
			}
		}
		return false; // does not exist
	}

	public Queue<VFSShare> getShares() {
		return userShares;
	}

	public String getSid() {
		return sid;
	}
}
