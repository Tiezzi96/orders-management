package com.unifi.ordersmgmt.view.swing;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

public class TableOrder extends JTable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public TableOrder(AbstractTableModel orderTableModel) {
		super(orderTableModel);
	}

	@Override
	public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
		Component c = super.prepareRenderer(renderer, row, column);
		if (!isRowSelected(row)) {
			if (row % 2 == 0) {
				c.setBackground(Color.LIGHT_GRAY); // Colore per righe pari
			} else {
				c.setBackground(Color.WHITE); // Colore per righe dispari
			}
		} else {
			c.setBackground(Color.YELLOW); // Colore per righe selezionate
		}

		return c;
	}

	@Override
	public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
		// Se sto tentando di selezionare la riga già selezionata...
		if (rowIndex == getSelectedRow() && !extend && !toggle) {
			// ...la deseleziono e termino qui
			clearSelection();
		} else {
			// Altrimenti comportamento standard
			super.changeSelection(rowIndex, columnIndex, toggle, extend);
		}
	}
}
