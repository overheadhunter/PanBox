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
package org.panbox.test;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.security.KeyPair;
import java.util.prefs.Preferences;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.panbox.Settings;
import org.panbox.core.crypto.CryptCore;
import org.panbox.core.identitymgmt.CloudProviderInfo;
import org.panbox.core.identitymgmt.PanboxContact;
import org.panbox.core.pairing.PAKCorePairingHandler.PairingType;

public abstract class AbstractTest {

	@Rule
	public TemporaryFolder settingsFolder = new TemporaryFolder();

	public Preferences dummyPrefs;

	private final boolean DEBUG = true;

	public void setupSettings() {
		try {
			System.out.println("PATH: "
					+ settingsFolder.getRoot().getAbsolutePath());
			dummyPrefs = Preferences.userNodeForPackage(AbstractTest.class);
			dummyPrefs.clear();
			File newFolder = settingsFolder.newFolder(".conf");
			newFolder.mkdirs();
			File mountFolder = settingsFolder.newFolder("mount");
			mountFolder.mkdirs();
			dummyPrefs.put("confDir", newFolder.getAbsolutePath());
			dummyPrefs.put("mountDir", mountFolder.getAbsolutePath());
			Constructor<Settings> cons = Settings.class
					.getDeclaredConstructor(Preferences.class);
			cons.setAccessible(true);
			Settings settings = cons.newInstance(this.dummyPrefs);

			Field instance = Settings.class.getDeclaredField("instance");
			instance.setAccessible(true);
			instance.set(null, settings);
			settings.setPairingType(PairingType.MASTER);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public void debug(String message) {
		if (DEBUG) {
			System.out.println("[DEBUG] " + message);
		}
	}

	public void debug(String message, byte[] input) {
		if (DEBUG) {
			StringBuilder buffer = new StringBuilder("[DEBUG] ");
			buffer.append(message);
			buffer.append("[");
			for (int i = 0; i < input.length; i++) {
				buffer.append(input[i]);
				buffer.append(i < input.length - 1 ? ", " : "]");
			}
			System.out.println(buffer.toString());
		}
	}
	
	protected PanboxContact createContact(String name, String email)
	{
		PanboxContact contact = new PanboxContact();
		
		contact.setName(name);
		contact.setFirstName(name + ".first");
		contact.setEmail(email);
				
		KeyPair ownerKeySign = CryptCore.generateKeypair();
		KeyPair ownerKeyEnc = CryptCore.generateKeypair();
				
		contact.setCertEnc(CryptCore.createSelfSignedX509Certificate(ownerKeyEnc.getPrivate(), ownerKeyEnc.getPublic(), contact));
		contact.setCertSign(CryptCore.createSelfSignedX509Certificate(ownerKeySign.getPrivate(), ownerKeySign.getPublic(), contact));
		
		for(int i=0; i<3; i++)
		{
			CloudProviderInfo cpi = new CloudProviderInfo("CloudProvider-" + i, "CloudProviderUser-" + i);			
			contact.addCloudProvider(cpi);
		}		
		
		return contact;
	}
	
}