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
package org.panbox.core.keymgmt;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;

import org.panbox.core.csp.StorageBackendType;
import org.panbox.core.identitymgmt.AbstractIdentity;

public final class VolumeParams implements Serializable {

	public static final class VolumeParamsFactory {

		private VolumeParamsFactory() {
		}


		public static VolumeParamsFactory getFactory() {
			return new VolumeParamsFactory();
		}

		public VolumeParams createVolumeParams() {
			VolumeParams volumeParams = new VolumeParams();
			return volumeParams;
		}

	}

	private static final long serialVersionUID = 8280341005536130781L;

	public PublicKey ownerSigKey;
	public PublicKey pubSigKey;
	public PublicKey pubEncKey;

	public PublicKey otherSigKey;
	public PublicKey otherEncKey;

	public PublicKey deviceKey;

	public PrivateKey privSigKey;
	public PrivateKey privEncKey;

	public String ownerAlias;
	public String userAlias;
	public String deviceAlias;

	public PrivateKey devicePrivateKey;

	public String path;

	public StorageBackendType type;

	public String shareName;

	public long seqNum = -1;

	public long splSeqNum = -1;

	private VolumeParams() {
		super();
	}

	public VolumeParams setShareName(String shareName) {
		this.shareName = shareName;
		return this;
	}

	public VolumeParams setPath(String path) {
		this.path = path;
		return this;
	}

	public VolumeParams setType(StorageBackendType type) {
		this.type = type;
		return this;
	}

	public VolumeParams setOwnerSignatureKey(PublicKey k) {
		this.ownerSigKey = k;
		return this;
	}

	public VolumeParams setPublicSignatureKey(PublicKey k) {
		this.pubSigKey = k;
		return this;
	}

	public VolumeParams setPublicDeviceKey(PublicKey k) {
		this.deviceKey = k;
		return this;
	}

	public VolumeParams setPublicEncryptionKey(PublicKey k) {
		this.pubEncKey = k;
		return this;
	}

	public VolumeParams setOtherSignatureKey(PublicKey k) {
		this.otherSigKey = k;
		return this;
	}

	public VolumeParams setOtherEncryptionKey(PublicKey k) {
		this.otherEncKey = k;
		return this;
	}

	public VolumeParams setPrivateSignatureKey(PrivateKey k) {
		this.privSigKey = k;
		return this;
	}

	public VolumeParams setPrivateEncryptionKey(PrivateKey k) {
		this.privEncKey = k;
		return this;
	}

	public VolumeParams setOwnerAlias(String s) {
		this.ownerAlias = s;
		return this;
	}

	public VolumeParams setUserAlias(String s) {
		this.userAlias = s;
		return this;
	}

	public VolumeParams setDeviceAlias(String s) {
		this.deviceAlias = s;
		return this;
	}

	public VolumeParams setKeys(AbstractIdentity id, char[] pw)
			throws UnrecoverableKeyException {
		return this.setPublicSignatureKey(id.getPublicKeySign())
				.setPublicEncryptionKey(id.getPublicKeyEnc())
				.setPrivateEncryptionKey(id.getPrivateKeyEnc(pw))
				.setPrivateSignatureKey(id.getPrivateKeySign(pw));
	}

	public VolumeParams setPrivateDeviceKey(PrivateKey privateKeyForDevice) {
		this.devicePrivateKey = privateKeyForDevice;
		return this;
	}
}
