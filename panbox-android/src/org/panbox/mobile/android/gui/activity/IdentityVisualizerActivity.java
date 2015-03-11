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

import org.panbox.core.Utils;
import org.panbox.core.crypto.CryptCore;
import org.panbox.core.identitymgmt.AbstractIdentity;
import org.panbox.mobile.android.R;
import org.panbox.mobile.android.gui.data.PanboxManager;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class IdentityVisualizerActivity extends CustomActionBarActivity {
	
	private static final String TAG = "IdentityVisualizerActivity:";
//	private static final CharSequence dateFormat = "yyyy-MM-dd hh:mm:ss";
	
	private Identicon identiconSig;
	private Identicon identiconEnc;
//	private TextView infoField;
	
	private TableLayout table;
	private TableRow row1;
	private TableRow row2;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		Log.v(TAG, "onCreate()");
		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.pb_identity_visualizer);
		getActionBar().show();

		context = getApplicationContext();
		panbox = PanboxManager.getInstance(context);
//		highlightActionbarItem(NFC_ACTIVITY);
		
		table = (TableLayout)findViewById(R.id.pb_nfc_receive_info_table);
		
		row1 = (TableRow)View.inflate(context,R.layout.pb_nfc_receiver_info_table_row, null);
		row2 = (TableRow)View.inflate(context,R.layout.pb_nfc_receiver_info_table_row, null);
		row1.setTag(1);
		row2.setTag(2);
		
		table.addView(row1);
		table.addView(row2);
		
		identiconSig = (Identicon) findViewById(R.id.pb_identityHashViewSig);
		identiconEnc = (Identicon) findViewById(R.id.pb_identityHashViewEnc);
		
		//infoField = (TextView) findViewById(R.id.pb_identityInfoTxt);
		
		AbstractIdentity myID = panbox.getIdentity();
		
		byte[] bytesEnc = CryptCore.getPublicKeyfingerprint(myID.getCertEnc().getPublicKey());
		byte[] bytesSig = CryptCore.getPublicKeyfingerprint(myID.getCertSign().getPublicKey());
		
		String hexEnc = Utils.bytesToHex(bytesEnc);
		String hexSig = Utils.bytesToHex(bytesSig);
		
		identiconEnc.show(hexEnc);
		identiconSig.show(hexSig);
		
		((TextView)row1.findViewById(R.id.pb_nfc_receive_table_column1)).setText(getString(R.string.pb_name));
		((TextView)row1.findViewById(R.id.pb_nfc_receive_table_column2)).setText(myID.getFirstName());
		
		((TextView)row2.findViewById(R.id.pb_nfc_receive_table_column1)).setText(getString(R.string.pb_email));
		((TextView)row2.findViewById(R.id.pb_nfc_receive_table_column2)).setText(myID.getEmail());
		
//		StringBuilder sb = new StringBuilder();
//		sb.append("Name:\t\t\t" + myID.getFirstName() + " " + myID.getName() + "\n");
//		sb.append("Email:\t\t\t" + myID.getEmail() + "\n");
//		
//		sb.append("SignCert:\t\t"
//				+ DateFormat.format(dateFormat, myID.getCertSign()
//						.getNotAfter()) + "\n");
//		sb.append("EncCert:\t\t"
//				+ DateFormat.format(dateFormat, myID.getCertEnc()
//						.getNotAfter()) + "\n");
//		
//		infoField.setText(sb.toString());
		
	}

}
