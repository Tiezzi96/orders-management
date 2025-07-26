package com.unifi.ordersmgmt.view.swing;

import static org.junit.Assert.*;

import java.util.regex.Pattern;

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

	}

}
