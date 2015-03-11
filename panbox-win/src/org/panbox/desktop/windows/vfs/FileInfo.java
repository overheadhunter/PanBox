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

import java.io.File;

import net.decasdev.dokan.ByHandleFileInformation;
import net.decasdev.dokan.FileAttribute;
import net.decasdev.dokan.Win32FindData;

import org.panbox.desktop.common.vfs.AbstractFileInfo;

public class FileInfo extends AbstractFileInfo {

    final ByHandleFileInformation byHandleInfo = new ByHandleFileInformation();


    public FileInfo(String fileName, boolean b, long i, long creationTime,
			long lastAccessTime, long lastWriteTime) {
		super(fileName, b, i, creationTime, lastAccessTime, lastWriteTime);
	}

	public int getDokanAttributes() {
        int attrib = FileAttribute.FILE_ATTRIBUTE_NORMAL;
        if (isDirectory) {
            attrib |= FileAttribute.FILE_ATTRIBUTE_DIRECTORY;
        }
        return attrib;
    }

	public ByHandleFileInformation toByhandleFileInformation(final int volumeSerialNumber) {
	    byHandleInfo.fileAttributes = getDokanAttributes();
	    byHandleInfo.creationTime = creationTime;
	    byHandleInfo.lastAccessTime = lastAccessTime;
	    byHandleInfo.lastWriteTime = lastWriteTime;
	    byHandleInfo.volumeSerialNumber = volumeSerialNumber;
	    byHandleInfo.fileSize = fileSize;
	    byHandleInfo.numberOfLinks = 1;
	    byHandleInfo.fileIndex = index;
	    return byHandleInfo;
	}

	public Win32FindData toFindData() {
	    final String name = new File(fileName).getName();
	    return new Win32FindData(getDokanAttributes(), unixLong2Longlong(creationTime), unixLong2Longlong(lastAccessTime), unixLong2Longlong(lastWriteTime), fileSize, 0, 0, name, name);
	}
	
	@Override
	public String toString() {
	    return "FileInfo(fileName=" + fileName + "/isDirectory=" + isDirectory + "/fileSize=" + fileSize + ")";
	}
}
