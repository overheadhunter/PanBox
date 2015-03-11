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
package org.panbox.desktop.common.urihandler;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import ezvcard.util.org.apache.commons.codec.binary.Base64;

/**
 * @author palige
 * 
 */
public abstract class PanboxURICmd {

	private static final Logger logger = Logger.getLogger(PanboxURICmd.class);

	public PanboxURICmd(byte[]... params) throws Exception {
		// tbi in subclasses
	}

	public abstract String getName();

	public abstract void execute() throws Exception;

	public static synchronized PanboxURICmd getCommandHander(String cmdstring)
			throws InstantiationException, IllegalAccessException,
			ClassNotFoundException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {

		String[] parts = cmdstring.split("\\?");

		if ((parts != null) && (parts.length == 2)) {
			String[] params = parts[1].split(":");
			ArrayList<byte[]> bparams = new ArrayList<byte[]>();
			for (int i = 0; i < params.length; i++) {
				bparams.add(Base64.decodeBase64(params[i]));
			}

			if ((parts[0] != null) && !parts[0].isEmpty()) {
				return resolveInstance(parts[0],
						bparams.toArray(new byte[bparams.size()][]));
			} else {
				logger.error("Could not determine operation to execute "
						+ cmdstring);
			}
		} else {
			logger.error("Could not parse command string " + cmdstring);
		}
		return null;
	}

	private static synchronized PanboxURICmd resolveInstance(
			String cmdIdentifier, byte[]... params)
			throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		Package p = PanboxURICmd.class.getPackage();
		String lookup = p.getName() + "." + PanboxURICmd.class.getSimpleName()
				+ cmdIdentifier;
		Class<?> tmp = Class.forName(lookup);
		return (PanboxURICmd) tmp.getConstructor(byte[][].class).newInstance(
				new Object[] { params });
	}
}
