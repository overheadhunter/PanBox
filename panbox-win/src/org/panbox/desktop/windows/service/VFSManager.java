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
package org.panbox.desktop.windows.service;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.panbox.WinRegistry;
import org.panbox.desktop.common.vfs.PanboxFS;
import org.panbox.desktop.windows.vfs.DokanUserFS;
import org.panbox.desktop.windows.vfs.PanboxFSWindows;

public class VFSManager {

	private static final Logger logger = Logger.getLogger("org.panbox");

	private static VFSManager instance;

	private PanboxFSWindows vfs;

	private final String mountpoint;

	private VFSManager() throws ConfigurationException,
			IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		mountpoint = WindowsOSIntegration.getPanboxMountPoint();
	}

	public static VFSManager getInstance() throws ConfigurationException,
			IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		if (instance == null)
			instance = new VFSManager();
		return instance;
	}
	
	public PanboxFS getVFS() {
		return vfs;
	}

	public synchronized void startVFS() {
		vfs = new PanboxFSWindows(new DokanUserFS());

		File mountFile = new File(mountpoint + ":\\");
		
		if(mountFile == null || mountFile.exists()) {
			logger.fatal("VFSManager : The configured Mountpoint '" + mountpoint + "' is already in used. Please use the AdminConfigurationUtility to change to mount point.");
			System.exit(1000);
		}
		
		vfs.mount(mountFile, false, null);
		WindowsOSIntegration.registerVFS(mountpoint);
	}

	public synchronized void stopVFS() {
		if (vfs != null) {
			vfs.unmount();

			WindowsOSIntegration.unregisterVFS(mountpoint);

			vfs = null;
		}
	}

	public static synchronized boolean isRunning() {
		if (instance == null) {
			return false;
		} else if (instance.vfs == null) {
			return false;
		} else {
			return true;
		}
	}
	
	public static String getMountPoint() throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		return WinRegistry.readString(
				WinRegistry.HKEY_LOCAL_MACHINE,
				"Software\\Panbox.org\\Panbox", "MountPoint");
	}
}
