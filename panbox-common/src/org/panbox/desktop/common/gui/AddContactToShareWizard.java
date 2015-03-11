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

import java.awt.TrayIcon.MessageType;
import java.io.File;
import java.net.URI;
import java.security.UnrecoverableKeyException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SizeRequirements;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.Element;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.InlineView;
import javax.swing.text.html.ParagraphView;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.panbox.Settings;
import org.panbox.core.Utils;
import org.panbox.core.csp.CSPAdapterFactory;
import org.panbox.core.csp.StorageBackendType;
import org.panbox.core.exception.ShareMetaDataException;
import org.panbox.desktop.common.PanboxClient;
import org.panbox.desktop.common.gui.PasswordEnterDialog.PermissionType;
import org.panbox.desktop.common.gui.addressbook.ContactListCellRenderer;
import org.panbox.desktop.common.gui.addressbook.ContactListModel;
import org.panbox.desktop.common.gui.addressbook.ContactShareParticipant;
import org.panbox.desktop.common.gui.addressbook.PanboxGUIContact;
import org.panbox.desktop.common.gui.addressbook.PanboxMyContact;
import org.panbox.desktop.common.gui.shares.PanboxShare;
import org.panbox.desktop.common.sharemgmt.ShareDoesNotExistException;
import org.panbox.desktop.common.sharemgmt.ShareManagerException;
import org.panbox.desktop.common.urihandler.PanboxURICmdShareInvitation;
import org.panbox.desktop.common.utils.DesktopApi;
import org.panbox.desktop.common.vfs.backend.dropbox.CSPApiException;
import org.panbox.desktop.common.vfs.backend.dropbox.DropboxAPIIntegration;

public class AddContactToShareWizard extends javax.swing.JDialog {

	@SuppressWarnings("serial")
	public class CustomEditorKit extends HTMLEditorKit {

		@Override
		public ViewFactory getViewFactory() {

			return new HTMLFactory() {
				@Override
				public View create(Element e) {
					View v = super.create(e);
					if (v instanceof InlineView) {
						return new InlineView(e) {
							@Override
							public int getBreakWeight(int axis, float pos,
									float len) {
								return GoodBreakWeight;
							}

							@Override
							public View breakView(int axis, int p0, float pos,
									float len) {
								if (axis == View.X_AXIS) {
									this.checkPainter();
									this.removeUpdate(null, null, null);
								}
								return super.breakView(axis, p0, pos, len);
							}
						};
					} else if (v instanceof ParagraphView) {
						return new ParagraphView(e) {
							@Override
							protected SizeRequirements calculateMinorAxisRequirements(
									int axis, SizeRequirements r) {
								if (r == null) {
									r = new SizeRequirements();
								}
								float pref = this.layoutPool
										.getPreferredSpan(axis);
								float min = this.layoutPool
										.getMinimumSpan(axis);
								// Don't include insets, Box.getXXXSpan will
								// include them.
								r.minimum = (int) min;
								r.preferred = Math.max(r.minimum, (int) pref);
								r.maximum = Integer.MAX_VALUE;
								r.alignment = 0.5f;
								return r;
							}

						};
					}
					return v;
				}
			};
		}
	}

	/**
	 *
	 */
	private static final long serialVersionUID = 4892202577910829210L;

	private final static Logger logger = Logger
			.getLogger(AddContactToShareWizard.class);

	private PanboxShare share;

	private static final ResourceBundle bundle = ResourceBundle.getBundle(
			"org.panbox.desktop.common.gui.Messages", Settings.getInstance()
					.getLocale());

	private PanboxClient client;

	private DropboxAPIIntegration dbapi;

	private boolean hasParticipants;

	private ContactListModel mainModel;

	public AddContactToShareWizard(java.awt.Frame parent, PanboxClient client,
			PanboxShare share, ContactListModel model) throws Exception {
		super(parent);
		this.client = client;
		this.share = share;
		this.mainModel = model;

		this.dbapi = (DropboxAPIIntegration) CSPAdapterFactory.getInstance(
				StorageBackendType.DROPBOX).getAPIAdapter();

		this.hasParticipants = (share.getContacts().size() != 0);

		initComponents();
		setLocationRelativeTo(parent);

		this.contactList.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {
					@Override
					public void valueChanged(ListSelectionEvent e) {
						changedStep();
					}
				});

		changedStep();
	}

	private ContactListModel genContactModel() {
		ContactListModel modelcopy = new ContactListModel();
		for (int i = 0, ctr = 0; i < mainModel.getSize(); i++) {
			PanboxGUIContact ccur = mainModel.get(i);
			List<PanboxGUIContact> clist = share.getContacts();
			// load contacts that may be added
			if (!(ccur instanceof PanboxMyContact) && !clist.contains(ccur)
					&& !clist.contains(ccur)) {
				modelcopy.add(ctr++, ccur);
			}
		}
		return modelcopy;
	}

	private int currentStep = this.STEP_CONTACT_SELECTION; // initial value

	private final int STEP_CONTACT_SELECTION = 0;
	private final int STEP_CSP_ACCESSCONTROL = 1;
	private final int STEP_CREATE_INVITATION = 2;

	private boolean finishedAccessControlConfig = false;

	private void changedStep() {
		JPanel curPanel = null;
		switch (currentStep) {
		case STEP_CONTACT_SELECTION:
			abortBackButton.setText(bundle.getString("Abort"));
			nextFinishButton
					.setText(bundle
							.getString("AddContactToShareWizard.nextButton.addContacts.text"));

			List<PanboxGUIContact> contacts = contactList.getSelectedValuesList();
			if ((contacts != null) && (contacts.size() > 0)
					|| finishedAccessControlConfig) {
				nextFinishButton.setEnabled(true);
			} else {
				nextFinishButton.setEnabled(false);
			}

			curPanel = contactListPanel;
			break;
		case STEP_CSP_ACCESSCONTROL:
			abortBackButton.setText(bundle.getString("Back"));
			try {
				// TODO: cleanup, CSP abstraction
				String link = dbapi.getShareConfigurationURL(share.getName(),
						(share.getContacts().size() != 0)).toString();

				shareConfigLinkEditorPane.setText("<html><p><a href=\"" + link
						+ "\">" + link + "</a></p></html>");
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (finishedAccessControlConfig) {
				nextFinishButton.setText(bundle.getString("Next"));
			} else {
				nextFinishButton
						.setText(bundle
								.getString("AddContactToShareWizard.nextButton.openLink.text"));
			}

			curPanel = accessControlPanelDropbox;
			break;
		case STEP_CREATE_INVITATION:
			abortBackButton.setText(bundle.getString("Back"));
			nextFinishButton.setText(bundle.getString("Finish"));
			String link = createShareInvitation();
			shareInvitationLinkEditorPane.setText("<html><p><a href=\"" + link
					+ "\">" + link + "</a></p></html>");
			nextFinishButton.setEnabled(true);
			curPanel = shareInvitationPanel;

			break;
		}

		dialogPanel.removeAll();
		dialogPanel.add(curPanel);
		// javax.swing.GroupLayout dialogPanelLayout = new
		// javax.swing.GroupLayout(
		// dialogPanel);
		// dialogPanel.setLayout(dialogPanelLayout);
		// dialogPanelLayout
		// .setHorizontalGroup(dialogPanelLayout
		// .createParallelGroup(
		// javax.swing.GroupLayout.Alignment.LEADING)
		// .addGroup(
		// dialogPanelLayout
		// .createSequentialGroup()
		// .addContainerGap()
		// .addComponent(
		// curPanel,
		// javax.swing.GroupLayout.DEFAULT_SIZE,
		// javax.swing.GroupLayout.DEFAULT_SIZE,
		// 6).addContainerGap()));
		// dialogPanelLayout
		// .setVerticalGroup(dialogPanelLayout
		// .createParallelGroup(
		// javax.swing.GroupLayout.Alignment.LEADING)
		// .addGroup(
		// dialogPanelLayout
		// .createSequentialGroup()
		// .addContainerGap()
		// .addComponent(
		// curPanel,
		// javax.swing.GroupLayout.DEFAULT_SIZE,
		// javax.swing.GroupLayout.DEFAULT_SIZE,
		// 6).addContainerGap()));
		nextFinishButton.requestFocus();
		invalidate();
		pack();
	}

	private String createShareInvitation() {
		return PanboxURICmdShareInvitation.getPanboxLink(
				share.getUuid().toString(), share.getType().name()).toString();
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

		contactListPanel = new javax.swing.JPanel();
		contactListScrollPane = new javax.swing.JScrollPane();
		contactList = new javax.swing.JList<PanboxGUIContact>();
		accessControlPanelDropbox = new javax.swing.JPanel();
		emailsToClipboardCheckBox = new javax.swing.JCheckBox();
		accessControlLinkLabel = new javax.swing.JLabel();
		shareConfigurationLinkPanel = new javax.swing.JPanel();
		jScrollPane3 = new javax.swing.JScrollPane();
		shareConfigLinkEditorPane = new javax.swing.JEditorPane();
		shareInvitationPanel = new javax.swing.JPanel();
		shareInvitationLinkLabel = new javax.swing.JLabel();
		mailInvitationCheckbox = new javax.swing.JCheckBox();
		inviteViaClipboardCheckbox = new javax.swing.JCheckBox();
		shareInvitationLinkPanel = new javax.swing.JPanel();
		jScrollPane2 = new javax.swing.JScrollPane();
		shareInvitationLinkEditorPane = new javax.swing.JEditorPane();
		nextFinishButton = new javax.swing.JButton();
		abortBackButton = new javax.swing.JButton();
		dialogPanel = new javax.swing.JPanel();

		contactList.setModel(genContactModel());
		contactList.setCellRenderer(new ContactListCellRenderer());
		contactListScrollPane.setViewportView(contactList);

		javax.swing.GroupLayout contactListPanelLayout = new javax.swing.GroupLayout(
				contactListPanel);
		contactListPanel.setLayout(contactListPanelLayout);
		contactListPanelLayout.setHorizontalGroup(contactListPanelLayout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addComponent(contactListScrollPane,
						javax.swing.GroupLayout.Alignment.TRAILING,
						javax.swing.GroupLayout.DEFAULT_SIZE, 282,
						Short.MAX_VALUE));
		contactListPanelLayout.setVerticalGroup(contactListPanelLayout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addComponent(contactListScrollPane,
						javax.swing.GroupLayout.Alignment.TRAILING,
						javax.swing.GroupLayout.DEFAULT_SIZE, 350,
						Short.MAX_VALUE));

		accessControlPanelDropbox.setMaximumSize(null);

		emailsToClipboardCheckBox.setSelected(true);
		emailsToClipboardCheckBox.setText(bundle
				.getString("PanboxClientGUI.copyEmailToClipboard")); // NOI18N

		accessControlLinkLabel
				.setText(bundle
						.getString("AddContactToShareWizard.accesscontrolLinkLabel.text")); // NOI18N

		shareConfigurationLinkPanel
				.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle
						.getString("AddContactToShareWizard.shareConfigurationLinkPanel.border.title"))); // NOI18N

		shareConfigLinkEditorPane.setEditable(false);
		shareConfigLinkEditorPane.setContentType("text/html"); // NOI18N
		shareConfigLinkEditorPane.setEditorKit(new CustomEditorKit());
		shareConfigLinkEditorPane.setAutoscrolls(false);
		shareConfigLinkEditorPane.setOpaque(false);
		shareConfigLinkEditorPane
				.addHyperlinkListener(new javax.swing.event.HyperlinkListener() {
					public void hyperlinkUpdate(
							javax.swing.event.HyperlinkEvent evt) {
						shareConfigLinkEditorPaneHyperlinkUpdate(evt);
					}
				});
		jScrollPane3.setViewportView(shareConfigLinkEditorPane);

		javax.swing.GroupLayout shareConfigurationLinkPanelLayout = new javax.swing.GroupLayout(
				shareConfigurationLinkPanel);
		shareConfigurationLinkPanel
				.setLayout(shareConfigurationLinkPanelLayout);
		shareConfigurationLinkPanelLayout
				.setHorizontalGroup(shareConfigurationLinkPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								shareConfigurationLinkPanelLayout
										.createSequentialGroup()
										.addContainerGap()
										.addComponent(jScrollPane3)
										.addContainerGap()));
		shareConfigurationLinkPanelLayout
				.setVerticalGroup(shareConfigurationLinkPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								shareConfigurationLinkPanelLayout
										.createSequentialGroup()
										.addContainerGap()
										.addComponent(
												jScrollPane3,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												77, Short.MAX_VALUE)
										.addContainerGap()));

		javax.swing.GroupLayout accessControlPanelDropboxLayout = new javax.swing.GroupLayout(
				accessControlPanelDropbox);
		accessControlPanelDropbox.setLayout(accessControlPanelDropboxLayout);
		accessControlPanelDropboxLayout
				.setHorizontalGroup(accessControlPanelDropboxLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addComponent(accessControlLinkLabel,
								javax.swing.GroupLayout.DEFAULT_SIZE, 470,
								Short.MAX_VALUE)
						.addGroup(
								accessControlPanelDropboxLayout
										.createSequentialGroup()
										.addComponent(emailsToClipboardCheckBox)
										.addGap(0, 0, Short.MAX_VALUE))
						.addComponent(shareConfigurationLinkPanel,
								javax.swing.GroupLayout.DEFAULT_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE,
								Short.MAX_VALUE));
		accessControlPanelDropboxLayout
				.setVerticalGroup(accessControlPanelDropboxLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								accessControlPanelDropboxLayout
										.createSequentialGroup()
										.addComponent(
												accessControlLinkLabel,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												43,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(
												shareConfigurationLinkPanel,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												Short.MAX_VALUE)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(emailsToClipboardCheckBox)));

		shareInvitationPanel.setMaximumSize(new java.awt.Dimension(406, 224));
		shareInvitationPanel.setPreferredSize(new java.awt.Dimension(481, 224));

		shareInvitationLinkLabel
				.setText(bundle
						.getString("AddContactToShareWizard.shareInvitationLinkLabel.text")); // NOI18N

		mailInvitationCheckbox
				.setText(bundle
						.getString("AddContactToShareWizard.mailInvitationCheckbox.text")); // NOI18N
		mailInvitationCheckbox.setEnabled(Settings.getInstance()
				.isMailtoSchemeSupported());

		inviteViaClipboardCheckbox.setSelected(true);
		inviteViaClipboardCheckbox
				.setText(bundle
						.getString("AddContactToShareWizard.inviteViaClipboardCheckbox.text")); // NOI18N

		shareInvitationLinkPanel
				.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle
						.getString("AddContactToShareWizard.shareInvitationLinkPanel.border.title"))); // NOI18N

		shareInvitationLinkEditorPane.setEditable(false);
		shareInvitationLinkEditorPane.setContentType("text/html"); // NOI18N
		shareInvitationLinkEditorPane.setEditorKit(new CustomEditorKit());
		shareInvitationLinkEditorPane
				.setToolTipText(bundle
						.getString("AddContactToShareWizard.shareInvitationLinkEditorPane.toolTipText")); // NOI18N
		shareInvitationLinkEditorPane.setMaximumSize(new java.awt.Dimension(
				406, 224));
		shareInvitationLinkEditorPane.setOpaque(false);
		shareInvitationLinkEditorPane
				.addHyperlinkListener(new javax.swing.event.HyperlinkListener() {
					public void hyperlinkUpdate(
							javax.swing.event.HyperlinkEvent evt) {
						shareInvitationLinkEditorPaneHyperlinkUpdate(evt);
					}
				});
		jScrollPane2.setViewportView(shareInvitationLinkEditorPane);

		javax.swing.GroupLayout shareInvitationLinkPanelLayout = new javax.swing.GroupLayout(
				shareInvitationLinkPanel);
		shareInvitationLinkPanel.setLayout(shareInvitationLinkPanelLayout);
		shareInvitationLinkPanelLayout
				.setHorizontalGroup(shareInvitationLinkPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								shareInvitationLinkPanelLayout
										.createSequentialGroup()
										.addContainerGap()
										.addComponent(jScrollPane2)
										.addContainerGap()));
		shareInvitationLinkPanelLayout
				.setVerticalGroup(shareInvitationLinkPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								shareInvitationLinkPanelLayout
										.createSequentialGroup()
										.addContainerGap()
										.addComponent(
												jScrollPane2,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												87, Short.MAX_VALUE)
										.addContainerGap()));

		javax.swing.GroupLayout shareInvitationPanelLayout = new javax.swing.GroupLayout(
				shareInvitationPanel);
		shareInvitationPanel.setLayout(shareInvitationPanelLayout);
		shareInvitationPanelLayout
				.setHorizontalGroup(shareInvitationPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								shareInvitationPanelLayout
										.createSequentialGroup()
										.addComponent(
												inviteViaClipboardCheckbox)
										.addGap(18, 18, 18)
										.addComponent(mailInvitationCheckbox)
										.addContainerGap(206, Short.MAX_VALUE))
						.addComponent(shareInvitationLinkLabel,
								javax.swing.GroupLayout.DEFAULT_SIZE, 451,
								Short.MAX_VALUE)
						.addComponent(shareInvitationLinkPanel,
								javax.swing.GroupLayout.Alignment.TRAILING,
								javax.swing.GroupLayout.DEFAULT_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE,
								Short.MAX_VALUE));
		shareInvitationPanelLayout
				.setVerticalGroup(shareInvitationPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								shareInvitationPanelLayout
										.createSequentialGroup()
										.addComponent(
												shareInvitationLinkLabel,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												43,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addComponent(
												shareInvitationLinkPanel,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												Short.MAX_VALUE)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(
												shareInvitationPanelLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(
																mailInvitationCheckbox)
														.addComponent(
																inviteViaClipboardCheckbox))));

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		setTitle(bundle.getString("AddContactToShareWizard.title")); // NOI18N
		setMinimumSize(null);
		setModal(true);

		nextFinishButton.setText(bundle
				.getString("AddContactToShareWizard.nextFinishButton.text")); // NOI18N
		nextFinishButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				nextFinishButtonActionPerformed(evt);
			}
		});

		abortBackButton.setText(bundle
				.getString("AddContactToShareWizard.abortBackButton.text")); // NOI18N
		abortBackButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				abortBackButtonActionPerformed(evt);
			}
		});

		dialogPanel.setLayout(new java.awt.GridLayout(1, 1));

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
												.addGroup(
														javax.swing.GroupLayout.Alignment.TRAILING,
														layout.createSequentialGroup()
																.addComponent(
																		abortBackButton)
																.addPreferredGap(
																		javax.swing.LayoutStyle.ComponentPlacement.RELATED,
																		132,
																		Short.MAX_VALUE)
																.addComponent(
																		nextFinishButton))
												.addComponent(
														dialogPanel,
														javax.swing.GroupLayout.Alignment.TRAILING,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														Short.MAX_VALUE))
								.addContainerGap()));
		layout.setVerticalGroup(layout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(
						layout.createSequentialGroup()
								.addContainerGap()
								.addComponent(dialogPanel,
										javax.swing.GroupLayout.DEFAULT_SIZE,
										327, Short.MAX_VALUE)
								.addPreferredGap(
										javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
								.addGroup(
										layout.createParallelGroup(
												javax.swing.GroupLayout.Alignment.BASELINE)
												.addComponent(abortBackButton)
												.addComponent(nextFinishButton))
								.addContainerGap()));

		pack();
	}// </editor-fold>//GEN-END:initComponents

	private void shareConfigLinkEditorPaneHyperlinkUpdate(
			javax.swing.event.HyperlinkEvent evt) {// GEN-FIRST:event_shareConfigLinkEditorPaneHyperlinkUpdate
		if (HyperlinkEvent.EventType.ACTIVATED.equals(evt.getEventType())) {
			if (this.finishedAccessControlConfig = openShareConfigLink()) {
				changedStep();
			}
		}
	}// GEN-LAST:event_shareConfigLinkEditorPaneHyperlinkUpdate

	private void shareInvitationLinkEditorPaneHyperlinkUpdate(
			javax.swing.event.HyperlinkEvent evt) {// GEN-FIRST:event_shareInvitationLinkEditorPaneHyperlinkUpdate

	}// GEN-LAST:event_shareInvitationLinkEditorPaneHyperlinkUpdate

	private boolean openShareConfigLink() {
		String shareid = FilenameUtils.getName(share.getPath());
		boolean shareIsOnline;
		try {
			shareIsOnline = ((shareid != null) && (shareid.length() > 0) && dbapi
					.exists(File.separator + shareid));
		} catch (CSPApiException e1) {
			logger.error("Could not determine if share with path "
					+ share.getPath() + " and shareid: " + shareid
					+ " has already been uploaded!");
			shareIsOnline = false;
		}

		if (shareIsOnline) {
			if (emailsToClipboardCheckBox.isSelected()) {
				String emails = PanboxGUIContact.getMailAsSepteratedValues(
						reallyAdded, ";", StorageBackendType.DROPBOX);
				// copy the email of the selected contact into the
				// clipboard
				DesktopApi.copyToClipboard(emails.toString(), false);
			}

			try {
				// share may either already have had participants from the very
				// beginning, or been configured to include participants during
				// accesscontrol config
				URI link = dbapi.getShareConfigurationURL(shareid,
						hasParticipants || finishedAccessControlConfig);
				DesktopApi.browse(link);
				return true;
			} catch (Exception e) {
				logger.error(
						"handleCSPShareParticipantConfiguration: Error opening share configuration",
						e);
				JOptionPane.showMessageDialog(this, bundle
						.getString("PanboxClientGUI.errorOpeningShareConfig"),
						bundle.getString("error"), JOptionPane.ERROR_MESSAGE);
			}

		} else {
			logger.warn("handleCSPShareParticipantConfiguration: Unknown share type!");
		}

		return false;
	}

	private void abortBackButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_abortBackButtonActionPerformed
		switch (currentStep) {
		case STEP_CONTACT_SELECTION:
			// initial step, only allow aborting this dialog
			this.dispose();
			break;
		case STEP_CSP_ACCESSCONTROL:
			currentStep--;
			changedStep();
			break;
		case STEP_CREATE_INVITATION:
			currentStep--;
			changedStep();
			break;
		}
	}// GEN-LAST:event_abortBackButtonActionPerformed

	private void nextFinishButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_nextFinishButtonActionPerformed
		switch (currentStep) {
		case STEP_CONTACT_SELECTION:
			if (handleContactSelection()) {
				finishedAccessControlConfig = false;
			}
			currentStep++;
			break;
		case STEP_CSP_ACCESSCONTROL:
			if (finishedAccessControlConfig
					|| (this.finishedAccessControlConfig = openShareConfigLink())) {
				currentStep++;
			}
			break;
		case STEP_CREATE_INVITATION:
			if (handleShareInvitationDistribution()) {
				// links were either copied to clipboard or sent via email
				this.dispose();
			}
			break;
		}

		changedStep();
	}// GEN-LAST:event_nextFinishButtonActionPerformed

	private boolean handleShareInvitationDistribution() {

		if (inviteViaClipboardCheckbox.isSelected()) {
			DesktopApi.copyToClipboard(createShareInvitation(), true);
		}

		if (Settings.getInstance().isMailtoSchemeSupported()) {

			if (mailInvitationCheckbox.isSelected()) {
				String emails = PanboxGUIContact.getMailAsSepteratedValues(
						reallyAdded, ",", StorageBackendType.DROPBOX);

				DesktopApi.browse(URI.create("mailto:" + emails
						+ "?subject=Panbox%20identity%20link&body="
						+ createShareInvitation()));
			}
		}

		return true;
	}

	ArrayList<PanboxGUIContact> reallyAdded = new ArrayList<PanboxGUIContact>();

	private boolean handleContactSelection() {
		List<PanboxGUIContact> contacts = contactList.getSelectedValuesList();
		// reallyAdded.clear();

		if (contacts.isEmpty()) {
			return false;
		}

		char[] password = PasswordEnterDialog.invoke(PermissionType.USER);
		try {
			client.showTrayMessage(bundle.getString("PleaseWait"),
					bundle.getString("tray.addContactToShare.waitMessage"),
					MessageType.INFO);
			PanboxShare sharenew = null;
			for (int i = 0; i < contacts.size(); i++) {
				// add permission for user
				ContactShareParticipant cp = new ContactShareParticipant(
						contacts.get(i));
				if (!cp.getContact().isVerified()) {
					int res = JOptionPane
							.showConfirmDialog(
									this,
									MessageFormat.format(
											bundle.getString("PanboxClientGUI.addUnverifiedContactWarning.text"),
											cp.getName()),
									bundle.getString("PanboxClientGUI.addUnverifiedContactWarning.title"),
									JOptionPane.YES_NO_OPTION,
									JOptionPane.WARNING_MESSAGE);

					if (res == JOptionPane.YES_OPTION) {
						sharenew = client.addPermissionToShare(share, cp,
								password);
						logger.info("Added unverified contact " + cp.getName()
								+ " to share.");
						reallyAdded.add(contacts.get(i));
						// TODO: update users list

					} else if (res == JOptionPane.NO_OPTION) {
						logger.info("Skipped adding unverified contact "
								+ cp.getName() + " to share.");
						continue;
					}
				} else {
					sharenew = client.addPermissionToShare(share, cp, password);
					reallyAdded.add(contacts.get(i));
					// TODO: update users list
				}
			}
			this.share = (sharenew != null) ? sharenew : this.share;
			if (reallyAdded.size() > 0) {
				return true;
			}
			client.showTrayMessage(
					bundle.getString("tray.addContactToShare.finishTitle"),
					bundle.getString("tray.addContactToShare.finishMessage"),
					MessageType.INFO);
		} catch (ShareDoesNotExistException e1) {
			logger.error("Share not found!", e1);
			JOptionPane
					.showMessageDialog(this,
							bundle.getString("PanboxClient.shareNotFound"),
							bundle.getString("client.error"),
							JOptionPane.ERROR_MESSAGE);
		} catch (ShareManagerException e1) {
			logger.error("Could not add contact to share!", e1);
			JOptionPane
					.showMessageDialog(
							this,
							bundle.getString("PanboxClientGUI.errorWhileAddingContactToShare"),
							bundle.getString("client.error"),
							JOptionPane.ERROR_MESSAGE);
		} catch (UnrecoverableKeyException e1) {
			logger.error("Unable to recover key!", e1);
			JOptionPane
					.showMessageDialog(this, bundle
							.getString("PanboxClient.unableToRecoverKeys"),
							bundle.getString("client.error"),
							JOptionPane.ERROR_MESSAGE);
		} catch (ShareMetaDataException e1) {
			logger.error("Error in share metadata", e1);
			JOptionPane
					.showMessageDialog(
							this,
							bundle.getString("PanboxClientGUI.errorWhileAccessingShareMetadata"),
							bundle.getString("client.error"),
							JOptionPane.ERROR_MESSAGE);
		} finally {
			Utils.eraseChars(password);
		}

		return false;
	}

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JButton abortBackButton;
	private javax.swing.JLabel accessControlLinkLabel;
	private javax.swing.JPanel accessControlPanelDropbox;
	private javax.swing.JList<PanboxGUIContact> contactList;
	private javax.swing.JPanel contactListPanel;
	private javax.swing.JScrollPane contactListScrollPane;
	private javax.swing.JPanel dialogPanel;
	private javax.swing.JCheckBox emailsToClipboardCheckBox;
	private javax.swing.JCheckBox inviteViaClipboardCheckbox;
	private javax.swing.JScrollPane jScrollPane2;
	private javax.swing.JScrollPane jScrollPane3;
	private javax.swing.JCheckBox mailInvitationCheckbox;
	private javax.swing.JButton nextFinishButton;
	private javax.swing.JEditorPane shareConfigLinkEditorPane;
	private javax.swing.JPanel shareConfigurationLinkPanel;
	private javax.swing.JEditorPane shareInvitationLinkEditorPane;
	private javax.swing.JLabel shareInvitationLinkLabel;
	private javax.swing.JPanel shareInvitationLinkPanel;
	private javax.swing.JPanel shareInvitationPanel;
	// End of variables declaration//GEN-END:variables
}
