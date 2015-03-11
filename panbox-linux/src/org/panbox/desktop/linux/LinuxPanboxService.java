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
package org.panbox.desktop.linux;

import java.io.File;
import java.io.FileNotFoundException;
import java.rmi.RemoteException;

import org.apache.commons.io.FilenameUtils;
import org.panbox.core.exception.ObfuscationException;
import org.panbox.core.keymgmt.VolumeParams;
import org.panbox.core.vfs.backend.VirtualVolume;
import org.panbox.desktop.common.sharemgmt.AbstractPanboxService;
import org.panbox.desktop.common.vfs.DropboxVirtualVolume;
import org.panbox.desktop.common.vfs.PanboxFS;
import org.panbox.desktop.common.vfs.backend.VFSShare;
import org.panbox.desktop.common.vfs.backend.VirtualRootVolume;

public class LinuxPanboxService extends AbstractPanboxService {

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
		VirtualRootVolume.getInstance().registerShare(vfsShare);
	}

	@Override
	protected boolean unregisterShare(VolumeParams p) {
		return VirtualRootVolume.getInstance().removeShare(p.shareName);
	}

	@Override
	public synchronized String getOnlineFilename(VolumeParams p, String fileName)
			throws RemoteException, FileNotFoundException, ObfuscationException {
		String shareid = FilenameUtils.getName(p.path); // Dropbox share name
		String path = File.separator + p.shareName + fileName;
		PanboxFS fs = VFSControl.getInstance().getVFS();
		String obf = fs.backingStorage.obfuscatePath(path, false);
		return obf.replace(p.shareName, shareid);
	}
}
