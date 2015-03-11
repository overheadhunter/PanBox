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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.Security;
import java.util.Arrays;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.panbox.core.crypto.randomness.SecureRandomWrapper;

public class EncRandomAccessStreamTest {

	static {
		// add bouncycastle as default crypto provider
		Security.addProvider(new BouncyCastleProvider());
	}

	@Rule
	public TemporaryFolder tmpTestDir = new TemporaryFolder();

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * FIXME: hard-coded values
	 */
	final int BLOCKSIZE = 16;
	final int CHUNKSIZE = BLOCKSIZE * EncRandomAccessFile.CHUNK_MULTIPLE;

	@Test
	public void testCBCImpl() throws Exception {
		File testFile = tmpTestDir.newFile("cbcimplTest");

		final SecretKey testKey;
		final SecureRandomWrapper wrapper = SecureRandomWrapper.getInstance();
		byte[] tmp = new byte[BLOCKSIZE];
		wrapper.nextBytes(tmp);
		testKey = new SecretKeySpec(tmp, "AES");

		// open new encrypting output stream
		EncRandomAccessOutputStream outStream = new EncRandomAccessOutputStream(
				AESCBCRandomAccessFile.create(0, testKey, testFile));

		// write single bytes, partial and full chunks

		for (int i = 0; i <= Byte.MAX_VALUE; i++) {
			outStream.write(i);
		}

		byte[] buf = new byte[CHUNKSIZE / 2 + 11];
		int i = 0x41;
		while (i < 0x61) {
			Arrays.fill(buf, (byte) i++);
			outStream.write(buf);
		}

		buf = new byte[CHUNKSIZE * 3 + 11];

		while (i < 0x71) {
			Arrays.fill(buf, (byte) i++);
			outStream.write(buf);
		}

		outStream.write(0xaa);
		outStream.write(0xbb);
		outStream.write(0xcc);
		outStream.write(0xdd);

		outStream.flush();
		outStream.close();

		// open decrypting input stream and compare values

		AESCBCRandomAccessFile aesfile = AESCBCRandomAccessFile.open(testFile,
				false);
		aesfile.initWithShareKey(testKey);
		EncRandomAccessInputStream inStream = new EncRandomAccessInputStream(
				aesfile);

		for (i = 0; i <= Byte.MAX_VALUE; i++) {
			assertEquals(i, inStream.read());
		}

		buf = new byte[CHUNKSIZE / 2 + 11];
		byte[] ref = new byte[CHUNKSIZE / 2 + 11];
		i = 0x41;
		while (i < 0x61) {
			Arrays.fill(ref, (byte) i++);
			inStream.read(buf);
			assertArrayEquals(ref, buf);
		}

		buf = new byte[CHUNKSIZE * 3 + 11];
		ref = new byte[CHUNKSIZE * 3 + 11];

		while (i < 0x71) {
			Arrays.fill(ref, (byte) i++);
			inStream.read(buf);
			assertArrayEquals(ref, buf);
		}

		assertEquals(0xaa, inStream.read());
		assertEquals(0xbb, inStream.read());
		assertEquals(0xcc, inStream.read());
		assertEquals(0xdd, inStream.read());
		assertEquals(-1, inStream.read());
		inStream.close();
	}

	@Test
	public void testGCMCompatImpl() throws Exception {
		File testFile = tmpTestDir.newFile("gcmcompatimplTest");

		final SecretKey testKey;
		final SecureRandomWrapper wrapper = SecureRandomWrapper.getInstance();
		byte[] tmp = new byte[BLOCKSIZE];
		wrapper.nextBytes(tmp);
		testKey = new SecretKeySpec(tmp, "AES");

		// open new encrypting output stream
		EncRandomAccessOutputStream outStream = new EncRandomAccessOutputStream(
				AESGCMRandomAccessFileCompat.create(0, testKey, testFile));

		// write single bytes, partial and full chunks

		for (int i = 0; i <= Byte.MAX_VALUE; i++) {
			outStream.write(i);
		}

		byte[] buf = new byte[CHUNKSIZE / 2 + 11];
		int i = 0x41;
		while (i < 0x61) {
			Arrays.fill(buf, (byte) i++);
			outStream.write(buf);
		}

		buf = new byte[CHUNKSIZE * 3 + 11];

		while (i < 0x71) {
			Arrays.fill(buf, (byte) i++);
			outStream.write(buf);
		}

		outStream.write(0xaa);
		outStream.write(0xbb);
		outStream.write(0xcc);
		outStream.write(0xdd);

		outStream.flush();
		outStream.close();

		// open decrypting input stream and compare values
		AESGCMRandomAccessFileCompat aesfile = AESGCMRandomAccessFileCompat.open(testFile,
				false);
		aesfile.initWithShareKey(testKey);
		assertTrue(aesfile.checkFileAuthenticationTag());
		EncRandomAccessInputStream inStream = new EncRandomAccessInputStream(
				aesfile);

		for (i = 0; i <= Byte.MAX_VALUE; i++) {
			assertEquals(i, inStream.read());
		}

		buf = new byte[CHUNKSIZE / 2 + 11];
		byte[] ref = new byte[CHUNKSIZE / 2 + 11];
		i = 0x41;
		while (i < 0x61) {
			Arrays.fill(ref, (byte) i++);
			inStream.read(buf);
			assertArrayEquals(ref, buf);
		}

		buf = new byte[CHUNKSIZE * 3 + 11];
		ref = new byte[CHUNKSIZE * 3 + 11];

		while (i < 0x71) {
			Arrays.fill(ref, (byte) i++);
			inStream.read(buf);
			assertArrayEquals(ref, buf);
		}

		assertEquals(0xaa, inStream.read());
		assertEquals(0xbb, inStream.read());
		assertEquals(0xcc, inStream.read());
		assertEquals(0xdd, inStream.read());
		assertEquals(-1, inStream.read());
		inStream.close();
	}
	
	@Test
	public void testGCMImpl() throws Exception {
		File testFile = tmpTestDir.newFile("gcmimplTest");

		final SecretKey testKey;
		final SecureRandomWrapper wrapper = SecureRandomWrapper.getInstance();
		byte[] tmp = new byte[BLOCKSIZE];
		wrapper.nextBytes(tmp);
		testKey = new SecretKeySpec(tmp, "AES");

		// open new encrypting output stream
		EncRandomAccessOutputStream outStream = new EncRandomAccessOutputStream(
				AESGCMRandomAccessFile.create(0, testKey, testFile));

		// write single bytes, partial and full chunks

		for (int i = 0; i <= Byte.MAX_VALUE; i++) {
			outStream.write(i);
		}

		byte[] buf = new byte[CHUNKSIZE / 2 + 11];
		int i = 0x41;
		while (i < 0x61) {
			Arrays.fill(buf, (byte) i++);
			outStream.write(buf);
		}

		buf = new byte[CHUNKSIZE * 3 + 11];

		while (i < 0x71) {
			Arrays.fill(buf, (byte) i++);
			outStream.write(buf);
		}

		outStream.write(0xaa);
		outStream.write(0xbb);
		outStream.write(0xcc);
		outStream.write(0xdd);

		outStream.flush();
		outStream.close();

		// open decrypting input stream and compare values

		AESGCMRandomAccessFile aesfile = AESGCMRandomAccessFile.open(testFile,
				false);
		aesfile.initWithShareKey(testKey);
		assertTrue(aesfile.checkFileAuthenticationTag());
		EncRandomAccessInputStream inStream = new EncRandomAccessInputStream(
				aesfile);

		for (i = 0; i <= Byte.MAX_VALUE; i++) {
			assertEquals(i, inStream.read());
		}

		buf = new byte[CHUNKSIZE / 2 + 11];
		byte[] ref = new byte[CHUNKSIZE / 2 + 11];
		i = 0x41;
		while (i < 0x61) {
			Arrays.fill(ref, (byte) i++);
			inStream.read(buf);
			assertArrayEquals(ref, buf);
		}

		buf = new byte[CHUNKSIZE * 3 + 11];
		ref = new byte[CHUNKSIZE * 3 + 11];

		while (i < 0x71) {
			Arrays.fill(ref, (byte) i++);
			inStream.read(buf);
			assertArrayEquals(ref, buf);
		}

		assertEquals(0xaa, inStream.read());
		assertEquals(0xbb, inStream.read());
		assertEquals(0xcc, inStream.read());
		assertEquals(0xdd, inStream.read());
		assertEquals(-1, inStream.read());
		inStream.close();
	}

	final String sampleFileName = "ubuntu-12.04.4-desktop-i386.iso";

	//@Test
	public void testBufferedStreamSample() throws Exception {
		final SecretKey testKey;
		final SecureRandomWrapper wrapper = SecureRandomWrapper.getInstance();
		byte[] tmp = new byte[BLOCKSIZE];
		wrapper.nextBytes(tmp);
		testKey = new SecretKeySpec(tmp, "AES");
		FileInputStream fis = new FileInputStream(new File(sampleFileName));

		File testFile = new File(sampleFileName + ".gcm-enc");

		BufferedInputStream in = new BufferedInputStream(fis);

		// open new encrypting output stream
		EncRandomAccessOutputStream outStream = new EncRandomAccessOutputStream(
				AESGCMRandomAccessFile.create(0, testKey, testFile));

		BufferedOutputStream out = new BufferedOutputStream(outStream,
				CHUNKSIZE);

		int i = 0;
		// sizeof(buf) is largely irrelevant for performance
		byte[] buf = new byte[100];
		long time = System.currentTimeMillis();
		while ((i = fis.read(buf)) != -1) {
			out.write(buf, 0, i);
		}
		out.flush();
		out.close();
		in.close();
		System.out
				.println("enc: " + (System.currentTimeMillis() - time) / 1000);

		AESGCMRandomAccessFile aesFile = AESGCMRandomAccessFile.open(testFile,
				false);
		aesFile.initWithShareKey(testKey);
		assertTrue(aesFile.checkFileAuthenticationTag());
		in = new BufferedInputStream(new EncRandomAccessInputStream(aesFile));

		File testFileDec = new File(sampleFileName + ".gcm-dec");
		out = new BufferedOutputStream(new FileOutputStream(testFileDec));
		time = System.currentTimeMillis();
		while ((i = in.read(buf)) != -1) {
			out.write(buf, 0, i);
		}

		in.close();
		out.flush();
		out.close();
		System.out
				.println("dec: " + (System.currentTimeMillis() - time) / 1000);
	}
}
