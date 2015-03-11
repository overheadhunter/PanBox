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
import java.security.UnrecoverableKeyException;
import java.util.Arrays;
import java.util.TreeMap;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.panbox.core.Utils;
import org.panbox.core.crypto.CryptCore;
import org.panbox.core.exception.ShareMetaDataException;
import org.panbox.core.identitymgmt.Identity;
import org.panbox.core.identitymgmt.SimpleAddressbook;
import org.panbox.core.keymgmt.VolumeParams.VolumeParamsFactory;

public class TestUserInvite {

	static PrivateKey A_Key_sig_priv;
	static PublicKey A_Key_sig_pub;
	static PrivateKey A_Key_enc_priv;
	static PublicKey A_Key_enc_pub;
	static PublicKey A_devKey_enc_pub;
	static PrivateKey A_devKey_enc_priv;

	static PrivateKey B_Key_sig_priv;
	static PublicKey B_Key_sig_pub;
	static PrivateKey B_Key_enc_priv;
	static PublicKey B_Key_enc_pub;
	static PublicKey B_devKey_enc_pub;
	static PrivateKey B_devKey_enc_priv;
	private String dbName;
	private static String aliasOwner = "Owner";
	private static String aliasGuest = "Guest";
	private static String deviceAliasOwner = "OwnerDev";
	private static String deviceAliasGuest = "GuestDev";
	
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

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
			A_devKey_enc_priv = pair.getPrivate();
			A_devKey_enc_pub = pair.getPublic();

			pair = gen.generateKeyPair();
			B_Key_sig_priv = pair.getPrivate();
			B_Key_sig_pub = pair.getPublic();
			gen.generateKeyPair();
			B_Key_enc_priv = pair.getPrivate();
			B_Key_enc_pub = pair.getPublic();
			pair = gen.generateKeyPair();
			B_devKey_enc_priv = pair.getPrivate();
			B_devKey_enc_pub = pair.getPublic();

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


	@Test
	public void test() {
		VolumeParamsFactory pf = VolumeParamsFactory.getFactory();

		// Create Share
		IVolume volume = new Volume(dbName);
		assertNotNull(volume);
		ShareMetaData smd = null;
		VolumeParams p = pf.createVolumeParams().setOwnerAlias(aliasOwner)
				.setPublicSignatureKey(A_Key_sig_pub)
				.setPrivateSignatureKey(A_Key_sig_priv)
				.setPublicEncryptionKey(A_Key_enc_pub)
				.setPrivateEncryptionKey(A_Key_enc_priv)
				.setDeviceAlias(deviceAliasOwner)
				.setPublicDeviceKey(A_devKey_enc_pub);
		try {
			smd = volume.createShareMetaData(p);
			assertNotNull(smd);

		} catch (Exception e) {
			fail(e.getMessage());
		}

		assertEquals(1, smd.getSharePartList().size());
		assertEquals(A_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasOwner));
		assertNull(smd.getSharePartList().getPublicKey(aliasGuest));

		assertEquals(1, smd.shareKeys.size());
		assertEquals(2, smd.shareKeys.getLastEntry().size());

		// Invite user
		p.setOtherSignatureKey(B_Key_sig_pub)
				.setOtherEncryptionKey(B_Key_enc_pub).setUserAlias(aliasGuest);
		try {

			volume.inviteUser(p);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		IVolume volume2 = new Volume(dbName);
		try {
			smd = volume2.loadShareMetaData(A_Key_sig_pub);
			assertNotNull(smd);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			fail(e.getMessage());
		}

		assertEquals(2, smd.getSharePartList().size());
		assertEquals(A_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasOwner));
		assertEquals(B_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasGuest));

		assertEquals(1, smd.shareKeys.size());
		assertEquals(3, smd.shareKeys.getLastEntry().size());

		try {
			ObfuscationKeyDB okdb = smd.obfuscationKeys;
			Field f = ObfuscationKeyDB.class
					.getDeclaredField("obfuscationKeys");
			f.setAccessible(true);
			@SuppressWarnings("unchecked")
			TreeMap<PublicKey, byte[]> obkeys = (TreeMap<PublicKey, byte[]>) f
					.get(okdb);
			int size = obkeys.size();
			assertEquals(3, size);
			assertTrue(obkeys.containsKey(A_Key_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey_enc_pub));
			assertTrue(obkeys.containsKey(B_Key_enc_pub));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// accept invitation
		volume2 = new Volume(dbName);
		VolumeParams p2 = pf.createVolumeParams()
				.setOwnerSignatureKey(A_Key_sig_pub).setUserAlias(aliasGuest)
				.setPublicDeviceKey(B_devKey_enc_pub)
				.setPublicEncryptionKey(B_Key_enc_pub)
				.setPublicSignatureKey(B_Key_sig_pub)
				.setPrivateEncryptionKey(B_Key_enc_priv)
				.setPrivateSignatureKey(B_Key_sig_priv)
				.setDeviceAlias(deviceAliasGuest);
		try {

			smd = volume2.acceptInvitation(p2);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertEquals(2, smd.getSharePartList().size());
		assertEquals(A_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasOwner));
		assertEquals(B_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasGuest));

		assertEquals(1, smd.shareKeys.size());
		assertEquals(4, smd.shareKeys.getLastEntry().size());

		try {
			ObfuscationKeyDB okdb = smd.obfuscationKeys;
			Field f = ObfuscationKeyDB.class
					.getDeclaredField("obfuscationKeys");
			f.setAccessible(true);
			@SuppressWarnings("unchecked")
			TreeMap<PublicKey, byte[]> obkeys = (TreeMap<PublicKey, byte[]>) f
					.get(okdb);
			int size = obkeys.size();
			assertEquals(4, size);
			assertTrue(obkeys.containsKey(A_Key_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey_enc_pub));
			assertTrue(obkeys.containsKey(B_Key_enc_pub));
			assertTrue(obkeys.containsKey(B_devKey_enc_pub));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// reload share to verify integrity
		IVolume volume3 = new Volume(dbName);
		try {

			smd = volume3.loadShareMetaData(A_Key_sig_pub);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertEquals(2, smd.getSharePartList().size());
		assertEquals(A_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasOwner));
		assertEquals(B_Key_sig_pub,
				smd.getSharePartList().getPublicKey(aliasGuest));

		assertEquals(1, smd.shareKeys.size());
		assertEquals(4, smd.shareKeys.getLastEntry().size());

		try {
			ObfuscationKeyDB okdb = smd.obfuscationKeys;
			Field f = ObfuscationKeyDB.class
					.getDeclaredField("obfuscationKeys");
			f.setAccessible(true);
			@SuppressWarnings("unchecked")
			TreeMap<PublicKey, byte[]> obkeys = (TreeMap<PublicKey, byte[]>) f
					.get(okdb);
			int size = obkeys.size();
			assertEquals(4, size);
			assertTrue(obkeys.containsKey(A_Key_enc_pub));
			assertTrue(obkeys.containsKey(A_devKey_enc_pub));
			assertTrue(obkeys.containsKey(B_Key_enc_pub));
			assertTrue(obkeys.containsKey(B_devKey_enc_pub));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testKey() {

		SimpleAddressbook ab = new SimpleAddressbook();
		Identity owner = new Identity(ab, "ali@baba.de", "Uli", "Mayer");

		KeyPair ownerKeySign = CryptCore.generateKeypair();
		KeyPair ownerKeyEnc = CryptCore.generateKeypair();
		KeyPair ownerDeviceEnc = CryptCore.generateKeypair();

		String ownerDev = "pc-owner";

		owner.setOwnerKeyEnc(ownerKeyEnc, "123456".toCharArray());
		owner.setOwnerKeySign(ownerKeySign, "123456".toCharArray());
		owner.addDeviceKey(ownerDeviceEnc, ownerDev);

		PrivateKey origKey = ownerKeySign.getPrivate();
		PrivateKey identKey = null;
		try {
			identKey = owner.getPrivateKeySign("123456".toCharArray());
		} catch (UnrecoverableKeyException e) {
			e.printStackTrace();
		}

		byte[] original = origKey.getEncoded();
		assertNotNull(identKey);
		byte[] fromIdentity = identKey.getEncoded();

		assertTrue(Arrays.equals(original, fromIdentity));
		assertEquals(origKey.getAlgorithm(), identKey.getAlgorithm());
		assertEquals(origKey.getFormat(), identKey.getFormat());

		assertTrue(Utils.keysMatch(ownerKeySign.getPublic(), identKey));
		assertTrue(Utils.keysMatch(owner.getPublicKeySign(),
				ownerKeySign.getPrivate()));
	}

	@Test
	public void testIntegration() throws InterruptedException {
		System.out.println("Creating share");
		SimpleAddressbook ab = new SimpleAddressbook();
		Identity owner = new Identity(ab, "ali@baba.de", "Uli", "Mayer");
		KeyPair ownerKeySign = CryptCore.generateKeypair();
		KeyPair ownerKeyEnc = CryptCore.generateKeypair();
		KeyPair ownerDeviceEnc = CryptCore.generateKeypair();
		String ownerDev = "pc-owner";
		owner.setOwnerKeyEnc(ownerKeyEnc, "123456".toCharArray());
		owner.setOwnerKeySign(ownerKeySign, "123456".toCharArray());
		owner.addDeviceKey(ownerDeviceEnc, ownerDev);

		SimpleAddressbook ab2 = new SimpleAddressbook();
		Identity guest = new Identity(ab2, "peter@mueller.de", "Peter",
				"Müller");
		KeyPair guestKeySign = CryptCore.generateKeypair();
		KeyPair guestKeyEnc = CryptCore.generateKeypair();
		KeyPair guestDeviceEnc = CryptCore.generateKeypair();
		String guestDev = "PC-GUEST";
		// String guestDev = "pc-guest";
		guest.setOwnerKeyEnc(guestKeyEnc, "7890".toCharArray());
		guest.setOwnerKeySign(guestKeySign, "7890".toCharArray());
		guest.addDeviceKey(guestDeviceEnc, guestDev);

		VolumeParamsFactory pf = VolumeParamsFactory.getFactory();

		String path = dbName;
		IVolume v1 = new Volume(path);
		VolumeParams p = null;
		try {
			p = pf.createVolumeParams().setOwnerAlias(owner.getEmail())
					.setKeys(owner, "123456".toCharArray())
					.setDeviceAlias(ownerDev)
					.setPublicDeviceKey(owner.getPublicKeyForDevice(ownerDev));
		} catch (UnrecoverableKeyException e2) {
			e2.printStackTrace();
			fail(e2.getMessage());
		}
		ShareMetaData smd = null;
		try {
			smd = v1.createShareMetaData(p);
		} catch (IllegalArgumentException | ShareMetaDataException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		System.out.println("loading share");
		try {
			new Volume(path).loadShareMetaData(owner.getPublicKeySign());
		} catch (IllegalArgumentException | ShareMetaDataException e1) {
			e1.printStackTrace();
			throw new RuntimeException(e1);
		}

		System.out.println("Inviting user to share");
		p.setOtherEncryptionKey(guest.getPublicKeyEnc())
				.setOtherSignatureKey(guest.getPublicKeySign())
				.setUserAlias(guest.getEmail());
		try {
			v1.inviteUser(p);
		} catch (ShareMetaDataException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		System.out.println("accepting invitation to share");
		IVolume v2 = new Volume(path);
		try {
			VolumeParams p2 = pf.createVolumeParams()
					.setKeys(guest, "7890".toCharArray())
					.setUserAlias(guest.getEmail()).setDeviceAlias(guestDev)
					.setPublicDeviceKey(guest.getPublicKeyForDevice(guestDev))
					.setOwnerSignatureKey(owner.getPublicKeySign());
			ShareMetaData smd2 = v2.acceptInvitation(p2);
		} catch (UnrecoverableKeyException | ShareMetaDataException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		System.out.println("Loading share");
		IVolume v3 = new Volume(path);
		try {
			PublicKey publicKeySign = owner.getPublicKeySign();
			v3.loadShareMetaData(publicKeySign);
		} catch (ShareMetaDataException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

}
