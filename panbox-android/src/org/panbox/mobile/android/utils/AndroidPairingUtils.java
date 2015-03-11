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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;

import org.panbox.core.devicemgmt.DeviceType;
import org.panbox.core.pairing.PAKCorePairingRequester;
import org.panbox.core.pairing.network.PAKNetworkPairingRequester;
import org.panbox.mobile.android.pairing.bluetooth.PAKBluetoothPairingRequester;

import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;
import android.util.Log;

public class AndroidPairingUtils {

	public static interface NetworkResolutionResultListener {
	    void onPostExecute(InetAddress result);
	}
	public static interface PairingResultListener {
		void onPostExecute(PAKCorePairingRequester result);
	}
	public static class BluetoothPairingParameters {
		private final BluetoothDevice bluetoothDevice;
		private final String password;
		private final DeviceType devType;
		private final KeyPair devKeyPair;

		public BluetoothPairingParameters(BluetoothDevice bluetoothDevice,
				String password, DeviceType devType, KeyPair devKeyPair) {
			this.bluetoothDevice = bluetoothDevice;
			this.password = password;
			this.devType = devType;
			this.devKeyPair = devKeyPair;
		}

		public KeyPair getDevKeyPair() {
			return devKeyPair;
		}

		public DeviceType getDevType() {
			return devType;
		}

		public BluetoothDevice getBluetoothDevice() {
			return bluetoothDevice;
		}

		public String getPassword() {
			return password;
		}
	}

	public static class BluetoothPairingTask
			extends
			AsyncTask<BluetoothPairingParameters, Void, PAKCorePairingRequester> {

		PairingResultListener listener;
		
		public BluetoothPairingTask(PairingResultListener listener) {
			this.listener = listener;
		}
		
		@Override
		protected void onPostExecute(PAKCorePairingRequester result) {
			super.onPostExecute(result);
			
			listener.onPostExecute(result);
		}
		@Override
		protected PAKCorePairingRequester doInBackground(
				BluetoothPairingParameters... params) {
			if (params.length != 1) {
				return null;
			} else {
				BluetoothPairingParameters param = params[0];
				try {
					PAKBluetoothPairingRequester requester = new PAKBluetoothPairingRequester(
							param.getPassword(), param.getDevType(),
							param.getDevKeyPair(), param.getBluetoothDevice());
					requester.runProtocol();
					return requester;
				} catch (IOException e) {
					Log.e("AndroidPairingUtils",
							"Exception thrown during bluetooth pairing", e);
				} catch (InvalidKeyException e) {
					Log.e("AndroidPairingUtils",
							"Exception thrown during bluetooth pairing", e);
				} catch (NoSuchAlgorithmException e) {
					Log.e("AndroidPairingUtils",
							"Exception thrown during bluetooth pairing", e);
				} catch (NoSuchPaddingException e) {
					Log.e("AndroidPairingUtils",
							"Exception thrown during bluetooth pairing", e);
				} catch (ClassNotFoundException e) {
					Log.e("AndroidPairingUtils",
							"Exception thrown during bluetooth pairing", e);
				} catch (Exception e) {
					Log.e("AndroidPairingUtils",
							"Exception thrown during bluetooth pairing", e);
				}
			}

			return null;
		}

	}

	public static class NetworkPairingParameters {
		private final InetAddress ipAddress;
		private final String password;
		private final DeviceType devType;
		private final KeyPair devKeyPair;

		public NetworkPairingParameters(InetAddress ipAddress, String password,
				DeviceType devType, KeyPair devKeyPair) {
			this.ipAddress = ipAddress;
			this.password = password;
			this.devType = devType;
			this.devKeyPair = devKeyPair;
		}

		public KeyPair getDevKeyPair() {
			return devKeyPair;
		}

		public DeviceType getDevType() {
			return devType;
		}

		public InetAddress getIpAddress() {
			return ipAddress;
		}

		public String getPassword() {
			return password;
		}
	}

	public static class NetworkResolutionTask extends
			AsyncTask<String, Void, InetAddress> {
		
		NetworkResolutionResultListener listener;
		
		public NetworkResolutionTask(NetworkResolutionResultListener listener){
			this.listener = listener;
		}
		
		@Override
		protected void onPostExecute(InetAddress result) {
			super.onPostExecute(result);
			
			listener.onPostExecute(result);
		}
		@Override
		protected InetAddress doInBackground(String... params) {
			Log.v("AndroidPairingUtils:NetworkResolutionTask", "in doInBackground()");
			try {
				return InetAddress.getByName(params[0]);
			} catch (UnknownHostException e) {
				Log.d("AndroidPairingUtils", "Failed to obtain network address. Perhaps this is a bluetooth address? Addr: " + params[0]);
				return null; // Host not found! Perhaps Bluetooth?
			}
		}
		

	}

	public static class NetworkPairingTask extends
			AsyncTask<NetworkPairingParameters, Void, PAKCorePairingRequester> {

		PairingResultListener listener;
		
		public NetworkPairingTask(PairingResultListener listener) {
			this.listener = listener;
		}
		
		@Override
		protected void onPostExecute(PAKCorePairingRequester result) {
			super.onPostExecute(result);
			
			listener.onPostExecute(result);
		}
		@Override
		protected PAKCorePairingRequester doInBackground(
				NetworkPairingParameters... params) {
			if (params.length != 1) {
				return null;
			} else {
				NetworkPairingParameters param = params[0];
				try {
					PAKNetworkPairingRequester requester = new PAKNetworkPairingRequester(
							param.ipAddress, param.password, param.devType,
							param.devKeyPair);
					requester.runProtocol();
					return requester;
				} catch (InvalidKeyException e) {
					Log.e("AndroidPairingUtils",
							"Exception thrown during network pairing", e);
				} catch (NoSuchAlgorithmException e) {
					Log.e("AndroidPairingUtils",
							"Exception thrown during network pairing", e);
				} catch (NoSuchPaddingException e) {
					Log.e("AndroidPairingUtils",
							"Exception thrown during network pairing", e);
				} catch (IOException e) {
					Log.e("AndroidPairingUtils",
							"Exception thrown during network pairing", e);
				} catch (ClassNotFoundException e) {
					Log.e("AndroidPairingUtils",
							"Exception thrown during network pairing", e);
				} catch (Exception e) {
					Log.e("AndroidPairingUtils",
							"Exception thrown during network pairing", e);
				}
				return null;
			}
		}

	}

}
