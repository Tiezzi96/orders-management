package com.unifi.ordersmgmt.transaction.mongo;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.MongoDBContainer;

import com.mongodb.DBRef;
import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.unifi.ordersmgmt.model.Client;
import com.unifi.ordersmgmt.model.Order;

public class MongoTransactionManagerTest {

	@ClassRule
	@SuppressWarnings("resource")
	public static final MongoDBContainer mongo = new MongoDBContainer("mongo:4.4.3").withExposedPorts(27017)
			.withCommand("--replSet rs0");

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		mongo.start();
		// inizializza replica set
		mongo.execInContainer("/bin/bash", "-c", "mongo --eval 'rs.initiate()' --quiet");

		// Attendi finché il nodo non diventa primary
		mongo.execInContainer("/bin/bash", "-c",
				"until mongo --eval 'rs.isMaster()' | grep ismaster | grep true > /dev/null 2>&1; do sleep 1; done");
		System.out.println("Replica set URL: " + mongo.getReplicaSetUrl());
	}

	private MongoClient mongoClient;
	private MongoTransactionManager transactionManager;

	private MongoCollection<Document> clientCollection;
	private MongoCollection<Document> orderCollection;

	@Before
	public void setup() {
		mongoClient = MongoClients.create(mongo.getReplicaSetUrl());
		MongoDatabase database = mongoClient.getDatabase("budget");
		database.drop();
		database.createCollection("client");
		database.createCollection("order");

		clientCollection = database.getCollection("client");
		orderCollection = database.getCollection("order");
		transactionManager = new MongoTransactionManager(mongoClient, "client", "order", "budget");
	}

	@After
	public void tearDown() {
		mongoClient.close();
	}

	@Test
	public void testDoInTransaction() {
		Pair<List<String>, Date> result = transactionManager.executeTransaction((clientRepo, orderRepo) -> {
			ClientSession session = clientRepo.getSession();
			System.out.println("session: " + session);
			String clientID = insertNewClientInDB("new client", session);
			Date date = new Date();
			String orderID = insertNewOrderInDB(clientID, date, 10.0, session);

			return new ImmutablePair<List<String>, Date>(asList(clientID, orderID), date);

		});
		List<Client> clientsInDatabase = findAllClientsInDB();
		assertThat(clientsInDatabase).containsOnly(new Client(result.getKey().get(0), "new client"));
		List<Order> ordersInDatabase = findAllOrdersInDB();
		System.out.println("ordersInDB: " + ordersInDatabase);
		System.out.println("elementAdded: " + result);

		assertThat(ordersInDatabase).containsOnly(new Order(result.getKey().get(1),
				new Client(result.getKey().get(0), "new client"), result.getValue(), 10.0));
	}
	
	@Test
	public void testRollBack() {
		assertThatThrownBy(() -> transactionManager.executeTransaction((clientRepo, orderRepo) -> {
			ClientSession session = clientRepo.getSession();
			String clientID = insertNewClientInDB("new client", session);
			insertNewOrderInDB(clientID, new Date(), 10.0, session);
			throw new MongoException("error: abort transaction");
		})).isInstanceOf(MongoException.class);
		List<Client> clientsInDatabase = findAllClientsInDB();
		assertThat(clientsInDatabase).isEmpty();
		List<Order> ordersInDatabase = findAllOrdersInDB();
		assertThat(ordersInDatabase).isEmpty();
	}
	
	private List<Client> findAllClientsInDB() {
		return StreamSupport.stream(clientCollection.find().spliterator(), false)
				.map(d -> new Client(d.getString("id"), d.getString("name"))).collect(Collectors.toList());
	}

	private List<Order> findAllOrdersInDB() {
		return StreamSupport.stream(orderCollection.find().spliterator(), false).map(d -> new Order(d.getString("id"),
				findClientById(((DBRef) d.get("client")).getId().toString()), d.getDate("date"), d.getDouble("price")))
				.collect(Collectors.toList());
	}

	private Client findClientById(String string) {
		Document clientFound = clientCollection.find(Filters.eq("id", string)).first();
		if (clientFound != null) {
			return new Client(clientFound.getString("id"), clientFound.getString("name"));
		}
		return null;
	}

	private String insertNewClientInDB(String name, ClientSession session) {
		String clientID = "CLIENT-00001";
		Document clientToInsert = new Document().append("id", clientID).append("name", name);
		System.out.println("document Order" + clientToInsert);
		System.out.println("session passed" + session.toString());
		clientCollection.insertOne(session, clientToInsert);
		return clientID;

	}

	private String insertNewOrderInDB(String client, Date date, double price, ClientSession session) {
		String orderID = "ORDER-00001";
		Document orderToInsert = new Document().append("id", orderID).append("client", new DBRef("client", client))
				.append("date", date).append("price", price);
		System.out.println("document Order" + orderToInsert);
		orderCollection.insertOne(session, orderToInsert);
		return orderID;
	}
}
