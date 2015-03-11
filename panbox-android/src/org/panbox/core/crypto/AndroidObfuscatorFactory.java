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

import java.util.HashMap;
import java.util.Map;

import org.panbox.core.exception.ObfuscationException;
import org.panbox.mobile.android.dropbox.csp.DropboxConnector;

import android.content.Context;

public class AndroidObfuscatorFactory extends AbstractObfuscatorFactory {

	HashMap<String, Obfuscator> cache = new HashMap<String, Obfuscator>();

	protected AndroidObfuscatorFactory() {
	}

	@Override
	public Obfuscator getInstance(String sharePath, String shareName) {

		throw new RuntimeException("Use other getInstance Method on android");
	}

	public synchronized Obfuscator getInstance(String sharePath, String shareName,
			DropboxConnector dbc, Context ctx) throws ObfuscationException {

		if (cache.containsKey(sharePath)) {
			return cache.get(sharePath);
		}

		Obfuscator ob = new Obfuscator(sharePath, new AndroidObfuscatorIVPool(
				dbc, ctx), shareName);
		cache.put(sharePath, ob);
		return ob;
	}
	
	@Override
	public boolean removeInstance(String sharePath) throws ObfuscationException {
		return (cache.remove(sharePath) != null);
	}
	
	@Override
	public boolean removeInstance(Obfuscator o) throws ObfuscationException {
		for (Map.Entry<String, Obfuscator> entry : cache.entrySet()) {
			if (entry.getValue().equals(o)) {
				cache.remove(entry.getKey());
				return true;
			}
		}
		return false;
	}


}
