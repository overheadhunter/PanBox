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
package org.panbox.core.identitymgmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.security.KeyPair;
import java.security.Security;
import java.security.UnrecoverableKeyException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.panbox.core.crypto.CryptCore;
import org.panbox.core.identitymgmt.exceptions.ContactExistsException;

public class TestCreateIdentity extends IdentityHelper {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {

		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {

		SimpleAddressbook addressbook = new SimpleAddressbook();

		Identity id = new Identity(addressbook, "testIdentity@example.org",
				"FirstName", "Lastname");

		assertNotNull(id);

		KeyPair ownerKeySign = CryptCore.generateKeypair();
		assertNotNull(ownerKeySign);

		KeyPair ownerKeyEnc = CryptCore.generateKeypair();
		assertNotNull(ownerKeyEnc);

		KeyPair deviceKey = CryptCore.generateKeypair();
		assertNotNull(deviceKey);

		id.setOwnerKeySign(ownerKeySign, "test".toCharArray());
		id.setOwnerKeyEnc(ownerKeyEnc, "test".toCharArray());
		id.addDeviceKey(deviceKey, "laptop");

		// add some contacts
		for (int i = 0; i < 3; i++) {
			PanboxContact contact = createContact("Contact Name " + i,
					"contact" + i + "@example.org");

			try {
				addressbook.addContact(contact);
			} catch (ContactExistsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// test if names match
		assertEquals("FirstName", id.getFirstName());
		assertEquals("Lastname", id.getName());

		// test if email matches
		assertEquals("testIdentity@example.org", id.getEmail());

		assertNotNull(id.getAddressbook());

		// test if there are 5 contacts in the addressbook
		assertEquals(id.getAddressbook().size(), 3);

		// test if device key matches
		try {
			assertEquals(deviceKey.getPublic(),
					id.getPublicKeyForDevice("laptop"));
			assertEquals(deviceKey.getPrivate(),
					id.getPrivateKeyForDevice("laptop"));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// test if sign key matches
		assertEquals(ownerKeySign.getPublic(), id.getPublicKeySign());
		try {
			assertEquals(ownerKeySign.getPrivate(),
					id.getPrivateKeySign("test".toCharArray()));
		} catch (UnrecoverableKeyException e1) {
			e1.printStackTrace();
		}

		// test if enc key matches
		try {
			assertEquals(ownerKeyEnc.getPrivate(),
					id.getPrivateKeyEnc("test".toCharArray()));
		} catch (UnrecoverableKeyException e) {
			fail();
		}
		assertEquals(ownerKeyEnc.getPublic(), id.getPublicKeyEnc());

	}

}
