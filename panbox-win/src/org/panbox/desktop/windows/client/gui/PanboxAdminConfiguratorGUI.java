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
package org.panbox.desktop.windows.client.gui;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;

import org.panbox.WinRegistry;
import org.panbox.desktop.windows.client.PanboxAdminConfigurator;

/**
 *
 * @author Clemens A. Schulz
 */
public class PanboxAdminConfiguratorGUI extends javax.swing.JFrame {

	private static final long serialVersionUID = 4759456681114955415L;

	private javax.swing.JButton abortButtonPressed;
	private javax.swing.JPanel buttonPanel;
	private javax.swing.JCheckBox debugModeBox;
	private javax.swing.JComboBox<Character> driveLettersBox;
	private javax.swing.JButton okButtonPressed;
	private javax.swing.JLabel panboxDriveLetter;
	private javax.swing.JLabel panboxVFSloggin;
	private javax.swing.JSeparator seperator;

	/**
	 * Creates new form PanboxAdminConfiguratorGUI
	 */
	public PanboxAdminConfiguratorGUI() {
		initComponents();
		initValues();
	}

	private void initComponents() {

		panboxDriveLetter = new javax.swing.JLabel();
		driveLettersBox = new javax.swing.JComboBox<Character>();
		panboxVFSloggin = new javax.swing.JLabel();
		debugModeBox = new javax.swing.JCheckBox();
		buttonPanel = new javax.swing.JPanel();
		okButtonPressed = new javax.swing.JButton();
		abortButtonPressed = new javax.swing.JButton();
		seperator = new javax.swing.JSeparator();

		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		setTitle("Panbox VFS Configuration Utility");

		panboxDriveLetter.setText("Drive Letter:");

		driveLettersBox.setModel(new DefaultComboBoxModel<>(
				getFreeDriveLetters()));

		panboxVFSloggin.setText("VFS Logging:");

		debugModeBox.setText("Debug Logging");

		okButtonPressed.setText("OK");
		okButtonPressed.setMaximumSize(new java.awt.Dimension(100, 23));
		okButtonPressed.setMinimumSize(new java.awt.Dimension(100, 23));
		okButtonPressed.setPreferredSize(new java.awt.Dimension(100, 23));
		okButtonPressed.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				okButtonPressedActionPerformed(evt);
			}
		});
		buttonPanel.add(okButtonPressed);

		abortButtonPressed.setText("Abort");
		abortButtonPressed.setMaximumSize(new java.awt.Dimension(100, 23));
		abortButtonPressed.setMinimumSize(new java.awt.Dimension(100, 23));
		abortButtonPressed.setPreferredSize(new java.awt.Dimension(100, 23));
		abortButtonPressed
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						abortButtonPressedActionPerformed(evt);
					}
				});
		buttonPanel.add(abortButtonPressed);

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(
				getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(layout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(
						layout.createSequentialGroup()
								.addContainerGap()
								.addGroup(
										layout.createParallelGroup(
												javax.swing.GroupLayout.Alignment.LEADING)
												.addComponent(seperator)
												.addComponent(
														buttonPanel,
														javax.swing.GroupLayout.Alignment.TRAILING,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														Short.MAX_VALUE)
												.addGroup(
														layout.createSequentialGroup()
																.addGroup(
																		layout.createParallelGroup(
																				javax.swing.GroupLayout.Alignment.LEADING)
																				.addComponent(
																						panboxDriveLetter,
																						javax.swing.GroupLayout.PREFERRED_SIZE,
																						150,
																						javax.swing.GroupLayout.PREFERRED_SIZE)
																				.addComponent(
																						panboxVFSloggin))
																.addPreferredGap(
																		javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																.addGroup(
																		layout.createParallelGroup(
																				javax.swing.GroupLayout.Alignment.LEADING)
																				.addComponent(
																						debugModeBox)
																				.addComponent(
																						driveLettersBox,
																						javax.swing.GroupLayout.PREFERRED_SIZE,
																						javax.swing.GroupLayout.DEFAULT_SIZE,
																						javax.swing.GroupLayout.PREFERRED_SIZE))
																.addGap(0,
																		0,
																		Short.MAX_VALUE)))
								.addContainerGap()));
		layout.setVerticalGroup(layout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(
						layout.createSequentialGroup()
								.addContainerGap()
								.addGroup(
										layout.createParallelGroup(
												javax.swing.GroupLayout.Alignment.BASELINE)
												.addComponent(panboxDriveLetter)
												.addComponent(
														driveLettersBox,
														javax.swing.GroupLayout.PREFERRED_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.PREFERRED_SIZE))
								.addPreferredGap(
										javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addGroup(
										layout.createParallelGroup(
												javax.swing.GroupLayout.Alignment.LEADING)
												.addComponent(panboxVFSloggin)
												.addComponent(debugModeBox))
								.addPreferredGap(
										javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
								.addComponent(seperator,
										javax.swing.GroupLayout.PREFERRED_SIZE,
										10,
										javax.swing.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(
										javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(buttonPanel,
										javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE,
										javax.swing.GroupLayout.PREFERRED_SIZE)
								.addContainerGap(
										javax.swing.GroupLayout.DEFAULT_SIZE,
										Short.MAX_VALUE)));

		pack();
	}

	private void okButtonPressedActionPerformed(java.awt.event.ActionEvent evt) {
		writeValues();
		dispose();
	}

	private void abortButtonPressedActionPerformed(
			java.awt.event.ActionEvent evt) {
		dispose();
	}

	private void initValues() {
		setDriveLetter('P');
		setDebugMode(false);

		try {
			String mountPoint = WinRegistry.readString(
					WinRegistry.HKEY_LOCAL_MACHINE,
					PanboxAdminConfigurator.PANBOX_LOCATION, "MountPoint");
			if (mountPoint != null && mountPoint.length() == 1) {
				setDriveLetter(mountPoint.charAt(0));
			}
			boolean debugMode = Boolean.valueOf(WinRegistry.readString(
					WinRegistry.HKEY_LOCAL_MACHINE,
					PanboxAdminConfigurator.PANBOX_LOCATION, "debugMode"));
			setDebugMode(debugMode);
		} catch (IllegalArgumentException | IllegalAccessException
				| InvocationTargetException e) {
			// ignore!
		}
	}

	private void writeValues() {
		String driveLetter = Character.toString((char) driveLettersBox
				.getSelectedItem());
		boolean debugModeOn = debugModeBox.isSelected();

		try {
			WinRegistry.writeStringValue(WinRegistry.HKEY_LOCAL_MACHINE,
					PanboxAdminConfigurator.PANBOX_LOCATION, "MountPoint",
					driveLetter);
			WinRegistry.writeStringValue(WinRegistry.HKEY_LOCAL_MACHINE,
					PanboxAdminConfigurator.PANBOX_LOCATION, "debugMode",
					Boolean.toString(debugModeOn));
		} catch (IllegalArgumentException | IllegalAccessException
				| InvocationTargetException e) {
			JOptionPane
					.showMessageDialog(
							this,
							"Failed to write Panbox configuration. Is Panbox still running?",
							"Error writing config", JOptionPane.ERROR_MESSAGE);
		}
	}

	private Character[] getFreeDriveLetters() {
		ArrayList<Character> letters = new ArrayList<>();

		Runtime rt = Runtime.getRuntime();

		for (char letter : "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()) {
			try {
				Process p = rt.exec("cmd /c IF exist " + letter
						+ ":\\NUL ( exit /b 1 ) ELSE ( exit /b 0 )");
				p.waitFor();
				if (p.exitValue() == 0) {
					letters.add(letter);
				} else {
					System.out
							.println("PanboxAdminConfiguratorGUI : getFreeDriveLetters : "
									+ letter + " seems to be in use already!");
				}
			} catch (IOException | InterruptedException ex) {
				// ignore entry!
			}
		}

		return letters.toArray(new Character[] {});
	}

	public void setDriveLetter(char driveLetter) {
		driveLettersBox.setSelectedItem(driveLetter);
	}

	public void setDebugMode(boolean debugModeOn) {
		debugModeBox.setSelected(debugModeOn);
	}
}
