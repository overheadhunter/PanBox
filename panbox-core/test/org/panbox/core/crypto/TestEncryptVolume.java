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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestEncryptVolume {

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
		
/*		*//**
		 * Create a volume object
		 *//*
	
		Volume vl = new Volume();	
		assertNotNull(vl);
		
		*//**
		 * Get meta data for the volume
		 *//*
		
		IShareMetaData getShareMetaData = vl.getShareMetaData();
		assertNotNull(getShareMetaData);
		
		*//**
		 * Get obfuscation key to decrypt the volume
		 *//*
		
		
		obfuskey = smd.getList with Obfuskey
				
	    *//**
	     * Get symm key to encrypt the volume			
	     *//*
			
		
		symkey = smd.getList withSymkey
				
		*//**
		 * 	Get public owner encryption key to decrypt the symmetric key	
		 *//*
				
		
		use my public owner encryption key to decrypt symmkey
		
		*//**
		 * for each file in the volume, deobfuscate using obfuskey
		 *//*
		
		foreach file f in vol
			deobfus f obfuskey
			
		*//**
		* for each file in the volume, encrypt using symmkey
		*//*
		
		foreach file f in vol
			encrypt f symmkey
			
		*//**
		* Get obfuscation key to encrypt the volume
		*//*
			
			foreach file f in vol
			obfus f obfuskey	
			
	    *//**
	     * test if checksum matches		
	     *//*
			
		test if checksum matches*/
	
	}

}
