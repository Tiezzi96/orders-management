package com.unifi.ordersmgmt.repository;

import com.mongodb.client.ClientSession;
import com.unifi.ordersmgmt.model.Client;

public interface ClientRepository extends Repository<Client> {

	public ClientSession getSession();

}
