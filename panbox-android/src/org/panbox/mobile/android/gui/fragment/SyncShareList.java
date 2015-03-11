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

import org.panbox.mobile.android.dropbox.csp.DropboxConnector;
import org.panbox.mobile.android.dropbox.vfs.DropboxVirtualFile;
import org.panbox.mobile.android.dropbox.vfs.DropboxVirtualVolume;
import org.panbox.mobile.android.gui.activity.ShareManagerActivity;
import org.panbox.mobile.android.gui.data.FileItem;

import android.os.AsyncTask;
import android.util.Log;

public class SyncShareList extends AsyncTask< Void, Void, ArrayList<FileItem> > {

	private final String TAG_SYNC_SHARE_LIST = ShareManagerFragment.TAG_CLASS + "SyncShareList:";
	private ShareManagerActivity.TaskListener listener;
	
	private ArrayList<FileItem> resShareList;

	private DropboxConnector mDBCon;
	private DropboxVirtualVolume volume = null;

	private final String root = "";

	
    public SyncShareList(ShareManagerActivity.TaskListener listener, DropboxConnector mDBCn, DropboxVirtualVolume vlm) {
        this.listener = listener;
        this.volume = vlm;
        this.mDBCon = mDBCn;
    }
  
		
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		Log.v(TAG_SYNC_SHARE_LIST," in onPreExecute()");
		resShareList = new ArrayList<FileItem>();
		listener.onPreExecute();
	}

	@Override
	protected ArrayList<FileItem> doInBackground(Void... arg0) {
		Log.v(TAG_SYNC_SHARE_LIST," in doInBackground()");
		ArrayList<DropboxVirtualFile> fileNameList = mDBCon.listFiles(root, volume);
		String modified;										
		for (DropboxVirtualFile dbf : fileNameList) {
			if (!dbf.isDirectory()) {
				// ignore files, we only need to check folders
				continue;
			}

			ArrayList<String> subDirFiles = mDBCon.list(root + File.separator	+ dbf.getFileName());
			
			for (String sfn : subDirFiles){
				if(sfn.equals(".panbox")){
					modified = mDBCon.getFileInfo(dbf.getPath()).modified;			
					resShareList.add(new FileItem(dbf.getFileName(), dbf
							.getPath(), modified.substring(0,
							modified.lastIndexOf("+")), mDBCon
							.getFileInfo(dbf.getPath()).size, dbf
							.isDirectory() ? String
							.valueOf((new DropboxVirtualFile(dbf
									.getPath(), volume)).list().length)
							: "", dbf.isDirectory()));
					
					
				}
			}
		}
		
		return resShareList;
	}

	@Override
	public void onPostExecute(ArrayList<FileItem> result) {
		super.onPostExecute(result);
		Log.v(TAG_SYNC_SHARE_LIST," in onPostExecute()");
		listener.populateListView(result);
		listener.onPostExecute();
		
	}
}
