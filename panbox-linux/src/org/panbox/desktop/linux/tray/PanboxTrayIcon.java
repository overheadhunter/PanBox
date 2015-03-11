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
package org.panbox.desktop.linux.tray;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.freedesktop.dbus.DBusConnection;
import org.panbox.desktop.linux.EnvironmentHandler;
import org.panbox.desktop.linux.dbus.PanboxTrayInterface;

/**
 * Created by Timo Nolle on 17.07.14.
 */
public class PanboxTrayIcon {

	public static final String DBUS = "org.panbox.tray";
	public static final String DBUS_PATH = "/org/panbox/tray";

	private final static Logger logger = Logger.getLogger(PanboxTrayIcon.class);

	public static class TrayIconException extends Exception {

		private static final long serialVersionUID = 8638144075592003924L;

		public TrayIconException(String msg) {
			super(msg);
		}
	}

	private Process p;
	private ProcessBuilder pb;
	private static PanboxTrayIcon instance;
	private final String script_name = "panbox_tray_icon.py";
	private ProcMonitor procmon;

	private final static class ProcMonitor implements Runnable {
		private Process _proc;
		private volatile boolean _complete;

		public ProcMonitor(Process proc) {
			this._proc = proc;
		}

		public boolean isComplete() {
			return _complete;
		}

		public void run() {
			try {
				_proc.waitFor();
				_complete = true;
			} catch (InterruptedException e) {
				// do nothing
			}
		}

		public static ProcMonitor create(Process proc) {
			ProcMonitor procMon = new ProcMonitor(proc);
			Thread t = new Thread(procMon);
			t.start();
			return procMon;
		}
	}

	public PanboxTrayIcon(boolean usegtk) {
		pb = new ProcessBuilder();

		String script_path = "";
		EnvironmentHandler.RE_TYPE re_type = EnvironmentHandler.getInstance()
				.getEnvironmentType();

		if (re_type == EnvironmentHandler.RE_TYPE.IDE) {
			script_path = "src/org/panbox/desktop/linux/gui/tray/"
					+ script_name;
		} else if (re_type == EnvironmentHandler.RE_TYPE.SYSTEM) {
			script_path = EnvironmentHandler.getInstance()
					.getExecutionDirectory().getPath()
					+ File.separator + script_name;
			pb.directory(EnvironmentHandler.getInstance()
					.getExecutionDirectory());
		}

		if (usegtk) {
			pb.command("python2.7", script_path, "--tray-gtk");
		} else {
			pb.command("python2.7", script_path, "--tray-appindicator");
		}
	}

	public static PanboxTrayIcon getInstance(boolean usegtk) {
		if (instance == null) {
			instance = new PanboxTrayIcon(usegtk);
		}
		return instance;
	}

	public void start() throws TrayIconException {
		try {
			p = pb.start();
			this.procmon = ProcMonitor.create(p);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// do nothing
			}
			if (procmon.isComplete()) {
				throw new TrayIconException("Error starting Tray script!");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void stop() {
		if (p != null) {
			p.destroy();
			this.procmon = null;
		}
	}

	public void restart() throws TrayIconException {
		if (isRunning()) {
			stop();
		}
		start();
	}

	public void showNotification(String message) throws TrayIconException {
		try {
			if (isRunning()) {
				DBusConnection sessionBus = DBusConnection
						.getConnection(DBusConnection.SESSION);
				PanboxTrayInterface p = sessionBus.getRemoteObject(DBUS,
						DBUS_PATH, PanboxTrayInterface.class);
				p.show_notification(message);
				sessionBus.disconnect();
			} else {
				throw new TrayIconException("Tray script ot running!");
			}
		} catch (Exception e) {
			logger.error("Error displaying notification with message \""
					+ message + "\"", e);
		}
	}

	public boolean isRunning() {
		return (this.procmon != null) ? !procmon.isComplete() : false;
	}
}
