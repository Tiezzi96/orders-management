package com.unifi.ordersmgmt.controller;

import java.util.Map;
import org.apache.logging.log4j.*;
import com.unifi.ordersmgmt.exception.NotFoundClientException;
import com.unifi.ordersmgmt.exception.NotFoundOrderException;
import com.unifi.ordersmgmt.model.Client;
import com.unifi.ordersmgmt.model.Order;
import com.unifi.ordersmgmt.service.ClientService;
import com.unifi.ordersmgmt.service.OrderService;
import com.unifi.ordersmgmt.view.OrderView;

public class OrderController {
	private static final String CLIENT = "client";
	private static final Logger logger = LogManager.getLogger(OrderController.class);
	private static final String CLIENT_ERROR_MESSAGE = "Cliente non più presente nel DB";
	private ClientService clientService;
	private OrderService orderService;
	private OrderView orderView;

	public OrderController(OrderView orderView, OrderService orderService, ClientService clientService) {
		this.orderView = orderView;
		this.orderService = orderService;
		this.clientService = clientService;
	}

	public void setupView() {
		orderView.showAllClients(clientService.findAllClients());
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
		try {
			orderView.showAllOrders(orderService.findallOrdersByClientByYear(client, year));
		} catch (NotFoundClientException e) {
			logger.warn("findOrdersByYearAndClient: Client not found");
			orderView.showErrorClient("Cliente non presente nel DB", client);
			orderView.clientRemoved(client);
		}

	}

	public void addClient(Client client) {
		Client saved = clientService.saveClient(client);
		logger.info("Client saved: id={}, name={}", saved.getIdentifier(), saved.getName());
		orderView.clientAdded(saved);
	}

	public void deleteClient(Client clientToDelete) {
		try {
			clientService.removeClient(clientToDelete);
			logger.info("Executed remove client of: clientID={}, name={}", clientToDelete.getIdentifier(),
					clientToDelete.getName());
			orderView.clientRemoved(clientToDelete);
		} catch (NotFoundClientException e) {
			orderView.showErrorClient(CLIENT_ERROR_MESSAGE, clientToDelete);
			orderView.clientRemoved(clientToDelete);
		}

	}

	public void addOrder(Order order) {
		try {
			orderService.addOrder(order);
			orderView.orderAdded(order);
		} catch (NotFoundClientException e) {
			logger.warn("Client order not found: orderID={}, client={}, errorMessage={}", order.getIdentifier(),
					order.getClient(), e.getMessage());
			orderView.showErrorClient(CLIENT_ERROR_MESSAGE, order.getClient());
			orderView.clientRemoved(order.getClient());
			orderView.removeOrdersByClient(order.getClient());
		}

	}

	public void deleteOrder(Order orderToDelete) {
		try {
			orderService.removeOrder(orderToDelete);
			orderView.orderRemoved(orderToDelete);
		} catch (NotFoundClientException e) {
			logger.warn("Client order to remove not found: orderID={}, client={}, errorMessage={}",
					orderToDelete.getIdentifier(), orderToDelete.getClient(), e.getMessage());
			orderView.showErrorClient(CLIENT_ERROR_MESSAGE, orderToDelete.getClient());
			orderView.clientRemoved(orderToDelete.getClient());
			orderView.removeOrdersByClient(orderToDelete.getClient());
		} catch (NotFoundOrderException e) {
			logger.warn("Order to remove not found: orderID={}, client={}, errorMessage={}",
					orderToDelete.getIdentifier(), orderToDelete.getClient(), e.getMessage());
			orderView.showOrderError("Ordine non più presente nel DB", orderToDelete);
			orderView.orderRemoved(orderToDelete);

		}
	}

	public void modifyOrder(Order orderToModify, Map<String, Object> updates) {
		try {
			logger.debug("Order controller order to modify: {}", orderToModify);
			logger.debug("Order controller updates: {}", updates);
			Order orderModified = orderService.updateOrder(orderToModify, updates);
			logger.debug("Order controller order modify: {}", orderModified);
			orderView.orderUpdated(orderModified);
		} catch (NotFoundOrderException e) {
			logger.warn("order to update not found: orderID={}, client={}, errorMessage={}",
					orderToModify.getIdentifier(), orderToModify.getClient(), e.getMessage());
			orderView.showOrderError("Ordine non più presente nel DB", orderToModify);
			orderView.orderRemoved(orderToModify);

		} catch (NotFoundClientException e) {
			if (e.getMessage().contains("originale")) {
				logger.warn("Original client order to update not found: orderID={}, client={}, errorMessage={}",
						orderToModify.getIdentifier(), orderToModify.getClient(), e.getMessage());
				orderView.showErrorClient(CLIENT_ERROR_MESSAGE, orderToModify.getClient());
				orderView.clientRemoved(orderToModify.getClient());
				orderView.removeOrdersByClient(orderToModify.getClient());
			} else {
				logger.warn("New client order to update not found: orderID={}, client={}, errorMessage={}",
						orderToModify.getIdentifier(), updates.get(CLIENT), e.getMessage());
				orderView.showErrorClient(CLIENT_ERROR_MESSAGE, ((Client) (updates.get(CLIENT))));
				orderView.clientRemoved(((Client) (updates.get(CLIENT))));
				orderView.removeOrdersByClient(((Client) (updates.get(CLIENT))));
			}
		}
	}

	public void allOrdersByClient(Client client) {
		try {
			orderView.showAllOrders(orderService.allOrdersByClient(client));
		} catch (NotFoundClientException e) {
			logger.warn("allOrdersByClient: Client not found");
			orderView.showErrorClient("Cliente non presente nel DB", client);
			orderView.clientRemoved(client);
		}
	}

	public void getAllOrders() {
		orderView.showAllOrders(orderService.findAllOrders());

	}
}
