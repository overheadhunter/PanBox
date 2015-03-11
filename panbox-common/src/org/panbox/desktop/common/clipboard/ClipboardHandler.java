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
package org.panbox.desktop.common.clipboard;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.Observable;

import org.apache.log4j.Logger;
import org.panbox.PanboxConstants;

public final class ClipboardHandler extends Observable implements Runnable {

	private boolean stop = false;

	private static final Logger logger = Logger.getLogger("org.panbox.common");
	
	public ClipboardHandler() {
	}

	private static boolean ignoreNext = false;

	public static synchronized void setIgnoreNextChange(boolean ignoreNextChange) {
		ClipboardHandler.ignoreNext = ignoreNextChange;
	}

	public void run() {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		String last = null;
		String current = null;
		while (!stop) {
			try {
				Transferable data = clipboard.getContents(null);

				if ((data != null)
						&& (data.isDataFlavorSupported(DataFlavor.stringFlavor))) {
					current = (String) data
							.getTransferData(DataFlavor.stringFlavor);

					if ((!current.equals(last))) {
						if (!ignoreNext) {
							if ((current.length() > PanboxConstants.PANBOX_URL_PREFIX
									.length())
									&& (current.substring(0,
											PanboxConstants.PANBOX_URL_PREFIX
													.length())
											.equalsIgnoreCase(PanboxConstants.PANBOX_URL_PREFIX))) {
								setChanged();
								notifyObservers(current);
							}
							last = current;
						} else {
							last = current;
							ignoreNext = false;
						}
					}
				}
			} catch (Exception e) {
				logger.error("ClipboardHandler: Problem on parsing clipboard data", e);
			}

			try {
				Thread.sleep(500L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void stop() {
		stop = true;
	}

	public void start() {
		if (stop == true) {
			System.err.println("ClipboardHandler already started");
			return;
		}
		Thread thread = new Thread(this);
		thread.setDaemon(true);
		thread.start();
	}
}