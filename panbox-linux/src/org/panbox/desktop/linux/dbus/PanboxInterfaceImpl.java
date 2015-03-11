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
package org.panbox.desktop.linux.dbus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.panbox.Settings;
import org.panbox.core.Utils;
import org.panbox.core.crypto.CryptCore;
import org.panbox.core.csp.CSPAdapterFactory;
import org.panbox.core.csp.ICSPAPIIntegration;
import org.panbox.core.csp.StorageBackendType;
import org.panbox.core.exception.ShareMetaDataException;
import org.panbox.core.identitymgmt.AbstractAddressbookManager;
import org.panbox.core.identitymgmt.AbstractIdentity;
import org.panbox.core.identitymgmt.AbstractIdentityManager;
import org.panbox.core.identitymgmt.Identity;
import org.panbox.core.identitymgmt.PanboxContact;
import org.panbox.core.identitymgmt.SimpleAddressbook;
import org.panbox.core.identitymgmt.VCardProtector;
import org.panbox.core.identitymgmt.exceptions.ContactExistsException;
import org.panbox.core.vfs.configs.VFSShareConfiguration;
import org.panbox.desktop.common.PanboxDesktopConstants;
import org.panbox.desktop.common.gui.AboutWindow;
import org.panbox.desktop.common.gui.RestoreRevisionDialog;
import org.panbox.desktop.common.gui.shares.DropboxPanboxShare;
import org.panbox.desktop.common.gui.shares.FolderPanboxShare;
import org.panbox.desktop.common.gui.shares.PanboxShare;
import org.panbox.desktop.common.identitymgmt.sqlightimpl.AddressbookManager;
import org.panbox.desktop.common.identitymgmt.sqlightimpl.IdentityManager;
import org.panbox.desktop.common.sharemgmt.IShareManager;
import org.panbox.desktop.common.sharemgmt.ShareManagerException;
import org.panbox.desktop.common.sharemgmt.ShareManagerImpl;
import org.panbox.desktop.common.sharemgmt.ShareNameAlreadyExistsException;
import org.panbox.desktop.common.sharemgmt.SharePathAlreadyExistsException;
import org.panbox.desktop.common.vfs.backend.dropbox.DropboxAPIIntegration;
import org.panbox.desktop.common.vfs.backend.dropbox.DropboxAdapterFactory;
import org.panbox.desktop.linux.PanboxClient;
import org.panbox.desktop.linux.VFSControl;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.property.StructuredName;

/**
 * @author Dominik Spychalski
 */
public class PanboxInterfaceImpl implements PanboxInterface {

	VFSControl vfsController = VFSControl.getInstance();

	Logger logger = Logger.getLogger(PanboxInterfaceImpl.class);

	public PanboxInterfaceImpl() {
	}

	@Override
	public String getMountPoint() {
		logger.debug("[DBUS] getMountPoint()");
		return Settings.getInstance().getMountDir();
	}

	@Override
	public boolean isMounted() {
		logger.debug("[DBUS] isMounted()");
		return vfsController.isMounted();
	}

	@Override
	public byte mount() {
		logger.debug("[DBUS] mount()");
		byte ret = StatusCode.DBUS_OK;

		try {
			vfsController.mount();
		} catch (Exception e) {
			ret = StatusCode.getAndLog(logger, e);
		}

		return ret;
	}

	@Override
	public byte unmount() {
		logger.debug("[DBUS] unmount()");
		byte ret = StatusCode.DBUS_OK;

		try {
			vfsController.unmount();
		} catch (Exception e) {
			ret = StatusCode.getAndLog(logger, e);
		}

		return ret;
	}

	@Override
	public byte addShare(String shareName, String shareType, String sharePath,
			byte[] password) {

		logger.debug("[DBUS] addShare (" + shareName + ", " + shareType + ", "
				+ sharePath + ")");

		byte ret = StatusCode.DBUS_OK;

		boolean shareNameAvailable = false;
		boolean sharePathAvailable = false;
		boolean shareMetaDataExists = false;
		StorageBackendType type = null;
		PanboxShare share = null;

		try {
			shareNameAvailable = ShareManagerImpl.getInstance()
					.shareNameAvailable(shareName);
			sharePathAvailable = ShareManagerImpl.getInstance()
					.sharePathAvailable(sharePath);
			shareMetaDataExists = new File(sharePath
					+ (sharePath.endsWith("/") ? "" : File.separator)
					+ ".panbox/pbmeta.db").exists();
			type = StorageBackendType.valueOf(shareType.toUpperCase());

			if (shareNameAvailable && sharePathAvailable) {

				switch (type) {
				case DROPBOX:
					share = new DropboxPanboxShare(null, sharePath, shareName,
							0);
					break;
				case FOLDER:
					share = new FolderPanboxShare(null, sharePath, shareName, 0);
					break;
				default:
					break;
				}

				share = ShareManagerImpl.getInstance().addNewShare(share,
						Utils.toChars(password));
				PanboxClient.getInstance().refreshDeviceShareList();
				// Events.shareChanged();
			}
		} catch (ShareManagerException | ShareMetaDataException
				| ShareNameAlreadyExistsException
				| SharePathAlreadyExistsException e) {

			ret = StatusCode.getAndLog(logger, e);

		} catch (UnrecoverableKeyException e) {

			logger.error("[DBUS] addShare: Wrong password!");
			ret = StatusCode.getAndLog(logger, e);
			// cleanup share .panbox directory
			// if the password is inserted wrong, parts of the pbmeta.db
			// will be created and stored(only if addNewShare is called
			// by CLI) this leads to a corrupt database file
			File pbmetadb = new File(share.getPath() + ".panbox/pbmeta.db");
			boolean shareMetaDataExistsNow = pbmetadb.exists();

			if (!shareMetaDataExists && shareMetaDataExistsNow) {
				pbmetadb.delete();
			}
		} catch (IllegalArgumentException e) {

			logger.error("[DBUS] addShare: Share Type '" + shareType
					+ "' not specified!");
			ret = StatusCode.getAndLog(logger, e);
		}
		return ret;
	}

	@Override
	public byte editShare(String oldShareName, String newShareName,
			String newShareType, String newSharePath) {

		logger.debug("[DBUS] editShare (" + oldShareName + ", " + newShareName
				+ ", " + newShareType + ", " + newSharePath + ")");
		// TODO: Implement for use with command line
		return StatusCode.DBUS_OK;
	}

	@Override
	public byte shareDirectory(String path) {

		logger.debug("[DBUS] shareDirectory (" + path + ")");

		byte ret = StatusCode.DBUS_OK;
		String shareName = "";

		if (path.startsWith(Settings.getInstance().getMountDir()))
			shareName = path.replace(Settings.getInstance().getMountDir(), "")
					.split("/")[1];

		try {
			PanboxShare share = ShareManagerImpl.getInstance().getShareForName(
					shareName);
			ICSPAPIIntegration csp = CSPAdapterFactory.getInstance(
					share.getType()).getAPIAdapter();
			csp.inviteUser(shareName, "");
		} catch (Exception e) {
			ret = StatusCode.getAndLog(logger, e);
		}

		return ret;
	}

	@Override
	public String[] getShares() {
		logger.debug("[DBUS] getShares()");
		byte ret;

		ArrayList<String> list = new ArrayList<>();
		try {
			for (String shareName : ShareManagerImpl.getInstance()
					.getInstalledShares()) {
				list.add(shareName);
			}
		} catch (ShareManagerException e) {
			// TODO return error codes associated with the specific
			// exceptions thrown, and give some corresponding feedback to
			// the user within the cli
			ret = StatusCode.getAndLog(logger, e);
			list.add("ERROR:" + StatusCode.toString(ret));
		}

		return list.toArray(new String[list.size()]);
	}

	@Override
	public byte openProperties() {
		logger.debug("[DBUS] openProperties()");

		byte ret = StatusCode.DBUS_OK;

		try {
			PanboxClient.getInstance().showGui();
		} catch (Exception e) {
			ret = StatusCode.getAndLog(logger, e);
		}
		return ret;
	}

	@Override
	public byte shutdown() {
		logger.debug("[DBUS] shutdown()");

		byte ret = StatusCode.DBUS_OK;

		try {
			if (PanboxClient.getInstance().checkShutdown()) {
				System.exit(0);
			}
		} catch (Exception e) {
			ret = StatusCode.getAndLog(logger, e);
		}

		return ret;
	}

	@Override
	public void about() {
		AboutWindow.getInstance().showWindow(5);
	}

	@Override
	public String getLocale() {
		logger.debug("[DBUS] getLocale()");
		return Settings.getInstance().getLanguage();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.freedesktop.dbus.DBusInterface#isRemote()
	 */
	@Override
	public boolean isRemote() {
		logger.debug("[DBUS] isRemote()");
		// defined in general DBus interface
		return false;
	}

	@Override
	public String getVersion() {
		logger.debug("[DBUS] getVersion()");
		return new String(PanboxDesktopConstants.PANBOX_VERSION);
	}

	public String[] getCSPs() {
		logger.debug("[DBUS] getCSPs()");
		return VFSShareConfiguration.getShareTypes();
	}

	private boolean shareExists(String share) {
		logger.debug("[DBUS] shareExists(" + share + ")");
		for (String s : getShares()) {
			if (s.equals(share)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public byte removeShare(String shareName, boolean rmDir) {
		logger.debug("[DBUS] removeShare(" + shareName + ", " + rmDir + ")");

		byte ret = StatusCode.DBUS_OK;

		if (shareExists(shareName)) {
			try {
				PanboxShare share = ShareManagerImpl.getInstance()
						.getShareForName(shareName);
				File shareRootDir = new File(share.getPath());
				ShareManagerImpl.getInstance().removeShare(share.getName(),
						share.getPath(), share.getType());
				if (rmDir) {
					FileUtils.deleteDirectory(shareRootDir);
				}
			} catch (Exception e) {
				ret = StatusCode.getAndLog(logger, e);
			}
		} else {
			ret = StatusCode.buildcode(StatusCode.DBUS,
					StatusCode.SHARENOTEXISTS);
		}
		return ret;
	}

	@Override
	public byte exportOwnIdentity(String path) {
		logger.debug("[DBUS] exportOwnIdentity(" + path + ")");

		byte ret = StatusCode.DBUS_OK;

		try {
			IdentityManager idm = IdentityManager.getInstance();
			idm.exportMyIdentity(PanboxClient.getInstance().getIdentity(),
					new File(path));
		} catch (Exception e) {
			ret = StatusCode.getAndLog(logger, e);
		}

		return ret;
	}

	@Override
	public String[][] getContacts() {
		logger.debug("[DBUS] getContacts()");

		Collection<PanboxContact> contacts = PanboxClient.getInstance()
				.getIdentity().getAddressbook().getContacts();

		String[][] ret = new String[contacts.size()][2];
		int i = 0;

		for (PanboxContact c : contacts) {
			ret[i][0] = String.valueOf(c.getID());
			ret[i][1] = c.getFirstName() + " " + c.getName() + " ("
					+ c.getEmail() + ")";

			i++;
		}
		return ret;
	}

	@Override
	public byte exportContacts(String[] contactIDs, String path) {

		String ids = "";
		for (int i = 0; i < contactIDs.length; i++) {
			ids = ids + "," + contactIDs[i];
		}
		logger.debug("[DBUS] exportContacts(" + ids + ", " + path + ")");

		byte ret = StatusCode.DBUS_OK;

		Collection<PanboxContact> contacts = PanboxClient.getInstance()
				.getIdentity().getAddressbook().getContacts();

		Collection<String> idList = Arrays.asList(contactIDs);

		Collection<VCard> vcards = new LinkedList<VCard>();

		for (PanboxContact c : contacts) {
			if (idList.contains(String.valueOf(c.getID()))) {
				vcards.add(AbstractAddressbookManager.contact2VCard(c));
			}
			if (vcards.size() == idList.size()) {
				break;
			}
		}

		try {
			File file = new File(path);

			if (file.getParentFile().exists()) {
				Ezvcard.write(vcards).go(file);
			}
		} catch (IOException e) {
			ret = StatusCode.getAndLog(logger, e);
		}

		return ret;
	}

	public byte verifyContacts(String vcard, String pin) {
		byte code = StatusCode.ADDRESSBOOKMANAGER_OK;
		File card = new File(vcard);
		File tmpFile = null;
		byte[] hmac;
		byte[] vcfbytes;
		boolean authVerified = false;

		if (!card.exists()) {
			code = StatusCode.buildAndLog(logger,
					StatusCode.ADDRESSBOOKMANAGER, StatusCode.IOERROR,
					"verifyContacts: vCard-File not found!");
			return code;
		}
		try {
			if (pin != "") {
				tmpFile = File.createTempFile(
						"panbox-tmp-"
								+ String.valueOf(System.currentTimeMillis()),
						".vcf");
				hmac = VCardProtector.unwrapVCF(card, tmpFile);
				vcfbytes = VCardProtector.loadVCFBytes(tmpFile);
				authVerified = VCardProtector.verifyVCFIntegrity(vcfbytes,
						hmac, pin.toCharArray());
			}

			if (pin.equals("") || !authVerified) {
				code = StatusCode.buildcode(StatusCode.ADDRESSBOOKMANAGER,
						StatusCode.PINVERIFICATIONFAILED);
			}

		} catch (IOException e) {
			code = StatusCode.getAndLog(logger, e);
		}

		return code;
	}

	public String[][] getContacts(String vcard) {
		File vcardFile = new File(vcard);
		File tmpFile = null;

		IdentityManager idm = IdentityManager.getInstance();

		int index = 0;
		String[][] ret;

		ArrayList<String> tmp = new ArrayList<String>();

		if (!vcardFile.exists()) {
			return null;
		}

		tmpFile = readVcfZip(vcardFile);

		idm.getAddressBookManager();
		VCard[] cards = AbstractAddressbookManager.readVCardFile(tmpFile);

		for (VCard card : cards) {
			StructuredName sn = card.getStructuredName();
			tmp.add(index, sn.getGiven() + " " + sn.getFamily() + " ("
					+ card.getEmails().get(0).getValue() + ")");
			index++;
		}

		ret = new String[tmp.size()][2];

		for (index = 0; index < tmp.size(); index++) {
			ret[index][0] = String.valueOf(index);
			ret[index][1] = tmp.get(index);
		}

		return ret;

	}

	private File readVcfZip(File vcfZip) {
		File tmpFile = null;

		if (vcfZip.exists()) {
			try {
				tmpFile = File.createTempFile(
						"panbox-tmp-"
								+ String.valueOf(System.currentTimeMillis()),
						".vcf");
				VCardProtector.unwrapVCF(vcfZip, tmpFile);
			} catch (IOException e) {
				StatusCode.buildAndLog(logger, StatusCode.DBUS,
						StatusCode.IOERROR, "Error on unpack contacts-file '"
								+ vcfZip.getAbsolutePath() + "'!");
			}
		}

		return tmpFile;
	}

	public byte importContact(String[] contactIDs, String vcard,
			boolean authVerified) {
		logger.debug("[DBUS] importContact(" + vcard + ")");
		byte code = StatusCode.ADDRESSBOOKMANAGER_OK;
		File file = new File(vcard);
		File tmpFile;

		if (file.exists()) {
			IdentityManager idm = IdentityManager.getInstance();
			tmpFile = readVcfZip(file);

			try {
				idm.getAddressBookManager().importContacts(
						PanboxClient.getInstance().getIdentity(), tmpFile,
						authVerified);
			} catch (ContactExistsException e) {
				code = StatusCode.getAndLog(logger, e);
			}
			// persist import of contact
			idm.storeMyIdentity(PanboxClient.getInstance().getIdentity());
			PanboxClient.getInstance().refreshAddressbookList();
		} else {
			code = StatusCode.buildAndLog(logger,
					StatusCode.ADDRESSBOOKMANAGER, StatusCode.IOERROR,
					"importContacts: vCard-File not found");
		}

		return code;
	}

	@Override
	public byte addContact(String mail, String firstname, String lastname) {
		logger.debug("[DBUS] addContact(" + mail + ", " + firstname + ", "
				+ lastname + ")");
		byte ret = StatusCode.DBUS_OK;

		try {
			PanboxContact contact = new PanboxContact();
			contact.setEmail(mail);
			contact.setFirstName(firstname);
			contact.setName(lastname);

			KeyPair ownerKeySign = CryptCore.generateKeypair();
			KeyPair ownerKeyEnc = CryptCore.generateKeypair();

			contact.setCertEnc(CryptCore.createSelfSignedX509Certificate(
					ownerKeyEnc.getPrivate(), ownerKeyEnc.getPublic(), contact));
			contact.setCertSign(CryptCore.createSelfSignedX509Certificate(
					ownerKeySign.getPrivate(), ownerKeySign.getPublic(),
					contact));

			PanboxClient.getInstance().getIdentity().getAddressbook()
					.addContact(contact);
			IdentityManager.getInstance().storeMyIdentity(
					PanboxClient.getInstance().getIdentity());
		} catch (Exception e) {
			ret = StatusCode.getAndLog(logger, e);
		}

		return ret;
	}

	@Override
	public byte deleteContact(String[] contactIDs) {

		String ids = "";
		for (int i = 0; i < contactIDs.length; i++) {
			ids = ids + "," + contactIDs[i];
		}
		logger.debug("[DBUS] deleteContact(" + ids + ")");

		byte ret = StatusCode.DBUS_OK;

		try {
			Collection<PanboxContact> contacts = PanboxClient.getInstance()
					.getIdentity().getAddressbook().getContacts();

			Collection<String> idList = Arrays.asList(contactIDs);
			int deleted = 0;

			for (PanboxContact c : contacts) {

				if (idList.contains(String.valueOf(c.getID()))) {
					PanboxClient.getInstance().getIdentity().getAddressbook()
							.deleteContact(c.getEmail());
					deleted++;
				}

				if (deleted == idList.size()) {
					break;
				}
			}
		} catch (Exception e) {
			ret = StatusCode.getAndLog(logger, e);
		}
		return ret;
	}

	public String[][] getOwnIdentity() {
		logger.debug("[DBUS] getOwnIdentity()");
		AbstractIdentity id = PanboxClient.getInstance().getIdentity();
		String[][] res = new String[5][2];
		res[0][0] = "mail";
		res[0][1] = id.getEmail();

		res[1][0] = "firstname";
		res[1][1] = id.getFirstName();

		res[2][0] = "lastname";
		res[2][1] = id.getName();

		res[3][0] = "pkenc";
		res[3][1] = id.getPublicKeyEnc().toString();

		res[4][0] = "pksig";
		res[4][1] = id.getPublicKeySign().toString();

		return res;
	}

	@Override
	public byte resetIdentity(String email, String firstname, String lastname,
			byte[] password, String devicename, boolean backup) {

		logger.debug("[DBUS] resetIdentity(" + email + ", " + firstname + ", "
				+ lastname + ", " + devicename + ", " + backup + ")");
		byte ret = StatusCode.DBUS_OK;

		try {
			if (backup) {
				backupIdentity();
			}

			deleteIdentity();
			createIdentity(email, firstname, lastname, password, devicename);
		} catch (Exception e) {
			ret = StatusCode.getAndLog(logger, e);
		}
		return ret;
	}

	@Override
	public byte deleteIdentity() {
		logger.debug("[DBUS] deleteIdentity()");

		byte ret = StatusCode.DBUS_OK;

		try {
			IShareManager shareMgr = ShareManagerImpl.getInstance();

			File[] confFiles = new File(Settings.getInstance().getConfDir())
					.listFiles();
			for (File f : confFiles) {
				f.delete();
			}

			// TODO This would delete sharemetadata! Do we really want this
			// here?
			List<String> installedShareNames = new ArrayList<String>();

			try {
				installedShareNames = shareMgr.getInstalledShares();
			} catch (ShareManagerException e) {

				ret = StatusCode.getAndLog(logger, e);
			}

			List<PanboxShare> installedShares = new ArrayList<PanboxShare>();
			for (String shareName : installedShareNames) {
				PanboxShare share = null;
				try {
					share = shareMgr.getShareForName(shareName);
				} catch (UnrecoverableKeyException | ShareMetaDataException
						| ShareManagerException e) {
					ret = StatusCode.getAndLog(logger, e);
				}
				if (null != share) {
					installedShares.add(share);
				}
			}

			for (PanboxShare share : installedShares) {
				File shareConf = new File(share.getPath() + ".panbox/");
				File[] files = shareConf.listFiles();

				for (File f : files) {
					f.delete();
				}
			}

		} catch (ShareManagerException e) {
			ret = StatusCode.getAndLog(logger, e);
		}

		return ret;
	}

	@Override
	public byte createIdentity(String email, String firstname, String lastname,
			byte[] password, String devicename) {

		logger.debug("[DBUS] createIdentity(" + email + ", " + firstname + ", "
				+ lastname + ", " + devicename + ")");
		byte ret = StatusCode.DBUS_OK;

		try {
			AbstractAddressbookManager addressbook = new AddressbookManager();
			AbstractIdentityManager identityManager = (AbstractIdentityManager) IdentityManager
					.getInstance();

			Identity id = new Identity(new SimpleAddressbook(), email,
					firstname, lastname);
			KeyPair ownerKeySign = CryptCore.generateKeypair();
			KeyPair ownerKeyEnc = CryptCore.generateKeypair();
			KeyPair deviceKey = CryptCore.generateKeypair();

			id.setOwnerKeySign(ownerKeySign, Utils.toChars(password));
			id.setOwnerKeyEnc(ownerKeyEnc, Utils.toChars(password));
			id.addDeviceKey(deviceKey, devicename);
			Settings.getInstance().setDeviceName(devicename);
			identityManager.init(addressbook);
			identityManager.storeMyIdentity(id);
		} catch (Exception e) {
			ret = StatusCode.getAndLog(logger, e);
		}

		return ret;
	}

	@Override
	public byte backupIdentity() {
		logger.debug("[DBUS] backupIdentity()");

		byte ret = StatusCode.DBUS_OK;

		File confDir = new File(Settings.getInstance().getConfDir());
		String backupPath = confDir.getParent() + File.separator + ".pbbackup";
		File backupDir = new File(backupPath);

		if (!backupDir.exists()) {
			backupDir.mkdir();
		}

		Calendar cal = Calendar.getInstance();

		String datetime = String.valueOf(cal.get(Calendar.YEAR))
				+ String.valueOf(cal.get(Calendar.MONTH))
				+ String.valueOf(cal.get(Calendar.DAY_OF_MONTH)) + "_"
				+ String.valueOf(cal.get(Calendar.MINUTE)) + "-"
				+ String.valueOf(cal.get(Calendar.DAY_OF_MONTH)) + "-"
				+ String.valueOf(cal.get(Calendar.SECOND));
		String outputFileName = backupDir.getPath() + File.separator + datetime
				+ ".pbbak";

		List<String> filesToZip = new ArrayList<String>();

		try {
			IShareManager shareMgr = ShareManagerImpl.getInstance();

			File[] confFiles = confDir.listFiles();
			for (File f : confFiles) {
				filesToZip.add(f.getAbsolutePath());
			}

			List<String> installedShareNames = shareMgr.getInstalledShares();

			List<PanboxShare> installedShares = new ArrayList<PanboxShare>();
			for (String shareName : installedShareNames) {
				PanboxShare share = shareMgr.getShareForName(shareName);
				installedShares.add(share);
			}

			for (PanboxShare share : installedShares) {
				File f = new File(share.getPath() + ".panbox/");
				File[] files = f.listFiles();
				for (File file : files) {
					filesToZip.add(file.getAbsolutePath());
				}
			}

			FileOutputStream fos = new FileOutputStream(outputFileName);
			ZipOutputStream zos = new ZipOutputStream(fos);
			zos.setLevel(9);

			for (String ftz : filesToZip) {

				File fileToZip = new File(ftz);

				if (fileToZip.exists()) {
					ZipEntry ze = new ZipEntry(fileToZip.getAbsolutePath());
					zos.putNextEntry(ze);
					FileInputStream in = new FileInputStream(
							fileToZip.getPath());

					int len;
					byte buffer[] = new byte[1024];
					while ((len = in.read(buffer)) > 0) {
						zos.write(buffer, 0, len);
					}
					in.close();
					zos.closeEntry();
				}
			}
			zos.finish();
			fos.close();

		} catch (Exception e) {
			ret = StatusCode.getAndLog(logger, e);
		}

		return ret;
	}

	@Override
	public byte restoreIdentity(String backupFile, boolean backupOldOne) {
		logger.debug("[DBUS] restoreIdentity(" + backupFile + ", "
				+ backupOldOne + ")");

		byte ret = StatusCode.DBUS_OK;

		File bakFile = new File(backupFile);
		FileOutputStream fos = null;
		InputStream eis = null;
		ZipFile zip = null;
		ZipEntry entry = null;

		if (bakFile.exists()) {

			if (backupOldOne) {
				backupIdentity();
			}

			try {
				zip = new ZipFile(bakFile);

				Enumeration<?> zipentries = zip.entries();

				while (zipentries.hasMoreElements()) {
					entry = (ZipEntry) zipentries.nextElement();
					eis = zip.getInputStream(entry);
					byte[] buffer = new byte[1024];
					int bytesRead = 0;

					File f = new File(entry.getName());

					if (f.exists()) {
						f.delete();
					} else {
						if (!f.getParentFile().exists()) {
							f.getParentFile().mkdirs();
						}
					}

					f.createNewFile();
					fos = new FileOutputStream(f);

					while ((bytesRead = eis.read(buffer)) != -1) {
						fos.write(buffer, 0, bytesRead);
					}
					eis.close();
					fos.flush();
					fos.close();

				}

			} catch (Exception e) {
				ret = StatusCode.getAndLog(logger, e);
			} finally {
				if (fos != null) {
					try {
						fos.close();
					} catch (IOException e) {
						ret = StatusCode.getAndLog(logger, e);
					}
				}
				if (zip != null) {
					try {
						zip.close();
					} catch (IOException e) {
						ret = StatusCode.getAndLog(logger, e);
					}
				}
			}
		}
		return ret;
	}

	@Override
	public byte openRevisionGui(String path) {
		String panboxDir = Settings.getInstance().getMountDir();
		String shareName = path.replace(panboxDir, "").split("/")[1];
		String pathInsideShare = path.replace(panboxDir + File.separator
				+ shareName, "");
		DropboxAdapterFactory dbxFac = (DropboxAdapterFactory) CSPAdapterFactory
				.getInstance(StorageBackendType.DROPBOX);
		DropboxAPIIntegration dbApiIntegration = (DropboxAPIIntegration) dbxFac
				.getAPIAdapter();

		RestoreRevisionDialog d = new RestoreRevisionDialog(
				PanboxClient.getInstance(), dbApiIntegration, shareName,
				pathInsideShare);
		d.setLocationRelativeTo(null);
		d.setVisible(true);
		d.toFront();
		return StatusCode.DBUS_OK;
	}

	@Override
	public String getShareStorageBackendType(String shareName) {
		logger.debug("[DBUS] getShareStorageBackendType(" + shareName + ")");

		try {
			PanboxShare share = ShareManagerImpl.getInstance().getShareForName(
					shareName);
			return share.getType().getDisplayName();
		} catch (ShareManagerException | UnrecoverableKeyException
				| ShareMetaDataException e) {
			byte ret = StatusCode.getAndLog(logger, e);
			return "ERROR:" + StatusCode.toString(ret);
		}
	}

}
