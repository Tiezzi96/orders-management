package com.unifi.ordersmgmt.view.swing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import javax.swing.table.AbstractTableModel;

import com.unifi.ordersmgmt.model.Order;

public class OrderTableModel extends AbstractTableModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private transient List<Order> orders;
	private static final String[] columns = { "Id", "Cliente", "Data", "Importo ($)" };

	public OrderTableModel() {
		// TODO Auto-generated constructor stub
		this.orders = new ArrayList<Order>();
	}

	@Override
	public int getColumnCount() {
		// TODO Auto-generated method stub
		return columns.length;
	}
	
	@Override
	public String getColumnName(int column) {
		// TODO Auto-generated method stub
		return columns[column];
	}

	@Override
	public int getRowCount() {
		// TODO Auto-generated method stub
		return orders.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		// TODO Auto-generated method stub
		Order order = orders.get(rowIndex);
		if (columnIndex % 4 == 0) {
			return order.getIdentifier();
		} else if (columnIndex % 4 == 1) {
			return order.getClient() != null ? Objects.toString(order.getClient().getName(), "—") : "—";
		} else if (columnIndex % 4 == 2) {
			return order.getDate();

		} else {
			return order.getPrice();
		}

	}

	public void addOrder(Order order) {
		orders.add(order);
		System.out.println("ORDER: " + order);
		Collections.sort(orders, Comparator.comparing(Order::getDate));
		fireTableDataChanged();
	}

	public void removedAllOrders() {
		orders.clear();
		fireTableStructureChanged();
	}

	public Order getOrderAt(int selectedRow) {
		// TODO Auto-generated method stub
		if (selectedRow == -1)
			return null;
		return orders.get(selectedRow);
	}

	public List<Order> getOrders() {
		// TODO Auto-generated method stub
		return orders;
	}

	public int getOrderIndex(Order orderSelected) {
		// TODO Auto-generated method stub
		return orders.indexOf(orderSelected);
	}

}
