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
package org.panbox.desktop.common.pairing.callables;

import java.net.BindException;
import java.net.InetAddress;
import java.nio.channels.ClosedByInterruptException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;
import org.panbox.Settings;
import org.panbox.core.identitymgmt.AbstractIdentity;
import org.panbox.core.pairing.PairingInformation;
import org.panbox.core.pairing.PairingNotificationReceiver;
import org.panbox.core.pairing.PAKCorePairingHandler.PairingType;
import org.panbox.core.pairing.PairingNotificationReceiver.PairingResult;
import org.panbox.core.pairing.network.NetworkPairingInformation;
import org.panbox.core.pairing.network.PAKNetworkPairingHandler;
import org.panbox.desktop.common.PanboxClient;
import org.panbox.desktop.common.devicemgmt.DeviceManagerException;
import org.panbox.desktop.common.gui.PleaseWaitDialog;

public class NetworkPairingCallable implements Callable<Void> {

	private final PairingType type;
	private final NetworkPairingInformation info;
	private final String password;
	private final PairingNotificationReceiver receiver;
	private final char[] keyPassword;
	private final PanboxClient client;

	public NetworkPairingCallable(PairingType type, PairingInformation info,
			String password, PairingNotificationReceiver receiver,
			char[] keyPassword, PanboxClient client) {
		this.type = type;
		this.info = (NetworkPairingInformation) info;
		this.password = password;
		this.receiver = receiver;
		this.keyPassword = ArrayUtils.clone(keyPassword);
		this.client = client;
	}

	protected static final Logger logger = Logger.getLogger("org.panbox");

	protected static final ResourceBundle bundle = ResourceBundle.getBundle(
			"org.panbox.desktop.common.gui.Messages", Settings.getInstance()
					.getLocale());

	@Override
	public Void call() throws Exception {
		PAKNetworkPairingHandler handler = null;
		try {
			PleaseWaitDialog dialog = new PleaseWaitDialog(null,
					bundle.getString("client.pairing.pleasewait.message"));
			dialog.setLocationRelativeTo(null);

			logger.debug("NetworkPairingCallable : Pairing started protocol.");

			String deviceName = info.getDeviceName();

			AbstractIdentity myId = client.getIdentity();

			switch (type) {
			case MASTER:
				logger.debug("NetworkPairingCallable : Pairing started MASTER protocol.");
				try {
					PrivateKey ownerKeyEnc = myId.getPrivateKeyEnc(keyPassword);
					PrivateKey ownerKeySign = myId
							.getPrivateKeySign(keyPassword);
					handler = new PAKNetworkPairingHandler(
							(InetAddress) info.getPairingAddress(), password,
							myId.getEmail(), myId.getFirstName(),
							myId.getName(), deviceName, keyPassword,
							ownerKeyEnc, ownerKeySign,
							client.getDevicePairingMap(),
							client.getContactsPairingList());
				} catch (UnrecoverableKeyException e) {
					logger.debug("NetworkPairingCallable : Wrong password!");
					receiver.inform(PairingResult.QUITEFAILED);
					JOptionPane
							.showMessageDialog(
									null,
									bundle.getString("BluetoothPairingCallable.wrongPassword"),
									bundle.getString("BluetoothPairingCallable.errorWhilePairing"),
									JOptionPane.ERROR_MESSAGE);
					return null;
				}
				break;
			case SLAVE:
				logger.debug("NetworkPairingCallable : Pairing started SLAVE protocol.");
				X509Certificate ownerCertEnc = myId.getCertEnc();
				X509Certificate ownerCertSign = myId.getCertSign();
				handler = new PAKNetworkPairingHandler(
						(InetAddress) info.getPairingAddress(), password,
						myId.getEmail(), myId.getFirstName(), myId.getName(),
						deviceName, ownerCertEnc, ownerCertSign,
						client.getDevicePairingMap(),
						client.getContactsPairingList());
				break;
			}
			logger.debug("NetworkPairingCallable : Pairing waiting for socket connect.");
			handler.acceptConnection();
			dialog.setVisible(true);
			logger.debug("NetworkPairingCallable : Pairing socket connected!");
			handler.runProtocol();
			try {
				handler.closeConnection();
			} catch (Exception e) {
				logger.debug("NetworkPairingCallable : Connection was already closed. Thats OK.");
			}
			logger.debug("NetworkPairingCallable : Will add device to device list.");
			try {
				client.deviceManager.addDevice(deviceName,
						handler.getDevCert(), handler.getDevType());
				client.refreshDeviceListModel();
			} catch (DeviceManagerException ex) {
				logger.error(
						"NetworkPairingCallable : Could not add device to device list.",
						ex);
			}
			dialog.dispose();
			logger.debug("NetworkPairingCallable : Pairing ended after protocol successfully.");
			handler = null;
			System.gc();
			receiver.inform(PairingResult.SUCCESS);
		} catch (ClosedByInterruptException ex) {
			logger.debug("NetworkPairingCallable : Pairing has been canceled.");
		} catch (BindException ex) {
			logger.debug("NetworkPairingCallable : Interface is already in use!");
			receiver.inform(PairingResult.QUITEFAILED);
			JOptionPane
					.showMessageDialog(
							null,
							bundle.getString("NetworkPairingCallable.interfaceAlreadyInUse"),
							bundle.getString("BluetoothPairingCallable.errorWhilePairing"),
							JOptionPane.ERROR_MESSAGE);
		} catch (Exception ex) {
			logger.debug("NetworkPairingCallable : Pairing failed.", ex);
			receiver.inform(PairingResult.FAILED);
		} finally {
			try {
				if (handler != null) {
					handler.closeConnection();
				}
			} catch (Exception e) {
				logger.debug("NetworkPairingCallable : Connection was already closed. Thats OK.");
			}
		}
		return null;
	}

}
