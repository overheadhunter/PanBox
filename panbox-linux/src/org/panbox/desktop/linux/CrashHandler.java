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

import java.io.BufferedReader;
import java.util.ArrayList;

import org.panbox.Settings;
import org.panbox.desktop.linux.dbus.DBusService;

/**
 * @author Dominik Spychalski
 * 
 *         Panbox CrashHandler, deliver functions which try to restore a save
 *         Panbox-State at application startup after a crash
 */

public class CrashHandler {

	private CrashHandler() {
	}

	private static CrashHandler instance;

	public static CrashHandler getInstance() {
		if (instance == null) {
			instance = new CrashHandler();
		}
		return instance;
	}

	// Tries to unmount panbox directory, returns true if directory not mounted
	public boolean umountPanbox() {
		boolean ret = false;

		if (CrashHandler.getInstance().panboxMounted()) {
			ret = CrashHandler.getInstance().fusermount();

			if (!ret) {
				ret = CrashHandler.getInstance().umount_f();
			}

			if (!ret) {
				ret = CrashHandler.getInstance().umount_l();
			}
		} else {
			ret = true;
		}

		return ret;
	}

	// checks and handles running panboy components (java and python scripts).
	// returns true if another panbox instance is currently running
	public boolean checkPanboxProcessRunning() {
		boolean ret = false;
		String executable = "";
		final String panboxTrayScript = "panbox_tray_icon.py";

		
		EnvironmentHandler.RE_TYPE re_type = EnvironmentHandler.getInstance().getEnvironmentType();
		if(re_type == EnvironmentHandler.RE_TYPE.IDE){
			// running in eclipse, not live, characteristic string to find
			// process
			executable = "Djava.library.path=/usr/lib/jni";
		}
		else if(re_type == EnvironmentHandler.RE_TYPE.SYSTEM){
			executable = EnvironmentHandler.getInstance().getExecutable();
		}

		boolean panboxJavaRunning = ProcessHandler.getInstance()
				.checkProcessRunning(executable);
		boolean panboxTrayRunning = ProcessHandler.getInstance()
				.checkProcessRunning(panboxTrayScript);

		if (panboxTrayRunning) {
			ArrayList<Integer> panboxJavaPIDs = ProcessHandler.getInstance()
					.getProcessID(executable);

			ArrayList<Integer> panboxTrayPIDs = ProcessHandler.getInstance()
					.getProcessID(panboxTrayScript);

			if (panboxJavaRunning) {
				if (panboxTrayPIDs.size() > 1) {
					for (int panboxTrayPID : panboxTrayPIDs) {
						int ppid = ProcessHandler.getInstance().getProcessPID(
								panboxTrayPID);
						if (ppid != panboxJavaPIDs.get(0)) {
							ProcessHandler.getInstance().killProcess(
									panboxTrayPID);
						}
					}
				}
			} else {
				for (int panboxTrayPID : panboxTrayPIDs) {
					ProcessHandler.getInstance().killProcess(panboxTrayPID);
				}
			}
		}

		if (panboxJavaRunning) {
			ret = true;
		} else {
			if (DBusService.getInstance().isRunning()) {
				boolean release = DBusService.getInstance().releaseOrphanDBus();

				if (!release) {
					System.out.println("releasing DBUS '"
							+ DBusService.getInstance().getDBusName()
							+ "' not possible...");
				}
			}
			ret = false;
		}

		return ret;
	}

	// checks if panbox directory is currently mounted. Returns true if mounted,
	// else false
	public boolean panboxMounted() {
		boolean ret = false;
		String outputLine = "";

		final String fstype = "fuse.panboxvirtualfilesystem";

		String[] args = new String[] { "mount", "-l" };
		BufferedReader input = ProcessHandler.getInstance()
				.executeProcess(args);
		try {
			while ((outputLine = input.readLine()) != null) {
				if (outputLine.toLowerCase().contains(fstype)) {
					ret = true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ret;
	}

	// tries to unmount the panbox directory with a fusermount -u terminal
	// command
	public boolean fusermount() {
		boolean ret = false;

		String mntPntDir = Settings.getInstance().getMountDir();
		String[] args = new String[] { "fusermount", "-u", mntPntDir };
		ProcessHandler.getInstance().executeProcess(args);

		ProcessHandler.getInstance().sleep();

		if (!this.panboxMounted()) {
			ret = true;
		}
		return ret;
	}

	// trues to unmount the panbox directory with a umount -f terminal command
	public boolean umount_f() {
		boolean ret = false;

		String mntPntDir = Settings.getInstance().getMountDir();
		String[] args = new String[] { "umount", "-f", mntPntDir };
		ProcessHandler.getInstance().executeProcess(args);

		ProcessHandler.getInstance().sleep();

		if (!this.panboxMounted()) {
			ret = true;
		}

		return ret;
	}

	// tries to unmount the panbox directory with a umount -l terminal command
	public boolean umount_l() {
		boolean ret = false;

		String mntPntDir = Settings.getInstance().getMountDir();
		String[] args = new String[] { "umount", "-l", mntPntDir };
		ProcessHandler.getInstance().executeProcess(args);

		ProcessHandler.getInstance().sleep();

		if (!this.panboxMounted()) {
			ret = true;
		}

		return ret;
	}

}
