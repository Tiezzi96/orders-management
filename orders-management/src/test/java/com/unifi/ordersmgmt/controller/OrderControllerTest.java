package com.unifi.ordersmgmt.controller;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.assertj.swing.annotation.GUITest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.unifi.ordersmgmt.exception.NotFoundClientException;
import com.unifi.ordersmgmt.model.Client;
import com.unifi.ordersmgmt.model.Order;
import com.unifi.ordersmgmt.service.ClientService;
import com.unifi.ordersmgmt.service.OrderService;
import com.unifi.ordersmgmt.view.OrderView;



public class OrderControllerTest {

	@Mock
	private ClientService clientService;
	@Mock
	private OrderService orderService;
	@Mock
	private OrderView orderView;
	@InjectMocks
	private OrderController controller;

	@After
	public void tearDown() throws Exception {

	}

	@Before
	public void setUp() {
		MockitoAnnotations.openMocks(this);
	}
	
	@Test
	public void testInitializeView() {
		List<Client> clients = asList(new Client());
		when(clientService.findAllClients()).thenReturn(clients);
		List<Integer> years = asList(2024);
		when(orderService.findYearsOfOrders()).thenReturn(years);
		controller.InitializeView();
		verify(orderView).showAllClients(clients);
		verify(orderView).setYearsOrders(years);
	}
	
	@Test
	public void testShowAllClients() {
		List<Client> clients = asList(new Client());
		when(clientService.findAllClients()).thenReturn(clients);
		controller.showAllClients();
		verify(orderView).showAllClients(clients);
	}

	@Test
	public void testAllOrdersByYear() {
		List<Order> orders = asList(new Order());
		when(orderService.allOrdersByYear(2024)).thenReturn(orders);
		controller.allOrdersByYear(2024);
		verify(orderView).showAllOrders(orders);
	}
	
	@Test
	public void testYearsOfOrders() {
		List<Integer> years = asList(2024);
		when(orderService.findYearsOfOrders()).thenReturn(years);
		controller.yearsOfTheOrders();
		verify(orderView).setYearsOrders(years);
	}

	@Test
	public void testOrdersByClientAndYearWhenClientIsNotInDB() {
		Client newClient = new Client("1", "client1");
		when(orderService.findallOrdersByClientByYear(newClient, 2024))
				.thenThrow(new NotFoundClientException("Client not found"));
		controller.findOrdersByYearAndClient(newClient, 2024);
		verify(orderView).showErrorClient("Cliente non presente nel DB", newClient);
		verify(orderView).clientRemoved(newClient);
	}

	@Test
	public void testOrdersByClientAndYear() {
		List<Order> ordersByClientAndYear = asList(new Order());
		when(orderService.findallOrdersByClientByYear(new Client(), 2024)).thenReturn(ordersByClientAndYear);
		controller.findOrdersByYearAndClient(new Client(), 2024);
		verify(orderView).showAllOrders(ordersByClientAndYear);
	}

}
