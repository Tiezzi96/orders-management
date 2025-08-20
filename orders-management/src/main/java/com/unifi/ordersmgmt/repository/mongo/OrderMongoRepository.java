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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

import com.mongodb.DBRef;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.UpdateResult;
import com.unifi.ordersmgmt.model.Client;
import com.unifi.ordersmgmt.model.Order;
import com.unifi.ordersmgmt.repository.ClientRepository;
import com.unifi.ordersmgmt.repository.OrderRepository;

public class OrderMongoRepository implements OrderRepository {

	private static final Logger logger = LogManager.getLogger(OrderMongoRepository.class);
	private static final String PRICE = "price";
	private static final String CLIENT = "client";
	private ClientSession clientSession;
	private MongoCollection<Document> orderCollection;

	private ClientRepository clientMongoRepository;
	private OrderSequenceGenerator seqGen;

	public OrderMongoRepository(MongoClient mongoClient, ClientSession clientSession, String dbName,
			String collectionOrderName, ClientRepository clientMongoRepository, OrderSequenceGenerator seqGen) {
		this.clientMongoRepository = clientMongoRepository;
		this.clientSession = clientSession;
		MongoDatabase database = mongoClient.getDatabase(dbName);
		if (!database.listCollectionNames().into(new ArrayList<String>()).contains(collectionOrderName)) {
			database.createCollection(collectionOrderName);
		}
		orderCollection = database.getCollection(collectionOrderName);
		this.seqGen = seqGen;

	}

	@Override
	public List<Order> findAll() {
		return StreamSupport.stream(orderCollection.find(clientSession).spliterator(), false)
				.map(d -> new Order(d.get("id").toString(),
						clientMongoRepository.findById(((DBRef) d.get(CLIENT)).getId().toString()), d.getDate("date"),
						d.getDouble(PRICE)))
				.collect(Collectors.toList());
	}

	@Override
	public Order findById(String id) {
		Document docFound = orderCollection.find(clientSession, Filters.eq("id", id)).first();
		if (docFound == null) {
			return null;
		}
		return new Order(docFound.get("id").toString(),
				clientMongoRepository.findById(((DBRef) docFound.get(CLIENT)).getId().toString()),
				docFound.getDate("date"), docFound.getDouble(PRICE));
	}

	@Override
	public Order save(Order obj) {
		if (obj.getIdentifier().trim().isEmpty()) {
			obj.setIdentifier(seqGen.generateCodiceCliente(clientSession));
			logger.info("Generated new order id {}", obj.getIdentifier());
		}
		Document docToInsert = new Document().append("id", obj.getIdentifier())
				.append(CLIENT, new DBRef(CLIENT, obj.getClient().getIdentifier())).append("date", obj.getDate())
				.append(PRICE, obj.getPrice());
		orderCollection.insertOne(clientSession, docToInsert);
		Document docInserted = orderCollection.find(clientSession, Filters.eq("id", obj.getIdentifier())).first();
		logger.info("Inserted order document: {}", docToInsert);
		Order orderInserted = new Order(docInserted.get("id").toString(),
				clientMongoRepository.findById(((DBRef) docInserted.get(CLIENT)).getId().toString()),
				docInserted.getDate("date"), docInserted.getDouble(PRICE));
		return orderInserted;
	}

	@Override
	public Order delete(String id) {
		Order orderToDelete = findById(id);
		if (orderToDelete != null) {
			orderCollection.deleteOne(clientSession, Filters.eq("id", orderToDelete.getIdentifier()));
			return orderToDelete;
		}
		return null;
	}

	@Override
	public List<Order> findOrderByYear(int year) {
		List<Order> orders = StreamSupport.stream(orderCollection.find(clientSession).spliterator(), false)
				.filter(d -> {
					Date dataOrdine = d.getDate("date");
					Calendar cal = Calendar.getInstance();
					cal.setTime(dataOrdine);
					return cal.get(Calendar.YEAR) == year;
				})
				.map(d -> new Order(d.get("id").toString(),
						clientMongoRepository.findById(((DBRef) d.get(CLIENT)).getId().toString()), d.getDate("date"),
						d.getDouble(PRICE)))
				.collect(Collectors.toList());
		logger.info("Found {} orders for year {}", orders.size(), year);
		return orders;
	}

	@Override
	public List<Integer> getYearsOfOrders() {
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
		List<Order> ordersToRemove = StreamSupport.stream(orderCollection.find(clientSession).spliterator(), false)
				.filter(d -> ((DBRef) d.get(CLIENT)).getId().toString().equals(clientId))
				.map(d -> new Order(d.getString("id"),
						clientMongoRepository.findById(((DBRef) d.get(CLIENT)).getId().toString()), d.getDate("date"),
						d.getDouble(PRICE)))
				.collect(Collectors.toList());
		if (!ordersToRemove.isEmpty()) {
			for (Order order : ordersToRemove) {
				Document docToremove = new Document().append("id", order.getIdentifier())
						.append(CLIENT, new DBRef(CLIENT, clientId)).append("date", order.getDate())
						.append(PRICE, order.getPrice());
				orderCollection.deleteOne(clientSession, docToremove);
			}
			// return ordersToRemove;
		}
		return ordersToRemove;
	}

	@Override
	public List<Order> findOrdersByClientAndYear(Client client, int year) {
		List<Order> orders = StreamSupport.stream(orderCollection.find(clientSession).spliterator(), false)
				.filter(d -> {
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(d.getDate("date"));
					return calendar.get(Calendar.YEAR) == year
							&& ((DBRef) d.get(CLIENT)).getId().toString().equals(client.getIdentifier());
				})
				.map(d -> new Order(d.getString("id"),
						clientMongoRepository.findById(((DBRef) d.get(CLIENT)).getId().toString()), d.getDate("date"),
						d.getDouble(PRICE)))
				.collect(Collectors.toList());

		return orders;
	}

	@Override
	public Order updateOrder(String orderID, Map<String, Object> updates) {
		Order orderToModify = findById(orderID);
		if (orderToModify != null) {
			Document docOfUpdates = new Document();
			if (updates.get(CLIENT) != null) {
				Client clientModify = (Client) updates.get(CLIENT);
				docOfUpdates.append(CLIENT, new DBRef(CLIENT, clientModify.getIdentifier()));
			}
			if (updates.get("date") != null) {
				docOfUpdates.append("date", updates.get("date"));
			}
			if (updates.get(PRICE) != null) {
				docOfUpdates.append(PRICE, Double.valueOf(updates.get(PRICE).toString()));
			}
			Document docModified = new Document("$set", docOfUpdates);
			UpdateResult result = orderCollection.updateOne(clientSession, Filters.eq("id", orderID), docModified);

			logger.info("Matched count: {}", result.getMatchedCount());
			logger.info("Modified count: {}", result.getModifiedCount());
			Order orderModified = findById(orderID);
			return orderModified;
		}
		return null;
	}

	@Override
	public List<Order> findOrdersByClient(Client client) {
		List<Order> orders = StreamSupport.stream(orderCollection.find(clientSession).spliterator(), false)
				.filter(d -> ((DBRef) d.get(CLIENT)).getId().toString().equals(client.getIdentifier()))
				.map(d -> new Order(d.get("id").toString(),
						clientMongoRepository.findById(((DBRef) d.get(CLIENT)).getId().toString()), d.getDate("date"),
						d.getDouble(PRICE)))
				.collect(Collectors.toList());
		return orders;
	}

}
