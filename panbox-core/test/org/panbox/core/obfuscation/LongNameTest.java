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

import static org.junit.Assert.*;

import java.io.File;

import org.junit.After;
import org.junit.Test;

/**
 * This test fails on windows if the complete path is longer than 260 chars...
 * @author triller
 *
 */
public class LongNameTest {

	File file = null;
	
	@Test
	public void test() {

		StringBuilder sb = new StringBuilder();

		// 255 chars is maximum directory name in windows
		for (int i = 0; i < 255; i++) {
			sb.append('a');
		}

		String fileName = sb.toString();

		// this works because this java program does not use the windows API
		// which limits path names to 260 chars ...
//		file = new File("playground" + File.separator + fileName);
		file = new File(fileName);
		try {
			file.mkdir();
		} catch (Exception e) // somehow there is no exception, even though if
								// the directory name is too long
		{
			e.printStackTrace();
		} catch (Throwable t) {
			System.out.println(t);
		}

		if (!file.exists())
			fail();

		// test to open a directory where the user has no permission (he cannot
		// open it in windows explorer)
//		File f2 = new File("C:/Users/adm-itm");
//
//		boolean ex = f2.exists();
//		System.out.println("f2 exists: " + ex);
//
//		System.out.println("f2 is directory: " + f2.isDirectory());
//
//		// try
//		// {
//		System.out.println("f2 is null?: " + (f2 == null));
//
//		String[] files = f2.list();
//
//		System.out.println("files is null?: " + (files == null));
//
//		if (files != null) {
//			for (String s : f2.list()) // list() returns null because the user
//										// does not have permission to access
//										// the directory
//			{
//				System.out.println(s);
//			}
//		}
		// }
		// catch (Exception e)
		// {
		// e.printStackTrace();
		// }
		// catch(Throwable t)
		// {
		// System.out.println(t);
		// }

	}
	
	@After
	public void tearDown()
	{
		if(file.exists())
		{
			file.delete();
		}
	}

}
