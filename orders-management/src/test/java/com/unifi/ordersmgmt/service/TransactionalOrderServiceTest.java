package com.unifi.ordersmgmt.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.unifi.ordersmgmt.exception.NotFoundClientException;
import com.unifi.ordersmgmt.exception.NotFoundOrderException;
import com.unifi.ordersmgmt.model.Client;
import com.unifi.ordersmgmt.model.Order;
import com.unifi.ordersmgmt.repository.ClientRepository;
import com.unifi.ordersmgmt.repository.OrderRepository;
import com.unifi.ordersmgmt.transaction.TransactionManager;
import com.unifi.ordersmgmt.transaction.TransactionalFunction;

public class TransactionalOrderServiceTest {

	private TransactionalOrderService orderService;

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
		orderService = new TransactionalOrderService(mongoTransactionManager);
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
	public void testallOrdersByYear() {
		Client client = new Client("CLIENT-00001", "Client 1");
		Order order = new Order("ORDER-00001", client, new Date(), 100.0);

		when(orderRepo.findOrderByYear(2025)).thenReturn(asList(order));

		List<Order> orders = orderService.allOrdersByYear(2025);
		assertThat(orders).containsExactly(order);
		verify(orderRepo).findOrderByYear(2025);

	}

	@Test
	public void testGetyearsofOrders() {
		Client client = new Client("CLIENT-00001", "Client 1");
		Order order = new Order("ORDER-00001", client, new Date(), 100.0);

		Order order2 = new Order("ORDER-00002", client,
				Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 100.0);

		when(orderRepo.getYearsOfOrders())
				.thenReturn(asList(order.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().getYear(),
						order2.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().getYear()));

		List<Integer> years = orderService.findYearsOfOrders();

		verify(orderRepo).getYearsOfOrders();
		assertThat(years).containsExactly(2025, 2024);

	}

	@Test
	public void testfindallOrdersByClientByYearSuccess() {
		Client client = new Client("CLIENT-00001", "Client 1");
		Order order = new Order("ORDER-00001", client,
				Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 100.0);
		Order order2 = new Order("ORDER-00002", client,
				Date.from(LocalDate.of(2024, 2, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 50.0);

		when(orderRepo.findOrdersByClientAndYear(client, 2024)).thenReturn(asList(order, order2));
		when(clientRepo.findById(client.getIdentifier())).thenReturn(client);

		List<Order> years = orderService.findallOrdersByClientByYear(client, 2024);

		verify(orderRepo).findOrdersByClientAndYear(client, 2024);
		assertThat(years).containsExactly(order, order2);

	}

	@Test
	public void testfindallOrdersByClientByYearWhenClientNoExists() {
		Client client = new Client("CLIENT-00001", "Client 1");
		Order order = new Order("ORDER-00001", client,
				Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 100.0);
		Order order2 = new Order("ORDER-00002", client,
				Date.from(LocalDate.of(2024, 2, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 50.0);

		when(orderRepo.findOrdersByClientAndYear(client, 2024)).thenReturn(asList(order, order2));
		when(clientRepo.findById(client.getIdentifier())).thenReturn(null);

		assertThatExceptionOfType(NotFoundClientException.class)
				.isThrownBy(() -> orderService.findallOrdersByClientByYear(client, 2024));

		verify(orderRepo, never()).findOrdersByClientAndYear(client, 2024);

	}

	@Test
	public void testAddOrderShouldSaveOrderWhenClientExists() {
		Client client = new Client("CLIENT-00001", "Client 1");
		Order order = new Order("ORDER-00001", client, new Date(), 100.0);

		when(clientRepo.findById(client.getIdentifier())).thenReturn(client);
		when(orderRepo.save(order)).thenReturn(order);

		Order orderSaved = orderService.addOrder(order);

		assertThat(order).isEqualTo(orderSaved);
		assertThat(orderSaved).isNotNull();
		verify(clientRepo, times(2)).findById(client.getIdentifier());
		verify(orderRepo).save(order);
	}

	@Test
	public void testAddOrderShouldNotSaveOrderWhenClientNoExists() {
		Client clientNotExist = new Client("CLIENT-00001", "Client Not Exist");
		Order order = new Order("ORDER-00001", clientNotExist, new Date(), 100.0);

		when(clientRepo.findById(clientNotExist.getIdentifier())).thenReturn(null);
		when(orderRepo.save(order)).thenReturn(order);

		assertThatExceptionOfType(NotFoundClientException.class).isThrownBy(() -> orderService.addOrder(order));
		verify(clientRepo, times(2)).findById(clientNotExist.getIdentifier());
		verify(orderRepo, never()).save(any());
	}

	@Test
	public void testRemoveOrderShouldRemoveOrderWhenClientAndOrderExist() {
		Client client = new Client("CLIENT-00001", "Client 1");
		Order order = new Order("ORDER-00001", client, new Date(), 100.0);

		when(clientRepo.findById(client.getIdentifier())).thenReturn(client);
		when(orderRepo.findById(order.getIdentifier())).thenReturn(order);
		when(orderRepo.delete(order.getIdentifier())).thenReturn(order);

		orderService.removeOrder(order);

		verify(clientRepo).findById(client.getIdentifier());
		verify(orderRepo).delete(order.getIdentifier());
	}

	@Test
	public void testDeleteOrderShouldNotDeleteOrderWhenClientNoExists() {
		Client clientNotExist = new Client("CLIENT-00001", "Client Not Exist");
		Order order = new Order("ORDER-00001", clientNotExist, new Date(), 100.0);

		when(clientRepo.findById(clientNotExist.getIdentifier())).thenReturn(null);
		when(orderRepo.delete(order.getIdentifier())).thenReturn(order);

		assertThatExceptionOfType(NotFoundClientException.class).isThrownBy(() -> orderService.removeOrder(order));
		verify(clientRepo).findById(clientNotExist.getIdentifier());
		verify(orderRepo, never()).delete(any());
	}

	@Test
	public void testDeleteOrderShouldNotDeleteOrderWhenOrderNoExists() {
		Client client = new Client("CLIENT-00001", "Client Not Exist");
		Order orderNotExist = new Order("ORDER-00001", client, new Date(), 100.0);

		when(clientRepo.findById(client.getIdentifier())).thenReturn(client);
		when(orderRepo.findById(orderNotExist.getIdentifier())).thenReturn(null);
		when(orderRepo.delete(orderNotExist.getIdentifier())).thenReturn(null);

		assertThatExceptionOfType(NotFoundOrderException.class)
				.isThrownBy(() -> orderService.removeOrder(orderNotExist));
		verify(clientRepo).findById(client.getIdentifier());
		verify(orderRepo).findById(orderNotExist.getIdentifier());
		verify(orderRepo, never()).delete(any());
	}

	@Test
	public void testUpdateOrderShouldUpdateOrderWhenClientAndOrderExist() {
		Client client1 = new Client("CLIENT-00001", "Client 1");
		Order order = new Order("ORDER-00001", client1, new Date(), 100.0);
		Map<String, Object> updates = new HashMap<String, Object>();
		updates.put("client", client1);
		updates.put("price", 120.0);
		Order orderModified = new Order("ORDER-00001", client1, order.getDate(), 120.0);

		when(clientRepo.findById(client1.getIdentifier())).thenReturn(client1);
		when(orderRepo.findById(order.getIdentifier())).thenReturn(order);
		when(orderRepo.updateOrder(order.getIdentifier(), updates)).thenReturn(orderModified);

		Order orderUpdated = orderService.updateOrder(order, updates);

		verify(clientRepo, times(2)).findById(client1.getIdentifier());
		verify(orderRepo).updateOrder(order.getIdentifier(), updates);
		assertThat(orderUpdated).isEqualTo(orderModified);
	}

	@Test
	public void testUpdateOrderShouldNotUpdateOrderWhenClientNoExists() {
		Client clientNotExist = new Client("CLIENT-00001", "Client Not Exist");
		Client clientOriginal = new Client("CLIENT-00002", "client ");
		Order order = new Order("ORDER-00001", clientOriginal, new Date(), 100.0);
		Map<String, Object> updates = new HashMap<String, Object>();
		updates.put("client", clientNotExist);
		updates.put("price", 120.0);

		when(clientRepo.findById(clientOriginal.getIdentifier())).thenReturn(clientOriginal);
		when(clientRepo.findById(clientNotExist.getIdentifier())).thenReturn(null);
		when(orderRepo.findById(order.getIdentifier())).thenReturn(order);
		when(orderRepo.updateOrder(order.getIdentifier(), updates)).thenReturn(null);

		assertThatExceptionOfType(NotFoundClientException.class)
				.isThrownBy(() -> orderService.updateOrder(order, updates));
		verify(clientRepo).findById(clientNotExist.getIdentifier());
		verify(orderRepo, never()).updateOrder(any(), any());
	}

	@Test
	public void testUpdateOrderShouldNotUpdateOrderWhenOriginalClientNoExists() {
		Client clientForUpdate = new Client("CLIENT-00001", "client");
		Client clientOriginal = new Client("CLIENT-00002", "Client Not Exist");
		Order order = new Order("ORDER-00001", clientOriginal, new Date(), 100.0);
		Order orderWithoutClient = new Order("ORDER-00001", null, new Date(), 100.0);
		Map<String, Object> updates = new HashMap<String, Object>();
		updates.put("client", clientForUpdate);
		updates.put("price", 120.0);

		when(clientRepo.findById(order.getClient().getIdentifier())).thenReturn(null);
		when(clientRepo.findById(clientOriginal.getIdentifier())).thenReturn(null);
		when(clientRepo.findById(clientForUpdate.getIdentifier())).thenReturn(clientForUpdate);
		when(orderRepo.findById(order.getIdentifier())).thenReturn(orderWithoutClient);
		when(orderRepo.updateOrder(order.getIdentifier(), updates)).thenReturn(null);

		assertThatExceptionOfType(NotFoundClientException.class)
				.isThrownBy(() -> orderService.updateOrder(order, updates));
		verify(clientRepo).findById(clientOriginal.getIdentifier());
		verify(orderRepo, never()).updateOrder(any(), any());
	}

	@Test
	public void testupdateOrderShouldNotDeleteOrderWhenOrderNoExists() {
		Client client = new Client("CLIENT-00001", "Client 1");
		Client client2 = new Client("CLIENT-00002", "Client 2");
		Order orderNotExist = new Order("ORDER-00001", client, new Date(), 100.0);
		Map<String, Object> updates = new HashMap<String, Object>();
		updates.put("client", client2);
		updates.put("price", 120.0);

		when(orderRepo.findById(orderNotExist.getIdentifier())).thenReturn(null);
		when(orderRepo.updateOrder(orderNotExist.getIdentifier(), updates)).thenReturn(null);

		assertThatExceptionOfType(NotFoundOrderException.class)
				.isThrownBy(() -> orderService.updateOrder(orderNotExist, updates));

		verify(orderRepo).findById(orderNotExist.getIdentifier());
		verify(orderRepo, never()).updateOrder(any(), any());
	}

	@Test
	public void testallOrdersByClientSuccess() {
		Client client = new Client("CLIENT-00001", "Client 1");
		Client client2 = new Client("CLIENT-00002", "Client 2");
		Order order = new Order("ORDER-00001", client,
				Date.from(LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 100.0);
		Order order2 = new Order("ORDER-00002", client2,
				Date.from(LocalDate.of(2024, 2, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), 50.0);

		when(orderRepo.findOrdersByClient(client)).thenReturn(asList(order));
		when(clientRepo.findById(client.getIdentifier())).thenReturn(client);

		List<Order> years = orderService.allOrdersByClient(client);

		verify(orderRepo).findOrdersByClient(client);
		assertThat(years).containsExactly(order).doesNotContain(order2);

	}

	@Test
	public void testallOrdersByClientWhenClientNoExists() {
		Client client = new Client("CLIENT-00001", "Client 1");

		when(clientRepo.findById(client.getIdentifier())).thenReturn(null);

		assertThatExceptionOfType(NotFoundClientException.class)
				.isThrownBy(() -> orderService.allOrdersByClient(client));

		verify(orderRepo, never()).findOrdersByClient(client);

	}

}
