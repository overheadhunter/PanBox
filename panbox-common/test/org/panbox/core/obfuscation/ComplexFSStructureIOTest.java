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
package org.panbox.core.obfuscation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.panbox.core.crypto.AbstractObfuscatorFactory;
import org.panbox.core.crypto.FileObfuscatorFactory;
import org.panbox.core.crypto.Obfuscator;
import org.panbox.core.crypto.io.AESGCMRandomAccessFileHW;
import org.panbox.core.crypto.io.EncRandomAccessOutputStream;
import org.panbox.core.exception.FileEncryptionException;
import org.panbox.core.exception.ObfuscationException;
import org.panbox.core.tests.AbstractTest;

public class ComplexFSStructureIOTest extends AbstractTest {

	final static File sourcedir = new File(
			"/home/palige/Downloads/linux-3.16.1/");
	// "C:\\Users\\palige\\Downloads\\linux-3.16.3");

	@Rule
	public TemporaryFolder testdir = new TemporaryFolder();

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		key = new SecretKeySpec(generateAESKey(), "AES");
		try {
			ob = AbstractObfuscatorFactory.getFactory(
					FileObfuscatorFactory.class).getInstance(
					testdir.getRoot().getAbsolutePath(), "myShareName");
		} catch (ClassNotFoundException | InstantiationException
				| IllegalAccessException e1) {
			e1.printStackTrace();
		}
	}

	@After
	public void tearDown() throws Exception {
	}

	/**
	 * obfuscates and encrypts all contents of the source directory into a
	 * temporary folder and measures the duration this takes
	 * 
	 * @throws Exception
	 */
	// @Test
	public void testStructureencryption() throws Exception {
		System.out.println("Source: " + sourcedir);
		System.out.println("Temp destination: "
				+ testdir.getRoot().getAbsolutePath());
		long time = System.currentTimeMillis();
		visitAllDirsAndFiles(sourcedir);
		System.out.println("Duration: " + (System.currentTimeMillis() - time)
				+ " ms");
	}

	static Obfuscator ob;

	static SecretKey key;

	static byte[] buf = new byte[4096];

	public void process(File f) {

		String dest = f.getAbsolutePath().replace(sourcedir.getAbsolutePath(),
				"");
		if (dest.isEmpty())
			return;

		try {
			String obFileName = ob.obfuscatePath(dest, key, true);

			if (f.isDirectory()) {
				(new File(testdir.getRoot(), obFileName)).mkdirs();
			} else if (f.isFile() && f.canRead()) {
				AESGCMRandomAccessFileHW aesfile;
				try {
					aesfile = AESGCMRandomAccessFileHW.getInstance(new File(
							testdir.getRoot(), obFileName), true);

					aesfile.create(0, key);
					EncRandomAccessOutputStream outs = new EncRandomAccessOutputStream(
							aesfile);
					FileInputStream fis = new FileInputStream(f);
					while (fis.read(buf) != -1) {
						outs.write(buf);
					}
					fis.close();
					outs.flush();
					outs.close();
				} catch (FileEncryptionException | IOException e) {
					System.err.println("Error encrypting file "
							+ f.getAbsolutePath() + "Exception: "
							+ e.getMessage());
				}
			} else {
				System.err.println("Error accessing file "
						+ f.getAbsolutePath());
			}
		} catch (ObfuscationException e) {
			System.err.println("Error obfuscating file " + f.getAbsolutePath());
		}
	}

	public void visitAllDirsAndFiles(File dir) {

		process(dir); // do something useful with the file or dir

		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				visitAllDirsAndFiles(new File(dir, children[i]));
			}
		}
	}

}
