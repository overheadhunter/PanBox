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
package org.panbox.core;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.panbox.PanboxConstants;
import org.panbox.core.crypto.CryptCore;
import org.panbox.core.crypto.KeyConstants;
import org.panbox.core.crypto.randomness.SecureRandomWrapper;

public class Utils {

	private static final Logger logger = Logger.getLogger(Utils.class);

	public final static Comparator<PublicKey> PK_COMPARATOR = new Comparator<PublicKey>() {
		@Override
		public int compare(PublicKey o1, PublicKey o2) {
			return ((RSAKey) o1).getModulus().compareTo(
					((RSAKey) o2).getModulus());
		}
	};

	public static boolean keysMatch(PublicKey pub, PrivateKey priv) {
		boolean matched = false;

		try {
			SecureRandomWrapper srWrapper = SecureRandomWrapper.getInstance();
			byte[] random = new byte[KeyConstants.SYMMETRIC_BLOCK_SIZE];
			srWrapper.nextBytes(random);

			matched = Arrays.equals(
					random,
					CryptCore._asymmetricDecrypt(
							CryptCore.encryptSymmetricKey(random, pub), priv));
		} catch (Exception e) {
			logger.error("Could not en/decrypt", e);
		}

		return matched;
	}

	/**
	 * Secure key comparison. See <a href=
	 * "https://www.securecoding.cert.org/confluence/display/jg/11.+Do+not+use+Object.equals%28%29+to+compare+cryptographic+keys"
	 * >https://www.securecoding.cert.org/confluence/display/jg/11.+Do+not+use+
	 * Object.equals%28%29+to+compare+cryptographic+keys</a>
	 * 
	 * @param key1
	 * @param key2
	 * @return
	 */
	public static boolean keysEqual(Key key1, Key key2) {
		// Same key or both null?
		if (key1 == key2) {
			return true;
		}
		//
		if ((key1 == null && key2 != null) || (key1 != null && key2 == null)) {
			return false;
		}
		if (key1.equals(key2)) {
			return true;
		}

		if (Arrays.equals(key1.getEncoded(), key2.getEncoded())) {
			return true;
		}

		// More code for different types of keys here
		// For example, the following code can check whether
		// an RSAPrivateKey and an RSAPrivateCrtKey are equal
		if ((key1 instanceof RSAPrivateKey) && (key2 instanceof RSAPrivateKey)) {

			if ((((RSAKey) key1).getModulus().equals(((RSAKey) key2)
					.getModulus()))
					&& (((RSAPrivateKey) key1).getPrivateExponent()
							.equals(((RSAPrivateKey) key2).getPrivateExponent()))) {
				return true;
			}
		}
		return false;
	}

	public static boolean valueEquals(HashMap<?, ?> map, HashMap<?, ?> map2) {
		Collection<?> v1 = map.values();
		Collection<?> v2 = map2.values();
		return map.keySet().equals(map2.keySet()) && v1.size() == v2.size()
				&& v1.containsAll(v2);
	}

	public static boolean valueEquals(TreeMap<?, ?> map, TreeMap<?, ?> map2) {
		Collection<?> v1 = map.values();
		Collection<?> v2 = map2.values();
		return map.keySet().equals(map2.keySet()) && v1.size() == v2.size()
				&& v1.containsAll(v2);
	}

	public static boolean valueArrayEquals(HashMap<?, byte[]> obfuscationKeys,
			HashMap<?, byte[]> obfuscationKeys2) {
		Collection<byte[]> v1 = obfuscationKeys.values();
		Collection<byte[]> v2 = obfuscationKeys2.values();
		boolean result = obfuscationKeys.keySet().equals(
				obfuscationKeys2.keySet())
				&& v1.size() == v2.size();
		if (result) {
			for (byte[] value : v1) {
				boolean found = false;
				for (byte[] value2 : v2) {
					if (Arrays.equals(value, value2)) {
						found = true;
						break;
					}
				}
				if (!found) {
					return false;
				}
			}
		} else {
			return false;
		}
		return true;
	}

	public static boolean valueArrayEquals(TreeMap<?, byte[]> obfuscationKeys,
			TreeMap<?, byte[]> obfuscationKeys2) {
		Collection<byte[]> v1 = obfuscationKeys.values();
		Collection<byte[]> v2 = obfuscationKeys2.values();
		boolean result = obfuscationKeys.keySet().equals(
				obfuscationKeys2.keySet())
				&& v1.size() == v2.size();
		if (result) {
			for (byte[] value : v1) {
				boolean found = false;
				for (byte[] value2 : v2) {
					if (Arrays.equals(value, value2)) {
						found = true;
						break;
					}
				}
				if (!found) {
					return false;
				}
			}
		} else {
			return false;
		}
		return true;
	}

	/**
	 * converts char array to UTF-8 byte array see
	 * http://stackoverflow.com/questions/5513144/converting-char-to-byte
	 * 
	 * @param chars
	 * @return
	 */
	public static byte[] toBytes(char[] chars) {
		CharBuffer charBuffer = CharBuffer.wrap(chars);
		ByteBuffer byteBuffer = Charset.forName(
				PanboxConstants.STANDARD_CHARSET).encode(charBuffer);
		byte[] bytes = Arrays.copyOfRange(byteBuffer.array(),
				byteBuffer.position(), byteBuffer.limit());
		eraseChars(charBuffer.array());
		Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
		return bytes;
	}

	/**
	 * String conversion into standard charset
	 * 
	 * @param s
	 * @return
	 */
	public static String toUTF8String(String s) {
		return new String(s.getBytes(),
				Charset.forName(PanboxConstants.STANDARD_CHARSET));
	}

	/**
	 * {@link #toBytes(char[])}, vice-versa
	 * 
	 * @param chars
	 * @return
	 */
	public static char[] toChars(byte[] bytes) {
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		CharBuffer charBuffer = Charset.forName(
				PanboxConstants.STANDARD_CHARSET).decode(byteBuffer);
		char[] chars = Arrays.copyOfRange(charBuffer.array(),
				charBuffer.position(), charBuffer.limit());
		Arrays.fill(byteBuffer.array(), (byte) 0x00); // clear sensitive data
		eraseChars(charBuffer.array());
		return chars;
	}

	public static void eraseChars(char[] p) {
		Arrays.fill(p, '\u0000');
	}

	final public static char[] hexArray = "0123456789ABCDEF".toCharArray();

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	public static byte[] hexToBytes(String s) {
		// return DatatypeConverter.parseHexBinary(s);
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character
					.digit(s.charAt(i + 1), 16));
		}
		return data;
	}

	public static String getCertFingerprint(X509Certificate cert) {
		return getPubKeyFingerprint(cert.getPublicKey());
	}

	public static String getPubKeyFingerprint(PublicKey k) {
		byte[] res = CryptCore.getPublicKeyfingerprint(k);
		return bytesToHex(res);
	}

}
