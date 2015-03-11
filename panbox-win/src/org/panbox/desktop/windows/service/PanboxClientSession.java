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
package org.panbox.desktop.windows.service;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.security.UnrecoverableKeyException;

import org.panbox.core.exception.ObfuscationException;
import org.panbox.core.exception.ShareMetaDataException;
import org.panbox.core.keymgmt.VolumeParams;
import org.panbox.desktop.common.gui.shares.PanboxShare;
import org.panbox.desktop.common.sharemgmt.IPanboxService;

public class PanboxClientSession implements Serializable, IPanboxService {

	private static final long serialVersionUID = -6065726595547632991L;

	private final int sessionId;

	private PanboxWindowsServiceInterface service = null;

	public PanboxClientSession(int sessionId) {
		this.sessionId = sessionId;
	}

	public void setService(PanboxWindowsServiceInterface service) {
		this.service = service;
	}

	@Override
	public PanboxShare createShare(VolumeParams p)
			throws FileNotFoundException, IllegalArgumentException,
			 ShareMetaDataException, RemoteException {
		if (service != null) {
			return service.createShare(this, p);
		} else {
			throw new RemoteException(
					"PanboxClientSession : createShare : Panbox Windows Service was not connected.");
		}
	}

	@Override
	public PanboxShare loadShare(VolumeParams p)
			throws  ShareMetaDataException,
			RemoteException, FileNotFoundException {
		if (service != null) {
			return service.loadShare(this, p);
		} else {
			throw new RemoteException(
					"PanboxClientSession : loadShare : Panbox Windows Service was not connected.");
		}
	}

	@Override
	public PanboxShare acceptInviation(VolumeParams p)
			throws  ShareMetaDataException,
			RemoteException, FileNotFoundException {
		if (service != null) {
			return service.acceptInviation(this, p);
		} else {
			throw new RemoteException(
					"PanboxClientSession : acceptInviation : Panbox Windows Service was not connected.");
		}
	}

	@Override
	public PanboxShare inviteUser(VolumeParams p)
			throws  ShareMetaDataException,
			RemoteException {
		if (service != null) {
			return service.inviteUser(this, p);
		} else {
			throw new RemoteException(
					"PanboxClientSession : inviteUser : Panbox Windows Service was not connected.");
		}
	}

	@Override
	public PanboxShare addDevice(VolumeParams p)
			throws IllegalArgumentException, 
			ShareMetaDataException, RemoteException {
		if (service != null) {
			return service.addDevice(this, p);
		} else {
			throw new RemoteException(
					"PanboxClientSession : addDevice : Panbox Windows Service was not connected.");
		}
	}

	@Override
	public PanboxShare removeDevice(VolumeParams p)
			throws IllegalArgumentException, ShareMetaDataException,
			 UnrecoverableKeyException, RemoteException {
		if (service != null) {
			return service.removeDevice(this, p);
		} else {
			throw new RemoteException(
					"PanboxClientSession : removeDevice : Panbox Windows Service was not connected.");
		}
	}

	@Override
	public void removeShare(VolumeParams p) throws RemoteException {
		if (service != null) {
			service.removeShare(this, p);
		} else {
			throw new RemoteException(
					"PanboxClientSession : removeShare : Panbox Windows Service was not connected.");
		}
	}

	@Override
	public String getOnlineFilename(VolumeParams p, String fileName)
			throws RemoteException, FileNotFoundException, ObfuscationException {
		if (service != null) {
			return service.getOnlineFilename(this, p, fileName);
		} else {
			throw new RemoteException(
					"PanboxClientSession : getOnlineFilename : Panbox Windows Service was not connected.");
		}
	}

	@Override
	public int hashCode() {
		return sessionId;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PanboxClientSession) {
			PanboxClientSession other = (PanboxClientSession) obj;
			if (other.sessionId == this.sessionId) {
				return true;
			}
		}
		return false;
	}

	@Override
	public PanboxShare reloadShareMetaData(VolumeParams p)
			throws ShareMetaDataException, RemoteException,
			FileNotFoundException {
		if (service != null) {
			return service.reloadShareMetaData(this, p);
		} else {
			throw new RemoteException(
					"PanboxClientSession : removeShare : Panbox Windows Service was not connected.");
		}		
	}
}
