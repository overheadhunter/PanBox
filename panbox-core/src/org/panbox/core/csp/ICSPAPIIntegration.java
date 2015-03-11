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
package org.panbox.core.csp;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.List;

import org.panbox.core.keymgmt.IVolume;

/**
 * @author palige
 *         <p/>
 *         Interface defines several methods for the integration of Panbox with
 *         CSP APIs.
 */
public interface ICSPAPIIntegration extends ICSPIntegration {

	/**
	 * returns a list of {@link Revision}-instances representing the different
	 * versions of the file stored at the given path.
	 * 
	 * @param path
	 * @return
	 * @throws Exception
	 */
	public List<Revision> getRevisions(String path) throws CSPException;

	/**
	 * tries to restore an old file version
	 * 
	 * @param path
	 * @param rev
	 * @return
	 * @throws Exception
	 */
	boolean restoreToRevision(String path, String rev) throws CSPException;

	/**
	 * Method indicates if this a specific CSP backend implementation for
	 * integration with panbox supports any kind of functionality to ensure
	 * metadata freshness.
	 * <p/>
	 * In order to safely be able to remove single users/devices from a a share,
	 * it is imperative there exists corresponding functionality, to make sure
	 * an attacker is unable to restore old versions of metadata. Specific
	 * implementations may be provider-dependent
	 * 
	 * @return <code>true</code>, if this backend implementation supports
	 *         funtions for safeguarding metadata freshness, <code>false</code>
	 *         otherwise
	 */
	public boolean supportsMetadataFreshness();

	/**
	 * retrieves and returns the latest metadata db for a given share
	 * 
	 * @param shareName
	 *            share identifier
	 * @return the latest metadata db for a given share, if possible,
	 *         <code>null</code> otherwise.
	 */
	@Deprecated
	public File getShareMetadata(String shareName) throws CSPException;

	/**
	 * returns the version number of the latest metadata file for a given share
	 * 
	 * @param shareName
	 *            share identifier
	 * @return
	 */
	@Deprecated
	public String getLatestShareMetadataVersion(String shareName) throws CSPException;

	/**
	 * Creates a public link for the given file, if possible. If necessary, the
	 * file might be copied or moved to corresponding "public" directories
	 * within a CSP clients directory tree or directly be uploaded to the CSP.
	 * 
	 * @param path
	 * @return public {@link URL} to the file, or <code>null</code>
	 * @throws Exception
	 */
	public URI createPublicLink(String path) throws CSPException;

	/**
	 * Method handles the invitation of another user of a CSP to join a share
	 * via the CSP's access control mechanisms. Note: This does <b>not</b>
	 * include the arrangement of share metadata for a specific user, which is
	 * handled within key management (see {@link IVolume}).
	 * <p/>
	 * The invitation process is to be conducted with the least amount of user
	 * interaction necessary, e.g. by making use of CSP-API calls, if possible.
	 * <p/>
	 * For user removal, see {@link #removeUser(String, String)}.
	 * 
	 * @param shareName
	 *            share identifier the user should be invited to join
	 * @param userIdentifier
	 */
	public void inviteUser(String shareName, String userIdentifier) throws CSPException;

	/**
	 * Method handles the removal of a user from a share via the CSP's access
	 * control mechanisms. Note: This does <b>not</b> include the removal of
	 * user-specific keys from share metadata, which instead is to be handled
	 * within key management (see {@link IVolume}).
	 * <p/>
	 * The removal process is to be conducted with the least amount of user
	 * interaction necessary, e.g. by making use of CSP-API calls, if possible.
	 * <p/>
	 * For user invitation, see {@link #inviteUser(String, String)}.
	 * 
	 * @param shareName
	 *            share identifier the user should be invited to join
	 * @param userIdentifier
	 */
	public void removeUser(String shareName, String userIdentifier) throws CSPException;

	/**
	 * Returns the current server time, if supported. Method may be used for
	 * timestamping share metadata.
	 * 
	 * @return current server time
	 * @throws Exception
	 */
	public Date getServerTime() throws CSPException;

	/**
	 * Methods tries to upload the given {@link File}-instance into the Public
	 * folder and creates a shareable for this file.
	 * 
	 * @param f
	 *            fiel to upload
	 * @return shareable dropbox link
	 * @throws IOException
	 */
	public URI publishFile(File f) throws CSPException;

	/**
	 * Methods handles uploading files via the Dropbox API. If there already
	 * exists a file at the location specified in serverPath, the file will be
	 * overwritten
	 * 
	 * @param serverPath
	 *            absolute upload path of the file
	 * @param f
	 *            {@link File}-instance of the file to upload
	 * @return the server path the file has been stored at
	 * @throws IOException
	 */
	public String uploadFile(String serverPath, File f) throws CSPException;

	/**
	 * deletes a file from the server
	 * 
	 * @param serverPath
	 * @throws Exception
	 */
	public void deleteFile(String serverPath) throws CSPException;

	/**
	 * indicates if a connection to dropbox via the dropbox api currently can be
	 * established
	 * 
	 * @return <code>true</code> if Dropbox currently can be accessed via API,
	 *         <code>false</code> otherwise
	 */
	public boolean isOnline();

	/**
	 * tries to create a lock file for the given path.
	 * 
	 * @param path
	 *            path to file, for which a lock file should be created
	 * @return <code>true</code> if lock file creation was successful,
	 *         <code>false</code> otherwise
	 * @throws Exception
	 */
	public boolean createLock(String path) throws CSPException;

	/**
	 * tries to create a temporary lock file for the given path.
	 *
	 * @param path
	 *            path to file, for which a lock file should be created
	 * @return <code>true</code> if lock file creation was successful,
	 *         <code>false</code> otherwise
	 * @throws Exception
	 */
	public boolean createTemporaryLock(String path) throws CSPException;

	/**
	 * indicates if the file at the given path currently is locked
	 * removes temporary locks if they surpassed their duration
	 * 
	 * @param path
	 *            path of file to check
	 * @return <code>true</code> if file is locked, <code>false</code> otherwise
	 * @throws Exception
	 */
	public boolean isLocked(String path) throws CSPException;

	/**
	 * tries to release the lock for the file at the given path
	 * 
	 * @param path
	 * @throws Exception
	 */
	public void releaseLock(String path) throws CSPException;

	/**
	 * tries to release the temporary lock for the file at the given path
	 *
	 * @param path
	 * @throws Exception
	 */
	public void releaseTemporaryLock(String path) throws CSPException;

	/**
	 * indicates if there currently exists a file with the given absolute path
	 * within the CSP
	 * 
	 * @param path
	 * @return <code>true</code> if file exists, <code>false</code> otherwise
	 * @throws Exception
	 */
	public boolean exists(String path) throws CSPException;

	/**
	 * reads and returns the last modification timestamp for the given file from
	 * the CSP
	 * 
	 * @param path
	 * @return
	 * @throws Exception
	 */
	public Date getLastModificationDate(String path) throws CSPException;

	/**
	 * Download a single file from the CSP to a local path
	 *
	 * @param remotePath
	 *            path of the file to download
	 * @param targetPath
	 *            local path where the remote file should be downloaded to
	 */
	public void downloadFile(String remotePath, String targetPath) throws CSPException;

	/**
	 * Download all entries of a folder from the CSP to a local path
	 * recursively.
	 * 
	 * @param remotePath
	 *            path of the folder to download
	 * @param targetPath
	 *            local path where the remote contents should be downloaded to
	 */
	public void downloadFolder(String remotePath, String targetPath) throws CSPException;

}
