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
package org.panbox.desktop.linux;

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
import java.net.BindException;
import java.rmi.NotBoundException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.panbox.PanboxConstants;
import org.panbox.Settings;
import org.panbox.core.crypto.AbstractObfuscatorFactory;
import org.panbox.core.crypto.FileObfuscatorFactory;
import org.panbox.core.crypto.randomness.SecureRandomWrapper;
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
import org.panbox.desktop.common.utils.SingleInstanceLock;
import org.panbox.desktop.linux.dbus.DBusService;
import org.panbox.desktop.linux.tray.PanboxTrayIcon;
import org.panbox.desktop.linux.tray.PanboxTrayIcon.TrayIconException;


/**
 * @author palige
 * 
 *         Control class implementation for application startup and shutdown
 */
public class PanboxClient extends org.panbox.desktop.common.PanboxClient {

	// static {
	// PropertyConfigurator.configure("log4j.properties");
	// }

	private static boolean NO_DBUS_OPTION = false;
	private static String NO_DBUS = "--no-dbus";

	private static boolean FALLBACK_TRAY_JAVA_OPTION = false;
	private static String FALLBACK_TRAY_JAVA = "--fallback-tray-java";

	private static boolean FALLBACK_TRAY_GTK_OPTION = false;
	private static String FALLBACK_TRAY_GTK = "--fallback-tray-gtk";
	private static String[] vfsoptions = null;

	public static void main(String[] args) {
		setGuiLookAndFeel();
		
		// params
		ArrayList<String> vfsopts = new ArrayList<String>();
		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase(NO_DBUS)) {
				NO_DBUS_OPTION = true;
				// no dbus alo means fallbaxk tray icon implicitely
				FALLBACK_TRAY_JAVA_OPTION = true;
				logger.info("Found argument " + NO_DBUS
						+ ", falling back to java tray support");
			} else if (args[i].equalsIgnoreCase(FALLBACK_TRAY_JAVA)) {
				FALLBACK_TRAY_JAVA_OPTION = true;
				logger.info("Found argument " + FALLBACK_TRAY_JAVA);
			} else if (args[i].equalsIgnoreCase(FALLBACK_TRAY_GTK)) {
				FALLBACK_TRAY_GTK_OPTION = true;
				logger.info("Found argument " + FALLBACK_TRAY_GTK);
			} else if (args[i].equals("-d") || args[i].equals("-f")
					|| args[i].equals("-s")) {
				vfsopts.add(args[i]);
			} else if (args[i].equals("-o")) {
				if ((args.length > (i + 1)) && (!args[i + 1].startsWith("-"))) {
					vfsopts.add(args[i]);
					vfsopts.add(args[i + 1]);
				} else if ((args.length > (i + 1))
						&& (args[i + 1].startsWith("-"))) {
					logger.error("Invalid fuse argument: " + (args[i + 1]));
				} else {
					logger.error("Invalid fuse argument.");
				}
			} else {
				if (!((i > 0) && (args[i - 1].equals("-o")))) {
					logger.error("Unknown argument: " + args[i]);
				}
			}
		}

		vfsoptions = vfsopts.toArray(new String[vfsopts.size()]);

		try {
			vfsControl = VFSControl.getInstance(vfsoptions);
			SecureRandomWrapper.getInstance();
			AbstractObfuscatorFactory.getFactory(FileObfuscatorFactory.class);
			instance = new PanboxClient(new LinuxPanboxService());

			instance.registerMainwindow(new PanboxClientGUI(instance));
			if (FALLBACK_TRAY_JAVA_OPTION && (instance.fallbackTrayApp == null)) {
				logger.fatal("No TrayIcon could be created - overriding default close operation for main window");
				instance.mainWindow
						.setDefaultCloseOperation(PanboxClientGUI.DO_NOTHING_ON_CLOSE);
				instance.mainWindow
						.addWindowListener(new java.awt.event.WindowAdapter() {
							@Override
							public void windowClosing(
									java.awt.event.WindowEvent windowEvent) {
								if (instance.checkShutdown()) {
									System.exit(0);
								}
							}
						});
			}

			instance.showGui();
			instance.showTrayMessage(bundle
					.getString("PanboxClient.panboxStartedMessage"));

		} catch (IOException e) {
			// do nothing
		} catch (OperationAbortedException e) {
			logger.error("PanboxClient : Wizard has been aborted.");
			System.exit(DesktopApi.EXIT_ERR_WIZARD_ABORTED);
		} catch (ShareManagerException | DeviceManagerException e) {
			logger.error("ShareManager or DeviceManager may be broken.", e);
			JOptionPane.showMessageDialog(null, bundle
					.getString("client.startup.failedCorruptManager.message"),
					bundle.getString("client.startup.error.title"),
					JOptionPane.ERROR_MESSAGE);
			System.exit(DesktopApi.EXIT_ERR_SERVICE_NOT_AVAILBLE);
		} catch (NotBoundException e) {
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
			logger.error("Could not find a specified library.", e);
			JOptionPane.showMessageDialog(null,
					bundle.getString("client.startup.failedToLoadLib.message"),
					bundle.getString("client.startup.error.title"),
					JOptionPane.ERROR_MESSAGE);
			System.exit(DesktopApi.EXIT_ERR_UNKNOWN);

		} catch (Exception e) {
			logger.error("An unknown error occurred while Panbox startup.", e);
			JOptionPane.showMessageDialog(null,
					bundle.getString("client.startup.failedUnknown.message"),
					bundle.getString("client.startup.error.title"),
					JOptionPane.ERROR_MESSAGE);
			System.exit(DesktopApi.EXIT_ERR_UNKNOWN);
		}
	}

	private void showTrayMessage(String string) {
		showTrayMessage("", string, MessageType.INFO);
	}

	private static void setGuiLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException
				| IllegalAccessException | UnsupportedLookAndFeelException e) {
			logger.debug("Could not load system specific look and feel. Will use default one.");
		}
	}

	private static PanboxClient instance;

	/**
	 * @return the instance
	 */
	public static PanboxClient getInstance() {
		return instance;
	}

	private PanboxClient(IPanboxService service) throws Exception {
		super(service);
		logger.info("Finished initialization");
		if (splash != null) { // Splashscreen is shown!
			try {
				// splash.close();
			} catch (Exception ex) {
				logger.error("Error closing splash screen.", ex);
			}
		}
	}

	@Override
	public void informAddShare(PanboxShare share) throws Exception {
		MessageFormat formatter = new MessageFormat("", Settings.getInstance()
				.getLocale());
		formatter.applyPattern(bundle.getString("tray.addedShareMessage"));
		showTrayMessage(formatter.format(new Object[] { share.getName() }));
	}

	@Override
	public void informRemoveShare(PanboxShare share) throws Exception {
		MessageFormat formatter = new MessageFormat("", Settings.getInstance()
				.getLocale());
		formatter.applyPattern(bundle.getString("tray.removedShareMessage"));
		showTrayMessage(formatter.format(new Object[] { share.getName() }));
		logger.debug("Share will be added to VFS view: " + share.getName());
	}

	@Override
	public void panboxFolderChanged(String path) {

	}

	private static ClipboardHandler ch;
	private static DBusService dBusService = DBusService.getInstance();
	private static VFSControl vfsControl;

	private void startClipboardHandler() {
		// register clipboard handler to get access to panbox urls in
		// clipboard
		ClipboardObserver co = new ClipboardObserver();
		ch = new ClipboardHandler();
		ch.addObserver(co);
		ch.start();
	}

	private void startURIServer() {
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

	private void stopClipboardHandler() {
		if (ch != null)
			ch.stop();
	}

	public void restartTrayIcon() {
		try {
			PanboxTrayIcon.getInstance(FALLBACK_TRAY_GTK_OPTION).restart();
		} catch (TrayIconException e) {
			JOptionPane.showMessageDialog(null, e.getMessage(),
					"TrayIcon error", JOptionPane.ERROR_MESSAGE);
			logger.fatal("Could not start TrayIcon", e);
			System.exit(-1);
		}
	}

	private boolean startVFS() {
		return vfsControl.mount();
	}

	private void stopVFS() {
		vfsControl.unmount();
	}

	public void showGui() {
		getMainWindow().setVisible(true);
		getMainWindow().toFront();
		// gui.setState(JFrame.NORMAL);
	}

	@Override
	public void shutdown() {
		stopClipboardHandler();
		PanboxTrayIcon.getInstance(FALLBACK_TRAY_GTK_OPTION).stop();
		dBusService.stop();
		stopVFS();
		SingleInstanceLock.forceUnlock();
	}

	@Override
	public void setup() throws Exception {
		if (!NO_DBUS_OPTION) {
			dBusService.start();
		} else {
			logger.info("DBUS service startup canceled ...");
		}

		// mountShares();
		if (!startVFS()) {
			logger.fatal("Failed to mount VFS - exiting application");
			System.exit(-1);
		}

		if (Settings.getInstance().isUriHandlerSupported()) {
			startURIServer();
			if (Settings.getInstance().isClipboardHandlerSupported()) {
				startClipboardHandler();
			}
		}

		try {
			if (FALLBACK_TRAY_JAVA_OPTION) {
				logger.info("Fallback tray icon support encabled. Starting fallback tray icon...");
				fallbackJavaTray();
			} else {
				PanboxTrayIcon.getInstance(FALLBACK_TRAY_GTK_OPTION).start();
			}

		} catch (TrayIconException e) {
			JOptionPane.showMessageDialog(null, e.getMessage(),
					"TrayIcon error", JOptionPane.ERROR_MESSAGE);
			logger.fatal("Could not start TrayIcon", e);
			System.exit(-1);
		}
	}

	private TrayIcon fallbackTrayApp;

	private void fallbackJavaTray() {
		logger.debug("initTray");
		Image image = getPanboxIcon();
		if (SystemTray.isSupported() && (image != null)) {
			fallbackTrayApp = new TrayIcon(image);
		} else {
			logger.fatal("System is missing tray icon support!");
			return;
		}

		fallbackTrayApp.setToolTip(bundle.getString("tray.toolTip"));

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
				logger.debug("showClientItem action called");
				showGui();
			}
		};

		showClientItem.addActionListener(showPanboxActionListener);

		openFolderItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				logger.debug("openFolderItem action called");
				try {
					// try to open panbox folder
					DesktopApi.open(vfsControl.getMountpoint());
				} catch (IllegalArgumentException e1) {
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
				logger.debug("LINUX:PanboxClient : aboutItem action called");
				AboutWindow.getInstance().showWindow(5);
			}
		});

		exitClientItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				logger.debug("exitClientItem action called");
				System.exit(0);
			}
		});

		popupMenu.add(showClientItem);
		popupMenu.add(openFolderItem);
		popupMenu.add(aboutItem);
		popupMenu.add(exitClientItem);

		fallbackTrayApp.setPopupMenu(popupMenu);
		fallbackTrayApp.addActionListener(showPanboxActionListener);

		SystemTray systemTray = SystemTray.getSystemTray();
		try {
			systemTray.add(fallbackTrayApp);
		} catch (AWTException e) {
			logger.debug("Could not add the PanboxClient tray app to the system tray.");
			System.exit(0);
		}

		fallbackTrayApp.displayMessage(bundle.getString("tray.panboxStarted"),
				bundle.getString("tray.nowReayMessage"), MessageType.INFO);
	}

	private static Image getPanboxIcon() {
		Image image = new ImageIcon().getImage();
		try {
			InputStream stream = null;
			stream = ClassLoader.class
					.getResourceAsStream("/img/panbox-icon.png");
			if (stream == null) {
				stream = ClassLoader.class
						.getResourceAsStream("/res/img/panbox-icon.png");
			}
			image = (stream != null) ? ImageIO.read(stream) : null;

		} catch (IllegalArgumentException | IOException e) {
			logger.warn("Could not obtain icon resource. Will use empty picture instead.");
		}
		return image;
	}

	@Override
	protected boolean checkPanboxProcessesRunning() {
		return CrashHandler.getInstance().checkPanboxProcessRunning();
	}

	@Override
	protected boolean mountPointHandler() {
		return CrashHandler.getInstance().umountPanbox();
	}

	@Override
	protected boolean panboxMounted() {
		return CrashHandler.getInstance().panboxMounted();
	}

	@Override
	public void showTrayMessage(String title, String message, MessageType type) {
		if (!FALLBACK_TRAY_JAVA_OPTION) {
			try {
				PanboxTrayIcon.getInstance(FALLBACK_TRAY_GTK_OPTION)
						.showNotification(message);
			} catch (TrayIconException e) {
				logger.error("Error sending notification via tray icon!", e);
			}
		} else if (fallbackTrayApp != null) {
			fallbackTrayApp.displayMessage(title, message, type);
		}
	}

	/**
	 * Sun property pointing the main class and its arguments. Might not be
	 * defined on non Hotspot VM implementations.
	 */
	public static final String SUN_JAVA_COMMAND = "sun.java.command";

	/**
	 * Restart the current Java application
	 * 
	 * @throws IOException
	 */
	public void restartApplication() throws IOException {
		if (!vfsControl.isUmountSafe() && !checkShutdown()) {
			return;
		} else {

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
				String[] mainCommand = System.getProperty(SUN_JAVA_COMMAND)
						.split(" ");
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
				// execute the command in a shutdown hook, to be sure that all
				// the
				// resources have been disposed before restarting the
				// application
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

				shutdown();
				// exit
				System.exit(0);
			} catch (Exception e) {
				// something went wrong
				throw new IOException(
						"Error while trying to restart the application", e);
			}
		}
	}

	public boolean checkShutdown() {
		boolean res;
		if (vfsControl.isUmountSafe()) {
			int ret = JOptionPane.showConfirmDialog(null,
					bundle.getString("client.reallyShutdown.message"),
					bundle.getString("client.reallyShutdown.title"),
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (ret == JOptionPane.YES_OPTION) {
				res = true;
			} else {
				res = false;
			}
		} else {
			int ret = JOptionPane.showConfirmDialog(null,
					bundle.getString("client.reallyShutdown.warning"),
					bundle.getString("client.reallyShutdown.title"),
					JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (ret == JOptionPane.YES_OPTION) {
				res = true;
			} else {
				res = false;
			}
		}
		return res;
	}

	@Override
	public void openShareFolder(String name) {
		File mpoint = vfsControl.getMountpoint();
		File vshare = new File(mpoint, name);
		DesktopApi.open(vshare);
	}

}
