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
package org.panbox.desktop.common.devicemgmt;

import org.apache.log4j.Logger;
import org.panbox.Settings;
import org.panbox.core.devicemgmt.DeviceType;
import org.panbox.core.identitymgmt.AbstractIdentity;
import org.panbox.desktop.common.gui.devices.PanboxDevice;
import org.panbox.desktop.common.identitymgmt.sqlightimpl.IdentityManager;

import java.security.KeyPair;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DeviceManagerImpl implements IDeviceManager {

	private static final String COL_TYPE = "type";
	private static final String COL_NAME = "name";
	private static final Logger logger = Logger.getLogger("org.panbox");

	private final String DEVICESDB = Settings.getInstance().getDevicesDBPath();
	private final String DEVICESDB_CONNECT_STRING = "jdbc:sqlite:" + DEVICESDB;
	private static final String TABLE_DEVICES = "devices";
	private static final String QUERY_DEVICENAME_NO_CASE = "SELECT * FROM "
			+ TABLE_DEVICES + " WHERE LOWER(name)=LOWER(?);";
	private static final String QUERY_DEVICENAME = "SELECT * FROM "
			+ TABLE_DEVICES + " WHERE name=?;";
	private static final String QUERY_ALL_DEVICES = "SELECT * FROM "
			+ TABLE_DEVICES;
	private static final String DELETE_DEVICE = "DELETE FROM " + TABLE_DEVICES
			+ " WHERE name=?;";
	private static final String INSERT_DEVICE = "insert into " + TABLE_DEVICES
			+ " VALUES (NULL, (?), (?))";
	private static final String CREATE_TABLES = "create table "
			+ TABLE_DEVICES
			+ " (id INTEGER PRIMARY KEY AUTOINCREMENT, name string, type string)";
	private static final String DROP_TABLES = "drop table if exists "
			+ TABLE_DEVICES;
	private static final String QUERY_TABLES = "SELECT name FROM sqlite_master WHERE type='table' AND name='"
			+ TABLE_DEVICES + "';";

	private final IdentityManager identityManager;

//	private final AddressbookManager aBookMgr;

	private AbstractIdentity identity;

	private static DeviceManagerImpl instance = null;

	private Connection connection = null;

	private DeviceManagerImpl() throws DeviceManagerException {

		// create a database connection
		try {
			connection = DriverManager.getConnection(DEVICESDB_CONNECT_STRING);
		} catch (SQLException ex) {
			throw new DeviceManagerException(
					"Could not get connection for SQL DB: "
							+ DEVICESDB_CONNECT_STRING, ex);
		}
		
		identityManager = IdentityManager.getInstance();

		try {
			Statement s = connection.createStatement();
			ResultSet rs = s.executeQuery(QUERY_TABLES);
			if (!rs.next()) {
				logger.debug("DeviceManager database did not exist. Creating a new one now...");
				createTables(s);
			} else {
				logger.debug("DeviceManager database exists. Will use that one...");
			}
		} catch (SQLException ex) {
			throw new DeviceManagerException(
					"Failed to run SQL command init: ", ex);
		}

	}

	public static DeviceManagerImpl getInstance() throws DeviceManagerException {
		if (instance == null) {
			instance = new DeviceManagerImpl();
		}
		return instance;
	}

	private void createTables(Statement statement)
			throws DeviceManagerException {
		logger.debug("ShareManager : createTables");
		try {
			statement.executeUpdate(DROP_TABLES);
			statement.executeUpdate(CREATE_TABLES);
		} catch (SQLException ex) {
			throw new DeviceManagerException(
					"Failed to run SQL command during createTables: ", ex);
		}
	}

	@Override
	public void addThisDevice(String name, KeyPair deviceKeyPair,
			DeviceType type) throws DeviceManagerException {
		// 1. Add device key to identity key store
		identity.addDeviceKey(deviceKeyPair, name);

		// 2. Add meta data entry to devicelist manager
		logger.debug("ShareManager : addDevice(" + name + "," + type + ")");

		try {
			addDeviceToDB(name, type);
		} catch (SQLException ex) {
			throw new DeviceManagerException("Failed to run addThisDevice: ",
					ex);
		}
	}

	private void addDeviceToDB(String name, DeviceType type)
			throws SQLException {
		PreparedStatement pStatement = connection
				.prepareStatement(INSERT_DEVICE);
		pStatement.setString(1, name);
		pStatement.setString(2, (type == null ? DeviceType.DESKTOP.toString()
				: type.toString()));
		pStatement.execute();
		pStatement.close();
	}

	@Override
	public void addDevice(String name, X509Certificate deviceCert,
			DeviceType type) throws DeviceManagerException {
		// 1. Add device key to identity key store
		identity.addDeviceCert(deviceCert, name);
		identityManager.storeMyIdentity(identity);

		// 2. Add meta data entry to devicelist manager
		logger.debug("ShareManager : addDevice(" + name + "," + type + ")");

		try {
			addDeviceToDB(name, type);
		} catch (SQLException ex) {
			throw new DeviceManagerException("Failed to run addDevice: ", ex);
		}
	}

	@Override
	public void removeDevice(PanboxDevice device) throws DeviceManagerException {
		logger.debug("ShareManager : removeDevice(" + device.getDeviceName()
				+ ")");
		try {
			PreparedStatement pStmt = connection
					.prepareStatement(DELETE_DEVICE);
			pStmt.setQueryTimeout(30); // set timeout to 30 sec.
			pStmt.setString(1, device.getDeviceName());
			pStmt.executeUpdate();
		} catch (SQLException ex) {
			throw new DeviceManagerException(
					"Failed to run SQL command removeDevice: ", ex);
		}
	}

	@Override
	public List<PanboxDevice> getDeviceList() throws DeviceManagerException {
		logger.debug("ShareManager : getDeviceList");

		List<PanboxDevice> shares = new ArrayList<PanboxDevice>();

		try {
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30); // set timeout to 30 sec.

			ResultSet rs = statement.executeQuery(QUERY_ALL_DEVICES);

			while (rs.next()) {
				PanboxDevice device = createPanboxDeviceWrapper(
						rs.getString(COL_NAME), DeviceType.valueOf(rs
								.getString(COL_TYPE).toUpperCase()));
				shares.add(device);
			}
		} catch (SQLException ex) {
			throw new DeviceManagerException(
					"Failed to run getInstalledShares: ", ex);
		}

		return shares;
	}

	@Override
	public PanboxDevice getDevice(String name) throws DeviceManagerException {
		logger.debug("ShareManager : getDevice(" + name + ")");

		try {
			PreparedStatement pStmt = connection
					.prepareStatement(QUERY_DEVICENAME);
			pStmt.setQueryTimeout(30); // set timeout to 30 sec.
			pStmt.setString(1, name);
			ResultSet rs = pStmt.executeQuery();

			if (rs.next()) {
				PanboxDevice device = createPanboxDeviceWrapper(
						rs.getString(COL_NAME), DeviceType.valueOf(rs
								.getString(COL_TYPE).toUpperCase()));
				pStmt.close();
				return device;
			}
			pStmt.close();
		} catch (SQLException ex) {
			throw new DeviceManagerException("Failed to run getDevice: ", ex);
		}
		throw new DeviceManagerException("No entry found for getDevice(" + name
				+ ").");
	}

	// TODO: This method is just a temporary fix:
	// The Problem is, that aliases stored in java keystores are not necessarily
	// case sensitive. This is plattfrom/implementation specific and a known
	// fact. Therefore we can not expect, that searching a device by an alias
	// taken from a java keystore will match a case sensitive query.
	// Obviously this is not a very good solution. We should enforce, for
	// example lower case when creating devicekeys to circumvent this problem
	@Override
	public PanboxDevice getDeviceIgnoreCase(String name)
			throws DeviceManagerException {
		logger.debug("ShareManager : getDeviceIgnoreCase(" + name + ")");
		try {
			PreparedStatement pStmt = connection
					.prepareStatement(QUERY_DEVICENAME_NO_CASE);
			pStmt.setQueryTimeout(30); // set timeout to 30 sec.
			pStmt.setString(1, name);

			ResultSet rs = pStmt.executeQuery();

			if (rs.next()) {
				PanboxDevice device = createPanboxDeviceWrapper(
						rs.getString(COL_NAME), DeviceType.valueOf(rs
								.getString(COL_TYPE).toUpperCase()));
				pStmt.close();
				return device;
			}
			pStmt.close();
		} catch (SQLException ex) {
			throw new DeviceManagerException("Failed to run getDevice: ", ex);
		}
		throw new DeviceManagerException("No entry found for getDevice(" + name
				+ ").");
	}

	private PanboxDevice createPanboxDeviceWrapper(String deviceName,
			DeviceType deviceType) {
		PublicKey dpKey;
		try {
			dpKey = identity.getPublicKeyForDevice(deviceName);
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException(e);
		}
		return new PanboxDevice(deviceName, deviceType, dpKey);
	}

	public void setIdentity(AbstractIdentity id) {
		this.identity = id;
	}
}
