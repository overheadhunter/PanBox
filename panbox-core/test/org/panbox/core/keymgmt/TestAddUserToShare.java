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
import java.security.SignatureException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.crypto.SecretKey;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.panbox.core.crypto.CryptCore;
import org.panbox.core.exception.SymmetricKeyEncryptionException;

public class TestAddUserToShare {

	IVolume volume;
	static PrivateKey aKey_sig;
	static PublicKey aKey_sig_pub;
	static PrivateKey aDevKey1_enc_priv;
	static PublicKey aDevKey1_enc;

	static PublicKey bKey_enc_pub;
	Map<String, PublicKey> dkList;
	
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	
	private String dbName;

	@BeforeClass
	public static void setUpClass() {
		try {
			KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			gen.initialize(1024, random);
			KeyPair aPair = gen.generateKeyPair();
			aKey_sig = aPair.getPrivate();
			aKey_sig_pub = aPair.getPublic();
			aPair = gen.generateKeyPair();
			aDevKey1_enc_priv = aPair.getPrivate();
			aDevKey1_enc = aPair.getPublic();
			KeyPair bPair = gen.generateKeyPair();
			bKey_enc_pub = bPair.getPublic();
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
		File dbfile = new File(dbName + File.separator + Volume.DB_FILE);
		if (dbfile.exists())
			dbfile.delete();

		// init ShareMetadata and SharePartList and sign the latter
		volume = new Volume(dbName);
		ShareMetaData shareMetaData = null;
		try {
			shareMetaData = new ShareMetaData(new JDBCHelperNonRevokeable(
					"jdbc:sqlite:" + dbName + File.separator), aKey_sig_pub);
		} catch (Exception e) {
			fail(e.getMessage());
		}
		try {
			Field smdField = Volume.class.getDeclaredField("shareMetaData");
			smdField.setAccessible(true);
			smdField.set(volume, shareMetaData);
		} catch (Exception e) {
			fail(e.getMessage());
		}
		try {
			shareMetaData.initSharePartList().sign(aKey_sig);
		} catch (SignatureException e2) {
			fail(e2.getMessage());
		}
		dkList = new HashMap<String, PublicKey>();
		dkList.put("Dev1", aDevKey1_enc);
		try {
			shareMetaData.createDeviceList(aKey_sig_pub, dkList).sign(aKey_sig);
		} catch (SignatureException e2) {
			fail(e2.getMessage());
		}
		SecretKey sk = CryptCore.generateSymmetricKey();

		Collection<DeviceList> deviceLists = shareMetaData.getDeviceLists()
				.values();
		Collection<PublicKey> pubKeys = new LinkedList<PublicKey>();
		for (DeviceList devList : deviceLists) {
			pubKeys.addAll(devList.getPublicKeys());
		}
		// fetch ShareKeyDB
		ShareKeyDB db = shareMetaData.getShareKeys();
		try {
			db.add(sk, pubKeys);
		} catch (SymmetricKeyEncryptionException e1) {
			fail(e1.getMessage());
		}

	}

	@After
	public void tearDown() {
		volume = null;
		File dbfile = new File(dbName + File.separator + Volume.DB_FILE);
		if (dbfile.exists())
			dbfile.delete();
	}

	@Test
	public void test() {
		// Precondition
		// Volume/Share has been initialized as in TestInitVolume
		// Owner A has a full KeySet
		// Owner A has Public Keys of User B

		assertNotNull(volume);

		try {
			// fetch Share MetaData
			ShareMetaData shareMetaData = volume
					.loadShareMetaData(aKey_sig_pub);
			assertNotNull(shareMetaData);

			// fetch SharekKeyDatabase
			ShareKeyDB db = shareMetaData.getShareKeys();
			assertNotNull(db);

			// Receive the latest set of encrypted ShareKeys
			ShareKeyDBEntry entry = db.getLastEntry();
			// decrypt ShareKey using owners device key
			SecretKey sk = CryptCore.decryptShareKey(entry, aDevKey1_enc,
					aDevKey1_enc_priv);
			assertNotNull(sk);

			// receive SharePartList
			SharePartList spl = shareMetaData.getSharePartList();
			assertEquals("SharePartList is not empty, although it should be",
					0, spl.size());
			// Verify SharePartList
			assertTrue(CryptCore.verifySignature(spl, spl.getSignature(),
					aKey_sig_pub));

			// Add User B's public master encryption key to SharePartList
			spl.add(bKey_enc_pub, "USER_B");
			assertEquals("SharePartList has not the expected size", 1,
					spl.size());
			// Sign new SharePartList
			spl.sign(aKey_sig);
			// Verify Signature
			assertTrue(CryptCore.verifySignature(spl, spl.getSignature(),
					aKey_sig_pub));

			// Add Encrypted Sharekey for User B to ShareKeyDatabase
			int before = entry.size();
			entry.addEncryptedKey(sk, bKey_enc_pub);
			assertEquals(before + 1, entry.size());

		} catch (Exception e) {
			fail(e.getMessage());
		}

	}

}
