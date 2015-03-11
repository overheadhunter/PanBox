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
package org.panbox.desktop.common.identitymgmt.sqlightimpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.panbox.Settings;
import org.panbox.core.crypto.KeyConstants;
import org.panbox.core.identitymgmt.AbstractAddressbookManager;
import org.panbox.core.identitymgmt.AbstractIdentity;
import org.panbox.core.identitymgmt.AbstractIdentityManager;
import org.panbox.core.identitymgmt.CloudProviderInfo;
import org.panbox.core.identitymgmt.IAddressbook;
import org.panbox.core.identitymgmt.Identity;

public class IdentityManager extends AbstractIdentityManager {

	public static String DB_FILE = Settings.getInstance().getIdentityPath();
	private static String IDENTITYDB_CONNECT_STRING = "jdbc:sqlite:" + DB_FILE;
	public static String KEYSTORE_PATH = Settings.getInstance()
			.getKeystorePath();

	private static IdentityManager idm = null;
	private Connection connection = null;

	private static final Logger logger = Logger.getLogger("org.panbox.common");

	private IdentityManager() {
	}

	public static IdentityManager getInstance() {
		if (idm == null) {
			idm = new IdentityManager();
		}
		return idm;
	}

	/**
	 * Creates the initial set of DB tables for the SQLITE DB to store our
	 * identity and addressbook entries
	 */
	@Override
	public void init(AbstractAddressbookManager aBooMgr) {
		setAddressBookManager(aBooMgr);
		reloadSettings();

		// create a database connection
		try {
			connection = DriverManager.getConnection(IDENTITYDB_CONNECT_STRING);
		} catch (SQLException e) {
			logger.error("IdentityManager: Failure to connect to db: "
					+ IDENTITYDB_CONNECT_STRING, e);
		}

		Statement statement;
		try {
			statement = connection.createStatement();
			statement.setQueryTimeout(30); // set timeout to 30 sec.

			statement
					.executeUpdate("create table if not exists "
							+ TABLE_IDENTITY
							+ " (id INTEGER PRIMARY KEY AUTOINCREMENT, name string, firstname string, email string, keystorePath string)");
			statement
					.executeUpdate("create table if not exists "
							+ TABLE_CLOUDPROVIDER
							+ " (id INTEGER PRIMARY KEY AUTOINCREMENT, name string, username string, password string)");
			statement.executeUpdate("create table if not exists "
					+ TABLE_CLOUDPROVIDER_MAP
					+ " (identityID integer, cloudProviderID integer)");

			aBooMgr.init();

		} catch (SQLException e) {
			logger.error("IdentityManager: Failure on creating tables", e);
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					logger.error(
							"IdentityManager: Failure to close db connection while creating tables",
							e);
				}
			}
		}
	}

	/**
	 * Stores the given Identity in a SQLITE DB and keys in a java keystore as a
	 * file
	 * 
	 * @param id
	 *            - The Identity to store
	 */
	@Override
	public void storeMyIdentity(AbstractIdentity id) {

		reloadSettings();
		Statement statement;

		try {
			connection = DriverManager.getConnection(IDENTITYDB_CONNECT_STRING);
		} catch (SQLException e) {
			logger.error("IdentityManager: Failure to open db connection", e);
		}

		int storedIdentityID = id.getID();

		String sql = "";
		if (storedIdentityID > 0) // do update
		{
			sql = "update "
					+ TABLE_IDENTITY
					+ " set name=(?), firstname=(?), email=(?), keystorePath=(?) where id=(?)";
		} else {
			sql = "insert into " + TABLE_IDENTITY
					+ " VALUES (NULL, (?), (?), (?), (?))";
		}

		PreparedStatement pStatement;
		try {
			pStatement = connection.prepareStatement(sql);
			pStatement.setString(1, id.getName());
			pStatement.setString(2, id.getFirstName());
			pStatement.setString(3, id.getEmail());
			pStatement.setString(4, KEYSTORE_PATH); // this will never be
			// updated, if keystore is
			// changed after identity
			// was created
			if (storedIdentityID > 0) {
				pStatement.setInt(5, storedIdentityID);
			}

			pStatement.executeUpdate();

			if (storedIdentityID < 0) {
				ResultSet keys = pStatement.getGeneratedKeys();
				storedIdentityID = keys.getInt("last_insert_rowid()");
				id.setID(storedIdentityID);
			}

			statement = connection.createStatement();
			statement.setQueryTimeout(30); // set timeout to 30 sec.

			// insert/update cloudproviders
			String sqlListCPIs = "select * from " + TABLE_CLOUDPROVIDER + ", "
					+ TABLE_CLOUDPROVIDER_MAP + " where " + TABLE_CLOUDPROVIDER
					+ ".id=" + TABLE_CLOUDPROVIDER_MAP
					+ ".cloudProviderID and " + TABLE_CLOUDPROVIDER_MAP
					+ ".identityID=" + storedIdentityID;

			for (CloudProviderInfo cp : id.getCloudProviders().values()) {
				// check if cp is in db

				if (cp.getId() > 0) {
					String upCPI = "update " + TABLE_CLOUDPROVIDER
							+ " set name=(?), username=(?) where id=(?)";
					PreparedStatement ps = connection.prepareStatement(upCPI);

					ps.setString(1, cp.getProviderName());
					ps.setString(2, cp.getUsername());
					ps.setInt(3, cp.getId());

					ps.execute();
				} else {
					statement.executeUpdate("insert into "
							+ TABLE_CLOUDPROVIDER + " VALUES(NULL, \""
							+ cp.getProviderName() + "\", \""
							+ cp.getUsername() + "\", NULL" + ")");

					ResultSet keys = statement.getGeneratedKeys();
					int cpID = keys.getInt("last_insert_rowid()");
					cp.setId(cpID);

					statement.executeUpdate("insert into "
							+ TABLE_CLOUDPROVIDER_MAP + " VALUES(\""
							+ storedIdentityID + "\", \"" + cpID + "\")");
				}
			}

			// check which cloudprovider need to be removed from db
			ResultSet r = statement.executeQuery(sqlListCPIs);

			LinkedList<Integer> toBeRemoved = new LinkedList<Integer>();
			while (r.next()) {
				boolean found = false;

				int cId = r.getInt("id");

				for (CloudProviderInfo cpi : id.getCloudProviders().values()) {
					if (cpi.getId() == cId) {
						found = true;
						break;
					}
				}

				if (!found) {
					// delete this cpi from db
					toBeRemoved.add(cId);
				}
			}

			for (Integer cId : toBeRemoved) {
				String sqlDel = "delete from " + TABLE_CLOUDPROVIDER
						+ " where id=" + cId;
				statement.execute(sqlDel);

				sqlDel = "delete from " + TABLE_CLOUDPROVIDER_MAP
						+ " where cloudProviderID=" + cId + " and identityID="
						+ storedIdentityID;
				statement.execute(sqlDel);
			}

		} catch (SQLException e) {
			logger.error(
					"IdentityManager: SQL error while storing the identity", e);
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					logger.error(
							"IdentityManager: Failure to close db connection while storing the identity",
							e);
				}
			}
		}

		// store contacts
		if (null != getAddressBookManager()) {
			getAddressBookManager().persistContacts(
					id.getAddressbook().getContacts(), storedIdentityID);
		} else {
			System.err.println("IdentityManager: No AddressbookManager set");
		}

		// store keystore with private keys in file
		File keyStoreFile = new File(KEYSTORE_PATH);

		FileOutputStream fos;
		try {
			fos = new FileOutputStream(keyStoreFile);

			Identity idCast = (Identity) id;
			idCast.getKeyStore()
					.store(fos, KeyConstants.OPEN_KEYSTORE_PASSWORD);

			fos.close();

		} catch (KeyStoreException | NoSuchAlgorithmException
				| CertificateException | IOException e) {
			logger.error("IdentityManager: Failure to write keystore", e);
		}

	}

	/**
	 * Loads our own Identity from a SQLITE DB and keys from a java keystore
	 * file
	 * 
	 * @param addressbook
	 *            - Plattform specific Implementation of addressbook
	 * @param aBookMgr
	 *            - Plattform specific manager to load and store contacts
	 * @return - Our own Identity, or null if no identity has been stored yet
	 */
	@Override
	public AbstractIdentity loadMyIdentity(IAddressbook addressbook) {

		Identity id = new Identity(addressbook);

		Statement statement;
		// String keystorePath = ""; // this is now loaded from settings
		try {
			reloadSettings();
			connection = DriverManager.getConnection(IDENTITYDB_CONNECT_STRING);
			statement = connection.createStatement();
			statement.setQueryTimeout(30); // set timeout to 30 sec.

			ResultSet rs = statement.executeQuery("select * from "
					+ TABLE_IDENTITY);

			int idCount = 0;
			while (rs.next()) {

				// we only support ONE ID
				if (idCount >= 1) {
					throw new RuntimeException(
							"More than one ID found - Not supported, DB corrupted");
				}

				// read the result set
				int identityID = rs.getInt("id");

				id.setName(rs.getString("name"));
				id.setFirstName(rs.getString("firstname"));
				id.setEmail(rs.getString("email"));

				// keystorePath = rs.getString("keystorePath"); // this is now
				// loaded from settings

				ResultSet rsCPs = statement.executeQuery("select * from "
						+ TABLE_CLOUDPROVIDER + ", " + TABLE_CLOUDPROVIDER_MAP
						+ " where " + TABLE_CLOUDPROVIDER_MAP
						+ ".identityID=\"" + identityID + "\" and "
						+ TABLE_CLOUDPROVIDER_MAP + ".cloudProviderID = "
						+ TABLE_CLOUDPROVIDER + ".id");

				while (rsCPs.next()) {

					CloudProviderInfo cpi = new CloudProviderInfo(
							rsCPs.getString("name"),
							rsCPs.getString("username"));
					cpi.setId(rsCPs.getInt("id"));

					id.addCloudProvider(cpi);
				}

				id.setID(identityID);

				getAddressBookManager().loadContacts(id);
				idCount++;
			}
			if (idCount == 0) {
				// could not load identity (table is empty)
				return null;
			}

		} catch (SQLException e) {
			throw new RuntimeException("Cannot access Identity.db", e);
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					logger.error(
							"IdentityManager: Failure to close db connection while loading the identity",
							e);
				}
			}
		}

		// load keystore from file
		KeyStore store = null;
		try {
			store = KeyStore.getInstance(KeyConstants.KEYSTORE_TYPE);
		} catch (KeyStoreException e1) {
			logger.error("IdentityManager: Failure instantiate keystore of type: " + KeyConstants.KEYSTORE_TYPE, e1);
		}
		File keystoreFile = new File(KEYSTORE_PATH);

		if (keystoreFile.exists()) {
			FileInputStream fis;
			try {
				fis = new FileInputStream(keystoreFile);
				store.load(fis, KeyConstants.OPEN_KEYSTORE_PASSWORD);
				
				fis.close();
			} catch (NoSuchAlgorithmException | CertificateException
					| IOException e) {
				logger.error("IdentityManager: Failure to load keystore from file " + KEYSTORE_PATH, e);
			}
			id.setKeyStore(store);
		} else {
			logger.error("IdentityManager: No Keystore found -> returning null as identity!");
			return null; // no keystore -> no identity
		}

		return id;
	}

	private void reloadSettings() {
		DB_FILE = Settings.getInstance().getIdentityPath();
		KEYSTORE_PATH = Settings.getInstance().getKeystorePath();
		IDENTITYDB_CONNECT_STRING = "jdbc:sqlite:" + DB_FILE;
	}
}
