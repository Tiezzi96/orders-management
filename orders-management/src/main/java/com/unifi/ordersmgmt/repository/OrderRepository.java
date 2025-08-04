package com.unifi.ordersmgmt.repository;

import java.util.List;
import java.util.Map;

import com.unifi.ordersmgmt.model.Client;
import com.unifi.ordersmgmt.model.Order;

public interface OrderRepository extends Repository<Order> {
	public List<Order> findOrderByYear(int year);

	public List<Integer> getYearsOfOrders();

	public List<Order> removeOrdersByClient(String clientId);

	public Order updateOrder(String orderID, Map<String, Object> updates);

	public List<Order> findOrdersByClientAndYear(Client client, int year);

	public List<Order> findOrdersByClient(Client client);

}
