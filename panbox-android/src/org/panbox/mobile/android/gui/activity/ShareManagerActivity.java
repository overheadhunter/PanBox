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
import java.io.FileNotFoundException;
import java.util.ArrayList;

import org.panbox.mobile.android.R;
import org.panbox.mobile.android.dropbox.csp.DropboxConnector;
import org.panbox.mobile.android.dropbox.vfs.DropboxVirtualVolume;
import org.panbox.mobile.android.gui.data.FileItem;
import org.panbox.mobile.android.gui.fragment.ShareManagerFragment;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class ShareManagerActivity extends CustomActionBarActivity implements OnTouchListener{

	public static interface TaskListener {
	    void onPreExecute();
	    void onPostExecute();    
	    void populateListView(ArrayList<FileItem> result);
	}	
	private final String root = "";
//	private boolean isShareSelected = false;
	
 	private ShareManagerFragment smFragment;
	private FragmentManager fm;
	private FragmentTransaction ft;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		Log.v("ShareManagerActivity:"," in onCreate()");
		setContentView(R.layout.pb_fragment_share_list);
		getActionBar().show();
		
		lastLanguage = settings.getLanguage();
		lastAccessToken = settings.getDropboxAuthToken();
		
		if (settings.isDropboxAuthTokenSet())	// check if token is set
			generateVolume();
		
		updateActionbarBehaviour();
		
		fm = getFragmentManager();
		ft = fm.beginTransaction();
		
		if ( (smFragment = (ShareManagerFragment)fm.findFragmentById(R.id.sharemanager_fragment)) == null){
			smFragment = new ShareManagerFragment();
		    ft.add(R.id.sharemanager_fragment, smFragment);
		    ft.commit();
		}
		
	}

	protected void generateVolume(){
		Log.v("ShareManagerActivity:", "in generateVolume()");
		if(panbox.getMyDBCon() == null)
			panbox.setMyDBCon(new DropboxConnector(this));
		panbox.getMyDBCon().connect(); // do the connect only if the accessToken provided

		try {
			// Generate new Volume
			panbox.setVolume(new DropboxVirtualVolume(root, panbox.getMyDBCon()));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
				
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.v("ShareManagerActivity:"," in onNewIntent()");
		
		highlightActionbarItem(CustomActionBarActivity.ITEMS.SHAREMANAGER.getNumVal());
		updateActionbarBehaviour();
		
		smFragment = (ShareManagerFragment)fm.findFragmentById(R.id.sharemanager_fragment);
		
		if (smFragment != null) {
						
			if (!lastLanguage.equals(settings.getLanguage()) || !settings.getDropboxAuthToken().equals(lastAccessToken)) {
				Log.v("ShareManagerActivity:onNewIntent()",
						"need to remove and add fragment because language has changed");
				
				lastLanguage = settings.getLanguage();
				lastAccessToken = settings.getDropboxAuthToken();
				
				fm.beginTransaction().remove(smFragment).commit();
				smFragment = null;
				Log.v("ShareManagerActivity:",
						"in onNewIntent(). fragment is removed");

			}
		}

	}

	@Override
	protected void onDestroy(){
		super.onDestroy();
		Log.v("ShareManagerActivity:"," in onDestroy()");
		
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.v("ShareManagerActivity:"," in onKeyDown()");
	    if(keyCode == KeyEvent.KEYCODE_BACK)
	    {
	            //moveTaskToBack(true);
	            
	    	Intent startMain = new Intent(Intent.ACTION_MAIN);
	    	startMain.addCategory(Intent.CATEGORY_HOME);
	    	startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    	startActivity(startMain);
	    	
	    	return true;
	    }

	    return false;
	}
	
//######################################################################################

	@Override
	protected void onResume() {
		super.onResume();
		Log.v("ShareManagerActivity:"," in onResume()");
		
		smFragment = (ShareManagerFragment)fm.findFragmentById(R.id.sharemanager_fragment);
//		fm.beginTransaction().replace(R.id.sharemanager_fragment, smFragment,null).commit();
		
		if(settings.isDropboxAuthTokenSet()){
			if (panbox.getMyDBCon().isLinked() || panbox.getMyDBCon().resume()) {
				
				if (smFragment == null) {
					smFragment = new ShareManagerFragment();
					fm.beginTransaction().add(R.id.sharemanager_fragment, smFragment).commit();	// the fragement will take care of initializing the adapter and populating the listview
				}
				else{
					
					if (smFragment.getShareList() == null || smFragment.getShareList().isEmpty()){
						
						fm.beginTransaction().replace(R.id.sharemanager_fragment, smFragment,null).commit();
					}
				}
				
			}
		}
		else {	// need to clean listview
			
			if(smFragment != null){
				Log.v("ShareManagerActivity:onResume()"," clearing adapter");
				fm.beginTransaction().remove(smFragment);
			}
				
		}
		updateActionbarBehaviour();
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		v.performClick();
		
		if(v.getId() == R.id.pb_update_container){
			
			Log.v("ShareManagerActivity:onTouch()", "update button in ShareManager was clicked");			
			
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				v.setBackgroundResource(R.color.custom_actionbar_item_highlight_bg);
			}
			if (event.getAction() == MotionEvent.ACTION_UP) {
				v.setBackgroundResource(R.color.custom_actionbar_bg);
				Intent shareManagerActivity = new Intent(ShareManagerActivity.this, ShareManagerActivity.class);
				shareManagerActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						
				smFragment = (ShareManagerFragment)fm.findFragmentById(R.id.sharemanager_fragment);
				
				if (smFragment != null)
					fm.beginTransaction().remove(smFragment).commit();
				
				startActivity(shareManagerActivity);
			}
		}
		else{
			super.onTouch(v, event);	//call the onTouch method of the customActionBarActivity
		}
		
		return false;
	}
	
/*	public boolean isShareSelected() {
		return isShareSelected;
	}

	public void setShareSelected(boolean isShareSelected) {
		this.isShareSelected = isShareSelected;
	}
*/	
}