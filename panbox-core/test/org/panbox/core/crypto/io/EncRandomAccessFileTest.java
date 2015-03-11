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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.panbox.PanboxConstants;
import org.panbox.core.crypto.KeyConstants;
import org.panbox.core.crypto.io.EncRandomAccessFile.FileHeader;
import org.panbox.core.crypto.io.EncRandomAccessFile.FileHeader.FieldOffsets;
import org.panbox.core.crypto.randomness.SecureRandomWrapper;
import org.panbox.core.exception.FileEncryptionException;
import org.panbox.core.exception.FileIntegrityException;
import org.panbox.core.exception.RandomDataGenerationException;

/**
 * @author palige
 * 
 *         Abstract unit test class for {@link EncRandomAccessFile}
 *         -implementations. Actual unit test for specific implementations is to
 *         be implemented as a subclass and, correspondingly, must also be
 *         invoked separately.
 */
public abstract class EncRandomAccessFileTest {

	static {
		// add bouncycastle as default crypto provider
		Security.addProvider(new BouncyCastleProvider());
	}

	@Rule
	public TemporaryFolder tmpTestDir = new TemporaryFolder();
	public static SecretKey testKey;
	public EncRandomAccessFile aesTestFile;
	public SecureRandomWrapper wrapper;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	abstract int getBlockSize() throws NoSuchAlgorithmException,
			NoSuchPaddingException, NoSuchProviderException;

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
		byte[] tmp = new byte[getBlockSize()];

		wrapper.nextBytes(tmp);
		testKey = new SecretKeySpec(tmp, "AES");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		// if ((aesTestFile != null) && (aesTestFile instanceof
		// AESCBCRandomAccessFile || aesTestFile instanceof
		// AESGCMRandomAccessFile))
		// aesTestFile.getInstanceMap().clear();
	}

	/**
	 * helper method to create new {@link EncRandomAccessFile}-instances and
	 * files. To be implemented by the implementation-specific unit tests.
	 * 
	 * @param testKey
	 * @param testFile
	 * @param mode
	 * @return
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InstantiationException
	 */
	abstract protected EncRandomAccessFile createEncRAFInstance(
			SecretKey testKey, File testFile) throws FileNotFoundException,
			InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, RandomDataGenerationException,
			InvalidAlgorithmParameterException, FileEncryptionException,
			NoSuchProviderException, IOException, IllegalBlockSizeException,
			BadPaddingException, InstantiationException;

	abstract protected void clean();

	/**
	 * helper method to create new {@link EncRandomAccessFile}-instances. To be
	 * implemented by the implementation-specific unit tests.
	 * 
	 * @param testFile
	 * @param mode
	 * @return
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InstantiationException
	 */
	abstract protected EncRandomAccessFile openEncRAFInstance(File testFile,
			boolean writable) throws FileNotFoundException,
			InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, RandomDataGenerationException,
			InvalidAlgorithmParameterException, FileEncryptionException,
			NoSuchProviderException, IOException, IllegalBlockSizeException,
			BadPaddingException, InstantiationException;

	/**
	 * basic file creation test
	 * 
	 * @throws Exception
	 */
	@Test
	public void testFileCreation() throws Exception {
		File testFile = tmpTestDir.newFile("creationTest");
		aesTestFile = createEncRAFInstance(testKey, testFile);
		assertTrue(testFile.exists());
		// should not work
		aesTestFile.close();
		EncRandomAccessFile aesTestFile2 = null;
		try {
			aesTestFile2 = createEncRAFInstance(testKey, testFile);
		} catch (Exception e) {
			assertEquals("Non-empty file " + testFile.getAbsolutePath()
					+ " already exists!", e.getMessage());
		} finally {
			if (aesTestFile2 != null)
				aesTestFile2.close();
			aesTestFile.close();
		}

		// no instance over directories
		File folder = tmpTestDir.newFolder();
		try {
			createEncRAFInstance(testKey, folder);
		} catch (IOException e) {
			assertEquals(
					"Instantiation failed. " + folder + " is a directory!",
					e.getMessage());
		}

	}

	@Test
	public void testBooleanByteConv() throws Exception {
		byte[] test;
		test = EncRandomAccessFile.BooleanByteConv.bool2byte(false);
		assertEquals(1, test.length);
		assertEquals((byte) 0, test[0]);
		test = EncRandomAccessFile.BooleanByteConv.bool2byte(true);
		assertEquals((byte) 1, test[0]);
		assertEquals(1, test.length);

		assertFalse(EncRandomAccessFile.BooleanByteConv.byte2bool((byte) 0));
		assertTrue(EncRandomAccessFile.BooleanByteConv.byte2bool((byte) 1));
	}

	@Test
	public void testLongByteConv() throws Exception {
		long l = wrapper.nextLong();
		byte[] res = EncRandomAccessFile.LongByteConv.long2Bytes(l);
		assertEquals(l, EncRandomAccessFile.LongByteConv.bytes2Long(res));
	}

	@Test
	public void testIntByteConv() throws Exception {
		int l = wrapper.nextInt();
		byte[] res = EncRandomAccessFile.IntByteConv.int2byte(l);
		assertEquals(l, EncRandomAccessFile.IntByteConv.byte2int(res));
	}

	/**
	 * Test case for iterative writing of multiple full chunks in alignment with
	 * chunk offsets
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAlignedFullChunkWrite() throws Exception {
		File testFile = tmpTestDir.newFile("alignedfullchunkwriteTest");
		aesTestFile = createEncRAFInstance(testKey, testFile);

		// printable ascii chars, just for testing purposes
		int i = 0x41;
		byte[] buf = new byte[aesTestFile.CHUNK_DATA_SIZE];
		while (i < 0x61) {
			Arrays.fill(buf, (byte) i++);
			aesTestFile.write(buf);
		}
		aesTestFile.flush();

		aesTestFile.close();

		aesTestFile = openEncRAFInstance(testFile, false);
		assertNotEquals(-1, aesTestFile.getShareKeyVersion());
		aesTestFile.initWithShareKey(testKey);

		// reset values
		i = 0x41;
		byte[] ref = new byte[aesTestFile.CHUNK_DATA_SIZE];
		while (i < 0x61) {
			Arrays.fill(ref, (byte) i++);
			aesTestFile.read(buf);
			assertArrayEquals(ref, buf);
		}

		// EOF?
		assertEquals(-1, aesTestFile.read(buf));

		aesTestFile.close();
	}

	@Test
	public void testSeekBorders() throws Exception {
		File testFile = tmpTestDir.newFile("seekbordersTest");
		aesTestFile = createEncRAFInstance(testKey, testFile);

		aesTestFile.seek(aesTestFile.CHUNK_ENC_SIZE + 1);
		aesTestFile.write(0x41);
		aesTestFile.seek(aesTestFile.CHUNK_ENC_SIZE - 1);
		aesTestFile.write(0x41);
		aesTestFile.seek(aesTestFile.CHUNK_IV_SIZE - 1);
		aesTestFile.write(0x41);
		aesTestFile.seek(aesTestFile.CHUNK_IV_SIZE
				+ aesTestFile.CHUNK_DATA_SIZE + 1);
		aesTestFile.write(0x41);
		aesTestFile.seek(aesTestFile.CHUNK_IV_SIZE
				+ aesTestFile.CHUNK_DATA_SIZE + aesTestFile.CHUNK_TLEN - 1);
		aesTestFile.write(0x41);
		aesTestFile.flush();
		aesTestFile.close();
	}

	/**
	 * Test case for iterative writing of multiple partial chunks in alignment
	 * with chunk offsets. only the first half of each chunk is written.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAlignedPartialChunkWrite() throws Exception {
		File testFile = tmpTestDir.newFile("alignedfullchunkwriteTest");
		aesTestFile = createEncRAFInstance(testKey, testFile);

		byte[] buf = new byte[aesTestFile.CHUNK_DATA_SIZE];

		// initially fill test file completely with null bytes
		int filesize = aesTestFile.CHUNK_DATA_SIZE * (0x7f - 0x20);
		for (int j = 0; j < filesize; j += aesTestFile.CHUNK_DATA_SIZE) {
			Arrays.fill(buf, (byte) 0x00);
			aesTestFile.write(buf);
		}

		// now write partial chunks
		aesTestFile.seek(0);

		int i = 0x20;
		buf = new byte[aesTestFile.CHUNK_DATA_SIZE / 2];
		while (i < 0x7f) {
			Arrays.fill(buf, (byte) i++);
			aesTestFile.write(buf);
			aesTestFile.seek(aesTestFile.getFilePointer()
					+ aesTestFile.CHUNK_DATA_SIZE / 2);
		}
		aesTestFile.flush();
		aesTestFile.close();

		aesTestFile = openEncRAFInstance(testFile, false);
		assertNotEquals(-1, aesTestFile.getShareKeyVersion());
		aesTestFile.initWithShareKey(testKey);

		// reset values
		i = 0x20;
		byte[] ref = new byte[aesTestFile.CHUNK_DATA_SIZE / 2];
		while (i < 0x7f) {
			// check written bytes
			Arrays.fill(ref, (byte) i++);
			aesTestFile.read(buf);
			assertArrayEquals(ref, buf);
			// check bytes which should not have been overwritten
			Arrays.fill(ref, (byte) 0x00);
			aesTestFile.read(buf);
			assertArrayEquals(ref, buf);
		}
		aesTestFile.close();
	}

	/**
	 * Test case for iterative writing of multiple full chunks not aligned with
	 * the respective chunk offsets. full chunks are written in each iteration,
	 * the first half in the old, the second half in the new chunk.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMisalignedFullChunkWrite() throws Exception {
		File testFile = tmpTestDir.newFile("misalignedfullchunkwriteTest");
		aesTestFile = createEncRAFInstance(testKey, testFile);

		byte[] buf = new byte[aesTestFile.CHUNK_DATA_SIZE / 2];

		// fill first half chunk of file with null bytes
		Arrays.fill(buf, (byte) 0x00);
		aesTestFile.write(buf);

		// now write misaligned full chunks
		int i = 0x41;
		buf = new byte[aesTestFile.CHUNK_DATA_SIZE];
		while (i < 0x61) {
			Arrays.fill(buf, (byte) i++);
			aesTestFile.write(buf);
		}
		aesTestFile.flush();
		aesTestFile.close();

		aesTestFile = openEncRAFInstance(testFile, false);
		assertNotEquals(-1, aesTestFile.getShareKeyVersion());
		aesTestFile.initWithShareKey(testKey);

		// initial partial chunk which should not have been overwritten
		buf = new byte[aesTestFile.CHUNK_DATA_SIZE / 2];
		byte[] ref = new byte[aesTestFile.CHUNK_DATA_SIZE / 2];

		Arrays.fill(ref, (byte) 0x00);
		aesTestFile.read(buf);
		assertArrayEquals(ref, buf);

		// reset values and check full misaligned chunks
		i = 0x41;
		buf = new byte[aesTestFile.CHUNK_DATA_SIZE];
		ref = new byte[aesTestFile.CHUNK_DATA_SIZE];
		while (i < 0x61) {
			// check written bytes
			Arrays.fill(ref, (byte) i++);
			aesTestFile.read(buf);
			assertArrayEquals(ref, buf);
		}
		assertEquals(-1, aesTestFile.read());
		assertEquals(-1, aesTestFile.read(buf));
		aesTestFile.close();
	}

	/**
	 * tests reading/writing an array spanning multiple chunks at once
	 * 
	 * @throws Exception
	 */
	@Test
	public void testBigWriteRead() throws Exception {
		File testFile = tmpTestDir.newFile("bigwritereadTest");
		aesTestFile = createEncRAFInstance(testKey, testFile);

		int bufsize = (aesTestFile.CHUNK_DATA_SIZE * 21) + 4321;
		byte[] buf = new byte[bufsize];
		Arrays.fill(buf, (byte) 0x43);
		aesTestFile.write(buf);
		aesTestFile.write(0xff);
		aesTestFile.flush();
		aesTestFile.close();

		aesTestFile = openEncRAFInstance(testFile, false);
		assertNotEquals(-1, aesTestFile.getShareKeyVersion());
		aesTestFile.initWithShareKey(testKey);

		buf = new byte[bufsize];
		aesTestFile.read(buf);
		byte[] ref = new byte[bufsize];
		Arrays.fill(ref, (byte) 0x43);
		assertArrayEquals(ref, buf);
		assertEquals(0xff, aesTestFile.read());
		assertEquals(-1, aesTestFile.read());
		assertEquals(-1, aesTestFile.read(buf));
		aesTestFile.close();
	}

	/**
	 * test case for iterative writing of multiple partial chunks not aligned
	 * with the respective chunk offsets. only half a chunk is written in each
	 * iteration, the first quarter in the old, the second quarter in the new
	 * chunk.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMisalignedPartialChunkWrite() throws Exception {
		File testFile = tmpTestDir.newFile("misalignedpartialchunkwriteTest");
		aesTestFile = createEncRAFInstance(testKey, testFile);

		byte[] buf = new byte[aesTestFile.CHUNK_DATA_SIZE];

		// initially fill test file completely with null bytes
		int filesize = aesTestFile.CHUNK_DATA_SIZE * 0x21;

		for (int j = 0; j < filesize; j += aesTestFile.CHUNK_DATA_SIZE) {
			Arrays.fill(buf, (byte) 0x00);
			aesTestFile.write(buf);
		}

		aesTestFile.flush();
		aesTestFile.close();

		aesTestFile = openEncRAFInstance(testFile, true);
		assertNotEquals(-1, aesTestFile.getShareKeyVersion());
		aesTestFile.initWithShareKey(testKey);

		aesTestFile.skipBytes((aesTestFile.CHUNK_DATA_SIZE / 4) * 3);

		// now write misaligned half chunks
		int i = 0x41;
		buf = new byte[aesTestFile.CHUNK_DATA_SIZE / 2];

		while (i < 0x61) {
			Arrays.fill(buf, (byte) i++);
			aesTestFile.write(buf);
			aesTestFile.skipBytes(aesTestFile.CHUNK_DATA_SIZE / 2);
		}
		aesTestFile.flush();
		aesTestFile.close();

		aesTestFile = openEncRAFInstance(testFile, false);
		assertNotEquals(-1, aesTestFile.getShareKeyVersion());
		aesTestFile.initWithShareKey(testKey);

		// initial partial chunk which should not have been overwritten
		buf = new byte[aesTestFile.CHUNK_DATA_SIZE / 4 * 3];
		byte[] ref = new byte[aesTestFile.CHUNK_DATA_SIZE / 4 * 3];

		Arrays.fill(ref, (byte) 0x00);
		aesTestFile.read(buf);
		assertArrayEquals(ref, buf);

		// reset values and check partial misaligned chunks
		i = 0x41;
		buf = new byte[aesTestFile.CHUNK_DATA_SIZE / 2];
		ref = new byte[aesTestFile.CHUNK_DATA_SIZE / 2];
		while (i < 0x61) {
			// check written bytes
			Arrays.fill(ref, (byte) i++);
			aesTestFile.read(buf);
			assertArrayEquals(ref, buf);
			// check bytes which should not have been overwritten
			Arrays.fill(ref, (byte) 0x00);
			aesTestFile.read(buf);
			assertArrayEquals(ref, buf);
		}

		aesTestFile.close();
	}

	/**
	 * test deep seek
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDeepSeek() throws Exception {
		File testFile = tmpTestDir.newFile("deepseekTest");
		aesTestFile = createEncRAFInstance(testKey, testFile);

		aesTestFile.write(0x41);
		int seekdepth = (aesTestFile.CHUNK_DATA_SIZE * 5) + 4321;
		aesTestFile.seek(seekdepth);
		long storedfp = aesTestFile.backingRandomAccessFile.getFilePointer();

		int i = 0x41;
		byte[] buf = new byte[aesTestFile.CHUNK_DATA_SIZE / 2 - 1];

		while (i < 0x61) {
			Arrays.fill(buf, (byte) i++);
			aesTestFile.write(buf);
		}
		aesTestFile.flush();
		aesTestFile.close();

		aesTestFile = openEncRAFInstance(testFile, false);
		assertNotEquals(-1, aesTestFile.getShareKeyVersion());
		aesTestFile.initWithShareKey(testKey);

		// check first byte
		buf = new byte[4];
		aesTestFile.read(buf);
		assertEquals((byte) 0x41, buf[0]);

		aesTestFile.seek(0);
		aesTestFile.skipBytes(seekdepth);
		assertEquals(storedfp,
				aesTestFile.backingRandomAccessFile.getFilePointer());

		i = 0x41;
		buf = new byte[aesTestFile.CHUNK_DATA_SIZE / 2 - 1];
		byte[] ref = new byte[aesTestFile.CHUNK_DATA_SIZE / 2 - 1];
		while (i < 0x61) {
			Arrays.fill(ref, (byte) i++);
			aesTestFile.read(buf);
			assertArrayEquals(ref, buf);
		}

		assertEquals(-1, aesTestFile.read());
		assertEquals(-1, aesTestFile.read(buf));
		aesTestFile.close();
	}

	/**
	 * basic test to check if seek works as expected
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSeek() throws Exception {
		File testFile = tmpTestDir.newFile("seekTest");
		aesTestFile = createEncRAFInstance(testKey, testFile);

		// initial file size should be zero
		assertEquals(0, aesTestFile.length());

		// we should nonetheless be able to seek into the file
		long testpos = 1023;
		aesTestFile.seek(testpos);
		assertEquals(aesTestFile.getFilePointer(), testpos);

		// file size should still be zero
		assertEquals(0, aesTestFile.length());
		aesTestFile.write(0x41);

		// now file size should have been extended
		assertEquals(testpos + 1, aesTestFile.length());
		assertEquals(testpos + 1, aesTestFile.getFilePointer());

		// write some overlapping blocks of data using seek()
		byte[] buf = new byte[aesTestFile.CHUNK_DATA_SIZE + 100];

		for (int j = 0, i = 0x41; i < 0x62; j += aesTestFile.CHUNK_DATA_SIZE, i++) {
			aesTestFile.seek(j);
			Arrays.fill(buf, (byte) i);
			aesTestFile.write(buf);
		}

		aesTestFile.flush();
		// size should be that of 0x20 chunks + 100 bytes extra
		assertEquals((0x62 - 0x41) * aesTestFile.CHUNK_DATA_SIZE + 100,
				aesTestFile.length());

		aesTestFile.close();

		// check contents
		aesTestFile = openEncRAFInstance(testFile, false);
		assertNotEquals(-1, aesTestFile.getShareKeyVersion());
		aesTestFile.initWithShareKey(testKey);

		int i = 0x41;
		buf = new byte[aesTestFile.CHUNK_DATA_SIZE];
		byte[] ref = new byte[aesTestFile.CHUNK_DATA_SIZE];
		while (i < 0x62) {
			// check written bytes
			Arrays.fill(ref, (byte) i++);
			aesTestFile.read(buf);
			assertArrayEquals(ref, buf);
		}
		// check last 100 bytes
		buf = new byte[100];
		ref = new byte[100];
		Arrays.fill(ref, (byte) 0x61);
		aesTestFile.read(buf);
		assertArrayEquals(ref, buf);
		assertEquals(-1, aesTestFile.read());
		assertEquals(-1, aesTestFile.read(buf));
		aesTestFile.close();
	}

	/**
	 * tests the single-byte read/write calls
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSingleByteRead() throws Exception {
		File testFile = tmpTestDir.newFile("singlebytereadTest");
		aesTestFile = createEncRAFInstance(testKey, testFile);

		// int iterations = 70;
		int iterations = 10;

		// more than one chunk
		for (int j = 0; j < iterations; j++) {
			// write
			for (int i = 0; i < 256; i++) {
				aesTestFile.write(i);
			}
			// System.out.println(j);
		}
		aesTestFile.flush();
		aesTestFile.close();

		// read
		aesTestFile = openEncRAFInstance(testFile, false);
		assertNotEquals(-1, aesTestFile.getShareKeyVersion());
		aesTestFile.initWithShareKey(testKey);

		assertEquals(iterations * 256, aesTestFile.length(), 0);
		for (int j = 0; j < iterations; j++) {
			for (int i = 0; i < 256; i++) {
				assertEquals(i, aesTestFile.read());
			}
		}

		// EOF?
		assertEquals(-1, aesTestFile.read());

		aesTestFile.close();
	}

	/**
	 * tests generation of random chunk IVs
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGenerateRandomChunkIV() throws Exception {
		File testFile = tmpTestDir.newFile("singlebytereadTest");
		aesTestFile = createEncRAFInstance(testKey, testFile);

		byte[] lastIV = null, currentIV = null;
		byte[] nullIV = new byte[aesTestFile.CHUNK_IV_SIZE];
		Arrays.fill(nullIV, (byte) 0x00);
		for (int i = 0; i < 100; i++) {
			currentIV = aesTestFile.generateRandomChunkIV();
			assertFalse(Arrays.equals(currentIV, lastIV));
			assertFalse(Arrays.equals(currentIV, nullIV));
			assertEquals(aesTestFile.CHUNK_IV_SIZE, currentIV.length);
			lastIV = currentIV;
		}

		aesTestFile.close();
	}

	/**
	 * basic single byte read()/write() test with sample file
	 * 
	 * @throws Exception
	 */
	// @Test
	public void testSingleByteSampleFile() throws Exception {
		String file = "ubuntu-12.04.4-desktop-i386.iso";

		FileInputStream fis = new FileInputStream(new File(file));

		File testFile = new File(file + ".enc");
		aesTestFile = createEncRAFInstance(testKey, testFile);

		int i;
		while ((i = fis.read()) != -1) {
			aesTestFile.write(i);
		}
		aesTestFile.flush();
		aesTestFile.close();
		fis.close();

		aesTestFile = openEncRAFInstance(testFile, false);
		assertNotEquals(-1, aesTestFile.getShareKeyVersion());
		aesTestFile.initWithShareKey(testKey);

		RandomAccessFile out = new RandomAccessFile(file + ".dec", "rw");

		while ((i = aesTestFile.read()) != -1) {
			out.write(i);
		}
		aesTestFile.close();
		out.close();
	}

	// @Test
	public void testByteArraySampleFile() throws Exception {
		// String file = "blubb.pdf";
		// String file = "all.lst";

		String file = "ubuntu-12.04.4-desktop-i386.iso";

		FileInputStream fis = new FileInputStream(new File(file));

		File testFile = new File(file + ".enc");
		aesTestFile = createEncRAFInstance(testKey, testFile);

		int i = 0;
		byte[] buf = new byte[aesTestFile.CHUNK_DATA_SIZE];
		while ((i = fis.read(buf)) != -1) {
			aesTestFile.write(buf, 0, i);
		}
		aesTestFile.flush();
		aesTestFile.close();
		fis.close();

		aesTestFile = openEncRAFInstance(testFile, false);
		assertNotEquals(-1, aesTestFile.getShareKeyVersion());
		aesTestFile.initWithShareKey(testKey);

		RandomAccessFile out = new RandomAccessFile(file + ".dec", "rw");

		while ((i = aesTestFile.read(buf)) != -1) {
			out.write(buf, 0, i);
		}
		aesTestFile.close();
		out.close();
	}

	@Test
	public void testReadRetVal() throws Exception {
		File testFile = tmpTestDir.newFile("readretvalTest");
		aesTestFile = createEncRAFInstance(testKey, testFile);

		byte[] buf = new byte[1024];

		// initially fill test file completely with null bytes
		Arrays.fill(buf, (byte) 0x00);
		aesTestFile.write(buf);

		aesTestFile.flush();
		aesTestFile.close();

		aesTestFile = openEncRAFInstance(testFile, false);
		assertNotEquals(-1, aesTestFile.getShareKeyVersion());
		aesTestFile.initWithShareKey(testKey);

		// read
		buf = new byte[1000];
		int ret = aesTestFile.read(buf);
		// should have been read fully
		assertEquals(buf.length, ret);

		// should have been read only partially, 24 remaining bytes
		ret = aesTestFile.read(buf);
		assertEquals(24, ret);

		// EOF?
		ret = aesTestFile.read(buf);
		assertEquals(-1, ret);
	}

	@Test
	public void testReadOnly() throws Exception {
		File testFile = tmpTestDir.newFile("readonlyTest");
		aesTestFile = createEncRAFInstance(testKey, testFile);

		// if we create a new file, naturally we should have write access
		assertTrue(aesTestFile.backingFile.canWrite());
		aesTestFile.close();

		// read-only file
		aesTestFile = openEncRAFInstance(testFile, false);
		assertNotEquals(-1, aesTestFile.getShareKeyVersion());
		aesTestFile.initWithShareKey(testKey);

		try {
			// should trigger IOException
			aesTestFile.write((byte) 0x41);
			// aesTestFile.flush();
			fail("Expected exception due to write to read-only file!");
		} catch (IOException e) {
			// do nothing
		}
	}

	// /**
	// * tests the derivation function for chunk IVs
	// *
	// * @throws Exception
	// */
	// @Test
	// public void testDeriveChunkIV() throws Exception {
	// File testFile = tmpTestDir.newFile("derivechunkivTest");
	// aesTestFile = new AESCBCIVRandomAccessFile(testKey, testFile, "rw");
	//
	// // derivation without base IV must fail
	// try {
	// aesTestFile.deriveChunkIV(0);
	// fail("exception expected!");
	// } catch (Exception e) {
	// // do nothing
	// }
	//
	//
	// IvParameterSpec spec = aesTestFile.deriveChunkIV(0);
	// assertNotNull(spec);
	// assertNotNull(spec.getIV());
	// assertEquals(spec.getIV().length, CIPHER_BLOCKSIZE);
	//
	// // at least some bits in iv should be set, so check for it
	// byte[] zeroarray = new byte[CIPHER_BLOCKSIZE];
	// Arrays.fill(zeroarray, (byte) 0x00);
	// assertFalse(Arrays.equals(zeroarray, spec.getIV()));
	//
	// // generated IVs should not repeat; basic check of some samples
	// IvParameterSpec oldspec;
	// for (int i = 1; i <= 100; i++) {
	// oldspec = spec;
	// spec = aesTestFile.deriveChunkIV(i);
	// assertEquals(oldspec.getIV().length, spec.getIV().length);
	// assertFalse(Arrays.equals(oldspec.getIV(), spec.getIV()));
	// }
	//
	// // generated IVs for the same chunk need to be equal
	// AESCBCIVRandomAccessFile aesTestFile2 = new AESCBCIVRandomAccessFile(
	// testKey, testFile, "r");
	// aesTestFile2.setFileBaseIV(fileBaseIV);
	//
	// spec = aesTestFile.deriveChunkIV(1234);
	// IvParameterSpec ref = aesTestFile2.deriveChunkIV(1234);
	// assertNotEquals(spec, ref);
	// assertArrayEquals(ref.getIV(), spec.getIV());
	// }

	// @Test
	// public void testSetFileBaseIV() throws Exception {
	// File testFile = tmpTestDir.newFile("setfilebaseivTest");
	// aesTestFile = new AESCBCIVRandomAccessFile(testKey, testFile, "rw");
	//
	// // null iv test
	// try {
	// aesTestFile.setFileBaseIV(null);
	// fail("exception missing!");
	// } catch (FileEncryptionException e) {
	// // do nothing
	// }
	//
	// // wrong size iv test
	// byte[] bla = "bla0".getBytes();
	// try {
	// aesTestFile.setFileBaseIV(new IvParameterSpec(bla));
	// fail("exception missing!");
	// } catch (FileEncryptionException e) {
	// // do nothing
	// }
	//
	// // uninitialized iv test
	// bla = new byte[16];
	// Arrays.fill(bla, (byte) 0x00);
	// try {
	// aesTestFile.setFileBaseIV(new IvParameterSpec(bla));
	// fail("exception missing!");
	// } catch (FileEncryptionException e) {
	// // do nothing
	// }
	//
	// // this should work
	//
	// }

	@Test
	public void testsetshareKey() throws Exception {
		File testFile = tmpTestDir.newFile("setsharekeyTest");
		try {
			aesTestFile = createEncRAFInstance(null, testFile);
			fail("exception expected!");
		} catch (FileEncryptionException e) {
			// do nothing
		} finally {
			clean();
		}

		aesTestFile = createEncRAFInstance(testKey, testFile);
		aesTestFile.write(0xff);
		aesTestFile.flush();
		aesTestFile.close();
		// // key already set
		// aesTestFile = createEncRAFInstance(testKey, testFile);
		// try {
		// aesTestFile.initWithShareKey(testKey);
		// fail("exception expected!");
		// } catch (FileEncryptionException e) {
		// // do nothing
		// }

		// same instance, key already set but header was re-read
		EncRandomAccessFile aesTestFile2 = openEncRAFInstance(testFile, true);
		// just to be sure, previous instance should have been invalidated by
		// close()
		assertNotEquals(aesTestFile, aesTestFile2);

		try {
			aesTestFile2.read();
			fail("exception expected!");
		} catch (Exception e) {
			// do nothing
		}

		// however, this should work
		aesTestFile2.initWithShareKey(testKey);
		aesTestFile2.read();
		aesTestFile2.close();

	}

	/**
	 * test for correct file truncation
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTruncate() throws Exception {
		File testFile = tmpTestDir.newFile("truncateTest");
		aesTestFile = createEncRAFInstance(testKey, testFile);

		int i = 0x41;
		byte[] buf = new byte[aesTestFile.CHUNK_DATA_SIZE];

		// initial size should be zero
		assertEquals(0, aesTestFile.length());

		// write contents
		while (i < 0x61) {
			Arrays.fill(buf, (byte) i++);
			aesTestFile.write(buf);
		}

		// size should have increased accordingly
		assertEquals((0x61 - 0x41) * aesTestFile.CHUNK_DATA_SIZE,
				aesTestFile.length());

		aesTestFile.setLength(aesTestFile.length() - 1000);

		// size should have decreased by 1000
		assertEquals((0x61 - 0x41) * aesTestFile.CHUNK_DATA_SIZE - 1000,
				aesTestFile.length());

		aesTestFile.flush();
		aesTestFile.close();

		// check if last full and partial chunk are still intact (i.e. been
		// rewritten correctly)
		aesTestFile = openEncRAFInstance(testFile, false);
		assertNotEquals(-1, aesTestFile.getShareKeyVersion());
		aesTestFile.initWithShareKey(testKey);

		// reset values
		i = 0x41;
		byte[] ref = new byte[aesTestFile.CHUNK_DATA_SIZE];
		// only read up to 0x60
		while (i < 0x60) {
			Arrays.fill(ref, (byte) i++);
			aesTestFile.read(buf);
			assertArrayEquals(ref, buf);
		}

		buf = new byte[aesTestFile.CHUNK_DATA_SIZE - 1000];
		// last partial chunk
		assertEquals(aesTestFile.CHUNK_DATA_SIZE - 1000, aesTestFile.read(buf));
		ref = new byte[aesTestFile.CHUNK_DATA_SIZE - 1000];
		Arrays.fill(ref, (byte) 0x60);
		assertArrayEquals(ref, buf);
		assertEquals(-1, aesTestFile.read());
		aesTestFile.close();
	}

	/**
	 * basic test for file extension through setLength(newLength)
	 * 
	 * @throws Exception
	 */
	@Test
	public void testExtend() throws Exception {
		File testFile = tmpTestDir.newFile("extendTest");
		aesTestFile = createEncRAFInstance(testKey, testFile);

		int i = 0x41;
		byte[] buf = new byte[aesTestFile.CHUNK_DATA_SIZE];

		// initial size should be zero
		assertEquals(0, aesTestFile.length());

		// write contents
		while (i < 0x61) {
			Arrays.fill(buf, (byte) i++);
			aesTestFile.write(buf);
		}

		// size should have increased accordingly
		assertEquals((0x61 - 0x41) * aesTestFile.CHUNK_DATA_SIZE,
				aesTestFile.length());

		aesTestFile.setLength(aesTestFile.length() + 1000);

		// size should have increased by 100
		assertEquals((0x61 - 0x41) * aesTestFile.CHUNK_DATA_SIZE + 1000,
				aesTestFile.length());

		aesTestFile.flush();

		aesTestFile.close();

		// check if last full and partial chunk are still intact (i.e. have been
		// rewritten correctly)
		aesTestFile = openEncRAFInstance(testFile, false);
		assertNotEquals(-1, aesTestFile.getShareKeyVersion());
		aesTestFile.initWithShareKey(testKey);

		// reset values
		i = 0x41;
		byte[] ref = new byte[aesTestFile.CHUNK_DATA_SIZE];
		while (i < 0x61) {
			Arrays.fill(ref, (byte) i++);
			aesTestFile.read(buf);
			assertArrayEquals(ref, buf);
		}

		buf = new byte[1000];
		// read "extended" part of file, 1000 additional bytes should be
		// available. contents *should* be zero (due to our implementation)
		assertEquals(1000, aesTestFile.read(buf));
		ref = new byte[1000];
		Arrays.fill(ref, (byte) 0x00);
		assertArrayEquals(ref, buf);

		assertEquals(-1, aesTestFile.read());
		aesTestFile.close();
	}

	@Test
	public void testHeaderNewFile() throws Exception {
		File testFile = tmpTestDir.newFile("headernewfileTest");
		aesTestFile = createEncRAFInstance(testKey, testFile);

		// check header contents for new file

		// test secret file key which should just have been generated randomly
		SecretKey fileKey = aesTestFile.fHeader.getDecryptedFileKey();
		assertNotNull(fileKey);
		assertEquals(KeyConstants.SYMMETRIC_FILE_KEY_SIZE_BYTES,
				fileKey.getEncoded().length);
		byte[] ref = new byte[getBlockSize()];
		Arrays.fill(ref, (byte) 0x00);
		assertFalse(Arrays.equals(ref, fileKey.getEncoded()));

		// check header fields
		RandomAccessFile rfile = new RandomAccessFile(testFile, "r");
		FieldOffsets offsets = aesTestFile.fHeader.OffsetTable;
		// magic
		rfile.seek(offsets.FIELD_PANBOX_FILE_MAGIC);
		byte[] tmp = new byte[PanboxConstants.PANBOX_FILE_MAGIC.length];
		rfile.read(tmp, 0, tmp.length);
		assertArrayEquals(PanboxConstants.PANBOX_FILE_MAGIC, tmp);

		// version
		rfile.seek(offsets.FIELD_PANBOX_FILE_VERSION);
		tmp = new byte[PanboxConstants.PANBOX_VERSION.length];
		rfile.read(tmp, 0, tmp.length);
		assertArrayEquals(PanboxConstants.PANBOX_VERSION, tmp);

		// check if decrypted file key != encrypted file key on disk
		rfile.seek(offsets.FIELD_FILE_KEY);
		byte[] tmpfilekey = new byte[getBlockSize()];
		rfile.read(tmpfilekey, 0, tmpfilekey.length);
		assertFalse(Arrays.equals(fileKey.getEncoded(), tmpfilekey));

		// share key version
		rfile.seek(offsets.FIELD_SHARE_KEY_VERSION);
		tmp = new byte[Integer.SIZE / 4];
		rfile.read(tmp, 0, tmp.length);
		// int conversion ..
		int tmp_sharekeyversion = (tmp[0] << 24) & 0xff000000 | (tmp[1] << 16)
				& 0xff0000 | (tmp[2] << 8) & 0xff00 | (tmp[3] << 0) & 0xff;
		assertEquals(0, tmp_sharekeyversion);

		// file auth tag
		byte[] tmpfileauthtag = null;
		if (aesTestFile.implementsAuthentication()) {
			rfile.seek(offsets.FIELD_FILE_AUTH_TAG);
			tmpfileauthtag = new byte[AuthTagVerifier.AUTH_TAG_SIZE];
			rfile.read(tmpfileauthtag, 0, tmpfileauthtag.length);
			// for a new file, auth tag initially should have been set to zero
			ref = new byte[tmpfileauthtag.length];
			Arrays.fill(ref, (byte) 0x00);
			assertArrayEquals(ref, tmpfileauthtag);
		}

		// header auth tag
		rfile.seek(offsets.FIELD_HEADER_AUTH_TAG);
		byte[] tmpheaderauthtag = new byte[FileHeader.AUTH_TAG_SIZE];
		rfile.read(tmpheaderauthtag, 0, tmpheaderauthtag.length);
		ref = new byte[tmpheaderauthtag.length];
		Arrays.fill(ref, (byte) 0x00);
		assertFalse(Arrays.equals(ref, tmpheaderauthtag));

		assertEquals(aesTestFile.fHeader.headerSize(), rfile.getFilePointer());

		// close, re-read, compare dynamic values with the ones read beforehand
		aesTestFile.close();
		rfile.close();

		aesTestFile = openEncRAFInstance(testFile, false);
		assertNotEquals(-1, aesTestFile.getShareKeyVersion());
		aesTestFile.initWithShareKey(testKey);

		rfile = new RandomAccessFile(testFile, "r");
		offsets = aesTestFile.fHeader.OffsetTable;

		// file key
		rfile.seek(offsets.FIELD_FILE_KEY);
		tmp = new byte[getBlockSize()];
		rfile.read(tmp, 0, tmp.length);
		assertArrayEquals(tmpfilekey, tmp);

		// share key version
		rfile.seek(offsets.FIELD_SHARE_KEY_VERSION);
		tmp = new byte[Integer.SIZE / 4];
		rfile.read(tmp, 0, tmp.length);
		// int conversion ..
		int tmp_int = (tmp[0] << 24) & 0xff000000 | (tmp[1] << 16) & 0xff0000
				| (tmp[2] << 8) & 0xff00 | (tmp[3] << 0) & 0xff;
		assertEquals(tmp_sharekeyversion, tmp_int);

		// file auth tag
		if (aesTestFile.implementsAuthentication()) {
			rfile.seek(offsets.FIELD_FILE_AUTH_TAG);
			tmp = new byte[AuthTagVerifier.AUTH_TAG_SIZE];
			rfile.read(tmp, 0, tmp.length);
			// for a new file, auth tag initially should have been set to zero
			assertArrayEquals(tmpfileauthtag, tmp);
		}

		// header auth tag
		rfile.seek(offsets.FIELD_HEADER_AUTH_TAG);
		tmp = new byte[FileHeader.AUTH_TAG_SIZE];
		rfile.read(tmp, 0, tmp.length);
		assertArrayEquals(tmpheaderauthtag, tmp);

		EncRandomAccessFile aesTestFile2 = null;
		try {
			aesTestFile2 = createEncRAFInstance(testKey, testFile);
			fail("Expected exception during creation of new file");
		} catch (Exception e) {
			assertEquals("Non-empty file " + testFile.getAbsolutePath()
					+ " already exists!", e.getMessage());
		} finally {
			if (aesTestFile2 != null) {
				aesTestFile2.close();
			}
			rfile.close();
		}
	}

	@Test
	public void testHeaderIntegrity() throws Exception {
		File testFile = tmpTestDir.newFile("headerintegrityTest");
		aesTestFile = createEncRAFInstance(testKey, testFile);

		// modify header contents for new file

		// test secret file key which should just have been generated randomly
		SecretKey fileKey = aesTestFile.fHeader.getDecryptedFileKey();
		assertNotNull(fileKey);
		assertEquals(KeyConstants.SYMMETRIC_FILE_KEY_SIZE_BYTES,
				fileKey.getEncoded().length);
		byte[] ref = new byte[getBlockSize()];
		Arrays.fill(ref, (byte) 0x00);
		assertFalse(Arrays.equals(ref, fileKey.getEncoded()));

		// check header fields
		RandomAccessFile rfile = new RandomAccessFile(testFile, "rw");
		FieldOffsets offsets = aesTestFile.fHeader.OffsetTable;

		// write one byte
		aesTestFile.write(1);
		aesTestFile.flush();
		aesTestFile.close();

		// modify encrypted file key
		rfile.seek(offsets.FIELD_FILE_KEY);
		byte[] tmpfilekey = new byte[getBlockSize()];
		Arrays.fill(tmpfilekey, (byte) 0x41);
		rfile.write(tmpfilekey, 0, tmpfilekey.length);
		rfile.close();

		try {
			aesTestFile = openEncRAFInstance(testFile, false);
			assertNotEquals(-1, aesTestFile.getShareKeyVersion());
			aesTestFile.initWithShareKey(testKey);

			fail("Expected exception due to invalid file header!");
		} catch (Exception e) {
			assertEquals("HMac of file header is invalid!", e.getMessage());
		} finally {
			aesTestFile.close();
		}
	}

	/**
	 * tests instance creation. for now, for each file there should only exist
	 * exactly one instance.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testInstanceManagement() throws Exception {
		//
		File testFile = tmpTestDir.newFile("concurrentaccessTest");
		EncRandomAccessFile aesTestFile2, aesTestFile3, aesTestFile4;

		// assertEquals(0, aesTestFile.getInstanceMap().size());
		// should work
		aesTestFile = createEncRAFInstance(testKey, testFile);
		// assertEquals(1, aesTestFile.getInstanceMap().size());
		aesTestFile2 = openEncRAFInstance(testFile, false);
		assertNotEquals(aesTestFile, aesTestFile2);
		// assertEquals(2, aesTestFile.getInstanceMap().size());
		aesTestFile2.close();
		// assertEquals(1, aesTestFile.getInstanceMap().size());

		// should fail
		try {
			aesTestFile = createEncRAFInstance(testKey, testFile);
			fail("Opening multiple instances with write access should cause an exception!");
		} catch (FileEncryptionException e) {
			assertEquals("Non-empty file " + testFile.getAbsolutePath()
					+ " already exists!", e.getMessage());
		}

		try {
			aesTestFile.open();
			fail("Calling open() on a file which is already open should cause an exception!");
		} catch (Exception e) {
			assertEquals("File has already been opened!", e.getMessage());
			// aesTestFile.close();
		}

		aesTestFile2 = getInstance(testFile, true);
		assertEquals(aesTestFile2, aesTestFile);

		aesTestFile3 = getInstance(testFile, true);
		assertEquals(aesTestFile2, aesTestFile3);

		aesTestFile3 = openEncRAFInstance(testFile, false);
		assertNotEquals(aesTestFile2, aesTestFile3);

		aesTestFile4 = getInstance(testFile, false);
		assertEquals(aesTestFile4, aesTestFile3);

		aesTestFile4.close();
		assertFalse(aesTestFile3.isOpen());
		// assertEquals(1, aesTestFile.getInstanceMap().size());
		aesTestFile2.close();
		// redundant call, just to be sure double-close() throws no exception
		assertFalse(aesTestFile.isOpen());
		// assertEquals(0, aesTestFile.getInstanceMap().size());
	}

	/**
	 * tests synchronization/locking in case of multiple threads reading and one
	 * thread writing one specific file. reduce number of iterations, if this
	 * test takes to long.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testReadWriteConcurreny() throws Exception {
		File testFile = tmpTestDir.newFile("readwriteconcurrencyTest");
		aesTestFile = createEncRAFInstance(testKey, testFile);

		final int MAXOFFSET = 15 * 1024 * 1024; // max 10MB +
												// max(random-chunksize)
		final int ITERATIONS = 1000; // number of random write calls
		// final int MIN_CHUNKSIZE = 1;
		final int MAX_CHUNKSIZE = 5 * 1024;

		byte[] TESTDATA = new byte[MAXOFFSET + MAX_CHUNKSIZE];
		(new Random(System.nanoTime())).nextBytes(TESTDATA);

		ExceptionHandlerImpl e = new ExceptionHandlerImpl();
		Thread[] threads = new Thread[16];
		for (int i = 0; i < threads.length; i += 2) {
			// writer thread
			threads[i] = new Thread(new RandomTestIO(true, aesTestFile,
					MAXOFFSET, 1, 5 * 1024, ITERATIONS, e, TESTDATA));
			// reader threads
			threads[i + 1] = new Thread(new RandomTestIO(false, aesTestFile,
					MAXOFFSET, 1, 5 * 1024, ITERATIONS, e, TESTDATA));
		}

		System.out.println("start");
		;
		for (int i = 0; i < threads.length; i++) {
			threads[i].start();
		}

		for (int i = 0; i < threads.length; i++) {
			threads[i].join();
		}

		aesTestFile.flush();
		aesTestFile.close();

		EncRandomAccessFile aesTestFile2 = openEncRAFInstance(testFile, false);
		aesTestFile2.initWithShareKey(testKey);

		byte[] buf = new byte[1024];

		int i = 0, ctr = 0;
		while ((i = aesTestFile2.read(buf)) != -1) {
			int end = Math.min(buf.length, i);
			for (int j = 0; j < end; j++) {
				if (buf[j] != 0) {
					// not all bytes in the output file may have been written,
					// so
					// just check if the ones which have been set have been
					// written
					// correctly
					assertEquals(TESTDATA[j + buf.length * ctr], buf[j]);
				}
			}
			ctr++;
		}

		aesTestFile2.close();
	}

	//
	/**
	 * tests synchronization/locking in case of multiple threads writing one
	 * specific file. reduce number of iterations, if this test takes to long.
	 * 
	 * @throws Exception
	 */
	// @Test
	public void testWriteWriteConcurrency() throws Exception {
		File testFile = tmpTestDir.newFile("writewriteconcurrencyTest");
		aesTestFile = createEncRAFInstance(testKey, testFile);
		final int MAXOFFSET = 15 * 1024 * 1024; // max 10MB +
												// max(random-chunksize)
		final int ITERATIONS = 1000; // number of random write calls
		final int MIN_CHUNKSIZE = 1;
		final int MAX_CHUNKSIZE = 5 * 1024;

		byte[] TESTDATA = new byte[MAXOFFSET + MAX_CHUNKSIZE];
		(new Random(System.nanoTime())).nextBytes(TESTDATA);

		ExceptionHandlerImpl e = new ExceptionHandlerImpl();
		Thread[] threads = new Thread[4];
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread(new RandomTestIO(true, aesTestFile,
					MAXOFFSET, MIN_CHUNKSIZE, MAX_CHUNKSIZE, ITERATIONS, e,
					TESTDATA));
		}

		for (int i = 0; i < threads.length; i++) {
			threads[i].start();
		}

		for (int i = 0; i < threads.length; i++) {
			threads[i].join();
		}

		aesTestFile.flush();
		aesTestFile.close();

		EncRandomAccessFile aesTestFile2 = openEncRAFInstance(testFile, false);
		aesTestFile2.initWithShareKey(testKey);

		byte[] buf = new byte[1024];

		int i = 0, ctr = 0;
		while ((i = aesTestFile2.read(buf)) != -1) {
			int end = Math.min(buf.length, i);
			for (int j = 0; j < end; j++) {
				// not all bytes in the output file may have been written, so
				// just check if the ones which have been set have been written
				// correctly
				if (buf[j] != 0) {
					assertEquals(TESTDATA[j + buf.length * ctr], buf[j]);
				}
			}
			ctr++;
		}

		aesTestFile2.close();
	}

	interface ExceptionHandler {
		public void notify(Exception e);
	}

	class ExceptionHandlerImpl implements ExceptionHandler {
		@Override
		public void notify(Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * @author palige
	 * 
	 *         helper class for spawning multiple threads concurrently
	 *         reading/writing random locations within a testfile. In case of
	 *         exceptions being thrown, those are passed to an implementation of
	 *         {@link ExceptionHandler}.
	 * 
	 *         Data to be written are taken from random ranges within TESTDATA.
	 *         Data to be read are just randomly read and discarded.
	 */
	class RandomTestIO implements Runnable {
		EncRandomAccessFile file;
		int MAXOFFSET;
		int bufmin;
		int bufmax;
		int iterations;
		byte[] buf;
		Random rnd = new Random(System.nanoTime());
		boolean write;
		private ExceptionHandler exceptionHandler;
		private byte[] TESTDATA;

		public RandomTestIO(boolean write, EncRandomAccessFile file,
				int mAXOFFSET, int bufmin, int bufmax, int iterations,
				ExceptionHandler exceptionHandler, byte[] TESTDATA) {
			super();
			this.file = file;
			this.MAXOFFSET = mAXOFFSET;
			this.bufmin = bufmin;
			this.bufmax = bufmax;
			this.iterations = iterations;
			this.write = write;
			this.exceptionHandler = exceptionHandler;
			this.TESTDATA = TESTDATA;
		}

		@Override
		public void run() {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e1) {
				// do nothing
			}

			for (int i = 0; i < iterations; i++) {
				// try {
				// Thread.sleep(randInt(50, 200));
				// } catch (InterruptedException e1) {
				// }
				try {
					if (write) {
						// System.out
						// .println("Thread: "
						// + Thread.currentThread().getName()
						// + " writing");

						int offset = rnd.nextInt(MAXOFFSET);
						int bufsize = Math.min(TESTDATA.length - offset - 1,
								randInt(bufmin, bufmax));
						file.writeAt(offset, TESTDATA, offset, bufsize);
					} else {
						// read
						// System.out
						// .println("Thread: "
						// + Thread.currentThread().getName()
						// + " reading");

						buf = new byte[randInt(bufmin, bufmax)];
						file.readAt(rnd.nextInt(MAXOFFSET), buf);
					}
				} catch (IOException | FileEncryptionException
						| FileIntegrityException e) {
					exceptionHandler.notify(e);
				}
			}
		}
	}

	/**
	 * see
	 * http://stackoverflow.com/questions/363681/generating-random-integers-in
	 * -a-range-with-java
	 * 
	 * Returns a pseudo-random number between min and max, inclusive. The
	 * difference between min and max can be at most
	 * <code>Integer.MAX_VALUE - 1</code>.
	 * 
	 * @param min
	 *            Minimum value
	 * @param max
	 *            Maximum value. Must be greater than min.
	 * @return Integer between min and max, inclusive.
	 * @see java.util.Random#nextInt(int)
	 */
	static int randInt(int min, int max) {

		// NOTE: Usually this should be a field rather than a method
		// variable so that it is not re-seeded every call.
		Random rand = new Random();

		// nextInt is normally exclusive of the top value,
		// so add 1 to make it inclusive
		int randomNum = rand.nextInt((max - min) + 1) + min;

		return randomNum;
	}

	@Test
	public void testRename() throws Exception {
		File testFile = tmpTestDir.newFile("renameTest");
		aesTestFile = createEncRAFInstance(testKey, testFile);
		File renamedFile = new File(tmpTestDir.getRoot(), "renamedFile");
		byte[] buf = new byte[12];
		Arrays.fill(buf, (byte) 0x41);
		// write in original file
		aesTestFile.write(buf);
		aesTestFile.flush();
		File oldbackingFile = aesTestFile.backingFile;

		// rename file
		assertTrue(aesTestFile.renameTo(renamedFile));
		// backing file instance should have been replaced
		assertNotEquals(oldbackingFile, aesTestFile.backingFile);
		// former backing file should have ceased to exist
		assertFalse(oldbackingFile.exists());
		Arrays.fill(buf, (byte) 0x61);
		// write in renamed file
		aesTestFile.write(buf);
		aesTestFile.flush();
		aesTestFile.close();

		aesTestFile = openEncRAFInstance(renamedFile, false);
		aesTestFile.initWithShareKey(testKey);
		byte[] ref = new byte[24];
		Arrays.fill(ref, 0, 12, (byte) 0x41);
		Arrays.fill(ref, 12, 24, (byte) 0x61);
		buf = new byte[24];
		// check file contents
		aesTestFile.read(buf);
		assertArrayEquals(ref, buf);
		aesTestFile.close();

		File testFileA = tmpTestDir.newFile("renameTestA");
		EncRandomAccessFile aesTestFileA = createEncRAFInstance(testKey,
				testFileA);
		// aesTestFileA.printInstanceMap();

		File testFileB = tmpTestDir.newFile("renameTestB");
		assertTrue(aesTestFileA.renameTo(testFileB));
		// aesTestFileA.printInstanceMap();

		// as instance lookup is based on using the backend files as keys for
		// the lookup table, trying to open a new instance upon the file we just
		// renamed to original file to should yield the same instance
		EncRandomAccessFile aesTestFileB = getInstance(testFileB, true);
		assertEquals(aesTestFileA, aesTestFileB);

		aesTestFileA.close();
		aesTestFileB = openEncRAFInstance(testFileB, true);
		assertNotEquals(aesTestFileA, aesTestFileB);

		// original file should not exist anymore
		assertFalse(testFileA.exists());
		try {
			// opening an instance should cause an exception correspondingly
			openEncRAFInstance(testFileA, true);
		} catch (Exception e) {
			assertEquals("File " + testFileA + " not found!", e.getMessage());
		}

		aesTestFileA.close();
		aesTestFileB.close();
		// assertTrue(testFileB.delete());
		//
		// aesTestFileA = createEncRAFInstance(testKey, testFileA);
		// aesTestFileB = createEncRAFInstance(testKey, testFileB);
		// assertNotEquals(aesTestFileA, aesTestFileB);
		// try {
		// // renaming should cause an exception if there already exists
		// // another EncRandomAccessFile instance of the file we try to rename
		// // to
		// aesTestFileA.renameTo(testFileB);
		// } catch (Exception e) {
		// assertTrue(e.getMessage().endsWith(
		// "instance already exists for the given file"));
		// }

	}

	// @Test
	public void dummy() {
		// random access file behaviour testing, to be removed
		RandomAccessFile file = null;
		String home = System.getProperty("user.home");

		try {

			// File tmp = tmpTestDir.newFile("dummy");
			File tmp = new File(home + "/pb/" + "dummy");
			file = new RandomAccessFile(tmp, "rw");

			byte[] buf = new byte[1024];

			// initially fill test file completely with null bytes
			Arrays.fill(buf, (byte) 0x41);
			file.write(buf);
			byte[] buf2 = new byte[1024];

			file.seek(512);

			long fp = file.getFilePointer();
			int ret = file.read(buf2);
			// file.write('a');
			System.out.println("ret: " + ret);
			System.out.println("lengh: " + file.length());
			System.out.println("fp: " + fp);
			file.close();

			// file.close();
			//
			// file = new RandomAccessFile(tmp, "rw");
			//
			// file.setLength(1025);
			// // read
			// buf = new byte[1000];
			// int ret = file.read(buf);
			// // should have been read fully
			// assertEquals(buf.length, ret);
			//
			// // should have been read only partially, EOF should result in -1
			// ret = file.read(buf);
			// assertEquals(24, ret);
			//
			// ret = file.read(buf);
			// assertEquals(-1, ret);

			// file.seek(2000);
			// byte[] buf = new byte[1000];
			// Arrays.fill(buf, (byte) 0x41);
			// file.write(buf);
			// file.close();

			// aesTestFile = new AESCBCIVRandomAccessFile(testKey, tmp, "rw");
			// // printable ascii chars, just for testing purposes
			// int i = 0x20;
			// byte[] buf = new byte[aesTestFile.CHUNK_DATA_SIZE / 2];
			// System.out.println("chunkSize: " + aesTestFile.CHUNK_DATA_SIZE);
			// while (i < 0x7f) {
			// Arrays.fill(buf, (byte) i++);
			// file.write(buf);
			// file.skipBytes(buf.length);
			// }
			// System.out.println("dummy() path: " + tmp.getAbsolutePath());
			// file.close();

			// System.out.println("getFilePointer(): " + file.getFilePointer());
			// byte[] foo = new byte[4096];
			// Arrays.fill(foo, (byte) 0x41);
			// file.write(foo);
			// System.out.println("length:_" + file.length());
			// System.out.println("getFilePointer(): " + file.getFilePointer());
			// Arrays.fill(foo, (byte) 0x61);
			// file.write(foo);
			// System.out.println("length:_" + file.length());
			// System.out.println("getFilePointer(): " + file.getFilePointer());
			//
			// file.setLength(12288);
			// // System.out.println("length:_" + file.length());
			// // System.out.println("getFilePointer(): " +
			// file.getFilePointer());
			// int i;
			// file.seek(0);
			// while ((i = file.read()) != -1)
			// System.out.print((char) i);
			// while ((i = file.read()) == -1)
			// System.out.print((char) i);
			//
			// file.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
			// } catch (InvalidKeyException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// } catch (NoSuchAlgorithmException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// } catch (NoSuchPaddingException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// } catch (InvalidAlgorithmParameterException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// } catch (RandomDataGenerationException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
		}

	}

	protected abstract EncRandomAccessFile getInstance(File testfile,
			boolean writeable) throws FileEncryptionException, IOException;
}
