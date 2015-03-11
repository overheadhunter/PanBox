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
package org.panbox.desktop.common.identitymgmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.security.KeyPair;
import java.util.Collection;
import java.util.LinkedList;

import org.junit.After;
import org.junit.Test;
import org.panbox.core.crypto.CryptCore;
import org.panbox.core.identitymgmt.AbstractAddressbookManager;
import org.panbox.core.identitymgmt.AbstractIdentity;
import org.panbox.core.identitymgmt.Identity;
import org.panbox.core.identitymgmt.IdentityHelper;
import org.panbox.core.identitymgmt.PanboxContact;
import org.panbox.core.identitymgmt.SimpleAddressbook;
import org.panbox.core.identitymgmt.exceptions.ContactExistsException;
import org.panbox.desktop.common.identitymgmt.sqlightimpl.AddressbookManager;

import ezvcard.VCard;

public class TestImportExportVcardContacts extends IdentityHelper {

	private static File contactFile = new File("addressbookExport.vcf");
	
	protected static void setUpBeforeClass() throws Exception {
	}

	protected static void tearDownAfterClass() throws Exception {
	}

	protected void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
		if(contactFile.exists())
			contactFile.delete();
	}

	@Test
	public void testExportImport()
	{
		AbstractAddressbookManager aBookMgr = new AddressbookManager();
				
		SimpleAddressbook addressbook = new SimpleAddressbook();
		AbstractIdentity id = new Identity(addressbook);		
		
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
		
		//do export
		LinkedList<VCard> vcards = new LinkedList<VCard>();
		for (PanboxContact c : id.getAddressbook().getContacts()) {
			VCard v = AbstractAddressbookManager.contact2VCard(c);			
			vcards.add(v);
		}
		boolean doExport = AbstractAddressbookManager.exportContacts(vcards, contactFile);		
		assertTrue(doExport);
		
		//do import		
		SimpleAddressbook dp1 = new SimpleAddressbook();
		AbstractIdentity idImport = new Identity(dp1);
		
		KeyPair ownerKeySign = CryptCore.generateKeypair();
		idImport.setOwnerKeySign(ownerKeySign, new char[] {'a', 'b', 'c'});
		idImport.setOwnerKeyEnc(ownerKeySign, new char[] {'a', 'b', 'c'});
		
		Collection<PanboxContact> imported = null;
		try {
			imported = aBookMgr.importContacts(idImport, contactFile, true);
		} catch (ContactExistsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertTrue(imported.size() > 0);		
		
		//need to have same size
		assertEquals(idImport.getAddressbook().size(), addressbook.size());
		
		//compare addressbook entries
		Collection<PanboxContact> c1 = idImport.getAddressbook().getContacts();
		Collection<PanboxContact> c2 = addressbook.getContacts();
		
		assertTrue(c1.size() == c2.size()); 
		
		boolean same = true;
		for(PanboxContact cs : c1)
		{			
			if(!c2.contains(cs))
			{
				same = false;
				break;
			}
		}
		assertTrue(same);
		
	}
	
}
