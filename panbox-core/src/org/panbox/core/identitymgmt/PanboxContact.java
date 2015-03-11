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
package org.panbox.core.identitymgmt;

import java.security.PublicKey;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map.Entry;

public class PanboxContact implements IPerson {

	protected String name = "";
	protected String firstName = "";
	protected String email = "";

	private int id = -1;

	protected X509Certificate certEnc;
	protected X509Certificate certSign;

	protected int trustLevel = -1;

	protected HashMap<String, CloudProviderInfo> cloudProviders = new HashMap<String, CloudProviderInfo>();
	public static final int UNTRUSTED_CONTACT = 0;
	public static final int TRUSTED_CONTACT = 1;

	public void setName(String name) {
		this.name = name;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getName() {
		return name;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getFirstName() {
		return this.firstName;
	}

	public String getEmail() {
		return this.email;
	}

	public PublicKey getPublicKeyEnc() {
		if (this.certEnc != null) {
			// return null if cert is not valid
			try {
				certEnc.checkValidity();
			} catch (CertificateExpiredException e) {
				return null;
			} catch (CertificateNotYetValidException e) {
				return null;
			}

			return this.certEnc.getPublicKey();
		}

		return null;
	}

	public PublicKey getPublicKeySign() {

		if (this.certSign != null) {
			// return null if cert is not valid
			try {
				certSign.checkValidity();
			} catch (CertificateExpiredException e) {
				return null;
			} catch (CertificateNotYetValidException e) {
				return null;
			}

			return this.certSign.getPublicKey();
		}

		return null;
	}

	public void setCertEnc(X509Certificate cert) {
		this.certEnc = cert;
	}

	@Override
	public X509Certificate getCertEnc() {
		return this.certEnc;
	}

	public void setCertSign(X509Certificate cert) {
		this.certSign = cert;
	}

	@Override
	public X509Certificate getCertSign() {
		return this.certSign;
	}

	public void addCloudProvider(CloudProviderInfo cpi) {
		if (!cloudProviders.containsKey(cpi.getProviderName())) {
			cloudProviders.put(cpi.getProviderName(), cpi);
		}
	}

	public void removeCloudProvider(CloudProviderInfo cpi) {
		if (cloudProviders.containsKey(cpi.getProviderName())) {
			cloudProviders.remove(cpi.getProviderName());
		}
	}

	public CloudProviderInfo getCloudProvider(String name) {
		return cloudProviders.get(name);
	}

	public HashMap<String, CloudProviderInfo> getCloudProviders() {
		return cloudProviders;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("Name: " + firstName + " " + name + "\n");
		sb.append("Email: " + email + "\n");
		sb.append("TrustLevel: " + trustLevel + "\n");
		sb.append("Sign-Cert: " + certSign + "\n");
		sb.append("Enc-Cert: " + certEnc + "\n");

		sb.append("CloudProviders: (" + cloudProviders.size() + ")\n");
		for (Entry<String, CloudProviderInfo> cpiEntry : cloudProviders
				.entrySet()) {
			sb.append("CPI: " + cpiEntry.getKey() + ": "
					+ cpiEntry.getValue().getUsername() + "\n");
		}

		return sb.toString();
	}

	@Override
	public int getID() {
		return this.id;
	}

	@Override
	public void setID(int id) {
		this.id = id;
	}

	@Override
	public void delCloudProvider(String providerName) {
		this.cloudProviders.remove(providerName);

	}

	public int getTrustLevel() {
		return trustLevel;
	}

	public void setTrustLevel(int trustLevel) {
		this.trustLevel = trustLevel;
	}

	public boolean isVerified() {
		return (this.getTrustLevel() == TRUSTED_CONTACT);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((certEnc == null) ? 0 : certEnc.hashCode());
		result = prime * result
				+ ((certSign == null) ? 0 : certSign.hashCode());
		result = prime * result
				+ ((cloudProviders == null) ? 0 : cloudProviders.hashCode());
		result = prime * result + ((email == null) ? 0 : email.hashCode());
		result = prime * result
				+ ((firstName == null) ? 0 : firstName.hashCode());
		result = prime * result + id;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + trustLevel;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PanboxContact other = (PanboxContact) obj;
		if (certEnc == null) {
			if (other.certEnc != null)
				return false;
		} else if (!certEnc.equals(other.certEnc))
			return false;
		if (certSign == null) {
			if (other.certSign != null)
				return false;
		} else if (!certSign.equals(other.certSign))
			return false;
		if (cloudProviders == null) {
			if (other.cloudProviders != null)
				return false;
		} else if (!cloudProviders.equals(other.cloudProviders))
			return false;
		if (email == null) {
			if (other.email != null)
				return false;
		} else if (!email.equals(other.email))
			return false;
		if (firstName == null) {
			if (other.firstName != null)
				return false;
		} else if (!firstName.equals(other.firstName))
			return false;
		if (id != other.id)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (trustLevel != other.trustLevel)
			return false;
		return true;
	}

	// @Override
	// public boolean equals(Object obj) {
	// if (!(obj instanceof PanboxContact))
	// return false;
	//
	// PanboxContact other = (PanboxContact) obj;
	//
	// if (!this.email.equals(other.getEmail()))
	// return false;
	//
	// if (!this.firstName.equals(other.getFirstName()))
	// return false;
	//
	// if (!this.name.equals(other.getName()))
	// return false;
	//
	// if (this.certEnc != null && other.getCertEnc() != null) {
	// if (!Arrays.equals(this.certEnc.getSignature(), other.getCertEnc()
	// .getSignature()))
	// return false;
	// }
	//
	// if (this.certSign != null && other.getCertSign() != null) {
	// if (!Arrays.equals(this.certSign.getSignature(), other
	// .getCertSign().getSignature()))
	// return false;
	// }
	//
	// for (Entry<String, CloudProviderInfo> cpiEntry : cloudProviders
	// .entrySet()) {
	// if (null == other.getCloudProvider(cpiEntry.getKey())) {
	// return false;
	// }
	// }
	//
	// return true;
	// }

}
