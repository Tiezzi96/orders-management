package com.unifi.ordersmgmt.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.unifi.ordersmgmt.exception.NotFoundClientException;
import com.unifi.ordersmgmt.model.Client;
import com.unifi.ordersmgmt.model.Order;
import com.unifi.ordersmgmt.repository.ClientRepository;
import com.unifi.ordersmgmt.repository.OrderRepository;
import com.unifi.ordersmgmt.transaction.TransactionManager;
import com.unifi.ordersmgmt.transaction.TransactionalFunction;

public class TransactionalClientServiceTest {

	private TransactionalClientService clientService;
	@Mock
	private TransactionManager mongoTransactionManager;
	@Mock
	private ClientRepository clientRepo;
	@Mock
	private OrderRepository orderRepo;
	private AutoCloseable closeable;

	@Before
	public void setUp() {
		closeable = MockitoAnnotations.openMocks(this);
		clientService = new TransactionalClientService(mongoTransactionManager);
		when(mongoTransactionManager.executeTransaction(any())).thenAnswer(inv -> {
			TransactionalFunction<Order> callback = inv.getArgument(0);
			return callback.apply(clientRepo, orderRepo);
		});
	}

	@After
	public void tearDown() throws Exception {
		closeable.close();
	}

	@Test
	public void testFindClients() {
		// Arrange
		Client client = new Client("CLIENT-00001", "Client 1");
		Client client2 = new Client("CLIENT-00002", "Client 2");

		when(clientRepo.findAll()).thenReturn(asList(client, client2));

		// Act
		clientService.findAllClients();

		// Assert
		verify(clientRepo, times(1)).findAll();
	}

	@Test
	public void testSave() {
		// Arrange
		Client client = new Client("CLIENT-00001", "Client 1");

		when(clientRepo.save(client)).thenReturn(client);

		// Act
		clientService.saveClient(client);

		// Assert
		verify(clientRepo, times(1)).save(client);
	}

	@Test
	public void testRemoveClientShouldRemoveClientWhenClientExists() {
		// Arrange
		Client client = new Client("CLIENT-00001", "Client 1");

		when(clientRepo.findById(client.getIdentifier())).thenReturn(client);
		when(clientRepo.delete(client.getIdentifier())).thenReturn(client);

		// Act
		clientService.removeClient(client);

		// Assert
		verify(clientRepo).findById(client.getIdentifier());
		verify(clientRepo).delete(client.getIdentifier());
	}

	@Test
	public void testDeleteClientShouldNotDeleteCliemtWhenClientNoExists() {
		// Arrange
		Client clientNotExist = new Client("CLIENT-00001", "Client Not Exist");

		when(clientRepo.findById(clientNotExist.getIdentifier())).thenReturn(null);

		// Assert
		assertThatExceptionOfType(NotFoundClientException.class)
				.isThrownBy(() -> clientService.removeClient(clientNotExist));
		verify(clientRepo).findById(clientNotExist.getIdentifier());
		verify(clientRepo, never()).delete(any());
	}

}
