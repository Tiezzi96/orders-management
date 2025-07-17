package com.unifi.ordersmgmt.transaction;

import com.unifi.ordersmgmt.repository.ClientRepository;
import com.unifi.ordersmgmt.repository.OrderRepository;

@FunctionalInterface
public interface TransactionalFunction<R> {
	R apply(ClientRepository clientRepository, OrderRepository orderRepository);

}
