package com.unifi.ordersmgmt.service;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.unifi.ordersmgmt.exception.NotFoundClientException;
import com.unifi.ordersmgmt.exception.NotFoundOrderException;
import com.unifi.ordersmgmt.model.Client;
import com.unifi.ordersmgmt.model.Order;
import com.unifi.ordersmgmt.transaction.TransactionManager;

public class TransactionalOrderService implements OrderService {

	private static final String CLIENT_MESSAGE_EXCEPTION = "Il cliente con id %s non è presente nel database";
	private TransactionManager mongoTransactionManager;
	private static final Logger logger = LogManager.getLogger(TransactionalOrderService.class);

	public TransactionalOrderService(TransactionManager mongoTransactionManager) {
		this.mongoTransactionManager = mongoTransactionManager;
	}

	@Override
	public List<Order> allOrdersByYear(int year) {
		return mongoTransactionManager.executeTransaction((clientRepo, orderRepo) -> orderRepo.findOrderByYear(year));
	}

	@Override
	public List<Order> findallOrdersByClientByYear(Client client, int year) {
		return mongoTransactionManager.executeTransaction((clientRepo, orderRepo) ->

		{
			if (clientRepo.findById(client.getIdentifier()) == null) {
				throw new NotFoundClientException(String.format(CLIENT_MESSAGE_EXCEPTION, client.getIdentifier()));
			}
			return orderRepo.findOrdersByClientAndYear(client, year);
		});

	}

	@Override
	public Order addOrder(Order order) {
		Order orderInserted = mongoTransactionManager.executeTransaction((clientRepo, orderRepo) -> {

			logger.info("client of the order: {}", order.getClient());

			logger.info("Client ID of the client of the order: {}", order.getClient().getIdentifier());

			logger.info("result of findById() method of clientRepository for client of the order: {}",
					clientRepo.findById(order.getClient().getIdentifier()));

			if (clientRepo.findById(order.getClient().getIdentifier()) == null) {
				throw new NotFoundClientException(
						String.format(CLIENT_MESSAGE_EXCEPTION, order.getClient().getIdentifier()));
			}
			return orderRepo.save(order);
		});
		logger.info("Order Inserted by service: {}", orderInserted);
		return orderInserted;
	}

	@Override
	public Order removeOrder(Order order) {
		return mongoTransactionManager.executeTransaction((clientRepo, orderRepo) -> {
			if (clientRepo.findById(order.getClient().getIdentifier()) == null) {
				throw new NotFoundClientException(
						String.format(CLIENT_MESSAGE_EXCEPTION, order.getClient().getIdentifier()));
			}
			if (orderRepo.findById(order.getIdentifier()) == null) {
				throw new NotFoundOrderException(
						String.format("L'ordine con id %s non è presente nel database", order.getIdentifier()));
			}
			return orderRepo.delete(order.getIdentifier());
		});

	}

	@Override
	public List<Integer> findYearsOfOrders() {
		return mongoTransactionManager.executeTransaction((clientRepo, orderRepo) -> orderRepo.getYearsOfOrders());
	}

	@Override
	public Order updateOrder(Order orderToModify, Map<String, Object> updates) {
		return mongoTransactionManager.executeTransaction((clientRepo, orderRepo) -> {
			if (orderRepo.findById(orderToModify.getIdentifier()) == null) {
				throw new NotFoundOrderException(
						String.format("L'ordine con id %s non è presente nel database", orderToModify.getIdentifier()));
			}
			if (clientRepo.findById(orderToModify.getClient().getIdentifier()) == null) {
				throw new NotFoundClientException(
						String.format("Il cliente originale con id %s non è presente nel database",
								orderToModify.getClient().getIdentifier()));
			}
			logger.info("client of the order to modify: {}", orderToModify.getClient());
			logger.info("ID of the client of the order to modify: {}", orderToModify.getClient().getIdentifier());
			if (clientRepo.findById(((Client) updates.get("client")).getIdentifier()) == null) {
				throw new NotFoundClientException(
						String.format(CLIENT_MESSAGE_EXCEPTION, ((Client) updates.get("client")).getIdentifier()));
			}
			return orderRepo.updateOrder(orderToModify.getIdentifier(), updates);
		});
	}

	@Override
	public List<Order> allOrdersByClient(Client client) {
		return mongoTransactionManager.executeTransaction((clientRepo, orderRepo) -> {
			if (clientRepo.findById(client.getIdentifier()) == null) {
				throw new NotFoundClientException(
						String.format(CLIENT_MESSAGE_EXCEPTION, client.getIdentifier()));
			}
			return orderRepo.findOrdersByClient(client);
		});
	}
	
	@Override
	public List<Order> findAllOrders() {
		return mongoTransactionManager.executeTransaction((clientRepo, orderRepo) -> orderRepo.findAll());
	}

}
