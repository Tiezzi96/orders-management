package com.unifi.ordersmgmt.controller;

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

}
