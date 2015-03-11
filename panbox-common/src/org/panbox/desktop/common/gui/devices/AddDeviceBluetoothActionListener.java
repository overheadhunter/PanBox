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
package org.panbox.desktop.common.gui.devices;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

import javax.bluetooth.BluetoothStateException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.panbox.Settings;
import org.panbox.core.pairing.bluetooth.BluetoothPairingInformation;
import org.panbox.desktop.common.PanboxClient;
import org.panbox.desktop.common.gui.PairNewDeviceDialog;

public class AddDeviceBluetoothActionListener implements ActionListener {

	protected static final Logger logger = Logger.getLogger("org.panbox");

	private final JFrame clientGuiFrame;
	private final PanboxClient client;
	protected static final ResourceBundle bundle = ResourceBundle.getBundle(
			"org.panbox.desktop.common.gui.Messages", Settings.getInstance()
					.getLocale());

	public AddDeviceBluetoothActionListener(PanboxClient client,
			JFrame clientGuiFrame) {
		this.client = client;
		this.clientGuiFrame = clientGuiFrame;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			BluetoothPairingInformation pInfo = client
					.initDevicePairingBluetooth();
			if (pInfo != null) {
				new PairNewDeviceDialog(clientGuiFrame, client, pInfo);
			} else {
				logger.info("AddDeviceBluetoothActionListener : Bluetooth device pairing aborted!");
			}
		} catch (BluetoothStateException | InterruptedException ex) {
			if (ex.getMessage().contains("Device is not available") || ex.getMessage().contains("BluetoothStack not detected")) {
				JOptionPane
						.showMessageDialog(
								clientGuiFrame,
								bundle.getString("AddDeviceBluetoothActionListener.noDevice.Message"),
								bundle.getString("error"),
								JOptionPane.ERROR_MESSAGE);
			}
			logger.error(
					"AddDeviceBluetoothActionListener : Bluetooth device exited with exception: ",
					ex);
		}
	}

}
