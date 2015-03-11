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

import java.util.Locale;

import org.panbox.mobile.android.R;
import org.panbox.mobile.android.dropbox.csp.DropboxConnector;
import org.panbox.mobile.android.gui.data.PanboxManager;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI.Account;


public class SettingsActivity extends CustomActionBarActivity implements OnClickListener{
	
//	private	LinearLayout popupLayout;
//	private	LinearLayout listView;
//	private	AlertDialog dialog;
//	private	Context context;
//	private	LayoutInflater inflater;
	
	private TextView tvHint;
	private TextView tvCurrUser;
	private TextView tvCurrUserLab;
	private Button authBtn;
	private Spinner langSpinner;
	private ArrayAdapter<CharSequence> adapter;
	private boolean isConnectMethodInvoked = false;

	private GetAccount getAccountTask;
	private Account userAccount;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		Log.v("SettingsActivity:"," in onCreate()");
		
		setContentView(R.layout.pb_settings);
		
		highlightActionbarItem(CustomActionBarActivity.ITEMS.SETTINGS.getNumVal());
		
		context = getApplicationContext();
		
		panbox = PanboxManager.getInstance(context);
		
//		inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);	// get an inflator
		
		tvHint = (TextView)findViewById(R.id.settings_hint);	// textview at the top of the activity
		
		tvCurrUser = (TextView)findViewById(R.id.settings_curr_user);
		
		tvCurrUserLab = (TextView)findViewById(R.id.settings_curr_user_label);
		
		tvHint.setGravity(Gravity.CENTER_HORIZONTAL);
		
		tvCurrUser.setText(settings.getUserName());
		
		authBtn = (Button) findViewById(R.id.settings_dropbox_auth_button);	// a settings button. when clicked a dialog window pops up allowing the user to change a dropbpx account
		
		authBtn.setOnClickListener(this);
		
		panbox.setMyDBCon(new DropboxConnector(this));
		
		//updateActionbarBehaviour();
		
		langSpinner = (Spinner)findViewById(R.id.pb_settings_language);		// dropdown menu for the languages.
		
		adapter = ArrayAdapter.createFromResource(this, R.array.languages, android.R.layout.simple_spinner_dropdown_item);	// creates the spinner view from the array items stored in the arrays.xml
		
		langSpinner.setAdapter(adapter);
		
		Locale locale = settings.getLocale();
		if(locale.equals(Locale.GERMANY)) {
			langSpinner.setSelection(1);
		} else {
			langSpinner.setSelection(0);
		}
		
		langSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				
				String language = adapter.getItem(position).toString();
				if(language == null || language.equals(""))
					settings.setLanguage("system_default");
				else 
					settings.setLanguage(language);
				
				settings.writeChanges();
				
				Locale locale = settings.getLocale();
				Locale.setDefault(locale);
				Configuration config = context.getApplicationContext().getResources().getConfiguration();
				config.locale = locale;
				context.getApplicationContext().getResources().updateConfiguration(config, null);
				
				Intent refresh = new Intent(SettingsActivity.this, SettingsActivity.class); 
				startActivity(refresh); 
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				
				
			}
		});

	}
	
	public void updateActionbarBehaviour(){
		//Log.v
		if (settings.isDropboxAuthTokenSet()){	// check if token is set
			
			authBtn.setText(R.string.db_setts_btn_logout_text);
			tvCurrUser.setText(settings.getUserName());
			setCustomActionbarItemActivated(CustomActionBarActivity.ITEMS.SHAREMANAGER,true);
			setCustomActionbarItemActivated(CustomActionBarActivity.ITEMS.FILEBROWSER,true);
			setCustomActionbarItemActivated(CustomActionBarActivity.ITEMS.NFC,true);
			
		}
		else{
			authBtn.setText(R.string.db_setts_btn_login_text);
			tvCurrUser.setText("");
			setCustomActionbarItemActivated(CustomActionBarActivity.ITEMS.SHAREMANAGER,false);
			setCustomActionbarItemActivated(CustomActionBarActivity.ITEMS.FILEBROWSER,false);
			setCustomActionbarItemActivated(CustomActionBarActivity.ITEMS.NFC,false);
		}
	}
	@Override
	protected void onNewIntent(Intent intent) {
		highlightActionbarItem(CustomActionBarActivity.ITEMS.SETTINGS.getNumVal());
		Log.v("SettingsActivity:"," in onNewIntent()");
		PanboxManager.getInstance(context).updateLanguage();
		super.onNewIntent(intent);
		tvHint.setText(R.string.settings_text);
		tvCurrUserLab.setText(R.string.pb_curr_user);
		tvCurrUser.setText(settings.getUserName());
		//updateActionbarBehaviour();
	}
	
	@Override
	public void onClick(View v) {
		Log.v("SettingsActivity:"," in onClick()");
		isConnectMethodInvoked = true;
		if (authBtn.getText().toString().equals(getString(R.string.db_setts_btn_login_text))){
			
			Log.v("SettingsActivity:","login");
			
		}
		else if(authBtn.getText().toString().equals(getString(R.string.db_setts_btn_logout_text))){
			
			Log.v("SettingsActivity:","remove credentials");
			settings.setDropboxAuthToken("");
			settings.setUserName("");
			settings.writeChanges();
			panbox.getMyDBCon().disconnect();
			Log.v("SettingsActivity:","the accessToken is unset");			
		}
		updateActionbarBehaviour();
		panbox.getMyDBCon().connect();
		
	}
	@Override
	protected void onStart() {
		super.onStart();
		Log.v("SettingsActivity:"," in onStart()");
	}

	@Override
	protected void onResume() {
		super.onResume();
		//super.onConfigurationChanged(context.getApplicationContext().getResources().getConfiguration());
		//context.getApplicationContext().getResources().updateConfiguration(config, null);
		Log.v("SettingsActivity:"," in onResume()");
		if (isConnectMethodInvoked){	// we need to check if this flag is true, if so then mDBCon method was invoked and session object mDBApi was created
			if (!(panbox.getMyDBCon().isLinked() || panbox.getMyDBCon().resume()))
				Toast.makeText(context, getString(R.string.pb_failed_auth), Toast.LENGTH_SHORT).show();
			else{
				getAccountTask = new GetAccount();
				getAccountTask.execute();
			}
		}
		updateActionbarBehaviour();
		isConnectMethodInvoked = false;

	}
	
	@Override
	protected void onPause() {
		super.onPause();
		Log.v("SettingsActivity:"," in onPause()");
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.v("SettingsActivity:"," in onStop()");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.v("SettingsActivity:"," in onDestroy()");
	}
	
	@Override
	public void onBackPressed() {
		if (settings.isDropboxAuthTokenSet()) {
			Intent shareManager = new Intent(SettingsActivity.this,
					ShareManagerActivity.class);
			startActivity(shareManager);
		}else{
			Intent startMain = new Intent(Intent.ACTION_MAIN);
	    	startMain.addCategory(Intent.CATEGORY_HOME);
	    	startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    	startActivity(startMain);
		}
		
	    return;
	}
	
	public void updateAuthenticationStatus(String userName){
		tvCurrUser.setText(userName);
		if(settings.isDropboxAuthTokenSet())
			authBtn.setText(getString(R.string.db_setts_btn_logout_text));
		
	}
/*	
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		   if (requestCode == 0) {
		      if (resultCode == RESULT_OK) {
		         String contents = intent.getStringExtra("SCAN_RESULT");
		         String format = intent.getStringExtra("SCAN_RESULT_FORMAT");
		         // Handle successful scan
		      } else if (resultCode == RESULT_CANCELED) {
		         // Handle cancel
		      }
		   }
		}
*/
	
	private class GetAccount extends AsyncTask<Void,Void,Boolean>{
		
		@Override
		protected Boolean doInBackground(Void... params) {
			userAccount = panbox.getMyDBCon().getUserAccountInfo();
			if (userAccount != null) {
				settings.setUserName(userAccount.displayName);
				settings.writeChanges();
				return true;
			}
			return false;
		}	
		
		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result) {
				tvCurrUser.setText(userAccount.displayName);
				authBtn.setText(getString(R.string.db_setts_btn_logout_text));
			}
		}
		
	}
}
