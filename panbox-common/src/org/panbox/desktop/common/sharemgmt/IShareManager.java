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

import java.io.FileNotFoundException;
import java.security.UnrecoverableKeyException;
import java.util.Collection;
import java.util.List;

import org.panbox.core.csp.StorageBackendType;
import org.panbox.core.exception.ShareMetaDataException;
import org.panbox.core.identitymgmt.AbstractIdentity;
import org.panbox.core.identitymgmt.PanboxContact;
import org.panbox.core.keymgmt.VolumeParams.VolumeParamsFactory;
import org.panbox.desktop.common.gui.shares.PanboxShare;

public interface IShareManager {

	public List<String> getInstalledShares() throws ShareManagerException;

	public PanboxShare getShareForName(String shareName)
			throws ShareDoesNotExistException, ShareManagerException,
			UnrecoverableKeyException, ShareMetaDataException;

	public boolean shareNameAvailable(String shareName)
			throws ShareManagerException, UnrecoverableKeyException,
			ShareMetaDataException;

	public void setIdentity(AbstractIdentity identity);

	public PanboxShare addNewShare(PanboxShare share, char[] password)
			throws ShareManagerException, ShareNameAlreadyExistsException,
			SharePathAlreadyExistsException, UnrecoverableKeyException,
			ShareMetaDataException;

	public PanboxShare addDevicePermission(PanboxShare share,
			String deviceName, char[] password)
			throws ShareDoesNotExistException, ShareManagerException,
			UnrecoverableKeyException, ShareMetaDataException;

	public PanboxShare addContactPermission(PanboxShare share, String email,
			char[] password) throws ShareDoesNotExistException,
			ShareManagerException, UnrecoverableKeyException,
			ShareMetaDataException;

	public PanboxShare editShare(String shareName, String newShareName,
			StorageBackendType newShareType, String newSharePath,
			char[] password) throws FileNotFoundException,
			ShareManagerException, ShareNameAlreadyExistsException,
			SharePathAlreadyExistsException, UnrecoverableKeyException,
			ShareMetaDataException;

	void removeShare(String shareName, String sharePath, StorageBackendType type)
			throws ShareManagerException;

	public VolumeParamsFactory getParamsFactory();

	public String getOnlineFilename(PanboxShare share, String path)
			throws ShareManagerException;

	/**
	 * @param share
	 * @param deviceName
	 * @param password
	 * @return
	 * @throws ShareDoesNotExistException
	 * @throws ShareManagerException
	 * @throws SequenceNumberException
	 * @throws ShareMetaDataException
	 * @throws UnrecoverableKeyException
	 */
	public PanboxShare removeDevicePermission(PanboxShare share,
			String deviceName, char[] password)
			throws ShareDoesNotExistException, ShareManagerException,
			UnrecoverableKeyException, ShareMetaDataException;

	public PanboxShare getShareForPath(String sharePath)
			throws UnrecoverableKeyException, ShareManagerException,
			ShareMetaDataException;

	public boolean sharePathAvailable(String path)
			throws UnrecoverableKeyException, ShareManagerException,
			ShareMetaDataException;

	void removeShareFromDB(String shareName) throws ShareManagerException;

	PanboxShare reloadShareMetadata(PanboxShare share)
			throws ShareManagerException;

	PanboxShare resetShareInvitation(PanboxShare share, String email,
			char[] password) throws UnrecoverableKeyException,
			ShareDoesNotExistException, ShareManagerException,
			ShareMetaDataException;

	Collection<PanboxContact> checkShareDeviceListIntegrity(PanboxShare share);
}
