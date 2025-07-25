package com.unifi.ordersmgmt.view.swing;

import java.util.List;

import javax.swing.JFrame;

import com.unifi.ordersmgmt.model.Client;
import com.unifi.ordersmgmt.model.Order;
import com.unifi.ordersmgmt.view.OrderView;


public class OrderSwingView extends JFrame implements OrderView  {

	public OrderSwingView() {
		// TODO Auto-generated constructor stub
	}
	@Override
	public void showAllClients(List<Client> clients) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setYearsOrders(List<Integer> yearsOfOrders) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void showAllOrders(List<Order> orders) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void showErrorClient(String message, Client client) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clientRemoved(Client clientRemoved) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clientAdded(Client clientAdded) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void orderAdded(Order orderAdded) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeOrdersByClient(Client client) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void showOrderError(String message, Order order) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void orderRemoved(Order orderRemoved) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void orderUpdated(Order orderModified) {
		// TODO Auto-generated method stub
		
	}

}
