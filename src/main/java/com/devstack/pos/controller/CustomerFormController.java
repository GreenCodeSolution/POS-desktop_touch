package com.devstack.pos.controller;

import com.devstack.pos.entity.Customer;
import com.devstack.pos.service.CustomerService;
import com.devstack.pos.service.OrderDetailService;
import com.devstack.pos.view.tm.CustomerTm;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javafx.scene.layout.VBox;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CustomerFormController extends BaseController {
    public TextField txtName;
    public TextField txtContact;
    public Button btnSaveUpdate;
    public TextField txtSearch;
    public TableView<CustomerTm> tbl;
    public TableColumn<CustomerTm, Long> colId;
    public TableColumn<CustomerTm, String> colName;
    public TableColumn<CustomerTm, String> colContact;
    public TableColumn<CustomerTm, Double> colTotalSpent;
    public TableColumn<CustomerTm, Double> colLoyaltyPoints;
    public TableColumn<CustomerTm, Double> colPendingPayments;
    public TableColumn<CustomerTm, Button> colOperate;
    public VBox keyboardPane;

    private TextField lastFocusedField;
    private String searchText = "";
    private Long selectedCustomerId = null;
    private final CustomerService customerService;
    private final OrderDetailService orderDetailService;

    public void initialize() {
        // Initialize sidebar
        initializeSidebar();

        // Initialize table columns
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colContact.setCellValueFactory(new PropertyValueFactory<>("contact"));
        colTotalSpent.setCellValueFactory(new PropertyValueFactory<>("totalSpent"));
        colLoyaltyPoints.setCellValueFactory(new PropertyValueFactory<>("loyaltyPoints"));
        colPendingPayments.setCellValueFactory(new PropertyValueFactory<>("pendingPayments"));
        colOperate.setCellValueFactory(new PropertyValueFactory<>("deleteButton"));

        // Format total spent column to show currency
        colTotalSpent.setCellFactory(column -> new TableCell<CustomerTm, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("Rs. %.2f", item));
                }
            }
        });

        // Format pending payments column to show currency
        colPendingPayments.setCellFactory(column -> new TableCell<CustomerTm, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("Rs. %.2f", item));
                }
            }
        });

        // Load customers
        loadAllCustomers(searchText);

        // Table selection listener
        tbl.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        setData(newValue);
                    }
                });

        // Search listener
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            searchText = newValue;
            loadAllCustomers(searchText);
        });

        // Setup keyboard navigation for form fields
        setupFieldNavigation();

        // Setup touch keyboard focus listeners
        setupTouchKeyboardListeners();
    }

    private void setupTouchKeyboardListeners() {
        txtName.focusedProperty().addListener((obs, old, isFocused) -> {
            if (isFocused) {
                lastFocusedField = txtName;
                showKeyboard();
            }
        });
        txtContact.focusedProperty().addListener((obs, old, isFocused) -> {
            if (isFocused) {
                lastFocusedField = txtContact;
                showKeyboard();
            }
        });
        txtSearch.focusedProperty().addListener((obs, old, isFocused) -> {
            if (isFocused) {
                lastFocusedField = txtSearch;
                showKeyboard();
            }
        });
    }

    private void showKeyboard() {
        if (keyboardPane != null) {
            keyboardPane.setVisible(true);
            keyboardPane.setManaged(true);
        }
    }

    private void setupFieldNavigation() {
        // Name field: Enter -> Focus Contact
        txtName.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                txtContact.requestFocus();
            } else if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                clearFields();
                tbl.getSelectionModel().clearSelection();
                btnSaveUpdate.setText("Save Customer");
            }
        });

        // Contact field: Enter -> Save/Update
        txtContact.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                btnSaveUpdateOnAction(new ActionEvent());
            } else if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                clearFields();
                tbl.getSelectionModel().clearSelection();
                btnSaveUpdate.setText("Save Customer");
            }
        });

        // Also allow ESC on the context to clear fields
        if (context != null) {
            context.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    clearFields();
                    tbl.getSelectionModel().clearSelection();
                    btnSaveUpdate.setText("Save Customer");
                }
            });
        }
    }

    @Override
    protected String getCurrentPageName() {
        return "Customers";
    }

    private void setData(CustomerTm newValue) {
        selectedCustomerId = newValue.getId();
        btnSaveUpdate.setText("Update Customer");
        txtName.setText(newValue.getName());
        txtContact.setText(newValue.getContact());
        txtName.requestFocus();
    }

    public void searchCustomer(ActionEvent actionEvent) {
        searchText = txtSearch.getText();
        loadAllCustomers(searchText);
    }

    private void loadAllCustomers(String searchText) {
        ObservableList<CustomerTm> observableList = FXCollections.observableArrayList();
        for (Customer customer : (searchText.length() > 0 ? customerService.searchCustomers(searchText)
                : customerService.findAllCustomers())) {
            // Get total spent by this customer using optimized query
            double totalSpent = orderDetailService.getTotalSpentByCustomerId(customer.getId());

            // Get pending payments total by this customer
            double pendingPayments = orderDetailService.getPendingPaymentsTotalByCustomerId(customer.getId());

            Button btn = new Button("Delete");
            CustomerTm tm = new CustomerTm(
                    customer.getId(), customer.getName(), customer.getContact(), totalSpent, pendingPayments,
                    customer.getLoyaltyPoints(), btn);
            observableList.add(tm);

            btn.setOnAction((e) -> {
                try {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                            "Are you sure you want to delete this customer?", ButtonType.YES, ButtonType.NO);
                    Optional<ButtonType> selectedButtonType = alert.showAndWait();
                    if (selectedButtonType.isPresent() && selectedButtonType.get().equals(ButtonType.YES)) {
                        if (customerService.deleteCustomer(customer.getId())) {
                            loadAllCustomers(searchText);
                            clearFields();
                            btnSaveUpdate.setText("Save Customer");
                            tbl.getSelectionModel().clearSelection();
                            hideKeyboard(null);
                        } else {
                            new Alert(Alert.AlertType.WARNING, "Try Again!").show();
                        }
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, exception.getMessage()).show();
                }
            });
        }
        tbl.setItems(observableList);
    }

    /**
     * Validates the customer name.
     * Name should only contain letters and spaces, and be between 2-50 characters.
     * 
     * @param name The customer name to validate
     * @return Error message if invalid, null if valid
     */
    private String validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Please enter customer name!";
        }

        String trimmedName = name.trim();

        // Check minimum length
        if (trimmedName.length() < 2) {
            return "Customer name must be at least 2 characters long!";
        }

        // Check maximum length
        if (trimmedName.length() > 50) {
            return "Customer name must not exceed 50 characters!";
        }

        // Check if name contains only letters and spaces
        if (!trimmedName.matches("^[a-zA-Z\\s]+$")) {
            return "Customer name should only contain letters and spaces!";
        }

        return null; // Valid
    }

    /**
     * Validates the contact number.
     * Contact should only contain digits and be between 9-15 characters (supporting
     * local and international formats).
     * 
     * @param contact The contact number to validate
     * @return Error message if invalid, null if valid
     */
    private String validateContact(String contact) {
        if (contact == null || contact.trim().isEmpty()) {
            return "Please enter contact number!";
        }

        String trimmedContact = contact.trim();

        // Remove any spaces or dashes for validation
        String digitsOnly = trimmedContact.replaceAll("[\\s\\-]", "");

        // Check if contact contains only digits (after removing spaces and dashes)
        if (!digitsOnly.matches("^[0-9]+$")) {
            return "Contact number should only contain digits!";
        }

        // Check minimum length (9 digits for local Sri Lankan numbers without country
        // code)
        if (digitsOnly.length() < 9) {
            return "Contact number must be at least 9 digits!";
        }

        return null; // Valid
    }

    public void btnSaveUpdateOnAction(ActionEvent actionEvent) {
        try {
            // Validate Name
            String nameError = validateName(txtName.getText());
            if (nameError != null) {
                new Alert(Alert.AlertType.WARNING, nameError).show();
                txtName.requestFocus();
                return;
            }

            // Validate Contact
            String contactError = validateContact(txtContact.getText());
            if (contactError != null) {
                new Alert(Alert.AlertType.WARNING, contactError).show();
                txtContact.requestFocus();
                return;
            }

            if (btnSaveUpdate.getText().equals("Save Customer")) {
                Customer customer = new Customer(txtName.getText().trim(), txtContact.getText().trim());
                if (customerService.saveCustomer(customer)) {
                    new Alert(Alert.AlertType.INFORMATION, "Customer Saved Successfully!").show();
                    clearFields();
                    loadAllCustomers(searchText);
                    hideKeyboard(null);
                } else {
                    new Alert(Alert.AlertType.WARNING, "Failed to save customer. Please try again!").show();
                }
            } else {
                if (selectedCustomerId != null) {
                    Customer existingCustomer = customerService.findCustomer(selectedCustomerId);
                    double existingLoyaltyPoints = existingCustomer != null ? existingCustomer.getLoyaltyPoints() : 0.0;
                    Customer customer = new Customer(selectedCustomerId, txtName.getText().trim(),
                            txtContact.getText().trim());
                    customer.setLoyaltyPoints(existingLoyaltyPoints);
                    if (customerService.updateCustomer(customer)) {
                        new Alert(Alert.AlertType.INFORMATION, "Customer Updated Successfully!").show();
                        clearFields();
                        loadAllCustomers(searchText);
                        selectedCustomerId = null;
                        btnSaveUpdate.setText("Save Customer");
                        hideKeyboard(null);
                    } else {
                        new Alert(Alert.AlertType.WARNING, "Failed to update customer. Please try again!").show();
                    }
                } else {
                    new Alert(Alert.AlertType.WARNING, "No customer selected for update!").show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage()).show();
        }
    }

    private void clearFields() {
        txtName.clear();
        txtContact.clear();
        selectedCustomerId = null;
    }

    // Navigation methods are inherited from BaseController

    public void btnBackToHomeOnAction(ActionEvent actionEvent) {
        btnHomeOnAction(actionEvent);
    }

    public void btnNewCustomerOnAction(ActionEvent actionEvent) {
        btnSaveUpdate.setText("Save Customer");
        clearFields();
        tbl.getSelectionModel().clearSelection();
        txtName.requestFocus();
    }

    public void handleFieldFocus(javafx.scene.input.MouseEvent event) {
        if (event.getSource() instanceof TextField) {
            lastFocusedField = (TextField) event.getSource();
        }
        keyboardPane.setVisible(true);
        keyboardPane.setManaged(true);
    }

    public void hideKeyboard(ActionEvent event) {
        keyboardPane.setVisible(false);
        keyboardPane.setManaged(false);
    }

    public void btnKeyOnAction(ActionEvent event) {
        if (lastFocusedField != null) {
            Button btn = (Button) event.getSource();
            lastFocusedField.appendText(btn.getText());
            lastFocusedField.requestFocus();
        }
    }

    public void btnBackspaceOnAction(ActionEvent event) {
        if (lastFocusedField != null) {
            String current = lastFocusedField.getText();
            if (!current.isEmpty()) {
                lastFocusedField.setText(current.substring(0, current.length() - 1));
            }
            lastFocusedField.requestFocus();
        }
    }

    public void btnEnterOnAction(ActionEvent event) {
        if (lastFocusedField == txtName) {
            txtContact.requestFocus();
            lastFocusedField = txtContact;
        } else if (lastFocusedField == txtContact) {
            btnSaveUpdateOnAction(new ActionEvent());
        } else if (lastFocusedField == txtSearch) {
            searchCustomer(new ActionEvent());
        }
    }
}
