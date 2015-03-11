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

import org.panbox.core.identitymgmt.IPerson;

import javax.swing.*;

public class PanboxMyContact extends PanboxGUIContact {

	public static Icon contactIcon;

//	public PanboxMyContact(String firstName, String lastName, String email) {
//		super(firstName, lastName, email);
//	}

	/**
	 * @param imContact
	 */
	public PanboxMyContact(IPerson imContact) {
		super(imContact);
//		AbstractIdentity identity = (AbstractIdentity) imContact;
//		for (CloudProviderInfo cpi : identity.getCloudProviders()) {
//			this.cloudProviders.put(cpi.getProviderName(), cpi);
//		}
	}

	@Override
	public Icon getIcon() {
		if (contactIcon == null) {
			contactIcon = new ImageIcon(getClass()
					.getResource("contact-my.png"), "Identity Icon");
		}
		return contactIcon;
	}

//	public TableModel generateCspInfoTableModel() {
//		CSPTableModel model = new CSPTableModel(cloudProviders.size());
//		for (CloudProviderInfo cspInfo : cloudProviders.values()) {
//			model.addRow(new String[]{cspInfo.getProviderName(), cspInfo.getUsername()});
//		}
//		return model;
//	}

}
