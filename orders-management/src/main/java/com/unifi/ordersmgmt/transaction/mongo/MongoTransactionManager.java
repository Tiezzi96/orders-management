package com.unifi.ordersmgmt.transaction.mongo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mongodb.MongoException;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.TransactionBody;
import com.unifi.ordersmgmt.repository.mongo.ClientMongoRepository;
import com.unifi.ordersmgmt.repository.mongo.ClientSequenceGenerator;
import com.unifi.ordersmgmt.repository.mongo.OrderMongoRepository;
import com.unifi.ordersmgmt.repository.mongo.OrderSequenceGenerator;
import com.unifi.ordersmgmt.transaction.TransactionManager;
import com.unifi.ordersmgmt.transaction.TransactionalFunction;

public class MongoTransactionManager implements TransactionManager {

	private MongoClient mongoclient;
	private String clientsCollectionName;
	private String ordersCollectionName;
	private String databaseName;
	private static final Logger logger = LogManager.getLogger(MongoTransactionManager.class);
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
		logger.info("clientService initializate successfully.");

		TransactionOptions options = TransactionOptions.builder().readPreference(ReadPreference.primary())
				.readConcern(ReadConcern.LOCAL).writeConcern(WriteConcern.MAJORITY).build();
		logger.info("transaction op initializate successfully.");


		ClientSession clientSession = mongoclient.startSession();
		logger.info("mongoclient session started");

		try {
			OrderSequenceGenerator seqOrderGen = new OrderSequenceGenerator(mongoclient, databaseName);
			ClientSequenceGenerator seqClientGen = new ClientSequenceGenerator(mongoclient, databaseName);
			ClientMongoRepository clientMongoRepository = new ClientMongoRepository(mongoclient, clientSession,
					databaseName, clientsCollectionName, seqClientGen);
			OrderMongoRepository orderMongoRepository = new OrderMongoRepository(mongoclient, clientSession,
					databaseName, ordersCollectionName, clientMongoRepository, seqOrderGen);
			TransactionBody<R> body = () -> transactionalFunction.apply(clientMongoRepository, orderMongoRepository);
			return clientSession.withTransaction(body, options);
		} catch (MongoException e) {
			// TODO: handle exception
			logger.info("eccezione lanciata: {}", e.getMessage(), e);
			throw e;
		}

	}

}
