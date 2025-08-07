package com.unifi.ordersmgmt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.unifi.ordersmgmt.exception.NotFoundClientException;
import com.unifi.ordersmgmt.exception.NotFoundOrderException;
import com.unifi.ordersmgmt.model.Client;
import com.unifi.ordersmgmt.model.Order;
import com.unifi.ordersmgmt.repository.mongo.ClientMongoRepository;
import com.unifi.ordersmgmt.repository.mongo.ClientSequenceGenerator;
import com.unifi.ordersmgmt.repository.mongo.OrderMongoRepository;
import com.unifi.ordersmgmt.repository.mongo.OrderSequenceGenerator;
import com.unifi.ordersmgmt.transaction.*;
import com.unifi.ordersmgmt.transaction.mongo.*;

public class OrderServiceMongoRepositoryIT {
	
	private MongoClient mongoClient;
	private TransactionalOrderService orderService;
	private ClientMongoRepository clientRepository;
	private OrderMongoRepository orderRepository;

	@Before
	public void setUp() {
		mongoClient = MongoClients.create("mongodb://localhost:27017/?replicaSet=rs0");
		TransactionManager transactionManager = new MongoTransactionManager(mongoClient, "client", "order", "budget");
		orderService = new TransactionalOrderService(transactionManager);
		ClientSession session = mongoClient.startSession();
		OrderSequenceGenerator seqGen = new OrderSequenceGenerator(mongoClient, "budget");
		ClientSequenceGenerator clientSeqGen = new ClientSequenceGenerator(mongoClient, "budget");

		clientRepository = new ClientMongoRepository(mongoClient, session, "budget", "client", clientSeqGen);
		orderRepository = new OrderMongoRepository(mongoClient, session, "budget", "order", clientRepository, seqGen);
		for (Client client : clientRepository.findAll()) {
			clientRepository.delete(client.getIdentifier());
		}
		for (Order order : orderRepository.findAll()) {
			orderRepository.delete(order.getIdentifier());
		}

		clientRepository.save(new Client("CLIENT-00001", "first client"));
		clientRepository.save(new Client("CLIENT-00002", "second client"));

	}

	@After
	public void tearDown() {
		mongoClient.close();
	}
	
	@Test
	public void testAddOrderWhenClientExistingInDB() {
		System.out.println(clientRepository.findById("CLIENT-00001"));
		Order order = new Order("ORDER-00001", new Client("CLIENT-00001", "first client"),
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10.0);
		orderService.addOrder(order);
		List<Order> ordersInDB = orderRepository.findAll();
		assertThat(ordersInDB).containsExactly(order);
	}

	@Test
	public void testAddNewOrderWhenClientNoExistingInDB() {
		Client clientOfOrderToAdd = new Client("CLIENT-00003", "test identifier");
		Order orderToAdd = new Order("ORDER-00001", clientOfOrderToAdd, new Date(), 10);
		try {
			orderService.addOrder(orderToAdd);
			fail("Expected a NotFoundClientException to be thrown");
		} catch (NotFoundClientException e) {
			assertThat("Il cliente con id " + clientOfOrderToAdd.getIdentifier() + " non è presente nel database")
					.isEqualTo(e.getMessage());
			return;
		}
	}
	
	@Test
	public void testRemoveOrderWhenOrderNoExistingInDB() {
		Client clientOfOrderToRemove = new Client("CLIENT-00001", "first client");
		Order orderToRemove = new Order("ORDER-00001", clientOfOrderToRemove, new Date(), 10);
		try {
			orderService.removeOrder(orderToRemove);
			fail("Expected a NotFoundOrderException to be thrown");
		} catch (NotFoundOrderException e) {
			assertThat("L'ordine con id " + orderToRemove.getIdentifier() + " non è presente nel database")
					.isEqualTo(e.getMessage());
		}
	}

	@Test
	public void testRemoveOrderWhenClientNoExistingInDB() {
		Client clientOfOrderToRemove = new Client("CLIENT-00001", "first client");
		clientRepository.delete(clientOfOrderToRemove.getIdentifier());
		Order order = new Order("ORDER-00001", clientOfOrderToRemove, new Date(), 10);
		try {
			orderService.removeOrder(order);
			fail("Expected a NotFoundClientException to be thrown");
		} catch (NotFoundClientException e) {
			assertThat("Il cliente con id " + clientOfOrderToRemove.getIdentifier() + " non è presente nel database")
					.isEqualTo(e.getMessage());
		}
	}

	@Test
	public void testRemoveOrderWhenClientAndOrderExistingInDB() {
		Order orderToRemove = orderRepository
				.save(new Order("ORDER-00001", new Client("CLIENT-00001", "first client"), new Date(), 10));
		orderRepository.save(new Order("ORDER-00002", new Client("CLIENT-00002", "first client"), new Date(), 10));
		orderService.removeOrder(orderToRemove);
		Order orderFound = orderRepository.findById(orderToRemove.getIdentifier());
		assertThat(orderFound).isNull();
	}
	
	@Test
	public void testallOrdersByYear() {
		Order order1 = new Order("ORDER-00001", new Client("CLIENT-00001", "first client"),
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		orderRepository.save(order1);
		Order order2 = new Order("ORDER-00002", new Client("CLIENT-00002", "second client"),
				Date.from(LocalDate.of(2025, 1, 2).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20);
		orderRepository.save(order2);
		Order order3 = new Order("ORDER-00003", new Client("CLIENT-00001", "first client"),
				Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 30);
		orderRepository.save(order3);
		List<Order> ordersOfYearFixtureFound = orderService.allOrdersByYear(2025);
		assertThat(ordersOfYearFixtureFound).containsExactly(order1, order2);
	}

	@Test
	public void testFindYearsOfTheOrders() {
		orderRepository.save(new Order("ORDER-00001", new Client("CLIENT-00001", "first client"),
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10));
		orderRepository.save(new Order("ORDER-00002", new Client("CLIENT-00002", "second client"),
				Date.from(LocalDate.of(2025, 1, 2).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20));
		orderRepository.save(new Order("ORDER-00003", new Client("CLIENT-00001", "first client"),
				Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 30));
		List<Integer> yearsOfTheOrdersFound = orderService.findYearsOfOrders();
		assertThat(yearsOfTheOrdersFound).containsExactly(2024, 2025);
	}

	@Test
	public void testFindOrdersOfAClientAndYear() {
		orderRepository.save(new Order("ORDER-00001", new Client("CLIENT-00001", "first client"),
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10));
		orderRepository.save(new Order("ORDER-00002", new Client("CLIENT-00002", "second client"),
				Date.from(LocalDate.of(2025, 1, 2).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20));
		orderRepository.save(new Order("ORDER-00003", new Client("CLIENT-00001", "first client"),
				Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 30));
		List<Order> orderOfYearCurrentClient1Found = orderService
				.findallOrdersByClientByYear(new Client("CLIENT-00001", "first client"), 2025);
		assertThat(orderOfYearCurrentClient1Found)
				.containsExactly(new Order("ORDER-00001", new Client("CLIENT-00001", "first client"),
						Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10));
	}

	@Test
	public void testFindOrdersOfAClientAndYearWhenClientNoExistingInDB() {
		Client client1 = new Client("CLIENT-00001", "first client");
		orderRepository.save(new Order("ORDER-00001", client1,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10));
		orderRepository.save(new Order("ORDER-00002", new Client("CLIENT-00002", "second client"),
				Date.from(LocalDate.of(2025, 1, 2).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20));
		orderRepository.save(new Order("ORDER-00003", new Client("CLIENT-00001", "first client"),
				Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 30));
		clientRepository.delete("CLIENT-00001");
		try {
			orderService.findallOrdersByClientByYear(client1, 2025);
			fail("Expected a NotFoundClientException to be thrown");
		} catch (NotFoundClientException e) {
			assertThat("Il cliente con id " + client1.getIdentifier() + " non è presente nel database")
					.isEqualTo(e.getMessage());
		}
	}
	
	@Test
	public void testUpdateOrderWhenOrderNoExistingInDB() {
		Client clientOfOrderToUpdate = new Client("CLIENT-00001", "first client");
		Order orderToUpdate = new Order("ORDER-00001", clientOfOrderToUpdate, new Date(), 10);
		Map<String, Object> updates = new HashMap<String, Object>();
		Date date2 = new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date2);
		calendar.add(Calendar.YEAR, -1);
		date2 = calendar.getTime();
		updates.put("date", date2);
		updates.put("price", 20.5);
		updates.put("client", new Client("CLIENT-00002", "second client"));
		try {
			orderService.updateOrder(orderToUpdate, updates);
			fail("Expected a NotFoundOrderException to be thrown");
		} catch (NotFoundOrderException e) {
			assertThat("L'ordine con id " + orderToUpdate.getIdentifier() + " non è presente nel database")
					.isEqualTo(e.getMessage());
		}
	}

	@Test
	public void testUpdateOrderWhenClientNoExistingInDB() {
		Client clientOfOrderToUpdate = new Client("CLIENT-00001", "first client");
		Map<String, Object> updates = new HashMap<String, Object>();
		Date date2 = new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date2);
		calendar.add(Calendar.YEAR, -1);
		date2 = calendar.getTime();
		updates.put("date", date2);
		updates.put("price", 20.5);
		Client clientOfOrderUpdated = new Client("CLIENT-00002", "second client");
		updates.put("client", clientOfOrderUpdated);
		clientRepository.delete("CLIENT-00002");
		Order order = new Order("ORDER-00001", clientOfOrderToUpdate, new Date(), 10);
		orderRepository.save(order);
		try {
			orderService.updateOrder(order, updates);
			fail("Expected a NotFoundClientException to be thrown");
		} catch (NotFoundClientException e) {
			assertThat("Il cliente con id " + clientOfOrderUpdated.getIdentifier() + " non è presente nel database")
					.isEqualTo(e.getMessage());
		}
	}

	@Test
	public void testUpdateOrderWhenClientAndOrderExistingInDB() {
		Order orderToUpdate = orderRepository
				.save(new Order("ORDER-00001", new Client("CLIENT-00001", "first client"), new Date(), 10));
		orderRepository.save(new Order("ORDER-00002", new Client("CLIENT-00002", "first client"), new Date(), 10));
		Map<String, Object> updates = new HashMap<String, Object>();
		Date date2 = new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date2);
		calendar.add(Calendar.YEAR, -1);
		date2 = calendar.getTime();
		updates.put("date", date2);
		updates.put("price", 20.5);
		Client clientOfOrderUpdated = new Client("CLIENT-00002", "second client");
		updates.put("client", clientOfOrderUpdated);
		orderService.updateOrder(orderToUpdate, updates);
		Order orderFound = orderRepository.findById(orderToUpdate.getIdentifier());
		assertThat(orderFound).isEqualTo(new Order(orderToUpdate.getIdentifier(), clientOfOrderUpdated, date2, 20.5));
		System.out.println(new Order(orderToUpdate.getIdentifier(), clientOfOrderUpdated, date2, 20.5));
	}
	
	@Test
	public void testFindOrdersOfAClient() {
		orderRepository.save(new Order("ORDER-00001", new Client("CLIENT-00001", "first client"),
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10));
		orderRepository.save(new Order("ORDER-00002", new Client("CLIENT-00002", "second client"),
				Date.from(LocalDate.of(2025, 1, 2).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20));
		orderRepository.save(new Order("ORDER-00003", new Client("CLIENT-00001", "first client"),
				Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 30));
		List<Order> orderOfYearCurrentClient1Found = orderService
				.allOrdersByClient(new Client("CLIENT-00001", "first client"));
		assertThat(orderOfYearCurrentClient1Found).containsExactly(
				new Order("ORDER-00001", new Client("CLIENT-00001", "first client"),
						Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10),
				new Order("ORDER-00003", new Client("CLIENT-00001", "first client"),
						Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 30));
	}

	@Test
	public void testFindOrdersOfAClientWhenClientNoExistingInDB() {
		Client client1 = new Client("CLIENT-00001", "first client");
		orderRepository.save(new Order("ORDER-00001", client1,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10));
		orderRepository.save(new Order("ORDER-00002", new Client("CLIENT-00002", "second client"),
				Date.from(LocalDate.of(2025, 1, 2).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20));
		orderRepository.save(new Order("ORDER-00003", new Client("CLIENT-00001", "first client"),
				Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 30));
		clientRepository.delete("CLIENT-00001");
		try {
			orderService.allOrdersByClient(client1);
			fail("Expected Not Client FoundException to be thrown");

		} catch (NotFoundClientException e) {
			assertThat("Il cliente con id " + client1.getIdentifier() + " non è presente nel database")
					.isEqualTo(e.getMessage());
		}
	}

	@Test
	public void testFindAllOrders() {
		orderRepository.save(new Order("ORDER-00001", new Client("CLIENT-00001", "first client"),
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10));
		orderRepository.save(new Order("ORDER-00002", new Client("CLIENT-00002", "second client"),
				Date.from(LocalDate.of(2025, 1, 2).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20));
		orderRepository.save(new Order("ORDER-00003", new Client("CLIENT-00001", "first client"),
				Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 30));
		List<Order> orders = orderService.findAllOrders();
		assertThat(orders).containsExactly(
				new Order("ORDER-00001", new Client("CLIENT-00001", "first client"),
						Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10),
				new Order("ORDER-00002", new Client("CLIENT-00002", "second client"),
						Date.from(LocalDate.of(2025, 1, 2).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20),
				new Order("ORDER-00003", new Client("CLIENT-00001", "first client"),
						Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 30));
	}

}
