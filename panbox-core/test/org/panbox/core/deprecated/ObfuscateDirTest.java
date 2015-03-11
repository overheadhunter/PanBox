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
package org.panbox.core.deprecated;

import static org.junit.Assert.fail;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.panbox.core.tests.AbstractTest;

public class ObfuscateDirTest extends AbstractTest {

	protected byte[] aesKey;
	protected byte[] iv;

	@Before
	public void setUp() throws Exception {

		aesKey = generateAESKey();
		iv = generateIV();

		// clean test directories
		File dir = new File(obfuscationDir);
		if (dir.exists()) {
			deleteDirectory(dir);
		}
		if (!dir.mkdir()) {
			fail("Can't create " + obfuscationDir);
		}

		dir = new File(plainDir);
		if (dir.exists()) {
			deleteDirectory(dir);
		}
		if (!dir.mkdir()) {
			fail("Can't create " + plainDir);
		}
		File file = new File(
				dir.getAbsolutePath()
						+ File.separator
						+ "Dieser Dateiname muesste jetzt wirklich die maximale Lange bei meinem Pfad unter Windoof ausnutzen da er 146 Zeichen besitzt und dder Pfad 29.txt");
		if (!file.createNewFile()) {
			fail("Can't create test file.");
		}
		file = new File(
				dir.getAbsolutePath()
						+ File.separator
						+ "DiesisteinSehrLanger DateiName zum TestderObfuskierung1234.txt");
		if (!file.createNewFile()) {
			fail("Can't create test file.");
		}
		file = new File(
				dir.getAbsolutePath()
						+ File.separator
						+ "Maximale DateinamenLaenge die Bei uns 175 zeichen lang sein muesste. Dies ist eine ganz schoen grosse Zahl weil soviele Zeichen gibt doch keiner ein fuer einen Dateinamen.txt");
		if (!file.createNewFile()) {
			fail("Can't create test file.");
		}
		file = new File(
				dir.getAbsolutePath()
						+ File.separator
						+ "Noch ein Dateiname mit Umlauten öhm, hier noch welche äüßöÖÄÜ... Mal schauen ob es geht.txt");
		if (!file.createNewFile()) {
			fail("Can't create test file.");
		}
		file = new File(
				dir.getAbsolutePath()
						+ File.separator
						+ "Noch ein Test mit einem Dateinamen von 109 Zeichen die mit Pfad noch in meinem Windows verarbeitet wirdd.txt");
		if (!file.createNewFile()) {
			fail("Can't create test file.");
		}
		dir = new File(dir.getAbsolutePath() + File.separator + "directoryTest");
		if (!dir.mkdir()) {
			fail("Can't create test directory.");
		}
		file = new File(dir.getAbsolutePath() + File.separator
				+ "DateiImUnterverzeichnis.txt");
		if (!file.createNewFile()) {
			fail("Can't create test file.");
		}

		dir = new File(deobfuscationDir);
		if (dir.exists()) {
			deleteDirectory(dir);
		}
		if (!dir.mkdir()) {
			fail("Can't create " + deobfuscationDir);
		}

	}

	@After
	public void tearDown() throws Exception {

		File dir = new File(obfuscationDir);
		deleteDirectory(dir);
		dir = new File(plainDir);
		deleteDirectory(dir);
		dir = new File(deobfuscationDir);
		deleteDirectory(dir);

	}

	// TODO These are not unit tests
//	@Test
//	public void testObfuscateDir() throws InvalidKeyException,
//			NoSuchAlgorithmException, NoSuchPaddingException,
//			InvalidAlgorithmParameterException, IllegalBlockSizeException,
//			BadPaddingException, IOException {
//
//		// FileHandler.obfuscateFileNamesInDir(plainDir, obfuscationDir, aesKey,
//		// iv, true, EncodingType.BASE64);
//
//	}

//	@Test
//	public void testDeobfuscateDir() throws InvalidKeyException,
//			NoSuchAlgorithmException, NoSuchPaddingException,
//			InvalidAlgorithmParameterException, IllegalBlockSizeException,
//			BadPaddingException, IOException, DataFormatException {
//		// FileHandler.deobfuscateFileNamesInDir(obfuscationDir,
//		// deobfuscationDir, aesKey, iv, true, EncodingType.BASE64);
//
//	}
}
