package com.unifi.ordersmgmt.model;

public abstract class BaseElement {
	protected String identifier;
	
	protected BaseElement() {
		// TODO Auto-generated constructor stub
	}
	
	protected BaseElement(String id) {
		identifier = id;
	}
	
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public String getIdentifier() {
		return identifier;
	}
}
