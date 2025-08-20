package com.unifi.ordersmgmt.repository.mongo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

	private static final Logger logger = LogManager.getLogger(OrderMongoRepositoryTest.class);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		mongo.start();
		// inizializza replica set
		mongo.execInContainer("/bin/bash", "-c", "mongo --eval 'rs.initiate()' --quiet");

		// Attendi finché il nodo non diventa primary
		mongo.execInContainer("/bin/bash", "-c",
				"until mongo --eval 'rs.isMaster()' | grep ismaster | grep true > /dev/null 2>&1; do sleep 1; done");
		logger.info("Replica set URL: {}", mongo.getReplicaSetUrl());
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
	public void tearDown() {
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
		logger.info("cod1: {}", cod1);
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
		logger.debug("Orders contained in DB: {}", orders);
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
		logger.info("cod1: {}", cod1);
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
		logger.info("new Order: {}", newOrder);
		when(seqGen.generateCodiceCliente(session)).thenReturn("ORDER-00001");
		when(clientMongoRepository.findById("CLIENT-00001")).thenReturn(newClient);
		Order orderSaved = orderRepository.save(newOrder);
		assertThat(orderSaved)
				.isEqualTo(new Order(cod1, newOrder.getClient(), newOrder.getDate(), newOrder.getPrice()));
		assertThat(orderSaved.getIdentifier()).isNotNull();
		List<Order> ordersInDatabase = getAllOrdersFromDB();
		logger.debug("Orders in Database: {}", ordersInDatabase);
		assertThat(ordersInDatabase).containsExactly(new Order(cod1, newClient, newOrder.getDate(), 10.0));
	}

	@Test
	public void testSaveWhenOrderIdIsEmpty() {
		String cod1 = "ORDER-00001";
		Client newClient = new Client("CLIENT-00001", "firstClient");
		Order newOrder = new Order(cod1, newClient, new Date(), 10.0);
		logger.info("new Order: {}", newOrder);
		when(clientMongoRepository.findById("CLIENT-00001")).thenReturn(newClient);
		Order orderSaved = orderRepository.save(newOrder);
		assertThat(orderSaved)
				.isEqualTo(new Order(cod1, newOrder.getClient(), newOrder.getDate(), newOrder.getPrice()));
		assertThat(orderSaved.getIdentifier()).isNotNull();
		List<Order> ordersInDatabase = getAllOrdersFromDB();
		logger.debug("Orders in Database: {}", ordersInDatabase);
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
		Order orderDeleted = orderRepository.delete(orderID);
		List<Order> ordersInDatabase = getAllOrdersFromDB();
		assertThat(orderDeleted).isNotNull();
		assertThat(ordersInDatabase).isEmpty();

	}

	@Test
	public void testFindOrdersByYearWhenDBIsEmpty() {
		List<Order> ordersOfYearSelected = orderRepository.findOrderByYear(2025);
		assertThat(ordersOfYearSelected).isEmpty();
	}

	@Test
	public void testFindOrdersByYearWhenDBContainsOrdersOfTheYearSelected() {
		insertNewOrderInDB(new Client("CLIENT-00001", "firstClient"), new Date(), 10.0, 1);
		Date currentDate = new Date(); // Data corrente
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(currentDate);
		calendar.add(Calendar.DAY_OF_MONTH, -1); // Rimuovi 1 giorno
		Date previousDate = calendar.getTime();
		insertNewOrderInDB(new Client("CLIENT-00001", "firstClient"), previousDate, 20.0, 2);
		when(clientMongoRepository.findById("CLIENT-00001")).thenReturn(new Client("CLIENT-00001", "firstClient"));
		List<Order> ordersOfYearSelected = orderRepository.findOrderByYear(2025);
		logger.debug("Orders of Year Selected: {}", ordersOfYearSelected);

		assertThat(ordersOfYearSelected).containsExactly(
				new Order("ORDER-00001", new Client("CLIENT-00001", "firstClient"), currentDate, 10.0),
				new Order("ORDER-00002", new Client("CLIENT-00001", "firstClient"), previousDate, 20.0));
	}

	@Test
	public void testFindOrdersByYearWhenDBContainsOrdersOfDifferentYears() {
		Date currentDate = new Date(); // Data corrente
		insertNewOrderInDB(new Client("CLIENT-00001", "firstClient"), currentDate, 10.0, 1);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(currentDate);
		calendar.add(Calendar.YEAR, -1); // Rimuovi 1 giorno
		Date previousDate = calendar.getTime();
		logger.info("previous Date : {}", previousDate);

		insertNewOrderInDB(new Client("CLIENT-00001", "firstClient"), previousDate, 20.0, 1);
		when(clientMongoRepository.findById("CLIENT-00001")).thenReturn(new Client("CLIENT-00001", "firstClient"));
		List<Order> ordersOfYearSelected = orderRepository.findOrderByYear(2025);

		logger.debug("Orders of Year Selected: {}", ordersOfYearSelected);
		assertThat(ordersOfYearSelected).containsExactly(
				new Order("ORDER-00001", new Client("CLIENT-00001", "firstClient"), currentDate, 10.0));

	}

	@Test
	public void testFindOrdersByYearWhenDBContainsOrdersOfLimitsOfYears() {
		LocalDate initCurrentYear = LocalDate.of(2025, 1, 1);
		Date firstDayCurrentYear = Date.from(initCurrentYear.atStartOfDay(ZoneId.systemDefault()).toInstant());
		LocalDate finishCurrentYear = LocalDate.of(2025, 12, 31);
		Date lastDayCurrentYear = Date.from(finishCurrentYear.atStartOfDay(ZoneId.systemDefault()).toInstant());
		LocalDate initPreviousYear = LocalDate.of(2024, 1, 1);
		Date firstDayPreviousYear = Date.from(initPreviousYear.atStartOfDay(ZoneId.systemDefault()).toInstant());
		LocalDate finishPreviousYear = LocalDate.of(2024, 12, 31);
		Date lastDayPreviousYear = Date.from(finishPreviousYear.atStartOfDay(ZoneId.systemDefault()).toInstant());
		insertNewOrderInDB(new Client("CLIENT-00001", "firstClient"), firstDayCurrentYear, 10.0, 1);
		insertNewOrderInDB(new Client("CLIENT-00001", "firstClient"), lastDayCurrentYear, 20.0, 2);
		insertNewOrderInDB(new Client("CLIENT-00001", "firstClient"), firstDayPreviousYear, 30.0, 3);
		insertNewOrderInDB(new Client("CLIENT-00001", "firstClient"), lastDayPreviousYear, 40.0, 3);
		when(clientMongoRepository.findById("CLIENT-00001")).thenReturn(new Client("CLIENT-00001", "firstClient"));
		List<Order> ordersOfYearSelected = orderRepository.findOrderByYear(2025);

		logger.debug("Orders of Year Selected: {}", ordersOfYearSelected);
		assertThat(ordersOfYearSelected).containsExactly(
				new Order("ORDER-00001", new Client("CLIENT-00001", "firstClient"), firstDayCurrentYear, 10.0),
				new Order("ORDER-00002", new Client("CLIENT-00001", "firstClient"), lastDayCurrentYear, 20.0));
	}

	@Test
	public void testGetYearsOfOrderWhenDBIsEmpty() {
		List<Integer> years = orderRepository.getYearsOfOrders();
		assertThat(years).isEmpty();
	}

	@Test
	public void testGetYearsOfOrderWhenDBIsNotEmpty() {
		insertNewOrderInDB(new Client("CLIENT-00001", "firstClient"), new Date(), 10.0, 1);
		Date currentDate = new Date(); // Data corrente
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(currentDate);
		calendar.add(Calendar.YEAR, -1); // Rimuovi 1 giorno
		Date previousDate = calendar.getTime();
		calendar.add(Calendar.YEAR, -1);
		Date twoYearsAgoDate = calendar.getTime();
		logger.info("revious date: {}", previousDate);
		insertNewOrderInDB(new Client("CLIENT-00001", "firstClient"), previousDate, 20.0, 1);
		insertNewOrderInDB(new Client("CLIENT-00001", "firstClient"), twoYearsAgoDate, 30.0, 2);

		List<Integer> years = orderRepository.getYearsOfOrders();
		assertThat(years).containsExactly(2023, 2024, 2025);
	}

	@Test
	public void testFindOrderByClientAndYearWhenDBContainsOnlyOrdersOfClientAndYearSelected() {
		Client newClient = new Client("CLIENT-00001", "new Client");
		Date date1 = new Date();
		Date date2 = new Date();

		insertNewOrderInDB(newClient, date1, 10, 1);
		insertNewOrderInDB(newClient, date2, 20.5, 2);
		when(clientMongoRepository.findById("CLIENT-00001")).thenReturn(newClient);
		List<Order> years = orderRepository.findOrdersByClientAndYear(newClient, 2025);

		assertThat(years).containsExactly(new Order("ORDER-00001", newClient, date1, 10),
				new Order("ORDER-00002", newClient, date2, 20.5));
	}

	@Test
	public void testFindOrderByClientAndYearWhenDBContainsOrdersOfClientSelectedAndDifferentYears() {
		Client newClient = new Client("CLIENT-00001", "new Client");
		Date date1 = new Date();
		Date date2 = new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date2);
		calendar.add(Calendar.YEAR, -1);
		date2 = calendar.getTime();
		insertNewOrderInDB(newClient, date1, 10, 1);
		insertNewOrderInDB(newClient, date2, 20.5, 2);
		when(clientMongoRepository.findById("CLIENT-00001")).thenReturn(newClient);
		List<Order> years = orderRepository.findOrdersByClientAndYear(newClient, 2025);

		assertThat(years).containsExactly(new Order("ORDER-00001", newClient, date1, 10));
	}

	@Test
	public void testFindOrderByClientAndYearWhenDBContainsOrdersOfDifferentClientAndYearSelected() {
		Client clientSelected = new Client("CLIENT-00001", "first Client");
		Client secondClient = new Client("CLIENT-00002", "second Client");
		Date date1 = new Date();
		Date date2 = new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date2);
		calendar.add(Calendar.YEAR, -1);
		date2 = calendar.getTime();
		insertNewOrderInDB(clientSelected, date1, 10, 1);
		insertNewOrderInDB(secondClient, date2, 20.5, 2);
		when(clientMongoRepository.findById("CLIENT-00001")).thenReturn(clientSelected);

		when(clientMongoRepository.findById("CLIENT-00002")).thenReturn(secondClient);
		List<Order> years = orderRepository.findOrdersByClientAndYear(clientSelected, 2025);

		assertThat(years).containsExactly(new Order("ORDER-00001", clientSelected, date1, 10));
	}

	@Test
	public void testFindOrderByClientAndYearWhenDBContainsOnlyOrdersOfDifferentClientsAndYears() {
		Client clientSelected = new Client("CLIENT-00001", "first Client");
		Client secondClient = new Client("CLIENT-00002", "second Client");
		Client thirdClient = new Client("CLIENT-00003", "third Client");

		Date date1 = new Date();
		Date date2 = new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date2);
		calendar.add(Calendar.YEAR, -1);
		date2 = calendar.getTime();
		insertNewOrderInDB(secondClient, date1, 10, 1);
		insertNewOrderInDB(thirdClient, date2, 20.5, 2);
		when(clientMongoRepository.findById("CLIENT-00001")).thenReturn(clientSelected);

		when(clientMongoRepository.findById("CLIENT-00002")).thenReturn(secondClient);

		when(clientMongoRepository.findById("CLIENT-00003")).thenReturn(thirdClient);
		List<Order> years = orderRepository.findOrdersByClientAndYear(clientSelected, 2025);

		assertThat(years).isEmpty();
	}

	@Test
	public void testDeleteOrdersByClient() {
		Client newClient = new Client("CLIENT-00001", "new Client");
		Client secondClient = new Client("CLIENT-00002", "second Client");
		Date date1 = new Date();
		Date date2 = new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date2);
		calendar.add(Calendar.YEAR, -1);
		date2 = calendar.getTime();
		insertNewOrderInDB(newClient, date1, 10, 1);
		insertNewOrderInDB(secondClient, date2, 20.5, 2);
		insertNewOrderInDB(newClient, date2, 15, 3);
		when(clientMongoRepository.findById(newClient.getIdentifier())).thenReturn(newClient);
		List<Order> ordersRemoved = orderRepository.removeOrdersByClient(newClient.getIdentifier());

		assertThat(ordersRemoved).isNotEmpty();

		List<Order> ordersRemainInDB = getAllOrdersFromDB();
		assertThat(ordersRemainInDB).containsExactly(new Order("ORDER-00002", secondClient, date2, 20.5));
	}

	@Test
	public void testDeleteOrdersByClientWhenClientIsNotInDB() {
		Client newClient = new Client("CLIENT-00001", "new Client");
		Client secondClient = new Client("CLIENT-00002", "second Client");
		Date date1 = new Date();
		Date date2 = new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date2);
		calendar.add(Calendar.YEAR, -1);
		date2 = calendar.getTime();
		insertNewOrderInDB(newClient, date1, 10, 1);
		insertNewOrderInDB(newClient, date2, 15, 2);
		when(clientMongoRepository.findById(secondClient.getIdentifier())).thenReturn(secondClient);
		List<Order> ordersRemoved = orderRepository.removeOrdersByClient(secondClient.getIdentifier());
		List<Order> ordersRemainInDB = getAllOrdersFromDB();
		assertThat(ordersRemoved).isEmpty();
		assertThat(ordersRemainInDB).containsExactly(new Order("ORDER-00001", newClient, date1, 10),
				new Order("ORDER-00002", newClient, date2, 15));
	}

	@Test
	public void testFindOrderByClientAndYearWhenDBIsEmpty() {
		Client newClient = new Client("CLIENT-00001", "new Client");

		List<Order> years = orderRepository.findOrdersByClientAndYear(newClient, 2025);
		assertThat(years).isEmpty();
	}

	@Test
	public void testModifyOrderWhenOrderExistInDB() {
		Client newClient = new Client("CLIENT-00001", "new Client");
		Date date1 = new Date();
		String orderID = insertNewOrderInDB(newClient, date1, 10, 1);
		Map<String, Object> updates = new HashMap<String, Object>();
		Date date2 = new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date2);
		calendar.add(Calendar.YEAR, -1);
		date2 = calendar.getTime();
		updates.put("date", date2);
		updates.put("price", 20.5);
		when(clientMongoRepository.findById("CLIENT-00001")).thenReturn(newClient);
		Order orderModified = orderRepository.updateOrder(orderID, updates);
		assertThat(orderModified).isEqualTo(new Order(orderID, newClient, (Date) updates.get("date"),
				Double.valueOf(updates.get("price").toString())));

	}

	@Test
	public void testModifyClientOfTheOrderWhenOrderExistInDB() {
		Client newClient = new Client("CLIENT-00001", "new Client");
		Client secondClient = new Client("CLIENT-00002", "second Client");
		Date date1 = new Date();
		String orderID = insertNewOrderInDB(newClient, date1, 10, 1);
		Map<String, Object> updates = new HashMap<String, Object>();
		updates.put("client", secondClient);
		when(clientMongoRepository.findById("CLIENT-00001")).thenReturn(newClient);
		when(clientMongoRepository.findById("CLIENT-00002")).thenReturn(secondClient);
		Order orderModified = orderRepository.updateOrder(orderID, updates);
		assertThat(orderModified).isEqualTo(new Order(orderID, secondClient, date1, 10));

	}

	@Test
	public void testModifyOrderWhenOrderNotExistInDB() {
		String orderID = "ORDER-00001";
		Map<String, Object> updates = new HashMap<String, Object>();
		Order orderModified = orderRepository.updateOrder(orderID, updates);
		assertThat(orderModified).isNull();

	}

	private List<Order> getAllOrdersFromDB() {
		return StreamSupport.stream(orderCollection.find().spliterator(), false)
				.map(d -> new Order(d.getString("id"),
						new Client(((DBRef) d.get("client")).getId().toString(), "firstClient"), (Date) d.get("date"),
						d.getDouble("price")))
				.collect(Collectors.toList());
	}

	private String insertNewOrderInDB(Client client, Date date, double price, int index) {
		String orderID = String.format("ORDER-0000%d", index);
		Document orderToInsert = new Document().append("id", orderID)
				.append("client", new DBRef("client", client.getIdentifier())).append("date", date)
				.append("price", price);
		logger.debug("Doc to insert: {}", orderToInsert);
		orderCollection.insertOne(orderToInsert);
		return orderID;
	}
	
	@Test
	public void testFindOrdersByClientWhenDBIsEmpty() {
		List<Order> ordersOfClientSelected = orderRepository
				.findOrdersByClient(new Client("CLIENT-00001", "firstClient"));
		assertThat(ordersOfClientSelected).isEmpty();
	}

	@Test
	public void testFindOrdersByClientWhenDBContainsOrdersOfTheClientSelected() {
		insertNewOrderInDB(new Client("CLIENT-00001", "firstClient"), new Date(), 10.0, 1);
		Date currentDate = new Date(); // Data corrente
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(currentDate);
		calendar.add(Calendar.DAY_OF_MONTH, -1); // Rimuovi 1 giorno
		Date previousDate = calendar.getTime();
		insertNewOrderInDB(new Client("CLIENT-00001", "firstClient"), previousDate, 20.0, 2);
		insertNewOrderInDB(new Client("CLIENT-00002", "secondClient"), previousDate, 20.0, 3);
		when(clientMongoRepository.findById("CLIENT-00001")).thenReturn(new Client("CLIENT-00001", "firstClient"));
		when(clientMongoRepository.findById("CLIENT-00002")).thenReturn(new Client("CLIENT-00002", "secondClient"));
		List<Order> ordersOfClientSelected = orderRepository
				.findOrdersByClient(new Client("CLIENT-00001", "firstClient"));
		System.out.println(ordersOfClientSelected);
		assertThat(ordersOfClientSelected).containsExactly(
				new Order("ORDER-00001", new Client("CLIENT-00001", "firstClient"), currentDate, 10.0),
				new Order("ORDER-00002", new Client("CLIENT-00001", "firstClient"), previousDate, 20.0));
	}

}
