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
package org.panbox.core.pairing;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.panbox.PanboxConstants;
import org.panbox.core.Utils;
import org.panbox.core.crypto.KeyConstants;
import org.panbox.core.devicemgmt.DeviceType;
import org.panbox.core.identitymgmt.AbstractAddressbookManager;

import ezvcard.VCard;
import ezvcard.util.org.apache.commons.codec.binary.Base64;

/**
 *
 * @author Clemens A. Schulz <c.schulz@sirrix.com>
 */
public abstract class PAKCorePairingHandler extends PAKCoreHandlerProtocol {

	public final static int PAIRING_TIMEOUT = 240000;

	private final PairingType pairingType;

	// entries to be send
	private final String eMail;
	private final String firstName;
	private final String lastName;
	private final String deviceName;
	private final PrivateKey ownerKeyEnc;
	private final PrivateKey ownerKeySign;
	private final char[] keyPassword;
	private final X509Certificate ownerCertEnc;
	private final X509Certificate ownerCertSign;

	private final Map<String, X509Certificate> knownDevices;
	private final Collection<VCard> knownContacts;

	// entries to be received
	private DeviceType devType;
	private X509Certificate devCert;

	public enum PairingType {
		MASTER, SLAVE
	}

	// Master pairing constructor
	public PAKCorePairingHandler(PairingType pairingType, String password,
			String eMail, String firstName, String lastName, String deviceName,
			char[] keyPassword, PrivateKey ownerKeyEnc,
			PrivateKey ownerKeySign, Map<String, X509Certificate> knownDevices,
			Collection<VCard> knownContacts) {
		super(password);
		this.pairingType = pairingType;
		if (pairingType == PairingType.SLAVE) {
			throw new IllegalArgumentException(
					"PAKCorePairingHandler : Unable to run SLAVE pairing without certificates.");
		}
		this.eMail = eMail;
		this.firstName = firstName;
		this.lastName = lastName;
		this.deviceName = deviceName;
		this.ownerKeyEnc = ownerKeyEnc;
		this.ownerKeySign = ownerKeySign;
		this.keyPassword = keyPassword;
		this.ownerCertEnc = null;
		this.ownerCertSign = null;
		this.knownContacts = knownContacts;
		this.knownDevices = knownDevices;
	}

	// Slave pairing constructor
	public PAKCorePairingHandler(PairingType pairingType, String password,
			String eMail, String firstName, String lastName, String deviceName,
			X509Certificate ownerCertEnc, X509Certificate ownerCertSign,
			Map<String, X509Certificate> knownDevices,
			Collection<VCard> knownContacts) {
		super(password);
		this.pairingType = pairingType;
		if (pairingType == PairingType.MASTER) {
			throw new IllegalArgumentException(
					"PAKCorePairingHandler : Unable to run MASTER pairing without privatekeys.");
		}
		this.eMail = eMail;
		this.firstName = firstName;
		this.lastName = lastName;
		this.deviceName = deviceName;
		this.ownerCertEnc = ownerCertEnc;
		this.ownerCertSign = ownerCertSign;
		this.keyPassword = null;
		this.ownerKeyEnc = null;
		this.ownerKeySign = null;
		this.knownContacts = knownContacts;
		this.knownDevices = knownDevices;
	}

	// acceptConnection will need to set idA and idB!
	public abstract void acceptConnection() throws Exception;

	// will be called in case pairing finished or some error occurred
	public abstract void closeConnection() throws Exception;

	@Override
	public void runOperation(Cipher cipher, SecretKeySpec spec)
			throws Exception {

		CertificateFactory certificateFactory = CertificateFactory
				.getInstance(KeyConstants.CERTIFICATE_ENCODING);

		String sendbase64;

		switch (pairingType) {
		case MASTER:
			logger.debug("PAKCorePairingHandler : runOperation : Started to handle MASTER pairing");
			cipher.init(Cipher.ENCRYPT_MODE, spec);

			// master transmission
			logger.debug("PAKCorePairingHandler : runOperation : Will now send master/slave information...");

			sendbase64 = Base64
					.encodeBase64String(cipher.doFinal(PairingType.MASTER
							.toString()
							.getBytes(
									Charset.forName(PanboxConstants.STANDARD_CHARSET))));
			dataOutputStream.writeObject(sendbase64);
			dataOutputStream.flush();
			logger.debug("PAKCorePairingHandler : runOperation : Sent: "
					+ sendbase64);

			// email, firstname, lastname, devicename transmission
			logger.debug("PAKCorePairingHandler : runOperation : Will now send personal information...");

			sendbase64 = Base64
					.encodeBase64String(cipher.doFinal(eMail.getBytes(Charset
							.forName(PanboxConstants.STANDARD_CHARSET))));
			dataOutputStream.writeObject(sendbase64);
			dataOutputStream.flush();
			logger.debug("PAKCorePairingHandler : runOperation : Sent: "
					+ sendbase64);

			sendbase64 = Base64
					.encodeBase64String(cipher.doFinal(firstName
							.getBytes(Charset
									.forName(PanboxConstants.STANDARD_CHARSET))));
			dataOutputStream.writeObject(sendbase64);
			dataOutputStream.flush();
			logger.debug("PAKCorePairingHandler : runOperation : Sent: "
					+ sendbase64);

			sendbase64 = Base64
					.encodeBase64String(cipher.doFinal(lastName
							.getBytes(Charset
									.forName(PanboxConstants.STANDARD_CHARSET))));
			dataOutputStream.writeObject(sendbase64);
			dataOutputStream.flush();
			logger.debug("PAKCorePairingHandler : runOperation : Sent: "
					+ sendbase64);

			sendbase64 = Base64
					.encodeBase64String(cipher.doFinal(deviceName
							.getBytes(Charset
									.forName(PanboxConstants.STANDARD_CHARSET))));
			dataOutputStream.writeObject(sendbase64);
			dataOutputStream.flush();
			logger.debug("PAKCorePairingHandler : runOperation : Sent: "
					+ sendbase64);

			// owner privatekeys + password
			logger.debug("PAKCorePairingHandler : runOperation : Will now send owner privatekeys...");

			sendbase64 = Base64.encodeBase64String(cipher.doFinal(Utils
					.toBytes(keyPassword)));
			Arrays.fill(keyPassword, '\u0000');
			dataOutputStream.writeObject(sendbase64);
			dataOutputStream.flush();
			logger.debug("PAKCorePairingHandler : runOperation : Sent: "
					+ sendbase64);

			sendbase64 = Base64.encodeBase64String(cipher
					.doFinal(new PKCS8EncodedKeySpec(ownerKeyEnc.getEncoded())
							.getEncoded()));
			// This code can be inserted once Java 8 implements destroy-Method
			// in order to remove key material securely from JVM memory
			// try {
			// Destroyable destroyEncKey = ownerKeyEnc;
			// destroyEncKey.destroy();
			// } catch (DestroyFailedException e1) {
			// logger.warn(
			// "PAKCorePairingHandler : runOperation : Could not destroy private enc key after pairing: ",
			// e1);
			// }
			dataOutputStream.writeObject(sendbase64);
			dataOutputStream.flush();
			logger.debug("PAKCorePairingHandler : runOperation : Sent: "
					+ sendbase64);

			sendbase64 = Base64.encodeBase64String(cipher
					.doFinal(new PKCS8EncodedKeySpec(ownerKeySign.getEncoded())
							.getEncoded()));
			// This code can be inserted once Java 8 implements destroy-Method
			// in order to remove key material securely from JVM memory
			// try {
			// Destroyable destroySignKey = ownerKeySign;
			// destroySignKey.destroy();
			// } catch (DestroyFailedException e1) {
			// logger.warn(
			// "PAKCorePairingHandler : runOperation : Could not destroy private sign key after pairing: ",
			// e1);
			// }
			dataOutputStream.writeObject(sendbase64);
			dataOutputStream.flush();
			logger.debug("PAKCorePairingHandler : runOperation : Sent: "
					+ sendbase64);

			logger.debug("PAKCorePairingHandler : runOperation : Will now send known devices...");

			sendbase64 = Base64
					.encodeBase64String(cipher.doFinal(Integer.toString(
							knownDevices.size()).getBytes(
							Charset.forName(PanboxConstants.STANDARD_CHARSET))));
			dataOutputStream.writeObject(sendbase64);
			dataOutputStream.flush();
			logger.debug("PAKCorePairingHandler : runOperation : Sent numOfDevices: "
					+ sendbase64);

			for (Map.Entry<String, X509Certificate> entry : knownDevices
					.entrySet()) {

				sendbase64 = Base64
						.encodeBase64String(cipher.doFinal(entry
								.getKey()
								.getBytes(
										Charset.forName(PanboxConstants.STANDARD_CHARSET))));
				dataOutputStream.writeObject(sendbase64);
				dataOutputStream.flush();
				logger.debug("PAKCorePairingHandler : runOperation : Sent devicename: "
						+ sendbase64);

				sendbase64 = Base64.encodeBase64String(cipher.doFinal(entry
						.getValue().getEncoded()));
				dataOutputStream.writeObject(sendbase64);
				dataOutputStream.flush();
				logger.debug("PAKCorePairingHandler : runOperation : Sent devicecert: "
						+ sendbase64);
			}

			logger.debug("PAKCorePairingHandler : runOperation : Will now send known contacts...");

			File vcardFile = File.createTempFile("panbox-pairing-temp", null);
			AbstractAddressbookManager.exportContacts(knownContacts, vcardFile);

			sendbase64 = Base64.encodeBase64String(cipher.doFinal(Files
					.readAllBytes(Paths.get(vcardFile.getAbsolutePath()))));
			dataOutputStream.writeObject(sendbase64);
			dataOutputStream.flush();
			logger.debug("PAKCorePairingHandler : runOperation : Sent vcards: "
					+ sendbase64);

			vcardFile.delete();

			// transmission of devicetype and devicekey

			cipher.init(Cipher.DECRYPT_MODE, spec);

			String base64encRecDevType = (String) dataInputStream.readObject();
			String base64encRecDevCert = (String) dataInputStream.readObject();
			logger.debug("PAKCorePairingHandler : runOperation : Received devType: "
					+ base64encRecDevType);
			logger.debug("PAKCorePairingHandler : runOperation : Received devCert: "
					+ base64encRecDevCert);

			devType = DeviceType.valueOf(new String(cipher.doFinal(Base64
					.decodeBase64(base64encRecDevType))));

			InputStream is = new ByteArrayInputStream(cipher.doFinal(Base64
					.decodeBase64(base64encRecDevCert)));
			devCert = (X509Certificate) certificateFactory
					.generateCertificate(is);

			break;
		case SLAVE:
			logger.debug("PAKCorePairingHandler : runOperation : Started to handle SLAVE pairing");
			cipher.init(Cipher.ENCRYPT_MODE, spec);

			// slave transmission
			logger.debug("PAKCorePairingHandler : runOperation : Will now send master/slave information...");

			sendbase64 = Base64
					.encodeBase64String(cipher.doFinal(PairingType.SLAVE
							.toString()
							.getBytes(
									Charset.forName(PanboxConstants.STANDARD_CHARSET))));
			dataOutputStream.writeObject(sendbase64);
			dataOutputStream.flush();
			logger.debug("PAKCorePairingHandler : runOperation : Sent: "
					+ sendbase64);

			// email, firstname, lastname, devicename transmission
			logger.debug("PAKCorePairingHandler : runOperation : Will now send personal information...");

			sendbase64 = Base64
					.encodeBase64String(cipher.doFinal(eMail.getBytes(Charset
							.forName(PanboxConstants.STANDARD_CHARSET))));
			dataOutputStream.writeObject(sendbase64);
			dataOutputStream.flush();
			logger.debug("PAKCorePairingHandler : runOperation : Sent: "
					+ sendbase64);

			sendbase64 = Base64
					.encodeBase64String(cipher.doFinal(firstName
							.getBytes(Charset
									.forName(PanboxConstants.STANDARD_CHARSET))));
			dataOutputStream.writeObject(sendbase64);
			dataOutputStream.flush();
			logger.debug("PAKCorePairingHandler : runOperation : Sent: "
					+ sendbase64);

			sendbase64 = Base64
					.encodeBase64String(cipher.doFinal(lastName
							.getBytes(Charset
									.forName(PanboxConstants.STANDARD_CHARSET))));
			dataOutputStream.writeObject(sendbase64);
			dataOutputStream.flush();
			logger.debug("PAKCorePairingHandler : runOperation : Sent: "
					+ sendbase64);

			sendbase64 = Base64
					.encodeBase64String(cipher.doFinal(deviceName
							.getBytes(Charset
									.forName(PanboxConstants.STANDARD_CHARSET))));
			dataOutputStream.writeObject(sendbase64);
			dataOutputStream.flush();
			logger.debug("PAKCorePairingHandler : runOperation : Sent: "
					+ sendbase64);

			// owner certs
			logger.debug("PAKCorePairingHandler : runOperation : Will now send owner certificates...");

			sendbase64 = Base64.encodeBase64String(cipher.doFinal(ownerCertEnc
					.getEncoded()));
			dataOutputStream.writeObject(sendbase64);
			dataOutputStream.flush();
			logger.debug("PAKCorePairingHandler : runOperation : Sent: "
					+ sendbase64);

			sendbase64 = Base64.encodeBase64String(cipher.doFinal(ownerCertSign
					.getEncoded()));
			dataOutputStream.writeObject(sendbase64);
			dataOutputStream.flush();
			logger.debug("PAKCorePairingHandler : runOperation : Sent: "
					+ sendbase64);

			logger.debug("PAKCorePairingHandler : runOperation : Will now receive device information for device manager...");

			logger.debug("PAKCorePairingHandler : runOperation : Will now send known devices...");

			sendbase64 = Base64
					.encodeBase64String(cipher.doFinal(Integer.toString(
							knownDevices.size()).getBytes(
							Charset.forName(PanboxConstants.STANDARD_CHARSET))));
			dataOutputStream.writeObject(sendbase64);
			dataOutputStream.flush();
			logger.debug("PAKCorePairingHandler : runOperation : Sent numOfDevices: "
					+ sendbase64);

			for (Map.Entry<String, X509Certificate> entry : knownDevices
					.entrySet()) {

				sendbase64 = Base64
						.encodeBase64String(cipher.doFinal(entry
								.getKey()
								.getBytes(
										Charset.forName(PanboxConstants.STANDARD_CHARSET))));
				dataOutputStream.writeObject(sendbase64);
				dataOutputStream.flush();
				logger.debug("PAKCorePairingHandler : runOperation : Sent devicename: "
						+ sendbase64);

				sendbase64 = Base64.encodeBase64String(cipher.doFinal(entry
						.getValue().getEncoded()));
				dataOutputStream.writeObject(sendbase64);
				dataOutputStream.flush();
				logger.debug("PAKCorePairingHandler : runOperation : Sent devicecert: "
						+ sendbase64);
			}

			logger.debug("PAKCorePairingHandler : runOperation : Will now send known contacts...");

			vcardFile = File.createTempFile("panbox-pairing-temp", null);
			AbstractAddressbookManager.exportContacts(knownContacts, vcardFile);

			sendbase64 = Base64.encodeBase64String(cipher.doFinal(Files
					.readAllBytes(Paths.get(vcardFile.getAbsolutePath()))));
			dataOutputStream.writeObject(sendbase64);
			dataOutputStream.flush();
			logger.debug("PAKCorePairingHandler : runOperation : Sent vcards: "
					+ sendbase64);

			vcardFile.delete();

			cipher.init(Cipher.DECRYPT_MODE, spec);

			// transmission of devicetype and devicekey
			base64encRecDevType = (String) dataInputStream.readObject();
			base64encRecDevCert = (String) dataInputStream.readObject();
			logger.debug("PAKCorePairingHandler : runOperation : Received devType: "
					+ base64encRecDevType);
			logger.debug("PAKCorePairingHandler : runOperation : Received devCert: "
					+ base64encRecDevCert);

			devType = DeviceType.valueOf(new String(cipher.doFinal(Base64
					.decodeBase64(base64encRecDevType))));

			is = new ByteArrayInputStream(cipher.doFinal(Base64
					.decodeBase64(base64encRecDevCert)));
			devCert = (X509Certificate) certificateFactory
					.generateCertificate(is);
			break;
		default:
		}

		logger.debug("PAKCorePairingHandler : runOperation : Pairing finished. Will terminate session now.");

		closeConnection();
	}

	public X509Certificate getDevCert() {
		return devCert;
	}

	public DeviceType getDevType() {
		return devType;
	}
}
