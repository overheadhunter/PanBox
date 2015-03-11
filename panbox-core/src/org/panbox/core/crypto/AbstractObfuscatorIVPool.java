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
package org.panbox.core.crypto;

import java.util.Map;

import org.panbox.PanboxConstants;
import org.panbox.core.KeyValuePair;
import org.panbox.core.LimitedHashMap;
import org.panbox.core.Utils;

public abstract class AbstractObfuscatorIVPool {

	public abstract void fetchIVPool(String absolutePath, String shareName);

	// Cache: filenameHash -> IV
	protected LimitedHashMap<String, byte[]> ivPool = new LimitedHashMap<String, byte[]>(
			PanboxConstants.OBFUSCATOR_IV_POOL_SIZE);

	public byte[] getCachedIV(String lookupHash, String shareName) {
		byte[] iv = null;

		if (ivPool.containsKey(lookupHash)) {
			iv = ivPool.get(lookupHash);
		}
		return iv;
	}

	/**
	 * IV offset with current selection of hashes
	 */
	final int index = KeyConstants.IV_LOOKUP_HASH_SIZE / 8 * 2;
	final int IV_SIDECAR_FILE_LEN = index + KeyConstants.SYMMETRIC_BLOCK_SIZE
			* 2;

	protected boolean ivEntryLengthValid(String filename) {
		return (filename.length() == IV_SIDECAR_FILE_LEN);
	}

	protected synchronized Map.Entry<String, byte[]> splitFilename(String fileName) {
		// filename should be in format: concat(hash,iv) (both in hex encoded format)
		StringBuilder buf = new StringBuilder(fileName);

		byte[] iv = Utils.hexToBytes(buf.substring(index));
		Map.Entry<String, byte[]> e = new KeyValuePair<String, byte[]>(
				buf.substring(0, index), iv);

		return e;
	}
}
