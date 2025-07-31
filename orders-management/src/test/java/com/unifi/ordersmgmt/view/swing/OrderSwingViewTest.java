package com.unifi.ordersmgmt.view.swing;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.atIndex;
import static org.assertj.swing.data.TableCell.row;
import static org.junit.Assert.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.ListSelectionModel;
import javax.swing.text.JTextComponent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.annotation.RunsInEDT;
import org.assertj.swing.core.MouseButton;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.core.matcher.JLabelMatcher;
import org.assertj.swing.core.matcher.JTextComponentMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JLabelFixture;
import org.assertj.swing.junit.runner.GUITestRunner;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.assertj.swing.timing.Pause;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.unifi.ordersmgmt.controller.OrderController;
import com.unifi.ordersmgmt.model.Client;
import com.unifi.ordersmgmt.model.Order;

@RunWith(GUITestRunner.class)
public class OrderSwingViewTest extends AssertJSwingJUnitTestCase {

	private AutoCloseable autoCloseable;
	private OrderSwingView orderSwingView;
	private static final Logger logger = LogManager.getLogger(OrderSwingViewTest.class);
	@Mock
	private OrderController orderController;
	private FrameFixture window;

	@Override
	public void onSetUp() throws Exception {

		autoCloseable = MockitoAnnotations.openMocks(this);
		GuiActionRunner.execute(() -> {
			orderSwingView = new OrderSwingView();
			orderSwingView.setOrderController(orderController);
			return orderSwingView;
		});

		window = new FrameFixture(robot(), orderSwingView);
		window.show();
		// serve per consentire al OS di dare la priorità alla finestra
		GuiActionRunner.execute(() -> {
			orderSwingView.setAlwaysOnTop(true);
			orderSwingView.toFront();
			orderSwingView.requestFocus();
		});

		Pause.pause(300);

		// Rimuove alwaysOnTop dopo il focus
		GuiActionRunner.execute(() -> orderSwingView.setAlwaysOnTop(false));
	}

	@Override
	protected void onTearDown() throws Exception {
		window.cleanUp();
		autoCloseable.close();
	}

	@Test
	@GUITest
	public void testInitializeOrderView() {
		window.label(JLabelMatcher.withText("CLIENTI"));
		window.list("clientsList");
		window.label(JLabelMatcher.withText("NUOVO CLIENTE"));
		window.label(JLabelMatcher.withText("INFO ORDINE"));
		window.label(JLabelMatcher.withText("Identificativo"));
		window.textBox("panelClientErrorMessage").requireText("");
		window.textBox("panelClientErrorMessage").requireNotEditable();
		window.textBox("textField_clientName").requireEnabled();

		window.textBox("textField_clientName").requireEnabled();
		window.textBox("textField_dayOfDateOrder").requireEnabled();
		window.textBox("textField_monthOfDateOrder").requireEnabled();
		window.textBox("textField_yearOfDateOrder").requireEnabled();
		window.textBox("textField_revenueOrder").requireEnabled();

		window.button(JButtonMatcher.withText("Aggiungi cliente")).requireDisabled();
		window.button(JButtonMatcher.withText("Rimuovi cliente")).requireDisabled();
		window.button(JButtonMatcher.withText("Aggiungi ordine")).requireDisabled();
		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).requireDisabled();
		window.button(JButtonMatcher.withText("<html><center>Rimuovi<br>ordine</center></html>")).requireDisabled();
		window.button(
				JButtonMatcher.withText(Pattern.compile("<html><center>Visualizza ordini<br>di tutti i clienti</center></html>")))
				.requireNotVisible();

		window.label(JLabelMatcher.withName("revenueLabel"));
		window.comboBox("comboboxClients");
		window.comboBox("yearsCombobox");
		window.textBox("panelOrderErrorMessage").requireText("");
		window.textBox("panelOrderErrorMessage").requireNotEditable();

		window.table("OrdersTable").requireColumnCount(4);
		window.table("OrdersTable").requireColumnNamed("Data");
		window.table("OrdersTable").requireColumnNamed("Importo ($)");
		window.table("OrdersTable").requireColumnNamed("Cliente");
		window.table("OrdersTable").requireColumnNamed("Id");

	}

	@Test
	@GUITest
	public void testShowAllClientsShouldAddClientsInOrderToComboboxAndClientsList() {
		Client client1 = new Client("1", "test id 1");
		Client client2 = new Client("2", "test id 2");
		GuiActionRunner.execute(() -> {
			orderSwingView.showAllClients(asList(client1, client2));
		});
		String[] listClients = window.list("clientsList").contents();
		assertThat(listClients).containsExactly(client1.toString(), client2.toString());
		String[] comboboxClientslist = window.comboBox("comboboxClients").contents();
		assertThat(comboboxClientslist).containsExactly(client1.toString(), client2.toString());

	}

	@Test
	@GUITest
	public void testShowAllClientsShouldRemovedPreviousContentBeforeAddClientsInOrderToClientListAndCombobox() {
		Client firstClient = new Client("1", "first id");
		Client secondClient = new Client("2", "second id");
		GuiActionRunner.execute(() -> {
			orderSwingView.getClientListModel().add(0, secondClient);
			orderSwingView.getComboboxClientsModel().addElement(secondClient);
			orderSwingView.showAllClients(asList(firstClient, secondClient));
		});
		assertThat(window.list("clientsList").contents()).containsExactly(firstClient.toString(),
				secondClient.toString());
		assertThat(window.comboBox("comboboxClients").contents()).containsExactly(firstClient.toString(),
				secondClient.toString());

	}

	@Test
	@GUITest
	public void testClientRemovedShouldRemovedClientSelectedFromListsAndCombobox() {
		Client clientToRemove = new Client("1", "client to remove id");
		Client anotherClient = new Client("2", "client id 2");
		GuiActionRunner.execute(() -> {
			orderSwingView.getClientListModel().addElement(clientToRemove);
			orderSwingView.getClientListModel().addElement(anotherClient);
			orderSwingView.getComboboxClientsModel().addElement(clientToRemove);
			orderSwingView.getComboboxClientsModel().addElement(anotherClient);
		});

		window.list("clientsList").selectItem(0);
		GuiActionRunner.execute(() -> {
			orderSwingView.clientRemoved(clientToRemove);
		});
		assertThat(window.list("clientsList").contents()).containsExactly(anotherClient.toString());
		assertThat(window.comboBox("comboboxClients").contents()).containsExactly(anotherClient.toString());
		window.list("clientsList").requireNoSelection();
	}

	@Test
	@GUITest
	public void testShowErrorClientShouldShowErrorInTheClientErrorLabel() {
		Client newClient = new Client("1", "new Client id");
		GuiActionRunner.execute(() -> {
			orderSwingView.showErrorClient("error message", newClient);
		});
		window.textBox("panelClientErrorMessage").requireText("error message: " + newClient.toString());

	}

	@Test
	@GUITest
	public void testWhenTextFieldAreNotEmptyAddClientButtonShouldBeEnabled() {
		window.textBox(JTextComponentMatcher.withName("textField_clientName")).enterText("test");
		window.button(JButtonMatcher.withText("Aggiungi cliente")).requireEnabled();
		window.textBox(JTextComponentMatcher.withName("textField_clientName")).setText("");
		window.textBox(JTextComponentMatcher.withName("textField_clientName")).enterText("  ");
		window.button(JButtonMatcher.withText("Aggiungi cliente")).requireDisabled();
	}

	@Test
	@GUITest
	public void testClientAddedShouldAddedClientToListAndComboboxListAndResetTextFieldAndErrorClient() {
		Client clientToAdd = new Client("1", "test 1");
		GuiActionRunner.execute(() -> {
			orderSwingView.clientAdded(clientToAdd);
		});
		String[] clients = window.list("clientsList").contents();
		assertThat(clients).containsExactly(clientToAdd.toString());
		String[] clientsCombobox = window.comboBox("comboboxClients").contents();
		assertThat(clientsCombobox).containsExactly(clientToAdd.toString());
		window.textBox(JTextComponentMatcher.withName("textField_clientName")).requireText("");
		window.textBox(JTextComponentMatcher.withName("panelClientErrorMessage")).requireText("");
		window.button(JButtonMatcher.withText("Aggiungi cliente")).requireDisabled();

	}

	@Test
	@GUITest
	public void testClientAddedShouldAddedClientToListAndComboboxInOrderAndResetTextFieldAndErrorClient() {
		Client firstClient = new Client("1", "first client id");
		Client secondClient = new Client("2", "second client id");
		Client thirdClient = new Client("3", "third client id");
		GuiActionRunner.execute(() -> {
			orderSwingView.getClientListModel().add(0, thirdClient);
			orderSwingView.getComboboxClientsModel().addElement(thirdClient);
			orderSwingView.getClientListModel().add(1, secondClient);
			orderSwingView.getComboboxClientsModel().addElement(secondClient);
		});
		window.list("clientsList").selectItem(0);
		window.comboBox("comboboxClients").selectItem(0);
		GuiActionRunner.execute(() -> {
			orderSwingView.clientAdded(firstClient);
		});
		assertThat(window.list("clientsList").contents()).containsExactly(firstClient.toString(),
				secondClient.toString(), thirdClient.toString());
		assertThat(window.comboBox("comboboxClients").contents()).containsExactly(firstClient.toString(),
				secondClient.toString(), thirdClient.toString());
		window.list("clientsList").requireSelectedItems(thirdClient.toString());
		window.comboBox("comboboxClients").requireSelection(thirdClient.toString());
		window.textBox(JTextComponentMatcher.withName("textField_clientName")).requireText("");
		window.textBox(JTextComponentMatcher.withName("panelClientErrorMessage")).requireText("");
		window.button(JButtonMatcher.withText("Aggiungi cliente")).requireDisabled();

	}

	@Test
	@GUITest
	public void testAddClientButtonShouldDelegateOrderControllerAddClientAndResetErrorLabel() {
		// se il client non ha id verrà assegnato dal controller
		window.textBox(JTextComponentMatcher.withName("textField_clientName")).enterText("test client 1");
		window.button(JButtonMatcher.withText("Aggiungi cliente")).click();
		verify(orderController).addClient(new Client("test client 1"));
		window.textBox(JTextComponentMatcher.withName("panelClientErrorMessage")).requireText("");
		window.textBox(JTextComponentMatcher.withName("textField_clientName")).requireText("");

	}

	@Test
	@GUITest
	public void testDeleteClientButtonShouldBeEnabledOnlyWhenAClientIsSelected() {
		Client clientToDelete = new Client("1", "client to delete");
		GuiActionRunner.execute(() -> {
			orderSwingView.getClientListModel().addElement(clientToDelete);
		});
		window.list("clientsList").selectItem(0);
		window.button(JButtonMatcher.withText("Rimuovi cliente")).requireEnabled();
		window.list("clientsList").clearSelection();
		window.button(JButtonMatcher.withText("Rimuovi cliente")).requireDisabled();

	}

	@Test
	@GUITest
	public void testDeleteClientButtonShouldDelegateToOrderControllerDeleteClient() {
		Client firstClient = new Client("1", "first client id");
		Client secondClient = new Client("2", "second client id");
		GuiActionRunner.execute(() -> {
			orderSwingView.getClientListModel().addElement(firstClient);
			orderSwingView.getClientListModel().addElement(secondClient);
		});
		window.list("clientsList").selectItem(0);
		window.button(JButtonMatcher.withText("Rimuovi cliente")).click();
		verify(orderController).deleteClient(new Client(firstClient.getIdentifier(), firstClient.getIdentifier()));

	}

	@Test
	@GUITest
	public void testShowAllOrdersAddOrdersToOrderTable() {
		Client newClient = new Client("1", "newClient id");
		Order firstOrder = new Order("1", newClient, new Date(), 10);
		GuiActionRunner.execute(() -> {
			orderSwingView.showAllOrders(asList(firstOrder));
		});
		window.table("OrdersTable").requireRowCount(1);
		assertThat(window.table("OrdersTable").contents()[0]).containsExactly(firstOrder.getIdentifier().toString(),
				firstOrder.getClient().getName(), firstOrder.getDate().toString(),
				String.valueOf(firstOrder.getPrice()));
	}

	@Test
	@GUITest
	public void testShowAllOrdersRemovedPreviousOrdersBeforeAddOrdersToOrderTable() {
		Client newClient = new Client("1", "newClient id");
		Order firstOrder = new Order("1", newClient, new Date(), 10);
		Order secondOrder = new Order("2", newClient, new Date(), 10);
		GuiActionRunner.execute(() -> {
			DefaultComboBoxModel<Integer> comboboxYearsModel = orderSwingView.getComboboxYearsModel();
			comboboxYearsModel.addElement(2025);
			comboboxYearsModel.setSelectedItem(2025);
			orderSwingView.getOrderTableModel().addOrder(secondOrder);
			orderSwingView.showAllOrders(asList(firstOrder, secondOrder));
		});
		window.table("OrdersTable").requireRowCount(2);
		assertThat(window.table("OrdersTable").contents()[0]).containsExactly(firstOrder.getIdentifier().toString(),
				firstOrder.getClient().getName(), firstOrder.getDate().toString(),
				String.valueOf(firstOrder.getPrice()));
		assertThat(window.table("OrdersTable").contents()[1]).containsExactly(secondOrder.getIdentifier().toString(),
				secondOrder.getClient().getName(), secondOrder.getDate().toString(),
				String.valueOf(secondOrder.getPrice()));
	}

	@Test
	@GUITest
	public void testShowAllOrdersRemovedPreviousOrdersBeforeAddOrdersInOrderToOrderTable() {
		Client newClient = new Client("1", "newClient id");
		LocalDateTime previousLocalDateTime = LocalDateTime.of(2025, 3, 25, 14, 30, 45);
		LocalDateTime nextLocalDateTime = LocalDateTime.of(2025, 3, 25, 15, 30, 45);
		Order firstOrder = new Order("1", newClient,
				Date.from(previousLocalDateTime.atZone(ZoneId.systemDefault()).toInstant()), 10);
		Order secondOrder = new Order("2", newClient,
				Date.from(nextLocalDateTime.atZone(ZoneId.systemDefault()).toInstant()), 10);
		GuiActionRunner.execute(() -> {
			DefaultComboBoxModel<Integer> comboboxYearsModel = orderSwingView.getComboboxYearsModel();
			comboboxYearsModel.addElement(2025);
			comboboxYearsModel.setSelectedItem(2025);
			orderSwingView.getOrderTableModel().addOrder(secondOrder);
			orderSwingView.showAllOrders(asList(secondOrder, firstOrder));
		});
		window.table("OrdersTable").requireRowCount(2);
		assertThat(window.table("OrdersTable").contents()[0]).containsExactly(firstOrder.getIdentifier().toString(),
				firstOrder.getClient().getName(), firstOrder.getDate().toString(),
				String.valueOf(firstOrder.getPrice()));
		assertThat(window.table("OrdersTable").contents()[1]).containsExactly(secondOrder.getIdentifier().toString(),
				secondOrder.getClient().getName(), secondOrder.getDate().toString(),
				String.valueOf(secondOrder.getPrice()));
	}

	@Test
	@GUITest
	public void testSetYearsOrdersInOrderAndResetWhenThereIsCurrentYear() {
		GuiActionRunner.execute(() -> {
			orderSwingView.setYearsOrders(asList(2024, 2023, 2025));
		});
		assertThat(window.comboBox("yearsCombobox").contents()).containsExactly("" + 2025, "" + 2024, "" + 2023,
				"-- Nessun anno --");
		window.comboBox("yearsCombobox").requireSelection("" + 2025);
	}

	@Test
	@GUITest
	public void testSetYearsOrdersInOrderAndResetWhenThereIsNotCurrentYear() {
		GuiActionRunner.execute(() -> {
			orderSwingView.setYearsOrders(asList(2024, 2023));
		});
		assertThat(window.comboBox("yearsCombobox").contents()).containsExactly("" + 2025, "" + 2024, "" + 2023,
				"-- Nessun anno --");
		window.comboBox("yearsCombobox").requireSelection("" + 2025);
	}

	@Test
	@GUITest
	public void testSelectYearShouldDelegateOrdersControllerFindYearsOrders() {
		GuiActionRunner.execute(() -> {
			orderSwingView.getComboboxYearsModel().addElement(2023);
			orderSwingView.getComboboxYearsModel().addElement(2024);
		});
		window.comboBox("yearsCombobox").selectItem(0);
		verify(orderController).allOrdersByYear(2023);
	}

	@Test
	@GUITest
	public void testSelectYearWhenClientIsSelectedShouldDelegateOrdersControllerFindOrdersByYear() {
		Client firstClient = new Client("1", "first client id");
		Client secondClient = new Client("2", "second client id");
		GuiActionRunner.execute(() -> {
			orderSwingView.getClientListModel().addElement(firstClient);
			orderSwingView.getClientListModel().addElement(secondClient);
			orderSwingView.getComboboxYearsModel().addElement(2024);
			orderSwingView.getComboboxYearsModel().addElement(2025);
		});
		logger.info("secondClient: {}", secondClient.toString());
		window.comboBox("yearsCombobox").clearSelection();
		window.list("clientsList").selectItem(Pattern.compile("" + secondClient.toString()));
		window.comboBox("yearsCombobox").selectItem(Pattern.compile("" + 2024));
		logger.info("value: {}", window.list("clientsList").item(1).value());
		verify(orderController).findOrdersByYearAndClient(secondClient, 2024);
	}

	@Test
	@GUITest
	public void testSwitchOrderSelectionTable() {
		Client firstClient = new Client("1", "client 1");
		LocalDateTime previouslocalDateTime = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
		LocalDateTime nextlocalDateTime = LocalDateTime.of(2025, 1, 3, 0, 0, 0);
		Order orderToAddPrevious = new Order("1", firstClient,
				Date.from(previouslocalDateTime.atZone(ZoneId.systemDefault()).toInstant()), 10);
		Order orderToAddNext = new Order("3", null, // testo il caso in cui il client non è presente e graficamente è
													// riportato "--" nella tabella
				Date.from(nextlocalDateTime.atZone(ZoneId.systemDefault()).toInstant()), 20);

		GuiActionRunner.execute(() -> {
			orderSwingView.getComboboxYearsModel().addElement(2024);
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().setSelectedItem(2025);
			orderSwingView.getOrderTableModel().addOrder(orderToAddPrevious);
			orderSwingView.getOrderTableModel().addOrder(orderToAddNext);
		});

		// Caso 1: click ripetuto sulla stessa riga (Deselezione)
		window.table("OrdersTable").click(row(1).column(0), MouseButton.LEFT_BUTTON);
		window.table("OrdersTable").click(row(0).column(0), MouseButton.LEFT_BUTTON);
		window.table("OrdersTable").click(row(0).column(0), MouseButton.LEFT_BUTTON);
		assertThat(window.table("OrdersTable").selectionValue()).isNull();

		// Abilitazione della selezione multipla per testare toggle ed extend
		GuiActionRunner.execute(() -> {
			orderSwingView.getOrderTable().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		});

		// Caso 2: CTRL + click su riga selezionata => toggle == true, extend == false
		window.table("OrdersTable").selectRows(0);
		window.table("OrdersTable").pressKey(KeyEvent.VK_CONTROL);
		window.table("OrdersTable").click(row(0).column(0), MouseButton.LEFT_BUTTON);
		window.table("OrdersTable").releaseKey(KeyEvent.VK_CONTROL);

		// Caso 3: CTRL + click su riga diversa => toggle == true, extend == false
		window.table("OrdersTable").selectRows(0);
		window.table("OrdersTable").pressKey(KeyEvent.VK_CONTROL);
		window.table("OrdersTable").click(row(1).column(0), MouseButton.LEFT_BUTTON);
		window.table("OrdersTable").releaseKey(KeyEvent.VK_CONTROL);

		// Caso 4: SHIFT + click => toggle == false, extend == true
		window.table("OrdersTable").selectRows(0);
		window.table("OrdersTable").pressKey(KeyEvent.VK_SHIFT);
		window.table("OrdersTable").click(row(0).column(0), MouseButton.LEFT_BUTTON);
		window.table("OrdersTable").releaseKey(KeyEvent.VK_SHIFT);

		// Caso 5: SHIFT + CTRL + click => toggle == true, extend == true
		window.table("OrdersTable").pressKey(KeyEvent.VK_CONTROL);
		window.table("OrdersTable").pressKey(KeyEvent.VK_SHIFT);
		window.table("OrdersTable").click(row(0).column(0), MouseButton.LEFT_BUTTON);
		window.table("OrdersTable").releaseKey(KeyEvent.VK_SHIFT);
		window.table("OrdersTable").releaseKey(KeyEvent.VK_CONTROL);

	}

	@Test
	@GUITest
	public void testOrderAddedWhenItsYearIsSelectedAndResetErrorLabel() {
		Client firstClient = new Client("1", "first client id");
		LocalDateTime localDateTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
		Order orderToAdd = new Order("1", firstClient,
				Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()), 10);
		GuiActionRunner.execute(() -> {
			orderSwingView.getComboboxYearsModel().addElement(2024);
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().setSelectedItem(2024);
			orderSwingView.orderAdded(orderToAdd);
		});
		window.table("OrdersTable").requireRowCount(1);
		assertThat(window.table("OrdersTable").contents()[0]).containsExactly(orderToAdd.getIdentifier().toString(),
				orderToAdd.getClient().getName(), orderToAdd.getDate().toString(),
				String.valueOf(orderToAdd.getPrice()));
		window.textBox(JTextComponentMatcher.withName("panelOrderErrorMessage")).requireText("");

	}

	@Test
	@GUITest
	public void testOrderAddedInOrderWhenItsYearIsSelectedAndResetErrorLabel_MantainingOrderPreSelected() {
		Client firstClient = new Client("1", "first client id");
		LocalDateTime previouslocalDateTime = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
		LocalDateTime currentlocalDateTime = LocalDateTime.of(2025, 1, 2, 0, 0, 0);
		LocalDateTime nextlocalDateTime = LocalDateTime.of(2025, 1, 3, 0, 0, 0);
		Order orderToAddPrevious = new Order("1", firstClient,
				Date.from(previouslocalDateTime.atZone(ZoneId.systemDefault()).toInstant()), 10);
		Order orderToAddNext = new Order("3", firstClient,
				Date.from(nextlocalDateTime.atZone(ZoneId.systemDefault()).toInstant()), 20);
		Order orderToAddCurrent = new Order("2", firstClient,
				Date.from(currentlocalDateTime.atZone(ZoneId.systemDefault()).toInstant()), 30);
		GuiActionRunner.execute(() -> {
			orderSwingView.getComboboxYearsModel().addElement(2024);
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().setSelectedItem(2025);
			orderSwingView.getOrderTableModel().addOrder(orderToAddPrevious);
			orderSwingView.getOrderTableModel().addOrder(orderToAddNext);
		});
		window.table("OrdersTable").selectRows(1);
		GuiActionRunner.execute(() -> {
			orderSwingView.orderAdded(orderToAddCurrent);
		});
		window.table("OrdersTable").requireRowCount(3);
		assertThat(window.table("OrdersTable").contents()[0]).containsExactly(
				orderToAddPrevious.getIdentifier().toString(), orderToAddPrevious.getClient().getName(),
				orderToAddPrevious.getDate().toString(), String.valueOf(orderToAddPrevious.getPrice()));
		assertThat(window.table("OrdersTable").contents()[1]).containsExactly(
				orderToAddCurrent.getIdentifier().toString(), orderToAddCurrent.getClient().getName(),
				orderToAddCurrent.getDate().toString(), String.valueOf(orderToAddCurrent.getPrice()));
		assertThat(window.table("OrdersTable").contents()[2]).containsExactly(orderToAddNext.getIdentifier().toString(),
				orderToAddNext.getClient().getName(), orderToAddNext.getDate().toString(),
				String.valueOf(orderToAddNext.getPrice()));
		// check if order preselected is always selected
		window.table("OrdersTable").requireSelectedRows(2);
		window.textBox(JTextComponentMatcher.withName("panelOrderErrorMessage")).requireText("");

	}

	@Test
	@GUITest
	public void testOrderAddedWhenItsYearsIsNotSelectedAndResetErrorLabel() {
		Client firstClient = new Client("1", "first client id");
		LocalDateTime localDateTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
		Order orderToAdd = new Order("1", firstClient,
				Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()), 10);
		GuiActionRunner.execute(() -> {
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().addElement(2022);
			orderSwingView.getComboboxYearsModel().addElement(2023);
			orderSwingView.getComboboxYearsModel().setSelectedItem(2025);
			orderSwingView.orderAdded(orderToAdd);
		});
		assertThat(window.comboBox("yearsCombobox").contents()).containsExactly("2025", "2024", "2023", "2022",
				"-- Nessun anno --");
		String[][] contents = window.table("OrdersTable").contents();
		assertThat(contents)
				.doesNotContain(new String[] { orderToAdd.getIdentifier().toString(), orderToAdd.getClient().getName(),
						orderToAdd.getDate().toString(), String.valueOf(orderToAdd.getPrice()) }, atIndex(0));
		window.comboBox("yearsCombobox").requireSelection("2025");
		window.textBox(JTextComponentMatcher.withName("panelOrderErrorMessage")).requireText("");

	}

	@Test
	@GUITest
	public void testControlThatOnlyCorrectValueIsInsertedInDateAndPriceTextFields() throws Exception {
		window.textBox("textField_dayOfDateOrder").enterText("day");
		window.textBox("textField_dayOfDateOrder").requireEmpty();
		window.textBox("textField_dayOfDateOrder").enterText("203");
		window.textBox("textField_dayOfDateOrder").requireText("20");
		window.textBox("textField_dayOfDateOrder").setText("");
		window.textBox("textField_dayOfDateOrder").enterText("2 3");
		window.textBox("textField_dayOfDateOrder").requireText("23");
		window.textBox("textField_dayOfDateOrder").setText("");
		window.textBox("textField_dayOfDateOrder").enterText("20");
		window.textBox("textField_dayOfDateOrder").requireText("20");

		window.textBox("textField_monthOfDateOrder").setText("");
		window.textBox("textField_monthOfDateOrder").enterText("month");
		window.textBox("textField_monthOfDateOrder").requireEmpty();
		window.textBox("textField_monthOfDateOrder").enterText("123");
		window.textBox("textField_monthOfDateOrder").requireText("12");
		window.textBox("textField_monthOfDateOrder").setText("");
		window.textBox("textField_monthOfDateOrder").enterText("1 2");
		window.textBox("textField_monthOfDateOrder").requireText("12");
		window.textBox("textField_monthOfDateOrder").setText("");
		window.textBox("textField_monthOfDateOrder").enterText("12");
		window.textBox("textField_monthOfDateOrder").requireText("12");

		window.textBox("textField_yearOfDateOrder").enterText("year");
		window.textBox("textField_yearOfDateOrder").requireEmpty();
		window.textBox("textField_yearOfDateOrder").enterText("20253");
		window.textBox("textField_yearOfDateOrder").requireText("2025");
		window.textBox("textField_yearOfDateOrder").setText("");
		window.textBox("textField_yearOfDateOrder").enterText("2 0 25");
		window.textBox("textField_yearOfDateOrder").requireText("2025");
		window.textBox("textField_yearOfDateOrder").setText("");
		window.textBox("textField_yearOfDateOrder").enterText("2025");
		window.textBox("textField_yearOfDateOrder").requireText("2025");

	}

	@RunsInEDT
	@Test
	@GUITest
	public void testControlThatOnlyCorrectValueIsInsertedInPriceTextField() {
		window.textBox("textField_revenueOrder").enterText("price");
		window.textBox("textField_revenueOrder").requireEmpty();
		window.textBox("textField_revenueOrder").enterText("500.203");
		window.textBox("textField_revenueOrder").requireText("500.20");
		window.textBox("textField_revenueOrder").setText("");
		window.textBox("textField_revenueOrder").enterText("2 50 0. 2 0");
		window.textBox("textField_revenueOrder").requireText("2500.20");
		window.textBox("textField_revenueOrder").setText("");
		window.textBox("textField_revenueOrder").enterText("500.50");
		window.textBox("textField_revenueOrder").requireText("500.50");
		window.textBox("textField_revenueOrder").enterText("500.5.0");
		window.textBox("textField_revenueOrder").requireText("500.50");
	}

	@Test
	@GUITest
	public void testWhenTextFieldsAreNotEmptyAndClientIsSelectedThenAddOrderButtonShouldbeEnabled() {
		Client newClient = new Client("1", "new Client id");
		GuiActionRunner.execute(() -> {
			orderSwingView.getComboboxClientsModel().addElement(newClient);
		});
		window.textBox("textField_dayOfDateOrder").enterText("1");
		window.textBox("textField_monthOfDateOrder").enterText("1");
		window.textBox("textField_yearOfDateOrder").enterText("2025");
		window.textBox("textField_revenueOrder").enterText("20.1");
		window.comboBox("comboboxClients").selectItem(0);
		window.button(JButtonMatcher.withText("Aggiungi ordine")).requireEnabled();

		window.textBox("textField_dayOfDateOrder").setText("");
		window.textBox("textField_monthOfDateOrder").setText("");
		window.textBox("textField_yearOfDateOrder").setText("");
		window.textBox("textField_revenueOrder").setText("");
		window.comboBox("comboboxClients").clearSelection();

		window.comboBox("comboboxClients").selectItem(0);
		window.textBox("textField_dayOfDateOrder").enterText("1");
		window.textBox("textField_monthOfDateOrder").enterText("1");
		window.textBox("textField_yearOfDateOrder").enterText("2025");
		window.textBox("textField_revenueOrder").enterText("20.1");
		window.button(JButtonMatcher.withText("Aggiungi ordine")).requireEnabled();

	}

	@Test
	@GUITest
	public void testWhenATextFieldIsEmptyOrClientIsNotSelectedThenAddOrderButtonShouldbeDisabled() throws Exception {

		Client newClient = new Client("1", "new Client id");
		GuiActionRunner.execute(() -> {
			orderSwingView.getComboboxClientsModel().addElement(newClient);
		});
		window.textBox("textField_dayOfDateOrder").enterText("");
		window.textBox("textField_monthOfDateOrder").enterText("");
		window.textBox("textField_yearOfDateOrder").enterText(" ");
		window.textBox("textField_revenueOrder").enterText("20.1");
		window.comboBox("comboboxClients").selectItem(0);
		window.button(JButtonMatcher.withText("Aggiungi ordine")).requireDisabled();

		window.textBox("textField_dayOfDateOrder").setText("");
		window.textBox("textField_monthOfDateOrder").setText("");
		window.textBox("textField_yearOfDateOrder").enterText("2025");
		window.textBox("textField_revenueOrder").setText("");
		window.comboBox("comboboxClients").selectItem(0);
		window.button(JButtonMatcher.withText("Aggiungi ordine")).requireDisabled();

		window.comboBox("comboboxClients").selectItem(0);
		window.textBox("textField_dayOfDateOrder").setText("1");
		window.textBox("textField_monthOfDateOrder").setText("");
		window.textBox("textField_yearOfDateOrder").enterText("2025");
		window.textBox("textField_revenueOrder").enterText("20.1");
		window.button(JButtonMatcher.withText("Aggiungi ordine")).requireDisabled();

		window.comboBox("comboboxClients").selectItem(0);
		window.textBox("textField_dayOfDateOrder").setText("1");
		window.textBox("textField_monthOfDateOrder").setText("1");
		window.textBox("textField_yearOfDateOrder").enterText("2025");
		window.textBox("textField_revenueOrder").setText(" ");
		window.button(JButtonMatcher.withText("Aggiungi ordine")).requireDisabled();

		window.comboBox("comboboxClients").clearSelection();
		window.textBox("textField_dayOfDateOrder").setText("1");
		window.textBox("textField_monthOfDateOrder").setText("1");
		window.textBox("textField_yearOfDateOrder").setText("2025");
		window.textBox("textField_revenueOrder").setText(" ");
		window.button(JButtonMatcher.withText("Aggiungi ordine")).requireDisabled();

	}

	@Test
	@GUITest
	public void testAddOrderButtonShouldDelegateToOrderControllerNewOrderWhenDateIsCorrectAndResetErrorLabel() {
		Client newClient = new Client("1", "new Client id");
		GuiActionRunner.execute(() -> {
			orderSwingView.getComboboxClientsModel().addElement(newClient);
		});
		window.textBox("textField_dayOfDateOrder").enterText("1");
		window.textBox("textField_monthOfDateOrder").enterText("1");
		window.textBox("textField_yearOfDateOrder").enterText("2025");
		window.textBox("textField_revenueOrder").enterText("20.1");
		window.comboBox("comboboxClients").selectItem(0);
		window.button(JButtonMatcher.withText("Aggiungi ordine")).click();

		LocalDateTime localDateTime = LocalDateTime.of(2025, 1, 1, 0, 0);
		verify(orderController).addOrder(
				new Order("", newClient, Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()), 20.1));

		window.textBox("textField_dayOfDateOrder").requireEmpty();
		window.textBox("textField_monthOfDateOrder").requireEmpty();
		window.textBox("textField_yearOfDateOrder").requireEmpty();
		window.textBox("textField_revenueOrder").requireEmpty();
		window.textBox("panelOrderErrorMessage").requireText("");
		window.comboBox("comboboxClients").requireNoSelection();
		window.button(JButtonMatcher.withText("Aggiungi ordine")).requireDisabled();

	}

	@Test
	@GUITest
	public void testAddOrderButtonShouldDelegateToOrderControllerNewOrderWhenYearIsNotCorrect() {
		Client newClient = new Client("1", "new Client id");
		GuiActionRunner.execute(() -> {
			orderSwingView.getComboboxClientsModel().addElement(newClient);
		});
		window.textBox("textField_dayOfDateOrder").enterText("1");
		window.textBox("textField_monthOfDateOrder").enterText("1");
		window.textBox("textField_yearOfDateOrder").enterText("2026");
		window.textBox("textField_revenueOrder").enterText("20.1");
		window.comboBox("comboboxClients").selectItem(0);
		window.button(JButtonMatcher.withText("Aggiungi ordine")).click();

		window.textBox("textField_dayOfDateOrder").requireEmpty();
		window.textBox("textField_monthOfDateOrder").requireEmpty();
		window.textBox("textField_yearOfDateOrder").requireEmpty();
		window.textBox("textField_revenueOrder").requireEmpty();
		verifyNoInteractions(orderController);
		window.textBox("panelOrderErrorMessage").requireText("La data 1/1/2026 non è corretta");

		window.textBox("textField_dayOfDateOrder").enterText("1");
		window.textBox("textField_monthOfDateOrder").enterText("1");
		window.textBox("textField_yearOfDateOrder").enterText("1924");
		window.textBox("textField_revenueOrder").enterText("20.1");
		window.comboBox("comboboxClients").selectItem(0);
		window.button(JButtonMatcher.withText("Aggiungi ordine")).click();

		window.textBox("textField_dayOfDateOrder").requireEmpty();
		window.textBox("textField_monthOfDateOrder").requireEmpty();
		window.textBox("textField_yearOfDateOrder").requireEmpty();
		window.textBox("textField_revenueOrder").requireEmpty();
		verifyNoInteractions(orderController);
		window.textBox("panelOrderErrorMessage").requireText("La data 1/1/1924 non è corretta");

	}

	@Test
	@GUITest
	public void testAddOrderButtonShouldDelegateToOrderControllerNewOrderWhenMonthIsNotCorrect() {
		Client newClient = new Client("1", "new Client id");
		GuiActionRunner.execute(() -> {
			orderSwingView.getComboboxClientsModel().addElement(newClient);
		});
		window.textBox("textField_dayOfDateOrder").enterText("1");
		window.textBox("textField_monthOfDateOrder").enterText("13");
		window.textBox("textField_yearOfDateOrder").enterText("2025");
		window.textBox("textField_revenueOrder").enterText("20.1");
		window.comboBox("comboboxClients").selectItem(0);
		window.button(JButtonMatcher.withText("Aggiungi ordine")).click();

		window.textBox("textField_dayOfDateOrder").requireEmpty();
		window.textBox("textField_monthOfDateOrder").requireEmpty();
		window.textBox("textField_yearOfDateOrder").requireEmpty();
		window.textBox("textField_revenueOrder").requireEmpty();
		verifyNoInteractions(orderController);
		window.textBox("panelOrderErrorMessage").requireText("La data 1/13/2025 non è corretta");

		window.textBox("textField_dayOfDateOrder").enterText("1");
		window.textBox("textField_monthOfDateOrder").enterText("0");
		window.textBox("textField_yearOfDateOrder").enterText("2025");
		window.textBox("textField_revenueOrder").enterText("20.1");
		window.comboBox("comboboxClients").selectItem(0);
		window.button(JButtonMatcher.withText("Aggiungi ordine")).click();

		window.textBox("textField_dayOfDateOrder").requireEmpty();
		window.textBox("textField_monthOfDateOrder").requireEmpty();
		window.textBox("textField_yearOfDateOrder").requireEmpty();
		window.textBox("textField_revenueOrder").requireEmpty();
		verifyNoInteractions(orderController);
		window.textBox("panelOrderErrorMessage").requireText("La data 1/0/2025 non è corretta");
	}

	@Test
	@GUITest
	public void testAddOrderButtonShouldDelegateToOrderControllerNewOrderWhenDayIsNotCorrect() {

		Client newClient = new Client("1", "new Client id");
		GuiActionRunner.execute(() -> {
			orderSwingView.getComboboxClientsModel().addElement(newClient);
		});
		window.textBox("textField_dayOfDateOrder").enterText("0");
		window.textBox("textField_monthOfDateOrder").enterText("1");
		window.textBox("textField_yearOfDateOrder").enterText("2025");
		window.textBox("textField_revenueOrder").enterText("20.1");
		window.comboBox("comboboxClients").selectItem(0);
		window.button(JButtonMatcher.withText("Aggiungi ordine")).click();

		window.textBox("textField_dayOfDateOrder").requireEmpty();
		window.textBox("textField_monthOfDateOrder").requireEmpty();
		window.textBox("textField_yearOfDateOrder").requireEmpty();
		window.textBox("textField_revenueOrder").requireEmpty();
		verifyNoInteractions(orderController);
		window.textBox("panelOrderErrorMessage").requireText("La data 0/1/2025 non è corretta");

		window.textBox("textField_dayOfDateOrder").enterText("32");
		window.textBox("textField_monthOfDateOrder").enterText("1");
		window.textBox("textField_yearOfDateOrder").enterText("2025");
		window.textBox("textField_revenueOrder").enterText("20.1");
		window.comboBox("comboboxClients").selectItem(0);
		window.button(JButtonMatcher.withText("Aggiungi ordine")).click();

		window.textBox("textField_dayOfDateOrder").requireEmpty();
		window.textBox("textField_monthOfDateOrder").requireEmpty();
		window.textBox("textField_yearOfDateOrder").requireEmpty();
		window.textBox("textField_revenueOrder").requireEmpty();
		verifyNoInteractions(orderController);
		window.textBox("panelOrderErrorMessage").requireText("La data 32/1/2025 non è corretta");

		window.textBox("textField_dayOfDateOrder").enterText("29");
		window.textBox("textField_monthOfDateOrder").enterText("2");
		window.textBox("textField_yearOfDateOrder").enterText("2025");
		window.textBox("textField_revenueOrder").enterText("20.1");
		window.comboBox("comboboxClients").selectItem(0);
		window.button(JButtonMatcher.withText("Aggiungi ordine")).click();

		window.textBox("textField_dayOfDateOrder").requireEmpty();
		window.textBox("textField_monthOfDateOrder").requireEmpty();
		window.textBox("textField_yearOfDateOrder").requireEmpty();
		window.textBox("textField_revenueOrder").requireEmpty();
		verifyNoInteractions(orderController);
		window.textBox("panelOrderErrorMessage").requireText("La data 29/2/2025 non è corretta");

		window.textBox("textField_dayOfDateOrder").enterText("30");
		window.textBox("textField_monthOfDateOrder").enterText("2");
		window.textBox("textField_yearOfDateOrder").enterText("2000");
		window.textBox("textField_revenueOrder").enterText("20.1");
		window.comboBox("comboboxClients").selectItem(0);
		window.button(JButtonMatcher.withText("Aggiungi ordine")).click();

		window.textBox("textField_dayOfDateOrder").requireEmpty();
		window.textBox("textField_monthOfDateOrder").requireEmpty();
		window.textBox("textField_yearOfDateOrder").requireEmpty();
		window.textBox("textField_revenueOrder").requireEmpty();
		verifyNoInteractions(orderController);
		window.textBox("panelOrderErrorMessage").requireText("La data 30/2/2000 non è corretta");

		window.textBox("textField_dayOfDateOrder").enterText("29");
		window.textBox("textField_monthOfDateOrder").enterText("2");
		window.textBox("textField_yearOfDateOrder").enterText("2023");
		window.textBox("textField_revenueOrder").enterText("20.1");
		window.comboBox("comboboxClients").selectItem(0);
		window.button(JButtonMatcher.withText("Aggiungi ordine")).click();

		window.textBox("textField_dayOfDateOrder").requireEmpty();
		window.textBox("textField_monthOfDateOrder").requireEmpty();
		window.textBox("textField_yearOfDateOrder").requireEmpty();
		window.textBox("textField_revenueOrder").requireEmpty();
		verifyNoInteractions(orderController);
		window.textBox("panelOrderErrorMessage").requireText("La data 29/2/2023 non è corretta");

		window.textBox("textField_dayOfDateOrder").enterText("30");
		window.textBox("textField_monthOfDateOrder").enterText("2");
		window.textBox("textField_yearOfDateOrder").enterText("2024");
		window.textBox("textField_revenueOrder").enterText("20.1");
		window.comboBox("comboboxClients").selectItem(0);
		window.button(JButtonMatcher.withText("Aggiungi ordine")).click();

		window.textBox("textField_dayOfDateOrder").requireEmpty();
		window.textBox("textField_monthOfDateOrder").requireEmpty();
		window.textBox("textField_yearOfDateOrder").requireEmpty();
		window.textBox("textField_revenueOrder").requireEmpty();
		verifyNoInteractions(orderController);
		window.textBox("panelOrderErrorMessage").requireText("La data 30/2/2024 non è corretta");

		window.textBox("textField_dayOfDateOrder").enterText("29");
		window.textBox("textField_monthOfDateOrder").enterText("2");
		window.textBox("textField_yearOfDateOrder").enterText("2100");
		window.textBox("textField_revenueOrder").enterText("20.1");
		window.comboBox("comboboxClients").selectItem(0);
		window.button(JButtonMatcher.withText("Aggiungi ordine")).click();

		window.textBox("textField_dayOfDateOrder").requireEmpty();
		window.textBox("textField_monthOfDateOrder").requireEmpty();
		window.textBox("textField_yearOfDateOrder").requireEmpty();
		window.textBox("textField_revenueOrder").requireEmpty();
		verifyNoInteractions(orderController);
		window.textBox("panelOrderErrorMessage").requireText("La data 29/2/2100 non è corretta");

		window.textBox("textField_dayOfDateOrder").enterText("31");
		window.textBox("textField_monthOfDateOrder").enterText("4");
		window.textBox("textField_yearOfDateOrder").enterText("2025");
		window.textBox("textField_revenueOrder").enterText("20.1");
		window.comboBox("comboboxClients").selectItem(0);
		window.button(JButtonMatcher.withText("Aggiungi ordine")).click();

		window.textBox("textField_dayOfDateOrder").requireEmpty();
		window.textBox("textField_monthOfDateOrder").requireEmpty();
		window.textBox("textField_yearOfDateOrder").requireEmpty();
		window.textBox("textField_revenueOrder").requireEmpty();
		verifyNoInteractions(orderController);
		window.textBox("panelOrderErrorMessage").requireText("La data 31/4/2025 non è corretta");
	}

	@Test
	@GUITest
	public void testUpdateTotalPriceWhenShowAllOrdersAndAClientIsSelected() {
		Client firstClient = new Client("1", "first client identifier");
		Client secondClient = new Client("2", "second client identifier");
		Order firstOrder = new Order("1", firstClient, new Date(), 10.1);
		Order secondOrder = new Order("2", firstClient, new Date(), 10.1);
		Order thirdOrder = new Order("3", firstClient,
				Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10.1);
		JTextComponent panelOrderError = window.textBox("panelOrderErrorMessage").target();
		GuiActionRunner.execute(() -> {
			panelOrderError.setText(" ");
			orderSwingView.getClientListModel().addElement(firstClient);
			orderSwingView.getClientListModel().addElement(secondClient);
			orderSwingView.getComboboxYearsModel().addElement(2024);
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().setSelectedItem(2025);
			orderSwingView.showAllOrders(asList(firstOrder, secondOrder, thirdOrder));
		});
		window.list("clientsList").selectItem(0);
		GuiActionRunner.execute(() -> {
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().addElement(2024);
			orderSwingView.getComboboxYearsModel().setSelectedItem(2025);
			orderSwingView.showAllOrders(asList(firstOrder, secondOrder, thirdOrder));
		});
		window.label("revenueLabel")
				.requireText("Il costo totale degli ordini del cliente " + firstClient.getIdentifier() + " nel "
						+ "2025" + " è di " + String.format("%.2f", firstOrder.getPrice() + secondOrder.getPrice())
						+ "€");
		window.textBox("panelOrderErrorMessage").requireText("");

	}

	@Test
	@GUITest
	public void testUpdateTotalPriceWhenShowAllOrdersAndAnyClientIsSelected() {
		Client firstClient = new Client("1", "first client identifier");
		Client secondClient = new Client("2", "second client identifier");
		Order firstOrder = new Order("1", firstClient, new Date(), 10.1);
		Order secondOrder = new Order("2", secondClient, new Date(), 10.1);
		window.textBox("panelOrderErrorMessage").setText(" ");
		GuiActionRunner.execute(() -> {
			orderSwingView.getClientListModel().addElement(firstClient);
			orderSwingView.getClientListModel().addElement(secondClient);
			orderSwingView.getComboboxYearsModel().addElement(2024);
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().setSelectedItem(2025);
			orderSwingView.showAllOrders(asList(firstOrder, secondOrder));
		});
		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2025" + " è di "
				+ String.format("%.2f", firstOrder.getPrice() + secondOrder.getPrice()) + "€");
		window.textBox("panelOrderErrorMessage").requireText("");
	}

	@Test
	@GUITest
	public void testUpdateTotalPriceWhenAnOrderOftheYearSelectedIsAddedAndClientIsNotSelected() {
		Client firstClient = new Client("1", "first client identifier");
		Client secondClient = new Client("2", "second client identifier");
		Order firstOrder = new Order("1", firstClient,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10.1);
		Order secondOrder = new Order("2", secondClient,
				Date.from(LocalDate.of(2025, 2, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10.1);

		GuiActionRunner.execute(() -> {
			orderSwingView.getClientListModel().addElement(firstClient);
			orderSwingView.getClientListModel().addElement(secondClient);
			orderSwingView.getComboboxYearsModel().addElement(2024);
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().setSelectedItem(2025);
			orderSwingView.showAllOrders(asList(firstOrder));
			orderSwingView.orderAdded(secondOrder);
		});
		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2025" + " è di "
				+ String.format("%.2f", firstOrder.getPrice() + secondOrder.getPrice()) + "€");
	}

	@Test
	@GUITest
	public void testUpdateTotalPriceWhenAnOrderOftheYearNotSelectedIsAddedAndClientIsNotSelected() {
		Client firstClient = new Client("1", "first client identifier");
		Client secondClient = new Client("2", "second client identifier");
		Order currentYearOrder = new Order("1", firstClient,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10.1);
		Order previousYearOrder = new Order("2", secondClient,
				Date.from(LocalDate.of(2024, 2, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10.1);

		GuiActionRunner.execute(() -> {
			orderSwingView.getClientListModel().addElement(firstClient);
			orderSwingView.getClientListModel().addElement(secondClient);
			orderSwingView.getComboboxYearsModel().addElement(2024);
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().setSelectedItem(2025);
			orderSwingView.showAllOrders(asList(currentYearOrder));
			orderSwingView.orderAdded(previousYearOrder);
		});
		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2025" + " è di "
				+ String.format("%.2f", currentYearOrder.getPrice()) + "€");
	}

	@Test
	@GUITest
	public void testUpdateTotalPriceWhenAnOrderOftheYearSelectedAndClientSelectedIsAdded() {
		Client firstClient = new Client("1", "first client identifier");
		Client secondClient = new Client("2", "second client identifier");
		Order order = new Order("1", secondClient,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10.1);
		GuiActionRunner.execute(() -> {
			orderSwingView.getClientListModel().addElement(firstClient);
			orderSwingView.getClientListModel().addElement(secondClient);
		});
		window.list("clientsList").selectItem(1);
		GuiActionRunner.execute(() -> {
			orderSwingView.getComboboxYearsModel().addElement(2024);
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().setSelectedItem(2025);
			orderSwingView.orderAdded(order);
		});
		window.label("revenueLabel")
				.requireText("Il costo totale degli ordini del cliente " + secondClient.getIdentifier() + " nel "
						+ "2025" + " è di " + String.format("%.2f", order.getPrice()) + "€");
	}

	@Test
	@GUITest
	public void testUpdateTotalPriceWhenAnOrderOftheYearNotSelectedAndClientSelectedIsAdded() {
		Client firstClient = new Client("1", "first client identifier");
		Client secondClient = new Client("2", "second client identifier");
		Order firstOrder = new Order("1", firstClient,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10.1);
		Order secondOrder = new Order("1", firstClient,
				Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20.1);
		GuiActionRunner.execute(() -> {
			orderSwingView.getClientListModel().addElement(firstClient);
			orderSwingView.getClientListModel().addElement(secondClient);
		});
		window.list("clientsList").selectItem(0);
		GuiActionRunner.execute(() -> {
			orderSwingView.getComboboxYearsModel().addElement(2024);
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().setSelectedItem(2025);
			orderSwingView.showAllOrders(asList(firstOrder));
			orderSwingView.orderAdded(secondOrder);
		});
		window.label("revenueLabel")
				.requireText("Il costo totale degli ordini del cliente " + firstClient.getIdentifier() + " nel 2025"
						+ " è di " + String.format("%.2f", firstOrder.getPrice()) + "€");
	}

	@Test
	@GUITest
	public void testUpdateTotalPriceWhenAnOrderOftheYearNotSelectedAndClientNotSelectedIsAdded() {
		Client firstClient = new Client("1", "first client identifier");
		Client secondClient = new Client("2", "second client identifier");
		Order orderOfClientAndYearSelected = new Order("1", firstClient,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10.1);
		Order orderOfClientAndYearNotSelected = new Order("1", secondClient,
				Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20.1);
		GuiActionRunner.execute(() -> {
			orderSwingView.getClientListModel().addElement(firstClient);
			orderSwingView.getClientListModel().addElement(secondClient);
		});
		window.list("clientsList").selectItem(0);
		GuiActionRunner.execute(() -> {
			orderSwingView.getComboboxYearsModel().addElement(2024);
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().setSelectedItem(2025);
			orderSwingView.showAllOrders(asList(orderOfClientAndYearSelected));
			orderSwingView.orderAdded(orderOfClientAndYearNotSelected);
		});
		window.label("revenueLabel")
				.requireText("Il costo totale degli ordini del cliente " + firstClient.getIdentifier() + " nel 2025"
						+ " è di " + String.format("%.2f", orderOfClientAndYearSelected.getPrice()) + "€");
	}

	@Test
	@GUITest
	public void testResetShowOrderOfCurrentYearWhenShowOrderIsCalledWithEmptyArgumentAndAnyClientIsSelected() {
		GuiActionRunner.execute(() -> {
			DefaultComboBoxModel<Integer> comboboxYearModel = orderSwingView.getComboboxYearsModel();
			comboboxYearModel.addElement(2025);
			comboboxYearModel.addElement(2024);
			comboboxYearModel.setSelectedItem(2024);
			orderSwingView.showAllOrders(asList());
		});
		verify(orderController).yearsOfTheOrders();
	}

	@Test
	@GUITest
	public void testNotResetShowOrderWhenShowOrderIsCalledWithEmptyArgumentAndAClientIsSelected() {
		Client newClient = new Client("1", "test id 1");
		JLabelFixture revenueLabel = window.label("revenueLabel");
		GuiActionRunner.execute(() -> {
			DefaultComboBoxModel<Integer> comboboxYearModel = orderSwingView.getComboboxYearsModel();
			comboboxYearModel.addElement(2025);
			comboboxYearModel.addElement(2024);
			comboboxYearModel.setSelectedItem(2024);
			revenueLabel.target().setText(" ");
			orderSwingView.getClientListModel().addElement(newClient);
		});
		window.list("clientsList").selectItem(0);
		GuiActionRunner.execute(() -> {
			orderSwingView.showAllOrders(asList());
		});
		window.textBox(JTextComponentMatcher.withName("panelOrderErrorMessage"))
				.requireText("Non sono presenti ordini del 2024 per il cliente " + newClient.getIdentifier());
		window.label("revenueLabel").requireText("");
		verify(orderController, never()).yearsOfTheOrders();
	}

	@Test
	@GUITest
	public void testNotResetShowOrderWhenShowOrderIsCalledWithEmptyArgumentAndCurrentYearIsSelected() {
		JLabel revenueLabel = window.label("revenueLabel").target();
		GuiActionRunner.execute(() -> {
			DefaultComboBoxModel<Integer> comboboxYearModel = orderSwingView.getComboboxYearsModel();
			comboboxYearModel.addElement(2025);
			comboboxYearModel.addElement(2024);
			comboboxYearModel.setSelectedItem(2025);
			orderSwingView.showAllOrders(asList());
			revenueLabel.setText("");
		});
		window.textBox(JTextComponentMatcher.withName("panelOrderErrorMessage"))
				.requireText("Non sono presenti ordini per il 2025");
		window.label("revenueLabel").requireText("");
		verify(orderController, never()).yearsOfTheOrders();
	}

	@Test
	@GUITest
	public void testRemoveOrderOfAClientWhenOrderRemanentInListIsEmptyAndAnyClientIsSelected() {
		Client firstClient = new Client("1", "test id 1");
		Order order1 = new Order("1", firstClient,
				Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		Order order2 = new Order("2", firstClient,
				Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20);
		logger.info("order1: {}", order1);
		GuiActionRunner.execute(() -> {
			DefaultListModel<Client> listClientModel = orderSwingView.getClientListModel();
			listClientModel.addElement(firstClient);
			DefaultComboBoxModel<Integer> comboboxYearModel = orderSwingView.getComboboxYearsModel();
			comboboxYearModel.addElement(2024);
			comboboxYearModel.addElement(2025);
			comboboxYearModel.setSelectedItem(2024);
			OrderTableModel listOrderModel = orderSwingView.getOrderTableModel();
			listOrderModel.addOrder(order1);
			listOrderModel.addOrder(order2);
		});
		GuiActionRunner.execute(() -> orderSwingView
				.removeOrdersByClient(new Client(firstClient.getIdentifier(), firstClient.getName())));
		String[][] tableContents = window.table("OrdersTable").contents();
		assertThat(tableContents).isEmpty();
		verify(orderController, never()).yearsOfTheOrders();
	}

	@Test
	@GUITest
	public void testRemoveOrderOfAClientWhenOrderRemanentInListIsEmptyAndAnOtherClientIsSelected() {
		Client firstClient = new Client("1", "test id 1");
		Client secondClient = new Client("2", "test id 2");
		Order orderOfClient1CurrentYear = new Order("1", firstClient,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		Order orderOfClient1YearFixture = new Order("2", firstClient,
				Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20);
		logger.info("orderOfClient1CurrentYear: {}", orderOfClient1CurrentYear);
		GuiActionRunner.execute(() -> {
			DefaultListModel<Client> listClientModel = orderSwingView.getClientListModel();
			listClientModel.addElement(firstClient);
			listClientModel.addElement(secondClient);
			DefaultComboBoxModel<Integer> comboboxYearModel = orderSwingView.getComboboxYearsModel();
			comboboxYearModel.addElement(2024);
			comboboxYearModel.addElement(2025);
			comboboxYearModel.setSelectedItem(2024);
			OrderTableModel listOrderModel = orderSwingView.getOrderTableModel();
			listOrderModel.addOrder(orderOfClient1CurrentYear);
			listOrderModel.addOrder(orderOfClient1YearFixture);
		});
		window.list("clientsList").selectItem(1);
		GuiActionRunner.execute(() -> orderSwingView
				.removeOrdersByClient(new Client(firstClient.getIdentifier(), firstClient.getName())));
		String[][] tableContents = window.table("OrdersTable").contents();
		assertThat(tableContents).isEmpty();
		verify(orderController, never()).yearsOfTheOrders();
	}

	@Test
	@GUITest
	public void testRemoveOrderWhenOrdersRemainedInTableAreNotEmptyAndUpdateTotalRevenue() {
		Client client = new Client("1", "test id 1");
		Order order = new Order("1", client,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		Order order2 = new Order("2", client,
				Date.from(LocalDate.of(2025, 2, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		GuiActionRunner.execute(() -> {
			OrderTableModel listOrderModel = orderSwingView.getOrderTableModel();
			listOrderModel.addOrder(order);
			listOrderModel.addOrder(order2);
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().setSelectedItem(2025);
			orderSwingView.orderRemoved(new Order("2", client, order2.getDate(), 10.0));

		});
		String[][] orders = window.table("OrdersTable").contents();
		List<List<String>> orderList = Arrays.stream(orders).map((String[] row) -> Arrays.asList(row))
				.collect(Collectors.toList());
		assertThat(orderList).containsOnly(Arrays.asList(order.getIdentifier().toString(),
				order.getClient().getName().toString(), order.getDate().toString(), String.valueOf(order.getPrice())));
		window.label(JLabelMatcher.withName("revenueLabel")).requireText(
				"Il costo totale degli ordini nel 2025" + " è di " + String.format("%.2f", order.getPrice()) + "€");
	}

	@Test
	@GUITest
	public void testRemoveOrderWhenOrdersTableISEmpty() {
		Client client = new Client("1", "test id 1");
		Order order = new Order("1", client,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		GuiActionRunner.execute(() -> {
			OrderTableModel listOrderModel = orderSwingView.getOrderTableModel();
			listOrderModel.addOrder(order);
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().addElement(2024);

			orderSwingView.getComboboxYearsModel().setSelectedItem(2024);
			orderSwingView.orderRemoved(new Order("1", client, order.getDate(), 10.0));

		});

		String[][] orders = window.table("OrdersTable").contents();
		List<List<String>> orderList = Arrays.stream(orders).map((String[] row) -> Arrays.asList(row))
				.collect(Collectors.toList());
		assertThat(orderList).isEmpty();
		verify(orderController).yearsOfTheOrders();

	}

	@Test
	@GUITest
	public void testShowOrderErrorShouldShowErrorInTheOrderErrorLabel() {
		Order order = new Order("1", new Client(),
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		GuiActionRunner.execute(() -> {
			orderSwingView.showOrderError("error message", order);
		});

		window.textBox("panelOrderErrorMessage").requireText("error message: " + order.toString());

	}

	@Test
	@GUITest
	public void testShowAllOrdersWhenOrdersOfClientSelectedAreShowedUpdateTotalPriceAndResetOrderError() {
		Client firstClient = new Client("1", "test id 1");
		Client secondClient = new Client("2", "test id 2");
		Order orderOfClient1CurrentYear = new Order("1", firstClient,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		Order orderOfClient1YearFixture = new Order("2", firstClient,
				Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20);
		logger.info("ordine del cliente 1 e dell'anno corrente: {}", orderOfClient1CurrentYear);

		window.textBox("panelOrderErrorMessage").setText(" ");

		GuiActionRunner.execute(() -> {
			orderSwingView.getClientListModel().addElement(firstClient);

			orderSwingView.getClientListModel().addElement(secondClient);
		});
		window.list("clientsList").selectItem(0);
		GuiActionRunner.execute(() -> {
			orderSwingView.showAllOrders(asList(orderOfClient1YearFixture, orderOfClient1CurrentYear));
		});

		window.label("revenueLabel").requireText("Il costo totale degli ordini del cliente "
				+ firstClient.getIdentifier() + " è di "
				+ String.format("%.2f", orderOfClient1CurrentYear.getPrice() + orderOfClient1YearFixture.getPrice())
				+ "€");
		window.textBox("panelOrderErrorMessage").requireText("");

	}

	@Test
	@GUITest
	public void testOrderModifiedWhenItsYearIsSelectedAndResetErrorLabel() {
		Client client = new Client("1", "first client id");
		GuiActionRunner.execute(() -> {
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().setSelectedItem(2025);
		});
		window.textBox(JTextComponentMatcher.withName("panelOrderErrorMessage")).setText(" ");
		LocalDateTime newLocalDate = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
		Order orderModified = new Order("1", client, Date.from(newLocalDate.atZone(ZoneId.systemDefault()).toInstant()),
				20);
		GuiActionRunner.execute(() -> {
			orderSwingView.orderUpdated(orderModified);
		});
		window.table("OrdersTable").requireRowCount(1);
		assertThat(window.table("OrdersTable").contents()[0]).containsExactly(orderModified.getIdentifier().toString(),
				orderModified.getClient().getName(), orderModified.getDate().toString(),
				String.valueOf(orderModified.getPrice()));
		window.textBox(JTextComponentMatcher.withName("panelOrderErrorMessage")).requireText("");

	}

	@Test
	@GUITest
	public void testOrderModifiedWhenItsYearIsNotSelected() {
		Client client = new Client("1", "first client id");
		GuiActionRunner.execute(() -> {
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().setSelectedItem(2025);
		});
		LocalDateTime newLocalDate = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
		Order orderModified = new Order("1", client, Date.from(newLocalDate.atZone(ZoneId.systemDefault()).toInstant()),
				20);
		GuiActionRunner.execute(() -> {
			orderSwingView.orderUpdated(orderModified);
		});
		window.table("OrdersTable").requireRowCount(0);

	}

	@Test
	@GUITest
	public void testOrderModifiedWhenOrderToModifyIsPresentAndItsYearIsSelected() {
		Client client = new Client("1", "first client id");
		LocalDateTime prelocalDate = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
		LocalDateTime localDate = LocalDateTime.of(2025, 1, 2, 0, 0, 0);
		LocalDateTime nextlocalDate = LocalDateTime.of(2025, 1, 3, 0, 0, 0);
		Order preOrder = new Order("1", client, Date.from(localDate.atZone(ZoneId.systemDefault()).toInstant()), 10);
		Order orderToModify = new Order("2", client, Date.from(localDate.atZone(ZoneId.systemDefault()).toInstant()),
				20);
		Order nextOrder = new Order("3", client, Date.from(localDate.atZone(ZoneId.systemDefault()).toInstant()), 30);
		GuiActionRunner.execute(() -> {
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().setSelectedItem(2025);
			orderSwingView.getOrderTableModel().addOrder(preOrder);
			orderSwingView.getOrderTableModel().addOrder(orderToModify);
			orderSwingView.getOrderTableModel().addOrder(nextOrder);
		});
		window.textBox(JTextComponentMatcher.withName("panelOrderErrorMessage")).setText(" ");
		LocalDateTime modifiedLocalDate = LocalDateTime.of(2025, 2, 1, 0, 0, 0);
		Order orderModified = new Order("2", client,
				Date.from(modifiedLocalDate.atZone(ZoneId.systemDefault()).toInstant()), 30.5);
		GuiActionRunner.execute(() -> {
			orderSwingView.orderUpdated(orderModified);
		});
		window.table("OrdersTable").requireRowCount(3);
		assertThat(window.table("OrdersTable").contents()[0]).containsExactly(preOrder.getIdentifier().toString(),
				preOrder.getClient().getName(), preOrder.getDate().toString(), String.valueOf(preOrder.getPrice()));
		assertThat(window.table("OrdersTable").contents()[1]).containsExactly(nextOrder.getIdentifier().toString(),
				nextOrder.getClient().getName(), nextOrder.getDate().toString(), String.valueOf(nextOrder.getPrice()));
		assertThat(window.table("OrdersTable").contents()[2]).containsExactly(orderModified.getIdentifier().toString(),
				orderModified.getClient().getName(), orderModified.getDate().toString(),
				String.valueOf(orderModified.getPrice()));
		window.textBox(JTextComponentMatcher.withName("panelOrderErrorMessage")).requireText("");

	}

	@Test
	@GUITest
	public void testUpdateTotalPriceWhenAnOrderOftheYearSelectedIsModifiedAndClientIsNotSelected() {
		Client firstClient = new Client("1", "first client identifier");
		Client secondClient = new Client("2", "second client identifier");
		Order firstOrder = new Order("1", firstClient,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10.1);
		Order secondOrder = new Order("2", secondClient,
				Date.from(LocalDate.of(2025, 2, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10.1);

		GuiActionRunner.execute(() -> {
			orderSwingView.getClientListModel().addElement(firstClient);
			orderSwingView.getClientListModel().addElement(secondClient);
			orderSwingView.getComboboxYearsModel().addElement(2024);
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().setSelectedItem(2025);
			orderSwingView.showAllOrders(asList(firstOrder, secondOrder));
		});
		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2025" + " è di "
				+ String.format("%.2f", firstOrder.getPrice() + secondOrder.getPrice()) + "€");
		Order secondOrderModified = new Order("2", secondClient,
				Date.from(LocalDate.of(2025, 2, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20.1);
		GuiActionRunner.execute(() -> {
			orderSwingView.orderUpdated(secondOrderModified);
		});
		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2025" + " è di "
				+ String.format("%.2f", firstOrder.getPrice() + secondOrderModified.getPrice()) + "€");
	}

	@Test
	@GUITest
	public void testUpdateTotalPriceWhenAnOrderOftheYearSelectedIsModifiedWithADifferentYearAndClientIsNotSelected() {
		Client firstClient = new Client("1", "first client identifier");
		Client secondClient = new Client("2", "second client identifier");
		Order firstOrder = new Order("1", firstClient,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10.1);
		Order secondOrder = new Order("2", secondClient,
				Date.from(LocalDate.of(2025, 2, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10.1);

		GuiActionRunner.execute(() -> {
			orderSwingView.getClientListModel().addElement(firstClient);
			orderSwingView.getClientListModel().addElement(secondClient);
			orderSwingView.getComboboxYearsModel().addElement(2024);
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().setSelectedItem(2025);
			orderSwingView.showAllOrders(asList(firstOrder, secondOrder));
		});
		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2025" + " è di "
				+ String.format("%.2f", firstOrder.getPrice() + secondOrder.getPrice()) + "€");
		Order secondOrderModified = new Order("2", secondClient,
				Date.from(LocalDate.of(2024, 2, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10.1);
		GuiActionRunner.execute(() -> {
			orderSwingView.orderUpdated(secondOrderModified);
		});
		window.label("revenueLabel").requireText("Il costo totale degli ordini nel " + "2025" + " è di "
				+ String.format("%.2f", firstOrder.getPrice()) + "€");
	}

	@Test
	@GUITest
	public void testUpdateTotalPriceWhenAnOrderOftheYearSelectedAndOfAClientSelectedIsModified() {
		Client firstClient = new Client("1", "first client identifier");
		Client secondClient = new Client("2", "second client identifier");
		Order order = new Order("1", secondClient,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10.1);
		GuiActionRunner.execute(() -> {
			orderSwingView.getClientListModel().addElement(firstClient);
			orderSwingView.getClientListModel().addElement(secondClient);
		});
		window.list("clientsList").selectItem(1);
		GuiActionRunner.execute(() -> {
			orderSwingView.getComboboxYearsModel().addElement(2024);
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().setSelectedItem(2025);
			orderSwingView.getOrderTableModel().addOrder(order);
		});
		Order orderModified = new Order("1", secondClient,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20.1);
		GuiActionRunner.execute(() -> {
			orderSwingView.orderUpdated(orderModified);
		});
		window.label("revenueLabel")
				.requireText("Il costo totale degli ordini del cliente " + secondClient.getIdentifier() + " nel "
						+ "2025" + " è di " + String.format("%.2f", orderModified.getPrice()) + "€");

		Order orderModifiedSecondTime = new Order("1", firstClient,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20.1);
		GuiActionRunner.execute(() -> {
			orderSwingView.orderUpdated(orderModifiedSecondTime);
		});
		window.textBox("panelOrderErrorMessage").requireText("Non sono presenti ordini del 2025 per il cliente 2");
		window.label("revenueLabel").requireText("");

	}

	@GUITest
	@Test
	public void testUpdateTotalPriceWhenAnOrderOfAClientSelectedIsModifiedWithTheSameClient() throws Exception {
		Client firstClient = new Client("1", "first client identifier");
		Client secondClient = new Client("2", "second client identifier");
		Order order = new Order("1", secondClient,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10.1);
		Order secondOrder = new Order("2", secondClient,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20.1);
		GuiActionRunner.execute(() -> {
			orderSwingView.getClientListModel().addElement(firstClient);
			orderSwingView.getClientListModel().addElement(secondClient);
		});
		GuiActionRunner.execute(() -> {
			orderSwingView.getComboboxYearsModel().addElement(2024);
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().setSelectedItem(2025);
			orderSwingView.getOrderTableModel().addOrder(order);
			orderSwingView.getOrderTableModel().addOrder(secondOrder);
		});
		window.list("clientsList").selectItem(1);
		window.comboBox("yearsCombobox").clearSelection();
		Order orderModifiedThirdTime = new Order("1", secondClient,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20.1);
		GuiActionRunner.execute(() -> {
			orderSwingView.orderUpdated(orderModifiedThirdTime);
		});
		window.label("revenueLabel")
				.requireText("Il costo totale degli ordini del cliente " + secondClient.getIdentifier() + " è di "
						+ String.format("%.2f", secondOrder.getPrice() + orderModifiedThirdTime.getPrice()) + "€");
		window.textBox("panelOrderErrorMessage").requireText("");
	}

	@GUITest
	@Test
	public void testUpdateTotalPriceWhenAnOrderOfAClientSelectedIsModifiedWithADifferentClient() throws Exception {
		Client firstClient = new Client("1", "first client identifier");
		Client secondClient = new Client("2", "second client identifier");
		Order order = new Order("1", secondClient,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10.1);
		Order secondOrder = new Order("2", secondClient,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20.1);
		GuiActionRunner.execute(() -> {
			orderSwingView.getClientListModel().addElement(firstClient);
			orderSwingView.getClientListModel().addElement(secondClient);
		});
		GuiActionRunner.execute(() -> {
			orderSwingView.getComboboxYearsModel().addElement(2024);
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().setSelectedItem(2025);
			orderSwingView.getOrderTableModel().addOrder(order);
			orderSwingView.getOrderTableModel().addOrder(secondOrder);
		});
		window.list("clientsList").selectItem(1);
		window.comboBox("yearsCombobox").clearSelection();
		Order firstOrderModified = new Order("1", firstClient,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 30.5);
		GuiActionRunner.execute(() -> {
			orderSwingView.orderUpdated(firstOrderModified);
		});
		window.label("revenueLabel").requireText("Il costo totale degli ordini del cliente "
				+ secondClient.getIdentifier() + " è di " + String.format("%.2f", secondOrder.getPrice()) + "€");
		window.textBox("panelOrderErrorMessage").requireText("");
	}

	@GUITest
	@Test
	public void testUpdateTotalPriceWhenAnOrderOfAClientNotSelectedAndYearNotSelectedIsModified() throws Exception {
		Client firstClient = new Client("1", "first client identifier");
		Client secondClient = new Client("2", "second client identifier");
		Order order = new Order("1", secondClient,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10.1);
		Order secondOrder = new Order("2", secondClient,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20.1);
		GuiActionRunner.execute(() -> {
			orderSwingView.getClientListModel().addElement(firstClient);
			orderSwingView.getClientListModel().addElement(secondClient);
		});
		GuiActionRunner.execute(() -> {
			orderSwingView.getComboboxYearsModel().addElement(2024);
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().setSelectedItem(2025);
			orderSwingView.getOrderTableModel().addOrder(order);
			orderSwingView.getOrderTableModel().addOrder(secondOrder);
		});
		window.comboBox("yearsCombobox").clearSelection();
		Order firstOrderModified = new Order("1", firstClient,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 30.5);
		GuiActionRunner.execute(() -> {
			orderSwingView.orderUpdated(firstOrderModified);
		});
		window.label("revenueLabel")
				.requireText("Il costo totale degli ordini presenti nel DB è di " + String
						.format("%.2f", (firstOrderModified.getPrice() + secondOrder.getPrice())).replace(".", ",")
						+ "€");
		window.textBox("panelOrderErrorMessage").requireText("");
	}

	@GUITest
	@Test
	public void testUpdateTotalPriceWhenModifyOnlyPriceOfOrder() throws Exception {
		Client firstClient = new Client("1", "first client identifier");
		Client secondClient = new Client("2", "second client identifier");
		JLabelFixture revenueLabel = window.label("revenueLabel");
		Order order = new Order("1", secondClient,
				Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10.1);
		GuiActionRunner.execute(() -> {
			orderSwingView.getClientListModel().addElement(firstClient);
			orderSwingView.getClientListModel().addElement(secondClient);
			revenueLabel.target().setText(" ");
		});
		GuiActionRunner.execute(() -> {
			orderSwingView.getComboboxYearsModel().addElement(2024);
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().setSelectedItem(2025);
			orderSwingView.getOrderTableModel().addOrder(order);
		});
		window.comboBox("yearsCombobox").clearSelection();
		window.list("clientsList").selectItem(0);
		Order firstOrderModified = new Order("1", secondClient,
				Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 30.5);
		GuiActionRunner.execute(() -> {
			orderSwingView.orderUpdated(firstOrderModified);
		});
		window.textBox("panelOrderErrorMessage").requireText("Non ci sono ordini per il cliente 1");
		window.label("revenueLabel").requireText("");

	}

	@GUITest
	@Test
	public void testUpdateTotalPrice_ClientSelected_ClientModified_TableIsEmpty() throws Exception {
		Client firstClient = new Client("1", "first client identifier");
		Client secondClient = new Client("2", "second client identifier");
		Order order = new Order("1", secondClient,
				Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10.1);
		GuiActionRunner.execute(() -> {
			orderSwingView.getClientListModel().addElement(firstClient);
			orderSwingView.getClientListModel().addElement(secondClient);
		});
		GuiActionRunner.execute(() -> {
			orderSwingView.getComboboxYearsModel().addElement(2024);
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().setSelectedItem(2024);
			orderSwingView.getOrderTableModel().addOrder(order);
		});
		window.list("clientsList").selectItem(1);
		Order firstOrderModified = new Order("1", secondClient,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 30.5);
		GuiActionRunner.execute(() -> {
			orderSwingView.orderUpdated(firstOrderModified);
		});

		assertThat(window.table("OrdersTable").contents()).isEmpty();

	}

	@GUITest
	@Test
	public void testUpdateTotalPriceWhenOrderHasIdNull() throws Exception {
		Client firstClient = new Client("1", "first client identifier");
		Client secondClient = new Client("2", "second client identifier");
		GuiActionRunner.execute(() -> {
			orderSwingView.getClientListModel().addElement(firstClient);
			orderSwingView.getClientListModel().addElement(secondClient);
		});
		GuiActionRunner.execute(() -> {
			orderSwingView.getComboboxYearsModel().addElement(2024);
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().setSelectedItem(2024);
		});
		window.list("clientsList").selectItem(1);
		Order orderWithIdNull = new Order(null, secondClient,
				Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10.1);
		GuiActionRunner.execute(() -> {
			orderSwingView.orderUpdated(orderWithIdNull);
		});

		assertThat(window.table("OrdersTable").contents()).isEmpty();

	}

	@GUITest
	@Test
	public void testUpdateTotalPrice_NoYearNoClient_Selected_OrderPriceChanged_NoError() {
		Client firstClient = new Client("1", "first client identifier");
		Client secondClient = new Client("2", "second client identifier");
		Order order = new Order("1", secondClient,
				Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10.1);
		GuiActionRunner.execute(() -> {
			orderSwingView.getClientListModel().addElement(firstClient);
			orderSwingView.getClientListModel().addElement(secondClient);
		});
		GuiActionRunner.execute(() -> {
			orderSwingView.getComboboxYearsModel().addElement(2024);
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().setSelectedItem(2025);
			orderSwingView.getOrderTableModel().addOrder(order);
		});
		window.comboBox("yearsCombobox").clearSelection();
		window.list("clientsList").clearSelection();
		Order firstOrderModified = new Order("1", secondClient,
				Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 30.5);
		GuiActionRunner.execute(() -> {
			orderSwingView.orderUpdated(firstOrderModified);
		});
		window.textBox("panelOrderErrorMessage").requireText("");
		assertThat(window.table("OrdersTable").contents()[0]).contains(firstOrderModified.getIdentifier(),
				firstOrderModified.getClient().getName(), firstOrderModified.getDate().toString(),
				"" + firstOrderModified.getPrice());

	}
	
	
	 	@Test
	@GUITest
	public void testWhenTableOrdersRowIsSelectedOrderTextFieldsShouldBeReportOrderData() {
		Client newClient = new Client("1", "new Client id");
		Client secondClient = new Client("2", "second Client id");

		Order newOrder = new Order("1", newClient,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		GuiActionRunner.execute(() -> {
			orderSwingView.getOrderTableModel().addOrder(newOrder);

			orderSwingView.getComboboxClientsModel().addElement(newClient);

			orderSwingView.getComboboxClientsModel().addElement(secondClient);

		});
		window.table("OrdersTable").selectRows(0);
		window.comboBox("comboboxClients").requireSelection(0);
		window.textBox("textField_dayOfDateOrder").requireText("1");
		window.textBox("textField_monthOfDateOrder").requireText("1");
		window.textBox("textField_yearOfDateOrder").requireText("2025");
		window.textBox("textField_revenueOrder").requireText("10.0");

		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).requireEnabled();

	}

	@Test
	@GUITest
	public void testWhenTableOrdersRowIsDeselectedOrderTextFieldsShouldBeEmpty() {
		Client newClient = new Client("1", "new Client id");
		Client secondClient = new Client("2", "second Client id");

		Order firstOrder = new Order("1", newClient,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		Order secondOrder = new Order("2", secondClient,
				Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 20);
		GuiActionRunner.execute(() -> {
			orderSwingView.getOrderTableModel().addOrder(firstOrder);
			orderSwingView.getOrderTableModel().addOrder(secondOrder);

			orderSwingView.getComboboxClientsModel().addElement(newClient);

			orderSwingView.getComboboxClientsModel().addElement(secondClient);

		});
		window.table("OrdersTable").selectRows(1); // gli ordini sono ordinati per data e non per identificativo
		window.comboBox("comboboxClients").requireSelection(0);
		window.textBox("textField_dayOfDateOrder").requireText("1");
		window.textBox("textField_monthOfDateOrder").requireText("1");
		window.textBox("textField_yearOfDateOrder").requireText("2025");
		window.textBox("textField_revenueOrder").requireText("10.0");

		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).requireEnabled();

		window.table("OrdersTable").unselectRows(1); // gli ordini sono ordinati per data e non per identificativo
		window.comboBox("comboboxClients").requireNoSelection();
		window.textBox("textField_dayOfDateOrder").requireText("");
		window.textBox("textField_monthOfDateOrder").requireText("");
		window.textBox("textField_yearOfDateOrder").requireText("");
		window.textBox("textField_revenueOrder").requireText("");

		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).requireDisabled();

		window.table("OrdersTable").selectRows(0);
		window.comboBox("comboboxClients").requireSelection(1);
		window.textBox("textField_dayOfDateOrder").requireText("1");
		window.textBox("textField_monthOfDateOrder").requireText("1");
		window.textBox("textField_yearOfDateOrder").requireText("2024");
		window.textBox("textField_revenueOrder").requireText("20.0");

		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).requireEnabled();

	}

	@Test
	@GUITest
	public void testModifyOrderButtonShouldBeEnabledWhenAOrderIsSelectedAndAtLeastOneTextFieldIsCorrect() {
		Client newClient = new Client("1", "new Client id");
		Client secondClient = new Client("2", "second Client id");

		Order newOrder = new Order("1", newClient,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		GuiActionRunner.execute(() -> {
			orderSwingView.getOrderTableModel().addOrder(newOrder);

			orderSwingView.getComboboxClientsModel().addElement(newClient);

			orderSwingView.getComboboxClientsModel().addElement(secondClient);

		});
		window.comboBox("comboboxClients").clearSelection();

		window.table("OrdersTable").selectRows(0);

		window.textBox("textField_monthOfDateOrder").setText("");
		window.textBox("textField_monthOfDateOrder").enterText(" ");
		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).requireDisabled();

		window.textBox("textField_dayOfDateOrder").setText("");
		window.textBox("textField_dayOfDateOrder").enterText(" ");
		window.textBox("textField_monthOfDateOrder").enterText("1");
		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).requireDisabled();

		window.textBox("textField_dayOfDateOrder").enterText("1");
		window.textBox("textField_yearOfDateOrder").setText("");
		window.textBox("textField_yearOfDateOrder").enterText(" ");
		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).requireDisabled();

		window.textBox("textField_yearOfDateOrder").enterText("2025");
		window.textBox("textField_revenueOrder").setText("");
		window.textBox("textField_revenueOrder").enterText(" ");
		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).requireDisabled();

		window.textBox("textField_revenueOrder").setText("20.0");
		window.comboBox("comboboxClients").clearSelection();
		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).requireDisabled();

		window.textBox("textField_revenueOrder").setText("");
		window.textBox("textField_revenueOrder").enterText("");
		window.comboBox("comboboxClients").selectItem(0);
		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).requireDisabled();
		// no combobox clients item selected. Buttno modify order disabled
		window.textBox("textField_revenueOrder").setText("");
		window.textBox("textField_revenueOrder").enterText("10.00");
		window.textBox("textField_dayOfDateOrder").setText("");
		window.textBox("textField_monthOfDateOrder").setText("");
		window.textBox("textField_yearOfDateOrder").setText("");

		window.textBox("textField_dayOfDateOrder").enterText("1");
		window.textBox("textField_monthOfDateOrder").enterText("1");
		window.textBox("textField_yearOfDateOrder").enterText("2024");
		window.comboBox("comboboxClients").clearSelection();
		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).requireDisabled();
	}

	@Test
	@GUITest
	public void testModifyOrderButtonShouldDelegateToOrderControllerModifyOrderWhenDateIsCorrectAndResetErrorLabel() {
		Client newClient = new Client("1", "new Client id");
		Client secondClient = new Client("2", "second Client id");

		Order newOrder = new Order("1", newClient,
				Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		GuiActionRunner.execute(() -> {
			orderSwingView.getOrderTableModel().addOrder(newOrder);

			orderSwingView.getComboboxClientsModel().addElement(newClient);

			orderSwingView.getComboboxClientsModel().addElement(secondClient);

		});
		window.textBox("panelOrderErrorMessage").setText(" ");
		window.table("OrdersTable").selectRows(0);
		window.textBox("textField_dayOfDateOrder").setText("");
		window.textBox("textField_dayOfDateOrder").enterText("2");
		window.textBox("textField_yearOfDateOrder").setText("");
		window.textBox("textField_yearOfDateOrder").enterText("2025");
		window.comboBox("comboboxClients").selectItem(1);
		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).click();

		Date newDate = Date.from(LocalDate.of(2025, 1, 2).atStartOfDay(ZoneId.systemDefault()).toInstant());
		String orderID = "1";
		Map<String, Object> updates = new HashMap<String, Object>();
		updates.put("date", newDate);
		updates.put("price", 10.0);
		updates.put("client", secondClient);
		verify(orderController).modifyOrder(newOrder, updates);

		window.table("OrdersTable").requireNoSelection();
		window.textBox("textField_dayOfDateOrder").requireEmpty();
		window.textBox("textField_monthOfDateOrder").requireEmpty();
		window.textBox("textField_yearOfDateOrder").requireEmpty();
		window.textBox("textField_revenueOrder").requireEmpty();
		window.textBox("panelOrderErrorMessage").requireText("");
		window.comboBox("comboboxClients").requireNoSelection();
		window.table("OrdersTable").requireNoSelection();
		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).requireDisabled();

		window.textBox("panelOrderErrorMessage").requireText("");

		window.textBox("panelOrderErrorMessage").setText(" ");
		window.table("OrdersTable").selectRows(0);
		Order orderModified = GuiActionRunner.execute(() -> orderSwingView.getOrderTableModel().getOrderAt(0));
		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).click();

		updates = new HashMap<String, Object>();
		updates.put("date", newDate);
		updates.put("price", 10.0);
		updates.put("client", secondClient);
		verify(orderController).modifyOrder(orderModified, updates);

		window.table("OrdersTable").requireNoSelection();
		window.textBox("textField_dayOfDateOrder").requireEmpty();
		window.textBox("textField_monthOfDateOrder").requireEmpty();
		window.textBox("textField_yearOfDateOrder").requireEmpty();
		window.textBox("textField_revenueOrder").requireEmpty();
		window.textBox("panelOrderErrorMessage").requireText("");
		window.comboBox("comboboxClients").requireNoSelection();
		window.table("OrdersTable").requireNoSelection();
		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).requireDisabled();

		window.textBox("panelOrderErrorMessage").requireText("");

	}

	@Test
	@GUITest
	public void testModifyOrderButtonShouldDelegateToOrderControllerNewOrderWhenDayIsNotCorrect() {
		Client newClient = new Client("1", "new Client id");
		Client secondClient = new Client("2", "second Client id");

		Order newOrder = new Order("1", newClient,
				Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		GuiActionRunner.execute(() -> {
			orderSwingView.getOrderTableModel().addOrder(newOrder);

			orderSwingView.getComboboxClientsModel().addElement(newClient);

			orderSwingView.getComboboxClientsModel().addElement(secondClient);

		});
		window.table("OrdersTable").selectRows(0);
		// pulisco il date text field dalla data attuale dell'ordine
		window.textBox("textField_dayOfDateOrder").setText("");
		window.textBox("textField_monthOfDateOrder").setText("");
		window.textBox("textField_yearOfDateOrder").setText("");

		window.textBox("textField_dayOfDateOrder").enterText("0");
		window.textBox("textField_monthOfDateOrder").enterText("1");
		window.textBox("textField_yearOfDateOrder").enterText("2025");
		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).click();

		window.textBox("textField_dayOfDateOrder").requireEmpty();
		window.textBox("textField_monthOfDateOrder").requireEmpty();
		window.textBox("textField_yearOfDateOrder").requireEmpty();
		verifyNoInteractions(orderController);
		window.textBox("panelOrderErrorMessage").requireText("La data 0/1/2025 non è corretta");

		window.textBox("textField_dayOfDateOrder").enterText("32");
		window.textBox("textField_monthOfDateOrder").enterText("1");
		window.textBox("textField_yearOfDateOrder").enterText("2025");
		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).click();

		window.textBox("textField_dayOfDateOrder").requireEmpty();
		window.textBox("textField_monthOfDateOrder").requireEmpty();
		window.textBox("textField_yearOfDateOrder").requireEmpty();
		verifyNoInteractions(orderController);
		window.textBox("panelOrderErrorMessage").requireText("La data 32/1/2025 non è corretta");

		window.textBox("textField_dayOfDateOrder").enterText("29");
		window.textBox("textField_monthOfDateOrder").enterText("2");
		window.textBox("textField_yearOfDateOrder").enterText("2025");
		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).click();

		window.textBox("textField_dayOfDateOrder").requireEmpty();
		window.textBox("textField_monthOfDateOrder").requireEmpty();
		window.textBox("textField_yearOfDateOrder").requireEmpty();
		verifyNoInteractions(orderController);
		window.textBox("panelOrderErrorMessage").requireText("La data 29/2/2025 non è corretta");

		window.textBox("textField_dayOfDateOrder").enterText("30");
		window.textBox("textField_monthOfDateOrder").enterText("2");
		window.textBox("textField_yearOfDateOrder").enterText("2000");
		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).click();

		window.textBox("textField_dayOfDateOrder").requireEmpty();
		window.textBox("textField_monthOfDateOrder").requireEmpty();
		window.textBox("textField_yearOfDateOrder").requireEmpty();
		verifyNoInteractions(orderController);
		window.textBox("panelOrderErrorMessage").requireText("La data 30/2/2000 non è corretta");

		window.textBox("textField_dayOfDateOrder").enterText("29");
		window.textBox("textField_monthOfDateOrder").enterText("2");
		window.textBox("textField_yearOfDateOrder").enterText("2023");
		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).click();

		window.textBox("textField_dayOfDateOrder").requireEmpty();
		window.textBox("textField_monthOfDateOrder").requireEmpty();
		window.textBox("textField_yearOfDateOrder").requireEmpty();
		verifyNoInteractions(orderController);
		window.textBox("panelOrderErrorMessage").requireText("La data 29/2/2023 non è corretta");

		window.textBox("textField_dayOfDateOrder").enterText("30");
		window.textBox("textField_monthOfDateOrder").enterText("2");
		window.textBox("textField_yearOfDateOrder").enterText("2024");
		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).click();

		window.textBox("textField_dayOfDateOrder").requireEmpty();
		window.textBox("textField_monthOfDateOrder").requireEmpty();
		window.textBox("textField_yearOfDateOrder").requireEmpty();
		verifyNoInteractions(orderController);
		window.textBox("panelOrderErrorMessage").requireText("La data 30/2/2024 non è corretta");

		window.textBox("textField_dayOfDateOrder").enterText("29");
		window.textBox("textField_monthOfDateOrder").enterText("2");
		window.textBox("textField_yearOfDateOrder").enterText("2100");
		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).click();

		window.textBox("textField_dayOfDateOrder").requireEmpty();
		window.textBox("textField_monthOfDateOrder").requireEmpty();
		window.textBox("textField_yearOfDateOrder").requireEmpty();
		verifyNoInteractions(orderController);
		window.textBox("panelOrderErrorMessage").requireText("La data 29/2/2100 non è corretta");

		window.textBox("textField_dayOfDateOrder").enterText("31");
		window.textBox("textField_monthOfDateOrder").enterText("4");
		window.textBox("textField_yearOfDateOrder").enterText("2025");
		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).click();

		window.textBox("textField_dayOfDateOrder").requireEmpty();
		window.textBox("textField_monthOfDateOrder").requireEmpty();
		window.textBox("textField_yearOfDateOrder").requireEmpty();
		verifyNoInteractions(orderController);
		window.textBox("panelOrderErrorMessage").requireText("La data 31/4/2025 non è corretta");
	}

	@Test
	@GUITest
	public void testModifyOrderButtonShouldDelegateToOrderControllerModifyOrderWhenMonthIsNotCorrect() {
		Client newClient = new Client("1", "new Client id");
		Client secondClient = new Client("2", "second Client id");

		Order newOrder = new Order("1", newClient,
				Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		GuiActionRunner.execute(() -> {
			orderSwingView.getOrderTableModel().addOrder(newOrder);

			orderSwingView.getComboboxClientsModel().addElement(newClient);

			orderSwingView.getComboboxClientsModel().addElement(secondClient);

		});
		window.table("OrdersTable").selectRows(0);
		// pulisco il date text field dalla data attuale dell'ordine
		window.textBox("textField_dayOfDateOrder").setText("");
		window.textBox("textField_monthOfDateOrder").setText("");
		window.textBox("textField_yearOfDateOrder").setText("");

		window.textBox("textField_dayOfDateOrder").enterText("1");
		window.textBox("textField_monthOfDateOrder").enterText("13");
		window.textBox("textField_yearOfDateOrder").enterText("2025");
		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).click();

		window.textBox("textField_dayOfDateOrder").requireEmpty();
		window.textBox("textField_monthOfDateOrder").requireEmpty();
		window.textBox("textField_yearOfDateOrder").requireEmpty();
		verifyNoInteractions(orderController);
		window.textBox("panelOrderErrorMessage").requireText("La data 1/13/2025 non è corretta");

		window.textBox("textField_dayOfDateOrder").enterText("1");
		window.textBox("textField_monthOfDateOrder").enterText("0");
		window.textBox("textField_yearOfDateOrder").enterText("2025");
		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).click();

		window.textBox("textField_dayOfDateOrder").requireEmpty();
		window.textBox("textField_monthOfDateOrder").requireEmpty();
		window.textBox("textField_yearOfDateOrder").requireEmpty();
		verifyNoInteractions(orderController);
		window.textBox("panelOrderErrorMessage").requireText("La data 1/0/2025 non è corretta");
	}

	@Test
	@GUITest
	public void testModifyOrderButtonShouldDelegateToOrderControllerModifyOrderWhenYearIsNotCorrect() {
		Client newClient = new Client("1", "new Client id");
		Client secondClient = new Client("2", "second Client id");

		Order newOrder = new Order("1", newClient,
				Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		GuiActionRunner.execute(() -> {
			orderSwingView.getOrderTableModel().addOrder(newOrder);

			orderSwingView.getComboboxClientsModel().addElement(newClient);

			orderSwingView.getComboboxClientsModel().addElement(secondClient);

		});
		window.table("OrdersTable").selectRows(0);
		// pulisco il date text field dalla data attuale dell'ordine
		window.textBox("textField_dayOfDateOrder").setText("");
		window.textBox("textField_monthOfDateOrder").setText("");
		window.textBox("textField_yearOfDateOrder").setText("");

		window.textBox("textField_dayOfDateOrder").enterText("1");
		window.textBox("textField_monthOfDateOrder").enterText("1");
		window.textBox("textField_yearOfDateOrder").enterText("2026");
		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).click();

		window.textBox("textField_dayOfDateOrder").requireEmpty();
		window.textBox("textField_monthOfDateOrder").requireEmpty();
		window.textBox("textField_yearOfDateOrder").requireEmpty();
		verifyNoInteractions(orderController);
		window.textBox("panelOrderErrorMessage").requireText("La data 1/1/2026 non è corretta");

		window.textBox("textField_dayOfDateOrder").enterText("1");
		window.textBox("textField_monthOfDateOrder").enterText("1");
		window.textBox("textField_yearOfDateOrder").enterText("1924");
		window.button(JButtonMatcher.withText("<html><center>Modifica<br>ordine</center></html>")).click();

		window.textBox("textField_dayOfDateOrder").requireEmpty();
		window.textBox("textField_monthOfDateOrder").requireEmpty();
		window.textBox("textField_yearOfDateOrder").requireEmpty();
		verifyNoInteractions(orderController);
		window.textBox("panelOrderErrorMessage").requireText("La data 1/1/1924 non è corretta");

	}

	@Test
	@GUITest
	public void testRemoveOrderButtonShouldBeEnebledOnlyWhenAOrderIsSelected() {
		Client client = new Client("1", "test id 1");
		Order order = new Order("1", client,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		GuiActionRunner.execute(() -> {
			OrderTableModel listOrderModel = orderSwingView.getOrderTableModel();
			listOrderModel.addOrder(order);
		});
		window.table("OrdersTable").selectRows(0);
		window.button(JButtonMatcher.withText("<html><center>Rimuovi<br>ordine</center></html>")).requireEnabled();
		window.table("OrdersTable").unselectRows(0);
		window.button(JButtonMatcher.withText("<html><center>Rimuovi<br>ordine</center></html>")).requireDisabled();

	}

	@Test
	@GUITest
	public void testRemoveOrderButtonShouldDelegateToOrderControllerDeleteOrder() {
		Client client = new Client("1", "test id 1");
		Order order = new Order("1", client,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		Order order2 = new Order("2", client,
				Date.from(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 10);
		GuiActionRunner.execute(() -> {
			OrderTableModel listOrderModel = orderSwingView.getOrderTableModel();
			listOrderModel.addOrder(order);
			listOrderModel.addOrder(order2);
			orderSwingView.getComboboxClientsModel().addElement(client);
		});
		window.table("OrdersTable").selectRows(0);
		window.button(JButtonMatcher.withText("<html><center>Rimuovi<br>ordine</center></html>")).click();
		verify(orderController).deleteOrder(order);

		window.textBox("textField_dayOfDateOrder").requireEmpty();
		window.textBox("textField_monthOfDateOrder").requireEmpty();
		window.textBox("textField_yearOfDateOrder").requireEmpty();
		window.textBox("textField_revenueOrder").requireEmpty();
		window.textBox("panelOrderErrorMessage").requireText("");
		window.comboBox("comboboxClients").requireNoSelection();
	}

	

	@Test
	@GUITest
	public void testOrderAddedWhenItsClientIsSelectedAndResetErrorLabel() {
		Client firstClient = new Client("1", "first client id");
		LocalDateTime localDateTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
		Order orderToAdd = new Order("1", firstClient,
				Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()), 10);
		GuiActionRunner.execute(() -> {
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().addElement(2024);
			orderSwingView.getComboboxYearsModel().addElement(2023);
			orderSwingView.getComboboxYearsModel().addElement("-- Nessun anno --");
			orderSwingView.getComboboxYearsModel().setSelectedItem("-- Nessun anno --");
			orderSwingView.getClientListModel().addElement(firstClient);
		});
		window.list("clientsList").selectItem(0);
		GuiActionRunner.execute(() -> {
			orderSwingView.orderAdded(orderToAdd);
		});
		assertThat(window.comboBox("yearsCombobox").contents()).containsExactly("2025", "2024", "2023",
				"-- Nessun anno --");
		String[][] contents = window.table("OrdersTable").contents();
		assertThat(contents)
				.contains(new String[] { orderToAdd.getIdentifier().toString(), orderToAdd.getClient().getName(),
						orderToAdd.getDate().toString(), String.valueOf(orderToAdd.getPrice()) }, atIndex(0));
		window.comboBox("yearsCombobox").requireNoSelection();
		window.textBox(JTextComponentMatcher.withName("panelOrderErrorMessage")).requireText("");

	}
	
	@RunsInEDT
	@Test
	@GUITest
	public void testAllOrdersButtonShouldBeVisibleOnlyWhenAClientIsSelected() {
		GuiActionRunner.execute(() -> {
			orderSwingView.getClientListModel().addElement(new Client("1", "new client 1"));
		});
		window.list("clientsList").selectItem(0);
		window.button(JButtonMatcher.withText("<html><center>Visualizza ordini<br>di tutti i clienti</center></html>"))
				.requireVisible();
		window.list("clientsList").clearSelection();
		window.button(JButtonMatcher.withText("<html><center>Visualizza ordini<br>di tutti i clienti</center></html>"))
				.requireNotVisible();

	}

	@RunsInEDT
	@Test
	@GUITest
	public void testShowAllOrdersShouldDelegateToControllerFindAllOrdersAndRevenue() {
		Client client = new Client("1", "new client id");
		GuiActionRunner.execute(() -> {
			orderSwingView.getClientListModel().addElement(client);
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().addElement(2024);

		});
		window.list("clientsList").selectItem(0);
		window.comboBox("yearsCombobox").selectItem("" + 2024);
		window.button(JButtonMatcher.withText("<html><center>Visualizza ordini<br>di tutti i clienti</center></html>")).click();
		verify(orderController).allOrdersByYear(2024);
		window.list("clientsList").requireNoSelection();
		window.button(JButtonMatcher.withText("<html><center>Visualizza ordini<br>di tutti i clienti</center></html>"))
				.requireNotVisible();

	}
	@Test
	@GUITest
	public void testOrderAddedWhenItsClientIsNotSelectedAndResetErrorLabel() {
		Client firstClient = new Client("1", "first client id");
		Client secondClient = new Client("2", "second client id");
		LocalDateTime localDateTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
		Order orderToAdd = new Order("1", firstClient,
				Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()), 10);
		GuiActionRunner.execute(() -> {
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().addElement(2024);
			orderSwingView.getComboboxYearsModel().addElement(2023);
			orderSwingView.getComboboxYearsModel().addElement("-- Nessun anno --");
			orderSwingView.getComboboxYearsModel().setSelectedItem("-- Nessun anno --");
			orderSwingView.getClientListModel().addElement(firstClient);
			orderSwingView.getClientListModel().addElement(secondClient);
		});
		window.list("clientsList").selectItem(1);
		GuiActionRunner.execute(() -> {
			orderSwingView.orderAdded(orderToAdd);
		});
		assertThat(window.comboBox("yearsCombobox").contents()).containsExactly("2025", "2024", "2023",
				"-- Nessun anno --");
		String[][] contents = window.table("OrdersTable").contents();
		assertThat(contents)
				.doesNotContain(new String[] { orderToAdd.getIdentifier().toString(), orderToAdd.getClient().getName(),
						orderToAdd.getDate().toString(), String.valueOf(orderToAdd.getPrice()) }, atIndex(0));
		window.comboBox("yearsCombobox").requireNoSelection();
		window.textBox(JTextComponentMatcher.withName("panelOrderErrorMessage")).requireText("");

	}
	
	@Test
	@GUITest
	public void testOrderAddedWhenItsClientIsNotSelectedShouldVerifyOrderControllerYearsofTheOrders() {
		Client firstClient = new Client("1", "first client id");
		Client secondClient = new Client("2", "second client id");
		LocalDateTime localDateTime = LocalDateTime.of(2023, 1, 1, 0, 0, 0);
		LocalDateTime secondLocalDateTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
		Order orderToAdd = new Order("1", firstClient,
				Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()), 10);
		Order secondOrderToAdd = new Order("2", firstClient,
				Date.from(secondLocalDateTime.atZone(ZoneId.systemDefault()).toInstant()), 10);
		GuiActionRunner.execute(() -> {
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().addElement(2024);
			orderSwingView.getComboboxYearsModel().addElement("-- Nessun anno --");
			orderSwingView.getComboboxYearsModel().setSelectedItem("-- Nessun anno --");
			orderSwingView.getClientListModel().addElement(firstClient);
			orderSwingView.getClientListModel().addElement(secondClient);
		});
		window.list("clientsList").selectItem(1);
		GuiActionRunner.execute(() -> {
			orderSwingView.orderAdded(orderToAdd);
		});
		GuiActionRunner.execute(() -> {
			orderSwingView.orderAdded(secondOrderToAdd);
		});
		verify(orderController, times(1)).yearsOfTheOrders();

	}

	@Test
	@GUITest
	public void testOrderAddedWhenItsNoOneClientIsSelectedAndYearAndResetErrorLabel() {
		Client firstClient = new Client("1", "first client id");
		Client secondClient = new Client("2", "second client id");
		LocalDateTime localDateTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
		Order orderToAdd = new Order("1", firstClient,
				Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()), 10);
		GuiActionRunner.execute(() -> {
			orderSwingView.getComboboxYearsModel().addElement(2025);
			orderSwingView.getComboboxYearsModel().addElement(2024);
			orderSwingView.getComboboxYearsModel().addElement(2023);
			orderSwingView.getComboboxYearsModel().addElement("-- Nessun anno --");
			orderSwingView.getComboboxYearsModel().setSelectedItem("-- Nessun anno --");
			orderSwingView.getClientListModel().addElement(firstClient);
			orderSwingView.getClientListModel().addElement(secondClient);
		});
		GuiActionRunner.execute(() -> {
			orderSwingView.orderAdded(orderToAdd);
		});
		assertThat(window.comboBox("yearsCombobox").contents()).containsExactly("2025", "2024", "2023",
				"-- Nessun anno --");
		String[][] contents = window.table("OrdersTable").contents();
		assertThat(contents)
				.contains(new String[] { orderToAdd.getIdentifier().toString(), orderToAdd.getClient().getName(),
						orderToAdd.getDate().toString(), String.valueOf(orderToAdd.getPrice()) }, atIndex(0));
		window.comboBox("yearsCombobox").requireNoSelection();
		window.textBox(JTextComponentMatcher.withName("panelOrderErrorMessage")).requireText("");

	}
 
	  
	 

}