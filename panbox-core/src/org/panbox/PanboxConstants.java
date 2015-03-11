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
package org.panbox;

/**
 * @author palige
 * 
 *         interface stores platform independent application constants
 */
public interface PanboxConstants {

	/**
	 * magic number, 6 bytes
	 */
	public static final byte[] PANBOX_FILE_MAGIC = new byte[] { 'P', 'A', 'N',
			'B', 'O', 'X' };

	/**
	 * version field for Panbox file format, 4 bytes
	 */
	public static final byte[] PANBOX_VERSION = new byte[] { '1', '.', '0', '0' };

	/**
	 * panbox metadata subdirectory identifier within share directories
	 */
	public static final String PANBOX_SHARE_METADATA_DIRECTORY = ".panbox";

	/**
	 * subfolder name of the folder containing invitation fingerprints inside
	 * the share_meta_data_directory
	 */
	public static final String PANBOX_SHARE_INVITATION_DIRECTORY = "invitation";

	/**
	 * filename for storage of a share owner's public key fingerprint
	 */
	public static final String PANBOX_SHARE_OWNER_FILE = "owner.pbox";

	public static final String PANBOX_SHARE_UUID_PREFIX = "uuid-";

	public static final int OBFUSCATOR_IV_POOL_SIZE = 1024 * 200;

	public static final String STANDARD_CHARSET = "UTF-8";

	public static final int DEFAULT_EXPORT_PIN_LENGHT = 8;
	public static final char[] DEFAULT_EXPORT_PIN_CHARSET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
			.toCharArray();

	public static final int CERTIFICATE_LIFETIME_YEARS = 10;

	public static final int PANBOX_DEFAULT_PORT = 54445;

	public static final String PANBOX_URL_PREFIX = "http://127.0.0.1:"
			+ PANBOX_DEFAULT_PORT + "/";

}
