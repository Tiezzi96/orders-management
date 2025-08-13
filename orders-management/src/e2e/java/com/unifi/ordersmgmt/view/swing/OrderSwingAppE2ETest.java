package com.unifi.ordersmgmt.view.swing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.swing.launcher.ApplicationLauncher.application;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import javax.swing.JFrame;

import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.core.GenericTypeMatcher;
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

	@Test
	@GUITest
	public void testOnStartShowAllClientsInDBAreShown() {
		assertThat(window.list("clientsList").contents())
				.anySatisfy(e -> assertThat(e).isEqualTo("CLIENT-00001, client 1"))
				.anySatisfy(e -> assertThat(e).isEqualTo("CLIENT-00002, client 2"));
	}

}
