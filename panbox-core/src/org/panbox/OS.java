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

public class OS {

	private static OperatingSystem operatingSystem = null;

	public enum OperatingSystem {

		LINUX32, LINUX64, OSX32, WIN32, ANDROID;

		public boolean isWindows() {
			return equals(WIN32);
		}
		
		public boolean isLinux() {
			return (equals(LINUX32) || equals(LINUX64));
		}
	}

	public static OperatingSystem getOperatingSystem() {
		if (operatingSystem != null) {
			return operatingSystem;
		}
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			operatingSystem = OperatingSystem.WIN32;
		} else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
			operatingSystem = OperatingSystem.OSX32;
		} else if (System.getProperty("os.name").toLowerCase()
				.contains("linux")) {
			if (System.getProperty("java.runtime.name").toLowerCase()
					.contains("android")) {
				operatingSystem = OperatingSystem.ANDROID;
			} else if (System.getProperty("os.arch").contains("64")) {
				operatingSystem = OperatingSystem.LINUX64;
			} else {
				operatingSystem = OperatingSystem.LINUX32;
			}
		}
		return operatingSystem;
	}
}
