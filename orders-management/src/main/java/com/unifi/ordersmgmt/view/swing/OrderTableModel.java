package com.unifi.ordersmgmt.view.swing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.table.AbstractTableModel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.unifi.ordersmgmt.model.Client;
import com.unifi.ordersmgmt.model.Order;

public class OrderTableModel extends AbstractTableModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LogManager.getLogger(OrderTableModel.class);
	private transient List<Order> orders;
	private static final String[] columns = { "Id", "Cliente", "Data", "Importo ($)" };

	public OrderTableModel() {
		this.orders = new ArrayList<>();
	}

	@Override
	public int getColumnCount() {
		return columns.length;
	}
	
	@Override
	public String getColumnName(int column) {
		return columns[column];
	}

	@Override
	public int getRowCount() {
		return orders.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
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
		logger.info("ORDER: {}", order);
		Collections.sort(orders, Comparator.comparing(Order::getDate));
		fireTableDataChanged();
	}

	public void removedAllOrders() {
		orders.clear();
		fireTableStructureChanged();
	}

	public Order getOrderAt(int selectedRow) {
		if (selectedRow == -1)
			return null;
		return orders.get(selectedRow);
	}

	public List<Order> getOrders() {
		return orders;
	}

	public int getOrderIndex(Order orderSelected) {
		return orders.indexOf(orderSelected);
	}

	public void removeOrdersOfClient(Client client) {
		List<Order> ordersOfClient = orders.stream()
				.filter(o -> o.getClient().getIdentifier().equals(client.getIdentifier())).collect(Collectors.toList());
		orders.removeAll(ordersOfClient);
		fireTableDataChanged();
		
	}

	public void removeOrder(Order orderRemoved) {
		orders.remove(orderRemoved);
	}

}
