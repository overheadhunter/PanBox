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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.UnrecoverableKeyException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;
import org.panbox.OS;
import org.panbox.Settings;
import org.panbox.core.Utils;
import org.panbox.core.csp.CSPAdapterFactory;
import org.panbox.core.csp.StorageBackendType;
import org.panbox.core.exception.ShareMetaDataException;
import org.panbox.core.identitymgmt.CloudProviderInfo;
import org.panbox.desktop.common.PanboxClient;
import org.panbox.desktop.common.gui.PasswordEnterDialog.PermissionType;
import org.panbox.desktop.common.gui.addressbook.CSPTableCellEditor;
import org.panbox.desktop.common.gui.addressbook.CSPTableModel;
import org.panbox.desktop.common.gui.addressbook.ContactListCellRenderer;
import org.panbox.desktop.common.gui.addressbook.ContactListModel;
import org.panbox.desktop.common.gui.addressbook.ContactShareParticipant;
import org.panbox.desktop.common.gui.addressbook.PanboxGUIContact;
import org.panbox.desktop.common.gui.addressbook.PanboxMyContact;
import org.panbox.desktop.common.gui.devices.AddDeviceBluetoothActionListener;
import org.panbox.desktop.common.gui.devices.AddDeviceFileActionListener;
import org.panbox.desktop.common.gui.devices.AddDeviceNetworkActionListener;
import org.panbox.desktop.common.gui.devices.DeviceListCellRenderer;
import org.panbox.desktop.common.gui.devices.DeviceListModel;
import org.panbox.desktop.common.gui.devices.DeviceShareParticipant;
import org.panbox.desktop.common.gui.devices.PanboxDevice;
import org.panbox.desktop.common.gui.shares.DropboxPanboxShare;
import org.panbox.desktop.common.gui.shares.FolderPanboxShare;
import org.panbox.desktop.common.gui.shares.PanboxShare;
import org.panbox.desktop.common.gui.shares.PanboxSharePermission;
import org.panbox.desktop.common.gui.shares.ShareListCellRenderer;
import org.panbox.desktop.common.gui.shares.ShareListModel;
import org.panbox.desktop.common.gui.shares.ShareParticipantListCellRenderer;
import org.panbox.desktop.common.gui.shares.ShareParticipantListModel;
import org.panbox.desktop.common.sharemgmt.ShareDoesNotExistException;
import org.panbox.desktop.common.sharemgmt.ShareManagerException;
import org.panbox.desktop.common.urihandler.PanboxURICmdShareInvitation;
import org.panbox.desktop.common.utils.DesktopApi;
import org.panbox.desktop.common.utils.FileUtils;
import org.panbox.desktop.common.utils.SupportedLanguage;
import org.panbox.desktop.common.vfs.backend.dropbox.CSPApiException;
import org.panbox.desktop.common.vfs.backend.dropbox.DropboxAPIIntegration;
import org.panbox.desktop.common.vfs.backend.dropbox.DropboxAdapterFactory;
import org.panbox.desktop.common.vfs.backend.dropbox.DropboxClientIntegration;

public class PanboxClientGUI extends javax.swing.JFrame {

	private static final long serialVersionUID = -8164912949809840568L;

	private final PanboxClient client;

	private final ShareListModel shareModel;

	private final ContactListModel contactModel;

	private final DeviceListModel deviceModel;

	private PanboxShare share;

	private PanboxGUIContact contact;

	private List<PanboxGUIContact> contacts;

	private PanboxDevice device;

	private final static Logger logger = Logger
			.getLogger(PanboxClientGUI.class);

	// no effect on initComponents
	private static final ResourceBundle bundle = ResourceBundle.getBundle(
			"org.panbox.desktop.common.gui.Messages", Settings.getInstance()
					.getLocale());

	private boolean unsavedContactChanges = false;
	private boolean unsavedSettings = false;
	private ArrayList<CloudProviderInfo> removedCSPs = new ArrayList<>();
	private int addedCSPCount = 0;
	private DropboxSettingsPanel dropboxSettingsPanel;

	public PanboxClientGUI(final PanboxClient client) {
		this.client = client;

		this.shareModel = client.getShareList();
		this.contactModel = client.getContactList();
		this.deviceModel = client.getDeviceList();

		initComponents();
		initSettingsConfig();

		// set the icon
		Toolkit kit = Toolkit.getDefaultToolkit();
		Image img = kit.createImage(getClass().getResource(
				"panbox-icon-big.png"));
		setIconImage(img);

		// set the default locale for popup messages
		JOptionPane.setDefaultLocale(Settings.getInstance().getLocale());

		// TODO: Hide these for now. Do we still need this?
		syncStatusLabel.setVisible(false);
		syncStatusTextField.setVisible(false);

		cspInfoTable
				.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE); // NOI18N

		shareList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				refreshShare();
			}
		});
		shareList.setSelectedIndex(0); // always try to select first share

		addressbookList.addListSelectionListener(new ListSelectionListener() {

			private DocumentListener firstNameFieldDocListener = new DocumentListener() {
				@Override
				public void insertUpdate(DocumentEvent e) {
					changed();
				}

				@Override
				public void removeUpdate(DocumentEvent e) {
					changed();
				}

				@Override
				public void changedUpdate(DocumentEvent e) {
				}

				public void changed() {
					if (!firstNameTextField.getText().equals(
							contact.getFirstName())
							&& !unsavedContactChanges) {
						setContactChangesDetected();
					}
				}
			};
			private DocumentListener lastNameFieldDocListener = new DocumentListener() {
				@Override
				public void insertUpdate(DocumentEvent e) {
					changed();
				}

				@Override
				public void removeUpdate(DocumentEvent e) {
					changed();
				}

				@Override
				public void changedUpdate(DocumentEvent e) {
				}

				public void changed() {
					if (!lastNameTextField.getText().equals(contact.getName())
							&& !unsavedContactChanges) {
						setContactChangesDetected();
					}
				}
			};
			private ListSelectionListener cspListSelectionListener = new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent e) {
					int selectedIndex = cspInfoTable.getSelectedRow();
					int max = ((CSPTableModel) cspInfoTable.getModel())
							.getMax();
					if (selectedIndex != -1 && selectedIndex < max) {
						removeCSPInfoButton.setEnabled(true);
					} else {
						removeCSPInfoButton.setEnabled(false);
					}
				}
			};

			boolean manuallySetSelection = false;

			@Override
			public void valueChanged(ListSelectionEvent e) {
				final List<PanboxGUIContact> selected = addressbookList
						.getSelectedValuesList();

				firstNameTextField.getDocument().removeDocumentListener(
						firstNameFieldDocListener);
				lastNameTextField.getDocument().removeDocumentListener(
						lastNameFieldDocListener);
				cspInfoTable.getSelectionModel().removeListSelectionListener(
						cspListSelectionListener);

				if (!manuallySetSelection) {
					if (!uneditedCSPsExist()) {
						if (unsavedContactChanges) {
							int saveUnchanged = JOptionPane.showConfirmDialog(
									null,
									bundle.getString("PanboxClientGUI.unsavedChangesToContact"), // NOI18N
									bundle.getString("PanboxClientGUI.panboxMessage"), // NOI18N
									JOptionPane.YES_NO_CANCEL_OPTION);
							if (saveUnchanged == JOptionPane.YES_OPTION) {
								saveContactChanges();
								refreshContact();
								resetContactApplyDiscardButtons();
							} else if (saveUnchanged == JOptionPane.CANCEL_OPTION) {
								manuallySetSelection = true;
								int previousIndex = e.getFirstIndex() == addressbookList
										.getSelectedIndex() ? e.getLastIndex()
										: e.getFirstIndex();
								addressbookList.setSelectedIndex(previousIndex);
							} else {
								refreshContact();
								resetContactApplyDiscardButtons();
							}
							unsavedContactChanges = false;
						}

						int selectedIndex = addressbookList.getSelectedIndex();
						if (selectedIndex != -1) {
							// enable Buttons
							removeContactButton.setEnabled(true);
							exportContactButton.setEnabled(true);

							// refresh contact infos
							contact = contactModel.getElementAt(selectedIndex);
							contacts = selected;
							firstNameTextField.setText(contact.getFirstName());
							lastNameTextField.setText(contact.getName());
							emailTextField.setText(contact.getEmail());
							cspInfoTable.setModel(contact
									.generateCspInfoTableModel());
							cspInfoTable.getSelectionModel()
									.addListSelectionListener(
											cspListSelectionListener);

							// show certificate info
							encKeyFprintTextField.setText(contact
									.getCertEncFingerprint());
							signKeyFprintTextField.setText(contact
									.getCertSignFingerprint());
							validFromUntilLabel.setText("Valid: "
									+ contact.getFromDate() + " - " // NOI18N
									+ contact.getUntilDate());

							// disable apply and discard buttons when contact
							// selection
							// changes
							contactApplyButton.setEnabled(false);
							contactDiscardButton.setEnabled(false);

							// disable csp add button when no further csps are
							// available to add
							if (contact.getAvailableCSPs() > 0) {
								addCSPInfoButton.setEnabled(true);
							} else {
								addCSPInfoButton.setEnabled(false);
							}
							removeCSPInfoButton.setEnabled(false);

							if (contact instanceof PanboxMyContact) {
								firstNameTextField.setEnabled(false);
								lastNameTextField.setEnabled(false);
								removeContactButton.setEnabled(false);
								contactVerificationStatusCheckBox
										.setEnabled(false);
							} else {
								firstNameTextField.setEnabled(true);
								lastNameTextField.setEnabled(true);
								removeContactButton.setEnabled(true);
								firstNameTextField.getDocument()
										.addDocumentListener(
												firstNameFieldDocListener);
								lastNameTextField.getDocument()
										.addDocumentListener(
												lastNameFieldDocListener);
								contactVerificationStatusCheckBox
										.setEnabled(true);
							}

						} else {
							// disable export and remove button when no item is
							// selected
							removeContactButton.setEnabled(false);
							exportContactButton.setEnabled(false);
						}
					} else {
						manuallySetSelection = true;
						int previousIndex = e.getFirstIndex() == addressbookList
								.getSelectedIndex() ? e.getLastIndex() : e
								.getFirstIndex();
						addressbookList.setSelectedIndex(previousIndex);
					}
				} else {
					manuallySetSelection = false;
				}

				if (contact.isVerified()) {
					contactVerificationStatusCheckBox.setSelected(true);
					contactVerificationStatusCheckBox.setText(bundle
							.getString("PanboxClientGUI.contact.verified"));
				} else {
					contactVerificationStatusCheckBox.setSelected(false);
					contactVerificationStatusCheckBox.setText(bundle
							.getString("PanboxClientGUI.contact.verified"));
				}
			}
		});
		addressbookList.setSelectedIndex(0); // always try to select first
		// contact

		// contact
		firstNameTextField.setDisabledTextColor(Color.BLACK);
		lastNameTextField.setDisabledTextColor(Color.BLACK);
		emailTextField.setDisabledTextColor(Color.BLACK);
		encKeyFprintTextField.setDisabledTextColor(Color.BLACK);
		signKeyFprintTextField.setDisabledTextColor(Color.BLACK);

		deviceList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				int selected = deviceList.getSelectedIndex();
				if (!e.getValueIsAdjusting() && selected != -1) {
					device = deviceModel.getElementAt(selected);

					deviceKeyFprintTextField.setText(device
							.getDevicePubKeyFingerprint());

					deviceShareList.setModel(client.getDeviceShares(device));
				}
			}
		});
		deviceList.setSelectedIndex(0); // always try to select first device

		// expert mode visible/invisible
		expertModeCheckBoxActionPerformed(null);

		ActionListener changesDetectedActionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setSettingsChangesDetected();
			}
		};

		languageComboBox.addActionListener(changesDetectedActionListener);
		expertModeCheckBox.addActionListener(changesDetectedActionListener);
		networkAddressComboBox.addActionListener(changesDetectedActionListener);
		networkInterfaceComboBox
				.addActionListener(changesDetectedActionListener);

		DocumentListener changesDetectedDocumentListener = new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				setSettingsChangesDetected();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				setSettingsChangesDetected();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {

			}
		};

		panboxFolderTextField.getDocument().addDocumentListener(
				changesDetectedDocumentListener);
		settingsFolderTextField.getDocument().addDocumentListener(
				changesDetectedDocumentListener);
		dropboxSettingsPanel = new DropboxSettingsPanel(
				changesDetectedActionListener, changesDetectedDocumentListener);

		// disable settings apply and discard buttons
		settingsApplyButton.setEnabled(false);
		settingsRevertButton.setEnabled(false);

		// TODO: add action and document listeners to the csp settings after it
		// has been fixed (see trac ticket #139)

		// Disable device pairing for SLAVE devices!
		if (Settings.getInstance().isSlave()) {
			addDeviceButton.setEnabled(false);
			addDeviceButton.setToolTipText(bundle
					.getString("client.disabledPairingSlave"));
			addDeviceButton.removeMouseListener(addDeviceButton
					.getMouseListeners()[0]);
		}

		// Don't show these in Windows!
		if (OS.getOperatingSystem().isWindows()) {
			panboxFolderLabel.setVisible(false);
			panboxFolderTextField.setVisible(false);
			panboxFolderChooseButton.setVisible(false);
		}
	}

	public void refreshShare() {
		int selected = shareList.getSelectedIndex();
		if (selected != -1) {
			share = shareModel.getElementAt(selected);
			ownerTextField.setText(share.isOwner() ? "Owner" : "User"); // NOI18N
			try {
				syncStatusTextField
						.setText(share.getSyncStatus() == 0 ? "Fully synchronized"
								: "Not fully synchronized"); // NOI18N
			} catch (NullPointerException ex) {
				syncStatusTextField.setText("Unknown synchronization state"); // NOI18N
			}
			if (share instanceof DropboxPanboxShare) {
				DropboxPanboxShare dbitem = (DropboxPanboxShare) share;
				urlTextField.setText(dbitem.getPath());
			} else if (share instanceof FolderPanboxShare) {
				FolderPanboxShare folderitem = (FolderPanboxShare) share;
				urlTextField.setText(folderitem.getPath());
			}

			usersList.setModel(client.getShareParticipantListForShare(share));

			removeShare.setEnabled(true);

			if (share instanceof DropboxPanboxShare) {
				restoreRevButton.setEnabled(true);
			} else {
				restoreRevButton.setEnabled(false);
			}
		} else {
			removeShare.setEnabled(false);
			restoreRevButton.setEnabled(false);
			usersList.setModel(new ShareParticipantListModel());
		}
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed"
	// <editor-fold defaultstate="collapsed"
	// desc="Generated Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		mainTabbedPane = new javax.swing.JTabbedPane();
		shareListTabPanel = new javax.swing.JPanel();
		shareListPanel = new javax.swing.JPanel();
		shareListScrollPane = new javax.swing.JScrollPane();
		shareList = new javax.swing.JList();
		removeShare = new javax.swing.JButton();
		addShare = new javax.swing.JButton();
		restoreRevButton = new javax.swing.JButton();
		sharePropertiesPanel = new javax.swing.JPanel();
		sharePropertiesLabel = new javax.swing.JLabel();
		usersLabel = new javax.swing.JLabel();
		usersListScrollPane = new javax.swing.JScrollPane();
		usersList = new javax.swing.JList();
		permissionsLabel = new javax.swing.JLabel();
		urlLabel = new javax.swing.JLabel();
		syncStatusLabel = new javax.swing.JLabel();
		ownerTextField = new javax.swing.JTextField();
		urlTextField = new javax.swing.JTextField();
		syncStatusTextField = new javax.swing.JTextField();
		removeDeviceContactShareButton = new javax.swing.JButton();
		addDeviceContactShareButton = new javax.swing.JButton();
		addressbookTabPanel = new javax.swing.JPanel();
		addressbookPanel = new javax.swing.JPanel();
		addressbookListScrollPane = new javax.swing.JScrollPane();
		addressbookList = new javax.swing.JList();
		exportContactButton = new javax.swing.JButton();
		removeContactButton = new javax.swing.JButton();
		importContactButton = new javax.swing.JButton();
		publishContactButton = new javax.swing.JButton();
		contactPropertiesPanell = new javax.swing.JPanel();
		contactPropertiesLabel = new javax.swing.JLabel();
		firstNameLabel = new javax.swing.JLabel();
		removeCSPInfoButton = new javax.swing.JButton();
		addCSPInfoButton = new javax.swing.JButton();
		lastNameLabel = new javax.swing.JLabel();
		emailLabel = new javax.swing.JLabel();
		cspAccountsLabel = new javax.swing.JLabel();
		firstNameTextField = new javax.swing.JTextField();
		lastNameTextField = new javax.swing.JTextField();
		emailTextField = new javax.swing.JTextField();
		cspInfoTableScrollPanel = new javax.swing.JScrollPane();
		cspInfoTable = new javax.swing.JTable();
		expertModeContactPanel = new javax.swing.JPanel();
		encKeyFprintLabel = new javax.swing.JLabel();
		encKeyFprintTextField = new javax.swing.JTextField();
		signKeyFprintLabel = new javax.swing.JLabel();
		signKeyFprintTextField = new javax.swing.JTextField();
		validFromUntilLabel = new javax.swing.JLabel();
		contactApplyButton = new javax.swing.JButton();
		contactDiscardButton = new javax.swing.JButton();
		contactVerificationStatusCheckBox = new javax.swing.JCheckBox();
		devicesTabPanel = new javax.swing.JPanel();
		deviceListPanel = new javax.swing.JPanel();
		deviceListScrollPane = new javax.swing.JScrollPane();
		deviceList = new javax.swing.JList();
		addDeviceButton = new javax.swing.JButton();
		removeDeviceButton = new javax.swing.JButton();
		devicePropertiesPanel = new javax.swing.JPanel();
		devicePropertiesLabel = new javax.swing.JLabel();
		expertModeDevicePanel = new javax.swing.JPanel();
		deviceKeyFprintLabel = new javax.swing.JLabel();
		deviceKeyFprintTextField = new javax.swing.JTextField();
		jLabel1 = new javax.swing.JLabel();
		usersListScrollPane1 = new javax.swing.JScrollPane();
		deviceShareList = new javax.swing.JList();
		settingsTabPanel = new javax.swing.JPanel();
		languageLabel = new javax.swing.JLabel();
		languageComboBox = new javax.swing.JComboBox();
		settingsFolderLabel = new javax.swing.JLabel();
		settingsFolderTextField = new javax.swing.JTextField();
		settingsFolderChooseButton = new javax.swing.JButton();
		panboxFolderLabel = new javax.swing.JLabel();
		panboxFolderTextField = new javax.swing.JTextField();
		panboxFolderChooseButton = new javax.swing.JButton();
		settingsApplyButton = new javax.swing.JButton();
		settingsRevertButton = new javax.swing.JButton();
		expertModeCheckBox = new javax.swing.JCheckBox();
		networkDevicePairingPanel = new javax.swing.JPanel();
		networkDevicePairingLabel = new javax.swing.JLabel();
		networkInterfaceLabel = new javax.swing.JLabel();
		networkInterfaceComboBox = new javax.swing.JComboBox();
		networkAddressLabel = new javax.swing.JLabel();
		networkAddressComboBox = new javax.swing.JComboBox();
		cspSettingsPanel = new javax.swing.JPanel();
		selectedCSPLabel = new javax.swing.JLabel();
		cspSelectionComboBox = new javax.swing.JComboBox();
		selectedCSPContentPanel = new javax.swing.JPanel();
		uriHandlerCheckbox = new javax.swing.JCheckBox();
		mailtoSchemeCheckbox = new javax.swing.JCheckBox();
		clipboardHandlerCheckbox = new javax.swing.JCheckBox();

		setTitle(bundle.getString("PanboxClientGUI.title")); // NOI18N
		setMinimumSize(new java.awt.Dimension(800, 600));
		setResizable(false);

		mainTabbedPane.setPreferredSize(new java.awt.Dimension(813, 609));

		shareListTabPanel.setLayout(new java.awt.GridLayout(1, 2, 10, 10));

		shareList.setModel(shareModel);
		shareList
				.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		shareList.setCellRenderer(new ShareListCellRenderer());
		shareList.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseClicked(java.awt.event.MouseEvent evt) {
				shareListMouseClicked(evt);
			}
		});
		shareList
				.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
					public void valueChanged(
							javax.swing.event.ListSelectionEvent evt) {
						shareListValueChanged(evt);
					}
				});
		shareListScrollPane.setViewportView(shareList);

		removeShare.setIcon(new javax.swing.ImageIcon(getClass().getResource(
				"/org/panbox/desktop/common/gui/removebutton.png"))); // NOI18N
		removeShare.setToolTipText(bundle
				.getString("client.shareList.removeShareToolTip")); // NOI18N
		removeShare.setEnabled(false);
		removeShare.setPreferredSize(new java.awt.Dimension(28, 28));
		removeShare.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				removeShareActionPerformed(evt);
			}
		});

		addShare.setIcon(new javax.swing.ImageIcon(getClass().getResource(
				"/org/panbox/desktop/common/gui/addbutton.png"))); // NOI18N
		addShare.setToolTipText(bundle
				.getString("client.shareList.addShareToolTip")); // NOI18N
		addShare.setPreferredSize(new java.awt.Dimension(28, 28));
		addShare.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				addShareActionPerformed(evt);
			}
		});

		restoreRevButton.setIcon(new javax.swing.ImageIcon(
				getClass().getResource(
						"/org/panbox/desktop/common/gui/restorebutton.png"))); // NOI18N
		restoreRevButton.setToolTipText(bundle
				.getString("PanboxClientGUI.restoreRevButton.tooltip")); // NOI18N
		restoreRevButton.setEnabled(false);
		restoreRevButton.setPreferredSize(new java.awt.Dimension(28, 28));
		restoreRevButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				restoreRevButtonActionPerformed(evt);
			}
		});

		javax.swing.GroupLayout shareListPanelLayout = new javax.swing.GroupLayout(
				shareListPanel);
		shareListPanel.setLayout(shareListPanelLayout);
		shareListPanelLayout
				.setHorizontalGroup(shareListPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								shareListPanelLayout
										.createSequentialGroup()
										.addContainerGap()
										.addGroup(
												shareListPanelLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.LEADING)
														.addComponent(
																shareListScrollPane,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																371,
																Short.MAX_VALUE)
														.addGroup(
																javax.swing.GroupLayout.Alignment.TRAILING,
																shareListPanelLayout
																		.createSequentialGroup()
																		.addGap(0,
																				0,
																				Short.MAX_VALUE)
																		.addComponent(
																				restoreRevButton,
																				javax.swing.GroupLayout.PREFERRED_SIZE,
																				javax.swing.GroupLayout.DEFAULT_SIZE,
																				javax.swing.GroupLayout.PREFERRED_SIZE)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(
																				addShare,
																				javax.swing.GroupLayout.PREFERRED_SIZE,
																				javax.swing.GroupLayout.DEFAULT_SIZE,
																				javax.swing.GroupLayout.PREFERRED_SIZE)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(
																				removeShare,
																				javax.swing.GroupLayout.PREFERRED_SIZE,
																				javax.swing.GroupLayout.DEFAULT_SIZE,
																				javax.swing.GroupLayout.PREFERRED_SIZE)))
										.addContainerGap()));
		shareListPanelLayout
				.setVerticalGroup(shareListPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								shareListPanelLayout
										.createSequentialGroup()
										.addContainerGap()
										.addComponent(
												shareListScrollPane,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												594, Short.MAX_VALUE)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(
												shareListPanelLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(
																removeShare,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.PREFERRED_SIZE)
														.addComponent(
																addShare,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.PREFERRED_SIZE)
														.addComponent(
																restoreRevButton,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.PREFERRED_SIZE))
										.addContainerGap()));

		shareListTabPanel.add(shareListPanel);

		sharePropertiesLabel.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
		sharePropertiesLabel.setText(bundle
				.getString("client.shareList.shareProperties")); // NOI18N

		usersLabel.setText(bundle.getString("PanboxClientGUI.usersLabel.text")); // NOI18N

		usersList
				.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		usersList.setCellRenderer(new ShareParticipantListCellRenderer());
		usersList
				.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
					public void valueChanged(
							javax.swing.event.ListSelectionEvent evt) {
						usersListValueChanged(evt);
					}
				});
		usersListScrollPane.setViewportView(usersList);

		permissionsLabel.setText(bundle
				.getString("client.shareList.permissions")); // NOI18N

		urlLabel.setText(bundle.getString("client.shareList.url")); // NOI18N

		syncStatusLabel
				.setText(bundle.getString("client.shareList.syncStatus")); // NOI18N

		ownerTextField.setEnabled(false);

		urlTextField.setEnabled(false);

		syncStatusTextField.setEnabled(false);

		removeDeviceContactShareButton.setIcon(new javax.swing.ImageIcon(
				getClass().getResource(
						"/org/panbox/desktop/common/gui/removebutton.png"))); // NOI18N
		removeDeviceContactShareButton
				.setToolTipText(bundle
						.getString("client.shareList.removeUserDeviceFromShareToolTip")); // NOI18N
		removeDeviceContactShareButton.setEnabled(false);
		removeDeviceContactShareButton
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						removeDeviceContactShareButtonActionPerformed(evt);
					}
				});

		addDeviceContactShareButton.setIcon(new javax.swing.ImageIcon(
				getClass().getResource(
						"/org/panbox/desktop/common/gui/addbutton.png"))); // NOI18N
		addDeviceContactShareButton.setToolTipText(bundle
				.getString("client.shareList.addUserDeviceToShareToolTip")); // NOI18N
		addDeviceContactShareButton
				.addMouseListener(new java.awt.event.MouseAdapter() {
					public void mousePressed(java.awt.event.MouseEvent evt) {
						addDeviceContactShareButtonMousePressed(evt);
					}
				});

		javax.swing.GroupLayout sharePropertiesPanelLayout = new javax.swing.GroupLayout(
				sharePropertiesPanel);
		sharePropertiesPanel.setLayout(sharePropertiesPanelLayout);
		sharePropertiesPanelLayout
				.setHorizontalGroup(sharePropertiesPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								sharePropertiesPanelLayout
										.createSequentialGroup()
										.addContainerGap()
										.addGroup(
												sharePropertiesPanelLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.LEADING)
														.addComponent(
																syncStatusLabel,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																Short.MAX_VALUE)
														.addComponent(
																permissionsLabel,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																Short.MAX_VALUE)
														.addComponent(
																usersListScrollPane)
														.addGroup(
																sharePropertiesPanelLayout
																		.createSequentialGroup()
																		.addGap(10,
																				10,
																				10)
																		.addGroup(
																				sharePropertiesPanelLayout
																						.createParallelGroup(
																								javax.swing.GroupLayout.Alignment.LEADING)
																						.addComponent(
																								urlTextField)
																						.addComponent(
																								syncStatusTextField)
																						.addComponent(
																								ownerTextField)))
														.addComponent(
																usersLabel,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																Short.MAX_VALUE)
														.addComponent(
																urlLabel,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																Short.MAX_VALUE)
														.addGroup(
																javax.swing.GroupLayout.Alignment.TRAILING,
																sharePropertiesPanelLayout
																		.createSequentialGroup()
																		.addGap(0,
																				283,
																				Short.MAX_VALUE)
																		.addComponent(
																				addDeviceContactShareButton)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(
																				removeDeviceContactShareButton))
														.addGroup(
																sharePropertiesPanelLayout
																		.createSequentialGroup()
																		.addComponent(
																				sharePropertiesLabel)
																		.addGap(0,
																				44,
																				Short.MAX_VALUE)))
										.addContainerGap()));
		sharePropertiesPanelLayout
				.setVerticalGroup(sharePropertiesPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								sharePropertiesPanelLayout
										.createSequentialGroup()
										.addContainerGap()
										.addComponent(sharePropertiesLabel)
										.addGap(18, 18, 18)
										.addComponent(usersLabel)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(
												usersListScrollPane,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												148,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(
												sharePropertiesPanelLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(
																removeDeviceContactShareButton)
														.addComponent(
																addDeviceContactShareButton))
										.addGap(18, 18, 18)
										.addComponent(permissionsLabel)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(
												ownerTextField,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addComponent(urlLabel)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addComponent(
												urlTextField,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addComponent(syncStatusLabel)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addComponent(
												syncStatusTextField,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addContainerGap(165, Short.MAX_VALUE)));

		sharePropertiesLabel.getAccessibleContext().setAccessibleName(
				bundle.getString("client.shareList.shareProperties")); // NOI18N
		usersLabel.getAccessibleContext().setAccessibleName(
				bundle.getString("client.shareList.usersDevices")); // NOI18N
		permissionsLabel.getAccessibleContext().setAccessibleName(
				bundle.getString("client.shareList.permissions")); // NOI18N
		urlLabel.getAccessibleContext().setAccessibleName(
				bundle.getString("client.shareList.url")); // NOI18N
		syncStatusLabel.getAccessibleContext().setAccessibleName(
				bundle.getString("client.shareList.syncStatus")); // NOI18N

		shareListTabPanel.add(sharePropertiesPanel);

		mainTabbedPane.addTab(bundle.getString("client.shareList.tabTitle"),
				shareListTabPanel); // NOI18N
		shareListTabPanel.getAccessibleContext().setAccessibleName(
				bundle.getString("client.shareList.tabTitle")); // NOI18N

		addressbookTabPanel.setLayout(new java.awt.GridLayout(1, 2, 10, 10));

		addressbookList.setModel(contactModel);
		addressbookList.setCellRenderer(new ContactListCellRenderer());
		addressbookListScrollPane.setViewportView(addressbookList);

		exportContactButton.setIcon(new javax.swing.ImageIcon(getClass()
				.getResource("/org/panbox/desktop/common/gui/savebutton.png"))); // NOI18N
		exportContactButton.setToolTipText(bundle
				.getString("client.addressList.exportContact")); // NOI18N
		exportContactButton.setPreferredSize(new java.awt.Dimension(28, 28));
		exportContactButton
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						exportContactButtonActionPerformed(evt);
					}
				});

		removeContactButton
				.setIcon(new javax.swing.ImageIcon(getClass().getResource(
						"/org/panbox/desktop/common/gui/removebutton.png"))); // NOI18N
		removeContactButton.setToolTipText(bundle
				.getString("client.addressList.removeContact")); // NOI18N
		removeContactButton.setPreferredSize(new java.awt.Dimension(28, 28));
		removeContactButton
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						removeContactButtonActionPerformed(evt);
					}
				});

		importContactButton.setIcon(new javax.swing.ImageIcon(getClass()
				.getResource("/org/panbox/desktop/common/gui/addbutton.png"))); // NOI18N
		importContactButton.setToolTipText(bundle
				.getString("client.addressList.importContact")); // NOI18N
		importContactButton.setPreferredSize(new java.awt.Dimension(28, 28));
		importContactButton
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						importContactButtonActionPerformed(evt);
					}
				});

		publishContactButton.setIcon(new javax.swing.ImageIcon(
				getClass().getResource(
						"/org/panbox/desktop/common/gui/publishbutton.png"))); // NOI18N
		publishContactButton.setToolTipText(bundle
				.getString("PanboxClientGUI.publishContactButton.toolTipText")); // NOI18N
		publishContactButton.setPreferredSize(new java.awt.Dimension(28, 28));
		publishContactButton
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						publishContactButtonActionPerformed(evt);
					}
				});

		javax.swing.GroupLayout addressbookPanelLayout = new javax.swing.GroupLayout(
				addressbookPanel);
		addressbookPanel.setLayout(addressbookPanelLayout);
		addressbookPanelLayout
				.setHorizontalGroup(addressbookPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								addressbookPanelLayout
										.createSequentialGroup()
										.addContainerGap()
										.addGroup(
												addressbookPanelLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.LEADING)
														.addComponent(
																addressbookListScrollPane,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																375,
																Short.MAX_VALUE)
														.addGroup(
																javax.swing.GroupLayout.Alignment.TRAILING,
																addressbookPanelLayout
																		.createSequentialGroup()
																		.addGap(0,
																				0,
																				Short.MAX_VALUE)
																		.addComponent(
																				importContactButton,
																				javax.swing.GroupLayout.PREFERRED_SIZE,
																				javax.swing.GroupLayout.DEFAULT_SIZE,
																				javax.swing.GroupLayout.PREFERRED_SIZE)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(
																				removeContactButton,
																				javax.swing.GroupLayout.PREFERRED_SIZE,
																				javax.swing.GroupLayout.DEFAULT_SIZE,
																				javax.swing.GroupLayout.PREFERRED_SIZE)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(
																				exportContactButton,
																				javax.swing.GroupLayout.PREFERRED_SIZE,
																				javax.swing.GroupLayout.DEFAULT_SIZE,
																				javax.swing.GroupLayout.PREFERRED_SIZE)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(
																				publishContactButton,
																				javax.swing.GroupLayout.PREFERRED_SIZE,
																				javax.swing.GroupLayout.DEFAULT_SIZE,
																				javax.swing.GroupLayout.PREFERRED_SIZE)))
										.addContainerGap()));
		addressbookPanelLayout
				.setVerticalGroup(addressbookPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								addressbookPanelLayout
										.createSequentialGroup()
										.addContainerGap()
										.addComponent(
												addressbookListScrollPane,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												594, Short.MAX_VALUE)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(
												addressbookPanelLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(
																exportContactButton,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.PREFERRED_SIZE)
														.addComponent(
																removeContactButton,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.PREFERRED_SIZE)
														.addComponent(
																importContactButton,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.PREFERRED_SIZE)
														.addComponent(
																publishContactButton,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.PREFERRED_SIZE))
										.addContainerGap()));

		exportContactButton.getAccessibleContext().setAccessibleDescription(
				bundle.getString("client.addressList.exportContact")); // NOI18N
		removeContactButton.getAccessibleContext().setAccessibleDescription(
				bundle.getString("client.addressList.removeContact")); // NOI18N
		importContactButton.getAccessibleContext().setAccessibleDescription(
				bundle.getString("client.addressList.importContact")); // NOI18N

		addressbookTabPanel.add(addressbookPanel);

		contactPropertiesLabel.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
		contactPropertiesLabel.setText(bundle
				.getString("client.addressList.properties")); // NOI18N

		firstNameLabel
				.setText(bundle.getString("client.addressList.firstName")); // NOI18N

		removeCSPInfoButton
				.setIcon(new javax.swing.ImageIcon(getClass().getResource(
						"/org/panbox/desktop/common/gui/removebutton.png"))); // NOI18N
		removeCSPInfoButton.setToolTipText(bundle
				.getString("client.addressList.cspRemoveAccount")); // NOI18N
		removeCSPInfoButton.setEnabled(false);
		removeCSPInfoButton
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						removeCSPInfoButtonActionPerformed(evt);
					}
				});

		addCSPInfoButton.setIcon(new javax.swing.ImageIcon(getClass()
				.getResource("/org/panbox/desktop/common/gui/addbutton.png"))); // NOI18N
		addCSPInfoButton.setToolTipText(bundle
				.getString("client.addressList.cspAddAccount")); // NOI18N
		addCSPInfoButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				addCSPInfoButtonActionPerformed(evt);
			}
		});

		lastNameLabel.setText(bundle.getString("client.addressList.lastName")); // NOI18N

		emailLabel.setText(bundle.getString("client.addressList.email")); // NOI18N

		cspAccountsLabel.setText(bundle
				.getString("client.addressList.cspAccounts")); // NOI18N

		emailTextField.setEditable(false);

		cspInfoTable.setModel(new javax.swing.table.DefaultTableModel(
				new Object[][] {

				}, new String[] { "Cloud Storage Provider", "Useraccount" }));
		cspInfoTable.setRowHeight(20);
		cspInfoTableScrollPanel.setViewportView(cspInfoTable);

		encKeyFprintLabel.setText(bundle
				.getString("client.addressList.fprintEnc")); // NOI18N

		encKeyFprintTextField.setEnabled(false);
		encKeyFprintTextField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 9));

		signKeyFprintLabel.setText(bundle
				.getString("client.addressList.fprintSign")); // NOI18N

		signKeyFprintTextField.setEnabled(false);
		signKeyFprintTextField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 9));

		validFromUntilLabel.setText("null");

		javax.swing.GroupLayout expertModeContactPanelLayout = new javax.swing.GroupLayout(
				expertModeContactPanel);
		expertModeContactPanel.setLayout(expertModeContactPanelLayout);
		expertModeContactPanelLayout
				.setHorizontalGroup(expertModeContactPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addComponent(validFromUntilLabel,
								javax.swing.GroupLayout.DEFAULT_SIZE, 375,
								Short.MAX_VALUE)
						.addComponent(signKeyFprintLabel,
								javax.swing.GroupLayout.DEFAULT_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE,
								Short.MAX_VALUE)
						.addComponent(encKeyFprintLabel,
								javax.swing.GroupLayout.DEFAULT_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE,
								Short.MAX_VALUE)
						.addGroup(
								expertModeContactPanelLayout
										.createSequentialGroup()
										.addGap(12, 12, 12)
										.addGroup(
												expertModeContactPanelLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.LEADING)
														.addComponent(
																encKeyFprintTextField)
														.addComponent(
																signKeyFprintTextField))));
		expertModeContactPanelLayout
				.setVerticalGroup(expertModeContactPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								expertModeContactPanelLayout
										.createSequentialGroup()
										.addContainerGap(
												javax.swing.GroupLayout.DEFAULT_SIZE,
												Short.MAX_VALUE)
										.addComponent(encKeyFprintLabel)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(
												encKeyFprintTextField,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(signKeyFprintLabel)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(
												signKeyFprintTextField,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addComponent(validFromUntilLabel)));

		encKeyFprintLabel.getAccessibleContext().setAccessibleName(
				bundle.getString("client.addressList.fprintEnc")); // NOI18N
		signKeyFprintLabel.getAccessibleContext().setAccessibleName(
				bundle.getString("client.addressList.fprintSign")); // NOI18N

		contactApplyButton.setText(bundle.getString("client.apply")); // NOI18N
		contactApplyButton.setEnabled(false);
		contactApplyButton
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						contactApplyButtonActionPerformed(evt);
					}
				});

		contactDiscardButton.setText(bundle.getString("client.discard")); // NOI18N
		contactDiscardButton.setEnabled(false);
		contactDiscardButton
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						contactDiscardButtonActionPerformed(evt);
					}
				});

		contactVerificationStatusCheckBox
				.setText(bundle
						.getString("PanboxClientGUI.contactVerificationStatusCheckBox.text")); // NOI18N
		contactVerificationStatusCheckBox
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						contactVerificationStatusCheckBoxActionPerformed(evt);
					}
				});

		javax.swing.GroupLayout contactPropertiesPanellLayout = new javax.swing.GroupLayout(
				contactPropertiesPanell);
		contactPropertiesPanell.setLayout(contactPropertiesPanellLayout);
		contactPropertiesPanellLayout
				.setHorizontalGroup(contactPropertiesPanellLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								contactPropertiesPanellLayout
										.createSequentialGroup()
										.addContainerGap()
										.addGroup(
												contactPropertiesPanellLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.LEADING)
														.addComponent(
																expertModeContactPanel,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																Short.MAX_VALUE)
														.addComponent(
																cspInfoTableScrollPanel)
														.addComponent(
																lastNameLabel,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																Short.MAX_VALUE)
														.addComponent(
																emailLabel,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																Short.MAX_VALUE)
														.addComponent(
																firstNameLabel,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																Short.MAX_VALUE)
														.addGroup(
																contactPropertiesPanellLayout
																		.createSequentialGroup()
																		.addGap(10,
																				10,
																				10)
																		.addGroup(
																				contactPropertiesPanellLayout
																						.createParallelGroup(
																								javax.swing.GroupLayout.Alignment.LEADING)
																						.addComponent(
																								lastNameTextField)
																						.addComponent(
																								emailTextField)
																						.addComponent(
																								firstNameTextField)))
														.addComponent(
																cspAccountsLabel,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																Short.MAX_VALUE)
														.addGroup(
																contactPropertiesPanellLayout
																		.createSequentialGroup()
																		.addComponent(
																				contactPropertiesLabel)
																		.addGap(0,
																				0,
																				Short.MAX_VALUE))
														.addGroup(
																javax.swing.GroupLayout.Alignment.TRAILING,
																contactPropertiesPanellLayout
																		.createSequentialGroup()
																		.addGap(0,
																				0,
																				Short.MAX_VALUE)
																		.addComponent(
																				contactDiscardButton)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
																		.addComponent(
																				contactApplyButton))
														.addGroup(
																javax.swing.GroupLayout.Alignment.TRAILING,
																contactPropertiesPanellLayout
																		.createSequentialGroup()
																		.addGap(12,
																				12,
																				12)
																		.addComponent(
																				contactVerificationStatusCheckBox,
																				javax.swing.GroupLayout.DEFAULT_SIZE,
																				javax.swing.GroupLayout.DEFAULT_SIZE,
																				Short.MAX_VALUE)
																		.addGap(18,
																				18,
																				18)
																		.addComponent(
																				addCSPInfoButton)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(
																				removeCSPInfoButton)))
										.addContainerGap()));
		contactPropertiesPanellLayout
				.setVerticalGroup(contactPropertiesPanellLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								contactPropertiesPanellLayout
										.createSequentialGroup()
										.addContainerGap()
										.addComponent(contactPropertiesLabel)
										.addGap(18, 18, 18)
										.addComponent(firstNameLabel)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addComponent(
												firstNameTextField,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addComponent(lastNameLabel)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addComponent(
												lastNameTextField,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addComponent(emailLabel)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addComponent(
												emailTextField,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addComponent(cspAccountsLabel)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addComponent(
												cspInfoTableScrollPanel,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												80,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(
												contactPropertiesPanellLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(
																removeCSPInfoButton)
														.addComponent(
																addCSPInfoButton)
														.addComponent(
																contactVerificationStatusCheckBox))
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(
												expertModeContactPanel,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												Short.MAX_VALUE)
										.addGroup(
												contactPropertiesPanellLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(
																contactApplyButton)
														.addComponent(
																contactDiscardButton))
										.addContainerGap()));

		contactPropertiesLabel.getAccessibleContext().setAccessibleName(
				bundle.getString("client.addressList.properties")); // NOI18N
		firstNameLabel.getAccessibleContext().setAccessibleName(
				bundle.getString("client.addressList.firstName")); // NOI18N
		removeCSPInfoButton.getAccessibleContext().setAccessibleDescription(
				bundle.getString("client.addressList.cspRemoveAccount")); // NOI18N
		addCSPInfoButton.getAccessibleContext().setAccessibleDescription(
				bundle.getString("client.addressList.cspAddAccount")); // NOI18N
		lastNameLabel.getAccessibleContext().setAccessibleName(
				bundle.getString("client.addressList.lastName")); // NOI18N
		emailLabel.getAccessibleContext().setAccessibleName(
				bundle.getString("client.addressList.email")); // NOI18N
		cspAccountsLabel.getAccessibleContext().setAccessibleName(
				bundle.getString("client.addressList.cspAccounts")); // NOI18N
		emailTextField.setBackground(Color.WHITE);
		contactApplyButton.getAccessibleContext().setAccessibleName(
				bundle.getString("client.apply")); // NOI18N
		contactDiscardButton.getAccessibleContext().setAccessibleName(
				bundle.getString("client.discard")); // NOI18N

		addressbookTabPanel.add(contactPropertiesPanell);

		mainTabbedPane.addTab(bundle.getString("client.addressList.tabTitle"),
				addressbookTabPanel); // NOI18N

		devicesTabPanel.setLayout(new java.awt.GridLayout(1, 2, 10, 10));

		deviceList.setModel(deviceModel);
		deviceList
				.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		deviceList.setCellRenderer(new DeviceListCellRenderer());
		deviceListScrollPane.setViewportView(deviceList);

		addDeviceButton.setIcon(new javax.swing.ImageIcon(getClass()
				.getResource("/org/panbox/desktop/common/gui/addbutton.png"))); // NOI18N
		addDeviceButton.setToolTipText(bundle
				.getString("client.deviceList.addDevice")); // NOI18N
		addDeviceButton.setPreferredSize(new java.awt.Dimension(28, 28));
		addDeviceButton.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mousePressed(java.awt.event.MouseEvent evt) {
				addDeviceButtonMousePressed(evt);
			}
		});

		removeDeviceButton
				.setIcon(new javax.swing.ImageIcon(getClass().getResource(
						"/org/panbox/desktop/common/gui/removebutton.png"))); // NOI18N
		removeDeviceButton.setToolTipText(bundle
				.getString("client.deviceList.removeDevice")); // NOI18N
		removeDeviceButton.setEnabled(false);
		removeDeviceButton.setPreferredSize(new java.awt.Dimension(28, 28));

		javax.swing.GroupLayout deviceListPanelLayout = new javax.swing.GroupLayout(
				deviceListPanel);
		deviceListPanel.setLayout(deviceListPanelLayout);
		deviceListPanelLayout
				.setHorizontalGroup(deviceListPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								deviceListPanelLayout
										.createSequentialGroup()
										.addContainerGap()
										.addGroup(
												deviceListPanelLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.LEADING)
														.addComponent(
																deviceListScrollPane,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																362,
																Short.MAX_VALUE)
														.addGroup(
																javax.swing.GroupLayout.Alignment.TRAILING,
																deviceListPanelLayout
																		.createSequentialGroup()
																		.addGap(0,
																				0,
																				Short.MAX_VALUE)
																		.addComponent(
																				addDeviceButton,
																				javax.swing.GroupLayout.PREFERRED_SIZE,
																				javax.swing.GroupLayout.DEFAULT_SIZE,
																				javax.swing.GroupLayout.PREFERRED_SIZE)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(
																				removeDeviceButton,
																				javax.swing.GroupLayout.PREFERRED_SIZE,
																				javax.swing.GroupLayout.DEFAULT_SIZE,
																				javax.swing.GroupLayout.PREFERRED_SIZE)))
										.addContainerGap()));
		deviceListPanelLayout
				.setVerticalGroup(deviceListPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								deviceListPanelLayout
										.createSequentialGroup()
										.addContainerGap()
										.addComponent(
												deviceListScrollPane,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												594, Short.MAX_VALUE)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(
												deviceListPanelLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(
																addDeviceButton,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.PREFERRED_SIZE)
														.addComponent(
																removeDeviceButton,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.PREFERRED_SIZE))
										.addContainerGap()));

		addDeviceButton.getAccessibleContext().setAccessibleDescription(
				bundle.getString("client.deviceList.addDevice")); // NOI18N
		removeDeviceButton.getAccessibleContext().setAccessibleDescription(
				bundle.getString("client.deviceList.removeDevice")); // NOI18N

		devicesTabPanel.add(deviceListPanel);

		devicePropertiesLabel.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
		devicePropertiesLabel.setText(bundle
				.getString("client.deviceList.properties")); // NOI18N

		deviceKeyFprintLabel.setText(bundle
				.getString("client.deviceList.fprintDeviceKey")); // NOI18N

		deviceKeyFprintTextField.setEnabled(false);
		deviceKeyFprintTextField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 9));

		javax.swing.GroupLayout expertModeDevicePanelLayout = new javax.swing.GroupLayout(
				expertModeDevicePanel);
		expertModeDevicePanel.setLayout(expertModeDevicePanelLayout);
		expertModeDevicePanelLayout
				.setHorizontalGroup(expertModeDevicePanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								expertModeDevicePanelLayout
										.createSequentialGroup()
										.addComponent(deviceKeyFprintLabel)
										.addGap(0, 0, Short.MAX_VALUE))
						.addGroup(
								javax.swing.GroupLayout.Alignment.TRAILING,
								expertModeDevicePanelLayout
										.createSequentialGroup()
										.addGap(0, 0, Short.MAX_VALUE)
										.addComponent(
												deviceKeyFprintTextField,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												359,
												javax.swing.GroupLayout.PREFERRED_SIZE)));
		expertModeDevicePanelLayout
				.setVerticalGroup(expertModeDevicePanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								expertModeDevicePanelLayout
										.createSequentialGroup()
										.addContainerGap()
										.addComponent(deviceKeyFprintLabel)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(
												deviceKeyFprintTextField,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addContainerGap(
												javax.swing.GroupLayout.DEFAULT_SIZE,
												Short.MAX_VALUE)));

		deviceKeyFprintLabel.getAccessibleContext().setAccessibleName(
				bundle.getString("client.deviceList.fprintDeviceKey")); // NOI18N

		jLabel1.setText(bundle.getString("client.deviceList.shareList")); // NOI18N

		deviceShareList
				.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		deviceShareList.setCellRenderer(new ShareListCellRenderer());
		usersListScrollPane1.setViewportView(deviceShareList);

		javax.swing.GroupLayout devicePropertiesPanelLayout = new javax.swing.GroupLayout(
				devicePropertiesPanel);
		devicePropertiesPanel.setLayout(devicePropertiesPanelLayout);
		devicePropertiesPanelLayout
				.setHorizontalGroup(devicePropertiesPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								devicePropertiesPanelLayout
										.createSequentialGroup()
										.addGroup(
												devicePropertiesPanelLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.LEADING)
														.addGroup(
																devicePropertiesPanelLayout
																		.createSequentialGroup()
																		.addContainerGap()
																		.addGroup(
																				devicePropertiesPanelLayout
																						.createParallelGroup(
																								javax.swing.GroupLayout.Alignment.LEADING)
																						.addComponent(
																								devicePropertiesLabel)
																						.addComponent(
																								jLabel1)
																						.addComponent(
																								expertModeDevicePanel,
																								javax.swing.GroupLayout.PREFERRED_SIZE,
																								javax.swing.GroupLayout.DEFAULT_SIZE,
																								javax.swing.GroupLayout.PREFERRED_SIZE))
																		.addGap(0,
																				20,
																				Short.MAX_VALUE))
														.addComponent(
																usersListScrollPane1,
																javax.swing.GroupLayout.Alignment.TRAILING))
										.addContainerGap()));
		devicePropertiesPanelLayout
				.setVerticalGroup(devicePropertiesPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								devicePropertiesPanelLayout
										.createSequentialGroup()
										.addContainerGap()
										.addComponent(devicePropertiesLabel)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addComponent(jLabel1)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(
												usersListScrollPane1,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												150,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(
												expertModeDevicePanel,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addContainerGap(299, Short.MAX_VALUE)));

		devicePropertiesLabel.getAccessibleContext().setAccessibleName(
				bundle.getString("client.deviceList.properties")); // NOI18N
		jLabel1.getAccessibleContext().setAccessibleName(
				bundle.getString("client.deviceList.shareList")); // NOI18N

		devicesTabPanel.add(devicePropertiesPanel);

		mainTabbedPane.addTab(bundle.getString("client.deviceList.tabTitle"),
				devicesTabPanel); // NOI18N
		devicesTabPanel.getAccessibleContext().setAccessibleName(
				bundle.getString("client.deviceList.tabTitle")); // NOI18N

		languageLabel.setText(bundle.getString("client.settings.language")); // NOI18N

		languageComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(
				SupportedLanguage.values()));
		languageComboBox
				.setRenderer(new org.panbox.desktop.common.gui.settings.LanguageListCellRenderer());

		settingsFolderLabel.setText(bundle
				.getString("client.settings.settingsFolder")); // NOI18N

		settingsFolderChooseButton.setText(bundle.getString("client.choose")); // NOI18N
		settingsFolderChooseButton
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						settingsFolderChooseButtonActionPerformed(evt);
					}
				});

		panboxFolderLabel.setText(bundle
				.getString("client.settings.panboxFolder")); // NOI18N

		panboxFolderChooseButton.setText(bundle.getString("client.choose")); // NOI18N
		panboxFolderChooseButton
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						panboxFolderChooseButtonActionPerformed(evt);
					}
				});

		settingsApplyButton.setText(bundle.getString("client.apply")); // NOI18N
		settingsApplyButton
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						settingsApplyButtonActionPerformed(evt);
					}
				});

		settingsRevertButton.setText(bundle.getString("client.discard")); // NOI18N
		settingsRevertButton
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						settingsRevertButtonActionPerformed(evt);
					}
				});

		expertModeCheckBox.setText(bundle
				.getString("client.settings.expertmode")); // NOI18N
		expertModeCheckBox
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						expertModeCheckBoxActionPerformed(evt);
					}
				});

		networkDevicePairingPanel
				.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle
						.getString("client.settings.devicePairing.netSettingsTitle"))); // NOI18N

		networkDevicePairingLabel.setText(bundle
				.getString("client.settings.devicePairingMessage")); // NOI18N

		networkInterfaceLabel.setText(bundle
				.getString("client.settings.devicePairing.netInterface")); // NOI18N

		networkInterfaceComboBox.setModel(new javax.swing.DefaultComboBoxModel(
				new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
		networkInterfaceComboBox
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						networkInterfaceComboBoxActionPerformed(evt);
					}
				});

		networkAddressLabel.setText(bundle
				.getString("client.settings.devicePairing.netAddress")); // NOI18N

		networkAddressComboBox.setModel(new javax.swing.DefaultComboBoxModel(
				new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
		networkAddressComboBox
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						networkAddressComboBoxActionPerformed(evt);
					}
				});

		javax.swing.GroupLayout networkDevicePairingPanelLayout = new javax.swing.GroupLayout(
				networkDevicePairingPanel);
		networkDevicePairingPanel.setLayout(networkDevicePairingPanelLayout);
		networkDevicePairingPanelLayout
				.setHorizontalGroup(networkDevicePairingPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								networkDevicePairingPanelLayout
										.createSequentialGroup()
										.addContainerGap()
										.addGroup(
												networkDevicePairingPanelLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.LEADING)
														.addGroup(
																networkDevicePairingPanelLayout
																		.createSequentialGroup()
																		.addComponent(
																				networkDevicePairingLabel)
																		.addGap(0,
																				150,
																				Short.MAX_VALUE))
														.addGroup(
																networkDevicePairingPanelLayout
																		.createSequentialGroup()
																		.addGroup(
																				networkDevicePairingPanelLayout
																						.createParallelGroup(
																								javax.swing.GroupLayout.Alignment.LEADING)
																						.addComponent(
																								networkInterfaceLabel,
																								javax.swing.GroupLayout.PREFERRED_SIZE,
																								192,
																								javax.swing.GroupLayout.PREFERRED_SIZE)
																						.addComponent(
																								networkAddressLabel))
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addGroup(
																				networkDevicePairingPanelLayout
																						.createParallelGroup(
																								javax.swing.GroupLayout.Alignment.LEADING)
																						.addComponent(
																								networkInterfaceComboBox,
																								0,
																								javax.swing.GroupLayout.DEFAULT_SIZE,
																								Short.MAX_VALUE)
																						.addComponent(
																								networkAddressComboBox,
																								0,
																								javax.swing.GroupLayout.DEFAULT_SIZE,
																								Short.MAX_VALUE))))
										.addContainerGap()));
		networkDevicePairingPanelLayout
				.setVerticalGroup(networkDevicePairingPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								networkDevicePairingPanelLayout
										.createSequentialGroup()
										.addContainerGap()
										.addComponent(networkDevicePairingLabel)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addGroup(
												networkDevicePairingPanelLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(
																networkInterfaceLabel)
														.addComponent(
																networkInterfaceComboBox,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addGroup(
												networkDevicePairingPanelLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(
																networkAddressLabel)
														.addComponent(
																networkAddressComboBox,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.PREFERRED_SIZE))
										.addContainerGap(
												javax.swing.GroupLayout.DEFAULT_SIZE,
												Short.MAX_VALUE)));

		cspSettingsPanel.setBorder(javax.swing.BorderFactory
				.createTitledBorder(bundle
						.getString("PanboxClientGUI.cspSettingsPanel.title"))); // NOI18N
		cspSettingsPanel.setMaximumSize(new java.awt.Dimension(694, 32767));

		selectedCSPLabel.setText(bundle
				.getString("PanboxClientGUI.selectedCSPLabel.text")); // NOI18N

		cspSelectionComboBox.setModel(generateCSPSelectionModel());
		cspSelectionComboBox
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						cspSelectionComboBoxActionPerformed(evt);
					}
				});

		selectedCSPContentPanel.setBorder(javax.swing.BorderFactory
				.createEmptyBorder(1, 1, 1, 1));
		selectedCSPContentPanel.setAutoscrolls(true);
		selectedCSPContentPanel.setLayout(new java.awt.GridLayout(0, 1));

		javax.swing.GroupLayout cspSettingsPanelLayout = new javax.swing.GroupLayout(
				cspSettingsPanel);
		cspSettingsPanel.setLayout(cspSettingsPanelLayout);
		cspSettingsPanelLayout
				.setHorizontalGroup(cspSettingsPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								cspSettingsPanelLayout
										.createSequentialGroup()
										.addContainerGap()
										.addGroup(
												cspSettingsPanelLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.LEADING)
														.addComponent(
																selectedCSPContentPanel,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																Short.MAX_VALUE)
														.addGroup(
																cspSettingsPanelLayout
																		.createSequentialGroup()
																		.addComponent(
																				selectedCSPLabel)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED,
																				javax.swing.GroupLayout.DEFAULT_SIZE,
																				Short.MAX_VALUE)
																		.addComponent(
																				cspSelectionComboBox,
																				javax.swing.GroupLayout.PREFERRED_SIZE,
																				535,
																				javax.swing.GroupLayout.PREFERRED_SIZE)))
										.addContainerGap()));
		cspSettingsPanelLayout
				.setVerticalGroup(cspSettingsPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								cspSettingsPanelLayout
										.createSequentialGroup()
										.addGroup(
												cspSettingsPanelLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(
																selectedCSPLabel)
														.addComponent(
																cspSelectionComboBox,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(
												selectedCSPContentPanel,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												Short.MAX_VALUE)));

		uriHandlerCheckbox.setText(bundle
				.getString("PanboxClientGUI.uriHandlerCheckbox.text")); // NOI18N
		uriHandlerCheckbox
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						uriHandlerCheckboxActionPerformed(evt);
					}
				});

		mailtoSchemeCheckbox.setText(bundle
				.getString("PanboxClientGUI.mailtoSchemeCheckbox.text")); // NOI18N
		mailtoSchemeCheckbox
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						mailtoSchemeCheckboxActionPerformed(evt);
					}
				});

		clipboardHandlerCheckbox.setText(bundle
				.getString("PanboxClientGUI.clipboardHandlerCheckbox.text")); // NOI18N
		clipboardHandlerCheckbox
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						clipboardHandlerCheckboxActionPerformed(evt);
					}
				});

		javax.swing.GroupLayout settingsTabPanelLayout = new javax.swing.GroupLayout(
				settingsTabPanel);
		settingsTabPanel.setLayout(settingsTabPanelLayout);
		settingsTabPanelLayout
				.setHorizontalGroup(settingsTabPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								javax.swing.GroupLayout.Alignment.TRAILING,
								settingsTabPanelLayout
										.createSequentialGroup()
										.addContainerGap()
										.addGroup(
												settingsTabPanelLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.TRAILING)
														.addComponent(
																networkDevicePairingPanel,
																javax.swing.GroupLayout.Alignment.LEADING,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																Short.MAX_VALUE)
														.addGroup(
																javax.swing.GroupLayout.Alignment.LEADING,
																settingsTabPanelLayout
																		.createSequentialGroup()
																		.addGroup(
																				settingsTabPanelLayout
																						.createParallelGroup(
																								javax.swing.GroupLayout.Alignment.TRAILING,
																								false)
																						.addComponent(
																								panboxFolderLabel,
																								javax.swing.GroupLayout.DEFAULT_SIZE,
																								javax.swing.GroupLayout.DEFAULT_SIZE,
																								Short.MAX_VALUE)
																						.addComponent(
																								settingsFolderLabel,
																								javax.swing.GroupLayout.DEFAULT_SIZE,
																								200,
																								Short.MAX_VALUE)
																						.addComponent(
																								languageLabel,
																								javax.swing.GroupLayout.DEFAULT_SIZE,
																								javax.swing.GroupLayout.DEFAULT_SIZE,
																								Short.MAX_VALUE))
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
																		.addGroup(
																				settingsTabPanelLayout
																						.createParallelGroup(
																								javax.swing.GroupLayout.Alignment.LEADING)
																						.addComponent(
																								languageComboBox,
																								0,
																								javax.swing.GroupLayout.DEFAULT_SIZE,
																								Short.MAX_VALUE)
																						.addGroup(
																								javax.swing.GroupLayout.Alignment.TRAILING,
																								settingsTabPanelLayout
																										.createSequentialGroup()
																										.addGroup(
																												settingsTabPanelLayout
																														.createParallelGroup(
																																javax.swing.GroupLayout.Alignment.TRAILING)
																														.addComponent(
																																panboxFolderTextField,
																																javax.swing.GroupLayout.Alignment.LEADING,
																																javax.swing.GroupLayout.DEFAULT_SIZE,
																																457,
																																Short.MAX_VALUE)
																														.addComponent(
																																settingsFolderTextField))
																										.addPreferredGap(
																												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																										.addGroup(
																												settingsTabPanelLayout
																														.createParallelGroup(
																																javax.swing.GroupLayout.Alignment.LEADING,
																																false)
																														.addComponent(
																																settingsFolderChooseButton,
																																javax.swing.GroupLayout.DEFAULT_SIZE,
																																javax.swing.GroupLayout.DEFAULT_SIZE,
																																Short.MAX_VALUE)
																														.addComponent(
																																panboxFolderChooseButton,
																																javax.swing.GroupLayout.DEFAULT_SIZE,
																																javax.swing.GroupLayout.DEFAULT_SIZE,
																																Short.MAX_VALUE)))
																						.addGroup(
																								settingsTabPanelLayout
																										.createSequentialGroup()
																										.addGroup(
																												settingsTabPanelLayout
																														.createParallelGroup(
																																javax.swing.GroupLayout.Alignment.LEADING)
																														.addComponent(
																																expertModeCheckBox)
																														.addComponent(
																																mailtoSchemeCheckbox))
																										.addGap(90,
																												90,
																												90)
																										.addGroup(
																												settingsTabPanelLayout
																														.createParallelGroup(
																																javax.swing.GroupLayout.Alignment.LEADING)
																														.addComponent(
																																clipboardHandlerCheckbox)
																														.addComponent(
																																uriHandlerCheckbox))
																										.addGap(0,
																												0,
																												Short.MAX_VALUE))))
														.addGroup(
																settingsTabPanelLayout
																		.createSequentialGroup()
																		.addGap(0,
																				0,
																				Short.MAX_VALUE)
																		.addComponent(
																				settingsRevertButton)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
																		.addComponent(
																				settingsApplyButton))
														.addComponent(
																cspSettingsPanel,
																javax.swing.GroupLayout.Alignment.LEADING,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																Short.MAX_VALUE))
										.addContainerGap()));
		settingsTabPanelLayout
				.setVerticalGroup(settingsTabPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								settingsTabPanelLayout
										.createSequentialGroup()
										.addContainerGap()
										.addGroup(
												settingsTabPanelLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(
																languageLabel)
														.addComponent(
																languageComboBox,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addGroup(
												settingsTabPanelLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(
																settingsFolderLabel)
														.addComponent(
																settingsFolderTextField,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.PREFERRED_SIZE)
														.addComponent(
																settingsFolderChooseButton))
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addGroup(
												settingsTabPanelLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(
																panboxFolderLabel)
														.addComponent(
																panboxFolderTextField,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.PREFERRED_SIZE)
														.addComponent(
																panboxFolderChooseButton))
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addGroup(
												settingsTabPanelLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(
																expertModeCheckBox)
														.addComponent(
																uriHandlerCheckbox))
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addGroup(
												settingsTabPanelLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(
																mailtoSchemeCheckbox)
														.addComponent(
																clipboardHandlerCheckbox))
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addComponent(
												networkDevicePairingPanel,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addComponent(
												cspSettingsPanel,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED,
												158, Short.MAX_VALUE)
										.addGroup(
												settingsTabPanelLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(
																settingsApplyButton)
														.addComponent(
																settingsRevertButton))
										.addContainerGap()));

		languageLabel.getAccessibleContext().setAccessibleName(
				bundle.getString("client.settings.language")); // NOI18N
		settingsFolderLabel.getAccessibleContext().setAccessibleName(
				bundle.getString("client.settings.settingsFolder")); // NOI18N
		settingsFolderChooseButton.getAccessibleContext().setAccessibleName(
				bundle.getString("client.choose")); // NOI18N
		panboxFolderLabel.getAccessibleContext().setAccessibleName(
				bundle.getString("client.settings.panboxFolder")); // NOI18N
		panboxFolderChooseButton.getAccessibleContext().setAccessibleName(
				bundle.getString("client.choose")); // NOI18N
		settingsApplyButton.getAccessibleContext().setAccessibleName(
				bundle.getString("client.apply")); // NOI18N
		settingsRevertButton.getAccessibleContext().setAccessibleName(
				bundle.getString("client.discard")); // NOI18N
		expertModeCheckBox.getAccessibleContext().setAccessibleName(
				bundle.getString("client.settings.expertmode")); // NOI18N

		mainTabbedPane.addTab(bundle.getString("client.settings.tabTitle"),
				settingsTabPanel); // NOI18N

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(
				getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(
				javax.swing.GroupLayout.Alignment.LEADING).addGroup(
				layout.createSequentialGroup()
						.addContainerGap()
						.addComponent(mainTabbedPane,
								javax.swing.GroupLayout.DEFAULT_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE,
								Short.MAX_VALUE).addContainerGap()));
		layout.setVerticalGroup(layout.createParallelGroup(
				javax.swing.GroupLayout.Alignment.LEADING).addGroup(
				layout.createSequentialGroup()
						.addContainerGap()
						.addComponent(mainTabbedPane,
								javax.swing.GroupLayout.DEFAULT_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE,
								Short.MAX_VALUE).addContainerGap()));

		mainTabbedPane
				.getAccessibleContext()
				.setAccessibleName(
						bundle.getString("PanboxClientGUI.mainTabbedPane.AccessibleContext.accessibleName")); // NOI18N

		pack();
		setLocationRelativeTo(null);
	}// </editor-fold>//GEN-END:initComponents

	private void shareListMouseClicked(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_shareListMouseClicked
		if (evt.getClickCount() == 2) {
			int index = shareList.locationToIndex(evt.getPoint());
			PanboxShare share = shareModel.getElementAt(index);
			client.openShareFolder(share.getName());
		}
	}// GEN-LAST:event_shareListMouseClicked

	private void uriHandlerCheckboxActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_uriHandlerCheckboxActionPerformed
		clipboardHandlerCheckbox.setEnabled(uriHandlerCheckbox.isSelected());
		setSettingsChangesDetected();
	}// GEN-LAST:event_uriHandlerCheckboxActionPerformed

	private void mailtoSchemeCheckboxActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_mailtoSchemeCheckboxActionPerformed
		setSettingsChangesDetected();
	}// GEN-LAST:event_mailtoSchemeCheckboxActionPerformed

	private void clipboardHandlerCheckboxActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_clipboardHandlerCheckboxActionPerformed
		setSettingsChangesDetected();
	}// GEN-LAST:event_clipboardHandlerCheckboxActionPerformed

	private void cspSelectionComboBoxActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cspSelectionComboBoxActionPerformed
		String sel = (String) cspSelectionComboBox.getSelectedItem();
		StorageBackendType t = StorageBackendType.fromDisplayName(sel);
		selectedCSPContentPanel.removeAll();
		if (t == StorageBackendType.DROPBOX) {
			selectedCSPContentPanel.add(dropboxSettingsPanel);
		}
		revalidate();
	}// GEN-LAST:event_cspSelectionComboBoxActionPerformed

	private void publishContactButtonActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_publishContactButtonActionPerformed
		PublishIdentitiesWoPINDialog d = new PublishIdentitiesWoPINDialog(client,
				contacts);
		d.setLocationRelativeTo(this);
		d.setVisible(true);
	}// GEN-LAST:event_publishContactButtonActionPerformed

	private void contactVerificationStatusCheckBoxActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_contactVerificationStatusCheckBoxActionPerformed
		if (contactVerificationStatusCheckBox.isSelected()) {
			String message = bundle
					.getString("PanboxClientGUI.really.verifiy.contact")  + "\nFingerprint Enc: " + contact.getCertEncFingerprint() + "\nFingerprint Sign: " + contact.getCertSignFingerprint();
			int reallyTrust = JOptionPane.showConfirmDialog(null, message,
					bundle.getString("PanboxClientGUI.panboxMessage"),
					JOptionPane.YES_NO_OPTION);
			if (reallyTrust == JOptionPane.NO_OPTION) {
				contactVerificationStatusCheckBox.setSelected(false);
				contactVerificationStatusCheckBox.setText(bundle
						.getString("PanboxClientGUI.contact.verified"));
			} else {
				contactVerificationStatusCheckBox.setText(bundle
						.getString("PanboxClientGUI.contact.verified"));
			}
		} else {
			contactVerificationStatusCheckBox.setText(bundle
					.getString("PanboxClientGUI.contact.verified"));
		}
		setContactChangesDetected();
	}// GEN-LAST:event_contactVerificationStatusCheckBoxActionPerformed

	private void usersListValueChanged(javax.swing.event.ListSelectionEvent evt) {// GEN-FIRST:event_usersListValueChanged
		if (!evt.getValueIsAdjusting()) {
			// Object selection = usersList.getSelectedValue();
			// TODO: Removing devices or contacts is not supported for now
			removeDeviceContactShareButton.setEnabled(false);
			// removeDeviceContactShareButton
			// .setEnabled((selection instanceof DeviceShareParticipant)
			// || (selection instanceof ContactShareParticipant
			// && shareList.getSelectedValue() != null && ((PanboxShare)
			// shareList
			// .getSelectedValue()).isOwner()));
		}
	}// GEN-LAST:event_usersListValueChanged

	private void removeDeviceContactShareButtonActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_removeDeviceContactShareButtonActionPerformed
		int selected = shareList.getSelectedIndex();
		if ((selected != -1)) {
			PanboxShare share = shareModel.getElementAt(selected);
			ShareParticipantListModel model = (ShareParticipantListModel) usersList
					.getModel();
			Object o = usersList.getSelectedValue();
			if (share != null && o != null) {
				if (o instanceof PanboxSharePermission) {
					char[] password = PasswordEnterDialog
							.invoke(PermissionType.SHARE);
					PanboxSharePermission p = (PanboxSharePermission) o;
					client.removePermissionFromShare(share, p, password);
					model.removeElement(p);
					Arrays.fill(password, (char) 0);
				}
			}
		}
	}// GEN-LAST:event_removeDeviceContactShareButtonActionPerformed

	private void restoreRevButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_restoreRevButtonActionPerformed
		PanboxShare share = (PanboxShare) shareList.getSelectedValue();
		if (share instanceof DropboxPanboxShare) {
			DropboxAdapterFactory dbxFac = (DropboxAdapterFactory) CSPAdapterFactory
					.getInstance(StorageBackendType.DROPBOX);
			DropboxAPIIntegration dbIntegration = (DropboxAPIIntegration) dbxFac
					.getAPIAdapter();

			RestoreRevisionDialog restoreRevisionDialog = new RestoreRevisionDialog(
					client, dbIntegration, share.getName());
			restoreRevisionDialog.setLocationRelativeTo(this);
			restoreRevisionDialog.setVisible(true);
		}
	}// GEN-LAST:event_restoreRevButtonActionPerformed

	private void shareListValueChanged(javax.swing.event.ListSelectionEvent evt) {// GEN-FIRST:event_shareListValueChanged
		// if (shareList.getSelectedValuesList().size() > 0) {
		// removeShare.setEnabled(true);
		// if (shareList.getSelectedValuesList().size() == 1) {
		// PanboxShare share = (PanboxShare) shareList.getSelectedValue();
		// if (share instanceof DropboxPanboxShare) {
		// restoreRevButton.setEnabled(true);
		// } else {
		// restoreRevButton.setEnabled(false);
		// }
		// } else {
		// restoreRevButton.setEnabled(false);
		// }
		// } else {
		// removeShare.setEnabled(false);
		// restoreRevButton.setEnabled(false);
		// }
	}// GEN-LAST:event_shareListValueChanged

	private void expertModeCheckBoxActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_expertModeCheckBoxActionPerformed
		if (expertModeCheckBox.isSelected()) {
			expertModeContactPanel.setVisible(true);
			expertModeDevicePanel.setVisible(true);
			contactVerificationStatusCheckBox.setVisible(true);
		} else {
			expertModeContactPanel.setVisible(false);
			expertModeDevicePanel.setVisible(false);
			contactVerificationStatusCheckBox.setVisible(false);
		}
	}// GEN-LAST:event_expertModeCheckBoxActionPerformed

	private void addShareActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_addShareActionPerformed
		AddShareDialog addShareDialog = new AddShareDialog(this);
		addShareDialog.setLocationRelativeTo(this);
		addShareDialog.setVisible(true);
		try {
			addShare(addShareDialog.getResult());
		} catch (OperationAbortedException e) {
			// do nothing if operation was aborted
		}
	}// GEN-LAST:event_addShareActionPerformed

	public void addShare(PanboxShare newShare) {
		char[] password = null;
		try {
			password = PasswordEnterDialog.invoke(PermissionType.SHARE);

			client.showTrayMessage(bundle.getString("PleaseWait"),
					bundle.getString("tray.addShare.message"), MessageType.INFO);
			client.addShare(newShare, password);
			// Also update share list for selected device
			if (device != null) {
				deviceShareList.setModel(client.getDeviceShares(device));
			}
		} finally {
			if (password != null) {
				Utils.eraseChars(password);
			}
		}
	}

	public void refreshDeviceShareList() {
		deviceShareList.setModel(client.getDeviceShares(device));
	}

	public void refreshAddressbookList() {
		addressbookList.setModel(client.getContactList());
	}

	private void removeShareActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_removeShareActionPerformed
		JCheckBox checkbox = new JCheckBox(
				bundle.getString("client.shareList.removeShareDirectoryMessage"));
		String message = bundle
				.getString("really.shareList.removeShareMessage");
		Object[] params = { message, checkbox };

		int reallyRemove = JOptionPane.showConfirmDialog(null, params,
				bundle.getString("PanboxClientGUI.panboxMessage"),
				JOptionPane.YES_NO_OPTION);
		// int reallyRemove =
		// JOptionPane.showConfirmDialog(this,
		// "You are about to remove a share. Do you really want to continue?",
		// "Really remove this share?", JOptionPane.YES_NO_OPTION);
		if (reallyRemove == JOptionPane.YES_OPTION) {
			PanboxShare share = (PanboxShare) shareList.getSelectedValue();

			if (checkbox.isSelected()) {
				PleaseWaitDialog d = null;
				try {
					d = new PleaseWaitDialog(
							this,
							bundle.getString("PanboxClient.operationInProgress"));
					d.setLocationRelativeTo(this);
					d.setVisible(true);
					FileUtils.deleteDirectoryTree(new File(share.getPath()));
					// shareList.setSelectedIndex(--idx);
					shareList.setSelectedIndex(-1);
				} catch (IOException e) {
					logger.error("Failed to remove share source directory!", e);
					JOptionPane
							.showMessageDialog(
									this,
									bundle.getString("PanboxClient.deleteShareContentsFailed"),
									bundle.getString("error"),
									JOptionPane.ERROR_MESSAGE);
				} finally {
					if (d != null) {
						d.dispose();
					}
				}
			}

			// remove share from view
			client.removeShare(share);
			// Also update share list for selected device
			if (device != null) {
				deviceShareList.setModel(client.getDeviceShares(device));
			}
		}
	}// GEN-LAST:event_removeShareActionPerformed

	private void addDeviceContactShareButtonMousePressed(
			java.awt.event.MouseEvent evt) {// GEN-FIRST:event_addDeviceContactShareButtonMousePressed
		final JFrame thisFrame = this;

		JPopupMenu menu = new JPopupMenu();
		JMenuItem addDevice = new JMenuItem(
				bundle.getString("shareUserList.device"));
		addDevice.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					int selected = shareList.getSelectedIndex();
					PanboxShare share = shareModel.getElementAt(selected);

					if (share instanceof DropboxPanboxShare) {
						DropboxAdapterFactory dbxFac = (DropboxAdapterFactory) CSPAdapterFactory
								.getInstance(StorageBackendType.DROPBOX);
						DropboxAPIIntegration api = (DropboxAPIIntegration) dbxFac
								.getAPIAdapter();
						DropboxClientIntegration c = (DropboxClientIntegration) dbxFac
								.getClientAdapter();
						if (!c.isClientRunning()) {
							int ret = JOptionPane.showConfirmDialog(
									client.getMainWindow(),
									bundle.getString("PanboxClientGUI.dropboxNotRunningError"),
									bundle.getString("client.warn"),
									JOptionPane.YES_NO_OPTION);
							if (ret == JOptionPane.NO_OPTION) {
								return;
							}
						}

						if (!api.isOnline()) {
							int ret = JOptionPane.showConfirmDialog(client
									.getMainWindow(), bundle
									.getString("PanboxClientGUI.offlineError"),
									bundle.getString("client.warn"),
									JOptionPane.YES_NO_OPTION);
							if (ret == JOptionPane.NO_OPTION) {
								return;
							}
						}
					}

					if (share != null) {
						AddDeviceToShareDialog dialog = new AddDeviceToShareDialog(
								thisFrame, deviceModel, share.getDevices());
						dialog.setVisible(true);
						List<PanboxDevice> result = dialog.getResult();

						if (result.isEmpty()) {
							logger.debug("PanboxClientGUI : addDevice.addActionListener : Operation aborted!");
							return;
						}
						char[] password = PasswordEnterDialog
								.invoke(PermissionType.DEVICE);
						PanboxShare sharenew = null;
						try {
							client.showTrayMessage(
									bundle.getString("PleaseWait"),
									bundle.getString("tray.addDeviceToShare.waitMessage"),
									MessageType.INFO);
							client.getShareWatchService().removeShare(share);
							for (PanboxDevice dev : result) {
								DeviceShareParticipant dp = new DeviceShareParticipant(
										dev);
								sharenew = client.addPermissionToShare(share,
										dp, password);
								((ShareParticipantListModel) usersList
										.getModel()).addElement(dp);
							}
							if (sharenew != null) {
								shareModel.setElementAt(sharenew, selected);
							}
							client.showTrayMessage(
									bundle.getString("tray.addDeviceToShare.finishTitle"),
									bundle.getString("tray.addDeviceToShare.finishMessage"),
									MessageType.INFO);
						} catch (ShareDoesNotExistException e1) {
							logger.error("Share not found!", e1);
							JOptionPane.showMessageDialog(client
									.getMainWindow(), bundle
									.getString("PanboxClient.shareNotFound"),
									bundle.getString("client.error"),
									JOptionPane.ERROR_MESSAGE);
						} catch (ShareManagerException e1) {
							logger.error("Could not add device to share!", e1);
							JOptionPane.showMessageDialog(
									client.getMainWindow(),
									bundle.getString("PanboxClientGUI.errorWhileAddingDeviceToShare"),
									bundle.getString("client.error"),
									JOptionPane.ERROR_MESSAGE);
						} catch (UnrecoverableKeyException e1) {
							logger.error("Unable to recover key!", e1);
							JOptionPane.showMessageDialog(
									client.getMainWindow(),
									bundle.getString("PanboxClient.unableToRecoverKeys"),
									bundle.getString("client.error"),
									JOptionPane.ERROR_MESSAGE);
						} catch (ShareMetaDataException e1) {
							logger.error("Error in share metadata", e1);
							JOptionPane.showMessageDialog(
									client.getMainWindow(),
									bundle.getString("PanboxClientGUI.errorWhileAccessingShareMetadata"),
									bundle.getString("client.error"),
									JOptionPane.ERROR_MESSAGE);
						} finally {
							Utils.eraseChars(password);
							PanboxShare tmp = (sharenew != null) ? sharenew
									: share;
							client.getShareWatchService().registerShare(tmp);
							// Also update share list for selected device
							if (device != null) {
								deviceShareList.setModel(client
										.getDeviceShares(device));
							}
						}
					}
				} catch (OperationAbortedException ex) {
					System.out.println("Operation aborted!");
				}
			}
		});
		JMenuItem addContact = new JMenuItem(
				bundle.getString("shareUserList.contact"));
		addContact.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// check if user is share owner
				int selected = shareList.getSelectedIndex();
				if ((selected != -1)
						&& shareModel.getElementAt(selected).isOwner()) {
					boolean hasParticipants = (share.getContacts().size() != 0);
					PanboxShare share = shareModel.getElementAt(selected);

					if (share instanceof DropboxPanboxShare) {
						DropboxAdapterFactory dbxFac = (DropboxAdapterFactory) CSPAdapterFactory
								.getInstance(StorageBackendType.DROPBOX);
						DropboxAPIIntegration api = (DropboxAPIIntegration) dbxFac
								.getAPIAdapter();
						DropboxClientIntegration c = (DropboxClientIntegration) dbxFac
								.getClientAdapter();
						if (!c.isClientRunning()) {
							int ret = JOptionPane.showConfirmDialog(
									client.getMainWindow(),
									bundle.getString("PanboxClientGUI.dropboxNotRunningError"),
									bundle.getString("client.warn"),
									JOptionPane.YES_NO_OPTION);
							if (ret == JOptionPane.NO_OPTION) {
								return;
							}
						}

						if (!api.isOnline()) {
							int ret = JOptionPane.showConfirmDialog(client
									.getMainWindow(), bundle
									.getString("PanboxClientGUI.offlineError"),
									bundle.getString("client.warn"),
									JOptionPane.YES_NO_OPTION);
							if (ret == JOptionPane.NO_OPTION) {
								return;
							}
						}
					}

					// AddContactToShareWizard contactWizard;
					// try {
					// contactWizard = new AddContactToShareWizard(thisFrame,
					// client, share, contactModel);
					// } catch (Exception e2) {
					// e2.printStackTrace();
					// return;
					// }
					//
					// contactWizard.setVisible(true);

					AddContactToShareDialog dialog = new AddContactToShareDialog(
							thisFrame, contactModel, share.getContacts());
					dialog.setVisible(true);

					List<PanboxGUIContact> result;
					try {
						result = dialog.getResult();
					} catch (OperationAbortedException e2) {
						logger.debug("PanboxClientGUI : addContact.addActionListener : Operation aborted!");
						return;
					}

					if (result.isEmpty()) {
						logger.debug("PanboxClientGUI : addContact.addActionListener : Operation aborted!");
						return;
					}

					char[] password = PasswordEnterDialog
							.invoke(PermissionType.USER);
					PanboxShare sharenew = null;
					try {
						client.showTrayMessage(
								bundle.getString("PleaseWait"),
								bundle.getString("tray.addContactToShare.waitMessage"),
								MessageType.INFO);
						ArrayList<PanboxGUIContact> reallyAdded = new ArrayList<PanboxGUIContact>();
						client.getShareWatchService().removeShare(share);
						for (int i = 0; i < result.size(); i++) {
							// add permission for user
							ContactShareParticipant cp = new ContactShareParticipant(
									result.get(i));
							if (Settings.getInstance().getExpertMode() && !cp.getContact().isVerified()) {
								int res = JOptionPane.showConfirmDialog(
										client.getMainWindow(),
										MessageFormat.format(
												bundle.getString("PanboxClientGUI.addUnverifiedContactWarning.text"),
												cp.getName()),
										bundle.getString("PanboxClientGUI.addUnverifiedContactWarning.title"),
										JOptionPane.YES_NO_OPTION,
										JOptionPane.WARNING_MESSAGE);

								if (res == JOptionPane.YES_OPTION) {
									sharenew = client.addPermissionToShare(
											share, cp, password);
									logger.info("Added unverified contact "
											+ cp.getName() + " to share.");
									reallyAdded.add(result.get(i));
									((ShareParticipantListModel) usersList
											.getModel()).addElement(cp);
								} else if (res == JOptionPane.NO_OPTION) {
									logger.info("Skipped adding unverified contact "
											+ cp.getName() + " to share.");
									continue;
								}
							} else {
								sharenew = client.addPermissionToShare(share,
										cp, password);
								reallyAdded.add(result.get(i));
								((ShareParticipantListModel) usersList
										.getModel()).addElement(cp);
							}
						}
						if (reallyAdded.size() > 0) {
							shareModel.setElementAt(sharenew, selected);
							handleCSPShareParticipantConfiguration(sharenew,
									hasParticipants, reallyAdded);
						}
						client.showTrayMessage(
								bundle.getString("tray.addContactToShare.finishTitle"),
								bundle.getString("tray.addContactToShare.finishMessage"),
								MessageType.INFO);
					} catch (ShareDoesNotExistException e1) {
						logger.error("Share not found!", e1);
						JOptionPane.showMessageDialog(client.getMainWindow(),
								bundle.getString("PanboxClient.shareNotFound"),
								bundle.getString("client.error"),
								JOptionPane.ERROR_MESSAGE);
					} catch (ShareManagerException e1) {
						logger.error("Could not add contact to share!", e1);
						JOptionPane.showMessageDialog(
								client.getMainWindow(),
								bundle.getString("PanboxClientGUI.errorWhileAddingContactToShare"),
								bundle.getString("client.error"),
								JOptionPane.ERROR_MESSAGE);
					} catch (UnrecoverableKeyException e1) {
						logger.error("Unable to recover key!", e1);
						JOptionPane.showMessageDialog(
								client.getMainWindow(),
								bundle.getString("PanboxClient.unableToRecoverKeys"),
								bundle.getString("client.error"),
								JOptionPane.ERROR_MESSAGE);
					} catch (ShareMetaDataException e1) {
						logger.error("Error in share metadata", e1);
						JOptionPane.showMessageDialog(
								client.getMainWindow(),
								bundle.getString("PanboxClientGUI.errorWhileAccessingShareMetadata"),
								bundle.getString("client.error"),
								JOptionPane.ERROR_MESSAGE);
					} finally {
						Utils.eraseChars(password);
						PanboxShare tmp = (sharenew != null) ? sharenew : share;
						client.getShareWatchService().registerShare(tmp);
					}
				}

			}
		});
		menu.add(addDevice);
		menu.add(addContact);
		menu.show(addDeviceContactShareButton, evt.getX(), evt.getY());
	}// GEN-LAST:event_addDeviceContactShareButtonMousePressed

	/**
	 * helper method for handling CSP-side access control configuration, after
	 * having invited or removed single users from or to a share.
	 * 
	 * @param share
	 *            share for whichparticipant list has changed
	 * @param shareHasParticipants
	 *            <code>true</code> if folder has just been shared initially and
	 *            participants have only been added
	 */
	private void handleCSPShareParticipantConfiguration(PanboxShare share,
			boolean shareHasParticipants,
			List<PanboxGUIContact> selectedcontacts) {
		if (share instanceof DropboxPanboxShare) {
			DropboxAdapterFactory dbxFac = (DropboxAdapterFactory) CSPAdapterFactory
					.getInstance(StorageBackendType.DROPBOX);
			DropboxAPIIntegration dbIntegration = (DropboxAPIIntegration) dbxFac
					.getAPIAdapter();

			// before we continue, first check if directory has already been
			// synced to csp
			String shareid = FilenameUtils.getName(share.getPath());
			boolean shareIsOnline, shareConfigured = false;
			try {
				shareIsOnline = ((shareid != null) && (shareid.length() > 0) && dbIntegration
						.exists("/" + shareid));
			} catch (CSPApiException e) {
				logger.error("Could not determine if share with path "
						+ share.getPath() + " and shareid: " + shareid
						+ " has already been uploaded!");
				shareIsOnline = false;
			}

			if (shareIsOnline) {
				String message = bundle
						.getString("PanboxClientGUI.openDropboxShareConfig");
				JCheckBox copyToClipboard = new JCheckBox(
						bundle.getString("PanboxClientGUI.copyEmailToClipboard"));
				Object[] params = new Object[] { message, copyToClipboard };

				int res = JOptionPane.showConfirmDialog(this, params,
						bundle.getString("PanboxClientGUI.openShareConfig"),
						JOptionPane.YES_NO_OPTION);

				if (res == JOptionPane.YES_OPTION) {
					if (copyToClipboard.isSelected()) {
						String emails = PanboxGUIContact
								.getMailAsSepteratedValues(selectedcontacts,
										";", StorageBackendType.DROPBOX);
						// copy the email of the selected contact into the
						// clipboard
						DesktopApi.copyToClipboard(emails.toString(), false);
					}

					try {
						if (shareHasParticipants) {
							// shows configuration page for shares with existing
							// users in case of dropbox
							dbIntegration.removeUser(shareid);
						} else {
							// shows initial setup page for new shares
							dbIntegration.inviteUser(shareid);
						}
						shareConfigured = true;
					} catch (Exception e) {
						logger.error(
								"handleCSPShareParticipantConfiguration: Error opening share configuration",
								e);
						JOptionPane
								.showMessageDialog(
										this,
										bundle.getString("PanboxClientGUI.errorOpeningShareConfig"),
										bundle.getString("error"),
										JOptionPane.ERROR_MESSAGE);
					}
				}
			} else {
				// JOptionPane
				// .showMessageDialog(
				// this,
				// bundle.getString("PublishIdentitiesDialog.fileNotFoundInCloudStorage"),
				// bundle.getString("PublishIdentitiesDialog.fileNotFound"),
				// JOptionPane.WARNING_MESSAGE);
			}

			if (Settings.getInstance().isMailtoSchemeSupported()) {
				// now, offer to send panbox share invitation link
				String message;
				if (shareConfigured) {
					message = bundle
							.getString("PanboxClientGUI.sendInvitationLink.message");
				} else {
					if (shareIsOnline) {
						message = bundle
								.getString("PanboxClientGUI.sendInvitationLinkNotConfigured.message");
					} else {
						message = bundle
								.getString("PanboxClientGUI.sendInvitationLinkNoSync.message");
					}
				}

				int ret = JOptionPane.showConfirmDialog(this, message, bundle
						.getString("PanboxClientGUI.InvitationLink.title"),
						JOptionPane.YES_NO_OPTION);
				if (ret == JOptionPane.YES_OPTION) {
					String emails = PanboxGUIContact.getMailAsSepteratedValues(
							selectedcontacts, ",", StorageBackendType.DROPBOX);

					String mailto = "mailto:"
							+ emails
							+ "?subject="
							+ bundle.getString("client.mailTo.shareInvitationSubject")
							+ "&body="
							+ PanboxURICmdShareInvitation.getPanboxLink(share
									.getUuid().toString(), share.getType()
									.name());

					DesktopApi.browse(URI.create(mailto));
				}
			} else {
				// offer to copy panbox share invitation link to
				// clipboard
				String message;
				if (shareConfigured) {
					message = bundle
							.getString("PanboxClientGUI.copyInvitationLink.message");
				} else {
					if (shareIsOnline) {
						message = bundle
								.getString("PanboxClientGUI.copyInvitationLinkNotConfigured.message");
					} else {
						message = bundle
								.getString("PanboxClientGUI.copyInvitationLinkNoSync.message");
					}
				}

				int ret = JOptionPane.showConfirmDialog(this, message, bundle
						.getString("PanboxClientGUI.InvitationLink.title"),
						JOptionPane.YES_NO_OPTION);
				if (ret == JOptionPane.YES_OPTION) {
					URI uri = PanboxURICmdShareInvitation.getPanboxLink(share
							.getUuid().toString(), share.getType().name());

					DesktopApi.copyToClipboard(uri.toASCIIString(), true);

					JOptionPane
							.showMessageDialog(
									this,
									bundle.getString("PanboxClientGUI.copyInvitationLink.info")
											+ "\n" + uri.toASCIIString(),
									bundle.getString("PanboxClientGUI.InvitationLink.title"),
									JOptionPane.INFORMATION_MESSAGE);
				}

			}

		} else {
			logger.warn("handleCSPShareParticipantConfiguration: Unknown share type!");
		}
	}

	// TODO: This feature is not supported for now!
	// private void removeDeviceContactShareButtonMousePressed(
	// java.awt.event.MouseEvent evt) {//
	// GEN-FIRST:event_removeDeviceContactShareButtonMousePressed
	// final JFrame thisFrame = this;
	// int selected = shareList.getSelectedIndex();
	// if ((selected != -1)) {
	// PanboxShare share = shareModel.getElementAt(selected);
	// ShareParticipantListModel model = (ShareParticipantListModel) usersList
	// .getModel();
	// Object o = usersList.getSelectedValue();
	// if (share != null && o != null) {
	// if (o instanceof PanboxSharePermission) {
	// char[] password = PasswordEnterDialog
	// .invoke(PermissionType.SHARE);
	// PanboxSharePermission p = (PanboxSharePermission) o;
	// client.removePermissionFromShare(share, p, password);
	// model.removeElement(p);
	// Arrays.fill(password, (char) 0);
	// }
	// }
	// }
	// }// GEN-LAST:event_removeDeviceContactShareButtonMousePressed

	private void importContactButtonActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_importContactButtonActionPerformed
		ImportIdentitiesWoPINDialog d = new ImportIdentitiesWoPINDialog(client);
		d.setLocationRelativeTo(this);
		d.setVisible(true);
	}// GEN-LAST:event_importContactButtonActionPerformed

	private void removeContactButtonActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_removeContactButtonActionPerformed
		// when mycontact is selcted, remove button should be disabled. just to
		// be sure
		int res = JOptionPane.showConfirmDialog(this, bundle
				.getString("PanboxClientGUI.reallyRemoveSelectedContacts"),
				bundle.getString("PanboxClientGUI.removeContacts"),
				JOptionPane.YES_NO_OPTION);
		if (res == JOptionPane.YES_OPTION) {
			if (!(addressbookList.isSelectionEmpty())) {
				for (PanboxGUIContact c : contacts) {
					if (!(c instanceof PanboxMyContact)) {
						client.removeContact(c);
					}
				}
			}
		}
	}// GEN-LAST:event_removeContactButtonActionPerformed

	private void exportContactButtonActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_exportContactButtonActionPerformed
		ExportIdentitiesWoPINDialog exportDialog = new ExportIdentitiesWoPINDialog(
				client, contacts);
		exportDialog.setLocationRelativeTo(this);
		exportDialog.setVisible(true);
	}// GEN-LAST:event_exportContactButtonActionPerformed

	private void addCSPInfoButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_addCSPInfoButtonActionPerformed
		CSPTableModel model = (CSPTableModel) cspInfoTable.getModel();
		if (!uneditedCSPsExist()) {
			ArrayList<String> usedProviders = new ArrayList<>();
			for (int i = 0; i < model.getRowCount(); i++) {
				usedProviders.add((String) model.getValueAt(i, 0));
			}
			cspInfoTable.getColumnModel().getColumn(0)
					.setCellEditor(new CSPTableCellEditor(usedProviders));
			model.addRow(new String[2]);
			cspInfoTable.editCellAt(model.getRowCount() - 1, 0); // select added
			// row first
			// cell
			if (addedCSPCount > 0) {
				model.maxPlus();
			}
			addedCSPCount++;
			setContactChangesDetected();
		}
	}// GEN-LAST:event_addCSPInfoButtonActionPerformed

	private boolean uneditedCSPsExist() {
		if (cspInfoTable.getModel() instanceof CSPTableModel) {
			CSPTableModel model = (CSPTableModel) cspInfoTable.getModel();
			if (model.getRowCount() > 0
					&& (model.getValueAt(model.getRowCount() - 1, 1) == null || model
							.getValueAt(model.getRowCount() - 1, 1).equals(""))) {
				JOptionPane.showConfirmDialog(null,
						bundle.getString("PanboxClientGUI.lastCSPNotFinished"),
						bundle.getString("PanboxClientGUI.panboxMessage"),
						JOptionPane.DEFAULT_OPTION);
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	private void removeCSPInfoButtonActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_removeCSPInfoButtonActionPerformed
		int[] selectedRows = cspInfoTable.getSelectedRows();
		ArrayUtils.reverse(selectedRows);
		for (int selectedRow : selectedRows) {
			String cspName = cspInfoTable.getModel().getValueAt(selectedRow, 0)
					.toString();
			removedCSPs.add(contact.getCloudProvider(cspName));
			CSPTableModel model = (CSPTableModel) cspInfoTable.getModel();
			model.removeRow(selectedRow);
			model.maxMinus();
		}
		setContactChangesDetected();
	}// GEN-LAST:event_removeCSPInfoButtonActionPerformed

	private void contactDiscardButtonActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_contactDiscardButtonActionPerformed
		refreshContact();
		resetContactApplyDiscardButtons();
	}// GEN-LAST:event_contactDiscardButtonActionPerformed

	private void contactApplyButtonActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_contactApplyButtonActionPerformed
		if (!uneditedCSPsExist()) {
			saveContactChanges();
			refreshContact();
			resetContactApplyDiscardButtons();
			addressbookList.requestFocus(); // this fixes the contact refresh
			// bug somehow
		}

	}// GEN-LAST:event_contactApplyButtonActionPerformed

	private void setContactChangesDetected() {
		contactApplyButton.setEnabled(true);
		contactDiscardButton.setEnabled(true);
		unsavedContactChanges = true;
	}

	private void resetContactApplyDiscardButtons() {
		contactApplyButton.setEnabled(false);
		contactDiscardButton.setEnabled(false);
		unsavedContactChanges = false;
		removedCSPs.clear();
		addedCSPCount = 0;
	}

	private void refreshContact() {
		firstNameTextField.setText(contact.getFirstName());
		lastNameTextField.setText(contact.getName());
		cspInfoTable.setModel(contact.generateCspInfoTableModel());
		if (contact.getAvailableCSPs() > 0) {
			addCSPInfoButton.setEnabled(true);
		} else {
			addCSPInfoButton.setEnabled(false);
		}
		if (contact.isVerified()) {
			contactVerificationStatusCheckBox.setSelected(true);
			contactVerificationStatusCheckBox.setText(bundle
					.getString("PanboxClientGUI.contact.verified"));
		} else {
			contactVerificationStatusCheckBox.setSelected(false);
			contactVerificationStatusCheckBox.setText(bundle
					.getString("PanboxClientGUI.contact.verified"));
		}
	}

	private void saveContactChanges() {
		DefaultTableModel model = (DefaultTableModel) cspInfoTable.getModel();
		for (CloudProviderInfo csp : removedCSPs) {
			contact.removeCloudProvider(csp.getProviderName());
		}
		ArrayList<CloudProviderInfo> addedCSPs = new ArrayList<>();
		for (int i = 0; i < addedCSPCount; i++) {
			String providerName = (String) model.getValueAt(model.getRowCount()
					- addedCSPCount + i, 0);
			String username = (String) model.getValueAt(model.getRowCount()
					- addedCSPCount + i, 1);
			CloudProviderInfo cpi = new CloudProviderInfo(providerName,
					username);
			addedCSPs.add(cpi);
			contact.addCloudProvider(cpi);
		}
		if (contact instanceof PanboxMyContact) {
			client.saveMyContact(removedCSPs, addedCSPs);
		} else {
			String newFirstName = firstNameTextField.getText();
			String newName = lastNameTextField.getText();
			// contact.setFirstName(newFirstName);
			// contact.setName(newName);
			client.saveContact(contact, newFirstName, newName, removedCSPs,
					addedCSPs, contactVerificationStatusCheckBox.isSelected());
		}
	}

	private void setSettingsChangesDetected() {
		settingsApplyButton.setEnabled(true);
		settingsRevertButton.setEnabled(true);
		unsavedSettings = true;
	}

	private void resetSettingsApplyDiscardButtons() {
		settingsApplyButton.setEnabled(false);
		settingsRevertButton.setEnabled(false);
		unsavedSettings = false;
	}

	private void settingsFolderChooseButtonActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_settingsFolderChooseButtonActionPerformed
		JFileChooser settingsFolderChooser = new JFileChooser();
		settingsFolderChooser
				.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int returnVal = settingsFolderChooser.showOpenDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = settingsFolderChooser.getSelectedFile();
			settingsFolderTextField.setText(file.getAbsolutePath());
		}
	}// GEN-LAST:event_settingsFolderChooseButtonActionPerformed

	private void panboxFolderChooseButtonActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_panboxFolderChooseButtonActionPerformed
		JFileChooser panboxFolderChooser = new JFileChooser();
		panboxFolderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int returnVal = panboxFolderChooser.showOpenDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = panboxFolderChooser.getSelectedFile();
			panboxFolderTextField.setText(file.getAbsolutePath());
		}
	}// GEN-LAST:event_panboxFolderChooseButtonActionPerformed

	private void settingsRevertButtonActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_settingsRevertButtonActionPerformed
		initSettingsConfig();
		dropboxSettingsPanel.refresh();
		resetSettingsApplyDiscardButtons();
	}// GEN-LAST:event_settingsRevertButtonActionPerformed

	private void settingsApplyButtonActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_settingsApplyButtonActionPerformed

		Settings s = Settings.getInstance();
		boolean languageChanged = false, restartRequired = false;
		String selLang = ((SupportedLanguage) languageComboBox
				.getSelectedItem()).getShorthand();
		if (!selLang.equals(s.getLanguage())) {
			s.setLanguage(selLang);
			// client.restartTrayIcon();
			languageChanged = true;
			restartRequired = true;
		}

		if (!(expertModeCheckBox.isSelected() == s.getExpertMode())) {
			s.setExpertMode(expertModeCheckBox.isSelected());
		}

		if (!(uriHandlerCheckbox.isSelected() == s.isUriHandlerSupported())) {
			s.setUriHandlerSupported(uriHandlerCheckbox.isSelected());
			restartRequired = true;
		}

		if (uriHandlerCheckbox.isSelected()
				&& !(clipboardHandlerCheckbox.isSelected() == s
						.isClipboardHandlerSupported())) {
			s.setClipboardHandlerSupported(clipboardHandlerCheckbox
					.isSelected());
			restartRequired = true;
		}

		if (!(mailtoSchemeCheckbox.isSelected() == s.isMailtoSchemeSupported())) {
			s.setMailtoSchemeSupported(mailtoSchemeCheckbox.isSelected());
			restartRequired = true;
		}

		File newConfDir = new File(settingsFolderTextField.getText());
		File oldConfDir = new File(s.getConfDir());
		if (!FilenameUtils.equalsNormalizedOnSystem(
				newConfDir.getAbsolutePath(), oldConfDir.getAbsolutePath())) {
			if (newConfDir.exists()) {
				s.setConfDir(FilenameUtils.normalizeNoEndSeparator(newConfDir
						.getAbsolutePath()));
				client.settingsFolderChanged(newConfDir);
			} else {
				JOptionPane.showMessageDialog(this, bundle
						.getString("client.settings.panboxConfDirNotExisting"));
				settingsFolderTextField.setText(s.getConfDir());
			}
		}

		InetAddress address = (InetAddress) networkAddressComboBox.getModel()
				.getSelectedItem();
		s.setPairingAddress(address);

		// save csp specific settings
		if (selectedCSPContentPanel.getComponentCount() > 0) {
			Component c = selectedCSPContentPanel.getComponent(0);
			if ((c != null) && (c instanceof Flushable)) {
				try {
					((Flushable) c).flush();
				} catch (IOException e) {
					logger.error("Error flushing csp settings config panel!", e);
				}
			} else {
				logger.error("Invalid csp content panel content!");
			}
		}

		// Linux only!
		if (OS.getOperatingSystem().isLinux()) {
			String newPath = panboxFolderTextField.getText();
			String oldPath = s.getMountDir();
			if (!FilenameUtils.equalsNormalizedOnSystem(newPath, oldPath)) {
				s.setMountDir(newPath);
				// client.panboxFolderChanged(newPath);
				// TODO: currently not needed, readd later
				restartRequired = true;
			}

		}

		if (restartRequired) {
			int ret = JOptionPane.showConfirmDialog(this,
					bundle.getString("PanboxClientGUI.restartMessage"),
					bundle.getString("PanboxClientGUI.restartMessage.title"),
					JOptionPane.YES_NO_OPTION);
			try {
				if (ret == JOptionPane.YES_OPTION) {
					client.restartApplication();
				}
			} catch (IOException e) {
				logger.error("Error while restarting the appication!", e);
				JOptionPane.showMessageDialog(this,
						bundle.getString("PanboxClientGUI.restartError"),
						bundle.getString("error"), JOptionPane.ERROR_MESSAGE);
			}
		}

		// reset the buttons
		resetSettingsApplyDiscardButtons();

	}// GEN-LAST:event_settingsApplyButtonActionPerformed

	private void addDeviceButtonMousePressed(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_addDeviceButtonMousePressed
		JPopupMenu menu = new JPopupMenu();
		JMenuItem bluetooth = new JMenuItem(
				bundle.getString("PanboxClientGui.bluetooth"));
		bluetooth.addActionListener(new AddDeviceBluetoothActionListener(
				client, this));
		JMenuItem file = new JMenuItem(bundle.getString("PanboxClientGui.File"));
		file.addActionListener(new AddDeviceFileActionListener(client, this));
		JMenuItem lan = new JMenuItem(
				bundle.getString("PanboxClientGui.lanandwlan"));
		lan.addActionListener(new AddDeviceNetworkActionListener(client, this));
		menu.add(bluetooth);
		menu.add(file);
		menu.add(lan);
		menu.show(addDeviceButton, evt.getX(), evt.getY());
	}// GEN-LAST:event_addDeviceButtonMousePressed

	private void networkInterfaceComboBoxActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_networkInterfaceComboBoxActionPerformed
		NetworkInterface nic = (NetworkInterface) networkInterfaceComboBox
				.getModel().getSelectedItem();
		networkAddressComboBox.setModel(generateNetworkAddressModel(nic));
	}// GEN-LAST:event_networkInterfaceComboBoxActionPerformed

	private void networkAddressComboBoxActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_networkAddressComboBoxActionPerformed
		// nothing to do here for now...
	}// GEN-LAST:event_networkAddressComboBoxActionPerformed

	private void initSettingsConfig() {
		Settings s = Settings.getInstance();

		settingsFolderTextField.setText(s.getConfDir());
		panboxFolderTextField.setText(s.getMountDir());
		languageComboBox.setSelectedItem(SupportedLanguage.fromShorthand(s
				.getLanguage()));
		expertModeCheckBox.setSelected(s.getExpertMode());
		uriHandlerCheckbox.setSelected(s.isUriHandlerSupported());
		if (uriHandlerCheckbox.isSelected()) {
			clipboardHandlerCheckbox.setEnabled(true);
			clipboardHandlerCheckbox.setSelected(s
					.isClipboardHandlerSupported());
		} else {
			clipboardHandlerCheckbox.setEnabled(false);
			clipboardHandlerCheckbox.setSelected(false);
		}
		mailtoSchemeCheckbox.setSelected(s.isMailtoSchemeSupported());
		selectedCSPContentPanel.removeAll();
		cspSelectionComboBox.setSelectedIndex(-1);

		NetworkInterface nic;
		try {
			DefaultComboBoxModel<NetworkInterface> model = generateNetworkInterfacesModel();
			nic = NetworkInterface.getByInetAddress(s.getPairingAddress());
			if (nic == null) {
				// The configured IP address does not exist anymore! Will reset
				// to localhost
				s.setPairingAddress(InetAddress.getByName("localhost"));
				nic = NetworkInterface.getByInetAddress(s.getPairingAddress());
			}
			model.setSelectedItem(nic);
			networkInterfaceComboBox.setModel(model);
			networkAddressComboBox.setModel(generateNetworkAddressModel(nic));
		} catch (SocketException | UnknownHostException e) {
			logger.warn("Paiting settings Exception", e);
		}
	}

	private DefaultComboBoxModel<NetworkInterface> generateNetworkInterfacesModel() {
		DefaultComboBoxModel<NetworkInterface> model = new DefaultComboBoxModel<>();
		try {
			List<NetworkInterface> nics = Collections.list(NetworkInterface
					.getNetworkInterfaces());
			for (NetworkInterface nic : nics) {
				List<InetAddress> addrs = Collections.list(nic
						.getInetAddresses());
				if (addrs.size() > 0) {
					model.addElement(nic);
				}
			}
		} catch (SocketException e) {
			logger.warn(
					"An exception occurred while iterating over available network interfaces. Will return unfinished list: ",
					e);
		}
		return model;
	}

	private DefaultComboBoxModel<InetAddress> generateNetworkAddressModel(
			NetworkInterface nic) {
		DefaultComboBoxModel<InetAddress> model = new DefaultComboBoxModel<>();
		List<InetAddress> addrs = Collections.list(nic.getInetAddresses());
		for (InetAddress addr : addrs) {
			if(addr instanceof Inet4Address) { // ignore IPv6 addresses!!!
				model.addElement(addr);
			}
		}
		return model;
	}

	private DefaultComboBoxModel<String> generateCSPSelectionModel() {
		DefaultComboBoxModel<String> ret = new DefaultComboBoxModel<String>();
		for (StorageBackendType t : StorageBackendType.values()) {
			if (t != StorageBackendType.FOLDER) {
				if (t == StorageBackendType.DROPBOX) {
					// Check if Dropbox is installed. If yes -> Add to list
					DropboxAdapterFactory dropboxAdapterFactory = (DropboxAdapterFactory) CSPAdapterFactory
							.getInstance(StorageBackendType.DROPBOX);
					DropboxClientIntegration dropboxClientIntegration = (DropboxClientIntegration) dropboxAdapterFactory
							.getClientAdapter();
					try {
						if (dropboxClientIntegration.isClientInstalled()) {
							ret.addElement(t.getDisplayName());
						}
					} catch (IOException e) {
						// an error occurred, but it could be possible that
						// Dropbox is installed
						ret.addElement(t.getDisplayName());
					}
				} else {
					ret.addElement(t.getDisplayName());
				}
			}
		}
		if (ret.getSize() == 0) {
			cspSelectionComboBox.setEnabled(false);
		}
		return ret;
	}

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JButton addCSPInfoButton;
	private javax.swing.JButton addDeviceButton;
	private javax.swing.JButton addDeviceContactShareButton;
	private javax.swing.JButton addShare;
	private javax.swing.JList<PanboxGUIContact> addressbookList;
	private javax.swing.JScrollPane addressbookListScrollPane;
	private javax.swing.JPanel addressbookPanel;
	private javax.swing.JPanel addressbookTabPanel;
	private javax.swing.JCheckBox clipboardHandlerCheckbox;
	private javax.swing.JButton contactApplyButton;
	private javax.swing.JButton contactDiscardButton;
	private javax.swing.JLabel contactPropertiesLabel;
	private javax.swing.JPanel contactPropertiesPanell;
	private javax.swing.JCheckBox contactVerificationStatusCheckBox;
	private javax.swing.JLabel cspAccountsLabel;
	private javax.swing.JTable cspInfoTable;
	private javax.swing.JScrollPane cspInfoTableScrollPanel;
	private javax.swing.JComboBox cspSelectionComboBox;
	private javax.swing.JPanel cspSettingsPanel;
	private javax.swing.JLabel deviceKeyFprintLabel;
	private javax.swing.JTextField deviceKeyFprintTextField;
	private javax.swing.JList<PanboxDevice> deviceList;
	private javax.swing.JPanel deviceListPanel;
	private javax.swing.JScrollPane deviceListScrollPane;
	private javax.swing.JLabel devicePropertiesLabel;
	private javax.swing.JPanel devicePropertiesPanel;
	private javax.swing.JList<PanboxShare> deviceShareList;
	private javax.swing.JPanel devicesTabPanel;
	private javax.swing.JLabel emailLabel;
	private javax.swing.JTextField emailTextField;
	private javax.swing.JLabel encKeyFprintLabel;
	private javax.swing.JTextField encKeyFprintTextField;
	private javax.swing.JCheckBox expertModeCheckBox;
	private javax.swing.JPanel expertModeContactPanel;
	private javax.swing.JPanel expertModeDevicePanel;
	private javax.swing.JButton exportContactButton;
	private javax.swing.JLabel firstNameLabel;
	private javax.swing.JTextField firstNameTextField;
	private javax.swing.JButton importContactButton;
	private javax.swing.JLabel jLabel1;
	private javax.swing.JComboBox languageComboBox;
	private javax.swing.JLabel languageLabel;
	private javax.swing.JLabel lastNameLabel;
	private javax.swing.JTextField lastNameTextField;
	private javax.swing.JCheckBox mailtoSchemeCheckbox;
	private javax.swing.JTabbedPane mainTabbedPane;
	private javax.swing.JComboBox<InetAddress> networkAddressComboBox;
	private javax.swing.JLabel networkAddressLabel;
	private javax.swing.JLabel networkDevicePairingLabel;
	private javax.swing.JPanel networkDevicePairingPanel;
	private javax.swing.JComboBox<NetworkInterface> networkInterfaceComboBox;
	private javax.swing.JLabel networkInterfaceLabel;
	private javax.swing.JTextField ownerTextField;
	private javax.swing.JButton panboxFolderChooseButton;
	private javax.swing.JLabel panboxFolderLabel;
	private javax.swing.JTextField panboxFolderTextField;
	private javax.swing.JLabel permissionsLabel;
	private javax.swing.JButton publishContactButton;
	private javax.swing.JButton removeCSPInfoButton;
	private javax.swing.JButton removeContactButton;
	private javax.swing.JButton removeDeviceButton;
	private javax.swing.JButton removeDeviceContactShareButton;
	private javax.swing.JButton removeShare;
	private javax.swing.JButton restoreRevButton;
	private javax.swing.JPanel selectedCSPContentPanel;
	private javax.swing.JLabel selectedCSPLabel;
	private javax.swing.JButton settingsApplyButton;
	private javax.swing.JButton settingsFolderChooseButton;
	private javax.swing.JLabel settingsFolderLabel;
	private javax.swing.JTextField settingsFolderTextField;
	private javax.swing.JButton settingsRevertButton;
	private javax.swing.JPanel settingsTabPanel;
	private javax.swing.JList<PanboxShare> shareList;
	private javax.swing.JPanel shareListPanel;
	private javax.swing.JScrollPane shareListScrollPane;
	private javax.swing.JPanel shareListTabPanel;
	private javax.swing.JLabel sharePropertiesLabel;
	private javax.swing.JPanel sharePropertiesPanel;
	private javax.swing.JLabel signKeyFprintLabel;
	private javax.swing.JTextField signKeyFprintTextField;
	private javax.swing.JLabel syncStatusLabel;
	private javax.swing.JTextField syncStatusTextField;
	private javax.swing.JCheckBox uriHandlerCheckbox;
	private javax.swing.JLabel urlLabel;
	private javax.swing.JTextField urlTextField;
	private javax.swing.JLabel usersLabel;
	private javax.swing.JList usersList;
	private javax.swing.JScrollPane usersListScrollPane;
	private javax.swing.JScrollPane usersListScrollPane1;
	private javax.swing.JLabel validFromUntilLabel;
	// End of variables declaration//GEN-END:variables
}
