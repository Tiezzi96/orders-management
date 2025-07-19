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
		// TODO Auto-generated method stub
		List<Client> clients = mongoTransactionManager
				.executeTransaction((clientRepo, orderRepo) -> clientRepo.findAll());
		return clients;
	}

	@Override
	public Client saveClient(Client client) {
		// TODO Auto-generated method stub
		return mongoTransactionManager.executeTransaction((clientRepo, orderRepo) -> clientRepo.save(client));
	}

	@Override
	public void removeClient(Client c) {
		// TODO Auto-generated method stub
		mongoTransactionManager.executeTransaction((clientRepo, orderRepo) -> {
			if (clientRepo.findById(c.getIdentifier()) == null) {
				throw new NotFoundClientException(
						String.format("Il cliente con id %s non è presente nel database", c.getIdentifier()));
			} else {
				orderRepo.removeOrdersByClient(c.getIdentifier());
				clientRepo.delete(c.getIdentifier());
				return true;
			}
		});
	}
}
