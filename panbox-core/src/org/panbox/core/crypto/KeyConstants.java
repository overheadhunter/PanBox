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

import ezvcard.parameter.KeyType;

public interface KeyConstants {

	// password for *unprotected* keystores
	public final static char[] OPEN_KEYSTORE_PASSWORD = "PanBox".toCharArray();

	public static final int SYMMETRIC_KEY_SIZE = 256;
	public static final int SYMMETRIC_KEY_SIZE_BYTES = SYMMETRIC_KEY_SIZE
			/ Byte.SIZE;

	public static final int SYMMETRIC_FILE_KEY_SIZE = 256; // 256;
	public static final int SYMMETRIC_FILE_KEY_SIZE_BYTES = SYMMETRIC_FILE_KEY_SIZE
			/ Byte.SIZE;

	public final static int ASYMMETRIC_KEYSIZE = 2048;

	public static final int SYMMETRIC_BLOCK_SIZE = 16;

	public final static String SYMMETRIC_ALGORITHM = "AES";
	public final static String ASYMMETRIC_ALGORITHM = "RSA/NONE/OAEPWithSHA256AndMGF1Padding";
	public final static String ASYMMETRIC_ALGORITHM_ALGO_ONLY = "RSA";

	public final static String CERTIFICATE_ENCODING = "X.509";

	// public final static String KEYSTORE_TYPE = "UBER";
	// public final static String KEYSTORE_TYPE = KeyStore.getDefaultType();
	public final static String KEYSTORE_TYPE = "BKS";

	public static final KeyType AB_CONTACT_PK_ENC = KeyType.get("pbEnc",
			"application/pbEnc", "pbEnc");
	public static final KeyType AB_CONTACT_PK_SIG = KeyType.get("pbSig",
			"application/pbSig", "pbSig");

	public static final String AB_CONTACT_TRUSTLEVEL = "X-PANBOX-TRUSTLEVEL";

	public static final String PROV_BC = org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;
	public static final String PROV_SunJCE = "SunJCE";

	public final static String DEFAULT_HASH = "SHA-256";
	public static final String PUBKEY_FINGERPRINT_DIGEST = DEFAULT_HASH;

	public static final String IV_LOOKUP_HASH_ALG = "SHA-1";
	public static final int IV_LOOKUP_HASH_SIZE = 160;

	public static final String OBFUSCATOR_ALG = "AES/CFB/NoPadding";

	public static final String KEY_FACTORY = "RSA";

	public static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

	public static final String VCARD_HMAC = "HmacSHA256";

	public static final String CSPRNG_ALGO = "SHA1PRNG";

}
