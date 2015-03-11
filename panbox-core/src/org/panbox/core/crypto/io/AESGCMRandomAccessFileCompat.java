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
package org.panbox.core.crypto.io;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.Set;
import java.util.WeakHashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import org.apache.log4j.Logger;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.panbox.core.crypto.KeyConstants;
import org.panbox.core.exception.FileEncryptionException;
import org.panbox.core.exception.FileIntegrityException;
import org.panbox.core.exception.RandomDataGenerationException;

/**
 * @author palige
 * 
 *         AES GCM based implementation of {@link EncRandomAccessFile} with
 *         additional integrity protection mechanism (see
 *         {@link AuthTagVerifier}. This implementation is compatible with java
 *         versions < 1.7 but requires Bouncycastle.
 */

public class AESGCMRandomAccessFileCompat extends
		AbstractAESGCMRandomAccessFile {

	private static final Logger log = Logger.getLogger("org.panbox.core");

	protected AESGCMRandomAccessFileCompat(File backingFile)
			throws InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidAlgorithmParameterException,
			NoSuchProviderException, RandomDataGenerationException, IOException {
		super(backingFile);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.panbox.core.crypto.io.EncRandomAccessFile#getCryptoProvider()
	 */
	@Override
	String getCryptoProvider() {
		return KeyConstants.PROV_BC;
	}

	/**
	 * initialize ciphers
	 * 
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 * @throws RandomDataGenerationException
	 * @throws InvalidAlgorithmParameterException
	 * @throws NoSuchProviderException
	 */
	protected void initCiphers() throws NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidKeyException,
			RandomDataGenerationException, InvalidAlgorithmParameterException,
			NoSuchProviderException {
		super.initCiphers();

		this.gcmEngine = new GCMBlockCipher(new AESFastEngine());
	}

	protected GCMBlockCipher gcmEngine;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.panbox.core.crypto.io.AbstractAESGCMRandomAccessFile#getBlockSize()
	 */
	@Override
	int getBlockSize() {
		return gcmEngine.getUnderlyingCipher().getBlockSize();
	}

	protected byte[] getFileKeyBytes() {
		return getFileKey().getFormat().equalsIgnoreCase("RAW") ? getFileKey()
				.getEncoded() : null;
	}

	@Override
	protected byte[] _readChunk(long index) throws IOException,
			FileEncryptionException, FileIntegrityException {
		// first, get chunk iv for decryption
		long oldpos = backingRandomAccessFile.getFilePointer();
		backingRandomAccessFile.seek(chunkOffset(index));

		// read iv
		byte[] iv = new byte[CHUNK_IV_SIZE];
		int ret = backingRandomAccessFile.read(iv);
		if (ret != CHUNK_IV_SIZE) {
			throw new FileEncryptionException("Size mismatch reading chunk IV!");
		}

		// prepare params for GCM decryption
		// retrieve key bytes from SecretKey
		byte[] key = getFileKeyBytes();
		if ((key == null)
				|| (key.length != KeyConstants.SYMMETRIC_FILE_KEY_SIZE_BYTES)) {
			throw new FileEncryptionException("Invalid encryption key format!");
		}

		// prepare additional authenticated data (index and lastchunkflag as
		// bytes) for verifying metadata integrity
		// byte[] indexAsBytes = IntByteConv.int2byte(index);
		byte[] indexAsBytes = LongByteConv.long2Bytes(index);
		byte[] lastchunkflagAsBytes = BooleanByteConv.bool2byte(false);

		if ((indexAsBytes == null) || (lastchunkflagAsBytes == null)
				|| (indexAsBytes.length == 0)
				|| (lastchunkflagAsBytes.length == 0)) {
			throw new FileEncryptionException(
					"Invalid additional autenticated data!");
		}

		byte[] associatedText = new byte[indexAsBytes.length
				+ lastchunkflagAsBytes.length];
		System.arraycopy(indexAsBytes, 0, associatedText, 0,
				indexAsBytes.length);
		System.arraycopy(lastchunkflagAsBytes, 0, associatedText,
				indexAsBytes.length, lastchunkflagAsBytes.length);

		AEADParameters gcmParams = new AEADParameters(new KeyParameter(key),
				GCM_AUTHENTICATION_TAG_LEN, iv, associatedText);

		GCMBlockCipher gcmEngine = new GCMBlockCipher(new AESFastEngine());
		gcmEngine.init(false, gcmParams);

		byte[] decMsg = new byte[gcmEngine.getOutputSize(CHUNK_ENC_DATA_SIZE)];
		byte[] encMsg = new byte[CHUNK_ENC_DATA_SIZE];

		ret = backingRandomAccessFile.read(encMsg);
		backingRandomAccessFile.seek(oldpos);

		if (ret != CHUNK_ENC_DATA_SIZE) {
			throw new FileEncryptionException(
					"Size mismatch reading encrypted chunk data!");
		}

		int decLen = gcmEngine
				.processBytes(encMsg, 0, encMsg.length, decMsg, 0);
		try {
			decLen += gcmEngine.doFinal(decMsg, decLen);
		} catch (IllegalStateException | InvalidCipherTextException e) {
			if ((e instanceof InvalidCipherTextException)
					&& (e.getMessage().contains("mac check in GCM failed"))) {
				throw new FileIntegrityException("Decryption error in chunk "
						+ index + ". Possible file integrity violation.", e);
			} else {
				throw new FileEncryptionException("Decryption error in chunk "
						+ index + ": " + e.getMessage(), e);
			}
		}

		if ((decMsg == null) || (decMsg.length != CHUNK_DATA_SIZE)) {
			throw new FileEncryptionException(
					"Decryption error or chunk size mismatch during decryption!");
		} else {
			if (implementsAuthentication()) {
				// check authentication tag for integrity
				byte[] tag = Arrays.copyOfRange(encMsg, decMsg.length,
						encMsg.length);
				if (!getAuthTagVerifier().verifyChunkAuthTag((int) index, tag)) {
					throw new FileIntegrityException(
							"File authentication tag verification failed in chunk "
									+ index);
				}
			}
			return decMsg;
		}
	}

	@Override
	protected byte[] _readLastChunk(long index) throws IOException,
			FileEncryptionException, FileIntegrityException {
		// check how many bytes there are left to read
		// System.err.println("_readLAstChunk index: " + index);
		long nRemaining = backingRandomAccessFile.length()
				- (chunkOffset(index));

		// just to be sure
		if (nRemaining > CHUNK_ENC_SIZE) {
			throw new FileEncryptionException(
					"Calculated size of size of last chunk bigger than default chunk size!");
		} else if (nRemaining <= CHUNK_IV_SIZE) {
			return new byte[] {};
			// throw new FileEncryptionException(
			// "Calculated size of size of last chunk smaller than minimum size!");
		}

		// get chunk iv for decryption
		long oldpos = backingRandomAccessFile.getFilePointer();
		backingRandomAccessFile.seek(chunkOffset(index));

		// read iv
		byte[] iv = new byte[CHUNK_IV_SIZE];
		int ret = backingRandomAccessFile.read(iv);
		if (ret != CHUNK_IV_SIZE) {
			throw new FileEncryptionException("Size mismatch reading chunk IV!");
		}

		// prepare params for GCM decryption
		// retrieve key bytes from SecretKey
		byte[] key = getFileKeyBytes();
		if ((key == null)
				|| (key.length != KeyConstants.SYMMETRIC_FILE_KEY_SIZE_BYTES)) {
			throw new FileEncryptionException("Invalid encryption key format!");
		}

		// prepare additional authenticated data (index and lastchunkflag as
		// bytes) for verifying metadata integrity
		// byte[] indexAsBytes = IntByteConv.int2byte(index);
		byte[] indexAsBytes = LongByteConv.long2Bytes(index);
		byte[] lastchunkflagAsBytes = BooleanByteConv.bool2byte(true);

		if ((indexAsBytes == null) || (lastchunkflagAsBytes == null)
				|| (indexAsBytes.length == 0)
				|| (lastchunkflagAsBytes.length == 0)) {
			throw new FileEncryptionException(
					"Invalid additional autenticated data!");
		}

		byte[] associatedText = new byte[indexAsBytes.length
				+ lastchunkflagAsBytes.length];
		System.arraycopy(indexAsBytes, 0, associatedText, 0,
				indexAsBytes.length);
		System.arraycopy(lastchunkflagAsBytes, 0, associatedText,
				indexAsBytes.length, lastchunkflagAsBytes.length);

		AEADParameters gcmParams = new AEADParameters(new KeyParameter(key),
				GCM_AUTHENTICATION_TAG_LEN, iv, associatedText);

		gcmEngine.init(false, gcmParams);

		// adjust number of remaining bytes for reading encrypted data
		nRemaining -= CHUNK_IV_SIZE;

		byte[] decMsg = new byte[gcmEngine.getOutputSize((int) nRemaining)];
		byte[] encMsg = new byte[(int) nRemaining];

		ret = backingRandomAccessFile.read(encMsg);
		backingRandomAccessFile.seek(oldpos);

		if (ret != nRemaining) {
			throw new FileEncryptionException(
					"Size mismatch reading encrypted chunk data!");
		}

		// decrypt data
		int decLen = gcmEngine
				.processBytes(encMsg, 0, encMsg.length, decMsg, 0);
		try {
			decLen += gcmEngine.doFinal(decMsg, decLen);
		} catch (IllegalStateException | InvalidCipherTextException e) {
			if ((e instanceof InvalidCipherTextException)
					&& (e.getMessage().contains("mac check in GCM failed"))) {
				throw new FileIntegrityException("Decryption error in chunk "
						+ index + ". Possible file integrity violation.", e);
			} else {
				throw new FileEncryptionException("Decryption error in chunk "
						+ index + ": " + e.getMessage(), e);
			}
		}

		if ((decMsg == null) || (decMsg.length != (nRemaining - CHUNK_TLEN))) {
			throw new FileEncryptionException(
					"Decryption error or chunk size mismatch during decryption!");
		} else {
			if (implementsAuthentication()) {
				// check authentication tag for integrity
				byte[] tag = Arrays.copyOfRange(encMsg, decMsg.length,
						encMsg.length);
				if (!getAuthTagVerifier().verifyChunkAuthTag((int) index, tag)) {
					throw new FileIntegrityException(
							"File authentication tag verification failed in last chunk "
									+ index);
				}
			}
			return decMsg;
		}
	}

	@Override
	protected void _writeChunk(byte[] buffer, long index)
			throws FileEncryptionException, RandomDataGenerationException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException, IOException {

		// write only if authentication tag verification succeeds.
		// if (implementsAuthentication() && length() > 0 && !onlyCachedData())
		// {
		// flushAuthData();
		// if (!getAuthTagVerifier().verifyFileAuthTag()) {
		// throw new FileEncryptionException(
		// "File authentication tag verification failed!");
		// }
		// }

		// initialize cipher with corresponding chunk IV
		byte[] iv = generateRandomChunkIV();

		// prepare params for GCM encryption
		// retrieve key bytes from SecretKey
		byte[] key = getFileKeyBytes();
		if ((key == null)
				|| (key.length != KeyConstants.SYMMETRIC_FILE_KEY_SIZE_BYTES)) {
			throw new FileEncryptionException("Invalid encryption key format!");
		}

		// prepare additional authenticated data (index and lastchunkflag as
		// bytes) for verifying metadata integrity
		// byte[] indexAsBytes = IntByteConv.int2byte(index);
		byte[] indexAsBytes = LongByteConv.long2Bytes(index);
		byte[] lastchunkflagAsBytes = BooleanByteConv.bool2byte(false);

		if ((indexAsBytes == null) || (lastchunkflagAsBytes == null)
				|| (indexAsBytes.length == 0)
				|| (lastchunkflagAsBytes.length == 0)) {
			throw new FileEncryptionException(
					"Invalid additional autenticated data!");
		}

		byte[] associatedText = new byte[indexAsBytes.length
				+ lastchunkflagAsBytes.length];
		System.arraycopy(indexAsBytes, 0, associatedText, 0,
				indexAsBytes.length);
		System.arraycopy(lastchunkflagAsBytes, 0, associatedText,
				indexAsBytes.length, lastchunkflagAsBytes.length);

		AEADParameters gcmParams = new AEADParameters(new KeyParameter(key),
				GCM_AUTHENTICATION_TAG_LEN, iv, associatedText);

		gcmEngine.init(true, gcmParams);

		byte[] encMsg = new byte[gcmEngine.getOutputSize(buffer.length)];
		int encLen = gcmEngine
				.processBytes(buffer, 0, buffer.length, encMsg, 0);
		try {
			encLen += gcmEngine.doFinal(encMsg, encLen);
		} catch (IllegalStateException | InvalidCipherTextException e) {
			throw new FileEncryptionException(
					"Error encrypting chunk " + index, e);
		}

		if (encMsg == null || encMsg.length != CHUNK_ENC_DATA_SIZE) {
			throw new FileEncryptionException(
					"Encrypted chunk length mismatch!");
		}

		// now write complete chunk into file
		long oldpos = backingRandomAccessFile.getFilePointer();

		backingRandomAccessFile.seek(chunkOffset(index));
		// first write chunk iv
		backingRandomAccessFile.write(iv);
		// next, write encrypted data
		backingRandomAccessFile.write(encMsg);
		// return to initial offset
		backingRandomAccessFile.seek(oldpos);

		if (implementsAuthentication()) {
			// extract & update auth tag
			byte[] tag = Arrays.copyOfRange(encMsg, buffer.length,
					encMsg.length);
			getAuthTagVerifier().updateChunkAuthTag((int) index, tag);
		}
	}

	@Override
	protected void _writeLastChunk(byte[] buffer, long index)
			throws FileEncryptionException, RandomDataGenerationException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException, IOException {
		// write only if authentication tag verification succeeds.
		// if (implementsAuthentication() && length() > 0 && !onlyCachedData())
		// {
		// // FIXME: flushing auth data & subsequently checking the file auth
		// // tag will always succeed
		// flushAuthData();
		// if (!getAuthTagVerifier().verifyFileAuthTag()) {
		// throw new FileEncryptionException(
		// "File authentication tag verification failed!");
		// }
		// }

		// initialize cipher with corresponding chunk IV
		byte[] iv = generateRandomChunkIV();

		// prepare params for GCM encryption
		// retrieve key bytes from SecretKey
		byte[] key = getFileKeyBytes();
		if ((key == null)
				|| (key.length != KeyConstants.SYMMETRIC_FILE_KEY_SIZE_BYTES)) {
			throw new FileEncryptionException("Invalid encryption key format!");
		}

		// prepare additional authenticated data (index and lastchunkflag as
		// bytes) for verifying metadata integrity
		// byte[] indexAsBytes = IntByteConv.int2byte(index);
		byte[] indexAsBytes = LongByteConv.long2Bytes(index);
		byte[] lastchunkflagAsBytes = BooleanByteConv.bool2byte(true);

		if ((indexAsBytes == null) || (lastchunkflagAsBytes == null)
				|| (indexAsBytes.length == 0)
				|| (lastchunkflagAsBytes.length == 0)) {
			throw new FileEncryptionException(
					"Invalid additional autenticated data!");
		}

		byte[] associatedText = new byte[indexAsBytes.length
				+ lastchunkflagAsBytes.length];
		System.arraycopy(indexAsBytes, 0, associatedText, 0,
				indexAsBytes.length);
		System.arraycopy(lastchunkflagAsBytes, 0, associatedText,
				indexAsBytes.length, lastchunkflagAsBytes.length);

		AEADParameters gcmParams = new AEADParameters(new KeyParameter(key),
				GCM_AUTHENTICATION_TAG_LEN, iv, associatedText);

		gcmEngine.init(true, gcmParams);

		byte[] encMsg = new byte[gcmEngine.getOutputSize(buffer.length)];
		int encLen = gcmEngine
				.processBytes(buffer, 0, buffer.length, encMsg, 0);
		try {
			encLen += gcmEngine.doFinal(encMsg, encLen);
		} catch (IllegalStateException | InvalidCipherTextException e) {
			throw new FileEncryptionException(
					"Error encrypting chunk " + index, e);
		}

		// length of plaintext should match ciphertext minus length of the
		// authentication tag
		if ((encMsg == null) || (encMsg.length != (buffer.length + CHUNK_TLEN))) {
			throw new FileEncryptionException(
					"Encrypted chunk length mismatch!");
		}

		long oldpos = backingRandomAccessFile.getFilePointer();

		backingRandomAccessFile.seek(chunkOffset(index));

		// first write iv
		backingRandomAccessFile.write(iv);
		// then write encrypted data
		backingRandomAccessFile.write(encMsg);
		// then return to initial position
		backingRandomAccessFile.seek(oldpos);

		if (implementsAuthentication()) {
			// extract & update auth tag
			byte[] tag = Arrays.copyOfRange(encMsg, buffer.length,
					encMsg.length);
			getAuthTagVerifier().updateChunkAuthTag((int) index, tag);
		}
		// System.out.println(authTagVerifier);
	}

	/**
	 * Method creates a new {@link AESGCMRandomAccessFileCompat} with the given
	 * arguments (and, correspondingly, read/write access). This method assumes
	 * there currently exists no file at the specified location, otherwise an
	 * exception is thrown. For opening existing files, see
	 * {@link #open(File, String)}.
	 * 
	 * @param shareKeyVersion
	 *            latest share key version
	 * @param shareKey
	 *            latest share key
	 * @param file
	 *            file to create
	 * @throws FileEncryptionException
	 *             if something went wrong during file creation
	 * @return
	 * @throws IOException
	 * @throws RandomDataGenerationException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws NoSuchProviderException
	 * @throws InvalidAlgorithmParameterException
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 */
	public static AESGCMRandomAccessFileCompat create(int shareKeyVersion,
			SecretKey shareKey, File file) throws FileEncryptionException,
			IOException {
		AESGCMRandomAccessFileCompat ret = AESGCMRandomAccessFileCompat
				.getInstance(file, true);
		ret.create(shareKeyVersion, shareKey);
		return ret;
	}

	public static AESGCMRandomAccessFileCompat create(int shareKeyVersion,
			SecretKey shareKey, String file, String mode)
			throws FileEncryptionException, InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidAlgorithmParameterException, NoSuchProviderException,
			IllegalBlockSizeException, BadPaddingException, IOException,
			RandomDataGenerationException {
		return create(shareKeyVersion, shareKey, new File(file));
	}

	/**
	 * Method opens a {@link AESGCMRandomAccessFileCompat}-instance with the
	 * given arguments for an already existing file. For creating new files, see
	 * {@link #create(int, SecretKey, File)}. NOTE: After an instance has been
	 * obtained with this method, ist still needs to be initialized with the
	 * corresponding share key from the share metadata DB.
	 * 
	 * @param file
	 *            file to open
	 * @param mode
	 *            access mode
	 * @return
	 * @throws FileEncryptionException
	 * @throws IOException
	 * @throws NoSuchProviderException
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidKeyException
	 * @throws RandomDataGenerationException
	 * @throws InvalidAlgorithmParameterException
	 */
	public static AESGCMRandomAccessFileCompat open(File file, boolean writable)
			throws FileEncryptionException, IOException {
		AESGCMRandomAccessFileCompat ret = AESGCMRandomAccessFileCompat
				.getInstance(file, writable);
		ret.open();
		return ret;
	}

	public static AESGCMRandomAccessFileCompat open(String file,
			boolean writable) throws FileEncryptionException, IOException {
		return open(new File(file), writable);
	}

	protected final static WeakHashMap<InstanceEntry, AESGCMRandomAccessFileCompat> instanceMap = new WeakHashMap<InstanceEntry, AESGCMRandomAccessFileCompat>();

	public static synchronized AESGCMRandomAccessFileCompat getInstance(
			File backingFile, boolean writable) throws FileEncryptionException,
			IOException {
		try {
			AESGCMRandomAccessFileCompat ret = instanceMap.get(InstanceEntry
					.instance(backingFile, writable));
			if (ret == null) {
				ret = new AESGCMRandomAccessFileCompat(backingFile);
				ret.writable = writable;
				instanceMap.put(InstanceEntry.instance(backingFile, writable),
						ret);
			}

			return ret;
		} catch (InvalidKeyException | NoSuchAlgorithmException
				| NoSuchPaddingException | InvalidAlgorithmParameterException
				| NoSuchProviderException | RandomDataGenerationException e) {
			throw new FileEncryptionException("Error getting instance!", e);
		}
	}

	protected static WeakHashMap<InstanceEntry, AESGCMRandomAccessFileCompat> getInstanceMap() {
		return instanceMap;
	}

	@Override
	protected void printInstanceMap() {
		Set<InstanceEntry> en = instanceMap.keySet();
		while (en.iterator().hasNext()) {
			EncRandomAccessFile.InstanceEntry instanceEntry = (EncRandomAccessFile.InstanceEntry) en
					.iterator().next();
			System.out.println("entry: "
					+ instanceEntry.getNormalizedFilename() + ", writable: "
					+ instanceEntry.isWritable());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.panbox.core.crypto.io.EncRandomAccessFile#renameTo(java.io.File)
	 */
	@Override
	public boolean renameTo(File f) {
		if (backingFile.renameTo(f)) {
			AESGCMRandomAccessFileCompat tmp;
			try {
				// all previous instances pointing to f are obsolete by now

				tmp = instanceMap.remove(InstanceEntry.instance(f, false));
				if (tmp != null) {
					if (tmp.isOpen())
						tmp.close();
				}
				tmp = instanceMap.remove(InstanceEntry.instance(f, true));
				if (tmp != null) {
					if (tmp.isOpen())
						tmp.close();
				}

			} catch (IOException e) {
				log.error(getClass()
						+ "::renameTo: unable to close instance on File \""
						+ f.getAbsolutePath() + "\": " + e.getMessage());
			}

			// replace entries in instance map
			if ((tmp = instanceMap.remove(InstanceEntry.instance(backingFile,
					false))) != null) {
				if (!writable) {
					// rename readonly to readonly -> OK
					instanceMap.put(InstanceEntry.instance(f, false), this);
					// rename writable to readonly -> not OK
					log.debug("rename: Readonly instance for file has been replaced. old="
							+ backingFile.getName() + ",new=" + f.getName());
				} else {
					// rename writable to readonly -> not OK
					log.debug("rename: Readonly instance for file has been discarded. old="
							+ backingFile.getName() + ",new=" + f.getName());
				}
			}
			if ((tmp = instanceMap.remove(InstanceEntry.instance(backingFile,
					true))) != null) {
				if (writable) {
					// rename readonly to readonly -> OK
					instanceMap.put(InstanceEntry.instance(f, true), this);
					log.debug("rename: Writable instance for file has been replaced. old="
							+ backingFile.getName() + ",new=" + f.getName());
				} else {
					// rename writable to readonly -> not OK
					log.debug("rename: Writable instance for file has been discarded. old="
							+ backingFile.getName() + ",new=" + f.getName());
				}
			}

			// switch backing file
			this.backingFile = f;

			return true;
		} else {
			return false;
		}
	}

	@Override
	public synchronized void close() throws IOException {
		super.close();
		// don't forget to remove instance - otherwise this might create a
		// memory
		// leak for large numbers of files
		instanceMap.remove(InstanceEntry.instance(backingFile, writable));
	}

}
