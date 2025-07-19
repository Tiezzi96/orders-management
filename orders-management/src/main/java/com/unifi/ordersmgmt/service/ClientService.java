package com.unifi.ordersmgmt.service;

import java.util.List;

import com.unifi.ordersmgmt.model.Client;


public interface ClientService {
	public List<Client> findAllClients();

	public Client saveClient(Client client);

	public void removeClient(Client c);
}
