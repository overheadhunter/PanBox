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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.security.KeyPair;
import java.util.prefs.Preferences;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.panbox.Settings;
import org.panbox.core.crypto.AbstractObfuscatorFactory;
import org.panbox.core.crypto.CryptCore;
import org.panbox.core.crypto.FileObfuscatorFactory;
import org.panbox.core.identitymgmt.IAddressbook;
import org.panbox.core.identitymgmt.Identity;
import org.panbox.core.identitymgmt.SimpleAddressbook;
import org.panbox.desktop.common.devicemgmt.DeviceManagerImpl;
import org.panbox.desktop.common.gui.shares.FolderPanboxShare;
import org.panbox.desktop.common.gui.shares.PanboxShare;
import org.panbox.desktop.common.identitymgmt.sqlightimpl.AddressbookManager;
import org.panbox.desktop.common.identitymgmt.sqlightimpl.IdentityManager;
import org.panbox.desktop.common.sharemgmt.ShareManagerImpl;

public abstract class InviteUserTest extends ShareMgmtTest {

	@Rule
	public TemporaryFolder env2 = new TemporaryFolder();

	public MyClassLoader cl = new MyClassLoader();

	public File guestvcf;
	public File ownervcf;

	public Object manager2;

	@Test
	public void inviteUser() {
		try {
			String folder = "test123share";
			String shareName = "Test123";

			File test123share = testFolder.newFolder(folder);
			test123share.mkdirs();

			PanboxShare s = new FolderPanboxShare(null,
					test123share.getAbsolutePath(), shareName, 0);
			s = manager.addNewShare(s, password);

			PanboxShare share = manager.getShareForName(shareName);

			assertEquals(shareName, share.getName());
			assertEquals(test123share.getAbsolutePath(), share.getPath());
			debug("new Share created");

			debug("creating 2nd user environment");
			ownervcf = testFolder.newFile("ownerid.vcf");
			IdentityManager.getInstance().exportMyIdentity(id, ownervcf);

			setupSettings2();

			debug("Inviting user to share");
			aBookMgr.importContacts(id, guestvcf, true);
			assertEquals(0, s.getContacts().size());
			assertEquals(1, s.getDevices().size());
			share = manager.addContactPermission(share,
					"testIdentity2@example.org", password);
			assertEquals(1, share.getContacts().size());
			assertEquals(1, share.getDevices().size());

			debug("Accepting invite");

			Object share2 = Class
					.forName(FolderPanboxShare.class.getName(), true, cl)
					.getConstructor(String.class, String.class, String.class,
							int.class)
					.newInstance(null, test123share.getAbsolutePath(),
							shareName, 0);

			Method[] methods = Class.forName(ShareManagerImpl.class.getName(),
					true, cl).getMethods();
			for (int i = 0; i < methods.length; i++) {
				Method m = methods[i];
				if (m.getName().equals("addNewShare")
						&& m.getParameterTypes().length == 1) {
					share2 = m.invoke(manager2, share2);
				}
			}

			assertEquals(1, share.getContacts().size());
			assertEquals(1, share.getDevices().size());

			debug("Test successful");

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void setupSettings2() {
		try {
			System.out.println("PATH: "
					+ settingsFolder.getRoot().getAbsolutePath());
			Preferences prefs = Preferences
					.userNodeForPackage(InviteUserTest.class);
			prefs.clear();
			File newFolder = env2.newFolder(".conf");
			newFolder.mkdirs();
			File mountFolder = env2.newFolder("mount");
			mountFolder.mkdirs();
			prefs.put("confDir", newFolder.getAbsolutePath());
			prefs.put("mountDir", mountFolder.getAbsolutePath());
			prefs.put("deviceName", "TestDevice2");

			Class<Settings> clazz = (Class<Settings>) Class.forName(
					Settings.class.getName(), true, cl);
			Constructor<Settings> cons = (Constructor<Settings>) clazz
					.getDeclaredConstructor(Preferences.class);
			cons.setAccessible(true);

			Field instance = clazz.getDeclaredField("instance");
			instance.setAccessible(true);
			instance.set(null, cons.newInstance(prefs));

			// test preparation
			Class<IdentityManager> identMgrClazz = (Class<IdentityManager>) Class
					.forName(IdentityManager.class.getName(), true, cl);

			// could not load identity -> create a new one!
			Class<SimpleAddressbook> simplAddr = (Class<SimpleAddressbook>) Class
					.forName(SimpleAddressbook.class.getName(), true, cl);
			Class<IAddressbook> iAddr = (Class<IAddressbook>) Class.forName(
					IAddressbook.class.getName(), true, cl);

			Class<Identity> idclazz = (Class<Identity>) Class.forName(
					Identity.class.getName(), true, cl);
			Object id = idclazz.getConstructor(iAddr, String.class,
					String.class, String.class).newInstance(
					simplAddr.newInstance(), "testIdentity2@example.org",
					"FirstName2", "Lastname2");
			KeyPair ownerKeySign = CryptCore.generateKeypair();
			KeyPair ownerKeyEnc = CryptCore.generateKeypair();
			KeyPair deviceKey = CryptCore.generateKeypair();
			idclazz.getMethod("setOwnerKeySign", KeyPair.class, char[].class)
					.invoke(id, ownerKeySign, "test".toCharArray());
			idclazz.getMethod("setOwnerKeyEnc", KeyPair.class, char[].class)
					.invoke(id, ownerKeyEnc, "test".toCharArray());
			idclazz.getMethod("addDeviceKey", KeyPair.class, String.class)
					.invoke(id, deviceKey, "TestDevice2");
			Class<?> abookmgrclazz = Class.forName(
					AddressbookManager.class.getName(), true, cl);
			Object abookmgr = abookmgrclazz.newInstance();
			Object identityManager = identMgrClazz.getMethod("getInstance",
					null).invoke(null, null);

			Method[] methods = identMgrClazz.getMethods();
			for (int i = 0; i < methods.length; i++) {
				Method m = methods[i];
				if (m.getName().equals("init")) {
					m.invoke(identityManager, abookmgr);
					break;
				}

			}
			for (int i = 0; i < methods.length; i++) {
				Method m = methods[i];
				if (m.getName().equals("storeMyIdentity")) {
					m.invoke(identityManager, id);
					break;
				}

			}

			// DeviceManagerImpl.getInstance().addThisDevice("TestDevice",
			// deviceKey, DeviceType.DESKTOP);
			Class<DeviceManagerImpl> dmiclazz = (Class<DeviceManagerImpl>) Class
					.forName(DeviceManagerImpl.class.getName(), true, cl);
			Object dmi = dmiclazz.getMethod("getInstance").invoke(null);
			methods = dmiclazz.getMethods();
			for (int i = 0; i < methods.length; i++) {
				Method m = methods[i];
				if (m.getName().equals("setIdentity")) {
					m.invoke(dmi, id);
				}
				
			}
			for (int i = 0; i < methods.length; i++) {
				Method m = methods[i];
				if (m.getName().equals("addThisDevice")) {
					m.invoke(dmi, "TestDevice2", deviceKey, null);
				}

			}
			

			Class<AbstractObfuscatorFactory> obclazz = (Class<AbstractObfuscatorFactory>) Class
					.forName(AbstractObfuscatorFactory.class.getName(), true,
							cl);

			Class<FileObfuscatorFactory> fobclazz = (Class<FileObfuscatorFactory>) Class
					.forName(FileObfuscatorFactory.class.getName(), true, cl);

			// AbstractObfuscatorFactory.getFactory(FileObfuscatorFactory.class);
			obclazz.getMethod("getFactory", Class.class).invoke(null, fobclazz);

			Class<?> smiclazz = (Class<?>) Class.forName(
					ShareManagerImpl.class.getName(), true, cl);

			methods = smiclazz.getMethods();
			for (int i = 0; i < methods.length; i++) {
				Method m = methods[i];
				if (m.getName().equals("getInstance")
						&& m.getParameterTypes().length == 1) {
					manager2 = m.invoke(null, getPanboxService2());
					break;
				}
			}
			for (int i = 0; i < methods.length; i++) {
				Method m = methods[i];
				if (m.getName().equals("setIdentity")
						&& m.getParameterTypes().length == 1) {
					m.invoke(manager2, id);
					break;
				}
			}

			guestvcf = env2.newFile("guest.vcf");

			methods = identMgrClazz.getMethods();
			for (int i = 0; i < methods.length; i++) {
				Method m = methods[i];
				if (m.getName().equals("exportMyIdentity")
						&& m.getParameterTypes().length == 2) {
					m.invoke(identityManager, id, guestvcf);
					break;
				}
			}

			methods = abookmgrclazz.getMethods();
			for (int i = 0; i < methods.length; i++) {
				Method m = methods[i];
				if (m.getName().equals("importContacts")
						&& m.getParameterTypes().length == 2) {
					m.invoke(abookmgr, id, ownervcf);
					break;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public abstract Object getPanboxService2();

	public static class MyClassLoader extends URLClassLoader {

		public MyClassLoader() {
			super(((URLClassLoader) getSystemClassLoader()).getURLs());
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			if (name.startsWith("org.panbox.")) {
				return super.findClass(name);
			}
			return super.loadClass(name);
		}

	}

}
