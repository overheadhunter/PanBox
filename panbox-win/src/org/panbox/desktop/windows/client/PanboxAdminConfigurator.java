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
package org.panbox.desktop.windows.client;

import java.lang.reflect.InvocationTargetException;
import java.util.ResourceBundle;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.panbox.Settings;
import org.panbox.WinRegistry;
import org.panbox.desktop.windows.client.gui.PanboxAdminConfiguratorGUI;

public class PanboxAdminConfigurator {

	public static final String PANBOX_LOCATION = "SOFTWARE\\Panbox.org\\Panbox";

	private static final ResourceBundle bundle = ResourceBundle.getBundle(
			"org.panbox.desktop.common.gui.Messages", Settings.getInstance()
					.getLocale());

	public static void main(String[] args) {
		setGuiLookAndFeel();

		checkAdmin();

		PanboxAdminConfiguratorGUI gui = new PanboxAdminConfiguratorGUI();
		gui.setVisible(true);
	}

	private static void checkAdmin() {
		try {
			WinRegistry.writeStringValue(WinRegistry.HKEY_LOCAL_MACHINE,
					PANBOX_LOCATION, "test", "test");
			WinRegistry.deleteValue(WinRegistry.HKEY_LOCAL_MACHINE,
					PANBOX_LOCATION, "test");
		} catch (IllegalArgumentException | IllegalAccessException
				| InvocationTargetException e) {
			JOptionPane
					.showMessageDialog(
							null,
							bundle.getString("adminConfigTool.errorNoAdmin.message"),
							bundle.getString("adminConfigTool.errorNoAdmin.title"),
							JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
	}

	private static void setGuiLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException
				| IllegalAccessException | UnsupportedLookAndFeelException e) {
			// ignore this!
		}
	}
}
