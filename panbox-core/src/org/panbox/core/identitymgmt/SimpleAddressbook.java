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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import org.panbox.core.identitymgmt.exceptions.ContactExistsException;

public class SimpleAddressbook implements IAddressbook {

	// mapping from emailaddress to contact
	private HashMap<String, PanboxContact> contacts = new HashMap<String, PanboxContact>();

	@Override
	public void addContact(PanboxContact contact) throws ContactExistsException{
		
		if(contactExists(contact.getEmail()) != null)
		{
			LinkedList<PanboxContact> contacts = new LinkedList<PanboxContact>();
			contacts.add(contact);
			ContactExistsException ce = new ContactExistsException("Contact with Email " + contact.getEmail() + " already exists", contacts);
			throw ce;
		}
		
		contacts.put(contact.getEmail(), contact);
	}

	@Override
	public int size() {
		return contacts.size();
	}

	@Override
	public Collection<PanboxContact> getContacts() {
		return contacts.values();
	}

	@Override
	public boolean deleteContact(String email) {
		if (contacts.containsKey(email)) {
			contacts.remove(email);
			return true;
		} else
			return false; // contact was not in addressbook
	}

	@Override
	public PanboxContact contactExists(String email) {

		if (contacts.containsKey(email)) {
			return contacts.get(email);
		}

		return null;
	}

	@Override
	public PanboxContact getContactBySignaturePubKey(PublicKey pubSigKey) {
		for (PanboxContact c : this.contacts.values()) {

			if (c.getPublicKeySign().equals(pubSigKey)) {
				return c;
			}
		}
		return null;
	}

}
