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
package org.panbox.desktop.linux;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.log4j.Logger;
import org.panbox.Settings;
import org.panbox.desktop.common.vfs.FuseUserFS;
import org.panbox.desktop.common.vfs.MountFailureWarningMessage;
import org.panbox.desktop.common.vfs.PanboxFS;
import org.panbox.desktop.common.vfs.PanboxFSLinux;

/**
 * @author palige
 * 
 *         Controller class for managing startup/shutdown/remounting the virtual
 *         filesystem.
 */
public class VFSControl {

	private static final Logger logger = Logger.getLogger(VFSControl.class);

	private final PanboxFSLinux loop;

	// private final static PanboxFSLinux loop = new PanboxFSLinux(new
	// JnetFS());
	private boolean mounted = false;

	private VFSControl(final String[] vfsoptions) {
		this.loop = new PanboxFSLinux(new FuseUserFS(vfsoptions));
	}

	private static VFSControl instance;

	public synchronized static VFSControl getInstance(final String[] vfsoptions) {
		return (instance == null) ? (instance = new VFSControl(vfsoptions))
				: instance;
	}

	public synchronized static VFSControl getInstance() {
		return getInstance(null);
	}

	public PanboxFS getVFS() {
		return loop;
	}

	/**
	 * helper method to check if mountpoint stored in Settings is valid
	 * 
	 * @return <code>true</code> if VFS can be mounted at given mountpoint
	 */
	protected boolean checkMountpoint() {
		// Test if backend dir is not empty and a directory
		File mountPointDir = new File(Settings.getInstance().getMountDir());
		if (!mountPointDir.exists()) {
			logger.fatal("non-existing mountpoint "
					+ mountPointDir.getAbsolutePath());
			return false;
		} else if (!mountPointDir.isDirectory()) {
			logger.fatal("Not a directory: " + mountPointDir.getAbsolutePath());
			return false;
		} else if (mountPointDir.isDirectory()
				&& mountPointDir.list().length > 0) {
			logger.fatal("Mountpoint not empty: "
					+ mountPointDir.getAbsolutePath());
			return false;
		}

		return true;
	}

	protected File getMountpoint() {
		return isMounted() ? new File(Settings.getInstance().getMountDir())
				: null;
	}

	public boolean mount() {
		if (!isMounted()) {
			if (checkMountpoint()) {
				// NOTE: As we mount the panbox drive via a non-blocking call,
				// we are unable to *directly* determine if the mount call
				// succeeds.
				loop.mount(new File(Settings.getInstance().getMountDir()),
						false, new MountFailureWarningMessage());
				setMounted(true);
				logger.info("mount: Successfully mounted Panbox VFS to "
						+ Settings.getInstance().getMountDir());

				return true;
			}
		} else {
			logger.error("mount: Panbox VFS is already mounted at "
					+ Settings.getInstance().getMountDir());

		}
		return false;
	}

	public boolean isUmountSafe() {
		return !loop.openFileAccessSessions();
	}

	public boolean unmount() {
		if (isMounted()) {
			loop.unmount();
			setMounted(false);
			logger.info("Sucessfully unmounted " + loop.getMountpoint());
			return true;
		} else {
			logger.error("unmount: Panbox VFS currently not mounted");
			return false;
		}
	}

	public boolean isMounted() {
		return mounted;
	}

	public void remount() throws FileNotFoundException, InterruptedException {
		unmount();
		mount();
	}

	public void setMounted(boolean mounted) {
		this.mounted = mounted;
	}

}
