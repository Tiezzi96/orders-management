package com.unifi.ordersmgmt.model;

import java.util.Objects;

public class Client extends BaseElement {

	private String name;

	public Client() {
		this.identifier = null;
		this.name = null;
	}

	public Client(String id, String name) {
		this.identifier = id;
		this.name = name;
	}

	public Client(String name) {
		this.name = name;
		this.identifier = null;
	}
	
	public String getName() {
		return name;
	}
	
	
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return Objects.toString(identifier, "null") + ", " +
        Objects.toString(name, "null");
	}
	
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		if (this == obj)
			return true;
		Client otherClient = (Client) obj;
		if (this.identifier == null) {
			if (otherClient.identifier != null)
				return false;
		} else if (!this.identifier.equals(otherClient.identifier)) {
			return false;
		}
		return true;

	}
	
	@Override
	public int hashCode() {
	    return Objects.hash(identifier); 
	}
}
