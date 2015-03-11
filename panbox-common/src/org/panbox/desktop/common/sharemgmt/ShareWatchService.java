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
package org.panbox.desktop.common.sharemgmt;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.panbox.PanboxConstants;
import org.panbox.core.Utils;
import org.panbox.core.keymgmt.Volume;
import org.panbox.desktop.common.PanboxClient;
import org.panbox.desktop.common.gui.shares.PanboxShare;

/**
 * @author palige
 * 
 *         Class implements a NIO {@link WatchService} looking for changes in
 *         the backend directories of the current list of configured shares,
 *         allowing for notification of the user in case of deletion events that
 *         have been triggered outside of Panbox.
 */
public class ShareWatchService {

	private final static Logger logger = Logger
			.getLogger(ShareWatchService.class);

	private WatchService watchService;
	private PanboxClient client;
	private Thread watchServiceThread;
	private static String devicelistIdentifier;

	private final static Hashtable<WatchKey, PanboxShare> watchedSharePathList = new Hashtable<WatchKey, PanboxShare>();

	public ShareWatchService(PanboxClient client) throws IOException {
		this.client = client;
		devicelistIdentifier = Utils.getCertFingerprint(
				client.getIdentity().getCertSign()).toLowerCase();
		this.watchService = FileSystems.getDefault().newWatchService();
		this.watchServiceThread = new Thread(new WatchServiceImpl());
	}

	public void start() {
		this.watchServiceThread.start();
	}

	public synchronized void registerShare(PanboxShare share) {
		// per default, watch for deletion of .panbox matadata folder
		try {
			File shareToWatch = new File(share.getPath());
			if (shareToWatch.exists()) {
				Path p = shareToWatch.toPath();
				if (!watchedSharePathList.containsValue(share)) {
					logger.info("Registering path " + p
							+ " to ShareWatchService");
					WatchKey key = p.register(watchService,
							StandardWatchEventKinds.ENTRY_DELETE);
					watchedSharePathList.put(key, share);

					// register .panbox subdirectory, if available
					File metadataDir = new File(shareToWatch,
							PanboxConstants.PANBOX_SHARE_METADATA_DIRECTORY);
					if (metadataDir.exists()) {
						p = metadataDir.toPath();
						logger.info("Registering metadata path " + p
								+ " to ShareWatchService");
						key = p.register(watchService,
								StandardWatchEventKinds.ENTRY_MODIFY,
								StandardWatchEventKinds.ENTRY_CREATE);
						watchedSharePathList.put(key, share);
					}
				} else {
					logger.warn("Path " + p
							+ " already seems to have been registered!");
					;
				}
			}
		} catch (IOException e) {
			logger.error("Error registering share " + share.getName(), e);
		}
	}

	public synchronized void removeShare(PanboxShare share) {
		PanboxShare tmp;
		ArrayList<WatchKey> tmplist = new ArrayList<WatchKey>();
		Iterator<Map.Entry<WatchKey, PanboxShare>> it = watchedSharePathList
				.entrySet().iterator();

		// only collect values to remove in first iteration to omit
		// concurrentmodificationexception
		while (it.hasNext()) {
			Map.Entry<WatchKey, PanboxShare> entry = (Map.Entry<WatchKey, PanboxShare>) it
					.next();
			if (entry.getValue().equals(share)) {
				tmplist.add(entry.getKey());
			}
		}

		for (int i = 0; i < tmplist.size(); i++) {
			if ((tmp = watchedSharePathList.remove(tmplist.get(i))) == null) {
				logger.error("Error deleting path from list of watched paths: "
						+ share.getPath());
			} else {
				((WatchKey) tmplist.get(i)).cancel();
				logger.info("Removed path " + tmp.getPath()
						+ " from ShareWatchService");
			}
		}
	}

	private class WatchServiceImpl implements Runnable {
		@Override
		public void run() {

			try {
				WatchKey key = watchService.take();
				while (key != null) {
					for (WatchEvent<?> event : key.pollEvents()) {
						logger.info("Received " + event.kind()
								+ " event for file: " + event.context());
						if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
							continue;
						} else {
							if ((event.kind() == StandardWatchEventKinds.ENTRY_DELETE)
									&& (event.context() instanceof Path)) {
								Path path = (Path) event.context();
								if (path.toString()
										.equals(PanboxConstants.PANBOX_SHARE_METADATA_DIRECTORY)) {
									// panbox share metadata dir was deleted ->
									// trigger share removal
									try {
										PanboxShare share = watchedSharePathList
												.get(key);
										if (share != null) {
											client.removeShare(share);
											key.cancel();
										}
									} catch (Exception e) {
										logger.error(
												"Removing share from DB failed!",
												e);
									}
								}
							} else if ((event.kind() == StandardWatchEventKinds.ENTRY_MODIFY)
									&& (event.context() instanceof Path)) {
								Path path = (Path) event.context();
								if (path.toString().equalsIgnoreCase(
										Volume.SPL_FILE)
										|| path.toString().equalsIgnoreCase(
												devicelistIdentifier)) {
									logger.info("Detected modification in share metadata file "
											+ path.toString()
											+ ". Initiating metadata refresh");
									try {
										PanboxShare share = watchedSharePathList
												.get(key);
										if (share != null) {
											PanboxShare newShare = client
													.reloadShare(share);
											removeShare(share);
											registerShare(newShare);
										}
									} catch (Exception e) {
										logger.error("Reloading share failed!",
												e);
									}
								}
							} else if ((event.kind() == StandardWatchEventKinds.ENTRY_CREATE)
									&& (event.context() instanceof Path)) {
								Path path = (Path) event.context();
								String chk = path.toString().toLowerCase();
								if (chk.startsWith(devicelistIdentifier)
										&& (chk.length() > (devicelistIdentifier
												.length() + ".db".length()))
										&& !chk.contains("journal")) {
									logger.info("Detected creation of potential conflicting copy in in device list file. Name: "
											+ path.toString()
											+ ". Initiating metadata refresh");
									try {
										PanboxShare share = watchedSharePathList
												.get(key);
										if (share != null) {
											client.conflictNotification(share,
													chk);
										}
									} catch (Exception e) {
										logger.error("Reloading share failed!",
												e);
									}
								}
							}
						}
					}
					key.reset();
					key = watchService.take();
				}
			} catch (InterruptedException e) {
				logger.warn("ShareWatchService interrupted.", e);
			}
		}
	}
}
