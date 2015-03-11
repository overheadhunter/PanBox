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
package org.panbox.core.pairing.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Map;

import org.panbox.core.pairing.PAKCorePairingHandler;

import ezvcard.VCard;

/**
 *
 * @author Clemens A. Schulz <c.schulz@sirrix.com>
 */
public class PAKNetworkPairingHandler extends PAKCorePairingHandler {

	private final ServerSocketChannel server;
	private Socket socket;

	public PAKNetworkPairingHandler(InetAddress idBAddr, String password,
			String eMail, String firstName, String lastName, String deviceName,
			char[] keyPassword, PrivateKey ownerKeyEnc,
			PrivateKey ownerKeySign, Map<String, X509Certificate> knownDevices,
			Collection<VCard> knownContacts) throws UnknownHostException,
			IOException {
		super(PairingType.MASTER, password, eMail, firstName, lastName,
				deviceName, keyPassword, ownerKeyEnc, ownerKeySign,
				knownDevices, knownContacts);
		server = ServerSocketChannel.open();
		server.socket().bind(new InetSocketAddress(idBAddr, 11112), 5);
	}

	public PAKNetworkPairingHandler(InetAddress idBAddr, String password,
			String eMail, String firstName, String lastName, String deviceName,
			X509Certificate ownerCertEnc, X509Certificate ownerCertSign,
			Map<String, X509Certificate> knownDevices,
			Collection<VCard> knownContacts) throws UnknownHostException,
			IOException {
		super(PairingType.SLAVE, password, eMail, firstName, lastName,
				deviceName, ownerCertEnc, ownerCertSign, knownDevices,
				knownContacts);
		server = ServerSocketChannel.open();
		server.socket().bind(new InetSocketAddress(idBAddr, 11112), 5);
	}

	@Override
	public void acceptConnection() throws Exception {
		SocketChannel channel = server.accept();
		socket = channel.socket();
	}

	@Override
	public void initCommunication() throws Exception {
		dataInputStream = new ObjectInputStream(socket.getInputStream());
		dataOutputStream = new ObjectOutputStream(socket.getOutputStream());
		idA = socket.getRemoteSocketAddress().toString();
		idB = socket.getLocalSocketAddress().toString();
	}

	@Override
	public void closeConnection() throws Exception {
		socket.close();
	}

}
