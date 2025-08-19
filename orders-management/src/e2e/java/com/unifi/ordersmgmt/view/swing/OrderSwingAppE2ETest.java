package com.unifi.ordersmgmt.view.swing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.swing.launcher.ApplicationLauncher.application;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

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

	private void removeOrderFromDatabase(String orderID) {
		mongoClient.getDatabase(DB_NAME).getCollection(ORDER_COLLECTION_NAME).deleteOne(Filters.eq("id", orderID));
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

	@Test
	@GUITest
	public void testShowAllOrders() {
		window.comboBox("yearsCombobox").selectItem("2025");
		window.list("clientsList").selectItem(0);
		window.button(JButtonMatcher
				.withText(Pattern.compile("<html><center>Visualizza ordini<br>di tutti i clienti</center></html>")))
				.click();
		window.list("clientsList").requireNoSelection();
		String[][] tableContents = window.table("OrdersTable").contents();
		assertThat(tableContents[0]).containsExactly(new String[] { "ORDER-00001", "client 1",
				Date.from(LocalDate.of(2025, 7, 31).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"10.0" });
		assertThat(tableContents[1]).containsExactly(new String[] { "ORDER-00002", "client 2",
				Date.from(LocalDate.of(2025, 7, 31).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"20.0" });

		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2025" + " è di " + "30,00€");

	}

	@Test
	@GUITest
	public void testAddNewOrder() {
		window.comboBox("yearsCombobox").selectItem("2024");
		window.comboBox("comboboxClients").selectItem(Pattern.compile("CLIENT-00002, client 2"));
		window.textBox("textField_dayOfDateOrder").enterText("4");
		window.textBox("textField_monthOfDateOrder").enterText("5");
		window.textBox("textField_yearOfDateOrder").enterText("" + 2024);
		window.textBox("textField_revenueOrder").enterText("100.25");
		window.button(JButtonMatcher.withText(Pattern.compile("Aggiungi ordine"))).click();
		window.list("clientsList").requireNoSelection();
		String[][] tableContents = window.table("OrdersTable").contents();
		assertThat(tableContents[0]).containsExactly(new String[] { "ORDER-00003", "client 1",
				Date.from(LocalDate.of(2024, 5, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"30.0" });
		assertThat(tableContents[1]).containsExactly(new String[] { "ORDER-00004", "client 2",
				Date.from(LocalDate.of(2024, 5, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"40.0" });
		assertThat(tableContents[2]).containsExactly(new String[] { "ORDER-00005", "client 2",
				Date.from(LocalDate.of(2024, 5, 4).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"100.25" });

		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2024" + " è di " + "170,25€");

	}

	@Test
	@GUITest
	public void testAddNewOrderOfDifferentYearThanYearSelected() {
		window.comboBox("yearsCombobox").selectItem("2025");
		window.comboBox("comboboxClients").selectItem(Pattern.compile("CLIENT-00001, client 1"));
		window.textBox("textField_dayOfDateOrder").enterText("13");
		window.textBox("textField_monthOfDateOrder").enterText("5");
		window.textBox("textField_yearOfDateOrder").enterText("" + 2024);
		window.textBox("textField_revenueOrder").enterText("100.25");
		window.button(JButtonMatcher.withText(Pattern.compile("Aggiungi ordine"))).click();
		window.list("clientsList").requireNoSelection();
		String[][] tableContents = window.table("OrdersTable").contents();
		assertThat(tableContents[0]).containsExactly(new String[] { "ORDER-00001", "client 1",
				Date.from(LocalDate.of(2025, 7, 31).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"10.0" });
		assertThat(tableContents[1]).containsExactly(new String[] { "ORDER-00002", "client 2",
				Date.from(LocalDate.of(2025, 7, 31).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"20.0" });

		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2025" + " è di " + "30,00€");

	}

	@Test
	@GUITest
	public void testAddNewOrderWhenClientSelectedNoExists() {
		window.comboBox("yearsCombobox").selectItem("2025");
		window.comboBox("comboboxClients").selectItem(Pattern.compile("CLIENT-00001, client 1"));
		window.textBox("textField_dayOfDateOrder").enterText("13");
		window.textBox("textField_monthOfDateOrder").enterText("5");
		window.textBox("textField_yearOfDateOrder").enterText("" + 2025);
		window.textBox("textField_revenueOrder").enterText("100.25");
		removeClientFromDatabase("CLIENT-00001");
		window.button(JButtonMatcher.withText(Pattern.compile("Aggiungi ordine"))).click();
		assertThat(window.textBox("panelClientErrorMessage").text()).contains("CLIENT-00001");
		String[] clientListContents = window.list("clientsList").contents();
		assertThat(clientListContents).noneMatch(e -> e.contains("CLIENT-00001"));

		String[] clientComboboxContents = window.comboBox("comboboxClients").contents();
		assertThat(clientComboboxContents).noneMatch(e -> e.contains("CLIENT-00001"));
		window.table("OrdersTable").requireRowCount(1);
		String[][] tableContents = window.table("OrdersTable").contents();

		assertThat(tableContents[0]).containsExactly(new String[] { "ORDER-00002", "client 2",
				Date.from(LocalDate.of(2025, 7, 31).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"20.0" });

		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2025" + " è di " + "20,00€");

	}

	@Test
	@GUITest
	public void testDeleteOrder() {
		window.comboBox("yearsCombobox").selectItem("2025");
		window.table("OrdersTable").selectRows(1);
		window.button(JButtonMatcher.withText(Pattern.compile("<html><center>Rimuovi<br>ordine</center></html>")))
				.click();
		String[][] tableContents = window.table("OrdersTable").contents();

		assertThat(tableContents[0]).containsExactly(new String[] { "ORDER-00001", "client 1",
				Date.from(LocalDate.of(2025, 7, 31).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"10.0" });
		List<String[]> rows = Arrays.asList(tableContents);
		assertThat(rows).isNotEmpty()
				.doesNotContain(new String[] { "ORDER-00002", "client 2", Date
						.from(LocalDate.of(2025, 7, 31).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
						"20.0" });

		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2025" + " è di " + "10,00€");

	}

	@Test
	@GUITest
	public void testDeleteOrderWhenOrderNoExists() {
		window.comboBox("yearsCombobox").selectItem("2024");
		window.table("OrdersTable").selectRows(0);
		removeOrderFromDatabase("ORDER-00003");
		window.button(JButtonMatcher.withText(Pattern.compile("<html><center>Rimuovi<br>ordine</center></html>")))
				.click();
		assertThat(window.textBox("panelOrderErrorMessage").text()).contains("ORDER-00003");
		String[][] tableContents = window.table("OrdersTable").contents();
		List<String[]> rows = Arrays.asList(tableContents);
		assertThat(rows)
				.doesNotContain(
						new String[] { "ORDER-00003", "client 1",
								Date.from(LocalDate.of(2024, 5, 1).atStartOfDay(ZoneId.systemDefault()).toInstant())
										.toString(),
								"30.0" })
				.containsExactly(new String[] { "ORDER-00004", "client 2",
						Date.from(LocalDate.of(2024, 5, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
						"40.0" });
		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2024" + " è di " + "40,00€");

	}

	@Test
	@GUITest
	public void testDeleteOrderWhenClientNoExists() {
		window.comboBox("yearsCombobox").selectItem("2025");
		window.table("OrdersTable").selectRows(1);
		removeClientFromDatabase("CLIENT-00002");
		window.button(JButtonMatcher.withText(Pattern.compile("<html><center>Rimuovi<br>ordine</center></html>")))
				.click();
		assertThat(window.textBox("panelClientErrorMessage").text()).contains("CLIENT-00002");
		String[][] tableContents = window.table("OrdersTable").contents();
		List<String[]> rows = Arrays.asList(tableContents);
		assertThat(rows)
				.doesNotContain(new String[] { "ORDER-00002", "client 2",
						Date.from(LocalDate.of(2025, 7, 31).atStartOfDay(ZoneId.systemDefault()).toInstant())
								.toString(),
						"20.0" })
				.containsExactly(new String[] { "ORDER-00001", "client 1", Date
						.from(LocalDate.of(2025, 7, 31).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
						"10.0" });
		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2025" + " è di " + "10,00€");

	}

	@Test
	@GUITest
	public void testModifyOrder() {
		window.comboBox("yearsCombobox").selectItem("2025");
		window.table("OrdersTable").selectRows(1);
		window.comboBox("comboboxClients").selectItem(Pattern.compile("CLIENT-00001, client 1"));
		window.textBox("textField_dayOfDateOrder").setText("");
		window.textBox("textField_monthOfDateOrder").setText("");
		window.textBox("textField_yearOfDateOrder").setText("");
		window.textBox("textField_revenueOrder").setText("");
		window.textBox("textField_dayOfDateOrder").enterText("22");
		window.textBox("textField_monthOfDateOrder").enterText("8");
		window.textBox("textField_yearOfDateOrder").enterText("" + 2025);
		window.textBox("textField_revenueOrder").enterText("80.75");
		window.button(JButtonMatcher.withText(Pattern.compile("<html><center>Modifica<br>ordine</center></html>")))
				.click();
		String[][] tableContents = window.table("OrdersTable").contents();
		window.table("OrdersTable").requireRowCount(2);
		assertThat(tableContents[0]).containsExactly(new String[] { "ORDER-00001", "client 1",
				Date.from(LocalDate.of(2025, 7, 31).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"10.0" });
		assertThat(tableContents[1]).containsExactly(new String[] { "ORDER-00002", "client 1",
				Date.from(LocalDate.of(2025, 8, 22).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"80.75" });

		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2025" + " è di " + "90,75€");

	}

	@Test
	@GUITest
	public void testModifyOrderWhenYearChangedIsDifferentThanYearSelected() {
		window.comboBox("yearsCombobox").selectItem("2024");
		window.table("OrdersTable").selectRows(1);
		window.comboBox("comboboxClients").selectItem(Pattern.compile("CLIENT-00001, client 1"));
		window.textBox("textField_dayOfDateOrder").setText("");
		window.textBox("textField_monthOfDateOrder").setText("");
		window.textBox("textField_yearOfDateOrder").setText("");
		window.textBox("textField_revenueOrder").setText("");
		window.textBox("textField_dayOfDateOrder").enterText("22");
		window.textBox("textField_monthOfDateOrder").enterText("8");
		window.textBox("textField_yearOfDateOrder").enterText("" + 2025);
		window.textBox("textField_revenueOrder").enterText("80.75");
		window.button(JButtonMatcher.withText(Pattern.compile("<html><center>Modifica<br>ordine</center></html>")))
				.click();
		String[][] tableContents = window.table("OrdersTable").contents();
		window.table("OrdersTable").requireRowCount(1);
		assertThat(tableContents[0]).containsExactly(new String[] { "ORDER-00003", "client 1",
				Date.from(LocalDate.of(2024, 5, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"30.0" });

		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2024" + " è di " + "30,00€");

	}

	@Test
	@GUITest
	public void testModifyOrderWhenClientChangedIsDifferentThanClientSelected() {
		window.comboBox("yearsCombobox").selectItem("2025");
		window.list("clientsList").selectItem("CLIENT-00001, client 1");
		window.table("OrdersTable").selectRows(0);
		window.comboBox("comboboxClients").selectItem(Pattern.compile("CLIENT-00002, client 2"));
		window.textBox("textField_dayOfDateOrder").setText("");
		window.textBox("textField_monthOfDateOrder").setText("");
		window.textBox("textField_yearOfDateOrder").setText("");
		window.textBox("textField_revenueOrder").setText("");
		window.textBox("textField_dayOfDateOrder").enterText("22");
		window.textBox("textField_monthOfDateOrder").enterText("8");
		window.textBox("textField_yearOfDateOrder").enterText("" + 2025);
		window.textBox("textField_revenueOrder").enterText("80.75");
		window.button(JButtonMatcher.withText(Pattern.compile("<html><center>Modifica<br>ordine</center></html>")))
				.click();
		assertThat(window.textBox("panelOrderErrorMessage").text()).contains("CLIENT-00001");
		window.table("OrdersTable").requireRowCount(0);
		window.label("revenueLabel").requireText("");

	}

	@Test
	@GUITest
	public void testModifyOrderWhenClientNoExists() {
		window.comboBox("yearsCombobox").selectItem("2024");
		window.table("OrdersTable").selectRows(0);
		window.comboBox("comboboxClients").selectItem(Pattern.compile("CLIENT-00002, client 2"));
		window.textBox("textField_dayOfDateOrder").setText("");
		window.textBox("textField_monthOfDateOrder").setText("");
		window.textBox("textField_yearOfDateOrder").setText("");
		window.textBox("textField_revenueOrder").setText("");
		window.textBox("textField_dayOfDateOrder").enterText("15");
		window.textBox("textField_monthOfDateOrder").enterText("5");
		window.textBox("textField_yearOfDateOrder").enterText("" + 2024);
		window.textBox("textField_revenueOrder").enterText("80.75");
		removeClientFromDatabase("CLIENT-00002");
		window.button(JButtonMatcher.withText(Pattern.compile("<html><center>Modifica<br>ordine</center></html>")))
				.click();
		assertThat(window.textBox("panelClientErrorMessage").text()).contains("CLIENT-00002");
		String[][] tableContents = window.table("OrdersTable").contents();
		window.table("OrdersTable").requireRowCount(1);
		assertThat(tableContents[0]).containsExactly(new String[] { "ORDER-00003", "client 1",
				Date.from(LocalDate.of(2024, 5, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"30.0" });

		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2024" + " è di " + "30,00€");

	}

	@Test
	@GUITest
	public void testModifyOrderWhenOrderNoExist() {
		window.comboBox("yearsCombobox").selectItem("2025");
		window.table("OrdersTable").selectRows(1);
		window.comboBox("comboboxClients").selectItem(Pattern.compile("CLIENT-00001, client 1"));
		window.textBox("textField_dayOfDateOrder").setText("");
		window.textBox("textField_monthOfDateOrder").setText("");
		window.textBox("textField_yearOfDateOrder").setText("");
		window.textBox("textField_revenueOrder").setText("");
		window.textBox("textField_dayOfDateOrder").enterText("15");
		window.textBox("textField_monthOfDateOrder").enterText("9");
		window.textBox("textField_yearOfDateOrder").enterText("" + 2025);
		window.textBox("textField_revenueOrder").enterText("60.75");
		removeOrderFromDatabase("ORDER-00002");
		window.button(JButtonMatcher.withText(Pattern.compile("<html><center>Modifica<br>ordine</center></html>")))
				.click();
		assertThat(window.textBox("panelOrderErrorMessage").text()).contains("ORDER-00002");
		String[][] tableContents = window.table("OrdersTable").contents();
		window.table("OrdersTable").requireRowCount(1);
		assertThat(tableContents[0]).containsExactly(new String[] { "ORDER-00001", "client 1",
				Date.from(LocalDate.of(2025, 7, 31).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"10.0" });

		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2025" + " è di " + "10,00€");

	}

	@Test
	@GUITest
	public void testAddNewOrderForClientDifferentFromClientSelectedWithNewYear() {
		window.comboBox("yearsCombobox").selectItem("-- Nessun anno --");

		window.list("clientsList").selectItem(1);
		window.comboBox("comboboxClients").selectItem(Pattern.compile("CLIENT-00001, client 1"));
		window.textBox("textField_dayOfDateOrder").enterText("13");
		window.textBox("textField_monthOfDateOrder").enterText("5");
		window.textBox("textField_yearOfDateOrder").enterText("" + 2021);
		window.textBox("textField_revenueOrder").enterText("100.25");
		window.button(JButtonMatcher.withText(Pattern.compile("Aggiungi ordine"))).click();
		window.comboBox("yearsCombobox").requireNoSelection();
		String[][] tableContents = window.table("OrdersTable").contents();
		assertThat(tableContents[0]).containsExactly(new String[] { "ORDER-00004", "client 2",
				Date.from(LocalDate.of(2024, 5, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"40.0" });
		assertThat(tableContents[1]).containsExactly(new String[] { "ORDER-00002", "client 2",
				Date.from(LocalDate.of(2025, 7, 31).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"20.0" });

		window.label("revenueLabel")
				.requireText("Il costo totale degli ordini del cliente " + "CLIENT-00002" + " è di " + "60,00€");
		assertThat(window.comboBox("yearsCombobox").contents()).contains("2021");
	}

	@Test
	@GUITest
	public void testAddNewOrderWithNoClientSelectedNoYearSelected() {
		window.comboBox("yearsCombobox").selectItem("-- Nessun anno --");
		window.list("clientsList").clearSelection();

		window.comboBox("comboboxClients").selectItem(Pattern.compile("CLIENT-00001, client 1"));
		window.textBox("textField_dayOfDateOrder").enterText("13");
		window.textBox("textField_monthOfDateOrder").enterText("5");
		window.textBox("textField_yearOfDateOrder").enterText("" + 2021);
		window.textBox("textField_revenueOrder").enterText("100.25");
		window.button(JButtonMatcher.withText(Pattern.compile("Aggiungi ordine"))).click();

		window.comboBox("yearsCombobox").requireNoSelection();
		window.list("clientsList").requireNoSelection();

		String[][] tableContents = window.table("OrdersTable").contents();
		assertThat(tableContents[0]).containsExactly(new String[] { "ORDER-00005", "client 1",
				Date.from(LocalDate.of(2021, 5, 13).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"100.25" });
		assertThat(tableContents[1]).containsExactly(new String[] { "ORDER-00003", "client 1",
				Date.from(LocalDate.of(2024, 5, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"30.0" });
		assertThat(tableContents[2]).containsExactly(new String[] { "ORDER-00004", "client 2",
				Date.from(LocalDate.of(2024, 5, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"40.0" });
		assertThat(tableContents[3]).containsExactly(new String[] { "ORDER-00001", "client 1",
				Date.from(LocalDate.of(2025, 7, 31).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"10.0" });
		assertThat(tableContents[4]).containsExactly(new String[] { "ORDER-00002", "client 2",
				Date.from(LocalDate.of(2025, 7, 31).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"20.0" });

		window.label("revenueLabel").requireText("Il costo totale degli ordini presenti nel DB" + " è di " + "200,25€");
		assertThat(window.comboBox("yearsCombobox").contents()).contains("2021");
	}

	@Test
	@GUITest
	public void testModifyOrderWhenNoYearsSelectedAndClientChangedIsDifferentFromClientSelected() {
		window.comboBox("yearsCombobox").selectItem("-- Nessun anno --");
		window.list("clientsList").selectItem("CLIENT-00001, client 1");
		window.table("OrdersTable").selectRows(0);
		window.comboBox("comboboxClients").selectItem(Pattern.compile("CLIENT-00002, client 2"));
		window.textBox("textField_dayOfDateOrder").setText("");
		window.textBox("textField_monthOfDateOrder").setText("");
		window.textBox("textField_yearOfDateOrder").setText("");
		window.textBox("textField_revenueOrder").setText("");
		window.textBox("textField_dayOfDateOrder").enterText("22");
		window.textBox("textField_monthOfDateOrder").enterText("8");
		window.textBox("textField_yearOfDateOrder").enterText("" + 2019);
		window.textBox("textField_revenueOrder").enterText("80.75");
		window.button(JButtonMatcher.withText(Pattern.compile("<html><center>Modifica<br>ordine</center></html>")))
				.click();
		window.table("OrdersTable").requireRowCount(1);
		window.textBox("panelOrderErrorMessage").requireEmpty();
		assertThat(window.comboBox("yearsCombobox").contents()).contains("" + 2019);
		window.label("revenueLabel")
				.requireText("Il costo totale degli ordini del cliente " + "CLIENT-00001" + " è di " + "10,00€");
	}

	@Test
	@GUITest
	public void testModifyOrderWhenNoYearsSelectedNoClientSelected() {
		window.comboBox("yearsCombobox").selectItem("-- Nessun anno --");
		window.list("clientsList").clearSelection();
		window.table("OrdersTable").selectRows(0);
		window.comboBox("comboboxClients").selectItem(Pattern.compile("CLIENT-00002, client 2"));
		window.textBox("textField_dayOfDateOrder").setText("");
		window.textBox("textField_monthOfDateOrder").setText("");
		window.textBox("textField_yearOfDateOrder").setText("");
		window.textBox("textField_revenueOrder").setText("");
		window.textBox("textField_dayOfDateOrder").enterText("22");
		window.textBox("textField_monthOfDateOrder").enterText("8");
		window.textBox("textField_yearOfDateOrder").enterText("" + 2019);
		window.textBox("textField_revenueOrder").enterText("80.75");
		window.button(JButtonMatcher.withText(Pattern.compile("<html><center>Modifica<br>ordine</center></html>")))
				.click();
		window.table("OrdersTable").requireRowCount(4);
		String[][] tableContents = window.table("OrdersTable").contents();
		assertThat(tableContents[0]).containsExactly(new String[] { "ORDER-00003", "client 2",
				Date.from(LocalDate.of(2019, 8, 22).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"80.75" });
		assertThat(tableContents[1]).containsExactly(new String[] { "ORDER-00004", "client 2",
				Date.from(LocalDate.of(2024, 5, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"40.0" });
		assertThat(tableContents[2]).containsExactly(new String[] { "ORDER-00001", "client 1",
				Date.from(LocalDate.of(2025, 7, 31).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"10.0" });
		assertThat(tableContents[3]).containsExactly(new String[] { "ORDER-00002", "client 2",
				Date.from(LocalDate.of(2025, 7, 31).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"20.0" });
		window.textBox("panelOrderErrorMessage").requireEmpty();
		assertThat(window.comboBox("yearsCombobox").contents()).contains("" + 2019);
		window.label("revenueLabel").requireText("Il costo totale degli ordini presenti nel DB è di 150,75€");
	}

	@Test
	@GUITest
	public void testShowAllOrdersClientsNoYearsSelected() {
		window.list("clientsList").selectItem(0);
		window.comboBox("yearsCombobox").selectItem("-- Nessun anno --");
		window.button(JButtonMatcher.withText("<html><center>Visualizza ordini<br>di tutti i clienti</center></html>"))
				.click();
		window.table("OrdersTable").requireRowCount(4);
		String[][] tableContents = window.table("OrdersTable").contents();
		assertThat(tableContents[0]).containsExactly("ORDER-00003", "client 1",
				Date.from(LocalDate.of(2024, 5, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"30.0");
		assertThat(tableContents[1]).containsExactly("ORDER-00004", "client 2",
				Date.from(LocalDate.of(2024, 5, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"40.0");
		assertThat(tableContents[2]).containsExactly("ORDER-00001", "client 1",
				Date.from(LocalDate.of(2025, 7, 31).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"10.0");
		assertThat(tableContents[3]).containsExactly("ORDER-00002", "client 2",
				Date.from(LocalDate.of(2025, 7, 31).atStartOfDay(ZoneId.systemDefault()).toInstant()).toString(),
				"20.0");
		window.label("revenueLabel").requireText("Il costo totale degli ordini presenti nel DB è di " + "100,00€");
	}

}
