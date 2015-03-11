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
package org.panbox.core.pairing;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;

import org.panbox.core.identitymgmt.CloudProviderInfo;
import org.panbox.core.identitymgmt.IPerson;

public class PairingIPersonDummy implements IPerson {
	
	private final String eMail;
	private final String firstName;
	private final String lastName;
	
	public PairingIPersonDummy(String eMail, String firstName, String lastName) {
		this.eMail = eMail;
		this.firstName = firstName;
		this.lastName = lastName;
	}
	
	@Override
	public String getEmail() {
		return eMail;
	}

	@Override
	public String getName() {
		return lastName;
	}

	@Override
	public String getFirstName() {
		return firstName;
	}

	@Override
	public PublicKey getPublicKeyEnc() {
		return null;
	}

	@Override
	public PublicKey getPublicKeySign() {
		return null;
	}

	@Override
	public X509Certificate getCertSign() {
		return null;
	}

	@Override
	public X509Certificate getCertEnc() {
		return null;
	}

	@Override
	public int getID() {
		return 0;
	}

	@Override
	public void setID(int id) {
	}

	@Override
	public HashMap<String, CloudProviderInfo> getCloudProviders() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addCloudProvider(CloudProviderInfo cloudProviderInfo) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delCloudProvider(String name) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setFirstName(String newFirstName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setName(String newName) {
		// TODO Auto-generated method stub
		
	}
	
}