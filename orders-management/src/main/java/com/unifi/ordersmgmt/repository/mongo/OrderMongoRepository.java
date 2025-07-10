package com.unifi.ordersmgmt.repository.mongo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.bson.Document;

import com.mongodb.DBRef;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.unifi.ordersmgmt.model.Client;
import com.unifi.ordersmgmt.model.Order;
import com.unifi.ordersmgmt.repository.ClientRepository;
import com.unifi.ordersmgmt.repository.OrderRepository;

public class OrderMongoRepository implements OrderRepository{

	private ClientSession clientSession;
	private MongoCollection<Document> orderCollection;

	private ClientRepository clientMongoRepository;
	private OrderSequenceGenerator seqGen;

	public OrderMongoRepository(MongoClient mongoClient, ClientSession clientSession, String DBName,
			String collectionOrderName, ClientRepository clientMongoRepository, OrderSequenceGenerator seqGen) {
		this.clientMongoRepository = clientMongoRepository;
		this.clientSession = clientSession;
		MongoDatabase database = mongoClient.getDatabase(DBName);
		if (!database.listCollectionNames().into(new ArrayList<String>()).contains(collectionOrderName)) {
			database.createCollection(collectionOrderName);
		}
		orderCollection = database.getCollection(collectionOrderName);
		this.seqGen =seqGen;

	}

	@Override
	public List<Order> findAll() {
		// TODO Auto-generated method stub
		return StreamSupport.stream(orderCollection.find(clientSession).spliterator(), false)
				.map(d -> new Order(d.get("id").toString(), clientMongoRepository.findById(((DBRef) d.get("client")).getId().toString()),
						d.getDate("date"), d.getDouble("price")))
				.collect(Collectors.toList());
	}

	@Override
	public Order findById(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Order save(Order obj) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Order delete(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Order> findOrderByYear(int year) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Integer> getYearsOfOrders() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Order> removeOrdersByClient(String clientId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Order updateOrder(String orderID, Map<String, Object> updates) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Order> findOrdersByClientAndYear(Client client, int year) {
		// TODO Auto-generated method stub
		return null;
	}

}
