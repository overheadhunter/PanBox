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
package org.panbox.desktop.common.utils;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.panbox.OS;
import org.panbox.OS.OperatingSystem;
import org.panbox.desktop.common.clipboard.ClipboardHandler;
import org.panbox.WinRegistry;

public class DesktopApi {

	private final static Logger logger = Logger.getLogger(DesktopApi.class);

	/**
	 * Desktop client return codes for main application to determine what kind
	 * of error occurred on execution.
	 */
	public static final int EXIT_SUCCESS = 0;
	public static final int EXIT_ERR_UNKNOWN = 1;
	public static final int EXIT_ERR_ALREADY_RUNNING = 2;
	public static final int EXIT_ERR_SERVICE_NOT_AVAILBLE = 3;
	public static final int EXIT_ERR_WIZARD_ABORTED = 4;
	public static final int EXIT_CRASH_DETECTED = 5;
	public static final int EXIT_ERR_SERVICE_AUTH_FAILED = 6;
	public static final int EXIT_INVALID_KEY_LENGTH = 7;

	public static boolean browse(URI uri) {

		if (browseDESKTOP(uri)) {
			return true;
		}

		if (openSystemSpecific(uri.toString())) {
			return true;
		}

		return false;
	}

	public static boolean open(File file) {

		if (openDesktop(file))
			return true;

		if(OS.getOperatingSystem().isLinux()) {
			if (openSystemSpecific(file.getPath()))
				return true;
		}

		return false;
	}

	public static boolean edit(File file) {

		// you can try something like
		// runCommand("gimp", "%s", file.getPath())
		// based on user preferences.

		if (openSystemSpecific(file.getPath()))
			return true;

		if (editDesktop(file))
			return true;

		return false;
	}

	private static boolean openSystemSpecific(String what) {

		OperatingSystem os = OS.getOperatingSystem();

		if (os.isLinux()) {
			if (runCommand("xdg-open", "%s", what)) {
				return true;
			}

			if (runCommand("gnome-open", "%s", what)) {
				return true;
			}

			if (runCommand("kde-open", "%s", what)) {
				return true;
			}
		}

		// if (os.isMac()) {
		// if (runCommand("open", "%s", what))
		// return true;
		// }

		if (os.isWindows()) {
			if (runCommand("cmd.exe", "start %s", what)) {
				return true;
			}
		}
		return false;
	}

	private static boolean browseDESKTOP(URI uri) {

		logOut("Trying to use Desktop.getDesktop().browse() with "
				+ uri.toString());
		try {
			if (!Desktop.isDesktopSupported()) {
				logErr("Platform is not supported.");
				return false;
			}

			if (!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
				logErr("BORWSE is not supported.");
				return false;
			}

			Desktop.getDesktop().browse(uri);

			return true;
		} catch (Throwable t) {
			logErr("Error using desktop browse.", t);
			return false;
		}
	}

	private static boolean openDesktop(File file) {

		logOut("Trying to use Desktop.getDesktop().open() with "
				+ file.toString());
		try {
			if (!Desktop.isDesktopSupported()) {
				logErr("Platform is not supported.");
				return false;
			}

			if (!Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
				logErr("OPEN is not supported.");
				return false;
			}

			Desktop.getDesktop().open(file);

			return true;
		} catch (Throwable t) {
			logErr("Error using desktop open.", t);
			return false;
		}
	}

	private static boolean editDesktop(File file) {

		logOut("Trying to use Desktop.getDesktop().edit() with " + file);
		try {
			if (!Desktop.isDesktopSupported()) {
				logErr("Platform is not supported.");
				return false;
			}

			if (!Desktop.getDesktop().isSupported(Desktop.Action.EDIT)) {
				logErr("EDIT is not supported.");
				return false;
			}

			Desktop.getDesktop().edit(file);

			return true;
		} catch (Throwable t) {
			logErr("Error using desktop edit.", t);
			return false;
		}
	}

	private static boolean runCommand(String command, String args, String file) {

		logOut("Trying to exec:\n   cmd = " + command + "\n   args = " + args
				+ "\n   %s = " + file);

		String[] parts = prepareCommand(command, args, file);

		try {
			Process p = Runtime.getRuntime().exec(parts);
			if (p == null)
				return false;

			try {
				int retval = p.exitValue();
				if (retval == 0) {
					logErr("Process ended immediately.");
					return false;
				} else {
					logErr("Process crashed.");
					return false;
				}
			} catch (IllegalThreadStateException itse) {
				logErr("Process is running.");
				return true;
			}
		} catch (IOException e) {
			logErr("Error running command.", e);
			return false;
		}
	}

	private static String[] prepareCommand(String command, String args,
			String file) {

		List<String> parts = new ArrayList<String>();
		parts.add(command);

		if (args != null) {
			for (String s : args.split(" ")) {
				s = String.format(s, file); // put in the filename thing

				parts.add(s.trim());
			}
		}

		return parts.toArray(new String[parts.size()]);
	}

	private static void logErr(String msg, Throwable t) {
		logger.error(DesktopApi.class.getName() + ": " + msg, t);
	}

	private static void logErr(String msg) {
		logger.error(DesktopApi.class.getName() + ": " + msg);
	}

	private static void logOut(String msg) {
		logger.info(DesktopApi.class.getName() + ": " + msg);
	}

	public static void copyToClipboard(String value,
			boolean disableClipboardMonitor) {
		ClipboardHandler.setIgnoreNextChange(disableClipboardMonitor);
		Toolkit.getDefaultToolkit().getSystemClipboard()
				.setContents(new StringSelection(value), null);
	}

	public static void downloadFile(URL source, File dest) throws IOException {
		dest.createNewFile();
		if (!dest.canWrite()) {
			throw new IOException("Destination file not writable!");
		} else {
			FileOutputStream fos = null;
			try {

				ReadableByteChannel rbc = Channels.newChannel(source
						.openStream());
				fos = new FileOutputStream(dest);
				fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			} catch (IOException e) {
				throw e;
			} finally {
				if (fos != null) {
					fos.flush();
					fos.close();
				}
			}
		}
	}

	public static File downloadTemporaryFile(URL source) throws IOException {
		File tmpfile = File.createTempFile(
				"panbox-tmp-" + String.valueOf(System.currentTimeMillis()), "");
		downloadFile(source, tmpfile);
		return tmpfile;
	}

	public static boolean isMultiuserModeDisabled() {
		String PANBOX_REGISTRY = "SOFTWARE\\Panbox.org\\Panbox";
		try {
			logger.info("PanboxWindowsService : Checking for VFS multiuser mode entry in registry.");
			String read = WinRegistry.readString(
					WinRegistry.HKEY_LOCAL_MACHINE, PANBOX_REGISTRY,
					"multiuserMode");
			logger.info("PanboxWindowsService : VFS multiuser mode entry existed: "
					+ read);
			if (read == null) {
				throw new IllegalArgumentException();
			}
			return !Boolean.valueOf(read);
		} catch (IllegalArgumentException | IllegalAccessException
				| InvocationTargetException e) {
			// invalid or non-existing value. Will disable!
			logger.info("PanboxWindowsService : VFS multiuser mode entry was not set.");
			return false;
		}
	}
}
