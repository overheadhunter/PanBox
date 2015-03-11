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
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.UnrecoverableKeyException;

import org.panbox.core.exception.ObfuscationException;
import org.panbox.core.exception.ShareMetaDataException;
import org.panbox.core.keymgmt.VolumeParams;
import org.panbox.desktop.common.gui.shares.PanboxShare;

public interface PanboxWindowsServiceInterface extends Remote {

	/**
	 * This service method is used to start the PanboxVFS from the
	 * PanboxWindowsService
	 */
	public void startupVFS() throws RemoteException;

	/**
	 * This service method is used to stop the PanboxVFS from the
	 * PanboxWindowsService
	 */
	public void shutdownVFS() throws RemoteException;

	/**
	 * This service method is used to shutdown the whole service and stop the
	 * exporting of methods;
	 * 
	 * @return true if unexport was successful, else false
	 * @throws RemoteException
	 */
	public void shutdownService() throws RemoteException;

	public void startupService() throws RemoteException;

	public String askLogin(String username) throws RemoteException;

	public PanboxClientSession authLogin(String username, String sharedSecret)
			throws RemoteException;

	public PanboxShare createShare(PanboxClientSession session, VolumeParams p)
			throws FileNotFoundException, IllegalArgumentException,
			ShareMetaDataException, RemoteException;

	public PanboxShare loadShare(PanboxClientSession session, VolumeParams p)
			throws ShareMetaDataException, RemoteException,
			FileNotFoundException;

	public PanboxShare reloadShareMetaData(PanboxClientSession session,
			VolumeParams p) throws ShareMetaDataException, RemoteException,
			FileNotFoundException;

	public PanboxShare acceptInviation(PanboxClientSession session,
			VolumeParams p) throws ShareMetaDataException, RemoteException,
			FileNotFoundException;

	public PanboxShare inviteUser(PanboxClientSession session, VolumeParams p)
			throws ShareMetaDataException, RemoteException;

	public PanboxShare addDevice(PanboxClientSession session, VolumeParams p)
			throws IllegalArgumentException, ShareMetaDataException,
			RemoteException;

	public PanboxShare removeDevice(PanboxClientSession session, VolumeParams p)
			throws IllegalArgumentException, ShareMetaDataException,
			UnrecoverableKeyException, RemoteException;

	public void removeShare(PanboxClientSession session, VolumeParams p)
			throws RemoteException;

	public String getOnlineFilename(PanboxClientSession session,
			VolumeParams p, String fileName) throws RemoteException,
			FileNotFoundException, ObfuscationException;
}