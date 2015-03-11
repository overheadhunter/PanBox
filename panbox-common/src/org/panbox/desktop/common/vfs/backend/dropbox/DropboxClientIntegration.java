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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.panbox.OS;
import org.panbox.Settings;
import org.panbox.WinRegistry;
import org.panbox.core.csp.ICSPClientIntegration;

import javax.swing.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * @author palige
 * 
 */
public class DropboxClientIntegration implements ICSPClientIntegration {

	private final static Logger logger = Logger
			.getLogger(DropboxClientIntegration.class);

	private static final ResourceBundle bundle = ResourceBundle.getBundle(
			"org/panbox/desktop/common/gui/Messages", Settings.getInstance()
					.getLocale());

	@Override
	public int getFileStatus(File f) throws Exception {
		int res;
		DropboxConstants status = DropboxCommandLineConnector.getFileStatus(f
				.getAbsolutePath());
		switch (status) {
		case DB_FILESTATUS_SYNCING:
			res = FILE_STATUS_SYNCHRONIZING;
			break;
		case DB_FILESTATUS_UNSYNCABLE:
			res = FILE_STATUS_SYNC_ERROR;
			break;
		case DB_FILESTATUS_UNWATCHED:
			res = FILE_STATUS_NOSYNC;
			break;
		case DB_FILESTATUS_UPTODATE:
			res = FILE_STATUS_SYNCHRONZED;
		default:
			res = FILE_STATUS_UNKNOWN;
			break;
		}
		return res;
	}

	/**
	 * helper method for obtaining the current local dropbox directory being
	 * synchronized by the dropbox client application.
	 * 
	 * TODO: move method to CSP integration package/corresponding subproject
	 * 
	 * @return {@link File}-instance pointing to the current default dropbox
	 *         synchronization diretory
	 * @throws IOException
	 */
	public File readSyncDirFromMetadata() throws IOException {

		File dropboxConfigDir = getClientConfigDir();
		if ((dropboxConfigDir != null) && dropboxConfigDir.exists()
				&& dropboxConfigDir.isDirectory() && dropboxConfigDir.canRead()) {
			File hostdb = new File(dropboxConfigDir,
					DropboxConstants.DROPBOX_HOST_DB);
			if (hostdb.exists() && hostdb.canRead()) {
				BufferedReader reader = new BufferedReader(new FileReader(
						hostdb));
				// just skip first line
				reader.readLine();
				// read second line with sync dir
				String dropboxhome_enc = reader.readLine();
				reader.close();
				if (dropboxhome_enc != null) {
					if (!dropboxhome_enc.isEmpty()) {
						String dropboxhome_dec = new String(
								Base64.decodeBase64(dropboxhome_enc));
						logger.info("getDropboxSyncDir: Sync directory resolved to: "
								+ dropboxhome_dec);
						File dropboxSyncDir = new File(dropboxhome_dec);
						if (dropboxSyncDir.exists()) {
							return dropboxSyncDir;
						} else {
							logger.error("getDropboxSyncDir: Resolved sync directory does not exist!");
						}
					}
				}
			} else {
				logger.error("getDropboxSyncDir: Couldn't read dropbox host.db file!");
			}
		} else {
			if (dropboxConfigDir != null) {
				logger.error("getDropboxSyncDir: Couldn't access dropbox config directory: "
						+ dropboxConfigDir.getAbsolutePath());
			} else {
				logger.error("getDropboxSyncDir: Couldn't access dropbox config directory");
			}
		}
		return null;
	}

	@Override
	public List<File> getClientSyncDirs() throws Exception {
		ArrayList<File> res = new ArrayList<File>();
		res.add(getCurrentSyncDir());
		return res;
	}

	@Override
	public File getCurrentSyncDir() throws IOException {
		Settings settings = Settings.getInstance();
		File f = new File(settings.getDropboxSynchronizationDir());
		if (!f.exists() || !f.isDirectory()) {
			// FIXME: this should be handled in a dedicated pane for
			// CSP-specific settings
			int res = JOptionPane.showConfirmDialog(null, bundle
					.getString("DropboxClientIntegration.configureDropboxDir"),
					bundle.getString("DropboxClientIntegration.syncDirLookup"),
					JOptionPane.YES_NO_OPTION);
			if (res == JOptionPane.YES_OPTION) {
				f = readSyncDirFromMetadata();
				JFileChooser chooser = new JFileChooser();
				chooser.setDialogTitle(bundle
						.getString("DropboxClientIntegration.syncDir"));
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				chooser.setSelectedFile(f);
				int ret = chooser.showOpenDialog(null);
				if (ret == JFileChooser.APPROVE_OPTION) {
					f = chooser.getSelectedFile();
					if (f.exists() && f.isDirectory()) {
						settings.setDropboxSynchronizationDir(f
								.getAbsolutePath());
						settings.flush();
						return f;
					}
				}
			}
		} else {
			return f;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.panbox.core.csp.ICSPClientIntegration#isClientInstalled()
	 */
	@Override
	public boolean isClientInstalled() throws IOException {
		if (OS.getOperatingSystem().isLinux()) {
			String user_home = System.getProperty("user.home");
			String testpath = null;
			for (int i = 0; i < DropboxConstants.LINUX_DB_DEAMON_PATH.length; i++) {
				if (DropboxConstants.LINUX_DB_DEAMON_PATH[i].startsWith("~")) {
					testpath = user_home
							+ File.separator
							+ DropboxConstants.LINUX_DB_DEAMON_PATH[i]
									.substring(1);
				} else {
					testpath = DropboxConstants.LINUX_DB_DEAMON_PATH[i];
				}
				if ((new File(testpath)).exists())
					return true;
			}
		} else if (OS.getOperatingSystem().isWindows()) {
			try {
				return (new File(getClientConfigDir() + File.separator
						+ DropboxConstants.WINDOWS_DB_BIN_PATH)).exists();
			} catch (NullPointerException ex) {
				return false;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.panbox.core.csp.ICSPClientIntegration#getClientConfigDir()
	 */
	@Override
	public File getClientConfigDir() throws IOException {
		if (OS.getOperatingSystem().isLinux()) {
			return new File(System.getProperty("user.home") + File.separator
					+ ".dropbox");
		} else if (OS.getOperatingSystem().isWindows()) {
			try {
				String installPath = WinRegistry.readString(
						WinRegistry.HKEY_CURRENT_USER, "Software\\Dropbox",
						"InstallPath");
				return new File(installPath + File.separator + "..");
			} catch (IllegalArgumentException | IllegalAccessException
					| InvocationTargetException e) {
				logger.error("Error reading installation path from registry!",
						e);
				throw new IOException(e);
			}
		}
		return null;
	}

	@Override
	public boolean isClientRunning() {
		if (OS.getOperatingSystem().isLinux()) {
			try {
				String pid = FileUtils.readFileToString(new File(
						getClientConfigDir(), DropboxConstants.LINUX_PID_FILE));
				if (pid != null) {
					pid = pid.trim();
				}
				String ret = FileUtils.readFileToString(new File(File.separator
						+ "proc" + File.separator + pid + File.separator
						+ "cmdline"));
				return ret.contains("dropbox");
			} catch (IOException e) {
				return false;
			}
		} else if (OS.getOperatingSystem().isWindows()) {
			try {
				ProcessBuilder pb = new ProcessBuilder("wmic", "process",
						"list");
				pb.redirectErrorStream(true);
				Process p = pb.start();
				p.getOutputStream().close();
				BufferedReader br = new BufferedReader(new InputStreamReader(
						p.getInputStream()));
				String tmp = null;

				while ((tmp = br.readLine()) != null) {
					//System.out.println(tmp);
					if (tmp.toLowerCase().contains("dropbox.exe")) {
						return true;
					}
				}
				p.waitFor();
			} catch (IOException | InterruptedException e) {
				logger.error("Failed to execute check for running Dropbox.exe: ", e);
			}
			return false;
		}
		return true;
	}
}
