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
package org.panbox.core.tests;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

public abstract class AbstractTest {
	
	protected String testStr = "Hello this is a Test to zip a fairly long filename and see how that goes Hopefully this string can be compressed.txt";

	//AES/CBC/PKCS5Padding
	protected String expectedEncrypted = "ntZFTVVbKNMkVzVUMLQfSSACth9PrhMP22numiy4YP0-KinUVQ-48aXWiY4FmvU935L4JQLkW57nE2SfcrI6qGqyjCEM_WLgdCG5_DDO7PuEDvM9yscw7M-Syc5EEKnZSoim3sqKTmuL1U5_lc0asT6gsGQsvgRU5aVDBlvsUnQ";
	
	//AES/CFB/NoPadding
	protected String expectedEncryptedCFBZip = "cggWOYBnMwX9x8-BzA9tURzRwoDnPGcIllYy0FRngB8iwY5EizwVyZGxl8QWCPN_JMXVmkK9i2D420grvDoy2cnR-daZINyuNXaukBAEvTS92PbFA3n46uRa8hHfn_jXaSTcGg";
	protected String expectedEncryptedCFB = "QvFn2S5OhC2YsLQxtXOLehsXXAMQcBwec4_z3YCJM2bUzYGSHCQYnnetYleZaeZjnUX6siUFRndyraiUVmxjef2tnGCDLsMpSdZg2E_Im-bXhTBgVKjfRx0K4mZw5r9y1c2PCE2KHZLWk6VZSTaMS5yVVvs";
	
	//AES/CBC/PKCS5Padding
	protected String expectedEncryptedZip =    "r2Fj17_JbEvCw_h_nCq6srfchBlBr-mtvwyUFdpg_FR64eloR7OycjGuYCGYj0PUjnWZIHXukpy0AEqhgmb5Z4f4KPvHMX5-TGjKbrVqM_X1S8W_bHRT4Z3ThWNFZ1-hRie_8ouGYvYuYiL9KLOF3w";
	
	//path tests (should be relative paths not absolute [in Windows without C:\\])
	protected String plainPathWin = "\\Benutzer\\testuser\\Meine Daten\\Panbox\\Toller Ordner\\test.txt";
	protected String plainPathLin = "/home/testuser/Panbox/Mein Ordner/test.txt";
	
	protected String expectedPathWin = "\\SPFlwDUUlTc\\fvF4wTQdlTc\\R_Fi2yROtCSFpvo\\WvVl1y4W\\Xvtn2SQc0AqDp_o9tA\\fvF4wW8aiDE";
	protected String expectedPathLin = "/Yvtm0A/fvF4wTQdlTc/WvVl1y4W/R_Fi22EhgiGfpuY/fvF4wW8aiDE";
	
	protected String testDir = "playground";
	protected String checkedInTestdir = "testFiles";
	
	protected String plainDir = testDir + File.separator + "testdir";
	protected String obfuscationDir = testDir + File.separator + "testdirOb";
	protected String deobfuscationDir = testDir + File.separator + "testdirResult";
	
	public static byte[] generateAESKey()
	{
		byte[] aesKey = new byte[16];
		for(int i=0; i<aesKey.length; i++)
		{
			aesKey[i] = (byte)i;
		}
		return aesKey;
	}
	
	protected byte[] generateIV()
	{
		return generateAESKey();
	}
	
	protected void deleteDirectory(File dir) {
		if (dir.exists()) {
			for (String entry : dir.list()) {
				File f = new File(dir + File.separator + entry);
				if (f.isDirectory()) {
					deleteDirectory(f);
				} else {
					f.delete();
				}
			}
			dir.delete();
		}
	}
	
	protected File createTestFile(File dir, String fileName, String content) throws IOException
	{
		File file = new File(dir.getAbsolutePath() + File.separator + fileName);
		if (!file.createNewFile()) {
			fail("Can't create test file.");
		}
		
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(content.getBytes());
		fos.close();
		
		return file;		
	}
	
	protected byte[] createSha1(File file) throws Exception  {
	    MessageDigest digest = MessageDigest.getInstance("SHA-1");
	    InputStream fis = new FileInputStream(file);
	    int n = 0;
	    byte[] buffer = new byte[8192];
	    while (n != -1) {
	        n = fis.read(buffer);
	        if (n > 0) {
	            digest.update(buffer, 0, n);
	        }
	    }
	    fis.close();
	    return digest.digest();
	}
	
}
