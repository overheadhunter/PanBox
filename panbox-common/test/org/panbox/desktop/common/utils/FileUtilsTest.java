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
package org.panbox.desktop.common.utils;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileUtilsTest {

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

	@Rule
	public TemporaryFolder tmpTestDir = new TemporaryFolder();

	@Test
	public void testDeleteDirectoryStructure() throws IOException {
		File testfolder = tmpTestDir.newFolder();
		String[] structure = { "a", "b", "c" };
		int len = structure.length;
		File p = null;
		for (int i = 0; i < len; i++) {
			for (int j = 0; j < len; j++) {
				for (int k = 0; k < len; k++) {
					String path = testfolder.getAbsolutePath() + File.separator
							+ structure[i] + File.separator + structure[j]
							+ File.separator + structure[k];
					p = new File(path);
					p.mkdirs();
					assertTrue(p.exists());
				}
			}
		}

		p = new File(testfolder, "test");
		p.createNewFile();
		assertTrue(p.exists());
		p = new File(testfolder, structure[0] + File.separator + structure[1]
				+ File.separator + structure[2] + File.separator + "test");
		p.createNewFile();
		assertTrue(p.exists());
		Files.createSymbolicLink(
				Paths.get(testfolder.getAbsolutePath() + File.separator
						+ "link"),
				Paths.get(testfolder.getAbsolutePath() + File.separator
						+ "test"));
		p = new File(testfolder, "link");
		assertTrue(p.exists());

		// start deletion
		FileUtils.deleteDirectoryTree(testfolder);
		assertFalse(testfolder.exists());
		assertTrue(tmpTestDir.getRoot().exists());
		assertEquals(0, tmpTestDir.getRoot().list().length);
	}
}
