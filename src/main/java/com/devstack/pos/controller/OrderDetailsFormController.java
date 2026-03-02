package com.devstack.pos.controller;

import com.devstack.pos.entity.OrderDetail;
import com.devstack.pos.entity.ReturnOrder;
import com.devstack.pos.service.*;
import com.devstack.pos.view.tm.OrderTm;
import com.jfoenix.controls.JFXButton;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderDetailsFormController extends BaseController {

    private final OrderDetailService orderDetailService;
    private final OrderItemService orderItemService;
    private final ReturnOrderService returnOrderService;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Summary Labels
    @FXML
    private Text lblTotalOrders;

    @FXML
    private Text lblTotalRevenue;

    @FXML
    private Text lblAvgOrder;

    @FXML
    private Text lblTodayOrders;

    @FXML
    private Text lblTodayRevenue;

    @FXML
    private Text lblOrdersPeriod;

    @FXML
    private Text lblRevenuePeriod;

    @FXML
    private Text lblRecordCount;

    // Filter Controls
    @FXML
    private com.jfoenix.controls.JFXTextField txtSearch;

    @FXML
    private DatePicker dateFrom;

    @FXML
    private DatePicker dateTo;

    // Table
    @FXML
    private TableView<OrderTm> tblOrders;

    @FXML
    private TableColumn<OrderTm, Long> colOrderId;

    @FXML
    private TableColumn<OrderTm, String> colCustomerName;

    @FXML
    private TableColumn<OrderTm, String> colProductNames;

    @FXML
    private TableColumn<OrderTm, LocalDateTime> colDate;

    @FXML
    private TableColumn<OrderTm, String> colDiscount;

    @FXML
    private TableColumn<OrderTm, String> colOperator;

    @FXML
    private TableColumn<OrderTm, Double> colTotal;

    @FXML
    private TableColumn<OrderTm, JFXButton> colAction;

    @FXML
    private TableColumn<OrderTm, JFXButton> colReturnOrders;

    @FXML
    public void initialize() {
        initializeSidebar();

        // Initialize date pickers (default to last 30 days)
        dateFrom.setValue(LocalDate.now().minusDays(30));
        dateTo.setValue(LocalDate.now());

        // Setup table columns
        colOrderId.setCellValueFactory(new PropertyValueFactory<>("code"));
        colCustomerName.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colProductNames.setCellValueFactory(new PropertyValueFactory<>("productNames"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("issuedDate"));
        colDate.setCellFactory(column -> new TableCell<OrderTm, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(dateTimeFormatter));
                }
            }
        });
        colDiscount.setCellValueFactory(new PropertyValueFactory<>("discount"));
        colOperator.setCellValueFactory(new PropertyValueFactory<>("operatorEmail"));
        if (colTotal != null) {
            colTotal.setCellValueFactory(new PropertyValueFactory<>("totalCost"));
            colTotal.setCellFactory(column -> new TableCell<OrderTm, Double>() {
                @Override
                protected void updateItem(Double item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(String.format("%.2f", item));
                    }
                }
            });
        }

        colAction.setCellValueFactory(new PropertyValueFactory<>("viewButton"));
        colReturnOrders.setCellValueFactory(new PropertyValueFactory<>("returnOrdersButton"));

        tblOrders.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        loadOrders();
        loadStatistics();
    }

    @Override
    protected String getCurrentPageName() {
        return "All Orders";
    }

    @FXML
    public void btnRefreshOnAction(ActionEvent event) {
        loadOrders();
        loadStatistics();
    }

    @FXML
    public void btnSearchOnAction(ActionEvent event) {
        loadOrders();
        loadStatistics();
    }

    @FXML
    public void btnClearOnAction(ActionEvent event) {
        txtSearch.clear();
        dateFrom.setValue(LocalDate.now().minusDays(30));
        dateTo.setValue(LocalDate.now());
        loadOrders();
        loadStatistics();
    }

    private void loadOrders() {
        try {
            String searchText = txtSearch.getText() != null ? txtSearch.getText().trim() : "";
            LocalDate fromDate = dateFrom.getValue();
            LocalDate toDate = dateTo.getValue();

            LocalDateTime startDateTime = null;
            LocalDateTime endDateTime = null;

            if (fromDate != null) {
                startDateTime = fromDate.atStartOfDay();
            }
            if (toDate != null) {
                endDateTime = toDate.atTime(LocalTime.MAX);
            }

            List<OrderDetail> orders;
            if (startDateTime != null && endDateTime != null) {
                orders = orderDetailService.findOrdersByDateRange(startDateTime, endDateTime);
            } else {
                orders = orderDetailService.findAllOrderDetails();
            }

            if (!searchText.isEmpty()) {
                String searchLower = searchText.toLowerCase();
                orders = orders.stream()
                        .filter(order -> (order.getCode() != null && order.getCode().toString().contains(searchText)) ||
                                (order.getCustomerName() != null
                                        && order.getCustomerName().toLowerCase().contains(searchLower))
                                ||
                                (order.getOperatorEmail() != null
                                        && order.getOperatorEmail().toLowerCase().contains(searchLower)))
                        .collect(Collectors.toList());
            }

            ObservableList<OrderTm> observableList = FXCollections.observableArrayList();

            for (OrderDetail order : orders) {
                JFXButton viewBtn = new JFXButton("View");
                viewBtn.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-background-radius: 5;");
                viewBtn.setOnAction(e -> viewOrderDetails(order));

                List<ReturnOrder> returnOrders = returnOrderService.findByOrderId(order.getCode().intValue());
                JFXButton returnOrdersBtn = null;
                double totalRefundsForOrder = 0.0;

                if (returnOrders != null && !returnOrders.isEmpty()) {
                    returnOrdersBtn = new JFXButton("View Returns (" + returnOrders.size() + ")");
                    returnOrdersBtn
                            .setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-background-radius: 5;");
                    returnOrdersBtn.setOnAction(e -> viewReturnOrders(order.getCode().intValue(), returnOrders));

                    totalRefundsForOrder = returnOrders.stream()
                            .filter(r -> "COMPLETED".equals(r.getStatus()))
                            .mapToDouble(r -> r.getRefundAmount() != null ? r.getRefundAmount() : 0.0)
                            .sum();
                } else {
                    returnOrdersBtn = new JFXButton("No Returns");
                    returnOrdersBtn
                            .setStyle("-fx-background-color: #9CA3AF; -fx-text-fill: white; -fx-background-radius: 5;");
                    returnOrdersBtn.setDisable(true);
                }

                String productNames = orderItemService.findByOrderId(order.getCode())
                        .stream()
                        .map(item -> {
                            Double qty = item.getQuantity();
                            String qtyStr;
                            if (qty != null) {
                                if (qty == qty.intValue()) {
                                    qtyStr = String.valueOf(qty.intValue());
                                } else {
                                    qtyStr = String.format("%.2f", qty);
                                }
                            } else {
                                qtyStr = "0";
                            }
                            return item.getProductName() + " (x" + qtyStr + ")";
                        })
                        .collect(Collectors.joining(", "));

                if (productNames.isEmpty()) {
                    productNames = "No products";
                }

                OrderTm tm = new OrderTm();
                tm.setCode(order.getCode());
                tm.setCustomerName(order.getCustomerName() != null ? order.getCustomerName() : "Guest");
                tm.setProductNames(productNames);
                tm.setIssuedDate(order.getIssuedDate());
                tm.setDiscount(order.getDiscount());
                tm.setOperatorEmail(order.getOperatorEmail());
                tm.setTotalCost(order.getTotalCost() - totalRefundsForOrder);
                tm.setViewButton(viewBtn);
                tm.setReturnOrdersButton(returnOrdersBtn);
                observableList.add(tm);
            }

            observableList.sort((o1, o2) -> {
                if (o1.getIssuedDate() == null && o2.getIssuedDate() == null)
                    return 0;
                if (o1.getIssuedDate() == null)
                    return 1;
                if (o2.getIssuedDate() == null)
                    return -1;
                return o2.getIssuedDate().compareTo(o1.getIssuedDate());
            });

            tblOrders.setItems(observableList);
            if (lblRecordCount != null) {
                lblRecordCount.setText("Showing " + orders.size() + " order(s)");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Error", "Failed to load orders: " + ex.getMessage());
        }
    }

    private void loadStatistics() {
        try {
            LocalDate fromDate = dateFrom.getValue();
            LocalDate toDate = dateTo.getValue();

            LocalDateTime startDateTime = null;
            LocalDateTime endDateTime = null;

            if (fromDate != null) {
                startDateTime = fromDate.atStartOfDay();
            }
            if (toDate != null) {
                endDateTime = toDate.atTime(LocalTime.MAX);
            }

            Long totalOrders;
            Double totalRevenue;
            Double avgOrder;
            Long totalReturnOrders;
            Double totalRefundAmount;

            if (startDateTime != null && endDateTime != null) {
                totalOrders = orderDetailService.countOrdersByDateRange(startDateTime, endDateTime);
                totalRevenue = orderDetailService.getRevenueByDateRange(startDateTime, endDateTime);
                avgOrder = orderDetailService.getAverageOrderValueByDateRange(startDateTime, endDateTime);
                totalReturnOrders = returnOrderService.countReturnsByDateRange(startDateTime, endDateTime);
                totalRefundAmount = returnOrderService.getTotalRefundAmountByDateRange(startDateTime, endDateTime);

                String period = fromDate.format(dateFormatter) + " to " + toDate.format(dateFormatter);
                if (lblOrdersPeriod != null)
                    lblOrdersPeriod.setText(period);
                if (lblRevenuePeriod != null)
                    lblRevenuePeriod.setText(period);
            } else {
                totalOrders = orderDetailService.getTotalOrderCount();
                totalRevenue = orderDetailService.getTotalRevenue();
                avgOrder = orderDetailService.getAverageOrderValue();
                totalReturnOrders = (long) returnOrderService.findAllReturnOrders().size();
                totalRefundAmount = returnOrderService.getTotalRefundAmount();

                if (lblOrdersPeriod != null)
                    lblOrdersPeriod.setText("All Time");
                if (lblRevenuePeriod != null)
                    lblRevenuePeriod.setText("All Time");
            }

            LocalDate today = LocalDate.now();
            LocalDateTime todayStart = today.atStartOfDay();
            LocalDateTime todayEnd = today.atTime(LocalTime.MAX);

            Long todayOrders = orderDetailService.countOrdersByDateRange(todayStart, todayEnd);
            Double todayRevenue = orderDetailService.getRevenueByDateRange(todayStart, todayEnd);
            Long todayReturnOrders = returnOrderService.countReturnsByDateRange(todayStart, todayEnd);
            Double todayRefundAmount = returnOrderService.getTotalRefundAmountByDateRange(todayStart, todayEnd);

            Double netRevenue = (totalRevenue != null ? totalRevenue : 0.0)
                    - (totalRefundAmount != null ? totalRefundAmount : 0.0);
            Double todayNetRevenue = (todayRevenue != null ? todayRevenue : 0.0)
                    - (todayRefundAmount != null ? todayRefundAmount : 0.0);

            String ordersText = String.valueOf(totalOrders != null ? totalOrders : 0L);
            if (totalReturnOrders != null && totalReturnOrders > 0) {
                ordersText += " (" + totalReturnOrders + " returns)";
            }
            if (lblTotalOrders != null)
                lblTotalOrders.setText(ordersText);
            if (lblTotalRevenue != null)
                lblTotalRevenue.setText(String.format("LKR %,.2f", netRevenue));
            if (lblAvgOrder != null)
                lblAvgOrder.setText(String.format("LKR %,.2f", avgOrder != null ? avgOrder : 0.0));

            String todayOrdersText = String.valueOf(todayOrders != null ? todayOrders : 0L);
            if (todayReturnOrders != null && todayReturnOrders > 0) {
                todayOrdersText += " (" + todayReturnOrders + " returns)";
            }
            if (lblTodayOrders != null)
                lblTodayOrders.setText(todayOrdersText);
            if (lblTodayRevenue != null)
                lblTodayRevenue.setText(String.format("LKR %,.2f", todayNetRevenue));

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Error", "Failed to load statistics: " + ex.getMessage());
        }
    }

    private void viewOrderDetails(OrderDetail order) {
        try {
            List<com.devstack.pos.entity.OrderItem> items = orderItemService.findByOrderId(order.getCode());

            List<ReturnOrder> returnOrders = returnOrderService.findByOrderId(order.getCode().intValue());
            double totalRefunds = 0.0;
            if (returnOrders != null) {
                totalRefunds = returnOrders.stream()
                        .filter(r -> "COMPLETED".equals(r.getStatus()))
                        .mapToDouble(r -> r.getRefundAmount() != null ? r.getRefundAmount() : 0.0)
                        .sum();
            }

            StringBuilder details = new StringBuilder();
            details.append("Order Details\n\n");
            details.append(String.format("Order ID: %d\n", order.getCode()));
            details.append(String.format("Customer: %s\n",
                    order.getCustomerName() != null ? order.getCustomerName() : "Guest"));
            details.append(String.format("Date: %s\n", order.getIssuedDate().format(dateTimeFormatter)));
            details.append(String.format("Discount: %.2f\n", order.getDiscount()));

            details.append(String.format("Total Amount: %.2f /=\n", order.getTotalCost() - totalRefunds));

            details.append(String.format("Operator: %s\n",
                    order.getOperatorEmail() != null ? order.getOperatorEmail() : "Unknown"));
            details.append(String.format("Payment Method: %s\n",
                    order.getPaymentMethod() != null ? order.getPaymentMethod() : "CASH"));
            details.append(String.format("Payment Status: %s\n\n",
                    order.getPaymentStatus() != null ? order.getPaymentStatus() : "PAID"));

            details.append("Items:\n");
            if (items.isEmpty()) {
                details.append("No items found.\n");
            } else {
                for (com.devstack.pos.entity.OrderItem item : items) {
                    double discountPerUnit = item.getDiscountPerUnit() != null ? item.getDiscountPerUnit() : 0.0;
                    double sellingPrice = item.getUnitPrice() - discountPerUnit;

                    if (discountPerUnit > 0) {
                        details.append(String.format("- %s (x%.2f) @ %.2f (Original: %.2f) = %.2f\n",
                                item.getProductName(),
                                item.getQuantity(),
                                sellingPrice,
                                item.getUnitPrice(),
                                item.getLineTotal()));
                    } else {
                        details.append(String.format("- %s (x%.2f) @ %.2f = %.2f\n",
                                item.getProductName(),
                                item.getQuantity(),
                                item.getUnitPrice(),
                                item.getLineTotal()));
                    }
                }
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Order Details");
            alert.setHeaderText("Order #" + order.getCode());
            alert.setContentText(details.toString());
            alert.getDialogPane().setPrefWidth(500);
            alert.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Error", "Failed to load order details: " + ex.getMessage());
        }
    }

    private void viewReturnOrders(Integer orderId, List<ReturnOrder> returnOrders) {
        try {
            StringBuilder details = new StringBuilder();
            details.append("Return Orders for Order #").append(orderId).append("\n\n");

            if (returnOrders.isEmpty()) {
                details.append("No return orders found for this order.");
            } else {
                for (ReturnOrder returnOrder : returnOrders) {
                    details.append("Return ID: ").append(returnOrder.getReturnId()).append("\n");
                    details.append("Status: ").append(returnOrder.getStatus()).append("\n");
                    details.append("Original Amount: LKR ")
                            .append(String.format("%.2f", returnOrder.getOriginalAmount())).append("\n");
                    details.append("Refund Amount: LKR ").append(String.format("%.2f", returnOrder.getRefundAmount()))
                            .append("\n");
                    details.append("Reason: ").append(returnOrder.getReturnReason()).append("\n");
                    if (returnOrder.getNotes() != null && !returnOrder.getNotes().isEmpty()) {
                        details.append("Notes: ").append(returnOrder.getNotes()).append("\n");
                    }
                    details.append("Return Date: ").append(returnOrder.getReturnDate().format(dateTimeFormatter))
                            .append("\n");
                    if (returnOrder.getProcessedBy() != null) {
                        details.append("Processed By: ").append(returnOrder.getProcessedBy()).append("\n");
                    }
                    details.append("\n---\n\n");
                }
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Return Orders");
            alert.setHeaderText("Return Orders for Order #" + orderId);
            alert.setContentText(details.toString());
            alert.getDialogPane().setPrefWidth(500);
            alert.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Error", "Failed to load return order details: " + ex.getMessage());
        }
    }
}
