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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.LinkedList;

import org.panbox.core.crypto.KeyConstants;
import org.panbox.core.identitymgmt.AbstractAddressbookManager;
import org.panbox.core.identitymgmt.AbstractIdentity;
import org.panbox.core.identitymgmt.AbstractIdentityManager;
import org.panbox.core.identitymgmt.CloudProviderInfo;
import org.panbox.core.identitymgmt.IAddressbook;
import org.panbox.core.identitymgmt.Identity;
import org.panbox.mobile.android.utils.AndroidSettings;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class IdentityManagerAndroid extends AbstractIdentityManager {

	private static IdentityManagerAndroid idm = null;
//	private Context ctx = null;
	private static IdentityDBHelper idbh = null;
	private SQLiteDatabase db = null;
	
	private IdentityManagerAndroid() {
	}
	
//	private IdentityManagerAndroid(Context ctx) {
//		this.ctx = ctx;
//	}

	public static AbstractIdentityManager getInstance(Context ctx) {
		if (idm == null) {
			idbh = new IdentityDBHelper(ctx);
			idm = new IdentityManagerAndroid();
		}
		return idm;
	}

	@Override
	public void init(AbstractAddressbookManager aBooMgr) {
		setAddressBookManager(aBooMgr);
	}

	@Override
	public void storeMyIdentity(AbstractIdentity id) {

		// long existingID = identityExists();
		long existingID = id.getID();
		System.out.println("Store: existing id= " + existingID);

		db = idbh.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(AbstractIdentityManager.COLUMN_FirstName, id.getFirstName());
		values.put(AbstractIdentityManager.COLUMN_Name, id.getName());
		values.put(AbstractIdentityManager.COLUMN_Email, id.getEmail());
		values.put(AbstractIdentityManager.COLUMN_KeystorePath, AndroidSettings
				.getInstance().getKeystorePath());

		if (existingID < 0) {
			existingID = db.insert(AbstractIdentityManager.TABLE_IDENTITY,
					null, values);
			id.setID((int) existingID);
		} else {
			db.update(AbstractIdentityManager.TABLE_IDENTITY, values, "_id=?",
					new String[] { String.valueOf(existingID) });
		}

		for (CloudProviderInfo cpi : id.getCloudProviders().values()) {

			// String q = "select * from "
			// + AbstractIdentityManager.TABLE_CLOUDPROVIDER + ", " +
			// AbstractIdentityManager.TABLE_CLOUDPROVIDER_MAP
			// + " where " + AbstractIdentityManager.TABLE_CLOUDPROVIDER_MAP
			// + ".identityID=\"" + existingID + "\" and "
			// + AbstractIdentityManager.TABLE_CLOUDPROVIDER_MAP +
			// ".cloudProviderID = "
			// + AbstractIdentityManager.TABLE_CLOUDPROVIDER +
			// "."+AbstractIdentityManager.COLUMN_ID + " and "
			// + AbstractIdentityManager.TABLE_CLOUDPROVIDER +
			// "."+AbstractIdentityManager.COLUMN_Name + "=\""
			// + cpi.getProviderName()+"\"";
			//
			// Cursor cur = db.rawQuery(q, null);
			// cur.moveToFirst();

			ContentValues cpiTableVals = new ContentValues();
			cpiTableVals.put(AbstractIdentityManager.COLUMN_Username,
					cpi.getUsername());
			cpiTableVals.put(AbstractIdentityManager.COLUMN_Name,
					cpi.getProviderName());

			// if (cur.getCount() == 0)
			if (cpi.getId() < 0) {
				// System.out.println("cpi not found -> adding it: " +
				// cpi.getProviderName() + " " + cpi.getUsername());
				long insertCpi = db.insert(
						AbstractIdentityManager.TABLE_CLOUDPROVIDER, null,
						cpiTableVals);

				cpi.setId((int) insertCpi);

				ContentValues cpiMapTableVals = new ContentValues();
				cpiMapTableVals.put(AbstractIdentityManager.COLUMN_IdentityId,
						existingID);
				cpiMapTableVals.put(
						AbstractIdentityManager.COLUMN_CloudproviderId,
						insertCpi);

				db.insert(AbstractIdentityManager.TABLE_CLOUDPROVIDER_MAP,
						null, cpiMapTableVals);
			} else {
				// long cpiIDinDB = cur.getLong(0);
				// System.out.println("cpi found: update ID: " + cpiIDinDB +
				// " with values: " + cpi.getProviderName() + " " +
				// cpi.getUsername());

				db.update(AbstractIdentityManager.TABLE_CLOUDPROVIDER,
						cpiTableVals, "_id=?",
						new String[] { String.valueOf(cpi.getId()) });
			}

			// cur.close();
		}

		// walk through cloud provider infos in db an delete those that are not
		// part of the current identity anymore
		Cursor cur = db.rawQuery("select * from "
				+ AbstractIdentityManager.TABLE_CLOUDPROVIDER + ", "
				+ AbstractIdentityManager.TABLE_CLOUDPROVIDER_MAP + " where "
				+ AbstractIdentityManager.TABLE_CLOUDPROVIDER_MAP
				+ ".identityID=\"" + existingID + "\" and "
				+ AbstractIdentityManager.TABLE_CLOUDPROVIDER_MAP
				+ ".cloudProviderID = "
				+ AbstractIdentityManager.TABLE_CLOUDPROVIDER + "."
				+ AbstractIdentityManager.COLUMN_ID, null);

		cur.moveToFirst();
		LinkedList<Long> toBeRemoved = new LinkedList<Long>();
		while (!cur.isAfterLast()) {

			long cpiID = cur.getLong(0);

			boolean found = false;
			for (CloudProviderInfo cpi : id.getCloudProviders().values()) {
				if (cpi.getId() == cpiID) {
					found = true;
					break;
				}
			}

			if (!found) {
				toBeRemoved.add(cpiID);
			}

			cur.moveToNext();
		}
		cur.close();

		for (long cpiID : toBeRemoved) {
			db.delete(AbstractIdentityManager.TABLE_CLOUDPROVIDER, "_id=?",
					new String[] { String.valueOf(cpiID) });
			db.delete(AbstractIdentityManager.TABLE_CLOUDPROVIDER_MAP,
					AbstractIdentityManager.COLUMN_CloudproviderId + "=?",
					new String[] { String.valueOf(cpiID) });
		}

		// TODO int vs long on id
		getAddressBookManager().persistContacts(id.getAddressbook().getContacts(),
				(int) existingID);

		// store keystore with private keys in file
		File keyStoreFile = new File(AndroidSettings.getInstance().getKeystorePath());

		FileOutputStream fos;
		try {
			fos = new FileOutputStream(keyStoreFile);

			Identity idCast = (Identity) id;
			idCast.getKeyStore()
					.store(fos, KeyConstants.OPEN_KEYSTORE_PASSWORD);

			fos.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public AbstractIdentity loadMyIdentity(IAddressbook addressbook) {

		AndroidSettings pbSettings = AndroidSettings.getInstance();

		Identity id = new Identity(addressbook);

		db = idbh.getReadableDatabase();

		//we use the path in panbox settings now
//		String keyStorePath = "";

		Cursor cur = db.query(AbstractIdentityManager.TABLE_IDENTITY,
				AbstractIdentityManager.ALL_IDENTITY_COLUMNS, null, null, null,
				null, null);

		long dbID = -1;

		cur.moveToFirst();
		if(cur.isAfterLast()) {
			return null;
		}
		while (!cur.isAfterLast()) {
			dbID = cur.getLong(0);

			id.setName(cur.getString(1));
			id.setFirstName(cur.getString(2));
			id.setEmail(cur.getString(3));

			//we use the path in panbox settings now
//			keyStorePath = cur.getString(4);

			cur.moveToNext();
		}
		// make sure to close the cursor
		cur.close();

		// load cloudprovider infos
		cur = db.rawQuery("select * from "
				+ AbstractIdentityManager.TABLE_CLOUDPROVIDER + ", "
				+ AbstractIdentityManager.TABLE_CLOUDPROVIDER_MAP + " where "
				+ AbstractIdentityManager.TABLE_CLOUDPROVIDER_MAP
				+ ".identityID=\"" + dbID + "\" and "
				+ AbstractIdentityManager.TABLE_CLOUDPROVIDER_MAP
				+ ".cloudProviderID = "
				+ AbstractIdentityManager.TABLE_CLOUDPROVIDER + "."
				+ AbstractIdentityManager.COLUMN_ID, null);

		cur.moveToFirst();
		while (!cur.isAfterLast()) {

			CloudProviderInfo cpi = new CloudProviderInfo(cur.getString(2),
					cur.getString(1));
			cpi.setId((int) cur.getLong(0));

			id.addCloudProvider(cpi);

			cur.moveToNext();
		}
		// make sure to close the cursor
		cur.close();

		id.setID((int) dbID);

		// load contacts
		getAddressBookManager().loadContacts(id);

		// load keystore from file
		KeyStore store = null;
		try {
			store = KeyStore.getInstance(KeyConstants.KEYSTORE_TYPE);
		} catch (KeyStoreException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		File keystoreFile = new File(pbSettings.getKeystorePath());
		
		Log.v("IDMA:", "keystoreFile: " + keystoreFile);

		if (keystoreFile.exists()) {
			FileInputStream fis;
			try {
				fis = new FileInputStream(keystoreFile);
				store.load(fis, KeyConstants.OPEN_KEYSTORE_PASSWORD);
				fis.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CertificateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			id.setKeyStore(store);
		} else {
			// TODO: error handling
			System.err.println("Could not find keystore file");
		}

		return id;
	}

	// private long identityExists()
	// {
	// db = idbh.getReadableDatabase();
	//
	// Cursor cur = db.query(AbstractIdentityManager.TABLE_IDENTITY,
	// AbstractIdentityManager.ALL_IDENTITY_COLUMNS, null, null, null, null,
	// null);
	//
	// if (cur.getCount() == 0)
	// {
	// return -1;
	// }
	// else if(cur.getCount() > 1)
	// {
	// //TODO: error handling
	// System.err.println("More than one Identity found -> Database corrupted");
	// }
	//
	// long dbID = -1;
	//
	// cur.moveToFirst();
	// // while (!cur.isAfterLast()) {
	// dbID = cur.getLong(0);
	//
	// // cur.moveToNext();
	// // }
	// // make sure to close the cursor
	// cur.close();
	//
	// return dbID;
	//
	// }

}
