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

import java.io.File;
import java.io.IOException;

public class SingleInstanceLock {

	private static final String LOCK_FILEPATH = System
			.getProperty("java.io.tmpdir") + File.separator + "panbox.lock";
	private static final File lock = new File(LOCK_FILEPATH);
	private static boolean locked = false;

	private SingleInstanceLock() {
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

		if (lock.exists())
			return false;

		lock.createNewFile();
		lock.deleteOnExit();
		locked = true;
		return true;
	}
	
	public static void forceUnlock() {
		lock.delete();
		locked = false;
	}
	

}