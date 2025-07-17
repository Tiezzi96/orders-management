package com.unifi.ordersmgmt.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.openMocks(this);
		orderService = new TransactionalOrderService(mongoTransactionManager);
		when(mongoTransactionManager.executeTransaction(any())).thenAnswer(inv -> {
			TransactionalFunction<Order> callback = inv.getArgument(0);
			return callback.apply(clientRepo, orderRepo);
		});
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testallOrdersByYear() {
		// Arrange
		Client client = new Client("CLIENT-00001", "Client 1");
		Order order = new Order("ORDER-00001", client, new Date(), 100.0);

		when(orderRepo.findOrderByYear(2025)).thenReturn(asList(order));

		List<Order> orders = orderService.allOrdersByYear(2025);
		assertThat(orders).containsExactly(order);
		verify(orderRepo).findOrderByYear(2025);

	}
	
	@Test
	public void testGetyearsofOrders() {
		// Arrange
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

}
