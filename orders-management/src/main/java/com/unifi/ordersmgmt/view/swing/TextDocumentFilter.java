package com.unifi.ordersmgmt.view.swing;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class TextDocumentFilter extends DocumentFilter {

	/**
	 * @param maxLength lunghezza massima del testo accettato (>=0)
	 * @param regex     espressione regolare che il testo completo deve rispettare
	 * 
	 */

	private final String regex;

	private final int maxLength;

	private final Runnable onChange;
	private final String spaces;

	public TextDocumentFilter(int maxLength, String regex, Runnable onChange, String spaces) {
		this.maxLength = maxLength;
		this.regex = regex;
		this.onChange = onChange;
		this.spaces = spaces;
	}

	@Override
	public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
			throws BadLocationException {
		if (isValidInput(fb, text)) {
			super.replace(fb, offset, length, text.replace(spaces, ""), attrs);
		}
		onChange.run();
	}

	@Override
	public void insertString(FilterBypass fb, int offset, String text, AttributeSet attrs) throws BadLocationException {
		replace(fb, offset, 0, text.replace(spaces, ""), attrs);

	}

	private boolean isValidInput(FilterBypass fb, String text) throws BadLocationException {
		String newText = fb.getDocument().getText(0, fb.getDocument().getLength()) + text.replace(spaces, "");
		return newText.matches(regex) && newText.length() <= maxLength;
	}
}
