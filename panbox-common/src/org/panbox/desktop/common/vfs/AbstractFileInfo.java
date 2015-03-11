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

public class AbstractFileInfo {

	public final String fileName;
	protected long fileSize;
	protected final int index;
	protected final boolean isDirectory;
	protected final long creationTime;
	protected final long lastAccessTime;
	protected final long lastWriteTime;
	protected long attr = -1;
	protected boolean symbolic;

	public AbstractFileInfo(final String fileName, final boolean isDirectory,
			final long fileSize, long creationTime, long lastAccessTime,
			long lastWriteTime, long attr, boolean symbolic) {
		this(fileName, isDirectory, fileSize, creationTime, lastAccessTime,
				lastWriteTime, attr);
		this.symbolic = symbolic;
	}

	public AbstractFileInfo(final String fileName, final boolean isDirectory,
			final long fileSize, long creationTime, long lastAccessTime,
			long lastWriteTime, long attr) {
		this(fileName, isDirectory, fileSize, creationTime, lastAccessTime,
				lastWriteTime);
		this.attr = attr;
	}

	public AbstractFileInfo(final String fileName, final boolean isDirectory,
			final long fileSize, long creationTime, long lastAccessTime,
			long lastWriteTime) {
		this.fileName = fileName;
		this.isDirectory = isDirectory;
		this.fileSize = fileSize;
		this.creationTime = creationTime;
		this.lastAccessTime = lastAccessTime;
		this.lastWriteTime = lastWriteTime;
		index = fileName.hashCode();
	}

	public static long unixLong2Longlong(long unixTimeMillisec) {
		return unixTimeMillisec * 10000L + 116444736000000000L;
	}

	public boolean isDirectory() {
		return isDirectory;
	}
	
	public boolean isSymbolic() {
		return symbolic;
	}

	public long getIndexInt() {
		return index;
	}

	public long getIndexLong() {
		return index;
	}

	public long getSize() {
		return fileSize;
	}

	public AbstractFileInfo setSize(final long size) {
		fileSize = size;
		return this;
	}

	/**
	 * @return the creationTime
	 */
	public long getCreationTime() {
		return creationTime;
	}

	/**
	 * @return the lastAccessTime
	 */
	public long getLastAccessTime() {
		return lastAccessTime;
	}

	/**
	 * @return the lastWriteTime
	 */
	public long getLastWriteTime() {
		return lastWriteTime;
	}

	public long getAttr() {
		return attr;
	}
}