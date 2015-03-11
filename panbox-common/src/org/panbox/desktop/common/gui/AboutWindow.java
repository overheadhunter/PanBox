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
package org.panbox.desktop.common.gui;

/**
 * Created by Dominik Spychalski on 02.12.14.
 */
import org.panbox.PanboxConstants;
import org.panbox.desktop.common.PanboxDesktopConstants;

import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;

public class AboutWindow extends javax.swing.JFrame {

	private static final long serialVersionUID = 565083917875539261L;

	private static AboutWindow instance = null;
	private Image splashimage = Toolkit.getDefaultToolkit().getImage(
			getClass().getResource("panbox_splashscreen.png"));
	private int width = 500;
	private int height = 335;
	private Timer timer;

	public static AboutWindow getInstance() {
		if (instance == null) {
			instance = new AboutWindow();
		}
		return instance;
	}

	private AboutWindow() {
		setUndecorated(true);
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

		int x = (screen.width - width) / 2;
		int y = (screen.height - height) / 2;
		setBounds(x, y, width, height);

		PanboxSplash splash = PanboxSplash.getInstance();
		splash.setBackgroundImage(splashimage);
		splash.setText("Version: " + getVersion());
		add(splash);

		addMouseListener(new java.awt.event.MouseAdapter() {
			public void mousePressed(java.awt.event.MouseEvent evt) {
				//setVisible(false);
				dispose();
				timer.cancel();
				timer.purge();
			}
		});
	}

	public void showWindow(int seconds) {
		timer = new Timer();
		timer.schedule(new CloseAboutWindow(), seconds * 1000);
		showWindow();
	}

	public void showWindow(){
		setVisible(true);
	}

	public void hideWindow(){
		dispose();
	}

	public Image getBackgroundImage() {
		return splashimage;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public String getVersion() {
		String ret = "";

		for (int i = 0; i < PanboxConstants.PANBOX_VERSION.length; i++) {
			ret += (char) PanboxDesktopConstants.PANBOX_VERSION[i];
		}
		return ret;
	}
}


class CloseAboutWindow extends TimerTask{
	public void run(){
		AboutWindow.getInstance().hideWindow();
	}
}