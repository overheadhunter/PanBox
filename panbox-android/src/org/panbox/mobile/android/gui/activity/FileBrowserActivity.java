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

import org.panbox.mobile.android.R;
import org.panbox.mobile.android.gui.data.PanboxManager;
import org.panbox.mobile.android.gui.fragment.FileBrowserFragment;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class FileBrowserActivity extends CustomActionBarActivity implements OnTouchListener{

	public static interface TaskListener {
	    void onPreExecute();
	    void onPostExecute();
	}
	
	private FileBrowserFragment fbFragment;
	private FragmentManager fm;
	private FragmentTransaction ft;

	protected Bundle bundle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.v("FileBrowserActivity:"," in onCreate()");
		setContentView(R.layout.pb_fragment_share_content);
		getActionBar().show();
		
		lastLanguage = settings.getLanguage();
		lastAccessToken = settings.getDropboxAuthToken();
		
		updateActionbarBehaviour();
		
		fm = getFragmentManager();
		ft = fm.beginTransaction();
		
		if ( (fbFragment = (FileBrowserFragment)fm.findFragmentById(R.id.filebrowser_fragment)) == null){
			fbFragment = new FileBrowserFragment();
		    ft.add(R.id.filebrowser_fragment, fbFragment);
		    ft.commit();
		}
				
		context = getApplicationContext();
			
		panbox = PanboxManager.getInstance(context);
		
		highlightActionbarItem(CustomActionBarActivity.ITEMS.FILEBROWSER.getNumVal());
	}
	
	/**
	 * this is a callback method called by the fragment when fragment creates itself in its onCreate()
	 * @return bundle that will be also used by the fragment
	 */
	//TODO: remove this method and make changes where needed
	public Bundle getBundleFromIntent(){
		return bundle == null ? getIntent().getExtras() : bundle;
	}
	
	/**
	 * Since the FileBrowsereActivity runs as a singleTask, an intent to start this activity results in the OS making call to the onNewIntent method instead of the onCreate.
	 * In this method we check if share chosen by the user is the same as the previous one. If this is the case, then we just show the current fragment. 
	 * Otherwise, we remove the fragment, to create a new one in the onResume() of this activity
	 */
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.v("FileBrowserActivity:", "in onNewIntent()");
		String oldShareName = null;
		highlightActionbarItem(CustomActionBarActivity.ITEMS.FILEBROWSER.getNumVal());
		
		updateActionbarBehaviour();		
		
		bundle = intent.getExtras();
		
		fbFragment = (FileBrowserFragment)fm.findFragmentById(R.id.filebrowser_fragment);
		
		if (fbFragment != null) {

			if(fbFragment.getShareContent() == null || fbFragment.getShareContent().isEmpty()) {							
				Log.v("FileBrowserActivity:onNewIntent()",
						"removed fragment because file list was empty.");
				fm.beginTransaction().remove(fbFragment).commit();
				fbFragment = null;
			} else if (!lastLanguage.equals(settings.getLanguage()) || !lastAccessToken.equals(settings.getDropboxAuthToken()) ) { // language has changed,
																// therefore we need to
																// remove current fragment.	onResume will
																// then take cares of creating a new fragment								
				Log.v("FileBrowserActivity:onNewIntent()",
						"need to remove and add fragment because language has changed");
				lastLanguage = settings.getLanguage();
				lastAccessToken = settings.getDropboxAuthToken();
				bundle = null; // need to remove bundle so that in fragment asyncTask is not started, but rather redirect to shareManagerActivity 
				fm.beginTransaction().remove(fbFragment).commit();
				fbFragment = null;
				Log.v("FileBrowserActivity:",
						"in onNewIntent(). fragment is removed");

			} else if (bundle != null) { // the user has chosen a new share,
											// therefore we need to remove
											// current fragment. onResume will
											// then take cares of creating a new
											// fragment

				Log.v("FileBrowserActivity:onNewIntent():", "chosen share is: "
						+ bundle.getString("chosenShare"));

				oldShareName = fbFragment.getShareName();

				if (oldShareName != null
						&& oldShareName.equals(bundle.getString("chosenShare"))) {
					Log.v("FileBrowserActivity:onNewIntent()",
							"no need to remove fragment, since the same share was chosen or fragment already does not exist");

				} else {
					fm.beginTransaction().remove(fbFragment).commit();
					fbFragment = null;
					Log.v("FileBrowserActivity:",
							"in onNewIntent(). fragment is removed");

				}
			}
		}
		
	}
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
		Log.v("FileBrowserActivity:", "in onDestroy()");
	}
	
//######################################################################################

	@Override
	protected void onResume() {
		super.onResume();
		Log.v("FileBrowserActivity:", "in onResume()");
		
		fbFragment = (FileBrowserFragment)fm.findFragmentById(R.id.filebrowser_fragment);
//		fm.beginTransaction().replace(R.id.filebrowser_fragment, fragment,null).commit();
						
		if (settings.isDropboxAuthTokenSet()) {

			if (panbox.getMyDBCon().isLinked() || panbox.getMyDBCon().resume()) {

				if (fbFragment == null) {
					Log.v("FileBrowserActivity:onResume()", "fragment is null, creating a new one");
					fbFragment = new FileBrowserFragment();
					fm.beginTransaction()
							.add(R.id.filebrowser_fragment, fbFragment).commit(); // the fragement will take care of initializing the adapter and populating the listview
				} 
				else {
//					if (fragment.getShareContent() == null
//							|| fragment.getShareContent().isEmpty()) {
//
//						fm.beginTransaction()
//								.replace(R.id.filebrowser_fragment, fragment,
//										null).commit();
//					}
				}

			}
		} else { // need to clean listview

			if (fbFragment != null) {
				Log.v("FileBrowserActivity:onResume()", " remove fragment");
				fm.beginTransaction().remove(fbFragment).commit();
			}

		}
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		v.performClick();
		
		if(v.getId() == R.id.pb_update_container){
			
			Log.v("FileBrowserActivity:onTouch()", "update button in FileBrowser was clicked");			
			
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				v.setBackgroundResource(R.color.custom_actionbar_item_highlight_bg);
			}
			if (event.getAction() == MotionEvent.ACTION_UP) {
				v.setBackgroundResource(R.color.custom_actionbar_bg);
				Intent fileBrowserActivity = new Intent(FileBrowserActivity.this, FileBrowserActivity.class);
				bundle = new Bundle();
				bundle.putString("chosenShare", fbFragment.getShareName());
				bundle.putString("path", fbFragment.getPath());
				bundle.putString("viewPath",fbFragment.getViewPath());
				fileBrowserActivity.putExtras(bundle);
				fileBrowserActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						
				fbFragment = (FileBrowserFragment)fm.findFragmentById(R.id.filebrowser_fragment);
				
				if (fbFragment != null)
					fm.beginTransaction().remove(fbFragment).commit();
				
				startActivity(fileBrowserActivity);
			}
		}
		else{
			super.onTouch(v, event);	//call the onTouch method of the customActionBarActivity
		}
		
		return false;
	}
	@Override
	protected void onStop() {
		super.onStop();
		Log.v("FileBrowserActivity:", "in onStop()");
	}
	
	@Override
	public void onBackPressed() {
	   
		Intent shareManager = new Intent(FileBrowserActivity.this,ShareManagerActivity.class);
		startActivity(shareManager);
		
	    return;
	}
	
	public Bundle getBundle() {
		return bundle;
	}
	public void setBundle(Bundle bundle) {
		this.bundle = bundle;
	}
//	public boolean isItemClicked() {
//		return isItemClicked;
//	}
//	public void setItemClicked(boolean isItemClicked) {
//		this.isItemClicked = isItemClicked;
//	}
}