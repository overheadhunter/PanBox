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

import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.panbox.PanboxConstants;
import org.panbox.Settings;
import org.panbox.desktop.common.urihandler.PanboxURICmd;

public class ClipboardObserver implements Observer {

	private final static Logger logger = Logger
			.getLogger(ClipboardObserver.class);

	private static ResourceBundle bundle = ResourceBundle.getBundle(
			"org.panbox.desktop.common.gui.Messages", Settings.getInstance()
					.getLocale());

	@Override
	public void update(Observable o, Object arg) {
		if (arg instanceof String) {
			String tmp = (String) arg;
			if (tmp.startsWith(PanboxConstants.PANBOX_URL_PREFIX)) {
				try {
					String cmdstring = tmp
							.substring(PanboxConstants.PANBOX_URL_PREFIX
									.length());
					PanboxURICmd cmd = PanboxURICmd.getCommandHander(cmdstring);

					int ret = JOptionPane
							.showConfirmDialog(
									null,
									bundle.getString("ClipboardObserver.linkDetectedMessage"),
									bundle.getString("ClipboardObserver.linkDetectedTitle"),
									JOptionPane.YES_NO_OPTION);
					if (ret == JOptionPane.YES_OPTION) {
						logger.info("Found command " + cmd.getName()
								+ ", starting execution...");
						cmd.execute();
						logger.info("Execution of Command " + cmd.getName()
								+ " finished.");
					}
				} catch (Exception e) {
					logger.error("Command lookup/execution failed.", e);
				}
			}
		}
	}
}
