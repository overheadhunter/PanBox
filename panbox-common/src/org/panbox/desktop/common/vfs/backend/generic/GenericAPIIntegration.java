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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.panbox.PanboxConstants;
import org.panbox.core.csp.CSPException;
import org.panbox.core.csp.ICSPAPIIntegration;
import org.panbox.core.csp.Revision;
import org.panbox.core.keymgmt.Volume;
import org.panbox.desktop.common.vfs.backend.dropbox.CSPFileNotFoundException;
import org.panbox.desktop.common.vfs.backend.dropbox.CSPIOException;

public class GenericAPIIntegration implements ICSPAPIIntegration {

	@Override
	public List<Revision> getRevisions(String path) throws CSPException {
		throw new NotImplementedException(
				"method not implemented for generic api adapter");
	}

	@Override
	public boolean restoreToRevision(String path, String rev) throws CSPException {
		throw new NotImplementedException(
				"method not implemented for generic api adapter");
	}

	@Override
	public boolean supportsMetadataFreshness() {
		return false;
	}

	@Override
	public File getShareMetadata(String shareName) throws CSPException {
		String filename = shareName + File.separator
				+ PanboxConstants.PANBOX_SHARE_METADATA_DIRECTORY
				+ File.separator + Volume.DB_FILE;
		if (exists(filename)) {
			return new File(filename);
		} else {
			try {
				throw new FileNotFoundException(filename);
			} catch (FileNotFoundException e) {
				throw new CSPFileNotFoundException(e);
			}
		}
	}

	@Override
	public String getLatestShareMetadataVersion(String shareName)
			throws CSPException {
		throw new NotImplementedException(
				"method not implemented for generic api adapter");
	}

	@Override
	public URI createPublicLink(String path) throws CSPException {
		throw new NotImplementedException(
				"method not implemented for generic api adapter");
	}

	@Override
	public void inviteUser(String shareName, String userIdentifier)
			throws CSPException {
		throw new NotImplementedException(
				"method not implemented for generic api adapter");
	}

	@Override
	public void removeUser(String shareName, String userIdentifier)
			throws CSPException {
		throw new NotImplementedException(
				"method not implemented for generic api adapter");
	}

	@Override
	public Date getServerTime() throws CSPException {
		throw new NotImplementedException(
				"method not implemented for generic api adapter");
	}

	@Override
	public URI publishFile(File f) throws CSPException {
		throw new NotImplementedException(
				"method not implemented for generic api adapter");
	}

	@Override
	public String uploadFile(String serverPath, File f) throws CSPIOException {
		File dest = new File(serverPath);
		try {
			Files.copy(f.toPath(), dest.toPath());
		} catch (IOException e) {
			throw new CSPIOException(e);
		}
		return dest.getAbsolutePath();
	}

	@Override
	public boolean isOnline() {
		throw new NotImplementedException(
				"method not implemented for generic api adapter");
	}

	@Override
	public boolean createLock(String path) throws CSPException {
		throw new NotImplementedException(
				"method not implemented for generic api adapter");
	}

	@Override
	public boolean createTemporaryLock(String path) throws CSPException {
		throw new NotImplementedException(
				"method not implemented for generic api adapter");
	}

	@Override
	public boolean isLocked(String path) throws CSPException {
		throw new NotImplementedException(
				"method not implemented for generic api adapter");
	}

	@Override
	public void releaseLock(String path) throws CSPException {
		throw new NotImplementedException(
				"method not implemented for generic api adapter");
	}

	@Override
	public void releaseTemporaryLock(String path) throws CSPException {
		throw new NotImplementedException(
				"method not implemented for generic api adapter");
	}

	@Override
	public boolean exists(String path) throws CSPException {
		return Files.exists(Paths.get(path));
	}

	@Override
	public Date getLastModificationDate(String path) throws CSPIOException {
		try {
			return new Date(Files.getLastModifiedTime(Paths.get(path)).toMillis());
		} catch (IOException e) {
			throw new CSPIOException(e);
		}
	}

	@Override
	public void deleteFile(String serverPath) throws CSPIOException {
		try {
			Files.delete(Paths.get(serverPath));
		} catch (IOException e) {
			throw new CSPIOException(e);
		}
	}

	@Override
	public void downloadFile(String remotePath, String targetPath) throws CSPIOException {
		File sourceFile = new File(remotePath);
		File targetFile = new File(targetPath);
		if (sourceFile.exists() && sourceFile.isFile()) {
			try {
				FileUtils.copyFile(sourceFile, targetFile);
			} catch (IOException e) {
				throw new CSPIOException(e);
			}
		}
	}

	@Override
	public void downloadFolder(String remotePath, String targetPath) throws CSPIOException {
		File sourceDir = new File(remotePath);
		File targetDir = new File(targetPath);
		if (sourceDir.exists() && sourceDir.isDirectory()) {
			try {
				FileUtils.copyDirectory(sourceDir, targetDir);
			} catch (IOException e) {
				throw new CSPIOException(e);
			}
		}
	}

}
