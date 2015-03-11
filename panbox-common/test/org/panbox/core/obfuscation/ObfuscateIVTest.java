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

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Random;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.panbox.core.crypto.AbstractObfuscatorFactory;
import org.panbox.core.crypto.FileObfuscatorFactory;
import org.panbox.core.crypto.Obfuscator;
import org.panbox.core.exception.ObfuscationException;
import org.panbox.core.tests.AbstractTest;

public class ObfuscateIVTest extends AbstractTest {

	private final int numFiles = 1000;

	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();

	@Rule
	public TemporaryFolder testFolderPlain = new TemporaryFolder();

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
	public void test() throws IOException, ObfuscationException {

		 String realFileTestPath = "/home/triller/pbTest";
//		String realFileTestPath = testFolder.getRoot().getAbsolutePath();

		System.out.println("Using temp directory: " + realFileTestPath);

		// System.in.read();
		// System.out.println("starting");

		AbstractObfuscatorFactory obFac = null;
		try {
			obFac = AbstractObfuscatorFactory
					.getFactory(FileObfuscatorFactory.class);
		} catch (ClassNotFoundException | InstantiationException
				| IllegalAccessException e1) {
			e1.printStackTrace();
		}
		Obfuscator ob = obFac.getInstance(realFileTestPath, "myShareName");

		// SecureRandom random = new SecureRandom();

		// LinkedList<String> filenames = new LinkedList<String>();
		// for(int i=0; i<30000; i++)
		// {
		// String s = generateString(random, 20);
		// filenames.add(s);
		// }

		LinkedList<String> filenames = getFileNamesFromFile(new File(
				"test/wortliste-deutsch.txt"), numFiles);

		SecretKey key = new SecretKeySpec(generateAESKey(), "AES");

		// long start = System.currentTimeMillis();
		for (String s : filenames) {
			String obFileName = ob.obfuscate(s, key, true);

			String deOb = ob.deObfuscate(obFileName, key);

			// System.out.println(s + " "+ deOb);
			assertEquals(s, deOb);

			// System.out.println("Obfuscate filename: " + obFileName);
			try {
				// testFolder.newFile(obFileName);
				new File(realFileTestPath + File.separator + obFileName)
						.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// long stop = System.currentTimeMillis();
		// System.out.println("Finished obfuscation of "+filenames.size()+" elements in "
		// + (stop-start) + "ms");

		// test deobfuscate with cache (only one fetch of IV Pool)

		// start = System.currentTimeMillis();
		// File files = new File(testFolder.getRoot().getAbsolutePath());
		File files = new File(realFileTestPath);

		// System.out.println(filenames.size());

		int match = 0;
		for (File f : files.listFiles()) {
			if (f.isDirectory()) {
				continue;
			}

			String deObfuscatedName = ob.deObfuscate(f.getName(), key);

			if (filenames.contains(deObfuscatedName)) {
				match++;
			}

			// System.out.println("Deobfuscate filename: " + deObfuscatedName);

			// File fp = new File(shareDirPlain + File.separator +
			// deObfuscatedName);
			try {
				testFolderPlain.newFile(deObfuscatedName);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		// stop = System.currentTimeMillis();
		// System.out.println("Finished deobfuscation of "+filenames.size()+" elements in "
		// + (stop-start) + "ms");

		// System.out.println("matches: " + match);
		assertEquals(numFiles, match);

	}

	public static String generateString(Random rng, int length) {
		StringBuilder tmp = new StringBuilder();
		for (char ch = '0'; ch <= '9'; ++ch)
			tmp.append(ch);
		for (char ch = 'a'; ch <= 'z'; ++ch)
			tmp.append(ch);
		String symbols = tmp.toString();

		char[] text = new char[length];
		for (int i = 0; i < length; i++) {
			text[i] = symbols.charAt(rng.nextInt(symbols.length()));
		}
		return new String(text);
	}

	public static LinkedList<String> getFileNamesFromFile(File file, int count) {
		LinkedList<String> filenames = new LinkedList<String>();

		FileReader fr = null;
		try {
			fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String s = "";
			int c = 0;
			while ((s = br.readLine()) != null) {
				if (c == count) {
					break;
				}
				filenames.add(s);
				c++;
			}

			br.close();
			fr.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return filenames;

	}

}
