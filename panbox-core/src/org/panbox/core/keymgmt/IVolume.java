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
package org.panbox.core.keymgmt;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.UUID;

import org.panbox.core.exception.ShareMetaDataException;
import org.panbox.core.exception.SymmetricKeyNotFoundException;

/**
 * @author Sinisa Dukanovic
 * 
 */
public interface IVolume {

	static String MASTER_KEY = "master_encrypted_key";

	/**
	 * This method will initialize a previously persisted ShareMetadata bundle.
	 * In this process all signatures of the metadata will be verified.
	 * 
	 * @param ownerPubSigKey
	 *            Public signature key of the owner of the Share/Volume
	 * @return a fully initialized ShareMetadata object
	 * @throws SequenceNumberException
	 * @throws ShareMetaDataException
	 */
	public ShareMetaData loadShareMetaData(PublicKey ownerPubSigKey) throws ShareMetaDataException;

	/**
	 * Creates new ShareMetaData set for a new Volume/Share. PrivateKey
	 * ownerPrivEncKey,In this process ShareMetadata will be initilized with the
	 * owner data given by the method's paramters. An obfuscation and share key
	 * will be initialized and made available for the device specified in the
	 * paramters of this method. All data will be signed and persisted.
	 * 
	 * @param ownerAlias
	 *            Alias of the Volume/Share owner
	 * @param ownerPubSigKey
	 *            Public signature key of the owner
	 * @param ownerPrivSigKey
	 *            Private signature key of the owner
	 * @param ownerPubEncKey
	 *            Public encryption key of the owner
	 * @param ownerPrivEncKey
	 *            Private encryption key of the owner
	 * @param deviceAlias
	 *            Alias for the owners current device
	 * @param devicePubKey
	 *            Public encryption key for the owners current device
	 * @param seqStore
	 *            Local sequence number storage
	 * @return the initialized and persisted ShareMetaData object
	 * @throws SequenceNumberException
	 *             if a sequnce number for the volume/share already existed in
	 *             the local sequence number storage and is higher than the new
	 *             sequence number
	 * @throws IllegalArgumentException
	 *             if any of the paramters is null or if a metadata db file
	 *             already exists at the given location
	 * @throws ShareMetaDataException
	 *             if any cryptography related errors occur, such as invalid
	 *             signatures, keys, etc.
	 */
	public ShareMetaData createShareMetaData(String ownerAlias,
			PublicKey ownerPubSigKey, PrivateKey ownerPrivSigKey,
			PublicKey ownerPubEncKey, PrivateKey ownerPrivEncKey,
			String deviceAlias, PublicKey devicePubKey) throws IllegalArgumentException,
			ShareMetaDataException;

	public ShareMetaData createShareMetaData(VolumeParams p)
			throws IllegalArgumentException, ShareMetaDataException;

	/**
	 * @return a unique id for the volume/share
	 */
	public UUID getUUID();

	/**
	 * @param devicePubKey
	 * @return
	 * @throws SymmetricKeyNotFoundException
	 */
	public byte[] getEncryptedObfuscationKey(PublicKey devicePubKey)
			throws SymmetricKeyNotFoundException;

	/**
	 * @param version
	 * @param devicePubKey
	 * @return
	 */
	public EncryptedShareKey getEncryptedShareKey(int version,
			PublicKey devicePubKey) throws SymmetricKeyNotFoundException;

	/**
	 * @param devicePubKey
	 * @return
	 * @throws SymmetricKeyNotFoundException
	 */
	public EncryptedShareKey getLatestEncryptedShareKey(PublicKey devicePubKey)
			throws SymmetricKeyNotFoundException;

	/**
	 * This method adds a new Device to the Share/Volume.
	 * 
	 * @param alias
	 *            master alias of the user
	 * @param masterPubSigKey
	 *            the user's master public signature key
	 * @param masterPrivSigKey
	 *            the user's master private signature key
	 * @param deviceAlias
	 *            name of the new Device
	 * @param newDevicePubKey
	 *            public encryption key of the new device
	 * @param seqStore
	 *            the local sequencenumber store for the users current device
	 * @param masterPubEncKey
	 *            the user's master public encryption key
	 * @param masterPrivEncKey
	 *            the user's master private encryption key
	 * @throws ShareMetaDataException
	 *             if any cryptography related exception occurs
	 * @throws SequenceNumberException
	 *             if the possibility of a replay attack was detected
	 */
	public void addDevice(String alias, PublicKey masterPubSigKey,
			PrivateKey masterPrivSigKey, String deviceAlias,
			PublicKey newDevicePubKey, PublicKey masterPubEncKey,
			PrivateKey masterPrivEncKey)
			throws IllegalArgumentException, ShareMetaDataException;

	public void addDevice(VolumeParams p) throws IllegalArgumentException,
			ShareMetaDataException;

	/**
	 * This method removes a device from the user's device list.
	 * 
	 * @param alias
	 *            master alias of the user
	 * @param masterPubSigKey
	 *            the user's master public signature key
	 * @param masterPrivSigKey
	 *            the user's master private signature key
	 * @param deviceAlias
	 *            name of the new Device
	 * @param seqStore
	 *            the local sequencenumber store for the users current device
	 * @throws IllegalArgumentException
	 *             If a the given deviceAlias is a reserved name
	 * @throws ShareMetaDataException
	 *             if any cryptography related exception occurs
	 * @throws SequenceNumberException
	 *             if the possibility of a replay attack was detected
	 */
	public void removeDevice(String alias, PublicKey masterPubSigKey,
			PrivateKey masterPrivSigKey, String deviceAlias) throws IllegalArgumentException,
			ShareMetaDataException;

	public void removeDevice(VolumeParams p) throws IllegalArgumentException,
			ShareMetaDataException;

	/**
	 * This method prepares the Share Metadata for a new User. The method can
	 * only be called by the Share Owner.
	 * 
	 * @param ownerAlias
	 *            Alias of the Share Owner
	 * @param ownerPrivSigKey
	 *            Private Signature key of the share owner
	 * @param ownerPubEncKey
	 *            public encryption key of the share owner
	 * @param ownerPrivEncKey
	 *            private encryption key of the share owner
	 * @param alias
	 *            alias of the new share user
	 * @param userPubEncKey
	 *            the new share user's public encryption key, used to prepare
	 *            sharekeys and obfuscation key for the user
	 * @param userPubSigKey
	 *            the new share user's public signature key, which is added to
	 *            the shareparticipants list
	 * @param seqStore
	 *            the local sequencenumber store for the users current device
	 * @throws ShareMetaDataException
	 *             if any cryptography or metadata related exception occurs
	 * @throws SequenceNumberException
	 *             if the possibility of a replay attack was detected
	 */
	public void inviteUser(String ownerAlias, PrivateKey ownerPrivSigKey,
			PublicKey ownerPubEncKey, PrivateKey ownerPrivEncKey, String alias,
			PublicKey userPubEncKey, PublicKey userPubSigKey) throws ShareMetaDataException;

	public void inviteUser(VolumeParams p) throws ShareMetaDataException;

	/**
	 * This method load and prepares a share for a newly invited user. It is
	 * called by the newly invited user AFTER the share owner has prepared the
	 * share for the new user by calling the method inviteUser on the Volume.
	 * 
	 * @param ownerPubSigKey
	 *            Public signature key of the share owner
	 * @param alias
	 *            the user's master alias
	 * @param masterPubSigKey
	 *            the user's master public signature key
	 * @param masterPrivSigKey
	 *            the user's master private signature key
	 * @param deviceAlias
	 *            the alias of the device the user's wants to add to the share
	 * @param newDevicePubKey
	 *            the user's device's public encryption key
	 * @param masterPubEncKey
	 *            the user's master public encryption key
	 * @param masterPrivEncKey
	 *            the user's master private encryption key
	 * @param seqStore
	 *            the local sequencenumber store for the users current device
	 * @return the loaded and initialized sharemetadata
	 * @throws SequenceNumberException
	 *             if any cryptography or metadata related exception occurs
	 * @throws ShareMetaDataException
	 *             if the possibility of a replay attack was detected
	 */
	public ShareMetaData acceptInvitation(PublicKey ownerPubSigKey,
			String alias, PublicKey masterPubSigKey,
			PrivateKey masterPrivSigKey, String deviceAlias,
			PublicKey newDevicePubKey, PublicKey masterPubEncKey,
			PrivateKey masterPrivEncKey)
			throws ShareMetaDataException;

	public ShareMetaData acceptInvitation(VolumeParams p)
			throws ShareMetaDataException;

	public void removeUser(String ownerAlias, PublicKey ownerPubSigKey,
			PrivateKey ownerPrivSigKey, String userAlias) throws ShareMetaDataException;

	public HashMap<PublicKey, String> getShareParticipants();

	public HashMap<PublicKey, String> getDeviceMap(PublicKey pubSigKey);

	public PublicKey getOwnerKey();

	public void reload() throws ShareMetaDataException;
}
