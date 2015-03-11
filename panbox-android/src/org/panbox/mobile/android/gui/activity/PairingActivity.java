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

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import org.panbox.core.identitymgmt.AbstractIdentity;
import org.panbox.core.identitymgmt.Identity;
import org.panbox.core.identitymgmt.SimpleAddressbook;
import org.panbox.core.identitymgmt.exceptions.ContactExistsException;
import org.panbox.core.pairing.file.PanboxFilePairingUtils;
import org.panbox.mobile.android.R;
import org.panbox.mobile.android.gui.data.PanboxManager;
import org.panbox.mobile.android.utils.AndroidSettings;
import org.panbox.mobile.android.utils.PasswordDialog;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class PairingActivity extends Activity implements OnClickListener,PasswordDialog.OnCompleteListener{
	private Button btnStartPairing;
	private Button btnBrowse;
	private Button btnAutoPairing;
	private Context context;
	private PanboxManager panbox;
	private EditText qrCodeEditText;
	private String password;
	private String fileName;
	private Intent data;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.pb_pairing_assistant);

		getActionBar().hide();
		
		context = getApplicationContext();
		
		panbox = PanboxManager.getInstance(context);
				
		btnStartPairing = (Button)findViewById(R.id.start_pairing_button);
		
		btnBrowse = (Button)findViewById(R.id.browse_file_button);

		btnAutoPairing = (Button)findViewById(R.id.auto_pairing_button);
		
		qrCodeEditText = (EditText)findViewById(R.id.pairing_code_edittext);
		
		btnStartPairing.setEnabled(false); 
		
		qrCodeEditText.addTextChangedListener(new TextWatcher(){

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				//Toast.makeText(context, "before text changed", Toast.LENGTH_LONG).show();
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				//Toast.makeText(context, "on text changed", Toast.LENGTH_LONG).show();
				if(s.length() > 20){
					btnStartPairing.setEnabled(true);
				}else {
					btnStartPairing.setEnabled(false);
				}
			}

			@Override
			public void afterTextChanged(Editable s) {
				//Toast.makeText(context, "after text changed", Toast.LENGTH_LONG).show();
			}
		});
	
		btnStartPairing.setOnClickListener(this);
		
		//btnStartPairing.setEnabled(false);
		
		btnBrowse.setOnClickListener(this);
		
		btnAutoPairing.setOnClickListener(this);

	}
	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.start_pairing_button){
			
			String pairingPassword = qrCodeEditText.getText().toString().trim();
			if (pairingPassword.matches("^[0-9A-Za-z.]+:[A-Z0-9a-z+/]+={0,2}$")) {
				
				Log.v("Pairing Activity", "Scanned valid QR code! Will start the pairing!!!");
				
				Intent pairingExecutionActivity = new Intent(this, PairingExecutionActivity.class);
				pairingExecutionActivity.putExtra("pairingPassword", pairingPassword);
				startActivity(pairingExecutionActivity);
			} else {
				Toast invalidQr = Toast.makeText(getApplicationContext(), "The entered pairing password was invalid!", Toast.LENGTH_SHORT);
				invalidQr.show();
			}
		}
		
		if (v.getId() == R.id.browse_file_button){	// choose a file from the file system
			
			Intent dirExplorerActivity = new Intent(this,DirectoryExplorerActivity.class);
		
			startActivityForResult(dirExplorerActivity,1);
			
		}
		
		if (v.getId() == R.id.auto_pairing_button){	// conduct pairing automatically
			
			Intent assistentActivity = new Intent(this, AssistentActivity.class);
			
			startActivity(assistentActivity);
		}
	}
	
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public Intent getData() {
		return data;
	}
	public void setData(Intent data) {
		this.data = data;
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != Activity.RESULT_CANCELED || data != null) {
			this.data = data;
			fileName = data.getStringExtra("fileName");

			if (fileName == null || fileName.equals("")) {
				Toast.makeText(context, getString(R.string.pb_no_data_passed),
						Toast.LENGTH_LONG).show();
				return;
			} else{
				//dialog.show();
				PasswordDialog pwdd = new PasswordDialog();
				pwdd.show(getFragmentManager(), "pwdd");
			}
		}else{
			Toast.makeText(context, getString(R.string.pb_file_not_chosen),
					Toast.LENGTH_LONG).show();
		}
	}
	
	public void processPairingFile(String fileName, String password) {
		
		PanboxFilePairingUtils.PanboxFilePairingLoadReturnContainer fpc = null;
		 
		    File pairingFile = new File(fileName);
		    
		    try {
		    	
		    	
		    	fpc = PanboxFilePairingUtils.loadPairingFile(pairingFile,password.toCharArray());
		    	
		    	AndroidSettings.getInstance().setDeviceName(fpc.getDeviceName());
		        		        
		    	AndroidSettings.getInstance().setConfDir(context.getFilesDir().getAbsolutePath());// internal storage, where databases and key-store file are to be stored
			    
		    	AndroidSettings.getInstance().writeChanges();
			    
			    Log.v("Android Default path:",getApplicationContext().getFilesDir().getAbsolutePath());
		       					
				// The identity is an owner of the share
			    AbstractIdentity identity = new Identity(new SimpleAddressbook());
			    identity.setEmail(fpc.geteMail());
				identity.setName(fpc.getLastName());
				identity.setFirstName(fpc.getFirstName());
				
				identity.setOwnerKeyEnc(fpc.getEncCert());
				identity.setOwnerKeySign(fpc.getSignCert());
				  
				KeyPair devKeyPair = new KeyPair(fpc.getDeviceCert().getPublicKey(), fpc.getDevicePrivKey()); 
				identity.addDeviceKey(devKeyPair,fpc.getDeviceCert(), fpc.getDeviceName());
				identity.addDeviceCert(fpc.getDeviceCert(), AndroidSettings.getInstance().getDeviceName());
				
				try {
					// we assume the pairingfile to be trustworthy
					// w.r.t. the contact trust level
					File contactsFile = fpc.getContactsFile();
					panbox.getAddressbookManager().importContacts(identity, contactsFile,true);
					contactsFile.delete(); // we can now remove this tempFile
					panbox.getIdentityManager().storeMyIdentity(identity);
					Log.v("PairingActivity: ", "finished pairing without errors");
					Intent settingsActivity = new Intent(context,SettingsActivity.class);
					startActivity(settingsActivity);
					finish(); // finish the activity
			    
				} catch (ContactExistsException e) {
					//Toast.makeText(context, getString(R.string.pb_could_not_import_all_contacts), Toast.LENGTH_LONG).show();
			    	Log.v("PairingActivity: ", e.toString() + ";" + "PanboxClient : setUpIdentity : Could not import all contacts to addressbook.");
			    	Toast.makeText(context, getString(R.string.pb_file_corrupted), Toast.LENGTH_LONG).show();
				}
				panbox = PanboxManager.getInstance(context);
				panbox.setIdentity(identity);
			
				if(pairingFile.delete()) {
					Log.d("PairingActivity", "Deleted pairing file successfully!");
				} else {
					Log.w("PairingActivity", "Deleting pairing file was not successful!");
				}
				
		    }catch(IllegalArgumentException ex){
		    	//Toast.makeText(context, ex.getMessage(), Toast.LENGTH_LONG).show();
		    	Log.v("PairingActivity: ", ex.toString());
		    	Toast.makeText(context, getString(R.string.pb_file_corrupted), Toast.LENGTH_LONG).show();
		    }catch(NoSuchAlgorithmException ex){
		    	//Toast.makeText(context, ex.getMessage(), Toast.LENGTH_LONG).show();
		    	Log.v("PairingActivity: ", ex.toString());
		    	Toast.makeText(context, getString(R.string.pb_file_corrupted), Toast.LENGTH_LONG).show();
		    }
		    catch(CertificateException ex){
		    	//Toast.makeText(context, ex.getMessage(), Toast.LENGTH_LONG).show();
		    	Log.v("PairingActivity: ", ex.toString());
		    	Toast.makeText(context, getString(R.string.pb_file_corrupted), Toast.LENGTH_LONG).show();
		    }
		    catch(KeyStoreException ex){
		    	//Toast.makeText(context, ex.getMessage(), Toast.LENGTH_LONG).show();
		    	Log.v("PairingActivity: ", ex.toString());
		    	Toast.makeText(context, getString(R.string.pb_file_corrupted), Toast.LENGTH_LONG).show();
		    }
		    catch(UnrecoverableKeyException ex){
		    	//Toast.makeText(context, ex.getMessage(), Toast.LENGTH_LONG).show();
		    	Log.v("PairingActivity: ", ex.toString());
		    	Toast.makeText(context, getString(R.string.pb_file_corrupted), Toast.LENGTH_LONG).show();
		    }catch(IOException ex){
		    	//Toast.makeText(context, ex.toString(), Toast.LENGTH_LONG).show();
		    	Log.v("PairingActivity: ", ex.toString());
		    	Toast.makeText(context, getString(R.string.pb_file_corrupted), Toast.LENGTH_LONG).show();
		    }
	}
	@Override
	public void onComplete(Bundle b) {
		this.password = b.getString("password"); 
		if (!password.equals("")) {
			processPairingFile(this.data.getStringExtra("fileName"), password);	
		}
	}
}
	
