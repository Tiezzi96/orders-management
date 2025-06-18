package com.unifi.ordersmgmt.model;

import static org.junit.Assert.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.junit.Test;

public class OrderTest {

	 private Client client1 = new Client("CLIENT-00001", "Mario Rossi");
	    private Client client2 = new Client("CLIENT-00002", "Luca Verdi");

	    private Date truncatedNow() {
	        return Date.from(Instant.now().truncatedTo(ChronoUnit.SECONDS));
	    }

	    @Test
	    public void testEqualsWithSameObjectShouldReturnTrue() {
	        Order order = new Order("ORDER-00001", client1, truncatedNow(), 100.0);
	        assertEquals(order, order);
	    }

	    @Test
	    public void testEqualsWithNullShouldReturnFalse() {
	        Order order = new Order("ORDER-00001", client1, truncatedNow(), 100.0);
	        assertNotEquals(order, null);
	    }

	    @Test
	    public void testEqualsWithDifferentClassShouldReturnFalse() {
	        Order order = new Order("ORDER-00001", client1, truncatedNow(), 100.0);
	        assertNotEquals(order, "not an order");
	    }

	    @Test
	    public void testEqualsWithDifferentIdentifierShouldReturnFalse() {
	        Date date = truncatedNow();
	        Order o1 = new Order("ORDER-00001", client1, date, 100.0);
	        Order o2 = new Order("ORDER-00002", client1, date, 100.0);
	        assertNotEquals(o1, o2);
	    }

	    @Test
	    public void testEqualsWithDifferentClientShouldReturnFalse() {
	        Date date = truncatedNow();
	        Order o1 = new Order("ORDER-00001", client1, date, 100.0);
	        Order o2 = new Order("ORDER-00001", client2, date, 100.0);
	        assertNotEquals(o1, o2);
	    }

	    @Test
	    public void testEqualsWithDifferentDateShouldReturnFalse() {
	        Date now = truncatedNow();
	        Date later = Date.from(now.toInstant().plusSeconds(1));
	        Order o1 = new Order("ORDER-00001", client1, now, 100.0);
	        Order o2 = new Order("ORDER-00001", client1, later, 100.0);
	        assertNotEquals(o1, o2);
	    }

	    @Test
	    public void testEqualsWithDifferentPriceShouldReturnFalse() {
	        Date date = truncatedNow();
	        Order o1 = new Order("ORDER-00001", client1, date, 100.0);
	        Order o2 = new Order("ORDER-00001", client1, date, 200.0);
	        assertNotEquals(o1, o2);
	    }

	    @Test
	    public void testEqualsWhenAllFieldsAreEqualShouldReturnTrue() {
	        Date date = truncatedNow();
	        Order o1 = new Order("ORDER-00001", client1, date, 100.0);
	        Order o2 = new Order("ORDER-00001", new Client("CLIENT-00001", "Mario Rossi"), date, 100.0);
	        assertEquals(o1, o2);
	    }
	    
	    @Test
	    public void testEqualsWhenAllIdentifierAreNullShouldReturnTrue() {
	        Order o1 = new Order();
	        Order o2 = new Order();
	        assertEquals(o1, o2);
	    }
	    
	    @Test
	    public void testEqualsWhenOnedentifierIsNullShouldReturnFalse() {
	        Date date = truncatedNow();
	        Order o1 = new Order();
	        Order o2 = new Order("ORDER-00001", client1, date, 100.0);
	        assertNotEquals(o1, o2);
	    }
	    
	    @Test
	    public void testHashCode_consistency() {
	        Order order = new Order("ORDER-00001", client1, truncatedNow(), 50.0);
	        int first = order.hashCode();
	        assertEquals(first, order.hashCode());
	    }

	    @Test
	    public void testHashCode_equalsContract() {
	        Date date = truncatedNow();
	        Order o1 = new Order("ORDER-00001", client1, date, 50.0);
	        Order o2 = new Order("ORDER-00001", new Client("CLIENT-00001", "Mario Rossi"), date, 50.0);
	        assertTrue(o1.equals(o2));
	        assertEquals(o1.hashCode(), o2.hashCode());
	    }
	    
	    @Test
	    public void testHashCode_allOrderNull() {
	        Order o1 = new Order();
	        Order o2 = new Order();
	        assertEquals(o1, o2);
	        assertEquals(o1.hashCode(), o2.hashCode());
	    }

}
