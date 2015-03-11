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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.TreeMap;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.panbox.core.exception.ShareMetaDataException;

public class TestAddDevice {
	
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	static PrivateKey A_Key_sig_priv;
	static PublicKey A_Key_sig_pub;
	static PrivateKey A_Key_enc_priv;
	static PublicKey A_Key_enc_pub;
	static PublicKey A_devKey1_enc_pub;
	static PrivateKey A_devKey1_enc_priv;
	static PublicKey A_devKey2_enc_pub;
	static PrivateKey A_devKey2_enc_priv;
	static PublicKey A_devKey3_enc_pub;
	static PrivateKey A_devKey3_enc_priv;

	static PrivateKey B_Key_sig_priv;
	static PublicKey B_Key_sig_pub;
	static PrivateKey B_Key_enc_priv;
	static PublicKey B_Key_enc_pub;
	static PublicKey B_devKey1_enc_pub;
	static PrivateKey B_devKey1_enc_priv;
	static PublicKey B_devKey2_enc_pub;
	static PrivateKey B_devKey2_enc_priv;
	static PublicKey B_devKey3_enc_pub;
	static PrivateKey B_devKey3_enc_priv;

	private String dbName = "./";
	private static String aliasOwner = "Owner";
	private static String aliasGuest = "Guest";
	private static String deviceAliasOwner1 = "OwnerDev1";
	private static String deviceAliasGuest1 = "GuestDev1";
	private static String deviceAliasOwner2 = "OwnerDev2";
	private static String deviceAliasGuest2 = "GuestDev2";
	private static String deviceAliasOwner3 = "OwnerDev3";
	private static String deviceAliasGuest3 = "GuestDev3";

	@BeforeClass
	public static void setUpClass() {
		try {
			KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA",
					new BouncyCastleProvider());
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			gen.initialize(1024, random);

			KeyPair pair = gen.generateKeyPair();
			A_Key_sig_priv = pair.getPrivate();
			A_Key_sig_pub = pair.getPublic();
			gen.generateKeyPair();
			A_Key_enc_priv = pair.getPrivate();
			A_Key_enc_pub = pair.getPublic();
			pair = gen.generateKeyPair();
			A_devKey1_enc_priv = pair.getPrivate();
			A_devKey1_enc_pub = pair.getPublic();
			pair = gen.generateKeyPair();
			A_devKey2_enc_priv = pair.getPrivate();
			A_devKey2_enc_pub = pair.getPublic();
			pair = gen.generateKeyPair();
			A_devKey3_enc_priv = pair.getPrivate();
			A_devKey3_enc_pub = pair.getPublic();

			pair = gen.generateKeyPair();
			B_Key_sig_priv = pair.getPrivate();
			B_Key_sig_pub = pair.getPublic();
			gen.generateKeyPair();
			B_Key_enc_priv = pair.getPrivate();
			B_Key_enc_pub = pair.getPublic();
			pair = gen.generateKeyPair();
			B_devKey1_enc_priv = pair.getPrivate();
			B_devKey1_enc_pub = pair.getPublic();
			pair = gen.generateKeyPair();
			B_devKey2_enc_priv = pair.getPrivate();
			B_devKey2_enc_pub = pair.getPublic();
			pair = gen.generateKeyPair();
			B_devKey3_enc_priv = pair.getPrivate();
			B_devKey3_enc_pub = pair.getPublic();

		} catch (NoSuchAlgorithmException e) {
			fail(e.getMessage());
		}
	}

	@Before
	public void setUp() throws NoSuchMethodException, SecurityException,
			InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, IOException {
		File newFolder = folder.newFolder(".metadata");
		System.out.println("Create temporary folder: " + newFolder.mkdirs());
		dbName = newFolder.getAbsolutePath();
		File db = new File(dbName + File.separator + Volume.DB_FILE);
		if (db.exists())
			db.delete();
	}

	@After
	public void tearDown() {
		File db = new File(dbName + Volume.DB_FILE);
		if (db.exists())
			db.delete();
		System.out.println("cleanup");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void test() {

		// Create Share
		IVolume volume = new Volume(dbName);
		assertNotNull(volume);
		ShareMetaData smd = null;
		try {
			smd = volume.createShareMetaData(aliasOwner, A_Key_sig_pub,
					A_Key_sig_priv, A_Key_enc_pub, A_Key_enc_priv,
					deviceAliasOwner1, A_devKey1_enc_pub);
			assertNotNull(smd);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			fail(e.getMessage());
		}

		assertEquals(1, smd.getSharePartList().size());
		assertEquals(A_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasOwner));
		assertNull(smd.getSharePartList().getPublicKey(aliasGuest));

		assertEquals(1, smd.shareKeys.size());
		assertEquals(0, smd.shareKeys.getLastEntry().getVersion());
		assertEquals(2, smd.shareKeys.getLastEntry().size());
		assertTrue(smd.shareKeys.getLastEntry().getEncryptedKey(A_Key_enc_pub) != null);
		assertTrue(smd.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey1_enc_pub) != null);

		// add device

		try {
			volume.addDevice(aliasOwner, A_Key_sig_pub, A_Key_sig_priv,
					deviceAliasOwner2, A_devKey2_enc_pub, A_Key_enc_pub,
					A_Key_enc_priv);
		} catch (IllegalArgumentException | ShareMetaDataException e) {
			e.printStackTrace(System.err);
			fail(e.getMessage());
		}

		assertEquals(1, smd.getSharePartList().size());
		assertEquals(A_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasOwner));
		assertNull(smd.getSharePartList().getPublicKey(aliasGuest));

		assertEquals(1, smd.shareKeys.size());
		assertEquals(0, smd.shareKeys.getLastEntry().getVersion());
		assertEquals(3, smd.shareKeys.getLastEntry().size());
		assertTrue(smd.shareKeys.getLastEntry().getEncryptedKey(A_Key_enc_pub) != null);
		assertTrue(smd.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey1_enc_pub) != null);
		assertTrue(smd.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey2_enc_pub) != null);
		try {
			ObfuscationKeyDB okdb = smd.obfuscationKeys;
			Field f = ObfuscationKeyDB.class
					.getDeclaredField("obfuscationKeys");
			f.setAccessible(true);
			TreeMap<PublicKey, byte[]> obkeys = (TreeMap<PublicKey, byte[]>) f
					.get(okdb);
			int size = obkeys.size();
			assertEquals(3, size);
			assertTrue(obkeys.containsKey(A_Key_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey1_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey2_enc_pub));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// reload volume to check persistency

		IVolume volume2 = new Volume(dbName);
		ShareMetaData smd2 = null;
		try {

			smd2 = volume2.loadShareMetaData(A_Key_sig_pub);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertEquals(1, smd2.getSharePartList().size());
		assertEquals(A_Key_sig_pub,
				smd2.getSharePartList().getPublicKey(aliasOwner));
		assertNull(smd2.getSharePartList().getPublicKey(aliasGuest));

		assertEquals(1, smd2.shareKeys.size());
		assertEquals(0, smd2.shareKeys.getLastEntry().getVersion());
		assertEquals(3, smd2.shareKeys.getLastEntry().size());
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(A_Key_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey1_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey2_enc_pub) != null);
		try {
			ObfuscationKeyDB okdb = smd2.obfuscationKeys;
			Field f = ObfuscationKeyDB.class
					.getDeclaredField("obfuscationKeys");
			f.setAccessible(true);
			TreeMap<PublicKey, byte[]> obkeys = (TreeMap<PublicKey, byte[]>) f
					.get(okdb);
			int size = obkeys.size();
			assertEquals(3, size);
			assertTrue(obkeys.containsKey(A_Key_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey1_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey2_enc_pub));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// add another device

		try {
			volume.addDevice(aliasOwner, A_Key_sig_pub, A_Key_sig_priv,
					deviceAliasOwner3, A_devKey3_enc_pub, A_Key_enc_pub,
					A_Key_enc_priv);
		} catch (IllegalArgumentException | ShareMetaDataException e) {
			e.printStackTrace(System.err);
			fail(e.getMessage());
		}

		assertEquals(1, smd.getSharePartList().size());
		assertEquals(A_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasOwner));
		assertNull(smd.getSharePartList().getPublicKey(aliasGuest));

		assertEquals(1, smd.shareKeys.size());
		assertEquals(0, smd.shareKeys.getLastEntry().getVersion());
		assertEquals(4, smd.shareKeys.getLastEntry().size());
		assertTrue(smd.shareKeys.getLastEntry().getEncryptedKey(A_Key_enc_pub) != null);
		assertTrue(smd.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey1_enc_pub) != null);
		assertTrue(smd.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey2_enc_pub) != null);
		assertTrue(smd.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey3_enc_pub) != null);
		try {
			ObfuscationKeyDB okdb = smd.obfuscationKeys;
			Field f = ObfuscationKeyDB.class
					.getDeclaredField("obfuscationKeys");
			f.setAccessible(true);
			TreeMap<PublicKey, byte[]> obkeys = (TreeMap<PublicKey, byte[]>) f
					.get(okdb);
			int size = obkeys.size();
			assertEquals(4, size);
			assertTrue(obkeys.containsKey(A_Key_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey1_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey2_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey3_enc_pub));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// reload volume to check persistency

		volume2 = new Volume(dbName);
		smd2 = null;
		try {

			smd2 = volume2.loadShareMetaData(A_Key_sig_pub);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertEquals(1, smd2.getSharePartList().size());
		assertEquals(A_Key_sig_pub,
				smd2.getSharePartList().getPublicKey(aliasOwner));
		assertNull(smd2.getSharePartList().getPublicKey(aliasGuest));

		assertEquals(1, smd2.shareKeys.size());
		assertEquals(0, smd2.shareKeys.getLastEntry().getVersion());
		assertEquals(4, smd2.shareKeys.getLastEntry().size());
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(A_Key_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey1_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey2_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey3_enc_pub) != null);
		try {
			ObfuscationKeyDB okdb = smd2.obfuscationKeys;
			Field f = ObfuscationKeyDB.class
					.getDeclaredField("obfuscationKeys");
			f.setAccessible(true);
			TreeMap<PublicKey, byte[]> obkeys = (TreeMap<PublicKey, byte[]>) f
					.get(okdb);
			int size = obkeys.size();
			assertEquals(4, size);
			assertTrue(obkeys.containsKey(A_Key_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey1_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey2_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey3_enc_pub));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// remove device
/*
		try {
			volume.removeDevice(aliasOwner, A_Key_sig_pub, A_Key_sig_priv,
					deviceAliasOwner1);
		} catch (IllegalArgumentException | ShareMetaDataException e) {
			e.printStackTrace(System.err);
			fail(e.getMessage());
		}

		assertEquals(1, smd.getSharePartList().size());
		assertEquals(A_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasOwner));
		assertNull(smd.getSharePartList().getPublicKey(aliasGuest));

		assertEquals(2, smd.shareKeys.size());
		assertEquals(1, smd.shareKeys.getLastEntry().getVersion());
		assertEquals(3, smd.shareKeys.getLastEntry().size());
		assertTrue(smd.shareKeys.getLastEntry().getEncryptedKey(A_Key_enc_pub) != null);
		assertTrue(smd.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey1_enc_pub) == null);
		assertTrue(smd.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey2_enc_pub) != null);
		assertTrue(smd.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey3_enc_pub) != null);
		try {
			ObfuscationKeyDB okdb = smd.obfuscationKeys;
			Field f = ObfuscationKeyDB.class
					.getDeclaredField("obfuscationKeys");
			f.setAccessible(true);
			TreeMap<PublicKey, byte[]> obkeys = (TreeMap<PublicKey, byte[]>) f
					.get(okdb);
			int size = obkeys.size();
			assertEquals(3, size);
			assertTrue(obkeys.containsKey(A_Key_enc_pub));
			assertTrue(!obkeys.containsKey(A_devKey1_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey2_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey3_enc_pub));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// reload volume to check persistency

		volume2 = new Volume(dbName);
		smd2 = null;
		try {

			smd2 = volume2.loadShareMetaData(A_Key_sig_pub);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertEquals(1, smd2.getSharePartList().size());
		assertEquals(A_Key_sig_pub,
				smd2.getSharePartList().getPublicKey(aliasOwner));
		assertNull(smd2.getSharePartList().getPublicKey(aliasGuest));

		assertEquals(2, smd2.shareKeys.size());
		assertEquals(1, smd2.shareKeys.getLastEntry().getVersion());
		assertEquals(3, smd2.shareKeys.getLastEntry().size());
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(A_Key_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey1_enc_pub) == null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey2_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey3_enc_pub) != null);
		try {
			ObfuscationKeyDB okdb = smd2.obfuscationKeys;
			Field f = ObfuscationKeyDB.class
					.getDeclaredField("obfuscationKeys");
			f.setAccessible(true);
			TreeMap<PublicKey, byte[]> obkeys = (TreeMap<PublicKey, byte[]>) f
					.get(okdb);
			int size = obkeys.size();
			assertEquals(3, size);
			assertTrue(obkeys.containsKey(A_Key_enc_pub));
			assertTrue(!obkeys.containsKey(A_devKey1_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey2_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey3_enc_pub));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// Invite user
		try {

			volume.inviteUser(aliasOwner, A_Key_sig_priv, A_Key_enc_pub,
					A_Key_enc_priv, aliasGuest, B_Key_enc_pub, B_Key_sig_pub);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertEquals(2, smd.getSharePartList().size());
		assertEquals(A_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasOwner));
		assertEquals(B_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasGuest));

		assertEquals(2, smd.shareKeys.size());
		assertEquals(1, smd.shareKeys.getLastEntry().getVersion());
		assertEquals(4, smd.shareKeys.getLastEntry().size());

		assertTrue(smd.shareKeys.getLastEntry().getEncryptedKey(A_Key_enc_pub) != null);
		assertTrue(smd.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey1_enc_pub) == null);
		assertTrue(smd.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey2_enc_pub) != null);
		assertTrue(smd.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey3_enc_pub) != null);
		assertTrue(smd.shareKeys.getLastEntry().getEncryptedKey(B_Key_enc_pub) != null);
		assertTrue(smd.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey1_enc_pub) == null);
		assertTrue(smd.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey2_enc_pub) == null);
		assertTrue(smd.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey3_enc_pub) == null);

		try {
			ObfuscationKeyDB okdb = smd.obfuscationKeys;
			Field f = ObfuscationKeyDB.class
					.getDeclaredField("obfuscationKeys");
			f.setAccessible(true);
			TreeMap<PublicKey, byte[]> obkeys = (TreeMap<PublicKey, byte[]>) f
					.get(okdb);
			int size = obkeys.size();
			assertEquals(4, size);
			assertTrue(obkeys.containsKey(A_Key_enc_pub));
			assertTrue(!obkeys.containsKey(A_devKey1_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey2_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey3_enc_pub));
			assertTrue(obkeys.containsKey(B_Key_enc_pub));
			assertTrue(!obkeys.containsKey(B_devKey1_enc_pub));
			assertTrue(!obkeys.containsKey(B_devKey2_enc_pub));
			assertTrue(!obkeys.containsKey(B_devKey3_enc_pub));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		volume2 = new Volume(dbName);
		try {
			smd2 = volume2.loadShareMetaData(A_Key_sig_pub);
			assertNotNull(smd);
		} catch (Exception e) {
			fail(e.getMessage());
		}

		assertEquals(2, smd2.getSharePartList().size());
		assertEquals(A_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasOwner));
		assertEquals(B_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasGuest));

		assertEquals(2, smd2.shareKeys.size());
		assertEquals(1, smd2.shareKeys.getLastEntry().getVersion());
		assertEquals(4, smd2.shareKeys.getLastEntry().size());
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(A_Key_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey1_enc_pub) == null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey2_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey3_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(B_Key_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey1_enc_pub) == null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey2_enc_pub) == null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey3_enc_pub) == null);

		try {
			ObfuscationKeyDB okdb = smd2.obfuscationKeys;
			Field f = ObfuscationKeyDB.class
					.getDeclaredField("obfuscationKeys");
			f.setAccessible(true);
			TreeMap<PublicKey, byte[]> obkeys = (TreeMap<PublicKey, byte[]>) f
					.get(okdb);
			int size = obkeys.size();
			assertEquals(4, size);
			assertTrue(obkeys.containsKey(A_Key_enc_pub));
			assertTrue(!obkeys.containsKey(A_devKey1_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey2_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey3_enc_pub));
			assertTrue(obkeys.containsKey(B_Key_enc_pub));
			assertTrue(!obkeys.containsKey(B_devKey1_enc_pub));
			assertTrue(!obkeys.containsKey(B_devKey2_enc_pub));
			assertTrue(!obkeys.containsKey(B_devKey3_enc_pub));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// accept invitation
		volume2 = new Volume(dbName);
		try {

			smd2 = volume2.acceptInvitation(A_Key_sig_pub, aliasGuest,
					B_Key_sig_pub, B_Key_sig_priv, deviceAliasGuest1,
					B_devKey1_enc_pub, B_Key_enc_pub, B_Key_enc_priv);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertEquals(2, smd2.getSharePartList().size());
		assertEquals(A_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasOwner));
		assertEquals(B_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasGuest));

		assertEquals(2, smd2.shareKeys.size());
		assertEquals(1, smd2.shareKeys.getLastEntry().getVersion());
		assertEquals(5, smd2.shareKeys.getLastEntry().size());
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(A_Key_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey1_enc_pub) == null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey2_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey3_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(B_Key_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey1_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey2_enc_pub) == null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey3_enc_pub) == null);

		try {
			ObfuscationKeyDB okdb = smd2.obfuscationKeys;
			Field f = ObfuscationKeyDB.class
					.getDeclaredField("obfuscationKeys");
			f.setAccessible(true);
			TreeMap<PublicKey, byte[]> obkeys = (TreeMap<PublicKey, byte[]>) f
					.get(okdb);
			int size = obkeys.size();
			assertEquals(5, size);
			assertTrue(obkeys.containsKey(A_Key_enc_pub));
			assertTrue(!obkeys.containsKey(A_devKey1_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey2_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey3_enc_pub));
			assertTrue(obkeys.containsKey(B_Key_enc_pub));
			assertTrue(obkeys.containsKey(B_devKey1_enc_pub));
			assertTrue(!obkeys.containsKey(B_devKey2_enc_pub));
			assertTrue(!obkeys.containsKey(B_devKey3_enc_pub));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// reload share to verify integrity
		volume2 = new Volume(dbName);
		try {

			smd2 = volume2.loadShareMetaData(A_Key_sig_pub);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertEquals(2, smd2.getSharePartList().size());
		assertEquals(A_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasOwner));
		assertEquals(B_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasGuest));

		assertEquals(2, smd2.shareKeys.size());
		assertEquals(1, smd2.shareKeys.getLastEntry().getVersion());
		assertEquals(5, smd2.shareKeys.getLastEntry().size());
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(A_Key_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey1_enc_pub) == null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey2_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey3_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(B_Key_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey1_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey2_enc_pub) == null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey3_enc_pub) == null);

		try {
			ObfuscationKeyDB okdb = smd2.obfuscationKeys;
			Field f = ObfuscationKeyDB.class
					.getDeclaredField("obfuscationKeys");
			f.setAccessible(true);
			TreeMap<PublicKey, byte[]> obkeys = (TreeMap<PublicKey, byte[]>) f
					.get(okdb);
			int size = obkeys.size();
			assertEquals(5, size);
			assertTrue(obkeys.containsKey(A_Key_enc_pub));
			assertTrue(!obkeys.containsKey(A_devKey1_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey2_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey3_enc_pub));
			assertTrue(obkeys.containsKey(B_Key_enc_pub));
			assertTrue(obkeys.containsKey(B_devKey1_enc_pub));
			assertTrue(!obkeys.containsKey(B_devKey2_enc_pub));
			assertTrue(!obkeys.containsKey(B_devKey3_enc_pub));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// let user B add another device

		try {

			volume2.addDevice(aliasGuest, B_Key_sig_pub, B_Key_sig_priv,
					deviceAliasGuest2, B_devKey2_enc_pub, B_Key_enc_pub,
					B_Key_enc_priv);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertEquals(2, smd2.getSharePartList().size());
		assertEquals(A_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasOwner));
		assertEquals(B_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasGuest));

		assertEquals(2, smd2.shareKeys.size());
		assertEquals(1, smd2.shareKeys.getLastEntry().getVersion());
		assertEquals(6, smd2.shareKeys.getLastEntry().size());
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(A_Key_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey1_enc_pub) == null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey2_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey3_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(B_Key_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey1_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey2_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey3_enc_pub) == null);

		try {
			ObfuscationKeyDB okdb = smd2.obfuscationKeys;
			Field f = ObfuscationKeyDB.class
					.getDeclaredField("obfuscationKeys");
			f.setAccessible(true);
			TreeMap<PublicKey, byte[]> obkeys = (TreeMap<PublicKey, byte[]>) f
					.get(okdb);
			int size = obkeys.size();
			assertEquals(6, size);
			assertTrue(obkeys.containsKey(A_Key_enc_pub));
			assertTrue(!obkeys.containsKey(A_devKey1_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey2_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey3_enc_pub));
			assertTrue(obkeys.containsKey(B_Key_enc_pub));
			assertTrue(obkeys.containsKey(B_devKey1_enc_pub));
			assertTrue(obkeys.containsKey(B_devKey2_enc_pub));
			assertTrue(!obkeys.containsKey(B_devKey3_enc_pub));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// reload share to verify integrity
		volume2 = new Volume(dbName);
		try {

			smd2 = volume2.loadShareMetaData(A_Key_sig_pub);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertEquals(2, smd2.getSharePartList().size());
		assertEquals(A_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasOwner));
		assertEquals(B_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasGuest));

		assertEquals(2, smd2.shareKeys.size());
		assertEquals(1, smd2.shareKeys.getLastEntry().getVersion());
		assertEquals(6, smd2.shareKeys.getLastEntry().size());
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(A_Key_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey1_enc_pub) == null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey2_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey3_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(B_Key_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey1_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey2_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey3_enc_pub) == null);

		try {
			ObfuscationKeyDB okdb = smd2.obfuscationKeys;
			Field f = ObfuscationKeyDB.class
					.getDeclaredField("obfuscationKeys");
			f.setAccessible(true);
			TreeMap<PublicKey, byte[]> obkeys = (TreeMap<PublicKey, byte[]>) f
					.get(okdb);
			int size = obkeys.size();
			assertEquals(6, size);
			assertTrue(obkeys.containsKey(A_Key_enc_pub));
			assertTrue(!obkeys.containsKey(A_devKey1_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey2_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey3_enc_pub));
			assertTrue(obkeys.containsKey(B_Key_enc_pub));
			assertTrue(obkeys.containsKey(B_devKey1_enc_pub));
			assertTrue(obkeys.containsKey(B_devKey2_enc_pub));
			assertTrue(!obkeys.containsKey(B_devKey3_enc_pub));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// let user B add another device

		try {

			volume2.addDevice(aliasGuest, B_Key_sig_pub, B_Key_sig_priv,
					deviceAliasGuest3, B_devKey3_enc_pub, B_Key_enc_pub,
					B_Key_enc_priv);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertEquals(2, smd2.getSharePartList().size());
		assertEquals(A_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasOwner));
		assertEquals(B_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasGuest));

		assertEquals(2, smd2.shareKeys.size());
		assertEquals(1, smd2.shareKeys.getLastEntry().getVersion());
		assertEquals(7, smd2.shareKeys.getLastEntry().size());
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(A_Key_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey1_enc_pub) == null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey2_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey3_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(B_Key_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey1_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey2_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey3_enc_pub) != null);

		try {
			ObfuscationKeyDB okdb = smd2.obfuscationKeys;
			Field f = ObfuscationKeyDB.class
					.getDeclaredField("obfuscationKeys");
			f.setAccessible(true);
			TreeMap<PublicKey, byte[]> obkeys = (TreeMap<PublicKey, byte[]>) f
					.get(okdb);
			int size = obkeys.size();
			assertEquals(7, size);
			assertTrue(obkeys.containsKey(A_Key_enc_pub));
			assertTrue(!obkeys.containsKey(A_devKey1_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey2_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey3_enc_pub));
			assertTrue(obkeys.containsKey(B_Key_enc_pub));
			assertTrue(obkeys.containsKey(B_devKey1_enc_pub));
			assertTrue(obkeys.containsKey(B_devKey2_enc_pub));
			assertTrue(obkeys.containsKey(B_devKey3_enc_pub));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// reload share to verify integrity
		volume2 = new Volume(dbName);
		try {

			smd2 = volume2.loadShareMetaData(A_Key_sig_pub);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertEquals(2, smd2.getSharePartList().size());
		assertEquals(A_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasOwner));
		assertEquals(B_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasGuest));

		assertEquals(2, smd2.shareKeys.size());
		assertEquals(1, smd2.shareKeys.getLastEntry().getVersion());
		assertEquals(7, smd2.shareKeys.getLastEntry().size());
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(A_Key_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey1_enc_pub) == null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey2_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey3_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(B_Key_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey1_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey2_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey3_enc_pub) != null);

		try {
			ObfuscationKeyDB okdb = smd2.obfuscationKeys;
			Field f = ObfuscationKeyDB.class
					.getDeclaredField("obfuscationKeys");
			f.setAccessible(true);
			TreeMap<PublicKey, byte[]> obkeys = (TreeMap<PublicKey, byte[]>) f
					.get(okdb);
			int size = obkeys.size();
			assertEquals(7, size);
			assertTrue(obkeys.containsKey(A_Key_enc_pub));
			assertTrue(!obkeys.containsKey(A_devKey1_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey2_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey3_enc_pub));
			assertTrue(obkeys.containsKey(B_Key_enc_pub));
			assertTrue(obkeys.containsKey(B_devKey1_enc_pub));
			assertTrue(obkeys.containsKey(B_devKey2_enc_pub));
			assertTrue(obkeys.containsKey(B_devKey3_enc_pub));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// let B remove a device

		// let user B add another device

		try {

			volume2.removeDevice(aliasGuest, B_Key_sig_pub, B_Key_sig_priv,
					deviceAliasGuest2);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertEquals(2, smd2.getSharePartList().size());
		assertEquals(A_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasOwner));
		assertEquals(B_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasGuest));

		assertEquals(3, smd2.shareKeys.size());
		assertEquals(2, smd2.shareKeys.getLastEntry().getVersion());
		assertEquals(6, smd2.shareKeys.getLastEntry().size());
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(A_Key_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey1_enc_pub) == null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey2_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey3_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(B_Key_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey1_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey2_enc_pub) == null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey3_enc_pub) != null);

		try {
			ObfuscationKeyDB okdb = smd2.obfuscationKeys;
			Field f = ObfuscationKeyDB.class
					.getDeclaredField("obfuscationKeys");
			f.setAccessible(true);
			TreeMap<PublicKey, byte[]> obkeys = (TreeMap<PublicKey, byte[]>) f
					.get(okdb);
			int size = obkeys.size();
			assertEquals(6, size);
			assertTrue(obkeys.containsKey(A_Key_enc_pub));
			assertTrue(!obkeys.containsKey(A_devKey1_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey2_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey3_enc_pub));
			assertTrue(obkeys.containsKey(B_Key_enc_pub));
			assertTrue(obkeys.containsKey(B_devKey1_enc_pub));
			assertTrue(!obkeys.containsKey(B_devKey2_enc_pub));
			assertTrue(obkeys.containsKey(B_devKey3_enc_pub));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// reload share to verify integrity
		volume2 = new Volume(dbName);
		try {

			smd2 = volume2.loadShareMetaData(A_Key_sig_pub);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertEquals(2, smd2.getSharePartList().size());
		assertEquals(A_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasOwner));
		assertEquals(B_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasGuest));

		assertEquals(3, smd2.shareKeys.size());
		assertEquals(2, smd2.shareKeys.getLastEntry().getVersion());
		assertEquals(6, smd2.shareKeys.getLastEntry().size());
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(A_Key_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey1_enc_pub) == null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey2_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey3_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(B_Key_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey1_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey2_enc_pub) == null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey3_enc_pub) != null);

		try {
			ObfuscationKeyDB okdb = smd2.obfuscationKeys;
			Field f = ObfuscationKeyDB.class
					.getDeclaredField("obfuscationKeys");
			f.setAccessible(true);
			TreeMap<PublicKey, byte[]> obkeys = (TreeMap<PublicKey, byte[]>) f
					.get(okdb);
			int size = obkeys.size();
			assertEquals(6, size);
			assertTrue(obkeys.containsKey(A_Key_enc_pub));
			assertTrue(!obkeys.containsKey(A_devKey1_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey2_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey3_enc_pub));
			assertTrue(obkeys.containsKey(B_Key_enc_pub));
			assertTrue(obkeys.containsKey(B_devKey1_enc_pub));
			assertTrue(!obkeys.containsKey(B_devKey2_enc_pub));
			assertTrue(obkeys.containsKey(B_devKey3_enc_pub));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// let user B add another device

		try {

			volume2.removeDevice(aliasGuest, B_Key_sig_pub, B_Key_sig_priv,
					deviceAliasGuest1);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertEquals(2, smd2.getSharePartList().size());
		assertEquals(A_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasOwner));
		assertEquals(B_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasGuest));

		assertEquals(4, smd2.shareKeys.size());
		assertEquals(3, smd2.shareKeys.getLastEntry().getVersion());
		assertEquals(5, smd2.shareKeys.getLastEntry().size());
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(A_Key_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey1_enc_pub) == null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey2_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey3_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(B_Key_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey1_enc_pub) == null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey2_enc_pub) == null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey3_enc_pub) != null);

		try {
			ObfuscationKeyDB okdb = smd2.obfuscationKeys;
			Field f = ObfuscationKeyDB.class
					.getDeclaredField("obfuscationKeys");
			f.setAccessible(true);
			TreeMap<PublicKey, byte[]> obkeys = (TreeMap<PublicKey, byte[]>) f
					.get(okdb);
			int size = obkeys.size();
			assertEquals(5, size);
			assertTrue(obkeys.containsKey(A_Key_enc_pub));
			assertTrue(!obkeys.containsKey(A_devKey1_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey2_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey3_enc_pub));
			assertTrue(obkeys.containsKey(B_Key_enc_pub));
			assertTrue(!obkeys.containsKey(B_devKey1_enc_pub));
			assertTrue(!obkeys.containsKey(B_devKey2_enc_pub));
			assertTrue(obkeys.containsKey(B_devKey3_enc_pub));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// reload share to verify integrity
		volume2 = new Volume(dbName);
		try {

			smd2 = volume2.loadShareMetaData(A_Key_sig_pub);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertEquals(2, smd2.getSharePartList().size());
		assertEquals(A_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasOwner));
		assertEquals(B_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasGuest));

		assertEquals(4, smd2.shareKeys.size());
		assertEquals(3, smd2.shareKeys.getLastEntry().getVersion());
		assertEquals(5, smd2.shareKeys.getLastEntry().size());
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(A_Key_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey1_enc_pub) == null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey2_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				A_devKey3_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(B_Key_enc_pub) != null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey1_enc_pub) == null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey2_enc_pub) == null);
		assertTrue(smd2.shareKeys.getLastEntry().getEncryptedKey(
				B_devKey3_enc_pub) != null);

		try {
			ObfuscationKeyDB okdb = smd2.obfuscationKeys;
			Field f = ObfuscationKeyDB.class
					.getDeclaredField("obfuscationKeys");
			f.setAccessible(true);
			TreeMap<PublicKey, byte[]> obkeys = (TreeMap<PublicKey, byte[]>) f
					.get(okdb);
			int size = obkeys.size();
			assertEquals(5, size);
			assertTrue(obkeys.containsKey(A_Key_enc_pub));
			assertTrue(!obkeys.containsKey(A_devKey1_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey2_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey3_enc_pub));
			assertTrue(obkeys.containsKey(B_Key_enc_pub));
			assertTrue(!obkeys.containsKey(B_devKey1_enc_pub));
			assertTrue(!obkeys.containsKey(B_devKey2_enc_pub));
			assertTrue(obkeys.containsKey(B_devKey3_enc_pub));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		*/
	}

}
