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

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

import org.apache.log4j.Logger;
import org.panbox.core.exception.SerializationException;
import org.panbox.core.keymgmt.ObfuscationKeyDB;
import org.panbox.core.keymgmt.ShareKeyDB;

public class SignatureHelper {

	private final static Logger logger = Logger
			.getLogger(SignatureHelper.class);

	public static byte[] sign(PrivateKey key, Signable... s)
			throws NoSuchAlgorithmException, SignatureException,
			InvalidKeyException, SerializationException {
		Signature signature = Signature
				.getInstance(KeyConstants.SIGNATURE_ALGORITHM);
		signature.initSign(key);

		for (Signable signable : s) {
			signature.update(signable.serialize());
		}
		return signature.sign();
	}

	public static boolean verify(byte[] sig, PublicKey key, Signable... s)
			throws NoSuchAlgorithmException, InvalidKeyException,
			SignatureException, SerializationException {
		if (s == null || sig == null || key == null) {
			logger.warn("verify called with null argument: returning false.");
			return false;
		}
		Signature signature = Signature
				.getInstance(KeyConstants.SIGNATURE_ALGORITHM);
		signature.initVerify(key);
		for (Signable signable : s) {
			signature.update(signable.serialize());
		}
		return signature.verify(sig);
	}

	public static byte[] sign(Signable s, PrivateKey key)
			throws NoSuchAlgorithmException, SignatureException,
			InvalidKeyException, SerializationException {
		return sign(key, s);
	}

	public static byte[] sign(PrivateKey key, ObfuscationKeyDB ok, ShareKeyDB sk)
			throws NoSuchAlgorithmException, SignatureException,
			InvalidKeyException, SerializationException {
		return sign(key, (Signable) sk, (Signable) ok);
	}

	public static byte[] sign(PrivateKey key, ShareKeyDB sk, ObfuscationKeyDB ok)
			throws NoSuchAlgorithmException, SignatureException,
			InvalidKeyException, SerializationException {
		return sign(key, (Signable) sk, (Signable) ok);
	}

	public static boolean verify(Signable s, byte[] sig, PublicKey key)
			throws NoSuchAlgorithmException, InvalidKeyException,
			SignatureException, SerializationException {
		return verify(sig, key, s);
	}

	public static boolean verify(byte[] sig, PublicKey key, ShareKeyDB sk,
			ObfuscationKeyDB ok) throws NoSuchAlgorithmException,
			InvalidKeyException, SignatureException, SerializationException {
		return verify(sig, key, (Signable) sk, (Signable) ok);
	}

	public static boolean verify(byte[] sig, PublicKey key,
			ObfuscationKeyDB ok, ShareKeyDB sk)
			throws NoSuchAlgorithmException, InvalidKeyException,
			SignatureException, SerializationException {
		return verify(sig, key, (Signable) sk, (Signable) ok);
	}

}
