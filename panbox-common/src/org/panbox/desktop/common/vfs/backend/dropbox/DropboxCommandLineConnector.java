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
package org.panbox.desktop.common.vfs.backend.dropbox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by nolle on 03.12.13.
 */

public class DropboxCommandLineConnector {

	public static DropboxConstants getFileStatus(String path) {
		Process p = null;
		try {
			p = Runtime.getRuntime().exec(
					new String[] { DropboxConstants.DB_CMD,
							DropboxConstants.DB_CMD_FILESTATUS, path });
			p.waitFor();

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			String line;
			if ((line = reader.readLine()) != null) {
				return DropboxConstants.fromString(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String getPubUrl(String path) {
		Process p = null;
		String completeMsg = "";
		try {
			p = Runtime.getRuntime().exec(
					new String[] { DropboxConstants.DB_CMD,
							DropboxConstants.DB_CMD_PUBURL, path });
			p.waitFor();

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				completeMsg += line + "\t";
				if (line.startsWith(DropboxConstants.DB_CONTENT_URL)) {
					return line;
				}
				if (line.startsWith("Couldn't get public url")) {
					throw new RuntimeException(
							"getPubUrl() works just with files in the publicfolder of Dropbox! Output:\t"
									+ completeMsg);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		throw new RuntimeException("ERROR: " + completeMsg);
	}
}
