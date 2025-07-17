package com.unifi.ordersmgmt.service;

import java.util.List;
import java.util.Map;

import com.unifi.ordersmgmt.model.Client;
import com.unifi.ordersmgmt.model.Order;
import com.unifi.ordersmgmt.transaction.TransactionManager;

public class TransactionalOrderService implements OrderService {

	private TransactionManager mongoTransactionManager;

	public TransactionalOrderService(TransactionManager mongoTransactionManager) {
		this.mongoTransactionManager = mongoTransactionManager;
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<Order> allOrdersByYear(int year) {
		// TODO Auto-generated method stub
		return mongoTransactionManager.executeTransaction((clientRepo, orderRepo) -> orderRepo.findOrderByYear(year));
	}

	@Override
	public List<Order> findallOrdersByClientByYear(Client client, int year) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Order addOrder(Order order) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeOrder(Order order) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Integer> findYearsOfOrders() {
		// TODO Auto-generated method stub
		return mongoTransactionManager.executeTransaction((clientRepo, orderRepo) -> orderRepo.getYearsOfOrders());
	}

	@Override
	public Order updateOrder(Order order, Map<String, Object> updates) {
		// TODO Auto-generated method stub
		return null;
	}

}
