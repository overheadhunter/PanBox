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
import java.io.RandomAccessFile;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import org.junit.Test;
import org.panbox.core.exception.FileEncryptionException;
import org.panbox.core.exception.FileIntegrityException;

/**
 * @author palige
 * 
 */
public class AESGCMRandomAccessFileTest extends EncRandomAccessFileTest {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.panbox.core.crypto.io.EncRandomAccessFileTest#newEncRAFInstance(javax
	 * .crypto.SecretKey, java.io.File, java.lang.String)
	 */
	@Override
	protected EncRandomAccessFile createEncRAFInstance(SecretKey testKey,
			File testFile) throws FileEncryptionException, IOException {
		return AESGCMRandomAccessFile.create(0, testKey, testFile);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.panbox.core.crypto.io.EncRandomAccessFileTest#getBlockSize()
	 */
	@Override
	int getBlockSize() throws NoSuchAlgorithmException, NoSuchPaddingException,
			NoSuchProviderException {
		return Cipher.getInstance(AESGCMRandomAccessFile.CIPHER_CHUNK)
				.getBlockSize();
	}

	/**
	 * tests if integrity checks work as expected in case of modification of an
	 * arbitrary byte within the file
	 * 
	 * @throws Exception
	 */
	@Test
	public void testIntegrityArbitraryModification() throws Exception {
		File testFile = tmpTestDir.newFile("arbitrarymodificationTest");
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

		// modify a single byte within one encrypted chunk
		RandomAccessFile randomAccessFile = new RandomAccessFile(testFile, "rw");
		int modification_idx = 7;
		long pos = aesTestFile.CHUNK_ENC_SIZE * modification_idx
				+ aesTestFile.CHUNK_IV_SIZE + 1234;
		randomAccessFile.seek(pos);
		randomAccessFile.write(0x41);

		randomAccessFile.close();

		// Opening the file will succeed, as no auth tag itself has been
		// modified and the file authentication tag itself stays valid. However,
		// any subsequent read call on the arbitrarily modified block will cause
		// a bad mac exception due to using an AE mode#
		aesTestFile = openEncRAFInstance(testFile, false);
		assertNotEquals(-1, aesTestFile.getShareKeyVersion());
		aesTestFile.initWithShareKey(testKey);

		// reset values
		i = 0;
		while (i < modification_idx) {
			i++;
			aesTestFile.read(buf);
		}

		try {
			aesTestFile.read(buf);
			fail("failed detection of chunk modification");
		} catch (FileIntegrityException e) {
			assertTrue(e.getMessage().contains("integrity violation"));
		} finally {
			aesTestFile.close();
		}
	}

	/**
	 * tests if integrity checks work as expected in case of manual/adversarial
	 * file truncation.
	 * 
	 * @throws Exception
	 */
	// @Test
	public void testIntegrityFileTruncationAligned() throws Exception {
		File testFile = tmpTestDir.newFile("alignedmalicioustruncationTest");
		aesTestFile = createEncRAFInstance(testKey, testFile);

		// printable ascii chars, just for testing purposes
		int i = 0x41;
		byte[] buf = new byte[aesTestFile.CHUNK_DATA_SIZE];
		while (i < 0x61) {
			Arrays.fill(buf, (byte) i++);
			aesTestFile.write(buf);
		}
		// aesTestFile.close();

		// truncate file at multiple of encrypted chunk length
		RandomAccessFile randomAccessFile = new RandomAccessFile(testFile, "rw");
		int nchunks = 17;

		randomAccessFile.setLength(nchunks * aesTestFile.CHUNK_ENC_SIZE);
		randomAccessFile.close();

		// reset values
		aesTestFile.seek(0);
		i = 1;
		while (i < nchunks) {
			i++;
			aesTestFile.read(buf);
		}

		try {
			aesTestFile.read(buf);
			fail("failed detection of chunk modification");
		} catch (FileEncryptionException e) {
			assertEquals("mac check in GCM failed", e.getMessage());
		} finally {
			aesTestFile.close();
		}

		try {
			// check if decryption now throws a bad MAC exception
			aesTestFile = openEncRAFInstance(testFile, false);
			assertNotEquals(-1, aesTestFile.getShareKeyVersion());
			aesTestFile.initWithShareKey(testKey);
		} catch (FileIntegrityException e) {
			assertEquals("File authentication tag verification failed!",
					e.getMessage());
		} finally {
			if (aesTestFile != null) {
				aesTestFile.close();
			}
		}

	}

	/**
	 * complementary test checking if integrity checks work as expected in case
	 * of manual/adversarial file truncation at a random offset in the file.
	 * NOTE: neither chunk index nor last chunk flag are needed as AAD here, as
	 * random file truncation should also damage the authentication tag in most
	 * cases.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testIntegrityFileTruncationMisaligned() throws Exception {
		File testFile = tmpTestDir.newFile("misalignedmalicioustruncationTest");
		aesTestFile = createEncRAFInstance(testKey, testFile);

		// printable ascii chars, just for testing purposes
		int i = 0x41;
		byte[] buf = new byte[aesTestFile.CHUNK_DATA_SIZE];
		while (i < 0x61) {
			Arrays.fill(buf, (byte) i++);
			aesTestFile.write(buf);
		}
		aesTestFile.flush();
		// aesTestFile.close();

		// truncate file at random location in file
		RandomAccessFile randomAccessFile = new RandomAccessFile(testFile, "rw");

		long offset = (long) (Math.random() * randomAccessFile.length());

		randomAccessFile.setLength(offset);
		randomAccessFile.close();

		/**
		 * NOTE: The subsequent test has been disabled for now as this kind of
		 * modification will already be detected upon opening the file due to
		 * the file authentication tag
		 */

		// reset values
		aesTestFile.seek(0);
		int tmp = (int) offset / aesTestFile.CHUNK_ENC_SIZE;
		i = 0;
		while (i < tmp) {
			i++;
			aesTestFile.read(buf);
		}

		try {
			aesTestFile.read(buf);
			fail("failed detection of chunk modification");
		} catch (FileIntegrityException e) {
			assertTrue(e.getMessage().contains("integrity violation"));
		} finally {
			aesTestFile.close();
		}

		try {
			// check if decryption now throws a bad MAC exception
			aesTestFile = openEncRAFInstance(testFile, false);
			assertNotEquals(-1, aesTestFile.getShareKeyVersion());
			aesTestFile.initWithShareKey(testKey);
		} catch (FileIntegrityException e) {
			assertEquals("File authentication tag verification failed!",
					e.getMessage());
		} finally {
			if (aesTestFile != null) {
				aesTestFile.close();
			}
		}
	}

	/**
	 * tests if integrity checks work as expected in case of manual swapping of
	 * chunks
	 * 
	 * @throws Exception
	 */
	@Test
	public void testIntegrityChunkSwap() throws Exception {
		File testFile = tmpTestDir.newFile("integritychunkswapTest");
		aesTestFile = createEncRAFInstance(testKey, testFile);

		// printable ascii chars, just for testing purposes
		int i = 0x41;
		byte[] buf = new byte[aesTestFile.CHUNK_DATA_SIZE];
		while (i < 0x61) {
			Arrays.fill(buf, (byte) i++);
			aesTestFile.write(buf);
		}
		aesTestFile.flush();

		// write 17th to 9th chunk
		RandomAccessFile randomAccessFile = new RandomAccessFile(testFile, "rw");

		byte[] buf2 = new byte[aesTestFile.CHUNK_ENC_SIZE];

		randomAccessFile.seek(aesTestFile.CHUNK_ENC_SIZE * 13);
		randomAccessFile.read(buf2);

		randomAccessFile.seek(aesTestFile.CHUNK_ENC_SIZE * 9);
		randomAccessFile.write(buf2);

		randomAccessFile.close();

		aesTestFile.seek(0);

		i = 1;
		while (i < 9) {
			i++;
			aesTestFile.read(buf);
		}

		try {
			aesTestFile.read(buf);
			fail("failed detection of chunk modification");
		} catch (FileIntegrityException e) {
			assertTrue(e.getMessage().contains("integrity violation"));
		} finally {
			aesTestFile.close();
		}

		try {
			// check if decryption now throws a bad MAC exception
			aesTestFile = openEncRAFInstance(testFile, false);
			assertNotEquals(-1, aesTestFile.getShareKeyVersion());
			aesTestFile.initWithShareKey(testKey);
		} catch (FileIntegrityException e) {
			assertEquals("File authentication tag verification failed!",
					e.getMessage());
		} finally {
			if (aesTestFile != null) {
				aesTestFile.close();
			}
		}
	}

	/**
	 * tests authentication tag tree management
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAuthTagVerification() throws Exception {
		File testFile = tmpTestDir.newFile("authtagverifierTest");
		aesTestFile = createEncRAFInstance(testKey, testFile);

		assertNull(aesTestFile.readFileAuthenticationTag());
		// printable ascii chars, just for testing purposes
		int i = 0x41;
		byte[] buf = new byte[aesTestFile.CHUNK_DATA_SIZE];
		while (i < 0x61) {
			Arrays.fill(buf, (byte) i++);
			aesTestFile.write(buf);
		}
		assertNull(aesTestFile.readFileAuthenticationTag());
		aesTestFile.flush();
		assertNotNull(aesTestFile.readFileAuthenticationTag());
		assertTrue(aesTestFile.checkFileAuthenticationTag());
		int header_size = aesTestFile.fHeader.headerSize();
		aesTestFile.close();

		RandomAccessFile raf = new RandomAccessFile(testFile, "r");
		raf.seek(header_size + aesTestFile.CHUNK_ENC_SIZE * 0x11);
		// store one complete encrypted chunk including its auth tag
		byte[] tmpChunk = new byte[aesTestFile.CHUNK_ENC_SIZE];
		raf.read(tmpChunk);
		raf.close();

		// now check handling of existing file
		AbstractAESGCMRandomAccessFile gcmTestFile = (AbstractAESGCMRandomAccessFile) openEncRAFInstance(
				testFile, true);
		assertNotEquals(-1, gcmTestFile.getShareKeyVersion());
		gcmTestFile.initWithShareKey(testKey);
		byte[] lastFileAuthTag, tmp = gcmTestFile.readFileAuthenticationTag();
		assertNotNull(tmp);

		lastFileAuthTag = Arrays.copyOf(tmp, tmp.length);
		// create a new version by overwriting data in second half of file
		gcmTestFile.seek(gcmTestFile.CHUNK_DATA_SIZE * 0x10);
		i = 0x71;
		while (i < 0x81) {
			Arrays.fill(buf, (byte) i++);
			gcmTestFile.write(buf);
			// should alwasy return true as no invalid changes have been made so
			// far
			assertTrue(gcmTestFile.checkFileAuthenticationTag());
			tmp = gcmTestFile.readFileAuthenticationTag();
			// file auth tag should be updated in each iteration
			assertFalse(Arrays.equals(tmp, lastFileAuthTag));
			lastFileAuthTag = Arrays.copyOf(tmp, tmp.length);
		}

		// restore previously cached old chunk (i.e. chunk from previous
		// "version" of this file)
		raf = new RandomAccessFile(testFile, "rw");
		raf.seek(header_size + aesTestFile.CHUNK_ENC_SIZE * 0x11);
		raf.write(tmpChunk);
		raf.close();

		// verification should fail due to chunk modification
		assertFalse(gcmTestFile.checkFileAuthenticationTag());

		// while writing to *another* chunk should work, the file
		// authentication tag should NEVER be updated with the maliciously
		// modified
		// chunk
		Arrays.fill(buf, (byte) 0xFF);
		gcmTestFile.seek(gcmTestFile.CHUNK_DATA_SIZE * 0x5);
		gcmTestFile.write(buf);
		gcmTestFile.flush();

		// verification should still fail due to chunk modification
		assertFalse(gcmTestFile.checkFileAuthenticationTag());

		// read call to maliciously restored old chunk should also fail
		gcmTestFile.seek(gcmTestFile.CHUNK_DATA_SIZE * 0x11);
		try {
			// read call should fail as chunk has been replaced by an old chunk.
			// Note: GCM decryption should still work as we copied an old,
			// *valid* chunk from the same position in the same file. However,
			// the file/chunk auth tag comparison should indicate that the chunk
			// auth tag deviates from its expected value
			gcmTestFile.read(buf);
			fail("Exception due to auth tag modification expected!");
		} catch (FileIntegrityException e) {
			assertEquals(
					"File authentication tag verification failed in chunk " + 0x11,
					e.getMessage());
		}

		// overwrite data in second half of file
		gcmTestFile.seek(gcmTestFile.CHUNK_DATA_SIZE * 0x11);
		try {
			// partial write call should also fail as this would involve merging
			// with a chunk has been replaced by an old chunk.
			gcmTestFile.write(new byte[] { 0x00, 0x00, 0x00, 0x00 });
			fail("Exception due to auth tag modification expected!");
		} catch (FileIntegrityException e) {
			assertEquals(
					"File authentication tag verification failed in chunk " + 0x11,
					e.getMessage());
		}

		// gcmTestFile.seek(gcmTestFile.CHUNK_DATA_SIZE * 0x11);
		// // however, full write call should work, as modified
		// // chunk will be replaced completely
		// gcmTestFile.write(buf);
		// gcmTestFile.flush();
		// gcmTestFile.close();

		try {
			// check if decryption now throws a bad MAC exception if we try to
			// open a new file
			gcmTestFile = (AbstractAESGCMRandomAccessFile) openEncRAFInstance(testFile,
					false);
			assertNotEquals(-1, gcmTestFile.getShareKeyVersion());
			gcmTestFile.initWithShareKey(testKey);
		} catch (FileIntegrityException e) {
			assertEquals("File authentication tag verification failed!",
					e.getMessage());
		} finally {
			if (gcmTestFile != null) {
				gcmTestFile.close();
			}
		}
	}

	@Override
	protected EncRandomAccessFile openEncRAFInstance(File testFile,
			boolean writable) throws FileEncryptionException, IOException {
		return AESGCMRandomAccessFile.open(testFile, writable);
	}

	@Override
	protected EncRandomAccessFile getInstance(File testfile, boolean writeable)
			throws FileEncryptionException, IOException {
		return AESGCMRandomAccessFile.getInstance(testfile, writeable);
	}

	@Override
	protected void clean() {
		AESGCMRandomAccessFile.instanceMap.clear();
	}
}
