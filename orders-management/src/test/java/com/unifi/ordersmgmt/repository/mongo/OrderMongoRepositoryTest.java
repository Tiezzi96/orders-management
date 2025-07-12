package com.unifi.ordersmgmt.repository.mongo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testcontainers.containers.MongoDBContainer;

import com.mongodb.DBRef;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.unifi.ordersmgmt.model.Client;
import com.unifi.ordersmgmt.model.Order;

public class OrderMongoRepositoryTest {

	@ClassRule
	@SuppressWarnings("resource")
	public static final MongoDBContainer mongo = new MongoDBContainer("mongo:4.4.3").withExposedPorts(27017)
			.withCommand("--replSet rs0");
	@Mock
	public ClientMongoRepository clientMongoRepository;

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
	private OrderMongoRepository orderRepository;

	private MongoCollection<Document> orderCollection;
	@Mock
	private OrderSequenceGenerator seqGen;

	private ClientSession session;

	@Before
	public void setup() {
		mongoClient = MongoClients.create(mongo.getReplicaSetUrl());
		session = mongoClient.startSession();
		MongoDatabase database = mongoClient.getDatabase("budget");
		database.drop();
		mongoClient.getDatabase("budget").createCollection("client");
		MockitoAnnotations.openMocks(this); // Inizializza i mock
		orderRepository = new OrderMongoRepository(mongoClient, session, "budget", "order", clientMongoRepository,
				seqGen);
		orderCollection = database.getCollection("order");

	}

	@After
	public void tearDown() throws Exception {
		mongoClient.close();
		session.close();
	}

	@Test
	public void testCreateClientCollectionIfNotExistingInDatabase() {
		String orderCollectionNotExisting = "order_collection_not_existing_in_db";
		orderRepository = new OrderMongoRepository(mongoClient, mongoClient.startSession(), "budget",
				orderCollectionNotExisting, clientMongoRepository, seqGen);
		assertThat(mongoClient.getDatabase("budget").listCollectionNames()).contains(orderCollectionNotExisting);
	}

	@Test
	public void testNotCreateClientCollectionIfExistingInDatabase() {
		String orderCollectionExisting = "order";
		MongoDatabase db = mongoClient.getDatabase("budget");
		List<String> previousCollectionsDB = new ArrayList<String>();
		for (String string : db.listCollectionNames()) {
			previousCollectionsDB.add(string);
		}
		orderRepository = new OrderMongoRepository(mongoClient, mongoClient.startSession(), "budget",
				orderCollectionExisting, clientMongoRepository, seqGen);
		assertThat(mongoClient.getDatabase("budget").listCollectionNames()).contains(orderCollectionExisting);
		List<String> nextCollectionsDB = new ArrayList<String>();
		for (String string : mongoClient.getDatabase("budget").listCollectionNames()) {
			nextCollectionsDB.add(string);
		}
		assertThat(nextCollectionsDB).isEqualTo(previousCollectionsDB);

	}

	@Test
	public void testFindAllOrdersWhenListIsEmpty() {
		List<Order> orders = orderRepository.findAll();
		assertThat(orders).isEmpty();
	}

	@Test
	public void testFindAllOrdersWhenDBIsNotEmpty() {
		String cod1 = "ORDER-00001";
		String cod2 = "ORDER-00002";
		System.out.println("cod1: " + cod1);
		Client client = new Client("CLIENT-00001", "client");
		Order firstOrder = new Order(cod1, client, new Date(), 10);
		Order secondOrder = new Order(cod2, client, new Date(), 20);
		when(clientMongoRepository.findById(client.getIdentifier())).thenReturn(client);
		Document firstOrderDoc = new Document().append("id", firstOrder.getIdentifier())
				.append("client", new DBRef("client", firstOrder.getClient().getIdentifier()))
				.append("date", firstOrder.getDate()).append("price", firstOrder.getPrice());
		Document secondOrderDoc = new Document().append("id", secondOrder.getIdentifier())
				.append("client", new DBRef("client", secondOrder.getClient().getIdentifier()))
				.append("date", secondOrder.getDate()).append("price", secondOrder.getPrice());
		orderCollection.insertOne(firstOrderDoc);
		orderCollection.insertOne(secondOrderDoc);
		List<Order> orders = orderRepository.findAll();
		System.out.println(orders);
		assertThat(orders).containsExactly(firstOrder, secondOrder);
	}
	
	@Test
	public void testFindByIdNotFound() {
		Order orderFound = orderRepository.findById("ORDER-00001");
		assertThat(orderFound).isNull();
	}

	@Test
	public void testFindByIdIsFound() {
		String cod1 = "ORDER-00001";
		String cod2 = "ORDER-00002";
		System.out.println("cod1: " + cod1);
		Client firstClient = new Client("CLIENT-00001", "first client");
		when(clientMongoRepository.findById("CLIENT-00001")).thenReturn(firstClient);
		Order firstOrder = new Order(cod1, firstClient, new Date(), 10.0);
		Order secondOrder = new Order(cod2, firstClient, new Date(), 20.0);
		Document firstOrderDoc = new Document().append("id", firstOrder.getIdentifier())
				.append("client", new DBRef("client", firstClient.getIdentifier())).append("date", firstOrder.getDate())
				.append("price", firstOrder.getPrice());
		orderCollection.insertOne(firstOrderDoc);
		Document secondOrderDoc = new Document().append("id", secondOrder.getIdentifier())
				.append("client", new DBRef("client", firstClient.getIdentifier())).append("date", firstOrder.getDate())
				.append("price", secondOrder.getPrice());
		orderCollection.insertOne(secondOrderDoc);
		Order orderFound = orderRepository.findById(firstOrder.getIdentifier());
		assertThat(orderFound).isEqualTo(new Order(cod1, firstClient, firstOrder.getDate(), firstOrder.getPrice()));
	}
	
	@Test
	public void testSave() {
		String cod1 = "ORDER-00001";
		Client newClient = new Client("CLIENT-00001", "firstClient");
		Order newOrder = new Order("", newClient, new Date(), 10.0);
		System.out.println("new Order: " + newOrder);
		when(seqGen.generateCodiceCliente(session)).thenReturn("ORDER-00001");
		when(clientMongoRepository.findById("CLIENT-00001")).thenReturn(newClient);
		Order orderSaved = orderRepository.save(newOrder);
		assertThat(orderSaved)
				.isEqualTo(new Order(cod1, newOrder.getClient(), newOrder.getDate(), newOrder.getPrice()));
		assertThat(orderSaved.getIdentifier()).isNotNull();
		List<Order> ordersInDatabase = getAllOrdersFromDB();
		System.out.println(ordersInDatabase);
		assertThat(ordersInDatabase).containsExactly(new Order(cod1, newClient, newOrder.getDate(), 10.0));
	}
	@Test
	public void testSaveWhenOrderIdIsEmpty() {
		String cod1 = "ORDER-00001";
		Client newClient = new Client("CLIENT-00001", "firstClient");
		Order newOrder = new Order(cod1, newClient, new Date(), 10.0);
		System.out.println("new Order: " + newOrder);
		when(clientMongoRepository.findById("CLIENT-00001")).thenReturn(newClient);
		Order orderSaved = orderRepository.save(newOrder);
		assertThat(orderSaved)
				.isEqualTo(new Order(cod1, newOrder.getClient(), newOrder.getDate(), newOrder.getPrice()));
		assertThat(orderSaved.getIdentifier()).isNotNull();
		List<Order> ordersInDatabase = getAllOrdersFromDB();
		System.out.println(ordersInDatabase);
		assertThat(ordersInDatabase).containsExactly(new Order(cod1, newClient, newOrder.getDate(), 10.0));
	}

	@Test
	public void testDeleteWhenOrderNotExistInDB() {
		when(seqGen.generateCodiceCliente(session)).thenReturn("ORDER-00001");
		Order orderRemoved = orderRepository.delete(seqGen.generateCodiceCliente(session));
		assertThat(orderRemoved).isNull();

	}

	@Test
	public void testDeleteWhenOrderExistInDB() {
		String orderID = insertNewOrderInDB(new Client("CLIENT-00001", "firstClient"), new Date(), 10.0, 1);
		Date currentDate = new Date(); // Data corrente
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(currentDate);
		calendar.add(Calendar.YEAR, -1); // Rimuovi 1 anno
		Date previousDate = calendar.getTime();
		Order orderDeleted = orderRepository.delete(orderID);
		List<Order> ordersInDatabase = getAllOrdersFromDB();
		assertThat(orderDeleted).isNotNull();
		assertThat(ordersInDatabase).isEmpty();

	}
	
	private List<Order> getAllOrdersFromDB() {
		// TODO Auto-generated method stub
		return StreamSupport.stream(orderCollection.find().spliterator(), false)
				.map(d -> new Order(d.getString("id"),
						new Client(((DBRef) d.get("client")).getId().toString(), "firstClient"), (Date) d.get("date"),
						Double.valueOf(d.getDouble("price"))))
				.collect(Collectors.toList());
	}

	private String insertNewOrderInDB(Client client, Date date, double price, int index) {
		// TODO Auto-generated method stub
		String orderID = String.format("ORDER-0000%d", index);
		Document orderToInsert = new Document().append("id", orderID)
				.append("client", new DBRef("client", client.getIdentifier())).append("date", date)
				.append("price", price);
		System.out.println("doc" + orderToInsert);
		orderCollection.insertOne(orderToInsert);
		return orderID;
	}

}
