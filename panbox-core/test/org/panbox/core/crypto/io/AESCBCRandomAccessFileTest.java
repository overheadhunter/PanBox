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

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import org.junit.Test;
import org.panbox.core.exception.FileEncryptionException;

/**
 * @author palige
 * 
 */
public class AESCBCRandomAccessFileTest extends EncRandomAccessFileTest {

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
		return AESCBCRandomAccessFile.create(0, testKey, testFile);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.panbox.core.crypto.io.EncRandomAccessFileTest#getBlockSize()
	 */
	@Override
	int getBlockSize() throws NoSuchAlgorithmException, NoSuchPaddingException {
		// TODO Auto-generated method stub
		return Cipher.getInstance(AESCBCRandomAccessFile.CIPHER_CHUNK)
				.getBlockSize();
	}

	@Override
	protected EncRandomAccessFile openEncRAFInstance(File testFile,
			boolean writable) throws FileEncryptionException, IOException {
		return AESCBCRandomAccessFile.open(testFile, writable);
	}

	@Test
	public void testDummy() {
		// do nothing
	}

	@Override
	protected EncRandomAccessFile getInstance(File testfile, boolean writeable)
			throws FileEncryptionException, IOException {
		return AESCBCRandomAccessFile.getInstance(testfile, writeable);
	}

	@Override
	protected void clean() {
		AESCBCRandomAccessFile.instanceMap.clear();
	}
}
