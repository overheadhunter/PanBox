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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.panbox.core.Utils;
import org.panbox.core.crypto.CryptCore;
import org.panbox.core.crypto.EncodingHelper;
import org.panbox.core.crypto.EncodingType;
import org.panbox.core.crypto.Signable;
import org.panbox.core.crypto.SignatureHelper;
import org.panbox.core.exception.SerializationException;

public class DeviceList implements Signable {

	private final TreeMap<String, PublicKey> devices;
	private final PublicKey id;
	private byte[] signature;

	public DeviceList(PublicKey id, Map<String, PublicKey> dkList) {
		this.id = id;
		if (dkList == null) {
			this.devices = new TreeMap<String, PublicKey>();
		} else {
			this.devices = new TreeMap<String, PublicKey>(dkList);
		}
	}

	public int size() {
		return this.devices.size();
	}

	void sign(PrivateKey key) throws SignatureException {
		this.signature = CryptCore.sign(this, key);
	}

	void sign(PrivateKey key, ShareKeyDB shareKeys, ObfuscationKeyDB obKeys)
			throws SignatureException {
		try {
			Collection<PublicKey> keys = this.devices.values();
			this.signature = SignatureHelper.sign(key, this,
					shareKeys.get(keys), obKeys.get(keys));
		} catch (InvalidKeyException | NoSuchAlgorithmException
				| SerializationException e) {
			throw new SignatureException(e);
		}
	}

	public byte[] getSignature() {
		return this.signature;
	}

	public PublicKey getMasterSignatureKey() {
		return this.id;
	}

	public Collection<PublicKey> getPublicKeys() {
		return this.devices.values();
	}

	public Iterator<String> getAliasIterator() {
		return this.devices.keySet().iterator();
	}

	public PublicKey getPublicKey(String alias) {
		return this.devices.get(alias);
	}

	@Override
	public byte[] serialize() {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		for (String alias : devices.keySet()) {
			try {
				os.write(devices.get(alias).getEncoded());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return os.toByteArray();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		for (PublicKey key : devices.values()) {
			sb.append("\t\tDev-Key: "
					+ EncodingHelper.encodeByte(key.getEncoded(),
							EncodingType.BASE64) + "\n");
		}

		return sb.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (obj.getClass() != getClass())
			return false;
		DeviceList dl = (DeviceList) obj;
		return dl.id.equals(id) && Arrays.equals(dl.signature, signature)
				&& Utils.valueEquals(dl.devices, devices);
	}

	@Override
	public int hashCode() {
		int hc = 17;
		int hashMultiplier = 61;
		hc = hc * hashMultiplier + (id == null ? 0 : id.hashCode())
				+ (devices == null ? 0 : devices.hashCode());
		return hc;
	}

	public void addDevice(String alias, PublicKey dKey) {
		if (alias == null || dKey == null)
			throw new IllegalArgumentException(
					"Null Parameters are not allowed!");
		this.devices.put(alias, dKey);
	}

	public PublicKey removeDevice(String deviceAlias) {
		return this.devices.remove(deviceAlias);
	}

	void setSignature(byte[] sig) {
		this.signature = sig;
	}

}
