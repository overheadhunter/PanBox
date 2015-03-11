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
package org.panbox.core.pairing.bluetooth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;

import org.apache.log4j.Logger;

/**
 * The RemoteDeviceDiscovery class implements a fully functional Bluetooth
 * RemoteDevice scanner and ServiceRecord scanner. First all devices that are in
 * range will be found and afterwards for all devices the ServiceRecord search
 * will be executed in order to find out if the found device is usable for
 * Panbox or not.
 */
public class RemoteDeviceDiscovery {

	private static final Logger logger = Logger.getLogger("org.panbox");

	static int counter = 0;
	private static final Object eventComplete = new Object();
	private static List<RemoteDevice> devicesDiscovered = new ArrayList<>();
	private static Map<String, List<ServiceRecord>> serviceRecordsByDeviceAddr = new HashMap<>();

	public static List<RemoteDevice> discover() throws BluetoothStateException,
			InterruptedException {
		devicesDiscovered.clear();
		serviceRecordsByDeviceAddr.clear();

		DiscoveryListener listener = new DiscoveryListener() {
			@Override
			public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
				devicesDiscovered.add(btDevice);
				try {
					logger.debug("RemoteDeviceDiscovery : Device "
							+ btDevice.getBluetoothAddress() + " ("
							+ btDevice.getFriendlyName(false) + ") found");
				} catch (IOException cantGetDeviceName) {
					// ignore this since we don't force for a device name
				}
			}

			@Override
			public void inquiryCompleted(int discType) {
				System.out
						.println("RemoteDeviceDiscovery : Device Inquiry completed!");
				synchronized (eventComplete) {
					eventComplete.notifyAll();
				}
			}

			@Override
			public void serviceSearchCompleted(int transID, int respCode) {
				--counter;
				if (counter == 0) {
					logger.debug("RemoteDeviceDiscovery : Service search completed!");
					synchronized (eventComplete) {
						eventComplete.notifyAll();
					}
				}
			}

			@Override
			public void servicesDiscovered(int transID,
					ServiceRecord[] servRecord) {
				for (ServiceRecord sr : servRecord) {
					System.out
							.println("RemoteDeviceDiscovery : Found services on device!");
					if (sr.getConnectionURL(
							ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false) != null) {
						logger.debug("RemoteDeviceDiscovery : Found Service on device: "
								+ sr.getConnectionURL(
										ServiceRecord.NOAUTHENTICATE_NOENCRYPT,
										false));
						serviceRecordsByDeviceAddr.get(
								sr.getHostDevice().getBluetoothAddress()
										.toUpperCase()).add(sr);
					}
				}
			}
		};

		synchronized (eventComplete) {
			boolean started = LocalDevice.getLocalDevice().getDiscoveryAgent()
					.startInquiry(DiscoveryAgent.GIAC, listener);
			if (started) {
				logger.debug("RemoteDeviceDiscovery : Wait for device inquiry to complete...");
				eventComplete.wait();
				logger.debug("RemoteDeviceDiscovery : "
						+ devicesDiscovered.size() + " device(s) found");
			}

			counter = devicesDiscovered.size();

			if (counter > 0) {
				logger.debug("RemoteDeviceDiscovery : Found at least one device. Will execute the ServiceRecord scan now.");
				UUID uuid = new UUID(0x1101); // UUID for SerialPort (might
												// change be changed to a Panbox
												// specific one!)
				UUID[] uuids = { uuid };

				for (RemoteDevice rd : devicesDiscovered) {
					serviceRecordsByDeviceAddr.put(rd.getBluetoothAddress()
							.toUpperCase(), new ArrayList<ServiceRecord>());
					LocalDevice.getLocalDevice().getDiscoveryAgent()
							.searchServices(null, uuids, rd, listener);
				}
				eventComplete.wait();
			} else {
				logger.debug("RemoteDeviceDiscovery : No devices found so no service discovery is needed!");
			}
		}

		return devicesDiscovered;
	}

	public static List<RemoteDevice> getDevicesDiscovered() {
		return devicesDiscovered;
	}

	public static List<ServiceRecord> getServiceRecordsByDeviceAddr(String addr) {
		return serviceRecordsByDeviceAddr.get(addr);
	}
}