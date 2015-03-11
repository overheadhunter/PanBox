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
package org.panbox.desktop.common.urihandler;

import java.io.File;
import java.io.FileFilter;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.ResourceBundle;

import javax.swing.JOptionPane;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.panbox.PanboxConstants;
import org.panbox.Settings;
import org.panbox.core.csp.CSPAdapterFactory;
import org.panbox.core.csp.ICSPClientIntegration;
import org.panbox.core.csp.StorageBackendType;
import org.panbox.desktop.common.gui.AddShareDialog;
import org.panbox.desktop.common.gui.OperationAbortedException;
import org.panbox.desktop.common.gui.PanboxClientGUI;
import org.panbox.desktop.common.sharemgmt.ShareManagerImpl;

/**
 * @author palige
 * 
 *         handler class for process share inviation links
 * 
 */
public class PanboxURICmdShareInvitation extends PanboxURICmd {

	private final static String NAME = "ShareInvitation";

	public PanboxURICmdShareInvitation(byte[]... params) throws Exception {
		super(params);
		if (params.length == 2) {
			this.sid = new String(params[0], PanboxConstants.STANDARD_CHARSET);
			this.stype = new String(params[1], PanboxConstants.STANDARD_CHARSET);
		} else {
			throw new Exception("Invalid params " + Arrays.toString(params));
		}
	}

	private String sid, stype;

	private final static Logger logger = Logger
			.getLogger(PanboxURICmdShareInvitation.class);

	private static ResourceBundle bundle = ResourceBundle.getBundle(
			"org.panbox.desktop.common.gui.Messages", Settings.getInstance()
					.getLocale());

	public static URI getPanboxLink(String sid, String stype) {
		StringBuilder b = new StringBuilder();
		b.append(NAME);
		b.append("?");

		b.append(Base64.encodeBase64URLSafeString(sid.getBytes(Charset
				.forName(PanboxConstants.STANDARD_CHARSET))));
		b.append(":");
		b.append(Base64.encodeBase64URLSafeString(stype.getBytes(Charset
				.forName(PanboxConstants.STANDARD_CHARSET))));

		return URI.create(PanboxConstants.PANBOX_URL_PREFIX + b.toString());
	}

	private final class UUIDFilter implements FileFilter {

		String uuid;

		public UUIDFilter(String uuid) {
			this.uuid = uuid;
		}

		@Override
		public boolean accept(File pathname) {
			return (pathname.isDirectory() && (new File(pathname,
					PanboxConstants.PANBOX_SHARE_METADATA_DIRECTORY
							+ File.separator
							+ PanboxConstants.PANBOX_SHARE_UUID_PREFIX
							+ this.uuid).exists()));
		}
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void execute() throws Exception {

		logger.info("Trying to process share invitaion with params shareid="
				+ sid + ", share type=" + stype);

		// check if there exists a share with the given sid for the
		// given share type
		try {
			StorageBackendType backendType = StorageBackendType.valueOf(stype);
			CSPAdapterFactory fac = CSPAdapterFactory.getInstance(backendType);
			if (fac != null) {
				ICSPClientIntegration client = fac.getClientAdapter();
				File syncDir = client.getCurrentSyncDir();
				if ((syncDir != null) && (syncDir.exists())
						&& (syncDir.canRead())) {
					// search for directory with given sid
					File[] subdirs = syncDir.listFiles(new UUIDFilter(sid));
					if ((subdirs == null) || (subdirs.length == 0)) {
						logger.warn("No share detected for UUID " + sid);
						JOptionPane
								.showMessageDialog(
										null,
										bundle.getString("PanboxURICmdShareInvitation.shareNotFoundMessage"),
										bundle.getString("PanboxURICmdShareInvitation.shareNotFoundMessageTitle"),
										JOptionPane.WARNING_MESSAGE);
					} else if (subdirs.length > 1) {
						logger.error("Multiple shares detected for UUID " + sid);
					} else {
						logger.info("Share detected for UUID " + sid
								+ " at location "
								+ subdirs[0].getAbsolutePath()
								+ ". Starting AddShareDialog ...");

						if (!ShareManagerImpl.getInstance().sharePathAvailable(
								subdirs[0].getAbsolutePath())) {
							JOptionPane
									.showMessageDialog(
											null,
											bundle.getString("PanboxURICmdShareInvitation.shareAlreadyImported"));
						} else {
							// if this succeeded, try opening import
							// dialog
							PanboxClientGUI g = PanboxHTTPServer
									.getPanboxClient().getMainWindow();
							if (!g.isVisible())
								g.setVisible(true);

							AddShareDialog d = new AddShareDialog(g,
									backendType, subdirs[0]);
							d.setLocationRelativeTo(g);
							d.setVisible(true);

							try {
								g.addShare(d.getResult());
							} catch (OperationAbortedException e) {
								// do nothing if operation was aborted
							}
						}
					}
				} else {
					logger.error("Unable to access current synchronization directory for share type "
							+ stype);
				}
			} else {
				logger.error("Unable to init adapters for share type " + stype);
			}
		} catch (IllegalArgumentException e) {
			logger.error("Received inviation with unknown share type " + stype,
					e);
			throw new Exception("Unknown share type " + stype);
		}
	}
}
