package com.unifi.ordersmgmt.controller;

import java.util.Map;

import com.unifi.ordersmgmt.exception.NotFoundClientException;
import com.unifi.ordersmgmt.exception.NotFoundOrderException;
import com.unifi.ordersmgmt.model.Client;
import com.unifi.ordersmgmt.model.Order;
import com.unifi.ordersmgmt.service.ClientService;
import com.unifi.ordersmgmt.service.OrderService;
import com.unifi.ordersmgmt.view.OrderView;

public class OrderController {
	private ClientService clientService;
	private OrderService orderService;
	private OrderView orderView;

	public OrderController(OrderView orderView, OrderService orderService, ClientService clientService) {
		this.orderView = orderView;
		this.orderService = orderService;
		this.clientService = clientService;
	}

	public void InitializeView() {
		orderView.showAllClients(clientService.findAllClients());
		System.out.println("giorno :" + orderService.findYearsOfOrders());
		orderView.setYearsOrders(orderService.findYearsOfOrders());

	}

	public void showAllClients() {
		orderView.showAllClients(clientService.findAllClients());
	}

	public void allOrdersByYear(int year) {
		orderView.showAllOrders(orderService.allOrdersByYear(year));

	}

	public void yearsOfTheOrders() {
		orderView.setYearsOrders(orderService.findYearsOfOrders());

	}

	public void findOrdersByYearAndClient(Client client, int year) {
		// TODO Auto-generated method stub
		try {
			orderView.showAllOrders(orderService.findallOrdersByClientByYear(client, year));
		} catch (NotFoundClientException e) {
			System.out.println("findOrdersByYearAndClient: exception thrown");
			// TODO Auto-generated catch block
			System.out.println("Client not found");
			orderView.showErrorClient("Cliente non presente nel DB", client);
			orderView.clientRemoved(client);
		}

	}

	public void addClient(Client client) {
		// TODO Auto-generated method stub
		Client saved = clientService.saveClient(client);
		System.out.println("SAVED : " + saved);
		orderView.clientAdded(saved);
	}

	public void deleteClient(Client clientToDelete) {
		// TODO Auto-generated method stub
		try {
			clientService.removeClient(clientToDelete);
			System.out.println("Executed remove client of: " + clientToDelete);
			orderView.clientRemoved(clientToDelete);
		} catch (NotFoundClientException e) {
			// TODO Auto-generated catch block
			orderView.showErrorClient("Cliente non più presente nel DB", clientToDelete);
			orderView.clientRemoved(clientToDelete);
		}

	}

	public void addOrder(Order order) {
		// TODO Auto-generated method stub
		try {
			orderService.addOrder(order);
			orderView.orderAdded(order);
		} catch (NotFoundClientException e) {
			// TODO Auto-generated catch block
			System.out.println(e.getMessage());
			orderView.showErrorClient("Cliente non più presente nel DB", order.getClient());
			orderView.clientRemoved(order.getClient());
			orderView.removeOrdersByClient(order.getClient());
		}

	}

	public void deleteOrder(Order orderToDelete) {
		try {
			orderService.removeOrder(orderToDelete);
			orderView.orderRemoved(orderToDelete);
		} catch (NotFoundClientException e) {
			// TODO Auto-generated catch block
			System.out.println("not client: " + e.getMessage());
			orderView.showErrorClient("Cliente non più presente nel DB", orderToDelete.getClient());
			orderView.clientRemoved(orderToDelete.getClient());
			orderView.removeOrdersByClient(orderToDelete.getClient());
		} catch (NotFoundOrderException e) {
			// TODO: handle exception
			System.out.println(e.getMessage());
			orderView.showOrderError("Ordine non più presente nel DB", orderToDelete);
			orderView.orderRemoved(orderToDelete);

		}
	}

	public void modifyOrder(Order orderToModify, Map<String, Object> updates) {
		// TODO Auto-generated method stub
		try {
			System.out.println("Order controller order to modify: "+orderToModify);
			System.out.println("Order controller updates: "+updates);

			Order orderModified = orderService.updateOrder(orderToModify, updates);
			System.out.println("Order controller order modify: "+orderModified);
			orderView.orderUpdated(orderModified);
		} catch (NotFoundOrderException e) {
			// TODO: handle exception
			System.out.println(e.getMessage());
			orderView.showOrderError("Ordine non più presente nel DB", orderToModify);
			orderView.orderRemoved(orderToModify);

		} catch (NotFoundClientException e) {
			// TODO Auto-generated catch block
			System.out.println(e.getMessage());
			if (e.getMessage().contains("originale")){				
				orderView.showErrorClient("Cliente non più presente nel DB", orderToModify.getClient());
				orderView.clientRemoved(orderToModify.getClient());
				orderView.removeOrdersByClient(orderToModify.getClient());
			}else {				
				orderView.showErrorClient("Cliente non più presente nel DB", ((Client)(updates.get("client"))));
				orderView.clientRemoved(((Client)(updates.get("client"))));
				orderView.removeOrdersByClient(((Client)(updates.get("client"))));
			}
		}
	}
}
