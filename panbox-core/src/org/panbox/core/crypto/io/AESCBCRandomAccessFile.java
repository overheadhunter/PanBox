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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Set;
import java.util.WeakHashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;

import org.apache.log4j.Logger;
import org.panbox.core.crypto.KeyConstants;
import org.panbox.core.crypto.randomness.SecureRandomWrapper;
import org.panbox.core.exception.FileEncryptionException;
import org.panbox.core.exception.FileIntegrityException;
import org.panbox.core.exception.RandomDataGenerationException;

/**
 * @author palige
 * 
 *         AES CBC/CFB based implementation of {@link EncRandomAccessFile}
 *         without any additional integrity protection mechanisms. May be used
 *         with SunJCE or Bouncycastle Providers.
 */
public class AESCBCRandomAccessFile extends EncRandomAccessFile {

	private static final Logger log = Logger.getLogger("org.panbox.core");

	private AESCBCRandomAccessFile(File backingFile)
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

	public final static String CIPHER_CHUNK = "AES/CBC/NoPadding";
	// 1-byte CFB for the last chunk
	public final static String CIPHER_LASTCHUNK = "AES/CFB8/NoPadding";
	private Cipher lastChunkCipher;

	protected void initParams() {
		BLOCK_LENGTH = encCipher.getBlockSize();
		CHUNK_IV_SIZE = BLOCK_LENGTH;
		CHUNK_DATA_SIZE = CHUNK_MULTIPLE * BLOCK_LENGTH;
		CHUNK_SIZE = CHUNK_DATA_SIZE + CHUNK_IV_SIZE;
		CHUNK_TLEN = 0;
		CHUNK_ENC_DATA_SIZE = CHUNK_DATA_SIZE + CHUNK_TLEN;
		CHUNK_ENC_SIZE = CHUNK_ENC_DATA_SIZE + CHUNK_IV_SIZE;
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

		this.encCipher = Cipher.getInstance(CIPHER_CHUNK, getCryptoProvider());
		this.decCipher = Cipher.getInstance(CIPHER_CHUNK, getCryptoProvider());
		this.lastChunkCipher = Cipher.getInstance(CIPHER_LASTCHUNK,
				getCryptoProvider());
		this.srWrapper = SecureRandomWrapper.getInstance();
	}

	/**
	 * Helper method for reading and decrypting a single chunk within the file
	 * 
	 * @param buffer
	 * @param index
	 * @throws InvalidAlgorithmParameterException
	 * @throws InvalidKeyException
	 * @throws FileEncryptionException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws IOException
	 * @throws ShortBufferException
	 */
	protected byte[] _readChunk(long index) throws IOException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			FileEncryptionException, IllegalBlockSizeException,
			BadPaddingException {
		// first, get chunk iv for decryption
		long oldpos = backingRandomAccessFile.getFilePointer();
		backingRandomAccessFile.seek(chunkOffset(index));
		// System.err.println("_readChunk(" + index + ")");

		// read iv
		byte[] iv = new byte[CHUNK_IV_SIZE];
		int ret = backingRandomAccessFile.read(iv);
		if (ret != CHUNK_IV_SIZE) {
			throw new FileEncryptionException("Size mismatch reading chunk IV!");
		}

		IvParameterSpec spec = new IvParameterSpec(iv);
		decCipher.init(Cipher.DECRYPT_MODE, getFileKey(), spec);

		byte[] res;
		byte[] buf = new byte[CHUNK_ENC_DATA_SIZE];

		ret = backingRandomAccessFile.read(buf);
		backingRandomAccessFile.seek(oldpos);

		if (ret != CHUNK_ENC_DATA_SIZE) {
			throw new FileEncryptionException(
					"Size mismatch reading encrypted chunk data!");
		}

		// decrypt data
		res = decCipher.doFinal(buf);
		if ((res == null) || (res.length != CHUNK_DATA_SIZE)) {
			throw new FileEncryptionException(
					"Decryption error or chunk size mismatch during decryption!");
		} else {
			return res;
		}
	}

	/**
	 * Helper method for reading and decrypting the last chunk of the file
	 * 
	 * @param buffer
	 * @param index
	 * @throws IOException
	 * @throws InvalidAlgorithmParameterException
	 * @throws InvalidKeyException
	 * @throws FileEncryptionException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws ShortBufferException
	 */
	protected byte[] _readLastChunk(long index) throws IOException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException,
			FileEncryptionException, ShortBufferException {

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
		// System.err.println("_readLastChunk(" + index + ")");

		// read iv
		byte[] iv = new byte[CHUNK_IV_SIZE];
		int ret = backingRandomAccessFile.read(iv);
		if (ret != CHUNK_IV_SIZE) {
			throw new FileEncryptionException("Size mismatch reading chunk IV!");
		}

		// initialize cipher with corresponding chunk IV
		IvParameterSpec spec = new IvParameterSpec(iv);
		lastChunkCipher.init(Cipher.DECRYPT_MODE, getFileKey(), spec);

		// adjust number of remaining bytes for reading encrypted data
		nRemaining -= CHUNK_IV_SIZE;

		byte[] buf = new byte[(int) nRemaining];
		byte[] res;
		ret = backingRandomAccessFile.read(buf);
		backingRandomAccessFile.seek(oldpos);

		if (ret != nRemaining) {
			throw new FileEncryptionException(
					"Size mismatch reading encrypted chunk data!");
		}

		// decrypt data
		res = lastChunkCipher.doFinal(buf);
		if ((res == null) || (res.length != (nRemaining - CHUNK_TLEN))) {
			throw new FileEncryptionException(
					"Decryption error or chunk size mismatch during decryption!");
		} else {
			return res;
		}

	}

	/**
	 * Helper method for encrypting and writing a chunk within the file
	 * 
	 * @param buffer
	 * @param index
	 * @throws RandomDataGenerationException
	 * @throws IOException
	 * @throws FileEncryptionException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidAlgorithmParameterException
	 * @throws InvalidKeyException
	 * @throws ShortBufferException
	 * @throws Exception
	 */
	protected void _writeChunk(byte[] buffer, long index)
			throws FileEncryptionException, RandomDataGenerationException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException, IOException {
		// initialize cipher with corresponding chunk IV
		byte[] iv = generateRandomChunkIV();
		IvParameterSpec spec = new IvParameterSpec(iv);
		encCipher.init(Cipher.ENCRYPT_MODE, getFileKey(), spec);

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
		// System.err.println("_writeChunk(" + index + ")");
		// first write chunk iv
		backingRandomAccessFile.write(iv);
		// next, write encrypted data
		backingRandomAccessFile.write(encChunk);
		// return to initial offset
		backingRandomAccessFile.seek(oldpos);
	}

	/**
	 * Helper method for encrypting and writing the last chunk of the file
	 * 
	 * @param buffer
	 * @param index
	 * @throws RandomDataGenerationException
	 * @throws FileEncryptionException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidAlgorithmParameterException
	 * @throws InvalidKeyException
	 * @throws IOException
	 * @throws ShortBufferException
	 */
	protected void _writeLastChunk(byte[] buffer, long index)
			throws FileEncryptionException, RandomDataGenerationException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException, IOException {
		// initialize cipher with corresponding chunk IV
		byte[] iv = generateRandomChunkIV();
		IvParameterSpec spec = new IvParameterSpec(iv);

		lastChunkCipher.init(Cipher.ENCRYPT_MODE, getFileKey(), spec);

		byte[] encChunk = lastChunkCipher.doFinal(buffer);

		// length of plaintext should match ciphertext minus length of the
		// authentication tag
		if ((encChunk == null)
				|| (encChunk.length != (buffer.length + CHUNK_TLEN))) {
			throw new FileEncryptionException(
					"Encrypted chunk length mismatch!");
		}

		long oldpos = backingRandomAccessFile.getFilePointer();

		backingRandomAccessFile.seek(chunkOffset(index));
		// System.err.println("_writeLastChunk(" + index + ")");

		// first write iv
		backingRandomAccessFile.write(iv);
		// then write encrypted data
		backingRandomAccessFile.write(encChunk);
		// then return to initial position
		backingRandomAccessFile.seek(oldpos);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.panbox.core.crypto.io.EncRandomAccessFile#readFileAuthenticationTag()
	 */
	@Override
	protected byte[] readFileAuthenticationTag() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.panbox.core.crypto.io.EncRandomAccessFile#writeFileAuthenticationTag
	 * (byte[])
	 */
	@Override
	protected void writeFileAuthenticationTag(byte[] rootAuthTag) {
		// TODO Auto-generated method stub
	}

	/**
	 * TEMPORARY static helper method which translates an encrypted files real
	 * file size to its corresponding virtual file size
	 * 
	 * @param reaFileSize
	 * @return
	 */
	public static long realToVirtualFileSize(long reaFileSize) {
		int csize = CHUNK_MULTIPLE * 16 + 16; // chunk size including iv without
												// auth tag
		long nchunks = ((reaFileSize % csize) == 0) ? (reaFileSize / csize)
				: ((reaFileSize / csize) + 1);
		return (reaFileSize > 78) ? reaFileSize - (nchunks * 16) - 78 : 0;
	}

	@Override
	final boolean implementsAuthentication() {
		return false;
	}

	@Override
	protected String getAlgorithmIdentifier() {
		return "AES";
	}

	@Override
	boolean implementsCaching() {
		return true;
	}

	public static AESCBCRandomAccessFile create(int shareKeyVersion,
			SecretKey shareKey, File file) throws FileEncryptionException,
			IOException {
		// create instance to return
		AESCBCRandomAccessFile ret;
		ret = AESCBCRandomAccessFile.getInstance(file, true);
		ret.create(shareKeyVersion, shareKey);
		return ret;
	}

	public static AESCBCRandomAccessFile create(int shareKeyVersion,
			SecretKey shareKey, String file) throws FileEncryptionException,
			IOException {
		return create(shareKeyVersion, shareKey, new File(file));
	}

	public static AESCBCRandomAccessFile open(File file, boolean writable)
			throws FileEncryptionException, IOException {
		AESCBCRandomAccessFile ret;
		ret = (AESCBCRandomAccessFile) getInstance(file, writable);
		ret.open();
		return ret;
	}

	public static AESCBCRandomAccessFile open(String file, boolean writable)
			throws FileEncryptionException, IOException {
		return open(new File(file), writable);
	}

	@Override
	protected void readMetadata() throws FileEncryptionException, IOException,
			InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, NoSuchAlgorithmException,
			NoSuchPaddingException, NoSuchProviderException,
			FileIntegrityException {
		this.fHeader.readAndVerify();
	}

	@Override
	protected void flushAuthData() {
		// do nothing for now
	}

	@Override
	public boolean checkFileAuthenticationTag() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void open() throws IOException, FileEncryptionException {
		// check if file exists - if mode is rw, RandomAccessFile would
		// attempt to auto-create
		if (!exists()) {
			throw new FileNotFoundException("File " + getAbsolutePath()
					+ " not found!");
		} else if (isOpen()) {
			throw new IOException("File has already been opened!");
		} else {
			try {
				this.backingRandomAccessFile = new RandomAccessFile(
						backingFile, this.writable ? "rw" : "r");

				// init with new cache
				this.cache = this.implementsCaching() ? new ChunkCache() : null;

				// file header
				this.fHeader = new FileHeader();

				// init header structure
				this.fHeader.readDontVerify();
				// goto first chunk
				seek(0);
				this.setOpen(true);
			} catch (NoSuchAlgorithmException | NoSuchPaddingException
					| NoSuchProviderException e) {
				throw new FileEncryptionException("Error opening file!", e);
			}
		}
	}

	@Override
	public void create(int shareKeyVersion, SecretKey shareKey)
			throws FileEncryptionException, IOException {

		if (!writable) {
			throw new IOException("Create call on readonly instance!");
		} else {
			// create new file
			if (exists() || backingFile.createNewFile()) {
				this.backingRandomAccessFile = new RandomAccessFile(
						backingFile, "rw");
				this.writable = true;
				// if file length != 0, throw exception
				if (realLength() > 0) {
					throw new FileEncryptionException("Non-empty file "
							+ getAbsolutePath() + " already exists!");
				} else {
					try {
						this.shareKey = shareKey;
						// initialize new file with header information

						this.fHeader = new FileHeader();

						// init with new cache
						this.cache = this.implementsCaching() ? new ChunkCache()
								: null;

						// initialize header with new random file key
						KeyGenerator kgen = KeyGenerator.getInstance(
								getAlgorithmIdentifier(), getCryptoProvider());
						kgen.init(KeyConstants.SYMMETRIC_FILE_KEY_SIZE);
						SecretKey fileKey = kgen.generateKey();

						this.fHeader.setDecryptedFileKey(fileKey);
						this.fHeader.setShareKeyVersion(shareKeyVersion);
						// data have been set, write initial file header
						this.fHeader.write();
						// seek() to virtual pos 0 (i.e. realpos +
						// sizeof(header))
						this.seek(0);
						this.setOpen(true);
						this.setInitialized(true);
					} catch (NoSuchAlgorithmException | NoSuchPaddingException
							| NoSuchProviderException | InvalidKeyException
							| IllegalBlockSizeException | BadPaddingException e) {
						throw new FileEncryptionException(
								"Error creating file header!", e);
					}
				}

			} else {
				throw new FileEncryptionException("Could not create file "
						+ getAbsolutePath());
			}
		}
	}

	protected final static WeakHashMap<InstanceEntry, AESCBCRandomAccessFile> instanceMap = new WeakHashMap<InstanceEntry, AESCBCRandomAccessFile>();

	public static synchronized AESCBCRandomAccessFile getInstance(
			File backingFile, boolean writable) throws FileEncryptionException,
			IOException {

		try {
			AESCBCRandomAccessFile ret = instanceMap.get(InstanceEntry
					.instance(backingFile, writable));
			if (ret == null) {
				ret = new AESCBCRandomAccessFile(backingFile);
				ret.writable = writable;
				instanceMap.put(InstanceEntry.instance(backingFile, writable),
						ret);
			}
			return ret;
		} catch (RandomDataGenerationException | InvalidKeyException
				| NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidAlgorithmParameterException | NoSuchProviderException e) {
			throw new FileEncryptionException("Error creating instance!", e);
		}
	}

	protected static WeakHashMap<InstanceEntry, AESCBCRandomAccessFile> getInstanceMap() {
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
			AESCBCRandomAccessFile tmp;
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
		// don't forget to remove instance - otherwise this may create a memory
		// leak for large numbers of files
		instanceMap.remove(InstanceEntry.instance(backingFile, writable));
	}

}
