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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.panbox.mobile.android.R;
import org.panbox.mobile.android.gui.adapter.FileItemAdapter;
import org.panbox.mobile.android.gui.data.FileItem;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class DirectoryExplorerActivity extends CustomActionBarActivity implements OnItemClickListener{
		private boolean isOpenedForExport = false;
		private Bundle bundle;
		private	 LayoutInflater inflater;
		
		private int itemPosition;
		/**	
		 * Displays information about user or shares
		 */
		private LinearLayout infoBarLine;
		private LinearLayout infoBarContainer;
		
		private FileItemAdapter adapter = null;
		
		protected ListView mainLv = null;

		protected String currentDir;
//		protected String reuseName;
		

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.pb_list_view);
		this.currentDir = Environment.getExternalStorageDirectory().getPath();
		
		LinearLayout updateButton = (LinearLayout)findViewById(R.id.pb_update_container);
		((RelativeLayout)updateButton.getParent()).removeView(updateButton);
		
		bundle = getIntent().getExtras();

		if(bundle!=null && bundle.getString("method").equals("export")){
			isOpenedForExport = true;
			LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			LinearLayout chooseLocBtnLayout = (LinearLayout)inflater.inflate(R.layout.pb_choose_dir_button_layout, null);
			LinearLayout ll = (LinearLayout)findViewById(R.id.pb_listview_parent_layout);
			ll.addView(chooseLocBtnLayout);
			Button chooseLocBtn = (Button)findViewById(R.id.pb_choose_location_button);
			chooseLocBtn.setText(R.string.choose_dir_text);
			chooseLocBtn.setOnClickListener(new OnClickListener() {				
				@Override
				public void onClick(View v) {
					Intent intent = new Intent();
					intent.putExtra("targetDir", currentDir);
					setResult(RESULT_OK, intent);
					finish();
				}
			});
		} 
		
		getActionBar().hide();
		
		setMainLv(R.id.pb_listview);
		
		
		inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);	// get an inflator
						
		infoBarContainer = (LinearLayout)findViewById(R.id.pb_infobar_container);
		infoBarLine= (LinearLayout)inflater.inflate(R.layout.pb_infobar_line, infoBarContainer, true);
		//infoBarContainer.addView(infoBarLine);
		((TextView)infoBarLine.findViewById(R.id.pb_infobar_line_name)).setText("sfdsdf");
		//currentDir = Environment.getExternalStorageDirectory();
		displayDirItemObjects(currentDir);	// draw the listview
		
	}
	/**
	 * This function displays the content of the current directory. 
	 * It instantiates File objects to get file properties. These properties are then used 
	 * to instantiate DirectoryItem objects, upon which then the custom adapter is called to display the ListView
	 * This method should be used when the onListItemClick event is triggered. Using the position variable passed to the eventhandler
	 * determine which element was touched, obtain its path, check if it is a directory. if so then call this method to display its content 
	 * @param curDirectory
	 */
	protected void displayDirItemObjects(String currDir){
		
		File currDirFile = new File(currDir);
		
		ArrayList<FileItem> dirItems = new ArrayList<FileItem>();		// define it as a local variable, so that memory is freed upon exiting the function

		int amountOfDirItems = 0;
		String lastModified = null;
		
		try {
			
			File[] files = currDirFile.listFiles();	// obtain a list of file objects
			
			for(File file : files){	
			
				// if file is a directory, then set it directory icon, otherwise file icon
				if (file.isDirectory()){	
				
					File[] its = file.listFiles();
					
					if (its != null)
					
						amountOfDirItems = its.length;
				}
				
				// obtain file creation date
				SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy , hh:mm:ss",Locale.US);	 
				
				lastModified = dateFormat.format(new Date(currDirFile.lastModified())); 
				
				// based on file properties retrieved initialize FileItem object, and add it to the list of FileItem objects
				dirItems.add( new FileItem( file.getName(), 
											file.getAbsolutePath(),  
											lastModified, 
											Long.valueOf(file.length()).toString(), 
											Integer.valueOf(amountOfDirItems).toString(),
											file.isDirectory()
											)
							);
				
			}	// for loop
			
			
			// on the top of the list, should always be a back-to-parent control. 
			//Its isDirectory must be true, to have "up to the parent" activated
			if (currDir.equals(Environment.getExternalStorageDirectory().getPath()))
				dirItems.add(0, new FileItem("/", currDirFile.getParent(), "", "", "0", true));
			else 	
				dirItems.add(0, new FileItem("..", currDirFile.getParent(), "", "", "0", true));
						
			
		}catch(Exception e){
			
			e.printStackTrace();
			
		}
		
		setInfoBarView(R.id.pb_infobar_container, getString(R.string.pb_path) + ": ", currDir + "/", infoBarLine);
				
		setAdapter(dirItems);
		
		mainLv.setAdapter(adapter);	// specify the adapter for this listview
		
		mainLv.setOnItemClickListener(this);
		
	}
	
	public String getCurrentDir() {
		return currentDir;
	}
	
	public void setCurrentDir(String currentDir) {
		this.currentDir = currentDir;
	}
	
	/**
	 * Set adapter by supplying it with objects to be displayed in the listview
	 * @param dbList
	 */
	public void setAdapter(ArrayList<FileItem> items){
		
		adapter = new FileItemAdapter(this, R.layout.pb_list_item, items);		// at this step the objects to be mapped to views are instantiated, so we can use our adapter to convert them to views
		
	}
	public FileItemAdapter getAdapter(){
		
		return adapter;
		
	}
	
	/**
	 * 
	 * @param id	- id of the infobar
	 * @param text	- text to be displayed in the infobar
	 */
	public void setInfoBarView(int id, String name, String value, LinearLayout infoBarLine){

		TextView nameView = (TextView)findViewById(R.id.pb_infobar_line_name);
		TextView valueView = (TextView)findViewById(R.id.pb_infobar_value);
		nameView.setText(name);
		nameView.setTextColor(getResources().getColor(R.color.black));
		valueView.setText(value);
		valueView.setTextColor(getResources().getColor(R.color.black));
	}	
	
	public ListView getMainLv() {
		return mainLv;
	}
	
	public void setMainLv(int id) {
		this.mainLv = (ListView) findViewById(id);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Log.v("DirectoryExplorerActivity:","in onItemClick()");
		itemPosition = position;
		String fullPath = adapter.getItem(itemPosition).getFullPath();
		
		if( !adapter.getItem(position).getFullPath().equals("/") && adapter.getItem(position).isDirectory()){	// if we are not in the root and this is directory, then can go back and forth	
			currentDir = adapter.getItem(position).getFullPath();
			displayDirItemObjects(currentDir);
		}
		else if(!isOpenedForExport && ( adapter.getItem(position).getItemTypeId() == R.drawable.ic_zip || adapter.getItem(position).getExtension().equals(".vcf" ))){	// if file is a document, then do nothing
			Intent intent = new Intent();
			intent.putExtra("fileName", fullPath);
			setResult(RESULT_OK, intent);
			finish();
		} 
//		else if (isOpenedForExport && adapter.getItem(position).getExtension() == ".vcf"){
//			reuseName = adapter.getItem(position).getName().substring(0,adapter.getItem(position).getName().lastIndexOf("."));
//			
//		}
		else{
			Toast.makeText(this,getString(R.string.pb_not_a_pairing_file),Toast.LENGTH_LONG).show();
		}
	}
}
