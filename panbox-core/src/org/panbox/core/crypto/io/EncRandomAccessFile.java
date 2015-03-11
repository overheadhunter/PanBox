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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.panbox.PanboxConstants;
import org.panbox.core.crypto.KeyConstants;
import org.panbox.core.crypto.randomness.SecureRandomWrapper;
import org.panbox.core.exception.FileEncryptionException;
import org.panbox.core.exception.FileIntegrityException;
import org.panbox.core.exception.RandomDataGenerationException;

/**
 * @author palige
 * 
 *         Class offers transparent encryption/decryption for an encapsulated
 *         {@link RandomAccessFile}-instance. Inheritance is intentionally
 *         omitted in order to <br>
 *         1) omit exposure of any methods which may allow inadvertant direct
 *         access to the ciphertext and <br>
 *         2) allow for custom exceptions <br>
 *         This abstract class handles offset calculation, crypto metadata
 *         management and offers multiple convenience methods replicating the
 *         mode of operation of regular {@link RandomAccessFile}- and
 *         {@link File}-instances. However, the actual work of encrypting and
 *         decrypting the file contents is to be handled by the algorithm
 *         specific implementations of this abstract class.
 */

public abstract class EncRandomAccessFile implements Flushable, Closeable {

	static {
		// add bouncycastle as default crypto provider
		Security.addProvider(new BouncyCastleProvider());
	}

	private static final Logger log = Logger.getLogger("org.panbox.core");

	/**
	 * indicates if the underlying {@link RandomAccessFile} has been opened with
	 * write access
	 */
	protected boolean writable;

	private boolean open;

	/**
	 * @return <code>true</code>, if this file currently is opened,
	 *         <code>false</code> otherwise
	 */
	public synchronized boolean isOpen() {
		return open;
	}

	/**
	 * set open parameter
	 */
	protected synchronized void setOpen(boolean open) {
		this.open = open;
	}

	/**
	 * stores the {@link RandomAccessFile}-instance being used for reading
	 * writing the actual encrypted data
	 */
	protected RandomAccessFile backingRandomAccessFile;

	/**
	 * stores a {@link File}-instance of the backend-file
	 */
	protected File backingFile;

	/**
	 * header access layer for this {@link EncRandomAccessFile}-instance
	 */
	protected FileHeader fHeader;

	/**
	 * indicates if an implementation of this abstract class implements
	 * integrity checking (or an AE mode)
	 */
	abstract boolean implementsAuthentication();

	/**
	 * indicates if an implementation of this class uses the read()/write()
	 * caches
	 */
	abstract boolean implementsCaching();

	/**
	 * stores the base multiple for chunk size calculation. One chunk comprises
	 * the given number of blocks
	 */
	protected static final int CHUNK_MULTIPLE = 4096; // 1024;
	protected int CHUNK_IV_SIZE;
	protected int CHUNK_DATA_SIZE;

	public int getVirtualChunkSize() {
		return CHUNK_DATA_SIZE;
	}

	protected int CHUNK_SIZE;

	/**
	 * corresponding length values of encrypted chunks (GCM needs additional
	 * space for authentication metadata)
	 */
	protected int CHUNK_ENC_DATA_SIZE;
	protected int CHUNK_ENC_SIZE;

	// public String CIPHER_CHUNK;

	protected int CHUNK_TLEN;

	/**
	 * stores the {@link SecretKey}-instance to be used for de-/encryption of
	 * the file key
	 */
	protected SecretKey shareKey;

	/**
	 * to be set by implementations
	 */
	public int BLOCK_LENGTH;

	/**
	 * @return name of algorithm used by implementations
	 */
	abstract String getAlgorithmIdentifier();

	/**
	 * @return the crypto provider an implementation uses
	 */
	abstract String getCryptoProvider();

	final int SHARE_KEY_SIZE = KeyConstants.SYMMETRIC_KEY_SIZE_BYTES;

	/**
	 * method sets this instances share key and completes initialization
	 * 
	 * @param shareKey
	 *            the share key for this file
	 * @throws IOException
	 * @throws FileIntegrityException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidKeyException
	 * @throws NoSuchProviderException
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 */
	public synchronized void initWithShareKey(SecretKey shareKey)
			throws FileEncryptionException, IOException, FileIntegrityException {
		if (!isOpen()) {
			throw new IOException("File has not been opened!");
		} else {
			if (shareKey == null) {
				throw new FileEncryptionException("Secret key is null!");
			} else if (!isInitialized()) {
				this.shareKey = shareKey;
				try {
					readMetadata();
					setInitialized(true);
				} catch (InvalidKeyException | IllegalBlockSizeException
						| BadPaddingException | NoSuchAlgorithmException
						| NoSuchPaddingException | NoSuchProviderException e) {
					this.shareKey = null;
					throw new FileEncryptionException(
							"Error reading metadata!", e);
				}
			} else {
				log.debug("initWithShareKey(): instance has already been initialized with share key!");
			}
		}
	}

	/**
	 * indicates if {@link #initWithShareKey(SecretKey)} has been called for
	 * this instance
	 */
	private boolean initialized = false;

	public boolean isInitialized() {
		return initialized;
	}

	protected void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}

	/**
	 * stores {@link Cipher}-instance for decryption
	 */
	protected Cipher decCipher;

	/**
	 * stores {@link Cipher}-instance for encryption
	 */
	protected Cipher encCipher;

	/**
	 * cache for temporary storage of chunks
	 */
	protected ChunkCache cache;

	/**
	 * stores an instance of the management class for verification of
	 * authentication tag integrity
	 */
	private AuthTagVerifier authTagVerifier;

	protected AuthTagVerifier getAuthTagVerifier() {
		return authTagVerifier;
	}

	protected void setAuthTagVerifier(AuthTagVerifier authTagVerifier)
			throws FileEncryptionException {
		if (this.authTagVerifier == null) {
			this.authTagVerifier = authTagVerifier;
		} else {
			throw new FileEncryptionException(
					"instance authetication tag verifier has already been set!");
		}
	}

	/**
	 * base constructor, instantiation work for actual backend files is handled
	 * by {@link #create(int, SecretKey)} and {@link #open()} methods 
	 * 
	 * @throws RandomDataGenerationException
	 * @throws NoSuchProviderException
	 * @throws InvalidAlgorithmParameterException
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 * @throws IOException
	 * 
	 */
	protected EncRandomAccessFile(File backingFile) throws InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidAlgorithmParameterException, NoSuchProviderException,
			RandomDataGenerationException, IOException {
		if (backingFile.isDirectory())
			throw new IOException("Instantiation failed. " + backingFile
					+ " is a directory!");
		initCiphers();
		initParams();
		this.backingFile = backingFile;
	}

	/**
	 * initialization of implementation specific parameters
	 */
	abstract void initParams();

	/**
	 * Method reads crypto meta data from the file header and should further, if
	 * the implementation supports integrity protection, perform initialization
	 * of the authentication tag tree structure for later checks of
	 * authentication tag integrity.
	 * 
	 * @throws FileEncryptionException
	 * @throws IOException
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws NoSuchProviderException
	 * @throws FileIntegrityException
	 */
	abstract protected void readMetadata() throws FileEncryptionException,
			IOException, InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, NoSuchAlgorithmException,
			NoSuchPaddingException, NoSuchProviderException,
			FileIntegrityException;

	/**
	 * Method handles share key updates by creating a new pseudo random file
	 * encryption key for this file, re-encrypting the files contents,then
	 * re-encrypting new file encryption key with the given share key and
	 * storing it within the file header alongside its corresponding version
	 * number.
	 * 
	 * @param shareKeyVersion
	 *            version number of the new share key
	 * @param shareKey
	 *            the new share key
	 */
	public synchronized void reencryptFile(int shareKeyVersion,
			SecretKey shareKey) {
		// TODO: implementation
	}

	/**
	 * retrieves the root authentication tag over all single chunk
	 * authentication tags stored within the file
	 * 
	 * @return the root authentication tag stored within the header of this
	 *         file, if integrity checking is implemented, <code>null</code>
	 *         otherwise
	 */
	abstract protected byte[] readFileAuthenticationTag();

	/**
	 * writes the root authentication tag over all single chunk authentication
	 * tags to the header
	 * 
	 * @param rootAuthTag
	 */
	abstract protected void writeFileAuthenticationTag(byte[] rootAuthTag)
			throws InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, FileEncryptionException, IOException;

	/**
	 * Method indicates if chunk authentication tags currently stored on disk
	 * are valid w.r.t. the central file authentication tag being stored in the
	 * file header. Prior to the verification, any chunk data currently being
	 * stored in the cache will be written to disk and the auth tag tree will be
	 * re-built.
	 * 
	 * @return <code>true</code>, if file chunk authentication tags are valid,
	 *         <code>false</code> otherwise
	 * @throws IOException
	 * @throws RandomDataGenerationException
	 * @throws FileEncryptionException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidAlgorithmParameterException
	 * @throws InvalidKeyException
	 */
	abstract public boolean checkFileAuthenticationTag()
			throws FileEncryptionException, IOException;

	/**
	 * initialize ciphers
	 * 
	 * @param key
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 * @throws RandomDataGenerationException
	 * @throws InvalidAlgorithmParameterException
	 * @throws NoSuchProviderException
	 */
	abstract protected void initCiphers() throws NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidKeyException,
			RandomDataGenerationException, InvalidAlgorithmParameterException,
			NoSuchProviderException;

	/**
	 * {@link SecureRandomWrapper} instance for IV generation
	 */
	protected SecureRandomWrapper srWrapper;

	/**
	 * generates a random initialization vector for chunk encryption
	 * 
	 * @return new random chunk IV of size {@link Cipher#getBlockSize()}
	 * @throws FileEncryptionException
	 * @throws RandomDataGenerationException
	 */
	protected byte[] generateRandomChunkIV() throws FileEncryptionException,
			RandomDataGenerationException {
		byte[] res = new byte[BLOCK_LENGTH];
		srWrapper.nextBytes(res);

		// just to be sure
		if ((res != null) && (res.length == BLOCK_LENGTH)) {
			return res;
		} else {
			// TODO: custom exception hierarchy
			throw new FileEncryptionException("Chunk IV generation failed!");
		}
	}

	/**
	 * Inner class for caching a single chunk for reading and writing,
	 * respectively. NOTE: Cached chunks are stored in plain text until they are
	 * actually to be written to disk. Correspondingly, no
	 * authentication/integrity checking is done when a cached chunk is being
	 * read.
	 */
	protected class ChunkCache {

		// /**
		// * stores the index of the chunk which has been cached before the
		// chunk
		// * being currently cached, or -1 if no chunk had been cached before
		// */
		// protected long previousChunkIdx = -1;

		/**
		 * stores the index of the chunk currently being cached
		 */
		protected long chunkIdx;

		/**
		 * holds the actual chunk
		 */
		protected byte[] chunkBuffer;

		/**
		 * indicates if the chunk currently being held in this cache still needs
		 * to be written to disk (i.e., has been set from within a write* call)
		 */
		protected boolean needsToBeWritten;

		/**
		 * indicates if chunk currently being held in cache is a last chunk
		 */
		protected boolean isLast;

		protected synchronized void setChunkBuffer(long idx, byte[] chunk,
				boolean needsdToBeWritten, boolean isLast) {

			this.chunkIdx = idx;
			// only store a *copy* of this array to avoid modification of its
			// contents due to stale pointers
			this.chunkBuffer = Arrays.copyOf(chunk, chunk.length);
			this.needsToBeWritten = needsdToBeWritten;
			this.isLast = isLast;
			// this.previousChunkIdx = previousChunkIdx;
		}

		protected synchronized byte[] getChunkBuffer(long index) {
			return ((this.chunkIdx == index) && (this.chunkBuffer != null)) ? Arrays
					.copyOf(chunkBuffer, chunkBuffer.length) : null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuffer res = new StringBuffer();
			res.append("ChunkCache:");
			res.append("index=" + this.chunkIdx + ";");
			res.append("length=" + this.chunkBuffer.length + ";");
			res.append("needsToBeWritten=" + this.needsToBeWritten + ";");
			res.append("isLast=" + this.isLast);
			return res.toString();
		}
	}

	/**
	 * convenience class for long to byte[] conversion (and vice-versa)
	 * 
	 * (see <a href=
	 * "https://stackoverflow.com/questions/4485128/how-do-i-convert-long-to-byte-and-back-in-java"
	 * >https://stackoverflow.com/questions/4485128/how-do-i-convert-long-to-
	 * byte-and-back-in-java</a>
	 */
	protected static class LongByteConv {
		private static ByteBuffer buf = ByteBuffer.allocate(Long.SIZE / 8);

		public static byte[] long2Bytes(long x) {
			buf.clear();
			buf.putLong(0, x);
			return buf.array();
		}

		public static long bytes2Long(byte[] bytes) {
			buf.clear();
			buf.put(bytes, 0, bytes.length);
			buf.flip();// need flip
			return buf.getLong();
		}
	}

	/**
	 * inner class for accessing header information stored within this
	 * {@link EncRandomAccessFile}-instance
	 */
	protected class FileHeader {

		/**
		 * we may use AES in ECB mode for file key encryption, as
		 * sizeof(file_key) == sizeof(share_key)
		 */
		private final String CIPHER_FILEKEY = "AES/ECB/NoPadding";
		private Cipher filekeyCipher;

		/**
		 * magic number, 6 bytes
		 */
		final byte[] PANBOX_FILE_MAGIC = PanboxConstants.PANBOX_FILE_MAGIC;

		/**
		 * version field, 4 bytes
		 */
		final byte[] PANBOX_FILE_VERSION = PanboxConstants.PANBOX_VERSION;

		/**
		 * decrypted file key
		 */
		private SecretKey decryptedFileKey;

		/**
		 * stores the version number of the share key
		 */
		private int shareKeyVersion = -1;

		/**
		 * stored file authentication tag
		 */
		private byte[] fileAuthTag;

		/**
		 * stored header authentication tag
		 */
		private byte[] headerAuthTag;

		/**
		 * stores this headers size, currently 6+4+4+32+32 = 78 bytes without
		 * authentication, or 6+4+4+32+32+32 = 110 bytes *with* authentication
		 */
		private final int HEADER_SIZE;

		/**
		 * hard coded auth tag size for SHA256Digest
		 */
		final static int AUTH_TAG_SIZE = 32;

		/**
		 * HMAC for creating the header authentication tag
		 */
		private HMac headerAuthHMac;

		protected final FieldOffsets OffsetTable;

		protected FileHeader() throws NoSuchAlgorithmException,
				NoSuchPaddingException, NoSuchProviderException {
			this.headerAuthHMac = new HMac(new SHA256Digest());
			this.headerAuthTag = new byte[AUTH_TAG_SIZE];
			this.filekeyCipher = Cipher.getInstance(CIPHER_FILEKEY, "BC");

			if (implementsAuthentication()) {
				// include file auth tag
				// this.fileAuthTag = new byte[AuthTagVerifier.AUTH_TAG_SIZE];
				this.HEADER_SIZE = PANBOX_FILE_MAGIC.length
						+ PANBOX_FILE_VERSION.length
						+ AuthTagVerifier.AUTH_TAG_SIZE
						+ FileHeader.AUTH_TAG_SIZE // header auth tag
						+ KeyConstants.SYMMETRIC_FILE_KEY_SIZE_BYTES // encrypted
																		// file
																		// key
						+ (Integer.SIZE / 8); // shareKeyVersion
			} else {
				this.HEADER_SIZE = PANBOX_FILE_MAGIC.length
						+ PANBOX_FILE_VERSION.length + FileHeader.AUTH_TAG_SIZE
						+ KeyConstants.SYMMETRIC_FILE_KEY_SIZE_BYTES // encrypted
																		// file
																		// key
						+ (Integer.SIZE / 8); // shareKeyVersion
			}
			this.OffsetTable = new FieldOffsets();
		}

		/**
		 * byte offsets of header fields
		 */
		class FieldOffsets {
			// offset_n := offset_(n-1) + sizeof(field@offset_(n-1))
			protected final int FIELD_PANBOX_FILE_MAGIC = 0;
			protected final int FIELD_PANBOX_FILE_VERSION = FIELD_PANBOX_FILE_MAGIC
					+ PANBOX_FILE_MAGIC.length;
			protected final int FIELD_SHARE_KEY_VERSION = FIELD_PANBOX_FILE_VERSION
					+ PANBOX_FILE_VERSION.length;
			protected final int FIELD_FILE_KEY = FIELD_SHARE_KEY_VERSION
					+ (Integer.SIZE / 8);
			protected final int FIELD_FILE_AUTH_TAG = FIELD_FILE_KEY
					+ KeyConstants.SYMMETRIC_FILE_KEY_SIZE_BYTES;;
			protected final int FIELD_HEADER_AUTH_TAG = implementsAuthentication() ? FIELD_FILE_AUTH_TAG
					+ AuthTagVerifier.AUTH_TAG_SIZE
					: FIELD_FILE_AUTH_TAG;
		}

		/**
		 * writes header data to the file
		 * 
		 * @throws FileEncryptionException
		 * @throws IOException
		 * @throws InvalidKeyException
		 * @throws BadPaddingException
		 * @throws IllegalBlockSizeException
		 */
		protected synchronized void write() throws FileEncryptionException,
				IOException, InvalidKeyException, IllegalBlockSizeException,
				BadPaddingException {
			// check if all necessary data has been set
			if ((getDecryptedFileKey() == null)
					|| (getDecryptedFileKey().getEncoded().length == 0)) {
				throw new FileEncryptionException(
						"Decrypted file key has not been set!");
			}

			// if (implementsAuthentication()
			// && ((getFileAuthTag() == null) || (getFileAuthTag().length ==
			// 0))) {
			// throw new FileEncryptionException(
			// "File authentication tag has not been set");
			// }

			if (getShareKeyVersion() < 0) {
				throw new FileEncryptionException(
						"Share key version number has not been set!");
			}

			// if all data have been set, initialize HMac with shareKey
			if (shareKey == null || shareKey.getEncoded().length == 0) {
				throw new FileEncryptionException(
						"Invalid share key in encrypting random access file!");
			} else {
				headerAuthHMac.reset();
				KeyParameter keyParameter = new KeyParameter(
						shareKey.getEncoded());
				headerAuthHMac.init(keyParameter);
			}

			// encrypt file key
			byte[] tmpencryptedFileKey = new byte[KeyConstants.SYMMETRIC_FILE_KEY_SIZE_BYTES];
			filekeyCipher.init(Cipher.ENCRYPT_MODE, shareKey);
			byte[] t2 = decryptedFileKey.getEncoded();
			if ((t2 == null)
					|| (t2.length != KeyConstants.SYMMETRIC_FILE_KEY_SIZE_BYTES)) {
				throw new FileEncryptionException(
						"Encoded file key null or invalid length!");
			}
			tmpencryptedFileKey = filekeyCipher.doFinal(t2);

			// create output buffer & write header data
			ByteArrayOutputStream bstream = new ByteArrayOutputStream();
			DataOutputStream ostream = new DataOutputStream(bstream);
			ostream.write(PANBOX_FILE_MAGIC);
			ostream.write(PANBOX_FILE_VERSION);
			ostream.writeInt(shareKeyVersion);
			ostream.write(tmpencryptedFileKey);
			if (implementsAuthentication()) {
				if (getFileAuthTag() == null) {
					// if no chunks have been stored yet, the initial file auth
					// tag will be set to zero
					byte[] emptyAuthTag = new byte[AuthTagVerifier.AUTH_TAG_SIZE];
					Arrays.fill(emptyAuthTag, (byte) 0x00);
					ostream.write(emptyAuthTag);
					// setFileAuthTag(null);
				} else {
					ostream.write(fileAuthTag);
				}
			}
			ostream.close();

			// all data have been written to stream, get array
			byte[] header_data = bstream.toByteArray();

			// calculate hmac
			headerAuthHMac.update(header_data, 0, header_data.length);
			headerAuthHMac.doFinal(headerAuthTag, 0);

			// write data and hmac
			long oldpos = backingRandomAccessFile.getFilePointer();
			backingRandomAccessFile.seek(0);
			backingRandomAccessFile.write(header_data);
			backingRandomAccessFile.write(headerAuthTag);
			backingRandomAccessFile.seek(oldpos);
		}

		/**
		 * reads magic + share key version without verification
		 * 
		 * @throws FileEncryptionException
		 * @throws IOException
		 */
		protected synchronized void readDontVerify() throws IOException,
				FileEncryptionException {
			// check file length
			if (backingRandomAccessFile.length() < headerSize()) {
				throw new FileEncryptionException("Invalid file header");
			}

			long oldpos = backingRandomAccessFile.getFilePointer();
			backingRandomAccessFile.seek(0);
			byte[] header_data = new byte[headerSize()
					- FileHeader.AUTH_TAG_SIZE];
			backingRandomAccessFile.read(header_data);
			backingRandomAccessFile.seek(oldpos);

			DataInputStream istream = new DataInputStream(
					new ByteArrayInputStream(header_data));

			// check magic number
			byte[] tmpmagic = new byte[PANBOX_FILE_MAGIC.length];
			istream.read(tmpmagic);
			if (!Arrays.equals(tmpmagic, PANBOX_FILE_MAGIC)) {
				throw new FileEncryptionException(
						"Invalid magic number in file header");
			}

			// check version field
			byte[] tmpversion = new byte[PANBOX_FILE_VERSION.length];
			istream.read(tmpversion);
			if (!Arrays.equals(tmpversion, PANBOX_FILE_VERSION)) {
				throw new FileEncryptionException(
						"Invalid version in file header. Expected version is "
								+ PANBOX_FILE_VERSION.toString());
			}

			// if we got here, read non-final fields

			// share key version
			this.shareKeyVersion = istream.readInt();

			istream.close();
		}

		/**
		 * reads and verifies all header data from the file
		 * 
		 * @throws InvalidKeyException
		 * @throws IOException
		 * @throws FileEncryptionException
		 * @throws BadPaddingException
		 * @throws IllegalBlockSizeException
		 * @throws FileIntegrityException
		 */
		protected synchronized void readAndVerify() throws InvalidKeyException,
				IOException, FileEncryptionException,
				IllegalBlockSizeException, BadPaddingException,
				FileIntegrityException {
			// check file length
			if (backingRandomAccessFile.length() < headerSize()) {
				throw new FileEncryptionException("Invalid file header");
			}

			// initialize HMac with shareKey
			if (shareKey == null || shareKey.getEncoded().length == 0) {
				throw new FileEncryptionException(
						"Invalid share key in encrypting random access file!");
			} else {
				headerAuthHMac.reset();
				KeyParameter keyParameter = new KeyParameter(
						shareKey.getEncoded());
				headerAuthHMac.init(keyParameter);
			}

			long oldpos = backingRandomAccessFile.getFilePointer();
			backingRandomAccessFile.seek(0);
			byte[] header_data = new byte[headerSize()
					- FileHeader.AUTH_TAG_SIZE];
			backingRandomAccessFile.read(header_data);

			// read stored value of header authentication tag
			backingRandomAccessFile.read(headerAuthTag);
			backingRandomAccessFile.seek(oldpos);

			// update hmac for header auth tag verification
			headerAuthHMac.update(header_data, 0, header_data.length);
			byte[] hmacRef = new byte[AUTH_TAG_SIZE];
			headerAuthHMac.doFinal(hmacRef, 0);

			if (!Arrays.equals(hmacRef, headerAuthTag)) {
				throw new FileIntegrityException(
						"HMac of file header is invalid!");
			} else {
				DataInputStream istream = new DataInputStream(
						new ByteArrayInputStream(header_data));

				// check magic number
				byte[] tmpmagic = new byte[PANBOX_FILE_MAGIC.length];
				istream.read(tmpmagic);
				if (!Arrays.equals(tmpmagic, PANBOX_FILE_MAGIC)) {
					throw new FileEncryptionException(
							"Invalid magic number in file header");
				}

				// check version field
				byte[] tmpversion = new byte[PANBOX_FILE_VERSION.length];
				istream.read(tmpversion);
				if (!Arrays.equals(tmpversion, PANBOX_FILE_VERSION)) {
					throw new FileEncryptionException(
							"Invalid version in file header. Expected version is "
									+ PANBOX_FILE_VERSION.toString());
				}

				// if we got here, read non-final fields

				// share key version
				this.shareKeyVersion = istream.readInt();

				// encrypted file key
				byte[] tmpencryptedFileKey = new byte[KeyConstants.SYMMETRIC_FILE_KEY_SIZE_BYTES];
				istream.read(tmpencryptedFileKey);

				filekeyCipher.init(Cipher.DECRYPT_MODE, shareKey);
				this.decryptedFileKey = new SecretKeySpec(
						filekeyCipher.doFinal(tmpencryptedFileKey),
						getAlgorithmIdentifier());

				// file auth tag
				if (implementsAuthentication()) {
					byte[] tmpFileAuthBuf = new byte[AuthTagVerifier.AUTH_TAG_SIZE];
					istream.read(tmpFileAuthBuf);
					// if an empty file auth tag has been stored (i.e., no
					// chunks have been stored so far), set the field value to
					// null
					byte[] zeroBuf = new byte[AuthTagVerifier.AUTH_TAG_SIZE];
					Arrays.fill(zeroBuf, (byte) 0x00);
					if (Arrays.equals(zeroBuf, tmpFileAuthBuf)) {
						setFileAuthTag(null);
					} else {
						setFileAuthTag(tmpFileAuthBuf);
					}
				}
				istream.close();
			}
		}

		/**
		 * @return the size of this header in bytes
		 */
		protected int headerSize() {
			return this.HEADER_SIZE;
		}

		protected SecretKey getDecryptedFileKey() {
			return decryptedFileKey;
		}

		protected void setDecryptedFileKey(SecretKey decryptedFileKey) {
			this.decryptedFileKey = decryptedFileKey;
		}

		protected int getShareKeyVersion() {
			return shareKeyVersion;
		}

		protected void setShareKeyVersion(int shareKeyVersion) {
			this.shareKeyVersion = shareKeyVersion;
		}

		protected byte[] getFileAuthTag() {
			return fileAuthTag;
		}

		protected void setFileAuthTag(byte[] fileAuthTag) {
			this.fileAuthTag = fileAuthTag;
		}
	}

	/**
	 * basic helper class converting byte to boolean values and vice versa
	 */
	protected static class BooleanByteConv {
		public static boolean byte2bool(byte b) {
			return ((b & 0x01) != 0);
		}

		public static byte[] bool2byte(boolean b) {
			return b ? new byte[] { 1 } : new byte[] { 0 };
		}
	}

	/**
	 * helper class for converting ints to their respective
	 * byte[]-representation and vice versa
	 */
	protected static class IntByteConv {

		public static byte[] int2byte(int arg) {
			byte[] ret = new byte[4];
			ret[0] = (byte) (arg >> 24);
			ret[1] = (byte) (arg >> 16);
			ret[2] = (byte) (arg >> 8);
			ret[3] = (byte) (arg >> 0);
			return ret;
		}

		public static int byte2int(byte[] arg) {
			return (arg[0] << 24) & 0xff000000 | (arg[1] << 16) & 0xff0000
					| (arg[2] << 8) & 0xff00 | (arg[3] << 0) & 0xff;
		}
	}

	protected SecretKey getFileKey() {
		return fHeader.getDecryptedFileKey();
	}

	/**
	 * Method for reading and decrypting a chunk within the file. To be
	 * implemented by the algorithm-specific implementations.
	 * 
	 * @param buffer
	 * @param index
	 * @throws InvalidAlgorithmParameterException
	 * @throws InvalidKeyException
	 * @throws FileEncryptionException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws IOException
	 * @throws FileIntegrityException
	 * @throws ShortBufferException
	 */
	abstract protected byte[] _readChunk(long index) throws IOException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			FileEncryptionException, IllegalBlockSizeException,
			BadPaddingException, FileIntegrityException;

	/**
	 * Method for reading and decrypting the last chunk of the file. To be
	 * implemented by the algorithm-specific implementations.
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
	 * @throws FileIntegrityException
	 */
	abstract protected byte[] _readLastChunk(long index) throws IOException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException,
			FileEncryptionException, ShortBufferException,
			FileIntegrityException;

	/**
	 * Method for encrypting and writing a chunk within the file. To be
	 * implemented by the algorithm-specific implementations.
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
	abstract protected void _writeChunk(byte[] buffer, long index)
			throws FileEncryptionException, RandomDataGenerationException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException, IOException;

	/**
	 * Method for encrypting and writing the last chunk of the file. To be
	 * implemented by the algorithm-specific implementations.
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
	abstract protected void _writeLastChunk(byte[] buffer, long index)
			throws FileEncryptionException, RandomDataGenerationException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException, IOException;

	protected synchronized byte[] readChunk(long index) throws IOException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException,
			FileEncryptionException, ShortBufferException,
			RandomDataGenerationException, FileIntegrityException {
		if (implementsCaching()) {
			// check cache
			byte[] tmpChunk = cache.getChunkBuffer(index);
			if (tmpChunk != null) {
				return tmpChunk;
			} else {
				flush(false);
				tmpChunk = _readChunk(index);
				cache.setChunkBuffer(index, tmpChunk, false, false);
				return tmpChunk;
			}
		} else {
			return _readChunk(index);
		}
	}

	protected synchronized byte[] readLastChunk(long index) throws IOException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException,
			FileEncryptionException, ShortBufferException,
			RandomDataGenerationException, FileIntegrityException {
		if (implementsCaching()) {
			// check cache
			byte[] tmpChunk = cache.getChunkBuffer(index);
			if (tmpChunk != null) {
				return tmpChunk;
			} else {
				flush(false);
				tmpChunk = _readLastChunk(index);
				cache.setChunkBuffer(index, tmpChunk, false, true);
				return tmpChunk;
			}
		} else {
			return _readLastChunk(index);
		}
	}

	protected synchronized void flush(boolean flushauthdata)
			throws InvalidKeyException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException,
			FileEncryptionException, RandomDataGenerationException, IOException {
		if (writable && implementsCaching()) {
			if (cache.needsToBeWritten) {
				if (!cache.isLast) {
					_writeChunk(cache.chunkBuffer, cache.chunkIdx);
				} else {
					_writeLastChunk(cache.chunkBuffer, cache.chunkIdx);
				}
				cache.needsToBeWritten = false;
			}

			if (flushauthdata) {
				flushAuthData();
			}
		}
	}

	@Override
	public synchronized void flush() throws IOException {
		try {
			if (implementsAuthentication()) {
				flush(true);
			} else {
				flush(false);
			}
		} catch (InvalidKeyException | InvalidAlgorithmParameterException
				| IllegalBlockSizeException | BadPaddingException
				| FileEncryptionException | RandomDataGenerationException e) {
			throw new IOException(e.getMessage());
		}
	}

	protected synchronized void writeChunk(byte[] buffer, long index)
			throws FileEncryptionException, RandomDataGenerationException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException, IOException {
		if (implementsCaching()) {
			// if we use caching mode, only write chunk in case it hasn't been
			// written before & if we're about to write a new chunk (with a new
			// index)
			if (index != cache.chunkIdx) {
				flush(false);
			}

			if (buffer.length == 0) {
				log.warn("writeLastChunk(): buffer has length 0!");
				return;
			} else {
				cache.setChunkBuffer(index, buffer, true, false);
				// System.err.println("writeChunk(" + index + ") - cached " +
				// cache);
				adjustBackingFileLength(buffer, index);
			}
		} else {
			if (buffer.length == 0) {
				log.warn("writeLastChunk(): buffer has length 0!");
				return;
			} else {
				_writeChunk(buffer, index);
			}
		}
	}

	protected synchronized void writeLastChunk(byte[] buffer, long index)
			throws FileEncryptionException, RandomDataGenerationException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException, IOException {
		if (implementsCaching()) {
			// if we use caching mode, only write chunk in case it hasn't been
			// written before & if we're about to write a new chunk (with a new
			// index)
			if (index != cache.chunkIdx) {
				flush(false);
			}

			if (buffer.length == 0) {
				log.warn("writeLastChunk(): buffer has length 0!");
				return;
			} else {
				cache.setChunkBuffer(index, buffer, true, true);
				// System.err.println("writeChunk(" + index + ") - cached " +
				// cache);
				adjustBackingFileLength(buffer, index);
			}
		} else {
			if (buffer.length == 0) {
				log.warn("writeLastChunk(): buffer has length 0!");
				return;
			} else {
				_writeLastChunk(buffer, index);
			}
		}

	}

	/**
	 * Helper methods which adjusts the actual length of the backing file for
	 * caching. As with caching, data is only actually written to disk, if a
	 * chunk has been filled completely and a new chunk is being accessed, the
	 * file size has to be extended in advance in order for methods like
	 * seek/skipBytes/... to still be able to work.
	 * 
	 * @param buffer
	 * @param index
	 * @throws IOException
	 */
	private void adjustBackingFileLength(byte[] buffer, long index)
			throws IOException {
		if (buffer == null || buffer.length == 0) {
			throw new IOException("Buffer null or empty!");
		}
		// as data is only written when a chunk has been filled completely,
		// backing file size has to be adapted, so skipBytes etc. still work
		long newsize = fHeader.headerSize() + (index * CHUNK_ENC_SIZE)
				+ CHUNK_IV_SIZE + buffer.length + CHUNK_TLEN;

		if (newsize > realLength()) {
			// only change size if it has been increased
			backingRandomAccessFile.setLength(newsize);
			// System.err.println("writeChunk(" + index + ") - setLen(" +
			// newsize + ")");
		}
	}

	public synchronized int read() throws IOException, FileEncryptionException,
			FileIntegrityException {
		// if we read beyond the EOF, always return -1
		if (getFilePointer() >= length())
			return -1;

		try {
			// check associated chunk index
			int offset = (int) currentchunkoffset();
			byte[] decChunk = null;
			// check if we read last chunk

			if (currentchunkpointer() == lastchunkpointer()) {
				decChunk = readLastChunk(currentchunkpointer());
			} else {
				decChunk = readChunk(currentchunkpointer());
			}

			skipBytes(1);
			if (offset >= decChunk.length) {
				throw new FileEncryptionException(
						"read() offset mismatch in decrypted data!");
			} else {
				// mask resulting byte
				return (int) (decChunk[offset] & 0xff);
			}
		} catch (InvalidKeyException | InvalidAlgorithmParameterException
				| IllegalBlockSizeException | BadPaddingException
				| ShortBufferException | RandomDataGenerationException e) {
			throw new FileEncryptionException("Decryption error during read()",
					e);
		}
	}

	/**
	 * current recursion level in {@link #read(byte[], int, int)}
	 */
	private int readRecursionProtector = -1;

	public synchronized int read(byte[] b, int off, int len)
			throws IOException, FileEncryptionException, FileIntegrityException {

		if (readRecursionProtector > 1) {
			throw new FileEncryptionException("read recursion level exceeded!");
		} else {
			readRecursionProtector++;
		}

		try {

			// if we read beyond the EOF, always return -1
			if (getFilePointer() >= length()) {
				if (readRecursionProtector > 0) {
					log.error("read(): recursive read() at end of file "
							+ backingFile.getName() + "!");
				}
				readRecursionProtector--;
				return -1;
			}

			if ((b == null) || ((b.length - off) < len))
				throw new IOException("buffer null or empty");

			// check preconditions
			int offset = (int) currentchunkoffset();
			boolean lastchunk, singlechunk;

			// true if we initially read within the last chunk
			lastchunk = (currentchunkpointer() == lastchunkpointer());
			// true if we have to read more than one chunk
			singlechunk = (len <= (CHUNK_DATA_SIZE - offset)) || lastchunk;

			byte[] chunk; // = new byte[CHUNK_SIZE];

			if (singlechunk) {
				// we keep in one chunk

				// check if we read last chunk

				chunk = lastchunk ? readLastChunk(currentchunkpointer())
						: readChunk(currentchunkpointer());

				// number of bytes to copy is the minimum of the following
				// values:
				// - length of bytes to write to given array
				// - length of chunk returned by readLastChunk minus read offset
				// within the last chunk

				int n = Math.min(chunk.length - offset, len);

				// copy current decrypted chunk to corresponding location in
				// target array
				System.arraycopy(chunk, offset, b, off, n);

				// advance file pointer
				skipBytes(n);

				readRecursionProtector--;
				return n;
			} else {
				// we need to read more than one chunk

				// strategy: split array into preceding bytes, a number of
				// chunk-sized pieces and remaining bytes. read these parts
				// separately and recursively; NOTE: max recursion level should
				// be 1

				int ret = 0;

				// calculate partial chunk at beginning
				int preceding = 0;
				if (offset != 0) {
					preceding = CHUNK_DATA_SIZE - offset;
					if (preceding > 0) {
						ret += read(b, off, preceding);
						if (ret < preceding) {
							readRecursionProtector--;
							return ret;
						}

					}
				}

				// we are now aligned with the current chunk offset
				// calculate number of chunks to be read
				int nchunks = (len - preceding) / CHUNK_DATA_SIZE;
				// read chunk-sized pieces
				for (int i = 0; i < nchunks; i++) {
					ret += read(b, off + preceding + (i * CHUNK_DATA_SIZE),
							CHUNK_DATA_SIZE);
					if (ret < (preceding + ((i + 1) * CHUNK_DATA_SIZE))) {
						readRecursionProtector--;
						return ret;
					}
				}

				// remaining bytes
				int remainder = (len - preceding) % CHUNK_DATA_SIZE;
				// read remaining bytes, if there are any
				if (remainder > 0) {
					ret += read(b, off + preceding
							+ (nchunks * CHUNK_DATA_SIZE), remainder);
					if (ret < (preceding + (nchunks * CHUNK_DATA_SIZE) + remainder)) {
						readRecursionProtector--;
						return ret;
					}
				}
				readRecursionProtector--;
				return ret;
			}
		} catch (InvalidKeyException | InvalidAlgorithmParameterException
				| IllegalBlockSizeException | BadPaddingException
				| ShortBufferException | RandomDataGenerationException e) {
			throw new FileEncryptionException("Error during read()", e);
		}
	}

	public synchronized int read(byte[] b) throws IOException,
			FileEncryptionException, FileIntegrityException {
		return this.read(b, 0, b.length);
	}

	/**
	 * Encrypts and writes a single byte to the file. If this file does not yet
	 * exist, {@link #createNewFile()} will be called upon the first write call.
	 * 
	 * @see java.io.RandomAccessFile#write(int)
	 * 
	 * @param b
	 * @throws IOException
	 * @throws FileEncryptionException
	 * @throws FileIntegrityException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidAlgorithmParameterException
	 * @throws InvalidKeyException
	 * @throws ShortBufferException
	 * @throws RandomDataGenerationException
	 */
	public synchronized void write(int b) throws IOException,
			FileEncryptionException, FileIntegrityException {
		if (!exists() || realLength() == 0) {
			throw new FileEncryptionException(
					"File has not been initialized properly!");
		}

		try {
			// offset within chunk
			long curpos = getFilePointer();

			// initial write call at beginning of the file needs extra care as
			// we
			// have no preceding chunks AND are in the last chunk at the same
			// time
			if ((length() == 0) && (curpos == 0)) {
				byte[] lastchunk = new byte[1];
				lastchunk[0] = (byte) b;
				writeLastChunk(lastchunk, 0);
				skipBytes(1);
				return;
			}

			int offset = currentchunkoffset();

			// first decrypt chunk, then write byte to plaintext, then
			// re-encrypt
			byte[] decChunk;

			// check where exactly we're writing to
			long curchunk = currentchunkpointer();
			if (curchunk < lastchunkpointer()) {
				// we're writing within one of the regular chunks
				decChunk = readChunk(currentchunkpointer());
				if (offset >= decChunk.length) {
					throw new FileEncryptionException(
							"read() offset mismatch in decrypted data!");
				}

				// set byte and write chunk
				decChunk[offset] = (byte) b;
				writeChunk(decChunk, currentchunkpointer());
				skipBytes(1);
			} else if (curchunk == lastchunkpointer()) {
				// we're starting to write within the current last chunk of this
				// file; we are NOT just about to start a new last chunk
				decChunk = readLastChunk(currentchunkpointer());
				if (offset >= decChunk.length) {
					// we write beyond the former last byte in the last chunk
					byte[] lastChunk = new byte[offset + 1];
					// copy old chunk
					System.arraycopy(decChunk, 0, lastChunk, 0, decChunk.length);
					Arrays.fill(lastChunk, decChunk.length, offset, (byte) 0x00);
					lastChunk[offset] = (byte) b;
					writeLastChunk(lastChunk, currentchunkpointer());
					skipBytes(1);
				} else {
					// simply set byte and write chunk
					decChunk[offset] = (byte) b;
					writeLastChunk(decChunk, currentchunkpointer());
					skipBytes(1);
				}

			} else {
				// we're writing beyond the former last chunk of this file. this
				// may
				// happen
				// 1. by seeking beyond the former length of this file,
				// 2. when just having completely filled the former last chunk

				// strategy:
				// 1. identify index of new last chunk
				// 2. CBC-reencrypt all chunks within
				// [index_oldlastchunk;index_new
				// astchunk-1]
				// 3. write new last chunk with given byte at given offset
				// note: data in between the files old length and the given new
				// offset to write to are undefined. in this case, we're going
				// to
				// write null bytes

				long idx_newlastchunk = curchunk;
				long idx_oldlastchunk = lastchunkpointer();

				// number of chunks between old and new last chunk, e.g. in case
				// of seek()
				long diff = idx_newlastchunk - idx_oldlastchunk - 1;

				// reencrypt old last chunk
				decChunk = readLastChunk(idx_oldlastchunk);
				byte[] chunk = new byte[CHUNK_DATA_SIZE];
				System.arraycopy(decChunk, 0, chunk, 0, decChunk.length);
				// fill remaining space with null bytes
				Arrays.fill(chunk, decChunk.length, CHUNK_DATA_SIZE,
						(byte) 0x00);
				writeChunk(chunk, idx_oldlastchunk);

				// write intermediate chunks, if there are any (e.g. seek into
				// file)
				for (long i = diff; i > 0; i--) {
					Arrays.fill(chunk, (byte) 0x00);
					writeChunk(chunk, idx_newlastchunk - i);
					skipBytes(CHUNK_DATA_SIZE);
				}

				// now write new last chunk
				byte[] lastchunk = new byte[offset + 1];
				Arrays.fill(lastchunk, (byte) 0x00);
				lastchunk[offset] = (byte) b;
				writeLastChunk(lastchunk, idx_newlastchunk);
				skipBytes(1);
			}
		} catch (InvalidKeyException | InvalidAlgorithmParameterException
				| IllegalBlockSizeException | BadPaddingException
				| ShortBufferException | RandomDataGenerationException e) {
			throw new FileEncryptionException("Error during read()", e);
		}

	}

	public synchronized void write(byte[] b) throws IOException,
			FileEncryptionException, FileIntegrityException {
		this.write(b, 0, b.length);
	}

	/**
	 * current recursion level in {@link #write(byte[], int, int)}
	 */
	private int writeRecursionProtector = -1;

	/**
	 * Encrypts and writes the given byte array to the file. If the number of
	 * bytes to write exceed the chunk-size, the array in split and recursively
	 * written in multiple write calls.
	 * 
	 * @param b
	 * @param off
	 * @param len
	 * @throws IOException
	 * @throws FileEncryptionException
	 * @throws FileIntegrityException
	 * @throws ShortBufferException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidAlgorithmParameterException
	 * @throws InvalidKeyException
	 * @throws RandomDataGenerationException
	 */
	public synchronized void write(byte[] b, int off, int len)
			throws IOException, FileEncryptionException, FileIntegrityException {
		if (!exists() || realLength() == 0) {
			throw new FileEncryptionException(
					"File has not been initialized properly!");
		}

		try {

			if (writeRecursionProtector > 1) {
				throw new FileEncryptionException(
						"write recursion level exceeded!");
			} else {
				writeRecursionProtector++;
			}

			if ((b == null) || ((b.length - off) < len))
				throw new IOException("buffer null or empty");

			long curpos = getFilePointer();
			// offset within chunk
			int offset = (int) currentchunkoffset();

			// check preconditions
			// 1. does buffer span multiple chunks?
			boolean singlechunk = (len <= (CHUNK_DATA_SIZE - offset));

			if (singlechunk) {
				byte[] tmp = Arrays.copyOfRange(b, off, off + len);

				// initial write call at beginning of the file needs extra care
				// as
				// we have no preceding chunks AND are in the last chunk at the
				// same
				// time
				if ((length() == 0) && (curpos == 0)) {
					// no need to decrypt anything as we're at the beginning ..
					writeLastChunk(tmp, 0);
					skipBytes(len);
					writeRecursionProtector--;
					return;
				}

				if (currentchunkpointer() < lastchunkpointer()) {

					// we're writing within one of the regular chunks
					if ((len == CHUNK_DATA_SIZE) && (offset == 0)) {
						// optimization - if the whole chunk is written at once,
						// no
						// data need to be merged
						writeChunk(tmp, currentchunkpointer());
						skipBytes(len);
					} else {
						// chunk data needs to be merged
						byte[] decChunk = readChunk(currentchunkpointer());
						if (offset >= decChunk.length) {
							throw new FileEncryptionException(
									"read() offset mismatch in decrypted data!");
						}

						// merge & write chunk data.
						System.arraycopy(b, off, decChunk, offset, len);
						writeChunk(decChunk, currentchunkpointer());
						skipBytes(len);
					}
				} else if (currentchunkpointer() == lastchunkpointer()) {
					// we're starting to write within the current last chunk of
					// this
					// file; we are NOT just about to start a new last chunk

					byte[] decChunk = readLastChunk(currentchunkpointer());

					// merge chunk data. a little bit more complex as wee may
					// need
					// to merge a partial chunk
					byte[] merged = new byte[Math.max(decChunk.length, offset
							+ len)];
					System.arraycopy(decChunk, 0, merged, 0, decChunk.length);
					System.arraycopy(b, off, merged, offset, len);

					writeLastChunk(merged, currentchunkpointer());
					skipBytes(len);

				} else {
					// we're writing beyond the former last chunk of this file.
					// this
					// may happen
					// 1. by seeking beyond the former length of this file,
					// 2. when just having completely filled the former last
					// chunk

					long idx_newlastchunk = currentchunkpointer();
					long idx_oldlastchunk = lastchunkpointer();

					// number of chunks between old and new last chunk, e.g. in
					// case
					// of seek()
					long diff = idx_newlastchunk - idx_oldlastchunk - 1;

					// reencrypt old last chunk
					byte[] decChunk = readLastChunk(idx_oldlastchunk);
					byte[] chunk = new byte[CHUNK_DATA_SIZE];
					System.arraycopy(decChunk, 0, chunk, 0, decChunk.length);
					// fill remaining space with null bytes
					Arrays.fill(chunk, decChunk.length, CHUNK_DATA_SIZE,
							(byte) 0x00);
					writeChunk(chunk, idx_oldlastchunk);
					skipBytes(CHUNK_DATA_SIZE);

					// write intermediate chunks, if there are any (e.g. seek
					// into
					// file)
					for (long i = diff; i > 0; i--) {
						Arrays.fill(chunk, (byte) 0x00);
						writeChunk(chunk, idx_newlastchunk - i);
						skipBytes(CHUNK_DATA_SIZE);
					}

					// now write new last chunk
					byte[] lastchunk = new byte[offset + len];
					Arrays.fill(lastchunk, 0, offset, (byte) 0x00);
					System.arraycopy(b, off, lastchunk, offset, len);
					writeLastChunk(lastchunk, idx_newlastchunk);
					skipBytes(lastchunk.length);
				}
				writeRecursionProtector--;
			} else {
				// we need to write in more than one chunk

				// strategy: split array into preceding bytes, a number of
				// chunk-sized pieces and remaining bytes. write these parts
				// separately and recursively; NOTE: max recursion level should
				// be 1

				// calculate partial chunk at beginning
				int preceding = 0;
				if (offset != 0) {
					preceding = CHUNK_DATA_SIZE - offset;
					if (preceding > 0) {
						write(b, off, preceding);
					}
				}

				// we are now aligned with the current chunk offset
				// calculate number of chunks to be written
				int nchunks = (len - preceding) / CHUNK_DATA_SIZE;
				// write chunk-sized pieces
				for (int i = 0; i < nchunks; i++) {
					write(b, off + preceding + (i * CHUNK_DATA_SIZE),
							CHUNK_DATA_SIZE);
				}

				// remaining bytes
				int remainder = (len - preceding) % CHUNK_DATA_SIZE;
				// write remaining bytes, if there are any
				if (remainder > 0) {
					write(b, off + preceding + (nchunks * CHUNK_DATA_SIZE),
							remainder);
				}
				writeRecursionProtector--;
			}
		} catch (InvalidKeyException | InvalidAlgorithmParameterException
				| IllegalBlockSizeException | BadPaddingException
				| ShortBufferException | RandomDataGenerationException e) {
			throw new FileEncryptionException("Error during write()", e);
		}
	}

	/**
	 * returns the offset of an encrypted chunk for the given chunk index,
	 * taking into account any preceding metadata within the file
	 * 
	 * @param index
	 * @return offset at which the given <b>encrypted </b> chunk starts
	 * @throws IOException
	 */
	protected synchronized long chunkOffset(long index) throws IOException {
		// convert to long *prior to* multiplication to avoid
		// overflow
		return ((long) fHeader.headerSize())
				+ ((long) CHUNK_ENC_SIZE * (long) index);
	}

	/**
	 * returns number of chunks
	 * 
	 * @return
	 * @throws IOException
	 */
	protected synchronized long numchunks() throws IOException {
		if (backingRandomAccessFile.length() <= fHeader.headerSize()) {
			return 0;
		} else {
			long len = backingRandomAccessFile.length() - fHeader.headerSize();

			// if file length is aligned with chunk size, we have exactly (len /
			// chunksize) chunks; otherwise also add the last partial chunk
			return (((len % CHUNK_ENC_SIZE) == 0) ? (len / CHUNK_ENC_SIZE)
					: ((len / CHUNK_ENC_SIZE) + 1));
		}
	}

	/**
	 * indicates to which chunk the file pointer currently points; the first
	 * chunk has the index 0
	 * 
	 * @return current chunk index, or -1, if fp points to header
	 * @throws IOException
	 */
	protected synchronized long currentchunkpointer() throws IOException {
		long fp = backingRandomAccessFile.getFilePointer();
		if (fp < fHeader.headerSize()) {
			return -1;
		} else {
			fp -= fHeader.headerSize();
			return (fp / CHUNK_ENC_SIZE);
		}
	}

	/**
	 * indicates to which data offset within the current chunk the file pointer
	 * currently points
	 * 
	 * @return
	 * @throws IOException
	 */
	protected synchronized int currentchunkoffset() throws IOException {
		long fp = backingRandomAccessFile.getFilePointer();
		if (fp < fHeader.headerSize()) {
			return -1;
		} else {
			fp -= fHeader.headerSize();
			if (fp == 0) {
				return 0;
			} else {
				return (int) ((fp % CHUNK_ENC_SIZE) - CHUNK_IV_SIZE);
			}
		}
	}

	/**
	 * always points to the respective last chunk
	 * 
	 * @return
	 * @throws IOException
	 */
	protected synchronized long lastchunkpointer() throws IOException {
		long len = backingRandomAccessFile.length();

		if (len < fHeader.headerSize()) {
			return -1;
		} else {
			len -= fHeader.headerSize();
			int offset = (int) (len % CHUNK_ENC_SIZE);

			if ((offset == 0) && (len == 0)) {
				// at the very beginning we're in chunk 0
				return 0;
			} else if ((len != 0) && (offset == 0)) {
				// right at the offset of a new chunk, we still have to return
				// the old chunk
				return ((len / CHUNK_ENC_SIZE) - 1);
			} else {
				return (len / CHUNK_ENC_SIZE);
			}
		}
	}

	/**
	 * @param n
	 * @return
	 * @throws IOException
	 * @throws FileEncryptionException
	 * @see java.io.RandomAccessFile#skipBytes(int)
	 */
	public synchronized int skipBytes(int n) throws IOException,
			FileEncryptionException {
		long pos, len, newpos;

		if (n <= 0) {
			return 0;
		}

		pos = getFilePointer();
		len = length();
		newpos = pos + n;
		if (newpos > len) {
			newpos = len;
		}
		seek(newpos);

		return (int) (newpos - pos);
	}

	/**
	 * Method encapsulates {@link java.io.RandomAccessFile#getFilePointer()} and
	 * adds additional logic for omitting chunk meta data. File pointer offsets
	 * are converted to corresponding positions within the encrypted
	 * {@link RandomAccessFile}
	 * 
	 * @return virtual plantext position within this file
	 * @throws IOException
	 * @see java.io.RandomAccessFile#getFilePointer()
	 */
	public long getFilePointer() throws IOException {
		long realFP = backingRandomAccessFile.getFilePointer();
		if (realFP < fHeader.headerSize()) {
			return -1;
		} else {
			realFP -= fHeader.headerSize();
			if (realFP == 0) {
				return 0;
			} else {
				// NOTE: We rely on the assumption the real file pointer never
				// points into any metadata structure. Any method which
				// manipulates the file pointer has to take this into account.
				long virtfp;
				long virtidx;
				int virtoffset;
				virtidx = (realFP / CHUNK_ENC_SIZE);
				virtoffset = (int) (realFP % CHUNK_ENC_SIZE - CHUNK_IV_SIZE);
				virtfp = virtidx * CHUNK_DATA_SIZE + virtoffset;

				return virtfp;
			}
		}
	}

	/**
	 * Method encapsulates {@link java.io.RandomAccessFile#seek(long)} and adds
	 * additional logic for omitting chunk meta data. File pointer offsets are
	 * converted to corresponding positions within the encrypted
	 * {@link RandomAccessFile}.
	 * 
	 * @param pos
	 *            virtual plaintext position within this file
	 * @throws IOException
	 * @throws FileEncryptionException
	 * @see java.io.RandomAccessFile#seek(long)
	 */
	public synchronized void seek(long pos) throws IOException,
			FileEncryptionException {
		long virtidx = (pos / CHUNK_DATA_SIZE);
		int virtoffset = (int) (pos % CHUNK_DATA_SIZE);
		// determine corresponding position w.r.t. chunk metadata; NOTE: we have
		// to make sure the file pointer never points into crypto metadata
		// structures
		long realPos = virtidx * CHUNK_ENC_SIZE + CHUNK_IV_SIZE + virtoffset
				+ fHeader.headerSize();

		if (isFPValid(realPos)) {
			backingRandomAccessFile.seek(realPos);
		} else {
			throw new FileEncryptionException(
					"FP may not point into crypto metadata!");
		}
	}

	/**
	 * checks if given file pointer points to a crypto meta data structure
	 * 
	 * @return <code>true</code> if fp points into crypto meta data (header, IV,
	 *         authentication tag), <code>false</code> otherwise
	 * @throws IOException
	 * @throws FileEncryptionException
	 *             if fp currently points into an initialization vector or into
	 *             the authentication tag
	 */
	protected boolean isFPValid(long newpos) throws IOException {
		if (newpos < fHeader.headerSize()) {
			return false;
		} else {
			newpos -= fHeader.headerSize();
		}

		long offset = newpos % CHUNK_ENC_SIZE;
		if (offset < CHUNK_IV_SIZE) {
			// fp currently points into IV data strcuture
			// throw new FileEncryptionException(
			// "FP points into IV data structure!");
			return false;
		}

		if (offset >= CHUNK_IV_SIZE + CHUNK_DATA_SIZE) {
			// fp currently points into authentication tag data structure
			// throw new FileEncryptionException(
			// "FP points into authentication tag!");
			return false;
		}

		return true;
	}

	/**
	 * @return
	 * @throws IOException
	 * @see java.io.RandomAccessFile#length()
	 */
	public synchronized long length() throws IOException {
		if (realLength() == 0) {
			return 0;
		} else {
			long metadatasize = (numchunks() * (CHUNK_IV_SIZE + CHUNK_TLEN))
					+ fHeader.headerSize();
			return realLength() - metadatasize;
		}
	}

	/**
	 * @return
	 * @see java.io.File#getAbsolutePath()
	 */
	public String getAbsolutePath() {
		return backingFile.getAbsolutePath();
	}

	/**
	 * returns the real file length including all meta data
	 * 
	 * @return
	 * @throws IOException
	 */
	public long realLength() throws IOException {
		long ret = backingRandomAccessFile.length();
		return ret;
	}

	/**
	 * Sets this encrypted file's length (@see
	 * java.io.RandomAccessFile#setLength(long)). File truncation/extension is
	 * specifically handled by re-encrypting this file's last full and partial
	 * chunk
	 * 
	 * @param newLength
	 * @throws IOException
	 * @throws FileEncryptionException
	 * @throws FileIntegrityException
	 * @throws ShortBufferException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidAlgorithmParameterException
	 * @throws InvalidKeyException
	 * @throws RandomDataGenerationException
	 * 
	 */
	public synchronized void setLength(long newLength) throws IOException,
			FileEncryptionException, FileIntegrityException {
		try {
			long oldlen = length();
			int offset_new = (int) (newLength % CHUNK_DATA_SIZE);
			int offset_old = (int) (oldlen % CHUNK_DATA_SIZE);

			long idx_newlastchunk;
			if ((offset_new == 0) && (newLength == 0)) {
				// at the very beginning we're in chunk 0
				idx_newlastchunk = 0;
			} else if ((newLength != 0) && (offset_new == 0)) {
				// we are right at the offset of a new chunk
				idx_newlastchunk = ((newLength / CHUNK_DATA_SIZE) - 1);
			} else {
				idx_newlastchunk = (newLength / CHUNK_DATA_SIZE);
			}

			long idx_oldlastchunk = lastchunkpointer();
			byte[] decChunk;
			boolean truncate = false;
			if (newLength > oldlen) {
				// file will be extended

				// check if one or more new chunks need to be created
				if (idx_newlastchunk == idx_oldlastchunk) {
					// we're operating within the current last chunk of this
					// file; no new chunks need to be created
					decChunk = readLastChunk(idx_oldlastchunk);

					// we write beyond the former last byte in the last chunk
					byte[] lastChunk = new byte[offset_new];
					// copy old chunk
					System.arraycopy(decChunk, 0, lastChunk, 0, decChunk.length);

					// contents in extended portion of the file are "undefined",
					// for
					// noew we just reset them to 0
					Arrays.fill(lastChunk, decChunk.length, offset_old,
							(byte) 0x00);
					writeLastChunk(lastChunk, idx_newlastchunk);
				} else if (idx_newlastchunk > idx_oldlastchunk) {
					// number of chunks need to be increased
					long diff = idx_newlastchunk - idx_oldlastchunk - 1;

					// reencrypt old last chunk
					decChunk = readLastChunk(idx_oldlastchunk);
					byte[] chunk = new byte[CHUNK_DATA_SIZE];
					System.arraycopy(decChunk, 0, chunk, 0, decChunk.length);
					// fill remaining space with null bytes
					Arrays.fill(chunk, decChunk.length, CHUNK_DATA_SIZE,
							(byte) 0x00);
					writeChunk(chunk, idx_oldlastchunk);

					// write intermediate chunks, if there are any (e.g. seek
					// into
					// file)
					for (long i = diff; i > 0; i--) {
						Arrays.fill(chunk, (byte) 0x00);
						writeChunk(chunk, idx_newlastchunk - i);
						skipBytes(CHUNK_DATA_SIZE);
					}

					// now write new last chunk
					byte[] lastchunk = new byte[offset_new];
					Arrays.fill(lastchunk, (byte) 0x00);
					writeLastChunk(lastchunk, idx_newlastchunk);
				} else {
					log.error("setLength(): Offset / Size mismatch while setting new length of file!");
				}

			} else if (newLength < oldlen) {
				// file will be truncated
				// check if one or more new chunks need to be removed
				if (idx_newlastchunk == idx_oldlastchunk) {
					// we're operating within the current last chunk of this
					// file; reencrypt its data
					decChunk = readLastChunk(idx_oldlastchunk);
					byte[] lastChunk = new byte[offset_new];

					// truncate old chunk and write it
					System.arraycopy(decChunk, 0, lastChunk, 0,
							lastChunk.length);
					writeLastChunk(lastChunk, idx_newlastchunk);
					truncate = true;
				} else if (idx_newlastchunk < idx_oldlastchunk) {
					// number of chunks need to be decreased

					if (implementsAuthentication()) {
						// also remove authentication tags of intermediate
						// chunks to
						// be removed
						for (long i = idx_newlastchunk + 1; i <= idx_oldlastchunk; i++) {
							authTagVerifier.removeChunkAuthTag(i);
						}
						// no need to update the file auth tag at this point, as
						// this will automatically be handled when writing the
						// new
						// last chunk
					}

					// reencrypt new last chunk
					decChunk = readChunk(idx_newlastchunk);
					byte[] chunk = new byte[offset_new];
					System.arraycopy(decChunk, 0, chunk, 0, offset_new);
					writeLastChunk(chunk, idx_newlastchunk);
					truncate = true;
				} else {
					log.error("setLength(): Offset / Size mismatch while setting new length of file!");
				}
			} else {
				log.info("setLength(): setLength(): No length adjustment was necessary!");
				return;
			}

			if (implementsCaching()) {
				flush();
			}

			// call setLength in case of truncation to remove any obsolete
			// trailing data.
			// NOTE: only re-adjust length in case of truncation. if the file is
			// to be extended, this has happened by now due to write() and
			// flush() calls. if the size did not change, calling setLength() is
			// not necessary
			if (truncate) {
				// derive real new length from given value
				long virtidx;
				int virtoffset;
				long realLen;
				virtidx = (newLength / CHUNK_DATA_SIZE);
				virtoffset = (int) (newLength % CHUNK_DATA_SIZE);
				// determine corresponding position w.r.t. chunk metadata
				realLen = virtidx
						* CHUNK_ENC_SIZE
						+ fHeader.headerSize()
						+ ((virtoffset > 0) ? CHUNK_IV_SIZE + CHUNK_TLEN
								+ virtoffset : 0);

				// reflect changes to backend file
				// also, setLength() re-adjusts file pointer in case of
				// truncation
				backingRandomAccessFile.setLength(realLen);
			}
		} catch (RandomDataGenerationException | InvalidKeyException
				| InvalidAlgorithmParameterException
				| IllegalBlockSizeException | BadPaddingException
				| ShortBufferException e) {
			throw new FileEncryptionException("Error during setLength()", e);
		}
	}

	/**
	 * indicates if this {@link EncRandomAccessFile}-instance has already been
	 * made persistent or if it only exists in memory (see {@link File#exists()}
	 * .
	 * 
	 * @return <code>true</code>, if this file has already been made persistent,
	 *         <code>false</code> otherwise
	 */
	public boolean exists() {
		return backingFile.exists();
	}

	// public boolean isWritable() {
	// return this.writable && backingFile.canWrite();
	// }

	/**
	 * returns the current share key version of this file as stored within the
	 * file header, which has been used for encrypting this files specific file
	 * encryption key
	 * 
	 * @return share key version of this file
	 * @throws FileEncryptionException
	 *             if share key version has not yet been defined
	 */
	public int getShareKeyVersion() throws FileEncryptionException, IOException {
		if (!isOpen()) {
			throw new IOException("File has not been opened!");
		} else {
			int ret;
			if ((fHeader == null) || (ret = fHeader.getShareKeyVersion()) == -1) {
				throw new FileEncryptionException(
						"Share key version has not been defined!");
			} else {
				return ret;
			}
		}
	}

	/**
	 * rebuilds the file authentication tag tree, if necessary, and writes the
	 * new file authentication tag to the header
	 */
	protected abstract void flushAuthData() throws FileEncryptionException,
			IOException;

	/**
	 * helper class for instance management
	 */
	protected static class InstanceEntry {
		private String normalizedFilename;
		private boolean writable;

		/**
		 * @param fileName
		 * @param writable
		 */
		private InstanceEntry(File file, boolean writable) {
			this.normalizedFilename = FilenameUtils.normalize(file
					.getAbsolutePath());
			this.writable = writable;
		}

		/**
		 * @return the normalizedFilename
		 */
		public String getNormalizedFilename() {
			return normalizedFilename;
		}

		/**
		 * @return the writable
		 */
		public boolean isWritable() {
			return writable;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime
					* result
					+ ((normalizedFilename == null) ? 0 : normalizedFilename
							.hashCode());
			result = prime * result + (writable ? 1231 : 1237);
			return result;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			InstanceEntry other = (InstanceEntry) obj;
			if (normalizedFilename == null) {
				if (other.normalizedFilename != null)
					return false;
			} else if (!normalizedFilename.equals(other.normalizedFilename))
				return false;
			if (writable != other.writable)
				return false;
			return true;
		}

		public static InstanceEntry instance(File backingFile, boolean writeable) {
			return new InstanceEntry(backingFile, writeable);
		}
	}

	// protected final static Hashtable<InstanceEntry, EncRandomAccessFile>
	// instanceMap = new Hashtable<InstanceEntry, EncRandomAccessFile>();
	//
	// protected static void registerInstance(EncRandomAccessFile encFile)
	// throws FileEncryptionException {
	//
	// if (getInstance(encFile.getAbsolutePath(), encFile.isWritable()) != null)
	// {
	// throw new FileEncryptionException("instance already registered!");
	// } else {
	// instanceMap.put(
	// InstanceEntry.instance(encFile.getAbsolutePath(),
	// encFile.isWritable()), encFile);
	// }
	// }
	//
	// protected static boolean removeInstance(String filename, boolean
	// writable) {
	// return (instanceMap.remove(InstanceEntry.instance(filename, writable)) !=
	// null);
	// }
	//
	// protected static EncRandomAccessFile getInstance(String filename,
	// boolean writable) {
	// return instanceMap.get(InstanceEntry.instance(filename, writable));
	// }

	/**
	 * Method opens an instance of a {@link EncRandomAccessFile}-implementation
	 * for an already existing file. For creating new files, see
	 * {@link #create(int, SecretKey)}.
	 * 
	 * NOTE: After an instance has been opened with this method, it still needs
	 * to be initialized with the corresponding share key from the share
	 * metadata DB.
	 * 
	 * @throws FileEncryptionException
	 * @throws IOException
	 * @throws NoSuchProviderException
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 */
	public abstract void open() throws IOException, FileEncryptionException;

	/**
	 * Method creates a new backend file which is to be managed with this
	 * {@link EncRandomAccessFile}-instance with the given arguments (and,
	 * correspondingly, read/write access). A new header is generated and
	 * written. This method assumes there currently exists no file at the
	 * specified location, otherwise an exception is thrown. For opening
	 * existing files, see {@link #open(String)}.
	 * 
	 * @param shareKeyVersion
	 *            latest share key version
	 * @param shareKey
	 *            latest share key
	 * @throws FileEncryptionException
	 * @throws IOException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidKeyException
	 * @throws RandomDataGenerationException
	 * @throws NoSuchProviderException
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 */
	public abstract void create(int shareKeyVersion, SecretKey shareKey)
			throws FileEncryptionException, IOException;

	/**
	 * @throws IOException
	 * @see java.io.RandomAccessFile#close()
	 */
	public synchronized void close() throws IOException {
		if (isOpen()) {
			// make sure all data have been flushed
			if (writable && implementsCaching() && cache.needsToBeWritten) {
				log.warn("close(): Cached data still need to be written, calling flush()");
				flush();
			}

			// invalidate cache, ...
			if (implementsCaching())
				this.cache = null;
			if (implementsAuthentication())
				this.authTagVerifier = null;

			backingRandomAccessFile.close();
			setOpen(false);
			setInitialized(false);
		} else {
			log.warn("close(): File not open!");
		}
	}

	/**
	 * Method creates a new {@link AESCBCRandomAccessFile} with the given
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
	 * @return
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidAlgorithmParameterException
	 * @throws NoSuchProviderException
	 * @throws RandomDataGenerationException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws FileEncryptionException
	 * @throws IOException
	 */
	public static EncRandomAccessFile create(int shareKeyVersion,
			SecretKey shareKey, File file) throws FileEncryptionException,
			IOException {
		// do nothing - to be implemented in subclasses
		return null;
	}

	public static EncRandomAccessFile create(int shareKeyVersion,
			SecretKey shareKey, String file) throws FileEncryptionException,
			IOException {
		// do nothing - to be implemented in subclasses
		return null;
	}

	/**
	 * Method opens a {@link AESCBCRandomAccessFile}-instance with the given
	 * arguments for an already existing file. For creating new files, see
	 * {@link #create(int, SecretKey, File)}. NOTE: After an instance has been
	 * obtained with this method, ist still needs to be initialized with the
	 * corresponding share key from the share metadata DB (see
	 * {@link #initWithShareKey(SecretKey)}).
	 * 
	 * @param file
	 * @param writable
	 * @return
	 * @throws FileEncryptionException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws NoSuchProviderException
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws InvalidAlgorithmParameterException
	 * @throws RandomDataGenerationException
	 */
	public static EncRandomAccessFile open(File file, boolean writable)
			throws FileEncryptionException, IOException {
		// do nothing - to be implemented in subclasses
		return null;
	}

	public static EncRandomAccessFile open(String file, boolean writable)
			throws FileEncryptionException, IOException {
		// do nothing - to be implemented in subclasses
		return null;
	}

	abstract protected void printInstanceMap();

	/**
	 * returns an instance for the given arguments
	 * 
	 * @param file
	 *            backend file to be written/read
	 * @param writable
	 *            <code>true</code> if file should be opened writable,
	 *            <code>false</code> otherwise
	 * @return
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidAlgorithmParameterException
	 * @throws NoSuchProviderException
	 * @throws RandomDataGenerationException
	 */
	public static EncRandomAccessFile getInstance(File file, boolean writable)
			throws FileEncryptionException, IOException {
		// do nothing - to be implemented in subclasses
		return null;
	}

	/**
	 * renames and switches the encapsulated {@link File}-instance.
	 * 
	 * NOTE: If the given File f to rename to currently exists, contains data
	 * and/or has an open file handle, success of this method may be
	 * platform-dependent. see {@link File#renameTo(File)}.
	 * 
	 * @param f
	 * @return <code>true</code> if renaming succeeded, <code>false</code>
	 *         otherwise
	 */
	abstract public boolean renameTo(File f);

	// protected static Hashtable getInstanceMap() {
	// return null;
	// }

	/**
	 * Convenience method unifying {@link #seek(long)} and {@link #read(byte[])}
	 * in one synchronized method
	 * 
	 * @param fileoffset
	 * @param buf
	 * @return
	 * @throws IOException
	 * @throws FileEncryptionException
	 * @throws FileIntegrityException
	 */
	public synchronized int readAt(long fileoffset, byte[] buf)
			throws IOException, FileEncryptionException, FileIntegrityException {
		seek(fileoffset);
		return read(buf);
	}

	/**
	 * Convenience method unifying {@link #seek(long)} and
	 * {@link #write(byte[], int, int)} in one synchronized method
	 * 
	 * @param fileoffset
	 * @param buf
	 * @param bufferoffset
	 * @param length
	 * @throws IOException
	 * @throws FileEncryptionException
	 * @throws FileIntegrityException
	 */
	public synchronized void writeAt(long fileoffset, byte[] buf,
			int bufferoffset, int length) throws IOException,
			FileEncryptionException, FileIntegrityException {
		seek(fileoffset);
		write(buf, bufferoffset, length);
	}

	/**
	 * tries to lock the underlying {@link RandomAccessFile}, if opened
	 * 
	 * @param blocking
	 * @return
	 * @throws IOException
	 */
	public synchronized FileLock lock(boolean blocking) throws IOException {
		if (isOpen()) {
			FileChannel channel = backingRandomAccessFile.getChannel();
			FileLock ret;
			if (blocking) {
				ret = channel.lock();
			} else {
				ret = channel.tryLock();
			}

			return ret;
		} else {
			throw new IOException("Can't acquire lock for closed file!");
		}
	}
}
