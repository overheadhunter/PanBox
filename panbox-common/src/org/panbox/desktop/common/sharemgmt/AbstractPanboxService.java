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
package org.panbox.desktop.common.sharemgmt;

import java.io.File;
import java.io.FileNotFoundException;
import java.rmi.RemoteException;
import java.security.KeyPair;
import java.security.UnrecoverableKeyException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.panbox.PanboxConstants;
import org.panbox.core.exception.DeviceListException;
import org.panbox.core.exception.ObfuscationException;
import org.panbox.core.exception.ShareMetaDataException;
import org.panbox.core.keymgmt.IVolume;
import org.panbox.core.keymgmt.Volume;
import org.panbox.core.keymgmt.VolumeParams;
import org.panbox.core.vfs.backend.VirtualVolume;
import org.panbox.desktop.common.ex.DeviceKeyException;
import org.panbox.desktop.common.gui.shares.PanboxShare;
import org.panbox.desktop.common.vfs.backend.VFSShare;

public abstract class AbstractPanboxService implements IPanboxService {

	private final HashMap<String, IVolume> volumeCache = new HashMap<>();
	static final Logger logger = Logger.getLogger("org.panbox");

	protected abstract void registerShare(VFSShare vfsShare, VolumeParams p);

	protected abstract boolean unregisterShare(VolumeParams p);

	protected abstract VirtualVolume getVirtualVolume(VolumeParams p)
			throws FileNotFoundException;

	private synchronized IVolume createVolume(VolumeParams p)
			throws IllegalArgumentException, ShareMetaDataException {
		String metaDataDir = p.path + File.separator
				+ PanboxConstants.PANBOX_SHARE_METADATA_DIRECTORY
				+ File.separator;
		IVolume result = new Volume(metaDataDir);
		result.createShareMetaData(p);
		this.volumeCache.put(p.path, result);
		return result;
	}

	private synchronized IVolume getVolumeFromCache(VolumeParams p)
			throws ShareMetaDataException {
		IVolume volume = this.volumeCache.get(p.path);
		if (volume == null) {
			throw new ShareMetaDataException(
					"Called a method on non-existing IVolume!");
		}
		return volume;
	}

	@Override
	public synchronized PanboxShare reloadShareMetaData(VolumeParams p)
			throws ShareMetaDataException, RemoteException,
			FileNotFoundException {

		IVolume volume = this.volumeCache.get(p.path);
		if (volume == null) {
			throw new ShareMetaDataException(
					"Called a method on non existing IVolume!");
		}
		volume.reload();
		return PanboxShare.fromVolume(p.shareName, p.path, volume,
				volume.getOwnerKey(), p.pubSigKey, p.type);
	}

	@Override
	public synchronized PanboxShare createShare(VolumeParams p)
			throws FileNotFoundException, IllegalArgumentException,
			ShareMetaDataException, RemoteException {
		VirtualVolume virtualVolume = getVirtualVolume(p);

		IVolume volume = createVolume(p);
		VFSShare vfsShare;
		try {
			vfsShare = new VFSShare(p.shareName, p.path, virtualVolume, volume,
					new KeyPair(p.deviceKey, p.devicePrivateKey));
		} catch (DeviceKeyException e) {
			// This should never ever happen:
			logger.fatal("No device keys found after creating a share!", e);
			throw new ShareMetaDataException(
					"No device keys found after creating a share!", e);
		}
		registerShare(vfsShare, p);
		logger.debug("PanboxService: createShare: Successfully added share to Panbox! Sharename: "
				+ p.shareName);
		return PanboxShare.fromVolume(p.shareName, p.path, volume, p.pubSigKey,
				p.pubSigKey, p.type);
	}

	@Override
	public synchronized PanboxShare loadShare(VolumeParams p)
			throws ShareMetaDataException, RemoteException,
			FileNotFoundException {
		String metaDataDir = p.path + File.separator
				+ PanboxConstants.PANBOX_SHARE_METADATA_DIRECTORY
				+ File.separator;
		IVolume volume = new Volume(metaDataDir);
		Exception ex = null;
		try {
			volume.loadShareMetaData(p.ownerSigKey);
			this.volumeCache.put(p.path, volume);
		} catch (ShareMetaDataException e) {
			if (e.getCause() instanceof DeviceListException) {
				ex = (DeviceListException) e.getCause();
				this.volumeCache.put(p.path, volume);
			} else {
				throw e;
			}
		}

		VirtualVolume virtualVolume = getVirtualVolume(p);

		VFSShare vfsShare;
		try {
			vfsShare = new VFSShare(p.shareName, p.path, virtualVolume, volume,
					new KeyPair(p.deviceKey, p.devicePrivateKey));
		} catch (DeviceKeyException e) {
			// At this point, possibly because of a conflict, the changes of
			// acceptInvitation have been reverted, so no keys for the user's
			// device have been found.
			throw new ShareMetaDataException(
					"No Obfuscation Key found for device " + p.deviceAlias, e);
		}
		registerShare(vfsShare, p);
		logger.debug("PanboxService : loadShare : Successfully added share to Panbox! Sharename: "
				+ p.shareName);

		return PanboxShare.fromVolume(p.shareName, p.path, volume,
				p.ownerSigKey, p.pubSigKey, p.type, ex);
	}

	@Override
	public synchronized PanboxShare acceptInviation(VolumeParams p)
			throws ShareMetaDataException, RemoteException,
			FileNotFoundException {
		String metaDataDir = p.path + File.separator
				+ PanboxConstants.PANBOX_SHARE_METADATA_DIRECTORY
				+ File.separator;
		IVolume volume = new Volume(metaDataDir);
		Exception ex = null;
		try {
			volume.acceptInvitation(p);
			this.volumeCache.put(p.path, volume);
		} catch (ShareMetaDataException e) {
			if (e.getCause() instanceof DeviceListException) {
				ex = (DeviceListException) e.getCause();
				this.volumeCache.put(p.path, volume);
			} else {
				throw e;
			}
		}

		VirtualVolume virtualVolume = getVirtualVolume(p);
		VFSShare vfsShare;
		try {
			vfsShare = new VFSShare(p.shareName, p.path, virtualVolume, volume,
					new KeyPair(p.deviceKey, p.devicePrivateKey));
		} catch (DeviceKeyException e) {
			// At this point, possibly because of a conflict, the changes of
			// acceptInvitation above have been reverted so no keys for the
			// users device have been found.
			throw new ShareMetaDataException(
					"No Obfuscation Key found for device " + p.deviceAlias, e);
		}
		registerShare(vfsShare, p);
		logger.debug("PanboxService : loadShare : Successfully added share to Panbox! Sharename: "
				+ p.shareName);

		return PanboxShare.fromVolume(p.shareName, p.path, volume,
				p.ownerSigKey, p.pubSigKey, p.type, ex);

	}

	@Override
	public synchronized PanboxShare inviteUser(VolumeParams p)
			throws ShareMetaDataException, RemoteException {

		IVolume volume = getVolumeFromCache(p);
		volume.inviteUser(p);
		logger.debug("PanboxService : inviteUser : Successfully invited user to share! Sharename: "
				+ p.shareName);
		return PanboxShare.fromVolume(p.shareName, p.path, volume,
				volume.getOwnerKey(), p.pubSigKey, p.type);
	}

	@Override
	public synchronized PanboxShare addDevice(VolumeParams p)
			throws IllegalArgumentException, ShareMetaDataException,
			RemoteException {
		IVolume volume = getVolumeFromCache(p);
		volume.addDevice(p);
		logger.info("PanboxService : addDevice : Successfully added device to share! Sharename: "
				+ p.shareName);
		return PanboxShare.fromVolume(p.shareName, p.path, volume,
				volume.getOwnerKey(), p.pubSigKey, p.type);
	}

	@Override
	public synchronized PanboxShare removeDevice(VolumeParams p)
			throws IllegalArgumentException, ShareMetaDataException,
			UnrecoverableKeyException, RemoteException {
		IVolume volume = getVolumeFromCache(p);
		logger.info("PanboxService : removeDevice : Successfully removed device from share! Sharename: "
				+ p.shareName);
		return PanboxShare.fromVolume(p.shareName, p.path, volume,
				volume.getOwnerKey(), p.pubSigKey, p.type);
	}

	@Override
	public synchronized void removeShare(VolumeParams p) throws RemoteException {
		boolean removed = unregisterShare(p);
		if (removed) {
			logger.debug("PanboxService : removeShare : Successfully removed share from Panbox! Sharename: "
					+ p.shareName);
			this.volumeCache.remove(p.path);
		} else {
			// TODO: Should an Exception be thrown here?
			logger.error("PanboxService : removeShare : Failed to remove share from Panbox! Sharename: "
					+ p.shareName);
		}
	}

	@Override
	public abstract String getOnlineFilename(VolumeParams p, String fileName)
			throws RemoteException, FileNotFoundException, ObfuscationException;

}
