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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;
import org.bouncycastle.util.Arrays;
import org.panbox.OS;
import org.panbox.OS.OperatingSystem;
import org.panbox.PanboxConstants;
import org.panbox.core.LimitedHashMap;
import org.panbox.core.Utils;
import org.panbox.core.exception.MissingIVException;
import org.panbox.core.exception.ObfuscationException;

public class Obfuscator {

	private static Cipher encryptCipher;
	private static boolean zip = false;
	private static final Logger logger = Logger.getLogger("org.panbox.core");

	public static final String IV_POOL_PATH = PanboxConstants.PANBOX_SHARE_METADATA_DIRECTORY
			+ File.separator + "IVPool";

	private String sharePath;
	private String shareName;
	private AbstractObfuscatorIVPool ivPoolImpl;
	private OperatingSystem os;

	public Obfuscator(String sharePath, AbstractObfuscatorIVPool ivPoolImpl,
			String shareName) throws ObfuscationException {
		this.sharePath = sharePath;
		this.shareName = shareName;
		this.ivPoolImpl = ivPoolImpl;
		this.os = OS.getOperatingSystem();

		try {
			encryptCipher = Cipher.getInstance(KeyConstants.OBFUSCATOR_ALG);
			this.lookupvalueDigest = MessageDigest
					.getInstance(KeyConstants.IV_LOOKUP_HASH_ALG);
			this.ivDigest = MessageDigest
					.getInstance(KeyConstants.IV_LOOKUP_HASH_ALG);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			encryptCipher = null;
			logger.fatal(
					"Could not initialize cipher or digest - Fatal error!", e);
			throw new ObfuscationException(
					"Unable to create obfuscator - Could not initialize cipher or digest!",
					e);
		}

		// init IV pool sub directories
		initIVPoolSubDirs();
	}

	private void initIVPoolSubDirs() {
		for (int i = 0; i < Utils.hexArray.length; i++) {
			char cur = Utils.hexArray[i];
			File dirs = new File(this.sharePath + File.separator + IV_POOL_PATH
					+ File.separator + cur);
			dirs.mkdirs();
		}
	}

	private synchronized File getIVPoolSubDir(char firstChar) {
		return new File(this.sharePath + File.separator + IV_POOL_PATH
				+ File.separator + firstChar);
	}

	// realFile -> obfuscatedFile
	private LimitedHashMap<String, String> cachedObfuscatedNames = new LimitedHashMap<String, String>(
			1024 * 10);

	// obfuscatedFile -> realFile
	private LimitedHashMap<String, String> cachedDeObfuscatedNames = new LimitedHashMap<String, String>(
			1024 * 10);

	private MessageDigest lookupvalueDigest;

	private synchronized byte[] createLookupHash(String obfuscatedFileName,
			SecretKey obKey) throws ObfuscationException {
		try {
			lookupvalueDigest.update(obfuscatedFileName
					.getBytes(PanboxConstants.STANDARD_CHARSET));
			lookupvalueDigest.update(obKey.getEncoded());
			return lookupvalueDigest.digest();

		} catch (UnsupportedEncodingException e) {
			logger.error("Unsupported encoding", e);
			throw new ObfuscationException(
					"Error creating IV lookup value due to unsupported encoding!",
					e);
		}
	}

	/**
	 * Deobfuscates a string with the given key and encodes it with Base64
	 * 
	 * @param str
	 *            - string to deobfuscate
	 * @param key
	 *            - symmetric key used to decrypt the string
	 * @return - deobfuscated string
	 * @throws MissingIVException
	 *             if for a given filename, no IV sidecar file could be found.
	 *             base64LookupHash will be set in this exception
	 * @throws ObfuscationException
	 */

	public synchronized String deObfuscate(String str, SecretKey key)
			throws MissingIVException, ObfuscationException {

		logger.debug("Obfuscator("+this.shareName+") DeObfuscate(String, key): " + str);		

		// lookup in cache
		if (cachedDeObfuscatedNames.containsKey(str)) {
			logger.debug("DeObfuscate(String, key) - return cached value for: " + str);
			return cachedDeObfuscatedNames.get(str);
		}

		// if not in cache, do processing
		byte[] lookupHash = createLookupHash(str, key);
		String sLookupHash = Utils.bytesToHex(lookupHash);

		byte[] iv = ivPoolImpl.getCachedIV(sLookupHash, this.shareName);

		if (null == iv) {
			// re-read IVs from file-system or server
			logger.debug("Obfuscator: RE-fetch IV Pool");
			ivPoolImpl.fetchIVPool(this.sharePath, this.shareName);
			iv = ivPoolImpl.getCachedIV(sLookupHash, this.shareName);

			// // "direct" IV lookup
			// char firstChar = base64LookupHash.toUpperCase().charAt(0);
			// File path = getIVPoolSubDir(firstChar);
			// File[] res = path.listFiles((FileFilter) new PrefixFileFilter(
			// base64LookupHash));
			// iv = Utils.hexToBytes(res[0].getName().substring(41));

		}

		if (iv == null) {
			// this may be due to the CSP trying to resolve a conflict by
			// renaming two conflicting copies of a file. in this case, the
			// obfuscator's filename conflict resolution handler need to be
			// called to try to recover this situation. however, this needs to
			// be done *outside* of this method as we need full context (i.e.
			// absolute filename etc)

			// error cannot decode
			logger.error("Could not find IV for hash: " + sLookupHash
					+ " generated from obfuscate file: " + str);
			throw new MissingIVException("Could not find IV for hash: "
					+ sLookupHash + " generated from obfuscate file: " + str,
					sLookupHash);
		}

		String originalName = null;
		try {
			originalName = decryptFileName(str, key.getEncoded(), iv, false,
					EncodingType.BASE64);
		} catch (InvalidKeyException | NoSuchAlgorithmException
				| NoSuchPaddingException | InvalidAlgorithmParameterException
				| IllegalBlockSizeException | BadPaddingException
				| DataFormatException | IOException e) {
			logger.error("Could not deobfuscate string: " + str, e);
			throw new ObfuscationException("Could not decrypt string: " + str,
					e);
		}

		// do caching
		cachedDeObfuscatedNames.put(str, originalName);

		logger.debug("Obfuscator("+this.shareName+") DeObfuscate(String, key)-return: " + originalName);
		return originalName;

	}

	private MessageDigest ivDigest;

	private synchronized byte[] createIV(String originalFileName,
			SecretKey obKey) throws ObfuscationException {
		try {
			ivDigest.update(originalFileName
					.getBytes(PanboxConstants.STANDARD_CHARSET));
			ivDigest.update(obKey.getEncoded());
			byte[] hash = ivDigest.digest();

			// truncate to IV size
			return Arrays.copyOf(hash, KeyConstants.SYMMETRIC_BLOCK_SIZE);

		} catch (UnsupportedEncodingException e) {
			logger.error("Unsupported encoding", e);
			throw new ObfuscationException(
					"Error creating IV for filename due to unsupported encoding!",
					e);
		}
	}

	private synchronized void createIVFile(byte[] iv, String encryptedName,
			SecretKey key) throws ObfuscationException {
		// store IV in pool
		String ivhex = Utils.bytesToHex(iv);
		byte[] lookupHashBytes = createLookupHash(encryptedName, key);
		String lookupHashStr = Utils.bytesToHex(lookupHashBytes);

		char firstChar = lookupHashStr.toUpperCase().charAt(0);

		File dirs = getIVPoolSubDir(firstChar);

		// java based file creation
		File ivFile = new File(dirs.getAbsolutePath() + File.separator
				+ lookupHashStr + ivhex);
		try {
			ivFile.createNewFile();
		} catch (IOException e) {
			logger.error("Unable to create IV sidecar file!", e);
			throw new ObfuscationException("Unable to create IV sidecar file!",
					e);
		}
	}

	/**
	 * Obfuscates a string with the given key and encodes it with a fixed
	 * encoding
	 * 
	 * @param str
	 *            - string to obfuscate
	 * @param key
	 *            - Symmetric key to encrypt string with
	 * @param createiv
	 *            - if set to <code>true</code>, the corresponding sidecar file
	 *            within the IV pool will be created during obfuscation
	 * @return - A string encoded in Base64, representing the obfuscated string
	 * @throws ObfuscationException
	 *             if obfuscation failed for some reason
	 */
	public synchronized String obfuscate(String str, SecretKey key,
			boolean createiv) throws ObfuscationException {

		logger.debug("Obfuscator("+this.shareName+") Obfuscate(String, key): " + str + " with createiv=? "
				+ createiv);
		String encryptedName = null;
		if (cachedObfuscatedNames.containsKey(str)) {
			encryptedName = cachedObfuscatedNames.get(str);

			// logger.debug("obfuscate() return cached value " + encryptedName +
			// " for plain str: " + str);

			// Remove to fix bug with lookup value pointing to different IVs
			// if (createiv) {
			// createIVFile(createIV(str, key), encryptedName, key);
			// }
		} else {

			// logger.debug("obfuscate() do new obfuscation for plain str: " +
			// str);

			// calculate IV
			byte[] iv = createIV(str, key);

			try {
				encryptedName = encryptFileName(str, key, iv, zip,
						EncodingType.BASE64);
			} catch (InvalidKeyException | NoSuchAlgorithmException
					| NoSuchPaddingException
					| InvalidAlgorithmParameterException
					| IllegalBlockSizeException | BadPaddingException
					| IOException e) {
				logger.error("Could not obfuscate string: " + str, e);
				throw new ObfuscationException("Could not obfuscate string: "
						+ str, e);
			}

			if (createiv) {
				createIVFile(iv, encryptedName, key);
				// do caching, but only if the IV-file was created
				cachedObfuscatedNames.put(str, encryptedName);
			}

		}
		logger.debug("Obfuscator("+this.shareName+") Obfuscate(String, key)-return: " + encryptedName);
		return encryptedName;
	}

	private String encryptFileName(String filename, SecretKey key, byte[] iv,
			boolean zip, EncodingType encoding)
			throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException, IOException {
		// System.out.println("Original String ("+filename.length()+" chars):\t"
		// + filename);

		IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
		encryptCipher.init(Cipher.ENCRYPT_MODE, key, ivParameterSpec);

		byte[] encryptedBytes;
		if (zip) {
			// test zip filename
			byte[] zippedFileName = zipFileName(filename);

			// System.out.println("Zip ("
			// + ((float) zippedFileName.length / (float) filename
			// .getBytes().length) * 100 + "%) OrigName bytes: "
			// + filename.getBytes().length + " Zipped bytes: "
			// + zippedFileName.length);
			encryptedBytes = encryptCipher.doFinal(zippedFileName);
		} else {
			// if we do not have UTF-8, convert the filename to UTF-8
			byte[] bytes = filename.getBytes(PanboxConstants.STANDARD_CHARSET);
			// encryptedBytes = encryptCipher.doFinal(filename.getBytes());
			encryptedBytes = encryptCipher.doFinal(bytes);
		}

		// String text = Base64.encodeBase64URLSafeString(encryptedBytes);
		// System.out.println("Base64-Url String ("+base64Text.length()+" chars):\t"
		// + base64Text);
		String text = EncodingHelper.encodeByte(encryptedBytes, encoding);

		return text;
	}

	private String decryptFileName(String filename, byte[] key, byte[] iv,
			boolean zip, EncodingType encoding)
			throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException,
			DataFormatException, IOException {
		// byte[] encFileName = Base64.decodeBase64(filename);
		byte[] encFileName = EncodingHelper.decodeString(filename, encoding);

		Cipher decryptCipher = encryptCipher;

		IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
		SecretKeySpec aesKeySpec = new SecretKeySpec(key, "AES");

		decryptCipher.init(Cipher.DECRYPT_MODE, aesKeySpec, ivParameterSpec);

		byte[] decryptedBytes = decryptCipher.doFinal(encFileName);

		String decoded;

		if (zip) {
			decoded = unzipFileName(decryptedBytes);
		} else {
			// decoded = new String(decryptedBytes);

			// since we now force the conversion to UTF-8, we expect the
			// decrypted filename to be in UTF-8
			decoded = new String(decryptedBytes,
					PanboxConstants.STANDARD_CHARSET);
		}

		// System.out.println("Decrypted String:\t" + decoded);

		return decoded;
	}

	/**
	 * Obfuscates a full path with the given key and encodes it with a fixed
	 * encoding
	 * 
	 * @param path
	 *            - path to obfuscate
	 * @param key
	 *            - Symmetric key to encrypt string with
	 * @param createiv
	 *            - if set to <code>true</code>, the corresponding sidecar files
	 *            within the IV pool will be created during obfuscation
	 * @return - obfuscated path
	 * @throws ObfuscationException
	 *             if obfuscation failed for some reason
	 */
	public String obfuscatePath(String path, SecretKey key, boolean createivs)
			throws ObfuscationException {

		logger.debug("ObfuscatePath: " + path);

		String[] dirs = {};
		String newPath = "";

		if (!path.equals(File.separator)) {
			String splitter = File.separator.toString();
			if (os.isWindows()) {
				// WINx
				splitter += File.separator;
			}
			if (path.startsWith(File.separator)) {
				dirs = path.substring(1).split(splitter);
			} else {
				dirs = path.split(splitter);
			}
			for (String dir : dirs) {
				newPath += File.separator + obfuscate(dir, key, createivs);
			}
		} else {
			logger.debug("ObfuscatePath-return: " + path);
			return path;
		}
		logger.debug("ObfuscatePath-return: " + newPath);
		return newPath;
	}

	public String deObfuscatePath(String path, SecretKey obfuscationKey)
			throws MissingIVException, ObfuscationException {
		logger.debug("DeObfuscatePath: " + path);

		String[] dirs = {};
		StringBuffer newPath = new StringBuffer(path);

		if (!newPath.toString().equals(File.separator)) {
			if (os.isWindows()) {
				// WINx
				dirs = newPath.substring(1).split(
						File.separator + File.separator);
			} else {
				// LINUX
				dirs = newPath.substring(1).split(File.separator);
			}
			newPath.delete(0, newPath.length());
			for (String dir : dirs) {
				newPath.append(File.separator);
				newPath.append(deObfuscate(dir, obfuscationKey));
			}
		}
		logger.debug("DeObfuscatePath-return: " + newPath.toString());
		return newPath.toString();
	}

	/**
	 * Method for handling obfuscated filename conflicts. Checks, if the last
	 * part of the given absolute filename may have been marked as a conflicting
	 * copy by the CSP, tries to resolve and deobfuscate the original filename,
	 * and, if successful generates a proposal for a new obfuscated filename
	 * which incorporates the conflict-indicating suffix.
	 * 
	 * @param path
	 *            obfuscated absolute path
	 * @param obfuscationKey
	 * @return proposed new obfuscated filename, or <code>null</code>, if
	 *         conflict resolution was not successful
	 */
	public synchronized String resolveConflictCandidate(String path,
			SecretKey key) throws ObfuscationException {
		logger.debug("Checking potential conflict candidate " + path);
		String[] dirs = {};
		StringBuffer newPath = new StringBuffer(path);

		if (!path.equals(File.separator)) {
			if (os.isWindows()) {
				// WINx
				dirs = newPath.substring(1).split(
						File.separator + File.separator);
			} else {
				// LINUX
				dirs = newPath.substring(1).split(File.separator);
			}

			String conflictingName = dirs[dirs.length - 1];
			// lookup-value should be at the beginning of the filename,
			// conflict-indicating suffix is usually appended after some
			// whitespace
			String[] conflictParts = conflictingName.split("\\s+");
			String obfResolvedName = null;
			if (conflictParts.length > 1) {
				// check if we can find an IV for the conflict prefix value
				try {
					String deobfConflict = deObfuscate(conflictParts[0], key);
					// we were able to deobfuscate the prefix, now resolve
					// conflict
					logger.debug("Identified conflict for filename "
							+ conflictingName);

					// new filename is obfuscate(deobfuscatedprefix +
					// conflict-indicating suffix)
					// TODO: regex interpretation of typical conflict sufixes
					String conflictIndicatorSuffix = conflictingName
							.substring(conflictParts[0].length());
					String resolvedName = deobfConflict
							+ conflictIndicatorSuffix;
					// create iv sidecar file in advance
					obfResolvedName = obfuscate(resolvedName, key, true);

					logger.debug("Resolved conflicting filename to "
							+ resolvedName + ", created obfuscated value: "
							+ obfResolvedName);
				} catch (MissingIVException e) {
					logger.error("Unable to resolve conflict for " + path, e);
					// ObfuscationException causing this problem already has
					// been thrown before, just return null at this point.
					return null;
				}

			} else {
				logger.error("Unable to determine conflict-indicating suffix in filename "
						+ path);
				return null;
			}

			newPath.delete(0, newPath.length());
			for (int i = 0; i < dirs.length - 1; i++) {
				newPath.append(File.separator);
				newPath.append(dirs[i]);
			}

			// append obfuscated resolved filename
			newPath.append(File.separator);
			newPath.append(obfResolvedName);

			// return proposed new filename
			logger.debug("Return proposed obfuscated conflict resolution filename "
					+ newPath.toString());
			return newPath.toString();
		} else {
			return null;
		}
	}

	// old stuff

	// @Deprecated
	// public static synchronized String obfuscateWithZip(String filename,
	// SecretKey key, byte[] iv, EncodingType encoding) {
	// try {
	// return encryptFileName(filename, key, iv, true, encoding);
	// } catch (InvalidKeyException | NoSuchAlgorithmException
	// | NoSuchPaddingException | InvalidAlgorithmParameterException
	// | IllegalBlockSizeException | BadPaddingException | IOException e) {
	// logger.error("Could not obfuscate and zip string: " + filename, e);
	// }
	// return null;
	// }
	//
	// @Deprecated
	// public static synchronized String deObfuscateWithZip(String filename,
	// byte[] key, byte[] iv, EncodingType encoding) {
	// try {
	// return decryptFileName(filename, key, iv, true, encoding);
	// } catch (InvalidKeyException | NoSuchAlgorithmException
	// | NoSuchPaddingException | InvalidAlgorithmParameterException
	// | IllegalBlockSizeException | BadPaddingException
	// | DataFormatException | IOException e) {
	// logger.error("Could not deobfuscate zipped string: " + filename, e);
	// }
	// return null;
	// }

	@Deprecated
	private static synchronized byte[] zipFileName(String fileName)
			throws IOException {
		Deflater deflater = new Deflater();

		byte[] bytes = fileName.getBytes();
		deflater.setInput(bytes);

		deflater.finish();

		ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
		byte[] buffer = new byte[1024];

		while (!deflater.finished()) {
			int bytesCompressed = deflater.deflate(buffer);
			bos.write(buffer, 0, bytesCompressed);
		}
		bos.close();

		int count = 0;
		for (int i = 0; i < buffer.length; i++) {
			if (buffer[i] != 0)
				count++;
		}

		byte[] result = new byte[count];
		for (int i = 0; i < result.length; i++) {
			result[i] = buffer[i];
		}

		return result;
	}

	@Deprecated
	private static synchronized String unzipFileName(byte[] zippedFileName)
			throws DataFormatException, IOException {
		Inflater inflater = new Inflater();
		inflater.setInput(zippedFileName);

		byte[] buffer = new byte[1024];
		inflater.inflate(buffer);
		inflater.end();

		int count = 0;
		for (int i = 0; i < buffer.length; i++) {
			if (buffer[i] != 0)
				count++;
		}

		byte[] result = new byte[count];
		for (int i = 0; i < result.length; i++) {
			result[i] = buffer[i];
		}
		return new String(result, PanboxConstants.STANDARD_CHARSET);

	}

}
