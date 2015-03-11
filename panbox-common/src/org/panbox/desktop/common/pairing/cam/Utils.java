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
package org.panbox.desktop.common.pairing.cam;

import java.util.ArrayList;

import org.bytedeco.javacpp.videoInputLib.videoInput;
import org.panbox.OS;

public class Utils {
	public static ArrayList<CameraDevice> listVideoDevices() {
		ArrayList<CameraDevice> devices = new ArrayList<>();

		if (OS.getOperatingSystem().isWindows()) {
			// On Windows we can use videoInputLib to iterate over all webcams
			// and list them including their real names!
			int n = videoInput.listDevices();
			if (n > 0) {
				for (int i = 0; i < n; i++) {
					devices.add(new CameraDevice(i, videoInput.getDeviceName(i)
							.getString()));
				}
			}
		} else if (OS.getOperatingSystem().isLinux()) {
			// Linux does not provide any videoInputLib so we need to search for
			// webcams on our own! Sadly we won't have any device names
			// available then :(
			int numDevices = 0;
			while (true) {
				try {
					OpenCVWebCam webcam = new OpenCVWebCam(numDevices, 640, 480);
					webcam.start(); // throws Exception!
					webcam.stop(); // stops again if works!
					numDevices++; // search for more webcams on success!
				} catch (Exception ex) {
					// we don't have any webcam anymore!
					break;
				}
			}
			for (int i = 0; i < numDevices; i++) {
				devices.add(new CameraDevice(i, "WebCam #" + i));
			}
		}

		return devices;
	}
}
