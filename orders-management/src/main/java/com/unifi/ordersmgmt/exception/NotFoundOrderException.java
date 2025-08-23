package com.unifi.ordersmgmt.exception;

public class NotFoundOrderException extends RuntimeException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public NotFoundOrderException(String message) {
		super(message);
	}

}
