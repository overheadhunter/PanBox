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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.panbox.core.crypto.CryptCore;
import org.panbox.core.identitymgmt.AbstractAddressbookManager;
import org.panbox.core.identitymgmt.AbstractIdentity;
import org.panbox.core.identitymgmt.CloudProviderInfo;
import org.panbox.core.identitymgmt.Identity;
import org.panbox.core.identitymgmt.PanboxContact;
import org.panbox.core.identitymgmt.SimpleAddressbook;
import org.panbox.core.identitymgmt.exceptions.ContactExistsException;
import org.panbox.desktop.common.identitymgmt.sqlightimpl.AddressbookManager;
import org.panbox.desktop.common.identitymgmt.sqlightimpl.IdentityManager;
import org.panbox.test.AbstractTest;

public class TestPersistentIdentity extends AbstractTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		setupSettings();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		
		IdentityManager idm = IdentityManager.getInstance();
		
		AbstractAddressbookManager aBookMgr = new AddressbookManager();
		idm.init(aBookMgr);
		
		SimpleAddressbook addressbook = new SimpleAddressbook();		
		Identity id = new Identity(addressbook);

		id.setEmail("testIdentity@example.org");
		id.setFirstName("Firstname");
		id.setName("Lastname");
		
		KeyPair ownerKeySign = CryptCore.generateKeypair();		
		assertNotNull(ownerKeySign);
		
		KeyPair ownerKeyEnc = CryptCore.generateKeypair();		
		assertNotNull(ownerKeyEnc);
		
		id.setOwnerKeySign(ownerKeySign, "test".toCharArray());
		id.setOwnerKeyEnc(ownerKeyEnc, "test".toCharArray());
		
		KeyPair deviceKeyLaptop = CryptCore.generateKeypair();
		assertNotNull(deviceKeyLaptop);		
		id.addDeviceKey(deviceKeyLaptop, "laptop");
		
		KeyPair deviceKeyMobile = CryptCore.generateKeypair();
		assertNotNull(deviceKeyMobile);		
		id.addDeviceKey(deviceKeyMobile, "mobile");
		
		//test adding a certificate for a tablet (we dont have the private key because that one is normally created on the tablet)
		KeyPair deviceKeyTablet = CryptCore.generateKeypair();
		assertNotNull(deviceKeyTablet);		
		//id.addDeviceKey(deviceKeyTablet, "test".toCharArray(), "mobile");
		X509Certificate certTablet = CryptCore.createSelfSignedX509Certificate(deviceKeyTablet.getPrivate(), deviceKeyTablet.getPublic(), id);		
		id.addDeviceCert(certTablet, "tablet");
		
		
		for(int i=0; i<3; i++)
		{
			CloudProviderInfo cpi = new CloudProviderInfo("Dropbox-" + i, "DropboxUser-" + i);			
			id.addCloudProvider(cpi);
		}
		
		//add some contacts
		for (int i=0; i<3; i++)
		{
			PanboxContact contact = createContact("Contact Name " +i, "contact"+i+"@example.org");
			
			try {
				addressbook.addContact(contact);
			} catch (ContactExistsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//store identity
		idm.storeMyIdentity(id);
		
		//load Identity		
		AbstractIdentity loadedID = idm.loadMyIdentity(new SimpleAddressbook());
		
//		System.out.println("Loaded: " + loadedID);
		
		assertEquals(id.getFirstName(), loadedID.getFirstName());
		assertEquals(id.getName(), loadedID.getName());
		assertEquals(id.getEmail(), loadedID.getEmail());
		
		assertEquals(id.getAddressbook().size(), loadedID.getAddressbook().size());
		assertEquals(id.getCloudProviders().size(), loadedID.getCloudProviders().size());
		
		PrivateKey privKeySign = null;
		try {
			privKeySign = loadedID.getPrivateKeySign("test".toCharArray());
		} catch (UnrecoverableKeyException e1) {
			e1.printStackTrace();
		}
		PrivateKey privKeyEnc = null;
		try {
			privKeyEnc = loadedID.getPrivateKeyEnc("test".toCharArray());
		} catch (UnrecoverableKeyException e) {
			fail();
		}
		
		//test private owner keys
		assertTrue(ownerKeySign.getPrivate().equals(privKeySign));		
		assertTrue(ownerKeyEnc.getPrivate().equals(privKeyEnc));

		//test public owner keys
		assertTrue(ownerKeySign.getPublic().equals(loadedID.getPublicKeySign()));
		assertTrue(ownerKeyEnc.getPublic().equals(loadedID.getPublicKeyEnc()));
		
		//test certificates for signature and encryption
		assertTrue(Arrays.equals(id.getCertEnc().getSignature(), loadedID.getCertEnc().getSignature()));
		assertTrue(Arrays.equals(id.getCertSign().getSignature(), loadedID.getCertSign().getSignature()));
		
		
		//test device keys
		try {
			assertTrue(deviceKeyLaptop.getPublic().equals(loadedID.getPublicKeyForDevice("laptop")));
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertTrue(deviceKeyLaptop.getPrivate().equals(loadedID.getPrivateKeyForDevice("laptop")));
		
		try {
			assertTrue(deviceKeyMobile.getPublic().equals(loadedID.getPublicKeyForDevice("mobile")));
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertTrue(deviceKeyMobile.getPrivate().equals(loadedID.getPrivateKeyForDevice("mobile")));
		
		//test device key for tablet: do we get its public key?
		PublicKey p = null;
		try {
			p = loadedID.getPublicKeyForDevice("tablet");
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		assertTrue(deviceKeyTablet.getPublic().equals(p));
		
		//test addressbook entries		
		Collection<PanboxContact> c1 = id.getAddressbook().getContacts();
		Collection<PanboxContact> c2 = loadedID.getAddressbook().getContacts();
		
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
		
//		idm.shutDownConnection();
		
	}
}
