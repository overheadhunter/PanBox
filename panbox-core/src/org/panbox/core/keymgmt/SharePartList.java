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
import java.util.Iterator;
import java.util.TreeMap;

import org.panbox.core.Utils;
import org.panbox.core.crypto.CryptCore;
import org.panbox.core.crypto.EncodingHelper;
import org.panbox.core.crypto.EncodingType;
import org.panbox.core.crypto.Signable;

public class SharePartList implements Signable {

	private byte[] signature;
	private TreeMap<String, PublicKey> participants;

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (obj.getClass() != getClass())
			return false;
		SharePartList o = (SharePartList) obj;
		return ((signature == o.signature) || (signature != null
				&& o.signature != null && Arrays.equals(signature, o.signature)))
				&& ((participants == o.participants) || (participants != null
						&& o.participants != null && Utils.valueEquals(
						participants, o.participants)));
	}

	@Override
	public int hashCode() {
		int hc = 19;
		int mul = 59;
		hc = hc * mul + (participants == null ? 0 : participants.hashCode());
		for (int i = 0; i < signature.length; i++) {
			hc += signature[i];
		}
		return hc;
	};

	SharePartList() {
		this.participants = new TreeMap<String, PublicKey>();
		this.signature = null;
	}

	int size() {
		return participants.size();
	}

	void sign(PrivateKey key) throws SignatureException {
		this.signature = CryptCore.sign(this, key);
	}

	byte[] getSignature() {
		return signature;
	}

	void add(PublicKey pubKey, String alias) {
		signature = null;
		this.participants.put(alias, pubKey);
	}

	Iterator<String> getAliases() {
		return this.participants.keySet().iterator();
	}

	PublicKey getPublicKey(String alias) {
		return this.participants.get(alias);
	}

	void delete(String alias) {
		signature = null;
		this.participants.remove(alias);
	}

	@Override
	public byte[] serialize() {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			for (String alias : participants.keySet()) {
				os.write(participants.get(alias).getEncoded());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return os.toByteArray();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("SharePartList:\n");
		for (PublicKey key : participants.values()) {
			sb.append("\t"
					+ EncodingHelper.encodeByte(key.getEncoded(),
							EncodingType.BASE64) + "\n");
		}
		return sb.toString();
	}

	void setSignature(byte[] signature) {
		this.signature = signature;
	}

}
