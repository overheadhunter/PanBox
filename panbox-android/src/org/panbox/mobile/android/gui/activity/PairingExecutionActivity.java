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

import java.net.InetAddress;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

import org.panbox.core.crypto.CryptCore;
import org.panbox.core.devicemgmt.DeviceType;
import org.panbox.core.identitymgmt.AbstractIdentity;
import org.panbox.core.identitymgmt.Identity;
import org.panbox.core.identitymgmt.SimpleAddressbook;
import org.panbox.core.identitymgmt.exceptions.ContactExistsException;
import org.panbox.core.pairing.PAKCorePairingRequester;
import org.panbox.mobile.android.R;
import org.panbox.mobile.android.gui.data.PanboxManager;
import org.panbox.mobile.android.utils.AndroidPairingUtils.BluetoothPairingParameters;
import org.panbox.mobile.android.utils.AndroidPairingUtils.BluetoothPairingTask;
import org.panbox.mobile.android.utils.AndroidPairingUtils.NetworkPairingParameters;
import org.panbox.mobile.android.utils.AndroidPairingUtils.NetworkPairingTask;
import org.panbox.mobile.android.utils.AndroidPairingUtils.NetworkResolutionResultListener;
import org.panbox.mobile.android.utils.AndroidPairingUtils.NetworkResolutionTask;
import org.panbox.mobile.android.utils.AndroidPairingUtils.PairingResultListener;
import org.panbox.mobile.android.utils.AndroidSettings;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import ezvcard.VCard;

public class PairingExecutionActivity extends Activity implements
		NetworkResolutionResultListener, PairingResultListener {

	// private String pairingPassword;
	private String addr;
	private String password;
	private KeyPair deviceKey;

	public static int counter = 0;

	private boolean isBTpairing = false;
	private boolean started = false;
	private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter
			.getDefaultAdapter();
	private final List<BluetoothDevice> foundDevices = new ArrayList<BluetoothDevice>();

	// Create a BroadcastReceiver for ACTION_FOUND
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				Log.d("PanboxExecutionActivity", "Found device: " + device);
				// Add the name and address to an array adapter to show in a
				// ListView
				foundDevices.add(device);
			}
			if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				Log.d("PanboxExecutionActivity", "Finished Bluetooth discovery");
				// Finished discovering devices

				if (isBTpairing) {
					BluetoothDevice handlerDevice = null;
					for (BluetoothDevice dev : foundDevices) {
						if (dev.getAddress()
								.replaceAll(":", "")
								.toUpperCase(
										AndroidSettings.getInstance()
												.getLocale())
								.equals(addr.toUpperCase(AndroidSettings
										.getInstance().getLocale()))) {
							handlerDevice = dev; // found device!
							break;
						}
					}

					// Device was found
					AsyncTask<BluetoothPairingParameters, Void, PAKCorePairingRequester> task = new BluetoothPairingTask(
							PairingExecutionActivity.this);

					task.execute(new BluetoothPairingParameters[] { new BluetoothPairingParameters(
							handlerDevice, password, DeviceType.MOBILE,
							deviceKey) });
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("PanboxExecutionActivity", "onCreate()");

		counter++;

		Log.d("PanboxExecutionActivity", "instance N: " + counter);

		setContentView(R.layout.activity_pairing_execution);

		getActionBar().setDisplayShowHomeEnabled(true);

		getActionBar().setDisplayShowTitleEnabled(true);

		getActionBar().setTitle("Pairing in progress...");

		setupScanBluetoothDevices();

		started = mBluetoothAdapter.startDiscovery();

		if (!started) {
			Log.e("PairingExecutionActivity",
					"Failed to start Bluetooth device discovery!!!");
		}
		Log.d("PanboxExecutionActivity", "Started Bluetooth discovery");

		String pairingPassword = getIntent().getStringExtra("pairingPassword");
		String[] splitPW = pairingPassword.split(":");
		addr = splitPW[0];
		password = splitPW[1];
		deviceKey = CryptCore.generateKeypair();

		//Log.v("Pairing Password:", pairingPassword);
		runPairingOperation();
	}

	@Override
	protected void onResume() {
		Log.v("PanboxExecutionActivity", "in onResume()");
		super.onResume();
	}

	private void runPairingOperation() {
		AsyncTask<String, Void, InetAddress> networkResolutionTask = new NetworkResolutionTask(
				this);

		networkResolutionTask.execute(new String[] { addr });
	}

	private void finishGeneralPairing(Context context, KeyPair deviceKey,
			PAKCorePairingRequester requester) {

		AndroidSettings.getInstance().setDeviceName(requester.getDeviceName());

		// internal storage, where databases and key-store file are to be stored
		AndroidSettings.getInstance().setConfDir(
				context.getFilesDir().getAbsolutePath());

		AndroidSettings.getInstance().writeChanges();

		Log.v("Android Default path:", context.getFilesDir().getAbsolutePath());

		AbstractIdentity identity = new Identity(new SimpleAddressbook());
		identity.setEmail(requester.geteMail());
		identity.setName(requester.getLastName());
		identity.setFirstName(requester.getFirstName());

		identity.setOwnerKeyEnc(requester.getOwnerCertEnc());
		identity.setOwnerKeySign(requester.getOwnerCertSign());

		identity.addDeviceKey(deviceKey, requester.getDevCert(),
				requester.getDeviceName());
		identity.addDeviceCert(requester.getDevCert(), AndroidSettings
				.getInstance().getDeviceName());

		PanboxManager panbox = PanboxManager.getInstance(context);

		try {
			// we assume the pairingfile to be trustworthy
			// w.r.t. the contact trust level
			panbox.getAddressbookManager().importContacts(identity,
					requester.getKnownContacts().toArray(new VCard[] {}), true);
			panbox.getIdentityManager().storeMyIdentity(identity);
			Log.v("AndroidPairingUtils: ", "finished pairing without errors");
		} catch (ContactExistsException e) {
			Log.v("AndroidPairingUtils: ",
					e.toString()
							+ ";"
							+ "PanboxClient : setUpIdentity : Could not import all contacts to addressbook.");
		}
		panbox = PanboxManager.getInstance(context);
		panbox.setIdentity(identity);
	}

	private void setupScanBluetoothDevices() {
		Log.d("PanboxExecutionActivity", "setupScanBluetoothDevices()");
		// Register the BroadcastReceiver
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(mReceiver, filter);
	}

	private void exitScanBluetoothDevices() {
		Log.d("PanboxExecutionActivity", "exitScanBluetoothDevices()");
		// Unregister the BroadcastReceiver
		unregisterReceiver(mReceiver);
	}

	@Override
	protected void onDestroy() {
		Log.d("PanboxExecutionActivity", "onDestroy()");
		exitScanBluetoothDevices();
		super.onDestroy();
	}

	@Override
	public void onPostExecute(InetAddress inetaddr) {

		Log.v("PairingExecutionActivity",
				"in NetworkResolutionResultListener::onPostExecute()");

		if (inetaddr != null) {

			AsyncTask<NetworkPairingParameters, Void, PAKCorePairingRequester> task = new NetworkPairingTask(
					this);

			task.execute(new NetworkPairingParameters[] { new NetworkPairingParameters(
					inetaddr, password, DeviceType.MOBILE, deviceKey) });
		} else {
			isBTpairing = true;
			// Try to connect via Bluetooth! If this also fails then the host
			// could not be connected.

			// Pairing will start after bluetooth scan has finished!
		}
	}

	@Override
	public void onPostExecute(PAKCorePairingRequester requester) {
		Log.v("PairingExecutionActivity",
				"in onPostExecute()");
		if (requester == null) {
			Log.e("PairingExecutionActivity:NetworkPairingResultListener:onPostExecute()",
					"requester is null");
			// Exception was thrown during pairing
			Intent assistentActivity = new Intent(this, AssistentActivity.class);
			startActivity(assistentActivity);
			finish();

		} else {
			Log.e("PairingExecutionActivity:NetworkPairingResultListener:onPostExecute()",
					"pairing was successful");
			finishGeneralPairing(getApplicationContext(), deviceKey, requester);

			// Pairing successful!
			Intent settingsActivity = new Intent(this, SettingsActivity.class);
			startActivity(settingsActivity);
			finish();
		}

	}

	// @Override
	// public void onBackPressed() {
	// Log.v("PairingExecutionActivity:","in onBackPressed(), finishing activity...");
	// finish();
	// }
}
