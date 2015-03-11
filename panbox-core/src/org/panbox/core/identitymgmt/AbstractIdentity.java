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

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.util.Collection;
import java.util.HashMap;

public abstract class AbstractIdentity implements IPerson {

	protected String name;
	protected String firstName;
	protected String email;
	protected int id = -1;
	protected final IAddressbook addressbook;
	// private final LinkedList<CloudProviderInfo> cloudProviders = new
	// LinkedList<CloudProviderInfo>();
	private HashMap<String, CloudProviderInfo> cloudProviders = new HashMap<String, CloudProviderInfo>();

	public AbstractIdentity(IAddressbook addressbook) {
		this.addressbook = addressbook;
	}

	public AbstractIdentity(String email, String firstName, String name,
			IAddressbook addressbook) {
		this.addressbook = addressbook;
		this.firstName = firstName;
		this.name = name;
		this.email = email;
	}

	/**
	 * Stores the owner key for signing of this identity and protects it by the
	 * given password
	 * 
	 * @param ownerKeySign
	 *            - Keypair representing the owner key for signing
	 * @param password
	 *            - to protect the private key
	 */
	public abstract void setOwnerKeySign(KeyPair ownerKeySign, char[] password);

	/**
	 * Stores only the public owner key for signing
	 * 
	 * @param cert
	 */
	public abstract void setOwnerKeySign(Certificate cert);

	/**
	 * Stores only the public owner key for encryption
	 * 
	 * @param cert
	 */
	public abstract void setOwnerKeyEnc(Certificate cert);

	/**
	 * Stores the owner key for encryption of this identity and protects it by
	 * the given password
	 * 
	 * @param ownerKeyEnc
	 *            - Keypair representing the owner key for encryption
	 * @param password
	 *            - to protect the private key
	 */
	public abstract void setOwnerKeyEnc(KeyPair ownerKeyEnc, char[] password);

	/**
	 * Stores a given device key of this identity and protects it by the given
	 * password
	 * 
	 * @param password
	 *            - to protect the private key
	 * @param deviceName
	 *            - name of the device where the key will be used
	 */
	public abstract void addDeviceKey(KeyPair deviceKey, String deviceName);

	public abstract void addDeviceKey(KeyPair deviceKey,
			Certificate deviceCert, String deviceName);

	/**
	 * Stores a certificate from a second device in our identity (i.e. we are
	 * running on a laptop and get the certificate from our mobile)
	 * 
	 * @param cert
	 *            - Certificate of the other device
	 * @param deviceName
	 *            - Name of the other device
	 */
	public abstract void addDeviceCert(Certificate cert, String deviceName);

	/**
	 * Loads a certificate from a second device in our identity (i.e. we are
	 * running on a laptop and get the certificate from our mobile)
	 * 
	 * @param deviceName
	 *            - Name of the other device
	 * @return - Certificate of the other device
	 */
	public abstract Certificate getDeviceCert(String deviceName);

	/**
	 * Returns the addressbook of the identity
	 * 
	 * @return Addressbook of this identity
	 */
	public IAddressbook getAddressbook() {
		return this.addressbook;
	}

	/**
	 * Retrieve the private owner key for signing of the identity
	 * 
	 * @param password
	 *            - Password to unlock the private key
	 * @return - Private Key for signing
	 * @throws UnrecoverableKeyException
	 */
	public abstract PrivateKey getPrivateKeySign(char[] password)
			throws UnrecoverableKeyException;

	/**
	 * Retrieve the private owner key for encryption of the identity
	 * 
	 * @param password
	 *            - Password to unlock the private key
	 * @return - Private key for encryption
	 * @throws UnrecoverableKeyException
	 */
	public abstract PrivateKey getPrivateKeyEnc(char[] password)
			throws UnrecoverableKeyException;

	/**
	 * Retrieve the public owner key for encryption
	 * 
	 * @return - public owner key for encryption
	 */
	public abstract PublicKey getPublicKeyEnc();

	/**
	 * Retrieve the public owner key for signing
	 * 
	 * @return - public owner key for signing
	 */
	public abstract PublicKey getPublicKeySign();

	/**
	 * Retrieve the private key of a device with name deviceName
	 * 
	 * @param password
	 *            - password to unlock the key
	 * @param deviceName
	 *            - name of the device to retrieve the private key for
	 * @return - private key for deviceName or null if we run on a different
	 *         device
	 */
	public abstract PrivateKey getPrivateKeyForDevice(String deviceName);

	/**
	 * Retrieve the public key of a device with name deviceName
	 * 
	 * @param deviceName
	 *            - name of the device to retrieve the public key for
	 * @return - public key of device deviceName
	 * @throws UnrecoverableKeyException
	 */
	public abstract PublicKey getPublicKeyForDevice(String deviceName)
			throws UnrecoverableKeyException;

	/**
	 * Sets the email address of the identity
	 * 
	 * @param email
	 *            E-Mail of the identity
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Sets the name of the identity
	 * 
	 * @param name
	 *            Name of the identity
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the first name of the identity
	 * 
	 * @return First name of the identity
	 */
	public String getFirstName() {
		return firstName;
	}

	/**
	 * Sets the first name of the identity
	 * 
	 * @param firstName
	 *            First name of the identity
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	/**
	 * Gets the name of the identity
	 * 
	 * @return Name of the identity
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the email address of the identity
	 * 
	 * @return E-Mail of the identity
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * Gets the cloud provider information of the identity
	 * 
	 * @return Cloud storage information of the identity
	 */
	@Override
	public HashMap<String, CloudProviderInfo> getCloudProviders() {
		return cloudProviders;
	}

	/**
	 * Adds a new cloud provider information to the identity
	 * 
	 * @param cp
	 *            New cloud providers information
	 */
	public void addCloudProvider(CloudProviderInfo cp) {
		this.cloudProviders.put(cp.getProviderName(), cp);
	}

	/**
	 * Remove a CloudProvider from the identity
	 * 
	 * @param cp
	 */
	public void delCloudProvider(String providerName) {
		this.cloudProviders.remove(providerName);
	}

	/**
	 * Alternative way to remove a cloudprovider from the identity deleting the
	 * csp with matching provider name
	 *
	 * @param cp
	 */
	public void delCloudProviderByProviderName(CloudProviderInfo cp) {
		CloudProviderInfo toRemove = null;
		for (CloudProviderInfo cpi : this.cloudProviders.values()) {
			if (cpi.getProviderName().equals(cp.getProviderName())) {
				toRemove = cpi;
				break;
			}
		}
		if (toRemove != null) {
			this.cloudProviders.remove(toRemove.getProviderName());
		}
	}

	/**
	 * Deletes a contact via its email from the Identity's addressbook
	 * 
	 * @param id
	 * @param email
	 * @return true if successful
	 */
	public boolean deleteContact(String email) {
		if (addressbook == null || email == null) {
			return false;
		}

		return addressbook.deleteContact(email);
	}

	/**
	 * Check whether certificates are valid
	 * 
	 * @return true if encryption AND signature certificates are valid
	 */
	public abstract boolean checkCertificateValidity();

	public String toString() {
		return firstName + " " + name + " " + email + " CP-count: "
				+ cloudProviders.size();
	}

	@Override
	public int getID() {
		return id;
	}

	@Override
	public void setID(int id) {
		this.id = id;
	}

	public abstract Collection<String> getDeviceList();

	public PanboxContact resolveContactPublicKey(PublicKey pk, String alias) {
		PanboxContact contact = getAddressbook()
				.getContactBySignaturePubKey(pk);
		if (contact == null) {
			if (!getPublicKeySign().equals(pk)) {
				contact = new UnknownContact(pk, alias);
			}
		}
		return contact;
	}

}
