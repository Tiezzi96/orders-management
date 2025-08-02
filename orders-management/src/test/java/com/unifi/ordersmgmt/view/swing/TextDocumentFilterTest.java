package com.unifi.ordersmgmt.view.swing;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.text.AbstractDocument;
import javax.swing.text.PlainDocument;

import org.assertj.core.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TextDocumentFilterTest {

	@Parameterized.Parameters(name = "{index}: input={0} expectedOutput={1} onChange={2}")
	public static List<Object> data() {
		return Arrays.asList(new Object[][] {{ "1234", "1234", true }, { "12345678910", "", true }, { "15a", "", true },
				{ "12.34", "12.34", true }, { "12,34", "", true }, { "1.345", "", true } });
	}

	private final String input;
	private final String expectedOutput;
	private final boolean expectOnChange;
	private PlainDocument doc;
	private AtomicBoolean onChangeCalled;

	public TextDocumentFilterTest(String input, String expectedOutput, boolean expectedOnChange) {
		this.input = input;
		this.expectedOutput = expectedOutput;
		this.expectOnChange = expectedOnChange;
	}

	@Before
	public void setUp() {
		doc = new PlainDocument();
		onChangeCalled = new AtomicBoolean(false);
		// maxLength = 4, regex = cifre con opzionali fino a 2 decimali
		((AbstractDocument) doc).setDocumentFilter(
				new TextDocumentFilter(10, "^\\d*(\\.\\d{0,2})?$", () -> onChangeCalled.set(true), " "));

	}
/*
	@Test
	public void shouldAcceptUpToFourDigits() throws Exception {
		onChangeCalled.set(false);
		doc.insertString(0, "1234", null);
		assertEquals("1234", doc.getText(0, doc.getLength()));
		assertTrue("onChange should have been called", onChangeCalled.get());

	}
	@Test
	public void shouldNotAcceptUpToFiveDigits() throws Exception {
		onChangeCalled.set(false);
		doc.insertString(0, "12345678910", null);
		assertEquals("", doc.getText(0, doc.getLength()));
		assertTrue("onChange should have been called", onChangeCalled.get());

	}

	@Test
	public void shouldNotAcceptUpToAlphaDigits() throws Exception {
		onChangeCalled.set(false);
		doc.insertString(0, "15a", null);
		assertEquals("", doc.getText(0, doc.getLength()));
		assertTrue("onChange should have been called", onChangeCalled.get());

	}

	@Test
	public void shouldAcceptValidDecimalTwoPlaces() throws Exception {
		onChangeCalled.set(false);
		doc.insertString(0, "12.34", null);
		assertEquals("12.34", doc.getText(0, doc.getLength()));
		assertTrue("onChange should have been called", onChangeCalled.get());

	}

	@Test
	public void shouldNotAcceptInvalidDecimalTwoPlaces() throws Exception {
		onChangeCalled.set(false);
		doc.insertString(0, "12,34", null);
		assertEquals("", doc.getText(0, doc.getLength()));
		assertTrue("onChange should have been called", onChangeCalled.get());

	}

	@Test
	public void shouldRejectDecimalWithThreePlaces() throws Exception {
		onChangeCalled.set(false);
		doc.insertString(0, "1.345", null);
		assertEquals("", doc.getText(0, doc.getLength()));
		assertTrue("onChange should have been called", onChangeCalled.get());

	}*/
	@Test
	public void shouldRejectDecimalWithThreePlaces() throws Exception {
		onChangeCalled.set(false);
		doc.insertString(0, input, null);
		assertEquals(expectedOutput, doc.getText(0, doc.getLength()));
		assertTrue("onChange should have been called", expectOnChange);
	}
}
