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
import java.security.cert.X509Certificate;
import java.util.HashMap;

public interface IPerson {

	public String getEmail();
	
	public String getName();
	
	public String getFirstName();
	
	public PublicKey getPublicKeyEnc();
	
	public PublicKey getPublicKeySign();
	
	public X509Certificate getCertSign();
	
	public X509Certificate getCertEnc();
	
	/**
	 * Represents the ID when stored in a database, otherwise its -1
	 */
	public int getID();
	
	/**
	 * Set the ID when storing the identity in a database to the ID representing the database entry
	 */
	public void setID(int id);
	
	public HashMap<String, CloudProviderInfo> getCloudProviders();
	
	public void addCloudProvider(CloudProviderInfo cloudProviderInfo);
	
	public void delCloudProvider(String name);
	
	public void setFirstName(String newFirstName);
	
	public void setName(String newName);
	
}
