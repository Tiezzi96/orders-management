package com.unifi.ordersmgmt.view.swing;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;

import java.util.regex.Pattern;

import org.assertj.swing.annotation.GUITest;
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
		window.button(JButtonMatcher.withText("Aggiungi cliente")).requireDisabled();
		window.button(JButtonMatcher.withText("Rimuovi cliente")).requireDisabled();
		window.label(JLabelMatcher.withName("revenueLabel"));
		window.comboBox("comboboxClients");


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

}
