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
package org.panbox.mobile.android.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.panbox.core.identitymgmt.AbstractAddressbookManager;
import org.panbox.core.identitymgmt.PanboxContact;
import org.panbox.core.identitymgmt.exceptions.ContactExistsException;
import org.panbox.mobile.android.R;
import org.panbox.mobile.android.gui.activity.DirectoryExplorerActivity;
import org.panbox.mobile.android.gui.data.PanboxManager;
import org.panbox.mobile.android.identitymgmt.AddressbookManagerAndroid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import ezvcard.VCard;

public class NoGUIActivity extends Activity implements PINDialog.OnCompleteListener{
	private Context context;
	private PanboxManager panbox;
	private String targetDir;
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
	
		context = getApplicationContext();
		panbox = PanboxManager.getInstance(context);
		
		Bundle bundleReceived = getIntent().getExtras();
		if(bundleReceived.getString("method").equals("import")){
			Intent dirExplorerActivity = new Intent(this,DirectoryExplorerActivity.class);
			
			startActivityForResult(dirExplorerActivity,1);
		} 
		else if(bundleReceived.getString("method").equals("export")){
			Bundle bundleToSend = new Bundle();
			bundleToSend.putString("method", "export");
			Intent dirExplorerActivity = new Intent(this,DirectoryExplorerActivity.class);
			dirExplorerActivity.putExtras(bundleToSend);
			startActivityForResult(dirExplorerActivity,1);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.v("NoGUIActivity:", "in onActivityResult()");
		
		if (resultCode == Activity.RESULT_CANCELED || data == null) {
			Toast.makeText(context, getString(R.string.pb_no_data_passed),
					Toast.LENGTH_LONG).show();
			finish();
		} else if (data.getStringExtra("fileName") != null && !data.getStringExtra("fileName").equals("")) {
			File vcardFile;
			VCard[] contacts;
			String vcardFilePath = data.getStringExtra("fileName");
			if (vcardFilePath != null) {
				vcardFile = new File(vcardFilePath);
				if ((contacts = loadVCardFile(vcardFile)) != null) {
					importContacts(contacts, true); // Android contacts are always verified!
				} else {
					Toast.makeText(context,
							getString(R.string.pb_could_not_read_file),
							Toast.LENGTH_LONG).show();
				}
			} else {
				Toast.makeText(context,
						getString(R.string.pb_could_not_read_file),
						Toast.LENGTH_LONG).show();
			}
			finish();
			
		} else if (data.getStringExtra("targetDir") != null && !data.getStringExtra("targetDir").equals("")) {
				targetDir = data.getStringExtra("targetDir");
				// here call dialog activity
				PINDialog pinDialogFragment = new PINDialog();
				pinDialogFragment.show(getFragmentManager(), "pin");
				//finish();

		}
	}
	public void exportContacts(List<PanboxContact> contacts, File vcardFile) {
		Log.v("NoGUIActvity:", "in exportContacts()");
		Collection<VCard> vcards = new LinkedList<VCard>();
		for (PanboxContact c : contacts) {
			VCard v;
			if (c instanceof PanboxContact) {
				v = AbstractAddressbookManager.contact2VCard((PanboxContact) c);
				vcards.add(v);
			}
		}
		vcards.add(AbstractAddressbookManager.contact2VCard(panbox.getIdentity()));
		Log.v("cache dir:", ""+context.getCacheDir());

		if (!AbstractAddressbookManager.exportContacts(vcards, vcardFile)) {
			Toast.makeText(context,
					"couldNotExportContacts",
					Toast.LENGTH_LONG).show();
		}

		Toast.makeText(context,
				getString(R.string.vcard_successfully_exported)+"\n"+vcardFile.getPath(),
				Toast.LENGTH_LONG).show();
	}
	public void importContacts(VCard[] vcs, boolean authVerified) {
		Log.v("NoGUIActivity:", "in importContacts()");
		try {
			panbox.getAddressbookManager().
			importContacts(panbox.getIdentity(), vcs, authVerified);
			Toast.makeText(context, getString(R.string.pb_contacts_successfully_imported), Toast.LENGTH_LONG).show();
		} catch (ContactExistsException e) {
			StringBuilder b = new StringBuilder();
			List<PanboxContact> existingContacts = e.getContacts();
			for (PanboxContact c : existingContacts) {
				if(c.getEmail().equals(panbox.getIdentity().getEmail())) {
					Log.v("NoGUIActivity", "Ignored request to add self user as contact.");
					continue;
				}
				
				b.append("- ");
				b.append(c.getFirstName() + " " + c.getName() + " ("
						+ c.getEmail() + ")");
				b.append("\n");
			}
			if(!b.toString().isEmpty()) {
				Toast.makeText(context, getString(R.string.pb_contact_exists), Toast.LENGTH_LONG).show();
			}
		} finally {
			panbox.getIdentityManager().storeMyIdentity(panbox.getIdentity());
		}
	}
	private VCard[] loadVCardFile(File vcardFile) {
		VCard[] vclist = null;
		
		if (!vcardFile.exists() || !vcardFile.canRead()) {
			Log.v("NoGUIActivity:loadVCardFile()", "can not read vcard file");
			return null;
		} else {
			// only continue if there are any VCards ..
			vclist = AddressbookManagerAndroid.readVCardFile(vcardFile);
			if (vclist != null 	&& (vclist.length > 0)) {
				Log.v("NoGUIActivity:loadVCardFile()", "vclist was null");
				return vclist;
			}
		}
		return null;
	}

	@Override
	public void onComplete(Bundle b) {
		String filename = b.getString("filename");
		
		//Toast.makeText(context, "Successfully got password:"+exportPin+" and targetdir:"+targetDir, Toast.LENGTH_LONG).show();
		
		File vcardFile = new File(targetDir+File.separator + filename + ".vcf");
		ArrayList<PanboxContact> contacts = new ArrayList<PanboxContact>();
		for (PanboxContact c : panbox.getIdentity().getAddressbook().getContacts()){
			contacts.add(c);
		}
		exportContacts(contacts, vcardFile);
		finish();
	}

}
