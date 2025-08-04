package com.unifi.ordersmgmt.controller;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.unifi.ordersmgmt.exception.NotFoundClientException;
import com.unifi.ordersmgmt.exception.NotFoundOrderException;
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
	private AutoCloseable closeable;

	@After
	public void tearDown() throws Exception {
		closeable.close();
	}

	@Before
	public void setUp() {
		closeable = MockitoAnnotations.openMocks(this);
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
	
	
	@Test
	public void testDeleteClientWhenItIsPresentInDB() {
		Client newClient = new Client();
		controller.deleteClient(newClient);
		InOrder inOrder = Mockito.inOrder(clientService, orderView);
		inOrder.verify(clientService).removeClient(newClient);
		inOrder.verify(orderView).clientRemoved(newClient);

	}

	@Test
	public void testDeleteClientWhenItIsNotPresentInDB() {
		Client newClient = new Client();
		doThrow(new NotFoundClientException("Client not found")).when(clientService).removeClient(newClient);
		controller.deleteClient(newClient);
		InOrder inOrder = Mockito.inOrder(clientService, orderView);
		inOrder.verify(clientService).removeClient(newClient);
		inOrder.verify(orderView).showErrorClient("Cliente non più presente nel DB", newClient);
		inOrder.verify(orderView).clientRemoved(newClient);

	}

	@Test
	public void testAddNewClient() {
		Client newClient = new Client("1", "client");
		when(clientService.saveClient(newClient)).thenReturn(newClient);
		controller.addClient(newClient);
		InOrder inOrder = Mockito.inOrder(clientService, orderView);
		inOrder.verify(clientService).saveClient(newClient);
		inOrder.verify(orderView).clientAdded(newClient);

	}

	@Test
	public void testAddOrderWhenClientIsInDB() {
		Order newOrder = new Order();
		controller.addOrder(newOrder);
		InOrder inOrder = Mockito.inOrder(orderService, orderView);
		inOrder.verify(orderService).addOrder(newOrder);
		inOrder.verify(orderView).orderAdded(newOrder);

	}

	@Test
	public void testAddOrderWhenClientIsNotInDB() {
		Client clientRemoved = new Client();
		Order newOrder = new Order("ORDER-00001", clientRemoved, new Date(), 10.0);
		doThrow(new NotFoundClientException("Client not Found")).when(orderService).addOrder(newOrder);
		controller.addOrder(newOrder);
		InOrder inOrder = Mockito.inOrder(orderService, orderView);
		inOrder.verify(orderService).addOrder(newOrder);
		inOrder.verify(orderView).showErrorClient("Cliente non più presente nel DB", newOrder.getClient());
		inOrder.verify(orderView).clientRemoved(clientRemoved);
		inOrder.verify(orderView).removeOrdersByClient(clientRemoved);

	}
	
	@Test
	public void testDeleteOrderWhenClientIsInDB() {
		Order newOrder = new Order();
		controller.deleteOrder(newOrder);
		InOrder inOrder = Mockito.inOrder(orderService, orderView);
		inOrder.verify(orderService).removeOrder(newOrder);
		inOrder.verify(orderView).orderRemoved(newOrder);

	}

	@Test
	public void testDeleteOrderWhenClientIsNotInDB() {
		Client clientRemoved = new Client();
		Order newOrder = new Order("ORDER-00001", clientRemoved, new Date(), 10.0);
		doThrow(new NotFoundClientException("Client not Found")).when(orderService).removeOrder(newOrder);
		controller.deleteOrder(newOrder);
		InOrder inOrder = Mockito.inOrder(orderService, orderView);
		inOrder.verify(orderService).removeOrder(newOrder);
		inOrder.verify(orderView).showErrorClient("Cliente non più presente nel DB", newOrder.getClient());
		inOrder.verify(orderView).clientRemoved(clientRemoved);
		inOrder.verify(orderView).removeOrdersByClient(clientRemoved);

	}

	@Test
	public void testDeleteOrderWhenOrderIsNotInDB() {
		Client client = new Client();
		Order newOrder = new Order("ORDER-00001", client, new Date(), 10.0);
		doThrow(new NotFoundOrderException("Order not Found")).when(orderService).removeOrder(newOrder);
		controller.deleteOrder(newOrder);
		InOrder inOrder = Mockito.inOrder(orderService, orderView);
		inOrder.verify(orderService).removeOrder(newOrder);
		inOrder.verify(orderView).showOrderError("Ordine non più presente nel DB", newOrder);
		inOrder.verify(orderView).orderRemoved(newOrder);

	}
	
	@Test
	public void testModifyOrderWhenOrderIsInDBAndClientIsInDB() {
		Order newOrder = new Order();
		Map<String, Object> updates = new HashMap<String, Object>();
		Date date = new Date();
		updates.put("price", 10.5);
		updates.put("date", new Date());
		Order orderModified = new Order(newOrder.getIdentifier(), newOrder.getClient(), date, 10.5);
		when(orderService.updateOrder(newOrder, updates)).thenReturn(orderModified);
		controller.modifyOrder(newOrder, updates);
		InOrder inOrder = Mockito.inOrder(orderService, orderView);
		inOrder.verify(orderService).updateOrder(newOrder, updates);
		inOrder.verify(orderView).orderUpdated(orderModified);
	}

	@Test
	public void testModifyOrderWhenOrderIsNotInDB() {
		Client client = new Client();
		Order orderRemoved = new Order("ORDER-00001", client, new Date(), 10.0);
		Map<String, Object> updates = new HashMap<String, Object>();
		updates.put("price", 10.5);
		updates.put("date", new Date());
		doThrow(new NotFoundOrderException("Order not Found")).when(orderService).updateOrder(orderRemoved, updates);
		controller.modifyOrder(orderRemoved, updates);
		InOrder inOrder = Mockito.inOrder(orderService, orderView);
		inOrder.verify(orderService).updateOrder(orderRemoved, updates);
		inOrder.verify(orderView).showOrderError("Ordine non più presente nel DB", orderRemoved);
		inOrder.verify(orderView).orderRemoved(orderRemoved);
	}

	@Test
	public void testModifyOrderWhenOriginalClientIsNotInDB() {
		Client clientRemoved = new Client();
		Order orderToModify = new Order("ORDER-00001", clientRemoved, new Date(), 10.0);
		Map<String, Object> updates = new HashMap<String, Object>();
		updates.put("price", 10.5);
		updates.put("date", new Date());
		doThrow(new NotFoundClientException(String.format("Il cliente originale con id %s non è presente nel database",
				clientRemoved.getIdentifier()))).when(orderService).updateOrder(orderToModify, updates);
		controller.modifyOrder(orderToModify, updates);
		InOrder inOrder = Mockito.inOrder(orderService, orderView);
		inOrder.verify(orderService).updateOrder(orderToModify, updates);
		inOrder.verify(orderView).showErrorClient("Cliente non più presente nel DB", orderToModify.getClient());
		inOrder.verify(orderView).clientRemoved(clientRemoved);
		inOrder.verify(orderView).removeOrdersByClient(clientRemoved);
	}
	
	@Test
	public void testModifyOrderWhenClientForUpdateIsNotInDB() {
		Client clientOriginal = new Client();
		Client clientForUpdate = new Client("2", "client not exists");
		Order orderToModify = new Order("ORDER-00001", clientOriginal, new Date(), 10.0);
		Map<String, Object> updates = new HashMap<String, Object>();
		updates.put("client", clientForUpdate);
		updates.put("price", 10.5);
		updates.put("date", new Date());
		doThrow(new NotFoundClientException(String.format("Il cliente con id %s non è presente nel database",
				((Client)updates.get("client")).getIdentifier()))).when(orderService).updateOrder(orderToModify, updates);
		controller.modifyOrder(orderToModify, updates);
		InOrder inOrder = Mockito.inOrder(orderService, orderView);
		inOrder.verify(orderService).updateOrder(orderToModify, updates);
		inOrder.verify(orderView).showErrorClient("Cliente non più presente nel DB", clientForUpdate);
		inOrder.verify(orderView).clientRemoved(clientForUpdate);
		inOrder.verify(orderView).removeOrdersByClient(clientForUpdate);
	}
	
	@Test
	public void testOrdersByClientWhenClientIsNotInDB() {
		Client newClient = new Client("1", "client1");
		when(orderService.allOrdersByClient(newClient))
				.thenThrow(new NotFoundClientException("Client not found"));
		controller.allOrdersByClient(newClient);
		verify(orderView).showErrorClient("Cliente non presente nel DB", newClient);
		verify(orderView).clientRemoved(newClient);
	}

	@Test
	public void testOrdersByClient() {
		List<Order> ordersByClient = asList(new Order());
		when(orderService.allOrdersByClient(new Client())).thenReturn(ordersByClient);
		controller.allOrdersByClient(new Client());
		verify(orderView).showAllOrders(ordersByClient);
	}

}
