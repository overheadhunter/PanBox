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

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author palige
 *
 *         Linux specific utils for Panbox
 */
public class Utils {

	public static int getUid() {
		return getXid("-u");
	}

	public static int getGid() {
		return getXid("-g");
	}

	private static int getXid(String arg) {
		InputStream in = null;
		try {
			String command = "id " + arg;
			Process child = Runtime.getRuntime().exec(command);

			// Get the input stream and read from it
			in = child.getInputStream();
			int c;
			StringBuffer buf = new StringBuffer();
			while ((c = in.read()) != -1) {
				buf.append((char) c);
			}

			Pattern p = Pattern.compile("\\d+");
			Matcher m = p.matcher(buf.toString());
			if (m.find()) {
				// convert & return retval
				return Integer.parseInt(m.group());
			} else {
				return 0;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// do nothing
				}
			}
		}
	}

}
