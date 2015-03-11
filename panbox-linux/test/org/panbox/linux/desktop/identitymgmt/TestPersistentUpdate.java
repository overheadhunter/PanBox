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

public class TestPersistentUpdate extends AbstractTest {

	// private static int testCounter = 0;

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

		// test adding a certificate for a tablet (we dont have the private key
		// because that one is normally created on the tablet)
		KeyPair deviceKeyTablet = CryptCore.generateKeypair();
		assertNotNull(deviceKeyTablet);
		// id.addDeviceKey(deviceKeyTablet, "test".toCharArray(), "mobile");
		X509Certificate certTablet = CryptCore.createSelfSignedX509Certificate(
				deviceKeyTablet.getPrivate(), deviceKeyTablet.getPublic(), id);
		id.addDeviceCert(certTablet, "tablet");

		for (int i = 0; i < 3; i++) {
			CloudProviderInfo cpi = new CloudProviderInfo("Dropbox-" + i,
					"DropboxUser-" + i);
			id.addCloudProvider(cpi);
		}

		// add some contacts
		for (int i = 0; i < 2; i++) {
			PanboxContact contact = createContact("Contact Name " + i,
					"contact" + i + "@example.org");

			try {
				addressbook.addContact(contact);
			} catch (ContactExistsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// store identity
		idm.storeMyIdentity(id);

		// load Identity
		AbstractIdentity loadedID = idm.loadMyIdentity(new SimpleAddressbook());
		// HACK: because we compare id with loadedID, but never update id by
		// loading it from the database, its id-field has to be set here
		// id.setID(loadedID.getID());

		compareIdentities(id, (Identity) loadedID);

		// test change identity name + email
		// id = (Identity) loadedID;
		id.setEmail("newmail@bla.com");
		id.setName("NewLast");
		id.setFirstName("NEWFirst");

		idm.storeMyIdentity(id);
		loadedID = idm.loadMyIdentity(new SimpleAddressbook());
		compareIdentities(id, (Identity) loadedID);

		// test change identity keys
		// id = (Identity) loadedID;
		KeyPair ownerKeySign2 = CryptCore.generateKeypair();
		KeyPair ownerKeyEnc2 = CryptCore.generateKeypair();

		id.setOwnerKeySign(ownerKeySign2, "test".toCharArray());
		id.setOwnerKeyEnc(ownerKeyEnc2, "test".toCharArray());

		idm.storeMyIdentity(id);
		loadedID = idm.loadMyIdentity(new SimpleAddressbook());
		compareIdentities(id, (Identity) loadedID);

		// loaded keys need to be different from the ones created at the
		// beginning
		try {
			assertTrue(loadedID.getPrivateKeyEnc("test".toCharArray()) != ownerKeyEnc
					.getPrivate());
		} catch (UnrecoverableKeyException e) {
			fail();
		}
		assertTrue(loadedID.getPublicKeyEnc() != ownerKeyEnc.getPublic());
		try {
			assertTrue(loadedID.getPrivateKeySign("test".toCharArray()) != ownerKeySign
					.getPrivate());
		} catch (UnrecoverableKeyException e) {
			e.printStackTrace();
		}
		assertTrue(loadedID.getPublicKeySign() != ownerKeySign.getPublic());

		// test edit cloudprovider
		// id = (Identity) loadedID;
		for (CloudProviderInfo cpi : id.getCloudProviders().values()) {
			if (cpi.getProviderName().equals("Dropbox-2")) {
				cpi.setUsername("ChangedDropbox");
			}
		}

		idm.storeMyIdentity(id);
		loadedID = idm.loadMyIdentity(new SimpleAddressbook());
		compareIdentities(id, (Identity) loadedID);

		// test delete cloudprovider
		// id = (Identity) loadedID;
		CloudProviderInfo delMe = null;
		for (CloudProviderInfo cpi : id.getCloudProviders().values()) {
			if (cpi.getProviderName().equals("Dropbox-1")) {
				delMe = cpi;
				break;
			}
		}
		id.delCloudProvider(delMe.getProviderName());

		idm.storeMyIdentity(id);
		loadedID = idm.loadMyIdentity(new SimpleAddressbook());
		compareIdentities(id, (Identity) loadedID);

		// test add cloudprovider
		// id = (Identity) loadedID;
		CloudProviderInfo newCP = new CloudProviderInfo("MyNewProv",
				"Mynewuser");
		id.addCloudProvider(newCP);

		idm.storeMyIdentity(id);
		loadedID = idm.loadMyIdentity(new SimpleAddressbook());
		compareIdentities(id, (Identity) loadedID);

		// test delete contact
		// id = (Identity) loadedID;
		id.getAddressbook().deleteContact("contact1@example.org");

		idm.storeMyIdentity(id);
		loadedID = idm.loadMyIdentity(new SimpleAddressbook());
		compareIdentities(id, (Identity) loadedID);

		// test change contact name
		// id = (Identity) loadedID;
		for (PanboxContact c : id.getAddressbook().getContacts()) {
			if (c.getEmail().equals("contact0@example.org")) {
				c.setName("ChangedName");
			}
		}

		idm.storeMyIdentity(id);
		loadedID = idm.loadMyIdentity(new SimpleAddressbook());
		compareIdentities(id, (Identity) loadedID);

		// test remove cloudprovider and change cloudprovider of a contact
		// id = (Identity) loadedID;
		for (PanboxContact c : id.getAddressbook().getContacts()) {
			if (c.getEmail().equals("contact0@example.org")) {
				CloudProviderInfo cpi1 = c.getCloudProvider("CloudProvider-1");
				// c.getCloudProviders().remove(cpi1);
				c.removeCloudProvider(cpi1);

				CloudProviderInfo cpi2 = c.getCloudProvider("CloudProvider-2");
				cpi2.setUsername("new user");
			}
		}

		idm.storeMyIdentity(id);
		loadedID = idm.loadMyIdentity(new SimpleAddressbook());
		compareIdentities(id, (Identity) loadedID);

		// test add cloudprovider to contact
		// id = (Identity) loadedID;
		for (PanboxContact c : id.getAddressbook().getContacts()) {
			if (c.getEmail().equals("contact0@example.org")) {
				CloudProviderInfo cpi = new CloudProviderInfo("Added-Provider",
						"added-username");
				// c.getCloudProviders().remove(cpi1);
				c.addCloudProvider(cpi);
			}
		}

		idm.storeMyIdentity(id);
		loadedID = idm.loadMyIdentity(new SimpleAddressbook());
		compareIdentities(id, (Identity) loadedID);

		// test add contact
		// id = (Identity) loadedID;
		PanboxContact contact = new PanboxContact();
		contact.setEmail("hallo@test.de");
		contact.setName("Clast");
		contact.setFirstName("CFirst");

		KeyPair cSign = CryptCore.generateKeypair();
		KeyPair cEnc = CryptCore.generateKeypair();

		contact.setCertEnc(CryptCore.createSelfSignedX509Certificate(
				cEnc.getPrivate(), cEnc.getPublic(), contact));
		contact.setCertSign(CryptCore.createSelfSignedX509Certificate(
				cSign.getPrivate(), cSign.getPublic(), contact));

		CloudProviderInfo cpi = new CloudProviderInfo("testAddedPro",
				"testaddedContactUser");
		contact.addCloudProvider(cpi);

		try {
			id.getAddressbook().addContact(contact);
		} catch (ContactExistsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		idm.storeMyIdentity(id);
		loadedID = idm.loadMyIdentity(new SimpleAddressbook());
		compareIdentities(id, (Identity) loadedID);

	}

	private void compareIdentities(Identity id, Identity loadedID) {
		// System.out.println(testCounter++);

		assertEquals(id.getFirstName(), loadedID.getFirstName());
		assertEquals(id.getName(), loadedID.getName());
		assertEquals(id.getEmail(), loadedID.getEmail());

		assertEquals(id.getAddressbook().size(), loadedID.getAddressbook()
				.size());
		assertEquals(id.getCloudProviders().size(), loadedID
				.getCloudProviders().size());

		PrivateKey privKeySign = null;
		try {
			privKeySign = loadedID.getPrivateKeySign("test".toCharArray());
		} catch (UnrecoverableKeyException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		PrivateKey privKeyEnc = null;
		try {
			privKeyEnc = loadedID.getPrivateKeyEnc("test".toCharArray());
		} catch (UnrecoverableKeyException e) {
			fail();
		}

		// test private owner keys
		try {
			assertTrue(loadedID.getPrivateKeySign("test".toCharArray()).equals(
					privKeySign));
		} catch (UnrecoverableKeyException e1) {
			e1.printStackTrace();
		}
		try {
			assertTrue(loadedID.getPrivateKeyEnc("test".toCharArray()).equals(
					privKeyEnc));
		} catch (UnrecoverableKeyException e) {
			fail();
		}

		// test public owner keys
		assertTrue(id.getPublicKeySign().equals(loadedID.getPublicKeySign()));
		assertTrue(id.getPublicKeyEnc().equals(loadedID.getPublicKeyEnc()));

		// test certificates for signature and encryption
		assertTrue(Arrays.equals(id.getCertEnc().getSignature(), loadedID
				.getCertEnc().getSignature()));
		assertTrue(Arrays.equals(id.getCertSign().getSignature(), loadedID
				.getCertSign().getSignature()));

		// test device keys
		try {
			assertTrue(id.getPublicKeyForDevice("laptop").equals(
					loadedID.getPublicKeyForDevice("laptop")));
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertTrue(id.getPrivateKeyForDevice("laptop").equals(
				loadedID.getPrivateKeyForDevice("laptop")));

		try {
			assertTrue(id.getPublicKeyForDevice("mobile").equals(
					loadedID.getPublicKeyForDevice("mobile")));
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertTrue(id.getPrivateKeyForDevice("mobile").equals(
				loadedID.getPrivateKeyForDevice("mobile")));

		// test device key for tablet: do we get its public key?
		PublicKey p = null;
		try {
			p = loadedID.getPublicKeyForDevice("tablet");
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			assertTrue(id.getPublicKeyForDevice("tablet").equals(p));
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// test addressbook entries
		Collection<PanboxContact> c1 = id.getAddressbook().getContacts();
		Collection<PanboxContact> c2 = loadedID.getAddressbook().getContacts();

		assertTrue(c1.size() == c2.size());

		boolean sameAB = true;
		for (PanboxContact cs : c1) {
			if (!c2.contains(cs)) {
				sameAB = false;
				break;
			}
		}
		assertTrue(sameAB);
	}

}
