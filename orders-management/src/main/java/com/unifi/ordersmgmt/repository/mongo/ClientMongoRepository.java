package com.unifi.ordersmgmt.repository.mongo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.bson.Document;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.unifi.ordersmgmt.model.Client;
import com.unifi.ordersmgmt.repository.ClientRepository;

public class ClientMongoRepository implements ClientRepository {

	private ClientSession clientSession;
	private MongoCollection<Document> clientCollection;
	private ClientSequenceGenerator seqGen;

	public ClientMongoRepository(MongoClient mongoClient, ClientSession clientSession, String DBName,
			String CollectionClientName, ClientSequenceGenerator seqGen) {
		MongoDatabase db = mongoClient.getDatabase(DBName);
		if (!db.listCollectionNames().into(new ArrayList<String>()).contains(CollectionClientName)) {
			db.createCollection(CollectionClientName);
		}
		clientCollection = db.getCollection(CollectionClientName);

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
		return null;
	}

	@Override
	public Client save(Client obj) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Client delete(String id) {
		// TODO Auto-generated method stub
		return null;
	}

}
