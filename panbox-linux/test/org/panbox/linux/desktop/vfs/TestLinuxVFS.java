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
package org.panbox.linux.desktop.vfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.panbox.core.crypto.AbstractObfuscatorFactory;
import org.panbox.core.crypto.CryptCore;
import org.panbox.core.crypto.FileObfuscatorFactory;
import org.panbox.core.crypto.Obfuscator;
import org.panbox.core.crypto.io.AESGCMRandomAccessFile;
import org.panbox.core.exception.ObfuscationException;
import org.panbox.core.exception.ShareMetaDataException;
import org.panbox.core.exception.SymmetricKeyDecryptionException;
import org.panbox.core.exception.SymmetricKeyNotFoundException;
import org.panbox.core.identitymgmt.Identity;
import org.panbox.core.identitymgmt.SimpleAddressbook;
import org.panbox.core.keymgmt.EncryptedShareKey;
import org.panbox.core.keymgmt.ShareKey;
import org.panbox.core.keymgmt.Volume;
import org.panbox.core.keymgmt.VolumeParams;
import org.panbox.core.keymgmt.VolumeParams.VolumeParamsFactory;
import org.panbox.desktop.common.identitymgmt.sqlightimpl.AddressbookManager;
import org.panbox.desktop.common.identitymgmt.sqlightimpl.IdentityManager;
import org.panbox.desktop.common.vfs.DropboxVirtualVolume;
import org.panbox.desktop.common.vfs.FuseUserFS;
import org.panbox.desktop.common.vfs.PanboxFSLinux;
import org.panbox.desktop.common.vfs.backend.VFSShare;
import org.panbox.desktop.common.vfs.backend.VirtualRootVolume;
import org.panbox.test.AbstractTest;

/**
 * implemented tests:
 * <p/>
 * - create files and folders - rename file - obfuscation
 */
public class TestLinuxVFS extends AbstractTest {

	final char[] PASSWORD = "test".toCharArray();
	final String DEVICE_NAME = "Laptop";

	private SecretKey symKey;
	private ShareKey sk;

	private PublicKey mKey_pub;
	private PrivateKey mKey_priv;
	private PublicKey devKey_pub;
	private PrivateKey devKey_priv;

	@Rule
	public TemporaryFolder tmpTestDir = new TemporaryFolder();

	private String mount;
	private String mountVFS;
	private String metadataPath;

	final static String TEST_SHARE_NAME = "testShare";
	private String mountVFS_prefix;

	private static List<String> virtualFiles = new ArrayList<String>();
	private static PanboxFSLinux loop = null;

	private SimpleAddressbook mAddressbook;
	private Identity owner;

	@Before
	public void setUp() throws IOException, InterruptedException,
			ObfuscationException {

		// Hack to set Settings and Confdir in a way that doesn't affect the
		// real runtime environment
		setupSettings();
		mAddressbook = new SimpleAddressbook();

		mount = tmpTestDir.newFolder(".panboxTest").getAbsolutePath();
		metadataPath = tmpTestDir.newFolder(".metadata").getAbsolutePath();
		mountVFS = tmpTestDir.newFolder(".virtualPanbox").getAbsolutePath();
		mountVFS_prefix = mountVFS + File.separator + TEST_SHARE_NAME;

		// Create identity
		createIdentity();

		// Create metadata
		createMetadata();

		// Retrieve the metadata
		Volume v = new Volume(metadataPath);
		try {
			v.loadShareMetaData(mKey_pub);
		} catch (Exception e2) {
			fail(e2.getMessage());
		}

		// Get the encrypted obfuscationkey
		byte[] encObfKey;
		EncryptedShareKey esk;
		// ShareKey sk = null;
		try {
			encObfKey = v.getEncryptedObfuscationKey(devKey_pub);
			assertNotNull(encObfKey);
			symKey = CryptCore.decryptSymmertricKey(encObfKey, devKey_priv);
			assertNotNull(symKey);
			esk = v.getLatestEncryptedShareKey(devKey_pub);
			sk = new ShareKey(CryptCore.decryptSymmertricKey(esk.encryptedKey,
					devKey_priv), esk.version);
		} catch (SymmetricKeyNotFoundException e1) {
			fail(e1.getMessage());
		} catch (SymmetricKeyDecryptionException e1) {
			fail(e1.getMessage());
		}

		Obfuscator obfuscator = null;
		try {
			obfuscator = AbstractObfuscatorFactory.getFactory(
					FileObfuscatorFactory.class).getInstance(mount, "myShareName");
		} catch (ClassNotFoundException | InstantiationException
				| IllegalAccessException | ObfuscationException e1) {
			e1.printStackTrace();
			fail(e1.getMessage());
		}

		// files in Dropboxfolder
		debug("create list with files in dropbox (obfuscated)");

		String unbenannterOrdner = obfuscator.obfuscate("Unbenannter Ordner",
				symKey, true);
		String testTxt = obfuscator.obfuscate("test.txt", symKey, true);
		String ordner1 = obfuscator.obfuscate("Ordner1", symKey, true);
		String ordner2 = obfuscator.obfuscate("Ordner2", symKey, true);
		String smartcardCfg = obfuscator.obfuscate("smartcard.cfg", symKey,
				true);

		List<String> dFiles = new ArrayList<String>();
		dFiles.add(File.separator + unbenannterOrdner + File.separator
				+ testTxt);
		dFiles.add(File.separator + ordner1 + File.separator + ordner2
				+ File.separator + testTxt);
		dFiles.add(File.separator + testTxt);
		dFiles.add(File.separator + smartcardCfg);

		// create subdirs
		debug("create testdirs");
		new File(mount + File.separator + unbenannterOrdner).mkdirs();
		new File(mount + File.separator + ordner1 + File.separator + ordner2)
				.mkdirs();

		// expected files
		debug("create list with files in VFS (deobfucated)");
		virtualFiles.add(File.separator + "Unbenannter Ordner" + File.separator
				+ "test.txt");
		virtualFiles.add(File.separator + "Ordner1" + File.separator
				+ "Ordner2" + File.separator + "test.txt");
		virtualFiles.add(File.separator + "test.txt");
		virtualFiles.add(File.separator + "smartcard.cfg");

		assertEquals("dFiles.size() != virtualFiles.size()",
				virtualFiles.size(), dFiles.size());

		// create files for testing
		debug("create Files in " + mount);
		for (int i = 0; i < dFiles.size(); i++) {
			if ((dFiles.get(i).equals(obfuscator.obfuscatePath(
					virtualFiles.get(i), symKey, true)))
					&& (virtualFiles.get(i).equals(obfuscator.deObfuscatePath(
							dFiles.get(i), symKey)))) {
				try {
					File f = new File(mount + dFiles.get(i));
					AESGCMRandomAccessFile.create(sk.version, sk.key, f)
							.close();

				} catch (Exception e) {
					fail("Can not create File: " + mount + dFiles.get(i) + "\n"
							+ e.getMessage() + e.getStackTrace());
				}
			} else {
				fail("deObfuscatePath(...) or obfuscatePath(...) does not work like expected ("
						+ obfuscator.deObfuscatePath(dFiles.get(i), symKey)
						+ " != " + virtualFiles.get(i) + ")");
			}
		}
	}

	private ArrayList<String> getDir(String root, String path,
			ArrayList<String> list) {
		File dir = new File(root + path);
		File[] fileList = dir.listFiles();

		for (File f : fileList) {
			if (f.isDirectory())
				getDir(root, path + File.separator + f.getName(), list);
			else
				list.add(path + File.separator + f.getName());
		}
		return list;
	}

	// collect filenames in List
	public ArrayList<String> getDir(String root) {
		return getDir(root, "", new ArrayList<String>());
	}

	private void listCompare(List<String> listA, List<String> listB, String msg) {
		for (String a : listA) {
			boolean contains = false;
			for (String b : listB) {
				if (a.equals(b)) {
					contains = true;
					break;
				}
			}
			assertTrue("File " + a + " is not in " + msg, contains);
		}
	}

	@Test
	public void createTestFiles() throws InterruptedException {
		// create Dirs
		debug("create dirs " + mount + " and " + mountVFS);
		File mountPath = new File(mount);
		assertFalse("can't create dir: " + mount, !mountPath.exists()
				&& !mountPath.mkdir());
		File mountVirt = new File(mountVFS);
		assertFalse("can't create dir: " + mountVFS, !mountVirt.exists()
				&& !mountVirt.mkdir());

		// mount
		debug("mount");
		DropboxVirtualVolume vfs;
		try {
			vfs = new DropboxVirtualVolume(mount);

			Volume v = new Volume(metadataPath);
			v.loadShareMetaData(mKey_pub);
			VirtualRootVolume.getInstance().registerShare(
					new VFSShare(TEST_SHARE_NAME, mount, vfs, v, new KeyPair(
							devKey_pub, devKey_priv)));
		} catch (Exception e1) {
			e1.printStackTrace();
			fail(e1.getMessage());
		}

		FuseUserFS fuse = new FuseUserFS();
		loop = new PanboxFSLinux(fuse);

		// non-blocking vfs mount
		loop.mount(new File(mountVFS), false, null);

		while (!(new File(mountVFS_prefix)).exists()) {
			System.err.println("waiting for mountpoint ..");
			Thread.sleep(10);
		}

		ArrayList<String> al = getDir(mountVFS_prefix);

		// test, if Files in VFS are the same like expected
		debug("compare files in VFS and expected files");
		listCompare(al, virtualFiles, "VFS");
		listCompare(virtualFiles, al, "Dropbox");

		// write plaintext, read plaintext
		debug("compare filecontent in VFS");
		assertTrue("Writecontent failed", writeContent(virtualFiles));
		assertTrue("Contentcheck failed", checkContent(virtualFiles));

		debug("move file: " + virtualFiles.get(1) + " --> "
				+ virtualFiles.get(1) + ".bak");

		File source = new File(mountVFS_prefix + File.separator
				+ "testfile.txt");
		try {
			source.createNewFile();
			debug("created: " + source.getName());
		} catch (IOException e) {
			e.printStackTrace();
			fail("Create file failed");
		}

		File target = new File(mountVFS_prefix + File.separator
				+ "testfile.bak");
		source.renameTo(target);

		assertTrue("file move failed" + target.getAbsolutePath(),
				target.exists());
		debug("file moved successful");
	}

	private boolean writeContent(List<String> virtualFiles) {
		boolean res = true;

		for (String s : virtualFiles) {
			try {
				RandomAccessFile file = new RandomAccessFile(mountVFS_prefix
						+ s, "rw");
				file.write((s + "\tTestTestTest").getBytes());
				file.close();
			} catch (FileNotFoundException e) {
				res = false;
				e.printStackTrace();
			} catch (IOException e) {
				res = false;
				e.printStackTrace();
			}

		}
		return res;
	}

	private boolean checkContent(List<String> virtualFiles) {
		boolean res = true;

		for (String s : virtualFiles) {
			try {
				FileReader reader = new FileReader(mountVFS_prefix + s);
				BufferedReader br = new BufferedReader(reader);
				String content;
				while ((content = br.readLine()) != null) {
					assertTrue("ContentError: " + s,
							content.equals(s + "\tTestTestTest"));
				}
				reader.close();
			} catch (Exception e) {
				res = false;
			}
		}

		return res;
	}

	public static boolean deleteDir(File path) {
		boolean res = true;
		for (File file : path.listFiles()) {
			if (file.isDirectory())
				res &= deleteDir(file);
			res &= file.delete();
		}
		return path.delete() & res;
	}

	private void delFileIfExists(String dir, String file) {
		File f = null;
		if (dir.isEmpty())
			f = new File(file);
		else
			f = new File(dir, file);

		if (f.exists())
			f.delete();
	}

	@After
	public void tearDown() throws Exception {
		loop.unmount();
		// deletion of VFS + backend Folders handled by junit
	}

	public void createIdentity() {
		// Delete old files
		IdentityManager idm = IdentityManager.getInstance();
		AddressbookManager aBookMgr = new AddressbookManager();
		idm.init(aBookMgr);

		Identity id = new Identity(mAddressbook);

		KeyPair ownerKeySign = CryptCore.generateKeypair();
		assertNotNull(ownerKeySign);

		KeyPair ownerKeyEnc = CryptCore.generateKeypair();
		assertNotNull(ownerKeyEnc);

		id.setOwnerKeySign(ownerKeySign, PASSWORD);
		id.setOwnerKeyEnc(ownerKeyEnc, PASSWORD);

		KeyPair deviceKeyLaptop = CryptCore.generateKeypair();
		assertNotNull(deviceKeyLaptop);
		id.addDeviceKey(deviceKeyLaptop, DEVICE_NAME);

		id.setEmail("max@mustermann.org");
		id.setFirstName("Max");
		id.setName("Mustermann");

		// store identity
		idm.storeMyIdentity(id);
	}

	public void createMetadata() {
		IdentityManager idm = IdentityManager.getInstance();
		AddressbookManager aBookMgr = new AddressbookManager();
		idm.init(aBookMgr);
		Identity loadedID = (Identity) idm.loadMyIdentity(mAddressbook);
		owner = loadedID;

		try {
			mKey_priv = loadedID.getPrivateKeySign(PASSWORD);
		} catch (UnrecoverableKeyException e2) {
			e2.printStackTrace();
		}
		mKey_pub = loadedID.getPublicKeySign();
		try {
			devKey_priv = loadedID.getPrivateKeyEnc(PASSWORD);
		} catch (UnrecoverableKeyException e1) {
			fail();
		}
		devKey_pub = loadedID.getPublicKeyEnc();

		VolumeParamsFactory pFac = VolumeParamsFactory.getFactory();
		Volume volume = new Volume(metadataPath);
		try {
			VolumeParams p = pFac
					.createVolumeParams()
					.setOwnerAlias("Me")
					.setPublicSignatureKey(mKey_pub)
					.setPrivateSignatureKey(mKey_priv)
					.setPublicEncryptionKey(loadedID.getPublicKeyEnc())
					.setPrivateEncryptionKey(
							loadedID.getPrivateKeyEnc(PASSWORD))
					.setDeviceAlias(DEVICE_NAME).setPublicDeviceKey(devKey_pub);

			volume.createShareMetaData(p);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException(e);
		} catch (ShareMetaDataException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException(e);
		} catch (UnrecoverableKeyException e) {
			fail();
		}
	}
}