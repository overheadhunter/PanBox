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

import java.util.Arrays;

public class EncryptedShareKey {

	public final byte[] encryptedKey;
	public final int version;

	public EncryptedShareKey(byte[] encryptedKey, int version) {
		this.encryptedKey = encryptedKey;
		this.version = version;
	}

	@Override
	public boolean equals(Object o) {
		if (o != null && o instanceof EncryptedShareKey) {
			EncryptedShareKey esk = (EncryptedShareKey) o;
			return this.version == esk.version
					&& Arrays.equals(this.encryptedKey, esk.encryptedKey);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hc = 12;
		int hashMultiplier = 41;

		hc = hc * hashMultiplier + version;
		if (encryptedKey != null) {
			for (int i = 0; i < encryptedKey.length; i++) {
				hc += encryptedKey[i];
			}
		}
		return hc;
	}
}
