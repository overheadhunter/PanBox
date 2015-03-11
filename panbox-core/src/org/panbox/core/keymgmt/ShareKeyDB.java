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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;

import javax.crypto.SecretKey;

import org.panbox.core.Utils;
import org.panbox.core.crypto.CryptCore;
import org.panbox.core.crypto.Signable;
import org.panbox.core.crypto.SignatureHelper;
import org.panbox.core.exception.SerializationException;
import org.panbox.core.exception.SymmetricKeyDecryptionException;
import org.panbox.core.exception.SymmetricKeyEncryptionException;

/**
 * @author dukan
 * 
 */
public class ShareKeyDB implements Signable {

	// Mapping of Version to Map of DevicePublicKey-Fingerprint to encrypted
	// symmetric Sharekey
	private TreeMap<Integer, ShareKeyDBEntry> shareKeys;
	// private int latest = -1;
	private String signer;
	private byte[] signature;

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (obj.getClass() != getClass())
			return false;

		ShareKeyDB o = (ShareKeyDB) obj;
		if (o.signer == null && signer == null) {
			return Utils.valueEquals(shareKeys, o.shareKeys);
		} else {
			return o.signer != null && o.signer.equals(signer)
					&& Arrays.equals(signature, o.signature)
					&& Utils.valueEquals(shareKeys, o.shareKeys);
		}
	}

	@Override
	public int hashCode() {
		int hc = 15;
		int hashMultiplier = 59;

		hc = hc * hashMultiplier
				+ (shareKeys == null ? 0 : shareKeys.hashCode())
				+ (signer == null ? 0 : signer.hashCode());
		if (signature != null) {
			for (int i = 0; i < signature.length; i++) {
				hc += signature[i];
			}
		}
		return hc;
	}

	public ShareKeyDB() {
		this.shareKeys = new TreeMap<Integer, ShareKeyDBEntry>();
	}

	ShareKeyDBEntry add(SecretKey sk, Collection<PublicKey> pubKeys)
			throws SymmetricKeyEncryptionException {
		int version = getLatestVersion() + 1;
		String algo = sk.getAlgorithm();
		ShareKeyDBEntry result = new ShareKeyDBEntry(algo, version);
		byte[] shareKey = sk.getEncoded();

		for (PublicKey pKey : pubKeys) {
			byte[] byteArray = CryptCore.encryptSymmetricKey(shareKey, pKey);
			result.addEncryptedKey(byteArray, pKey);
		}
		add(version, result);
		return result;
	}

	ShareKeyDBEntry add(SecretKey sk) throws SymmetricKeyEncryptionException {
		int version = getLatestVersion() + 1;
		String algo = sk.getAlgorithm();
		ShareKeyDBEntry result = new ShareKeyDBEntry(algo, version);
		byte[] shareKey = sk.getEncoded();

		ShareKeyDBEntry shareKeyDBEntry = this.shareKeys.get((version - 1));
		if (shareKeyDBEntry == null) {
			throw new RuntimeException(
					"Trying to add new version of ShareKey when there was no previous version!");
		}

		Iterator<PublicKey> pubKeys = shareKeyDBEntry.getKeyIterator();
		while (pubKeys.hasNext()) {
			PublicKey pKey = (PublicKey) pubKeys.next();
			byte[] byteArray = CryptCore.encryptSymmetricKey(shareKey, pKey);
			result.addEncryptedKey(byteArray, pKey);
			add(version, result);
		}
		return result;
	}

	void addDevice(PublicKey oldDevicePubKey, PrivateKey oldDevicePrivKey,
			PublicKey newDevicePubKey) throws SymmetricKeyDecryptionException,
			SymmetricKeyEncryptionException {
		for (ShareKeyDBEntry entry : shareKeys.values()) {
			byte[] encSK = entry.getEncryptedKey(oldDevicePubKey);
			if (encSK != null) {
				SecretKey sk = CryptCore.decryptSymmertricKey(encSK,
						oldDevicePrivKey);
				entry.addEncryptedKey(sk, newDevicePubKey);
			}

		}
	}

	public void removeDevice(PublicKey deviceKey) {
		for (ShareKeyDBEntry entry : shareKeys.values()) {
			entry.removeEncryptedKey(deviceKey);
		}
	}

	void add(int version, ShareKeyDBEntry entry) {
		shareKeys.put(version, entry);
	}

	public int size() {
		return this.shareKeys.size();
	}

	public ShareKeyDBEntry getEntry(int version) {
		return this.shareKeys.get(version);
	}

	public Iterator<Integer> getKeyIterator() {
		return this.shareKeys.keySet().iterator();
	}

	public byte[] getEntry(int version, PublicKey key) {
		return shareKeys.get(version).getEncryptedKey(key);
	}

	/**
	 * @return either the ShareKeyDBEntryObject containing the latest version
	 *         ShareKeys or <code>null</code> if no ShareKeys have been
	 *         generated so far
	 */
	public ShareKeyDBEntry getLastEntry() {
		if (this.size() > 0) {
			return shareKeys.get(getLatestVersion());
		} else {
			return null;
		}
	}

	private int getLatestVersion() {
		int latest = -1;
		if (!shareKeys.isEmpty()) {
			latest = shareKeys.lastKey();
		}
		return latest;
	}

	@Override
	public byte[] serialize() throws SerializationException {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			for (Integer i : shareKeys.keySet()) {
				os.write(i.intValue());
				os.write(shareKeys.get(i).serialize());
			}
			return os.toByteArray();
		} catch (IOException e) {
			throw new SerializationException("Could not serialize ShareKeyDB",
					e);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ShareKeyDB:\n");
		for (Integer i : shareKeys.keySet()) {
			sb.append("\t" + i.intValue() + "\n");
			sb.append("\t" + shareKeys.get(i) + "\n");
		}

		return sb.toString();
	}

	String getSigner() {
		return this.signer;
	}

	void setSigner(String signer) {
		this.signer = signer;
	}

	byte[] getSignature() {
		return this.signature;
	}

	void sign(PrivateKey privateKey, String signer) throws SignatureException {
		setSigner(signer);
		try {
			this.signature = SignatureHelper.sign(this, privateKey);
		} catch (Exception e) {
			throw new SignatureException("Could not sign ShareKeyDB", e);
		}
	}

	public void setSignature(byte[] signature) {
		this.signature = signature;
	}

	ShareKeyDB get(Collection<PublicKey> keys) {
		ShareKeyDB result = new ShareKeyDB();
		Collection<ShareKeyDBEntry> entries = shareKeys.values();
		for (ShareKeyDBEntry entry : entries) {
			ShareKeyDBEntry e = new ShareKeyDBEntry(entry.getAlgorithm(),
					entry.getVersion());
			for (PublicKey publicKey : keys) {
				byte[] encKey = entry.getEncryptedKey(publicKey);
				if (encKey != null) {
					e.addEncryptedKey(encKey, publicKey);
					;
				}
			}
			result.add(e.getVersion(), e);
		}
		return result;
	}

}
