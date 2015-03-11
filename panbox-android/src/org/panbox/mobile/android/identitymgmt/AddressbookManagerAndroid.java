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

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.panbox.core.crypto.CryptCore;
import org.panbox.core.identitymgmt.AbstractAddressbookManager;
import org.panbox.core.identitymgmt.AbstractIdentity;
import org.panbox.core.identitymgmt.CloudProviderInfo;
import org.panbox.core.identitymgmt.PanboxContact;
import org.panbox.core.identitymgmt.exceptions.ContactExistsException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.RawContacts.Entity;
import android.provider.ContactsContract.Settings;
import android.text.format.DateFormat;
import android.util.Base64;

public class AddressbookManagerAndroid extends AbstractAddressbookManager {

	private final String accountName = "Panbox";
	private final String accountType = "org.panbox";

	private final CharSequence dateFormat = "yyyy-MM-dd hh:mm:ss";

	private ContentResolver cr = null;
	// private Context context = null;

	private AccountManager am = null;
	private Account panboxAccount = null;

	public AddressbookManagerAndroid(Context context,
			ContentResolver contentResolver) {
		this.cr = contentResolver;
		// this.context = context;

		am = AccountManager.get(context);

		panboxAccount = new Account(accountName, accountType);
		am.addAccountExplicitly(panboxAccount, null, null);

		ContentProviderClient client = contentResolver
				.acquireContentProviderClient(ContactsContract.AUTHORITY_URI);
		ContentValues values = new ContentValues();
		values.put(ContactsContract.Groups.ACCOUNT_NAME, accountName);
		values.put(Groups.ACCOUNT_TYPE, accountType);
		values.put(Settings.UNGROUPED_VISIBLE, true);
		values.put(Settings.SHOULD_SYNC, false);
		try {
			client.insert(
					Settings.CONTENT_URI
							.buildUpon()
							.appendQueryParameter(
									ContactsContract.CALLER_IS_SYNCADAPTER,
									"true").build(), values);
		} catch (RemoteException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void init() {
		// TODO Auto-generated method stub

	}

	@Override
	public void loadContacts(AbstractIdentity identity) {

		Uri rawUri = RawContacts.CONTENT_URI.buildUpon()
				.appendQueryParameter(RawContacts.ACCOUNT_NAME, accountName)
				.appendQueryParameter(RawContacts.ACCOUNT_TYPE, accountType)
				.build();

		LinkedList<Integer> deletedContacts = new LinkedList<Integer>();

		Cursor items = null;
		try {
			items = cr.query(rawUri, null, null, null, null);

			if (items.getCount() > 0) {
				items.moveToFirst();

				do {
					// System.out.print("Load(raw): Creating contact: ");
					PanboxContact contact = new PanboxContact();

					int idxID = items
							.getColumnIndex(ContactsContract.RawContacts._ID);
					int id = items.getInt(idxID);
					contact.setID(id); // set contact ID to its id in the
										// RAW_CONTACTS table

					// System.out.println("RawID: " + id);

					int idxDeleted = items
							.getColumnIndex(ContactsContract.RawContacts.DELETED);
					int deleted = items.getInt(idxDeleted);
					if (deleted == 1) {
						deletedContacts.add(id);
					} else {
						Uri rawContactUri = ContentUris.withAppendedId(
								RawContacts.CONTENT_URI, id);
						Uri entityUri = Uri.withAppendedPath(rawContactUri,
								Entity.CONTENT_DIRECTORY);
						Cursor c = cr.query(entityUri, new String[] {
								RawContacts.SOURCE_ID, Entity.DATA_ID,
								Entity.MIMETYPE, Entity.DATA1, Entity.DATA2,
								Entity.DATA3 }, null, null, null);
						try {
							while (c.moveToNext()) {
								// String sourceId = c.getString(0);
								if (!c.isNull(1)) {
									long dataID = c.getLong(1);
									String mimeType = c.getString(2);
									String data = c.getString(3);
									String data2 = c.getString(4);
									String data3 = c.getString(5);

									if ("vnd.android.cursor.item/panbox-sign-cert"
											.equals(mimeType)) {
										X509Certificate sigCert = CryptCore
												.createCertificateFromBytes(Base64
														.decode(data,
																Base64.DEFAULT));
										// System.out.println("Cert: " +
										// sigCert.toString());
										contact.setCertSign(sigCert);

									} else if ("vnd.android.cursor.item/panbox-enc-cert"
											.equals(mimeType)) {
										X509Certificate encCert = CryptCore
												.createCertificateFromBytes(Base64
														.decode(data,
																Base64.DEFAULT));
										// System.out.println("Cert: " +
										// encCert.toString());
										contact.setCertEnc(encCert);
									} else if ("vnd.android.cursor.item/panbox-trustLevel"
											.equals(mimeType)) {
										contact.setTrustLevel(Integer
												.parseInt(data));
									} else if (CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
											.equals(mimeType)) {
										contact.setFirstName(data2);
										contact.setName(data3);
									} else if (CommonDataKinds.Email.CONTENT_ITEM_TYPE
											.equals(mimeType)) {
										contact.setEmail(data);
									} else if ("vnd.android.cursor.item/panbox-cpi"
											.equals(mimeType)) {
										CloudProviderInfo cpi = new CloudProviderInfo(
												data, data2);
										cpi.setId((int) dataID); // set
																	// cloudprovider
																	// id
																	// to its id
																	// in DATA
																	// TABLE

										contact.addCloudProvider(cpi);
									}
								}
							}
						} finally {
							c.close();
						}
						// System.out.println("Add contact to addressbook: " +
						// contact);
						try {
							identity.getAddressbook().addContact(contact);
						} catch (ContactExistsException e) {
							// should not happen here, otherwise DB is corrupted
						}
					}

				} while (items.moveToNext());
			}
		} finally {
			if (null != items) {
				items.close();
			}
		}

		// remove contacts marked as deleted (due to other apps, i.e. default
		// contacts app of android
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

		for (int id : deletedContacts) {
			Uri rawContactUri = ContentUris.withAppendedId(
					RawContacts.CONTENT_URI, id);

			ops.add(ContentProviderOperation.newDelete(
					rawContactUri
							.buildUpon()
							.appendQueryParameter(
									ContactsContract.CALLER_IS_SYNCADAPTER,
									"true").build()).build());
		}

		try {
			cr.applyBatch(ContactsContract.AUTHORITY, ops);

		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OperationApplicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void persistContacts(Collection<PanboxContact> contacts,
			int identityKey) {

		// we ignore the identityKey id because all contacts of account type
		// org.panbox belong to our id

		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

		for (PanboxContact contact : contacts) {
			long rawContactIDinDB = contact.getID();

			System.out.println("Rawcontact in DB: " + rawContactIDinDB);

			if (rawContactIDinDB > 0) // Update contact
			{
				System.out.println("contact exists: updating it");

				ops.add(ContentProviderOperation
						.newUpdate(Data.CONTENT_URI)
						.withSelection(
								Data.RAW_CONTACT_ID + "=? and " + Data.MIMETYPE
										+ "=?",
								new String[] {
										String.valueOf(rawContactIDinDB),
										StructuredName.CONTENT_ITEM_TYPE })
						.withValue(
								StructuredName.DISPLAY_NAME,
								contact.getFirstName() + " "
										+ contact.getName())
						.withValue(StructuredName.FAMILY_NAME,
								contact.getName())
						.withValue(StructuredName.GIVEN_NAME,
								contact.getFirstName()).build());

				ops.add(ContentProviderOperation
						.newUpdate(Data.CONTENT_URI)
						.withSelection(
								Data.RAW_CONTACT_ID + "=? and " + Data.MIMETYPE
										+ "=?",
								new String[] {
										String.valueOf(rawContactIDinDB),
										Email.CONTENT_ITEM_TYPE })
						.withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
						.withValue(Email.ADDRESS, contact.getEmail()).build());

				ops.add(ContentProviderOperation
						.newUpdate(Data.CONTENT_URI)
						.withSelection(
								Data.RAW_CONTACT_ID + "=? and " + Data.MIMETYPE
										+ "=?",
								new String[] {
										String.valueOf(rawContactIDinDB),
										"vnd.android.cursor.item/panbox-trustLevel" })
						.withValue(Data.MIMETYPE,
								"vnd.android.cursor.item/panbox-trustLevel")
						.withValue(Data.DATA1, contact.getTrustLevel())
						.withValue(Data.DATA2,
								"TrustLevel: " + contact.getTrustLevel())
						.build());

				try {
					ops.add(ContentProviderOperation
							.newUpdate(Data.CONTENT_URI)
							.withSelection(
									Data.RAW_CONTACT_ID + "=? and "
											+ Data.MIMETYPE + "=?",
									new String[] {
											String.valueOf(rawContactIDinDB),
											"vnd.android.cursor.item/panbox-sign-cert" })
							.withValue(Data.MIMETYPE,
									"vnd.android.cursor.item/panbox-sign-cert")
							.withValue(
									Data.DATA1,
									Base64.encodeToString(contact.getCertSign()
											.getEncoded(), Base64.DEFAULT))
							.withValue(
									Data.DATA2,
									"SignCert: "
											+ DateFormat.format(dateFormat,
													contact.getCertSign()
															.getNotAfter()))
							.build());

					ops.add(ContentProviderOperation
							.newUpdate(Data.CONTENT_URI)
							.withSelection(
									Data.RAW_CONTACT_ID + "=? and "
											+ Data.MIMETYPE + "=?",
									new String[] {
											String.valueOf(rawContactIDinDB),
											"vnd.android.cursor.item/panbox-enc-cert" })
							.withValue(Data.MIMETYPE,
									"vnd.android.cursor.item/panbox-enc-cert")
							.withValue(
									Data.DATA1,
									Base64.encodeToString(contact.getCertEnc()
											.getEncoded(), Base64.DEFAULT))
							.withValue(
									Data.DATA2,
									"EncCert: "
											+ DateFormat.format(dateFormat,
													contact.getCertEnc()
															.getNotAfter()))
							.build());

				} catch (CertificateEncodingException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				// update cloudprovider information
				for (Entry<String, CloudProviderInfo> cpiEntry : contact
						.getCloudProviders().entrySet()) {

					CloudProviderInfo cpi = cpiEntry.getValue();
					if (cpi.getId() > 0) // update
					{

						ops.add(ContentProviderOperation
								.newUpdate(Data.CONTENT_URI)
								.withSelection(
										Data.RAW_CONTACT_ID + "=? and "
												+ Data.MIMETYPE + "=?",
										new String[] {
												String.valueOf(cpi.getId()),
												"vnd.android.cursor.item/panbox-cpi" })
								.withValue(Data.MIMETYPE,
										"vnd.android.cursor.item/panbox-cpi")
								.withValue(Data.DATA1, cpiEntry.getKey())
								.withValue(Data.DATA2,
										cpiEntry.getValue().getUsername())
								.withValue(
										Data.DATA3,
										"CPI: "
												+ cpiEntry.getKey()
												+ " ("
												+ cpiEntry.getValue()
														.getUsername() + ")")
								.build());
					} else {
						// insert
						ops.add(ContentProviderOperation
								.newInsert(Data.CONTENT_URI)
								.withValue(Data.RAW_CONTACT_ID,
										rawContactIDinDB)
								.withValue(Data.MIMETYPE,
										"vnd.android.cursor.item/panbox-cpi")
								.withValue(Data.DATA1, cpiEntry.getKey())
								.withValue(Data.DATA2,
										cpiEntry.getValue().getUsername())
								.withValue(
										Data.DATA3,
										"CPI: "
												+ cpiEntry.getKey()
												+ " ("
												+ cpiEntry.getValue()
														.getUsername() + ")")
								.build());
					}
				}

				// walk trough db cloudprovider and remove those that are not
				// part of the contact anymore
				Uri rawContactUri = ContentUris.withAppendedId(
						RawContacts.CONTENT_URI, rawContactIDinDB);
				Uri entityUri = Uri.withAppendedPath(rawContactUri,
						Entity.CONTENT_DIRECTORY);
				Cursor c = cr.query(entityUri, new String[] { Entity.DATA_ID,
						Entity.MIMETYPE }, Entity.MIMETYPE + "=?",
						new String[] { "vnd.android.cursor.item/panbox-cpi" },
						null);

				while (c.moveToNext()) {

					boolean found = false;
					long dataID = c.getLong(0);

					for (Entry<String, CloudProviderInfo> cpiEntry : contact
							.getCloudProviders().entrySet()) {

						CloudProviderInfo cpi = cpiEntry.getValue();
						if (cpi.getId() == dataID) {
							found = true;
							break;
						}
					}
					if (!found) {
						ops.add(ContentProviderOperation.newDelete(
								ContentUris.withAppendedId(Data.CONTENT_URI,
										dataID)).build());
					}
				}
				c.close();

				try {
					ContentProviderResult[] res = cr.applyBatch(
							ContactsContract.AUTHORITY, ops);

					if (res.length > 0) {
						// long contactID = ContentUris.parseId(res[0].uri);
						// contact.setID((int)contactID);
						// System.out.println("updated contact with ID: " +
						// contactID);
						System.out.println("updated existing contact");
					}

					ops.clear();

				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (OperationApplicationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else { // insert

				int rawContactInsertIndex = ops.size();
				ops.add(ContentProviderOperation
						.newInsert(RawContacts.CONTENT_URI)
						.withValue(RawContacts.ACCOUNT_TYPE, accountType)
						.withValue(RawContacts.ACCOUNT_NAME, accountName)
						.build());

				ops.add(ContentProviderOperation
						.newInsert(Data.CONTENT_URI)
						.withValueBackReference(Data.RAW_CONTACT_ID,
								rawContactInsertIndex)
						.withValue(Data.MIMETYPE,
								StructuredName.CONTENT_ITEM_TYPE)
						// .withValue(Data.IS_READ_ONLY, 1)
						.withValue(
								StructuredName.DISPLAY_NAME,
								contact.getFirstName() + " "
										+ contact.getName())
						.withValue(StructuredName.GIVEN_NAME,
								contact.getFirstName())
						.withValue(StructuredName.FAMILY_NAME,
								contact.getName()).build());

				ops.add(ContentProviderOperation
						.newInsert(Data.CONTENT_URI)
						.withValueBackReference(Data.RAW_CONTACT_ID,
								rawContactInsertIndex)
						// .withValue(Data.IS_READ_ONLY, 1)
						.withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
						.withValue(Email.ADDRESS, contact.getEmail()).build());

				ops.add(ContentProviderOperation
						.newInsert(Data.CONTENT_URI)
						.withValueBackReference(Data.RAW_CONTACT_ID,
								rawContactInsertIndex)
						.withValue(Data.MIMETYPE,
								"vnd.android.cursor.item/panbox-trustLevel")
						.withValue(Data.DATA1, contact.getTrustLevel())
						.withValue(Data.DATA2,
								"TrustLevel: " + contact.getTrustLevel())
						.build());

				try {
					ops.add(ContentProviderOperation
							.newInsert(Data.CONTENT_URI)
							.withValueBackReference(Data.RAW_CONTACT_ID,
									rawContactInsertIndex)
							.withValue(Data.MIMETYPE,
									"vnd.android.cursor.item/panbox-sign-cert")
							.withValue(
									Data.DATA1,
									Base64.encodeToString(contact.getCertSign()
											.getEncoded(), Base64.DEFAULT))
							.withValue(
									Data.DATA2,
									"SignCert: "
											+ DateFormat.format(dateFormat,
													contact.getCertSign()
															.getNotAfter()))
							.build());

					ops.add(ContentProviderOperation
							.newInsert(Data.CONTENT_URI)
							.withValueBackReference(Data.RAW_CONTACT_ID,
									rawContactInsertIndex)
							.withValue(Data.MIMETYPE,
									"vnd.android.cursor.item/panbox-enc-cert")
							.withValue(
									Data.DATA1,
									Base64.encodeToString(contact.getCertEnc()
											.getEncoded(), Base64.DEFAULT))
							.withValue(
									Data.DATA2,
									"EncCert: "
											+ DateFormat.format(dateFormat,
													contact.getCertEnc()
															.getNotAfter()))
							.build());

				} catch (CertificateEncodingException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				// store cloudprovider information
				for (Entry<String, CloudProviderInfo> cpiEntry : contact
						.getCloudProviders().entrySet()) {
					ops.add(ContentProviderOperation
							.newInsert(Data.CONTENT_URI)
							.withValueBackReference(Data.RAW_CONTACT_ID,
									rawContactInsertIndex)
							.withValue(Data.MIMETYPE,
									"vnd.android.cursor.item/panbox-cpi")
							.withValue(Data.DATA1, cpiEntry.getKey())
							.withValue(Data.DATA2,
									cpiEntry.getValue().getUsername())
							.withValue(
									Data.DATA3,
									"CPI: " + cpiEntry.getKey() + " ("
											+ cpiEntry.getValue().getUsername()
											+ ")").build());
				}

				try {
					ContentProviderResult[] res = cr.applyBatch(
							ContactsContract.AUTHORITY, ops);

					if (res.length > 0) {
						long contactID = ContentUris.parseId(res[0].uri);
						contact.setID((int) contactID);
						// System.out.println("added contact with ID: " +
						// contactID);
					}

					ops.clear();

				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (OperationApplicationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		ops.clear();

		// walk through raw contacts in db and delete those that are not
		// part of the identity's addressbook anymore

		Uri rawUri = RawContacts.CONTENT_URI.buildUpon()
				.appendQueryParameter(RawContacts.ACCOUNT_NAME, accountName)
				.appendQueryParameter(RawContacts.ACCOUNT_TYPE, accountType)
				.build();

		Cursor items = null;
		try {
			items = cr.query(rawUri, null, null, null, null);

			if (items.getCount() > 0) {
				items.moveToFirst();
				do {
					int idxID = items
							.getColumnIndex(ContactsContract.RawContacts._ID);
					int id = items.getInt(idxID);

					boolean found = false;
					for (PanboxContact contact : contacts) {
						if (contact.getID() == id) {
							found = true;
							break;
						}
					}
					if (!found) {
						Uri rawContactUri = ContentUris.withAppendedId(
								RawContacts.CONTENT_URI, id);

						ops.add(ContentProviderOperation
								.newDelete(
										rawContactUri
												.buildUpon()
												.appendQueryParameter(
														ContactsContract.CALLER_IS_SYNCADAPTER,
														"true").build())
								.build());

					}

				} while (items.moveToNext());
			}

		} finally {
			if(items != null)
			{
				items.close();
			}
		}

		try {
			cr.applyBatch(ContactsContract.AUTHORITY, ops);

		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OperationApplicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
