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
package org.panbox.desktop.common.urihandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.Charset;

import org.apache.log4j.Logger;

public class PanboxHTTPConnectionHandler implements Runnable {

	static final Logger logger = Logger
			.getLogger(PanboxHTTPConnectionHandler.class);

	private String cmdstring;

	public PanboxHTTPConnectionHandler(Socket s) throws IOException {

		if (s != null) {
			BufferedReader r = new BufferedReader(new InputStreamReader(
					s.getInputStream()));
			String arg = r.readLine();
			if (arg != null) {
				String[] parts = arg.split("\\s+");
				if ((parts == null) || (parts.length != 3)
						|| (!parts[0].equals("GET"))
						|| (!parts[2].startsWith("HTTP"))) {
					logger.error("Received invalid request " + arg);
					s.getOutputStream().write(
							("Received invalid request " + arg)
									.getBytes(Charset.forName("US-ASCII")));
				} else {
					// remove trailing slash
					this.cmdstring = parts[1].substring(1).trim();
					logger.info("Received valid HTTP request with argument "
							+ cmdstring);
					s.getOutputStream().write(
							("Received valid HTTP request with argument "
									+ cmdstring + "\r\nStarting execution...")
									.getBytes(Charset.forName("US-ASCII")));
				}
			}
			s.close();
		}
	}

	@Override
	public void run() {
		if (this.cmdstring != null) {
			try {
				PanboxURICmd cmd = PanboxURICmd.getCommandHander(cmdstring);
				logger.info("Found command " + cmd.getName()
						+ ", starting execution...");
				cmd.execute();
				logger.info("Execution of Command " + cmd.getName()
						+ " finished.");
			} catch (Exception e) {
				logger.error("Command execution failed.", e);
			}
		}
	}
}
