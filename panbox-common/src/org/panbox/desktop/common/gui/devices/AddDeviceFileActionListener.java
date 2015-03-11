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

import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.util.Arrays;
import java.util.ResourceBundle;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.panbox.Settings;
import org.panbox.core.devicemgmt.DeviceType;
import org.panbox.core.pairing.PAKCorePairingHandler.PairingType;
import org.panbox.desktop.common.PanboxClient;
import org.panbox.desktop.common.gui.PasswordEnterDialog;
import org.panbox.desktop.common.gui.PasswordEnterDialog.PermissionType;

public class AddDeviceFileActionListener implements ActionListener {

	private final JFrame clientGuiFrame;
	private final PanboxClient client;

	private static final ResourceBundle bundle = ResourceBundle.getBundle(
			"org.panbox.desktop.common.gui.Messages", Settings.getInstance()
					.getLocale());

	public AddDeviceFileActionListener(PanboxClient client,
			JFrame clientGuiFrame) {
		this.client = client;
		this.clientGuiFrame = clientGuiFrame;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JFileChooser chooser = new JFileChooser();
		int retVal = chooser.showSaveDialog(clientGuiFrame);
		if (retVal == JFileChooser.APPROVE_OPTION) {
			File file = null;
			if (chooser.getSelectedFile().getAbsolutePath().endsWith(".zip")) {
				file = chooser.getSelectedFile();
			} else {
				file = new File(chooser.getSelectedFile() + ".zip");
			}

			final JTextField deviceNameField = new JTextField(
					bundle.getString("PanboxClient.chooseDeviceName"));
			deviceNameField.addFocusListener(new FocusAdapter() {
				@Override
				public void focusGained(FocusEvent e) {
					if (deviceNameField.getText().equals(
							bundle.getString("PanboxClient.chooseDeviceName"))) {
						deviceNameField.setText("");
					}
				}
			});
			retVal = JOptionPane.showConfirmDialog(clientGuiFrame,
					deviceNameField,
					bundle.getString("PanboxClient.enterDeviceName"),
					JOptionPane.OK_CANCEL_OPTION);
			if (retVal != JOptionPane.OK_OPTION
					|| deviceNameField.getText().trim().isEmpty()
					|| deviceNameField
							.getText()
							.equals("Please choose a device name for the new device...")) {
				return;
			}

			JComboBox<DeviceType> devType = new JComboBox<>(new DeviceType[] {
					DeviceType.DESKTOP, DeviceType.MOBILE });
			devType.getModel().setSelectedItem(DeviceType.DESKTOP);

			retVal = JOptionPane.showConfirmDialog(clientGuiFrame, devType,
					bundle.getString("PanboxClient.chooseDeviceType"),
					JOptionPane.OK_CANCEL_OPTION);

			if (retVal != JOptionPane.OK_OPTION) {
				return;
			}

			JComboBox<PairingType> pairingType = new JComboBox<>(
					new PairingType[] { PairingType.MASTER, PairingType.SLAVE });
			pairingType.getModel().setSelectedItem(PairingType.SLAVE);

			// Only possible for DESKTOP devices
			if (devType.getModel().getSelectedItem() == DeviceType.DESKTOP) {
				retVal = JOptionPane.showConfirmDialog(clientGuiFrame,
						pairingType,
						bundle.getString("PanboxClient.choosePairingType"),
						JOptionPane.OK_CANCEL_OPTION);

				if (retVal != JOptionPane.OK_OPTION) {
					return;
				}
			}

			char[] password = new char[] {};
			if (pairingType.getModel().getSelectedItem() == PairingType.SLAVE) {
				do {
					password = PasswordEnterDialog
							.invoke(PermissionType.DEVICE_FILESLAVE);

					if (password.length < 8) {
						JOptionPane.showMessageDialog(null,
								bundle.getString("PasswordPolicyNotMatched"),
								bundle.getString("InvalidInput"),
								JOptionPane.ERROR_MESSAGE);
					}
				} while (password.length < 8);
			} else {
				password = PasswordEnterDialog.invoke(PermissionType.DEVICE);
			}

			client.showTrayMessage(bundle.getString("PleaseWait"),
					bundle.getString("client.pairing.file.pleaseWait"),
					MessageType.INFO);
			client.storePairingFile(file, deviceNameField.getText(), password,
					(PairingType) pairingType.getModel().getSelectedItem(),
					(DeviceType) devType.getModel().getSelectedItem());
			client.showTrayMessage(
					bundle.getString("tray.addDeviceToShare.finishTitle"),
					bundle.getString("client.pairing.file.finished"),
					MessageType.INFO);
			Arrays.fill(password, (char) 0);
		}
	}
}
