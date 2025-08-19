package com.unifi.ordersmgmt.service;

import java.util.List;
import java.util.Map;

import com.unifi.ordersmgmt.model.Client;
import com.unifi.ordersmgmt.model.Order;

public interface OrderService {
	public List<Order> allOrdersByYear(int year);

	public List<Order> findallOrdersByClientByYear(Client client, int year);

	public List<Order> allOrdersByClient(Client client);

	public Order addOrder(Order order);

	public Order removeOrder(Order order);

	public List<Integer> findYearsOfOrders();

	public Order updateOrder(Order order, Map<String, Object> updates);

	List<Order> findAllOrders();
}
