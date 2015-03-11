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
package org.panbox.mobile.android.identitymgmt;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class AccountService extends Service {

	private static AbstractAccountAuthenticator authenticator = null;
	
	private static final String accountName = "Panbox";
	private static final String accountType = "org.panbox";
	
	@Override
	public IBinder onBind(Intent intent) {
		
		IBinder ret = null;
		if (intent.getAction().equals(android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT))
			ret = getAuthenticator().getIBinder();
		return ret;
		
	}
	
	public AbstractAccountAuthenticator getAuthenticator() {
		if (authenticator == null)
			authenticator = new AccountAuthenticatorImpl(this);

		return authenticator;
	}	

	private static class AccountAuthenticatorImpl extends AbstractAccountAuthenticator {
		private Context context;

		public AccountAuthenticatorImpl(Context context) {
			super(context);
			this.context = context;
		}

		/**
		 * Check if a PanboxAccount already exists. If this is the case, we display an error since only one PanboxAccount is supported yet.
		 * If no PanboxAccount exists, we create one.
		 */
		@Override
		public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures,
				Bundle options) throws NetworkErrorException {
			Bundle reply = new Bundle();
			System.out.println(reply);
			Account panboxAccount = getPanboxAccount(context);
			
			if (panboxAccount != null)
			{
				handler.sendEmptyMessage(0);
			}
			else
				createPanboxAccount(context);

			return reply;
		}
		
		private Handler	handler	= new Handler() {
			@Override
			public void handleMessage(android.os.Message msg) {
				if (msg.what == 0)
					Toast.makeText(context, "PanboxAccount already exists.\nOnly one account is supported.", Toast.LENGTH_SHORT).show();
			};
		};
		
		/**
		 * Ensures that the Panbox SyncAccount exists and creates it if it doesn't.
		 */
		private void createPanboxAccount(Context context)
		{
			Log.i(AccountService.class.getSimpleName(), "Creating Panbox Account");
			AccountManager am = AccountManager.get(context);
//			final String syncAccountName = context.getResources().getString(R.string.SYNC_ACCOUNT_NAME);
//			final String syncAccountType = context.getResources().getString(R.string.SYNC_ACCOUNT_TYPE);
			
			final String syncAccountName = accountName;
			final String syncAccountType = accountType;
			
			Account panboxAccount = new Account(syncAccountName, syncAccountType);
			am.addAccountExplicitly(panboxAccount, null, null);
//			ContentResolver.setSyncAutomatically(panboxAccount, ContactsContract.AUTHORITY, false);
			
		}
		
		private Account getPanboxAccount(Context context) {
			AccountManager am = AccountManager.get(context);
			//final String syncAccountType = context.getResources().getString(R.string.SYNC_ACCOUNT_TYPE);
			final String syncAccountType = accountType;
			Account[] accounts = am.getAccountsByType(syncAccountType);
			
			if (accounts.length > 1)
				Log.w(AccountService.class.getSimpleName(), "More than one Panbox-Account exists.");
			
			if (accounts.length > 0)
				return accounts[0];
			else
				return null;
		}

		@Override
		public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) { 
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options)
				throws NetworkErrorException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getAuthTokenLabel(String authTokenType) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options)
				 {
			// TODO Auto-generated method stub
			return null;
		}
	}

}
