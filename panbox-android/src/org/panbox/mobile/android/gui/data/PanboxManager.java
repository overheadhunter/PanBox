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
package org.panbox.mobile.android.gui.data;

import java.util.Locale;

import javax.crypto.SecretKey;

import org.panbox.core.identitymgmt.AbstractAddressbookManager;
import org.panbox.core.identitymgmt.AbstractIdentity;
import org.panbox.core.identitymgmt.AbstractIdentityManager;
import org.panbox.core.identitymgmt.SimpleAddressbook;
import org.panbox.core.keymgmt.ShareKey;
import org.panbox.mobile.android.dropbox.csp.DropboxConnector;
import org.panbox.mobile.android.dropbox.vfs.DropboxVirtualVolume;
import org.panbox.mobile.android.identitymgmt.AddressbookManagerAndroid;
import org.panbox.mobile.android.identitymgmt.IdentityManagerAndroid;
import org.panbox.mobile.android.utils.AndroidSettings;

import android.content.Context;
import android.content.res.Configuration;

public class PanboxManager {

	private static PanboxManager instance = null;
	
	private AbstractIdentityManager idm;
	private AbstractIdentity identity;
	private AbstractAddressbookManager adm;
	private Context context;
	private DropboxConnector myDBCon;
	private DropboxVirtualVolume volume;
	
	private SecretKey cachedObfuscationKey;
	private ShareKey cachedShareKey;
	
		
	private PanboxManager(Context context){
		this.context = context;
		initIdentityManager();
	}
	public static PanboxManager getInstance(Context cntxt){
		if(instance == null){
			instance = new PanboxManager(cntxt);
		}
		return instance;
	}
	
	public void initIdentityManager(){
		idm = IdentityManagerAndroid.getInstance(context);
		idm.init(
				this.getAddressbookManager());
	}
	/*getters and setters*/
	public AbstractIdentityManager getIdentityManager(){
		if(idm == null){
			this.initIdentityManager();
		}
		return idm;
	}
	
	public AbstractAddressbookManager getAddressbookManager(){
		if (adm == null)
			adm = (AbstractAddressbookManager) new AddressbookManagerAndroid(
				context, context.getContentResolver());
		return adm;
	}
	
	public AbstractIdentity getIdentity() {
		
		if(identity != null)
			return identity;
		else if( (identity = this.getIdentityManager().loadMyIdentity(new SimpleAddressbook()/*, adm*/)) != null)
			return identity;
//		else{
//			Intent i = new Intent(context, PairingActivity.class);
//			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            context.startActivity(i);
//		}
		
		return null;
	}

	public void updateLanguage(){
		
		Locale locale = AndroidSettings.getInstance().getLocale();
		Locale.setDefault(locale);
		Configuration config = context.getApplicationContext().getResources().getConfiguration();
		config.locale = locale;
		context.getApplicationContext().getResources().updateConfiguration(config, null);
	}
	public void setIdentity(AbstractIdentity identity) {
		this.identity = identity;
		
	}
	
	public SecretKey getCachedObfuscationKey() {
		return cachedObfuscationKey;
	}
	public void setCachedObfuscationKey(SecretKey cachedObfuscationKey) {
		this.cachedObfuscationKey = cachedObfuscationKey;
	}
	public ShareKey getCachedShareKey() {
		return cachedShareKey;
	}
	public void setCachedShareKey(ShareKey cachedShareKey) {
		this.cachedShareKey = cachedShareKey;
	}
	public DropboxConnector getMyDBCon() {
		return myDBCon;
	}
	public void setMyDBCon(DropboxConnector myDBCon) {
		this.myDBCon = myDBCon;
	}
	public DropboxVirtualVolume getVolume() {
		return volume;
	}
	public void setVolume(DropboxVirtualVolume volume) {
		this.volume = volume;
	}	
}