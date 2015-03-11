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
package org.panbox.desktop.common;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.SplashScreen;
import java.awt.TrayIcon.MessageType;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.ServiceRecord;
import javax.crypto.Cipher;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.panbox.OS;
import org.panbox.Settings;
import org.panbox.core.Utils;
import org.panbox.core.crypto.CryptCore;
import org.panbox.core.crypto.KeyConstants;
import org.panbox.core.csp.CSPAdapterFactory;
import org.panbox.core.csp.StorageBackendType;
import org.panbox.core.devicemgmt.DeviceType;
import org.panbox.core.exception.ShareMetaDataException;
import org.panbox.core.identitymgmt.AbstractAddressbookManager;
import org.panbox.core.identitymgmt.AbstractIdentity;
import org.panbox.core.identitymgmt.AbstractIdentityManager;
import org.panbox.core.identitymgmt.CloudProviderInfo;
import org.panbox.core.identitymgmt.Identity;
import org.panbox.core.identitymgmt.PanboxContact;
import org.panbox.core.identitymgmt.SimpleAddressbook;
import org.panbox.core.identitymgmt.VCardProtector;
import org.panbox.core.identitymgmt.exceptions.ContactExistsException;
import org.panbox.core.pairing.PAKCorePairingHandler;
import org.panbox.core.pairing.PAKCorePairingHandler.PairingType;
import org.panbox.core.pairing.PAKCorePairingRequester;
import org.panbox.core.pairing.PairingInformation;
import org.panbox.core.pairing.PairingNotificationReceiver;
import org.panbox.core.pairing.PairingNotificationReceiver.PairingResult;
import org.panbox.core.pairing.bluetooth.BluetoothPairingInformation;
import org.panbox.core.pairing.bluetooth.PAKBluetoothPairingRequester;
import org.panbox.core.pairing.bluetooth.RemoteDeviceDiscovery;
import org.panbox.core.pairing.file.PanboxFilePairingUtils;
import org.panbox.core.pairing.file.PanboxFilePairingUtils.PanboxFilePairingLoadReturnContainer;
import org.panbox.core.pairing.file.PanboxFilePairingUtils.PanboxFilePairingWriteReturnContainer;
import org.panbox.core.pairing.network.NetworkPairingInformation;
import org.panbox.core.pairing.network.PAKNetworkPairingRequester;
import org.panbox.desktop.common.csp.gui.dropbox.DropboxWizardDialog;
import org.panbox.desktop.common.devicemgmt.DeviceManagerException;
import org.panbox.desktop.common.devicemgmt.DeviceManagerImpl;
import org.panbox.desktop.common.gui.OperationAbortedException;
import org.panbox.desktop.common.gui.PairOrCreateDialog;
import org.panbox.desktop.common.gui.PairThisDeviceDialog;
import org.panbox.desktop.common.gui.PanboxClientGUI;
import org.panbox.desktop.common.gui.PasswordEnterDialog;
import org.panbox.desktop.common.gui.PasswordEnterDialog.PermissionType;
import org.panbox.desktop.common.gui.PleaseWaitDialog;
import org.panbox.desktop.common.gui.SetupWizardDialog;
import org.panbox.desktop.common.gui.TextContextMenu;
import org.panbox.desktop.common.gui.addressbook.ContactListModel;
import org.panbox.desktop.common.gui.addressbook.ContactShareParticipant;
import org.panbox.desktop.common.gui.addressbook.PanboxGUIContact;
import org.panbox.desktop.common.gui.addressbook.PanboxMyContact;
import org.panbox.desktop.common.gui.devices.DeviceListModel;
import org.panbox.desktop.common.gui.devices.DeviceShareParticipant;
import org.panbox.desktop.common.gui.devices.PanboxDevice;
import org.panbox.desktop.common.gui.shares.PanboxShare;
import org.panbox.desktop.common.gui.shares.PanboxSharePermission;
import org.panbox.desktop.common.gui.shares.ShareListModel;
import org.panbox.desktop.common.gui.shares.ShareParticipantListModel;
import org.panbox.desktop.common.identitymgmt.sqlightimpl.AddressbookManager;
import org.panbox.desktop.common.identitymgmt.sqlightimpl.IdentityManager;
import org.panbox.desktop.common.pairing.callables.BluetoothPairingCallable;
import org.panbox.desktop.common.pairing.callables.NetworkPairingCallable;
import org.panbox.desktop.common.sharemgmt.CreateShareNotAllowedException;
import org.panbox.desktop.common.sharemgmt.IPanboxService;
import org.panbox.desktop.common.sharemgmt.IShareManager;
import org.panbox.desktop.common.sharemgmt.ShareDoesNotExistException;
import org.panbox.desktop.common.sharemgmt.ShareInaccessibleException;
import org.panbox.desktop.common.sharemgmt.ShareManagerException;
import org.panbox.desktop.common.sharemgmt.ShareManagerImpl;
import org.panbox.desktop.common.sharemgmt.ShareNameAlreadyExistsException;
import org.panbox.desktop.common.sharemgmt.SharePathAlreadyExistsException;
import org.panbox.desktop.common.sharemgmt.ShareWatchService;
import org.panbox.desktop.common.sharemgmt.UnknownOwnerException;
import org.panbox.desktop.common.utils.DesktopApi;
import org.panbox.desktop.common.utils.FileUtils;
import org.panbox.desktop.common.utils.SingleInstanceLock;
import org.panbox.desktop.common.utils.SingleInstanceLockWMIC;
import org.panbox.desktop.common.vfs.backend.dropbox.DropboxAdapterFactory;
import org.panbox.desktop.common.vfs.backend.dropbox.DropboxClientIntegration;

import ezvcard.VCard;

public abstract class PanboxClient {

	protected static final Logger logger = Logger.getLogger("org.panbox");

	protected static final ResourceBundle bundle = ResourceBundle.getBundle(
			"org.panbox.desktop.common.gui.Messages", Settings.getInstance()
					.getLocale());

	protected final ShareListModel shareList = new ShareListModel();

	protected final ContactListModel contactList = new ContactListModel();

	protected final DeviceListModel deviceList = new DeviceListModel();

	public final IShareManager shareManager;

	public final AbstractIdentityManager identityManager;

	public final AddressbookManager addressbookManager;

	public final DeviceManagerImpl deviceManager;

	private final ExecutorService executor = Executors
			.newSingleThreadExecutor();

	private final ScheduledExecutorService canceller = Executors
			.newSingleThreadScheduledExecutor();

	private Future<?> task = null;

	private ScheduledFuture<Void> scheduledTask = null;

	private AbstractIdentity myId;

	protected SplashScreen splash;
	protected Graphics2D splashGraphics;

	enum SplashScreenState {
		CHECK_ALREADY_RUNNING, LOADING_IDENTITY, LOADING_SHARES, LOADING_DEVICES, LOADING_CONTACTS, ABOUT
	};

	protected void renderSplashFrame(SplashScreenState state) {
		if (splash != null && splashGraphics != null) {
			try {
				String splashLoading = null;
				if (state == SplashScreenState.CHECK_ALREADY_RUNNING) {
					splashLoading = bundle
							.getString("splashscreen.checkingInstance");
				} else if (state == SplashScreenState.LOADING_IDENTITY) {
					splashLoading = bundle
							.getString("splashscreen.loadingIdentity");
				} else if (state == SplashScreenState.LOADING_SHARES) {
					splashLoading = bundle
							.getString("splashscreen.loadingShares");
				} else if (state == SplashScreenState.LOADING_DEVICES) {
					splashLoading = bundle
							.getString("splashscreen.loadingDevices");
				} else if (state == SplashScreenState.LOADING_CONTACTS) {
					splashLoading = bundle
							.getString("splashscreen.loadingContacts");
				} else if (state == SplashScreenState.ABOUT) {
					splashLoading = "Version: "
							+ new String(PanboxDesktopConstants.PANBOX_VERSION,
									StandardCharsets.UTF_8);
				} else {
					splashLoading = "Loading...";
				}
				splashGraphics.setComposite(AlphaComposite.Clear);
				splashGraphics.fillRect(0, 0, 500, 335);
				splashGraphics.setPaintMode();
				splashGraphics.setColor(Color.BLACK);
				splashGraphics.drawString(splashLoading, 120, 250);
				splash.update();
			} catch (IllegalStateException e) {
				logger.error(
						"Encountered error while trying to render splash screen. Will try to close ..",
						e);
				try {
					splash.close();
				} catch (Exception ex) {
					logger.error("Error closing splash screen.", ex);
				}
			}
		}
	}

	/**
	 * initializes global CCP context menu. See {@link TextContextMenu}.
	 */
	private void initGlobalContextMenu() {
		UIManager.addAuxiliaryLookAndFeel(new LookAndFeel() {
			private final UIDefaults defaults = new UIDefaults() {
				private static final long serialVersionUID = 4521137314455023162L;

				@Override
				public javax.swing.plaf.ComponentUI getUI(JComponent c) {
					if (c instanceof javax.swing.text.JTextComponent) {
						if (c.getClientProperty(this) == null) {
							c.setComponentPopupMenu(TextContextMenu.INSTANCE);
							c.putClientProperty(this, Boolean.TRUE);
						}
					}
					return null;
				}
			};

			@Override
			public UIDefaults getDefaults() {
				return defaults;
			};

			@Override
			public String getID() {
				return "TextContextMenu";
			}

			@Override
			public String getName() {
				return getID();
			}

			@Override
			public String getDescription() {
				return getID();
			}

			@Override
			public boolean isNativeLookAndFeel() {
				return false;
			}

			@Override
			public boolean isSupportedLookAndFeel() {
				return true;
			}
		});
	}

	public PanboxClient(IPanboxService service) throws Exception {
		splash = SplashScreen.getSplashScreen();
		if (splash == null) {
			splashGraphics = null;
			System.out.println("SplashScreen.getSplashScreen() returned null");
		} else {
			splashGraphics = splash.createGraphics();
			if (splashGraphics == null) {
				System.out.println("g is null");
			}
		}

		renderSplashFrame(SplashScreenState.CHECK_ALREADY_RUNNING);
				
		// init context menu early, otherwise it may not be available for setup
		// dialogs
		initGlobalContextMenu();

		// some health tests
		if (!checkSupportedKeySize()) {
			logger.error("Symmetric key of " + KeyConstants.SYMMETRIC_KEY_SIZE
					+ " not supported by this JVM. Application will exit...");
			JOptionPane.showMessageDialog(null,
					bundle.getString("client.startup.invalidkeysize"),
					bundle.getString("client.startup.error.title"),
					JOptionPane.ERROR_MESSAGE);
			System.exit(DesktopApi.EXIT_INVALID_KEY_LENGTH);
		}


		if (OS.getOperatingSystem().isLinux()) {
			try {
				if (!SingleInstanceLock.lock()) {
					if (checkPanboxProcessesRunning()) {
						// panbox processes running in background

						JOptionPane
								.showMessageDialog(
										null,
										bundle.getString("PanboxClient.alreadyRunning"));
						System.exit(DesktopApi.EXIT_ERR_ALREADY_RUNNING);
					} else {
						// file-lock detected and no panbox processes running in
						// background -> system crash

						if (panboxMounted()) {
							int ret = JOptionPane
									.showConfirmDialog(
											null,
											bundle.getString("PanboxClient.crashDetected"),
											bundle.getString("PanboxClient.unmountPanbox"),
											JOptionPane.YES_NO_OPTION);

							if (ret == JOptionPane.YES_OPTION) {
								boolean umount = mountPointHandler();

								if (!umount) {
									JOptionPane
											.showMessageDialog(
													null,
													MessageFormat.format(
															bundle.getString("PanboxClient.unmountNotPossible"),
															Settings.getInstance()
																	.getMountDir(),
															Settings.getInstance()
																	.getMountDir()));
									System.exit(DesktopApi.EXIT_CRASH_DETECTED);
								}

							} else if (ret == JOptionPane.NO_OPTION
									|| ret == JOptionPane.CLOSED_OPTION) {
								JOptionPane
										.showMessageDialog(
												null,
												MessageFormat.format(
														bundle.getString("PanboxClient.unmountDirectory"),
														Settings.getInstance()
																.getMountDir()));
								System.exit(DesktopApi.EXIT_CRASH_DETECTED);
							}
						}
						// SingleInstanceLock.forceUnlock();
					}

				}
			} catch (IOException e) {
				logger.error(
						"PanboxClient : Exception thrown while determining if program is already running: ",
						e);
				System.exit(DesktopApi.EXIT_ERR_UNKNOWN);
			}
		} else {
			// Windows uses WMIC implementation
			if (!SingleInstanceLockWMIC.lock()) {
				JOptionPane.showMessageDialog(null,
						bundle.getString("PanboxClient.alreadyRunning"));
				System.exit(DesktopApi.EXIT_ERR_ALREADY_RUNNING);
			}
		}

		renderSplashFrame(SplashScreenState.LOADING_IDENTITY);

		this.addressbookManager = new AddressbookManager();
		this.identityManager = IdentityManager.getInstance();
		this.identityManager.init(addressbookManager);
		this.myId = identityManager.loadMyIdentity(new SimpleAddressbook());
		this.deviceManager = DeviceManagerImpl.getInstance();
		this.shareManager = ShareManagerImpl.getInstance(service);
		// create identity if necessary
		boolean justInited = false;
		if (null == this.myId) {
			initIdentity();
			justInited = true;
		}

		registerShutdownHook();
		// os-specific actions

		this.deviceManager.setIdentity(myId);
		this.shareManager.setIdentity(myId);

		// init share watch service
		this.shareWatchService = new ShareWatchService(this);
		this.shareWatchService.start();

		if (justInited) {
			// reinit splash screen for another try
			splash = SplashScreen.getSplashScreen();
			if (splash == null) {
				splashGraphics = null;
				System.out
						.println("SplashScreen.getSplashScreen() returned null");
			} else {
				splashGraphics = splash.createGraphics();
				if (splashGraphics == null) {
					System.out.println("g is null");
				}
			}
		}

		renderSplashFrame(SplashScreenState.LOADING_SHARES);

		// fill models, but catch exceptions to still allow for application
		// startup (and the user to save contacts, data, ...)
		try {
			refreshShareListModel();
		} catch (Exception e) {
			logger.error("Unable to initialize share list!", e);
			JOptionPane
					.showMessageDialog(
							getMainWindow(),
							bundle.getString("PanboxClient.initialLoadingOfSharesFailed"),
							bundle.getString("client.error"),
							JOptionPane.ERROR_MESSAGE);
		}

		renderSplashFrame(SplashScreenState.LOADING_DEVICES);

		try {
			refreshDeviceListModel();
		} catch (Exception e) {
			logger.error(bundle.getString("PanboxClient.unableInitDeviceList"),
					e);
			JOptionPane
					.showMessageDialog(
							getMainWindow(),
							bundle.getString("PanboxClient.initialDeviceSetupFailed"),
							bundle.getString("client.error"),
							JOptionPane.ERROR_MESSAGE);
		}

		renderSplashFrame(SplashScreenState.LOADING_CONTACTS);

		try {
			refreshContactListModel();
		} catch (Exception e) {
			logger.error("Unable to initialize contact list!", e);
			JOptionPane
					.showMessageDialog(
							getMainWindow(),
							bundle.getString("PanboxClient.initialContactSetupFailed"),
							bundle.getString("client.error"),
							JOptionPane.ERROR_MESSAGE);
		}

		setup();
	}

	private ShareWatchService shareWatchService;

	public ShareWatchService getShareWatchService() {
		return shareWatchService;
	}

	public abstract void restartApplication() throws IOException;

	// returns true if processes running
	abstract protected boolean checkPanboxProcessesRunning();

	// returns true if panbox-directory successful unmounted
	abstract protected boolean mountPointHandler();

	// returns true if panbox-directory mounted
	abstract protected boolean panboxMounted();

	public ContactListModel getContactList() {
		return contactList;
	}

	public DeviceListModel getDeviceList() {
		return deviceList;
	}

	public ShareListModel getShareList() {
		return shareList;
	}

	public ShareParticipantListModel getShareParticipantListForShare(
			PanboxShare share) {
		return share.generatePermissionsModel(myId);
	}

	public AbstractIdentity getIdentity() {
		return myId;
	}

	// ==================== INIT FUNCTIONS ====================

	public void refreshDeviceShareList() {
		try {
			refreshShareListModel();
			mainWindow.refreshDeviceShareList();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void refreshAddressbookList() {
		refreshContactListModel();
		mainWindow.refreshAddressbookList();
	}

	/**
	 * checks this shares device lists.
	 * 
	 * @param share
	 * @return
	 */
	public PanboxShare checkShareIntegrity(PanboxShare share) {
		if (share.isOwner()) {
			Collection<PanboxContact> coll = shareManager
					.checkShareDeviceListIntegrity(share);
			char[] password = null;
			PanboxShare retShare = null;
			try {
				for (Iterator<PanboxContact> it = coll.iterator(); it.hasNext();) {
					PanboxContact contact = (PanboxContact) it.next();
					int ret = JOptionPane
							.showConfirmDialog(
									getMainWindow(),
									MessageFormat.format(
											bundle.getString("client.checkShareIntegrity.devicelistreset"),
											contact.getEmail()), bundle
											.getString("client.warn"),
									JOptionPane.YES_NO_OPTION,
									JOptionPane.WARNING_MESSAGE);

					if (ret == JOptionPane.YES_OPTION) {
						if (password == null) {
							password = PasswordEnterDialog
									.invoke(PermissionType.SHARE);
						}
						logger.info("Will try to reinitialize share device list for contact "
								+ contact.getEmail()
								+ " in share "
								+ share.getName() + " ...");

						try {
							retShare = shareManager.resetShareInvitation(share,
									contact.getEmail(), password);
						} catch (UnrecoverableKeyException e) {
							logger.error(
									"Re-initializing share device list failed due to unrecoverable key!",
									e);
							JOptionPane
									.showMessageDialog(
											getMainWindow(),
											bundle.getString("PanboxClient.unableToRecoverKeys"),
											bundle.getString("client.error"),
											JOptionPane.ERROR_MESSAGE);
						} catch (ShareManagerException | ShareMetaDataException e) {
							logger.error(
									"Re-initializing share device list failed!",
									e);
							JOptionPane
									.showMessageDialog(
											getMainWindow(),
											MessageFormat.format(
													bundle.getString("client.checkShareIntegrity.devicelistresetfailed"),
													contact.getEmail()), bundle
													.getString("client.error"),
											JOptionPane.ERROR_MESSAGE);
						}
					} else {
						JOptionPane
								.showMessageDialog(
										getMainWindow(),
										MessageFormat.format(
												bundle.getString("client.checkShareIntegrity.devicelistnotreset"),
												contact.getEmail()), bundle
												.getString("client.warn"),
										JOptionPane.WARNING_MESSAGE);
					}
				}
			} finally {
				if (password != null) {
					Utils.eraseChars(password);
				}
			}
			return retShare;
		}
		return null;
	}

	public void refreshShareListModel() throws ShareManagerException {
		// remove old elements
		shareList.removeAllElements();

		// insert new elements
		List<String> shareNames = new ArrayList<String>();
		try {
			shareNames = shareManager.getInstalledShares();
		} catch (ShareManagerException sme) {
			logger.error(
					"Could not obtain list of installed share names! No shares will be loaded ...",
					sme);
			throw sme;
		}

		boolean succ = true;
		for (String shareName : shareNames) {
			try {
				PanboxShare share = shareManager.getShareForName(shareName);
				PanboxShare tmp;
				if ((tmp = checkShareIntegrity(share)) != null) {
					share = tmp;
				}
				shareWatchService.registerShare(share);
				shareList.addElement(share);
			} catch (ShareDoesNotExistException e) {
				logger.error(
						"Share does not exist in the local database! Sharename: "
								+ shareName, e);
				succ = false;
			} catch (ShareInaccessibleException e) {
				logger.error("Unable to access backend diretory for share "
						+ shareName
						+ ". Trying to remove share from local database", e);
				try {
					shareManager.removeShareFromDB(shareName);
					logger.info("Share entry " + shareName
							+ " has been removed from local database!");
				} catch (ShareManagerException e2) {
					logger.error("Unable to remove share entry " + shareName
							+ " from local database!");
				}
				succ = false;
			}

			catch (UnrecoverableKeyException | ShareMetaDataException e) {
				logger.error("Unable to load share " + shareName
						+ ". Share metadata may be corrupt", e);
				succ = false;
			}
		}
		if (!succ) {
			throw new ShareManagerException(
					"Encountered error while loading one or more shares.");
		}
	}

	public void refreshContactListModel() {
		// remove old elements
		contactList.removeAllElements();

		// create & insert own identity into data model
		contactList.addElement(new PanboxMyContact(myId));

		// load & add remaining contacts
		Collection<org.panbox.core.identitymgmt.PanboxContact> contacts = myId
				.getAddressbook().getContacts();
		for (org.panbox.core.identitymgmt.PanboxContact c : contacts) {
			contactList.addElement(new PanboxGUIContact(c));
		}
	}

	public void refreshDeviceListModel() throws DeviceManagerException {
		// remove old elements
		deviceList.removeAllElements();

		// insert new elements
		List<PanboxDevice> devices = deviceManager.getDeviceList();

		for (PanboxDevice d : devices) {
			deviceList.addElement(d);
		}
	}

	/**
	 * Method handles os-independent application initialization actions, then
	 * calls abstract os-specific startup handlers.
	 * 
	 * @throws Exception
	 */
	@Deprecated
	protected void initApplication() throws Exception {

		// create identity if necessary
		if (null == this.myId) {
			initIdentity();
		}

		registerShutdownHook();
		// os-specific actions
		setup();
	}

	protected PanboxClientGUI mainWindow;

	protected void registerMainwindow(PanboxClientGUI mainwindow) {
		mainWindow = mainwindow;
	}

	/**
	 * @return the mainWindow
	 */
	public PanboxClientGUI getMainWindow() {
		return mainWindow;
	}

	/**
	 * Method for handling all actions to be taken upon application startup. To
	 * be defined in os-specifc implementations
	 * 
	 * @throws Exception
	 */
	abstract protected void setup() throws Exception;

	/**
	 * Method for handling all actions to be taken upon application shutdown. To
	 * be defined in os-specifc implementations
	 * 
	 * @throws Exception
	 */
	abstract protected void shutdown() throws Exception;

	public void initIdentity() throws Exception {

		if (this.myId != null) {
			throw new IllegalStateException(
					"You can not initialize the identityManager if it has been initialized before.");
		}
		while (this.myId == null) {
			PairOrCreateDialog pocDialog = new PairOrCreateDialog(null, true);
			pocDialog.setLocationRelativeTo(null);
			pocDialog.setVisible(true);

			if (pocDialog.isPairing() == null) {
				JOptionPane.showMessageDialog(null,
						bundle.getString("client.setupwizard.aborted.msg"),
						bundle.getString("client.warn"),
						JOptionPane.WARNING_MESSAGE);
				logger.debug("Panbox setup wizard has been aborted.");
				throw new OperationAbortedException(
						"The setup wizard has been aborted.");
			} else if (pocDialog.isPairing()) {
				PairThisDeviceDialog pairDialog = new PairThisDeviceDialog(null);
				pairDialog.setVisible(true);

				File pFile = pairDialog.getPairingFile();
				if (pFile != null) {
					loadPairingFile(pFile);
				} else {
					try {
						String pairingPassword = pairDialog
								.getPairingPassword();

						runGeneralPairingRequester(pairingPassword);
					} catch (OperationAbortedException ex) {
						logger.debug("Panbox pairing wizard has been aborted.");
					}
				}

				this.identityManager.init(addressbookManager);
				this.myId = identityManager
						.loadMyIdentity(new SimpleAddressbook());
			} else {
				SetupWizardDialog dialog = new SetupWizardDialog(null, true);
				dialog.setLocationRelativeTo(null);
				dialog.setVisible(true);

				if (dialog.wasCanceled()) {
					logger.debug("Panbox setup wizard has been aborted.");
					continue;
				}

				// show please wait dialog
				PleaseWaitDialog loadDialog = new PleaseWaitDialog(null,
						bundle.getString("PanboxGeneratingIdentity"));
				loadDialog.setLocationRelativeTo(null);
				loadDialog.setVisible(true);

				// create identity and initial keypairs
				Identity id = new Identity(new SimpleAddressbook(),
						dialog.getEmail(), dialog.getFirstname(),
						dialog.getLastname());
				KeyPair ownerKeySign = CryptCore.generateKeypair();
				KeyPair ownerKeyEnc = CryptCore.generateKeypair();
				KeyPair deviceKey = CryptCore.generateKeypair();
				char[] password = dialog.getPassword();
				String deviceName = dialog.getDevicename();

				id.setOwnerKeySign(ownerKeySign, password);
				id.setOwnerKeyEnc(ownerKeyEnc, password);
				Settings.getInstance().setPairingType(PairingType.MASTER);
				id.addDeviceKey(deviceKey, dialog.getDevicename());
				Settings.getInstance().setDeviceName(deviceName);
				identityManager.init(addressbookManager);
				identityManager.storeMyIdentity(id);

				try {
					deviceManager.setIdentity(id);
					deviceManager.addThisDevice(deviceName, deviceKey,
							DeviceType.DESKTOP);
					refreshDeviceListModel();
				} catch (DeviceManagerException ex) {
					// Simply ignore this for now!
				}

				this.myId = id;

				// hide please wait dialog
				loadDialog.dispose();
			}
		}

		// update share manager identity to new identity!
		shareManager.setIdentity(myId);

		refreshContactListModel();

		// Check if Dropbox is installed. If yes -> Show wizard dialog!
		DropboxAdapterFactory dropboxAdapterFactory = (DropboxAdapterFactory) CSPAdapterFactory
				.getInstance(StorageBackendType.DROPBOX);
		DropboxClientIntegration dropboxClientIntegration = (DropboxClientIntegration) dropboxAdapterFactory
				.getClientAdapter();
		if (dropboxClientIntegration.isClientInstalled()) {
			DropboxWizardDialog dialog = new DropboxWizardDialog(mainWindow,
					true);
			dialog.setAccessToken(Settings.getInstance()
					.getDropboxAccessToken());
			dialog.setDropboxSyncDirPath(Settings.getInstance()
					.getDropboxSynchronizationDir());
			dialog.setVisible(true);

			if (dialog.getAccessToken() != null
					&& dialog.getDropboxSyncDirPath() != null) {
				Settings.getInstance().setDropboxAccessToken(
						dialog.getAccessToken());
				Settings.getInstance().setDropboxSynchronizationDir(
						dialog.getDropboxSyncDirPath());
			}
		}

		// Show message about the successful client setup!
		JOptionPane.showMessageDialog(null,
				bundle.getObject("client.setupwizard.success.msg"),
				bundle.getString("client.setupwizard.title"),
				JOptionPane.INFORMATION_MESSAGE);
	}

	// ==================== CLIENT SPECIFIC EVENTS ====================

	// Share list events

	public void addShare(PanboxShare share, char[] password) {
		try {
			share = ShareManagerImpl.getInstance().addNewShare(share, password);
			PanboxShare tmp;
			if ((tmp = checkShareIntegrity(share)) != null) {
				share = tmp;
			}

			shareWatchService.registerShare(share);
			shareList.addElement(share);
			informAddShare(share); // inform OS specific implementation about
									// this event!
		} catch (CreateShareNotAllowedException e) {
			logger.error("Slave tried to create a new share!", e);
			JOptionPane
					.showMessageDialog(getMainWindow(), bundle
							.getString("client.slaveCreateShareNotAllowed"),
							bundle.getString("client.error"),
							JOptionPane.ERROR_MESSAGE);
		} catch (UnrecoverableKeyException e) {
			logger.error("Unable to recover key!", e);
			JOptionPane
					.showMessageDialog(getMainWindow(), bundle
							.getString("PanboxClient.unableToRecoverKeys"),
							bundle.getString("client.error"),
							JOptionPane.ERROR_MESSAGE);
		} catch (ShareMetaDataException e) {
			logger.error("Error in share metadata", e);
			JOptionPane
					.showMessageDialog(
							getMainWindow(),
							bundle.getString("PanboxClient.errorWhileAccessingShareMetadata"),
							bundle.getString("client.error"),
							JOptionPane.ERROR_MESSAGE);
		} catch (ShareNameAlreadyExistsException e) {
			logger.error("Share name already exists", e);
			JOptionPane
					.showMessageDialog(getMainWindow(), bundle
							.getString("PanboxClient.shareNameAlreadyExists"),
							bundle.getString("client.error"),
							JOptionPane.ERROR_MESSAGE);
		} catch (SharePathAlreadyExistsException e) {
			logger.error("Share path has already been configured!", e);
			JOptionPane
					.showMessageDialog(
							getMainWindow(),
							bundle.getString("PanboxClient.sharePathAlreadyConfigured"),
							bundle.getString("client.error"),
							JOptionPane.ERROR_MESSAGE);
		} catch (UnknownOwnerException ex) {
			logger.error(
					"PanboxClient : addShare : UnknownOwnerException occured: ",
					ex);
			JOptionPane
					.showMessageDialog(getMainWindow(),
							bundle.getString("PanboxClient.shareOwnerUnknown"),
							bundle.getString("client.error"),
							JOptionPane.ERROR_MESSAGE);
		} catch (ShareManagerException ex) {
			logger.error(
					"PanboxClient : addShare : ShareManagerException occured: ",
					ex);
			JOptionPane
					.showMessageDialog(getMainWindow(),
							bundle.getString("client.error.addShare"),
							bundle.getString("client.error"),
							JOptionPane.ERROR_MESSAGE);
		} catch (Exception e) {
			logger.error("PanboxClient : addShare : Exception occured: ", e);
		}
	}

	public abstract void informAddShare(PanboxShare share) throws Exception;

	public void removeShare(PanboxShare share) {
		try {
			ShareManagerImpl.getInstance().removeShare(share.getName(),
					share.getPath(), share.getType());
			shareWatchService.removeShare(share);
			shareList.removeElement(share);
			informRemoveShare(share); // inform OS specific implementation
			// about
			// // this event!
		} catch (ShareManagerException ex) {
			logger.error(
					"PanboxClient : removeShare : ShareManagerException occured: ",
					ex);
			JOptionPane
					.showMessageDialog(null,
							bundle.getString("client.error.removeShare"),
							bundle.getString("client.error"),
							JOptionPane.ERROR_MESSAGE);
		} catch (Exception e) {
			logger.error("PanboxClient : removeShare : Exception occured: ", e);
		}
	}

	public abstract void informRemoveShare(PanboxShare share) throws Exception;

	// Share participants list events

	public void removePermissionFromShare(PanboxShare share,
			PanboxSharePermission permission, char[] password) {

		// first panbox release does not support permission removal. disabled
		// for now

		// if (permission instanceof DeviceShareParticipant) {
		// DeviceShareParticipant dPermission = (DeviceShareParticipant)
		// permission;
		//
		// PanboxDevice device = dPermission.getDevice();
		//
		// try {
		// share = ShareManagerImpl.getInstance().removeDevicePermission(
		// share, device.getDeviceName(), password);
		// } catch (ShareDoesNotExistException e) {
		// logger.error("Share not found!", e);
		// JOptionPane.showMessageDialog(getMainWindow(),
		// bundle.getString("PanboxClient.shareNotFound"),
		// bundle.getString("client.error"),
		// JOptionPane.ERROR_MESSAGE);
		// } catch (ShareManagerException e) {
		// logger.error("Could not remove device from share!", e);
		// JOptionPane
		// .showMessageDialog(
		// getMainWindow(),
		// bundle.getString("PanboxClient.errorWhileRemovingDeviceFromShare"),
		// bundle.getString("client.error"),
		// JOptionPane.ERROR_MESSAGE);
		// } catch (UnrecoverableKeyException e) {
		// logger.error("Unable to recover key!", e);
		// JOptionPane.showMessageDialog(getMainWindow(),
		// bundle.getString("PanboxClient.unableToRecoverKeys"),
		// bundle.getString("client.error"),
		// JOptionPane.ERROR_MESSAGE);
		// } catch (ShareMetaDataException e) {
		// logger.error("Error in share metadata", e);
		// JOptionPane
		// .showMessageDialog(
		// getMainWindow(),
		// bundle.getString("PanboxClient.errorWhileAccessingShareMetadata"),
		// bundle.getString("client.error"),
		// JOptionPane.ERROR_MESSAGE);
		// }
		// } else if (permission instanceof ContactShareParticipant) {
		// // TODO Add implementation here, once user deletion is
		// // supported
		// }

	}

	public PanboxShare addPermissionToShare(PanboxShare share,
			PanboxSharePermission permission, char[] password)
			throws UnrecoverableKeyException, ShareDoesNotExistException,
			ShareManagerException, ShareMetaDataException {

		if (permission instanceof DeviceShareParticipant) {
			// New device has been added to share
			DeviceShareParticipant dPermission = (DeviceShareParticipant) permission;
			PanboxDevice device = dPermission.getDevice();
			share = ShareManagerImpl.getInstance().addDevicePermission(share,
					device.getDeviceName(), password);
		} else if (permission instanceof ContactShareParticipant) {
			// New contact has been added to share
			ContactShareParticipant cPermission = (ContactShareParticipant) permission;
			PanboxGUIContact contact = cPermission.getContact();
			share = ShareManagerImpl.getInstance().addContactPermission(share,
					contact.getEmail(), password);
		}

		// update sharelist contents
		for (int i = 0; i < shareList.size(); i++) {
			if (shareList.get(i).getUuid().equals(share.getUuid())) {
				shareList.set(i, share);
				break;
			}
		}

		return share;
	}

	public void saveMyContact(ArrayList<CloudProviderInfo> removedCSPs,
			ArrayList<CloudProviderInfo> addedCSPs) {
		for (CloudProviderInfo csp : removedCSPs) {
			this.myId.delCloudProviderByProviderName(csp);
		}
		for (CloudProviderInfo csp : addedCSPs) {
			this.myId.addCloudProvider(csp);
		}
		identityManager.storeMyIdentity(this.myId);
	}

	// Contact list events
	// public void saveContact(String mail, String newFirstName, String newName,
	public void saveContact(PanboxGUIContact contact, String newFirstName,
			String newName, ArrayList<CloudProviderInfo> removedCSPs,
			ArrayList<CloudProviderInfo> addedCSPs, boolean verified) {
		// org.panbox.core.identitymgmt.PanboxContact contact = this.myId
		// .getAddressbook().contactExists(mail);
		contact.setFirstName(newFirstName);
		contact.setName(newName);
		for (CloudProviderInfo csp : removedCSPs) {
			contact.removeCloudProvider(csp.getProviderName());
		}
		for (CloudProviderInfo csp : addedCSPs) {
			contact.addCloudProvider(csp);
		}
		if (verified) {
			contact.setTrustLevel(PanboxContact.TRUSTED_CONTACT);
		} else {
			contact.setTrustLevel(PanboxContact.UNTRUSTED_CONTACT);
		}
		addressbookManager.persistContacts(this.myId.getAddressbook()
				.getContacts(), this.myId.getID());
	}

	public void importContacts(VCard[] vcs, boolean authVerified) {
		try {
			addressbookManager.importContacts(this.myId, vcs, authVerified);
		} catch (ContactExistsException e) {
			StringBuilder b = new StringBuilder();
			List<PanboxContact> existingContacts = e.getContacts();
			for (PanboxContact c : existingContacts) {
				b.append("- ");
				b.append(c.getFirstName() + " " + c.getName() + " ("
						+ c.getEmail() + ")");
				b.append("\n");
			}
			JOptionPane.showMessageDialog(getMainWindow(), MessageFormat
					.format(bundle
							.getString("PanboxClient.contactAlreadyExists"), b
							.toString()), "Info",
					JOptionPane.INFORMATION_MESSAGE);
		} finally {
			identityManager.storeMyIdentity(this.myId);
			refreshContactListModel();
		}
	}

	public void removeContact(PanboxGUIContact contact) {
		// First, check if contact that is to be removed, still is owner of any
		// share we currently have in our share list

		try {
			List<String> shareNames = this.shareManager.getInstalledShares();
			List<PanboxShare> knownShares = new ArrayList<PanboxShare>();
			for (String shareName : shareNames) {
				PanboxShare share = null;
				share = shareManager.getShareForName(shareName);
				if (share != null) {
					knownShares.add(share);
				}
			}

			List<PanboxShare> sharesToBeRemoved = new ArrayList<PanboxShare>();
			StringBuffer slist = new StringBuffer();
			for (PanboxShare panboxShare : knownShares) {
				if (panboxShare.isOwner(contact.getPublicKeySign())) {
					sharesToBeRemoved.add(panboxShare);
					// prepare message
					slist.append("\n- ");
					slist.append(panboxShare.getName());
				}
			}

			if (sharesToBeRemoved.size() > 0) {
				// there still are share to be removed
				JCheckBox checkbox = new JCheckBox(
						bundle.getString("client.shareList.removeShareDirectoryMessage"));
				String message = MessageFormat.format(bundle
						.getString("PanboxClient.shareOwnerRemoveMessage"),
						slist.toString());
				Object[] params = { message, checkbox };

				int reallyRemove = JOptionPane.showConfirmDialog(
						getMainWindow(), params,
						bundle.getString("PanboxClient.reallyRemoveShares"),
						JOptionPane.YES_NO_OPTION);

				if (reallyRemove == JOptionPane.YES_OPTION) {
					// user chose to remove shares
					for (PanboxShare share : sharesToBeRemoved) {
						PleaseWaitDialog d = null;
						if (checkbox.isSelected()) {
							try {
								d = new PleaseWaitDialog(
										getMainWindow(),
										bundle.getString("PanboxClient.operationInProgress"));
								d.setLocationRelativeTo(getMainWindow());
								d.setVisible(true);
								FileUtils.deleteDirectoryTree(new File(share
										.getPath()));
								// FileUtils.deleteDirectory(new File(share
								// .getPath()));
							} catch (IOException e) {
								logger.error(
										"Failed to remove share source directory!",
										e);
								JOptionPane
										.showMessageDialog(
												getMainWindow(),
												bundle.getString("PanboxClient.deleteShareContentsFailed"),
												bundle.getString("error"),
												JOptionPane.ERROR_MESSAGE);
							} finally {
								if (d != null) {
									d.dispose();
								}
							}
						}
						removeShare(share);
					}
					// contact will be removed later on
				} else if (reallyRemove == JOptionPane.NO_OPTION) {
					// do nothing
					return;
				}
			}
		} catch (NullPointerException | ShareManagerException
				| UnrecoverableKeyException | ShareMetaDataException e) {
			logger.error("Could not obtain list of installed shares!", e);
			int ret = JOptionPane.showConfirmDialog(getMainWindow(), bundle
					.getString("PanboxClient.unableToDetermineIfUserIsOwner"),
					bundle.getString("PanboxClient.unableToReadShares"),
					JOptionPane.YES_NO_OPTION);
			if (ret == JOptionPane.NO_OPTION) {
				return;
			}
		}

		// try to remove contact
		if (!this.myId.deleteContact(contact.getEmail())) {
			JOptionPane
					.showMessageDialog(
							getMainWindow(),
							bundle.getString("PanboxClient.couldNotDeleteFromAddressbook"),
							bundle.getString("client.error"),
							JOptionPane.ERROR_MESSAGE);
		} else {
			identityManager.storeMyIdentity(this.myId);
			// operation was successful, refresh view
			contactList.removeElement(contact);
		}
	}

	public void exportContacts(List<PanboxGUIContact> contacts, File vcardFile,
			char[] exportPIN) {
		Collection<VCard> vcards = new LinkedList<VCard>();
		for (PanboxGUIContact c : contacts) {
			VCard v;
			if (c instanceof PanboxMyContact) {
				v = AbstractAddressbookManager.contact2VCard(this.myId);
			} else {
				v = AbstractAddressbookManager.contact2VCard((PanboxContact) c
						.getModel());
			}
			vcards.add(v);
		}

		File tmpFileForVcard = new File(System.getProperty("java.io.tmpdir")
				+ File.separator + "panboxTMPex.vcf");

		if (!AbstractAddressbookManager.exportContacts(vcards, tmpFileForVcard)) {
			JOptionPane
					.showMessageDialog(getMainWindow(), bundle
							.getString("PanboxClient.couldNotExportContacts"),
							bundle.getString("client.error"),
							JOptionPane.ERROR_MESSAGE);
		}

		try {
			logger.info("Exporting contacts with export PIN "
					+ String.valueOf(exportPIN));
			VCardProtector.protectVCF(vcardFile, tmpFileForVcard, exportPIN);
		} catch (Exception e) {
			logger.warn("protectVCF Exception", e);
		}

		if (tmpFileForVcard.exists()) {
			tmpFileForVcard.delete();
		}
	}

	public void exportContacts(List<PanboxGUIContact> contacts, File vcardFile) {
		Collection<VCard> vcards = new LinkedList<VCard>();
		for (PanboxGUIContact c : contacts) {
			VCard v;
			if (c instanceof PanboxMyContact) {
				v = AbstractAddressbookManager.contact2VCard(this.myId);
			} else {
				v = AbstractAddressbookManager.contact2VCard((PanboxContact) c
						.getModel());
			}
			vcards.add(v);
		}

		if (!AbstractAddressbookManager.exportContacts(vcards, vcardFile)) {
			JOptionPane
					.showMessageDialog(getMainWindow(), bundle
							.getString("PanboxClient.couldNotExportContacts"),
							bundle.getString("client.error"),
							JOptionPane.ERROR_MESSAGE);
		}
	}

	public void addCSPtoContact(PanboxGUIContact contact, String csp,
			String account) {
		contact.addCloudProvider(new CloudProviderInfo(csp, account));
	}

	public void removeCSPfromContact(PanboxGUIContact contact, String csp) {
		contact.removeCloudProvider(csp);
	}

	// Device list events

	public NetworkPairingInformation initDevicePairingLAN()
			throws SocketException {
		Settings s = Settings.getInstance();
		NetworkPairingInformation info = new NetworkPairingInformation(
				s.getPairingInterface(), s.getPairingAddress());
		extendPairingInformation(info);
		return info;
	}

	public BluetoothPairingInformation initDevicePairingBluetooth()
			throws BluetoothStateException, InterruptedException {
		// This is the first time we are going to touch the Bluetooth API so we
		// should attach our logger to the com.intel.bluetooth logging
		appendBluetoothLogging();

		// Make bluetooth device discoverbale!
		LocalDevice local = LocalDevice.getLocalDevice();
		try {
			local.setDiscoverable(DiscoveryAgent.GIAC);
		} catch (BluetoothStateException e) {
			logger.debug("PanboxClient : initDevicePairingBluetooth : First try to set discoverable failed. Will try again in 1sec.");
			Thread.sleep(1000);
			local.setDiscoverable(DiscoveryAgent.GIAC);
		}

		// setting LocalDevice is only need for Linux, since on Windows we
		// bluecove only supports one device!
		BluetoothPairingInformation info = new BluetoothPairingInformation(
				local);
		extendPairingInformation(info);
		return info;
	}

	private void appendBluetoothLogging() {
		Logger bluecoveLog = Logger.getLogger("com.intel.bluetooth");
		@SuppressWarnings("rawtypes")
		Enumeration loggers = logger.getAllAppenders();
		while (loggers.hasMoreElements()) {
			Appender appender = (Appender) loggers.nextElement();
			bluecoveLog.addAppender(appender);
		}
	}

	private void extendPairingInformation(PairingInformation info) {
		logger.debug("PanboxClient : extendPairingInformation : Will now extend given device pairing information...");
		final JTextField deviceNameField = new JTextField(
				bundle.getString("PanboxClient.chooseDeviceName"));
		deviceNameField.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				if (deviceNameField.getText().equals(
						bundle.getString("PanboxClient.chooseDeviceName"))) {
					deviceNameField.setText("");
				}
			}
		});
		JOptionPane.showMessageDialog(null, deviceNameField,
				bundle.getString("PanboxClient.enterDeviceName"),
				JOptionPane.INFORMATION_MESSAGE);
		info.setDeviceName(deviceNameField.getText());
		logger.debug("PanboxClient : extendPairingInformation : Set device name to: "
				+ info.getDeviceName());
	}

	public void runMasterPairingOnPairingHandle(final PairingInformation info,
			final String password, final PairingNotificationReceiver receiver,
			final char[] keyPassword) {
		logger.debug("PanboxClient : runMasterPairingOnPairingHandle : Master pairing started: "
				+ info);
		runGeneralPairingHandler(PairingType.MASTER, info, password, receiver,
				keyPassword);
	}

	public void runSlavePairingOnPairingHandle(final PairingInformation info,
			final String password, final PairingNotificationReceiver receiver) {
		logger.debug("PanboxClient : runSlavePairingOnPairingHandle : Slave pairing started: "
				+ info);
		runGeneralPairingHandler(PairingType.SLAVE, info, password, receiver,
				null);
	}

	private void runGeneralPairingHandler(final PairingType type,
			final PairingInformation info, final String password,
			final PairingNotificationReceiver receiver, final char[] keyPassword) {
		logger.debug("PanboxClient : runGeneralPairingHandler : Pairing is about to start: "
				+ "Type: " + type + " for " + info);
		final PanboxClient thisClient = this;

		try {
			if (info instanceof NetworkPairingInformation) {
				task = executor.submit(new NetworkPairingCallable(type, info,
						password, receiver, keyPassword, this));
			} else if (info instanceof BluetoothPairingInformation) {
				task = executor.submit(new BluetoothPairingCallable(type, info,
						password, receiver, keyPassword, this));
			}
		} catch (Exception e) {
			logger.error(
					"PanboxClient : runGeneralPairingHandler : Setting up pairing failed: ",
					e);
			JOptionPane.showMessageDialog(getMainWindow(),
					bundle.getString("client.pairing.couldnotsetup.message"),
					bundle.getString("client.pairing.failed"),
					JOptionPane.ERROR_MESSAGE);
			task = null;
			return; // pairing setup failed. Return and don't start!
		}
		scheduledTask = canceller.schedule(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				synchronized (thisClient) {
					if (task != null && !task.isCancelled() && !task.isDone()) {
						task = null;
						logger.debug("PanboxClient : runGeneralPairingHandler : Timeout for pairing task!");
						receiver.inform(PairingResult.TIMEOUT);
					} else {
						logger.debug("PanboxClient : runGeneralPairingHandler : Timeout for task that has been finished/canceled already!");
					}
					return null;
				}
			}

		}, PAKCorePairingHandler.PAIRING_TIMEOUT, TimeUnit.MILLISECONDS);
	}

	public synchronized void stopDevicePairing() {
		logger.debug("PanboxClient : stopDevicePairing : called");
		if (task != null && scheduledTask != null) {
			task.cancel(true);
			logger.debug("PanboxClient : stopDevicePairing : Old pairing task has been canceled. Canceled?: "
					+ task.isCancelled());
			task = null;
			scheduledTask.cancel(true);
			scheduledTask = null;
		} else {
			logger.debug("PanboxClient : stopDevicePairing : No current running device pairing to stop!");
		}
	}

	private void runGeneralPairingRequester(final String qrPassword) {
		String[] splitPW = qrPassword.split(":");
		String addr = splitPW[0];
		String password = splitPW[1];

		KeyPair deviceKey = CryptCore.generateKeypair();

		PleaseWaitDialog dialog = new PleaseWaitDialog(null,
				bundle.getString("client.pairing.pleasewait.message"));
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);

		// try if addr is InetAddress
		try {
			InetAddress inetaddr = InetAddress.getByName(addr);
			PAKNetworkPairingRequester requester = new PAKNetworkPairingRequester(
					inetaddr, password,
					org.panbox.core.devicemgmt.DeviceType.DESKTOP, deviceKey);
			requester.runProtocol();

			finishGeneralPairing(deviceKey, requester);
			dialog.dispose();
		} catch (UnknownHostException e) {
			// Try to connect via Bluetooth! If this also fails then the host
			// could not be connected.
			try {
				// do the search for service discovery
				RemoteDeviceDiscovery.discover();
				List<ServiceRecord> records = RemoteDeviceDiscovery
						.getServiceRecordsByDeviceAddr(addr);
				// check whether we found our device or not
				if (records != null) {
					try {
						PAKBluetoothPairingRequester requester = new PAKBluetoothPairingRequester(
								password, DeviceType.DESKTOP, deviceKey,
								records.get(0)); // Since we only search for a
						// single UUID we will only
						// find a single
						// ServiceRecord!
						finishGeneralPairing(deviceKey, requester);
						dialog.dispose();
						return;
					} catch (IOException ex) {
						logger.error(
								"PanboxClient : runGeneralPairingRequester : Bluetooth pairing failed with IO exception!",
								ex);
					}
				} else {
					logger.error("PanboxClient : runGeneralPairingRequester : The specified device was not found on Device search!");
				}
			} catch (BluetoothStateException | InterruptedException ex) {
				logger.error(
						"PanboxClient : runGeneralPairingRequester : Bluetooth device lookup failed!",
						ex);
			}
			dialog.dispose();

			// Neither LAN nor Bluetooth could find a host. Host could not be
			// connected!
			JOptionPane.showMessageDialog(null,
					bundle.getString("client.pairing.couldnotconnect.message"),
					bundle.getString("client.pairing.failed"),
					JOptionPane.ERROR_MESSAGE);
			logger.error("PanboxClient : runGeneralPairingRequester : Could not connect to host.");
		} catch (Exception e) {
			dialog.dispose();
			JOptionPane.showMessageDialog(null,
					bundle.getString("client.pairing.failed.message"),
					bundle.getString("client.pairing.failed"),
					JOptionPane.ERROR_MESSAGE);
			logger.error(
					"PanboxClient : runGeneralPairingRequester : Pairing failed with exception: ",
					e);
		}
	}

	private void finishGeneralPairing(KeyPair deviceKey,
			PAKCorePairingRequester requester) {
		switch (requester.getType()) {
		case MASTER:
			finishMasterPairing(requester.geteMail(), requester.getFirstName(),
					requester.getLastName(), requester.getKeyPassword(),
					requester.getOwnerCertEnc(), requester.getOwnerKeyEnc(),
					requester.getOwnerCertSign(), requester.getOwnerKeySign(),
					deviceKey, requester.getDeviceName(),
					requester.getDevCert(), requester.getKnownDevices(),
					requester.getKnownContacts());
			break;
		case SLAVE:
			finishSlavePairing(requester.geteMail(), requester.getFirstName(),
					requester.getLastName(), requester.getOwnerCertEnc(),
					requester.getOwnerCertSign(), deviceKey,
					requester.getDeviceName(), requester.getDevCert(),
					requester.getKnownDevices(), requester.getKnownContacts());
			break;
		default:
			logger.error("PanboxClient : runGeneralPairingRequester : Unknown pairing type");
		}
	}

	private void finishMasterPairing(String eMail, String firstName,
			String lastName, char[] keyPassword, X509Certificate certEnc,
			KeyPair ownerKeyEnc, X509Certificate certSign,
			KeyPair ownerKeySign, KeyPair deviceKey, String deviceName,
			X509Certificate deviceCert, Map<String, X509Certificate> devices,
			Collection<VCard> contacts) {
		logger.debug("PanboxClient : finishMasterPairing : Will set up master identity.");
		// create identity and initial keypairs
		Identity id = new Identity(new SimpleAddressbook(), eMail, firstName,
				lastName);
		id.setOwnerKeySign(certSign);
		id.setOwnerKeyEnc(certEnc);
		id.setOwnerKeySign(ownerKeySign, keyPassword);
		id.setOwnerKeyEnc(ownerKeyEnc, keyPassword);
		Settings.getInstance().setPairingType(PairingType.MASTER);
		id.addDeviceKey(deviceKey, deviceName);
		Settings.getInstance().setDeviceName(deviceName);
		identityManager.init(addressbookManager);
		identityManager.storeMyIdentity(id);

		// Erase keys and passwords from memory
		Arrays.fill(keyPassword, '\u0000');

		// This code can be inserted once Java 8 implements destroy-Method
		// in order to remove key material securely from JVM memory
		// try {
		// ownerKeyEnc.getPrivate().destroy();
		// } catch (DestroyFailedException e1) {
		// logger.warn(
		// "PanboxClient : finishMasterPairing : Could not destroy private enc key after pairing: ",
		// e1);
		// }
		// try {
		// ownerKeySign.getPrivate().destroy();
		// } catch (DestroyFailedException e1) {
		// logger.warn(
		// "PanboxClient : finishMasterPairing : Could not destroy private sign key after pairing: ",
		// e1);
		// }
		try {
			File contactsFile = File.createTempFile("panbox-contacts", null);
			AbstractAddressbookManager.exportContacts(contacts, contactsFile);
			addressbookManager.importContacts(id, contactsFile, true);
			contactsFile.delete(); // we can now remove this tempFile
			identityManager.storeMyIdentity(id);
		} catch (ContactExistsException | IOException e) {
			logger.warn("PanboxClient : setUpIdentity : Could not import all contacts to addressbook.");
		}

		try {
			deviceManager.setIdentity(id);
			deviceManager.addThisDevice(deviceName, deviceKey,
					DeviceType.DESKTOP);

			// add other known devices
			for (Entry<String, X509Certificate> dev : devices.entrySet()) {
				deviceManager.addDevice(dev.getKey(), dev.getValue(),
						DeviceType.DESKTOP);
			}
			refreshDeviceListModel();
		} catch (DeviceManagerException ex) {
			logger.error(
					"PanboxClient : finishMasterPairing : Could not add device to device list.",
					ex);
		}
	}

	private void finishSlavePairing(String eMail, String firstName,
			String lastName, X509Certificate ownerCertSign,
			X509Certificate ownerCertEnc, KeyPair deviceKey, String deviceName,
			X509Certificate deviceCert, Map<String, X509Certificate> devices,
			Collection<VCard> contacts) {
		logger.debug("PanboxClient : finishSlavePairing : Will set up slave identity.");
		// create identity and initial keypairs
		Identity id = new Identity(new SimpleAddressbook(), eMail, firstName,
				lastName);

		id.setOwnerKeyEnc(ownerCertEnc);
		id.setOwnerKeySign(ownerCertSign);
		Settings.getInstance().setPairingType(PairingType.SLAVE);
		id.addDeviceKey(deviceKey, deviceName);
		Settings.getInstance().setDeviceName(deviceName);
		identityManager.init(addressbookManager);
		identityManager.storeMyIdentity(id);
		try {
			File contactsFile = File.createTempFile("panbox-contacts", null);
			AbstractAddressbookManager.exportContacts(contacts, contactsFile);
			addressbookManager.importContacts(id, contactsFile, true);
			contactsFile.delete(); // we can now remove this tempFile
			identityManager.storeMyIdentity(id);
		} catch (ContactExistsException | IOException e) {
			logger.warn("PanboxClient : setUpIdentity : Could not import all contacts to addressbook.");
		}

		try {
			deviceManager.setIdentity(id);
			deviceManager.addThisDevice(deviceName, deviceKey,
					DeviceType.DESKTOP);

			// add other known devices
			for (Entry<String, X509Certificate> dev : devices.entrySet()) {
				deviceManager.addDevice(dev.getKey(), dev.getValue(),
						DeviceType.DESKTOP);
			}

			refreshDeviceListModel();
		} catch (DeviceManagerException ex) {
			logger.error(
					"PanboxClient : finishMasterPairing : Could not add device to device list.",
					ex);
		}
	}

	/**
	 * Stores a pairing file at the specified path for the specified device and
	 * type
	 * 
	 * @param outputFile
	 *            Pairing file to be saved
	 * @param devicename
	 *            Name of the device that should be paired
	 * @param password
	 *            Password of the identity
	 */
	public void storePairingFile(File outputFile, String devicename,
			char[] password, PairingType type, DeviceType devType) {
		logger.debug("PanboxClient : storePairingFile : Storing pairing container to: "
				+ outputFile.getAbsolutePath());

		try {
			PanboxFilePairingWriteReturnContainer retCon = null;
			if (type == PairingType.MASTER) {
				retCon = PanboxFilePairingUtils.storePairingFile(outputFile,
						devicename, password, type, devType, myId.getEmail(),
						myId.getFirstName(), myId.getName(),
						myId.getPrivateKeyEnc(password), myId.getCertEnc(),
						myId.getPrivateKeySign(password), myId.getCertSign(),
						getDevicePairingMap(), getContactsPairingList());
			} else {
				retCon = PanboxFilePairingUtils.storePairingFile(outputFile,
						devicename, password, type, devType, myId.getEmail(),
						myId.getFirstName(), myId.getName(), null,
						myId.getCertEnc(), null, myId.getCertSign(),
						getDevicePairingMap(), getContactsPairingList());
			}
			logger.debug("PanboxClient : storePairingFile : Storing pairing container finished.");

			deviceManager.addDevice(retCon.getDevicename(),
					retCon.getDevCert(), retCon.getDevType());
			refreshDeviceListModel();
		} catch (IOException | KeyStoreException | NoSuchAlgorithmException
				| CertificateException ex) {
			logger.error(
					"PanboxClient : storePairingFile : Exception caught: ", ex);
			JOptionPane
					.showMessageDialog(
							null,
							bundle.getString("PanboxClient.errorWhileStoringPairingContainer"),
							bundle.getString("PanboxClient.errorWhileStoringPairingContainer"),
							JOptionPane.ERROR_MESSAGE);
		} catch (UnrecoverableKeyException ex) {
			logger.error("PanboxClient : storePairingFile : Wrong password: ",
					ex);
			JOptionPane
					.showMessageDialog(
							null,
							bundle.getString("PanboxClient.wrongPassword"),
							bundle.getString("PanboxClient.errorWhileStoringPairingContainer"),
							JOptionPane.ERROR_MESSAGE);
		} catch (DeviceManagerException ex) {
			logger.warn(
					"PanboxClient : storePairingFile : Exception caught after pairing file: ",
					ex);
			JOptionPane
					.showMessageDialog(
							null,
							bundle.getString("PanboxClient.errorCouldNotAddDevicePairing"),
							bundle.getString("PanboxClient.errorCouldNotAddDevicePairingTitle"),
							JOptionPane.ERROR_MESSAGE);
		}
	}

	public Map<String, X509Certificate> getDevicePairingMap() {
		Map<String, X509Certificate> devices = new HashMap<>();
		try {
			for (PanboxDevice dev : deviceManager.getDeviceList()) {
				X509Certificate cert = (X509Certificate) myId.getDeviceCert(dev
						.getDeviceName());
				if (cert != null) {
					devices.put(dev.getDeviceName(), cert);
				} else {
					logger.error("PanboxClient : storePairingFile : Could not get device key for device "
							+ dev.getDeviceName()
							+ ". Will not add it to device list.");
				}
			}
		} catch (DeviceManagerException ex) {
			logger.error("PanboxClient : storePairingFile : Could not get device list from DeviceManager. Will not add any device to list.");
		}
		return devices;
	}

	public Collection<VCard> getContactsPairingList() {
		Collection<VCard> vcards = new ArrayList<VCard>();
		for (PanboxContact contact : myId.getAddressbook().getContacts()) {
			vcards.add(AbstractAddressbookManager.contact2VCard(contact));
		}
		return vcards;
	}

	public void loadPairingFile(File inputFile) throws IOException {
		logger.debug("PanboxClient : loadPairingFile : Started importing pairing file: "
				+ inputFile);

		char[] password = PasswordEnterDialog.invoke(PermissionType.DEVICE);

		try {
			PanboxFilePairingLoadReturnContainer retCon = PanboxFilePairingUtils
					.loadPairingFile(inputFile, password);

			setUpIdentity(retCon.geteMail(), retCon.getFirstName(),
					retCon.getLastName(), retCon.getPassword(),
					retCon.getDeviceName(), retCon.getDevicePrivKey(),
					retCon.getDeviceCert(), retCon.getSignPrivKey(),
					retCon.getSignCert(), retCon.getEncPrivKey(),
					retCon.getEncCert(), retCon.getDevices(),
					retCon.getContactsFile());
		} catch (IllegalArgumentException e) {
			logger.error("PanboxClient : loadPairingFile : Could not read pairing file!");
			JOptionPane
					.showMessageDialog(
							null,
							"The provided Panbox pairing file was corrupt. Please create a new one.",
							"Panbox Pairing failed", JOptionPane.ERROR_MESSAGE);
		} catch (NoSuchAlgorithmException | CertificateException
				| KeyStoreException | InvalidKeySpecException e) {
			logger.error("PanboxClient : loadPairingFile : Exception: ", e);
			JOptionPane.showMessageDialog(null,
					"An error occurred while pairing. Please try again",
					"Panbox Pairing failed", JOptionPane.ERROR_MESSAGE);
		} catch (UnrecoverableKeyException e) {
			logger.error("PanboxClient : loadPairingFile : Wrong password while pairing...");
			JOptionPane.showMessageDialog(null,
					"The password you entered was wrong. Please try again.",
					"Panbox Pairing failed", JOptionPane.ERROR_MESSAGE);
		} finally {
			if (password != null) {
				Arrays.fill(password, (char) 0);
			}
		}
	}

	private void setUpIdentity(String email, String firstName, String lastName,
			char[] password, String devicename, PrivateKey deviceKey,
			Certificate deviceCert, PrivateKey ownerKeySign,
			Certificate ownerCertSign, PrivateKey ownerKeyEnc,
			Certificate ownerCertEnc, Map<String, X509Certificate> devices,
			File contactsFile) throws NoSuchAlgorithmException,
			InvalidKeySpecException {
		// create identity and initial keypairs
		Identity id = new Identity(new SimpleAddressbook(), email, firstName,
				lastName);

		id.setOwnerKeyEnc(ownerCertEnc);
		id.setOwnerKeySign(ownerCertSign);
		if (ownerKeySign != null && ownerKeyEnc != null) {
			id.setOwnerKeySign(CryptCore.privateKeyToKeyPair(ownerKeySign),
					password);
			id.setOwnerKeyEnc(CryptCore.privateKeyToKeyPair(ownerKeyEnc),
					password);
			Settings.getInstance().setPairingType(PairingType.MASTER);
		} else {
			Settings.getInstance().setPairingType(PairingType.SLAVE);
		}
		id.addDeviceKey(CryptCore.privateKeyToKeyPair(deviceKey), deviceCert,
				devicename);
		Settings.getInstance().setDeviceName(devicename);
		identityManager.init(addressbookManager);
		identityManager.storeMyIdentity(id);
		try {
			// we assume the pairingfile to be trustworthy
			// w.r.t. the contact trust level
			addressbookManager.importContacts(id, contactsFile, true);
			contactsFile.delete(); // we can now remove this tempFile
			identityManager.storeMyIdentity(id);
		} catch (ContactExistsException e) {
			logger.warn("PanboxClient : setUpIdentity : Could not import all contacts to addressbook.");
		}

		this.myId = id;
		deviceManager.setIdentity(id);
		shareManager.setIdentity(id);

		try {
			// add this device
			deviceManager.addThisDevice(devicename,
					CryptCore.privateKeyToKeyPair(deviceKey),
					DeviceType.DESKTOP);

			// add other known devices
			for (Entry<String, X509Certificate> dev : devices.entrySet()) {
				deviceManager.addDevice(dev.getKey(), dev.getValue(),
						DeviceType.DESKTOP);
			}
			refreshDeviceListModel();
		} catch (DeviceManagerException ex) {
			// Simply ignore this for now!
		}
	}

	// Settings events

	public void languageChanged(Locale locale) {
		// TODO: reload GUI with the chosen locale
	}

	public void settingsFolderChanged(File file) {
		// TODO: Is something needed here?
	}

	public abstract void panboxFolderChanged(String path);

	/**
	 * adds JVM shutdown hook to safeguard execution of clean up operations like
	 * unmounting the vfs
	 */
	protected void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					shutdown();
				} catch (Exception e) {
					logger.error("Encountered error during application shutdown: "
							+ e.getMessage());
				}
			}
		});
	}

	public abstract void restartTrayIcon();

	public ShareListModel getDeviceShares(PanboxDevice device) {
		ShareListModel result = new ShareListModel();
		int max = shareList.getSize();
		for (int i = 0; i < max; i++) {
			PanboxShare share = shareList.get(i);
			if (share.getDevices().contains(device)) {
				result.addElement(share);
			}
		}
		return result;
	}

	public String getOnlineFilename(String shareName, String path) {
		String res = null;
		try {
			PanboxShare share = shareManager.getShareForName(shareName);
			res = shareManager.getOnlineFilename(share, path);
		} catch (Exception ex) {
			logger.error(
					"PanboxClient : getOnlineFilename : Exception occured: ",
					ex);
			JOptionPane
					.showMessageDialog(null,
							bundle.getString("client.error.getOnlineFilename"),
							bundle.getString("client.error"),
							JOptionPane.ERROR_MESSAGE);
		}
		return res;
	}

	public abstract void showTrayMessage(String title, String message,
			MessageType type);

	public void conflictNotification(PanboxShare share, String chk) {
		JOptionPane.showMessageDialog(null, MessageFormat.format(
				bundle.getString("client.warning.conflictNotification"),
				share.getName(), chk), bundle.getString("client.warn"),
				JOptionPane.WARNING_MESSAGE);
	}

	public PanboxShare reloadShare(PanboxShare share) {
		showTrayMessage(bundle.getString("client.warn"), MessageFormat.format(
				bundle.getString("client.shareReloadNotification"),
				share.getName()), MessageType.WARNING);
		try {
			PanboxShare nshare = shareManager.reloadShareMetadata(share);
			PanboxShare tmp;
			if ((tmp = checkShareIntegrity(share)) != null) {
				nshare = tmp;
			}

			int index = shareList.indexOf(share);
			if (index != -1) {
				shareList.setElementAt(nshare, index);
			} else {
				logger.error("Could not find share instance " + share
						+ " in shareList");
			}
			getMainWindow().refreshShare();
			return nshare;
		} catch (ShareManagerException e) {
			JOptionPane.showMessageDialog(null,
					bundle.getString("client.error.shareReloadFailed"),
					bundle.getString("error"), JOptionPane.ERROR_MESSAGE);
		}
		return null;
	}

	public abstract void openShareFolder(String name);

	/**
	 * checks if symmetric key size defined in {@link KeyConstants} is supported
	 * by JVM
	 * 
	 * @return <code>true</code> if key size is valid, <code>false</code>
	 *         otherwise
	 */
	public static boolean checkSupportedKeySize() {
		try {
			int ksize = Cipher
					.getMaxAllowedKeyLength(KeyConstants.SYMMETRIC_ALGORITHM);
			logger.info("Maximum supported key size: " + ksize);
			if (ksize < KeyConstants.SYMMETRIC_KEY_SIZE) {	
				return false;
			}
		} catch (NoSuchAlgorithmException e) {
			logger.error("Unable to check supported key size!", e);
			return false;
		}
		return true;
	}
}
