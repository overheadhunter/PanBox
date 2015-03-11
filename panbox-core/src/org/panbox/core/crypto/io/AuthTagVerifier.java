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

import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.panbox.core.exception.FileEncryptionException;

/**
 * @author palige
 * 
 *         Class manages an authentication tag tree. The purpose of this
 *         structure is for verifying the integrity of a single chunks
 *         authentication tag within an {@link EncRandomAccessFile}-instance
 *         w.r.t. to the file as a whole. Apart from the central file
 *         authentication tag, all authentication tag management is conducted
 *         in-memory only, i.e. in particular chunk authentication tags still
 *         need to be written to disk after having been updated.
 */
public class AuthTagVerifier {

	/**
	 * {@link EncRandomAccessFile}-instance using this {@link AuthTagVerifier}
	 * for auth tag verification
	 */
	private EncRandomAccessFile backEncRandomAccessFile;

	private int CHUNK_AUTH_TAG_LENGTH;

	/**
	 * HMAC for calculating the root authentication tag of this file.
	 */
	protected final HMac authTagHMac;

	/**
	 * hard-coded file auth tag size for SHA256Digest
	 */
	final static int AUTH_TAG_SIZE = 32;

	/**
	 * @param
	 */
	public AuthTagVerifier(EncRandomAccessFile encRandomAccessFile) {
		this.backEncRandomAccessFile = encRandomAccessFile;
		this.CHUNK_AUTH_TAG_LENGTH = encRandomAccessFile.CHUNK_TLEN;
		this.authTagHMac = new HMac(new SHA256Digest());
		// KeyParameter keyParame = new
		// KeyParameter(backEncRandomAccessFile.secretKey.)
		// authTagHMac.init(null);

		this.atagList = new ArrayList<byte[]>();
	}

	/**
	 * updates the central file authentication tag based upon the current
	 * contents of the authentication tag table;
	 * 
	 * @throws FileEncryptionException
	 * @throws IOException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidKeyException
	 */
	public synchronized void updateFileAuthTag()
			throws FileEncryptionException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException, IOException {
		byte[] tmp = buildFileAuthTag();
		backEncRandomAccessFile.writeFileAuthenticationTag(tmp);
		this.needsUpdate = false;
	}

	// /**
	// * @return the size of a file authentication tag in bytes
	// */
	// public static int getFileTagSize() {
	// // return authTagHMac.getMacSize();
	// return AUTH_TAG_SIZE;
	// }

	/**
	 * builds and returns the root authentication tag
	 * 
	 * @return
	 * @throws FileEncryptionException
	 */
	private byte[] buildFileAuthTag() throws FileEncryptionException {
		SecretKey key = backEncRandomAccessFile.shareKey;
		if (key == null || key.getEncoded().length == 0) {
			throw new FileEncryptionException(
					"Invalid file encryption key in encrypting random access file!");
		} else {
			authTagHMac.reset();
			KeyParameter keyParameter = new KeyParameter(key.getEncoded());
			authTagHMac.init(keyParameter);
		}

		if (atagList.size() == 0) {
			throw new FileEncryptionException(
					"No chunk authentication tags have been set yet!");
		} else {
			for (int i = 0; i < atagList.size(); i++) {
				byte[] curChunkTag = atagList.get(i);
				if ((curChunkTag == null)
						|| (curChunkTag.length != CHUNK_AUTH_TAG_LENGTH)) {
					throw new FileEncryptionException(
							"Invalid chunk authentication tag in auth tag table at offset: "
									+ i);
				} else {
					authTagHMac.update(curChunkTag, 0, CHUNK_AUTH_TAG_LENGTH);
				}
			}
			byte[] tmp = new byte[AUTH_TAG_SIZE];
			authTagHMac.doFinal(tmp, 0);
			return tmp;
		}
	}

	private ArrayList<byte[]> atagList = new ArrayList<byte[]>();

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		if (atagList == null) {
			buf.append("null");
		} else {
			for (int i = 0; i < atagList.size(); i++) {
				buf.append(i + ": ");
				byte[] bla = atagList.get(i);
				if (bla == null) {
					buf.append("null");
				} else {
					for (int j = 0; j < bla.length; j++) {
						buf.append(bla[j] + " ");
					}
					buf.append("\n");
				}
			}
		}
		return buf.toString();
	}

	/**
	 * verifies the complete list of all authentication tags w.r.t. the central
	 * file authentication tag
	 * 
	 * @return <code>true</code> if the verification was positive,
	 *         <code>false</code> otherwise
	 * @throws FileEncryptionException
	 */
	public synchronized boolean verifyFileAuthTag()
			throws FileEncryptionException {
		byte[] storedTag = backEncRandomAccessFile.readFileAuthenticationTag();
		if (storedTag == null || storedTag.length != AUTH_TAG_SIZE) {
			throw new FileEncryptionException(
					"Encrypted file has invalid file authentication tag!");
		} else {
			byte[] tmp = buildFileAuthTag();
			return Arrays.equals(tmp, storedTag);
		}
	}

	/**
	 * verifies if the given chunk authentication tag equals the one in the
	 * authentication tag list, but DOES NOT verify if the complete list of all
	 * authentication tags is valid w.r.t. the central file authentication tag
	 * 
	 * @param chunkIdx
	 * @param chunkAuthTag
	 * @return
	 * @throws FileEncryptionException
	 */
	public synchronized boolean verifyChunkAuthTag(int chunkIdx,
			byte[] chunkAuthTag) throws FileEncryptionException {
		if ((chunkIdx < 0) || (chunkAuthTag == null)
				|| (chunkAuthTag.length != backEncRandomAccessFile.CHUNK_TLEN)) {
			throw new FileEncryptionException(
					"Invalid auth tag parameter given!");
		} else {
			byte[] storedAuthTag = atagList.get(chunkIdx);
			if (storedAuthTag == null
					|| !Arrays.equals(chunkAuthTag, storedAuthTag)) {
				return false;
			} else {
				// return verifyFileAuthTag();
				return true;
			}
		}
	}

	/**
	 * updates the value of an existing authentication tag in the authentication
	 * tag tree AND sets the needsUpdate flag. It is imperative that the
	 * corresponding chunk auth tag has already been written to disk prior to
	 * calling this function. NOTE: This mthod neither rebuilds the file
	 * authentication tag tree nor writes the resulting file authentication tag.
	 * See {@link #updateFileAuthTag()}
	 * 
	 * @param chunkIdx
	 * @param chunkAuthTag
	 * @throws FileEncryptionException
	 * @throws IOException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidKeyException
	 */
	public synchronized void updateChunkAuthTag(int chunkIdx,
			byte[] chunkAuthTag) throws FileEncryptionException,
			InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, IOException {
		insertChunkAuthTag(chunkIdx, chunkAuthTag);
		// updateFileAuthTag();
		this.needsUpdate = true;
	}

	/**
	 * helper method returning single chunk authentication tag that has been
	 * inserted before
	 * 
	 * @param chunkIdx
	 *            index of chunk auth tag to be returned
	 * @return chunk authentication tag as byte array, if set, <code>null</code>
	 *         otherwise
	 * @throws FileEncryptionException
	 */
	public final synchronized byte[] getStoredChunkAuthTag(int chunkIdx)
			throws FileEncryptionException {
		if (chunkIdx < 0) {
			throw new FileEncryptionException("Invalid chunk index given!");
		} else {
			return atagList.get(chunkIdx);
		}
	}

	/**
	 * Method inserts a new authentication tag WITHOUT setting the needsUpdate
	 * flag. To be used for initial construction of the authentication tag
	 * table.
	 * 
	 * @param chunkIdx
	 * @param chunkAuthTag
	 * @throws FileEncryptionException
	 */
	public synchronized void insertChunkAuthTag(int chunkIdx,
			byte[] chunkAuthTag) throws FileEncryptionException {
		if (chunkIdx < 0 || chunkAuthTag == null
				|| chunkAuthTag.length != CHUNK_AUTH_TAG_LENGTH) {
			throw new FileEncryptionException(
					"Invalid auth tag parameter given!");
		} else {
			if (atagList.size() <= chunkIdx) {
				atagList.ensureCapacity(chunkIdx + 1);
				padTo(atagList, chunkIdx);
				atagList.add(chunkIdx,
						Arrays.copyOf(chunkAuthTag, CHUNK_AUTH_TAG_LENGTH));
			} else {
				// list capacity is sufficient and element exists
				atagList.set(chunkIdx,
						Arrays.copyOf(chunkAuthTag, CHUNK_AUTH_TAG_LENGTH));
			}
		}
	}

	/**
	 * indicates if the file authentication tag needs to be re-built
	 */
	private boolean needsUpdate;

	public boolean needsUpdate() {
		return needsUpdate;
	}

	private static void padTo(List<?> list, int size) {
		for (int i = list.size(); i < size; i++)
			list.add(null);
	}

	/**
	 * Method removes an existing authentication tag WITHOUT rebuilding the
	 * value of the central file authentication tag. May e.g. be used in case of
	 * file truncation. Note: In case of removal of intermediate chunks, it may
	 * become necessary to rebuild the authentication tag table
	 * 
	 * @param chunkIdx
	 * @param chunkAuthTag
	 * @throws FileEncryptionException
	 */
	protected synchronized void removeChunkAuthTag(long chunkIdx)
			throws FileEncryptionException {
		if (chunkIdx < 0) {
			throw new FileEncryptionException(
					"Invalid auth tag parameter given!");
		} else {
			atagList.remove(chunkIdx);
		}
	}

	/**
	 * completely resets this authentication tag tree instance
	 */
	public synchronized void reset() {
		authTagHMac.reset();
		atagList.clear();
	}
}
