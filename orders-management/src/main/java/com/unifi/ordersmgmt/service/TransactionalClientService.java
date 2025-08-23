package com.unifi.ordersmgmt.service;

import java.util.List;

import com.unifi.ordersmgmt.exception.NotFoundClientException;
import com.unifi.ordersmgmt.model.Client;
import com.unifi.ordersmgmt.transaction.TransactionManager;

public class TransactionalClientService implements ClientService {

	private TransactionManager mongoTransactionManager;

	public TransactionalClientService(TransactionManager mongoTransactionManager) {
		this.mongoTransactionManager = mongoTransactionManager;
	}

	@Override
	public List<Client> findAllClients() {
		return mongoTransactionManager
				.executeTransaction((clientRepo, orderRepo) -> clientRepo.findAll());
	}

	@Override
	public Client saveClient(Client client) {
		return mongoTransactionManager.executeTransaction((clientRepo, orderRepo) -> clientRepo.save(client));
	}

	@Override
	public Client removeClient(Client c) {
		return mongoTransactionManager.executeTransaction((clientRepo, orderRepo) -> {
			if (clientRepo.findById(c.getIdentifier()) == null) {
				throw new NotFoundClientException(
						String.format("Il cliente con id %s non è presente nel database", c.getIdentifier()));
			} else {
				orderRepo.removeOrdersByClient(c.getIdentifier());
				return clientRepo.delete(c.getIdentifier());
			}
		});
	}
}
