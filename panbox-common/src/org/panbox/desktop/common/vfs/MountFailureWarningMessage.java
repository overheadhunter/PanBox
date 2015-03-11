package org.panbox.desktop.common.vfs;

import java.util.ResourceBundle;

import javax.swing.JOptionPane;

import org.panbox.Settings;

public class MountFailureWarningMessage implements MountFailureHandler {

	protected static final ResourceBundle bundle = ResourceBundle.getBundle(
			"org.panbox.desktop.common.gui.Messages", Settings.getInstance()
					.getLocale());

	@Override
	public void exec() {
		JOptionPane.showMessageDialog(null,
				bundle.getString("client.startup.mountFailedWarning"),
				bundle.getString("client.warn"), JOptionPane.WARNING_MESSAGE);
	}
}
