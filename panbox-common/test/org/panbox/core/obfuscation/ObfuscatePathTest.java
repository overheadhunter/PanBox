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
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

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

public class ObfuscatePathTest extends AbstractTest {

	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	
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
	public void test() throws ObfuscationException {
	
		String sharePath = testFolder.getRoot().getAbsolutePath();
//		System.out.println("sharePath: " + sharePath);
		
		AbstractObfuscatorFactory aof = null;
		try {
			aof = AbstractObfuscatorFactory.getFactory(FileObfuscatorFactory.class);
		} catch (ClassNotFoundException | InstantiationException
				| IllegalAccessException e1) {
			e1.printStackTrace();
			fail(e1.getMessage());
		}
		
		Obfuscator ob = aof.getInstance(sharePath, "myShareName");
				
		String originalDirName = File.separator + "testFolder" + File.separator +  "testFile.txt";
		SecretKey key = new SecretKeySpec(generateAESKey(), "AES");
					
		String obfuscatedDirName = ob.obfuscatePath(originalDirName, key, true);
//		System.out.println("obName: " + obfuscatedDirName);
		
		File testFile = new File(sharePath + File.separator + obfuscatedDirName);
		try {
			testFile.getParentFile().mkdirs();
			
			testFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String deObfuscated = "";
		
		//get first folder (testFolder)
		File dir = null;
		for (File d : testFolder.getRoot().listFiles())
		{
			if(! d.getName().startsWith("."))
			{
				dir = d;
			}
		}
		
		String path = File.separator;
		if(dir.isDirectory())
		{
			path += dir.getName() + File.separator;
			
			//get first File (testFile.txt)
			File f = dir.listFiles()[0];
			path += f.getName();				
//			System.out.println("path: " + path);
			
//			ob = Obfuscator.getInstance(sharePath, null);
			
			deObfuscated = ob.deObfuscatePath(path, key);
				
//			System.out.println("deOb: " + deObfuscated);
				
		}
		
		assertEquals(originalDirName, deObfuscated);
		
	}

}
