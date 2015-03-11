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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.panbox.core.Utils;
import org.panbox.core.crypto.CryptCore;
import org.panbox.core.crypto.KeyConstants;
import org.panbox.core.crypto.Signable;
import org.panbox.core.crypto.SignatureHelper;
import org.panbox.core.exception.DeviceListException;
import org.panbox.core.exception.InitializaionException;
import org.panbox.core.exception.PersistanceException;
import org.sqlite.SQLiteErrorCode;

public class JDBCHelperNonRevokeable implements DBHelper {

	private static final Logger logger = Logger
			.getLogger(JDBCHelperNonRevokeable.class);

	private final String dbURL;

	private static final Properties p = new Properties();

	static {
		p.setProperty("journal_mode", "MEMORY");
	}

	public JDBCHelperNonRevokeable(String sqLitepath) {
		this.dbURL = sqLitepath;
	}

	@Override
	public void init(ShareMetaData smd) throws InitializaionException,
			SignatureException, DeviceListException {
		load(smd);
	}

	private void loadSPLValues(Connection con, ShareMetaData smd)
			throws SQLException, InitializaionException, SignatureException {
		loadID(con, smd);
		loadAlgorithms(con, smd);
		loadSharePaticipants(con, smd);
	}

	private void loadAlgorithms(Connection con, ShareMetaData smd)
			throws SQLException, InitializaionException {
		smd.publicKeyAlgorithm = loadMetadata(con, KEY_PK_ALGO);
		if (smd.publicKeyAlgorithm == null) {
			smd.publicKeyAlgorithm = KeyConstants.ASYMMETRIC_ALGORITHM;
		}
		smd.shareKeyAlgorithm = loadMetadata(con, KEY_SK_ALGO);
		if (smd.shareKeyAlgorithm == null) {
			smd.shareKeyAlgorithm = KeyConstants.SYMMETRIC_ALGORITHM;
		}
		smd.obfuscationKeyAlgorithm = loadMetadata(con, KEY_OK_ALGO);
		if (smd.obfuscationKeyAlgorithm == null) {
			smd.obfuscationKeyAlgorithm = KeyConstants.SYMMETRIC_ALGORITHM;
		}
	}

	private void loadSharePaticipants(Connection con, ShareMetaData smd)
			throws SQLException, SignatureException {

		smd.shareParticipants = new SharePartList();

		// Load Publickeys of participants
		PreparedStatement s = con.prepareStatement(QUERY_SPL);
		ResultSet rs = s.executeQuery();
		while (rs.next()) {
			PublicKey key = CryptCore.createPublicKeyFromBytes(rs
					.getBytes(COL_PUB_KEY));
			smd.shareParticipants.add(key, rs.getString(COL_ALIAS));
		}
		rs.close();

		// Load and verify signature
		s = con.prepareStatement(QUERY_SIGNATURE);
		rs = s.executeQuery();
		while (rs.next()) {
			byte[] signature = rs.getBytes(COL_SIGNATURE);
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
		rs.close();
		s.close();
	}

	private void loadID(Connection con, ShareMetaData smd) throws SQLException,
			InitializaionException {
		smd.id = UUID.fromString(loadMetadata(con, KEY_UUID));
	}

	protected String loadMetadata(Connection con, String key)
			throws SQLException, InitializaionException {
		String result = null;
		PreparedStatement s = con.prepareStatement(QUERY_METADATA);
		s.setString(1, key);

		ResultSet rs = s.executeQuery();
		if (rs.next()) {
			result = rs.getString(COL_VALUE);
		} else {
			throw new InitializaionException("Could not find " + key
					+ " in database! DB might be corrupt.");
		}
		rs.close();
		return result;
	}

	protected void initSPL(ShareMetaData smd) throws SQLException,
			InitializaionException, SignatureException {
		Connection con = DriverManager
				.getConnection(dbURL + Volume.SPL_FILE, p);
		try {
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery(SPL_HAS_TABLES);
			if (rs.next() && SPL_NUM_TABLES == rs.getInt(1)) {
				logger.debug("Tables exist, loading values...");
				loadSPLValues(con, smd);
			} else {
				logger.debug("new Volume, creating tables...");
				createSPLTables(s, smd);
			}
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					logger.warn("Could not close DataBase Connection", e);
				}
			}
		}
	}

	private void createSPLTables(Statement s, ShareMetaData smd)
			throws SQLException {
		// Create generall metadata table
		s.executeUpdate("drop table if exists " + TABLE_METADATA + ";");
		s.executeUpdate("create table " + TABLE_METADATA + " (" + COL_KEY
				+ " string, " + COL_VALUE + " string);");

		s.executeUpdate("insert into " + TABLE_METADATA + " values('"
				+ KEY_UUID + "', '" + smd.id.toString() + "')");
		// Set database version: this can be used to determine whether a
		// migration of data is needed
		s.executeUpdate("insert into " + TABLE_METADATA + " values('"
				+ KEY_DB_VERSION + "', '" + Integer.toString(DB_VERSION) + "')");

		// Create shareparticipants table
		s.executeUpdate("drop table if exists " + TABLE_SHARE_PARTICIPANTS
				+ ";");
		s.executeUpdate(CREATE_SPL);

		// Create signatures table
		s.executeUpdate("drop table if exists " + TABLE_SIGNATURES + ";");
		s.executeUpdate("create table " + TABLE_SIGNATURES + " ("
				+ COL_SIGNATURE + " blob);");

	}

	@Override
	public void load(ShareMetaData smd) throws SignatureException,
			InitializaionException, DeviceListException {
		try {
			initSPL(smd);
			initDeviceLists(smd);
		} catch (SQLException e) {
			throw new InitializaionException("Could not load Share Meta Data",
					e);
		}
	}

	private void loadShareKeys(Connection con, ShareMetaData smd)
			throws SQLException, SignatureException {
		if (smd.shareKeys == null) {
			smd.shareKeys = new ShareKeyDB();
		}

		PreparedStatement s = con.prepareStatement(QUERY_SHARE_KEYS);
		ResultSet rs = s.executeQuery();
		while (rs.next()) {
			PublicKey key = CryptCore.createPublicKeyFromBytes(rs
					.getBytes(COL_DEV_PUB_KEY));
			int keyID = rs.getInt(COL_KEY_ID);
			byte[] encKey = rs.getBytes(COL_ENC_KEY);
			ShareKeyDBEntry entry = smd.shareKeys.getEntry(keyID);
			if (entry == null) {
				logger.debug("Creating new ShareKeyEntry: " + keyID);
				entry = new ShareKeyDBEntry(smd.shareKeyAlgorithm, keyID);
				smd.shareKeys.add(keyID, entry);
			}
			logger.debug("Adding ShareKey to entry " + keyID + " for key "
					+ DigestUtils.md2Hex(key.getEncoded()));
			entry.addEncryptedKey(encKey, key);
			logger.debug("Number of keys in entry: " + entry.size());
		}
		rs.close();
	}

	private void loadObfuscationKeys(Connection con, ShareMetaData smd)
			throws SQLException, SignatureException {
		if (smd.obfuscationKeys == null) {
			smd.obfuscationKeys = new ObfuscationKeyDB();
		}
		PreparedStatement s = con.prepareStatement(QUERY_OBFUSCATION_KEYS);
		ResultSet rs = s.executeQuery();
		while (rs.next()) {
			PublicKey key = CryptCore.createPublicKeyFromBytes(rs
					.getBytes(COL_DEV_PUB_KEY));
			byte[] encKey = rs.getBytes(COL_ENC_KEY);
			smd.obfuscationKeys.add(key, encKey);
		}
		rs.close();
	}

	protected void loadKeyValues(Connection con, ShareMetaData smd)
			throws SQLException, InitializaionException, SignatureException {
		loadShareKeys(con, smd);
		loadObfuscationKeys(con, smd);
	}

	private void loadDeviceList(Connection con, ShareMetaData smd,
			PublicKey masterPubKey) throws SQLException, SignatureException,
			NumberFormatException, InitializaionException {

		DeviceList list = smd.deviceLists.get(masterPubKey);
		if (list == null) {
			list = new DeviceList(masterPubKey, null);
			smd.deviceLists.put(masterPubKey, list);
		}

		PreparedStatement s = con.prepareStatement(QUERY_DEVICE_LIST);
		ResultSet rs = s.executeQuery();
		while (rs.next()) {
			String device = rs.getString(COL_DEV_ALIAS);
			logger.debug("Loading device: " + device);
			PublicKey devicePubKey = CryptCore.createPublicKeyFromBytes(rs
					.getBytes(COL_DEV_PUB_KEY));
			list.addDevice(device, devicePubKey);
		}
		rs.close();
		s.close();

		loadKeyValues(con, smd);

		loadDeviceListSignature(con, smd, masterPubKey, list);
	}

	protected byte[] getDeviceListSignature(Connection con)
			throws SQLException, SignatureException {
		PreparedStatement s = con.prepareStatement(QUERY_SIGNATURE);
		ResultSet rs = s.executeQuery();
		if (rs.next()) {
			byte[] result = rs.getBytes(COL_SIGNATURE);
			if (rs.next()) {
				logger.error("More than one device list signature found");
				throw new SignatureException(
						"More than one device list signature found");
			}
			rs.close();
			s.close();
			return result;
		} else {
			rs.close();
			s.close();
			// throw new
			// SignatureException("No signature found for device list");
			return null;
		}
	}

	private void loadDeviceListSignature(Connection con, ShareMetaData smd,
			PublicKey masterPubKey, DeviceList list) throws SQLException,
			SignatureException {
		// Check Signature
		if (list != null) {
			byte[] signature = getDeviceListSignature(con);
			list.setSignature(signature);
		}
	}

	private void verifyDeviceList(ShareMetaData smd, PublicKey masterPubKey,
			DeviceList list) throws SignatureException {
		Collection<PublicKey> pKeys = list.getPublicKeys();
		byte[] signature = list.getSignature();
		if (signature == null) {
			logger.fatal("No signature for devicelist found");
			throw new SignatureException("No signature for devicelist found");
		}
		boolean verified = false;
		try {
			// Either signed by the device list owner or by the
			// shareOwner
			Signable sKeys = smd.shareKeys.get(pKeys);
			Signable obKeys = smd.obfuscationKeys.get(pKeys);

			verified = SignatureHelper.verify(signature, masterPubKey, list,
					sKeys, obKeys);
			if (!verified) {
				verified = SignatureHelper.verify(signature,
						smd.ownerPubSigKey, list, sKeys, obKeys);
			}
		} catch (Exception e) {
			throw new SignatureException("Could not verify signature", e);
		}
		if (!verified) {
			logger.fatal("Could not verify devicelist");
			throw new SignatureException("Could not verify devicelist");
		}
	}

	private void createDLTables(Statement s) throws SQLException {

		// Create generall metadata table, for timestamp
		s.executeUpdate("drop table if exists " + TABLE_METADATA + ";");
		s.executeUpdate("create table " + TABLE_METADATA + " (" + COL_KEY
				+ " string, " + COL_VALUE + " string);");

		// Create shareparticipants table
		s.executeUpdate("drop table if exists " + TABLE_DEVICE_LIST + ";");
		s.executeUpdate(CREATE_DEVICELIST);

		// Create sharekeys table
		s.executeUpdate("drop table if exists " + TABLE_SHARE_KEYS + ";");
		s.executeUpdate(CREATE_SHAREKEYS);

		// Create obfuscationkeys table
		s.executeUpdate("drop table if exists " + TABLE_OBFUSCATION_KEYS + ";");
		s.executeUpdate(CREATE_OBKEYS);

		// Create signatures table
		s.executeUpdate("drop table if exists " + TABLE_SIGNATURES + ";");
		s.executeUpdate("create table " + TABLE_SIGNATURES + " ("
				+ COL_SIGNATURE + " blob);");

	}

	private void initDeviceLists(ShareMetaData smd) throws SQLException,
			InitializaionException, SignatureException, DeviceListException {

		smd.deviceLists = new TreeMap<PublicKey, DeviceList>(
				Utils.PK_COMPARATOR);

		Connection con = null;
		SharePartList spl = smd.getSharePartList();
		if (spl != null) {
			Iterator<String> it = spl.getAliases();

			while (it.hasNext()) {
				String alias = (String) it.next();
				PublicKey pKey = spl.getPublicKey(alias);
				String fingerprint = DigestUtils.sha256Hex(pKey.getEncoded());
				String url = dbURL + fingerprint + ".db";
				try {
					con = DriverManager.getConnection(url, p);
					Statement s = con.createStatement();
					ResultSet rs = s.executeQuery(DEVICELIST_HAS_TABLES);
					if (rs.next() && DEVICELIST_NUM_TABLES == rs.getInt(1)) {
						rs.close();
						s.close();
						logger.debug("Tables exist, loading devicelist for user "
								+ alias);
						loadDeviceList(con, smd, pKey);
					} else {
						rs.close();
						logger.debug("new Volume, creating tables for devicelist for user "
								+ alias);
						createDLTables(s);
						s.close();
					}
				} catch (SQLException e) {
					logger.error("Error reading device list DB", e);
					SQLiteErrorCode code = SQLiteErrorCode.getErrorCode(e
							.getErrorCode());
					if (code.equals(SQLiteErrorCode.SQLITE_NOTADB)
							|| code.equals(SQLiteErrorCode.SQLITE_CORRUPT)) {
						// TODO: corrupt DB, consider deleting .db file
					}
					continue;
				} finally {
					if (con != null) {
						try {
							con.close();
						} catch (SQLException e) {
							logger.warn("Could not close DataBase Connection",
									e);
						}
					}
				}
			}

			// check devicelist signatures
			it = spl.getAliases();
			LinkedList<PublicKey> corruptDeviceList = new LinkedList<>();
			while (it.hasNext()) {
				String alias = (String) it.next();
				PublicKey pKey = spl.getPublicKey(alias);
				DeviceList list = smd.deviceLists.get(pKey);
				try {
					verifyDeviceList(smd, pKey, list);
				} catch (Exception e) {
					logger.warn(
							"Could not verifiy device list of user" + alias, e);
					corruptDeviceList.add(pKey);
					if (list != null) {
						for (PublicKey deviceKey : list.getPublicKeys()) {
							smd.removeObfuscationKey(deviceKey);
							smd.getShareKeys().removeDevice(deviceKey);
						}
					}
				}
			}
			if (!corruptDeviceList.isEmpty()) {
				throw new DeviceListException(
						"Could not verify DeviceList(s)!", corruptDeviceList);
			}
		}
	}

	@Override
	public void store(DeviceList deviceList, ObfuscationKeyDB obKeys,
			ShareKeyDB shareKeys) throws PersistanceException {
		try {
			storeDeviceList(deviceList, obKeys, shareKeys);
		} catch (Exception e) {
			throw new PersistanceException("Could not store DeviceList", e);
		}
	}

	private void storeDeviceList(DeviceList deviceList,
			ObfuscationKeyDB obKeys, ShareKeyDB shareKeys) throws SQLException,
			PersistanceException {
		Connection con = null;
		PublicKey pKey = deviceList.getMasterSignatureKey();
		String fingerprint = DigestUtils.sha256Hex(pKey.getEncoded());
		String url = dbURL + fingerprint + ".db";
		try {
			con = DriverManager.getConnection(url, p);
			storeDeviceList(con, deviceList, pKey);
			Collection<PublicKey> pKeys = deviceList.getPublicKeys();
			logger.debug("User "
					+ DigestUtils.md2Hex(deviceList.getMasterSignatureKey()
							.getEncoded()) + " has " + pKeys.size()
					+ " devices");
			storeKeys(shareKeys.get(pKeys), obKeys.get(pKeys), con);
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					logger.warn("Could not close DataBase Connection", e);
				}
			}
		}
	}

	protected void storeKeys(ShareKeyDB shareKeys, ObfuscationKeyDB obKeys,
			Connection con) throws SQLException {
		// Store ShareKeys
		PreparedStatement insert = con.prepareStatement(INSERT_SHAREKEYS);
		Iterator<Integer> entries = shareKeys.getKeyIterator();
		while (entries.hasNext()) {
			int version = entries.next();
			ShareKeyDBEntry entry = shareKeys.getEntry(version);

			insert.setInt(1, version);

			Iterator<PublicKey> keys = entry.getKeyIterator();
			while (keys.hasNext()) {
				PublicKey pKey = (PublicKey) keys.next();
				byte[] encKey = entry.getEncryptedKey(pKey);
				// Store id, time, pkey enckey;
				insert.setBytes(2, pKey.getEncoded());
				insert.setBytes(3, encKey);
				int count = insert.executeUpdate();
				logger.debug("Inserted " + count + " sharekey");
			}
		}
		insert.close();

		// Store ObKeys
		insert = con.prepareStatement(INSERT_OBFUSCATIONKEYS);
		Iterator<PublicKey> keys = obKeys.getKeys();
		while (keys.hasNext()) {
			PublicKey pKey = (PublicKey) keys.next();
			byte[] encKey = obKeys.get(pKey);
			insert.setBytes(1, pKey.getEncoded());
			insert.setBytes(2, encKey);
			int count = insert.executeUpdate();
			logger.debug("Inserted " + count + " obkey");
			insert.clearParameters();
		}
		insert.close();
	}

	private void storeDeviceList(Connection con, DeviceList deviceList,
			PublicKey pKey) throws SQLException, PersistanceException {

		Statement s = con.createStatement();
		try {
			createDLTables(s);
		} catch (SQLException e) {
			logger.error("Could not create DeviceList tables", e);
		} finally {
			s.close();
		}

		PreparedStatement insert = con.prepareStatement(INSERT_DEVICE_LIST);

		Iterator<String> it = deviceList.getAliasIterator();
		while (it.hasNext()) {
			String devAlias = it.next();
			PublicKey devPubKey = deviceList.getPublicKey(devAlias);
			final byte[] encodedPubKey = devPubKey.getEncoded();
			insert.setString(1, devAlias);
			insert.setBytes(2, encodedPubKey);
			int i = insert.executeUpdate();
			logger.debug("Inserted " + i + " rows of devicelist");
		}

		storeSignature(con, deviceList.getSignature());
		if (insert != null) {
			try {
				insert.close();
			} catch (Exception e) {
				logger.warn("Could not close Statement", e);
			}
		}
	}

	private void storeSignature(Connection con, byte[] signature)
			throws SQLException, PersistanceException {
		PreparedStatement insert = con.prepareStatement(INSERT_SIGNATURE);
		insert.setBytes(1, signature);
		insert.executeUpdate();
		if (insert != null) {
			try {
				insert.close();
			} catch (Exception e) {
				logger.warn("Could not close Statement", e);
			}
		}
	}

	@Override
	public void storeSPL(ShareMetaData smd) throws PersistanceException {

		Connection con = null;
		try {
			con = DriverManager.getConnection(dbURL + Volume.SPL_FILE, p);
			Statement s = con.createStatement();
			createSPLTables(s, smd);
			s.close();

			storeID(con, smd);
			storeAlgorithms(con, smd);
			storeSharePaticipants(con, smd);
		} catch (SQLException e) {
			throw new PersistanceException(
					"Error while trying to store share participants to db", e);
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					logger.warn("Could not close DataBase Connection", e);
				}
			}
		}
	}

	private void storeSharePaticipants(Connection con, ShareMetaData smd)
			throws SQLException, PersistanceException {
		Iterator<String> it = smd.shareParticipants.getAliases();

		Statement s = con.createStatement();
		s.executeUpdate(DROP_SPL);
		s.executeUpdate(CREATE_SPL);
		s.close();
		PreparedStatement insert = con.prepareStatement(INSERT_SPL);
		while (it.hasNext()) {
			String alias = it.next();
			PublicKey pKey = smd.shareParticipants.getPublicKey(alias);
			insert.setString(1, alias);
			insert.setBytes(2, pKey.getEncoded());
			int i = insert.executeUpdate();
			logger.debug("Inserted " + i + " rows of shareparticipants");
			insert.clearParameters();
		}
		if (insert != null) {
			try {
				insert.close();
			} catch (Exception e) {
				logger.warn("Could not close Statement", e);
			}
		}
		storeSignature(con, smd.shareParticipants.getSignature());
	}

	private void storeMetadata(Connection con, String key, String value)
			throws SQLException, PersistanceException {
		PreparedStatement s = con.prepareStatement(UPDATE_METADATA);
		s.setString(1, value);
		s.setString(2, key);
		// Check update
		int rows = s.executeUpdate();
		if (rows == 0) {
			// Update unsuccessful because element was not in db, insert
			// instead
			s.close();
			s = con.prepareStatement(INSERT_METADATA);
			s.setString(1, key);
			s.setString(2, value);
			int i = s.executeUpdate();
			logger.debug("Inserted " + i + " rows of metadata");
			s.close();
		} else if (rows != 1) {
			throw new PersistanceException("Updating metadata entry " + key
					+ " affected more than one row:" + rows);
		}
	}

	private void storeID(Connection con, ShareMetaData smd)
			throws SQLException, PersistanceException {
		storeMetadata(con, KEY_UUID, smd.id.toString());
	}

	private void storeAlgorithms(Connection con, ShareMetaData smd)
			throws SQLException, PersistanceException {
		storeMetadata(con, KEY_PK_ALGO, smd.publicKeyAlgorithm);
		storeMetadata(con, KEY_SK_ALGO, smd.shareKeyAlgorithm);
		storeMetadata(con, KEY_OK_ALGO, smd.obfuscationKeyAlgorithm);
	}

	@Override
	public boolean exists() {
		return new File(this.dbURL).exists();
	}

}
