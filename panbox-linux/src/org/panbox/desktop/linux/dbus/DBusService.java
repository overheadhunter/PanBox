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
package org.panbox.desktop.linux.dbus;

import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;


/**
 * Created by Timo Nolle on 21.11.13.
 * <p/>
 * Creates a dbus service for panbox methods.
 * <p/>
 * For the communication between the python nautilus extension
 * and the panbox software.
 */
public class DBusService {

	private static DBusService instance = null;
	private final String DBUS = "org.panbox.client";
	private final String DBUS_PATH = "/org/panbox/client";
	private DBusConnection conn = null;

	public static DBusService getInstance() {
		if (instance == null)
			instance = new DBusService();
		return instance;
	}

	public void start() {
		try {

			conn = DBusConnection.getConnection(DBusConnection.SESSION);
			conn.requestBusName(DBUS);
			conn.exportObject(DBUS_PATH, new PanboxInterfaceImpl());
		} catch (DBusException e) {
			e.printStackTrace();
		}
	}

	public void stop() {
		if (conn != null)
			conn.disconnect();
	}

	public boolean isRunning() {
		boolean ret = true;

		if (conn != null) {
			return true;
		}

		try {
			DBusConnection conn = DBusConnection.getConnection(DBusConnection.SESSION);
			conn.requestBusName(DBUS);
			conn.exportObject(DBUS_PATH, new PanboxInterfaceImpl());
			ret = false;
			conn.disconnect();
		} catch (DBusException e) {
			//DBUS-Service already running
			ret = true;
		}
		return ret;
	}

	public boolean releaseOrphanDBus() {

		//String[] command = new String[]{"dbus-cleanup-sockets", this.getDBusName()};
		//ProcessHandler.getInstance().executeProcess(command);

		//return this.isRunning();
		return false;
	}

	public String getDBusName() {
		return this.DBUS;
	}

	public String getDBusPath() {
		return this.DBUS_PATH;
	}

}
