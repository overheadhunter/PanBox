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
package org.panbox.desktop.sharemgmt;

import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.security.KeyPair;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.panbox.Settings;
import org.panbox.core.crypto.AbstractObfuscatorFactory;
import org.panbox.core.crypto.CryptCore;
import org.panbox.core.crypto.FileObfuscatorFactory;
import org.panbox.core.devicemgmt.DeviceType;
import org.panbox.core.identitymgmt.Identity;
import org.panbox.core.identitymgmt.SimpleAddressbook;
import org.panbox.desktop.common.devicemgmt.DeviceManagerImpl;
import org.panbox.desktop.common.identitymgmt.sqlightimpl.AddressbookManager;
import org.panbox.desktop.common.identitymgmt.sqlightimpl.IdentityManager;
import org.panbox.desktop.common.sharemgmt.IPanboxService;
import org.panbox.desktop.common.sharemgmt.ShareManagerImpl;
import org.panbox.test.AbstractTest;

public abstract class ShareMgmtTest extends AbstractTest {

	public ShareManagerImpl manager;
	public AddressbookManager aBookMgr;
	public Identity id;

	final static char[] password = "test".toCharArray();
	
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();

	@Before
	public void setUp() throws Exception {
		// make environment headless, so that PasswordEnterDialog always return
		// "test" as password
		System.setProperty("java.awt.headless", "true");
		// test cleanup
		setupSettings();

		// test preparation
		IdentityManager identMgmr = IdentityManager.getInstance();

		// could not load identity -> create a new one!
		id = new Identity(new SimpleAddressbook(), "testIdentity@example.org",
				"FirstName", "Lastname");
		KeyPair ownerKeySign = CryptCore.generateKeypair();
		KeyPair ownerKeyEnc = CryptCore.generateKeypair();
		KeyPair deviceKey = CryptCore.generateKeypair();

		id.setOwnerKeySign(ownerKeySign, "test".toCharArray());
		id.setOwnerKeyEnc(ownerKeyEnc, "test".toCharArray());
		id.addDeviceKey(deviceKey, "TestDevice");
		Settings.getInstance().setDeviceName("TestDevice");
		aBookMgr = new AddressbookManager();
		identMgmr.init(aBookMgr);
		identMgmr.storeMyIdentity(id);
		DeviceManagerImpl.getInstance().setIdentity(id);
		try {
			DeviceManagerImpl.getInstance().addThisDevice("TestDevice",
					deviceKey, DeviceType.DESKTOP);
		} catch (Exception e) {
			e.printStackTrace();
		}

		AbstractObfuscatorFactory.getFactory(FileObfuscatorFactory.class);

		manager = ShareManagerImpl.getInstance(getPanboxService());
		manager.setIdentity(id);
	}

	@After
	public void tearDown() {
		try {
			// clear Prefs
			this.dummyPrefs.clear();
			// Clear SharemanagerImpl instance
			Field instance = ShareManagerImpl.class
					.getDeclaredField("instance");
			instance.setAccessible(true);
			instance.set(null, null);
			
			instance = DeviceManagerImpl.class.getDeclaredField("instance");
			instance.setAccessible(true);
			instance.set(null, null);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public abstract IPanboxService getPanboxService();
}
