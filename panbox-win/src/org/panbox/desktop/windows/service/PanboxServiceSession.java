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
package org.panbox.desktop.windows.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;

import javax.naming.ConfigurationException;

import org.apache.commons.io.FilenameUtils;
import org.panbox.core.exception.ObfuscationException;
import org.panbox.core.keymgmt.VolumeParams;
import org.panbox.core.vfs.backend.VirtualVolume;
import org.panbox.desktop.common.sharemgmt.AbstractPanboxService;
import org.panbox.desktop.common.utils.DesktopApi;
import org.panbox.desktop.common.vfs.PanboxFS;
import org.panbox.desktop.common.vfs.backend.IRootVolume;
import org.panbox.desktop.common.vfs.backend.VFSShare;
import org.panbox.desktop.common.vfs.backend.VirtualRootMultiuserVolume;
import org.panbox.desktop.common.vfs.backend.VirtualRootVolume;
import org.panbox.desktop.common.vfs.backend.dropbox.DropboxVirtualVolume;

/**
 * PanboxServiceSession is used in Windows Panbox to implement the
 * IPanboxService interface for a single user who authenticated with the
 * PanboxWindowsService already so that it is known which Windows user is
 * running this call.
 * 
 * @author Clemens A. Schulz <c.schulz@sirrix.com>
 * 
 */
public class PanboxServiceSession extends AbstractPanboxService {

	private final String username;

	public PanboxServiceSession(String username) {
		this.username = username;
	}

	@Override
	public String getOnlineFilename(VolumeParams p, String fileName)
			throws RemoteException, FileNotFoundException, ObfuscationException {
		String shareid = FilenameUtils.getName(p.path); // Dropbox share name
		String path = File.separator + username + File.separator + p.shareName
				+ File.separator + fileName;
		PanboxFS fs;
		try {
			fs = VFSManager.getInstance().getVFS();
			String obf = fs.backingStorage.obfuscatePath(path, false);
			String windowsPath = obf.replace(p.shareName, shareid);
			return windowsPath.replace("\\", "/");
		} catch (ConfigurationException | IllegalArgumentException
				| IllegalAccessException | InvocationTargetException e) {
			throw new RemoteException("Error obtaining VFS manager instance", e);
		}
	}

	@Override
	protected VirtualVolume getVirtualVolume(VolumeParams p)
			throws FileNotFoundException {
		VirtualVolume virtualVolume = null;
		switch (p.type) {
		case FOLDER:
			// same
		case DROPBOX:
			virtualVolume = new DropboxVirtualVolume(p.path);
			// TODO: Refactor to generic virtualvolume instead of dropbox
			break;
		}
		return virtualVolume;
	}

	@Override
	protected void registerShare(VFSShare vfsShare, VolumeParams p) {
		IRootVolume vrv = null;
		if (DesktopApi.isMultiuserModeDisabled()) {
			vrv = VirtualRootVolume.getInstance();
		} else {
			vrv = VirtualRootMultiuserVolume.getInstance();
		}
		if (vrv.existsAndChanged(username, vfsShare)) {
			vrv.removeShare(username, p.shareName);
			vrv.registerShare(username, vfsShare);
		} else {
			vrv.registerShare(username, vfsShare);
		}
	}

	@Override
	protected boolean unregisterShare(VolumeParams p) {
		boolean removed = false;
		if (DesktopApi.isMultiuserModeDisabled()) {
			removed = VirtualRootVolume.getInstance().removeShare(p.shareName);
		} else {
			removed = VirtualRootMultiuserVolume.getInstance().removeShare(
					username, p.shareName);
		}
		return removed;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PanboxServiceSession) {
			PanboxServiceSession other = (PanboxServiceSession) obj;
			if (other.username.equals(this.username)) {
				return true;
			}
		}
		return false;
	}

}
