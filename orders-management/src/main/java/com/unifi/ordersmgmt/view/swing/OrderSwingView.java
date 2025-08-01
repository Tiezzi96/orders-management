package com.unifi.ordersmgmt.view.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DocumentFilter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.unifi.ordersmgmt.controller.OrderController;
import com.unifi.ordersmgmt.model.Client;
import com.unifi.ordersmgmt.model.Order;
import com.unifi.ordersmgmt.view.OrderView;

public class OrderSwingView extends JFrame implements OrderView {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String NO_YEAR_ITEM = "-- Nessun anno --";
	private static final Logger logger = LogManager.getLogger(OrderSwingView.class);
	private JPanel contentPane;
	private String FONT_TEXT = "Segoe UI";
	private DefaultListModel<Client> clientListModel;
	private JList<Client> listClients;
	private JButton btnRemoveClient;
	private JTextPane paneClientError;
	private JLabel lblNewClient;
	private JTextField textFieldNewClient;
	private JLabel lblClientName;
	private JButton btnNewClient;
	private transient OrderController orderController;
	private JLabel lblrevenue;
	private DefaultComboBoxModel<Client> comboboxClientsModel;
	private JComboBox<Client> comboboxClients;
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
	private JButton btnModifyOrder;
	private JButton btnRemoveOrder;
	private JButton btnShowAllClientsOrders;

	public OrderSwingView() {
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
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

		comboboxYearsModel = new DefaultComboBoxModel<Object>();
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
		
		JLabel lblRevenueNewOrder_1 = new JLabel("€");
		lblRevenueNewOrder_1.setFont(new Font(FONT_TEXT, Font.PLAIN, 14));
		lblRevenueNewOrder_1.setBounds(270, 124, 15, 16);
		panel_orderViewAndAdd.add(lblRevenueNewOrder_1);

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

		btnModifyOrder = new JButton();
		btnModifyOrder.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btnModifyOrder.setHorizontalTextPosition(SwingConstants.CENTER);
		btnModifyOrder.setText("<html><center>Modifica<br>ordine</center></html>");
		btnModifyOrder.setBounds(2, 400, 200, 45);
		panel_orderView.add(btnModifyOrder);
		btnModifyOrder.setEnabled(false);
		btnModifyOrder.setFont(new Font(FONT_TEXT, Font.BOLD, 14));

		btnRemoveOrder = new JButton();
		btnRemoveOrder.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btnRemoveOrder.setHorizontalTextPosition(SwingConstants.CENTER);
		btnRemoveOrder.setText("<html><center>Rimuovi<br>ordine</center></html>");
		btnRemoveOrder.setBounds(2, 2, 200, 45);
		btnRemoveOrder.setEnabled(false);
		panel_orderView.add(btnRemoveOrder);
		btnRemoveOrder.setFont(new Font(FONT_TEXT, Font.BOLD, 14));

		btnShowAllClientsOrders = new JButton("<html><center>Visualizza ordini<br>di tutti i clienti</center></html>");
		btnShowAllClientsOrders.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btnShowAllClientsOrders.setVisible(false);
		btnShowAllClientsOrders.setHorizontalTextPosition(SwingConstants.CENTER);
		btnShowAllClientsOrders.setBounds(204, 2, 200, 45);
		btnShowAllClientsOrders.setFont(new Font(FONT_TEXT, Font.BOLD, 14));
		panel_orderView.add(btnShowAllClientsOrders);
		
		JPanel panel_headBar = new JPanel();
		panel_headBar.setBackground(new Color(15, 81, 50)); // #0F5132 verde scuro
		panel_headBar.setBounds(0, 0, 1000, 53);
		contentPane.add(panel_headBar);

		comboboxYears.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub

				if (comboboxYears.getSelectedIndex() != -1 && comboboxYears.getSelectedItem().equals(NO_YEAR_ITEM)) {
					comboboxYears.setSelectedIndex(-1);
					Client selectedValue = (Client) listClients.getSelectedValue();
					if (selectedValue != null) {
						orderController.allOrdersByClient(selectedValue);
						logger.info("selected index on combobobox Years: {}" + comboboxYears.getSelectedIndex());
					}else {
						// bisogna mostrare tutti i clienti
						logger.info("mostra tutti gli ordini dei client di tutti gli anni");
						orderController.getAllOrders();
					}
				}
				if (comboboxYears.getSelectedIndex() != -1) {
					logger.info("anno selezionato");
					Integer actionSelected = (Integer) comboboxYears.getSelectedItem();
					Client selectedValue = (Client) listClients.getSelectedValue();
					logger.info(selectedValue);
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
				super.keyReleased(e);
				checkCompleteNewOrderInfo();
			}
		};

		comboboxClients.addActionListener(e -> {
			checkCompleteNewOrderInfo();
		});

		comboboxYears.addItemListener(e -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				panelOrderError.setText("");
				paneClientError.setText("");
			}
		});
		textFieldDayNewOrder.addKeyListener(btnNewOrderEnabler);
		textFieldMonthNewOrder.addKeyListener(btnNewOrderEnabler);
		textFieldYearNewOrder.addKeyListener(btnNewOrderEnabler);
		textFieldRevenueNewOrder.addKeyListener(btnNewOrderEnabler);
		btnNewOrder.addActionListener(e -> {
			logger.info("new Order");
			newOrder();
		});

		((AbstractDocument) textFieldDayNewOrder.getDocument()).setDocumentFilter(createTextFilter(2, "\\d*", () -> {
			checkCompleteNewOrderInfo();

		}, " "));
		((AbstractDocument) textFieldMonthNewOrder.getDocument()).setDocumentFilter(createTextFilter(2, "\\d*", () -> {
			checkCompleteNewOrderInfo();
		}, " "));
		((AbstractDocument) textFieldYearNewOrder.getDocument()).setDocumentFilter(createTextFilter(4, "\\d*", () -> {
			checkCompleteNewOrderInfo();
		}, " "));
		((AbstractDocument) textFieldRevenueNewOrder.getDocument())
				.setDocumentFilter(createTextFilter(10, "^\\d*(\\.\\d{0,2})?$", () -> {
					checkCompleteNewOrderInfo();
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
				btnShowAllClientsOrders.setVisible(true);
				btnRemoveClient.setEnabled(true);
				panelOrderError.setText("");
			} else {
				btnShowAllClientsOrders.setVisible(false);
				btnRemoveClient.setEnabled(false);

			}
			if (!e.getValueIsAdjusting() && comboboxYears.getSelectedIndex() != -1) {
				Integer yearSelected = (Integer) comboboxYears.getSelectedItem();
				Client clientSelected = (Client) listClients.getSelectedValue();
				logger.info("yearselected: {}", yearSelected);
				if (clientSelected != null) {
					orderController.findOrdersByYearAndClient(clientSelected, yearSelected);
				} else {
					orderController.allOrdersByYear(yearSelected);
				}
			}
			if (!e.getValueIsAdjusting() && comboboxYears.getSelectedIndex() == -1) {
				Client clientSelected = (Client) listClients.getSelectedValue();
				if (clientSelected != null) {
					logger.info("cliente selezionato, nessun anno selezionato: {}", clientSelected);
					orderController.allOrdersByClient(clientSelected);
				}
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

		KeyAdapter btnModifyOrderEnabler = new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				// TODO Auto-generated method stub
				super.keyReleased(e);

				checkCompleteModifyOrderInfo();

			}
		};

		comboboxClients.addActionListener(e -> {
			checkCompleteModifyOrderInfo();
		});
		textFieldDayNewOrder.addKeyListener(btnModifyOrderEnabler);
		textFieldMonthNewOrder.addKeyListener(btnModifyOrderEnabler);
		textFieldYearNewOrder.addKeyListener(btnModifyOrderEnabler);
		textFieldRevenueNewOrder.addKeyListener(btnModifyOrderEnabler);
		tableOrders.addKeyListener(btnModifyOrderEnabler);

		tableOrders.getSelectionModel().addListSelectionListener(e -> {
			if (tableOrders.getSelectedRow() != -1) {
				Order order = orderTableModel.getOrderAt(tableOrders.getSelectedRow());
				logger.info("row of table order selected: {}", tableOrders.getSelectedRow());
				LocalDate localDate = order.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
				comboboxClients.setSelectedItem(order.getClient());
				textFieldDayNewOrder.setText(String.valueOf(localDate.getDayOfMonth()));
				textFieldMonthNewOrder.setText(String.valueOf(localDate.getMonthValue()));
				textFieldYearNewOrder.setText(String.valueOf(localDate.getYear()));
				textFieldRevenueNewOrder.setText(String.valueOf(order.getPrice()));
				btnModifyOrder.setEnabled(true);
				btnRemoveOrder.setEnabled(true);

			} else {
				comboboxClients.setSelectedIndex(-1);
				textFieldDayNewOrder.setText("");
				textFieldMonthNewOrder.setText("");
				textFieldYearNewOrder.setText("");
				textFieldRevenueNewOrder.setText("");
				btnModifyOrder.setEnabled(false);
				btnRemoveOrder.setEnabled(false);

			}
			checkCompleteModifyOrderInfo();
		});

		btnModifyOrder.addActionListener(e -> {
			logger.info("Update order");
			updateOrder();
		});

		btnRemoveOrder.addActionListener(e -> {
			logger.info("remove order");
			removeOrder();
		});

		btnShowAllClientsOrders.addActionListener(e -> {
			btnShowAllClientsOrders.setVisible(false);
			listClients.clearSelection();
		});
	}

	private void removeOrder() {
		// TODO Auto-generated method stub
		orderController.deleteOrder(getOrderTableModel().getOrderAt(tableOrders.getSelectedRow()));
		tableOrders.clearSelection();
		textFieldDayNewOrder.setText("");
		textFieldMonthNewOrder.setText("");
		textFieldYearNewOrder.setText("");
		textFieldRevenueNewOrder.setText("");

	}

	private void updateOrder() {
		// TODO Auto-generated method stub
		Order orderToModify = orderTableModel.getOrderAt(tableOrders.getSelectedRow());
		Map<String, Object> updates = new HashMap<>();
		updates.put("client", comboboxClientsModel.getElementAt(comboboxClients.getSelectedIndex()));
		int day = 0;
		int month = 0;
		int year = 0;

		day = Integer.valueOf(textFieldDayNewOrder.getText());

		month = Integer.valueOf(textFieldMonthNewOrder.getText());

		year = Integer.valueOf(textFieldYearNewOrder.getText());

		LocalDateTime localDate = LocalDateTime.of(1900, 1, 1, 0, 0);
		if (isValidDate(day, month, year)) {

			Date newDate = Date.from(LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault()).toInstant());
			updates.put("date", newDate);
			updates.put("client", comboboxClientsModel.getElementAt(comboboxClients.getSelectedIndex()));
			updates.put("price", Double.valueOf(textFieldRevenueNewOrder.getText()));
			panelOrderError.setText("");
			orderController.modifyOrder(orderToModify, updates);
			tableOrders.clearSelection();
		} else {
			panelOrderError.setText("La data " + day + "/" + month + "/" + year + " non è corretta");
			textFieldDayNewOrder.setText("");
			textFieldMonthNewOrder.setText("");
			textFieldYearNewOrder.setText("");

		}

	}

	private void checkCompleteModifyOrderInfo() {
		if (((!textFieldDayNewOrder.getText().trim().isEmpty()) && (!textFieldMonthNewOrder.getText().trim().isEmpty())
				&& (!textFieldYearNewOrder.getText().trim().isEmpty()))
				&& (!textFieldRevenueNewOrder.getText().trim().isEmpty()) && (comboboxClients.getSelectedIndex() != -1)
				&& tableOrders.getSelectedRow() != -1) {

			logger.info("Data for modify order are completed");
			btnModifyOrder.setEnabled(true);
		} else {
			btnModifyOrder.setEnabled(false);
		}
	}

	private void newOrder() {
		int day = Integer.parseInt(textFieldDayNewOrder.getText());

		int month = Integer.parseInt(textFieldMonthNewOrder.getText());

		int year = Integer.parseInt(textFieldYearNewOrder.getText());

		Client client = (Client) getComboboxClientsModel().getSelectedItem();

		double price = Double.parseDouble(textFieldRevenueNewOrder.getText().replace(",", "."));

		if (isValidDate(day, month, year)) {
			LocalDateTime localDate = LocalDateTime.of(year, month, day, 0, 0);
			orderController.addOrder(
					new Order("", client, Date.from(localDate.atZone(ZoneId.systemDefault()).toInstant()), price));
			panelOrderError.setText("");
		} else {
			logger.info("data non valida: {}/{}/{}", day, month, year);
			panelOrderError.setText("La data " + day + "/" + month + "/" + year + " non è corretta");
		}
		textFieldDayNewOrder.setText("");
		textFieldMonthNewOrder.setText("");
		textFieldYearNewOrder.setText("");
		textFieldRevenueNewOrder.setText("");
		comboboxClients.setSelectedIndex(-1);

	}

	private boolean isValidDate(int day, int month, int year) {
		// anno bisestile se divisibile per 4 non per 100 ma divisibile per 400
		boolean isLeap = (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0));
		int maxDay;

		switch (month) {
		case 2:
			maxDay = isLeap ? 29 : 28;
			break;
		case 4:
		case 6:
		case 9:
		case 11:
			maxDay = 30;
			break;
		default:
			maxDay = 31;
		}
		return year >= (2025 - 100) && year <= 2025 && month >= 1 && month <= 12 && (day >= 1 && day <= maxDay);

	}

	private DocumentFilter createTextFilter(int maxLength, String regex, Runnable onChange, String spaces) {
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
		boolean wasEmpty = comboboxYears.getItemCount() == 0;
		Integer prevSelection = (Integer) comboboxYears.getSelectedItem();

		ActionListener[] ls = comboboxYears.getActionListeners();
		for (ActionListener l : ls)
			comboboxYears.removeActionListener(l);

		comboboxYearsModel.removeAllElements();
		Collections.sort(yearsOfOrders, Collections.reverseOrder());
		logger.info("yearsOfTheOrders: {}", yearsOfOrders);
		if (!yearsOfOrders.contains(2025)) {
			comboboxYearsModel.addElement(2025);
		}
		for (Integer integer : yearsOfOrders) {
			comboboxYearsModel.addElement(integer);
		}

		comboboxYearsModel.addElement(NO_YEAR_ITEM);

		for (ActionListener l : ls)
			comboboxYears.addActionListener(l);

		if (wasEmpty) {
			comboboxYears.setSelectedItem(2025);

		} else {
			Integer toSelect = (prevSelection != null && yearsOfOrders.contains(prevSelection)) ? prevSelection : null;
			if (toSelect != null) {
				comboboxYears.setSelectedItem(toSelect); // nessun evento perché i listener sono staccati
			} else {
				comboboxYears.setSelectedIndex(-1);
			}
		}

	}

	@Override
	public void showAllOrders(List<Order> orders) {
		boolean currentYearIsNotSelected = false;
		boolean aYearIsSelected = false;
		logger.info("comboboxYears.getSelectedIndex(): {}", comboboxYears.getSelectedIndex());
		// --- stato selezione anno ---
		Integer yearSelected = getYearSelected();
		if (yearSelected != null) {
			aYearIsSelected = true;
			if (yearSelected != 2025) {
				currentYearIsNotSelected = true;
			}

		} else {
			logger.info("show all orders anno non selezionato");
			orderController.yearsOfTheOrders(); // se aggiungo un ordine che non appartiene al cliente selezionato devo
												// chiamare il metodo per controllare che l'anno, se non presente nel
												// combobox, sia aggiunto
			comboboxYears.setSelectedIndex(-1);
		}
		if (orders.isEmpty()) {
			logger.info("orders is Empty");
			logger.debug("listClients.getSelectedIndex() != -1 : {}", (listClients.getSelectedIndex() != -1));
			Client clientSelected = getClientSelected();
			if (currentYearIsNotSelected && listClients.getSelectedIndex() == -1) {
				orderController.yearsOfTheOrders();
				return;
			}
			// anno selezionato (qualsiasi) ma non ci sono ordini
			if (!currentYearIsNotSelected && aYearIsSelected) {
				getOrderTableModel().removedAllOrders();
				panelOrderError.setText("Non sono presenti ordini per il " + yearSelected);
			}
			if (clientSelected != null && !aYearIsSelected) {
				getOrderTableModel().removedAllOrders();
				panelOrderError.setText("Non ci sono ordini per il cliente " + clientSelected.getIdentifier());
				lblrevenue.setText("");

			}

			if (clientSelected != null && aYearIsSelected) {
				logger.info("cliente selezionato, anno selezionato ma non ci sono ordini");
				getOrderTableModel().removedAllOrders();
				panelOrderError.setText("Non sono presenti ordini del " + comboboxYears.getSelectedItem()
						+ " per il cliente " + clientSelected.getIdentifier());
				lblrevenue.setText("");
			}

			// cliente selezionato e anno selezionato, ma nessun ordine
			if (clientSelected != null && aYearIsSelected) {
				getOrderTableModel().removedAllOrders();
				panelOrderError.setText("Non sono presenti ordini del " + comboboxYears.getSelectedItem()
						+ " per il cliente " + clientSelected.getIdentifier());
				lblrevenue.setText("");
			}

			if (clientSelected == null && !aYearIsSelected) {
				getOrderTableModel().removedAllOrders();
				panelOrderError.setText("Non sono presenti ordini");
				lblrevenue.setText("");

			}
			return;

		}
		getOrderTableModel().removedAllOrders();
		for (Order order : orders) {
			orderTableModel.addOrder(order);
		}
		resetRevenueLabel(orders);

	}

	private Client getClientSelected() {
		int index = listClients.getSelectedIndex();
		if (index == -1)
			return null;
		return getClientListModel().getElementAt(index);
	}

	private Integer getYearSelected() {
		int index = comboboxYears.getSelectedIndex();
		if (index == -1)
			return null;
		return Integer.parseInt(getComboboxYearsModel().getElementAt(index).toString());
	}

	private boolean matchesYearAndClientFilter(Order order, Integer year, Client client) {
		boolean isYearMatching = year == null
				|| order.getDate().toInstant().atZone(ZoneId.systemDefault()).getYear() == year;
		boolean isClientMatching = client == null || client.equals(order.getClient());
		return isYearMatching && isClientMatching;
	}

	private void resetRevenueLabel(List<Order> orders) {
		if (listClients.getSelectedIndex() != -1 && comboboxYears.getSelectedIndex() != -1) {
			Client clientSelected = (Client) listClients.getSelectedValue();
			lblrevenue
					.setText("Il costo totale degli ordini del cliente " + clientSelected.getIdentifier() + " nel "
							+ comboboxYears.getSelectedItem() + " è di "
							+ String.format("%.2f", (orders.stream()
									.filter(o -> o.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
											.getYear() == Integer.valueOf(comboboxYears.getSelectedItem().toString()))
									.mapToDouble(Order::getPrice).sum())).replace(".", ",")
							+ "€");
		}
		if (listClients.getSelectedIndex() != -1 && comboboxYears.getSelectedIndex() == -1) {
			Client clientSelected = (Client) listClients.getSelectedValue();
			lblrevenue.setText("Il costo totale degli ordini del cliente " + clientSelected.getIdentifier() + " è di "
					+ String.format("%.2f", (orders.stream().mapToDouble(Order::getPrice).sum())).replace(".", ",")
					+ "€");
		}
		if (listClients.getSelectedIndex() == -1 && comboboxYears.getSelectedIndex() != -1) {
			lblrevenue.setText("Il costo totale degli ordini nel " + comboboxYears.getSelectedItem() + " è di "
					+ String.format("%.2f", (orders.stream().mapToDouble(Order::getPrice).sum())).replace(".", ",")
					+ "€");

		}
		if (listClients.getSelectedIndex() == -1 && comboboxYears.getSelectedIndex() == -1) {
			lblrevenue.setText("Il costo totale degli ordini presenti nel DB è di "
					+ String.format("%.2f", (orders.stream().mapToDouble(Order::getPrice).sum())).replace(".", ",")
					+ "€");

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
		logger.info("client to remove: {}", clientRemoved);
		logger.info("comboboxclients size: {}", comboboxClients.getItemCount());
		listClients.clearSelection();

	}

	@Override
	public void clientAdded(Client clientAdded) {
		// TODO Auto-generated method stub
		clientListModel.addElement(clientAdded);
		// mantiene la selezione vecchia del ComboBoxclient dopo l'inserimento
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

		logger.info("listClients.getSelectedValue(): {}", listClients.getSelectedValue());
		logger.info("comboboxClients.getSelectedItem(): {}", comboboxClients.getSelectedItem());

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
		logger.info(": {}", selectedItem);
		int yearSelected = -1;
		if (selectedItem != null) {
			yearSelected = (Integer) selectedItem;
			logger.info("orderselected: {}", orderSelected);
			logger.info("yearsoFOrder: {}", yearOfOrder);

			if (getComboboxYearsModel().getIndexOf(yearOfOrder) == -1) {
				List<Object> items = getAllElements(getComboboxYearsModel());
				items.add(yearOfOrder);
				logger.info("years: {}", items);
				items.remove(Pattern.compile(NO_YEAR_ITEM));
				List<Integer> years = items.stream().map(obj -> (Integer) obj).collect(Collectors.toList());
				Collections.sort(years);
				Collections.reverse(years);
				getComboboxYearsModel().removeAllElements();
				for (Integer integer : years) {
					getComboboxYearsModel().addElement(integer);
				}
				getComboboxYearsModel().addElement(NO_YEAR_ITEM);
				getComboboxYearsModel().setSelectedItem(yearSelected);
			}

			if (yearSelected == yearOfOrder) {
				getOrderTableModel().addOrder(orderAdded);
				logger.info("orderAdded function: orders: {}", getOrderTableModel().getOrders());
				// usa la deepCopy, si puo anche fare altro
				List<Order> ordersList = new ArrayList<Order>(getOrderTableModel().getOrders());
				showAllOrders(ordersList);

			}
			if (orderSelected != null) {
				logger.info("order select index");
				int orderSelectedIndex = getOrderTableModel().getOrderIndex(orderSelected);
				tableOrders.setRowSelectionInterval(orderSelectedIndex, orderSelectedIndex);
			}

		}
		if (selectedItem == null && listClients.getSelectedIndex() != -1) {
			Client clientSelected = getClientListModel().getElementAt(listClients.getSelectedIndex());
			// se l'ordine è del cliente che l'utente sta guardando, lo aggiungiamo
			if (clientSelected.equals(orderAdded.getClient())) {
				getOrderTableModel().addOrder(orderAdded);
				// Copia per passare a showAllOrders
				List<Order> ordersList = new ArrayList<Order>(getOrderTableModel().getOrders());
				showAllOrders(ordersList);
				// Qui la view sta mostrando tutti gli ordini del cliente (senza filtro anno)
				// showAllOrders applicherà la logica coerente (client selezionato, anno non
				// selezionato)
			} else {
				if (comboboxYearsModel
						.getIndexOf(orderAdded.getDate().toInstant().atZone(ZoneId.systemDefault()).getYear()) == -1) {

					orderController.yearsOfTheOrders(); // nel caso l'anno dell'ordine non sia contenuto nella lista
														// degli ordini
				}
			}
		} // Se non c’è né anno né cliente selezionati, mostriamo “tutti gli ordini”
		if (selectedItem == null && listClients.getSelectedIndex() == -1) {
			getOrderTableModel().addOrder(orderAdded);
			List<Order> ordersList = new ArrayList<>(getOrderTableModel().getOrders());
			showAllOrders(ordersList);
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
		getOrderTableModel().removeOrdersOfClient(client);

	}

	@Override
	public void showOrderError(String errorMessage, Order order) {
		panelOrderError.setText(errorMessage + ": " + order);

	}

	@Override
	public void orderRemoved(Order orderRemoved) {
		// TODO Auto-generated method stub
		getOrderTableModel().removeOrder(orderRemoved);
		List<Order> orderCopy = new ArrayList<Order>(orderTableModel.getOrders());
		showAllOrders(orderCopy);

	}

	@Override
	public void orderUpdated(Order orderModified) {
		if (orderModified.getIdentifier() == null)
			return;

		Integer yearSelected = getYearSelected();
		Client clientSelected = getClientSelected();
		boolean isOrderIDPresent = false;

		List<Order> updatedOrders = new ArrayList<>();
		for (Order order : getOrderTableModel().getOrders()) {
			if (!order.getIdentifier().equals(orderModified.getIdentifier())) {
				updatedOrders.add(order);
			} else {
				isOrderIDPresent = true;
				if (matchesYearAndClientFilter(orderModified, yearSelected, clientSelected)) {
					updatedOrders.add(orderModified);
				}
			}
		}

		if (!isOrderIDPresent && matchesYearAndClientFilter(orderModified, yearSelected, clientSelected)) {
			getOrderTableModel().addOrder(orderModified);
		} else {
			showAllOrders(updatedOrders);
		}

		if (!getOrderTableModel().getOrders().isEmpty()) {
			panelOrderError.setText("");
		}
	}

	public void setOrderController(OrderController controller) {
		this.orderController = controller;
	}

	public DefaultListModel<Client> getClientListModel() {
		return clientListModel;
	}

	public DefaultComboBoxModel<Client> getComboboxClientsModel() {
		return comboboxClientsModel;
	}

	public DefaultComboBoxModel getComboboxYearsModel() {
		return comboboxYearsModel;
	}

	public OrderTableModel getOrderTableModel() {
		return orderTableModel;
	}

	public JTable getOrderTable() {
		return tableOrders;
	}

}
