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
import java.util.Locale;

import android.content.SharedPreferences;

public class AndroidSettings {
	
	private String deviceName;
	private String dropboxAuthToken;
	private String confDir;
	private String language;
	private String userName;
	private String userEmail;
	
	private final SharedPreferences prefs;
	
	private static AndroidSettings instance = null;
	
	private AndroidSettings(SharedPreferences prefs) {
		this.prefs = prefs;
		
		this.deviceName = prefs.getString("deviceName", "FailedToLoadDeviceName!!!");
		this.dropboxAuthToken = prefs.getString("dropboxAuthToken", "");
		this.confDir = prefs.getString("confDir", "FailedToLoadConfDir!!!");
		this.language = prefs.getString("language", "system_default");
		this.userName = prefs.getString("userName", "");
		this.userEmail = prefs.getString("userEmail", "");
	}
	
	public static AndroidSettings initSettings(SharedPreferences prefs) {
		instance = new AndroidSettings(prefs);
		return instance;
	}
	
	public static AndroidSettings getInstance() {
		if(instance == null) {
			throw new IllegalStateException("You must first call initSettings on AndroidSettings.");
		}
		return instance;
	}
	
	public String getDeviceName() {
		return deviceName;
	}
	
	public String getDropboxAuthToken() {
		return dropboxAuthToken;
	}
	
	public String getConfDir() {
		return confDir;
	}
	
	public String getLanguage() {
		return language;
	}
	public String getUserName() {
		return userName;
	}

	public String getUserEmail() {
		return userEmail;
	}
	
	public Locale getLocale() {
		if (!language.equals("system_default")) {
			String[] split = language.split("_");
			return new Locale(split[0], split[1]);
		} else {
			return Locale.getDefault();
		}
	}
	
	public String getKeystorePath() {
		return confDir + File.separator + "keystore.jks";
	}
	
	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}
	
	public void setDropboxAuthToken(String dropboxAuthToken) {
		this.dropboxAuthToken = dropboxAuthToken;
	}
	
	public void setConfDir(String confDir) {
		this.confDir = confDir;
	}
	
	public void setLanguage(String language) {
		this.language = language;
	}
	public boolean isDropboxAuthTokenSet(){
		return getDropboxAuthToken().equals("") ? false : true;
	}
	public void writeChanges() {
		SharedPreferences.Editor edit = prefs.edit();
		edit.putString("deviceName", deviceName);
		edit.putString("dropboxAuthToken", dropboxAuthToken);
		edit.putString("confDir", confDir);
		edit.putString("language", language);
		edit.putString("userName",userName);
		edit.putString("userEmail",userEmail);
		edit.commit();
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}
}
