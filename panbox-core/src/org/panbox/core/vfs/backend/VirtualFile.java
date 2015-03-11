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
package org.panbox.core.vfs.backend;

import java.io.File;
import java.io.IOException;

public abstract class VirtualFile {
	private final String fileName;
	protected final VirtualVolume volume;
	protected long attr = -1;

	protected VirtualFile(String fileName, VirtualVolume volume) {
		this.fileName = fileName;
		this.volume = volume;
	}

	public String getFileName() {
		return fileName;
	}

	public boolean delete() {
		return false;
	}
	
	public void setDeleteOnClose() {
	}

	public boolean createNewFile() {
		return false;
	}

	public boolean createNewDirectory() {
		return false;
	}

	public boolean renameTo(VirtualFile newFile) {
		return false;
	}

	public abstract VirtualFile[] list() throws IOException;

	// --------------------- File operations ---------------------

	public File getFile() {
		return null;
	}

	public abstract String getPath();

	public abstract String getRelativePath();

	// --------------------- Standard getters ---------------------

	public boolean exists() {
		return true;
	}

	public boolean isDirectory() {
		return true;
	}

	public long length() throws IOException {
		return 0;
	}

	public long getCreationTime() {
		return 0;
	}

	public long getLastAccessTime() {
		return 0;
	}

	public long getLastWriteTime() {
		return 0;
	}

	public boolean isSymbolic() {
		return false;
	}

	@Override
	public boolean equals(Object obj) {

		if (null == obj) {
			return false;
		}

		if (obj.getClass() != this.getClass()) {
			return false;
		}

		VirtualFile other = (VirtualFile) obj;

		if (null == fileName) {
			if (null != other.fileName) {
				return false;
			}
		}

		if (fileName != null && fileName.equals(other.fileName)) {
			return true;
		}
		return false;
	}

	public long getAttr() {
		return attr;
	}

	public void setAttr(long attr) throws IOException {
		this.attr = attr;
	}

	public void setModifiedTime(long mtime) throws IOException {

	}

	public void setLastAccessTime(long atime) throws IOException {

	}

	public void createSymlink(String link) {

	}

	public boolean canRead() {
		return false;
	}

	public boolean canWrite() {
		return false;
	}

	public boolean canExecute() {
		return false;
	}

}
