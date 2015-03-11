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
package org.panbox.desktop.common.vfs.backend;

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

import javax.crypto.SecretKey;

import org.apache.log4j.Logger;
import org.panbox.core.crypto.CryptCore;
import org.panbox.core.crypto.FileObfuscatorFactory;
import org.panbox.core.crypto.Obfuscator;
import org.panbox.core.exception.ObfuscationException;
import org.panbox.core.exception.SymmetricKeyDecryptionException;
import org.panbox.core.exception.SymmetricKeyNotFoundException;
import org.panbox.core.keymgmt.EncryptedShareKey;
import org.panbox.core.keymgmt.IVolume;
import org.panbox.core.keymgmt.ShareKey;
import org.panbox.core.vfs.backend.VirtualFile;
import org.panbox.core.vfs.backend.VirtualVolume;
import org.panbox.desktop.common.ex.DeviceKeyException;

/**
 * VFSShare represents a share that can be mounted in the Panbox virtual
 * filesystem component, containing a share name, the share key used for
 * encryption and obfuscation and the VirtualFile backend storage, which is used
 * to access the concrete data files.
 * 
 * @author Clemens A. Schulz <c.schulz@sirrix.com>
 */
public class VFSShare {
	private static final Logger log = Logger.getLogger(VFSShare.class);

	private final SecretKey obfuscationKey;
	private final String shareName;
	private final VirtualVolume backend;

	private final IVolume volume;
	private final PublicKey publicDeviceKey;
	private final PrivateKey privateDeviceKey;
	private ShareKey[] shareKeyCache = new ShareKey[20];

	private final Obfuscator obfuscator;

	/**
	 * Returns the share key of this share.
	 * 
	 * @return Share key as SecretKey instance.
	 */

	public VFSShare(String shareName, String path, VirtualVolume backend,
			IVolume volume, KeyPair deviceKeys) throws DeviceKeyException {

		this.backend = backend;
		this.shareName = shareName;
		this.volume = volume;
		this.publicDeviceKey = deviceKeys.getPublic();
		this.privateDeviceKey = deviceKeys.getPrivate();
		try {
			this.obfuscationKey = CryptCore.decryptSymmertricKey(
					volume.getEncryptedObfuscationKey(publicDeviceKey),
					privateDeviceKey);
		} catch (SymmetricKeyDecryptionException e) {
			// TODO: Handle this case
			throw new RuntimeException("Could not decrypt obfuscation key.");
		} catch (SymmetricKeyNotFoundException e) {
			throw new DeviceKeyException(e);
		}
		try {
			this.obfuscator = org.panbox.core.crypto.AbstractObfuscatorFactory
					.getFactory(FileObfuscatorFactory.class).getInstance(path,
							shareName);
		} catch (ClassNotFoundException | InstantiationException
				| IllegalAccessException | ObfuscationException e) {
			log.fatal("obfuscator intialization failed, quitting!", e);
			//TODO handle this case with custom exception
			throw new RuntimeException(e);
		}
	}

	public Obfuscator getObfuscator() {
		return obfuscator;
	}

	/**
	 * To decrypt a file, this method can be used to retrieve the sharekey
	 * specified in the header of the corresponding file.
	 * 
	 * @param version
	 *            the version of the sharekey as extracted from the file's
	 *            metadata
	 * @return the SecretKey that can be used to decrypt the file's FEK
	 */
	public SecretKey getShareKey(int version) {
		if (version >= this.shareKeyCache.length) {
			growCache();
		}
		ShareKey shareKey = this.shareKeyCache[version];
		if (shareKey == null) {
			// Get key from backend
			try {
				log.debug("Retrieving ShareKey version " + version
						+ "from Backend...");
				EncryptedShareKey key = volume.getEncryptedShareKey(version,
						publicDeviceKey);
				SecretKey secretKey = CryptCore.decryptSymmertricKey(
						key.encryptedKey, privateDeviceKey);
				shareKey = new ShareKey(secretKey, key.version);
			} catch (SymmetricKeyNotFoundException
					| SymmetricKeyDecryptionException e) {
				throw new RuntimeException(e);
			}
			// Store in cache
			log.debug("Using ShareKey version " + version);
			this.shareKeyCache[version] = shareKey;
		}
		return shareKey.key;
	}

	/**
	 * To decrypt a file, this method can be used to retrieve the sharekey
	 * specified in the header of the corresponding file.
	 * 
	 * @return the SecretKey that can be used to decrypt the file's FEK
	 */
	public ShareKey getLatestShareKey() {
		try {
			EncryptedShareKey key = volume
					.getLatestEncryptedShareKey(publicDeviceKey);
			if (key.version >= this.shareKeyCache.length) {
				growCache();
			}
			ShareKey shareKey = this.shareKeyCache[key.version];
			if (shareKey == null) {
				log.debug("Retrieving ShareKey from Backend...");
				SecretKey secKey = CryptCore.decryptSymmertricKey(
						key.encryptedKey, privateDeviceKey);
				shareKey = new ShareKey(secKey, key.version);
				this.shareKeyCache[key.version] = shareKey;
			}
			log.debug("Using latest sharekey: Version " + shareKey.version);
			return shareKey;
		} catch (SymmetricKeyNotFoundException
				| SymmetricKeyDecryptionException e) {
			log.fatal("Decryption of ShareKey failed", e);
			// TODO This should be handled with a custom exception, instead of
			// quitting the application
			throw new RuntimeException(e);
		}
	}

	private void growCache() {
		this.shareKeyCache = Arrays.copyOf(this.shareKeyCache,
				this.shareKeyCache.length + 10);
	}

	public SecretKey getObfuscationKey() {
		return this.obfuscationKey;
	}

	/**
	 * Returns the backend storage of this share.
	 * 
	 * @return VirtualVolume instance of the backend.
	 */
	public VirtualVolume getBackend() {
		return backend;
	}

	/**
	 * Returns the name of this share.
	 * 
	 * @return Share name string.
	 */
	public String getShareName() {
		return shareName;
	}

	/**
	 * This method decides whether the specified fileName is a file in this
	 * share or not.
	 * 
	 * @param fileName
	 *            Relative path to a file.
	 * @return true if the file is part of this share, else false.
	 */
	public boolean contains(String fileName) {
		// NOTE: share names may not be prefix free
		if (fileName.startsWith(shareName + File.separator)
				|| fileName.substring(1).startsWith(shareName + File.separator)
				|| fileName.equals(shareName)
				|| fileName.substring(1).equals(shareName)) {
			return true; // file is part of this share!
		}
		return false; // file is not part of this share!
	}

	/**
	 * This method decides whether the specified VirtualFile is a file in this
	 * share or not.
	 * 
	 * @param file
	 *            VirtualFile instance of a file.
	 * @return true if the file is part of this share, else false.
	 */
	public boolean contains(VirtualFile file) {

		if (file.getFileName().equals(shareName)
				|| file.getFileName().equals(File.separator + shareName)) {
			return true; // share-file of the share is always included!
		}
		if (file.getFile() == null) {
			return false; // root-file is never included!
		}

		// NOTE: filenames are not necessarily prefix-free
		String vfilename = file.getFile().getAbsolutePath();
		String vrootname = backend.getRoot().getFile().getAbsolutePath();

		if (vfilename.startsWith(vrootname + File.separator)
				|| vfilename.equals(vrootname)) {
			return true; // file is part of this share!
		}

		return false;
	}

	/**
	 * Returns the relative Path in this share to the specified file.
	 * 
	 * @param file
	 *            VirtualFile instance of a file.
	 * @return Relative path of the file as String.
	 */
	public String getRelativePath(VirtualFile file) {
		if (file.getRelativePath().length() > 0
				&& file.getRelativePath().startsWith("/"))
			return File.separator + shareName + file.getRelativePath();
		else
			return File.separator + shareName + File.separator
					+ file.getRelativePath();
	}

	/**
	 * Returns a VirtualFile instance for the containing file with the specified
	 * file path.
	 * 
	 * @param fileName
	 *            Relative path to a file.
	 * @return VirtualFile instance of the specified file.
	 * @throws IOException
	 */
	public VirtualFile getFile(String fileName) throws IOException {
		return backend.getFile(getRelativePathInVolume(fileName));
	}

	// returns the relative path in volume for the specified filename. This
	// means especially a path WITHOUT the sharename!
	private String getRelativePathInVolume(String fileName) {
		if (fileName.startsWith(File.separator)) {
			if (fileName.substring(1).equals(shareName)) {
				return File.separator;
			} else {
				fileName = fileName.substring(1);
				return fileName.substring(fileName.indexOf(File.separator));
			}
		}
		if (fileName.equals(shareName)) {
			return File.separator;
		} else {
			return fileName.substring(fileName.indexOf(File.separator));
		}
	}

	@Override
	public String toString() {
		return getShareName();
	}
}
