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
package org.panbox.core.keymgmt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.apache.log4j.Logger;
import org.panbox.core.Utils;
import org.panbox.core.crypto.CryptCore;
import org.panbox.core.crypto.EncodingHelper;
import org.panbox.core.crypto.EncodingType;
import org.panbox.core.crypto.KeyConstants;
import org.panbox.core.crypto.Signable;
import org.panbox.core.exception.DeviceListException;
import org.panbox.core.exception.InitializaionException;
import org.panbox.core.exception.PersistanceException;
import org.panbox.core.exception.SerializationException;
import org.panbox.core.exception.ShareMetaDataException;
import org.panbox.core.exception.SymmetricKeyDecryptionException;
import org.panbox.core.exception.SymmetricKeyEncryptionException;

public final class ShareMetaData implements Signable {

	public static final Logger logger = Logger.getLogger(ShareMetaData.class);

	protected UUID id = null;
	protected SharePartList shareParticipants;
	protected TreeMap<PublicKey, DeviceList> deviceLists;
	protected ShareKeyDB shareKeys;
	protected String shareKeyAlgorithm = KeyConstants.SYMMETRIC_ALGORITHM;
	protected String publicKeyAlgorithm = KeyConstants.ASYMMETRIC_ALGORITHM;

	/**
	 * Public Signature Key of share owner, used to verify the share participant
	 * list
	 */
	protected final PublicKey ownerPubSigKey;

	protected ObfuscationKeyDB obfuscationKeys;

	protected String obfuscationKeyAlgorithm = KeyConstants.SYMMETRIC_ALGORITHM;
	byte[] signature;

	private final DBHelper db;

	/**
	 * 
	 * @param url
	 *            - path incl filename for sqlite db (null = inmemory db)
	 * @throws InitializaionException
	 * @throws SequenceNumberException
	 * @throws SignatureException
	 */
	ShareMetaData(DBHelper dbHelper, PublicKey owner) {
		this.ownerPubSigKey = owner;
		this.db = dbHelper;

		//
		this.deviceLists = new TreeMap<PublicKey, DeviceList>(
				Utils.PK_COMPARATOR);
		this.shareKeys = new ShareKeyDB();
		this.obfuscationKeys = new ObfuscationKeyDB();
		this.id = UUID.randomUUID();
		//
	}

	SharePartList initSharePartList() {
		shareParticipants = new SharePartList();
		return this.shareParticipants;
	}

	DeviceList createDeviceList(PublicKey key, Map<String, PublicKey> dkList) {
		if (dkList != null && !dkList.isEmpty() && key != null) {
			DeviceList deviceList = new DeviceList(key, dkList);
			this.deviceLists.put(key, deviceList);
			return deviceList;
		} else {
			throw new IllegalArgumentException(
					"key or dkList was either empty or null!");
		}
	}

	Map<PublicKey, DeviceList> getDeviceLists() {
		return this.deviceLists;
	}

	ShareKeyDB getShareKeys() {
		return this.shareKeys;
	}

	/**
	 * The sole use of this method is to retrieve the latest ShareKey to use
	 * when encrypting files.
	 * 
	 * @param pKey
	 *            PublicKey for the current user device
	 * @return either an EncryptedShareKey Object or <code>null</code> if the
	 *         ShareKey Database contained no ShareKeys, which means, that the
	 *         corresponding volume has just been initialized or there is no
	 *         encrypted ShareKey for the given PublicKey available
	 */
	EncryptedShareKey getLatestEncryptedShareKey(PublicKey pKey) {
		ShareKeyDBEntry lastEntry = this.shareKeys.getLastEntry();
		if (lastEntry != null) {
			byte[] encryptedKey = lastEntry.getEncryptedKey(pKey);
			if (encryptedKey != null) {
				return new EncryptedShareKey(encryptedKey,
						lastEntry.getVersion());
			}
		}
		return null;
	}

	/**
	 * This method is to be used for retrieving a user's device's ShareKey for
	 * decrypting a file.
	 * 
	 * @param version
	 *            The ShareKey version as retrieved from the encrypted file's
	 *            metadata
	 * @param pKey
	 *            The PublicKey of the user's currently used device
	 * @return either an EncryptedShareKey Object or <code>null</code> if the
	 *         ShareKey Database didn't hold ShareKeys of the given version or
	 *         there is no encrypted ShareKey for the given PublicKey available
	 */
	EncryptedShareKey getEncryptedShareKey(int version, PublicKey pKey) {
		ShareKeyDBEntry entry = this.shareKeys.getEntry(version);
		if (entry != null) {
			byte[] encryptedKey = entry.getEncryptedKey(pKey);
			if (encryptedKey != null) {
				return new EncryptedShareKey(encryptedKey, entry.getVersion());
			}
		}
		return null;
	}

	/**
	 * @param key
	 *            DeviceKey for which the encrypted
	 * @return
	 */
	byte[] getEncryptedObfuscationKey(PublicKey key) {
		return obfuscationKeys.get(key);
	}

	byte[] removeObfuscationKey(PublicKey deviceKey) {
		return obfuscationKeys.remove(deviceKey);
	}

	void addObfuscationKey(PublicKey oldDevicePubKey,
			PrivateKey oldDevicePrivKey, PublicKey devicePubKey)
			throws SymmetricKeyEncryptionException,
			SymmetricKeyDecryptionException {
		if (oldDevicePubKey == null) {
			throw new IllegalArgumentException("Keys cannot be null!");
		}
		if (this.obfuscationKeys.isEmpty()) {
			logger.debug("This is a new share: Initializing ObfuscationKey");
			obfuscationKeys.add(oldDevicePubKey, CryptCore.encryptSymmetricKey(
					CryptCore.generateSymmetricKey().getEncoded(),
					oldDevicePubKey));
		} else {
			if (oldDevicePrivKey == null || devicePubKey == null) {
				throw new IllegalArgumentException("Keys cannot be null!");
			}
			SecretKey obKey = CryptCore.decryptSymmertricKey(
					getEncryptedObfuscationKey(oldDevicePubKey),
					oldDevicePrivKey);
			obfuscationKeys.add(devicePubKey, CryptCore.encryptSymmetricKey(
					obKey.getEncoded(), devicePubKey));
		}
	}

	void addObfuscationKeys(PublicKey oldDevicePubKey,
			PrivateKey oldDevicePrivKey, Collection<PublicKey> deviceKeys)
			throws SymmetricKeyEncryptionException,
			SymmetricKeyDecryptionException {
		if (oldDevicePubKey == null) {
			throw new IllegalArgumentException("Keys cannot be null!");
		}
		SecretKey obKey = null;
		if (this.obfuscationKeys.isEmpty()) {
			logger.debug("This is a new share: Initializing ObfuscationKey");
			obKey = CryptCore.generateSymmetricKey();
			obfuscationKeys.add(oldDevicePubKey, CryptCore.encryptSymmetricKey(
					obKey.getEncoded(), oldDevicePubKey));
		} else {
			if (oldDevicePrivKey == null || deviceKeys == null) {
				throw new IllegalArgumentException("Keys cannot be null!");
			}
			obKey = CryptCore.decryptSymmertricKey(
					getEncryptedObfuscationKey(oldDevicePubKey),
					oldDevicePrivKey);
		}

		byte[] encoded = obKey.getEncoded();
		for (PublicKey deviceKey : deviceKeys) {
			obfuscationKeys.add(deviceKey,
					CryptCore.encryptSymmetricKey(encoded, deviceKey));
		}
	}

	byte[] getSignature() {
		return this.signature;
	}

	SharePartList getSharePartList() {
		return this.shareParticipants;
	}

	@Override
	public byte[] serialize() throws SerializationException {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			os.write(id.toString().getBytes());
			os.write(obfuscationKeyAlgorithm.getBytes());
			os.write(shareKeyAlgorithm.getBytes());
			os.write(publicKeyAlgorithm.getBytes());
			os.write(shareParticipants.serialize());
			for (PublicKey key : deviceLists.keySet()) {
				os.write(key.getEncoded());
				os.write(deviceLists.get(key).serialize());
			}
			os.write(shareKeys.serialize());
			os.write(obfuscationKeys.serialize());
			return os.toByteArray();
		} catch (IOException e) {
			throw new SerializationException(
					"Could not serialize Share Metadata", e);
		}
	}

	void persist() throws ShareMetaDataException {
		try {
			this.db.storeSPL(this);
		} catch (PersistanceException e) {
			throw new ShareMetaDataException("Could not persist ShareMetaData",
					e);
		}
	}

	void persist(DeviceList devices) throws ShareMetaDataException {
		try {
			this.db.store(devices, this.obfuscationKeys, this.shareKeys);
		} catch (PersistanceException e) {
			throw new ShareMetaDataException("Could not persist ShareMetaData",
					e);
		}
	}

	/**
	 * @throws DeviceListException
	 * @throws SignatureException
	 * @throws InitializaionException
	 *             If old Sharemetadata object exists: reload its content from
	 *             db
	 * 
	 * @return true is successful
	 * @throws SequenceNumberException
	 * @throws
	 */
	void load() throws InitializaionException, SignatureException,
			DeviceListException {
		try {
			this.db.load(this);
		} catch (SQLException e) {
			throw new InitializaionException("Could not load share metadata", e);
		}
	}

	UUID getUUID() {
		return this.id;
	}

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();

		sb.append("ShareMetaData (ID: " + id.toString() + ")\n");
		sb.append(shareParticipants);

		sb.append("DeviceLists:\n");
		for (PublicKey key : deviceLists.keySet()) {
			sb.append("\tPK: "
					+ EncodingHelper.encodeByte(key.getEncoded(),
							EncodingType.BASE64) + "\n");
			sb.append(deviceLists.get(key));
		}

		sb.append(shareKeys);
		sb.append("--End Signature--\n");

		sb.append("--End ShareMetaData--\n");

		return sb.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (obj.getClass() != getClass())
			return false;
		ShareMetaData smd = (ShareMetaData) obj;
		return ((smd.deviceLists == deviceLists) || (smd.deviceLists != null
				&& deviceLists != null && Utils.valueEquals(smd.deviceLists,
				deviceLists)))
				&& ((smd.id == id) || (smd.id != null && smd.id.equals(id)))
				&& ((smd.obfuscationKeyAlgorithm == obfuscationKeyAlgorithm) || (smd.obfuscationKeyAlgorithm != null && smd.obfuscationKeyAlgorithm
						.equals(obfuscationKeyAlgorithm)))
				&& ((smd.obfuscationKeys == obfuscationKeys) || (smd.obfuscationKeys != null && smd.obfuscationKeys
						.equals(obfuscationKeys)))
				&& (ownerPubSigKey != null ? ownerPubSigKey
						.equals(smd.ownerPubSigKey)
						: smd.ownerPubSigKey == null)
				&& ((publicKeyAlgorithm == smd.publicKeyAlgorithm) || (smd.publicKeyAlgorithm != null && smd.publicKeyAlgorithm
						.equals(publicKeyAlgorithm)))
				&& ((smd.shareKeys == shareKeys) || (smd.shareKeys != null && smd.shareKeys
						.equals(shareKeys)))
				&& ((smd.shareKeyAlgorithm == shareKeyAlgorithm) || (smd.shareKeyAlgorithm != null && smd.shareKeyAlgorithm
						.equals(shareKeyAlgorithm)))
				&& ((smd.shareParticipants == shareParticipants) || (smd.shareParticipants != null && smd.shareParticipants
						.equals(shareParticipants)))
				&& ((signature == smd.signature) || (smd.signature != null
						&& signature != null && Arrays.equals(smd.signature,
						signature)));
	}

	@Override
	public int hashCode() {
		int hc = 11;
		int mul = 37;
		hc = hc * mul;
		hc += (deviceLists == null ? 0 : deviceLists.hashCode());
		hc += (id == null ? 0 : id.hashCode());
		hc += (obfuscationKeyAlgorithm == null ? 0 : obfuscationKeyAlgorithm
				.hashCode());
		hc += (obfuscationKeys == null ? 0 : obfuscationKeys.hashCode());
		hc += (ownerPubSigKey == null ? 0 : ownerPubSigKey.hashCode());
		hc += (publicKeyAlgorithm == null ? 0 : publicKeyAlgorithm.hashCode());
		hc += (shareKeys == null ? 0 : shareKeys.hashCode());
		hc += (shareKeyAlgorithm == null ? 0 : shareKeyAlgorithm.hashCode());
		hc += (shareParticipants == null ? 0 : shareParticipants.hashCode());
		if (signature != null) {
			for (int i = 0; i < signature.length; i++) {
				hc += signature[i];
			}
		}
		return hc;
	}

}
