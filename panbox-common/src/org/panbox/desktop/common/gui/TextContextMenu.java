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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.text.JTextComponent;

import org.panbox.Settings;

/**
 * Global application context menu for text fields. Source: <a href=
 * "http://stackoverflow.com/questions/19424574/adding-a-context-menu-to-all-swing-text-components-in-application"
 * >http://stackoverflow.com/questions/19424574/adding-a-context-menu-to-all-
 * swing-text-components-in-application</a>
 *
 */
public class TextContextMenu extends JPopupMenu implements ActionListener {

	private static final long serialVersionUID = -8783567464951170179L;
	public static final TextContextMenu INSTANCE = new TextContextMenu();
	private final JMenuItem itemCut;
	private final JMenuItem itemCopy;
	private final JMenuItem itemPaste;
	private final JMenuItem itemDelete;
	private final JMenuItem itemSelectAll;

	private final ResourceBundle bundle = ResourceBundle.getBundle(
			"org.panbox.desktop.common.gui.Messages", Settings.getInstance()
					.getLocale());

	private TextContextMenu() {
		itemCut = newItem(bundle.getString("TextContextMenu.cut"), 'T');
		itemCopy = newItem(bundle.getString("TextContextMenu.copy"), 'C');
		itemPaste = newItem(bundle.getString("TextContextMenu.paste"), 'P');
		itemDelete = newItem(bundle.getString("TextContextMenu.delete"), 'D');
		addSeparator();
		itemSelectAll = newItem(bundle.getString("TextContextMenu.selectAll"),
				'A');
	}

	private JMenuItem newItem(String text, char mnemonic) {
		JMenuItem item = new JMenuItem(text, mnemonic);
		item.addActionListener(this);
		return add(item);
	}

	@Override
	public void show(Component invoker, int x, int y) {
		JTextComponent tc = (JTextComponent) invoker;
		boolean changeable = tc.isEditable() && tc.isEnabled();
		itemCut.setVisible(changeable);
		itemPaste.setVisible(changeable);
		itemDelete.setVisible(changeable);
		super.show(invoker, x, y);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JTextComponent tc = (JTextComponent) getInvoker();
		tc.requestFocus();

		boolean haveSelection = tc.getSelectionStart() != tc.getSelectionEnd();
		if (e.getSource() == itemCut) {
			if (!haveSelection)
				tc.selectAll();
			tc.cut();
		} else if (e.getSource() == itemCopy) {
			if (!haveSelection)
				tc.selectAll();
			tc.copy();
		} else if (e.getSource() == itemPaste) {
			tc.paste();
		} else if (e.getSource() == itemDelete) {
			if (!haveSelection)
				tc.selectAll();
			tc.replaceSelection("");
		} else if (e.getSource() == itemSelectAll) {
			tc.selectAll();
		}
	}
}