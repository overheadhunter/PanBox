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
package org.panbox.desktop.common.gui.addressbook;

import java.security.PublicKey;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.table.TableModel;

import org.panbox.core.Utils;
import org.panbox.core.csp.StorageBackendType;
import org.panbox.core.identitymgmt.AbstractIdentity;
import org.panbox.core.identitymgmt.CloudProviderInfo;
import org.panbox.core.identitymgmt.IPerson;
import org.panbox.core.identitymgmt.PanboxContact;

public class PanboxGUIContact {

	private IPerson contact = null;

	public static Icon contactIcon;

	/**
	 * helper method returning the e-mail address for the given contacts as a
	 * separated value list. if no e-mail has been configured for the CSP
	 * indicated by backend, the default value is being added
	 * 
	 * @param contacts
	 * @param separator
	 * @param backend
	 * @return
	 */
	public static String getMailAsSepteratedValues(
			List<PanboxGUIContact> contacts, String separator,
			StorageBackendType backend) {
		StringBuffer emails = new StringBuffer();

		for (int i = 0; i < contacts.size(); i++) {
			PanboxGUIContact c = contacts.get(i);
			CloudProviderInfo info = c.getCloudProvider(backend
					.getDisplayName());
			if (info != null) {
				String mail = c.getCloudProvider(backend.getDisplayName())
						.getUsername();
				if ((mail != null) && !mail.isEmpty()) {
					emails.append(mail);
					if (i < contacts.size() - 1) {
						emails.append(separator);
					}
				} else {
					emails.append(c.getEmail());
					if (i < contacts.size() - 1) {
						emails.append(separator);
					}
				}
			} else {
				emails.append(c.getEmail());
				if (i < contacts.size() - 1) {
					emails.append(separator);
				}
			}
		}
		return emails.toString();
	}

	// public PanboxGUIContact(String firstName, String lastName, String email)
	// {
	// super();
	// this.firstName = firstName;
	// this.name = lastName;
	// this.email = email;
	// }
	//
	// public PanboxGUIContact(String firstName, String lastName, String email,
	// X509Certificate certEnc, X509Certificate certSign) {
	// super();
	// this.firstName = firstName;
	// this.name = lastName;
	// this.email = email;
	// this.certEnc = certEnc;
	// this.certSign = certSign;
	// }

	/**
	 * Constructor for creating model over
	 * {@link org.panbox.core.identitymgmt.IPerson}-instance from identity
	 * management.
	 */
	public PanboxGUIContact(IPerson imContact) {

		this.contact = imContact;

		// this(imContact.getFirstName(), imContact.getName(), imContact
		// .getEmail(), imContact.getCertEnc(), imContact.getCertSign());

		// if (imContact instanceof org.panbox.core.identitymgmt.PanboxContact)
		// {
		// this.cloudProviders = ((org.panbox.core.identitymgmt.PanboxContact)
		// imContact).getCloudProviders();
		// }
	}

	public Icon getIcon() {
		if (contactIcon == null) {
			contactIcon = new ImageIcon(getClass().getResource("contact.png"),
					"Dropbox Icon");
		}
		return contactIcon;
	}

	public String getCertEncFingerprint() {
		return Utils.getCertFingerprint(contact.getCertEnc());
	}

	public String getCertSignFingerprint() {
		return Utils.getCertFingerprint(contact.getCertSign());
	}

	

	public String getFromDate() {
		return formatDate(contact.getCertEnc().getNotBefore());
	}

	public String getUntilDate() {
		return formatDate(contact.getCertEnc().getNotAfter());
	}

	private String formatDate(Date date) {
		DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		return df.format(date);
	}

	public TableModel generateCspInfoTableModel() {
		CSPTableModel model = new CSPTableModel(contact.getCloudProviders()
				.size());
		for (CloudProviderInfo cspInfo : contact.getCloudProviders().values()) {
			model.addRow(new String[] { cspInfo.getProviderName(),
					cspInfo.getUsername() });
		}
		return model;
	}

	public IPerson getModel() {
		return this.contact;
	}

	public void addCloudProvider(CloudProviderInfo cloudProviderInfo) {
		this.contact.addCloudProvider(cloudProviderInfo);

	}

	public String getEmail() {
		return this.contact.getEmail();
	}

	public void removeCloudProvider(String csp) {
		this.contact.delCloudProvider(csp);
	}

	public String getFirstName() {
		return this.contact.getFirstName();
	}

	public String getName() {
		return this.contact.getName();
	}

	public CloudProviderInfo getCloudProvider(String cspName) {
		return this.contact.getCloudProviders().get(cspName);
	}

	public void setFirstName(String newFirstName) {
		this.contact.setFirstName(newFirstName);
	}

	public void setName(String newName) {
		this.contact.setName(newName);
	}

	public PublicKey getPublicKeySign() {
		return this.contact.getPublicKeySign();
	}

	public int getAvailableCSPs() {
		return StorageBackendType.values().length - 1
				- this.contact.getCloudProviders().size();
	}

	public boolean isVerified() {
		if (this.contact instanceof AbstractIdentity) {
			return true;
		} else if (this.contact instanceof PanboxContact) {
			return ((PanboxContact) this.contact).isVerified();
		}
		return false;
	}

	public void setTrustLevel(int level) {
		if (this.contact instanceof PanboxContact) {
			((PanboxContact) this.contact).setTrustLevel(level);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((contact == null) ? 0 : contact.hashCode());
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
		PanboxGUIContact other = (PanboxGUIContact) obj;
		if (contact == null) {
			if (other.contact != null)
				return false;
		} else if (!contact.equals(other.contact))
			return false;
		return true;
	}
	
	

}
