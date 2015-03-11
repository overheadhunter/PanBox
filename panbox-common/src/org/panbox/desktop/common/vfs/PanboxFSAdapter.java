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

/**
 * @author palige
 * 
 *         Adapter interface to be implemented by all VFS-Library connector
 *         classes aiming at integrating subclasses of PanboxFS. 
 * 
 */
public interface PanboxFSAdapter {

	public boolean userfs_mount(PanboxFS panboxFS, File mountPoint);

	public boolean userfs_unmount(File mountPoint);

	public AbstractFileInfo createFileInfo(String fileName,
			boolean isDirectory, long fileSize, long creationTime,
			long lastAccessTime, long lastWriteTime, long attr, boolean isSymbolic);
}
