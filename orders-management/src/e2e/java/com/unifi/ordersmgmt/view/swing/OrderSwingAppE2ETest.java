package com.unifi.ordersmgmt.view.swing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.swing.launcher.ApplicationLauncher.application;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import javax.swing.JFrame;

import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.runner.GUITestRunner;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.bson.Document;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.mongodb.DBRef;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;

@RunWith(GUITestRunner.class)
public class OrderSwingAppE2ETest extends AssertJSwingJUnitTestCase {

	private static final String DB_NAME = "test-db";

	private static final String COLLECTION_NAME = "clients";
	private static final String ORDER_COLLECTION_NAME = "orders";

	private MongoClient mongoClient;

	private FrameFixture window;

	@Override
	protected void onSetUp() throws Exception {
		mongoClient = MongoClients.create("mongodb://localhost:27017/?replicaSet=rs0");

		mongoClient.getDatabase(DB_NAME).drop();
		addTestClientToDB("CLIENT-00001", "client 1");
		addTestClientToDB("CLIENT-00002", "client 2");
		addTestOrderToDB("ORDER-00001", "CLIENT-00001",
				Date.from(LocalDate.of(2025, 7, 31).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10.0);
		addTestOrderToDB("ORDER-00002", "CLIENT-00002",
				Date.from(LocalDate.of(2025, 7, 31).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20.0);
		addTestOrderToDB("ORDER-00003", "CLIENT-00001",
				Date.from(LocalDate.of(2024, 5, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 30.0);
		addTestOrderToDB("ORDER-00004", "CLIENT-00002",
				Date.from(LocalDate.of(2024, 5, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 40.0);
		updateClientSequence(2);
		updateOrderSequence(4);
		application("com.unifi.ordersmgmt.app.swing.OrderSwingApp")
				.withArgs("--mongo-host=" + "localhost", "--mongo-port=" + "27017", "--db-name=" + DB_NAME,
						"--db-clients-collection=" + COLLECTION_NAME, "--db-orders-collection=" + ORDER_COLLECTION_NAME)
				.start();
		robot().waitForIdle();
		window = WindowFinder.findFrame(new GenericTypeMatcher<JFrame>(JFrame.class) {
			@Override
			protected boolean isMatching(JFrame component) {
				return "Order Management View".equals(component.getTitle()) && component.isShowing();
			}
		}).using(robot());

	}

	@Override
	protected void onTearDown() throws Exception {
		if (mongoClient != null) {
			mongoClient.close();
		}

	}

	private void addTestClientToDB(String id, String name) {
		Document doc = new Document().append("id", id).append("name", name);
		mongoClient.getDatabase(DB_NAME).getCollection(COLLECTION_NAME).insertOne(doc); 
	}

	private void updateClientSequence(int lastIdUsed) {
		mongoClient.getDatabase(DB_NAME).getCollection("counters").updateOne(new Document("_id", "client"),
				new Document("$set", new Document("seq", lastIdUsed)), new UpdateOptions().upsert(true));
	}

	private void updateOrderSequence(int lastIdUsed) {
		mongoClient.getDatabase(DB_NAME).getCollection("counters").updateOne(new Document("_id", "orders"),
				new Document("$set", new Document("seq", lastIdUsed)), new UpdateOptions().upsert(true));
	}

	private void addTestOrderToDB(String orderID, String clientId, Date date, double price) {
		Document doc = new Document().append("id", orderID).append("client", new DBRef("client", clientId))
				.append("date", date).append("price", price);
		mongoClient.getDatabase(DB_NAME).getCollection(ORDER_COLLECTION_NAME).insertOne(doc);
	}
	
	private void removeClientFromDatabase(String clientID) {
		mongoClient.getDatabase(DB_NAME).getCollection(ORDER_COLLECTION_NAME)
				.deleteMany(Filters.eq("client.$id", clientID));
		mongoClient.getDatabase(DB_NAME).getCollection(COLLECTION_NAME).deleteOne(Filters.eq("id", clientID));
	}

	@Test
	@GUITest
	public void testOnStartShowAllClientsInDBAreShown() {
		assertThat(window.list("clientsList").contents())
				.anySatisfy(e -> assertThat(e).isEqualTo("CLIENT-00001, client 1"))
				.anySatisfy(e -> assertThat(e).isEqualTo("CLIENT-00002, client 2"));
	}
	
	
	@Test
	@GUITest
	public void testOnStartShowAllOrdersOfCurrentYearInDBAreShown() {
		window.comboBox("yearsCombobox").requireSelection("2025");
		window.table("OrdersTable").requireRowCount(2);
		String[][] tableContents = window.table("OrdersTable").contents();
		assertThat(tableContents[0]).containsExactly("ORDER-00001", "client 1",
				Date.from(LocalDate.of(2025, 7, 31).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"10.0");
		assertThat(tableContents[1]).containsExactly("ORDER-00002", "client 2",
				Date.from(LocalDate.of(2025, 7, 31).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"20.0");
	}

	@Test
	@GUITest
	public void testOnStartInitialRevenueLabelOfYearsSelectedIsShown() {
		String[][] tableContents = window.table("OrdersTable").contents();
		assertThat(tableContents).isNotEmpty();
		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2025" + " è di " + "30,00€");
	}

	@Test
	@GUITest
	public void testShowAllOrdersOfAYearSelected() {
		window.comboBox("yearsCombobox").selectItem("2024");
		window.table("OrdersTable").requireRowCount(2);
		String[][] tableContents = window.table("OrdersTable").contents();
		assertThat(tableContents[0]).containsExactly("ORDER-00003", "client 1",
				Date.from(LocalDate.of(2024, 5, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"30.0");
		assertThat(tableContents[1]).containsExactly("ORDER-00004", "client 2",
				Date.from(LocalDate.of(2024, 5, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"40.0");
		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2024" + " è di " + "70,00€");
	}
	
	@Test
	@GUITest
	public void testShowAllOrdersOfAYearAndClientSelected() {
		window.comboBox("yearsCombobox").selectItem("2024");
		window.list("clientsList").selectItem(1);
		window.table("OrdersTable").requireRowCount(1);
		String[][] tableContents = window.table("OrdersTable").contents();
		assertThat(tableContents[0]).containsExactly("ORDER-00004", "client 2",
				Date.from(LocalDate.of(2024, 5, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"40.0");
		window.label("revenueLabel").requireText(
				"Il costo totale degli ordini del cliente CLIENT-00002" + " nel 2024" + " è di " + "40,00€");
	}

	@Test
	@GUITest
	public void testShowAllOrdersOfAClientAndYearSelected() {
		window.list("clientsList").selectItem(0);
		window.comboBox("yearsCombobox").selectItem("2025");
		window.table("OrdersTable").requireRowCount(1);
		String[][] tableContents = window.table("OrdersTable").contents();
		assertThat(tableContents[0]).containsExactly("ORDER-00001", "client 1",
				Date.from(LocalDate.of(2025, 7, 31).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"10.0");
		window.label("revenueLabel").requireText(
				"Il costo totale degli ordini del cliente CLIENT-00001" + " nel 2025" + " è di " + "10,00€");
	}

	@Test
	@GUITest
	public void testShowAllOrdersOfAClientRemovedFromDB() {
		window.comboBox("yearsCombobox").selectItem("2025");
		removeClientFromDatabase("CLIENT-00002");
		window.list("clientsList").selectItem(1);
		assertThat(window.textBox("panelClientErrorMessage").text()).contains("CLIENT-00002");

		String[] clientListContents = window.list("clientsList").contents();
		assertThat(clientListContents).noneMatch(e -> e.contains("CLIENT-00002"));

		String[] clientComboboxContents = window.comboBox("comboboxClients").contents();
		assertThat(clientComboboxContents).noneMatch(e -> e.contains("CLIENT-00002"));

		String[][] tableContents = window.table("OrdersTable").contents();
		assertThat(tableContents[0]).containsOnly(new String[] { "ORDER-00001", "client 1",
				Date.from(LocalDate.of(2025, 7, 31).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"10.0" });

		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2025" + " è di " + "10,00€");
	}

	@Test
	@GUITest
	public void testAddNewClient() {
		window.textBox("textField_clientName").enterText("client 3");
		window.button(JButtonMatcher.withText("Aggiungi cliente")).click();
		String[] clientListContents = window.list("clientsList").contents();
		assertThat(clientListContents).contains("CLIENT-00003, client 3");
		String[] clientComboboxContents = window.comboBox("comboboxClients").contents();
		assertThat(clientComboboxContents).contains("CLIENT-00003, client 3");

	}

	@Test
	@GUITest
	public void testRemoveClient() {
		window.comboBox("yearsCombobox").selectItem("2025");
		window.list("clientsList").selectItem(1);
		window.button(JButtonMatcher.withText("Rimuovi cliente")).click();
		String[] clientListContents = window.list("clientsList").contents();
		assertThat(clientListContents).noneMatch(e -> e.contains("CLIENT-00002"));

		String[] clientComboboxContents = window.comboBox("comboboxClients").contents();
		assertThat(clientComboboxContents).noneMatch(e -> e.contains("CLIENT-00002"));

		String[][] tableContents = window.table("OrdersTable").contents();
		assertThat(tableContents[0]).containsOnly(new String[] { "ORDER-00001", "client 1",
				Date.from(LocalDate.of(2025, 7, 31).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"10.0" });

		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2025" + " è di " + "10,00€");

	}

	@Test
	@GUITest
	public void testRemoveClientWhenClientNoExists() {
		window.comboBox("yearsCombobox").selectItem("2024");
		removeClientFromDatabase("CLIENT-00001");
		window.list("clientsList").selectItem(0);
		window.button(JButtonMatcher.withText("Rimuovi cliente")).click();
		assertThat(window.textBox("panelClientErrorMessage").text()).contains("CLIENT-00001");
		String[] clientListContents = window.list("clientsList").contents();
		assertThat(clientListContents).noneMatch(e -> e.contains("CLIENT-00001"));

		String[] clientComboboxContents = window.comboBox("comboboxClients").contents();
		assertThat(clientComboboxContents).noneMatch(e -> e.contains("CLIENT-00001"));

		String[][] tableContents = window.table("OrdersTable").contents();
		assertThat(tableContents[0]).containsOnly(new String[] { "ORDER-00004", "client 2",
				Date.from(LocalDate.of(2024, 5, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"40.0" });

		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2024" + " è di " + "40,00€");

	}

}
