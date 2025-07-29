package com.unifi.ordersmgmt.view.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DocumentFilter;

import com.unifi.ordersmgmt.controller.OrderController;
import com.unifi.ordersmgmt.model.Client;
import com.unifi.ordersmgmt.model.Order;
import com.unifi.ordersmgmt.view.OrderView;

public class OrderSwingView extends JFrame implements OrderView {

	private static final String NO_YEAR_ITEM = "-- Nessun anno --";
	private JPanel contentPane;
	private String FONT_TEXT = "Segoe UI";
	private DefaultListModel<Client> clientListModel;
	private JList listClients;
	private JButton btnRemoveClient;
	private JTextPane paneClientError;
	private JLabel lblNewClient;
	private JTextField textFieldNewClient;
	private JLabel lblClientName;
	private JButton btnNewClient;
	private OrderController orderController;
	private JLabel lblrevenue;
	private DefaultComboBoxModel comboboxClientsModel;
	private JComboBox comboboxClients;
	private DefaultComboBoxModel comboboxYearsModel;
	private JComboBox comboboxYears;
	private JTable tableOrders;
	private OrderTableModel orderTableModel;
	private JTextPane panelOrderError;
	private JTextField textFieldDayNewOrder;
	private JTextField textFieldMonthNewOrder;
	private JTextField textFieldYearNewOrder;
	private JTextField textFieldRevenueNewOrder;
	private JButton btnNewOrder;

	public OrderSwingView() {
		// TODO Auto-generated constructor stub
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setPreferredSize(new Dimension(1000, 850));
		setBounds(100, 100, 1000, 850);
		setTitle("Order Management View");
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		// racchiude l'intero pannello cliente
		JPanel panel_clientManagement = new JPanel();
		panel_clientManagement.setBackground(new Color(1, 1, 1));
		panel_clientManagement.setBounds(0, 52, 1000, 304);
		contentPane.add(panel_clientManagement);
		panel_clientManagement.setLayout(null);

		JPanel panel_clientTitle = new JPanel();
		panel_clientTitle.setForeground(Color.WHITE);
		panel_clientTitle.setBounds(360, 0, 640, 58);
		panel_clientTitle.setBackground(new Color(24, 103, 64));

		panel_clientManagement.add(panel_clientTitle);
		panel_clientTitle.setLayout(null);

		JLabel lblClientsTitle = new JLabel("CLIENTI");
		lblClientsTitle.setFont(new Font(FONT_TEXT, Font.BOLD, 17));
		lblClientsTitle.setHorizontalAlignment(SwingConstants.CENTER);
		lblClientsTitle.setBounds(0, 0, 640, 58);
		lblClientsTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

		lblClientsTitle.setForeground(Color.WHITE);
		panel_clientTitle.add(lblClientsTitle);

		JScrollPane scrollPaneClientsList = new JScrollPane();
		scrollPaneClientsList.setBorder(null);
		scrollPaneClientsList.setBounds(360, 58, 640, 215);
		panel_clientManagement.add(scrollPaneClientsList);

		clientListModel = new DefaultListModel<>();
		// clientListModel.addElement(new Client("CLIENT-00001", "Marta").toString());
		// clientListModel.addElement(new Client("2", "Martino").toString());
		listClients = new JList<>(clientListModel);
		listClients.setFont(new Font(FONT_TEXT, Font.BOLD, 16));
		listClients.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		listClients.setBorder(null);
		listClients.setName("clientsList");
		listClients.setSelectionBackground(Color.LIGHT_GRAY);
		listClients.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listClients.setBackground(new Color(207, 234, 217)); // #CFEAD9
		listClients.setFixedCellHeight(35);
		scrollPaneClientsList.setViewportView(listClients);

		btnRemoveClient = new JButton("Rimuovi cliente");
		btnRemoveClient.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btnRemoveClient.setBorder(new SoftBevelBorder(BevelBorder.LOWERED, null, null, null, null));
		btnRemoveClient.setEnabled(false);
		btnRemoveClient.setFont(new Font(FONT_TEXT, Font.BOLD, 14));
		btnRemoveClient.setForeground(Color.WHITE);
		btnRemoveClient.setBounds(673, 274, 312, 29);
		panel_clientManagement.add(btnRemoveClient);

		paneClientError = new JTextPane();
		paneClientError.setText("");
		paneClientError.setEditable(false);
		paneClientError.setName("panelClientErrorMessage");
		paneClientError.setForeground(new Color(255, 102, 102));
		paneClientError.setBackground(new Color(15, 81, 50)); // #0F5132 verde scuro
		paneClientError.setBounds(10, 5, 340, 43);
		panel_clientManagement.add(paneClientError);

		JPanel panel_newClient = new JPanel();
		panel_newClient.setBounds(0, 58, 360, 243);
		panel_newClient.setBackground(new Color(15, 81, 50)); // #0F5132 verde scuro
		panel_clientManagement.add(panel_newClient);
		panel_newClient.setLayout(null);

		lblNewClient = new JLabel("NUOVO CLIENTE");
		lblNewClient.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewClient.setBounds(6, 18, 350, 17);
		lblNewClient.setFont(new Font(FONT_TEXT, Font.PLAIN, 18));
		lblNewClient.setForeground(Color.WHITE);
		panel_newClient.add(lblNewClient);

		// riga per inserire id
		JSeparator separator = new JSeparator();
		separator.setForeground(Color.WHITE);
		separator.setBounds(133, 78, 180, 17);
		panel_newClient.add(separator);

		textFieldNewClient = new JTextField();
		textFieldNewClient.setFont(new Font(FONT_TEXT, Font.BOLD, 13));
		textFieldNewClient.setHorizontalAlignment(SwingConstants.CENTER);
		textFieldNewClient.setBackground(Color.WHITE);
		textFieldNewClient.setName("textField_clientName");
		textFieldNewClient.setBorder(null);
		textFieldNewClient.setBounds(133, 60, 180, 16);
		textFieldNewClient.setBackground(new Color(15, 81, 50)); // #0F5132 verde scuro
		textFieldNewClient.setCaretColor(Color.WHITE);
		textFieldNewClient.setForeground(Color.WHITE);
		textFieldNewClient.setColumns(10);
		panel_newClient.add(textFieldNewClient);

		lblClientName = new JLabel("Identificativo");
		lblClientName.setHorizontalAlignment(SwingConstants.CENTER);
		lblClientName.setFont(new Font(FONT_TEXT, Font.PLAIN, 14));
		lblClientName.setForeground(Color.WHITE);
		lblClientName.setBounds(6, 60, 130, 25);
		panel_newClient.add(lblClientName);

		btnNewClient = new JButton("Aggiungi cliente");
		btnNewClient.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btnNewClient.setEnabled(false);
		btnNewClient.setBorder(new SoftBevelBorder(BevelBorder.LOWERED, null, null, null, null));
		btnNewClient.setForeground(Color.WHITE);
		btnNewClient.setBounds(12, 98, 286, 29);
		btnNewClient.setFont(new Font(FONT_TEXT, Font.BOLD, 14));
		btnNewClient.setBounds(360, 274, 312, 29);
		btnNewClient.setName("btnAddClient");
		panel_clientManagement.add(btnNewClient);

		JPanel panel_orderManagement = new JPanel();
		panel_orderManagement.setBackground(Color.WHITE);
		panel_orderManagement.setBounds(0, 360, 1000, 646);
		contentPane.add(panel_orderManagement);
		panel_orderManagement.setLayout(null);

		JPanel panel_revenueLabel = new JPanel();
		panel_revenueLabel.setBorder(null);
		panel_revenueLabel.setBounds(0, 5, 360, 88);
		panel_revenueLabel.setBackground(new Color(245, 245, 245));
		panel_orderManagement.add(panel_revenueLabel);
		panel_revenueLabel.setLayout(null);

		lblrevenue = new JLabel();
		lblrevenue.setText(" ");
		lblrevenue.setFont(new Font(FONT_TEXT, Font.PLAIN, 14));
		lblrevenue.setBounds(2, 0, 355, 88);
		lblrevenue.setForeground(new Color(89, 89, 89));
		lblrevenue.setHorizontalAlignment(SwingConstants.CENTER);
		lblrevenue.setName("revenueLabel");
		panel_revenueLabel.add(lblrevenue);

		JPanel panel_orderViewAndAdd = new JPanel();
		panel_orderViewAndAdd.setBackground(new Color(207, 234, 217)); // #CFEAD9
		panel_orderViewAndAdd.setBounds(0, 95, 360, 355);
		panel_orderManagement.add(panel_orderViewAndAdd);
		panel_orderViewAndAdd.setLayout(null);

		JPanel panel_orderView = new JPanel();
		panel_orderView.setBackground(Color.WHITE);
		panel_orderView.setBounds(360, 0, 625, 450);
		panel_orderManagement.add(panel_orderView);
		panel_orderView.setLayout(null);

		comboboxYearsModel = new DefaultComboBoxModel<>();
		comboboxYears = new JComboBox<>(comboboxYearsModel);
		comboboxYears.setBackground(Color.WHITE);
		comboboxYears.setBounds(523, 20, 101, 27);
		panel_orderView.add(comboboxYears);
		comboboxYears.setFont(new Font(FONT_TEXT, Font.BOLD, 16));
		comboboxYears.setName("yearsCombobox");
		comboboxYears.setBorder(null);
		comboboxYears.setBackground(new Color(245, 245, 245));

		JScrollPane scrollPanelOrdersList = new JScrollPane();

		scrollPanelOrdersList.setBackground(new Color(207, 234, 217)); // #CFEAD9
		scrollPanelOrdersList.getViewport().setBackground(new Color(207, 234, 217));

		scrollPanelOrdersList.setBounds(3, 48, 620, 350);
		scrollPanelOrdersList.setFont(new Font(FONT_TEXT, Font.PLAIN, 14));
		orderTableModel = new OrderTableModel();

		// orderTableModel = new DefaultTableModel();

		tableOrders = new TableOrder(orderTableModel);
		// orderTableModel.addOrder(new Order("ORDER-00001", new Client("CLIENT-00001",
		// "Marta"), new Date(), 10.0));

		// tableOrders.setBackground(Color.YELLOW);

		tableOrders.setBorder(new EmptyBorder(0, 0, 0, 0));
		tableOrders.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tableOrders.setFont(new Font(FONT_TEXT, Font.PLAIN, 13));
		tableOrders.setName("OrdersTable");
		scrollPanelOrdersList.setViewportView(tableOrders);

		panel_orderView.add(scrollPanelOrdersList);

		JLabel lblNewOrder = new JLabel("INFO ORDINE");
		lblNewOrder.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewOrder.setFont(new Font(FONT_TEXT, Font.PLAIN, 14));
		lblNewOrder.setBounds(6, 6, 350, 22);
		panel_orderViewAndAdd.add(lblNewOrder);

		JLabel lblClientNewOrder = new JLabel("Cliente");
		lblClientNewOrder.setFont(new Font(FONT_TEXT, Font.PLAIN, 14));
		lblClientNewOrder.setBounds(33, 47, 84, 16);
		panel_orderViewAndAdd.add(lblClientNewOrder);
		// combobox selezione clienti
		comboboxClientsModel = new DefaultComboBoxModel<>();
		comboboxClients = new JComboBox<>(comboboxClientsModel);
		comboboxClients.setName("comboboxClients");
		comboboxClients.setBounds(99, 43, 193, 27);
		panel_orderViewAndAdd.add(comboboxClients);

		JLabel lblDateNewOrder = new JLabel("Data");
		lblDateNewOrder.setFont(new Font(FONT_TEXT, Font.PLAIN, 14));
		lblDateNewOrder.setBounds(33, 91, 84, 16);
		panel_orderViewAndAdd.add(lblDateNewOrder);

		JSeparator separator_1 = new JSeparator();
		separator_1.setForeground(Color.BLACK);
		separator_1.setBounds(124, 103, 33, 12);
		panel_orderViewAndAdd.add(separator_1);

		JLabel lblSlashDate1NewOrder = new JLabel("/");
		lblSlashDate1NewOrder.setFont(new Font(FONT_TEXT, Font.PLAIN, 16));
		lblSlashDate1NewOrder.setBounds(161, 87, 8, 16);
		panel_orderViewAndAdd.add(lblSlashDate1NewOrder);

		JSeparator separator_1_1 = new JSeparator();
		separator_1_1.setForeground(Color.BLACK);
		separator_1_1.setBounds(167, 103, 33, 12);
		panel_orderViewAndAdd.add(separator_1_1);

		JLabel lblSlashDate2NewOrder = new JLabel("/");
		lblSlashDate2NewOrder.setFont(new Font(FONT_TEXT, Font.PLAIN, 16));
		lblSlashDate2NewOrder.setBounds(203, 87, 8, 16);
		panel_orderViewAndAdd.add(lblSlashDate2NewOrder);

		JSeparator separator_1_1_1 = new JSeparator();
		separator_1_1_1.setForeground(Color.BLACK);
		separator_1_1_1.setBounds(211, 103, 57, 12);
		panel_orderViewAndAdd.add(separator_1_1_1);

		textFieldDayNewOrder = new JTextField();
		textFieldDayNewOrder.setHorizontalAlignment(SwingConstants.CENTER);
		textFieldDayNewOrder.setBorder(null);
		textFieldDayNewOrder.setName("textField_dayOfDateOrder");
		textFieldDayNewOrder.setBounds(125, 82, 31, 22);
		textFieldDayNewOrder.setBackground(new Color(245, 245, 245));
		panel_orderViewAndAdd.add(textFieldDayNewOrder);
		textFieldDayNewOrder.setColumns(10);

		textFieldMonthNewOrder = new JTextField();
		textFieldMonthNewOrder.setHorizontalAlignment(SwingConstants.CENTER);
		textFieldMonthNewOrder.setBorder(null);
		textFieldMonthNewOrder.setName("textField_monthOfDateOrder");
		textFieldMonthNewOrder.setBounds(169, 82, 31, 22);
		textFieldMonthNewOrder.setBackground(new Color(245, 245, 245));
		panel_orderViewAndAdd.add(textFieldMonthNewOrder);
		textFieldMonthNewOrder.setColumns(10);

		textFieldYearNewOrder = new JTextField();
		textFieldYearNewOrder.setHorizontalAlignment(SwingConstants.CENTER);
		textFieldYearNewOrder.setName("textField_yearOfDateOrder");
		textFieldYearNewOrder.setColumns(10);
		textFieldYearNewOrder.setBorder(null);
		textFieldYearNewOrder.setBounds(212, 82, 56, 22);
		textFieldYearNewOrder.setBackground(new Color(245, 245, 245));
		panel_orderViewAndAdd.add(textFieldYearNewOrder);

		JLabel lblRevenueNewOrder = new JLabel("Importo");
		lblRevenueNewOrder.setFont(new Font(FONT_TEXT, Font.PLAIN, 14));
		lblRevenueNewOrder.setBounds(33, 129, 84, 16);
		panel_orderViewAndAdd.add(lblRevenueNewOrder);

		JSeparator separator_1_1_1_1 = new JSeparator();
		separator_1_1_1_1.setForeground(new Color(0, 0, 0));
		separator_1_1_1_1.setBounds(118, 140, 150, 12);
		panel_orderViewAndAdd.add(separator_1_1_1_1);

		textFieldRevenueNewOrder = new JTextField();
		textFieldRevenueNewOrder.setHorizontalAlignment(SwingConstants.CENTER);
		textFieldRevenueNewOrder.setName("textField_revenueOrder");
		textFieldRevenueNewOrder.setColumns(10);
		textFieldRevenueNewOrder.setBorder(null);
		textFieldRevenueNewOrder.setBounds(118, 118, 150, 22);
		textFieldRevenueNewOrder.setBackground(new Color(245, 245, 245));
		panel_orderViewAndAdd.add(textFieldRevenueNewOrder);
		
		btnNewOrder = new JButton("Aggiungi ordine");
		btnNewOrder.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btnNewOrder.setEnabled(false);
		btnNewOrder.setBorder(new LineBorder(new Color(23, 35, 51), 1, true));
		btnNewOrder.setFont(new Font(FONT_TEXT, Font.BOLD, 14));
		btnNewOrder.setBounds(50, 200, 250, 50);
		panel_orderViewAndAdd.add(btnNewOrder);
		
		panelOrderError = new JTextPane();
		panelOrderError.setText("");
		panelOrderError.setEditable(false);
		panelOrderError.setForeground(new Color(255, 51, 51));
		panelOrderError.setName("panelOrderErrorMessage");
		panelOrderError.setBounds(10, 255, 340, 93);
		panel_orderViewAndAdd.add(panelOrderError);

		comboboxYears.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub

				if (comboboxYears.getSelectedIndex() != -1) {
					System.out.println("anno selezionato");
					Integer actionSelected = (Integer) comboboxYears.getSelectedItem();
					Client selectedValue = (Client) listClients.getSelectedValue();
					System.out.println(selectedValue);
					if (selectedValue != null) {
						orderController.findOrdersByYearAndClient(selectedValue, actionSelected.intValue());
					} else {
						orderController.allOrdersByYear(actionSelected.intValue());
					}
				}

			}
		});
		
		KeyAdapter btnNewOrderEnabler = new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				// TODO Auto-generated method stub
				super.keyReleased(e);
				checkCompleteNewOrderInfo();
			}
		};

		comboboxClients.addActionListener(e -> {
			checkCompleteNewOrderInfo();
		});
		textFieldDayNewOrder.addKeyListener(btnNewOrderEnabler);
		textFieldMonthNewOrder.addKeyListener(btnNewOrderEnabler);
		textFieldYearNewOrder.addKeyListener(btnNewOrderEnabler);
		textFieldRevenueNewOrder.addKeyListener(btnNewOrderEnabler);

		((AbstractDocument) textFieldDayNewOrder.getDocument()).setDocumentFilter(createTextFilter(2, "\\d*", () -> {checkCompleteNewOrderInfo();

		}, " "));
		((AbstractDocument) textFieldMonthNewOrder.getDocument()).setDocumentFilter(createTextFilter(2, "\\d*", () -> {checkCompleteNewOrderInfo();
		}, " "));
		((AbstractDocument) textFieldYearNewOrder.getDocument()).setDocumentFilter(createTextFilter(4, "\\d*", () -> {checkCompleteNewOrderInfo();
		}, " "));
		((AbstractDocument) textFieldRevenueNewOrder.getDocument())
				.setDocumentFilter(createTextFilter(10, "^\\d*(\\.\\d{0,2})?$", () -> {checkCompleteNewOrderInfo();
				}, " "));
		((AbstractDocument) textFieldNewClient.getDocument())
				.setDocumentFilter(createTextFilter(20, "[\\s\\S]*", () -> {
					if (!textFieldNewClient.getText().trim().isEmpty()) {
						btnNewClient.setEnabled(true);
					} else {
						btnNewClient.setEnabled(false);
					}

				}, ""));

		listClients.addListSelectionListener(e -> {
			if (listClients.getSelectedIndex() != -1) {
				btnRemoveClient.setEnabled(true);
			} else {
				btnRemoveClient.setEnabled(false);

			}
		});

		btnNewClient.addActionListener(e -> {
			orderController.addClient(new Client(textFieldNewClient.getText()));
			textFieldNewClient.setText("");
			paneClientError.setText("");
		});

		btnRemoveClient.addActionListener(e -> {
			orderController.deleteClient((Client) listClients.getSelectedValue());
		});

	}

	private DocumentFilter createTextFilter(int maxLength, String regex, Runnable onChange, String spaces) {
		// TODO Auto-generated method stub
		return new TextDocumentFilter(maxLength, regex, onChange, spaces);
	}
	
	private void checkCompleteNewOrderInfo() {
		if ((!textFieldDayNewOrder.getText().trim().isEmpty()) && (!textFieldMonthNewOrder.getText().trim().isEmpty())
				&& (!textFieldYearNewOrder.getText().trim().isEmpty())
				&& (!textFieldRevenueNewOrder.getText().trim().isEmpty())
				&& (comboboxClients.getSelectedIndex() != -1)) {

			btnNewOrder.setEnabled(true);
		} else {
			btnNewOrder.setEnabled(false);
		}
	}

	@Override
	public void showAllClients(List<Client> clients) {
		clientListModel.removeAllElements();
		comboboxClients.removeAllItems();
		for (Client client : clients) {
			clientListModel.addElement(client);
		}
		for (Client client : clients) {
			comboboxClients.addItem(client);
		}

	}

	@Override
	public void setYearsOrders(List<Integer> yearsOfOrders) {
		// TODO Auto-generated method stub
		comboboxYearsModel.removeAllElements();
		Collections.sort(yearsOfOrders, Collections.reverseOrder());
		System.out.println("yearsOfTheOrders: " + yearsOfOrders);
		if (!yearsOfOrders.contains(2025)) {
			comboboxYearsModel.addElement(2025);
		}
		for (Integer integer : yearsOfOrders) {
			comboboxYearsModel.addElement(integer);
		}

		comboboxYearsModel.addElement(NO_YEAR_ITEM);

	}

	@Override
	public void showAllOrders(List<Order> orders) {
		// TODO Auto-generated method stub
		getOrderTableModel().removedAllOrders();
		for (Order order : orders) {
			orderTableModel.addOrder(order);
		}

	}

	@Override
	public void showErrorClient(String message, Client client) {
		paneClientError.setText(message + ": " + client.toString());

	}

	@Override
	public void clientRemoved(Client clientRemoved) {
		clientListModel.removeElement(clientRemoved);
		comboboxClients.removeItem(clientRemoved);
		System.out.println("client to remove: " + clientRemoved);
		System.out.println("comboboxclients size: " + comboboxClients.getItemCount());
		listClients.clearSelection();

	}

	@Override
	public void clientAdded(Client clientAdded) {
		// TODO Auto-generated method stub
		clientListModel.addElement(clientAdded);
		// mantiene la selezione vecchia del comboboxclient dopo l'inserimento
		Object selectedItem = comboboxClientsModel.getSelectedItem();
		comboboxClientsModel.addElement(clientAdded);
		comboboxClientsModel.setSelectedItem(selectedItem);

		// Ordinare la lista e la combo box per ID dopo ogni aggiunta
		sortClientsById();
		textFieldNewClient.setText("");
		paneClientError.setText("");

	}

	private void sortClientsById() {
		// TODO Auto-generated method stub
		Client selectedValue = null;
		Client selectedItemValue = null;

		System.out.println("listClients.getSelectedValue(): " + listClients.getSelectedValue());
		System.out.println("comboboxClients.getSelectedItem(): " + comboboxClients.getSelectedItem());

		if (listClients.getSelectedValue() != null) {
			selectedValue = (Client) listClients.getSelectedValue();
		}
		if (comboboxClients.getSelectedItem() != null) {
			selectedItemValue = (Client) comboboxClients.getSelectedItem();
		}
		List<Client> sortedClients = new ArrayList<>();

		for (int i = 0; i < clientListModel.getSize(); i++) {
			sortedClients.add(clientListModel.getElementAt(i));
		}

		// Ordina per ID numerico
		sortedClients.sort(Comparator.comparing(Client::getIdentifier));

		// Aggiorna la lista e la combo box con i clienti ordinati
		clientListModel.clear();
		comboboxClientsModel.removeAllElements();

		for (Client client : sortedClients) {
			clientListModel.addElement(client);
			comboboxClientsModel.addElement(client);
		}
		if (selectedValue != null) {
			listClients.setSelectedValue(selectedValue, true);

		}
		if (selectedItemValue != null) {
			comboboxClients.setSelectedItem(selectedItemValue);
		}

	}

	@Override
	public void orderAdded(Order orderAdded) {
		// TODO Auto-generated method stub
		Order orderSelected = getOrderTableModel().getOrderAt(tableOrders.getSelectedRow());
		int yearOfOrder = orderAdded.getDate().toInstant().atZone(ZoneId.systemDefault()).getYear();

		Object selectedItem = getComboboxYearsModel().getSelectedItem();
		System.out.println(":" + selectedItem);
		int yearSelected = -1;
		if (selectedItem != null) {
			yearSelected = (Integer) selectedItem;
			System.out.println("orderselected: " + orderSelected);
			System.out.println("yearsoFOrder: " + yearOfOrder);
			if (getComboboxYearsModel().getIndexOf(yearOfOrder) == -1) {
				List<Integer> years = getAllElements(getComboboxYearsModel());
				years.add(yearOfOrder);
				System.out.println("years:" + years);
				years.remove(Pattern.compile(NO_YEAR_ITEM));
				Collections.sort(years);
				Collections.reverse(years);
				System.out.println(years);
				getComboboxYearsModel().removeAllElements();
				for (Integer integer : years) {
					getComboboxYearsModel().addElement(integer);
				}
				getComboboxYearsModel().setSelectedItem(yearSelected);
			}

			if (yearSelected == yearOfOrder) {
				getOrderTableModel().addOrder(orderAdded);
				System.out.println("orderAdded function: orders: " + getOrderTableModel().getOrders());
				// usa la deepCopy, si puo anche fare altro
				List<Order> ordersList = new ArrayList<Order>(getOrderTableModel().getOrders());
				showAllOrders(ordersList);

			}
			if (orderSelected != null) {
				System.out.println("order select index");
				int orderSelectedIndex = getOrderTableModel().getOrderIndex(orderSelected);
				tableOrders.setRowSelectionInterval(orderSelectedIndex, orderSelectedIndex);
			}

		}
		// reset error label
		panelOrderError.setText("");

	}

	private static <T> List<T> getAllElements(DefaultComboBoxModel<T> comboboxModel) {
		// TODO Auto-generated method stub
		List<T> items = new ArrayList<>();
		for (int iter = 0; iter < comboboxModel.getSize(); iter++) {
			items.add(comboboxModel.getElementAt(iter));
		}
		return items;
	}

	@Override
	public void removeOrdersByClient(Client client) {
		// TODO Auto-generated method stub

	}

	@Override
	public void showOrderError(String message, Order order) {
		// TODO Auto-generated method stub

	}

	@Override
	public void orderRemoved(Order orderRemoved) {
		// TODO Auto-generated method stub

	}

	@Override
	public void orderUpdated(Order orderModified) {
		// TODO Auto-generated method stub

	}

	public void setOrderController(OrderController controller) {
		// TODO Auto-generated method stub
		this.orderController = controller;
	}

	public DefaultListModel<Client> getClientListModel() {
		return clientListModel;
	}

	public DefaultComboBoxModel<Client> getComboboxClientsModel() {
		return comboboxClientsModel;
	}

	public DefaultComboBoxModel<Integer> getComboboxYearsModel() {
		// TODO Auto-generated method stub
		return comboboxYearsModel;
	}

	public OrderTableModel getOrderTableModel() {
		// TODO Auto-generated method stub
		return orderTableModel;
	}

	public JTable getOrderTable() {
		// TODO Auto-generated method stub
		return tableOrders;
	}

}
