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
package org.panbox.desktop.common.gui.addressbook;

import org.panbox.core.csp.StorageBackendType;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;

import java.awt.*;
import java.util.ArrayList;

/**
 * Created by Timo Nolle on 25.08.14.
 */
public class CSPTableCellEditor extends AbstractCellEditor implements TableCellEditor {

	private static final long serialVersionUID = -6180293453032252863L;
	private JComboBox<String> editor;
	private ArrayList<String> valuesList = getStorageTypes();

	public CSPTableCellEditor() {
		editor = new JComboBox<String>(valuesList.toArray(new String[]{}));
	}

	public CSPTableCellEditor(ArrayList<String> alreadyUsedCsps) {
		valuesList.removeAll(alreadyUsedCsps);
		editor = new JComboBox<String>(valuesList.toArray(new String[]{}));
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int rowIndex, int colIndex) {

		// Set the model data of the table
		if (isSelected) {
			editor.setSelectedItem((String) value);
			TableModel model = table.getModel();
			model.setValueAt(value, rowIndex, colIndex);
		}

		return editor;
	}

	@Override
	public Object getCellEditorValue() {
		return editor.getSelectedItem();
	}

	private ArrayList<String> getStorageTypes() {
		ArrayList<String> list = new ArrayList<>();
		for (StorageBackendType t : StorageBackendType.values()) {
			if (t != StorageBackendType.FOLDER)
				list.add(t.getDisplayName());
		}
		return list;
	}
}
