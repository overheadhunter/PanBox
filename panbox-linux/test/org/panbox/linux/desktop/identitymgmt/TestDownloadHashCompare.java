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
package org.panbox.linux.desktop.identitymgmt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import ezvcard.util.org.apache.commons.codec.binary.Hex;
import junit.framework.TestCase;

public class TestDownloadHashCompare extends TestCase {
	
	String fileName = "tmpfile.txt";
	File tmpFile = new File(fileName);

	protected static void setUpBeforeClass() throws Exception {
	}

	protected static void tearDownAfterClass() throws Exception {
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		if(tmpFile.exists())
			tmpFile.delete();
	}

	@Deprecated
	public void testDownload() {
		
		String hexValueSha256 = "76c1000023b3f0e5003599bfff12639de4dfc8d72859e9c1a4fdf8e63ac45802";
		
		URL url = null;
		try {
			url = new URL("http://www.stefantriller.de/panboxtest.txt");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			org.apache.commons.io.FileUtils.copyURLToFile(url, tmpFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//byte[] digest = CryptCore.createFileHash(tmpFile);
		byte[] digest = createFileHash(tmpFile);
		String h = Hex.encodeHexString(digest);
		
//		System.out.println(Arrays.toString(digest));
//		System.out.println(digest.length * 8);		
//		System.out.println(h);
		
		assertEquals(hexValueSha256, h);
		
	}
	
	/**
	 * Creates a Hash value for a given file
	 * @param file
	 * @return
	 */
	private byte[] createFileHash(File file)
	{
		MessageDigest md = null;
		try (InputStream is = Files.newInputStream(Paths.get(file.getAbsolutePath()))) {
			
			md = MessageDigest.getInstance("SHA-256");
			DigestInputStream dis = new DigestInputStream(is, md);
			
			/* Read stream to EOF as normal... */
			
			while (dis.available() > 0)
			{
				dis.read();
			}

		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		byte[] digest = md.digest();
		
		//String h = Hex.encodeHexString( digest );
		
		return digest;
	}

}
