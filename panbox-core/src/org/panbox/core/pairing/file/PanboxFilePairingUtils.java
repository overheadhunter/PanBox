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
package org.panbox.core.pairing.file;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.log4j.Logger;
import org.panbox.core.crypto.CryptCore;
import org.panbox.core.devicemgmt.DeviceType;
import org.panbox.core.identitymgmt.AbstractAddressbookManager;
import org.panbox.core.pairing.PairingIPersonDummy;
import org.panbox.core.pairing.PAKCorePairingHandler.PairingType;

import ezvcard.VCard;

public class PanboxFilePairingUtils {

	private static String DELIMITER = ":_:_:_:";

	private static final Logger logger = Logger.getLogger("org.panbox");

	/**
	 * Stores a pairing file at the specified path for the specified device and
	 * type
	 * 
	 * @param outputFile
	 *            Pairing file to be saved
	 * @param devicename
	 *            Name of the device that should be paired
	 * @param password
	 *            Password of the identity
	 */
	public static PanboxFilePairingWriteReturnContainer storePairingFile(
			File outputFile, String devicename, char[] password,
			PairingType type, DeviceType devType, String eMail,
			String firstName, String lastName, PrivateKey privEncKey,
			X509Certificate encCert, PrivateKey privSignKey,
			X509Certificate signCert, Map<String, X509Certificate> devices,
			Collection<VCard> contacts) throws IOException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException {
		logger.debug("PanboxFilePairingUtils : storePairingFile : Storing pairing container to: "
				+ outputFile.getAbsolutePath());

		ZipArchiveOutputStream out = new ZipArchiveOutputStream(
				new FileOutputStream(outputFile));

		// 1. add device name to pairing file
		ZipArchiveEntry entry = new ZipArchiveEntry("devicename");
		entry.setSize(devicename.getBytes().length);
		out.putArchiveEntry(entry);

		out.write(devicename.getBytes());
		out.flush();

		out.closeArchiveEntry();

		// 2. add device name to pairing file
		entry = new ZipArchiveEntry("email");
		entry.setSize(eMail.getBytes().length);
		out.putArchiveEntry(entry);

		out.write(eMail.getBytes());
		out.flush();

		out.closeArchiveEntry();

		// 3. add device name to pairing file
		entry = new ZipArchiveEntry("firstname");
		entry.setSize(firstName.getBytes().length);
		out.putArchiveEntry(entry);

		out.write(firstName.getBytes());
		out.flush();

		out.closeArchiveEntry();

		// 4. add device name to pairing file
		entry = new ZipArchiveEntry("lastname");
		entry.setSize(lastName.getBytes().length);
		out.putArchiveEntry(entry);

		out.write(lastName.getBytes());
		out.flush();

		out.closeArchiveEntry();

		// 5. generate and add a new device key + cert for the newly device
		KeyPair devKey = CryptCore.generateKeypair();
		X509Certificate devCert = CryptCore.createSelfSignedX509Certificate(
				devKey.getPrivate(), devKey.getPublic(),
				new PairingIPersonDummy(eMail, firstName, lastName));

		KeyStore devKeyStore = KeyStore.getInstance("PKCS12");
		devKeyStore.load(null, null);
		devKeyStore.setKeyEntry(devicename, (Key) devKey.getPrivate(),
				password, new Certificate[] { devCert });
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		devKeyStore.store(baos, password);
		baos.flush();

		byte[] data = baos.toByteArray();
		entry = new ZipArchiveEntry("devicekey.p12");
		entry.setSize(data.length);
		out.putArchiveEntry(entry);
		out.write(data);
		out.flush();

		out.closeArchiveEntry();

		// 6. add device certs and names for all known devices

		baos = new ByteArrayOutputStream();
		ByteArrayOutputStream deviceNamesFile = new ByteArrayOutputStream();
		KeyStore deviceKeyStore = KeyStore.getInstance("BKS");
		deviceKeyStore.load(null, null);
		int i = 0;

		for (Entry<String, X509Certificate> device : devices.entrySet()) {
			deviceKeyStore.setCertificateEntry("device" + i, device.getValue());
			deviceNamesFile
					.write(("device" + i + DELIMITER + device.getKey() + "\n")
							.getBytes());
			++i;
		}

		deviceKeyStore.store(baos, password);
		baos.flush();
		deviceNamesFile.flush();

		byte[] data2 = deviceNamesFile.toByteArray();
		entry = new ZipArchiveEntry("knownDevices.list");
		entry.setSize(data2.length);
		out.putArchiveEntry(entry);
		out.write(data2);
		out.flush();

		data = baos.toByteArray();
		entry = new ZipArchiveEntry("knownDevices.bks");
		entry.setSize(data.length);
		out.putArchiveEntry(entry);
		out.write(data);
		out.flush();

		// 7. add vcard for all known contacts

		File tempContacts = File.createTempFile("panboxContacts", null);
		AbstractAddressbookManager.exportContacts(contacts, tempContacts);
		FileInputStream fis = new FileInputStream(tempContacts);
		data = new byte[(int) tempContacts.length()];
		fis.read(data);
		fis.close();
		tempContacts.delete();

		entry = new ZipArchiveEntry("contacts.vcard");
		entry.setSize(data.length);
		out.putArchiveEntry(entry);
		out.write(data);
		out.flush();

		// 8. add owner certs or keys in case of main/restricted
		KeyStore ownerKeyStore = null;
		if (type == PairingType.MASTER) {
			ownerKeyStore = KeyStore.getInstance("PKCS12");
			ownerKeyStore.load(null, null);
			ownerKeyStore.setKeyEntry("ownerEncKey", privEncKey, password,
					new Certificate[] { encCert });
			ownerKeyStore.setKeyEntry("ownerSignKey", privSignKey, password,
					new Certificate[] { signCert });
			entry = new ZipArchiveEntry("ownerKeys.p12");
		} else {
			ownerKeyStore = KeyStore.getInstance("BKS");
			ownerKeyStore.load(null, null);
			ownerKeyStore.setCertificateEntry("ownerEncCert", encCert);
			ownerKeyStore.setCertificateEntry("ownerSignCert", signCert);
			entry = new ZipArchiveEntry("ownerCerts.bks");
		}
		baos = new ByteArrayOutputStream();
		ownerKeyStore.store(baos, password);
		baos.flush();

		data = baos.toByteArray();
		entry.setSize(data.length);
		out.putArchiveEntry(entry);
		out.write(data);
		out.flush();

		out.closeArchiveEntry();

		out.flush();
		out.close();
		logger.debug("PanboxFilePairingUtils : storePairingFile : Storing pairing container finished.");

		return new PanboxFilePairingWriteReturnContainer(devicename, devCert, devType);
	}

	public static class PanboxFilePairingWriteReturnContainer {
		private final String devicename;
		private final X509Certificate devCert;
		private final DeviceType devType;

		public PanboxFilePairingWriteReturnContainer(String devicename,
				X509Certificate devCert, DeviceType devType) {
			this.devicename = devicename;
			this.devCert = devCert;
			this.devType = devType;
		}

		public X509Certificate getDevCert() {
			return devCert;
		}

		public String getDevicename() {
			return devicename;
		}

		public DeviceType getDevType() {
			return devType;
		}
	}

	public static PanboxFilePairingLoadReturnContainer loadPairingFile(
			File inputFile, char[] password) throws IOException,
			NoSuchAlgorithmException, CertificateException, KeyStoreException,
			UnrecoverableKeyException, IllegalArgumentException {
		ZipArchiveInputStream in = new ZipArchiveInputStream(
				new FileInputStream(inputFile));
		try {
			byte[] buffer = new byte[1048576]; //1MB
			
			ArchiveEntry entry;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int len = 0;

			// ENTRY 1: devicename
			entry = in.getNextEntry();

			if (entry == null) {
				logger.error("PanboxClient : loadPairingFile : Could not find entry for device name.");
				throw new IllegalArgumentException(
						"Could not find entry for device name.");
			}

			baos = new ByteArrayOutputStream();
			len = 0;
			while ((len = in.read(buffer)) > 0) {
				baos.write(buffer, 0, len);
			}

			String devicename = new String(baos.toByteArray());

			// ENTRY 2: eMail
			entry = in.getNextEntry();

			if (entry == null) {
				logger.error("PanboxClient : loadPairingFile : Could not find entry for eMail.");
				throw new IllegalArgumentException(
						"Could not find entry for eMail.");
			}

			baos = new ByteArrayOutputStream();
			len = 0;
			while ((len = in.read(buffer)) > 0) {
				baos.write(buffer, 0, len);
			}

			String eMail = new String(baos.toByteArray());

			// ENTRY 3: firstName
			entry = in.getNextEntry();

			if (entry == null) {
				logger.error("PanboxClient : loadPairingFile : Could not find entry for first name.");
				throw new IllegalArgumentException(
						"Could not find entry for first name.");
			}

			baos = new ByteArrayOutputStream();
			len = 0;
			while ((len = in.read(buffer)) > 0) {
				baos.write(buffer, 0, len);
			}

			String firstName = new String(baos.toByteArray());

			// ENTRY 4: lastName
			entry = in.getNextEntry();

			if (entry == null) {
				logger.error("PanboxClient : loadPairingFile : Could not find entry for last name.");
				throw new IllegalArgumentException(
						"Could not find entry for last name.");
			}

			baos = new ByteArrayOutputStream();
			len = 0;
			while ((len = in.read(buffer)) > 0) {
				baos.write(buffer, 0, len);
			}

			String lastName = new String(baos.toByteArray());

			// ENTRY 5: devKeyStore.p12
			entry = in.getNextEntry();

			if (entry == null) {
				logger.error("PanboxClient : loadPairingFile : Could not find entry for device key store.");
				throw new IllegalArgumentException(
						"Could not find entry for device key store.");
			}

			KeyStore devKeyStore = KeyStore.getInstance("PKCS12");
			devKeyStore.load(in, password);
			PrivateKey devPKey = (PrivateKey) devKeyStore.getKey(
					devicename.toLowerCase(), password);
			Certificate[] devCert = devKeyStore.getCertificateChain(devicename
					.toLowerCase());

			// ENTRY 6: knownDevices.list/knownDevices.bks
			entry = in.getNextEntry(); // knownDevices.list

			if (entry == null) {
				logger.error("PanboxClient : loadPairingFile : Could not find entry for knownDevices.list.");
				throw new IllegalArgumentException("Could not find entry for knownDevices.list.");
			}

			Map<String, X509Certificate> devices = new HashMap<String, X509Certificate>();

			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			Map<String, String> deviceNames = new HashMap<String, String>();

			String line;
			while ((line = br.readLine()) != null) {
				String[] values = line.split(DELIMITER);
				deviceNames.put(values[0], values[1]);
			}

			entry = in.getNextEntry(); // knownDevices.bks

			if (entry == null) {
				logger.error("PanboxClient : loadPairingFile : Could not find entry for knownDevices.bks.");
				throw new IllegalArgumentException("Could not find entry for knownDevices.bks.");
			}

			KeyStore devicesStore = KeyStore.getInstance("BKS");
			devicesStore.load(in, password);

			for (Entry<String, String> device : deviceNames.entrySet()) {
				X509Certificate deviceCert = (X509Certificate) devicesStore.getCertificate(device.getKey());
				devices.put(device.getValue(), deviceCert);
			}

			// ENTRY 7: contacts.vcard
			entry = in.getNextEntry();

			if (entry == null) {
				logger.error("PanboxClient : loadPairingFile : Could not find entry for contacts.");
				throw new IllegalArgumentException("Could not find entry for contacts.");
			}

			File contacts = File.createTempFile("panbox" + (new Random().nextInt(65536)-32768), null);
			FileOutputStream fos = new FileOutputStream(contacts);
			len = 0;
			while ((len = in.read(buffer)) > 0) {
				fos.write(buffer, 0, len);
			}
			fos.flush();
			fos.close();
			
			// ENTRY 8: ownerKeyStore/ownerCertStore.jks
			entry = in.getNextEntry();
			
			ByteArrayOutputStream tmp = new ByteArrayOutputStream();
			IOUtils.copy(in, tmp);
			ByteArrayInputStream buf = new ByteArrayInputStream(tmp.toByteArray());

			if (entry == null) {
				logger.error("PanboxClient : loadPairingFile : Could not find entry for owner key store.");
				throw new IllegalArgumentException(
						"Could not find entry for owner key store.");
			}

			KeyStore ownerKeyStore = null;
			try {
				 // Check if pairing is MASTER
				ownerKeyStore = KeyStore.getInstance("PKCS12");
				ownerKeyStore.load(buf, password);
				// At this point we know it's a PKCS11 file!
				PrivateKey ownerEncKey = (PrivateKey) ownerKeyStore.getKey(
						"ownerEncKey", password);
				Certificate[] ownerEncCert = ownerKeyStore
						.getCertificateChain("ownerEncKey");
				PrivateKey ownerSignKey = (PrivateKey) ownerKeyStore.getKey(
						"ownerSignKey", password);
				Certificate[] ownerSignCert = ownerKeyStore
						.getCertificateChain("ownerSignKey");
				in.close();
				removeInputFile(inputFile);
				
				return new PanboxFilePairingLoadReturnContainer(eMail,
						firstName, lastName, password, devicename, devPKey,
						devCert[0], ownerSignKey, ownerSignCert[0],
						ownerEncKey, ownerEncCert[0], devices, contacts);
			} catch (Exception e) {
				// SLAVE
				try {
					buf = new ByteArrayInputStream(tmp.toByteArray());
					ownerKeyStore = KeyStore.getInstance("BKS");
					ownerKeyStore.load(buf, password);
					Certificate ownerEncCert = ownerKeyStore
							.getCertificate("ownerEncCert");
					Certificate ownerSignCert = ownerKeyStore
							.getCertificate("ownerSignCert");
					in.close();
					removeInputFile(inputFile);
					
					return new PanboxFilePairingLoadReturnContainer(eMail,
							firstName, lastName, password, devicename, devPKey,
							devCert[0], null, ownerSignCert, null, ownerEncCert, devices, contacts);
				} catch (Exception ex) {
					logger.error("PanboxClient : loadPairingFile : Could not determine if pairing file was master or slave.");
					throw new IllegalArgumentException(
							"Pairing type was unknown. Broken file?");
				}
			}
		} catch (IOException | NoSuchAlgorithmException | CertificateException
				| KeyStoreException | UnrecoverableKeyException
				| IllegalArgumentException e) {
			in.close();
			throw e;
		}

	}

	private static void removeInputFile(File inputFile) {
		if(inputFile.delete()) {
			logger.debug("PanboxClient : loadPairingFile : Deleted pairing file successfully!");
		} else {
			logger.warn("PanboxClient : loadPairingFile : Deleting pairing file was not successful!");
		}
	}

	public static class PanboxFilePairingLoadReturnContainer {
		private final String eMail;
		private final String firstName;
		private final String lastName;
		private final char[] password;
		private final String deviceName;
		private final PrivateKey devicePrivKey;
		private final Certificate deviceCert;
		private final PrivateKey signPrivKey;
		private final Certificate signCert;
		private final PrivateKey encPrivKey;
		private final Certificate encCert;
		private final Map<String, X509Certificate> devices;
		private final File contactsFile;

		public PanboxFilePairingLoadReturnContainer(String eMail,
				String firstName, String lastName, char[] password,
				String deviceName, PrivateKey devicePrivKey,
				Certificate devCert, PrivateKey signPrivKey,
				Certificate ownerSignCert, PrivateKey encPrivKey,
				Certificate ownerEncCert, Map<String, X509Certificate> devices,
				File contactsFile) {
			this.eMail = eMail;
			this.firstName = firstName;
			this.lastName = lastName;
			this.password = password;
			this.deviceName = deviceName;
			this.devicePrivKey = devicePrivKey;
			this.deviceCert = devCert;
			this.signPrivKey = signPrivKey;
			this.signCert = ownerSignCert;
			this.encPrivKey = encPrivKey;
			this.encCert = ownerEncCert;
			this.devices = devices;
			this.contactsFile = contactsFile;
		}

		public Certificate getDeviceCert() {
			return deviceCert;
		}

		public String getDeviceName() {
			return deviceName;
		}

		public PrivateKey getDevicePrivKey() {
			return devicePrivKey;
		}

		public String geteMail() {
			return eMail;
		}

		public Certificate getEncCert() {
			return encCert;
		}

		public PrivateKey getEncPrivKey() {
			return encPrivKey;
		}

		public String getFirstName() {
			return firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public char[] getPassword() {
			return password;
		}

		public Certificate getSignCert() {
			return signCert;
		}

		public PrivateKey getSignPrivKey() {
			return signPrivKey;
		}

		public Map<String, X509Certificate> getDevices() {
			return devices;
		}

		public File getContactsFile() {
			return contactsFile;
		}
	}

}
