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
package org.panbox.mobile.android.gui.fragment;

import java.io.File;
import java.util.ArrayList;

import org.panbox.mobile.android.R;
import org.panbox.mobile.android.gui.activity.FileBrowserActivity;
import org.panbox.mobile.android.gui.activity.ShareManagerActivity;
import org.panbox.mobile.android.gui.adapter.FileItemAdapter;
import org.panbox.mobile.android.gui.data.FileItem;
import org.panbox.mobile.android.gui.data.PanboxManager;
import org.panbox.mobile.android.utils.AndroidSettings;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class ShareManagerFragment extends Fragment implements ShareManagerActivity.TaskListener,OnItemClickListener{
	
	public static final String TAG_CLASS = "ShareManagerFragment:";
	
	private ProgressDialog progressDialog;
	private boolean isTaskRunning = false;
	private SyncShareList asyncTask;
	
	private LinearLayout infoBarLine;
	private LinearLayout infoBarContainer;
	protected String currentDir;
	protected String currentUser;
	
	protected ListView mainLv = null;
	protected FileItemAdapter adapter = null;
	protected ArrayList<FileItem> shareList;
	protected boolean isItemClicked = false;
	private LinearLayout updateButton;
	private OnTouchListener onUpdateButtonListener;
	
	protected Bundle bundle;
	private PanboxManager panbox;
	private View fragmentLayout;
	 
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
//		listener = (Tasklistener) activity;
		onUpdateButtonListener = (OnTouchListener)activity;
	}
	 
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		
		panbox = PanboxManager.getInstance(getActivity());		
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		 fragmentLayout = inflater.inflate(R.layout.pb_list_view, container, false);
		 mainLv = (ListView)fragmentLayout.findViewById(R.id.pb_listview);	// get handle to the listview
		 
		 infoBarContainer = (LinearLayout)fragmentLayout.findViewById(R.id.pb_infobar_container);
		 infoBarLine = (LinearLayout) inflater.inflate(R.layout.pb_infobar_line, container, false);
		 infoBarContainer.addView(infoBarLine);
		 
		 mainLv.setOnItemClickListener(this);
		 return fragmentLayout;
	}
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		 setInfoBarView(R.id.pb_infobar_container, getString(R.string.pb_shares_for) + ":\n", AndroidSettings.getInstance().getUserName(), infoBarLine);
		// If we are returning here from a screen orientation
		// and the AsyncTask is still working, re-create and display the
		// progress dialog.
		if (isTaskRunning) {
			progressDialog = ProgressDialog.show(getActivity(), getString(R.string.pb_loading),
					getString(R.string.pb_please_wait));
		}
		else{
			
			if( shareList != null){
				Log.v(TAG_CLASS+"onActivityCreated()","shareList is not null, so show its content instead of syncing");
	    		populateListView(shareList);
			} 
			else {
				// asyncTask = new
				// SyncDirectories(this,context,volume,dbList,mDBCon);
				asyncTask = new SyncShareList(this,
						panbox.getMyDBCon(),
						panbox.getVolume()
						);
				asyncTask.execute();
			}
		}
	}	

	@Override
    public void onPreExecute() {
        isTaskRunning = true;
		progressDialog = ProgressDialog.show(getActivity(),
				getString(R.string.pb_loading),
				getString(R.string.pb_please_wait));
    }
 
    @Override
    public void onPostExecute() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        isTaskRunning = false;
    }
    
    @Override
    public void onDetach() {
        // All dialogs should be closed before leaving the activity in order to avoid
        // the: Activity has leaked window com.android.internal.policy... exception
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        
        super.onDetach();
    }


	@Override
	public void populateListView(ArrayList<FileItem> res) {
		shareList = res;
		adapter = initFileAdapter(res);
		mainLv.setAdapter(adapter);
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, android.view.View view, int position, long id) {
			Log.v(TAG_CLASS," in onItemClick()");
			Intent fileBrowserActivity = new Intent(getActivity(), FileBrowserActivity.class);
			
			bundle = new Bundle();
			
			bundle.putString("chosenShare", adapter.getItem(position).getName());
			bundle.putString("path", File.separator + adapter.getItem(position).getName());
			bundle.putString("viewPath","/");
			//((ShareManagerActivity) getActivity()).setShareSelected(true);
			//bundle.putString("chosenShare", shareList.get((int) id).getName());
			
			fileBrowserActivity.putExtras(bundle);
			fileBrowserActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(fileBrowserActivity);
			
			Log.v(TAG_CLASS, "share " + adapter.getItem(position).getName() + ", position in list " + adapter.getItemId(position) + " was clicked");
	}
	/**
	 * 
	 * @param id	- id of the infobar container to inflate
	 * @param name	- name of the line
	 * @param value - value of the line
	 * @param view	- LinearLayout - layout to which name and value textviews are added
	 */
	public void setInfoBarView(int id, String name, String value, LinearLayout infoBarLine) {
		
		TextView nameView = (TextView)fragmentLayout.findViewById(R.id.pb_infobar_line_name);
		TextView valueView = (TextView)fragmentLayout.findViewById(R.id.pb_infobar_value);
		nameView.setText(name);
		valueView.setText(value);
		
		updateButton = (LinearLayout) getActivity().findViewById(R.id.pb_update_container);
		updateButton.setClickable(true);
		updateButton.setOnTouchListener(this.onUpdateButtonListener);
	}
	/**
	 * Set adapter by supplying it with objects to be displayed in the listview
	 * @param dbList
	 */
	public FileItemAdapter initFileAdapter(ArrayList<FileItem> items){
		
		return new FileItemAdapter(getActivity(), R.layout.pb_list_item, items);		// at this step the objects to be mapped to views are instantiated, so we can use our adapter to convert them to views
		
	}
	
	public ArrayList<FileItem> getShareList() {
		return shareList;
	}

	public void setShareList(ArrayList<FileItem> shareList) {
		this.shareList = shareList;
	}

	@Override
	public void onStart() {
		Log.v(TAG_CLASS,"in onStart()");
		super.onStart();
	}
	
	@Override
	public void onPause() {
		Log.v(TAG_CLASS,"in onPause()");
		super.onPause();
	}

	@Override
	public void onStop() {
		Log.v(TAG_CLASS,"in onStop()");
		super.onStop();
	}

	@Override
	public void onDestroy() {
		Log.v(TAG_CLASS,"in onDestroy()");
		super.onDestroy();
	}

}


