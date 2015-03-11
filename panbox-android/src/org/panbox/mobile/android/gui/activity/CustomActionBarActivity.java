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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import org.panbox.core.identitymgmt.AbstractIdentity;
import org.panbox.core.identitymgmt.AbstractIdentityManager;
import org.panbox.mobile.android.R;
import org.panbox.mobile.android.gui.data.CustomActionBar;
import org.panbox.mobile.android.gui.data.PanboxManager;
import org.panbox.mobile.android.identitymgmt.IdentityManagerAndroid;
import org.panbox.mobile.android.utils.AndroidSettings;
import org.panbox.mobile.android.utils.NoGUIActivity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Toast;

public class CustomActionBarActivity extends Activity implements OnTouchListener,
		CreateNdefMessageCallback, OnNdefPushCompleteCallback {

	protected AndroidSettings settings;
	
	protected String lastLanguage;
	protected String lastAccessToken; 

	public enum ITEMS {
		SETTINGS(0), SHAREMANAGER(1), FILEBROWSER(2), NFC(3); 
	    
	    private int numVal;

		ITEMS(int numVal) {
			this.numVal = numVal;
	    }

	    public int getNumVal() {
	        return numVal;
	    }
	}
	
	
	private static final String TAG = "MainActivity:";
	private static final String MIME_TYPE = "application/org.panbox.identity";

	//protected AbstractIdentityManager idm;
	protected PanboxManager panbox;
	protected Context context;

	protected CustomActionBar customActionbar;
	
	protected MenuInflater menuInflater;	

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		try{
			settings = AndroidSettings.getInstance();
			Log.v("CustomActionBarActivity:onCreate()", "settings is not null!");
		}catch(IllegalStateException e){
			e.printStackTrace();
			Log.v("CustomActionBarActivity:onCreate()", "settings was null -> need to do restart panbox");
			Intent restart = new Intent(CustomActionBarActivity.this, StartActivity.class); 
			startActivity(restart); 
		}
		
		context = getApplicationContext();
		
		PanboxManager.getInstance(context).updateLanguage();
		
		getRidOfHomeIcon(getActionBar());

		customActionbar = new CustomActionBar(context);

		customActionbar.addActionItem(R.drawable.ic_actionbar_share,
				R.layout.pb_actionbar_item, R.string.shares_label, this, ITEMS.SHAREMANAGER);

		customActionbar.addActionItem(R.drawable.ic_actionbar_filebrowser,
				R.layout.pb_actionbar_item, R.string.filebrowser_label, this, ITEMS.FILEBROWSER);

		customActionbar.addActionItem(R.drawable.ic_actionbar_nfc,
				R.layout.pb_actionbar_item, R.string.nfc_label, this, ITEMS.NFC);

		if (this instanceof ShareManagerActivity) {
			highlightActionbarItem(ITEMS.SHAREMANAGER.getNumVal());
		} else if (this instanceof FileBrowserActivity) {
			highlightActionbarItem(ITEMS.FILEBROWSER.getNumVal());
		} else if (this instanceof NFCActivity) {
			highlightActionbarItem(ITEMS.NFC.getNumVal());
		}

		getActionBar().setCustomView(customActionbar.getIcContainer());

		getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		panbox = PanboxManager.getInstance(context);

		// register NFC, if available
		NfcAdapter mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		if (mNfcAdapter != null) {
			// Register callback
			Log.v(TAG, "register NFC");
			mNfcAdapter.setNdefPushMessageCallback(this, this);
			mNfcAdapter.setOnNdefPushCompleteCallback(this, this, this);
		}
	}
	
	public void updateActionbarBehaviour(){
		if (this instanceof ShareManagerActivity) {
			Log.v("CustomActionBarActivity:ShareManagerActivity:","in updateActionbarBehaviour()");
		} else if (this instanceof FileBrowserActivity) {
			Log.v("CustomActionBarActivity:FileBroserActivity:","in updateActionbarBehaviour()");
		} else if (this instanceof NFCActivity) {
			Log.v("CustomActionBarActivity:NFCActivity:","in updateActionbarBehaviour()");
		}	
		
		if(settings.isDropboxAuthTokenSet()){
			customActionbar.getIcContainer().getChildAt(CustomActionBarActivity.ITEMS.SHAREMANAGER.getNumVal()).setEnabled(true);
			customActionbar.getIcContainer().getChildAt(CustomActionBarActivity.ITEMS.FILEBROWSER.getNumVal()).setEnabled(true);
			customActionbar.getIcContainer().getChildAt(CustomActionBarActivity.ITEMS.NFC.getNumVal()).setEnabled(true);
		}
		else{
			customActionbar.getIcContainer().getChildAt(CustomActionBarActivity.ITEMS.SHAREMANAGER.getNumVal()).setEnabled(false);
			customActionbar.getIcContainer().getChildAt(CustomActionBarActivity.ITEMS.FILEBROWSER.getNumVal()).setEnabled(false);
			customActionbar.getIcContainer().getChildAt(CustomActionBarActivity.ITEMS.NFC.getNumVal()).setEnabled(false);
			Toast.makeText(context, getString(R.string.pb_user_not_authenticated_text), Toast.LENGTH_LONG).show();
			Intent settingsActivity = new Intent(CustomActionBarActivity.this,SettingsActivity.class);
			startActivity(settingsActivity);
		}
	}
	public void generateVCard(MenuItem menuItem){
		if(settings.isDropboxAuthTokenSet()){
			Bundle bundle = new Bundle();
			bundle.putString("method", "export");
			Intent noGUIActivity = new Intent(this,NoGUIActivity.class);
			noGUIActivity.putExtras(bundle);
			startActivityForResult(noGUIActivity,1);
		}
		else
			Toast.makeText(context, getString(R.string.pb_user_not_authenticated_text), Toast.LENGTH_LONG).show();
	}
	public void processVCard(MenuItem menuItem) {
		if(settings.isDropboxAuthTokenSet()){
			Bundle bundle = new Bundle();
			bundle.putString("method", "import");
			Intent noGUIActivity = new Intent(this,NoGUIActivity.class);
			noGUIActivity.putExtras(bundle);
			startActivityForResult(noGUIActivity,1);
		}
		else
			Toast.makeText(context, getString(R.string.pb_user_not_authenticated_text), Toast.LENGTH_LONG).show();
	}
	/**
	 * Callback message that will be called on NFC activity and creates an NFC
	 * message containing our identity to be send out
	 */
	@Override
	public NdefMessage createNdefMessage(NfcEvent event) {
		Log.v(TAG, "createNdefMessage()");

		AbstractIdentity idToSend = panbox.getIdentity();

		Log.v(TAG, "identity null? " + idToSend);

		if (idToSend == null) {
			return null;
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		AbstractIdentityManager idm = IdentityManagerAndroid
				.getInstance(context);

		idm.exportMyIdentity(idToSend, baos);

		try {
			baos.flush();
			baos.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		//
		byte[] idFileData = baos.toByteArray();

		NdefRecord record = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
				MIME_TYPE.getBytes(Charset.forName("US-ASCII")), new byte[0],
				idFileData);
		NdefMessage msg = new NdefMessage(new NdefRecord[] { record });

		return msg;
	}

	/**
	 * This method handles an event that a custom actionbar item was clicked
	 */
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		v.performClick();
		
		if (event.getAction() == MotionEvent.ACTION_DOWN) {

			v.setBackgroundResource(R.color.custom_actionbar_item_click_bg);

			if (v.getId() == ITEMS.SHAREMANAGER.getNumVal()) { // 1 - is the id of the shares icon in the
									// custom actionbar, 2 - is the id of the
									// filebrowser
				Log.v(TAG, "in onTouch(), shareManager icon was clicked");
				Intent shareManagerActivity = new Intent(this,
						ShareManagerActivity.class);
				shareManagerActivity.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				startActivity(shareManagerActivity);

			}
			if (v.getId() == ITEMS.FILEBROWSER.getNumVal()) {
				Log.v(TAG, "in onTouch(), fileBrowser icon was clicked");
				Intent fileBrowserActivity = new Intent(this,
						FileBrowserActivity.class);
				fileBrowserActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(fileBrowserActivity);

			}
			if (v.getId() == ITEMS.NFC.getNumVal()) {
				Log.v(TAG, "in onTouch(), nfc icon was clicked");
				Intent nfcActivity = new Intent(this, NFCActivity.class);
				nfcActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(nfcActivity);

			}

		}

		if (event.getAction() == MotionEvent.ACTION_UP) {

			//v.setBackgroundResource(R.color.custom_actionbar_bg);
		}

		return false;
	}

	/**
	 * @param actionbar
	 *            Remove standard icon and title from the actionbar
	 */
	public void getRidOfHomeIcon(ActionBar actionbar) {

		actionbar.setDisplayShowCustomEnabled(true);

		actionbar.setDisplayShowHomeEnabled(false);

		actionbar.setDisplayShowTitleEnabled(false);
	}

	/**
	 * Inflate the Three Dot Menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.main_activity_actions, menu);
		 
		return true;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		// WORKAROUND: clear and inflate 3Dot-Menu each time it is opened to
		// change locale in case if it was changed. Couldn't make Android to
		// handle that
		menu.clear();
		
		menuInflater.inflate(R.menu.main_activity_actions, menu);

		return true;
	}

	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Log.v("MainActivity:", "in onConfigurationChanged()");
	}

	public void showSettingsActivity(MenuItem menuItem) {

		Intent settingsActivity = new Intent(CustomActionBarActivity.this, SettingsActivity.class);

		startActivity(settingsActivity);
	}
	public void showAboutActivity(MenuItem menuItem) {

		Intent aboutActivity = new Intent(CustomActionBarActivity.this, AboutActivity.class);

		startActivity(aboutActivity);
	}
	public void showNFCLayoutActivity(MenuItem menuItem) {

//		Intent nfcLayoutActivity = new Intent(CustomActionBarActivity.this, NFCReceiverLayoutTestActivity.class);
//
//		startActivity(nfcLayoutActivity);
	}
	
	/**
	 * This method is called when the custom actionbar is instantiated. It is
	 * needed to determine which icon needs to be highlighted
	 * 
	 * @param activeItemId
	 */
	public void highlightActionbarItem(int activeItemId) {

		for (int i = 0; i < customActionbar.getIcContainer().getChildCount(); i++) {

			customActionbar.getIcContainer().getChildAt(i)
					.setBackgroundResource(R.color.custom_actionbar_bg); // remove
																			// the
																			// highlight
																			// from
																			// all
																			// elements

		}

		if (activeItemId != ITEMS.SETTINGS.numVal) { // if not settings activity then highlight

			customActionbar
					.getIcContainer()
					.findViewById(activeItemId)
					.setBackgroundResource(
							R.color.custom_actionbar_item_highlight_bg);

			Log.v(TAG, "highlight the icon: " + String.valueOf(activeItemId));
		}
	}

	void setCustomActionbarItemActivated(CustomActionBarActivity.ITEMS item, boolean flag){
		customActionbar.getIcContainer().getChildAt(item.getNumVal()).setEnabled(flag);
	}
	public CustomActionBar getCustomActionbar() {
		return customActionbar;
	}

	public void setCustomActionbar(CustomActionBar customActionbar) {
		this.customActionbar = customActionbar;
	}

	@Override
	protected void onResume() {
		Log.v(TAG, "onResume()");
		super.onResume();
	}

	@Override
	public void onNdefPushComplete(NfcEvent arg0) {
		// show my Identity so the receiver can compare it
		startActivity(new Intent(this, IdentityVisualizerActivity.class));
	}

}
