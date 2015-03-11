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
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.panbox.core.Utils;
import org.panbox.core.crypto.Signable;

public class ObfuscationKeyDB implements Signable {

	@SuppressWarnings("unused")
	private final static Logger logger = Logger
			.getLogger(ObfuscationKeyDB.class);

	private final TreeMap<PublicKey, byte[]> obfuscationKeys;

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (obj.getClass() != getClass())
			return false;
		ObfuscationKeyDB o = (ObfuscationKeyDB) obj;
		return (obfuscationKeys == null && o.obfuscationKeys == null)
				|| ((obfuscationKeys != null && o.obfuscationKeys != null) && Utils
						.valueArrayEquals(obfuscationKeys, o.obfuscationKeys));
	}

	@Override
	public int hashCode() {
		int hc = 19;
		int hashMultiplier = 67;

		hc = hc * hashMultiplier
				+ (obfuscationKeys == null ? 0 : obfuscationKeys.hashCode());
		return hc;
	}

	public ObfuscationKeyDB() {
		obfuscationKeys = new TreeMap<PublicKey, byte[]>(Utils.PK_COMPARATOR);
	}

	@Override
	public byte[] serialize() {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			for (PublicKey key : obfuscationKeys.keySet()) {
				os.write(key.getEncoded());
				os.write(obfuscationKeys.get(key));
			}
			return os.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	void add(PublicKey key, byte[] encKey) {
		this.obfuscationKeys.put(key, encKey);
	}

	byte[] remove(PublicKey key) {
		return this.obfuscationKeys.remove(key);
	}

	public Iterator<PublicKey> getKeys() {
		return this.obfuscationKeys.keySet().iterator();
	}

	public byte[] get(PublicKey devicePublicKey) {
		return this.obfuscationKeys.get(devicePublicKey);
	}

	public boolean isEmpty() {
		return this.obfuscationKeys.isEmpty();
	}

	ObfuscationKeyDB get(Collection<PublicKey> keys) {
		ObfuscationKeyDB result = new ObfuscationKeyDB();

		for (PublicKey publicKey : keys) {
			byte[] obKey = obfuscationKeys.get(publicKey);
			if (obKey != null) {
				result.add(publicKey, obKey);
			}
		}

		return result;
	}

}
