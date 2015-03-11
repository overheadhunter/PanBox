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
package org.panbox;

import org.apache.log4j.Logger;
import org.panbox.core.pairing.PAKCorePairingHandler.PairingType;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class Settings {

	private static final Logger logger = Logger.getLogger("org.panbox");

	private static Settings instance = null;

	private String language;
	private String panboxMountDir;
	private String panboxConfDir;
	private String deviceName;
	private boolean expertMode;
	private InetAddress pairingAddress;
	private NetworkInterface pairingInterface;
	private PairingType pairingType;

	private String dropboxAccessToken;
	private String dropboxSynchronizationDir;

	private boolean uriHandlerSupported;
	private boolean mailtoSchemeSupported;
	private boolean clipboardHandlerSupported;

	private final Preferences prefs;
	private final static String PANBOX_DEFAULT_CONF_DIR = System
			.getProperty("user.home") + File.separator + ".panbox";

	private Settings(Preferences prefs) {
		this.prefs = prefs;
		language = prefs.get("language", Locale.getDefault().toString());

		panboxMountDir = prefs.get("mountDir", System.getProperty("user.home")
				+ File.separator + "panbox");
		if (!dirExists(panboxMountDir) && OS.getOperatingSystem().isLinux()) {
			logger.error("Panbox mount-directory (" + panboxMountDir
					+ ") does not exist!");
			new File(panboxMountDir).mkdir();
		}

		panboxConfDir = prefs.get("confDir", "");

		if (!dirExists(panboxConfDir)) {
			logger.error("Panbox directory for configuration (" + panboxConfDir
					+ ") does not exists, reset to default: "
					+ PANBOX_DEFAULT_CONF_DIR);
			panboxConfDir = PANBOX_DEFAULT_CONF_DIR;
		}

		// Make sure that config directory exists when accessing PANBOX_CONF_DIR
		File pbConf = new File(panboxConfDir);
		if (!pbConf.exists()) {
			pbConf.mkdir();
			prefs.put("confDir", panboxConfDir);
		}

		String hostname = "panbox-device";
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			logger.warn("Unable to read default device name! (UnknownHostException)");
		} catch (Exception e) {
			logger.warn("Unable to read default device name! (RuntimeException)");
		}

		deviceName = prefs.get("deviceName", hostname);
		expertMode = Boolean.valueOf(prefs.get("expertMode", "false"));
		dropboxAccessToken = prefs.get("dropboxAccessToken", "");
		dropboxSynchronizationDir = prefs.get("dropboxSynchronizationDir", "");

		mailtoSchemeSupported = Boolean.valueOf(prefs.get(
				"mailtoSchemeSupported", "true"));
		uriHandlerSupported = Boolean.valueOf(prefs.get("uriHandlerSupported",
				"true"));
		clipboardHandlerSupported = Boolean.valueOf(prefs.get(
				"clipboardHandlerSupported", "true"));

		// Set pairing network interface and address (by default to localhost
		// since it is available on every device)
		String pairingAddressStr = prefs.get("pairingAddress", "127.0.0.1");

		try {
			pairingAddress = InetAddress.getByName(pairingAddressStr);
			pairingInterface = determineInterfaceForInetAddress(pairingAddress);
		} catch (UnknownHostException e) {
			logger.warn("Unable to get interfaces! (UnknownHostException)");
		}

		// Set pairingType at default to SLAVE. We will set it to master when
		// pairing has been finished and was MASTER.
		pairingType = PairingType.valueOf(prefs.get("pairingType",
				PairingType.SLAVE.toString()));
	}

	private Settings() {
		this.prefs = null;
	}

	public static Settings getInstance() {
		if (instance == null) {
			Preferences prefs = null;
			if (OS.getOperatingSystem().isWindows()) {
				prefs = new PreferencesRegistryWrapper();
			} else {
				prefs = Preferences.userNodeForPackage(Settings.class);
			}
			instance = new Settings(prefs);
		}
		return instance;
	}

	private boolean dirExists(String dir) {
		File f = new File(dir);
		return (f.exists() && f.isDirectory());
	}

	public String getLanguage() {
		return language;
	}

	public Locale getLocale() {
		if (!language.equals("system_default")) {
			String[] split = language.split("_");
			return new Locale(split[0], split[1]);
		} else {
			return Locale.getDefault();
		}
	}

	/* LANGUAGE */
	public void setLanguage(String lang) {
		language = lang;
		prefs.put("language", lang);
	}

	public String getMountDir() {
		return panboxMountDir;
	}

	/* MOUNT_DIR */
	public void setMountDir(String dir) {
		if (dirExists(dir)) {
			panboxMountDir = dir;
			prefs.put("mountDir", dir);
		} else {
			logger.warn("[setMountDir] Directory '" + dir + "' does not exists");
		}
	}

	public String getConfDir() {
		return panboxConfDir;
	}

	public void setConfDir(String dir) {
		if (dirExists(dir)) {
			panboxConfDir = dir;
			prefs.put("confDir", dir);
		} else {
			logger.warn("[setConfDir] Directory '" + dir + "' does not exists");
		}
	}

	public String getKeystorePath() {
		return panboxConfDir + File.separator + "keystore.jks";
	}

	public String getIdentityPath() {
		return panboxConfDir + File.separator + "identity.db";
	}

	public String getAdressbook() {
		return panboxConfDir + File.separator + "addressbook.db";
	}

	public String getSharesDBPath() {
		return panboxConfDir + File.separator + "shares.db";
	}

	public String getDevicesDBPath() {
		return panboxConfDir + File.separator + "devices.db";
	}

	public String getDeviceName() {
		return deviceName;
	}

	/* Device name shall not be changed after creation of the identity */
	public void setDeviceName(String panboxDeviceName) {
		this.deviceName = panboxDeviceName;
		prefs.put("deviceName", panboxDeviceName);
	}

	public void setExpertMode(boolean panboxExpertMode) {
		expertMode = panboxExpertMode;
		prefs.put("expertMode", Boolean.toString(panboxExpertMode));
	}

	public boolean getExpertMode() {
		return expertMode;
	}

	public boolean isUriHandlerSupported() {
		return uriHandlerSupported;
	}

	public void setUriHandlerSupported(boolean uriHandlerSupported) {
		this.uriHandlerSupported = uriHandlerSupported;
		prefs.put("uriHandlerSupported",
				Boolean.toString(this.uriHandlerSupported));
	}

	public boolean isMailtoSchemeSupported() {
		return mailtoSchemeSupported;
	}

	public void setMailtoSchemeSupported(boolean mailtoSchemeSupported) {
		this.mailtoSchemeSupported = mailtoSchemeSupported;
		prefs.put("mailtoSchemeSupported",
				Boolean.toString(this.mailtoSchemeSupported));
	}

	public boolean isClipboardHandlerSupported() {
		return clipboardHandlerSupported;
	}

	public void setClipboardHandlerSupported(boolean clipboardHandlerSupported) {
		this.clipboardHandlerSupported = clipboardHandlerSupported;
		prefs.put("clipboardHandlerSupported",
				Boolean.toString(this.clipboardHandlerSupported));
	}

	public String getDropboxAccessToken() {
		return dropboxAccessToken;
	}

	public void setDropboxAccessToken(String dropboxAccessToken) {
		this.dropboxAccessToken = dropboxAccessToken;
		prefs.put("dropboxAccessToken", dropboxAccessToken);
	}

	public String getDropboxSynchronizationDir() {
		return dropboxSynchronizationDir;
	}

	public void setDropboxSynchronizationDir(String dropboxSynchronizationDir) {
		this.dropboxSynchronizationDir = dropboxSynchronizationDir;
		prefs.put("dropboxSynchronizationDir", dropboxSynchronizationDir);
	}

	public InetAddress getPairingAddress() {
		return pairingAddress;
	}

	public NetworkInterface getPairingInterface() {
		return pairingInterface;
	}

	public void setPairingAddress(InetAddress pairingAddress) {
		this.pairingAddress = pairingAddress;
		this.pairingInterface = determineInterfaceForInetAddress(pairingAddress);
		prefs.put("pairingAddress", pairingAddress.getHostAddress());
	}

	public void flush() {
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

	private NetworkInterface determineInterfaceForInetAddress(InetAddress addr) {
		try {
			List<NetworkInterface> nics = Collections.list(NetworkInterface
					.getNetworkInterfaces());
			for (NetworkInterface nic : nics) {
				for (InetAddress a : Collections.list(nic.getInetAddresses())) {
					if (a.equals(addr)) {
						return nic;
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
		return null;
	}

	public PairingType getPairingType() {
		return pairingType;
	}

	public void setPairingType(PairingType pairingType) {
		this.pairingType = pairingType;
		prefs.put("pairingType", pairingType.toString());
	}

	public boolean isSlave() {
		return !isMaster();
	}

	public boolean isMaster() {
		return pairingType == PairingType.MASTER;
	}
}
