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
package org.panbox.core.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.security.KeyPair;

import javax.crypto.SecretKey;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestSymmetricKeyEnc {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {

		try {
			SecretKey symKey = CryptCore.generateSymmetricKey();
			assertEquals(KeyConstants.SYMMETRIC_KEY_SIZE,
					symKey.getEncoded().length * Byte.SIZE);
			assertEquals(KeyConstants.SYMMETRIC_ALGORITHM,
					symKey.getAlgorithm());

			KeyPair asymKey = CryptCore.generateKeypair();
			assertEquals(KeyConstants.ASYMMETRIC_ALGORITHM, asymKey
					.getPrivate().getAlgorithm());
			assertEquals(KeyConstants.ASYMMETRIC_ALGORITHM, asymKey.getPublic()
					.getAlgorithm());
			

			byte[] cryptedSymKey = CryptCore.encryptSymmetricKey(
					symKey.getEncoded(), asymKey.getPublic());

			SecretKey decryptedSymKey = CryptCore.decryptSymmertricKey(
					cryptedSymKey, asymKey.getPrivate());
			assertArrayEquals(symKey.getEncoded(), decryptedSymKey.getEncoded());
		} catch (Exception e) {
			fail(e.getMessage());
		}

	}

}
