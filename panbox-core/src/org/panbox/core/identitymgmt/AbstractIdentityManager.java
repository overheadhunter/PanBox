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

import ezvcard.Ezvcard;
import ezvcard.VCard;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public abstract class AbstractIdentityManager {

	public static final String TABLE_ADDRESSBOOK = "addressbook";
	public static final String TABLE_ADDRESSBOOK_MAP = "addressbookMap";
	public static final String TABLE_ADDRESSBOOK_CPI = "addressbookCpi";
	public static final String TABLE_ADDRESSBOOK_CPI_MAP = "addressbookCpiMap";

	//android dbhelper
	public static final String TABLE_IDENTITY = "identity";
	public static final String COLUMN_ID = "_id";

	public static final String COLUMN_Name = "name";
	public static final String COLUMN_FirstName = "firstname";
	public static final String COLUMN_Email = "email";
	public static final String COLUMN_KeystorePath = "keystorePath";

	public static final String[] ALL_IDENTITY_COLUMNS = new String[]{COLUMN_ID, COLUMN_Name, COLUMN_FirstName, COLUMN_Email, COLUMN_KeystorePath};

	public static final String TABLE_CLOUDPROVIDER = "cloudProvider";
	public static final String COLUMN_Username = "username";
	public static final String COLUMN_Password = "password";

	public static final String TABLE_CLOUDPROVIDER_MAP = "cloudProviderMap";
	public static final String COLUMN_IdentityId = "identityID";
	public static final String COLUMN_CloudproviderId = "cloudProviderID";

	private static final Logger logger = Logger
			.getLogger("org.panbox.core");
	
	private AbstractAddressbookManager aBookMgr;

	/**
	 * Creates an initial identity manager state, which is used to store the
	 * identity in it.
	 *
	 * @param aBooMgr Addressbook which should be used for this identity
	 */
	public abstract void init(AbstractAddressbookManager aBooMgr);

	/**
	 * Stores the given Identity the identity manager
	 *
	 * @param id - The Identity to store
	 */
	public abstract void storeMyIdentity(AbstractIdentity id);

	/**
	 * Loads our own Identity from the identity manager
	 *
	 * @param addressbook - Plattform specific Implementation of addressbook
	 * @return - Our own Identity
	 */
	public abstract AbstractIdentity loadMyIdentity(
			IAddressbook addressbook);

	/**
	 * Copies the public values (name, firstname, email) and keys into a contact
	 * and exports it to a vcard file
	 *
	 * @param id   - Identity to be exported
	 * @param file - vcard file to store the public identity
	 * @return true if successful
	 */
	public boolean exportMyIdentity(AbstractIdentity id, File file) {

		FileOutputStream fos;
		try {
			fos = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			logger.error("Could not write identity to file " + file.getAbsolutePath(), e);
			
			return false;
		}
		
		return exportMyIdentity(id, fos);
	}
	
	public boolean exportMyIdentity(AbstractIdentity id, OutputStream os)
	{
		VCard vc = AbstractAddressbookManager.contact2VCard(id);

		try {
			Ezvcard.write(vc).go(os);
		} catch (IOException e) {
			logger.error("Could not write identity to OutputStream", e);

			return false;
		}

		return true;
	}
	
	public AbstractAddressbookManager getAddressBookManager() {
		return this.aBookMgr;
	}
	
	public void setAddressBookManager(AbstractAddressbookManager aBookMgr) {
		this.aBookMgr = aBookMgr;
	}

}
