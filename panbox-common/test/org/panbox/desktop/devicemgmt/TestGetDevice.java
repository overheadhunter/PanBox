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
package org.panbox.desktop.devicemgmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.panbox.desktop.common.devicemgmt.DeviceManagerException;
import org.panbox.desktop.common.devicemgmt.DeviceManagerImpl;
import org.panbox.desktop.common.gui.devices.PanboxDevice;

public class TestGetDevice {

	private DeviceManagerImpl manager;

	@Before
	public void setUp() throws Exception {
		manager = DeviceManagerImpl.getInstance();
	}

	@Test
	public void test() {
		try {
			assertEquals(4, manager.getDeviceList().size()); // check if both
																// add tests ran

			PanboxDevice device = manager.getDevice("testdeviceDesktop");

			assertEquals("testdeviceDesktop", device.getDeviceName());

			PanboxDevice device2 = manager.getDevice("testdeviceMobile");

			assertEquals("testdeviceMobile", device2.getDeviceName());
		} catch (DeviceManagerException e) {
			e.printStackTrace();
			fail();
		}
	}
}
