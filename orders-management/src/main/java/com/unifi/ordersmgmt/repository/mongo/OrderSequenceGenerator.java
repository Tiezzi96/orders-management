package com.unifi.ordersmgmt.repository.mongo;

import org.bson.Document;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;

public class OrderSequenceGenerator {

	private final MongoDatabase db;

	public OrderSequenceGenerator(MongoClient client, String db) {
		this.db = client.getDatabase(db);
	}

	public long getNextSequence(ClientSession session, String sequenceName) {
		Document result = db.getCollection("counters").findOneAndUpdate(session, Filters.eq("_id", sequenceName),
				Updates.inc("seq", 1), new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));
		Number seqValue = result.get("seq", Number.class);
		return seqValue.longValue();

	}

	public String generateCodiceCliente(ClientSession session) {
		long nextId = getNextSequence(session, "orders");
		return String.format("ORDER-%05d", nextId);
	}
}
