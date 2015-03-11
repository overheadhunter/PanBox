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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.SecretKey;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.panbox.core.crypto.CryptCore;
import org.panbox.core.crypto.SignatureHelper;
import org.panbox.core.exception.SerializationException;
import org.panbox.core.exception.ShareMetaDataException;
import org.panbox.core.exception.SymmetricKeyDecryptionException;
import org.panbox.core.exception.SymmetricKeyEncryptionException;
import org.panbox.core.keymgmt.VolumeParams.VolumeParamsFactory;

public class TestInitVolume {

	static PrivateKey mKey_sig;
	static PublicKey mKey_sig_pub;
	static PrivateKey mKey_enc;
	static PublicKey mKey_enc_pub;
	static PublicKey devKey1_enc;
	static PrivateKey devKey1_enc_priv;
	static PublicKey devKey2_enc;
	static PrivateKey devKey2_enc_priv;
	Map<String, PublicKey> dkList;
	IVolume volume;

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private String dbName = null;
	private static String alias = "Owner";
	private static String deviceAlias = "Dev1";
	private static String deviceAlias2 = "Dev2";

	@BeforeClass
	public static void setUpClass() {
		try {
			KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA",
					new BouncyCastleProvider());
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			gen.initialize(1024, random);
			KeyPair pair = gen.generateKeyPair();
			mKey_sig = pair.getPrivate();
			mKey_sig_pub = pair.getPublic();
			gen.generateKeyPair();
			mKey_enc = pair.getPrivate();
			mKey_enc_pub = pair.getPublic();
			pair = gen.generateKeyPair();
			devKey1_enc_priv = pair.getPrivate();
			devKey1_enc = pair.getPublic();
			pair = gen.generateKeyPair();
			devKey2_enc_priv = pair.getPrivate();
			devKey2_enc = pair.getPublic();

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
		volume = new Volume(dbName);
		dkList = new TreeMap<String, PublicKey>();
	}

	@After
	public void tearDown() {
		volume = null;
		dkList = null;

		File db = new File(dbName + Volume.DB_FILE);
		if (db.exists())
			db.delete();

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testNewAPI() throws NoSuchFieldException, SecurityException,
			IllegalArgumentException, IllegalAccessException {
		VolumeParamsFactory pf = VolumeParamsFactory.getFactory();
		assertNotNull(volume);
		ShareMetaData smd = null;
		Volume volume2 = null;
		ShareMetaData smd2 = null;

		try {
			VolumeParams p = pf.createVolumeParams().setOwnerAlias(alias)
					.setPublicSignatureKey(mKey_sig_pub)
					.setPrivateSignatureKey(mKey_sig)
					.setPublicEncryptionKey(mKey_enc_pub)
					.setPrivateEncryptionKey(mKey_enc)
					.setDeviceAlias(deviceAlias)
					.setPublicDeviceKey(devKey1_enc);

			smd = volume.createShareMetaData(p);
			assertNotNull(smd);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		try {
			ObfuscationKeyDB okdb = smd.obfuscationKeys;
			Field f = ObfuscationKeyDB.class
					.getDeclaredField("obfuscationKeys");
			f.setAccessible(true);
			int size = ((TreeMap<PublicKey, byte[]>) f.get(okdb)).size();
			assertEquals(2, size);
		} catch (Exception e) {
			fail(e.getMessage());
		}
		try {
			volume2 = new Volume(dbName);
			smd2 = volume2.loadShareMetaData(mKey_sig_pub);
			assertNotNull(smd2);
		} catch (Exception e) {
			fail(e.getMessage());
		}
		assertEquals(smd, smd2);

		try {
			volume.addDevice(alias, mKey_sig_pub, mKey_sig, deviceAlias2,
					devKey2_enc, mKey_enc_pub, mKey_enc);
		} catch (ShareMetaDataException e) {
			fail(e.getMessage());
		}

		try {
			ObfuscationKeyDB okdb = smd.obfuscationKeys;
			Field f = ObfuscationKeyDB.class
					.getDeclaredField("obfuscationKeys");
			f.setAccessible(true);
			int size = ((TreeMap<PublicKey, byte[]>) f.get(okdb)).size();
			assertEquals(3, size);
		} catch (Exception e) {
			fail(e.getMessage());
		}
		// try {
		// volume.removeDevice(alias, mKey_sig_pub, mKey_sig, deviceAlias);
		// } catch (Exception e) {
		// fail(e.getMessage());
		// }
		//
		// try {
		// ObfuscationKeyDB okdb = smd.obfuscationKeys;
		// Field f = ObfuscationKeyDB.class
		// .getDeclaredField("obfuscationKeys");
		// f.setAccessible(true);
		// int size = ((TreeMap<PublicKey, byte[]>) f.get(okdb)).size();
		// assertEquals(2, size);
		// } catch (Exception e) {
		// fail(e.getMessage());
		// }
		//
		// try {
		// volume.removeDevice(alias, mKey_sig_pub, mKey_sig, deviceAlias);
		// } catch (Exception e) {
		// fail(e.getMessage());
		// }
		//
		// try {
		// ObfuscationKeyDB okdb = smd.obfuscationKeys;
		// Field f = ObfuscationKeyDB.class
		// .getDeclaredField("obfuscationKeys");
		// f.setAccessible(true);
		// int size = ((TreeMap<PublicKey, byte[]>) f.get(okdb)).size();
		// assertEquals(2, size);
		// } catch (Exception e) {
		// fail(e.getMessage());
		// }
		//
		// try {
		// volume.removeDevice(alias, mKey_sig_pub, mKey_sig, deviceAlias2);
		// } catch (Exception e) {
		// fail(e.getMessage());
		// }
		//
		// try {
		// ObfuscationKeyDB okdb = smd.obfuscationKeys;
		// Field f = ObfuscationKeyDB.class
		// .getDeclaredField("obfuscationKeys");
		// f.setAccessible(true);
		// int size = ((TreeMap<PublicKey, byte[]>) f.get(okdb)).size();
		// assertEquals(1, size);
		// } catch (Exception e) {
		// fail(e.getMessage());
		// }
		//
		// try {
		// volume.addDevice(alias, mKey_sig_pub, mKey_sig, deviceAlias2,
		// devKey2_enc, mKey_enc_pub, mKey_enc);
		// } catch (ShareMetaDataException e) {
		// fail(e.getMessage());
		// }
		//
		// try {
		// ObfuscationKeyDB okdb = smd.obfuscationKeys;
		// Field f = ObfuscationKeyDB.class
		// .getDeclaredField("obfuscationKeys");
		// f.setAccessible(true);
		// int size = ((TreeMap<PublicKey, byte[]>) f.get(okdb)).size();
		// assertEquals(2, size);
		// } catch (Exception e) {
		// fail(e.getMessage());
		// }

	}

	@Test
	public void obfuscationKeys() throws SymmetricKeyEncryptionException,
			SymmetricKeyDecryptionException {
		ObfuscationKeyDB ok = new ObfuscationKeyDB();
		assertTrue(ok.isEmpty());

		SecretKey key = CryptCore.generateSymmetricKey();
		byte[] encKey = CryptCore.encryptSymmetricKey(key.getEncoded(),
				devKey1_enc);
		assertArrayEquals(key.getEncoded(),
				CryptCore.decryptSymmertricKey(encKey, devKey1_enc_priv)
						.getEncoded());

		ok.add(devKey1_enc, encKey);
		assertFalse(ok.isEmpty());
		assertNull(ok.get(devKey2_enc));

		byte[] fromOK = ok.get(devKey1_enc);
		assertNotNull(fromOK);
		assertArrayEquals(encKey, fromOK);
		SecretKey decryptSymmertricKey = CryptCore.decryptSymmertricKey(fromOK,
				devKey1_enc_priv);
		assertArrayEquals(key.getEncoded(), decryptSymmertricKey.getEncoded());
		assertFalse(ok.isEmpty());

		ok.add(devKey2_enc, CryptCore.encryptSymmetricKey(
				decryptSymmertricKey.getEncoded(), devKey2_enc));
		byte[] encSK2 = ok.get(devKey2_enc);
		assertNotNull(encSK2);
		assertArrayEquals(key.getEncoded(),
				CryptCore.decryptSymmertricKey(encSK2, devKey2_enc_priv)
						.getEncoded());
		assertFalse(ok.isEmpty());

		ok.remove(devKey1_enc);
		assertNull(ok.get(devKey1_enc));
		assertNotNull(ok.get(devKey2_enc));
		assertFalse(ok.isEmpty());

		ok.remove(devKey2_enc);
		assertNull(ok.get(devKey1_enc));
		assertNull(ok.get(devKey2_enc));
		assertTrue(ok.isEmpty());
	}

	@Test
	public void test() throws SymmetricKeyDecryptionException,
			SymmetricKeyEncryptionException {
		// Precondition:
		// Owner has pw-protected MasterKeystore containig MKey_sig and MKey_enc
		// Owner hat lesser protected DeviceKeystore containing at least one
		// DeviceKey DevKey_1_enc

		// Create Empty SharePartList
		assertNotNull(volume);

		// ShareMetaData shareMetaData = volume.createShareMetaData(alias,
		// mKey_sig_pub, mKey_sig, deviceAlias, devKey1_enc);
		ShareMetaData shareMetaData = null;
		try {
			shareMetaData = new ShareMetaData(new JDBCHelperNonRevokeable(
					"jdbc:sqlite:" + dbName + File.separator), mKey_sig_pub);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		try {
			Field smdField = Volume.class.getDeclaredField("shareMetaData");
			smdField.setAccessible(true);
			smdField.set(volume, shareMetaData);
		} catch (Exception e) {
			fail(e.getMessage());
		}
		assertNotNull(shareMetaData);

		SharePartList spl = shareMetaData.initSharePartList();
		assertEquals("SharePartList is not empty, although it should be", 0,
				spl.size());
		// Sign SharePartList
		try {
			spl.sign(mKey_sig);
			assertTrue(CryptCore.verifySignature(spl, spl.getSignature(),
					mKey_sig_pub));
		} catch (SignatureException e2) {
			fail(e2.getMessage());
		}

		spl.add(mKey_sig_pub, alias);
		assertEquals("SharePartList is not empty, although it should be", 1,
				spl.size());
		try {
			spl.sign(mKey_sig);
			assertTrue(CryptCore.verifySignature(spl, spl.getSignature(),
					mKey_sig_pub));
		} catch (SignatureException e2) {
			fail(e2.getMessage());
		}

		// get DevKeys for the Devices that the owner wants to enable for this
		// Volume
		// Put keys in dkList
		dkList.put(deviceAlias, devKey1_enc);
		assertEquals("DeviceKeyList should have 1 element", 1, dkList.size());

		// Create and sign devicelist
		DeviceList dl = shareMetaData.createDeviceList(mKey_sig_pub, dkList);
		assertEquals("ShareMetaData should have 1 DeviceList", 1, shareMetaData
				.getDeviceLists().values().size());

		// Create symmetric ShareKey
		SecretKey sk = CryptCore.generateSymmetricKey();
		assertNotNull(sk);

		// Encrypt sk for all deviceLists
		Collection<DeviceList> deviceLists = shareMetaData.getDeviceLists()
				.values();
		Collection<PublicKey> pubKeys = new LinkedList<PublicKey>();
		for (DeviceList devList : deviceLists) {
			pubKeys.addAll(devList.getPublicKeys());
		}
		assertEquals(1, pubKeys.size());
		// fetch ShareKeyDB
		ShareKeyDB db = shareMetaData.getShareKeys();
		assertNotNull(db);
		// add Encrypted Keys to db
		try {
			db.add(sk, pubKeys);
		} catch (SymmetricKeyEncryptionException e1) {
			fail(e1.getMessage());
		}
		assertEquals(1, db.size());
		try {
			db.sign(mKey_sig, alias);
		} catch (SignatureException e1) {
			fail(e1.getMessage());
		}

		// Encrypt skOb for all deviceLists
		shareMetaData.addObfuscationKey(devKey1_enc, null, null);

		System.out.println("Making ShareMetaData persistent...");

		try {
			dl.sign(mKey_sig, shareMetaData.shareKeys,
					shareMetaData.obfuscationKeys);
			assertTrue(SignatureHelper.verify(dl.getSignature(), mKey_sig_pub,
					dl, shareMetaData.shareKeys.get(dl.getPublicKeys()),
					shareMetaData.obfuscationKeys.get(dl.getPublicKeys())));

		} catch (SignatureException | InvalidKeyException
				| NoSuchAlgorithmException | SerializationException e2) {
			fail(e2.getMessage());
		}

		// make sharemetadata persistent
		try {
			shareMetaData.persist();
			shareMetaData.persist(dl);
		} catch (Exception e) {
			fail(e.getMessage());
		}

		System.out.println("Loading Volume...");

		// load volume with sharemetadata
		try {
			Volume loadedVol = new Volume(dbName);
			ShareMetaData loadedSMD = loadedVol.loadShareMetaData(mKey_sig_pub);

			assertArrayEquals(shareMetaData.getSignature(),
					loadedSMD.getSignature());

			System.out.println("DONE");
		} catch (Exception e) {
			e.printStackTrace(System.err);
			fail(e.getMessage());
		}
	}
}
