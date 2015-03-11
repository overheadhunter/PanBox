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
import java.security.PublicKey;
import java.util.Iterator;
import java.util.TreeMap;

import javax.crypto.SecretKey;

import org.apache.log4j.Logger;
import org.panbox.core.Utils;
import org.panbox.core.crypto.CryptCore;
import org.panbox.core.crypto.EncodingHelper;
import org.panbox.core.crypto.EncodingType;
import org.panbox.core.crypto.Signable;
import org.panbox.core.exception.SerializationException;
import org.panbox.core.exception.SymmetricKeyEncryptionException;

/**
 * @author Sinisa Dukanovic
 * 
 */
public class ShareKeyDBEntry implements Signable {

	private final static Logger logger = Logger
			.getLogger(ShareKeyDBEntry.class);

	private final String algorithm;
	private final ShareKeyEntryStorage storage;
	private final int version;

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (obj.getClass() != getClass())
			return false;
		ShareKeyDBEntry o = (ShareKeyDBEntry) obj;
		return ((algorithm == o.algorithm) || (algorithm != null && algorithm
				.equals(o.algorithm)))
				&& ((storage == o.storage) || (storage != null && storage
						.equals(o.storage))) && version == o.version;
	}

	@Override
	public int hashCode() {
		int hc = 11;
		int hashMultiplier = 71;

		hc = hc * hashMultiplier + version
				+ (algorithm == null ? 0 : algorithm.hashCode())
				+ (storage == null ? 0 : storage.hashCode());
		return hc;
	}

	public ShareKeyDBEntry(String algo, int version) {
		this.algorithm = algo;
		this.storage = new MapStorage();
		this.version = version;
	}

	public int getVersion() {
		return version;
	}

	public String getAlgorithm() {
		return algorithm;
	}

	public int size() {
		return this.storage.size();
	}

	public void addEncryptedKey(byte[] sk, PublicKey key) {
		this.storage.put(key, sk);
	}

	public byte[] getEncryptedKey(PublicKey pubKey) {
		return this.storage.get(pubKey);
	}

	public Iterator<PublicKey> getKeyIterator() {
		return this.storage.getIterator();
	}

	public void addEncryptedKey(SecretKey sk, PublicKey key)
			throws SymmetricKeyEncryptionException {
		this.storage.put(key,
				CryptCore.encryptSymmetricKey(sk.getEncoded(), key));
	}

	public void removeEncryptedKey(PublicKey deviceKey) {
		this.storage.remove(deviceKey);
	}

	@Override
	public byte[] serialize() throws SerializationException {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			os.write(algorithm.getBytes());
			os.write(storage.serialize());
			return os.toByteArray();
		} catch (IOException e) {
			throw new SerializationException(
					"Could not serialize ShareKeyDBEntry", e);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ALG: ");
		sb.append(algorithm);
		sb.append("\n");
		sb.append("\t Storage:\n ");
		sb.append(storage);

		return sb.toString();
	}

	private abstract class ShareKeyEntryStorage implements Signable {

		public abstract void put(PublicKey key, byte[] encKey);

		public abstract void remove(PublicKey deviceKey);

		public abstract Iterator<PublicKey> getIterator();

		public abstract byte[] get(PublicKey pubKey);

		public abstract int size();

	}

	private class MapStorage extends ShareKeyEntryStorage {
		private TreeMap<PublicKey, byte[]> map = new TreeMap<PublicKey, byte[]>(
				Utils.PK_COMPARATOR);

		@Override
		public boolean equals(Object obj) {
			if (obj == null)
				return false;
			if (obj == this)
				return true;
			if (obj.getClass() != getClass())
				return false;
			MapStorage o = (MapStorage) obj;
			return map == o.map || Utils.valueArrayEquals(map, o.map);
		}

		@Override
		public int hashCode() {
			return map.hashCode();
		}

		@Override
		public void put(PublicKey key, byte[] encKey) {
			this.map.put(key, encKey);
		}

		@Override
		public void remove(PublicKey deviceKey) {
			this.map.remove(deviceKey);
		}

		@Override
		public int size() {
			return map.values().size();
		}

		@Override
		public byte[] get(PublicKey pubKey) {
			return this.map.get(pubKey);
		}

		@Override
		public byte[] serialize() throws SerializationException {
			try {
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				for (PublicKey key : map.keySet()) {
					os.write(key.getEncoded());
					os.write(map.get(key));
				}
				return os.toByteArray();
			} catch (IOException e) {
				logger.error("Could not serialize ShareKeyDBEntry.", e);
				throw new SerializationException(
						"Could not serialize ShareKeyDBEntry", e);
			}
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("\t");
			for (PublicKey key : map.keySet()) {
				sb.append("\tPK: ");
				sb.append(EncodingHelper.encodeByte(key.getEncoded(),
						EncodingType.BASE64) + "\n");
				sb.append("\t\tCrypt-for-PK: ");
				sb.append(EncodingHelper.encodeByte(map.get(key),
						EncodingType.BASE64));
				sb.append("\n");
			}
			return sb.toString();
		}

		@Override
		public Iterator<PublicKey> getIterator() {
			return this.map.keySet().iterator();
		}
	}

}
