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
package org.panbox.desktop.common.vfs.backend.generic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.panbox.core.vfs.backend.VirtualFile;
import org.panbox.core.vfs.backend.VirtualVolume;

public class GenericVirtualVolumeImpl extends VirtualVolume {

	protected File realFile;

	public GenericVirtualVolumeImpl(String rootDirectory)
			throws FileNotFoundException {
		super(new GenericVirtualFileImpl(rootDirectory, null));
		realFile = new File(rootDirectory);
		if (!realFile.exists()) {
			throw new FileNotFoundException("Rootdir does not exists: "
					+ rootDirectory);
		}
	}

	@Override
	public VirtualFile getFile(String fileName) throws IOException {
		File f = fileName.startsWith(realFile.getAbsolutePath()) ? new File(
				fileName) : new File(realFile, fileName);

		// TODO: filename normalization at higher level
		// only return one VirtualFile-instance for a given normalized filename
		String normalizedName = FilenameUtils.normalize(f.getAbsolutePath());
		return new GenericVirtualFileImpl(normalizedName, this);
	}

	@Override
	public String getRelativePathForFile(VirtualFile file) {
		String relativePath = file.getPath().substring(
				realFile.getAbsolutePath().length());
		if (relativePath.length() > 0) {
			return relativePath;
		} else {
			return File.separator;
		}
	}

}
