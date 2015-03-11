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
package org.panbox.desktop.linux.dbus;

import org.freedesktop.dbus.DBusInterface;

/**
 * Created by nolle on 26.11.13.
 */
public interface PanboxInterface extends DBusInterface {

	String getMountPoint();

	boolean isMounted();

	byte mount();

	byte unmount();

	byte addShare(String shareName, String shareType, String sharePath,
			byte[] password);

	byte removeShare(String shareName, boolean removeDir);

	byte editShare(String oldShareName, String newShareName,
			String newShareType, String newSharePath);

	// int getFileStatus(String path);

	byte openRevisionGui(String path);

	// void shareFile(String path);

	byte shareDirectory(String path);

	String[] getShares();
	
	boolean isRemote();

	byte openProperties();

	byte shutdown();

	void about();

	String getLocale();

	String getVersion();

	String[] getCSPs();

	byte exportOwnIdentity(String path);

	String[][] getContacts();

	byte exportContacts(String[] contactIDs, String path);
	
	byte verifyContacts(String vcard, String pin);
	
	String[][] getContacts(String vcard);

	byte importContact(String[] contactIDs, String vcard, boolean authVerified);

	byte createIdentity(String email, String firstname, String lastname,
			byte[] password, String devicename);

	byte deleteIdentity();

	byte resetIdentity(String email, String firstname, String lastname,
			byte[] password, String devicename, boolean backup);

	byte backupIdentity();

	byte restoreIdentity(String backupFile, boolean backupOldOne);

	String[][] getOwnIdentity();

	byte addContact(String mail, String firstname, String lastname);

	byte deleteContact(String[] contactIDs);

	String getShareStorageBackendType(String shareName);
}
