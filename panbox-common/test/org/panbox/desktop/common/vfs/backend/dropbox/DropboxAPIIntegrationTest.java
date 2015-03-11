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
package org.panbox.desktop.common.vfs.backend.dropbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.Date;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.dropbox.core.DbxClient;

public class DropboxAPIIntegrationTest {
	private static DropboxAPIIntegration dbApi;
	private static DbxClient client;

	// temporary testing folder
	public static TemporaryFolder tmpTestDir;

	// folder names
	private static final String remoteRoot = "/testing";
	private static final String testFolder1 = remoteRoot + "/" + "TestFolder1";
	private static final String testFolder2 = remoteRoot + "/" + "TestFolder2";
	private static final String insideTestFolder2 = testFolder2 + "/"
			+ "InsideTestFolder2";
	private static final String insideInsideTestFolder2 = insideTestFolder2
			+ "/" + "InsideInsideTestFolder2";
	private static final String testFolder3 = remoteRoot + "/" + "TestFolder3";

	// test file strings
	private static final String fileName = "hello.txt";
	private static final String fileContent = "Hello World!";

	private static File testFile;

	// access token for dropboxfritz@gmail.com account
	private static String ACCESS_TOKEN = "d1m6UQOJwfUAAAAAAAAAW-AkB1l1a-uQQVog8KFqTe45YAMYzWlSswLy8IXbdrlW";

	@BeforeClass
	public static void setUp() throws Exception {
		Constructor<?>[] cons = DropboxAPIIntegration.class
				.getDeclaredConstructors();
		for (int i = 0; i < cons.length; i++) {
			Constructor con = cons[i];			
			Class[] par = con.getParameterTypes();
			
			if (par.length == 1 && par[0].equals(String.class)) {
				con.setAccessible(true);
				dbApi = (DropboxAPIIntegration) con.newInstance(ACCESS_TOKEN);
				break;
			}
		}

		assertNotNull(dbApi);

		client = dbApi.client;

		tmpTestDir = new TemporaryFolder();
		tmpTestDir.create();
		testFile = tmpTestDir.newFile(fileName);
		PrintWriter out = new PrintWriter(testFile);
		out.write(fileContent);
		out.close();

		// create test folders on db server
		client.createFolder(remoteRoot);
		client.createFolder(testFolder1);
		client.createFolder(testFolder2);
		client.createFolder(testFolder3);
		client.createFolder(insideTestFolder2);
		client.createFolder(insideInsideTestFolder2);

		// upload some test tiles to db server
		dbApi.uploadFile(remoteRoot + "/" + fileName, testFile);
		dbApi.uploadFile(testFolder1 + "/" + fileName, testFile);
		dbApi.uploadFile(testFolder2 + "/" + fileName, testFile);
		dbApi.uploadFile(insideInsideTestFolder2 + "/" + fileName, testFile);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		// delete the created files from db server
		client.delete(remoteRoot);
	}

	@Test
	public void testDownloadFile() throws Exception {
		// download test file from db
		File targetFolder = tmpTestDir.newFolder("testDownloadFile");
		dbApi.downloadFile(remoteRoot + "/" + fileName,
				targetFolder.getAbsolutePath() + File.separator + "hello.txt");

		// test if file exists and content matches
		assertTrue(new File(targetFolder.getAbsolutePath(), fileName).exists());
		assertEquals(fileContent + "\n",
				readFile(targetFolder.getAbsolutePath() + File.separator
						+ fileName));
	}

	@Test
	public void testDownloadFolder() throws Exception {
		String remotePath = "/testing";
		File targetFolder = tmpTestDir.newFolder("target");
		String targetPath = targetFolder.getAbsolutePath();
		dbApi.downloadFolder(remotePath, targetPath);

		// test folders for existence
		assertTrue((new File(targetPath + remoteRoot).exists()));
		assertTrue((new File(targetPath + testFolder1).exists()));
		assertTrue((new File(targetPath + testFolder2).exists()));
		assertTrue((new File(targetPath + testFolder3).exists()));
		assertTrue((new File(targetPath + insideTestFolder2).exists()));
		assertTrue((new File(targetPath + insideInsideTestFolder2).exists()));

		// test files for existence and content
		assertTrue(new File(targetPath + remoteRoot, fileName).exists());
		assertEquals(fileContent + "\n", readFile(targetPath + remoteRoot
				+ File.separator + fileName));
		assertTrue(new File(targetPath + testFolder1, fileName).exists());
		assertEquals(fileContent + "\n", readFile(targetPath + testFolder1
				+ File.separator + fileName));
		assertTrue(new File(targetPath + testFolder2, fileName).exists());
		assertEquals(fileContent + "\n", readFile(targetPath + testFolder2
				+ File.separator + fileName));
		assertTrue(new File(targetPath + insideInsideTestFolder2, fileName)
				.exists());
		assertEquals(fileContent + "\n", readFile(targetPath
				+ insideInsideTestFolder2 + File.separator + fileName));
	}

	/**
	 * Helper method to read the contents of the testfile
	 *
	 * @param filePath
	 *            path to the file
	 * @return Content of the file as a String
	 * @throws IOException
	 */
	private String readFile(String filePath) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		try {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();
			while (line != null) {
				sb.append(line);
				sb.append("\n");
				line = br.readLine();
			}
			return sb.toString();
		} finally {
			br.close();
		}
	}

	@Test
	public void testExists() throws Exception {
		String filePath = remoteRoot + "/" + fileName;

		assertTrue(dbApi.exists(filePath));
		assertEquals(fileName, client.getMetadata(filePath).name);
		assertFalse(dbApi.exists("/asdjalsdlasdjal"));
	}

	@Test
	public void testUploadAndDeleteFile() throws Exception {
		String remotePath = "/testing/hello/" + fileName;

		dbApi.uploadFile(remotePath, testFile);
		assertTrue(dbApi.exists(remotePath));

		dbApi.deleteFile(remotePath);
		assertFalse(dbApi.exists(remotePath));
	}

	@Test
	public void testGetServerTime() throws Exception {
		// system and server time should not be more than 10 seconds apart
		// check this 10 times
		for (int i = 0; i < 10; i++) {
			Date systemDate = new Date();
			Date serverDate = dbApi.getServerTime();
			assertTrue(serverDate.compareTo(systemDate) < 10);
		}
	}

	@Test
	public void testLock() throws Exception {
		String filePath = remoteRoot + "/" + fileName;

		dbApi.createLock(filePath);
		assertTrue(dbApi.exists(filePath + DropboxConstants.LOCK_SUFFIX));
		assertTrue(dbApi.isLocked(filePath));

		dbApi.releaseLock(filePath);
		assertFalse(dbApi.exists(filePath + DropboxConstants.LOCK_SUFFIX));
		assertFalse(dbApi.isLocked(filePath));
	}

	@Test
	public void testTemporaryLock() throws Exception {
		String filePath = remoteRoot + "/" + fileName;

		dbApi.createTemporaryLock(filePath);
		assertTrue(dbApi.exists(filePath + DropboxConstants.TEMP_LOCK_SUFFIX));
		assertTrue(dbApi.isLocked(filePath));

		dbApi.releaseTemporaryLock(filePath);
		assertFalse(dbApi.exists(filePath + DropboxConstants.TEMP_LOCK_SUFFIX));
		assertFalse(dbApi.isLocked(filePath));
	}
}