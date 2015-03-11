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
package org.panbox.desktop.common.gui.devices;

import java.security.PublicKey;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.apache.commons.codec.digest.DigestUtils;
import org.panbox.core.Utils;
import org.panbox.core.devicemgmt.DeviceType;

public class PanboxDevice {

	public static class UnknownDevice extends PanboxDevice {

		public UnknownDevice(String deviceName) {
			super(deviceName, DeviceType.DESKTOP, null);
		}

		@Override
		public String getDevicePubKeyFingerprint() {
			return "Unknown device.";
		}

	}

	public static Icon deviceIcon;

	private final String deviceName;
	private final DeviceType deviceType;
	private final PublicKey devicePubKey;

	public PanboxDevice(String deviceName, DeviceType deviceType,
			PublicKey devicePubKey) {
		this.deviceName = deviceName;
		this.deviceType = deviceType;
		this.devicePubKey = devicePubKey;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public DeviceType getDeviceType() {
		return deviceType;
	}

	public PublicKey getDevicePublicKey() {
		return devicePubKey;
	}

	public String getDevicePubKeyFingerprint() {
		return DigestUtils.sha256Hex(devicePubKey.getEncoded());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PanboxDevice) {
			PanboxDevice o = (PanboxDevice) obj;
			return Utils.keysEqual(devicePubKey, o.devicePubKey);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hc = 19;
		int mul = 79;
		hc = hc * mul + devicePubKey.hashCode();
		return hc;
	}

	public Icon getIcon() {
		if (deviceType == DeviceType.DESKTOP) {
			return new ImageIcon(getClass().getResource("device.png"),
					"Desktop Device Icon");
		} else {
			return new ImageIcon(getClass().getResource("device-mobile.png"),
					"Mobile Device Icon");
		}
	}

}
