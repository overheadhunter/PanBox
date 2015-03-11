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

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class NFCActivity extends CustomActionBarActivity {

	private static final int NFC_ONTIME = 60 * 1000;

	private static final String TAG = "NFCActivity:";

	Button activateNFC;
	private boolean nfcReceiverActive = false;

	private PackageManager pm;

	// runs without a timer by reposting this handler at the end of the runnable
	protected Handler timerHandler = new Handler();
	protected Runnable timerRunnable = new Runnable() {

		@Override
		public void run() {
			Log.v(TAG, "Runnable");
			timerHandler.removeCallbacks(this);
			pm = getPackageManager();
			// if not disabled via user interaction, disable it now
			if (!(pm.getComponentEnabledSetting(new ComponentName(
					context,
					org.panbox.mobile.android.gui.activity.NFCReceiverActivity.class)) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED)) {

				Log.v(TAG, "NFC timeout");
				Toast.makeText(context, getString(R.string.pb_nfc_stop_msg),
						Toast.LENGTH_SHORT).show();
				disable();
			}

		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.pb_nfc_view);
		getActionBar().show();

		context = getApplicationContext();

		highlightActionbarItem(CustomActionBarActivity.ITEMS.NFC.getNumVal());

		updateActionbarBehaviour();
		
		activateNFC = (Button) findViewById(R.id.pb_nfcActivateBtn);

		activateNFC.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				if (nfcReceiverActive) {
					Log.v(TAG, "Stop NFC clicked");
					timerHandler.removeCallbacks(timerRunnable);
					disable();
				} else {
					enable(); // enable receiver activity

					Toast.makeText(
							context,
							getString(R.string.pb_nfc_start_msg,
									NFC_ONTIME / 1000), Toast.LENGTH_SHORT)
							.show();

					Log.v(TAG, "Start NFC clicked");
					timerHandler.postDelayed(timerRunnable, NFC_ONTIME);
				}

			}
		});
		
		setButtonStatus();

		pm = getPackageManager();
	}

	@Override
	protected void onResume() {
		Log.v(TAG, "onResume()");
		super.onResume();

		setButtonStatus();
	}
	
	@Override
	public void onBackPressed() {
	   
		Intent shareManager = new Intent(NFCActivity.this,ShareManagerActivity.class);
		startActivity(shareManager);
		
	    return;
	}
	private void enable() {
		pm.setComponentEnabledSetting(
				new ComponentName(
						this,
						org.panbox.mobile.android.gui.activity.NFCReceiverActivity.class),
				PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
				PackageManager.DONT_KILL_APP);

		nfcReceiverActive = true;
		activateNFC.setText(getResources()
				.getString(R.string.pb_deactivate_btn));
	}

	protected void disable() {
		PackageManager pm = getPackageManager();
		pm.setComponentEnabledSetting(
				new ComponentName(
						this,
						org.panbox.mobile.android.gui.activity.NFCReceiverActivity.class),
				PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
				PackageManager.DONT_KILL_APP);

		nfcReceiverActive = false;
		activateNFC.setText(getResources().getString(R.string.pb_activate_btn));
	}
	
	private void setButtonStatus()
	{
		if (nfcReceiverActive) {
			activateNFC.setText(getResources().getString(
					R.string.pb_deactivate_btn));
		} else {
			activateNFC.setText(getResources().getString(
					R.string.pb_activate_btn));
		}
	}
}
