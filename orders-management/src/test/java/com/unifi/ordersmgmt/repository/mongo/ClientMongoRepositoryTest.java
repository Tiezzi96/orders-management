package com.unifi.ordersmgmt.repository.mongo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testcontainers.containers.MongoDBContainer;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.unifi.ordersmgmt.model.Client;

public class ClientMongoRepositoryTest {

	@ClassRule
	@SuppressWarnings("resource")
	public static final MongoDBContainer mongo = new MongoDBContainer("mongo:4.4.3").withExposedPorts(27017)
			.withCommand("--replSet rs0");

	private MongoClient mongoClient;
	private ClientMongoRepository clientRepository;
	private MongoCollection<Document> clientCollection;
	@Mock
	private ClientSequenceGenerator seqGen;
	private ClientSession startSession;

	@Before
	public void setup() {
		mongoClient = MongoClients.create(mongo.getReplicaSetUrl());
		MongoDatabase database = mongoClient.getDatabase("budget");
		database.drop();
		mongoClient.getDatabase("budget").createCollection("client");
		MockitoAnnotations.openMocks(this);
		startSession = mongoClient.startSession();
		clientRepository = new ClientMongoRepository(mongoClient, startSession, "budget", "client", seqGen);
		clientCollection = database.getCollection("client");

	}

	@After
	public void tearDown() throws Exception {
		mongoClient.close();
	}

	@BeforeClass
	public static void initialize() throws Exception {
		mongo.start();
		// Initialized replica set
		mongo.execInContainer("/bin/bash", "-c", "mongo --eval 'rs.initiate()' --quiet");

		// It waits until the node becomes primary by checking the isMaster field. The
		// until-do-done loop keeps running until isMaster is set to true.
		mongo.execInContainer("/bin/bash", "-c",
				"until mongo --eval 'rs.isMaster()' | grep ismaster | grep true > /dev/null 2>&1; do sleep 1; done");
		System.out.println("Replica set URL: " + mongo.getReplicaSetUrl());
	}

	@AfterClass
	public static void tearDownAfterClass() {
		if (mongo != null && mongo.isRunning()) {
			mongo.stop();

		}
	}
	
	@Test
	public void testCreateClientCollectionIfNotExistingInDatabase() {
		String clientCollectionNotExisting = "client_collection_not_existing_in_db";
		clientRepository = new ClientMongoRepository(mongoClient, mongoClient.startSession(), "budget",
				clientCollectionNotExisting, seqGen);
		assertThat(mongoClient.getDatabase("budget").listCollectionNames()).contains(clientCollectionNotExisting);
	}

	@Test
	public void testGetClientCollection() {
		assertThat(clientRepository.getClientCollection().getNamespace()).isEqualTo(clientCollection.getNamespace());
	}

	@Test
	public void testFindAllClientsWhenListIsEmpty() {
		List<Client> clients = clientRepository.findAll();
		assertThat(clients).isEmpty();
	}

	@Test
	public void testFindAllClientsWhenDBIsNotEmpty() {
		String cod1 = "CLIENT-00001";
		String cod2 = "CLIENT-00002";
		System.out.println("cod1: " + cod1);
		Client firstClient = new Client(cod1, "first client");
		Client secondClient = new Client(cod2, "second client");
		Document firstClientDoc = new Document().append("id", firstClient.getIdentifier()).append("name",
				firstClient.getName());
		clientCollection.insertOne(firstClientDoc);
		Document secondClientDoc = new Document().append("id", secondClient.getIdentifier()).append("name",
				secondClient.getName());
		clientCollection.insertOne(secondClientDoc);
		List<Client> clients = clientRepository.findAll();
		System.out.println(clients);
		assertThat(clients).containsExactly(firstClient, secondClient);
	}
	
	@Test
	public void testFindByIdNotFound() {
		Client clientFound = clientRepository.findById("CLIENT-00001");
		assertThat(clientFound).isNull();
	}

	@Test
	public void testFindByIdIsFound() {
		String cod1 = "CLIENT-00001";
		String cod2 = "CLIENT-00002";
		System.out.println("cod1: " + cod1);
		Client firstClient = new Client(cod1, "first client");
		Client secondClient = new Client(cod2, "second client");
		Document firstClientDoc = new Document().append("id", firstClient.getIdentifier()).append("name",
				firstClient.getName());
		clientCollection.insertOne(firstClientDoc);
		Document secondClientDoc = new Document().append("id", secondClient.getIdentifier()).append("name",
				secondClient.getName());
		clientCollection.insertOne(secondClientDoc);
		Client clientFound = clientRepository.findById(firstClient.getIdentifier());
		assertThat(clientFound).isEqualTo(new Client(cod1, "first client"));
	}

	@Test
	public void testSave() {
		String cod1 = "CLIENT-00001";
		Client newClient = new Client(cod1, "firstClient");
		Client clientSaved = clientRepository.save(newClient);
		assertThat(clientSaved).isEqualTo(new Client(cod1, "firstClient"));

		assertThat(clientSaved.getIdentifier()).isEqualTo(cod1);
		assertThat(clientSaved.getName()).isEqualTo("firstClient");

		assertThat(clientSaved.getIdentifier()).isNotNull();
		List<Client> clientsInDatabase = getAllClientsFromDB();
		assertThat(clientsInDatabase).containsExactly(new Client(cod1, "firstClient"));
	}

	@Test
	public void testSaveWhenIdentifierIsNull() {
		when(seqGen.generateCodiceCliente(startSession)).thenReturn("CLIENT-00001");
		Client newClient = new Client("firstClient");
		Client clientSaved = clientRepository.save(newClient);
		String cod1 = "CLIENT-00001";
		assertThat(clientSaved).isEqualTo(new Client(cod1, "firstClient"));

		assertThat(clientSaved.getIdentifier()).isEqualTo(cod1);
		assertThat(clientSaved.getName()).isEqualTo("firstClient");

		assertThat(clientSaved.getIdentifier()).isNotNull();
		List<Client> clientsInDatabase = getAllClientsFromDB();
		assertThat(clientsInDatabase).containsExactly(new Client(cod1, "firstClient"));
	}

	@Test
	public void testDeleteWhenClientNotExistInDB() {
		String cod1 = "CLIENT-00001";
		Client removed = clientRepository.delete(cod1);
		assertThat(removed).isNull();
	}

	@Test
	public void testDeleteWhenClientExistsInDB() {
		String cod1 = "CLIENT-00001";
		String cod2 = "CLIENT-00002";
		Client firstClient = new Client(cod1, "firstClient");
		Client secondClient = new Client(cod2, "secondClient");
		Document firstClientDoc = new Document().append("id", firstClient.getIdentifier()).append("name",
				firstClient.getName());
		clientCollection.insertOne(firstClientDoc);
		Document secondClientDoc = new Document().append("id", secondClient.getIdentifier()).append("name",
				secondClient.getName());
		clientCollection.insertOne(secondClientDoc);
		Client removed = clientRepository.delete(cod1);
		assertThat(removed).isEqualTo(new Client(cod1, "firstClient"));
		List<Client> clientsInDatabase = getAllClientsFromDB();
		assertThat(clientsInDatabase).containsExactly(new Client(cod2, "secondClient"));
	}

	private List<Client> getAllClientsFromDB() {
		return StreamSupport.stream(clientCollection.find().spliterator(), false)
				.map(d -> new Client(d.get("id").toString(), d.getString("name"))).collect(Collectors.toList());
	}

}
