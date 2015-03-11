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
package org.panbox.core.identitymgmt;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.panbox.core.crypto.CryptCore;
import org.panbox.core.crypto.KeyConstants;

public class Identity extends AbstractIdentity {

	// KeyStore holds all Keypairs and is only protected by
	// CryptCore.DEFAULT_PASSWORD. Private Owner keys must be protected by
	// dedicated password
	/**
	 * Local KeyStore holding owner and device keys
	 */

	private static final Logger logger = Logger.getLogger("org.panbox.core");

	protected KeyStore keyStore;

	public static final String OWNER_KEY_SIGN_PRIV = "ownerKeySignPriv";
	public static final String OWNER_KEY_ENC_PRIV = "ownerKeyEncPriv";

	public static final String OWNER_CERT_ENC = "ownerCertEnc";
	public static final String OWNER_CERT_SIGN = "ownerCertSign";

	public Identity(IAddressbook addressbook, String email, String firstName,
			String name) {
		super(email, firstName, name, addressbook);

		initKeystore();
	}

	public Identity(IAddressbook addressbook) {
		super(addressbook);
		initKeystore();
	}

	/**
	 * Creates an empty Keystore, protected by the Cryptcore.DEFAULT_PASSWORD
	 */
	private void initKeystore() {
		this.keyStore = CryptCore.createUnprotectedKeyStore();
	}

	/**
	 * Stores the owner key for signing in the keystore of this identity and
	 * protects it by the given password
	 * 
	 * @param ownerKeySign
	 *            - Keypair representing the owner key for signing
	 * @param password
	 *            - to protect the private key
	 */
	@Override
	public void setOwnerKeySign(KeyPair ownerKeySign, char[] password) {
		try {
			// set key for existing signature cert if it already has been set,
			// otherwise create completely new one
			X509Certificate certChain = getCertSign();
			if (certChain == null) {
				certChain = CryptCore.createSelfSignedX509Certificate(
						ownerKeySign.getPrivate(), ownerKeySign.getPublic(),
						this);
			}

			this.keyStore.setKeyEntry(OWNER_KEY_SIGN_PRIV,
					ownerKeySign.getPrivate(), password,
					new java.security.cert.Certificate[] { certChain });

			// store as certificate as well
			this.keyStore.setCertificateEntry(OWNER_CERT_SIGN, certChain);

		} catch (KeyStoreException e) {
			logger.error("Could not add " + OWNER_KEY_SIGN_PRIV
					+ " to identity's keystore", e);
		}

	}

	/**
	 * Stores the owner key for encryption in the keystore of this identity and
	 * protects it by the given password
	 * 
	 * @param ownerKeyEnc
	 *            - Keypair representing the owner key for encryption
	 * @param password
	 *            - to protect the private key
	 */
	@Override
	public void setOwnerKeyEnc(KeyPair ownerKeyEnc, char[] password) {
		try {
			// set key for existing signature cert if it already has been set,
			// otherwise create completely new one
			X509Certificate certChain = getCertEnc();
			if (certChain == null) {
				certChain = CryptCore
						.createSelfSignedX509Certificate(
								ownerKeyEnc.getPrivate(),
								ownerKeyEnc.getPublic(), this);
			}

			this.keyStore.setKeyEntry(OWNER_KEY_ENC_PRIV,
					ownerKeyEnc.getPrivate(), password,
					new java.security.cert.Certificate[] { certChain });

			// store as certificate as well
			this.keyStore.setCertificateEntry(OWNER_CERT_ENC, certChain);

		} catch (KeyStoreException e) {
			logger.error("Could not add " + OWNER_KEY_ENC_PRIV
					+ " to identity's keystore", e);
		}
	}

	/**
	 * Retrieve the public key for encryption as a certificate (i.e. with expiry
	 * dates)
	 * 
	 * @return null if an error occurred
	 */
	@Override
	public X509Certificate getCertEnc() {
		try {
			return (X509Certificate) this.keyStore
					.getCertificate(OWNER_CERT_ENC);
		} catch (KeyStoreException e) {
			logger.error("Could not fetch " + OWNER_CERT_ENC
					+ " from identity's keystore", e);
		}

		return null;
	}

	/**
	 * Retrieve the public key for signatures as a certificate (i.e. with expiry
	 * dates)
	 * 
	 * @return null if an error occurred
	 */
	@Override
	public X509Certificate getCertSign() {
		try {
			return (X509Certificate) this.keyStore
					.getCertificate(OWNER_CERT_SIGN);
		} catch (KeyStoreException e) {
			logger.error("Could not fetch " + OWNER_CERT_ENC
					+ " from identity's keystore", e);
		}

		return null;
	}

	/**
	 * Stores a given device key in the keystore of this identity
	 * 
	 * @param ownerKeySign
	 *            - Keypair representing the device key
	 * @param deviceName
	 *            - name of the device where the key will be used
	 */
	@Override
	public void addDeviceKey(KeyPair deviceKey, String deviceName) {

		try {
			X509Certificate certChain = CryptCore
					.createSelfSignedX509Certificate(deviceKey.getPrivate(),
							deviceKey.getPublic(), this);

			this.keyStore.setKeyEntry(deviceName, deviceKey.getPrivate(),
					KeyConstants.OPEN_KEYSTORE_PASSWORD,
					new java.security.cert.Certificate[] { certChain });

		} catch (KeyStoreException e) {
			logger.error("Could not add device key for device " + deviceName
					+ " to identity's keystore", e);
		}
	}

	/**
	 * Stores a given device key and its certificate in the keystore of this
	 * identity
	 * 
	 * @param deviceKey
	 *            - Keypair representing the device key
	 * @param deviceCert
	 *            - X509 certificate for the device
	 * @param deviceName
	 *            - name of the device where the key will be used
	 */
	@Override
	public void addDeviceKey(KeyPair deviceKey, Certificate deviceCert,
			String deviceName) {

		try {
			this.keyStore.setKeyEntry(deviceName, deviceKey.getPrivate(),
					KeyConstants.OPEN_KEYSTORE_PASSWORD,
					new java.security.cert.Certificate[] { deviceCert });

		} catch (KeyStoreException e) {
			logger.error("Could not add device key for device " + deviceName
					+ " to identity's keystore", e);
		}
	}

	/**
	 * Stores a certificate from a second device in our keystore (i.e. we are
	 * running on a laptop and get the certificate from our mobile)
	 * 
	 * @param cert
	 *            - Certificate of the other device
	 * @param deviceName
	 *            - Name of the other device
	 */
	@Override
	public void addDeviceCert(Certificate cert, String deviceName) {

		try {

			this.keyStore.setCertificateEntry(deviceName, cert);

		} catch (KeyStoreException e) {
			logger.error("Could not add device certificate for device "
					+ deviceName + " to identity's keystore", e);
		}
	}

	@Override
	public Certificate getDeviceCert(String deviceName) {
		try {
			return this.keyStore.getCertificate(deviceName);
		} catch (KeyStoreException e) {
			logger.error("Could not get device certificate for device "
					+ deviceName + " from identity's keystore", e);
		}
		return null;
	}

	public KeyStore getKeyStore() {
		return keyStore;
	}

	/**
	 * If an existing keystore is loaded from a file, set it with this method
	 * 
	 * @param keyStore
	 */
	public void setKeyStore(KeyStore keyStore) {
		this.keyStore = keyStore;
	}

	/**
	 * Retrieve the private owner key for signing out of the keystore
	 * 
	 * @param password
	 *            - Password to unlock the private key
	 * @return - Private Key for signing
	 */
	@Override
	public PrivateKey getPrivateKeySign(char[] password)
			throws UnrecoverableKeyException {

		PrivateKey key = null;
		try {
			key = (PrivateKey) this.keyStore.getKey(OWNER_KEY_SIGN_PRIV,
					password);
		} catch (KeyStoreException | NoSuchAlgorithmException e) {
			logger.error("Could not fetch " + OWNER_KEY_SIGN_PRIV
					+ " from identity's keystore", e);
			throw new UnrecoverableKeyException("Could not fetch "
					+ OWNER_KEY_SIGN_PRIV + " from identity's keystore");
		}

		return key;
	}

	/**
	 * Retrieve the private owner key for encryption out of the keystore
	 * 
	 * @param password
	 *            - Password to unlock the private key
	 * @return - Private key for encryption, or null if an error occurred
	 * @throws UnrecoverableKeyException
	 */
	@Override
	public PrivateKey getPrivateKeyEnc(char[] password)
			throws UnrecoverableKeyException {

		PrivateKey key = null;
		try {
			key = (PrivateKey) this.keyStore.getKey(OWNER_KEY_ENC_PRIV,
					password);
		} catch (KeyStoreException | NoSuchAlgorithmException e) {
			throw new UnrecoverableKeyException("Could not fetch private key "
					+ OWNER_KEY_ENC_PRIV + " from identity's keystore");
		}

		return key;
	}

	/**
	 * Retrieve the public owner key for encryption
	 * 
	 * @return - public owner key for encryption, or null if an error occurred
	 */
	@Override
	public PublicKey getPublicKeyEnc() {

		PublicKey key = null;
		try {
			//OWNER_CERT_ENC
//			key = (PublicKey) this.keyStore.getCertificate(OWNER_KEY_ENC_PRIV)
//					.getPublicKey();
			key = (PublicKey) this.keyStore.getCertificate(OWNER_CERT_ENC)
					.getPublicKey();
		} catch (KeyStoreException e) {
			logger.error("Could not fetch public key " + OWNER_CERT_ENC
					+ " from identity's keystore", e);
		}

		return key;
	}

	/**
	 * Retrieve the public owner key for signing
	 * 
	 * @return - public owner key for signing, or null if an error occurred
	 */
	@Override
	public PublicKey getPublicKeySign() {

		PublicKey key = null;
		try {
			//OWNER_CERT_SIGN
//			key = (PublicKey) this.keyStore.getCertificate(OWNER_KEY_SIGN_PRIV)
//					.getPublicKey();			
			key = (PublicKey) this.keyStore.getCertificate(OWNER_CERT_SIGN)
					.getPublicKey();
		} catch (KeyStoreException e) {
			logger.error("Could not fetch certificate " + OWNER_CERT_SIGN
					+ " from identity's keystore", e);
		}

		return key;
	}

	/**
	 * Retrieve the private key of a device with name deviceName
	 * 
	 * @param password
	 *            - password to unlock the key
	 * @param deviceName
	 *            - name of the device to retrieve the private key for
	 * @return - private key for deviceName or null if we run on a different
	 *         device
	 */
	@Override
	public PrivateKey getPrivateKeyForDevice(String deviceName) {

		PrivateKey key = null;
		try {
			key = (PrivateKey) this.keyStore.getKey(deviceName,
					KeyConstants.OPEN_KEYSTORE_PASSWORD);
		} catch (UnrecoverableKeyException | KeyStoreException
				| NoSuchAlgorithmException e) {
			logger.error("Could not fetch private key for device " + deviceName
					+ " from identity's keystore", e);
		}

		return key;
	}

	/**
	 * Retrieve the public key of a device with name deviceName
	 * 
	 * @param deviceName
	 *            - name of the device to retrieve the public key for
	 * @return - public key of device deviceName, or null if an error occurred
	 * @throws UnrecoverableKeyException
	 */
	@Override
	public PublicKey getPublicKeyForDevice(String deviceName)
			throws UnrecoverableKeyException {
		PublicKey key = null;
		try {
			Certificate certificate = this.keyStore.getCertificate(deviceName);
			if (certificate == null) {
				throw new UnrecoverableKeyException(
						"Could not find Certificate for device " + deviceName);
			}
			key = (PublicKey) certificate.getPublicKey();
		} catch (KeyStoreException e) {
			logger.error("Could not fetch public key for device " + deviceName
					+ " from identity's keystore", e);
		}

		return key;
	}

	@Override
	/**
	 * Obtain a list of all device names the identity has keys for
	 * 
	 * @return - List of devices as strings
	 */
	public Collection<String> getDeviceList() {

		LinkedList<String> myDevices = new LinkedList<String>();
		String alias = null;
		try {
			Enumeration<String> aliases = this.keyStore.aliases();
			while (aliases.hasMoreElements()) {
				alias = aliases.nextElement();
				if (!OWNER_CERT_ENC.equalsIgnoreCase(alias)
						&& !OWNER_CERT_SIGN.equalsIgnoreCase(alias)
						&& !OWNER_KEY_ENC_PRIV.equalsIgnoreCase(alias)
						&& !OWNER_KEY_SIGN_PRIV.equalsIgnoreCase(alias)) {
					myDevices.add(alias);
				}
			}
		} catch (KeyStoreException e) {
			logger.error(
					"Could not fetch key aliases from identity's keystore", e);
		}
		return myDevices;
	}

	@Override
	public void setOwnerKeySign(Certificate cert) {
		try {
			this.keyStore.setCertificateEntry(OWNER_CERT_SIGN, cert);
		} catch (KeyStoreException e) {
			logger.error(
					"Could not store certificate for signing in identity's keystore",
					e);
		}
	}

	@Override
	public void setOwnerKeyEnc(Certificate cert) {
		try {
			this.keyStore.setCertificateEntry(OWNER_CERT_ENC, cert);
		} catch (KeyStoreException e) {
			logger.error(
					"Could not store certificate for encryption in identity's keystore",
					e);
		}

	}

	@Override
	public boolean checkCertificateValidity() {
		X509Certificate sigCert = getCertSign();
		X509Certificate encCert = getCertEnc();

		if (null == sigCert || null == encCert) {
			logger.error("Cannot obtain Certificates for validation");
			// treat as invalid
			return false;
		}

		Date now = new Date();
		try {
			sigCert.checkValidity(now);
			encCert.checkValidity(now);
		} catch (CertificateExpiredException | CertificateNotYetValidException e) {
			// certificate not (yet) valid
			return false;
		}

		return true;
	}

}
