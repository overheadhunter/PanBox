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

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

import org.panbox.PanboxConstants;
import org.panbox.desktop.common.PanboxClient;

/**
 * @author rw
 * 
 */
public class PanboxHTTPServer implements Runnable {

	final static int PORT = PanboxConstants.PANBOX_DEFAULT_PORT;

	private static Thread instance;
	private ServerSocket socket;

	private static PanboxClient client;

	public static PanboxClient getPanboxClient() {
		return client;
	}

	/**
	 * @throws IOException
	 * @throws UnknownHostException
	 * 
	 */
	private PanboxHTTPServer(PanboxClient client) throws UnknownHostException,
			IOException {
		this.socket = new ServerSocket(PORT, 0, InetAddress.getByName(null));
		PanboxHTTPServer.client = client;
		// registerCustomURIHandler();
	}

	/**
	 * @return the instance
	 * @throws IOException
	 * @throws UnknownHostException
	 */
	public static synchronized Thread getInstance(PanboxClient client)
			throws UnknownHostException, IOException {
		return (instance == null) ? (instance = new Thread(
				new PanboxHTTPServer(client))) : instance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		while (true) {
			try {
				(new Thread(new PanboxHTTPConnectionHandler(socket.accept())))
						.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// private final static String PANBOX_URI_SCHEME = "panbox";
	//
	// private void registerCustomURIHandler() {
	// URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
	// public URLStreamHandler createURLStreamHandler(String protocol) {
	// return PANBOX_URI_SCHEME.equals(protocol) ? new URLStreamHandler() {
	// protected URLConnection openConnection(URL url)
	// throws IOException {
	// try {
	// String cmdstring = url.toString().substring(
	// PANBOX_URI_SCHEME.length() + 1);
	// PanboxURICmd cmd = PanboxURICmd
	// .getCommandHander(cmdstring);
	// cmd.execute();
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// return null;
	// }
	// } : null;
	// }
	// });
	// }
}
