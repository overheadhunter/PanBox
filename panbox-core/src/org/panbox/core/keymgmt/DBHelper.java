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

import java.security.SignatureException;
import java.sql.SQLException;

import org.panbox.core.exception.DeviceListException;
import org.panbox.core.exception.InitializaionException;
import org.panbox.core.exception.PersistanceException;

interface DBHelper {
	final static int DB_VERSION = 2;
	final static int NUM_BACKUP = 5;

	static final int TYPE_SPL = 1;
	static final int TYPE_DEVLIST = 2;
	static final int TYPE_SK = 3;
	static final int TYPE_OK = 4;
	static final int TYPE_SMD = 0;

	static final String TABLE_SHARE_PARTICIPANTS = "share_participants";
	static final String TABLE_DEVICE_LIST = "devicelist";
	static final String TABLE_SHARE_KEYS = "sharekeys";
	static final String TABLE_OBFUSCATION_KEYS = "obfuscationkeys";
	static final String TABLE_SIGNATURES = "signatures";
	static final String TABLE_METADATA = "metadata";

	static final int SPL_NUM_TABLES = 3;
	static final int DEVICELIST_NUM_TABLES = 3;
	static final int KEYS_NUM_TABLES = 4;

	final static String KEY_UUID = "uuid";
	final static String KEY_DB_VERSION = "db_version";
	final static String KEY_SK_ALGO = "sk_algorithm";
	final static String KEY_OK_ALGO = "ok_algorithm";
	final static String KEY_PK_ALGO = "pk_algorithm";
	final static String KEY_TIMESTAMP = "timestamp";

	final static String COL_SIGNATURE = "sig";
	final static String COL_SIGNER = "signer";
	final static String COL_TYPE = "type";
	final static String COL_KEY = "key";
	final static String COL_KEY_ID = "id";
	final static String COL_VALUE = "value";
	final static String COL_ALIAS = "alias";
	final static String COL_DEV_ALIAS = "device_alias";
	final static String COL_PUB_KEY = "pubkey";
	final static String COL_ENC_KEY = "enckey";
	final static String COL_VERSION = "version";
	final static String COL_DEV_PUB_KEY = "device_key";
	final static String COL_DEV_PUB_KEY_IDX = "device_key_idx";
	final static String COL_ROWID = "ROWID";

	final static String SPL_HAS_TABLES = "SELECT COUNT(name) FROM sqlite_master WHERE type='table' AND name='"
			+ TABLE_SHARE_PARTICIPANTS
			+ "' OR name='"
			+ TABLE_METADATA
			+ "' OR name='" + TABLE_SIGNATURES + "';";

	final static String DEVICELIST_HAS_TABLES = "SELECT COUNT(name) FROM sqlite_master WHERE type='table' AND name='"
			+ TABLE_DEVICE_LIST
			+ "' OR name='"
			+ TABLE_METADATA
			+ "' OR name='" + TABLE_SIGNATURES + "';";

	final static String KEYS_HAS_TABLES = "SELECT COUNT(name) FROM sqlite_master WHERE type='table' AND name='"
			+ TABLE_SHARE_KEYS
			+ "' OR name='"
			+ TABLE_OBFUSCATION_KEYS
			+ "' OR name='"
			+ TABLE_METADATA
			+ "' OR name='"
			+ TABLE_SIGNATURES
			+ "';";

	final static String QUERY_METADATA = "SELECT " + COL_VALUE + " FROM "
			+ TABLE_METADATA + " WHERE " + COL_KEY + "=?;";

	final static String DROP_SPL = "DROP TABLE " + TABLE_SHARE_PARTICIPANTS
			+ ";";
	final static String DROP_DEVICELIST = "DROP TABLE " + TABLE_DEVICE_LIST
			+ ";";
	final static String DROP_OBKEYS = "DROP TABLE " + TABLE_OBFUSCATION_KEYS
			+ ";";
	final static String DROP_SHAREKEYS = "DROP TABLE " + TABLE_SHARE_KEYS + ";";

	final static String CREATE_SPL = "create table " + TABLE_SHARE_PARTICIPANTS
			+ " (" + COL_ALIAS + " string, " + COL_PUB_KEY + " blob);";

	final static String CREATE_DEVICELIST = "create table " + TABLE_DEVICE_LIST
			+ " (" + COL_DEV_ALIAS + " string, " + COL_DEV_PUB_KEY + " blob);";

	final static String CREATE_OBKEYS = "create table "
			+ TABLE_OBFUSCATION_KEYS + " (" + COL_DEV_PUB_KEY + " blob, "
			+ COL_ENC_KEY + " blob);";

	final static String CREATE_SHAREKEYS = "create table " + TABLE_SHARE_KEYS
			+ " (" + COL_KEY_ID + " integer, " + COL_DEV_PUB_KEY + " blob, "
			+ COL_ENC_KEY + " blob);";

	final static String QUERY_SIGNATURE = "SELECT " + COL_SIGNATURE + " FROM "
			+ TABLE_SIGNATURES + ";";

	final static String QUERY_KEYS_SIGNATURE = "SELECT * FROM "
			+ TABLE_SIGNATURES + ";";

	final static String QUERY_SHARE_KEYS_SIGNATURE = "SELECT " + COL_SIGNATURE
			+ "," + COL_SIGNER + " FROM " + TABLE_SIGNATURES + " WHERE "
			+ COL_TYPE + "=" + TYPE_SK + ";";
	final static String QUERY_OBFUSCATION_KEYS_SIGNATURE = "SELECT "
			+ COL_SIGNATURE + "," + COL_SIGNER + " FROM " + TABLE_SIGNATURES
			+ " WHERE " + COL_TYPE + "=" + TYPE_OK + ";";
	final static String QUERY_METADATA_SIGNATURE = "SELECT " + COL_SIGNATURE
			+ "," + COL_SIGNER + " FROM " + TABLE_SIGNATURES + " WHERE "
			+ COL_TYPE + "=" + TYPE_SMD + ";";
	final static String QUERY_DEVICE_LIST_SIGNATURE = "SELECT " + COL_SIGNATURE
			+ " FROM " + TABLE_SIGNATURES + " WHERE " + COL_TYPE + "="
			+ TYPE_DEVLIST + " AND " + COL_SIGNER + "=?;";

	final static String QUERY_SPL = "SELECT * FROM " + TABLE_SHARE_PARTICIPANTS
			+ ";";

	final static String QUERY_DEVICE_LIST = "SELECT * FROM "
			+ TABLE_DEVICE_LIST + ";";

	final static String QUERY_DEVICE_IDX = "SELECT " + COL_ROWID + " FROM "
			+ TABLE_DEVICE_LIST + " WHERE " + COL_DEV_PUB_KEY + "=?;";

	final static String QUERY_SHARE_KEYS = "SELECT * FROM " + TABLE_SHARE_KEYS
			+ ";";

	final static String QUERY_OBFUSCATION_KEYS = "SELECT * FROM "
			+ TABLE_OBFUSCATION_KEYS + ";";

	final static String UPDATE_METADATA = "UPDATE " + TABLE_METADATA + " SET "
			+ COL_VALUE + "=? WHERE " + COL_KEY + "=?;";
	final static String INSERT_METADATA = "INSERT INTO " + TABLE_METADATA
			+ " VALUES (?,?);";
	final static String INSERT_SPL = "INSERT INTO " + TABLE_SHARE_PARTICIPANTS
			+ " VALUES (?,?);";

	final static String INSERT_DEVICE_LIST = "INSERT INTO " + TABLE_DEVICE_LIST
			+ " VALUES (?,?);";

	final static String INSERT_SHAREKEYS = "INSERT INTO " + TABLE_SHARE_KEYS
			+ " VALUES (?,?,?);";
	final static String INSERT_OBFUSCATIONKEYS = "INSERT INTO "
			+ TABLE_OBFUSCATION_KEYS + " VALUES (?,?);";

	final static String UPDATE_SIGNATURE = "UPDATE " + TABLE_SIGNATURES
			+ " SET " + COL_SIGNATURE + "=?, " + COL_SIGNER + "=? WHERE "
			+ COL_TYPE + "=?;";

	final static String UPDATE_SIGNATURE_DEVLIST = "UPDATE " + TABLE_SIGNATURES
			+ " SET " + COL_SIGNATURE + "=? WHERE " + COL_SIGNER + "=? AND "
			+ COL_TYPE + "=?;";
	final static String INSERT_SIGNATURE = "INSERT INTO " + TABLE_SIGNATURES
			+ " VALUES (?);";
	final static String INSERT_SIGNATURE_SIGNER = "INSERT INTO "
			+ TABLE_SIGNATURES + " VALUES (?,?);";

	void init(ShareMetaData smd) throws InitializaionException,
			SignatureException, DeviceListException;

	void store(DeviceList deviceList, ObfuscationKeyDB obKeys,
			ShareKeyDB shareKeys) throws PersistanceException;

	void storeSPL(ShareMetaData smd) throws PersistanceException;

	void load(ShareMetaData smd) throws SQLException, SignatureException,
			InitializaionException, DeviceListException;

	boolean exists();
}