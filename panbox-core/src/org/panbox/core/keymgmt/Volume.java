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

import java.io.File;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.apache.log4j.Logger;
import org.panbox.core.Utils;
import org.panbox.core.crypto.CryptCore;
import org.panbox.core.crypto.SignatureHelper;
import org.panbox.core.exception.DeviceListException;
import org.panbox.core.exception.InitializaionException;
import org.panbox.core.exception.SerializationException;
import org.panbox.core.exception.ShareMetaDataException;
import org.panbox.core.exception.SymmetricKeyDecryptionException;
import org.panbox.core.exception.SymmetricKeyEncryptionException;
import org.panbox.core.exception.SymmetricKeyNotFoundException;

/**
 * @author Sinisa Dukanovic
 * 
 */
public class Volume implements IVolume {

	private final static Logger logger = Logger.getLogger(Volume.class);

	public final static String DB_FILE = "pbmeta.db";
	public final static String SPL_FILE = "pb_spl.db";
	public final static String KEYS_FILE = "pb_keys.db";
	private final DBHelper db;

	private ShareMetaData shareMetaData;

	/**
	 * Default Constructor for Panpox Volumes
	 * 
	 * @param path
	 */
	public Volume(String path) throws IllegalArgumentException {
		this(new JDBCHelperNonRevokeable(getPath(path)));
	}

	public Volume(DBHelper dbHelper) throws IllegalArgumentException {
		this.db = dbHelper;
		if (dbHelper == null) {
			throw new IllegalAccessError("Database Helper can't be null");
		}
	}

	@Override
	public ShareMetaData loadShareMetaData(PublicKey owner)
			throws ShareMetaDataException {
		// logger.info("Loading ShareMetaData from db file: " + path);
		logger.info("Loading ShareMetaData from db");
		if (this.shareMetaData == null) {
			this.shareMetaData = new ShareMetaData(this.db, owner);
			try {
				this.shareMetaData.load();
			} catch (InitializaionException e) {
				throw new ShareMetaDataException(
						"Could not initialize ShareMetaData", e);
			} catch (SignatureException e) {
				throw new ShareMetaDataException("Could not verify signature",
						e);
			} catch (DeviceListException e) {
				throw new ShareMetaDataException(
						"Could not verify signature of device list", e);
			}
		}
		return this.shareMetaData;
	}

	private static String getPath(final String path) {
		String url = "jdbc:sqlite:";
		if (path == null) {
			logger.info("Choosing inmemory db");
		} else {
			logger.info("Choosing " + path + " as metadata database");
			url = url + path
					+ (path.endsWith(File.separator) ? "" : File.separator);
		}
		return url;
	}

	@Override
	public UUID getUUID() {
		return this.shareMetaData.getUUID();
	}

	@Override
	public ShareMetaData createShareMetaData(VolumeParams p)
			throws IllegalArgumentException, ShareMetaDataException {
		return this.createShareMetaData(p.ownerAlias, p.pubSigKey,
				p.privSigKey, p.pubEncKey, p.privEncKey, p.deviceAlias,
				p.deviceKey);
	}

	@Override
	public ShareMetaData createShareMetaData(String ownerAlias,
			PublicKey ownerPubSigKey, PrivateKey ownerPrivSigKey,
			PublicKey ownerPubEncKey, PrivateKey ownerPrivEncKey,
			String deviceAlias, PublicKey devicePubKey)
			throws IllegalArgumentException, ShareMetaDataException {

		if (IVolume.MASTER_KEY.equals(deviceAlias)) {
			throw new IllegalArgumentException(
					"Illegal alias for device. Choose another alias");
		}

		if (db.exists()) {
			throw new IllegalArgumentException(
					"Can't initialize new share metadata, because database already exists.");
		}

		// verify matching public/private keys
		if (!Utils.keysMatch(ownerPubSigKey, ownerPrivSigKey)) {
			throw new IllegalArgumentException(
					"Owners master private and public key do not match!");
		}

		shareMetaData = new ShareMetaData(db, ownerPubSigKey);
		
		try {
			this.shareMetaData.load();
		} catch (InitializaionException e) {
			throw new ShareMetaDataException(
					"Could not initialize ShareMetaData", e);
		} catch (SignatureException e) {
			throw new ShareMetaDataException("Could not verify signature", e);
		} catch (DeviceListException e) {
			throw new ShareMetaDataException("Could not verify signature", e);
		}

		// Init Participants list
		SharePartList spl = shareMetaData.initSharePartList();
		spl.add(ownerPubSigKey, ownerAlias);

		// Init DeviceList
		HashMap<String, PublicKey> dkList = new HashMap<String, PublicKey>();
		dkList.put(IVolume.MASTER_KEY, ownerPubEncKey);
		dkList.put(deviceAlias, devicePubKey);
		DeviceList devices = shareMetaData.createDeviceList(ownerPubSigKey,
				dkList);

		// Create ObfuscationKey
		try {
			shareMetaData.addObfuscationKey(ownerPubEncKey, null, null);
			shareMetaData.addObfuscationKey(ownerPubEncKey, ownerPrivEncKey,
					devicePubKey);
		} catch (SymmetricKeyEncryptionException
				| SymmetricKeyDecryptionException e) {
			throw new ShareMetaDataException(
					"Could not initialize obfuscation key", e);
		}

		// Create initial ShareKey
		ShareKeyDB shareKeys = shareMetaData.getShareKeys();
		SecretKey shareKey = CryptCore.generateSymmetricKey();
		try {
			shareKeys.add(shareKey, dkList.values());
		} catch (SymmetricKeyEncryptionException e1) {
			throw new ShareMetaDataException(
					"Could not encrypt sharekey for device", e1);
		}
		try {
			spl.sign(ownerPrivSigKey);
		} catch (SignatureException e2) {
			throw new ShareMetaDataException("Could not sign SharePartList", e2);
		}

		try {
			devices.sign(ownerPrivSigKey, shareKeys,
					shareMetaData.obfuscationKeys);
		} catch (SignatureException e2) {
			throw new ShareMetaDataException("Could not sign DeviceList", e2);
		}

		shareMetaData.persist();
		shareMetaData.persist(devices);
		return shareMetaData;
	}

	@Override
	public byte[] getEncryptedObfuscationKey(PublicKey devicePubKey)
			throws SymmetricKeyNotFoundException {
		byte[] result = this.shareMetaData
				.getEncryptedObfuscationKey(devicePubKey);
		if (result == null) {
			throw new SymmetricKeyNotFoundException(
					SymmetricKeyNotFoundException.OBFUSCATION, devicePubKey);
		}
		return result;
	}

	@Override
	public EncryptedShareKey getEncryptedShareKey(int version,
			PublicKey devicePubKey) throws SymmetricKeyNotFoundException {
		EncryptedShareKey result = this.shareMetaData.getEncryptedShareKey(
				version, devicePubKey);
		if (result == null) {
			throw new SymmetricKeyNotFoundException(
					SymmetricKeyNotFoundException.FILE_ENCRYPTION, devicePubKey);
		}
		return result;
	}

	@Override
	public EncryptedShareKey getLatestEncryptedShareKey(PublicKey devicePubKey)
			throws SymmetricKeyNotFoundException {
		EncryptedShareKey result = this.shareMetaData
				.getLatestEncryptedShareKey(devicePubKey);
		if (result == null) {
			throw new SymmetricKeyNotFoundException(
					SymmetricKeyNotFoundException.FILE_ENCRYPTION, devicePubKey);
		}
		return result;
	}

	@Override
	public void addDevice(VolumeParams p) throws IllegalArgumentException,
			ShareMetaDataException {
		this.addDevice(p.userAlias, p.pubSigKey, p.privSigKey, p.deviceAlias,
				p.deviceKey, p.pubEncKey, p.privEncKey);
	}

	@Override
	public void addDevice(String alias, PublicKey masterPubSigKey,
			PrivateKey masterPrivSigKey, String newDeviceAlias,
			PublicKey newDevicePubKey, PublicKey masterPubEncKey,
			PrivateKey masterPrivEncKey) throws ShareMetaDataException {

		if (IVolume.MASTER_KEY.equals(newDeviceAlias)) {
			throw new IllegalArgumentException(
					"Illegal alias for device. Choose another alias");
		}

		// verify matching public/private keys
		if (!Utils.keysMatch(masterPubSigKey, masterPrivSigKey)) {
			throw new IllegalArgumentException(
					"User's master private and public signature keys do not match!");
		}
		if (!Utils.keysMatch(masterPubEncKey, masterPrivEncKey)) {
			throw new IllegalArgumentException(
					"User's master private and public encryption keys do not match!");
		}

		// verify integrity of ShareParticipantList
		SharePartList sharePartList = shareMetaData.getSharePartList();
		try {
			SignatureHelper.verify(sharePartList, sharePartList.getSignature(),
					shareMetaData.ownerPubSigKey);
		} catch (InvalidKeyException | NoSuchAlgorithmException
				| SignatureException | SerializationException e) {
			throw new ShareMetaDataException(
					"Could not verify ShareParticipantsList signature", e);
		}

		// check if masterPubSigKey is in ShareParticipants
		if (!sharePartList.getPublicKey(alias).equals(masterPubSigKey)) {
			throw new ShareMetaDataException(
					"Given user singature publickey is not "
							+ "in sharepartiticapnts list");
		}

		// Get DeviceList for user
		DeviceList deviceList = shareMetaData.getDeviceLists().get(
				masterPubSigKey);
		if (deviceList == null) {
			throw new ShareMetaDataException("DeviceList for user " + alias
					+ " was empty, which should never be the case.");
		}

		// add device
		deviceList.addDevice(newDeviceAlias, newDevicePubKey);

		// add encrypted Obfuscation key for new device
		try {
			shareMetaData.addObfuscationKey(masterPubEncKey, masterPrivEncKey,
					newDevicePubKey);
		} catch (SymmetricKeyEncryptionException
				| SymmetricKeyDecryptionException e) {
			throw new ShareMetaDataException(
					"Could not add encrypted obfuscation key for new device", e);
		}

		// add encrypted sharekey for device
		try {
			this.shareMetaData.shareKeys.addDevice(masterPubEncKey,
					masterPrivEncKey, newDevicePubKey);
		} catch (Exception e) {
			throw new ShareMetaDataException(
					"Could not add encrypted share keys for new device", e);
		}
		// Sign everything
		try {
			deviceList.sign(masterPrivSigKey, shareMetaData.shareKeys,
					shareMetaData.obfuscationKeys);
		} catch (SignatureException e) {
			throw new ShareMetaDataException("Could not sign devicelist", e);
		}

		this.shareMetaData.persist(deviceList);
	}

	@Override
	public void removeDevice(VolumeParams p) throws IllegalArgumentException,
			ShareMetaDataException {
		this.removeDevice(p.userAlias, p.pubSigKey, p.privSigKey, p.deviceAlias);
	}

	@Override
	public void removeDevice(String alias, PublicKey masterPubSigKey,
			PrivateKey masterPrivSigKey, String deviceAlias)
			throws ShareMetaDataException {

		throw new ShareMetaDataException("Not yet implemented");
		/*
		 * // Check name of device that should be removed if
		 * (IVolume.MASTER_KEY.equals(deviceAlias)) { throw new
		 * IllegalArgumentException(
		 * "Illegal alias for device. Choose another alias"); }
		 * 
		 * // verify matching public/private keys if
		 * (!Utils.keysMatch(masterPubSigKey, masterPrivSigKey)) { throw new
		 * ShareMetaDataException(
		 * "Owners master private and public key do not match!"); }
		 * 
		 * // check if masterPubSigKey is in ShareParticipants if
		 * (!shareMetaData.getSharePartList().getPublicKey(alias)
		 * .equals(masterPubSigKey)) { throw new ShareMetaDataException(
		 * "Given master singature publickey is not " +
		 * "in sharepartiticapnts list"); }
		 * 
		 * // Get DeviceList for user DeviceList deviceList =
		 * this.shareMetaData.getDeviceLists().get( masterPubSigKey);
		 * 
		 * if (deviceList == null || deviceList.getPublicKey(deviceAlias) ==
		 * null) { // Device wasn't in list... exit method here return; } //
		 * verify existing list and add device boolean verified = false; try {
		 * verified = SignatureHelper.verify(deviceList,
		 * deviceList.getSignature(), masterPubSigKey); } catch (Exception e) {
		 * throw new ShareMetaDataException(
		 * "Could not verify DeviceList signature", e); } if (!verified) { throw
		 * new ShareMetaDataException(
		 * "Could not verify DeviceList with the given PublicKey", new
		 * SignatureException( "DeviceList signature verification failed")); }
		 * PublicKey deviceKey = deviceList.removeDevice(deviceAlias);
		 * 
		 * try { deviceList.sign(masterPrivSigKey); } catch (SignatureException
		 * e) { throw new ShareMetaDataException("Could not sign devicelist",
		 * e); }
		 * 
		 * // remove encrypted Obfuscation key for new device byte[] obKey =
		 * shareMetaData.removeObfuscationKey(deviceKey); if (obKey != null) {
		 * try { this.shareMetaData.signObfuscationKeys(masterPrivSigKey,
		 * alias); } catch (SignatureException e) { throw new
		 * ShareMetaDataException( "Could not sign obfuscation keys", e); } }
		 * 
		 * // remove encrypted sharekey for device
		 * this.shareMetaData.shareKeys.removeDevice(deviceKey);
		 * 
		 * // add new sharekey version for the share try {
		 * this.shareMetaData.shareKeys.add(CryptCore.generateSymmetricKey()); }
		 * catch (SymmetricKeyEncryptionException e) { throw new
		 * ShareMetaDataException(
		 * "Could not add new sharekey version to sharekeydb.", e); }
		 * 
		 * try { this.shareMetaData.shareKeys.sign(masterPrivSigKey, alias); }
		 * catch (SignatureException e) { throw new
		 * ShareMetaDataException("Could not sign sharekeys", e); }
		 * 
		 * try { this.shareMetaData.sign(masterPrivSigKey, alias); } catch
		 * (SignatureException e) { throw new
		 * ShareMetaDataException("Could not sign share metadata", e); }
		 * this.shareMetaData.persist();
		 */
	}

	@Override
	public void inviteUser(VolumeParams p) throws ShareMetaDataException {
		this.inviteUser(p.ownerAlias, p.privSigKey, p.pubEncKey, p.privEncKey,
				p.userAlias, p.otherEncKey, p.otherSigKey);
	}

	@Override
	public void inviteUser(String ownerAlias, PrivateKey ownerPrivSigKey,
			PublicKey ownerPubEncKey, PrivateKey ownerPrivEncKey, String alias,
			PublicKey userPubEncKey, PublicKey userPubSigKey)
			throws ShareMetaDataException {

		// Check if called by owner
		if (!Utils.keysMatch(shareMetaData.ownerPubSigKey, ownerPrivSigKey)) {
			throw new ShareMetaDataException(
					"This method can only be called by the share owner!");
		}

		SharePartList spl = shareMetaData.getSharePartList();
		// Check if user alias already exists
		if (spl.getPublicKey(alias) == null) {
			spl.add(userPubSigKey, alias);
		}

		// Create deviceList and sign for user
		HashMap<String, PublicKey> dkList = new HashMap<String, PublicKey>();
		dkList.put(IVolume.MASTER_KEY, userPubEncKey);
		DeviceList devices = shareMetaData.createDeviceList(userPubSigKey,
				dkList);

		try {
			shareMetaData.addObfuscationKey(ownerPubEncKey, ownerPrivEncKey,
					userPubEncKey);
		} catch (SymmetricKeyEncryptionException
				| SymmetricKeyDecryptionException e) {
			throw new ShareMetaDataException(
					"Could not add encrypted obfuscation key for new user", e);
		}

		// add encrypted sharekey for device
		try {
			this.shareMetaData.shareKeys.addDevice(ownerPubEncKey,
					ownerPrivEncKey, userPubEncKey);
		} catch (Exception e) {
			throw new ShareMetaDataException(
					"Could not add encrypted share keys for new user", e);
		}

		// Sign everything
		try {
			spl.sign(ownerPrivSigKey);
		} catch (SignatureException e) {
			throw new ShareMetaDataException(
					"Could not add new user to ShareParticipantList.", e);
		}

		try {
			devices.sign(ownerPrivSigKey, shareMetaData.shareKeys,
					shareMetaData.obfuscationKeys);
		} catch (SignatureException e) {
			throw new ShareMetaDataException("Could not sign devicelist", e);
		}

		this.shareMetaData.persist();
		this.shareMetaData.persist(devices);
	}

	@Override
	public ShareMetaData acceptInvitation(VolumeParams p)
			throws ShareMetaDataException {
		return this.acceptInvitation(p.ownerSigKey, p.userAlias, p.pubSigKey,
				p.privSigKey, p.deviceAlias, p.deviceKey, p.pubEncKey,
				p.privEncKey);
	}

	@Override
	public ShareMetaData acceptInvitation(PublicKey ownerPubSigKey,
			String alias, PublicKey masterPubSigKey,
			PrivateKey masterPrivSigKey, String deviceAlias,
			PublicKey newDevicePubKey, PublicKey masterPubEncKey,
			PrivateKey masterPrivEncKey) throws ShareMetaDataException {

		loadShareMetaData(ownerPubSigKey);
		addDevice(alias, masterPubSigKey, masterPrivSigKey, deviceAlias,
				newDevicePubKey, masterPubEncKey, masterPrivEncKey);
		return this.shareMetaData;
	}

	@Override
	public void removeUser(String ownerAlias, PublicKey ownerPubSigKey,
			PrivateKey ownerPrivSigKey, String userAlias)
			throws ShareMetaDataException {
		// TODO Auto-generated method stub
		throw new ShareMetaDataException("Not yet implemented");
	}

	@Override
	public HashMap<PublicKey, String> getShareParticipants() {
		HashMap<PublicKey, String> result = new HashMap<PublicKey, String>(
				this.shareMetaData.shareParticipants.size());
		Iterator<String> it = this.shareMetaData.shareParticipants.getAliases();
		while (it.hasNext()) {
			String alias = (String) it.next();
			PublicKey key = this.shareMetaData.shareParticipants
					.getPublicKey(alias);
			result.put(key, alias);
		}
		return result;
	}

	@Override
	public HashMap<PublicKey, String> getDeviceMap(PublicKey pubSigKey) {
		DeviceList list = this.shareMetaData.deviceLists.get(pubSigKey);
		HashMap<PublicKey, String> result = new HashMap<>(list.size());
		Iterator<String> it = list.getAliasIterator();
		while (it.hasNext()) {
			String alias = (String) it.next();
			if (!IVolume.MASTER_KEY.equals(alias)) {
				result.put(list.getPublicKey(alias), alias);
			}
		}
		return result;
	}

	@Override
	public PublicKey getOwnerKey() {
		return this.shareMetaData.ownerPubSigKey;
	}

	@Override
	public void reload() throws ShareMetaDataException {
		PublicKey ownerKey = this.shareMetaData.ownerPubSigKey;
		this.shareMetaData = null;
		this.loadShareMetaData(ownerKey);
	}
}
