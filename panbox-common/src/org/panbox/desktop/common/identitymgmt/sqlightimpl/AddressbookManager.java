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

import org.apache.log4j.Logger;
import org.panbox.Settings;
import org.panbox.core.crypto.CryptCore;
import org.panbox.core.identitymgmt.AbstractAddressbookManager;
import org.panbox.core.identitymgmt.AbstractIdentity;
import org.panbox.core.identitymgmt.CloudProviderInfo;
import org.panbox.core.identitymgmt.PanboxContact;
import org.panbox.core.identitymgmt.exceptions.ContactExistsException;

import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.sql.*;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map.Entry;

public class AddressbookManager extends AbstractAddressbookManager {

	private Connection connection = null;

	private static final Logger logger = Logger.getLogger("org.panbox.common");
	
	private static final String TABLE_ADDRESSBOOK = IdentityManager.TABLE_ADDRESSBOOK;
	private static final String TABLE_ADDRESSBOOK_MAP = IdentityManager.TABLE_ADDRESSBOOK_MAP;

	private final String DB_FILE = Settings.getInstance().getAdressbook();

	private final String ADDRESSBOOKDB_CONNECT_STRING = "jdbc:sqlite:" + DB_FILE;

	@Override
	public void init() {

		try {
			connection = DriverManager
					.getConnection(ADDRESSBOOKDB_CONNECT_STRING);

			Statement statement = connection.createStatement();

			// statement.executeUpdate("drop table if exists " +
			// TABLE_ADDRESSBOOK);
			statement
					.executeUpdate("create table if not exists "
							+ TABLE_ADDRESSBOOK
							+ " (id INTEGER PRIMARY KEY AUTOINCREMENT, name string, firstname string, email string, trustLevel int, certEnc blob, certSign blob)");

			// statement.executeUpdate("drop table if exists "
			// + TABLE_ADDRESSBOOK_MAP);
			statement.executeUpdate("create table if not exists "
					+ TABLE_ADDRESSBOOK_MAP
					+ " (identityID integer, addressbookID integer)");

			statement
					.executeUpdate("create table if not exists "
							+ IdentityManager.TABLE_ADDRESSBOOK_CPI
							+ " (id INTEGER PRIMARY KEY AUTOINCREMENT, providerName string, userName string)");

			statement.executeUpdate("create table if not exists "
					+ IdentityManager.TABLE_ADDRESSBOOK_CPI_MAP
					+ " (abcID INTEGER, cpID INTEGER)");

		} catch (SQLException e) {
			logger.error("AddressbookManager: Failure to create SQLite tables");
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					logger.error("AddressbookManager: Failure to close SQlite connection on table creation");
				}
			}
		}
	}

	@Override
	public void loadContacts(AbstractIdentity id) {

		try {
			connection = DriverManager
					.getConnection(ADDRESSBOOKDB_CONNECT_STRING);

			Statement statement = connection.createStatement();

			ResultSet rsAB = statement.executeQuery("select * from "
					+ TABLE_ADDRESSBOOK + ", " + TABLE_ADDRESSBOOK_MAP
					+ " where " + TABLE_ADDRESSBOOK_MAP + ".identityID=\""
					+ id.getID() + "\" and " + TABLE_ADDRESSBOOK_MAP
					+ ".addressbookID = " + TABLE_ADDRESSBOOK + ".id");

			while (rsAB.next()) {

				PanboxContact c = new PanboxContact();

				c.setID(rsAB.getInt("id"));
				c.setName(rsAB.getString("name"));
				c.setFirstName(rsAB.getString("firstname"));
				c.setEmail(rsAB.getString("email"));
				c.setTrustLevel(rsAB.getInt("trustLevel"));

				byte[] certEncBytes = rsAB.getBytes("certEnc");
				X509Certificate certEnc = CryptCore
						.createCertificateFromBytes(certEncBytes);

				byte[] certSignBytes = rsAB.getBytes("certSign");
				X509Certificate certSign = CryptCore
						.createCertificateFromBytes(certSignBytes);

				// check if certificate is still valid, otherwise set it to null
				try {
					certEnc.checkValidity();
					c.setCertEnc(certEnc);
				} catch (CertificateExpiredException e) {
					c.setCertEnc(null);
				} catch (CertificateNotYetValidException e) {
					c.setCertEnc(null);
				}

				try {
					certSign.checkValidity();
					c.setCertSign(certSign);
				} catch (CertificateExpiredException e) {
					c.setCertSign(null);
				} catch (CertificateNotYetValidException e) {
					c.setCertSign(null);
				}

				// get cloud provider information
				Statement statementCP = connection.createStatement();
				ResultSet rsCP = statementCP.executeQuery("select * from "
						+ IdentityManager.TABLE_ADDRESSBOOK_CPI + ", "
						+ IdentityManager.TABLE_ADDRESSBOOK_CPI_MAP + " where "
						+ IdentityManager.TABLE_ADDRESSBOOK_CPI_MAP
						+ ".abcID=\"" + c.getID() + "\" and "
						+ IdentityManager.TABLE_ADDRESSBOOK_CPI + ".id="
						+ IdentityManager.TABLE_ADDRESSBOOK_CPI_MAP + ".cpID");

				while (rsCP.next()) {
					CloudProviderInfo cpi = new CloudProviderInfo(rsCP.getString("providerName"), rsCP.getString("userName"));
					cpi.setId(rsCP.getInt("id"));
					c.addCloudProvider(cpi);
				}

				try {
					id.getAddressbook().addContact(c);
				} catch (ContactExistsException e) {
					//should not happen here, otherwise DB is corrupted
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					logger.error("AddressbookManager: Failure to close SQlite connection while loading contacts");
				}
			}
		}

	}

	@Override
	public void persistContacts(Collection<PanboxContact> contacts,
								int identityKey) {

		try {
			connection = DriverManager
					.getConnection(ADDRESSBOOKDB_CONNECT_STRING);

			PreparedStatement pStatement;

			for (PanboxContact c : contacts) {
				int contactID = c.getID();
				if (contactID > 0) {
					// update contact
					pStatement = connection
							.prepareStatement("update "
									+ TABLE_ADDRESSBOOK
									+ " set name=(?), firstname=(?), email=(?), trustLevel=(?), certEnc=(?), certSign=(?) where id=(?)");

				} else {
					// insert new contact
					pStatement = connection.prepareStatement("insert into "
							+ TABLE_ADDRESSBOOK
							+ " VALUES(NULL, (?), (?),(?),(?),(?),(?))");
				}

				pStatement.setString(1, c.getName());
				pStatement.setString(2, c.getFirstName());
				pStatement.setString(3, c.getEmail());
				pStatement.setInt(4, c.getTrustLevel());

				try {
					X509Certificate cEnc = c.getCertEnc();
					if (cEnc != null)
						pStatement.setBytes(5, cEnc.getEncoded());
					else
						pStatement.setBytes(5, null);

					X509Certificate cSig = c.getCertSign();
					if (cSig != null)
						pStatement.setBytes(6, cSig.getEncoded());
					else
						pStatement.setBytes(6, null);

				} catch (CertificateEncodingException e) {
					logger.error("AddressbookManager: Certificate encoding problem while writing contacts", e);
				}

				if (contactID > 0) {
					pStatement.setInt(7, contactID);
				}
				pStatement.execute();

				Statement statement = connection.createStatement();
				if (contactID < 0) { // insert new contact
					ResultSet keys = pStatement.getGeneratedKeys();
					contactID = keys.getInt("last_insert_rowid()");
					statement.executeUpdate("insert into "
							+ TABLE_ADDRESSBOOK_MAP + " VALUES(\""
							+ identityKey + "\", \"" + contactID + "\")");

					c.setID(contactID);
				}

				// insert/update cpis of contact

				for (Entry<String, CloudProviderInfo> cpiEntry : c
						.getCloudProviders().entrySet()) {

					CloudProviderInfo cpi = cpiEntry.getValue();
					if (cpi.getId() > 0) //update
					{
						String upCPI = "update "
								+ IdentityManager.TABLE_ADDRESSBOOK_CPI
								+ " set providerName=(?), username=(?) where id=(?)";
						PreparedStatement ps = connection
								.prepareStatement(upCPI);

						ps.setString(1, cpiEntry.getKey());
						ps.setString(2, cpi.getUsername());
						ps.setInt(3, cpi.getId());

						ps.execute();
					} else //insert
					{
						pStatement = connection.prepareStatement("insert into "
								+ IdentityManager.TABLE_ADDRESSBOOK_CPI
								+ " VALUES(NULL, (?), (?))");

						pStatement.setString(1, cpiEntry.getKey());
						pStatement.setString(2, cpi.getUsername());

						pStatement.execute();

						ResultSet k = pStatement.getGeneratedKeys();
						int cpiID = k.getInt("last_insert_rowid()");

						pStatement = connection.prepareStatement("insert into "
								+ IdentityManager.TABLE_ADDRESSBOOK_CPI_MAP
								+ " VALUES((?), (?))");

						pStatement.setInt(1, contactID);
						pStatement.setInt(2, cpiID);

						pStatement.execute();

						cpi.setId(cpiID);
					}
				}

				String sqlCPI = "select * from "
						+ IdentityManager.TABLE_ADDRESSBOOK_CPI + ", "
						+ IdentityManager.TABLE_ADDRESSBOOK_CPI_MAP + " where "
						+ IdentityManager.TABLE_ADDRESSBOOK_CPI + ".id="
						+ IdentityManager.TABLE_ADDRESSBOOK_CPI_MAP
						+ ".cpID and "
						+ IdentityManager.TABLE_ADDRESSBOOK_CPI_MAP + ".abcID="
						+ contactID;
				ResultSet r = statement.executeQuery(sqlCPI);
				LinkedList<Integer> toBeRemoved = new LinkedList<Integer>();
				while (r.next()) {
					int cpID = r.getInt("id");

					boolean found = false;
					for (CloudProviderInfo cpi : c.getCloudProviders().values()) {

						if (cpi.getId() == cpID) {
							found = true;
							break;
						}

					}
					if (!found) {
						// delete from db
						toBeRemoved.add(cpID);
					}
				}

				for (int cpID : toBeRemoved) {
					String sqlDel = "delete from "
							+ IdentityManager.TABLE_ADDRESSBOOK_CPI
							+ " where id=" + cpID;
					statement.execute(sqlDel);

					sqlDel = "delete from "
							+ IdentityManager.TABLE_ADDRESSBOOK_CPI_MAP
							+ " where abcID=" + contactID + " and cpID=" + cpID;
					statement.execute(sqlDel);
				}
			}

			// walk through db and remove all contacts which do not exist in our
			// addressbook anymore
			Statement statement = connection.createStatement();
			String sql = "select id, email from " + TABLE_ADDRESSBOOK + ", "
					+ TABLE_ADDRESSBOOK_MAP + " where " + TABLE_ADDRESSBOOK_MAP
					+ ".identityID=\"" + identityKey + "\" and "
					+ TABLE_ADDRESSBOOK_MAP + ".addressbookID = "
					+ TABLE_ADDRESSBOOK + ".id";
			ResultSet r = statement.executeQuery(sql);
			while (r.next()) {
				int contactID = r.getInt("id");

				boolean found = false;
				for (PanboxContact c : contacts) {
					if (c.getID() == contactID) {
						found = true;
						break;
					}
				}
				if (!found) {
					String sqlDel = "";
					String queryCPIs = "select * from "
							+ IdentityManager.TABLE_ADDRESSBOOK_CPI + ", "
							+ IdentityManager.TABLE_ADDRESSBOOK_CPI_MAP
							+ " where " + IdentityManager.TABLE_ADDRESSBOOK_CPI
							+ ".id="
							+ IdentityManager.TABLE_ADDRESSBOOK_CPI_MAP
							+ ".cpID and "
							+ IdentityManager.TABLE_ADDRESSBOOK_CPI_MAP
							+ ".abcID=" + contactID;

					ResultSet cpis = statement.executeQuery(queryCPIs);
					LinkedList<Integer> toBeRemoved = new LinkedList<Integer>();
					while (cpis.next()) {
						int cpID = cpis.getInt("id");

						toBeRemoved.add(cpID);
					}

					for (int cpID : toBeRemoved) {
						sqlDel = "delete from "
								+ IdentityManager.TABLE_ADDRESSBOOK_CPI
								+ " where id=" + cpID;
						statement.execute(sqlDel);

						sqlDel = "delete from "
								+ IdentityManager.TABLE_ADDRESSBOOK_CPI_MAP
								+ " where cpID=" + cpID + " and abcID="
								+ contactID;
						statement.execute(sqlDel);
					}

					sqlDel = "delete from " + TABLE_ADDRESSBOOK + " where id="
							+ contactID;
					statement.execute(sqlDel);

					sqlDel = "delete from " + TABLE_ADDRESSBOOK_MAP
							+ " where addressbookID=" + contactID
							+ " and identityID=" + identityKey;
					statement.execute(sqlDel);
				}
			}

		} catch (SQLException e1) {
			logger.error("AddressbookManager: SQL Exception while writing contacts", e1);
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					logger.error("AddressbookManager: Failure to close SQlite connection while storing contacts");
				}
			}
		}
	}

}
