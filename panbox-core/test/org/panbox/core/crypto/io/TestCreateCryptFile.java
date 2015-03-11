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

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.panbox.PanboxConstants;
import org.panbox.core.LimitedHashMap;
import org.panbox.core.crypto.AbstractObfuscatorFactory;
import org.panbox.core.crypto.AbstractObfuscatorIVPool;
import org.panbox.core.crypto.Obfuscator;
import org.panbox.core.exception.FileEncryptionException;
import org.panbox.core.exception.FileIntegrityException;
import org.panbox.core.exception.ObfuscationException;
import org.panbox.core.exception.RandomDataGenerationException;
import org.panbox.core.tests.AbstractTest;

public class TestCreateCryptFile extends AbstractTest {

	public static final class Factory extends AbstractObfuscatorFactory {

		public Factory() {
		}

		@Override
		public Obfuscator getInstance(String sharePath, String shareName)
				throws ObfuscationException {
			Obfuscator ob = new Obfuscator(sharePath, new IVPool(), shareName);
			return ob;
		}

		@Override
		public boolean removeInstance(String sharePath)
				throws ObfuscationException {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean removeInstance(Obfuscator o) throws ObfuscationException {
			// TODO Auto-generated method stub
			return false;
		}
	}

	public static final class IVPool extends AbstractObfuscatorIVPool {

		public IVPool() {
		}

		@Override
		public void fetchIVPool(String absolutePath, String shareName) {
			File path = new File(absolutePath + File.separator
					+ Obfuscator.IV_POOL_PATH);

			LimitedHashMap<String, byte[]> ivs = new LimitedHashMap<String, byte[]>(
					PanboxConstants.OBFUSCATOR_IV_POOL_SIZE);

			for (File f : path.listFiles()) {
				if (!f.isDirectory()) {
					// ignore files, we only
					// react on directories a-z
					// and 0-9
					continue;
				}

				for (String fileName : f.list()) {
					Map.Entry<String, byte[]> e = splitFilename(fileName);

					ivs.put(e.getKey(), e.getValue());
				}

			}

			// cache it
			this.ivPool = ivs;
		}
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	TemporaryFolder folder;

	@Before
	public void setUp() throws Exception {
		folder = new TemporaryFolder();
		folder.create();
	}

	@After
	public void tearDown() throws Exception {
		folder.delete();
	}

	@Test
	public void test() throws InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidAlgorithmParameterException,
			NoSuchProviderException, IllegalBlockSizeException,
			BadPaddingException, RandomDataGenerationException,
			FileEncryptionException, IOException, ShortBufferException,
			ObfuscationException, FileIntegrityException {

		final File dir = folder.newFolder();
		dir.mkdirs();
		Obfuscator obfuscator = null;
		try {
			obfuscator = AbstractObfuscatorFactory.getFactory(Factory.class)
					.getInstance(dir.getAbsolutePath(), "myShareName");
		} catch (ClassNotFoundException | InstantiationException
				| IllegalAccessException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		byte[] keyByte = generateAESKey();
		SecretKey shareKey = new SecretKeySpec(keyByte, "AES");

		File file = folder.newFile("androidEncFile.txt");

		// AESCBCRandomAccessFile eFile = AESCBCRandomAccessFile.create(0,
		// shareKey, file);
		AESGCMRandomAccessFile eFile = AESGCMRandomAccessFile.create(0,
				shareKey, file);

		eFile.write("aaaa".getBytes());

		eFile.flush();
		eFile.close();

		String obfusName = obfuscator.obfuscate(file.getName(), shareKey, true);

		File dest = folder.newFile(obfusName);
		file.renameTo(dest);

		// eFile = AESCBCRandomAccessFile.open(new File(obfusName), false);
		eFile = AESGCMRandomAccessFile.open(dest, false);
		eFile.initWithShareKey(shareKey);
		byte[] buffer = new byte[4];
		eFile.read(buffer);

		System.out.println(Arrays.toString(buffer));
		System.out
				.println(new String(buffer, PanboxConstants.STANDARD_CHARSET));

	}

	// public void testBufferedStreamSample() throws Exception {
	//
	// final int BLOCKSIZE = 16;
	// final int CHUNKSIZE = BLOCKSIZE * EncRandomAccessFile.CHUNK_MULTIPLE;
	//
	// final SecretKey testKey;
	// final SecureRandomWrapper wrapper = SecureRandomWrapper.getInstance();
	// byte[] tmp = new byte[BLOCKSIZE];
	// wrapper.nextBytes(tmp);
	// testKey = new SecretKeySpec(tmp, "AES");
	// FileInputStream fis = new FileInputStream(new File(sampleFileName));
	//
	// File testFile = new File(sampleFileName + ".gcm-enc");
	//
	// BufferedInputStream in = new BufferedInputStream(fis);
	//
	// // open new encrypting output stream
	// EncRandomAccessOutputStream outStream = new EncRandomAccessOutputStream(
	// AESGCMRandomAccessFile.create(0, testKey, testFile));
	//
	// BufferedOutputStream out = new BufferedOutputStream(outStream,
	// CHUNKSIZE);
	//
	// int i = 0;
	// // sizeof(buf) is largely irrelevant for performance
	// byte[] buf = new byte[100];
	// long time = System.currentTimeMillis();
	// while ((i = fis.read(buf)) != -1) {
	// out.write(buf, 0, i);
	// }
	// out.flush();
	// out.close();
	// in.close();
	// System.out
	// .println("enc: " + (System.currentTimeMillis() - time) / 1000);
	//
	// AESGCMRandomAccessFile aesFile = AESGCMRandomAccessFile.open(testFile,
	// false);
	// aesFile.initWithShareKey(testKey);
	// assertTrue(aesFile.checkFileAuthenticationTag());
	// in = new BufferedInputStream(new EncRandomAccessInputStream(aesFile));
	//
	// File testFileDec = new File(sampleFileName + ".gcm-dec");
	// out = new BufferedOutputStream(new FileOutputStream(testFileDec));
	// time = System.currentTimeMillis();
	// while ((i = in.read(buf)) != -1) {
	// out.write(buf, 0, i);
	// }
	//
	// in.close();
	// out.flush();
	// out.close();
	// System.out
	// .println("dec: " + (System.currentTimeMillis() - time) / 1000);
	// }

}
