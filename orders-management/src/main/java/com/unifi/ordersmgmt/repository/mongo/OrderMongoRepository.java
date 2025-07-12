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
import com.mongodb.client.model.Filters;
import com.unifi.ordersmgmt.model.Client;
import com.unifi.ordersmgmt.model.Order;
import com.unifi.ordersmgmt.repository.ClientRepository;
import com.unifi.ordersmgmt.repository.OrderRepository;

public class OrderMongoRepository implements OrderRepository {

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
		this.seqGen = seqGen;

	}

	@Override
	public List<Order> findAll() {
		// TODO Auto-generated method stub
		return StreamSupport.stream(orderCollection.find(clientSession).spliterator(), false)
				.map(d -> new Order(d.get("id").toString(),
						clientMongoRepository.findById(((DBRef) d.get("client")).getId().toString()), d.getDate("date"),
						d.getDouble("price")))
				.collect(Collectors.toList());
	}

	@Override
	public Order findById(String id) {
		// TODO Auto-generated method stub
		Document docFound = orderCollection.find(clientSession, Filters.eq("id", id)).first();
		if (docFound == null) {
			return null;
		}
		return new Order(docFound.get("id").toString(),
				clientMongoRepository.findById(((DBRef) docFound.get("client")).getId().toString()),
				docFound.getDate("date"), docFound.getDouble("price"));
	}

	@Override
	public Order save(Order obj) {
		// TODO Auto-generated method stub
		if (obj.getIdentifier().trim().isEmpty()) {
			obj.setIdentifier(seqGen.generateCodiceCliente(clientSession));
			System.out.println("Order id" + obj.getIdentifier());
		}
		Document docToInsert = new Document().append("id", obj.getIdentifier())
				.append("client", new DBRef("client", obj.getClient().getIdentifier())).append("date", obj.getDate())
				.append("price", obj.getPrice());
		orderCollection.insertOne(clientSession, docToInsert);
		Document docInserted = orderCollection.find(clientSession, Filters.eq("id", obj.getIdentifier().toString()))
				.first();
		System.out.println("DOC INSERTED: " + docInserted);
		Order orderInserted = new Order(docInserted.get("id").toString(),
				clientMongoRepository.findById(((DBRef) docInserted.get("client")).getId().toString()),
				docInserted.getDate("date"), docInserted.getDouble("price"));
		return orderInserted;
	}

	@Override
	public Order delete(String id) {
		// TODO Auto-generated method stub
		Order orderToDelete = findById(id);
		if (orderToDelete != null) {
			orderCollection.deleteOne(clientSession, Filters.eq("id", orderToDelete.getIdentifier()));
			return orderToDelete;
		}
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
