package com.unifi.ordersmgmt.transaction.mongo;

import com.mongodb.client.MongoClient;
import com.unifi.ordersmgmt.transaction.TransactionManager;
import com.unifi.ordersmgmt.transaction.TransactionalFunction;

public class MongoTransactionManager implements TransactionManager {

	private MongoClient mongoclient;
	private String clientsCollectionName;
	private String ordersCollectionName;
	private String databaseName;
	public MongoTransactionManager(MongoClient mongoclient, String clientsCollectionName, String ordersCollectionName,
			String databaseName) {
		super();
		this.mongoclient = mongoclient;
		this.clientsCollectionName = clientsCollectionName;
		this.ordersCollectionName = ordersCollectionName;
		this.databaseName = databaseName;
	}
	@Override
	public <R> R executeTransaction(TransactionalFunction<R> transactionalFunction) {
		// TODO Auto-generated method stub
		return null;
	}

}
