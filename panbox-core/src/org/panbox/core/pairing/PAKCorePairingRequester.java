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
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.panbox.core.Utils;
import org.panbox.core.crypto.CryptCore;
import org.panbox.core.crypto.KeyConstants;
import org.panbox.core.devicemgmt.DeviceType;
import org.panbox.core.pairing.PAKCorePairingHandler.PairingType;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.util.org.apache.commons.codec.binary.Base64;

/**
 *
 * @author Clemens A. Schulz <c.schulz@sirrix.com>
 */
public abstract class PAKCorePairingRequester extends PAKCoreRequesterProtocol {

	// entries to be send
	private final DeviceType devType;
	private final KeyPair devKey;

	// to be created while pairing
	private X509Certificate devCert;

	// entries to be received
	private PairingType type;
	private String eMail;
	private String firstName;
	private String lastName;
	private String deviceName;
	private KeyPair ownerKeyEnc;
	private KeyPair ownerKeySign;
	private char[] keyPassword;
	private X509Certificate ownerCertEnc;
	private X509Certificate ownerCertSign;

	private Map<String, X509Certificate> knownDevices;
	private Collection<VCard> knownContacts;

	public PAKCorePairingRequester(String password, DeviceType devType,
			KeyPair devKey) {
		super(password);
		this.devType = devType;
		this.devKey = devKey;
	}

	@Override
	public void runOperation(Cipher cipher, SecretKeySpec spec)
			throws Exception {
		logger.debug("PAKCorePairingHandler : runOperation : Started to request pairing");

		KeyFactory keyFactory = KeyFactory
				.getInstance(KeyConstants.ASYMMETRIC_ALGORITHM_ALGO_ONLY);
		CertificateFactory certificateFactory = CertificateFactory
				.getInstance(KeyConstants.CERTIFICATE_ENCODING);

		cipher.init(Cipher.DECRYPT_MODE, spec);

		String base64received;

		base64received = (String) dataInputStream.readObject();
		logger.debug("PAKCorePairingRequester : runOperation : Received pairingType: "
				+ base64received);
		byte[] encType = Base64.decodeBase64(base64received);
		String strType = new String(cipher.doFinal(encType));
		type = PairingType.valueOf(strType);

		switch (type) {
		case MASTER:
			logger.info("PAKCorePairingRequester : runOperation : This device will be paired as master device!");

			base64received = (String) dataInputStream.readObject();
			logger.debug("PAKCorePairingRequester : runOperation : Received email: "
					+ base64received);
			eMail = new String(cipher.doFinal(Base64
					.decodeBase64(base64received)));

			base64received = (String) dataInputStream.readObject();
			logger.debug("PAKCorePairingRequester : runOperation : Received firstname: "
					+ base64received);
			firstName = new String(cipher.doFinal(Base64
					.decodeBase64(base64received)));

			base64received = (String) dataInputStream.readObject();
			logger.debug("PAKCorePairingRequester : runOperation : Received lastname: "
					+ base64received);
			lastName = new String(cipher.doFinal(Base64
					.decodeBase64(base64received)));

			base64received = (String) dataInputStream.readObject();
			logger.debug("PAKCorePairingRequester : runOperation : Received devicename: "
					+ base64received);
			deviceName = new String(cipher.doFinal(Base64
					.decodeBase64(base64received)));

			base64received = (String) dataInputStream.readObject();
			logger.debug("PAKCorePairingRequester : runOperation : Received keyPassword: "
					+ base64received);

			keyPassword = Utils.toChars(cipher.doFinal(Base64
					.decodeBase64(base64received)));

			base64received = (String) dataInputStream.readObject();
			logger.debug("PAKCorePairingRequester : runOperation : Received ownerKeyEnc: "
					+ base64received);
			PKCS8EncodedKeySpec ownerKeyEncSpec = new PKCS8EncodedKeySpec(
					cipher.doFinal(Base64.decodeBase64(base64received)));
			PrivateKey pKey = keyFactory.generatePrivate(ownerKeyEncSpec);
			ownerKeyEnc = CryptCore.privateKeyToKeyPair(pKey);

			base64received = (String) dataInputStream.readObject();
			logger.debug("PAKCorePairingRequester : runOperation : Received ownerKeySign: "
					+ base64received);
			PKCS8EncodedKeySpec ownerKeySignSpec = new PKCS8EncodedKeySpec(
					cipher.doFinal(Base64.decodeBase64(base64received)));
			pKey = keyFactory.generatePrivate(ownerKeySignSpec);
			ownerKeySign = CryptCore.privateKeyToKeyPair(pKey);

			base64received = (String) dataInputStream.readObject();
			logger.debug("PAKCorePairingRequester : runOperation : Received numOfDevices: "
					+ base64received);

			int numOfDevices = Integer.valueOf(new String(cipher.doFinal(Base64
					.decodeBase64(base64received))));

			knownDevices = new HashMap<String, X509Certificate>();

			for (int i = 0; i < numOfDevices; ++i) {
				base64received = (String) dataInputStream.readObject();
				logger.debug("PAKCorePairingRequester : runOperation : Received device name ("
						+ i + "): " + base64received);

				String knownDeviceName = new String(cipher.doFinal(Base64
						.decodeBase64(base64received)));

				base64received = (String) dataInputStream.readObject();
				logger.debug("PAKCorePairingRequester : runOperation : Received device certificate ("
						+ i + "): " + base64received);

				InputStream is = new ByteArrayInputStream(cipher.doFinal(Base64
						.decodeBase64(base64received)));
				X509Certificate knownDeviceCert = (X509Certificate) certificateFactory
						.generateCertificate(is);

				knownDevices.put(knownDeviceName, knownDeviceCert);
				logger.debug("PAKCorePairingRequester : runOperation : Added device ("
						+ i + "): " + knownDeviceName + ": " + knownDeviceCert);
			}

			base64received = (String) dataInputStream.readObject();
			logger.debug("PAKCorePairingRequester : runOperation : Received contacts: "
					+ base64received);

			InputStream is = new ByteArrayInputStream(cipher.doFinal(Base64
					.decodeBase64(base64received)));

			knownContacts = Ezvcard.parse(is).all();

			// --- SEND Device Type and Key ---

			cipher.init(Cipher.ENCRYPT_MODE, spec);

			byte[] encDevType = cipher.doFinal(devType.toString().getBytes());

			devCert = CryptCore.createSelfSignedX509Certificate(
					devKey.getPrivate(), devKey.getPublic(),
					new PairingIPersonDummy(eMail, firstName, lastName));

			byte[] encDevCert = cipher.doFinal(devCert.getEncoded());

			String base64encDevType = Base64.encodeBase64String(encDevType);
			String base64encDevCert = Base64.encodeBase64String(encDevCert);
			logger.debug("PAKCorePairingRequester : runOperation : Send devicetype: "
					+ base64encDevType);
			logger.debug("PAKCorePairingRequester : runOperation : Send devicecert: "
					+ base64encDevCert);

			dataOutputStream.writeObject(base64encDevType);
			dataOutputStream.flush();
			dataOutputStream.writeObject(base64encDevCert);
			dataOutputStream.flush();

			break;
		case SLAVE:
			logger.info("PAKCorePairingRequester : runOperation : This device will be paired as slave device!");

			base64received = (String) dataInputStream.readObject();
			logger.debug("PAKCorePairingRequester : runOperation : Received email: "
					+ base64received);
			eMail = new String(cipher.doFinal(Base64
					.decodeBase64(base64received)));

			base64received = (String) dataInputStream.readObject();
			logger.debug("PAKCorePairingRequester : runOperation : Received firstname: "
					+ base64received);
			firstName = new String(cipher.doFinal(Base64
					.decodeBase64(base64received)));

			base64received = (String) dataInputStream.readObject();
			logger.debug("PAKCorePairingRequester : runOperation : Received lastname: "
					+ base64received);
			lastName = new String(cipher.doFinal(Base64
					.decodeBase64(base64received)));

			base64received = (String) dataInputStream.readObject();
			logger.debug("PAKCorePairingRequester : runOperation : Received devicename: "
					+ base64received);
			deviceName = new String(cipher.doFinal(Base64
					.decodeBase64(base64received)));

			base64received = (String) dataInputStream.readObject();
			logger.debug("PAKCorePairingRequester : runOperation : Received ownerCertEnc: "
					+ base64received);
			is = new ByteArrayInputStream(cipher.doFinal(Base64
					.decodeBase64(base64received)));
			ownerCertEnc = (X509Certificate) certificateFactory
					.generateCertificate(is);

			base64received = (String) dataInputStream.readObject();
			logger.debug("PAKCorePairingRequester : runOperation : Received ownerCertSign: "
					+ base64received);
			is = new ByteArrayInputStream(cipher.doFinal(Base64
					.decodeBase64(base64received)));
			ownerCertSign = (X509Certificate) certificateFactory
					.generateCertificate(is);

			base64received = (String) dataInputStream.readObject();
			logger.debug("PAKCorePairingRequester : runOperation : Received numOfDevices: "
					+ base64received);

			numOfDevices = Integer.valueOf(new String(cipher.doFinal(Base64
					.decodeBase64(base64received))));

			knownDevices = new HashMap<String, X509Certificate>();

			for (int i = 0; i < numOfDevices; ++i) {
				base64received = (String) dataInputStream.readObject();
				logger.debug("PAKCorePairingRequester : runOperation : Received device name ("
						+ i + "): " + base64received);

				String knownDeviceName = new String(cipher.doFinal(Base64
						.decodeBase64(base64received)));

				base64received = (String) dataInputStream.readObject();
				logger.debug("PAKCorePairingRequester : runOperation : Received device certificate ("
						+ i + "): " + base64received);

				is = new ByteArrayInputStream(cipher.doFinal(Base64
						.decodeBase64(base64received)));
				X509Certificate knownDeviceCert = (X509Certificate) certificateFactory
						.generateCertificate(is);

				knownDevices.put(knownDeviceName, knownDeviceCert);
				logger.debug("PAKCorePairingRequester : runOperation : Added device ("
						+ i + "): " + knownDeviceName + ": " + knownDeviceCert);
			}

			base64received = (String) dataInputStream.readObject();
			logger.debug("PAKCorePairingRequester : runOperation : Received contacts: "
					+ base64received);

			is = new ByteArrayInputStream(cipher.doFinal(Base64
					.decodeBase64(base64received)));

			knownContacts = Ezvcard.parse(is).all();

			// --- SEND Device Type and Key ---

			cipher.init(Cipher.ENCRYPT_MODE, spec);

			encDevType = cipher.doFinal(devType.toString().getBytes());

			devCert = CryptCore.createSelfSignedX509Certificate(
					devKey.getPrivate(), devKey.getPublic(),
					new PairingIPersonDummy(eMail, firstName, lastName));

			encDevCert = cipher.doFinal(devCert.getEncoded());

			base64encDevType = Base64.encodeBase64String(encDevType);
			base64encDevCert = Base64.encodeBase64String(encDevCert);
			logger.debug("PAKCorePairingRequester : runOperation : Send devicetype: "
					+ base64encDevType);
			logger.debug("PAKCorePairingRequester : runOperation : Send devicecert: "
					+ base64encDevCert);

			dataOutputStream.writeObject(base64encDevType);
			dataOutputStream.flush();
			dataOutputStream.writeObject(base64encDevCert);
			dataOutputStream.flush();
			break;
		default:
			logger.error("PAKCorePairingRequester : runOperation : Unknown pairing type!");
			break;
		}

		logger.debug("PAKCorePairingRequester : runOperation : Pairing finished. Will wait for session to be closed!.");

		try {
			dataInputStream.readBoolean();
		} catch (Exception ex) {
			// Connection has been closed successfully! Pairing done :)
		}
	}

	public PairingType getType() {
		return type;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public String geteMail() {
		return eMail;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public KeyPair getOwnerKeyEnc() {
		return ownerKeyEnc;
	}

	public KeyPair getOwnerKeySign() {
		return ownerKeySign;
	}

	public char[] getKeyPassword() {
		return keyPassword;
	}

	public X509Certificate getOwnerCertEnc() {
		return ownerCertEnc;
	}

	public X509Certificate getOwnerCertSign() {
		return ownerCertSign;
	}

	public X509Certificate getDevCert() {
		return devCert;
	}

	public Map<String, X509Certificate> getKnownDevices() {
		return knownDevices;
	}
	
	public Collection<VCard> getKnownContacts() {
		return knownContacts;
	}
}
