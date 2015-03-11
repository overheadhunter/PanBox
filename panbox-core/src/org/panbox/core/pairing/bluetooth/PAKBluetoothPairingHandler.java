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
package org.panbox.core.pairing.bluetooth;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Map;

import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

import org.panbox.core.pairing.PAKCorePairingHandler;

import ezvcard.VCard;

/**
 *
 * @author trusteddisk
 */
public class PAKBluetoothPairingHandler extends PAKCorePairingHandler {

	private final StreamConnectionNotifier streamConnNotifier;
	private StreamConnection connection;

	public PAKBluetoothPairingHandler(String password, String eMail,
			String firstName, String lastName, String deviceName,
			X509Certificate ownerCertEnc, X509Certificate ownerCertSign,
			Map<String, X509Certificate> knownDevices,
			Collection<VCard> knownContacts) throws IOException {
		super(PairingType.SLAVE, password, eMail, firstName, lastName,
				deviceName, ownerCertEnc, ownerCertSign, knownDevices,
				knownContacts);
		UUID uuid = new UUID(0x1101); // TODO: Create new unique UUID
		String connectionString = "btspp://localhost:" + uuid
				+ ";name=PanboxImportListener;encrypt=false;authenticate=false";
		streamConnNotifier = (StreamConnectionNotifier) Connector.open(
				connectionString, Connector.READ_WRITE);
		ServiceRecord record = LocalDevice.getLocalDevice().getRecord(
				streamConnNotifier);
		logger.debug("PAKBluetoothPairingHandler : connection is up at: "
				+ record.getConnectionURL(0, false));
	}

	public PAKBluetoothPairingHandler(String password, String eMail,
			String firstName, String lastName, String deviceName,
			char[] keyPassword, PrivateKey ownerKeyEnc,
			PrivateKey ownerKeySign, Map<String, X509Certificate> knownDevices,
			Collection<VCard> knownContacts) throws IOException {
		super(PairingType.MASTER, password, eMail, firstName, lastName,
				deviceName, keyPassword, ownerKeyEnc, ownerKeySign,
				knownDevices, knownContacts);
		UUID uuid = new UUID(0x1101); // TODO: Create new unique UUID
		String connectionString = "btspp://localhost:" + uuid
				+ ";name=PanboxImportListener;encrypt=false;authenticate=false";
		streamConnNotifier = (StreamConnectionNotifier) Connector.open(
				connectionString, Connector.READ_WRITE);
		ServiceRecord record = LocalDevice.getLocalDevice().getRecord(
				streamConnNotifier);
		logger.debug("PAKBluetoothPairingHandler : connection is up at: "
				+ record.getConnectionURL(0, false));
	}

	@Override
	public void acceptConnection() throws Exception {
		connection = streamConnNotifier.acceptAndOpen();
	}

	@Override
	public void initCommunication() throws Exception {
		this.dataInputStream = new ObjectInputStream(
				connection.openDataInputStream());
		this.dataOutputStream = new ObjectOutputStream(
				connection.openDataOutputStream());
		this.idA = RemoteDevice.getRemoteDevice(connection)
				.getBluetoothAddress();
		this.idB = LocalDevice.getLocalDevice().getBluetoothAddress();
	}

	@Override
	public void closeConnection() throws Exception {
		dataInputStream.close();
		dataOutputStream.close();
		connection.close();
	}
}
