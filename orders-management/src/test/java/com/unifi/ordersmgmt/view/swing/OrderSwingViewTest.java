package com.unifi.ordersmgmt.view.swing;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import java.util.regex.Pattern;

import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.core.matcher.JLabelMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.runner.GUITestRunner;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.assertj.swing.timing.Pause;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

import com.unifi.ordersmgmt.controller.OrderController;
import com.unifi.ordersmgmt.model.Client;

@RunWith(GUITestRunner.class)
public class OrderSwingViewTest extends AssertJSwingJUnitTestCase {

	private AutoCloseable autoCloseable;
	private OrderSwingView orderSwingView;
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

}
