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
package org.panbox.mobile.android.gui.activity;

import im.delight.android.identicons.Identicon;

import java.util.List;

import org.panbox.core.Utils;
import org.panbox.core.crypto.CryptCore;
import org.panbox.core.identitymgmt.AbstractAddressbookManager;
import org.panbox.core.identitymgmt.AbstractIdentity;
import org.panbox.core.identitymgmt.PanboxContact;
import org.panbox.core.identitymgmt.exceptions.ContactExistsException;
import org.panbox.mobile.android.R;
import org.panbox.mobile.android.gui.data.PanboxManager;
import org.panbox.mobile.android.identitymgmt.IdentityManagerAndroid;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class NFCReceiverActivity extends CustomActionBarActivity {

	private static final CharSequence dateFormat = "yyyy-MM-dd hh:mm:ss";

	private static final String TAG = "NFCReceiverActivity:";

	private PanboxContact receivedContact = null;
	private AbstractIdentity myID;

//	private TextView infoField;
	private TextView msgField;

	private Button importBtn;
	private Button cancelBtn;

	private Identicon identiconSig;
	private Identicon identiconEnc;
	
	private TableLayout table;
	private TableRow row1;
	private TableRow row2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.pb_nfc_receiver_view);
		getActionBar().show();

		context = getApplicationContext();
		panbox = PanboxManager.getInstance(context);
		
		table = (TableLayout)findViewById(R.id.pb_nfc_receive_info_table);
		
		row1 = (TableRow)View.inflate(context,R.layout.pb_nfc_receiver_info_table_row, null);
		row2 = (TableRow)View.inflate(context,R.layout.pb_nfc_receiver_info_table_row, null);
		row1.setTag(1);
		row2.setTag(2);
		
		table.addView(row1);
		table.addView(row2);
		
		// highlightActionbarItem(NFC_ACTIVITY);
		
		//infoField = (TextView) findViewById(R.id.pb_nfcReceiverInfoTxt);
		msgField = (TextView) findViewById(R.id.pb_nfc_receive_noticeTxt);

		importBtn = (Button) findViewById(R.id.pb_nfc_receiver_okBtn);
		cancelBtn = (Button) findViewById(R.id.pb_nfc_receiver_cancelBtn);

		identiconSig = (Identicon) findViewById(R.id.pb_nfcReceiverHashViewSig);
		identiconEnc = (Identicon) findViewById(R.id.pb_nfcReceiverHashViewEnc);

		toogleButtons(View.GONE);

		myID = panbox.getIdentity();
		Log.v(TAG, "on processNFC() fetched Identity: " + myID);

		importBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				Log.v(TAG, "Clicked OK on import");
				doImport();

				toogleButtons(View.GONE);
				table.setVisibility(View.GONE);
				//infoField.setVisibility(View.GONE);
				msgField.setVisibility(View.GONE);

				disableMyself(false);
			}
		});

		cancelBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				Log.v(TAG, "Clicked Cancel on import");
				toogleButtons(View.GONE);
				table.setVisibility(View.GONE);
				//infoField.setVisibility(View.GONE);
				msgField.setVisibility(View.GONE);

				disableMyself(false);
			}
		});

	}

	@Override
	protected void onNewIntent(Intent intent) {
		Log.v(TAG, "onNewIntent()");

		// onResume gets called after this to handle the intent
		setIntent(intent);
	}

	@Override
	protected void onResume() {
		Log.v(TAG, "onResume()");
		super.onResume();

		// security problem: ANY app could send this intent, not only
		// another phone through NFC -> we activate this receiver only for a
		// certain amount of time

		// Check to see that the Activity started due to an Android Beam
		Intent i = getIntent();
		// if (active &&
		// NfcAdapter.ACTION_NDEF_DISCOVERED.equals(i.getAction())) {
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(i.getAction())) {

			if (!(i.hasExtra("status") && i.getIntExtra("status", 0) == 1)) {
				Log.v(TAG, "on Resume() got intent");

				// reset received Contact
				receivedContact = null;
				processNFC(i);
			}
		}
	}

	private void processNFC(Intent intent) {
		Parcelable[] rawMsgs = intent
				.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		// only one message sent during the beam
		NdefMessage msg = (NdefMessage) rawMsgs[0];
		// record 0 contains the MIME type, record 1 is the AAR, if present

		byte[] rawBytes = msg.getRecords()[0].getPayload();

		// mark intent as processed
		intent.putExtra("status", 1);
		Log.v(TAG, "on processNFC() set status on intent to 1");

		List<PanboxContact> importContacts = AbstractAddressbookManager
				.vcardBytes2Contacts(rawBytes, true);
		receivedContact = importContacts.get(0);

		showReceivedData();

		toogleButtons(View.VISIBLE);

		Log.v(TAG, "processed");
	}

	private void showReceivedData() {
		if (null == receivedContact) {
			Log.e(TAG, "showReceivedData(): Did not receive a contact yet");
			return;
		}

		((TextView)row1.findViewById(R.id.pb_nfc_receive_table_column1)).setText(getString(R.string.pb_name));
		((TextView)row1.findViewById(R.id.pb_nfc_receive_table_column2)).setText(receivedContact.getFirstName());
		
		((TextView)row2.findViewById(R.id.pb_nfc_receive_table_column1)).setText(getString(R.string.pb_email));
		((TextView)row2.findViewById(R.id.pb_nfc_receive_table_column2)).setText(receivedContact.getEmail());
		
//		StringBuilder sb = new StringBuilder();
//		sb.append("Name:\t\t\t" + receivedContact.getFirstName() + " "
//				+ receivedContact.getName() + "\n");
//		sb.append("Email:\t\t\t" + receivedContact.getEmail() + "\n");
//
//		sb.append("SignCert:\t\t"
//				+ DateFormat.format(dateFormat, receivedContact.getCertSign()
//						.getNotAfter()) + "\n");
//		sb.append("EncCert:\t\t"
//				+ DateFormat.format(dateFormat, receivedContact.getCertEnc()
//						.getNotAfter()) + "\n");

		byte[] bytesEnc = CryptCore.getPublicKeyfingerprint(receivedContact
				.getCertEnc().getPublicKey());
		byte[] bytesSig = CryptCore.getPublicKeyfingerprint(receivedContact
				.getCertSign().getPublicKey());

		String hexEnc = Utils.bytesToHex(bytesEnc);
		String hexSig = Utils.bytesToHex(bytesSig);

//		infoField.setText(sb.toString());

		identiconSig.show(hexSig);
		identiconEnc.show(hexEnc);
	}

	private void doImport() {
		try {
			// importContacts =
			// IdentityManagerAndroid.getInstance(context).getAddressBookManager()
			// .importContacts(myID, rawBytes, true);

			myID.getAddressbook().addContact(receivedContact);

			Log.v(TAG, "on processNFC() imported contacts");

		} catch (ContactExistsException e) {
			Log.v(TAG, "on processNFC() contact exists exception");

			StringBuilder sb = new StringBuilder();
			for (PanboxContact pc : e.getContacts()) {
				sb.append(pc.getFirstName() + " " + pc.getName() + " "
						+ pc.getEmail() + "\n");
				sb.append("SignCert: "
						+ DateFormat.format(dateFormat, pc.getCertSign()
								.getNotAfter()));
			}

			Toast.makeText(this,
					getString(R.string.pb_contact_existsToast) + ":\n" + sb,
					Toast.LENGTH_LONG).show();

			return;
		}

		IdentityManagerAndroid.getInstance(context).storeMyIdentity(myID);

		Log.v(TAG, "on processNFC() identity stored");

		Toast.makeText(this,
				getString(R.string.pb_contact_import_successToast),
				Toast.LENGTH_LONG).show();
	}

	private void disableMyself(boolean kill) {
		PackageManager pm = getPackageManager();
		if (!kill) {
			pm.setComponentEnabledSetting(
					new ComponentName(
							this,
							org.panbox.mobile.android.gui.activity.NFCReceiverActivity.class),
					PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
		} else {
			pm.setComponentEnabledSetting(
					new ComponentName(
							this,
							org.panbox.mobile.android.gui.activity.NFCReceiverActivity.class),
					PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
		}

	}

	private void toogleButtons(int visibility) {
		importBtn.setVisibility(visibility);
		cancelBtn.setVisibility(visibility);
		identiconSig.setVisibility(visibility);
		identiconEnc.setVisibility(visibility);
	}

}
