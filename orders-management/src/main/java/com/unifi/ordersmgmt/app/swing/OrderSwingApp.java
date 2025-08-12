package com.unifi.ordersmgmt.app.swing;

import java.awt.EventQueue;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.unifi.ordersmgmt.controller.OrderController;
import com.unifi.ordersmgmt.service.TransactionalClientService;
import com.unifi.ordersmgmt.service.TransactionalOrderService;
import com.unifi.ordersmgmt.transaction.mongo.MongoTransactionManager;
import com.unifi.ordersmgmt.view.swing.OrderSwingView;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(mixinStandardHelpOptions = true) // aggiunge le opzioni standard --help --version
public class OrderSwingApp implements Callable<Void> {

	@Option(names = { "--mongo-host" }, description = "MongoDB host address")
	private String mongoHost = "localhost";

	@Option(names = { "--mongo-port" }, description = "MongoDB host port")
	private int mongoPort = 27017;

	@Option(names = { "--db-name" }, description = "Database name")
	private String databaseName = "budget";

	@Option(names = { "--db-clients-collection" }, description = "Collection clients name")
	private String clientsCollection = "clients";

	@Option(names = { "--db-orders-collection" }, description = "Collection orders name")

	private String orderCollection = "orders";

	private static final Logger logger = LogManager.getLogger(OrderSwingApp.class);

	public static void main(String[] args) {
		new CommandLine(new OrderSwingApp()).execute(args);
	}

	@Override
	public Void call() throws Exception {
		EventQueue.invokeLater(() -> {
			try {
				logger.info("1. Building connection string...");
				String connectionString = String.format("mongodb://%s:%d/?replicaSet=rs0", mongoHost, mongoPort);
				logger.info(mongoHost);

				logger.info(mongoPort);
				logger.info(connectionString);
				logger.info("2. Creating mongo client...");
				MongoClient mongoClient = MongoClients.create(connectionString);
				logger.info("2b. Mongo client created.");

				logger.info("2c. Creating transaction manager...");
				MongoTransactionManager transactionManager = new MongoTransactionManager(mongoClient, clientsCollection,
						orderCollection, databaseName);
				logger.info("2d. Transaction manager created.");
				logger.info("3. Creating services...");
				TransactionalClientService clientService = new TransactionalClientService(transactionManager);
				TransactionalOrderService orderService = new TransactionalOrderService(transactionManager);
				OrderSwingView newGui = new OrderSwingView();
				OrderController controller = new OrderController(newGui, orderService, clientService);
				newGui.setOrderController(controller);
				newGui.setVisible(true);
				controller.InitializeView();

			} catch (Exception e) {
				logger.error("Startup error", e);
			}
		});

		return null;

	}

}
