package com.unifi.ordersmgmt.view.swing;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
	private static final Logger logger = LogManager.getLogger(OrderSwingViewIT.class);

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
	
	@Test
	@GUITest
	public void testAllOrdersByYear() {
		Client client1 = clientRepository.save(new Client("client 1 name"));
		Client client2 = clientRepository.save(new Client("client 2 name"));
		logger.info("client1: {}", client1);
		logger.info("client2: {}", client2);
		Order order1 = new Order("ORDER-00001", client1,
				Date.from(LocalDate.of(2025, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		Order order2 = new Order("ORDER-00002", client2,
				Date.from(LocalDate.of(2025, 4, 2).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20);
		Order order3 = new Order("ORDER-00003", client2,
				Date.from(LocalDate.of(2024, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 40);
		orderRepository.save(order1);
		orderRepository.save(order2);
		orderRepository.save(order3);
		GuiActionRunner.execute(() -> {
			orderController.yearsOfTheOrders();
			orderController.allOrdersByYear(2025);
		});
		window.table("OrdersTable").requireRowCount(2);
		String[][] tableContents = window.table("OrdersTable").contents();
		assertThat(tableContents[0]).containsExactly(order1.getIdentifier(), order1.getClient().getName(),
				order1.getDate().toString(), String.valueOf(order1.getPrice()));
		assertThat(tableContents[1]).containsExactly(order2.getIdentifier(), order2.getClient().getName(),
				order2.getDate().toString(), String.valueOf(order2.getPrice()));
	}

	@Test
	@GUITest
	public void testViewOrdersAndAnnualTotalPriceByYear() {
		Client client1 = clientRepository.save(new Client("client 1 name"));
		Client client2 = clientRepository.save(new Client("client 2 name"));
		Order order1 = new Order("ORDER-00001", client1,
				Date.from(LocalDate.of(2025, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		Order order2 = new Order("ORDER-00002", client2,
				Date.from(LocalDate.of(2025, 4, 2).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20);
		Order order3 = new Order("ORDER-00003", client2,
				Date.from(LocalDate.of(2024, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 40);
		Order order4 = new Order("ORDER-00004", client2,
				Date.from(LocalDate.of(2026, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 40);
		orderRepository.save(order1);
		orderRepository.save(order2);
		orderRepository.save(order3);
		orderRepository.save(order4);
		GuiActionRunner.execute(() -> orderController.yearsOfTheOrders());
		window.comboBox("yearsCombobox").selectItem("2025");
		window.table("OrdersTable").requireRowCount(2);
		String[][] tableContents = window.table("OrdersTable").contents();
		assertThat(tableContents[0]).containsExactly(order1.getIdentifier(), order1.getClient().getName(),
				order1.getDate().toString(), String.valueOf(order1.getPrice()));
		assertThat(tableContents[1]).containsExactly(order2.getIdentifier(), order2.getClient().getName(),
				order2.getDate().toString(), String.valueOf(order2.getPrice()));
		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2025" + " è di "
				+ String.format("%.2f", order1.getPrice() + order2.getPrice()) + "€");
	}

	@Test
	@GUITest
	public void testViewOrdersAndAnnualPriceByClientAndYear() {
		Client client1 = clientRepository.save(new Client("client 1 name"));
		Client client2 = clientRepository.save(new Client("client 2 name"));
		Order order1 = new Order("ORDER-00001", client1,
				Date.from(LocalDate.of(2025, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		Order order2 = new Order("ORDER-00002", client2,
				Date.from(LocalDate.of(2025, 4, 2).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20);
		Order order3 = new Order("ORDER-00003", client2,
				Date.from(LocalDate.of(2024, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 40);
		Order order4 = new Order("ORDER-00004", client2,
				Date.from(LocalDate.of(2026, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 40);
		orderRepository.save(order1);
		orderRepository.save(order2);
		orderRepository.save(order3);
		orderRepository.save(order4);
		GuiActionRunner.execute(() -> orderController.InitializeView());
		window.comboBox("yearsCombobox").selectItem("2025");
		window.list("clientsList").selectItem(client1.toString());
		window.table("OrdersTable").requireRowCount(1);
		String[][] tableContents = window.table("OrdersTable").contents();
		assertThat(tableContents[0]).containsExactly(order1.getIdentifier(), order1.getClient().getName(),
				order1.getDate().toString(), String.valueOf(order1.getPrice()));
		window.label("revenueLabel").requireText("Il costo totale degli ordini del cliente " + client1.getIdentifier()
				+ " nel " + "2025" + " è di " + String.format("%.2f", order1.getPrice()) + "€");
	}
	
	
	@Test
	@GUITest
	public void testViewOrdersAndAnnualRevenueByClientAndYearWhenClientIsNotPresentInDatabase() {
		Client client1 = clientRepository.save(new Client("client 1 name"));
		Client client2 = clientRepository.save(new Client("client 2 name"));
		Order order1 = new Order("ORDER-00001", client1,
				Date.from(LocalDate.of(2025, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		Order order2 = new Order("ORDER-00002", client2,
				Date.from(LocalDate.of(2025, 4, 2).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20);
		Order order3 = new Order("ORDER-00003", client1,
				Date.from(LocalDate.of(2025, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 40);
		orderRepository.save(order1);
		orderRepository.save(order2);
		orderRepository.save(order3);
		GuiActionRunner.execute(() -> orderController.InitializeView());
		orderRepository.removeOrdersByClient(client1.getIdentifier());
		clientRepository.delete(client1.getIdentifier());
		window.comboBox("yearsCombobox").selectItem("2025");
		window.list("clientsList").selectItem(client1.toString());
		window.textBox("panelClientErrorMessage").requireText("Cliente non presente nel DB: " + client1);
		assertThat(window.list("clientsList").contents()).doesNotContain(client1.toString());
		window.table("OrdersTable").requireRowCount(1);
		String[][] tableContents = window.table("OrdersTable").contents();
		assertThat(tableContents[0]).containsExactly(order2.getIdentifier(), order2.getClient().getName(),
				order2.getDate().toString(), String.valueOf(order2.getPrice()));
		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2025" + " è di "
				+ String.format("%.2f", order2.getPrice()) + "€");
	}

	@Test
	@GUITest
	public void testViewAllOrdersAndAnnualRevenueAfterSelectingAClient() {
		Client client1 = clientRepository.save(new Client("client 1 name"));
		Client client2 = clientRepository.save(new Client("client 2 name"));
		Order order1 = new Order("ORDER-00001", client1,
				Date.from(LocalDate.of(2025, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		Order order2 = new Order("ORDER-00002", client2,
				Date.from(LocalDate.of(2025, 4, 2).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20);
		Order order3 = new Order("ORDER-00003", client1,
				Date.from(LocalDate.of(2024, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 40);
		orderRepository.save(order1);
		orderRepository.save(order2);
		orderRepository.save(order3);
		GuiActionRunner.execute(() -> orderController.InitializeView());
		window.comboBox("yearsCombobox").selectItem("2025");
		window.list("clientsList").selectItem(client1.toString());
		window.button(JButtonMatcher.withText("<html><center>Visualizza ordini<br>di tutti i clienti</center></html>")).click();
		String[][] tableContents = window.table("OrdersTable").contents();
		assertThat(tableContents[0]).containsExactly(order1.getIdentifier(), order1.getClient().getName(),
				order1.getDate().toString(), String.valueOf(order1.getPrice()));
		assertThat(tableContents[1]).containsExactly(order2.getIdentifier(), order2.getClient().getName(),
				order2.getDate().toString(), String.valueOf(order2.getPrice()));
		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2025" + " è di "
				+ String.format("%.2f", order1.getPrice() + order2.getPrice()) + "€");
	}

	@Test
	@GUITest
	public void testRemoveClientButtonWithSuccess() {
		Client client1 = clientRepository.save(new Client("client 1 name"));
		Client client2 = clientRepository.save(new Client("client 2 name"));
		Order order1 = new Order("ORDER-00001", client1,
				Date.from(LocalDate.of(2025, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		Order order2 = new Order("ORDER-00002", client2,
				Date.from(LocalDate.of(2025, 4, 2).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20);
		Order order3 = new Order("ORDER-00003", client1,
				Date.from(LocalDate.of(2024, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 40);
		orderRepository.save(order1);
		orderRepository.save(order2);
		orderRepository.save(order3);
		GuiActionRunner.execute(() -> orderController.InitializeView());
		window.comboBox("yearsCombobox").selectItem("2025");
		window.list("clientsList").selectItem(client1.toString());
		window.button(JButtonMatcher.withText("Rimuovi cliente")).click();
		logger.info("client1: {}", client1);
		logger.info("client1 id: {}", client1.getIdentifier());
		assertThat(window.list("clientsList").contents())
				.noneSatisfy(item -> assertThat(item).contains(client1.getIdentifier()));

		assertThat(window.comboBox("comboboxClients").contents())
				.noneSatisfy(item -> assertThat(item).contains(client1.getIdentifier()));
		String[][] tableContents = window.table("OrdersTable").contents();
		window.table("OrdersTable").requireRowCount(1);
		assertThat(tableContents[0]).containsOnly(order2.getIdentifier(), order2.getClient().getName(),
				order2.getDate().toString(), String.valueOf(order2.getPrice()));
		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2025" + " è di "
				+ String.format("%.2f", order2.getPrice()) + "€");
	}

	@Test
	@GUITest
	public void testRemoveClientButtonThrowError() {
		Client client1 = clientRepository.save(new Client("client 1 name"));
		Client client2 = clientRepository.save(new Client("client 2 name"));
		Order orderOfClient2 = new Order("ORDER-00001", client2,
				Date.from(LocalDate.of(2025, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		orderRepository.save(orderOfClient2);
		GuiActionRunner.execute(() -> orderController.InitializeView());
		window.comboBox("yearsCombobox").selectItem("2025");
		window.list("clientsList").selectItem(client1.toString());
		clientRepository.delete(client1.getIdentifier());
		window.button(JButtonMatcher.withText("Rimuovi cliente")).click();
		assertThat(window.list("clientsList").contents())
				.noneSatisfy(item -> assertThat(item).contains(client1.getIdentifier()));

		assertThat(window.comboBox("comboboxClients").contents())
				.noneSatisfy(item -> assertThat(item).contains(client1.getIdentifier()));
		String[][] tableContents = window.table("OrdersTable").contents();
		assertThat(tableContents[0])
				.containsOnly(new String[] { orderOfClient2.getIdentifier(), orderOfClient2.getClient().getName(),
						orderOfClient2.getDate().toString(), String.valueOf(orderOfClient2.getPrice()) });
		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2025" + " è di "
				+ String.format("%.2f", orderOfClient2.getPrice()) + "€");
		window.textBox("panelClientErrorMessage")
				.requireText("" + "Cliente non più presente nel DB: " + client1.toString());
	}

	@Test
	@GUITest
	public void testAddOrderOfYearSelectedButtonSuccess() {
		Client client1 = clientRepository.save(new Client("client 1 name"));
		Client client2 = clientRepository.save(new Client("client 2 name"));
		Order order1 = new Order("", client1,
				Date.from(LocalDate.of(2025, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		Order order2 = new Order("", client2,
				Date.from(LocalDate.of(2025, 4, 2).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20);
		Order order3 = new Order("", client1,
				Date.from(LocalDate.of(2024, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 40);
		orderRepository.save(order1);
		orderRepository.save(order2);
		orderRepository.save(order3);
		GuiActionRunner.execute(() -> orderController.InitializeView());
		window.comboBox("yearsCombobox").selectItem("2025");
		window.comboBox("comboboxClients").selectItem(client1.toString());
		window.textBox("textField_dayOfDateOrder").enterText("1");
		window.textBox("textField_monthOfDateOrder").enterText("5");
		window.textBox("textField_yearOfDateOrder").enterText("2025");
		window.textBox("textField_revenueOrder").enterText("10.20");
		window.button(JButtonMatcher.withText("Aggiungi ordine")).click();
		Order orderAdded = new Order("", client1,
				Date.from(LocalDate.of(2025, 5, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10.20);
		window.table("OrdersTable").requireRowCount(3);
		String[][] tableContents = window.table("OrdersTable").contents();
		assertThat(tableContents[0]).containsExactly("ORDER-00001", order1.getClient().getName(),
				order1.getDate().toString(), String.valueOf(order1.getPrice()));
		assertThat(tableContents[1]).containsExactly("ORDER-00002", order2.getClient().getName(),
				order2.getDate().toString(), String.valueOf(order2.getPrice()));
		assertThat(tableContents[2]).containsExactly("ORDER-00004", orderAdded.getClient().getName(),
				orderAdded.getDate().toString(), String.valueOf(orderAdded.getPrice()));
		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2025" + " è di "
				+ String.format("%.2f", order1.getPrice() + order2.getPrice() + orderAdded.getPrice()) + "€");
	}

	@Test
	@GUITest
	public void testAddOrderOfNotYearSelectedButtonSuccess() {
		Client client1 = clientRepository.save(new Client("client 1 name"));
		Client client2 = clientRepository.save(new Client("client 2 name"));
		Order order1 = new Order("", client1,
				Date.from(LocalDate.of(2025, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		Order order2 = new Order("", client2,
				Date.from(LocalDate.of(2025, 4, 2).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20);
		Order order3 = new Order("", client1,
				Date.from(LocalDate.of(2024, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 40);
		orderRepository.save(order1);
		orderRepository.save(order2);
		orderRepository.save(order3);
		GuiActionRunner.execute(() -> orderController.InitializeView());
		window.comboBox("yearsCombobox").selectItem("2024");
		window.comboBox("comboboxClients").selectItem(client1.toString());
		window.textBox("textField_dayOfDateOrder").enterText("1");
		window.textBox("textField_monthOfDateOrder").enterText("5");
		window.textBox("textField_yearOfDateOrder").enterText("2025");
		window.textBox("textField_revenueOrder").enterText("10.20");
		window.button(JButtonMatcher.withText("Aggiungi ordine")).click();
		Order orderAdded = new Order("", client1,
				Date.from(LocalDate.of(2025, 5, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10.20);
		String[][] tableContents = window.table("OrdersTable").contents();
		List<String[]> rows = Arrays.asList(tableContents);
		String[] notExpectedRow = { "ORDER-00004", orderAdded.getClient().getName(), orderAdded.getDate().toString(),
				String.valueOf(orderAdded.getPrice()) };
		assertThat(rows).isNotEmpty().doesNotContain(notExpectedRow);
		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2024" + " è di "
				+ String.format("%.2f", order3.getPrice()) + "€");
	}

	@Test
	@GUITest
	public void testAddOrderOfFirstDayOfYearButtonSuccess() {
		Client client1 = clientRepository.save(new Client("client 1 name"));
		Order order1 = new Order("", client1,
				Date.from(LocalDate.of(2024, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		orderRepository.save(order1);
		GuiActionRunner.execute(() -> orderController.InitializeView());
		window.comboBox("yearsCombobox").selectItem("2024");
		window.comboBox("comboboxClients").selectItem(client1.toString());
		window.textBox("textField_dayOfDateOrder").enterText("1");
		window.textBox("textField_monthOfDateOrder").enterText("1");
		window.textBox("textField_yearOfDateOrder").enterText("2024");
		window.textBox("textField_revenueOrder").enterText("10.20");
		window.button(JButtonMatcher.withText("Aggiungi ordine")).click();
		Order orderAdded = new Order("", client1,
				Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10.20);
		String[][] tableContents = window.table("OrdersTable").contents();
		List<String[]> rows = Arrays.asList(tableContents);
		String[] expectedRow = { "ORDER-00002", orderAdded.getClient().getName(), orderAdded.getDate().toString(),
				String.valueOf(orderAdded.getPrice()) };
		assertThat(rows).contains(expectedRow);
		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2024" + " è di "
				+ String.format("%.2f", order1.getPrice() + 10.20) + "€");
		GuiActionRunner.execute(() -> orderController.yearsOfTheOrders());
		assertThat(window.comboBox("yearsCombobox").contents()).containsExactly("2025", "2024", "-- Nessun anno --");
	}

	@Test
	@GUITest
	public void testAddOrderOfLastDayOfYearButtonSuccess() {
		Client client1 = clientRepository.save(new Client("client 1 name"));
		Order order1 = new Order("", client1,
				Date.from(LocalDate.of(2024, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		orderRepository.save(order1);
		GuiActionRunner.execute(() -> orderController.InitializeView());
		window.comboBox("yearsCombobox").selectItem("2024");
		window.comboBox("comboboxClients").selectItem(client1.toString());
		window.textBox("textField_dayOfDateOrder").enterText("31");
		window.textBox("textField_monthOfDateOrder").enterText("12");
		window.textBox("textField_yearOfDateOrder").enterText("2024");
		window.textBox("textField_revenueOrder").enterText("10.20");
		window.button(JButtonMatcher.withText("Aggiungi ordine")).click();
		Order orderAdded = new Order("", client1,
				Date.from(LocalDate.of(2024, 12, 31).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10.20);
		String[][] tableContents = window.table("OrdersTable").contents();
		List<String[]> rows = Arrays.asList(tableContents);
		String[] expectedRow = { "ORDER-00002", orderAdded.getClient().getName(), orderAdded.getDate().toString(),
				String.valueOf(orderAdded.getPrice()) };
		assertThat(rows).contains(expectedRow);
		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2024" + " è di "
				+ String.format("%.2f", order1.getPrice() + 10.20) + "€");
		GuiActionRunner.execute(() -> orderController.yearsOfTheOrders());
		assertThat(window.comboBox("yearsCombobox").contents()).containsExactly("2025", "2024", "-- Nessun anno --");
	}
	
	@Test
	@GUITest
	public void testAddOrderButtonErrorNoExistingClient() {
		Client client1 = clientRepository.save(new Client("client 1 name"));
		Client client2 = clientRepository.save(new Client("client 2 name"));
		Order order1 = new Order("", client1,
				Date.from(LocalDate.of(2024, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		Order order2 = new Order("", client2,
				Date.from(LocalDate.of(2024, 4, 2).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20);
		Order order3 = new Order("", client1,
				Date.from(LocalDate.of(2023, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 40);
		orderRepository.save(order1);
		orderRepository.save(order2);
		orderRepository.save(order3);
		GuiActionRunner.execute(() -> orderController.InitializeView());
		window.comboBox("yearsCombobox").selectItem("2024");
		window.comboBox("comboboxClients").selectItem(client1.toString());
		window.textBox("textField_dayOfDateOrder").enterText("1");
		window.textBox("textField_monthOfDateOrder").enterText("5");
		window.textBox("textField_yearOfDateOrder").enterText("2024");
		window.textBox("textField_revenueOrder").enterText("10.20");
		clientRepository.delete(client1.getIdentifier());
		window.button(JButtonMatcher.withText("Aggiungi ordine")).click();
		window.textBox("panelClientErrorMessage")
				.requireText("" + "Cliente non più presente nel DB: " + client1.toString());
		String[][] tableContents = window.table("OrdersTable").contents();
		List<String[]> rows = Arrays.asList(tableContents);
		String[] expectedRow = { "ORDER-00002", order2.getClient().getName(), order2.getDate().toString(),
				String.valueOf(order2.getPrice()) };

		assertThat(rows).containsOnly(expectedRow);

		assertThat(window.list("clientsList").contents()).noneMatch(e -> e.contains(client1.toString()));
		assertThat(window.comboBox("comboboxClients").contents()).noneMatch(e -> e.contains(client1.toString()));
		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2024" + " è di "
				+ String.format("%.2f", order2.getPrice()) + "€");
	}

	@Test
	@GUITest
	public void testRemoveOrderButtonSuccess() {
		Client client1 = clientRepository.save(new Client("client 1 name"));
		Client client2 = clientRepository.save(new Client("client 2 name"));
		Order order1 = new Order("", client1,
				Date.from(LocalDate.of(2024, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		Order order2 = new Order("", client2,
				Date.from(LocalDate.of(2024, 4, 2).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20);
		orderRepository.save(order1);
		orderRepository.save(order2);
		GuiActionRunner.execute(() -> orderController.InitializeView());
		window.comboBox("yearsCombobox").selectItem("2024");
		int rowOrderToDeleted = orderSwingView.getOrderTableModel().getOrderIndex(order1);
		window.table("OrdersTable").selectRows(rowOrderToDeleted);
		window.button(JButtonMatcher.withText("<html><center>Rimuovi<br>ordine</center></html>")).click();
		String[][] tableContents = window.table("OrdersTable").contents();
		List<String[]> rows = Arrays.asList(tableContents);
		String[] expectedRow = { "ORDER-00001", order1.getClient().getName(), order1.getDate().toString(),
				String.valueOf(order1.getPrice()) };

		assertThat(rows).isNotEmpty().doesNotContain(expectedRow);
		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2024" + " è di "
				+ String.format("%.2f", order2.getPrice()) + "€");
	}

	@Test
	@GUITest
	public void testRemoveOrderButtonErrorClientNotExistInDB() {
		Client clientRemainInList = clientRepository.save(new Client("client 1 name"));
		Client clientToDelete = clientRepository.save(new Client("client 2 name"));
		Order order1 = new Order("", clientRemainInList,
				Date.from(LocalDate.of(2024, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		Order order2 = new Order("", clientToDelete,
				Date.from(LocalDate.of(2024, 4, 2).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20);
		orderRepository.save(order1);
		orderRepository.save(order2);
		GuiActionRunner.execute(() -> orderController.InitializeView());
		window.comboBox("yearsCombobox").selectItem("2024");
		int rowOrderToDeleted = orderSwingView.getOrderTableModel().getOrderIndex(order2);
		window.table("OrdersTable").selectRows(rowOrderToDeleted);
		clientRepository.delete(clientToDelete.getIdentifier());
		window.button(JButtonMatcher.withText("<html><center>Rimuovi<br>ordine</center></html>")).click();
		window.textBox("panelClientErrorMessage")
				.requireText("Cliente non più presente nel DB: " + clientToDelete.toString());
		assertThat(window.list("clientsList").contents()).noneMatch(e -> e.contains(clientToDelete.toString()));
		assertThat(window.comboBox("comboboxClients").contents()).noneMatch(e -> e.contains(clientToDelete.toString()));
		String[][] tableContents = window.table("OrdersTable").contents();
		List<String[]> rows = Arrays.asList(tableContents);
		String[] expectedRow = { "ORDER-00002", order2.getClient().getName(), order2.getDate().toString(),
				String.valueOf(order2.getPrice()) };

		assertThat(rows).isNotEmpty().doesNotContain(expectedRow);
		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2024" + " è di "
				+ String.format("%.2f", order1.getPrice()) + "€");
	}

	@Test
	@GUITest
	public void testRemoveOrderButtonErrorOrderNotExistInDB() {
		Client client1 = clientRepository.save(new Client("client 1 name"));
		Client client2 = clientRepository.save(new Client("client 2 name"));
		Order order1 = new Order("", client1,
				Date.from(LocalDate.of(2024, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		Order order2 = new Order("", client2,
				Date.from(LocalDate.of(2024, 4, 2).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20);
		orderRepository.save(order1);
		orderRepository.save(order2);
		GuiActionRunner.execute(() -> orderController.InitializeView());
		window.comboBox("yearsCombobox").selectItem("2024");
		int rowOrderToDelete = orderSwingView.getOrderTableModel().getOrderIndex(order2);
		window.table("OrdersTable").selectRows(rowOrderToDelete);
		orderRepository.delete(order2.getIdentifier());
		window.button(JButtonMatcher.withText("<html><center>Rimuovi<br>ordine</center></html>")).click();
		window.textBox("panelOrderErrorMessage").requireText("Ordine non più presente nel DB: " + order2.toString());

		String[][] tableContents = window.table("OrdersTable").contents();
		List<String[]> rows = Arrays.asList(tableContents);
		String[] expectedRow = { "ORDER-00002", order2.getClient().getName(), order2.getDate().toString(),
				String.valueOf(order2.getPrice()) };

		assertThat(rows).isNotEmpty().doesNotContain(expectedRow);
		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2024" + " è di "
				+ String.format("%.2f", order1.getPrice()) + "€");
	}
	
	@Test
	@GUITest
	public void testModifyOrderOfYearSelectedMaintainingSameYearButtonSuccess() {
		Client client1 = clientRepository.save(new Client("client 1 name"));
		Client client2 = clientRepository.save(new Client("client 2 name"));
		Order order1 = new Order("", client1,
				Date.from(LocalDate.of(2025, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		Order order2 = new Order("", client2,
				Date.from(LocalDate.of(2025, 4, 2).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20);
		Order order3 = new Order("", client1,
				Date.from(LocalDate.of(2024, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 40);
		orderRepository.save(order1);
		orderRepository.save(order2);
		orderRepository.save(order3);
		GuiActionRunner.execute(() -> orderController.InitializeView());
		int roworderToModify = orderSwingView.getOrderTableModel().getOrderIndex(order2);
		window.comboBox("yearsCombobox").selectItem("2025");
		window.table("OrdersTable").selectRows(roworderToModify);
		window.comboBox("comboboxClients").selectItem(client1.toString());
		window.textBox("textField_dayOfDateOrder").setText("");
		window.textBox("textField_monthOfDateOrder").setText("");
		window.textBox("textField_yearOfDateOrder").setText("");
		window.textBox("textField_revenueOrder").setText("");

		window.textBox("textField_dayOfDateOrder").enterText("30");
		window.textBox("textField_monthOfDateOrder").enterText("6");
		window.textBox("textField_yearOfDateOrder").enterText("2025");
		window.textBox("textField_revenueOrder").enterText("20.20");

		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).click();
		Order orderModified = new Order("ORDER-00002", client1,
				Date.from(LocalDate.of(2025, 6, 30).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20.20);
		window.table("OrdersTable").requireRowCount(2);
		String[][] tableContents = window.table("OrdersTable").contents();
		assertThat(tableContents[0]).containsExactly("ORDER-00001", order1.getClient().getName(),
				order1.getDate().toString(), String.valueOf(order1.getPrice()));
		assertThat(tableContents[1]).containsExactly("ORDER-00002", orderModified.getClient().getName(),
				orderModified.getDate().toString(), String.valueOf(orderModified.getPrice()));
		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2025" + " è di "
				+ String.format("%.2f", order1.getPrice() + orderModified.getPrice()) + "€");
	}

	@Test
	@GUITest
	public void testModifyOrderOfYearSelectedChangeYearButtonSuccess() {
		Client client1 = clientRepository.save(new Client("client 1 name"));
		Client client2 = clientRepository.save(new Client("client 2 name"));
		Order order1 = new Order("", client1,
				Date.from(LocalDate.of(2025, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		Order order2 = new Order("", client2,
				Date.from(LocalDate.of(2025, 4, 2).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20);
		Order order3 = new Order("", client1,
				Date.from(LocalDate.of(2024, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 40);
		orderRepository.save(order1);
		orderRepository.save(order2);
		orderRepository.save(order3);
		GuiActionRunner.execute(() -> orderController.InitializeView());
		int roworderToModify = orderSwingView.getOrderTableModel().getOrderIndex(order2);
		window.comboBox("yearsCombobox").selectItem("2025");
		window.table("OrdersTable").selectRows(roworderToModify);
		window.comboBox("comboboxClients").selectItem(client1.toString());
		window.textBox("textField_dayOfDateOrder").setText("");
		window.textBox("textField_monthOfDateOrder").setText("");
		window.textBox("textField_yearOfDateOrder").setText("");
		window.textBox("textField_revenueOrder").setText("");

		window.textBox("textField_dayOfDateOrder").enterText("31");
		window.textBox("textField_monthOfDateOrder").enterText("7");
		window.textBox("textField_yearOfDateOrder").enterText("2024");
		window.textBox("textField_revenueOrder").enterText("20.20");

		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).click();
		window.table("OrdersTable").requireRowCount(1);
		String[][] tableContents = window.table("OrdersTable").contents();
		assertThat(tableContents[0]).containsExactly("ORDER-00001", order1.getClient().getName(),
				order1.getDate().toString(), String.valueOf(order1.getPrice()));
		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2025" + " è di "
				+ String.format("%.2f", order1.getPrice()) + "€");
	}

	@Test
	@GUITest
	public void testModifyOrderOfClientSelectedChangeClientButtonSuccess() {
		Client client1 = clientRepository.save(new Client("client 1 name"));
		Client client2 = clientRepository.save(new Client("client 2 name"));
		Order order1 = new Order("", client1,
				Date.from(LocalDate.of(2025, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		Order order2 = new Order("", client2,
				Date.from(LocalDate.of(2024, 4, 2).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20);
		Order order3 = new Order("", client1,
				Date.from(LocalDate.of(2025, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 40);
		orderRepository.save(order1);
		orderRepository.save(order2);
		orderRepository.save(order3);
		GuiActionRunner.execute(() -> {
			orderController.InitializeView();
		});
		window.list("clientsList").selectItem(client1.toString());
		window.comboBox("yearsCombobox").clearSelection();
		int roworderToModify = orderSwingView.getOrderTableModel().getOrderIndex(order1);
		window.table("OrdersTable").selectRows(roworderToModify);
		window.comboBox("comboboxClients").selectItem(client2.toString());
		window.textBox("textField_dayOfDateOrder").setText("");
		window.textBox("textField_monthOfDateOrder").setText("");
		window.textBox("textField_yearOfDateOrder").setText("");
		window.textBox("textField_revenueOrder").setText("");

		window.textBox("textField_dayOfDateOrder").enterText("31");
		window.textBox("textField_monthOfDateOrder").enterText("12");
		window.textBox("textField_yearOfDateOrder").enterText("2024");
		window.textBox("textField_revenueOrder").enterText("40.50");

		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).click();
		window.table("OrdersTable").requireRowCount(1);
		String[][] tableContents = window.table("OrdersTable").contents();
		assertThat(tableContents[0]).containsExactly("ORDER-00003", order3.getClient().getName(),
				order3.getDate().toString(), String.valueOf(order3.getPrice()));
		window.label("revenueLabel").requireText("Il costo totale degli ordini del cliente " + client1.getIdentifier()
				+ " è di " + String.format("%.2f", order3.getPrice()).replace(".", ",") + "€");
	}

	@Test
	@GUITest
	public void testModifyOrderOfClientAndYearSelectedMaintainingClientButtonSuccess() {
		Client client1 = clientRepository.save(new Client("client 1 name"));
		Client client2 = clientRepository.save(new Client("client 2 name"));
		Order order1 = new Order("", client1,
				Date.from(LocalDate.of(2025, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		Order order2 = new Order("", client2,
				Date.from(LocalDate.of(2024, 4, 2).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20);
		Order order3 = new Order("", client1,
				Date.from(LocalDate.of(2025, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 40);
		orderRepository.save(order1);
		orderRepository.save(order2);
		orderRepository.save(order3);
		GuiActionRunner.execute(() -> {
			orderController.InitializeView();
		});
		window.list("clientsList").selectItem(client1.toString());
		int roworderToModify = orderSwingView.getOrderTableModel().getOrderIndex(order1);
		window.table("OrdersTable").selectRows(roworderToModify);
		window.comboBox("comboboxClients").selectItem(client1.toString());
		window.textBox("textField_dayOfDateOrder").setText("");
		window.textBox("textField_monthOfDateOrder").setText("");
		window.textBox("textField_yearOfDateOrder").setText("");
		window.textBox("textField_revenueOrder").setText("");

		window.textBox("textField_dayOfDateOrder").enterText("1");
		window.textBox("textField_monthOfDateOrder").enterText("1");
		window.textBox("textField_yearOfDateOrder").enterText("2025");
		window.textBox("textField_revenueOrder").enterText("70.50");

		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).click();
		Order orderModified = new Order("ORDER-00001", client1,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 70.50);
		window.table("OrdersTable").requireRowCount(2);
		String[][] tableContents = window.table("OrdersTable").contents();
		assertThat(tableContents[0]).containsExactly("ORDER-00001", orderModified.getClient().getName(),
				orderModified.getDate().toString(), String.valueOf(orderModified.getPrice()));
		assertThat(tableContents[1]).containsExactly("ORDER-00003", order3.getClient().getName(),
				order3.getDate().toString(), String.valueOf(order3.getPrice()));
		window.label("revenueLabel")
				.requireText("Il costo totale degli ordini del cliente " + client1.getIdentifier() + " nel " + "2025"
						+ " è di "
						+ String.format("%.2f", order3.getPrice() + orderModified.getPrice()).replace(".", ",") + "€");
	}
}
