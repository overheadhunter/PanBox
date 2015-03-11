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
package org.panbox.desktop.common.gui;

import ezvcard.VCard;
import ezvcard.property.StructuredName;

import org.apache.log4j.Logger;
import org.panbox.Settings;
import org.panbox.core.identitymgmt.AbstractAddressbookManager;
import org.panbox.core.identitymgmt.VCardProtector;
import org.panbox.desktop.common.PanboxClient;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * @author palige
 * 
 *         Dialog for importing contacts from a VCard
 */
public class ImportIdentitiesDialog extends javax.swing.JDialog {

	private static final String VERIFICATION_OK_LABEL = "\u2714";
	private static final String VERIFICATION_FAILED_LABEL = "\u2718";

	private final static Logger logger = Logger
			.getLogger(ImportIdentitiesDialog.class);

	private static final long serialVersionUID = 7665762037632230830L;
	private PanboxClient client;

	private static final ResourceBundle bundle = ResourceBundle.getBundle(
			"org/panbox/desktop/common/gui/Messages", Settings.getInstance()
					.getLocale()); // NOI18N

	private DocumentListener contactLoadingListener = new DocumentListener() {

		@Override
		public void removeUpdate(DocumentEvent e) {
			checkCurrentPIN();
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			checkCurrentPIN();
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			checkCurrentPIN();
		}
	};

	private ListSelectionListener contactSelectionListener = new ListSelectionListener() {

		@Override
		public void valueChanged(ListSelectionEvent e) {
			checkImportability();
		}
	};

	private class VCardNameTableCellRenderer extends DefaultTableCellRenderer {

		/**
		 * 
		 */
		private static final long serialVersionUID = 6913177900407781180L;

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {

			if (value instanceof VCard) {
				VCard vc = (VCard) value;
				StructuredName sn = vc.getStructuredName();
				String name = sn.getGiven() + " " + sn.getFamily() + " ("
						+ vc.getEmails().get(0).getValue() + ")";
				return super.getTableCellRendererComponent(table, name,
						isSelected, hasFocus, row, column);
			} else {
				return super.getTableCellRendererComponent(table, value,
						isSelected, hasFocus, row, column);
			}
		}
	}

	// private class VCardVerificationStatusCellRenderer extends
	// DefaultTableCellRenderer {
	//
	// /**
	// *
	// */
	// private static final long serialVersionUID = -8334626838871672490L;
	//
	// @Override
	// public Component getTableCellRendererComponent(JTable table,
	// Object value, boolean isSelected, boolean hasFocus, int row,
	// int column) {
	// if (value instanceof VCard) {
	// VCard vc = (VCard) value;
	// PersonRole role = AbstractAddressbookManager
	// .getRoleFromVCard(vc);
	//
	// boolean check = false;
	// if (vcVerificationState) {
	// check = (role == PersonRole.IDENTITY) ? true
	// : (AbstractAddressbookManager
	// .getTrustLevelFromVCard(vc) == PanboxContact.TRUSTED_CONTACT);
	// }
	//
	// String verified = check ? bundle
	// .getString("PanboxClientGUI.contact.verified") : bundle
	// .getString("PanboxClientGUI.contact.not.verified");
	//
	// Component cell = super.getTableCellRendererComponent(table,
	// verified, isSelected, hasFocus, row, column);
	//
	// if (check) {
	// cell.setForeground(PanboxConstants.verifiedColor);
	// } else {
	// cell.setForeground(PanboxConstants.notVerifiedColor);
	// setFont(getFont().deriveFont(Font.BOLD));
	// }
	// setHorizontalAlignment(JLabel.CENTER);
	//
	// return cell;
	// } else {
	// return super.getTableCellRendererComponent(table, value,
	// isSelected, hasFocus, row, column);
	// }
	// }
	// }

	/**
	 * Creates new form ImportIdentitiesDialog
	 */
	public ImportIdentitiesDialog(PanboxClient client) {
		super(client.getMainWindow());
		this.client = client;
		initComponents();

		importPINTextField.getDocument().addDocumentListener(
				contactLoadingListener);
		// fileLocTextField.getDocument().addDocumentListener(
		// contactLoadingListener);
		// importContactList.getSelectionModel().addListSelectionListener(
		// contactSelectionListener);

		importContactsTable.getSelectionModel().addListSelectionListener(
				contactSelectionListener);
	}

	public ImportIdentitiesDialog(PanboxClient client, File identityFile) {
		this(client);
		fileLocTextField.setText(identityFile.getAbsolutePath());
		loadVCardFile(identityFile);
		checkCurrentPIN();
	}

	private void checkCurrentPIN() {
		if (this.VCFContents != null) {
			if (ignorePINCheckbox.isSelected()) {
				pinCheckStateLabel.setText(VERIFICATION_FAILED_LABEL);
				pinCheckStateLabel
						.setForeground(PanboxDesktopGUIConstants.notVerifiedColor);
				// pinCheckStateLabel.setFont(getFont().deriveFont(Font.BOLD));
				isVCFVerified = false;
				checkImportability();
				return;
			} else {
				// check current pin value
				if ((importPINTextField.getText() != null)
						&& (importPINTextField.getText().length() >= 4)) {
					boolean ispinvalid = VCardProtector.verifyVCFIntegrity(
							VCFContents.extracedVCF, VCFContents.storedHMac,
							importPINTextField.getText().toCharArray());
					if (ispinvalid) {
						pinCheckStateLabel.setText(VERIFICATION_OK_LABEL);
						pinCheckStateLabel
								.setForeground(PanboxDesktopGUIConstants.verifiedColor);
						// pinCheckStateLabel.setFont(getFont().deriveFont(
						// Font.BOLD));
						isVCFVerified = true;
						checkImportability();
						return;
					}
				}
			}
		}
		// default
		pinCheckStateLabel.setText(VERIFICATION_FAILED_LABEL);
		pinCheckStateLabel
				.setForeground(PanboxDesktopGUIConstants.notVerifiedColor);
		// pinCheckStateLabel.setFont(getFont().deriveFont(Font.BOLD));
		isVCFVerified = false;
		importButton.setEnabled(false);
	}

	private void checkImportability() {
		if (isVCFVerified || (ignorePINCheckbox.isSelected())) {
			ListSelectionModel model = (ListSelectionModel) importContactsTable
					.getSelectionModel();

			if ((importContactsTable.getRowCount() > 0)
					&& (!model.isSelectionEmpty())) {
				importButton.setEnabled(true);
				return;
			}
		}
		importButton.setEnabled(false);
	}

	private class VCFArchive {

		public VCFArchive(byte[] storedHMac, byte[] extracedVCF) {
			this.storedHMac = storedHMac;
			this.extracedVCF = extracedVCF;
		}

		byte[] storedHMac;
		byte[] extracedVCF;
	}

	// /**
	// * helper method for checking if all prerequisites for loading contacts
	// from
	// * the VCard zip archive are being met
	// */
	// private void checkLoadability() {
	// // check if load button should be disabled or enabled
	// if (((this.fileLocTextField.getText() != null)
	// && ((this.fileLocTextField.getText().length() != 0))
	// && (importPINTextField.getText() != null) && (importPINTextField
	// .getText().length() >= 4)) || (ignorePINCheckbox.isSelected())) {
	// loadContactsButton.setEnabled(true);
	// } else {
	// loadContactsButton.setEnabled(false);
	// }
	//
	// // remove list contents, if there are any
	// if (importContactsTable.getModel().getRowCount() > 0) {
	// ((DefaultTableModel) importContactsTable.getModel()).setRowCount(0);
	// }
	// vcVerificationState = false;
	// importButton.setEnabled(false);
	// }

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		fileLocLabel = new javax.swing.JLabel();
		importPINLabel = new javax.swing.JLabel();
		fileLocTextField = new javax.swing.JTextField();
		importPINTextField = new javax.swing.JTextField();
		fileLocButton = new javax.swing.JButton();
		verificationStateLabel = new javax.swing.JLabel();
		ignorePINCheckbox = new javax.swing.JCheckBox();
		contactsPanel = new javax.swing.JPanel();
		jScrollPane2 = new javax.swing.JScrollPane();
		importContactsTable = new javax.swing.JTable();
		importButton = new javax.swing.JButton();
		cancelButton = new javax.swing.JButton();
		pinCheckStateLabel = new javax.swing.JLabel();

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		setTitle(bundle.getString("ImportIdentitiesDialog.title")); // NOI18N
		setModal(true);
		setResizable(false);

		fileLocLabel.setText(bundle.getString("ImportIdentitiesDialog.fileLocLabel")); // NOI18N

		importPINLabel.setText(bundle.getString("ImportIdentitiesDialog.importPINLabel")); // NOI18N

		fileLocTextField.setEditable(false);
		fileLocTextField.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				fileLocTextFieldActionPerformed(evt);
			}
		});

		fileLocButton.setText(bundle.getString("ImportIdentitiesDialog.fileLocButton")); // NOI18N
		fileLocButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				fileLocButtonActionPerformed(evt);
			}
		});

		ignorePINCheckbox.setText(bundle.getString("ImportIdentitiesDialog.importPINCheckbox")); // NOI18N
		ignorePINCheckbox.addItemListener(new java.awt.event.ItemListener() {
			public void itemStateChanged(java.awt.event.ItemEvent evt) {
				ignorePINCheckboxItemStateChanged(evt);
			}
		});
		ignorePINCheckbox.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				ignorePINCheckboxActionPerformed(evt);
			}
		});

		contactsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("ImportIdentitiesDialog.contactsPanel.title"))); // NOI18N

		importContactsTable.setModel(new javax.swing.table.DefaultTableModel(
				new Object[][]{

				},
				new String[]{
						"Contact name"
				}
		) {
			private static final long serialVersionUID = -3383450872951099560L;
			
			@SuppressWarnings("rawtypes")
			Class[] types = new Class[]{
					java.lang.String.class
			};
			boolean[] canEdit = new boolean[]{
					false
			};

			public Class<?> getColumnClass(int columnIndex) {
				return types[columnIndex];
			}

			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return canEdit[columnIndex];
			}
		});
		importContactsTable.getTableHeader().setReorderingAllowed(false);
		jScrollPane2.setViewportView(importContactsTable);
		importContactsTable.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		if (importContactsTable.getColumnModel().getColumnCount() > 0) {
			importContactsTable.getColumnModel().getColumn(0).setResizable(false);
			importContactsTable.getColumnModel().getColumn(0).setPreferredWidth(140);
			importContactsTable.getColumnModel().getColumn(0).setHeaderValue(bundle.getString("ImportIdentitiesDialog.importContactsTable.columnname")); // NOI18N
			importContactsTable.getColumnModel().getColumn(0).setCellRenderer(new VCardNameTableCellRenderer());
		}

		javax.swing.GroupLayout contactsPanelLayout = new javax.swing.GroupLayout(contactsPanel);
		contactsPanel.setLayout(contactsPanelLayout);
		contactsPanelLayout.setHorizontalGroup(
				contactsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(contactsPanelLayout.createSequentialGroup()
								.addContainerGap()
								.addComponent(jScrollPane2)
								.addContainerGap())
		);
		contactsPanelLayout.setVerticalGroup(
				contactsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(contactsPanelLayout.createSequentialGroup()
								.addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE)
								.addContainerGap())
		);

		importButton.setText(bundle.getString("ImportIdentitiesDialog.importButton")); // NOI18N
		importButton.setEnabled(false);
		importButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				importButtonActionPerformed(evt);
			}
		});

		cancelButton.setText(bundle.getString("ImportIdentitiesDialog.cancelButton")); // NOI18N
		cancelButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				cancelButtonActionPerformed(evt);
			}
		});

		pinCheckStateLabel.setFont(new java.awt.Font("Dialog", 1, 24)); // NOI18N
		pinCheckStateLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(
				layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(layout.createSequentialGroup()
								.addContainerGap()
								.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
										.addGroup(layout.createSequentialGroup()
												.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
														.addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
																.addGap(387, 387, 387)
																.addComponent(verificationStateLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
														.addGroup(layout.createSequentialGroup()
																.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
																		.addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
																				.addComponent(fileLocLabel)
																				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																				.addComponent(fileLocTextField))
																		.addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
																				.addComponent(importPINLabel)
																				.addGap(6, 6, 6)
																				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
																						.addGroup(layout.createSequentialGroup()
																								.addComponent(ignorePINCheckbox)
																								.addGap(0, 0, Short.MAX_VALUE))
																						.addComponent(importPINTextField))))
																.addGap(6, 6, 6)))
												.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
												.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
														.addComponent(pinCheckStateLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE)
														.addComponent(fileLocButton, javax.swing.GroupLayout.Alignment.TRAILING)))
										.addComponent(contactsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
										.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
												.addComponent(importButton)
												.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
												.addComponent(cancelButton)))
								.addContainerGap())
		);
		layout.setVerticalGroup(
				layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
								.addContainerGap()
								.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
										.addComponent(fileLocLabel)
										.addComponent(fileLocTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
										.addComponent(fileLocButton))
								.addGap(15, 15, 15)
								.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
										.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
												.addComponent(importPINLabel)
												.addComponent(importPINTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
										.addComponent(pinCheckStateLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE))
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
								.addComponent(ignorePINCheckbox)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(contactsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(verificationStateLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
										.addComponent(cancelButton)
										.addComponent(importButton))
								.addContainerGap())
		);

		pack();
	}// </editor-fold>//GEN-END:initComponents

	private boolean isVCFVerified;
	private VCFArchive VCFContents = null;

	// private void loadContactsButtonActionPerformed(
	// java.awt.event.ActionEvent evt) {//
	// GEN-FIRST:event_loadContactsButtonActionPerformed
	// try {
	// VCard[] vclist = null;
	// File sourceFile = new File(fileLocTextField.getText());
	// File vcardFile = File.createTempFile(
	// "panbox-tmp-" + String.valueOf(System.currentTimeMillis()),
	// ".vcf");
	// vcardFile.deleteOnExit();
	//
	// char[] pin = ignorePINCheckbox.isSelected() ? null
	// : importPINTextField.getText().toCharArray();
	//
	// byte[] hmac = VCardProtector.unwrapVCF(sourceFile, vcardFile);
	//
	// vcVerificationState = VCardProtector.verifyVCFIntegrity(vcardFile,
	// hmac, pin);
	//
	// // user either provided the correct pin or chose to ignore
	// // verification
	// if ((vcVerificationState == true) || (pin == null)) {
	// // only continue if there are any VCards ..
	// if (((vclist = AbstractAddressbookManager
	// .readVCardFile(vcardFile)) != null)
	// && (vclist.length > 0)) {
	// // remove all existing entries before loading any new ones
	// // remove list contents, if there are any
	// DefaultTableModel tableModel = (DefaultTableModel) importContactsTable
	// .getModel();
	// if (importContactsTable.getModel().getRowCount() > 0) {
	// tableModel.setRowCount(0);
	// }
	//
	// for (VCard vc : vclist) {
	// tableModel.addRow(new Object[] { vc, vc });
	// }
	// } else {
	// JOptionPane.showMessageDialog(this,
	// bundle.getString("cannot.read.contacts.from.file"),
	// bundle.getString("AddShareDialog.errorTitle"),
	// JOptionPane.ERROR_MESSAGE);
	// }
	// } else {
	// JOptionPane.showMessageDialog(this,
	// bundle.getString("import.pin.could.not.be.validated"),
	// bundle.getString("AddShareDialog.errorTitle"),
	// JOptionPane.ERROR_MESSAGE);
	// }
	// } catch (Exception e) {
	// logger.error(bundle
	// .getString("could.not.read.contacts.or.unwrap.vcf.file"), e);
	// JOptionPane.showMessageDialog(this,
	// bundle.getString("error.reading.contacts.file"),
	// bundle.getString("AddShareDialog.errorTitle"),
	// JOptionPane.ERROR_MESSAGE);
	// }
	// }// GEN-LAST:event_loadContactsButtonActionPerformed

	private void ignorePINCheckboxItemStateChanged(java.awt.event.ItemEvent evt) {// GEN-FIRST:event_ignorePINCheckboxItemStateChanged

	}// GEN-LAST:event_ignorePINCheckboxItemStateChanged

	private void fileLocTextFieldActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_fileLocTextFieldActionPerformed
		// TODO add your handling code here:
	}// GEN-LAST:event_fileLocTextFieldActionPerformed

	private void loadVCardFile(File vcardFile) {
		if (!vcardFile.exists() || !vcardFile.canRead()) {
			JOptionPane.showMessageDialog(this,
					bundle.getString("cannot.read.vcard.file"),
					bundle.getString("error"), JOptionPane.ERROR_MESSAGE);
		} else {
			File tmpFile;
			try {
				tmpFile = File.createTempFile(
						"panbox-tmp-"
								+ String.valueOf(System.currentTimeMillis()),
						".vcf");
				byte[] hmac = VCardProtector.unwrapVCF(vcardFile, tmpFile);

				// load table values
				VCard[] vclist = null;
				// only continue if there are any VCards ..
				if (((vclist = AbstractAddressbookManager
						.readVCardFile(tmpFile)) != null)
						&& (vclist.length > 0)) {
					// remove all existing entries before loading any
					// new ones
					DefaultTableModel tableModel = (DefaultTableModel) importContactsTable
							.getModel();

					tableModel.setRowCount(0);

					for (VCard vc : vclist) {
						tableModel.addRow(new Object[] { vc });
					}
					byte[] vcfbytes = VCardProtector.loadVCFBytes(tmpFile);
					this.VCFContents = new VCFArchive(hmac, vcfbytes);

				} else {
					JOptionPane.showMessageDialog(this,
							bundle.getString("error.reading.contacts.file"),
							bundle.getString("error"),
							JOptionPane.ERROR_MESSAGE);
				}

				this.fileLocTextField.setText(vcardFile.getAbsolutePath());
			} catch (IOException e) {
				logger.error("Could not read VCF archive!", e);
				JOptionPane.showMessageDialog(this,
						bundle.getString("cannot.read.vcard.file"),
						bundle.getString("error"), JOptionPane.ERROR_MESSAGE);
				this.fileLocTextField.setText("");
				this.VCFContents = null;
			}
		}
	}

	private void fileLocButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_fileLocButtonActionPerformed
		JFileChooser fileChooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
				"Zip Archive (.zip)", "zip");
		fileChooser.setFileFilter(filter);
		fileChooser.setMultiSelectionEnabled(false);

		if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			loadVCardFile(fileChooser.getSelectedFile());

		}

		checkCurrentPIN();
	}// GEN-LAST:event_fileLocButtonActionPerformed

	private void importButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_importButtonActionPerformed
		// get selected items from list and try to start import ..
		List<VCard> selectedContacts = new ArrayList<VCard>();
		DefaultTableModel tableModel = (DefaultTableModel) importContactsTable
				.getModel();
		if (tableModel.getRowCount() == 0) {
			JOptionPane.showMessageDialog(this,
					bundle.getString("list.of.contacts.is.empty"),
					bundle.getString("error"), JOptionPane.ERROR_MESSAGE);
		} else {
			int[] sel = importContactsTable.getSelectedRows();
			for (int i = 0; i < sel.length; i++) {
				VCard v = (VCard) tableModel.getValueAt(sel[i], 0);
				selectedContacts.add(v);
			}

			if (selectedContacts.size() == 0) {
				JOptionPane.showMessageDialog(this,
						bundle.getString("no.contacts.selected"),
						bundle.getString("error"), JOptionPane.ERROR_MESSAGE);
			} else {
				// start import of selected contacts
				client.importContacts(selectedContacts
						.toArray(new VCard[selectedContacts.size()]),
						isVCFVerified);
				this.dispose();
			}
			for (VCard vc : selectedContacts) {
				System.out.println(vc.getStructuredName().getFamily());
			}
		}

	}// GEN-LAST:event_importButtonActionPerformed

	private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cancelButtonActionPerformed
		this.dispose();
	}// GEN-LAST:event_cancelButtonActionPerformed

	private void ignorePINCheckboxActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_ignorePINCheckboxActionPerformed
		if (!ignorePINCheckbox.isSelected()) {
			importPINTextField.setEnabled(true);
		} else {
			if (JOptionPane
					.showConfirmDialog(
							this,
							bundle.getString("ImportIdentitiesDialog.continueWithoutEnteringPin"),
							bundle.getString("ImportIdentitiesDialog.verificationWarning"),
							JOptionPane.YES_NO_OPTION,
							JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
				importPINTextField.setText("");
				importPINTextField.setEnabled(false);
				ignorePINCheckbox.setSelected(true);
				isVCFVerified = false;
			} else {
				ignorePINCheckbox.setSelected(false);
			}
		}
		checkCurrentPIN();
	}// GEN-LAST:event_ignorePINCheckboxActionPerformed

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JButton cancelButton;
	private javax.swing.JPanel contactsPanel;
	private javax.swing.JButton fileLocButton;
	private javax.swing.JLabel fileLocLabel;
	private javax.swing.JTextField fileLocTextField;
	private javax.swing.JCheckBox ignorePINCheckbox;
	private javax.swing.JButton importButton;
	private javax.swing.JTable importContactsTable;
	private javax.swing.JLabel importPINLabel;
	private javax.swing.JTextField importPINTextField;
	private javax.swing.JScrollPane jScrollPane2;
	private javax.swing.JLabel pinCheckStateLabel;
	private javax.swing.JLabel verificationStateLabel;
	// End of variables declaration//GEN-END:variables
}
