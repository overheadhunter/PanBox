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
package org.panbox.desktop.linux.dbus;

import java.security.UnrecoverableKeyException;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.panbox.core.exception.ShareMetaDataException;
import org.panbox.desktop.common.sharemgmt.ShareManagerException;

/**
 * @author Dominik Spychalski
 * 
 */
public class StatusCodeTest extends TestCase {
	public byte GENERAL_OK = 0b00000000;
	public byte GUI_SHAREMETADATA = 0b01101000;
	public byte VFS_SQLERROR = 0b01000100;

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
	public void testGetComponentID() {

		assertEquals(StatusCode.GENERAL, StatusCode.getComponentID(GENERAL_OK));
		assertEquals(StatusCode.GUI,
				StatusCode.getComponentID(GUI_SHAREMETADATA));
		assertEquals(StatusCode.VFS, StatusCode.getComponentID(VFS_SQLERROR));
	}

	@Test
	public void testBuildCode() {

		assertEquals(GENERAL_OK,
				StatusCode.buildcode(StatusCode.GENERAL, StatusCode.OK));
		assertEquals(GUI_SHAREMETADATA,
				StatusCode.buildcode(StatusCode.GUI, StatusCode.SHAREMETADATA));
		assertEquals(VFS_SQLERROR,
				StatusCode.buildcode(StatusCode.VFS, StatusCode.SQLERROR));
	}

	@Test
	public void testGetCode() {
		// System.out.println(StatusCode.getCode(GUI_SHAREMETADATA));
		// System.out.println(StatusCode.SHAREMETADATA);

		System.out.println(StatusCode.VFS_OK);

		assertEquals(StatusCode.OK, StatusCode.getCode(GENERAL_OK));
		assertEquals(StatusCode.SHAREMETADATA,
				StatusCode.getCode(GUI_SHAREMETADATA));
		assertEquals(StatusCode.SQLERROR, StatusCode.getCode(VFS_SQLERROR));
	}

	@Test
	public void testGet() {

		// Exception e = new IllegalArgumentException();
		// assertEquals(StatusCode.ILLEGALARGUMENT, StatusCode.get(e));
		//
		// e = new
		// ShareManagerException("Could not add Device to ShareMetadata");
		// assertEquals(StatusCode.DEVICEEXISTS, StatusCode.get(e));
		//
		// e = new ShareManagerException("");
		// assertEquals(StatusCode.SQLERROR, StatusCode.get(e));
		//
		// e = new IndexOutOfBoundsException();
		// assertEquals(StatusCode.UNKNOWN, StatusCode.get(e));

	}

	@Test
	public void testFinal() {
		Throwable[] ex = new Throwable[5];

		ex[0] = new IllegalArgumentException();
		ex[1] = new UnrecoverableKeyException();
		ex[2] = new ShareManagerException("");
		ex[3] = new ShareMetaDataException("");
		ex[4] = new ShareManagerException(
				"Could not add Device to ShareMetadata");

		for (int i = 0; i < ex.length; i++) {
			try {
				throw ex[i];

			} catch (IllegalArgumentException | UnrecoverableKeyException
					| ShareManagerException | ShareMetaDataException e) {

				// System.out.println(StatusCode.get(e));

			} catch (Throwable e) {
				e.printStackTrace();
			}
		}

	}
}
