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

import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import org.apache.log4j.Logger;
import org.panbox.core.crypto.KeyConstants;
import org.panbox.core.exception.FileEncryptionException;
import org.panbox.core.exception.FileIntegrityException;
import org.panbox.core.exception.RandomDataGenerationException;

/**
 * @author palige
 * 
 *         AES GCM based implementation of {@link EncRandomAccessFile} with
 *         additional integrity protection mechanism (see
 *         {@link AuthTagVerifier}. This implementation is incompatible with
 *         java versions < 1.7. May be used with either SunJCE or Bouncycastle.
 */

public class AESGCMRandomAccessFile extends AbstractAESGCMRandomAccessFile {

	private static final Logger log = Logger.getLogger("org.panbox.core");

	private AESGCMRandomAccessFile(File backingFile)
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

	@Override
	protected void initCiphers() throws NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidKeyException,
			RandomDataGenerationException, InvalidAlgorithmParameterException,
			NoSuchProviderException {
		super.initCiphers();
		this.encCipher = Cipher.getInstance(CIPHER_CHUNK, getCryptoProvider());
		this.decCipher = Cipher.getInstance(CIPHER_CHUNK, getCryptoProvider());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.panbox.core.crypto.io.AbstractAESGCMRandomAccessFile#getBlockSize()
	 */
	@Override
	int getBlockSize() {
		return encCipher.getBlockSize();
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

		GCMParameterSpec spec = new GCMParameterSpec(
				GCM_AUTHENTICATION_TAG_LEN, iv);
		byte[] res, buf;
		try {
			decCipher.init(Cipher.DECRYPT_MODE, getFileKey(), spec);

			// set chunk metadata for verifying metadata integrity

			// index of current chunk
			decCipher.updateAAD(LongByteConv.long2Bytes(index));
			// flag indicating if we're writing the last chunk
			decCipher.updateAAD(BooleanByteConv.bool2byte(false));

			buf = new byte[CHUNK_ENC_DATA_SIZE];

			ret = backingRandomAccessFile.read(buf);
			backingRandomAccessFile.seek(oldpos);

			if (ret != CHUNK_ENC_DATA_SIZE) {
				throw new FileEncryptionException(
						"Size mismatch reading encrypted chunk data!");
			}

			// decrypt data
			res = decCipher.doFinal(buf);
		} catch (AEADBadTagException e) {
			throw new FileIntegrityException("Decryption error in chunk "
					+ index + ". Possible file integrity violation.", e);
		} catch (InvalidKeyException | InvalidAlgorithmParameterException
				| IllegalBlockSizeException | BadPaddingException e) {
			throw new FileEncryptionException("Decryption error in chunk "
					+ index + ": " + e.getMessage(), e);
		}

		if ((res == null) || (res.length != CHUNK_DATA_SIZE)) {
			throw new FileEncryptionException(
					"Decryption error or chunk size mismatch during decryption!");
		} else {
			if (implementsAuthentication()) {
				// check authentication tag for integrity
				byte[] tag = Arrays.copyOfRange(buf, res.length, buf.length);
				if (!getAuthTagVerifier().verifyChunkAuthTag((int) index, tag)) {
					throw new FileIntegrityException(
							"File authentication tag verification failed in chunk "
									+ index);
				}
			}
			return res;
		}
	}

	@Override
	protected byte[] _readLastChunk(long index) throws IOException,
			FileEncryptionException, FileIntegrityException {
		// check how many bytes there are left to read
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

		// initialize cipher with corresponding chunk IV
		GCMParameterSpec spec = new GCMParameterSpec(
				GCM_AUTHENTICATION_TAG_LEN, iv);

		byte[] res, buf;
		try {
			decCipher.init(Cipher.DECRYPT_MODE, getFileKey(), spec);

			// set chunk metadata for verifying metadata integrity

			// index of current chunk
			decCipher.updateAAD(LongByteConv.long2Bytes(index));
			// flag indicating if we're writing the last chunk
			decCipher.updateAAD(BooleanByteConv.bool2byte(true));

			// adjust number of remaining bytes for reading encrypted data
			nRemaining -= CHUNK_IV_SIZE;

			buf = new byte[(int) nRemaining];

			ret = backingRandomAccessFile.read(buf);
			backingRandomAccessFile.seek(oldpos);

			if (ret != nRemaining) {
				throw new FileEncryptionException(
						"Size mismatch reading encrypted chunk data!");
			}

			// decrypt data
			res = decCipher.doFinal(buf);
		} catch (AEADBadTagException e) {
			throw new FileIntegrityException("Decryption error in chunk "
					+ index + ". Possible file integrity violation.", e);
		} catch (InvalidKeyException | InvalidAlgorithmParameterException
				| IllegalBlockSizeException | BadPaddingException e) {
			throw new FileEncryptionException("Decryption error in chunk "
					+ index + ": " + e.getMessage(), e);
		}

		if ((res == null) || (res.length != (nRemaining - CHUNK_TLEN))) {
			throw new FileEncryptionException(
					"Decryption error or chunk size mismatch during decryption!");
		} else {
			if (implementsAuthentication()) {
				// check authentication tag for integrity
				byte[] tag = Arrays.copyOfRange(buf, res.length, buf.length);
				if (!getAuthTagVerifier().verifyChunkAuthTag((int) index, tag)) {
					throw new FileIntegrityException(
							"File authentication tag verification failed in last chunk "
									+ index);
				}
			}
			return res;
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

		GCMParameterSpec spec = new GCMParameterSpec(
				GCM_AUTHENTICATION_TAG_LEN, iv);

		encCipher.init(Cipher.ENCRYPT_MODE, getFileKey(), spec);

		// set chunk metadata, i.e. chunk index and flag indicating if
		// current chunk is last chunk. NOTE: the latter is essential to be able
		// to
		// determine if a file has been manually truncated

		// index of current chunk
		encCipher.updateAAD(LongByteConv.long2Bytes(index));
		// flag indicating if we're writing the last chunk
		encCipher.updateAAD(BooleanByteConv.bool2byte(false));

		// no need to use update(). cipher iterates over blocks & chunk is
		// held in memory anyway
		byte[] encChunk = encCipher.doFinal(buffer);

		if (encChunk == null || encChunk.length != CHUNK_ENC_DATA_SIZE) {
			throw new FileEncryptionException(
					"Encrypted chunk length mismatch!");
		}

		// now write complete chunk into file
		long oldpos = backingRandomAccessFile.getFilePointer();

		backingRandomAccessFile.seek(chunkOffset(index));
		// first write chunk iv
		backingRandomAccessFile.write(iv);
		// next, write encrypted data
		backingRandomAccessFile.write(encChunk);
		// return to initial offset
		backingRandomAccessFile.seek(oldpos);

		if (implementsAuthentication()) {
			// extract & update auth tag
			byte[] tag = Arrays.copyOfRange(encChunk, buffer.length,
					encChunk.length);
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

		GCMParameterSpec spec = new GCMParameterSpec(
				GCM_AUTHENTICATION_TAG_LEN, iv);

		encCipher.init(Cipher.ENCRYPT_MODE, getFileKey(), spec);

		// set chunk metadata, i.e. chunk index and flag indicating if current
		// chunk is last chunk. NOTE: the latter is essential to be able to
		// determine if a file has been manually truncated

		// index of current chunk
		encCipher.updateAAD(LongByteConv.long2Bytes(index));
		// flag indicating if we're writing the last chunk
		encCipher.updateAAD(BooleanByteConv.bool2byte(true));

		byte[] encChunk = encCipher.doFinal(buffer);

		// length of plaintext should match ciphertext minus length of the
		// authentication tag
		if ((encChunk == null)
				|| (encChunk.length != (buffer.length + CHUNK_TLEN))) {
			throw new FileEncryptionException(
					"Encrypted chunk length mismatch!");
		}

		long oldpos = backingRandomAccessFile.getFilePointer();

		backingRandomAccessFile.seek(chunkOffset(index));

		// first write iv
		backingRandomAccessFile.write(iv);
		// then write encrypted data
		backingRandomAccessFile.write(encChunk);
		// then return to initial position
		backingRandomAccessFile.seek(oldpos);

		if (implementsAuthentication()) {
			// extract & update auth tag
			byte[] tag = Arrays.copyOfRange(encChunk, buffer.length,
					encChunk.length);
			getAuthTagVerifier().updateChunkAuthTag((int) index, tag);
		}
		// System.out.println(authTagVerifier);
	}

	/**
	 * Method creates a new {@link AESGCMRandomAccessFile} with the given
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
	public static AESGCMRandomAccessFile create(int shareKeyVersion,
			SecretKey shareKey, File file) throws FileEncryptionException,
			IOException {
		AESGCMRandomAccessFile ret = AESGCMRandomAccessFile.getInstance(file,
				true);
		ret.create(shareKeyVersion, shareKey);
		return ret;
	}

	public static AESGCMRandomAccessFile create(int shareKeyVersion,
			SecretKey shareKey, String file, String mode)
			throws FileEncryptionException, InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidAlgorithmParameterException, NoSuchProviderException,
			IllegalBlockSizeException, BadPaddingException, IOException,
			RandomDataGenerationException {
		return create(shareKeyVersion, shareKey, new File(file));
	}

	/**
	 * Method opens a {@link AESGCMRandomAccessFile}-instance with the given
	 * arguments for an already existing file. For creating new files, see
	 * {@link #create(int, SecretKey, File)}. NOTE: After an instance has been
	 * obtained with this method, ist still needs to be initialized with the
	 * corresponding share key from the share metadata DB.
	 * 
	 * @param file
	 *            file to open
	 * @param mode
	 *            access mode
	 * @throws FileEncryptionException
	 * @throws IOException
	 */
	public static AESGCMRandomAccessFile open(File file, boolean writable)
			throws FileEncryptionException, IOException {
		AESGCMRandomAccessFile ret = AESGCMRandomAccessFile.getInstance(file,
				writable);
		ret.open();
		return ret;
	}

	public static AESGCMRandomAccessFile open(String file, boolean writable)
			throws FileEncryptionException, IOException {
		return open(new File(file), writable);
	}

	protected final static WeakHashMap<InstanceEntry, AESGCMRandomAccessFile> instanceMap = new WeakHashMap<InstanceEntry, AESGCMRandomAccessFile>();

	public static synchronized AESGCMRandomAccessFile getInstance(
			File backingFile, boolean writable) throws FileEncryptionException,
			IOException {
		try {
			AESGCMRandomAccessFile ret = instanceMap.get(InstanceEntry
					.instance(backingFile, writable));
			if (ret == null) {
				ret = new AESGCMRandomAccessFile(backingFile);
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

	protected static WeakHashMap<InstanceEntry, AESGCMRandomAccessFile> getInstanceMap() {
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
			AESGCMRandomAccessFile tmp;
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

	// /*
	// * (non-Javadoc)
	// *
	// * @see
	// org.panbox.core.crypto.io.EncRandomAccessFile#renameTo(java.io.File)
	// */
	// public static boolean renameTo(File fold, File fnew) {
	// if (fold.renameTo(fnew)) {
	// AESGCMRandomAccessFile tmp;
	// boolean btmp = false;
	// int i = 2; // 2 iterations..
	// // try {
	// // // all previous instances pointing to f are obsolete by now
	// // do {
	// // // close all previous instances pointing to f, which are
	// // // obsolete by now
	// // tmp = instanceMap
	// // .remove(InstanceEntry.instance(fnew, btmp));
	// // if ((tmp != null) && (tmp.isOpen())) {
	// // tmp.close();
	// // }
	// // i--;
	// // btmp = true;
	// // } while (i > 0);
	// // } catch (IOException e) {
	// // log.error("renameTo: unable to close instance on File \""
	// // + fold.getAbsolutePath() + "\": " + e.getMessage());
	// // }
	//
	// do {
	// tmp = instanceMap.remove(InstanceEntry.instance(fold, btmp));
	// if (tmp != null) {
	// tmp.backingFile = fnew;
	// instanceMap.put(InstanceEntry.instance(fnew, btmp), tmp);
	// }
	// i--;
	// btmp = true;
	// } while (i > 0);
	//
	// return true;
	// } else {
	// return false;
	// }
	// }

	@Override
	public synchronized void close() throws IOException {
		super.close();
		// don't forget to remove instance - otherwise this may create a memory
		// leak for large numbers of files
		instanceMap.remove(InstanceEntry.instance(backingFile, writable));
	}

}
