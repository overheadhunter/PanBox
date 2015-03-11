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
package org.panbox.desktop.common.vfs.backend.dropbox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.panbox.PanboxConstants;
import org.panbox.Settings;
import org.panbox.core.csp.CSPException;
import org.panbox.core.csp.ICSPAPIIntegration;
import org.panbox.core.csp.Revision;
import org.panbox.core.keymgmt.Volume;
import org.panbox.desktop.common.utils.DesktopApi;

import com.dropbox.core.DbxAccountInfo;
import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxWebAuthNoRedirect;
import com.dropbox.core.DbxWriteMode;

/**
 * Created by Timo Nolle on 01.09.14.
 */
public class DropboxAPIIntegration implements ICSPAPIIntegration {

	private static final ResourceBundle bundle = ResourceBundle.getBundle(
			"org/panbox/desktop/common/gui/Messages", Settings.getInstance()
					.getLocale());

	private final static Logger logger = Logger
			.getLogger(DropboxAPIIntegration.class);

	private static final String METADATA_PATH = "/"
			+ PanboxConstants.PANBOX_SHARE_METADATA_DIRECTORY + "/"
			+ Volume.DB_FILE;

	public DbxClient client;

	private static DropboxAPIIntegration instance = null;

	private DbxRequestConfig dbxRConfig;

	private DropboxAPIIntegration() {
		this.dbxRConfig = new DbxRequestConfig("panbox.org", Settings
				.getInstance().getLocale().toString());
		String ACCESS_TOKEN = Settings.getInstance().getDropboxAccessToken();

		try {
			if ((ACCESS_TOKEN == null) || ACCESS_TOKEN.equals("")) {
				ACCESS_TOKEN = authenticate();
			}
			client = new DbxClient(dbxRConfig, ACCESS_TOKEN);
			Settings.getInstance().setDropboxAccessToken(ACCESS_TOKEN);
		} catch (DropboxIntegrationException e) {
			logger.error("No access token available!", e);
		}
	}

	private DropboxAPIIntegration(String accessToken) {
		this.dbxRConfig = new DbxRequestConfig("panbox.org", Settings
				.getInstance().getLocale().toString());
		client = new DbxClient(dbxRConfig, accessToken);
	}

	/**
	 * Get singleton instance of DropboxIntegration
	 * 
	 * @return
	 * @throws DropboxIntegrationException
	 */
	public synchronized static DropboxAPIIntegration getInstance() {
		if (instance == null) {
			instance = new DropboxAPIIntegration();
		}
		return instance;
	}


	/**
	 * get the share metadata file for a given share
	 * 
	 * @param shareName
	 * @return
	 * @throws Exception
	 */
	@Override
	public File getShareMetadata(String shareName) throws CSPIOException,
			CSPApiException {
		try {
			File tmp = File.createTempFile("panbox", "tmp");
			OutputStream o = new FileOutputStream(tmp);
			client.getFile("/" + shareName + METADATA_PATH, "", o);
			o.flush();
			o.close();
			return tmp;
		} catch (IOException e) {
			throw new CSPIOException(e);
		} catch (DbxException e) {
			throw new CSPApiException(e);
		}
	}

	@Override
	public String getLatestShareMetadataVersion(String shareName)
			throws CSPApiException {
		try {
			return client.getRevisions("/" + shareName + METADATA_PATH).get(0)
					.asFile().rev;
		} catch (DbxException e) {
			throw new CSPApiException(e);
		}
	}

	@Override
	public String uploadFile(String serverPath, File f) throws CSPIOException,
			CSPApiException {
		if ((f == null) || !f.exists() || !f.canRead()) {
			throw new CSPIOException("Could not access file " + f);
		} else {
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(f);
				DbxEntry.File uploadedFile = client.uploadFile(serverPath,
						DbxWriteMode.force(), f.length(), fis);
				return uploadedFile.path;
			} catch (DbxException e) {
				throw new CSPApiException("Error uploading file!", e);
			} catch (IOException e) {
				throw new CSPIOException(e);
			} finally {
				if (fis != null) {
					try {
						fis.close();
					} catch (IOException e) {
						throw new CSPIOException(e);
					}
				}
			}
		}
	}

	@Override
	public URI publishFile(File f) throws CSPIOException, CSPApiException {
		String uploadPath = DropboxConstants.DB_PUBLIC_FOLDER
				+ DropboxConstants.DB_SEPARATOR + f.getName();
		return createPublicLink(uploadFile(uploadPath, f));
	}

	@Override
	public URI createPublicLink(String path) throws CSPApiException {
		try {
			String res = client.createShareableUrl(path);
			return URI.create(res.toString().replace("dl=0", "dl=1"));
		} catch (DbxException e) {
			throw new CSPApiException("Could not create public link.");
		}
	}

	@Override
	public void inviteUser(String shareName, String userIdentifier)
			throws CSPException {
		DesktopApi.browse(getShareConfigurationURL(shareName, false));
	}

	public void inviteUser(String shareName) throws CSPException {
		this.inviteUser(shareName, "");
	}

	@Override
	public void removeUser(String shareName, String userIdentifier)
			throws CSPException {
		DesktopApi.browse(getShareConfigurationURL(shareName, true));
	}

	public void removeUser(String shareName) throws CSPException {
		this.removeUser(shareName, "");
	}

	public URI getShareConfigurationURL(String shareName,
			boolean hasParticipants) throws CSPException {
		try {
			if (hasParticipants) {
				return new URI(DropboxConstants.DB_MODE,
						DropboxConstants.DB_URL,
						DropboxConstants.DB_SHARE_PREFIX + shareName,
						DropboxConstants.DB_PARAM_SHAREOPTIONS, null);
			} else {
				return new URI(DropboxConstants.DB_MODE,
						DropboxConstants.DB_URL,
						DropboxConstants.DB_SHARE_PREFIX + shareName,
						DropboxConstants.DB_PARAM_SHARE, null);
			}
		} catch (URISyntaxException e) {
			throw new CSPException(
					"Share configuration URL could not be generated.");
		}
	}

	/**
	 * Authenticates the client with the users dropbox and returns the access
	 * token which can then be used in the future.
	 *
	 * @return The access token for the user
	 * @throws DropboxIntegrationException
	 */
	public static String authenticate() throws DropboxIntegrationException {
		DbxAppInfo appInfo = new DbxAppInfo(DropboxConstants.APP_KEY,
				DropboxConstants.APP_SECRET);
		DbxRequestConfig dbxRConfig = new DbxRequestConfig("panbox.org",
				Settings.getInstance().getLocale().toString());
		DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(dbxRConfig,
				appInfo);

		// open browser - user has to allow the app and then copy the access
		// token
		String authorizeUrl = webAuth.start();
		DbxAuthFinish authFinish = null;
		try {
			JOptionPane.showMessageDialog(null, bundle
					.getString("DropboxApiIntegration.copyAccessTokenMessage"),
					bundle.getString("DropboxAPIIntegration.allowAccess"),
					JOptionPane.INFORMATION_MESSAGE);

			DesktopApi.browse(new URI(authorizeUrl));
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// do nothing
			}
			String code = JOptionPane.showInputDialog(null,
					bundle.getString("DropboxAPIIntegration.enterAccessToken"))
					.trim();
			// String code = new BufferedReader(new
			// InputStreamReader(System.in))
			// .readLine().trim();
			authFinish = webAuth.finish(code);
		} catch (URISyntaxException | DbxException e) {
			throw new DropboxIntegrationException(e);
		}

		if (instance != null) {
			// set new client in instance
			instance.client = new DbxClient(dbxRConfig, authFinish.accessToken);
		}
		// and return the access token
		logger.info("Received Dropbox authentication token "
				+ authFinish.accessToken);
		return authFinish.accessToken;
	}

	@Override
	public Date getServerTime() throws CSPException {
		return getServerTimeViaFile();
	}

	public Date getServerTimeViaHeader() throws DbxException, IOException,
			ParseException {
		URL url = new URL("https://api.dropbox.com");
		URLConnection conn = url.openConnection();
		String sDate = conn.getHeaderField("date");
		Date date = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z")
				.parse(sDate);
		return date;
	}

	public Date getServerTimeViaFile() throws CSPException {
		return getServerTimeViaFile("");
	}

	private Date getServerTimeViaFile(String path) throws CSPException {
		try {
			String filename = path + "/" + UUID.randomUUID().toString();
			File tmp = File.createTempFile("panbox-tmp",
					String.valueOf(System.currentTimeMillis()));
			// FileWriter out = new FileWriter(tmp);
			// out.write('1');
			// out.close();
			client.uploadFile(filename, DbxWriteMode.force(), tmp.length(),
					new FileInputStream(tmp));
			Date date = getLastModificationDate(filename);
			client.delete(filename);
			tmp.delete();
			return date;
		} catch (IOException e) {
			throw new CSPIOException(e);
		} catch (DbxException e) {
			throw new CSPApiException(e);
		}
	}

	@Override
	public Date getLastModificationDate(String path)
			throws CSPFileNotFoundException, CSPApiException {
		try {
			DbxEntry entry = client.getMetadata(path);
			if (entry == null) {
				throw new FileNotFoundException("File at location " + path
						+ " could not be found!");
			} else {
				return entry.asFile().lastModified;
			}
		} catch (DbxException e) {
			throw new CSPApiException(e);
		} catch (FileNotFoundException e) {
			throw new CSPFileNotFoundException(e);
		}
	}

	@Override
	public synchronized boolean createLock(String path) throws CSPIOException,
			CSPApiException {
		String filename = path + DropboxConstants.LOCK_SUFFIX;
		File tmp = null;
		try {
			tmp = File.createTempFile(UUID.randomUUID().toString(), "");
		} catch (IOException e) {
			throw new CSPIOException(e);
		}
		if (!isLocked(path)) {
			try {
				client.uploadFile(filename, DbxWriteMode.force(), tmp.length(),
						new FileInputStream(tmp));
				return true;
			} catch (DbxException | IOException e) {
				try {
					client.delete(filename);
					if (e instanceof DbxException) {
						throw new CSPApiException(e);
					} else if (e instanceof IOException) {
						throw new CSPIOException(e);
					}
				} catch (DbxException e1) {
					logger.error("Could not remove lock file after failure!", e);
					throw new CSPApiException(e1);
				}
			}
		}
		return false;
	}

	@Override
	public synchronized boolean createTemporaryLock(String path)
			throws CSPIOException, CSPApiException {
		String filename = path + DropboxConstants.TEMP_LOCK_SUFFIX;
		File tmp = null;
		try {
			tmp = File.createTempFile(UUID.randomUUID().toString(), "");
		} catch (IOException e) {
			throw new CSPIOException(e);
		}
		if (!isLocked(path)) {
			try {
				client.uploadFile(filename, DbxWriteMode.force(), tmp.length(),
						new FileInputStream(tmp));
				return true;
			} catch (DbxException | IOException e) {
				try {
					client.delete(filename);
					if (e instanceof DbxException) {
						throw new CSPApiException(e);
					} else if (e instanceof IOException) {
						throw new CSPIOException(e);
					}
				} catch (DbxException e1) {
					logger.error("Could not remove lock file after failure!", e);
					throw new CSPApiException(e1);
				}
			}
		}
		return false;
	}

	@Override
	public synchronized boolean isLocked(String path) throws CSPApiException {
		try {
			DbxEntry lockFile = client.getMetadata(path
					+ DropboxConstants.LOCK_SUFFIX);
			DbxEntry tempLockFile = client.getMetadata(path
					+ DropboxConstants.TEMP_LOCK_SUFFIX);
			if (lockFile != null) {
				return true;
			} else if (tempLockFile != null) {
				if (new Date().compareTo(tempLockFile.asFile().lastModified) < DropboxConstants.TEMP_LOCK_DURATION) {
					return true;
				} else {
					releaseTemporaryLock(path);
					return false;
				}
			} else {
				return false;
			}
		} catch (DbxException e) {
			throw new CSPApiException(e);
		}
	}

	@Override
	public synchronized void releaseLock(String path) throws CSPApiException {
		try {
			client.delete(path + DropboxConstants.LOCK_SUFFIX);
		} catch (DbxException e) {
			throw new CSPApiException(e);
		}
	}

	@Override
	public synchronized void releaseTemporaryLock(String path)
			throws CSPApiException {
		try {
			client.delete(path + DropboxConstants.TEMP_LOCK_SUFFIX);
		} catch (DbxException e) {
			throw new CSPApiException(e);
		}
	}

	@Override
	public synchronized boolean exists(String path) throws CSPApiException {
		try {
			return (client.getMetadata(path) != null);
		} catch (DbxException e) {
			throw new CSPApiException(e);
		}
	}

	@Override
	public List<Revision> getRevisions(String path) throws CSPApiException {
		try {
			List<DbxEntry.File> revs = client.getRevisions(path);
			List<Revision> out = new ArrayList<>();
			for (DbxEntry.File f : revs) {
				Revision rev = new Revision(f.rev, f.lastModified, f.numBytes);
				out.add(rev);
			}
			return out;

		} catch (DbxException e) {
			throw new CSPApiException(e);
		}
	}

	@Override
	public boolean restoreToRevision(String path, String rev) {
		try {
			return client.restoreFile(path, rev) != null;
		} catch (DbxException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean supportsMetadataFreshness() {
		// FIXME: only if freshness checks with server-based timestamps are
		// working
		return true;
	}

	@Override
	public boolean isOnline() {
		try {
			DbxAccountInfo accountInfo = client.getAccountInfo();
			if (accountInfo != null) {
				return true;
			} else {
				return false;
			}
		} catch (DbxException e) {
			// first, check if second try succeeds
			String ACCESS_TOKEN = Settings.getInstance()
					.getDropboxAccessToken();
			client = new DbxClient(dbxRConfig, ACCESS_TOKEN);
			try {
				DbxAccountInfo accountInfo = client.getAccountInfo();
				if (accountInfo != null) {
					return true;
				} else {
					return false;
				}
			} catch (DbxException e2) {
				return false;
			}
		}
	}

	@Override
	public void deleteFile(String serverPath) throws CSPApiException {
		try {
			client.delete(serverPath);
		} catch (DbxException e) {
			throw new CSPApiException(e);
		}
	}

	@Override
	public void downloadFile(String remotePath, String targetPath)
			throws CSPException {
		try {
			if (client.getMetadata(remotePath).isFile()) {
				File targetFile = new File(targetPath);
				if (!targetFile.getParentFile().exists()) {
					targetFile.getParentFile().mkdirs();
				}
				OutputStream o = new FileOutputStream(targetFile);
				client.getFile(remotePath, "", o);
				o.flush();
				o.close();
			}
		} catch (FileNotFoundException e) {
			throw new CSPFileNotFoundException(e);
		} catch (DbxException e) {
			throw new CSPApiException(e);
		} catch (IOException e) {
			throw new CSPIOException(e);
		}
	}

	@Override
	public void downloadFolder(String remotePath, String targetPath)
			throws CSPException {
		downloadFolder(remotePath, targetPath, 0);
	}

	public void downloadFolder(String remotePath, String targetPath, int depth)
			throws CSPException {
		try {
			DbxEntry folder = client.getMetadata(remotePath);
			if (folder.isFolder()) {
				File remoteFolder = new File(remotePath);
				File targetFolder = new File(targetPath + File.separator
						+ remoteFolder.getName());
				if (!targetFolder.exists()) {
					targetFolder.mkdirs();
				}
				DbxEntry.WithChildren folderWithChildren = client
						.getMetadataWithChildren(remotePath);
				for (DbxEntry file : folderWithChildren.children) {
					if (depth >= DropboxConstants.MAX_TREE_SEARCH_DEPTH) {
						break;
					}
					if (file.isFile()) {
						downloadFile(file.path, targetPath + "/" + folder.name
								+ "/" + file.name);
					} else if (file.isFolder()) {
						downloadFolder(file.path, targetPath + "/"
								+ folder.name, depth + 1);
					}
				}
			}
		} catch (DbxException e) {
			e.printStackTrace();
		}
	}

}
