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
package org.panbox.mobile.android.identitymgmt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyPair;
import java.util.Collection;
import java.util.LinkedList;

import org.panbox.Settings;
import org.panbox.core.crypto.CryptCore;
import org.panbox.core.exception.RandomDataGenerationException;
import org.panbox.core.identitymgmt.AbstractAddressbookManager;
import org.panbox.core.identitymgmt.AbstractIdentity;
import org.panbox.core.identitymgmt.AbstractIdentityManager;
import org.panbox.core.identitymgmt.CloudProviderInfo;
import org.panbox.core.identitymgmt.Identity;
import org.panbox.core.identitymgmt.PanboxContact;
import org.panbox.core.identitymgmt.SimpleAddressbook;
import org.panbox.core.identitymgmt.VCardProtector;
import org.panbox.core.identitymgmt.exceptions.ContactExistsException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;
import ezvcard.VCard;

public class IdentityDebugApp {

	private final String accountName = "Panbox";
	private final String accountType = "org.panbox";

	private ContentResolver cr = null;
	private Context context = null;

	private AccountManager am = null;
	private Account panboxAccount = null;

	private AbstractIdentity identity = null;

	public IdentityDebugApp(Context context, ContentResolver contentResolver) {
		this.cr = contentResolver;
		this.context = context;

		am = AccountManager.get(context);
	}

	public void createPanboxAccount() {

		panboxAccount = new Account(accountName, accountType);
		am.addAccountExplicitly(panboxAccount, null, null);

		Log.i(IdentityDebugApp.class.getSimpleName(), "Created Panbox Account");
	}

	public void deletePanboxAccount() {

		Account[] accounts = am.getAccountsByType(accountType);
		Account ac = null;

		String[] names = new String[accounts.length];
		for (int i = 0; i < names.length; i++) {
			if (accounts[i].type.equals(accountType)) {
				ac = accounts[i];
			}
		}

		if (null != ac) {
			am.removeAccount(ac, null, null);
			Log.i(IdentityDebugApp.class.getSimpleName(),
					"Removed Panbox Account");
		}

		//
		// for(int i=2; i<5; i++)
		// {
		// String where = RawContacts._ID + "=?";
		// String[] selectionArgs = new String[] { String.valueOf(i) };
		//
		// Uri uri = RawContacts.CONTENT_URI;
		// Uri updateUri =
		// uri.buildUpon().appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER,
		// "true").build();
		//
		// int res = cr.delete(updateUri, where, selectionArgs);
		//
		// System.out.println("Delete result: " + res);
		// }
	}

	/**
	 * Call Create Identity first!
	 */	
	public void addContactTest() {

		if (null == identity) {
			Toast.makeText(context,
					"Create one ID first that we can load afterwards",
					Toast.LENGTH_LONG).show();
			System.err
					.println("No identity to add contact to, please create one");

			return;
		}
		PanboxContact c = new PanboxContact();

		c.setEmail("contactAdded@bla.de");
		c.setName("AddedLastName");
		c.setFirstName("Alice");
		
		c.setTrustLevel(2);

		CloudProviderInfo cpi1 = new CloudProviderInfo("Cloud1", "Alice-Cloud1");
		c.addCloudProvider(cpi1);

		KeyPair cSigKey = CryptCore.generateKeypair();
		KeyPair cEncKey = CryptCore.generateKeypair();

		c.setCertEnc(CryptCore.createSelfSignedX509Certificate(
				cEncKey.getPrivate(), cEncKey.getPublic(), c));
		c.setCertSign(CryptCore.createSelfSignedX509Certificate(
				cSigKey.getPrivate(), cSigKey.getPublic(), c));

		try {
			identity.getAddressbook().addContact(c);
		} catch (ContactExistsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// change cpi of a contact

//		PanboxContact pbc = identity.getAddressbook().contactExists(
//				"contact1@test.de");
//		CloudProviderInfo cpInfo = pbc.getCloudProvider("Skydrive");
//		cpInfo.setUsername("changed-Bobs-Skydriveuser");
//
//		// remove cpi in contact
//		cpInfo = pbc.getCloudProvider("Dropbox");
//		pbc.removeCloudProvider(cpInfo);
//
//		// add cpi to contact
//		CloudProviderInfo cpi123 = new CloudProviderInfo("Wuala", "Bobs-Wuala");
//		pbc.addCloudProvider(cpi123);
//
//		// change name, email etc of contact
//		pbc.setEmail("12345@12345.com");
//		pbc.setName("12Name");
//		pbc.setFirstName("12Firstname");
//
//		// change mail
//		identity.setEmail("newMail@testing.org");
//
//		identity.setName("NewLastName");
//		identity.setFirstName("NewFirstName");
//
//		// del cpi
//		CloudProviderInfo del = null;
//		for (CloudProviderInfo cpi : identity.getCloudProviders().values()) {
//			if (cpi.getProviderName().equals("Dropbox")) {
//				del = cpi;
//			}
//		}
//		identity.delCloudProvider(del.getProviderName());
//
//		// add new cpi
//		CloudProviderInfo newCPI = new CloudProviderInfo("NewCloud",
//				"myNewuser@bla.com");
//		identity.addCloudProvider(newCPI);
//
//		Settings pbSettings = Settings.getInstance();
//		pbSettings.setConfDir(context.getFilesDir().getAbsolutePath());
//		// pbSettings.setPanboxIdentityDBFile(context.getFilesDir()
//		// + File.separator + "identity.db");
//		// pbSettings.setPanboxKeystore(context.getFilesDir() + File.separator
//		// + "keystore.jks");

		AbstractIdentityManager idm = IdentityManagerAndroid
				.getInstance(context);

		idm.storeMyIdentity(identity);

	}

	public void deleteContactsTest() {

		if (null == identity) {
			Toast.makeText(context,
					"Create one ID first that we can load afterwards",
					Toast.LENGTH_LONG).show();
			System.err
					.println("No identity to add contact to, please create one");

			return;
		}

		identity.deleteContact("contact1@test.de");

		Settings pbSettings = Settings.getInstance();
		pbSettings.setConfDir(context.getFilesDir().getAbsolutePath());
		// pbSettings.setPanboxIdentityDBFile(context.getFilesDir()
		// + File.separator + "identity.db");
		// pbSettings.setPanboxKeystore(context.getFilesDir() + File.separator
		// + "keystore.jks");

		AbstractIdentityManager idm = IdentityManagerAndroid
				.getInstance(context);

		idm.storeMyIdentity(identity);

		// String where = RawContacts.ACCOUNT_TYPE + "=?";
		// String[] selectionArgs = new String[] { accountType };
		//
		// // delete
		// Uri uri = RawContacts.CONTENT_URI;
		//
		// Uri updateUri = uri
		// .buildUpon()
		// .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER,
		// "true").build();
		//
		// int res = cr.delete(updateUri, where, selectionArgs);
		// System.out.println("Delete result: " + res);
	}

	public void createIdentity() {
//		AbstractAddressbookManager adm = (AbstractAddressbookManager) new AddressbookManagerAndroid(
//				context, cr);
		
		AddressbookManagerAndroid adm = new AddressbookManagerAndroid(context, cr);

		Settings pbSettings = Settings.getInstance();
		pbSettings.setConfDir(context.getFilesDir().getAbsolutePath());
		// pbSettings.setPanboxIdentityDBFile(context.getFilesDir()
		// + File.separator + "identity.db");
		// pbSettings.setPanboxKeystore(context.getFilesDir() + File.separator
		// + "keystore.jks");

		AbstractIdentityManager idm = IdentityManagerAndroid
				.getInstance(context);
		
		//IMPORTANT TO INIT ADDRESSBOOKMANAGER HERE!!!
		//DO THIS BEFORE ANY OTHER CALL ON THE IDENTITYMANAGER
		idm.init(adm);
		//IMPORTANT TO INIT ADDRESSBOOKMANAGER HERE!!!
		
		AbstractIdentity id = null;

		System.out.println("Create identity in file: "
				+ pbSettings.getIdentityPath());

		SimpleAddressbook ab = new SimpleAddressbook();
		id = new Identity(ab);

		id.setFirstName("Panbox");
		id.setName("Tester");
		id.setEmail("panbox@tester.org");

		KeyPair ownerKeySign = CryptCore.generateKeypair();
		KeyPair ownerKeyEnc = CryptCore.generateKeypair();
		KeyPair deviceKey = CryptCore.generateKeypair();

		id.setOwnerKeySign(ownerKeySign, "test".toCharArray());
		id.setOwnerKeyEnc(ownerKeyEnc, "test".toCharArray());
		id.addDeviceKey(deviceKey, "laptop");

		CloudProviderInfo cpi = new CloudProviderInfo("Dropbox",
				"myUser@domain.com");

		id.addCloudProvider(cpi);

//		PanboxContact cs = new PanboxContact();
//		cs.setEmail("contact1@test.de");
//		cs.setName("BobLastname");
//		cs.setFirstName("Bob");
//		
//		cs.setTrustLevel(1);
//
//		CloudProviderInfo cpiContact = new CloudProviderInfo("Dropbox",
//				"myUser123@bla.com");
//		cs.addCloudProvider(cpiContact);
//
//		CloudProviderInfo cpiContact2 = new CloudProviderInfo("Skydrive",
//				"test@hallo.com");
//		cs.addCloudProvider(cpiContact2);
//
//		id.getAddressbook().addContact(cs);
//
//		KeyPair cSigKey = CryptCore.generateKeypair();
//		KeyPair cEncKey = CryptCore.generateKeypair();
//
//		cs.setCertEnc(CryptCore.createSelfSignedX509Certificate(
//				cEncKey.getPrivate(), cEncKey.getPublic(), cs));
//		cs.setCertSign(CryptCore.createSelfSignedX509Certificate(
//				cSigKey.getPrivate(), cSigKey.getPublic(), cs));

		idm.storeMyIdentity(id);

		// for local testing
		// identity = idm.loadMyIdentity(new SimpleAddressbook(), adm);
		identity = id;

	}

	public void deleteIdentity() {

		// boolean dbDeleted = context.deleteDatabase(context.getFilesDir() +
		// File.separator + "identity.db");
		boolean dbDeleted = context.deleteDatabase("identity.db");

		System.out.println("DB deleted?: " + dbDeleted);

		File f = new File(context.getFilesDir() + File.separator
				+ "keystore.jks");

		System.out.println("Trying to delete file: " + f.getAbsolutePath());

		if (f.exists()) {
			f.delete();
		} else {
			System.err.println("Could not find keystore file to delete it.");
		}

		// deleteContactsTest();

	}

	public void loadIdentityTest() {

		// if(identity == null)
		// {
		// Toast.makeText(context,
		// "Create one ID first that we can load afterwards",
		// Toast.LENGTH_LONG).show();
		// System.out.println("Create one ID first that we can load");
		// return;
		// }

		AbstractAddressbookManager adm = (AbstractAddressbookManager) new AddressbookManagerAndroid(
				context, cr);

		Settings pbSettings = Settings.getInstance();
		pbSettings.setConfDir(context.getFilesDir().getAbsolutePath());
		// pbSettings.setPanboxIdentityDBFile(context.getFilesDir()
		// + File.separator + "identity.db");
		// pbSettings.setPanboxKeystore(context.getFilesDir() + File.separator
		// + "keystore.jks");

		AbstractIdentityManager idm = IdentityManagerAndroid
				.getInstance(context);
		idm.init(adm);

		AbstractIdentity id = idm.loadMyIdentity(new SimpleAddressbook());

		System.out.println("ID loaded: " + id.getFirstName() + " "
				+ id.getName() + " " + id.getEmail());
		
		identity = id;

	}
	
	public void exportAddressbook() {
		File[] files = ContextCompat.getExternalFilesDirs(this.context, null);
		
		for(File f : files)
		{
			System.out.println(f.getAbsolutePath());
		}
		
		String rootPath = files[0].getAbsolutePath();
		
		boolean createdDirs = files[0].mkdirs();
		System.out.println("created dirs? " + createdDirs);
		
		File aBookFileTMP = new File(rootPath + File.separator + "abookTMP.vcf");
				
		Collection<PanboxContact> contacts = identity.getAddressbook().getContacts();
		
		LinkedList<VCard> vcards = new LinkedList<VCard>(); 
		for(PanboxContact c : contacts)
		{
			vcards.add(AbstractAddressbookManager.contact2VCard(c));			
		}
		
		//export myself too
		vcards.add(AbstractAddressbookManager.contact2VCard(identity));
		
		//temporary vcf file
		AbstractAddressbookManager.exportContacts(vcards, aBookFileTMP);
		
		char[] pass = null;
		try {
			pass = VCardProtector.generatePassword();
		} catch (RandomDataGenerationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.err.println("Export password is: " + String.valueOf(pass));
		
		//TODO: HACK for testing
//		pass = new char[]{'1','2','3'};
		
		
		File aBookFile = new File(rootPath + File.separator + "abook.zip");
		
		try {
			VCardProtector.protectVCF(aBookFile, aBookFileTMP, pass);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(aBookFileTMP.exists())
		{
			aBookFileTMP.delete();
		}
		
	}
	
	public void importContacts()
	{
		File[] files = ContextCompat.getExternalFilesDirs(this.context, null);
				
		String rootPath = files[0].getAbsolutePath();
		
		AbstractIdentityManager idm = IdentityManagerAndroid
				.getInstance(context);
		
		File aBookFileTMP = new File(rootPath + File.separator + "abookTMP.vcf");
		
		File sourceFile = new File(rootPath + File.separator + "abook.zip");
		/*		try {
		boolean hashMatch;
			hashMatch = VCardProtector.unwrapVCF(sourceFile, aBookFileTMP, new char[]{'6','w','2','y','r','a'});
			System.out.println("Hash match?: " + hashMatch);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	*/	
		//import example without password check
		try {
			try {
				VCardProtector.unwrapVCF(sourceFile, aBookFileTMP);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			idm.getAddressBookManager().importContacts(identity, aBookFileTMP);
			idm.getAddressBookManager().importContacts(identity, aBookFileTMP, true);
		} catch (ContactExistsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(aBookFileTMP.exists())
		{
			aBookFileTMP.delete();
		}
	}

}
