package com.unifi.ordersmgmt.view.swing;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Test;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.unifi.ordersmgmt.controller.OrderController;
import com.unifi.ordersmgmt.model.Client;
import com.unifi.ordersmgmt.model.Order;
import com.unifi.ordersmgmt.repository.ClientRepository;
import com.unifi.ordersmgmt.repository.OrderRepository;
import com.unifi.ordersmgmt.repository.mongo.ClientMongoRepository;
import com.unifi.ordersmgmt.repository.mongo.ClientSequenceGenerator;
import com.unifi.ordersmgmt.repository.mongo.OrderMongoRepository;
import com.unifi.ordersmgmt.repository.mongo.OrderSequenceGenerator;
import com.unifi.ordersmgmt.service.ClientService;
import com.unifi.ordersmgmt.service.OrderService;
import com.unifi.ordersmgmt.service.TransactionalClientService;
import com.unifi.ordersmgmt.service.TransactionalOrderService;
import com.unifi.ordersmgmt.transaction.TransactionManager;
import com.unifi.ordersmgmt.transaction.mongo.MongoTransactionManager;

public class OrderSwingViewIT extends AssertJSwingJUnitTestCase {
	
	private MongoClient mongoClient;
	private ClientRepository clientRepository;
	private OrderRepository orderRepository;
	private OrderSwingView orderSwingView;
	private OrderController orderController;
	private FrameFixture window;

	@Override
	protected void onSetUp() throws Exception {
		mongoClient = MongoClients.create("mongodb://localhost:27017/?replicaSet=rs0");
		ClientSession clientSession = mongoClient.startSession();
		ClientSequenceGenerator clientSeqGen = new ClientSequenceGenerator(mongoClient, "budget");

		clientRepository = new ClientMongoRepository(mongoClient, clientSession, "budget", "client", clientSeqGen);
		OrderSequenceGenerator seqGen = new OrderSequenceGenerator(mongoClient, "budget");
		orderRepository = new OrderMongoRepository(mongoClient, clientSession, "budget", "order", clientRepository,
				seqGen);
		for (Client client : clientRepository.findAll()) {
			clientRepository.delete(client.getIdentifier());
		}
		for (Order order : orderRepository.findAll()) {
			orderRepository.delete(order.getIdentifier());
		}

		// Reset del contatore ORDINI
		MongoDatabase db = mongoClient.getDatabase("budget");
		db.getCollection("counters").deleteOne(Filters.eq("_id", "orders"));

		// Reset del contatore CLIENTI
		db.getCollection("counters").deleteOne(Filters.eq("_id", "client"));

		GuiActionRunner.execute(() -> {
			orderSwingView = new OrderSwingView();
			TransactionManager transactionManager = new MongoTransactionManager(mongoClient, "client", "order",
					"budget");
			ClientService clientService = new TransactionalClientService(transactionManager);
			OrderService orderService = new TransactionalOrderService(transactionManager);
			orderController = new OrderController(orderSwingView, orderService, clientService);
			orderSwingView.setOrderController(orderController);
			return orderSwingView;
		});

		robot().settings().delayBetweenEvents(200);
		window = new FrameFixture(robot(), orderSwingView);
		window.show();
	}

	@Override
	protected void onTearDown() throws Exception {
		mongoClient.close();
	}

	@Test
	@GUITest
	public void testAShowAllClients() {
		Client client1 = clientRepository.save(new Client("client 1 name"));
		Client client2 = clientRepository.save(new Client("client 2 name"));
		GuiActionRunner.execute(() -> orderController.showAllClients());
		String[] clientsListContents = window.list("clientsList").contents();
		assertThat(clientsListContents).containsExactly(client1.toString(), client2.toString());
	}
	
	@Test
	@GUITest
	public void testAddClientButton() {
		window.textBox("textField_clientName").enterText("test identifier");
		window.button(JButtonMatcher.withText("Aggiungi cliente")).click();
		assertThat(window.list("clientsList").contents())
				.containsOnly(new Client("CLIENT-00001", "test identifier").toString());
		assertThat(window.comboBox("comboboxClients").contents())
				.containsExactly(new Client("CLIENT-00001", "test identifier").toString());
	}

}
