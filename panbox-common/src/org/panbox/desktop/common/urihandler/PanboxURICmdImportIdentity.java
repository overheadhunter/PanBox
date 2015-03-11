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
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.ResourceBundle;

import javax.swing.JOptionPane;

import org.apache.commons.codec.binary.Base64;
import org.panbox.PanboxConstants;
import org.panbox.Settings;
import org.panbox.core.crypto.CryptCore;
import org.panbox.desktop.common.gui.ImportIdentitiesWoPINDialog;
import org.panbox.desktop.common.gui.PanboxClientGUI;
import org.panbox.desktop.common.gui.PleaseWaitDialog;
import org.panbox.desktop.common.utils.DesktopApi;

/**
 * @author palige
 * 
 *         handler class for processing links to panbox identities, which have
 *         been uploaded to a storage service
 */
public class PanboxURICmdImportIdentity extends PanboxURICmd {

	private final static String NAME = "ImportIdentity";

	public PanboxURICmdImportIdentity(byte[]... params) throws Exception {
		super(params);
		if (params.length == 1) {
			this.url = new String(params[0], PanboxConstants.STANDARD_CHARSET);
                } else if (params.length == 2) {
			this.url = new String(params[0], PanboxConstants.STANDARD_CHARSET);
			this.chksum = params[1];
		} else {
			throw new Exception("Invalid params " + Arrays.toString(params));
		}
	}

	private static final ResourceBundle bundle = ResourceBundle.getBundle(
			"org.panbox.desktop.common.gui.Messages", Settings.getInstance()
					.getLocale());

	private String url;
	private byte[] chksum;

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void execute() throws Exception {

		// try to download file
		URL identityfile = new URL(url);
		File tmpfile = null;
		PleaseWaitDialog waitDialog = null;
		try {
			waitDialog = new PleaseWaitDialog(null,
					bundle.getString("PanboxURICmd.identityDownload.info"));
			waitDialog.setLocationRelativeTo(null);
			waitDialog.setVisible(true);
			tmpfile = DesktopApi.downloadTemporaryFile(identityfile);
                        if(chksum != null) { //otherwise we don't use export PIN!
                            if (!CryptCore.checkChecksum(tmpfile, chksum)) {
                                    JOptionPane
                                                    .showMessageDialog(
                                                                    null,
                                                                    bundle.getString("PanboxURICmd.identityDownloadChksumFailed.message"),
                                                                    bundle.getString("error"),
                                                                    JOptionPane.ERROR_MESSAGE);
                                    return;
                            }
                        }
		} catch (Exception e) {
			throw e;
		} finally {
			if (waitDialog != null)
				waitDialog.dispose();
		}

		// if this successded, try opening import dialog
		PanboxClientGUI g = PanboxHTTPServer.getPanboxClient().getMainWindow();
		if (!g.isVisible())
			g.setVisible(true);

		          ImportIdentitiesWoPINDialog d = new ImportIdentitiesWoPINDialog(
				PanboxHTTPServer.getPanboxClient(), tmpfile);
		d.setLocationRelativeTo(g);
		d.setVisible(true);

	}

	public static URI getPanboxLink(URI uri, byte[] checksum) {
		StringBuilder b = new StringBuilder();
		b.append(NAME);
		b.append("?");
		b.append(Base64.encodeBase64URLSafeString(uri.toString().getBytes(
				Charset.forName(PanboxConstants.STANDARD_CHARSET))));
		b.append(":");
		b.append(Base64.encodeBase64URLSafeString(checksum));
		return URI.create(PanboxConstants.PANBOX_URL_PREFIX + b.toString());
	}

	public static URI getPanboxLink(URI uri) {
		StringBuilder b = new StringBuilder();
		b.append(NAME);
		b.append("?");
		b.append(Base64.encodeBase64URLSafeString(uri.toString().getBytes(
				Charset.forName(PanboxConstants.STANDARD_CHARSET))));
		return URI.create(PanboxConstants.PANBOX_URL_PREFIX + b.toString());
	}
}
