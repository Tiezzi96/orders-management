package com.unifi.ordersmgmt.model;

import static org.junit.Assert.*;


import org.junit.Test;

public class ClientTest {

	@Test
    public void testEqualsWithSameInstanceShouldReturnTrue() {
        Client c1 = new Client("123");
        assertEquals(c1, c1);
    }

    @Test
    public void testEqualsWithNullShouldReturnFalse() {
        Client c1 = new Client("123");
        assertNotEquals(c1, null);
    }

    @Test
    public void testEqualsWithDifferentClassShouldReturnFalse() {
        Client c1 = new Client("123");
        Object obj = new Object();
        assertNotEquals(c1, obj);
    }

    @Test
    public void testEqualsWhenNoIdentifierDefined() {
        Client c1 = new Client("123");
        Client c2 = new Client("456");
        assertEquals(c1, c2);
    }

    @Test
    public void testEqualsWithSameIdentifierShouldReturnTrue() {
        Client c1 = new Client("123", "");
        Client c2 = new Client("123", "");
        assertEquals(c1, c2);
    }

    @Test
    public void testEqualsWithBothNullIdentifierShouldReturnTrue() {
        Client c1 = new Client(null, "Mario");
        Client c2 = new Client(null, "Mario");
        assertEquals(c1, c2);
    }

    @Test
    public void testEqualsWhenNullIdentifierIsOneOnlyShouldReturnFalse() {
        Client c1 = new Client(null, "");
        Client c2 = new Client("123", "");
        assertNotEquals(c1, c2);
    }
    
    @Test
    public void testEqualsWhenIdentifierAreDifferentShouldReturnFalse() {
        Client c1 = new Client("1234", "");
        Client c2 = new Client("123", "");
        assertNotEquals(c1, c2);
    }
    
    @Test
    public void testHashCodeWithSameIdentifierShouldBeEqual() {
        Client c1 = new Client("123", "Mario");
        Client c2 = new Client("123", "Mario"); 

        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testHashCodeWithNullIdentifierShouldBeEqual() {
        Client c1 = new Client(null, "Anna");
        Client c2 = new Client(null, "Marco");

        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testHashCodeWhenIdentifierAreDifferentShouldBeDifferent() {
        Client c1 = new Client("123", "Mario");
        Client c2 = new Client("456", "Mario");

        assertNotEquals(c1.hashCode(), c2.hashCode());
    }

}
