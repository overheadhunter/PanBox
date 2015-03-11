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

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;

import org.panbox.core.crypto.KeyConstants;
import org.panbox.core.crypto.randomness.SecureRandomWrapper;
import org.panbox.core.exception.FileEncryptionException;
import org.panbox.core.exception.FileIntegrityException;
import org.panbox.core.exception.RandomDataGenerationException;

/**
 * @author palige
 * 
 *         Abstract superclass for AES GCM based implementations of
 *         {@link EncRandomAccessFile} with additional support for integrity
 *         protection.
 */

public abstract class AbstractAESGCMRandomAccessFile extends
		EncRandomAccessFile {

	// private static final Logger log = Logger.getLogger("org.panbox.core");

	protected AbstractAESGCMRandomAccessFile(File backingFile)
			throws InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidAlgorithmParameterException,
			NoSuchProviderException, RandomDataGenerationException, IOException {
		super(backingFile);
	}

	public final static String CIPHER_CHUNK = "AES/GCM/NoPadding";

	/**
	 * length of GCM authentication tag (in bits) as recommended by BSI/NIST in
	 * their respective guidelines
	 */
	public final static int GCM_AUTHENTICATION_TAG_LEN = 96;

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

		// this.encCipher = Cipher.getInstance(CIPHER_CHUNK, "BC");
		// this.decCipher = Cipher.getInstance(CIPHER_CHUNK, "BC");
		this.srWrapper = SecureRandomWrapper.getInstance();
	}

	protected void initParams() {
		BLOCK_LENGTH = getBlockSize();
		CHUNK_IV_SIZE = BLOCK_LENGTH;
		CHUNK_DATA_SIZE = CHUNK_MULTIPLE * BLOCK_LENGTH;
		CHUNK_SIZE = CHUNK_DATA_SIZE + CHUNK_IV_SIZE;
		CHUNK_TLEN = GCM_AUTHENTICATION_TAG_LEN / Byte.SIZE;
		CHUNK_ENC_DATA_SIZE = CHUNK_DATA_SIZE + CHUNK_TLEN;
		CHUNK_ENC_SIZE = CHUNK_ENC_DATA_SIZE + CHUNK_IV_SIZE;
	}

	abstract int getBlockSize();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.panbox.core.crypto.io.EncRandomAccessFile#readFileAuthenticationTag()
	 */
	@Override
	protected synchronized byte[] readFileAuthenticationTag() {
		return fHeader.getFileAuthTag();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.panbox.core.crypto.io.EncRandomAccessFile#writeFileAuthenticationTag
	 * (byte[])
	 */
	@Override
	protected void writeFileAuthenticationTag(byte[] rootAuthTag)
			throws InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, FileEncryptionException, IOException {
		fHeader.setFileAuthTag(rootAuthTag);
		fHeader.write();
	}

	/**
	 * Helper method for reading and decrypting a single chunk within the file
	 * 
	 * @param buffer
	 * @param index
	 * @throws FileIntegrityException
	 */
	protected abstract byte[] _readChunk(long index) throws IOException,
			FileEncryptionException, FileIntegrityException;

	/**
	 * Helper method for reading and decrypting the last chunk of the file
	 * 
	 * @param buffer
	 * @param index
	 * @throws IOException
	 * @throws InvalidAlgorithmParameterException
	 * @throws InvalidKeyException
	 * @throws FileEncryptionException
	 * @throws FileIntegrityException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws ShortBufferException
	 */
	protected abstract byte[] _readLastChunk(long index) throws IOException,
			FileEncryptionException, FileIntegrityException;

	/**
	 * indicates if currently data is only held in the cache but has not been
	 * written to disk yet
	 * 
	 * @return <code>true</code> if data only exists wihin the cache,
	 *         <code>false</code> otherwise
	 */
	protected boolean onlyCachedData() {
		return (this.cache.needsToBeWritten && !isAuthTagInitialized());
	}

	/**
	 * @return <code>true</code>, if the file authentication tag has already
	 *         been set, <code>false</code> otherwise
	 */
	protected boolean isAuthTagInitialized() {
		return (readFileAuthenticationTag() != null);
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
	protected abstract void _writeChunk(byte[] buffer, long index)
			throws FileEncryptionException, RandomDataGenerationException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException, IOException;

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
	protected abstract void _writeLastChunk(byte[] buffer, long index)
			throws FileEncryptionException, RandomDataGenerationException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException, IOException;

	@Override
	boolean implementsAuthentication() {
		return true;
	}

	@Override
	String getAlgorithmIdentifier() {
		return "AES";
	}

	@Override
	boolean implementsCaching() {
		return true;
	}

	@Override
	protected void readMetadata() throws FileIntegrityException, IOException,
			InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, NoSuchAlgorithmException,
			NoSuchPaddingException, NoSuchProviderException,
			FileEncryptionException {
		AuthTagVerifier instanceVerifier = new AuthTagVerifier(this);
		fHeader.readAndVerify();
		initVerifier(instanceVerifier);

		if ((numchunks() > 0) && (!instanceVerifier.verifyFileAuthTag())) {
			throw new FileIntegrityException(
					"File authentication tag verification failed!");
		} else {
			setAuthTagVerifier(instanceVerifier);
		}
	}

	/**
	 * helper method which reads the single chunk authentication tags within
	 * this file and stores the corresponding values within the given
	 * {@link AuthTagVerifier}-instance
	 * 
	 * @param verifier
	 *            {@link AuthTagVerifier}-instance for storing chunk
	 *            authentication tags
	 * @throws FileEncryptionException
	 * @throws IOException
	 */
	private void initVerifier(AuthTagVerifier verifier)
			throws FileEncryptionException, IOException {
		// this.authTagVerifier = new AuthTagVerifier(this);
		int hlen = fHeader.headerSize();
		long flen = realLength();
		long oldpos = backingRandomAccessFile.getFilePointer();
		// goto virtual offset 0
		seek(0);
		flen -= hlen;

		// determine number of full chunks

		long nchunks = numchunks();

		// only try parsing auth tags if there already been wri tten any chunks
		if (nchunks != 0) {
			// determine if there is a last *partial* chunk
			boolean partial = (flen % CHUNK_ENC_SIZE) != 0;
			long tmp;
			byte[] tmpATag = new byte[CHUNK_TLEN];

			if (nchunks == 1 && !partial) {
				tmp = hlen + CHUNK_IV_SIZE + CHUNK_DATA_SIZE;
				backingRandomAccessFile.seek(tmp);
				backingRandomAccessFile.read(tmpATag);
				verifier.insertChunkAuthTag(0, tmpATag);
			} else if (nchunks > 1 && !partial) {
				// parse all full chunks
				for (long i = 0; i < nchunks; i++) {
					// read authentication tag for each chunk
					tmp = hlen + CHUNK_ENC_SIZE * i + CHUNK_IV_SIZE
							+ CHUNK_DATA_SIZE;
					backingRandomAccessFile.seek(tmp);
					backingRandomAccessFile.read(tmpATag);
					verifier.insertChunkAuthTag((int) i, tmpATag);
				}
			} else {
				// parse all full chunks
				for (long i = 0; i < (nchunks - 1); i++) {
					// read authentication tag for each chunk
					tmp = hlen + CHUNK_ENC_SIZE * i + CHUNK_IV_SIZE
							+ CHUNK_DATA_SIZE;
					backingRandomAccessFile.seek(tmp);
					backingRandomAccessFile.read(tmpATag);
					verifier.insertChunkAuthTag((int) i, tmpATag);
				}

				// parse last chunk
				backingRandomAccessFile.seek(realLength() - CHUNK_TLEN);
				backingRandomAccessFile.read(tmpATag);
				verifier.insertChunkAuthTag((int) (nchunks - 1), tmpATag);
			}
		}

		backingRandomAccessFile.seek(oldpos);
	}

	@Override
	protected synchronized void flushAuthData() throws FileEncryptionException,
			IOException {
		try {
			if (getAuthTagVerifier().needsUpdate()) {
				getAuthTagVerifier().updateFileAuthTag();
			}
		} catch (InvalidKeyException | IllegalBlockSizeException
				| BadPaddingException e) {
			throw new FileEncryptionException("Error flushing auth data!", e);
		}
	}

	@Override
	public boolean checkFileAuthenticationTag() throws FileEncryptionException,
			IOException {
		try {
			// if file is opened for writing, first flush all cached chunk as
			// well as auth tag tree data
			if (writable) {
				flush(true);
			}

			// now build a new, temporary auth tag tree to check if there has
			// been any arbitrary chunk auth tag modification
			AuthTagVerifier tmpVerifier = new AuthTagVerifier(this);
			initVerifier(tmpVerifier);
			return tmpVerifier.verifyFileAuthTag();
		} catch (InvalidKeyException | InvalidAlgorithmParameterException
				| IllegalBlockSizeException | BadPaddingException
				| RandomDataGenerationException e) {
			throw new FileEncryptionException(
					"Error checking file authentication tag!", e);
		}
	}

	/**
	 * TEMPORARY static helper method which translates an encrypted files real
	 * file size to its corresponding virtual file size
	 * 
	 * @param reaFileSize
	 * @return
	 */
	public static long realToVirtualFileSize(long reaFileSize) {
		int csize = CHUNK_MULTIPLE * 16 + 28; // chunk size including iv
												// including
												// auth tag
		long chunkpart = reaFileSize - 110;
		// long nchunks = ((reaFileSize % csize) == 0) ? (reaFileSize / csize)
		// : ((reaFileSize / csize) + 1);
		long nchunks = ((chunkpart % csize) == 0) ? (chunkpart / csize)
				: ((chunkpart / csize) + 1);
		return (reaFileSize > 110) ? chunkpart - (nchunks * 28) : 0;
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
				// first create instance to return
				this.backingRandomAccessFile = new RandomAccessFile(
						backingFile, this.writable ? "rw" : "r");

				// init with new cache
				this.cache = this.implementsCaching() ? new ChunkCache() : null;

				// file header

				this.fHeader = this.new FileHeader();

				// read share key version from header (without verifying it)
				this.fHeader.readDontVerify();
				// goto virtual offset 0
				this.seek(0);
				this.setOpen(true);
			} catch (NoSuchAlgorithmException | NoSuchPaddingException
					| NoSuchProviderException e) {
				throw new FileEncryptionException(
						"Error creating file header!", e);
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
			if (backingFile.exists() || backingFile.createNewFile()) {
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
						this.setAuthTagVerifier(new AuthTagVerifier(this));
						// initialize new file with header information
						this.fHeader = this.new FileHeader();

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
					} catch (InvalidKeyException | IllegalBlockSizeException
							| BadPaddingException | NoSuchAlgorithmException
							| NoSuchPaddingException | NoSuchProviderException e) {
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
}
