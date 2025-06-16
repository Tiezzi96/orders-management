package com.unifi.ordersmgmt.model;

import java.util.Date;

public class Order extends BaseElement {
	
	private Date date;
	private double price;
	private Client client;

	public Order(String id, Client client, Date date, double price) {
		super(id);
		this.client = client;
		this.date = date;
		this.price = price;
	}

	public Order() {
		// TODO Auto-generated constructor stub
		this.identifier = null;
		this.price = -1;
		this.date = null;
	}

	public Client getClient() {
		return client;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}


	@Override
	public String toString() {
		return "Order{id='" + identifier + "', client=" + client.getIdentifier() + ", date=" + date + ", price=" + price
				+ "}";
	}

}
