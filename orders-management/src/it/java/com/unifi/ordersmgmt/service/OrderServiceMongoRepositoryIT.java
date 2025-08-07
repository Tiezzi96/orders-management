package com.unifi.ordersmgmt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.unifi.ordersmgmt.exception.NotFoundClientException;
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
		}
	}

}
