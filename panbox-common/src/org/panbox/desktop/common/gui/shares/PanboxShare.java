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
package org.panbox.desktop.common.gui.shares;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.swing.Icon;

import org.apache.log4j.Logger;
import org.panbox.core.csp.StorageBackendType;
import org.panbox.core.identitymgmt.AbstractIdentity;
import org.panbox.core.keymgmt.IVolume;
import org.panbox.desktop.common.devicemgmt.DeviceManagerException;
import org.panbox.desktop.common.devicemgmt.DeviceManagerImpl;
import org.panbox.desktop.common.gui.addressbook.ContactShareParticipant;
import org.panbox.desktop.common.gui.addressbook.PanboxGUIContact;
import org.panbox.desktop.common.gui.devices.DeviceShareParticipant;
import org.panbox.desktop.common.gui.devices.PanboxDevice;

public abstract class PanboxShare implements Serializable {

	private final static Logger logger = Logger.getLogger(PanboxShare.class);

	protected volatile Icon icon;
	protected volatile int syncStatus;
	protected List<PanboxGUIContact> contacts = new ArrayList<>();
	protected List<PanboxDevice> devices = new ArrayList<>();

	// Things to serialize for RMI
	private static final long serialVersionUID = -1513650758608236985L;
	protected UUID uuid;
	protected String name;
	protected PublicKey ownerKey;
	protected HashMap<PublicKey, String> deviceMap;
	protected HashMap<PublicKey, String> shareParticipants;
	protected boolean owner;
	protected String path;
	protected Exception ex;

	public PanboxShare(UUID id, String name, String path,
			HashMap<PublicKey, String> deviceMap,
			HashMap<PublicKey, String> shareParticipants) {
		this.uuid = id;
		this.name = name;
		this.path = path;
		this.deviceMap = deviceMap;
		this.shareParticipants = shareParticipants;
	}

	public UUID getUuid() {
		return uuid;
	}

	private void loadPermissions(AbstractIdentity identity) {
		this.contacts.clear();
		for (PublicKey pk : this.shareParticipants.keySet()) {
			org.panbox.core.identitymgmt.PanboxContact contact = identity
					.resolveContactPublicKey(pk, shareParticipants.get(pk));
			if (contact != null) {
				this.contacts.add(new PanboxGUIContact(contact));
			}
		}

		DeviceManagerImpl dmi;
		try {
			dmi = DeviceManagerImpl.getInstance();
		} catch (DeviceManagerException e) {
			logger.fatal("Could not fetch DeviceManagerImpl. Quitting!", e);
			throw new RuntimeException(e);
		}
		this.devices.clear();
		for (String alias : deviceMap.values()) {
			try {
				this.devices.add(dmi.getDeviceIgnoreCase(alias));
			} catch (DeviceManagerException e) {
				logger.warn("Unknown Device from Share detected: " + alias);
				this.devices.add(new PanboxDevice.UnknownDevice(alias));
			}
		}
	}

	public Icon getIcon() {
		return icon;
	}

	public String getName() {
		return name;
	}

	abstract public StorageBackendType getType();

	public int getSyncStatus() {
		return syncStatus;
	}

	public List<PanboxGUIContact> getContacts() {
		return contacts;
	}

	public List<PanboxDevice> getDevices() {
		return devices;
	}

	public ShareParticipantListModel generatePermissionsModel(
			AbstractIdentity id) {
		loadPermissions(id);
		ShareParticipantListModel model = new ShareParticipantListModel();

		for (PanboxDevice d : devices) {
			model.addElement(new DeviceShareParticipant(d));
		}
		for (PanboxGUIContact c : contacts) {
			model.addElement(new ContactShareParticipant(c));
		}

		return model;
	}

	private void setOwner(boolean o) {
		this.owner = o;
	}

	public boolean isOwner() {
		return this.owner;
	}

	public boolean isOwner(PublicKey userkey) {
		return ownerKey.equals(userkey);
	}

	private void setException(Exception e) {
		this.ex = e;
	}

	public Exception getException() {
		return ex;
	}

	public static PanboxShare fromVolume(String name, String path, IVolume v,
			PublicKey owner, PublicKey user, StorageBackendType type,
			Exception ex) {
		PanboxShare result = fromVolume(name, path, v, owner, user, type);
		result.setException(ex);
		return result;
	}

	public static PanboxShare fromVolume(String name, String path, IVolume v,
			PublicKey owner, PublicKey user, StorageBackendType type) {
		PanboxShare result = null;
		switch (type) {
		case FOLDER:
			result = new FolderPanboxShare(v.getUUID(), name, path,
					v.getDeviceMap(user), v.getShareParticipants());
			break;
		case DROPBOX:
			result = new DropboxPanboxShare(v.getUUID(), name, path,
					v.getDeviceMap(user), v.getShareParticipants());
			break;
		default:
			throw new RuntimeException("Unknown share type");
		}

		result.ownerKey = owner;
		result.setOwner(owner.equals(user));
		return result;
	}

	public PanboxShare(String id, String path, String name, int syncStatus) {
		this.syncStatus = syncStatus;
		this.path = path;
		this.name = name;
		if (id != null)
			this.uuid = UUID.fromString(id);
	}

	public String getPath() {
		return this.path;
	}
}
