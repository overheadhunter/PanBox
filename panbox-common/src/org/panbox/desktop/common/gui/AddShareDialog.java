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

import java.awt.HeadlessException;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.security.UnrecoverableKeyException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileView;

import org.apache.log4j.Logger;
import org.panbox.PanboxConstants;
import org.panbox.Settings;
import org.panbox.core.csp.CSPAdapterFactory;
import org.panbox.core.csp.StorageBackendType;
import org.panbox.core.exception.ShareMetaDataException;
import org.panbox.desktop.common.gui.shares.DropboxPanboxShare;
import org.panbox.desktop.common.gui.shares.FolderPanboxShare;
import org.panbox.desktop.common.gui.shares.PanboxShare;
import org.panbox.desktop.common.sharemgmt.ShareManagerException;
import org.panbox.desktop.common.sharemgmt.ShareManagerImpl;
import org.panbox.desktop.common.vfs.backend.dropbox.DropboxAdapterFactory;
import org.panbox.desktop.common.vfs.backend.dropbox.DropboxClientIntegration;

public class AddShareDialog extends javax.swing.JDialog {

	/**
     *
     */
	private static final long serialVersionUID = -8323088984757291309L;

	private final static Logger logger = Logger.getLogger(AddShareDialog.class);

	// no effect on initComponents
	private static ResourceBundle bundle = ResourceBundle.getBundle(
			"org.panbox.desktop.common.gui.Messages", Settings.getInstance()
					.getLocale());

	private DefaultComboBoxModel<Object> comboBoxModel = new DefaultComboBoxModel<Object>();
	private PanboxShare share;
	private boolean aborted = false;

	public static boolean shareNameUserAction = false;

	private DropboxClientIntegration dbClientIntegration;
	private boolean dropboxBrowseButtonClicked = false;

	private boolean nameWasAutoSet = true;

	/**
	 * Creates new form AddShareDialog
	 */
	public AddShareDialog(java.awt.Frame parent) {
		this(parent, null, null);
	}

	public AddShareDialog(java.awt.Frame parent, StorageBackendType type,
			File preselectedshare) {
		super(parent);
		DropboxAdapterFactory dbxFac = (DropboxAdapterFactory) CSPAdapterFactory
				.getInstance(StorageBackendType.DROPBOX);
		this.dbClientIntegration = (DropboxClientIntegration) dbxFac
				.getClientAdapter();
		initComponents();

		try {
			if (dbClientIntegration.isClientInstalled()) {
				reInitDropboxShareList();
			}
		} catch (IOException e) {
			// somc error with the dropbox list occurred. Will ignore them.
		}

		if (type != null) {
			typeComboBox.setSelectedItem(type.getDisplayName());
		} else {
			typeComboBoxActionPerformed(null);
		}

		if (preselectedshare != null) {
			DropboxShare ref = new DropboxShare(preselectedshare.getName(),
					preselectedshare.getAbsolutePath());
			dropboxSharesComboBox.setSelectedItem(ref);
		}

		// If in textfield someone types manually a name, then do not switch
		// names!
		nameTextField.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
				nameWasAutoSet = false;
			}

			@Override
			public void keyReleased(KeyEvent e) {
				nameWasAutoSet = false;
			}

			@Override
			public void keyPressed(KeyEvent e) {
				nameWasAutoSet = false;
			}
		});
	}

	private void reInitDropboxShareList() {
		comboBoxModel.removeAllElements();
		comboBoxModel.addElement(bundle.getString("AddShareDialog.select"));
		ArrayList<DropboxShare> shares = getDropboxShares();
		for (DropboxShare s : shares) {
			try {
				if (ShareManagerImpl.getInstance().sharePathAvailable(
						s.getPath())) {
					comboBoxModel.addElement(s);
				}
			} catch (ShareManagerException | UnrecoverableKeyException
					| ShareMetaDataException e) {
				logger.error("Error checking if share path is available", e);
				JOptionPane.showMessageDialog(this,
						bundle.getString("AddShareDialog.checkPathAvailable"),
						bundle.getString("error"), JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private String[] getSupportedShareTypes() {
		ArrayList<String> list = new ArrayList<String>();
		list.add(StorageBackendType.FOLDER.getDisplayName());
		try {
			if (dbClientIntegration.isClientInstalled()) {
				list.add(StorageBackendType.DROPBOX.getDisplayName());
			}
		} catch (IOException e) {
			logger.error("Error checking Dropbox installation", e);
		}
		return (String[]) list.toArray(new String[list.size()]);
	}

	private ArrayList<DropboxShare> getDropboxShares() {
		File dropboxSyncDir;
		ArrayList<DropboxShare> result = new ArrayList<>();
		try {
			dropboxSyncDir = dbClientIntegration.getCurrentSyncDir();
			if (dropboxSyncDir != null) {

				File[] sortedFiles = dropboxSyncDir.listFiles();
				Arrays.sort(sortedFiles);
				for (File f : sortedFiles) {
					if ((new File(f.getAbsolutePath() + File.separator
							+ PanboxConstants.PANBOX_SHARE_METADATA_DIRECTORY)
							.exists())) {
						result.add(new DropboxShare(f.getName(), f
								.getAbsolutePath()));
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	private class DropboxShare {

		String name, path;

		public DropboxShare(String name, String path) {
			this.name = name;
			this.path = path;
		}

		public String toString() {
			return this.name;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((path == null) ? 0 : path.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			DropboxShare other = (DropboxShare) obj;
			if (!getOuterType().equals(other.getOuterType())) {
				return false;
			}
			if (name == null) {
				if (other.name != null) {
					return false;
				}
			} else if (!name.equals(other.name)) {
				return false;
			}
			if (path == null) {
				if (other.path != null) {
					return false;
				}
			} else if (!path.equals(other.path)) {
				return false;
			}
			return true;
		}

		public String getPath() {
			return this.path;
		}

		private AddShareDialog getOuterType() {
			return AddShareDialog.this;
		}
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed"
	// desc="Generated Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		typeComboBox = new javax.swing.JComboBox<String>();
		typeLabel = new javax.swing.JLabel();
		nameTextField = new javax.swing.JTextField();
		nameLabel = new javax.swing.JLabel();
		abortButton = new javax.swing.JButton();
		okButton = new javax.swing.JButton();
		dropboxPanel = new javax.swing.JPanel();
		dropboxSharesComboBox = new javax.swing.JComboBox<Object>();
		dropboxSharesLabel = new javax.swing.JLabel();
		dropboxNewButton = new javax.swing.JButton();
		dropboxBrowseButton = new javax.swing.JButton();
		directoryPanel = new javax.swing.JPanel();
		directoryTextLabel = new javax.swing.JLabel();
		directoryTextField = new javax.swing.JTextField();
		directoryChooseButton = new javax.swing.JButton();

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		setTitle(bundle.getString("AddShareDialog.title")); // NOI18N
		setModal(true);

		typeComboBox.setModel(new DefaultComboBoxModel<String>(
				getSupportedShareTypes()));
		typeComboBox.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				typeComboBoxActionPerformed(evt);
			}
		});

		typeLabel.setText(bundle.getString("AddShareDialog.typeLabel.text")); // NOI18N

		nameLabel.setText(bundle.getString("AddShareDialog.nameLabel.text")); // NOI18N

		abortButton
				.setText(bundle.getString("AddShareDialog.abortButton.text")); // NOI18N
		abortButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				abortButtonActionPerformed(evt);
			}
		});

		okButton.setText(bundle.getString("AddShareDialog.okButton.text")); // NOI18N
		okButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				okButtonActionPerformed(evt);
			}
		});

		dropboxSharesComboBox.setModel(this.comboBoxModel);
		dropboxSharesComboBox
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						dropboxSharesComboBoxActionPerformed(evt);
					}
				});

		dropboxSharesLabel.setText(bundle
				.getString("AddShareDialog.dropboxSharesLabel.text")); // NOI18N

		dropboxNewButton.setText(bundle.getString("AddShareDialog.new"));
		dropboxNewButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				dropboxNewButtonActionPerformed(evt);
			}
		});

		dropboxBrowseButton.setText(bundle.getString("AddShareDialog.browse"));
		dropboxBrowseButton
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						dropboxBrowseButtonActionPerformed(evt);
					}
				});

		javax.swing.GroupLayout dropboxPanelLayout = new javax.swing.GroupLayout(
				dropboxPanel);
		dropboxPanel.setLayout(dropboxPanelLayout);
		dropboxPanelLayout
				.setHorizontalGroup(dropboxPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								javax.swing.GroupLayout.Alignment.TRAILING,
								dropboxPanelLayout
										.createSequentialGroup()
										.addComponent(
												dropboxSharesLabel,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												130,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addComponent(dropboxSharesComboBox, 0,
												140, Short.MAX_VALUE)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(dropboxBrowseButton)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(dropboxNewButton)));
		dropboxPanelLayout
				.setVerticalGroup(dropboxPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								dropboxPanelLayout
										.createParallelGroup(
												javax.swing.GroupLayout.Alignment.BASELINE)
										.addComponent(
												dropboxSharesComboBox,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addComponent(dropboxSharesLabel)
										.addComponent(dropboxNewButton)
										.addComponent(dropboxBrowseButton)));

		directoryTextLabel.setText(bundle
				.getString("AddShareDialog.directoryTextLabel.text")); // NOI18N

		directoryChooseButton.setText(bundle
				.getString("AddShareDialog.directoryChooseButton.text")); // NOI18N
		directoryChooseButton
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						directoryChooseButtonActionPerformed(evt);
					}
				});

		javax.swing.GroupLayout directoryPanelLayout = new javax.swing.GroupLayout(
				directoryPanel);
		directoryPanel.setLayout(directoryPanelLayout);
		directoryPanelLayout
				.setHorizontalGroup(directoryPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								javax.swing.GroupLayout.Alignment.TRAILING,
								directoryPanelLayout
										.createSequentialGroup()
										.addComponent(
												directoryTextLabel,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												130,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addComponent(directoryTextField)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(directoryChooseButton)));
		directoryPanelLayout
				.setVerticalGroup(directoryPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								directoryPanelLayout
										.createParallelGroup(
												javax.swing.GroupLayout.Alignment.BASELINE)
										.addComponent(
												directoryTextField,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addComponent(directoryTextLabel)
										.addComponent(directoryChooseButton)));

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(
				getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(layout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(
						layout.createSequentialGroup()
								.addContainerGap()
								.addGroup(
										layout.createParallelGroup(
												javax.swing.GroupLayout.Alignment.LEADING)
												.addComponent(
														directoryPanel,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														Short.MAX_VALUE)
												.addComponent(
														dropboxPanel,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														Short.MAX_VALUE)
												.addGroup(
														layout.createSequentialGroup()
																.addGroup(
																		layout.createParallelGroup(
																				javax.swing.GroupLayout.Alignment.LEADING)
																				.addComponent(
																						typeLabel,
																						javax.swing.GroupLayout.PREFERRED_SIZE,
																						130,
																						javax.swing.GroupLayout.PREFERRED_SIZE)
																				.addComponent(
																						nameLabel))
																.addPreferredGap(
																		javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
																.addGroup(
																		layout.createParallelGroup(
																				javax.swing.GroupLayout.Alignment.LEADING)
																				.addComponent(
																						nameTextField)
																				.addComponent(
																						typeComboBox,
																						0,
																						javax.swing.GroupLayout.DEFAULT_SIZE,
																						Short.MAX_VALUE)))
												.addGroup(
														javax.swing.GroupLayout.Alignment.TRAILING,
														layout.createSequentialGroup()
																.addGap(0,
																		0,
																		Short.MAX_VALUE)
																.addComponent(
																		okButton)
																.addPreferredGap(
																		javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																.addComponent(
																		abortButton)))
								.addContainerGap()));
		layout.setVerticalGroup(layout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(
						layout.createSequentialGroup()
								.addContainerGap()
								.addGroup(
										layout.createParallelGroup(
												javax.swing.GroupLayout.Alignment.BASELINE)
												.addComponent(
														typeComboBox,
														javax.swing.GroupLayout.PREFERRED_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.PREFERRED_SIZE)
												.addComponent(typeLabel))
								.addPreferredGap(
										javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(dropboxPanel,
										javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE,
										javax.swing.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(
										javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(directoryPanel,
										javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE,
										javax.swing.GroupLayout.PREFERRED_SIZE)
								.addGap(3, 3, 3)
								.addGroup(
										layout.createParallelGroup(
												javax.swing.GroupLayout.Alignment.BASELINE)
												.addComponent(
														nameTextField,
														javax.swing.GroupLayout.PREFERRED_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.PREFERRED_SIZE)
												.addComponent(nameLabel))
								.addPreferredGap(
										javax.swing.LayoutStyle.ComponentPlacement.RELATED,
										javax.swing.GroupLayout.DEFAULT_SIZE,
										Short.MAX_VALUE)
								.addGroup(
										layout.createParallelGroup(
												javax.swing.GroupLayout.Alignment.BASELINE)
												.addComponent(abortButton)
												.addComponent(okButton))
								.addContainerGap()));

		pack();
	}// </editor-fold>//GEN-END:initComponents

	private void dropboxNewButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_dropboxNewButtonActionPerformed

		try {
			final File dropboxSyncDir = dbClientIntegration.getCurrentSyncDir();
			if (dropboxSyncDir != null) {
				final JTextField shareNameField = new JTextField(
						bundle.getString("AddShareDialog.pleaseEnterNewName"));

				shareNameField.addFocusListener(new FocusAdapter() {
					@Override
					public void focusGained(FocusEvent e) {

						if (!AddShareDialog.shareNameUserAction) {
							shareNameField.setText("");
							AddShareDialog.shareNameUserAction = true;
						}
					}
				});

				int retVal = JOptionPane.showConfirmDialog(this,
						shareNameField,
						bundle.getString("AddShareDialog.addNewDropboxShare"),
						JOptionPane.OK_CANCEL_OPTION);

				if (retVal == JOptionPane.OK_OPTION
						&& AddShareDialog.shareNameUserAction
						&& !shareNameField.getText().equals("")) {
					File newShareFolder = new File(dropboxSyncDir,
							shareNameField.getText());
					if (newShareFolder.exists()) {
						JOptionPane
								.showMessageDialog(
										this,
										MessageFormat.format(
												bundle.getString("AddShareDialog.shareWithNameExists"),
												shareNameField.getText()),
										bundle.getString("AddShareDialog.couldNotAddNewDropboxShare"),
										JOptionPane.INFORMATION_MESSAGE);
						logger.info("Adding new Dropbox share failed. File exists: "
								+ newShareFolder.getAbsolutePath());
					} else {
						if (dropboxBrowseButtonClicked) {
							comboBoxModel.removeElementAt(comboBoxModel
									.getSize() - 1);
						}
						comboBoxModel.addElement(new DropboxShare(
								newShareFolder.getName(), newShareFolder
										.getAbsolutePath()));
						dropboxSharesComboBox
								.setSelectedIndex(dropboxSharesComboBox
										.getItemCount() - 1);
						if (nameWasAutoSet) {
							nameTextField.setText(newShareFolder.getName());
						}
					}
				} else {
					logger.debug("Adding new Dropbox share has ben aborted.");
				}

				AddShareDialog.shareNameUserAction = false;
			}
		} catch (IOException e) {
			// ignore this
		}
	}// GEN-LAST:event_dropboxNewButtonActionPerformed

	private void dropboxBrowseButtonActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_dropboxBrowseButtonActionPerformed

		try {
			final File dropboxSyncDir = dbClientIntegration.getCurrentSyncDir();
			if (dropboxSyncDir != null) {
				JFileChooser fileChooser = new JFileChooser(dropboxSyncDir);
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				fileChooser.setFileView(new FileView() {
					@Override
					public Boolean isTraversable(File f) {
						return dropboxSyncDir.equals(f)
								|| dropboxSyncDir.equals(f.getParentFile());
					}
				});
				int ret = fileChooser.showOpenDialog(this);
				if (ret == JFileChooser.APPROVE_OPTION
						&& fileChooser.getSelectedFile().getParentFile()
								.equals(dropboxSyncDir)) {
					if (dropboxBrowseButtonClicked) {
						comboBoxModel
								.removeElementAt(comboBoxModel.getSize() - 1);
					}
					comboBoxModel.addElement(new DropboxShare(fileChooser
							.getSelectedFile().getName(), fileChooser
							.getSelectedFile().getAbsolutePath()));
					dropboxSharesComboBox
							.setSelectedIndex(dropboxSharesComboBox
									.getItemCount() - 1);
					if (nameWasAutoSet) {
						nameTextField.setText(fileChooser.getSelectedFile()
								.getName());
					}
					dropboxBrowseButtonClicked = true;
				}
			}
		} catch (IOException e) {
			// ignore this
		}
	}// GEN-LAST:event_dropboxBrowseButtonActionPerformed

	private void typeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_typeComboBoxActionPerformed
		if (typeComboBox.getSelectedItem().equals(
				StorageBackendType.DROPBOX.getDisplayName())) {
			directoryPanel.setVisible(false);
			dropboxPanel.setVisible(true);
		} else if (typeComboBox.getSelectedItem().equals(
				StorageBackendType.FOLDER.getDisplayName())) {
			directoryPanel.setVisible(true);
			dropboxPanel.setVisible(false);
		}
		// remove old share name
		nameWasAutoSet = true;
		nameTextField.setText("");
		nameTextField.invalidate();
	}// GEN-LAST:event_typeComboBoxActionPerformed

	private void abortButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_abortButtonActionPerformed
		aborted = true;
		this.dispose();
	}// GEN-LAST:event_abortButtonActionPerformed

	private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_okButtonActionPerformed
		String shareName = nameTextField.getText();
		try {
			if (shareName.equals("")) {
				JOptionPane.showMessageDialog(this, bundle
						.getString("AddShareDialog.directoryCannotBeEmpty"));
			} else if (!ShareManagerImpl.getInstance().shareNameAvailable(
					shareName)) {
				JOptionPane.showMessageDialog(this,
						bundle.getString("AddShareDialog.shareAlreadyExists"));
			} else if (typeComboBox.getSelectedItem().equals("Dropbox")) {
				String path = "";
				if (!dropboxSharesComboBox.getSelectedItem().equals(
						bundle.getString("AddShareDialog.select"))) {
					path = ((DropboxShare) dropboxSharesComboBox
							.getSelectedItem()).getPath();
					File ftmp = new File(path);
					if (ftmp.exists()
							&& !(new File(
									ftmp,
									PanboxConstants.PANBOX_SHARE_METADATA_DIRECTORY))
									.exists() && ftmp.list().length > 0) {
						JOptionPane
								.showMessageDialog(
										this,
										bundle.getString("AddShareDialog.ContainsIncompatibleFiles"));
					} else if (!ShareManagerImpl.getInstance()
							.sharePathAvailable(path)) {
						JOptionPane
								.showMessageDialog(
										this,
										bundle.getString("AddShareDialog.pathAlreadyAssigned"));
					} else {
						path = ((DropboxShare) dropboxSharesComboBox
								.getSelectedItem()).getPath();
						share = new DropboxPanboxShare(null, path,
								nameTextField.getText(), 0);
						this.dispose();
					}
				} else {
					JOptionPane
							.showMessageDialog(
									this,
									bundle.getString("AddShareDialog.directoryCannotBeEmpty"));
				}
			} else if (typeComboBox.getSelectedItem().equals("Directory")) {
				String path = directoryTextField.getText();
				File metadata = new File(path,
						PanboxConstants.PANBOX_SHARE_METADATA_DIRECTORY);
				if (!metadata.exists() && new File(path).list().length > 0) {
					JOptionPane
							.showMessageDialog(
									this,
									bundle.getString("AddShareDialog.ContainsIncompatibleFiles"));
				} else if (path.equals("")) {
					JOptionPane
							.showMessageDialog(
									this,
									bundle.getString("AddShareDialog.directoryCannotBeEmpty"));
				} else if (!ShareManagerImpl.getInstance().sharePathAvailable(
						path)) {
					JOptionPane.showMessageDialog(this, bundle
							.getString("AddShareDialog.pathAlreadyAssigned"));
				} else {
					File f = new File(path);
					if (!(f).exists()) { // in case someone just types a path
						// and expects it to be created
						f.mkdirs();
					}

					share = new FolderPanboxShare(null, path, shareName, 0);
					this.dispose();
				}
			}
		} catch (ShareManagerException | UnrecoverableKeyException
				| HeadlessException | ShareMetaDataException e) {
			JOptionPane.showMessageDialog(this,
					bundle.getString("AddShareDialog.errorAddingShare"),
					bundle.getString("error"), JOptionPane.ERROR_MESSAGE);
		}
	}// GEN-LAST:event_okButtonActionPerformed

	private void directoryChooseButtonActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_directoryChooseButtonActionPerformed
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int ret = fileChooser.showOpenDialog(this);
		if (ret == JFileChooser.APPROVE_OPTION) {
			directoryTextField.setText(fileChooser.getSelectedFile()
					.getAbsolutePath());
			directoryTextField.invalidate();
			if (nameTextField.getText().trim().length() == 0) {
				nameTextField.setText(fileChooser.getSelectedFile().getName());
				nameTextField.invalidate();
			}
		}
	}// GEN-LAST:event_directoryChooseButtonActionPerformed

	private void dropboxSharesComboBoxActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_sharesComboBoxActionPerformed
		try {
			final File dropboxSyncDir = dbClientIntegration.getCurrentSyncDir();
			if (dropboxSyncDir != null) {
				if (dropboxSharesComboBox.getSelectedItem().equals(
						bundle.getString("AddShareDialog.select"))) {
					// ignore this entry!
				} else {
					DropboxShare share = (DropboxShare) dropboxSharesComboBox
							.getSelectedItem();
					if (nameWasAutoSet) {
						nameTextField.setText(share.name);
						nameTextField.invalidate();
					}
				}
			} else {
				JOptionPane
						.showMessageDialog(
								this,
								bundle.getString("AddShareDialog.error.ReadingDropboxSyncDir"));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}// GEN-LAST:event_sharesComboBoxActionPerformed

	public PanboxShare getResult() throws OperationAbortedException {
		if (aborted || share == null) {
			throw new OperationAbortedException(
					"AddPanboxShare dialog has been canceled.");
		}
		return share;
	}

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JButton abortButton;
	private javax.swing.JButton directoryChooseButton;
	private javax.swing.JPanel directoryPanel;
	private javax.swing.JTextField directoryTextField;
	private javax.swing.JLabel directoryTextLabel;
	private javax.swing.JButton dropboxBrowseButton;
	private javax.swing.JButton dropboxNewButton;
	private javax.swing.JPanel dropboxPanel;
	private javax.swing.JComboBox<Object> dropboxSharesComboBox;
	private javax.swing.JLabel dropboxSharesLabel;
	private javax.swing.JLabel nameLabel;
	private javax.swing.JTextField nameTextField;
	private javax.swing.JButton okButton;
	private javax.swing.JComboBox<String> typeComboBox;
	private javax.swing.JLabel typeLabel;
	// End of variables declaration//GEN-END:variables
}
