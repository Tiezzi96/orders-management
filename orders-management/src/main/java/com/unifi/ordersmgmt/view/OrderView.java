package com.unifi.ordersmgmt.view;

import java.util.List;

import com.unifi.ordersmgmt.model.Client;
import com.unifi.ordersmgmt.model.Order;

public interface OrderView {

	public void showAllClients(List<Client> clients);

	public void setYearsOrders(List<Integer> yearsOfOrders);

	public void showAllOrders(List<Order> orders);

	public void showErrorClient(String message, Client client);

	public void clientRemoved(Client clientRemoved);

}



