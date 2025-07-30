package com.unifi.ordersmgmt.view.swing;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.atIndex;
import static org.assertj.swing.data.TableCell.row;
import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.regex.Pattern;

import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;

import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.annotation.RunsInEDT;
import org.assertj.swing.core.MouseButton;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.core.matcher.JLabelMatcher;
import org.assertj.swing.core.matcher.JTextComponentMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
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
		System.out.println(secondClient.toString());
		window.comboBox("yearsCombobox").clearSelection();
		window.list("clientsList").selectItem(Pattern.compile("" + secondClient.toString()));
		window.comboBox("yearsCombobox").selectItem(Pattern.compile("" + 2024));
		System.out.println("value: " + window.list("clientsList").item(1).value());
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
		assertThat(window.comboBox("yearsCombobox").contents()).containsExactly("2025", "2024", "2023", "2022", "-- Nessun anno --");
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

}
