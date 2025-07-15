package com.unifi.ordersmgmt.repository.mongo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.unifi.ordersmgmt.model.Client;
import com.unifi.ordersmgmt.repository.ClientRepository;

public class ClientMongoRepository implements ClientRepository {

	private static final Logger logger = LogManager.getLogger(ClientMongoRepository.class); 
	private ClientSession clientSession;
	private MongoCollection<Document> clientCollection;
	private ClientSequenceGenerator seqGen;

	public ClientMongoRepository(MongoClient mongoClient, ClientSession clientSession, String dbName,
			String collectionClientName, ClientSequenceGenerator seqGen) {
		MongoDatabase db = mongoClient.getDatabase(dbName);
		if (!db.listCollectionNames().into(new ArrayList<String>()).contains(collectionClientName)) {
			db.createCollection(collectionClientName);
		}
		clientCollection = db.getCollection(collectionClientName);

		this.clientSession = clientSession;
		this.seqGen = seqGen;
	}

	public MongoCollection<Document> getClientCollection() {
		return clientCollection;
	}

	@Override
	public List<Client> findAll() {
		// TODO Auto-generated method stub
		List<Client> clients = StreamSupport.stream(clientCollection.find(clientSession).spliterator(), false)
				.map(d -> new Client(d.getString("id"), d.getString("name"))).collect(Collectors.toList());
		return clients;
	}

	@Override
	public Client findById(String id) {
		// TODO Auto-generated method stub
		Document d = clientCollection.find(clientSession, Filters.eq("id", id)).first();
		if (d != null) {
			return new Client(d.getString("id"), d.getString("name"));
		}
		return null;
	}

	@Override
	public Client save(Client clientToSave) {
		// TODO Auto-generated method stub
		if (clientToSave.getIdentifier() == null) {
			clientToSave.setIdentifier(seqGen.generateCodiceCliente(clientSession));
		}
		logger.info("CLIENT TO SAVE: {}", clientToSave);
		Document doc = new Document().append("id", clientToSave.getIdentifier()).append("name", clientToSave.getName());
		clientCollection.insertOne(clientSession, doc);
		Client saved = new Client(doc.getString("id"), doc.getString("name"));
		return saved;
	}

	@Override
	public Client delete(String idToDelete) {
		Client clientToDelete = findById(idToDelete);
		
		if(clientToDelete!=null) {
			clientCollection.deleteOne(clientSession, Filters.eq("id", idToDelete));
			return clientToDelete;
			
		}
		return null;
	}

}
