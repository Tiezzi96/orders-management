package com.unifi.ordersmgmt.repository.mongo;

import static org.junit.Assert.*;

import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.MongoDBContainer;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class OrderSequenceGeneratorTest {

	private static final String DB_NAME = "testdb";
	private static final String COUNTER_COLLECTION = "counters";

	private MongoClient mongoClient;
	private MongoDatabase database;
	private OrderSequenceGenerator generator;

	@ClassRule
	@SuppressWarnings("resource")
	public static final MongoDBContainer mongo = new MongoDBContainer("mongo:4.4.3").withExposedPorts(27017)
			.withCommand("--replSet rs0");

	@Before
	public void setUp() {
		mongo.start();
		mongoClient = MongoClients.create(mongo.getConnectionString());
		database = mongoClient.getDatabase(DB_NAME);
		generator = new OrderSequenceGenerator(mongoClient, DB_NAME);
	}

	@After
	public void tearDown() {
		if (mongoClient != null) {
			mongoClient.close();
		}
		mongo.stop();

	}

	@Test
	public void testGenerateCodiceClienteIncrementsSequence() {
		try (ClientSession session = mongoClient.startSession()) {
			String codice1 = generator.generateCodiceCliente(session);
			String codice2 = generator.generateCodiceCliente(session);

			assertNotNull(codice1);
			assertNotNull(codice2);
			assertNotEquals(codice1, codice2);

			assertTrue(codice1.matches("ORDER-\\d{5}"));
			assertTrue(codice2.matches("ORDER-\\d{5}"));

			Document counterDoc = database.getCollection(COUNTER_COLLECTION).find().first();
			assertNotNull(counterDoc);
			Number seq = counterDoc.get("seq", Number.class);
			assertTrue(seq.longValue() >= 2);
		}
	}

	@Test
	public void testInitialSequenceStartsAtOne() {
		try (ClientSession session = mongoClient.startSession()) {
			String codice = generator.generateCodiceCliente(session);
			assertEquals("ORDER-00001", codice);
		}
	}

}
