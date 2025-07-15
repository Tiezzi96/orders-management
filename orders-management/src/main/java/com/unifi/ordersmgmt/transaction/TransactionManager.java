package com.unifi.ordersmgmt.transaction;

public interface TransactionManager {
	public <R> R executeTransaction(TransactionalFunction<R> transactionalFunction);
}
