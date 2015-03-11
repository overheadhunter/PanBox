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
import java.util.Set;
import java.util.WeakHashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.panbox.core.crypto.KeyConstants;
import org.panbox.core.exception.FileEncryptionException;
import org.panbox.core.exception.RandomDataGenerationException;

/**
 * @author palige
 * 
 *         AES GCM based implementation of {@link EncRandomAccessFile} with
 *         additional integrity protection mechanism (see
 *         {@link AuthTagVerifier}. This implementation supports AES NI Hardware
 *         acceleration, if available and supported by the underlying JDK but
 *         requires Bouncycastle *and* SunJCE.
 */
public class AESGCMRandomAccessFileHW extends AESGCMRandomAccessFileCompat {

	private static final Logger log = Logger.getLogger("org.panbox.core");

	protected AESGCMRandomAccessFileHW(File backingFile)
			throws InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidAlgorithmParameterException,
			NoSuchProviderException, RandomDataGenerationException, IOException {
		super(backingFile);
	}

	@Override
	protected void initCiphers() throws NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidKeyException,
			RandomDataGenerationException, InvalidAlgorithmParameterException,
			NoSuchProviderException {

		super.initCiphers();

		// TODO: This following code mixes the SunJCE AES blockcipher
		// implementation with Bouncycastle's GCMBlockCipher to improve
		// performance due to SunJCE's AES NI support. Replace this with
		// "native" BC code, as soon as they introduce AES NI support
		// themselves. For more information see
		// http://bouncy-castle.1462172.n4.nabble.com/Using-BC-AES-GCM-for-S3-td4657050.html
		this.gcmEngine = new GCMBlockCipher(new BlockCipher() {
			Cipher aes = Cipher.getInstance("AES/ECB/NoPadding",
					KeyConstants.PROV_SunJCE);

			public void reset() {
			}

			public int processBlock(byte[] in, int inOff, byte[] out, int outOff)
					throws DataLengthException, IllegalStateException {
				try {
					aes.update(in, outOff, getBlockSize(), out, outOff);
				} catch (ShortBufferException e) {
					throw new DataLengthException();
				}
				return getBlockSize();
			}

			public void init(boolean forEncryption, CipherParameters params)
					throws IllegalArgumentException {
				KeyParameter kp = (KeyParameter) params;
				SecretKeySpec key = new SecretKeySpec(kp.getKey(), "AES");
				try {
					aes.init(forEncryption ? Cipher.ENCRYPT_MODE
							: Cipher.DECRYPT_MODE, key);
				} catch (InvalidKeyException e) {
					throw new IllegalArgumentException(e);
				}
			}

			public int getBlockSize() {
				return aes.getBlockSize();
			}

			public String getAlgorithmName() {
				return aes.getAlgorithm();
			}
		});
	}

	/**
	 * Method creates a new {@link AESGCMRandomAccessFileHW} with the given
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
	public static AESGCMRandomAccessFileHW create(int shareKeyVersion,
			SecretKey shareKey, File file) throws FileEncryptionException,
			IOException {
		AESGCMRandomAccessFileHW ret = AESGCMRandomAccessFileHW.getInstance(
				file, true);
		ret.create(shareKeyVersion, shareKey);
		return ret;
	}

	public static AESGCMRandomAccessFileHW create(int shareKeyVersion,
			SecretKey shareKey, String file, String mode)
			throws FileEncryptionException, InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidAlgorithmParameterException, NoSuchProviderException,
			IllegalBlockSizeException, BadPaddingException, IOException,
			RandomDataGenerationException {
		return create(shareKeyVersion, shareKey, new File(file));
	}

	/**
	 * Method opens a {@link AESGCMRandomAccessFileHW}-instance with the given
	 * arguments for an already existing file. For creating new files, see
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
	public static AESGCMRandomAccessFileHW open(File file, boolean writable)
			throws FileEncryptionException, IOException {
		AESGCMRandomAccessFileHW ret = AESGCMRandomAccessFileHW.getInstance(
				file, writable);
		ret.open();
		return ret;
	}

	public static AESGCMRandomAccessFileHW open(String file, boolean writable)
			throws FileEncryptionException, IOException {
		return open(new File(file), writable);
	}

	protected final static WeakHashMap<InstanceEntry, AESGCMRandomAccessFileHW> instanceMap = new WeakHashMap<InstanceEntry, AESGCMRandomAccessFileHW>();

	public static synchronized AESGCMRandomAccessFileHW getInstance(
			File backingFile, boolean writable) throws FileEncryptionException,
			IOException {
		try {
			AESGCMRandomAccessFileHW ret = instanceMap.get(InstanceEntry
					.instance(backingFile, writable));
			if (ret == null) {
				ret = new AESGCMRandomAccessFileHW(backingFile);
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

	protected static WeakHashMap<InstanceEntry, AESGCMRandomAccessFileHW> getInstancemap() {
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
			AESGCMRandomAccessFileHW tmp;
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
