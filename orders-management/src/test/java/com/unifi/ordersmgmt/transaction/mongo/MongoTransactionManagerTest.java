package com.unifi.ordersmgmt.transaction.mongo;

import static org.junit.Assert.*;

import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.MongoDBContainer;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

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
	private ClientSession clientSession;

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

}
