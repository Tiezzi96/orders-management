package com.unifi.ordersmgmt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

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
import com.unifi.ordersmgmt.transaction.mongo.MongoTransactionManager;

public class ClientServiceMongoRepositoryIT {
	private MongoClient mongoclient;
	private ClientService clientService;
	private ClientMongoRepository clientRepository;
	private OrderMongoRepository orderRepository;

	@Before
	public void setUp() {
		mongoclient = MongoClients.create("mongodb://localhost:27017/?replicaSet=rs0");
		TransactionManager transactionManager = new MongoTransactionManager(mongoclient, "client", "order", "budget");
		clientService = new TransactionalClientService(transactionManager);
		ClientSession session = mongoclient.startSession();
		ClientSequenceGenerator clientSeqGen = new ClientSequenceGenerator(mongoclient, "budget");
		clientRepository = new ClientMongoRepository(mongoclient, session, "budget", "client", clientSeqGen);
		OrderSequenceGenerator seqGen = new OrderSequenceGenerator(mongoclient, "budget");
		orderRepository = new OrderMongoRepository(mongoclient, session, "budget", "order", clientRepository, seqGen);
		for (Client client : clientRepository.findAll()) {
			clientRepository.delete(client.getIdentifier());
		}
		for (Order order : orderRepository.findAll()) {
			orderRepository.delete(order.getIdentifier());
		}

	}

	@After
	public void tearDown() {
		mongoclient.close();
	}

	@Test
	public void testAddClient() {
		Client newClient = new Client("1", "test id");
		clientService.saveClient(newClient);
		assertThat(clientRepository.findAll()).containsExactly(newClient);
	}

	@Test
	public void testFindAllClients() {
		Client newClient = new Client("CLIENT-00001", "client id");

		Client secondClient = new Client("CLIENT-00002", "second client id");
		clientService.saveClient(newClient);
		clientService.saveClient(secondClient);
		List<Client> clients = clientService.findAllClients();
		assertThat(clients).contains(newClient, secondClient);
	}
	
	@Test
	public void testRemoveClientWhenClientExistingInDatabase() {
		Client clientToRemove=clientRepository.save(new Client("CLIENT-00001", "CLIENT 1"));
		clientRepository.save(new Client("CLIENT-00002", "CLIENT 2"));
		clientService.removeClient(clientToRemove);
		Client clientFound=clientRepository.findById(clientToRemove.getIdentifier());
		assertThat(clientFound).isNull();
	}
	
	@Test
	public void testRemoveClientWhenClienNotExistingInDatabase() {
		Client clientNotExistInDB = new Client("CLIENT-00001", "client not exist in db");
		try {
			clientService.removeClient(clientNotExistInDB);
			fail("Excpected a ClientNotFoundException to be thrown");
		}
		catch(NotFoundClientException e) {
			assertThat("Il cliente con id "+clientNotExistInDB.getIdentifier()+" non è presente nel database")
					.isEqualTo(e.getMessage());
		}	
	}

}
