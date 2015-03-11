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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

import net.fusejna.types.TypeMode;

import org.panbox.core.vfs.backend.VirtualVolume;
import org.panbox.desktop.common.vfs.backend.generic.GenericVirtualFileImpl;

public class DropboxVirtualFile extends GenericVirtualFileImpl {

	DropboxVirtualFile(String fileName, VirtualVolume volume) throws IOException {
		super(fileName, volume);

		if (file.exists()) {
			Set<PosixFilePermission> perms = Files.getPosixFilePermissions(Paths.get(fileName));
			long attr = (perms.contains(PosixFilePermission.OWNER_READ) ? TypeMode.S_IRUSR : 0L) |
					(perms.contains(PosixFilePermission.OWNER_WRITE) ? TypeMode.S_IWUSR : 0L) |
					(perms.contains(PosixFilePermission.OWNER_EXECUTE) ? TypeMode.S_IXUSR : 0L) |
					(perms.contains(PosixFilePermission.GROUP_READ) ? TypeMode.S_IRGRP : 0L) |
					(perms.contains(PosixFilePermission.GROUP_WRITE) ? TypeMode.S_IWGRP : 0L) |
					(perms.contains(PosixFilePermission.GROUP_EXECUTE) ? TypeMode.S_IXGRP : 0L) |
					(perms.contains(PosixFilePermission.OTHERS_READ) ? TypeMode.S_IROTH : 0L) |
					(perms.contains(PosixFilePermission.OTHERS_WRITE) ? TypeMode.S_IWOTH : 0L) |
					(perms.contains(PosixFilePermission.OTHERS_EXECUTE) ? TypeMode.S_IXOTH : 0L);

			this.attr = attr;
		} else {
			this.attr = 256; // 420 init: owner R+W, group+other R
		}
	}

	@Override
	public void setAttr(long attr) throws IOException {
		this.attr = attr;

		if (file.exists()) {
			Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();

			// add owners permission
			if ((attr & TypeMode.S_IRUSR) > 0) perms.add(PosixFilePermission.OWNER_READ);
			if ((attr & TypeMode.S_IWUSR) > 0) perms.add(PosixFilePermission.OWNER_WRITE);
			if ((attr & TypeMode.S_IXUSR) > 0) perms.add(PosixFilePermission.OWNER_EXECUTE);
			// add group permissions
			if ((attr & TypeMode.S_IRGRP) > 0) perms.add(PosixFilePermission.GROUP_READ);
			if ((attr & TypeMode.S_IWGRP) > 0) perms.add(PosixFilePermission.GROUP_WRITE);
			if ((attr & TypeMode.S_IXGRP) > 0) perms.add(PosixFilePermission.GROUP_EXECUTE);
			// add others permissions
			if ((attr & TypeMode.S_IROTH) > 0) perms.add(PosixFilePermission.OTHERS_READ);
			if ((attr & TypeMode.S_IWOTH) > 0) perms.add(PosixFilePermission.OTHERS_WRITE);
			if ((attr & TypeMode.S_IXOTH) > 0) perms.add(PosixFilePermission.OTHERS_EXECUTE);

			Files.setPosixFilePermissions(Paths.get(file.getPath()), perms);
		}
	}

}
