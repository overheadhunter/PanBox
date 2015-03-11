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

import javax.swing.*;
import java.awt.*;

/**
 * Created by Dominik Spychalski on 02.12.14.
 */
class PanboxSplash extends JComponent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5315560795018530568L;
	private static PanboxSplash instance = null;
	private Image backgroundImage = null;
	private String text = "";

	public static PanboxSplash getInstance() {
		if (instance == null) {
			instance = new PanboxSplash();
		}
		return instance;
	}

	private PanboxSplash() {
		
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}

	public void setBackgroundImage(Image backgroundImage) {
		this.backgroundImage = backgroundImage;
	}

	public Image getBackgroundImage() {
		return backgroundImage;
	}

	public void paint(Graphics g) {
		Graphics2D splashGraphics = (Graphics2D) g;
		splashGraphics.setColor(Color.BLACK);

		if (getBackgroundImage() != null) {
			splashGraphics.drawImage(getBackgroundImage(), 0, 0, this);
		}

		if (!getText().equals("")) {
			splashGraphics.drawString(getText(), 120, 250);
		}
	}

}
