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
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import org.junit.Test;
import org.panbox.core.crypto.KeyConstants;
import org.panbox.core.exception.FileEncryptionException;

public class AESGCMRandomAcessFileCompatTest extends AESGCMRandomAccessFileTest {

	@Override
	protected EncRandomAccessFile createEncRAFInstance(SecretKey testKey,
			File testFile) throws FileEncryptionException, IOException {

		return AESGCMRandomAccessFileCompat.create(0, testKey, testFile);
	}

	@Override
	int getBlockSize() throws NoSuchAlgorithmException, NoSuchPaddingException,
			NoSuchProviderException {
		return Cipher.getInstance(AESGCMRandomAccessFileCompat.CIPHER_CHUNK,
				KeyConstants.PROV_BC).getBlockSize();
	}

	@Override
	protected EncRandomAccessFile openEncRAFInstance(File testFile,
			boolean writable) throws FileEncryptionException, IOException {
		// TODO Auto-generated method stub
		return AESGCMRandomAccessFileCompat.open(testFile, writable);
	}

	@Override
	protected EncRandomAccessFile getInstance(File testfile, boolean writeable)
			throws FileEncryptionException, IOException {
		return AESGCMRandomAccessFileCompat.getInstance(testfile, writeable);
	}

	@Override
	protected void clean() {
		AESGCMRandomAccessFileCompat.instanceMap.clear();
	}

	@Test
	public void testGCMImplCompatibility() throws Exception {
		File testFile = tmpTestDir.newFile("gcmImplCompatilbilty1");
		aesTestFile = createEncRAFInstance(testKey, testFile);

		final int MAXSIZE = 5 * 1024 * 1024; // max 10MB

		byte[] TESTDATA = new byte[MAXSIZE];
		(new Random(System.nanoTime())).nextBytes(TESTDATA);

		aesTestFile.write(TESTDATA);
		aesTestFile.flush();
		aesTestFile.close();

		aesTestFile = AESGCMRandomAccessFile.open(testFile, false);
		aesTestFile.initWithShareKey(testKey);
		byte[] ref = new byte[MAXSIZE];
		aesTestFile.read(ref);
		aesTestFile.close();

		assertArrayEquals(TESTDATA, ref);

		// now test the other way
		testFile = tmpTestDir.newFile("gcmImplCompatilbilty2");
		aesTestFile = AESGCMRandomAccessFile.create(0, testKey, testFile);

		(new Random(System.nanoTime())).nextBytes(TESTDATA);

		aesTestFile.write(TESTDATA);
		aesTestFile.flush();
		aesTestFile.close();

		aesTestFile = openEncRAFInstance(testFile, false);
		aesTestFile.initWithShareKey(testKey);
		ref = new byte[MAXSIZE];
		aesTestFile.read(ref);
		aesTestFile.close();

		assertArrayEquals(TESTDATA, ref);
	}

}
