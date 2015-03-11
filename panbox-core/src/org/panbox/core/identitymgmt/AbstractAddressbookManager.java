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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.panbox.core.crypto.CryptCore;
import org.panbox.core.crypto.KeyConstants;
import org.panbox.core.identitymgmt.exceptions.ContactExistsException;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.io.text.VCardReader;
import ezvcard.property.Email;
import ezvcard.property.Key;
import ezvcard.property.RawProperty;
import ezvcard.property.Role;
import ezvcard.property.StructuredName;

public abstract class AbstractAddressbookManager {

	private static final String PANBOX_CP_EXTENSION = "X-PanboxCP";

	private static final Logger logger = Logger.getLogger("org.panbox.core");

	public abstract void init();

	public abstract void loadContacts(AbstractIdentity id);

	public abstract void persistContacts(Collection<PanboxContact> contacts,
			int identityKey);

	/**
	 * Writes all contacts into a vCard file represented by vcardFile
	 * 
	 * @param id
	 *            Identity to export contacts from
	 * @param vcardFile
	 *            File to store vcard entries
	 * @return true if export is successful, otherwise false
	 */
	public static boolean exportContacts(Collection<VCard> vcards,
			File vcardFile) {
		try {
			Ezvcard.write(vcards).go(vcardFile);
		} catch (IOException e) {
			logger.error(
					"Could not export contacts to file: "
							+ vcardFile.getAbsolutePath() + File.separator
							+ vcardFile.getName(), e);

			return false;
		}
		return true;
	}

	public List<PanboxContact> importContacts(AbstractIdentity id,
			VCard[] vcards, boolean authVerified) throws ContactExistsException {

		LinkedList<PanboxContact> importedContacts = new LinkedList<PanboxContact>();

		if (vcards != null) {
			IAddressbook addressbook = id.getAddressbook();
			LinkedList<PanboxContact> existingContacts = new LinkedList<PanboxContact>();

			for (VCard vc : vcards) {

				PanboxContact curContact = vcard2Contact(vc, authVerified);

				PublicKey pk = curContact.getPublicKeySign();
				PanboxContact refContact = addressbook
						.getContactBySignaturePubKey(pk);

				// if contact does not exist create it, otherwise update it
				if (refContact == null) {

					// don't import the contact if it has the same public
					// sign/enc key as my own identity
					if (Arrays.equals(id.getCertSign().getPublicKey()
							.getEncoded(), pk.getEncoded())
							|| (Arrays.equals(id.getCertEnc().getPublicKey()
									.getEncoded(), curContact.getPublicKeyEnc()
									.getEncoded()))) {
						logger.warn("VCard entry matched identity, skipping...");
						continue;
					}

					addressbook.addContact(curContact);
					// to return imported contacts
					importedContacts.add(curContact);
				} else {
					// if we have this contact in our addressbook as untrusted
					// and now got a verified trusted contact -> update
					// trust level
					if (authVerified
							&& (curContact.getTrustLevel() == PanboxContact.TRUSTED_CONTACT)) {
						refContact.setTrustLevel(PanboxContact.TRUSTED_CONTACT);
					}
					existingContacts.add(curContact);
				}
			}

			if (!existingContacts.isEmpty()) {
				throw new ContactExistsException(
						"One or more contacts already existed in the addressbook.",
						existingContacts);
			}

		}

		return importedContacts;
	}

	public List<PanboxContact> importContacts(AbstractIdentity id,
			File vcardFile, boolean authVerified) throws ContactExistsException {
		return importContacts(id, readVCardFile(vcardFile), authVerified);
	}

	public List<PanboxContact> importContacts(AbstractIdentity id,
			byte[] rawData, boolean authVerified) throws ContactExistsException {
		return importContacts(id, readVCardBytes(rawData), authVerified);
	}

	public static VCard[] readVCardBytes(byte[] rawData) {
		ArrayList<VCard> vcards = new ArrayList<VCard>();
		VCardReader vcr = null;
		try {
			vcr = new VCardReader(new ByteArrayInputStream(rawData));
			VCard vc = null;
			while ((vc = vcr.readNext()) != null) {
				vcards.add(vc);
			}
			return vcards.toArray(new VCard[vcards.size()]);
		} catch (IOException e) {
			logger.error("Error reading VCard byte[] data", e);
		} finally {
			if (vcr != null) {
				try {
					vcr.close();
				} catch (IOException e) {
					// do nothing
				}
			}
		}
		return null;
	}

	public static VCard[] readVCardFile(File vcardFile) {
		ArrayList<VCard> vcards = new ArrayList<VCard>();
		VCardReader vcr = null;
		try {
			vcr = new VCardReader(vcardFile);
			VCard vc = null;
			while ((vc = vcr.readNext()) != null) {
				vcards.add(vc);
			}
			return vcards.toArray(new VCard[vcards.size()]);
		} catch (IOException e) {
			logger.error(
					"Error reading VCard file " + vcardFile.getAbsolutePath(),
					e);
		} finally {
			if (vcr != null) {
				try {
					vcr.close();
				} catch (IOException e) {
					// do nothing
				}
			}
		}
		return null;
	}

	/**
	 * Imports contacts from a given vcard file vcardFile to the addressbook of
	 * the identity id
	 * 
	 * @param id
	 *            - Identity to add contacts to
	 * @param vcardFile
	 *            - vcard file containing the contacts
	 * @return true if successful
	 * @throws ContactExistsException
	 *             - with list of contacts that have been skipped because they
	 *             already exist
	 */
	public static PanboxContact vcard2Contact(VCard vc, boolean authVerified) {
		// we only support ONE email address per contact
		// (first one in list)!
		String email = vc.getEmails().get(0).getValue();

		PanboxContact ret = new PanboxContact();

		if (authVerified) {
			// PersonRole role = getRoleFromVCard(vc);
			// if (role == PersonRole.IDENTITY) {
			// // always trust identity, if verified
			// ret.setTrustLevel(PanboxContact.TRUSTED_CONTACT);
			// } else if ((role == PersonRole.CONTACT)
			// && (getTrustLevelFromVCard(vc) == PanboxContact.TRUSTED_CONTACT))
			// {
			// // transitive trust for contacts, if verified
			// ret.setTrustLevel(PanboxContact.TRUSTED_CONTACT);
			// } else {
			// ret.setTrustLevel(PanboxContact.UNTRUSTED_CONTACT);
			// }

			// for now, the trust-flag should *only* indicate if the user has
			// verified this import
			ret.setTrustLevel(PanboxContact.TRUSTED_CONTACT);
		} else {
			// never trust anything unverified
			ret.setTrustLevel(PanboxContact.UNTRUSTED_CONTACT);
		}

		ret.setEmail(email);

		StructuredName sn = vc.getStructuredName();
		ret.setName(sn.getFamily());
		ret.setFirstName(sn.getGiven());

		ret.setCertEnc(getPublicEncCertFromVCard(vc));
		ret.setCertSign(getPublicSigCertFromVCard(vc));

		List<RawProperty> cps = vc.getExtendedProperties(PANBOX_CP_EXTENSION);
		for (RawProperty cp : cps) {

			CloudProviderInfo cpi = new CloudProviderInfo(
					cp.getParameter("TYPE"), cp.getParameter("USERNAME"));
			ret.addCloudProvider(cpi);
		}

		return ret;
	}

	/**
	 * Converts a given contact into a Vcard instance
	 * 
	 * @param c
	 *            - contact
	 * @return the vcard object of the contact, or null if an error occurs
	 */
	public static VCard contact2VCard(IPerson c) {
		VCard vcard = new VCard();

		StructuredName n = new StructuredName();
		n.setFamily(c.getName());
		n.setGiven(c.getFirstName());
		vcard.setStructuredName(n);

		vcard.setFormattedName(c.getFirstName() + " " + c.getName());

		Email email = new Email(c.getEmail());
		vcard.setProperty(Email.class, email);

		// this is a contact, not an identity
		if (c instanceof PanboxContact) {
			vcard.addRole("contact");
			PanboxContact pc = (PanboxContact) c;
			vcard.addExtendedProperty(KeyConstants.AB_CONTACT_TRUSTLEVEL,
					String.valueOf(pc.getTrustLevel()));
		} else if (c instanceof AbstractIdentity) {
			vcard.addRole("identity");
		}

		Key key = new Key();
		try {
			key.setData(c.getCertEnc().getEncoded(),
					KeyConstants.AB_CONTACT_PK_ENC);
		} catch (CertificateEncodingException e) {
			logger.error(
					"Cannot obtain encoded version of user's encryption certificate",
					e);
			return null;
		}
		vcard.addKey(key);

		try {
			key = new Key(c.getCertSign().getEncoded(),
					KeyConstants.AB_CONTACT_PK_SIG);
		} catch (CertificateEncodingException e) {
			logger.error(
					"Cannot obtain encoded version of user's signature certificate",
					e);
			return null;
		}
		vcard.addKey(key);

		for (Entry<String, CloudProviderInfo> cpiEntry : c.getCloudProviders()
				.entrySet()) {
			RawProperty rp = vcard.addExtendedProperty(PANBOX_CP_EXTENSION, "");
			rp.addParameter("TYPE", cpiEntry.getKey());
			rp.addParameter("USERNAME", cpiEntry.getValue().getUsername());
		}

		return vcard;
	}

	public static X509Certificate getPublicSigCertFromVCard(VCard vc) {
		for (Key k : vc.getKeys()) {
			if (k.getContentType() == KeyConstants.AB_CONTACT_PK_SIG) {
				return CryptCore.createCertificateFromBytes(k.getData());
			}
		}
		return null;
	}

	public static X509Certificate getPublicEncCertFromVCard(VCard vc) {
		for (Key k : vc.getKeys()) {
			if (k.getContentType() == KeyConstants.AB_CONTACT_PK_ENC) {
				return CryptCore.createCertificateFromBytes(k.getData());
			}
		}
		return null;
	}

	public static int getTrustLevelFromVCard(VCard vc) {
		RawProperty rawProperty = vc
				.getExtendedProperty(KeyConstants.AB_CONTACT_TRUSTLEVEL);
		return Integer.valueOf(rawProperty.getValue());
	}

	public static PersonRole getRoleFromVCard(VCard vc) {
		PersonRole role = PersonRole.NONE;
		List<Role> roles = vc.getRoles();
		
		if ((null != roles) && (roles.size() == 1)) {
			// we assume there is be only one role defined per vcard
			Role ref = roles.get(0);
			if (ref.getValue().equals("contact")) {
				role = PersonRole.CONTACT;
			} else if (ref.getValue().equals("identity")) {
				role = PersonRole.IDENTITY;
			} else {
				role = PersonRole.NONE;
			}
		} else if ((null != roles) &&  roles.size() > 1) {
			logger.error("VCard contains more than one role!");
		}		
		return role;
	}

	public static List<PanboxContact> vcardBytes2Contacts(byte[] rawBytes,
			boolean verified) {

		LinkedList<PanboxContact> contacts = new LinkedList<PanboxContact>();

		VCard[] vcards = readVCardBytes(rawBytes);

		for (VCard vc : vcards) {
			contacts.add(vcard2Contact(vc, verified));
		}

		return contacts;
	}

}
