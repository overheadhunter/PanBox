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
package org.panbox.desktop.windows.client;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.net.BindException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.panbox.PanboxConstants;
import org.panbox.Settings;
import org.panbox.WinRegistry;
import org.panbox.core.keymgmt.VolumeParams.VolumeParamsFactory;
import org.panbox.desktop.common.clipboard.ClipboardHandler;
import org.panbox.desktop.common.clipboard.ClipboardObserver;
import org.panbox.desktop.common.devicemgmt.DeviceManagerException;
import org.panbox.desktop.common.gui.AboutWindow;
import org.panbox.desktop.common.gui.OperationAbortedException;
import org.panbox.desktop.common.gui.PanboxClientGUI;
import org.panbox.desktop.common.gui.shares.PanboxShare;
import org.panbox.desktop.common.sharemgmt.IPanboxService;
import org.panbox.desktop.common.sharemgmt.ShareManagerException;
import org.panbox.desktop.common.urihandler.PanboxHTTPServer;
import org.panbox.desktop.common.utils.DesktopApi;
import org.panbox.desktop.windows.service.PanboxClientSession;
import org.panbox.desktop.windows.service.PanboxWindowsServiceInterface;
import org.panbox.desktop.windows.service.VFSManager;

import com.sun.jna.platform.win32.Advapi32Util;

public class PanboxClient extends org.panbox.desktop.common.PanboxClient {

	static {
		try {
			File programDataFolder = new File(System.getenv("APPDATA")
					+ "\\Panbox.org\\Panbox\\");
			if (!programDataFolder.exists()) {
				if (!programDataFolder.mkdirs()) {
					System.err
							.println("Failed to create subdir in ProgramData: "
									+ programDataFolder.getAbsolutePath());
				} else {
					System.out.println("Created subdir in ProgramData");
				}
			}

			File logFile = new File(programDataFolder,
					"PanboxWindowsClient.log");

			PatternLayout layout = new PatternLayout("%d %-5p [%t]: %m%n");

			logger.setLevel(Level.ALL);
			logger.addAppender(new ConsoleAppender(new PatternLayout()));
			logger.addAppender(new RollingFileAppender(layout, logFile
					.getAbsolutePath(), true));
		} catch (IOException ex) {
			System.err
					.println("Failed to add appender for logging files! Logging might not be available!");
			logger.error(
					"PanboxClient : Failed to add appender for logging files!",
					ex);
		}
		logger.debug("PanboxClient : Class constructed");
	}

	public static void main(String[] args) {
		setGuiLookAndFeel();

		try {
			// Connect to the Panbox service
			PanboxWindowsServiceInterface service = (PanboxWindowsServiceInterface) Naming
					.lookup("//localhost/PanboxWindowsService");
			PanboxClientSession session = null;
			String username = Advapi32Util.getUserName();
			setupRegistryForAuthentication();
			String secretLookup = service.askLogin(username);
			try {
				session = service.authLogin(username,
						getSharedSecretFromRegistry(secretLookup));
				if (session == null) {
					throw new IllegalAccessException();
				}
			} catch (IllegalAccessException e) {
				logger.error("PanboxClient : Authentication on service failed!");
				JOptionPane
						.showMessageDialog(
								null,
								bundle.getString("client.startup.serviceAuthFailed.message"),
								bundle.getString("client.startup.serviceAuthFailed.title"),
								JOptionPane.ERROR_MESSAGE);
				System.exit(DesktopApi.EXIT_ERR_SERVICE_AUTH_FAILED);
			}
			session.setService(service);
			PanboxClient client = new PanboxClient(session);

			// update MountPath for user
			try {
				Settings.getInstance().setMountDir(
						VFSManager.getMountPoint() + ":" + File.separator
								+ System.getProperty("user.name"));
			} catch (IllegalArgumentException e) {
				logger.warn("PanboxClient : Could not read Panbox mount point so that mountDir could not be updated!");
			}

			// if initialization succeeds, we can create and set the gui
			PanboxClientGUI gui = new PanboxClientGUI(client);
			gui.setIconImage(getPanboxIcon(false));
			client.registerMainwindow(gui);

			gui.setVisible(true);
		} catch (OperationAbortedException e) {
			logger.error("PanboxClient : Wizard has been aborted.");
			System.exit(DesktopApi.EXIT_ERR_WIZARD_ABORTED);
		} catch (ShareManagerException | DeviceManagerException e) {
			logger.error(
					"PanboxClient : ShareManager or DeviceManager may be broken.",
					e);
			JOptionPane.showMessageDialog(null, bundle
					.getString("client.startup.failedCorruptManager.message"),
					bundle.getString("client.startup.error.title"),
					JOptionPane.ERROR_MESSAGE);
			System.exit(DesktopApi.EXIT_ERR_SERVICE_NOT_AVAILBLE);
		} catch (MalformedURLException | RemoteException | NotBoundException e) {
			logger.error("PanboxClient : Could not connect to Panbox Service.",
					e);
			JOptionPane
					.showMessageDialog(
							null,
							bundle.getString("client.startup.couldNotConnectService.message"),
							bundle.getString("client.startup.couldNotConnectService.title"),
							JOptionPane.ERROR_MESSAGE);
			System.exit(DesktopApi.EXIT_ERR_SERVICE_NOT_AVAILBLE);
		} catch (UnsatisfiedLinkError e) {
			logger.error("PanboxClient : Could not find a specified library.",
					e);
			JOptionPane.showMessageDialog(null,
					bundle.getString("client.startup.failedToLoadLib.message"),
					bundle.getString("client.startup.error.title"),
					JOptionPane.ERROR_MESSAGE);
			System.exit(DesktopApi.EXIT_ERR_UNKNOWN);
		} catch (Exception e) {
			logger.error(
					"PanboxClient : An unknown error occurred while Panbox startup.",
					e);
			JOptionPane.showMessageDialog(null,
					bundle.getString("client.startup.failedUnknown.message"),
					bundle.getString("client.startup.error.title"),
					JOptionPane.ERROR_MESSAGE);
			System.exit(DesktopApi.EXIT_ERR_UNKNOWN);
		}
	}

	private static void setupRegistryForAuthentication() {
		try {
			WinRegistry.deleteKey(WinRegistry.HKEY_CURRENT_USER,
					"SOFTWARE\\Panbox.org\\Panbox\\session");
		} catch (IllegalArgumentException | IllegalAccessException
				| InvocationTargetException e) {
			// ignore
		}

		try {
			WinRegistry.createKey(WinRegistry.HKEY_CURRENT_USER,
					"SOFTWARE\\Panbox.org\\Panbox\\session");
		} catch (IllegalArgumentException | IllegalAccessException
				| InvocationTargetException e) {
			logger.error(
					"PanboxClient : Failed to setup registry for authentication. Will continue.",
					e);
		}
	}

	private static String getSharedSecretFromRegistry(String secretLookup)
			throws IllegalAccessException {
		try {
			String value = WinRegistry.readString(
					WinRegistry.HKEY_CURRENT_USER,
					"SOFTWARE\\Panbox.org\\Panbox\\session", secretLookup);
			WinRegistry.deleteValue(WinRegistry.HKEY_CURRENT_USER,
					"SOFTWARE\\Panbox.org\\Panbox\\session", secretLookup);
			return value;
		} catch (IllegalArgumentException | IllegalAccessException
				| InvocationTargetException e) {
			throw new IllegalAccessException(
					"Access denied on service. Could not lookup shared secret!");
		}
	}

	private static void setGuiLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException
				| IllegalAccessException | UnsupportedLookAndFeelException e) {
			logger.debug("Could not load system specific look and feel. Will use default one.");
		}
	}

	private final IPanboxService session;

	private TrayIcon trayApp;
	
	private ClipboardHandler ch;

	public PanboxClient(IPanboxService session) throws Exception {
		super(session);
		this.session = session;
		logger.debug("WIN:PanboxClient : PanboxClient");
		if (splash != null) { // Splashscreen is shown!
			splash.close();
		}
	}

	private void mountShares() throws Exception {
		logger.debug("WIN:PanboxClient : mountShares");
	}

	private void unmountShares() throws Exception {
		logger.debug("WIN:PanboxClient : unmountShares");
		for (PanboxShare share : Collections.list(shareList.elements())) {
			VolumeParamsFactory paramsFactory = shareManager.getParamsFactory();
			session.removeShare(paramsFactory.createVolumeParams()
					.setShareName(share.getName()).setPath(share.getPath())
					.setType(share.getType()));
		}
	}

	private static Image getPanboxIcon(boolean trayIcon) {
		Image image = new ImageIcon().getImage();
		try {
			InputStream stream = null;
			if (trayIcon) {
				stream = ClassLoader.class
						.getResourceAsStream("/img/panbox-trayicon.png");
			} else {
				stream = ClassLoader.class
						.getResourceAsStream("/img/panbox-icon.png");
			}
			Image loadedImage = ImageIO.read(stream);
			if (loadedImage != null) {
				image = loadedImage;
			}
		} catch (IOException e) {
			logger.debug("Could not obtain icon resource. Will use empty picture instead.");
		}
		return image;
	}

	private void initTray() {
		logger.debug("WIN:PanboxClient : initTray");
		Image image = getPanboxIcon(true);
		trayApp = new TrayIcon(image);
		trayApp.setToolTip(bundle.getString("tray.toolTip"));

		PopupMenu popupMenu = new PopupMenu();
		MenuItem showClientItem = new MenuItem(
				bundle.getString("tray.showPanboxClient"));
		MenuItem openFolderItem = new MenuItem(
				bundle.getString("tray.openPanboxFolder"));
		MenuItem aboutItem = new MenuItem(bundle.getString("tray.about"));
		MenuItem exitClientItem = new MenuItem(bundle.getString("tray.exit"));

		ActionListener showPanboxActionListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				logger.debug("WIN:PanboxClient : showClientItem action called");
				mainWindow.setVisible(true);
			}
		};

		showClientItem.addActionListener(showPanboxActionListener);

		openFolderItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				logger.debug("WIN:PanboxClient : openFolderItem action called");
				try {
					String mountPoint = VFSManager.getMountPoint();
					// try to open panbox folder
					if (!DesktopApi.open(new File(mountPoint + ":\\"
							+ System.getProperty("user.name")))) {
						// if the folder does not exist, user does not have any
						// shares! open panbox drive then so user will see that
						// no folder exists
						DesktopApi.open(new File(mountPoint + ":\\"));
					}
				} catch (IllegalArgumentException | IllegalAccessException
						| InvocationTargetException e1) {
					JOptionPane.showMessageDialog(null,
							bundle.getString("tray.CouldNotFindPanboxDrive"),
							bundle.getString("tray.PanboxError"),
							JOptionPane.ERROR_MESSAGE);
					logger.error(
							getClass().getName()
									+ " : Error while determining Panbox drive on system. ",
							e1);
				}
			}
		});

		aboutItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				logger.debug("WIN:PanboxClient : aboutItem action called");
				AboutWindow.getInstance().showWindow(5);
			}
		});

		exitClientItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				logger.debug("WIN:PanboxClient : exitClientItem action called");
				System.exit(0);
			}
		});

		popupMenu.add(showClientItem);
		popupMenu.addSeparator();
		popupMenu.add(openFolderItem);
		popupMenu.addSeparator();
		popupMenu.add(aboutItem);
		popupMenu.add(exitClientItem);

		trayApp.setPopupMenu(popupMenu);
		trayApp.addActionListener(showPanboxActionListener);

		SystemTray systemTray = SystemTray.getSystemTray();
		try {
			systemTray.add(trayApp);
		} catch (AWTException e) {
			logger.debug("Could not add the PanboxClient tray app to the system tray.");
			System.exit(0);
		}

		trayApp.displayMessage(bundle.getString("tray.panboxStarted"),
				bundle.getString("tray.nowReayMessage"), MessageType.INFO);
	}

	@Override
	public void informAddShare(PanboxShare share) throws Exception {
		if (trayApp != null) {
			MessageFormat formatter = new MessageFormat("", Settings
					.getInstance().getLocale());
			formatter.applyPattern(bundle.getString("tray.addedShareMessage"));
			trayApp.displayMessage(bundle.getString("tray.addedShare"),
					formatter.format(new Object[] { share.getName() }),
					MessageType.INFO);
		}
	}

	@Override
	public void informRemoveShare(PanboxShare share) throws Exception {
		if (trayApp != null) {
			MessageFormat formatter = new MessageFormat("", Settings
					.getInstance().getLocale());
			formatter
					.applyPattern(bundle.getString("tray.removedShareMessage"));
			trayApp.displayMessage(bundle.getString("tray.removedShare"),
					formatter.format(new Object[] { share.getName() }),
					MessageType.INFO);
		}
	}

	@Override
	public void panboxFolderChanged(String path) {
		// This feature does not exist in Windows version!
	}

	@Override
	protected void shutdown() throws Exception {
		logger.debug("WIN:PanboxClient : shutdown");
		unmountShares();
		
		stopClipboardHandler();
	}

	@Override
	protected void setup() throws Exception {
		logger.debug("WIN:PanboxClient : setup");
		initTray();
		mountShares();

		if (Settings.getInstance().isUriHandlerSupported()) {
			startURIServer();
			if (Settings.getInstance().isClipboardHandlerSupported()) {
				startClipboardHandler();
			}
		}
	}

	private void startClipboardHandler() {
		// register clipboard handler to get access to panbox urls in
		// clipboard
		ClipboardObserver co = new ClipboardObserver();
		ch = new ClipboardHandler();
		ch.addObserver(co);
		ch.start();
	}

	private void stopClipboardHandler() {
		if (ch != null)
			ch.stop();
	}

	private void startURIServer() {
		logger.debug("WIN:PanboxClient : startURIServer");
		try {
			PanboxHTTPServer.getInstance(this).start();
		} catch (BindException e) {
			logger.error("Error binding Panbox URI Handler to default port", e);
			int ret = JOptionPane.showConfirmDialog(null, MessageFormat.format(
					bundle.getString("PanboxClient.BindException.message"),
					String.valueOf(PanboxConstants.PANBOX_DEFAULT_PORT)),
					bundle.getString("error"), JOptionPane.YES_NO_OPTION);
			if (ret == JOptionPane.YES_OPTION) {
				Settings.getInstance().setUriHandlerSupported(false);
			}
		} catch (Exception e) {
			logger.error("Error starting the Panbox URI Handler", e);
		}
	}

	@Override
	public void restartTrayIcon() {
		logger.debug("WIN:PanboxClient : restartTrayIcon");
		// This makes no sense at all because language won't change after an
		// update. Bundle is static and will only be reloaded in case client
		// will be restarted!

		// remove before adding the new one!
		// SystemTray.getSystemTray().remove(trayApp);
		// initTray();
	}

	@Override
	protected boolean checkPanboxProcessesRunning() {
		return true;
	}

	@Override
	protected boolean mountPointHandler() {
		return true;

	}

	@Override
	protected boolean panboxMounted() {
		return true;
	}

	public void showTrayMessage(String title, String message, MessageType type) {
		trayApp.displayMessage(title, message, type);
	}

	/**
	 * Sun property pointing the main class and its arguments. Might not be
	 * defined on non Hotspot VM implementations.
	 */
	public static final String SUN_JAVA_COMMAND = "sun.java.command";

	/**
	 * Restart the current Java application
	 * 
	 * @param runBeforeRestart
	 *            some custom code to be run before restarting
	 * @throws IOException
	 */
	public void restartApplication() throws IOException {
		try {
			// java binary
			String java = System.getProperty("java.home") + "/bin/java";
			// vm arguments
			List<String> vmArguments = ManagementFactory.getRuntimeMXBean()
					.getInputArguments();
			StringBuffer vmArgsOneLine = new StringBuffer();
			for (String arg : vmArguments) {
				// if it's the agent argument : we ignore it otherwise the
				// address of the old application and the new one will be in
				// conflict
				if (!arg.contains("-agentlib")) {
					vmArgsOneLine.append(arg);
					vmArgsOneLine.append(" ");
				}
			}
			// init the command to execute, add the vm args
			final StringBuffer cmd = new StringBuffer(java + " "
					+ vmArgsOneLine);

			// program main and program arguments
			String[] mainCommand = System.getProperty(SUN_JAVA_COMMAND).split(
					" ");
			// program main is a jar
			if (mainCommand[0].endsWith(".jar")) {
				// if it's a jar, add -jar mainJar
				cmd.append("-jar " + new File(mainCommand[0]).getPath());
			} else {
				// else it's a .class, add the classpath and mainClass
				cmd.append("-cp \"" + System.getProperty("java.class.path")
						+ "\" " + mainCommand[0]);
			}
			// finally add program arguments
			for (int i = 1; i < mainCommand.length; i++) {
				cmd.append(" ");
				cmd.append(mainCommand[i]);
			}
			// execute the command in a shutdown hook, to be sure that all the
			// resources have been disposed before restarting the application
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					try {
						Runtime.getRuntime().exec(cmd.toString());
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			// // execute some custom code before restarting
			// if (runBeforeRestart != null) {
			// runBeforeRestart.run();
			// }
			shutdown();
			// exit
			System.exit(0);
		} catch (Exception e) {
			// something went wrong
			throw new IOException(
					"Error while trying to restart the application", e);
		}
	}

	@Override
	public void openShareFolder(String name) {
		logger.debug("WIN:PanboxClient : openFolderItem action called");
		try {
			String mountPoint = VFSManager.getMountPoint();
			// try to open panbox folder
			if (!DesktopApi.open(new File(mountPoint + ":\\"
					+ System.getProperty("user.name") + File.separator + name))) {
				// if the folder does not exist, user does not have any
				// shares! open panbox drive then so user will see that
				// no folder exists
				DesktopApi.open(new File(mountPoint + ":\\" + File.separator
						+ name));
			}
		} catch (IllegalArgumentException | IllegalAccessException
				| InvocationTargetException e1) {
			JOptionPane.showMessageDialog(null,
					bundle.getString("tray.CouldNotFindPanboxDrive"),
					bundle.getString("tray.PanboxError"),
					JOptionPane.ERROR_MESSAGE);
			logger.error(getClass().getName()
					+ " : Error while determining Panbox drive on system. ", e1);
		}
	}
}
