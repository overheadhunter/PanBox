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
package org.panbox.core.crypto.io;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.panbox.core.crypto.randomness.SecureRandomWrapper;
import org.panbox.core.exception.FileEncryptionException;

/**
 * @author palige
 * 
 */
public class AuthTagVerifierTest {

	private static int TAGLEN;

	@Rule
	public TemporaryFolder tmpTestDir = new TemporaryFolder();
	public static SecretKey testKey;
	public AESGCMRandomAccessFile aesTestFile;
	public SecureRandomWrapper wrapper;

	private AuthTagVerifier testVerifier;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TAGLEN = AESGCMRandomAccessFile.GCM_AUTHENTICATION_TAG_LEN / Byte.SIZE;
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		wrapper = SecureRandomWrapper.getInstance();
		// TODO: dynamic blocksize
		byte[] tmp = new byte[16];

		wrapper.nextBytes(tmp);
		testKey = new SecretKeySpec(tmp, "AES");

		File testFile = tmpTestDir.newFile("authtagverificationTest");

		aesTestFile = AESGCMRandomAccessFile.create(0, testKey, testFile);
		testVerifier = aesTestFile.getAuthTagVerifier();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		testVerifier = null;
		aesTestFile.close();
	}

	/**
	 * tests central file auth tag initialization
	 * 
	 * @throws FileEncryptionException
	 */
	@Test
	public void testInitialization() {
		assertNotNull(testVerifier);
		// for a new file, initially, file auth tag should be null
		assertNull(aesTestFile.readFileAuthenticationTag());
	}

	/**
	 * Test method for
	 * {@link org.panbox.core.crypto.io.AuthTagVerifier#updateFileAuthTag()}.
	 * 
	 * @throws FileEncryptionException
	 */
	@Test
	public void testUpdateFileAuthTag() throws Exception {
		byte[] testTag = new byte[TAGLEN];
		byte[] referenceFileTag = new byte[testVerifier.authTagHMac
				.getMacSize()];
		HMac refHMac = testVerifier.authTagHMac.getClass()
				.getConstructor(Digest.class).newInstance(new SHA256Digest());

		refHMac.init(new KeyParameter(testKey.getEncoded()));

		// exception should be thrown if AuTagVerifier-HMac cannot be properly
		// initialized
		aesTestFile.shareKey = null;
		try {
			testVerifier.updateFileAuthTag();
			fail("Expected exception for invalid file encryption key!");
		} catch (FileEncryptionException e) {
			assertEquals(
					"Invalid file encryption key in encrypting random access file!",
					e.getMessage());
		}
		// restore key
		aesTestFile.shareKey = testKey;

		// test if exception is thrown upon file auth tag update without chunk
		// auth tags
		try {
			testVerifier.updateFileAuthTag();
			fail("Expected exception as no chunk auth tags have been set!");
		} catch (FileEncryptionException e) {
			assertEquals("No chunk authentication tags have been set yet!",
					e.getMessage());
		}

		Arrays.fill(testTag, (byte) 0x41);
		refHMac.update(testTag, 0, TAGLEN);
		testVerifier.insertChunkAuthTag(0, testTag);
		// skip one chunk
		Arrays.fill(testTag, (byte) 0x42);
		refHMac.update(testTag, 0, TAGLEN);
		testVerifier.insertChunkAuthTag(2, testTag);
		try {
			testVerifier.updateFileAuthTag();
			fail("Missing authentication tag should cause an exception");
		} catch (FileEncryptionException e) {
			assertEquals(
					"Invalid chunk authentication tag in auth tag table at offset: 1",
					e.getMessage());
		}
		Arrays.fill(testTag, (byte) 0x43);
		refHMac.update(testTag, 0, TAGLEN);
		testVerifier.insertChunkAuthTag(1, testTag);
		assertNull(aesTestFile.readFileAuthenticationTag());
		refHMac.doFinal(referenceFileTag, 0);
		testVerifier.updateFileAuthTag();
		assertNotNull(aesTestFile.readFileAuthenticationTag());
		// should NOT be equal as update order for refHMac was 0 - 2 - 1
		assertFalse(Arrays.equals(referenceFileTag,
				aesTestFile.readFileAuthenticationTag()));

		// calculate actual value
		refHMac.reset();
		Arrays.fill(testTag, (byte) 0x41);
		refHMac.update(testTag, 0, TAGLEN);
		Arrays.fill(testTag, (byte) 0x43);
		refHMac.update(testTag, 0, TAGLEN);
		Arrays.fill(testTag, (byte) 0x42);
		refHMac.update(testTag, 0, TAGLEN);
		refHMac.doFinal(referenceFileTag, 0);

		assertArrayEquals(referenceFileTag,
				aesTestFile.readFileAuthenticationTag());
	}

	/**
	 * Test method for
	 * {@link org.panbox.core.crypto.io.AuthTagVerifier#verifyFileAuthTag()}.
	 */
	@Test
	public void testVerifyFileAuthTag() throws Exception {
		// verification with empty file auth tag should cause exception
		assertNull(aesTestFile.readFileAuthenticationTag());
		try {
			testVerifier.verifyFileAuthTag();
			fail("Expected exception for invalid file authentication tag in encrypted file");
		} catch (Exception e) {
			assertEquals("Encrypted file has invalid file authentication tag!",
					e.getMessage());
		}
		// verification with file auth tag of invalid size should cause
		// exception
		aesTestFile.writeFileAuthenticationTag(new byte[] { 1, 2, 3, 4 });
		try {
			testVerifier.verifyFileAuthTag();
			fail("Expected exception for invalid file authentication tag in encrypted file");
		} catch (Exception e) {
			assertEquals("Encrypted file has invalid file authentication tag!",
					e.getMessage());
		}

		// insert 2 test chunks auth tags
		byte[] testTag1 = new byte[TAGLEN];
		byte[] testTag2 = new byte[TAGLEN];
		Arrays.fill(testTag1, (byte) 0x41);
		Arrays.fill(testTag2, (byte) 0x42);
		testVerifier.insertChunkAuthTag(0, testTag1);
		testVerifier.insertChunkAuthTag(1, testTag2);

		// create reference for calculating expected file auth tag
		// value
		HMac refHMac = testVerifier.authTagHMac.getClass()
				.getConstructor(Digest.class).newInstance(new SHA256Digest());
		refHMac.init(new KeyParameter(testKey.getEncoded()));

		refHMac.update(testTag1, 0, TAGLEN);
		refHMac.update(testTag2, 0, TAGLEN);

		byte[] refRootTag = new byte[refHMac.getMacSize()];
		refHMac.doFinal(refRootTag, 0);

		// first, test verification of valid file auth tag
		aesTestFile.writeFileAuthenticationTag(refRootTag);
		assertTrue(testVerifier.verifyFileAuthTag());

		// second, test verification for invalid file auth tag
		testVerifier.updateFileAuthTag();
		byte[] tmp = aesTestFile.readFileAuthenticationTag();

		// invalidate file auth tag
		Arrays.fill(tmp, 0, 4, (byte) 0x00);
		aesTestFile.writeFileAuthenticationTag(tmp);

		// check if verification fails for invalid file auth tag
		assertFalse(testVerifier.verifyFileAuthTag());
	}

	/**
	 * tests the verification of a single chunk authentication tag.
	 * 
	 * @throws FileEncryptionException
	 * @throws IOException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidKeyException
	 */
	@Test
	public void testVerifyChunkAuthTag() throws FileEncryptionException,
			InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, IOException {
		byte[] testTag = new byte[TAGLEN];
		byte[] refTag = new byte[TAGLEN];
		Arrays.fill(testTag, (byte) 0x41);
		testVerifier.insertChunkAuthTag(0, testTag);
		// System.out.println(testVerifier);
		Arrays.fill(testTag, (byte) 0x42);
		testVerifier.insertChunkAuthTag(1, testTag);
		// System.out.println(testVerifier);
		Arrays.fill(testTag, (byte) 0x43);
		testVerifier.insertChunkAuthTag(2, testTag);
		// System.out.println(testVerifier);
		testVerifier.updateFileAuthTag();
		// System.out.println(testVerifier);

		Arrays.fill(refTag, (byte) 0x42);

		// invalid index
		try {
			testVerifier.verifyChunkAuthTag(-1, testTag);
			fail("Expected exception due to invalid arguments!");
		} catch (Exception e) {
			// do nothing
		}

		// invalid tags
		try {
			testVerifier.verifyChunkAuthTag(0, null);
			fail("Expected exception due to invalid arguments!");
		} catch (Exception e) {
			// do nothing
		}

		try {
			testVerifier.verifyChunkAuthTag(1, new byte[] { 0, 1, 2, 3 });
			fail("Expected exception due to invalid arguments!");
		} catch (Exception e) {
			// do nothing
		}

		assertFalse(testVerifier.verifyChunkAuthTag(0, refTag));
		assertTrue(testVerifier.verifyChunkAuthTag(1, refTag));
		assertFalse(testVerifier.verifyChunkAuthTag(2, refTag));

		// invalidate file auth tag
		byte[] tmp = aesTestFile.readFileAuthenticationTag();
		Arrays.fill(tmp, 0, 4, (byte) 0x00);
		aesTestFile.writeFileAuthenticationTag(tmp);

		// check detection of invalid file auth tag
		assertFalse(testVerifier.verifyFileAuthTag());
	}

	/**
	 * Test method for
	 * {@link org.panbox.core.crypto.io.AuthTagVerifier#updateChunkAuthTag(long, byte[])}
	 * .
	 * 
	 * @throws FileEncryptionException
	 * @throws IOException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidKeyException
	 */
	@Test
	public void testUpdateAuthTag() throws FileEncryptionException,
			InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, IOException {
		byte[] testTag = new byte[TAGLEN];
		byte[] refTag = new byte[AuthTagVerifier.AUTH_TAG_SIZE];
		assertFalse(testVerifier.needsUpdate());
		Arrays.fill(testTag, (byte) 0x41);
		testVerifier.insertChunkAuthTag(0, testTag);
		Arrays.fill(testTag, (byte) 0x42);
		testVerifier.insertChunkAuthTag(1, testTag);
		assertFalse(testVerifier.needsUpdate());
		Arrays.fill(testTag, (byte) 0x43);
		testVerifier.updateChunkAuthTag(2, testTag);
		assertTrue(testVerifier.needsUpdate());
		testVerifier.updateFileAuthTag();
		System.arraycopy(aesTestFile.readFileAuthenticationTag(), 0, refTag, 0,
				refTag.length);

		testVerifier.reset();

		Arrays.fill(testTag, (byte) 0x41);
		assertFalse(testVerifier.needsUpdate());
		testVerifier.updateChunkAuthTag(0, testTag);
		assertTrue(testVerifier.needsUpdate());
		testVerifier.updateFileAuthTag();
		assertFalse(testVerifier.needsUpdate());
		assertFalse(Arrays.equals(aesTestFile.readFileAuthenticationTag(),
				refTag));

		Arrays.fill(testTag, (byte) 0x42);
		assertFalse(testVerifier.needsUpdate());
		testVerifier.updateChunkAuthTag(1, testTag);
		assertTrue(testVerifier.needsUpdate());
		testVerifier.updateFileAuthTag();
		assertFalse(testVerifier.needsUpdate());
		assertFalse(Arrays.equals(aesTestFile.readFileAuthenticationTag(),
				refTag));

		// first test with deviating auth tag value
		Arrays.fill(testTag, (byte) 0x44);
		assertFalse(testVerifier.needsUpdate());
		testVerifier.updateChunkAuthTag(2, testTag);
		assertTrue(testVerifier.needsUpdate());
		testVerifier.updateFileAuthTag();
		assertFalse(testVerifier.needsUpdate());
		assertFalse(Arrays.equals(aesTestFile.readFileAuthenticationTag(),
				refTag));

		// now test with correct auth tag value
		Arrays.fill(testTag, (byte) 0x43);
		assertFalse(testVerifier.needsUpdate());
		testVerifier.updateChunkAuthTag(2, testTag);
		assertTrue(testVerifier.needsUpdate());
		testVerifier.updateFileAuthTag();
		assertFalse(testVerifier.needsUpdate());
		assertArrayEquals(refTag, aesTestFile.readFileAuthenticationTag());
	}

	/**
	 * Test method for
	 * {@link org.panbox.core.crypto.io.AuthTagVerifier#insertChunkAuthTag(long, byte[])}
	 * .
	 * 
	 * @throws FileEncryptionException
	 * @throws IOException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidKeyException
	 */
	@Test
	public void testInsertChunkAuthTag() throws FileEncryptionException,
			InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, IOException {
		byte[] testTag = new byte[TAGLEN];
		byte[] refTag1 = new byte[TAGLEN];
		byte[] refTag2 = new byte[TAGLEN];
		byte[] refTag3 = new byte[TAGLEN];
		byte[] invalidTag = new byte[TAGLEN - 1];
		try {
			testVerifier.insertChunkAuthTag(-1, testTag);
			fail("Failed to detect invalid argument!");
		} catch (FileEncryptionException e) {
			// do nothing
		}

		try {
			testVerifier.insertChunkAuthTag(0, null);
			fail("Failed to detect invalid argument!");
		} catch (FileEncryptionException e) {
			// do nothing
		}

		try {
			testVerifier.insertChunkAuthTag(0, invalidTag);
			fail("Failed to detect invalid argument!");
		} catch (FileEncryptionException e) {
			// do nothing
		}

		Arrays.fill(refTag1, (byte) 0x41);
		Arrays.fill(refTag2, (byte) 0x61);
		Arrays.fill(refTag3, (byte) 0x81);

		// should just work
		Arrays.fill(testTag, (byte) 0x41);
		testVerifier.insertChunkAuthTag(0, testTag);
		// insertion does not need to be in order
		Arrays.fill(testTag, (byte) 0x81);
		testVerifier.insertChunkAuthTag(2, testTag);
		Arrays.fill(testTag, (byte) 0x61);
		testVerifier.insertChunkAuthTag(1, testTag);

		assertArrayEquals(refTag1, testVerifier.getStoredChunkAuthTag(0));
		assertArrayEquals(refTag2, testVerifier.getStoredChunkAuthTag(1));
		assertArrayEquals(refTag3, testVerifier.getStoredChunkAuthTag(2));

		// switch auth tags
		Arrays.fill(testTag, (byte) 0x81);
		testVerifier.insertChunkAuthTag(1, testTag);
		Arrays.fill(testTag, (byte) 0x61);
		testVerifier.insertChunkAuthTag(2, testTag);

		assertArrayEquals(refTag2, testVerifier.getStoredChunkAuthTag(2));
		assertArrayEquals(refTag3, testVerifier.getStoredChunkAuthTag(1));

		// file auth tag should still be null as we did not call update
		assertNull(aesTestFile.readFileAuthenticationTag());
		testVerifier.updateFileAuthTag();
		// after update, file auth tag shoudl not be null anymore
		assertNotNull(aesTestFile.readFileAuthenticationTag());
		byte[] refTag = new byte[AuthTagVerifier.AUTH_TAG_SIZE];
		System.arraycopy(aesTestFile.readFileAuthenticationTag(), 0, refTag, 0,
				refTag.length);
		// however, inserting additional chunk auth tags should not directly
		// change the file auth tag
		testVerifier.insertChunkAuthTag(3, testTag);
		testVerifier.insertChunkAuthTag(4, testTag);
		assertArrayEquals(refTag, aesTestFile.readFileAuthenticationTag());
	}
}
