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
package org.panbox.desktop.common.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

public class SingleInstanceLockSocket {
	//
	// private static final String LOCK_FILEPATH = System
	// .getProperty("java.io.tmpdir") + File.separator + "panbox.lock";
	// private static final File lock = new File(LOCK_FILEPATH);

	private static final int PORT_NUMBER = 28765;
	private static boolean locked = false;

	private static ServerSocket socket = null;

	private SingleInstanceLockSocket() {
	}

	/**
	 * Creates the lock file if it's not present and requests its deletion on
	 * program termination or informs that the program is already running if
	 * that's the case.
	 * 
	 * @return true - if the operation was successful or if the program already
	 *         has the lock.<br>
	 *         false - if the program is already running
	 * @throws IOException
	 *             if the lock file cannot be created.
	 */
	public static boolean lock() throws IOException {
		if (locked)
			return true;

		if (!canOpenSocket())
			return false;

		lockPortNumber();
		locked = true;
		return true;
	}

	private static boolean canOpenSocket() {
		try {
			ServerSocket socket = new ServerSocket(PORT_NUMBER, 0, InetAddress.getLocalHost());
			// Socket port is good now!
			socket.close();
			return true;
		} catch (IOException e) {
			// Socket port is already used by other instance!
			return false;
		}
	}

	private static void lockPortNumber() {
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					try {
						socket = new ServerSocket(PORT_NUMBER, 0, InetAddress.getLocalHost());
						socket.accept();
						socket.close();
					} catch (IOException e) {
						// we already checked for a running instance here...
					}
				}
			}
		});
		t.start();
	}
}