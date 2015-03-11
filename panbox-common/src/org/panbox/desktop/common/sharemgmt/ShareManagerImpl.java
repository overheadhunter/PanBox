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
package org.panbox.desktop.common.sharemgmt;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.panbox.PanboxConstants;
import org.panbox.Settings;
import org.panbox.core.Utils;
import org.panbox.core.crypto.KeyConstants;
import org.panbox.core.csp.StorageBackendType;
import org.panbox.core.exception.DeviceListException;
import org.panbox.core.exception.ObfuscationException;
import org.panbox.core.exception.ShareMetaDataException;
import org.panbox.core.identitymgmt.AbstractIdentity;
import org.panbox.core.identitymgmt.IPerson;
import org.panbox.core.identitymgmt.PanboxContact;
import org.panbox.core.keymgmt.VolumeParams;
import org.panbox.core.keymgmt.VolumeParams.VolumeParamsFactory;
import org.panbox.desktop.common.ex.DeviceKeyException;
import org.panbox.desktop.common.gui.PasswordEnterDialog;
import org.panbox.desktop.common.gui.PasswordEnterDialog.PermissionType;
import org.panbox.desktop.common.gui.shares.PanboxShare;
import org.panbox.desktop.common.utils.FileUtils;

public class ShareManagerImpl implements IShareManager {

	private static final Logger logger = Logger.getLogger("org.panbox");

	private final String SHARESDB = Settings.getInstance().getSharesDBPath();
	private final String SHARESDB_CONNECT_STRING = "jdbc:sqlite:" + SHARESDB;
	private static final String TABLE_SHARES = "shares";

	private static ShareManagerImpl instance = null;
	private Connection connection = null;

	private AbstractIdentity identity;

	private final VolumeParamsFactory paramsFactory = VolumeParamsFactory
			.getFactory();

	public final IPanboxService service;

	private ShareManagerImpl(IPanboxService service)
			throws ShareManagerException {
		this.service = service;

		// create a database connection
		try {
			connection = DriverManager.getConnection(SHARESDB_CONNECT_STRING);
		} catch (SQLException ex) {
			throw new ShareManagerException(
					"Could not get connection for SQL DB: "
							+ SHARESDB_CONNECT_STRING, ex);
		}
		init();
	}

	public static ShareManagerImpl getInstance() throws ShareManagerException {
		if (instance == null) {
			throw new RuntimeException(
					"Service implementation has not been set yet!");
		}
		return instance;
	}

	public static ShareManagerImpl getInstance(IPanboxService service)
			throws ShareManagerException {
		if (instance == null) {
			instance = new ShareManagerImpl(service);
			// init();
		}
		return instance;
	}

	private void init() throws ShareManagerException {
		Connection con = null;
		try {
			con = DriverManager.getConnection(SHARESDB_CONNECT_STRING);
			Statement s = con.createStatement();
			ResultSet rs = s
					.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='"
							+ TABLE_SHARES + "';");
			if (!rs.next()) {
				logger.debug("ShareManager database did not exist. Creating a new one now...");
				System.out.println("new ShareManager, creating table...");
				createTables(s);
			} else {
				logger.debug("ShareManager database exists. Will use that one...");
			}
		} catch (SQLException ex) {
			throw new ShareManagerException("Failed to run SQL command init: ",
					ex);
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException ex) {
					throw new ShareManagerException(
							"Failed to run SQL command init: ", ex);
				}
			}
		}
	}

	private static void createTables(Statement statement)
			throws ShareManagerException {
		logger.debug("ShareManager : createTables");
		try {
			statement.executeUpdate("drop table if exists " + TABLE_SHARES);
			statement
					.executeUpdate("create table "
							+ TABLE_SHARES
							+ " (id INTEGER PRIMARY KEY AUTOINCREMENT, uuid string, name string, type string, backendURL string)");
		} catch (SQLException ex) {
			throw new ShareManagerException(
					"Failed to run SQL command during createTables: ", ex);
		}
	}

	@Override
	public List<String> getInstalledShares() throws ShareManagerException {
		logger.debug("ShareManager : getInstalledShares");

		List<String> shares = new ArrayList<String>();
		ResultSet rs = null;
		Statement statement = null;
		try {
			statement = connection.createStatement();
			statement.setQueryTimeout(30); // set timeout to 30 sec.

			rs = statement.executeQuery("select * from " + TABLE_SHARES);

			while (rs.next()) {
				String shareName = rs.getString("name");
				shares.add(shareName);
			}
		} catch (SQLException ex) {
			throw new ShareManagerException(
					"Failed to run getInstalledShares: ", ex);
		} finally {
			try {
				if (rs != null)
					rs.close();
			} catch (Exception e) {
			}

			try {
				if (statement != null)
					statement.close();
			} catch (Exception e) {
			}
		}
		return shares;
	}

	private StorageBackendType getShareType(String shareType) {
		if (shareType.equals(StorageBackendType.DROPBOX.toString())) {
			return StorageBackendType.DROPBOX;
		} else if (shareType.equals(StorageBackendType.FOLDER.toString())) {
			return StorageBackendType.FOLDER;
		}
		// should not have gotten here
		logger.warn("Could not find correct StorageBackenType for type description: "
				+ shareType);
		logger.warn("Returning StorageBackendType FOLDER as fallback!");
		return StorageBackendType.FOLDER;
	}

	@Override
	public boolean shareNameAvailable(String shareName)
			throws ShareManagerException, UnrecoverableKeyException,
			ShareMetaDataException {
		logger.debug("ShareManager : isShareNameAvailable(" + shareName + ")");
		ResultSet rs = null;
		String sql = "select * from " + TABLE_SHARES + " where name=?";
		PreparedStatement pstatement = null;
		try {
			pstatement = connection.prepareStatement(sql);
			pstatement.setString(1, shareName);
			pstatement.setQueryTimeout(30); // set timeout to 30 sec.
			rs = pstatement.executeQuery();

			if (rs.next()) {
				return false;
			} else {
				return true;
			}
		} catch (SQLException ex) {
			throw new ShareManagerException(
					"Failed to run shareNameAvailable: ", ex);
		} finally {
			try {
				if (rs != null)
					rs.close();
			} catch (Exception e) {
			}

			try {
				if (pstatement != null)
					pstatement.close();
			} catch (Exception e) {
			}
		}
	}

	@Override
	public PanboxShare getShareForName(String shareName)
			throws ShareDoesNotExistException, ShareManagerException,
			UnrecoverableKeyException, ShareMetaDataException {
		logger.debug("ShareManager : getShareForName(" + shareName + ")");
		ResultSet rs = null;
		String sql = "select * from " + TABLE_SHARES + " where name=?";
		PreparedStatement pstatement = null;
		try {
			pstatement = connection.prepareStatement(sql);
			pstatement.setString(1, shareName);
			pstatement.setQueryTimeout(30); // set timeout to 30 sec.
			rs = pstatement.executeQuery();

			if (rs.next()) {
				String shareType = rs.getString("type");
				String shareURL = rs.getString("backendURL");
				String shareUUID = rs.getString("uuid");
				try {
					PanboxShare share = nameTypeUrlToVolumeData(shareName,
							shareURL, getShareType(shareType),
							UUID.fromString(shareUUID));
					return share;
				} catch (FileNotFoundException ex) {
					throw new ShareInaccessibleException(
							"A share with the specified share name ("
									+ shareName
									+ ") does exist, but is not accessible anymore!",
							ex);
				}
			} else {
				throw new ShareDoesNotExistException(
						"A share with the specified share name (" + shareName
								+ ") does not exist.");
			}
		} catch (SQLException | IOException ex) {
			throw new ShareManagerException("Failed to run getShareForName: ",
					ex);
		} finally {
			try {
				if (rs != null)
					rs.close();
			} catch (Exception e) {
			}

			try {
				if (pstatement != null)
					pstatement.close();
			} catch (Exception e) {
			}
		}
	}

	@Override
	public PanboxShare getShareForPath(String sharePath)
			throws UnrecoverableKeyException, ShareManagerException,
			ShareMetaDataException {
		logger.debug("ShareManager: getShareForPath(" + sharePath + ")");
		ResultSet rs = null;
		String sql = "select * from " + TABLE_SHARES + " where backendURL=?";
		PreparedStatement pstatement = null;
		try {
			pstatement = connection.prepareStatement(sql);
			pstatement.setString(1, sharePath);
			pstatement.setQueryTimeout(30); // set timeout to 30 sec.
			rs = pstatement.executeQuery();

			if (rs.next()) {
				String shareName = rs.getString("name");
				String shareType = rs.getString("type");
				String shareUUID = rs.getString("uuid");
				try {
					PanboxShare share = nameTypeUrlToVolumeData(shareName,
							sharePath, getShareType(shareType),
							UUID.fromString(shareUUID));
					return share;
				} catch (FileNotFoundException ex) {
					throw new ShareInaccessibleException(
							"A share with the specified share path ("
									+ sharePath
									+ ") does exist, but is not accessible anymore!",
							ex);
				}
			} else {
				logger.warn("A share with the specified share path ("
						+ sharePath + ") does not exist.");
				return null;
			}
		} catch (SQLException | IOException ex) {
			throw new ShareManagerException("Failed to run getShareForPath: ",
					ex);
		} finally {
			try {
				if (rs != null)
					rs.close();
			} catch (Exception e) {
			}

			try {
				if (pstatement != null)
					pstatement.close();
			} catch (Exception e) {
			}
		}
	}

	@Override
	public boolean sharePathAvailable(String path)
			throws UnrecoverableKeyException, ShareManagerException,
			ShareMetaDataException {
		logger.debug("ShareManager : isSharePathAvailable(" + path + ")");

		ResultSet rs = null;
		String sql = "select * from " + TABLE_SHARES + " where backendURL=?";
		PreparedStatement pstatement = null;
		try {
			pstatement = connection.prepareStatement(sql);
			pstatement.setString(1, path);
			pstatement.setQueryTimeout(30); // set timeout to 30 sec.
			rs = pstatement.executeQuery();

			if (rs.next()) {
				return false;
			} else {
				return true;
			}
		} catch (SQLException ex) {
			throw new ShareManagerException(
					"Failed to run sharePathAvailable: ", ex);
		} finally {
			try {
				if (rs != null)
					rs.close();
			} catch (Exception e) {
			}

			try {
				if (pstatement != null)
					pstatement.close();
			} catch (Exception e) {
			}
		}
	}

	@Override
	public PanboxShare editShare(String shareName, String newShareName,
			StorageBackendType newShareType, String newSharePath,
			char[] password) throws ShareManagerException,
			ShareNameAlreadyExistsException, SharePathAlreadyExistsException,
			UnrecoverableKeyException, ShareMetaDataException {
		logger.debug("ShareManager : editShare(" + shareName + ","
				+ newShareName + "," + newShareType + "," + newSharePath + ")");
		String sql = "UPDATE " + TABLE_SHARES
				+ " SET name = ?, type = ?, backendURL = ?" + "WHERE name = ?;";
		//String sqluuid = "SELECT FROM " + TABLE_SHARES + " uuid " + "WHERE name = ?;";

		boolean nameChanged = !shareName.equals(newShareName);
		boolean pathChanged = false;

		try {
			pathChanged = !getShareForName(shareName).getPath().equals(
					newSharePath);
		} catch (ShareDoesNotExistException e) {
			e.printStackTrace();
		}

		if ((!nameChanged || shareNameAvailable(newShareName))
				&& (!pathChanged || sharePathAvailable(newSharePath))) {
			PreparedStatement pStatement = null;
			ResultSet rs = null;
			try {
				pStatement = connection.prepareStatement(sql);

				pStatement.setString(1, newShareName);
				pStatement.setString(2, newShareType.toString());
				pStatement.setString(3, newSharePath);
				pStatement.setString(4, shareName);
				pStatement.executeUpdate();
				pStatement.close();

				return getShareForName(newShareName);
			} catch (SQLException | ShareDoesNotExistException ex) {
				// FIXME: consider reverting DB changes if something went wrong
				throw new ShareManagerException(
						"Failed to run SQL command editShare: ", ex);
			} finally {
				try {
					if (rs != null)
						rs.close();
				} catch (Exception e) {
				}

				try {
					if (pStatement != null)
						pStatement.close();
				} catch (Exception e) {
				}
			}
		} else {
			if (nameChanged && !shareNameAvailable(newShareName)) {
				throw new ShareNameAlreadyExistsException(
						"A share with the name " + newShareName
								+ " does already exist.");
			} else {
				throw new SharePathAlreadyExistsException(
						"A share with the url " + newSharePath
								+ " does already exist.");
			}
		}

	}

	@Override
	public void removeShareFromDB(String shareName)
			throws ShareManagerException {
		logger.debug("ShareManager : removeShareFromDB(" + shareName + ")");
		PreparedStatement pstatement = null;
		String sql = "DELETE from " + TABLE_SHARES + " WHERE name=?;";

		try {
			pstatement = connection.prepareStatement(sql);

			pstatement.setString(1, shareName);
			pstatement.setQueryTimeout(30); // set timeout to 30 sec.
			pstatement.executeUpdate();

		} catch (SQLException ex) {
			throw new ShareManagerException(
					"Failed to run SQL command removeShareFromDB: ", ex);
		} finally {
			try {
				if (pstatement != null)
					pstatement.close();
			} catch (Exception e) {
			}
		}
	}

	@Override
	public void removeShare(String shareName, String sharePath,
			StorageBackendType type) throws ShareManagerException {
		logger.debug("ShareManager : removeShare(" + shareName + ")");

		try {
			removeShareFromDB(shareName);
			service.removeShare(paramsFactory.createVolumeParams()
					.setShareName(shareName).setPath(sharePath).setType(type));

		} catch (RemoteException ex) {
			throw new ShareManagerException(
					"Failed to run command removeShare: ", ex);
		}
	}

	@Override
	public PanboxShare addDevicePermission(PanboxShare share,
			String deviceName, char[] password)
			throws ShareDoesNotExistException, ShareManagerException,
			UnrecoverableKeyException, ShareMetaDataException {

		try {
			VolumeParams p = paramsFactory
					.createVolumeParams()
					.setKeys(identity, password)
					.setUserAlias(identity.getEmail())
					.setDeviceAlias(deviceName)
					.setPublicDeviceKey(
							identity.getPublicKeyForDevice(deviceName))
					.setShareName(share.getName()).setPath(share.getPath())
					.setType(share.getType());
			PanboxShare pbShare = service.addDevice(p);
			return pbShare;
		} catch (IllegalArgumentException | RemoteException e) {
			throw new ShareManagerException(
					"Could not add Device to ShareMetadata", e);
		}
	}

	@Override
	public PanboxShare removeDevicePermission(PanboxShare share,
			String deviceName, char[] password)
			throws ShareDoesNotExistException, ShareManagerException,
			UnrecoverableKeyException, ShareMetaDataException {

		try {
			VolumeParams p = paramsFactory
					.createVolumeParams()
					.setUserAlias(identity.getEmail())
					.setPublicSignatureKey(identity.getPublicKeySign())
					.setPrivateSignatureKey(
							identity.getPrivateKeySign(password))
					.setDeviceAlias(deviceName).setShareName(share.getName())
					.setPath(share.getPath()).setType(share.getType());

			PanboxShare pbShare = service.removeDevice(p);
			return pbShare;
		} catch (IllegalArgumentException | RemoteException e) {
			throw new ShareManagerException(
					"Could not add Device to ShareMetadata", e);
		}

	}

	@Override
	public PanboxShare addContactPermission(PanboxShare share, String email,
			char[] password) throws ShareDoesNotExistException,
			ShareManagerException, UnrecoverableKeyException,
			ShareMetaDataException {
		PanboxContact contact = identity.getAddressbook().contactExists(email);
		if (contact == null) {
			throw new ShareManagerException("Contact " + email
					+ " is not in addressbook.");
		}
		try {

			VolumeParams p = paramsFactory.createVolumeParams()
					.setKeys(identity, password)
					.setOwnerAlias(identity.getEmail())
					.setOtherSignatureKey(contact.getPublicKeySign())
					.setOtherEncryptionKey(contact.getPublicKeyEnc())
					.setUserAlias(contact.getEmail())
					.setShareName(share.getName()).setPath(share.getPath())
					.setType(share.getType());

			// Add Invitation fingerPrint so invited user can detect
			// invitation
			File invitationFolder = new File(share.getPath() + File.separator
					+ PanboxConstants.PANBOX_SHARE_METADATA_DIRECTORY
					+ File.separator
					+ PanboxConstants.PANBOX_SHARE_INVITATION_DIRECTORY);
			if (invitationFolder.isFile()) {
				// invitationFolder is not a folder
				throw new RuntimeException(
						"invitation folder is a file, not a folder!");
			}
			if (!invitationFolder.exists()) {
				invitationFolder.mkdir();
			}
			String fingerPrint = DigestUtils.md5Hex(contact.getCertSign()
					.getPublicKey().getEncoded());
			File fpFile = new File(invitationFolder.getAbsolutePath()
					+ File.separator + fingerPrint);
			fpFile.createNewFile();

			PanboxShare pbShare = service.inviteUser(p);
			pbShare.generatePermissionsModel(identity);
			return pbShare;
		} catch (IOException e) {
			throw new ShareManagerException(
					"Could not invite Contact to ShareMetadata", e);
		}
	}

	public void setIdentity(AbstractIdentity identity) {
		this.identity = identity;
	}

	@Override
	public PanboxShare addNewShare(PanboxShare share, char[] password)
			throws ShareManagerException, ShareNameAlreadyExistsException,
			SharePathAlreadyExistsException, UnrecoverableKeyException,
			ShareMetaDataException {

		logger.debug("ShareManager : addNewShare(" + share.getName() + ","
				+ share.getType() + "," + share.getPath() + ")");
		boolean shareDirCreated = false;

		if (shareNameAvailable(share.getName())
				&& sharePathAvailable(share.getPath())) {

			File shareFolder = new File(share.getPath());

			String sql = "insert into " + TABLE_SHARES
					+ " VALUES (NULL, (?), (?), (?), (?))";

			PreparedStatement pStatement = null;
			try {
				if (!shareFolder.exists()) {
					shareDirCreated = shareFolder.mkdirs();
				}

				PanboxShare resultShare = nameTypeUrlToVolumeData(share,
						password);

				pStatement = connection.prepareStatement(sql);
				pStatement.setString(1, resultShare.getUuid().toString());
				pStatement.setString(2, share.getName());
				pStatement.setString(3, share.getType().toString());
				pStatement.setString(4, share.getPath());
				pStatement.executeUpdate();

				return resultShare;
			} catch (SQLException | NoSuchAlgorithmException | IOException e) {
				// in case of error we need to remove the share again
				removeShare(share.getName(), share.getPath(), share.getType());

				try {
					if (shareFolder.exists() && shareDirCreated) {
						FileUtils.deleteDirectoryTree(shareFolder);
					}
				} catch (Exception e1) {
				}

				throw new ShareManagerException("Failed to run addNewShare: ",
						e);

			} catch (ShareMetaDataException | UnrecoverableKeyException
					| UnknownOwnerException e) {

				// in case of error we need to remove the share again
				removeShare(share.getName(), share.getPath(), share.getType());

				try {
					if (shareFolder.exists() && shareDirCreated) {
						FileUtils.deleteDirectoryTree(shareFolder);
					}
				} catch (Exception e1) {
				}

				throw e;
			} finally {
				try {

					if (pStatement != null) {
						pStatement.close();
					}

				} catch (Exception e) {
				}
			}
		} else {
			if (!shareNameAvailable(share.getName())) {
				throw new ShareNameAlreadyExistsException(
						"A share with the name " + share.getName()
								+ " does already exist.");
			} else {
				throw new SharePathAlreadyExistsException(
						"A share with the url " + share.getPath()
								+ " does already exist.");
			}
		}
	}

	@Override
	public Collection<PanboxContact> checkShareDeviceListIntegrity(
			PanboxShare share) {
		Exception e = null;
		LinkedList<PanboxContact> corruptedDLContacts = new LinkedList<PanboxContact>();
		if ((e = share.getException()) != null) {
			logger.warn("One or more device lists in share " + share.getName()
					+ " seem to be corrupt... ");
			if (e instanceof DeviceListException) {
				DeviceListException ex = (DeviceListException) e;
				Collection<PublicKey> coll = ex.getUserKeys();
				for (Iterator<PublicKey> it = coll.iterator(); it.hasNext();) {
					PublicKey publicKey = (PublicKey) it.next();
					logger.warn("Device list "
							+ Utils.getPubKeyFingerprint(publicKey)
							+ ".db in share " + share.getName()
							+ " seem to be corrupt.");
					PanboxContact c = identity.getAddressbook()
							.getContactBySignaturePubKey(publicKey);
					corruptedDLContacts.add(c);
				}
			}
		}
		return corruptedDLContacts;
	}

	@Override
	public PanboxShare resetShareInvitation(PanboxShare share, String email,
			char[] password) throws UnrecoverableKeyException,
			ShareDoesNotExistException, ShareManagerException,
			ShareMetaDataException {
		// TODO: check if corrupt device list file has to be removed physically
		// first
		return addContactPermission(share, email, password);
	}

	private PanboxShare nameTypeUrlToVolumeData(String shareName,
			String sharePath, StorageBackendType type, UUID uuid,
			char[] password) throws IOException, ShareManagerException,
			ShareMetaDataException, UnrecoverableKeyException {

		if (!new File(sharePath).exists()) {
			throw new FileNotFoundException();
		}

		String metaDataDir = sharePath + File.separator
				+ PanboxConstants.PANBOX_SHARE_METADATA_DIRECTORY
				+ File.separator;
		File metaDataFile = new File(metaDataDir);
		File ownerFile = new File(metaDataDir
				+ PanboxConstants.PANBOX_SHARE_OWNER_FILE);

		String deviceName = Settings.getInstance().getDeviceName();

		PanboxShare pbShare = null;

		// create new share
		if (!metaDataFile.exists()) {
			if (password != null) {
				pbShare = createNewShare(shareName, sharePath, type, password,
						metaDataFile, ownerFile, deviceName);
			} else {
				// empty password field indicates we were trying to load a share
				// from the DB, but the metadata file was missing
				throw new ShareInaccessibleException("Metadatafile for share "
						+ shareName + " could not be found!");
			}
		} else {

			if (!ownerFile.exists() || !ownerFile.canRead()) {
				throw new ShareManagerException("Can't access owner file at "
						+ metaDataFile.getAbsolutePath());
			}

			MessageDigest md = getMessageDigestForOwnerFile();
			byte[] ownerFp = getOwnerFpFromMessageDigest(ownerFile, md);
			byte[] me = md.digest(identity.getPublicKeySign().getEncoded());
			md.reset();

			VolumeParams p = paramsFactory
					.createVolumeParams()
					.setPublicSignatureKey(identity.getPublicKeySign())
					.setDeviceAlias(deviceName)
					.setPublicDeviceKey(
							identity.getPublicKeyForDevice(deviceName))
					.setPrivateDeviceKey(
							identity.getPrivateKeyForDevice(deviceName))
					.setShareName(shareName).setPath(sharePath).setType(type);

			if (Arrays.equals(me, ownerFp)) {
				// I am the owner
				try {
					logger.debug("I am the owner of this preinitialized share, loading...");
					pbShare = service.loadShare(p.setOwnerAlias(
							identity.getEmail()).setOwnerSignatureKey(
							identity.getPublicKeySign()));
				} catch (ShareMetaDataException e) {
					boolean success = false;
					if (e.getCause() instanceof DeviceKeyException) {
						// the sharemetadata is ok, but the user's current
						// device has no keys, possibly because the metadata
						// state has been reverted due to a file conflict. try
						// if re-adding the device works, otherwise show error
						try {
							logger.warn(
									"Detected missing device key. This may be because of the metadata state having been reverted due to a file conflict. Will try re-adding the device ...",
									e);
							if (password == null) {
								password = PasswordEnterDialog
										.invoke(PermissionType.SHARE);
							}
							VolumeParams ptmp = paramsFactory
									.createVolumeParams()
									.setKeys(identity, password)
									.setUserAlias(identity.getEmail())
									.setDeviceAlias(deviceName)
									.setPublicDeviceKey(
											identity.getPublicKeyForDevice(deviceName))
									.setShareName(shareName).setPath(sharePath)
									.setType(type);
							pbShare = service.addDevice(ptmp);
							logger.warn("Successfully re-added device key. Trying to re-run loadShare...");
							pbShare = service.loadShare(p.setOwnerAlias(
									identity.getEmail()).setOwnerSignatureKey(
									identity.getPublicKeySign()));
							success = true;
						} catch (Exception e2) {
							logger.error("Re-addeding device key failed.", e2);
						}
					}
					// else if (e.getCause() instanceof DeviceListException) {
					// try {
					// logger.warn(
					// "Detected corrupt device list. Will try to reset device list by re-inviting user...",
					// e);
					// if (password == null) {
					// password = PasswordEnterDialog
					// .invoke(PermissionType.SHARE);
					// }
					//
					// PublicKey pk = ((DeviceListException) e.getCause())
					// .getUserKey();
					// PanboxContact c = identity.getAddressbook()
					// .getContactBySignaturePubKey(pk);
					//
					// if (c != null) {
					// logger.warn("DeviceListException associated caused by contact list of contact \""
					// + c.getEmail() + "\" ");
					//
					//
					//
					// VolumeParams pinv = paramsFactory.createVolumeParams()
					// .setKeys(identity, password)
					// .setOwnerAlias(identity.getEmail())
					// .setOtherSignatureKey(c.getPublicKeySign())
					// .setOtherEncryptionKey(c.getPublicKeyEnc())
					// .setUserAlias(c.getEmail())
					// .setShareName(shareName).setPath(sharePath)
					// .setType(type);
					//
					// // Add Invitation fingerPrint so invited user can detect
					// // invitation
					// File invitationFolder = new File(sharePath +
					// File.separator
					// + PanboxConstants.PANBOX_SHARE_METADATA_DIRECTORY
					// + File.separator
					// + PanboxConstants.PANBOX_SHARE_INVITATION_DIRECTORY);
					// if (invitationFolder.isFile()) {
					// // invitationFolder is not a folder
					// throw new RuntimeException(
					// "invitation folder is a file, not a folder!");
					// }
					// if (!invitationFolder.exists()) {
					// invitationFolder.mkdir();
					// }
					// String fingerPrint = DigestUtils.md5Hex(c.getCertSign()
					// .getPublicKey().getEncoded());
					// File fpFile = new File(invitationFolder.getAbsolutePath()
					// + File.separator + fingerPrint);
					// fpFile.createNewFile();
					//
					// pbShare = service.inviteUser(p);
					// pbShare.generatePermissionsModel(identity);
					// pbShare = pbShare = service.loadShare(p.setOwnerAlias(
					// identity.getEmail()).setOwnerSignatureKey(
					// identity.getPublicKeySign()));
					//
					// success = true;
					// } else {
					// logger.warn("DeviceListException could not be attributed to any existing contact. Publiy Key fingerprint: \""
					// + Utils.getPubKeyFingerprint(pk)
					// + "\" ");
					// }
					//
					// } catch (Exception e2) {
					// logger.error("Re-inviting user failed.", e2);
					// }
					// }
					if (!success) {
						logger.error("Unable to load preinitialized share!", e);
						throw e;
					}
				}
			} else {
				// I am not the owner
				IPerson owner = getOwnerForShare(md, ownerFp, p);
				if (owner != null) {
					// Found the owner, could be previously initialized share or
					// a new share that i'm invited to

					// Is there an invitation for me?
					File invitationFolder = new File(metaDataDir
							+ PanboxConstants.PANBOX_SHARE_INVITATION_DIRECTORY);
					String fingerPrint = DigestUtils.md5Hex(identity
							.getPublicKeySign().getEncoded());
					File fpFile = new File(invitationFolder.getAbsolutePath()
							+ File.separator + fingerPrint);
					if (invitationFolder.isDirectory() && fpFile.isFile()) {
						// I have been invited to this share
						// accept invitation and delete invitational
						// fingerprint
						pbShare = acceptInvitation(password, deviceName, p);

						logger.debug("Accepted invitation, deleting invitational fingerprint...");
						if (!fpFile.delete()) {
							logger.warn("Could not delete invitational fingerprint file from invitations folder: "
									+ fpFile.getAbsolutePath());
							logger.warn("If this share is added again it will try to accept an invitation that has already been accepted.");
						}
					} else {
						// There is no invitation for me lying around, so
						// i'll just assume that this is a previously
						// existing and correctly initialized share
						logger.debug("Found owner, assuming preinitialized Share...");
						try {
							pbShare = service.loadShare(p);
						} catch (ShareMetaDataException e) {
							boolean success = false;
							if (e.getCause() instanceof DeviceKeyException) {
								// the sharemetadata is ok, but the user's
								// current device has no keys, possibly because
								// the metadata state has been reverted due to a
								// file conflict. try if re-adding the device
								// works, otherwise show error
								try {
									logger.warn(
											"Detected missing device key. This may be because of the metadata state having been reverted due to a file conflict. Will try re-adding the device ...",
											e);
									if (password == null)
										password = PasswordEnterDialog
												.invoke(PermissionType.SHARE);
									VolumeParams ptmp = paramsFactory
											.createVolumeParams()
											.setKeys(identity, password)
											.setUserAlias(identity.getEmail())
											.setDeviceAlias(deviceName)
											.setPublicDeviceKey(
													identity.getPublicKeyForDevice(deviceName))
											.setShareName(shareName)
											.setPath(sharePath).setType(type);
									pbShare = service.addDevice(ptmp);
									logger.warn("Successfully re-added device key. Trying to re-run loadShare...");
									pbShare = service.loadShare(p);
									success = true;
								} catch (Exception e2) {
									logger.error(
											"Re-addeding device key failed.", e);
								}
							}
							if (!success) {
								logger.error(
										"Unable to load preinitialized share!",
										e);
								throw e;
							}
						}
					}
				} else {
					// The Owner Fingerprint in the share did not match
					// any of my contacts or myself. Either the real owner is
					// not in my addressbook, or somebody manipulated the
					// fingerprint in the share

					// TODO: if somebody messed with the owner fingerprint
					// in the share, i could just try to load the share with all
					// my available contacts and myself as owner and see which
					// initialization actually runs through

					throw new UnknownOwnerException(
							"The Owner Fingerprint in the share did not match "
									+ "any of my contacts or myself. Either the real owner is "
									+ "not in my addressbook, or somebody manipulated the "
									+ "fingerprint in the share");
				}
			}

			// share was loaded successfully - now, we still need to check if
			// the current users device list has been marked as corrupted.
			// in this case, accessing the share will fail as no keys will be
			// available. Thus, an exception needs to be thrown at this point
			Exception e = null;
			if ((e = pbShare.getException()) != null) {
				logger.warn("One or more device lists in share "
						+ pbShare.getName() + " seem to be corrupt... ");
				if (e instanceof DeviceListException) {
					DeviceListException ex = (DeviceListException) e;
					Collection<PublicKey> coll = ex.getUserKeys();
					for (Iterator<PublicKey> it = coll.iterator(); it.hasNext();) {
						PublicKey publicKey = (PublicKey) it.next();
						if (Utils.keysEqual(identity.getPublicKeySign(),
								publicKey)) {
							// exception was thrown because our own devicelist
							// was corrupt. this means no sharekey or
							// obfuscationkeys will be available.
							logger.fatal("Own device list "
									+ Utils.getPubKeyFingerprint(publicKey)
									+ ".db in share "
									+ pbShare.getName()
									+ " seems to be corrupt. Share will not be available.");
							throw new ShareMetaDataException(
									"Could not verify signature of device list",
									ex);
						}
						logger.warn("Device list "
								+ Utils.getPubKeyFingerprint(publicKey)
								+ ".db in share " + pbShare.getName()
								+ " seems to be corrupt.");
					}
				}
			}
		}
		pbShare.generatePermissionsModel(identity);
		return pbShare;
	}

	private PanboxShare nameTypeUrlToVolumeData(String shareName,
			String sharePath, StorageBackendType type, UUID uuid)
			throws IOException, ShareManagerException,
			UnrecoverableKeyException, ShareMetaDataException {
		return nameTypeUrlToVolumeData(shareName, sharePath, type, uuid, null);
	}

	private PanboxShare nameTypeUrlToVolumeData(PanboxShare share,
			char[] password) throws NoSuchAlgorithmException, IOException,
			ShareManagerException, UnrecoverableKeyException,
			ShareMetaDataException {
		return nameTypeUrlToVolumeData(share.getName(), share.getPath(),
				share.getType(), share.getUuid(), password);
	}

	private PanboxShare acceptInvitation(char[] password, String deviceName,
			VolumeParams p) throws UnrecoverableKeyException,
			ShareMetaDataException, RemoteException, FileNotFoundException {
		PanboxShare pbShare;
		logger.debug("Detected invitation, accepting it...");

		if (password == null) {
			password = PasswordEnterDialog
					.invoke(PasswordEnterDialog.PermissionType.SHARE);
		}
		p.setKeys(identity, password)
				.setPublicDeviceKey(identity.getPublicKeyForDevice(deviceName))
				.setPrivateDeviceKey(
						identity.getPrivateKeyForDevice(deviceName))
				.setUserAlias(identity.getEmail());
		pbShare = service.acceptInviation(p);
		return pbShare;
	}

	private IPerson getOwnerForShare(MessageDigest md, byte[] ownerFp,
			VolumeParams p) {
		IPerson owner = null;
		for (PanboxContact contact : identity.getAddressbook().getContacts()) {
			byte[] c = md.digest(contact.getCertSign().getPublicKey()
					.getEncoded());
			md.reset();
			if (Arrays.equals(ownerFp, c)) {
				owner = contact;
				p.setOwnerAlias(owner.getEmail()).setOwnerSignatureKey(
						owner.getPublicKeySign());
				break;
			}
		}
		return owner;
	}

	private byte[] getOwnerFpFromMessageDigest(File ownerFile, MessageDigest md)
			throws IOException {
		byte[] ownerFp = new byte[md.getDigestLength()];
		try (BufferedInputStream bis = new BufferedInputStream(
				new FileInputStream(ownerFile))) {
			bis.read(ownerFp);
		}
		return ownerFp;
	}

	private MessageDigest getMessageDigestForOwnerFile()
			throws ShareManagerException, IOException {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance(
					KeyConstants.PUBKEY_FINGERPRINT_DIGEST,
					KeyConstants.PROV_BC);
		} catch (NoSuchProviderException | NoSuchAlgorithmException e1) {
			throw new ShareManagerException(
					"Error initializing message-digest!", e1);
		}
		return md;
	}

	private PanboxShare createNewShare(String shareName, String sharePath,
			StorageBackendType type, char[] password, File metaDataFile,
			File ownerFile, String deviceName) throws IOException,
			UnrecoverableKeyException, ShareMetaDataException,
			ShareManagerException {
		PanboxShare pbShare;// Creating new Share

		if (Settings.getInstance().isSlave()) {
			// Slave clients are not allowed to create a new share!
			throw new CreateShareNotAllowedException(
					"Slave is not allowed to create a new share.");
		}

		try {
			logger.debug("Creating new Share...");
			VolumeParams p = paramsFactory
					.createVolumeParams()
					.setOwnerAlias(identity.getEmail())
					.setDeviceAlias(deviceName)
					.setPublicDeviceKey(
							identity.getPublicKeyForDevice(deviceName))
					.setKeys(identity, password)
					.setPrivateDeviceKey(
							identity.getPrivateKeyForDevice(deviceName))
					.setShareName(shareName).setPath(sharePath).setType(type);

			metaDataFile.mkdirs();

			pbShare = service.createShare(p);

			// write uuid
			Files.createFile(Paths.get(metaDataFile.getAbsolutePath()
					+ File.separator + PanboxConstants.PANBOX_SHARE_UUID_PREFIX
					+ pbShare.getUuid()));

			// Owner ID is the SHA256-Hash of the PublicSignKey
			MessageDigest md = MessageDigest.getInstance(
					KeyConstants.PUBKEY_FINGERPRINT_DIGEST,
					KeyConstants.PROV_BC);
			try (BufferedOutputStream bos = new BufferedOutputStream(
					new FileOutputStream(ownerFile))) {
				bos.write(md.digest(identity.getPublicKeySign().getEncoded()));
				bos.flush();
			}
		} catch (IllegalArgumentException | NoSuchProviderException
				| NoSuchAlgorithmException e) {
			throw new ShareManagerException("Could not create share!", e);
		}
		return pbShare;
	}

	public VolumeParamsFactory getParamsFactory() {
		return paramsFactory;
	}

	public String getOnlineFilename(PanboxShare share, String path)
			throws ShareManagerException {
		VolumeParams p = paramsFactory.createVolumeParams()
				.setShareName(share.getName()).setPath(share.getPath());
		try {
			return service.getOnlineFilename(p, path);
		} catch (RemoteException | FileNotFoundException | ObfuscationException e) {
			throw new ShareManagerException(
					"Failed to resolve online filename for path " + path, e);
		}
	}

	@Override
	public PanboxShare reloadShareMetadata(PanboxShare share)
			throws ShareManagerException {
		VolumeParams p = paramsFactory.createVolumeParams()
				.setPublicSignatureKey(identity.getPublicKeySign())
				.setShareName(share.getName()).setPath(share.getPath())
				.setType(share.getType());
		try {
			return service.reloadShareMetaData(p);
		} catch (RemoteException | FileNotFoundException
				| ShareMetaDataException e) {
			throw new ShareManagerException(
					"Failed to reload share metadata for share "
							+ share.getName(), e);
		}
	}
}
