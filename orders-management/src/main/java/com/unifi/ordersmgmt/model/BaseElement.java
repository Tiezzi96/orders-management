package com.unifi.ordersmgmt.model;

public abstract class BaseElement {
	protected String identifier;
	
	public BaseElement() {
		// TODO Auto-generated constructor stub
	}
	
	public BaseElement(String id) {
		identifier = id;
	}
	
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public String getIdentifier() {
		return identifier;
	}
}
