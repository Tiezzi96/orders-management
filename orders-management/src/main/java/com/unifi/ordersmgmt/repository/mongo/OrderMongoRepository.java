package com.unifi.ordersmgmt.repository.mongo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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

		for (Document doc : orderCollection.find()) {
			System.out.println("oi " + ((DBRef) doc.get("client")).getId());
			System.out.println("oi string " + ((DBRef) doc.get("client")).getId().toString());
		}
		List<Order> orders = StreamSupport.stream(orderCollection.find(clientSession).spliterator(), false)
				.filter(d -> {
					Date dataOrdine = d.getDate("date");
					Calendar cal = Calendar.getInstance();
					cal.setTime(dataOrdine);
					return cal.get(Calendar.YEAR) == year;
				})
				.map(d -> new Order(d.get("id").toString(),
						clientMongoRepository.findById(((DBRef) d.get("client")).getId().toString()), d.getDate("date"),
						d.getDouble("price")))
				.collect(Collectors.toList());
		return orders;
	}

	@Override
	public List<Integer> getYearsOfOrders() {
		// TODO Auto-generated method stub
		Set<Integer> setOfYears = StreamSupport.stream(orderCollection.find(clientSession).spliterator(), false)
				.map(d -> {
					Date date = d.getDate("date");
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(date);
					return calendar.get(Calendar.YEAR);
				}).collect(Collectors.toCollection(TreeSet::new));
		List<Integer> years = new ArrayList<>(setOfYears);
		return years;
	}

	@Override
	public List<Order> removeOrdersByClient(String clientId) {
		// TODO Auto-generated method stub
		List<Order> ordersToRemove = StreamSupport.stream(orderCollection.find(clientSession).spliterator(), false).filter(d -> {
			return ((DBRef) d.get("client")).getId().toString().equals(clientId);
		}).map(d -> new Order(d.getString("id"),
				clientMongoRepository.findById(((DBRef) d.get("client")).getId().toString()), d.getDate("date"),
				d.getDouble("price"))).collect(Collectors.toList());
		if (!ordersToRemove.isEmpty()) {
			for (Order order : ordersToRemove) {
				Document docToremove = new Document().append("id", order.getIdentifier())
						.append("client", new DBRef("client", clientId)).append("date", order.getDate())
						.append("price", order.getPrice());
				orderCollection.deleteOne(clientSession, docToremove);
			}
			return ordersToRemove;
		}
		return null;
	}


	@Override
	public List<Order> findOrdersByClientAndYear(Client client, int year) {
		// TODO Auto-generated method stub
		List<Order> orders = StreamSupport.stream(orderCollection.find(clientSession).spliterator(), false).filter(d -> {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(d.getDate("date"));
			return calendar.get(Calendar.YEAR) == year
					&& ((DBRef) d.get("client")).getId().toString().equals(client.getIdentifier().toString());
		}).map(d -> new Order(d.getString("id"),
				clientMongoRepository.findById(((DBRef) d.get("client")).getId().toString()), d.getDate("date"),
				d.getDouble("price"))).collect(Collectors.toList());

		return orders;
	}

	@Override
	public Order updateOrder(String orderID, Map<String, Object> updates) {
		// TODO Auto-generated method stub
		return null;
	}
}
