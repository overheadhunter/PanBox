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

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Test;
import org.panbox.core.identitymgmt.exceptions.ContactExistsException;

public class TestDeleteContact extends IdentityHelper {

	protected static void setUpBeforeClass() throws Exception {
	}

	protected static void tearDownAfterClass() throws Exception {
	}

	public void setUp() throws Exception {
//		super.setUp();
	}

	@After
	public void tearDown() throws Exception {
//		super.tearDown();
	}
	
	@Test
	public void testDeleteContact()
	{	
		SimpleAddressbook addressbook = new SimpleAddressbook();
		AbstractIdentity id = new Identity(addressbook);
		
//		AddressbookProvider.addProvider(addressbook);
//		AddressbookProvider addressbook = AddressbookProvider.getProvider();
//		id.setAddressbook(addressbook);
		
		for (int i=0; i<2; i++)
		{
			PanboxContact contact = createContact("Contact Name " +i, "contact"+i+"@example.org");
			
			try {
				addressbook.addContact(contact);
			} catch (ContactExistsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//delete a contact
		String removeEmail = "contact0@example.org";
		
		id.deleteContact(removeEmail);
		
		//test if contact was deleted
		
		assertEquals(1, addressbook.size());
		
		for(PanboxContact c : addressbook.getContacts())
		{
			assertFalse(c.getEmail().equals(removeEmail));
		}
		
	}

}
