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
import java.security.PublicKey;
import java.security.SignatureException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.TreeMap;
import java.util.UUID;

import org.panbox.core.Utils;
import org.panbox.core.crypto.CryptCore;
import org.panbox.core.crypto.KeyConstants;
import org.panbox.core.crypto.Signable;
import org.panbox.core.crypto.SignatureHelper;
import org.panbox.core.exception.InitializaionException;
import org.panbox.core.exception.PersistanceException;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

public class AndroidJDBCHelperNonRevokeable implements DBHelper {

	private final String splPath;
	private final String devListPath;
	private final PublicKey masterPubKey;

	public AndroidJDBCHelperNonRevokeable(String splPath, String devListPath,
			PublicKey masterPubKey) {
		Log.i(AndroidJDBCHelperNonRevokeable.class.getName(),
				"Creating android database handler for sharemetadata");
		this.splPath = splPath;
		this.devListPath = devListPath;
		this.masterPubKey = masterPubKey;
	}

	@Override
	public void init(ShareMetaData smd) throws InitializaionException,
			SignatureException {

		SQLiteDatabase dbSPL = SQLiteDatabase.openOrCreateDatabase(splPath,
				null);
		SQLiteDatabase dbDL = SQLiteDatabase.openOrCreateDatabase(devListPath,
				null);

		try {
			dbSPL.beginTransactionNonExclusive();
			SQLiteStatement s = dbSPL.compileStatement(SPL_HAS_TABLES);
			int tables = (int) s.simpleQueryForLong();
			s.close();
			if (SPL_NUM_TABLES == tables) {
				Log.d(AndroidJDBCHelperNonRevokeable.class.getName(),
						"Tables exist, loading values...");
				loadSPLValues(dbSPL, smd);

				// Load DeviceList
				dbDL.beginTransaction();
				s = dbDL.compileStatement(DEVICELIST_HAS_TABLES);
				tables = (int) s.simpleQueryForLong();
				s.close();
				if (DEVICELIST_NUM_TABLES == tables) {
					Log.d(AndroidJDBCHelperNonRevokeable.class.getName(),
							"DeviceList exists, loading values...");
					loadDeviceList(dbDL, smd);
				}
				dbDL.setTransactionSuccessful();
			}
			dbSPL.setTransactionSuccessful();

		} finally {
			dbSPL.endTransaction();
			dbSPL.close();
			dbDL.endTransaction();
			dbDL.close();
		}
	}

	private void loadSharePaticipants(SQLiteDatabase dbSPL, ShareMetaData smd)
			throws InitializaionException, SignatureException {

		smd.shareParticipants = new SharePartList();

		// Load Publickeys of participants
		Cursor cursor = dbSPL.rawQuery(QUERY_SPL, null);
		try {
			while (cursor.moveToNext()) {
				PublicKey key = CryptCore.createPublicKeyFromBytes(cursor
						.getBlob(cursor.getColumnIndex(COL_PUB_KEY)));
				smd.shareParticipants.add(key,
						cursor.getString(cursor.getColumnIndex(COL_ALIAS)));
			}
			cursor.close();

			// Load and verify signature
			cursor = dbSPL.rawQuery(QUERY_SIGNATURE, null);
			while (cursor.moveToNext()) {
				byte[] signature = cursor.getBlob(cursor
						.getColumnIndex(COL_SIGNATURE));
				boolean verified = false;
				try {
					verified = SignatureHelper.verify(smd.shareParticipants,
							signature, smd.ownerPubSigKey);
					smd.shareParticipants.setSignature(signature);
				} catch (Exception e) {
				}
				if (!verified) {
					throw new SignatureException(
							"ShareParticipantList signature could not be verified!");
				}
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private void loadDeviceList(SQLiteDatabase dbDL, ShareMetaData smd)
			throws SignatureException {

		smd.deviceLists = new TreeMap<PublicKey, DeviceList>(
				Utils.PK_COMPARATOR);

		DeviceList list = smd.deviceLists.get(masterPubKey);
		if (list == null) {
			list = new DeviceList(masterPubKey, null);
			smd.deviceLists.put(masterPubKey, list);
		}

		Cursor cursor = dbDL.rawQuery(QUERY_DEVICE_LIST, null);
		while (cursor.moveToNext()) {
			String device = cursor.getString(cursor
					.getColumnIndex(COL_DEV_ALIAS));
			Log.d(AndroidJDBCHelperNonRevokeable.class.getName(),
					"Loading device: " + device);
			PublicKey devicePubKey = CryptCore.createPublicKeyFromBytes(cursor
					.getBlob(cursor.getColumnIndex(COL_DEV_PUB_KEY)));
			list.addDevice(device, devicePubKey);
		}
		cursor.close();

		loadKeyValues(dbDL, smd);

		checkDeviceListSignature(dbDL, smd, list);

	}

	private void checkDeviceListSignature(SQLiteDatabase dbDL,
			ShareMetaData smd, DeviceList list) throws SignatureException {
		// Check Signature
		if (list != null) {
			Collection<PublicKey> pKeys = list.getPublicKeys();
			byte[] signature = getDeviceListSignature(dbDL);
			if (signature == null) {
				Log.e(AndroidJDBCHelperNonRevokeable.class.getName(),
						"No signature for devicelist found");
				throw new SignatureException(
						"No signature for devicelist found");
			}
			boolean verified = false;
			try {
				// Either signed by the device list owner or by the
				// shareOwner
				Signable sKeys = smd.shareKeys.get(pKeys);
				Signable obKeys = smd.obfuscationKeys.get(pKeys);

				verified = SignatureHelper.verify(signature, masterPubKey,
						list, sKeys, obKeys);
				if (!verified) {
					verified = SignatureHelper.verify(signature,
							smd.ownerPubSigKey, list, sKeys, obKeys);
				}
			} catch (Exception e) {
				throw new SignatureException("Could not verify signature", e);
			}
			if (!verified) {
				Log.e(AndroidJDBCHelperNonRevokeable.class.getName(),
						"Could not verify devicelist");
				throw new SignatureException("Could not verify devicelist");
			}
			list.setSignature(signature);
		}
	}

	private byte[] getDeviceListSignature(SQLiteDatabase dbDL)
			throws SignatureException {
		Cursor cursor = dbDL.rawQuery(QUERY_SIGNATURE, null);
		if (cursor.moveToNext()) {
			byte[] signature = cursor.getBlob(cursor
					.getColumnIndex(COL_SIGNATURE));
			if (cursor.moveToNext()) {
				Log.e(AndroidJDBCHelperNonRevokeable.class.getName(),
						"More than one device list signature found");
				cursor.close();
				throw new SignatureException(
						"More than one device list signature found");
			}
			cursor.close();
			return signature;
		} else {
			cursor.close();
			throw new SignatureException("No signature found for device list");
		}
	}

	private void loadKeyValues(SQLiteDatabase dbDL, ShareMetaData smd) {
		loadShareKeys(dbDL, smd);
		loadObfuscationKeys(dbDL, smd);
	}

	private void loadObfuscationKeys(SQLiteDatabase dbDL, ShareMetaData smd) {
		if (smd.obfuscationKeys == null) {
			smd.obfuscationKeys = new ObfuscationKeyDB();
		}

		Cursor cursor = dbDL.rawQuery(QUERY_OBFUSCATION_KEYS, null);
		while (cursor.moveToNext()) {
			PublicKey key = CryptCore.createPublicKeyFromBytes(cursor
					.getBlob(cursor.getColumnIndex(COL_DEV_PUB_KEY)));
			byte[] encKey = cursor.getBlob(cursor.getColumnIndex(COL_ENC_KEY));
			smd.obfuscationKeys.add(key, encKey);
		}
		cursor.close();
	}

	private void loadShareKeys(SQLiteDatabase dbDL, ShareMetaData smd) {
		if (smd.shareKeys == null) {
			smd.shareKeys = new ShareKeyDB();
		}

		Cursor cursor = dbDL.rawQuery(QUERY_SHARE_KEYS, null);
		while (cursor.moveToNext()) {
			PublicKey key = CryptCore.createPublicKeyFromBytes(cursor
					.getBlob(cursor.getColumnIndex(COL_DEV_PUB_KEY)));
			int keyID = cursor.getInt(cursor.getColumnIndex(COL_KEY_ID));
			byte[] encKey = cursor.getBlob(cursor.getColumnIndex(COL_ENC_KEY));
			ShareKeyDBEntry entry = smd.shareKeys.getEntry(keyID);
			if (entry == null) {
				Log.d(AndroidJDBCHelperNonRevokeable.class.getName(),
						"Creating new ShareKeyEntry: " + keyID);
				entry = new ShareKeyDBEntry(smd.shareKeyAlgorithm, keyID);
				smd.shareKeys.add(keyID, entry);
			}
			Log.d(AndroidJDBCHelperNonRevokeable.class.getName(),
					"Adding ShareKey to entry " + keyID);
			entry.addEncryptedKey(encKey, key);
			Log.d(AndroidJDBCHelperNonRevokeable.class.getName(),
					"Number of keys in entry: " + entry.size());
		}
		cursor.close();
	}

	private void loadSPLValues(SQLiteDatabase dbSPL, ShareMetaData smd)
			throws InitializaionException, SignatureException {
		loadID(dbSPL, smd);
		loadAlgorithms(dbSPL, smd);
		loadSharePaticipants(dbSPL, smd);

	}

	private void loadAlgorithms(SQLiteDatabase db, ShareMetaData smd)
			throws InitializaionException {
		smd.publicKeyAlgorithm = loadMetadata(db, smd, KEY_PK_ALGO);
		if (smd.publicKeyAlgorithm == null) {
			smd.publicKeyAlgorithm = KeyConstants.ASYMMETRIC_ALGORITHM;
		}
		smd.shareKeyAlgorithm = loadMetadata(db, smd, KEY_SK_ALGO);
		if (smd.shareKeyAlgorithm == null) {
			smd.shareKeyAlgorithm = KeyConstants.SYMMETRIC_ALGORITHM;
		}
		smd.obfuscationKeyAlgorithm = loadMetadata(db, smd, KEY_OK_ALGO);
		if (smd.obfuscationKeyAlgorithm == null) {
			smd.obfuscationKeyAlgorithm = KeyConstants.SYMMETRIC_ALGORITHM;
		}
	}

	private void loadID(SQLiteDatabase db, ShareMetaData smd)
			throws InitializaionException {
		smd.id = UUID.fromString(loadMetadata(db, smd, KEY_UUID));
	}

	private String loadMetadata(SQLiteDatabase db, ShareMetaData smd, String key)
			throws InitializaionException {
		String result = null;
		Cursor cursor = db.rawQuery(QUERY_METADATA, new String[] { key });
		int count = cursor.getCount();
		if (count == 0) {
			throw new InitializaionException("Could not find " + key
					+ " in metadata database! DB might be corrupt.");
		} else {
			cursor.moveToFirst();
			result = cursor.getString(cursor.getColumnIndex(COL_VALUE));
		}
		cursor.close();
		return result;
	}

	@Override
	public void store(DeviceList deviceList, ObfuscationKeyDB obKeys,
			ShareKeyDB shareKeys) throws PersistanceException {
		throw new RuntimeException("Not yet implemented");
	}

	@Override
	public void storeSPL(ShareMetaData smd) throws PersistanceException {
		throw new RuntimeException("Not yet implemented");
	}

	@Override
	public void load(ShareMetaData smd) throws SQLException,
			SignatureException, InitializaionException {
		smd.shareParticipants = null;
		smd.deviceLists.clear();
		smd.deviceLists = null;
		smd.shareKeys = null;
		smd.obfuscationKeys = null;
		init(smd);
	}

	@Override
	public boolean exists() {
		return new File(splPath).isFile() && new File(devListPath).isFile();
	}

}
