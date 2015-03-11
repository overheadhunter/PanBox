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
package org.panbox.desktop.windows.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.ConfigurationException;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.panbox.WinRegistry;
import org.panbox.core.crypto.AbstractObfuscatorFactory;
import org.panbox.core.crypto.FileObfuscatorFactory;
import org.panbox.core.crypto.randomness.SecureRandomWrapper;
import org.panbox.core.exception.ObfuscationException;
import org.panbox.core.exception.RandomDataGenerationException;
import org.panbox.core.exception.ShareMetaDataException;
import org.panbox.core.keymgmt.VolumeParams;
import org.panbox.desktop.common.gui.shares.PanboxShare;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

public class PanboxWindowsService extends UnicastRemoteObject implements
		PanboxWindowsServiceInterface {

	private static final Logger logger = Logger.getLogger("org.panbox");

	private static final Map<String, String> usernameAuthSecrets = new ConcurrentHashMap<String, String>();
	private static final Map<PanboxClientSession, PanboxServiceSession> serviceSessions = new ConcurrentHashMap<PanboxClientSession, PanboxServiceSession>();

	static {
		try {
			PatternLayout layout = new PatternLayout("%d %-5p [%t]: %m%n");

			// Logs will be logged to C:\System32\...
			RollingFileAppender rfasrv = new RollingFileAppender(layout,
					"PanboxWindowsService.log", true);
			rfasrv.setEncoding("UTF-8");
			rfasrv.activateOptions();
			logger.addAppender(rfasrv);

			ConsoleAppender casrv = new ConsoleAppender(layout);
			casrv.setEncoding("UTF-8");
			casrv.activateOptions();
			logger.addAppender(casrv);

			if (isVFSDebugModeEnabled()) {
				logger.setLevel(Level.ALL);
			} else {
				logger.setLevel(Level.INFO);
			}
		} catch (IOException ex) {
			logger.error(
					"PanboxWindowsService : Failed to add appender for logging files!",
					ex);
		}
		logger.debug("PanboxWindowsService : Class constructed");
	}

	private static final long serialVersionUID = 5129881340657647351L;

	private static PanboxWindowsService service;

	private static boolean stopThread = false;
	private static final Thread rebindThread = new Thread(new Runnable() {

		@Override
		public void run() {
			try {
				while (!stopThread) {
					if (service != null) {
						try {
							LocateRegistry.createRegistry(1099);
						} catch (RemoteException e) {
							// do nothing, error means registry already exists
						}

						try {
							Naming.rebind("//localhost/PanboxWindowsService",
									service);
						} catch (RemoteException | MalformedURLException e) {
							logger.warn("PanboxWindowsService : rebindThread : Failed to rebind service to registry.");
						}
					}
					Thread.sleep(5000);
				}
			} catch (InterruptedException e) {
				logger.debug("PanboxWindowsService : rebindThread : Interrupted. Will stop rebindThread.");
			}
		}
	});

	public PanboxWindowsService() throws RemoteException {
		super(0);
	}

	public static boolean isVFSDebugModeEnabled() {
		String PANBOX_REGISTRY = "SOFTWARE\\Panbox.org\\Panbox";
		try {
			logger.info("PanboxWindowsService : Checking for VFS debug mode entry in registry.");
			String read = WinRegistry.readString(
					WinRegistry.HKEY_LOCAL_MACHINE, PANBOX_REGISTRY,
					"debugMode");
			logger.info("PanboxWindowsService : VFS debug mode entry existed: "
					+ read);
			if (read == null) {
				throw new IllegalArgumentException();
			}
			return Boolean.valueOf(read);
		} catch (IllegalArgumentException | IllegalAccessException
				| InvocationTargetException e) {
			// invalid or non-existing value. Will disable!
			logger.info("PanboxWindowsService : VFS debug mode entry was not set.");
			return false;
		}
	}

	// ------------------------------- CLIENT-RELATED METHODS
	// -------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.panbox.desktop.windows.service.PanboxWindowsServiceInterface#startupVFS
	 * ()
	 */
	@Override
	public void startupVFS() {
		try {
			SecureRandomWrapper.getInstance();
			AbstractObfuscatorFactory.getFactory(FileObfuscatorFactory.class);
			VFSManager.getInstance().startVFS();
		} catch (ConfigurationException | IllegalArgumentException
				| IllegalAccessException | InvocationTargetException
				| ClassNotFoundException | InstantiationException
				| RandomDataGenerationException ex) {
			logger.error(
					"PanboxWindowsService : startupVFS : Caught exception from VFSManager",
					ex);
			shutdownService();
			System.exit(-1);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.panbox.desktop.windows.service.PanboxWindowsServiceInterface#shutdownVFS
	 * ()
	 */
	@Override
	public void shutdownVFS() {
		try {
			VFSManager.getInstance().stopVFS();
		} catch (ConfigurationException | IllegalArgumentException
				| IllegalAccessException | InvocationTargetException ex) {
			logger.error(
					"PanboxWindowsService : shutdownVFS : Caught exception from VFSManager",
					ex);
			shutdownService();
			System.exit(-1);
		}
	}

	// ------------------------------- SERVICE-RELATED METHODS

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.panbox.desktop.windows.service.PanboxWindowsServiceInterface#
	 * shutdownService()
	 */
	@Override
	public void shutdownService() {
		try {
			UnicastRemoteObject.unexportObject(this, true);
		} catch (NoSuchObjectException e) {
			// ignore: no valid instance was registred!
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.panbox.desktop.windows.service.PanboxWindowsServiceInterface#
	 * startupService()
	 */
	@Override
	public void startupService() {
		startupVFS();
	}

	/**
	 * startService is used by the prunsrv application as start method for
	 * running the Panbox Windows Service
	 * 
	 * @throws RemoteException
	 * @throws MalformedURLException
	 */
	public static void startService(String[] args) {
		if (!VFSManager.isRunning()) {
			try {
				logger.debug("PanboxWindowsService : startService : RMI server is starting...");

				try {
					LocateRegistry.createRegistry(1099);
					logger.debug("PanboxWindowsService : startService : java RMI registry created.");
				} catch (RemoteException e) {
					// do nothing, error means registry already exists
					logger.debug("PanboxWindowsService : startService : java RMI registry already exists.");
				}

				service = new PanboxWindowsService();

				service.startupService();

				Naming.rebind("//localhost/PanboxWindowsService", service);
				logger.info("PanboxWindowsService : startService : PeerServer bound in registry to port " + 1099);

				rebindThread.start();
			} catch (Exception ex) {
				logger.error(
						"PanboxWindowsService : startService : Exception: ", ex);
			}
		} else {
			logger.debug("PanboxWindowsService : startService : Can not start service, because VFS is already running!");
		}
	}

	/**
	 * stopService is used by the prunsrv application as stop method for
	 * shutting down the Panbox Windows Service
	 * 
	 * @throws RemoteException
	 * @throws MalformedURLException
	 */
	public static void stopService(String[] args) {
		try {
			logger.debug("PanboxWindowsService : stopService : RMI server is stopping...");

			stopThread = true;

			PanboxWindowsServiceInterface service = (PanboxWindowsServiceInterface) Naming
					.lookup("//localhost/PanboxWindowsService");

			int tries = 3;
			while (VFSManager.isRunning() && tries > 0) {
				logger.debug("PanboxWindowsService : stopService : Can not stop service, because VFS is still running! Will stop it now! Tries: "
						+ tries);
				service.shutdownVFS();
				Thread.sleep(1000);
				--tries;
			}

			if (VFSManager.isRunning()) {
				logger.error("PanboxWindowsService : stopService : Stopping VFS failed! Will not shut down service!");
				System.exit(-1);
			}

			service.shutdownService();

			Naming.unbind("//localhost/PanboxWindowsService");
			logger.debug("PanboxWindowsService : stopService : PeerServer unbound from registry");

			System.exit(0);
		} catch (Exception ex) {
			logger.error("PanboxWindowsService : stopService : Exception: ", ex);
		}
		System.exit(-1);
	}

	@Override
	public String askLogin(String username) throws RemoteException {
		String genSecret = genNewSecret();
		setAuthSecretOnUserRegistry(username, genSecret);
		usernameAuthSecrets.put(username, genSecret);
		return username;
	}

	private String genNewSecret() {
		SecureRandom random = new SecureRandom();
		return new BigInteger(130, random).toString(32);
	}

	private void setAuthSecretOnUserRegistry(String username, String genSecret) {
		String sidString = Advapi32Util.getAccountByName(username).sidString;
		Advapi32Util.registrySetStringValue(WinReg.HKEY_USERS, sidString
				+ "\\SOFTWARE\\Panbox.org\\Panbox\\session", username,
				genSecret);
	}

	@Override
	public PanboxClientSession authLogin(String username, String sharedSecret) {
		if (!sharedSecret.equals(usernameAuthSecrets.get(username))) {
			logger.warn("PanboxService : Authentication failed for user '"
					+ username + "'. Wrong secret!!!");
			usernameAuthSecrets.remove(username);
			return null;
		}

		PanboxServiceSession session = new PanboxServiceSession(username);
		PanboxClientSession cSession = new PanboxClientSession(
				new SecureRandom().nextInt());

		serviceSessions.put(cSession, session);
		usernameAuthSecrets.remove(username);

		return cSession;
	}

	@Override
	public synchronized PanboxShare createShare(PanboxClientSession session,
			VolumeParams p) throws FileNotFoundException,
			IllegalArgumentException, ShareMetaDataException, RemoteException {
		return serviceSessions.get(session).createShare(p);
	}

	@Override
	public synchronized PanboxShare loadShare(PanboxClientSession session,
			VolumeParams p) throws ShareMetaDataException, RemoteException,
			FileNotFoundException {
		return serviceSessions.get(session).loadShare(p);
	}

	@Override
	public synchronized PanboxShare acceptInviation(
			PanboxClientSession session, VolumeParams p)
			throws ShareMetaDataException, RemoteException,
			FileNotFoundException {
		return serviceSessions.get(session).acceptInviation(p);
	}

	@Override
	public synchronized PanboxShare inviteUser(PanboxClientSession session,
			VolumeParams p) throws ShareMetaDataException, RemoteException {
		return serviceSessions.get(session).inviteUser(p);
	}

	@Override
	public synchronized PanboxShare addDevice(PanboxClientSession session,
			VolumeParams p) throws IllegalArgumentException,
			ShareMetaDataException, RemoteException {
		return serviceSessions.get(session).addDevice(p);
	}

	@Override
	public synchronized PanboxShare removeDevice(PanboxClientSession session,
			VolumeParams p) throws IllegalArgumentException,
			ShareMetaDataException, UnrecoverableKeyException, RemoteException {
		return serviceSessions.get(session).removeDevice(p);
	}

	@Override
	public synchronized void removeShare(PanboxClientSession session,
			VolumeParams p) throws RemoteException {
		serviceSessions.get(session).removeShare(p);
	}

	@Override
	public synchronized String getOnlineFilename(PanboxClientSession session,
			VolumeParams p, String fileName) throws RemoteException,
			FileNotFoundException, ObfuscationException {
		return serviceSessions.get(session).getOnlineFilename(p, fileName);
	}

	@Override
	public synchronized PanboxShare reloadShareMetaData(
			PanboxClientSession session, VolumeParams p)
			throws ShareMetaDataException, RemoteException,
			FileNotFoundException {
		return serviceSessions.get(session).reloadShareMetaData(p);
	}

}
