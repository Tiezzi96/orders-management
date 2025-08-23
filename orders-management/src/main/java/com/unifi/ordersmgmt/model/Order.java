package com.unifi.ordersmgmt.model;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Order extends BaseElement {

	private Date date;
	private double price;
	private Client client;
	private static final Logger logger = LogManager.getLogger(Order.class);

	public Order(String id, Client client, Date date, double price) {
		super(id);
		this.client = client;
		this.date = date;
		this.price = price;
	}

	public Order() {
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
		return "Order{id='" + identifier + "', client="
				+ (client != null && client.getIdentifier() != null ? client.getIdentifier() : "null") + ", date="
				+ date + ", price=" + price + "}";
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		if (this == obj)
			return true;
		Order otherOrder = (Order) obj;
		if (this.identifier == null) {
			if (otherOrder.identifier != null)
				return false;
		} else if (!this.identifier.equals(otherOrder.identifier)) {
			return false;
		} else if (!this.client.equals(otherOrder.getClient())) {
			logger.info("different clients");
			return false;
		} else if (!this.date.toInstant().truncatedTo(ChronoUnit.DAYS)
				.equals(otherOrder.getDate().toInstant().truncatedTo(ChronoUnit.DAYS))) {
			logger.info("different dates: {}, {}", date, otherOrder.getDate());

			return false;
		} else if (this.price != otherOrder.getPrice()) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int idHash = (identifier != null) ? identifier.hashCode() : 0;
		int clientHash = (client != null) ? client.hashCode() : 0;

		int dateHash;
		if (date != null) {
			dateHash = date.toInstant().truncatedTo(ChronoUnit.SECONDS).hashCode();
		} else {
			dateHash = 0;
		}

		int priceHash = Double.valueOf(price).hashCode();

		return Objects.hash(idHash, clientHash, dateHash, priceHash);
	}

}
