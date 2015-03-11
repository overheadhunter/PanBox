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
package org.panbox.mobile.android.dropbox.vfs;

import com.dropbox.client2.DropboxAPI.Entry;

import org.panbox.core.vfs.backend.VirtualFile;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DropboxVirtualFile extends VirtualFile {

	private Entry fileInfo; // file Information from Dropbox
	private String path;
	private DropboxVirtualVolume volume;

	public DropboxVirtualFile(String path, DropboxVirtualVolume volume) {
		super(path, volume);
		this.volume = volume;
		if (volume != null) {
			this.fileInfo = volume.mDBCon.getFileInfo(path);
		}
		this.path = path;
	}
	
	//TODO: check this
	public DropboxVirtualFile(String path, DropboxVirtualVolume volume, Entry fileinfo) {
		super(path, null);
		this.path = path;
		this.fileInfo = fileinfo;
		this.volume = volume;
	}

	@Override
	public boolean isDirectory() {
		return fileInfo.isDir;
	}

	@Override
	public boolean delete() {
		return volume.mDBCon.deleteFile(path);
	}

	/**
	 * Returns the file name of the DropboxVirtualFile
	 */
	@Override
	public String getFileName() {
		return new File(path).getName();
	}

	/**
	 * This function is bugged.
	 * Caution: you cannot upload an empty file, which means you cannot create an empty file within dropbox
	 */
	@Override
	public boolean createNewFile() {
		return volume.mDBCon.createFile(path);
	}

	@Override
	public boolean createNewDirectory() {
		return volume.mDBCon.createFolder(path);
	}

	@Override
	public boolean exists() {
		return fileInfo != null;
	}

	@Override
	public long length() {
		return fileInfo.bytes;
	}

	@Override
	public DropboxVirtualFile[] list() {
		DropboxVirtualFile[] files = new DropboxVirtualFile[fileInfo.contents.size()];
		if (isDirectory()) {
			for (int i = 0; i < fileInfo.contents.size(); i++) {
				files[i] = (new DropboxVirtualFile(fileInfo.contents.get(i).fileName(), volume));
			}
			return files;
		} else {
			return null;
		}
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public String getRelativePath() {
		return volume.getRelativePathForFile(this);
	}

	public boolean renameTo(String newFileName) {
		boolean result = volume.mDBCon.renameFile(path, newFileName);
		this.path = new File(path).getParentFile().getAbsolutePath() + File.separator + newFileName;
		return result;
	}

	@Override
	public long getLastAccessTime() {
		SimpleDateFormat sdfToDate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.getDefault());
		Date date;
		try {
			date = sdfToDate.parse(fileInfo.modified);
		} catch (ParseException e) {
			e.printStackTrace();
			return 0;
		}
		return date.getTime();
	}

	@Override
	public long getCreationTime() {
		return getLastAccessTime();
	}

	@Override
	public long getLastWriteTime() {
		return getLastAccessTime();
	}

}
