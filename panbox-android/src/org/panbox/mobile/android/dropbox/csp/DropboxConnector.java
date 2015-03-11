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
package org.panbox.mobile.android.dropbox.csp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.panbox.mobile.android.R;
import org.panbox.mobile.android.dropbox.vfs.DropboxVirtualFile;
import org.panbox.mobile.android.dropbox.vfs.DropboxVirtualVolume;
import org.panbox.mobile.android.utils.AndroidSettings;

import android.app.Activity;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Account;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.session.AppKeyPair;

public class DropboxConnector extends CSPConnector {
	
	private DropboxAPI<AndroidAuthSession> mDBApi;
	private String accessToken;
	private Activity activity = null;
	private AndroidSettings settings = AndroidSettings.getInstance();
	private final String APP_KEY = "0c4z87ogromgnt5";
	private final String APP_SECRET = "bg768wuoswhk54n";

	private final AppKeyPair mAppKeys = new AppKeyPair(APP_KEY, APP_SECRET);

	public DropboxConnector(Activity activity) {
		this.activity = activity;
		this.accessToken = settings.getDropboxAuthToken();
		settings.writeChanges();
	}

	/**
	 * Handles the authentication process.
	 * The generated accessToken is saved, so that the user does not have to enter his/her credentials every time.
	 */
	@Override
	public void connect() {
		// Start new authentication
		AndroidAuthSession mSession = new AndroidAuthSession(mAppKeys);
		
		// The DropboxAPI Instance
		mDBApi = new DropboxAPI<AndroidAuthSession>(mSession);
		
		this.accessToken = settings.getDropboxAuthToken();
		
		// When access token already exists in properties use it instead of starting new authentication
		if (accessToken.isEmpty()) {
			mDBApi.getSession().startOAuth2Authentication(activity);
		} else {
			mDBApi.getSession().setOAuth2AccessToken(accessToken);
		}
	}

	/**
	 * Unlinks the app from the users dropbox.
	 */
	@Override
	public void disconnect() {
		// Check first if application DB API link is still up!
		if(mDBApi == null) {
			// Start new authentication
			AndroidAuthSession mSession = new AndroidAuthSession(mAppKeys);
			
			// The DropboxAPI Instance
			mDBApi = new DropboxAPI<AndroidAuthSession>(mSession);
		}
		mDBApi.getSession().unlink();
		settings.setDropboxAuthToken("");
		settings.writeChanges();
		accessToken = "";
	}

	/**
	 * This function is called by the onResume() android function.
	 * It is called when the user reactivates the app.
	 * It finishes the dropbox authentication.
	 *
	 * @return true if the authentication was successful, false otherwise
	 */
	@Override
	public boolean resume() {
		if (mDBApi.getSession().authenticationSuccessful()) {
			try {
				mDBApi.getSession().finishAuthentication();
				String accessToken = mDBApi.getSession().getOAuth2AccessToken();
				settings.setDropboxAuthToken(accessToken);
				settings.writeChanges();
				return true;
			} catch (IllegalStateException e) {
				Log.d(String.valueOf(R.string.app_name), "Error Authenticating...");
				return false;
			}
		} else
			return false;
	}
//	public boolean isAccessTockenSet(){
//		return getAccessToken().equals("") ? false : true;
//	}
	
//	public boolean unsetAccessToken(){
//		//activity.getPreferences(Context.MODE_PRIVATE).edit().putString(activity.getString(R.string.dropbox_access_token), "").commit();
//		setAccessToken("");
//		accessToken = "";
//		return getAccessToken().equals("") ? true : false;
//	}
	
//	private String getAccessToken(){
//		return PreferenceManager.getDefaultSharedPreferences(activity).getString(activity.getString(R.string.dropbox_access_token), "");
//	}
//	private void setAccessToken(String accessToken){
//		PreferenceManager.getDefaultSharedPreferences(activity).edit().putString(activity.getString(R.string.dropbox_access_token), accessToken).commit();
//	}
	
	/**
	 * Downloads a file from the dropbox to a given location
	 *
	 * @param path The file path within the dropbox
	 * @param dest The path where the file should be saved
	 * @return true when download successful, false otherwise
	 */
	@Override
	public boolean downloadFile(String path, String dest) {
		try {
			FileOutputStream fos = new FileOutputStream(new File(dest));
			mDBApi.getFile(path, null, fos, null);
			fos.close();
			return true;
		} catch (DropboxException e) {
			e.printStackTrace();
			return false;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Downloads a file from the dropbox and returns an InputStream.
	 *
	 * @param path The file path within the dropbox
	 * @return The InputStream to the file
	 */
	public InputStream downloadFileStream(String path) {
		DropboxAPI.DropboxInputStream stream = null;
		try {
			stream = mDBApi.getFileStream(path, null);
		} catch (DropboxException e) {
			e.printStackTrace();
		}
		return stream;
	}

	/**
	 * Uploads a file from source to the dropbox
	 *
	 * @param source Path to file that shall be uploaded
	 * @param dest   Path to the file within dropbox. The filename can be different
	 * @return true when upload successful, false otherwise
	 */
	@Override
	public boolean uploadFile(String source, String dest) {
		FileInputStream fis;
		try {
			File file = new File(source);
			fis = new FileInputStream(file);
			mDBApi.putFileOverwrite(dest, fis, file.length(), null);
			fis.close();
			return true;
		} catch (DropboxUnlinkedException e) {
			Log.e(String.valueOf(R.string.app_name), "User has unlinked.");
			return false;
		} catch (DropboxException e) {
			Log.e(String.valueOf(R.string.app_name), "Something went wrong while uploading.");
			return false;
		} catch (FileNotFoundException e) {
			Log.e(String.valueOf(R.string.app_name), "File not found.");
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Creates a new folder at given path
	 *
	 * @param path Path to the new folder
	 * @return true if creation was successful, false otherwise
	 */
	@Override
	public boolean createFolder(String path) {
		try {
			mDBApi.createFolder(path);
			return true;
		} catch (DropboxException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Creates a new empty file at given path.
	 *
	 * @param path
	 * @return
	 */
	public boolean createFile(String path) {
		try {
			File tmpFile = new File(activity.getCacheDir() + path);
			if (!tmpFile.getParentFile().exists())
				tmpFile.getParentFile().mkdirs();
			tmpFile.createNewFile();
			FileInputStream fis = new FileInputStream(tmpFile);
			mDBApi.putFile(path, fis, 0, null, null);
			fis.close();
		} catch (DropboxException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * Deletes a file or folder from the dropbox
	 *
	 * @param path Path to file or folder in dropbox
	 * @return true, if deletion successful, false otherwise
	 */
	@Override
	public boolean deleteFile(String path) {
		try {
			mDBApi.delete(path);
			return true;
		} catch (DropboxException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Renames a file.
	 *
	 * @param path        Path to file within dropbox
	 * @param newFileName New file name
	 * @return true if successful, false otherwise
	 */
	@Override
	public boolean renameFile(String path, String newFileName) {
		String parent = new File(path).getParent();
		String newPath = parent + File.separator + newFileName;
		return moveFile(path, newPath);
	}

	/**
	 * Copys a file from one location to another.
	 *
	 * @param source Path to source file
	 * @param dest   Path to destination
	 * @return true if successful, false otherwise
	 */
	@Override
	public boolean copyFile(String source, String dest) {
		try {
			mDBApi.copy(source, dest);
			return true;
		} catch (DropboxException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Move a file from one location to another.
	 *
	 * @param source Path to source file
	 * @param dest   Path to destination
	 * @return true if successful, false otherwise
	 */
	@Override
	public boolean moveFile(String source, String dest) {
		try {
			mDBApi.move(source, dest);
			return true;
		} catch (DropboxException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Returns an ArrayList which contains the name of every file within given folder.
	 *
	 * @param path Path to folder
	 * @return ArrayList
	 */
	@Override
	public ArrayList<String> list(String path) {
		ArrayList<String> fileNameList = new ArrayList<String>();
		DropboxAPI.Entry files = null;
		try {
			files = mDBApi.metadata(path, 25000, null, true, null);
			for (Entry e : files.contents) {
				fileNameList.add(e.fileName());
			}
		} catch (DropboxException e) {
			e.printStackTrace();
		}
		return fileNameList;
	}
	
	public ArrayList<DropboxVirtualFile> listFiles(String path, DropboxVirtualVolume volume)
	{
		ArrayList<DropboxVirtualFile> fileList = new ArrayList<DropboxVirtualFile>();
		DropboxAPI.Entry files = null;
		try {
			files = mDBApi.metadata(path, 25000, null, true, null);
			for (Entry e : files.contents) {
				DropboxVirtualFile dbf = new DropboxVirtualFile(path + File.separator + e.fileName(), volume, e);
				
				
//				DropboxFile df = new DropboxFile(e.fileName(), e.isDir);
				fileList.add(dbf);
			}
		} catch (DropboxException e) {
			e.printStackTrace();
		}		
		
		return fileList;
	}

	/**
	 * Returns the file information in form of a dropbox Entry class.
	 *
	 * @param path Path to file or folder
	 * @return Entry
	 */
	public Entry getFileInfo(String path) {
		try {
			return mDBApi.metadata(path, 25000, null, true, null);
		} catch (DropboxException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Returns whether the Connector is linked to the dropbox or not.
	 *
	 * @return true if linked, false otherwise
	 */
	public boolean isLinked() {
		return mDBApi.getSession().isLinked();
	}
	
	public Account getUserAccountInfo(){
		try {
			return mDBApi.accountInfo();
		} catch (DropboxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
