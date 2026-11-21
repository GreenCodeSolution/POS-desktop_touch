package com.devstack.pos.controller;

import com.devstack.pos.entity.*;
import com.devstack.pos.service.*;
import com.devstack.pos.util.AuthorizationUtil;
import com.devstack.pos.util.ReceiptPrinter;
import com.devstack.pos.util.TerminalConfigManager;
import com.devstack.pos.util.UserSessionData;
import com.devstack.pos.view.tm.CartTm;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.geometry.Bounds;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PlaceOrderFormController extends BaseController {
    public TextField txtContact;
    public Text txtName;
    public TextField txtBarcode;
    public TextField txtDescription;
    public TextField txtSellingPrice;
    public TextField txtDiscount;
    public TextField txtQtyOnHand;
    public TextField txtQty;
    public TableView<CartTm> tblCart;
    public TableColumn<CartTm, String> colCode;
    public TableColumn<CartTm, String> colDesc;
    public TableColumn<CartTm, Double> colSelPrice;
    public TableColumn<CartTm, Double> colSelDisc;
    public TableColumn<CartTm, Double> colSelQty;
    public TableColumn<CartTm, Double> colSelTotal;
    public TableColumn<CartTm, Button> colSelOperation;
    public Text txtTotal;
    public Text txtBalance;
    public JFXTextField txtCustomerPaid;
    public JFXButton btnPrint;
    public JFXComboBox<String> cmbPaymentMethod;
    public Text txtCustomerLoyaltyPoints;
    public JFXTextField txtPointsToUse;
    public JFXTextField txtBillDiscount;
    public Text txtPointsToEarn;
    public ImageView imgProduct;
    public FlowPane productGrid;
    public HBox categoryPane;

    private Long selectedCustomerId = null;
    private double availableLoyaltyPoints = 0.0;
    private Long lastCompletedOrderId = null; // Store order ID for printing
    private final CustomerService customerService;
    private final ProductDetailService productDetailService;
    private final ProductService productService;
    private final OrderDetailService orderDetailService;
    private final OrderItemService orderItemService;
    private final PDFReportService pdfReportService;
    private final ReceiptPrinter receiptPrinter;
    private final SystemSettingsService systemSettingsService;
    private final CategoryService categoryService;
    private final NotifyLkSmsService notifyLkSmsService;
    private boolean isUpdatingBarcodeProgrammatically = false;
    private Integer activeCategoryId = null;

    private double currentShowPrice = 0.0;

    public void initialize() {
        if (tms != null) {
            tms.clear();
        }
        selectedCustomerId = null;
        lastCompletedOrderId = null;

        // Disable print button initially (no order to print yet) - matches Kumara Enterprises
        if (btnPrint != null) {
            btnPrint.setDisable(true);
        }

        initializeSidebar();

        javafx.application.Platform.runLater(() -> {
            if (context != null && context.getScene() != null) {
                context.getScene().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                        clear();
                        event.consume();
                    }
                });
            }
        });

        if (!AuthorizationUtil.canAccessPOSOrders()) {
            AuthorizationUtil.showUnauthorizedAlert();
            btnHomeOnAction(null);
            return;
        }

        tblCart.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colCode.setCellValueFactory(new PropertyValueFactory<>("code"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colSelPrice.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        colSelDisc.setCellValueFactory(new PropertyValueFactory<>("discount"));
        colSelQty.setCellValueFactory(new PropertyValueFactory<>("qty"));
        colSelQty.setCellFactory(column -> new TableCell<CartTm, Double>() {
            @Override
            protected void updateItem(Double qty, boolean empty) {
                super.updateItem(qty, empty);
                if (empty || qty == null)
                    setText(null);
                else
                    setText(qty == qty.intValue() ? String.valueOf(qty.intValue()) : String.format("%.2f", qty));
            }
        });
        colSelTotal.setCellValueFactory(new PropertyValueFactory<>("totalCost"));
        colSelTotal.setCellFactory(column -> new TableCell<CartTm, Double>() {
            @Override
            protected void updateItem(Double total, boolean empty) {
                super.updateItem(total, empty);
                if (empty || total == null)
                    setText(null);
                else
                    setText(String.format("%.2f", total));
            }
        });
        colSelOperation.setCellValueFactory(new PropertyValueFactory<>("btn"));

        cmbPaymentMethod.setItems(FXCollections.observableArrayList("CASH", "CARD", "CHEQUE", "CREDIT"));
        cmbPaymentMethod.setValue("CASH");

        setupBarcodeScannerDetection();
        setupQuantityInputValidation();
        setupCustomerAutocomplete();
        loadCategories();
        loadAllProducts(null, null);
    }

    private void setupQuantityInputValidation() {
        txtQty.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) {
                txtQty.setText(oldValue);
            }
        });
    }

    @FXML
    public void handleDiscountKeyPress(javafx.scene.input.KeyEvent event) {
        if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
            txtQty.requestFocus();
        }
    }

    @Override
    protected String getCurrentPageName() {
        return "POS - Order Placement";
    }

    private void loadCategories() {
        categoryPane.getChildren().clear();
        JFXButton allBtn = new JFXButton("All Items");
        allBtn.getStyleClass().addAll("category-btn", "active");
        allBtn.setOnAction(e -> {
            activeCategoryId = null;
            updateCategoryActiveState(allBtn);
            loadAllProducts(null, txtBarcode.getText());
        });
        categoryPane.getChildren().add(allBtn);

        try {
            List<Category> categories = categoryService.findAll();
            for (Category category : categories) {
                JFXButton catBtn = new JFXButton(category.getName());
                catBtn.getStyleClass().add("category-btn");
                catBtn.setOnAction(e -> {
                    activeCategoryId = category.getId();
                    updateCategoryActiveState(catBtn);
                    loadAllProducts(category.getId(), txtBarcode.getText());
                });
                categoryPane.getChildren().add(catBtn);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateCategoryActiveState(JFXButton selectedBtn) {
        categoryPane.getChildren().forEach(node -> {
            if (node instanceof JFXButton) {
                node.getStyleClass().remove("active");
            }
        });
        selectedBtn.getStyleClass().add("active");
    }

    private void loadAllProducts(Integer categoryId, String searchText) {
        productGrid.getChildren().clear();
        try {
            List<ProductDetail> allProducts = productDetailService.findAllProductDetails();
            String lowerSearch = (searchText != null) ? searchText.toLowerCase().trim() : "";

            for (ProductDetail detail : allProducts) {
                if (detail.getQtyOnHand() > 0 && !detail.isExpired()) {
                    Product product = productService.findProduct(detail.getProductCode());

                    // Filter by category
                    if (categoryId != null && (product == null || product.getCategory() == null
                            || !product.getCategory().getId().equals(categoryId))) {
                        continue;
                    }

                    // Filter by search text (keyword or barcode)
                    if (!lowerSearch.isEmpty()) {
                        boolean matchesBarcode = detail.getCode().toLowerCase().contains(lowerSearch) ||
                                (detail.getBarcode() != null
                                        && detail.getBarcode().toLowerCase().contains(lowerSearch));
                        boolean matchesDescription = product != null
                                && product.getDescription().toLowerCase().contains(lowerSearch);

                        if (!matchesBarcode && !matchesDescription) {
                            continue;
                        }
                    }

                    productGrid.getChildren().add(createProductTile(detail));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private javafx.scene.Node createProductTile(ProductDetail detail) {
        VBox tile = new VBox(12);
        tile.getStyleClass().add("touch-tile");
        tile.setPrefSize(180, 240);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(130);
        imageView.setFitHeight(130);
        imageView.setPreserveRatio(true);

        if (detail.getImage() != null && !detail.getImage().isEmpty()) {
            try {
                String base64Image = detail.getImage();
                if (base64Image.contains(",")) {
                    base64Image = base64Image.split(",")[1];
                }
                byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                imageView.setImage(new Image(new ByteArrayInputStream(imageBytes)));
            } catch (Exception e) {
                // Default image or empty
            }
        }

        StackPane imgContainer = new StackPane(imageView);
        imgContainer.setPrefSize(140, 140);
        imgContainer.setMaxSize(140, 140);
        imgContainer.getStyleClass().add("touch-tile-img");
        imgContainer.setAlignment(Pos.CENTER);

        Product product = productService.findProduct(detail.getProductCode());
        Label lblName = new Label(product != null ? product.getDescription() : "Product");
        lblName.getStyleClass().add("touch-tile-title");
        lblName.setWrapText(true);
        lblName.setMaxWidth(160);

        Label lblPrice = new Label(String.format("%.2f /=", detail.getSellingPrice()));
        lblPrice.getStyleClass().add("touch-tile-price");

        tile.getChildren().addAll(imgContainer, lblName, lblPrice);

        tile.setOnMouseClicked(event -> {
            // Programmatically "scan" this product
            isUpdatingBarcodeProgrammatically = true;
            txtBarcode.setText(detail.getCode());
            isUpdatingBarcodeProgrammatically = false;

            // Fill hidden fields for existing logic
            txtDescription.setText(product != null ? product.getDescription() : "N/A");
            txtSellingPrice.setText(String.valueOf(detail.getSellingPrice()));
            txtQtyOnHand.setText(String.valueOf(detail.getQtyOnHand()));
            txtDiscount.setText("0.0");
            currentShowPrice = detail.getShowPrice();

            // Add to cart with default qty 1
            addToCartInternal(detail.getCode(), product != null ? product.getDescription() : "N/A",
                    detail.getSellingPrice(), 1.0, 0.0, detail.getShowPrice());
        });

        return tile;
    }

    public void btnBackToHomeOnAction(ActionEvent actionEvent) {
        btnHomeOnAction(actionEvent);
    }

    public void btnAddNewCustomerOnAction(ActionEvent actionEvent) throws IOException {
        setUi("CustomerForm", false);
    }

    public void btnAddNewProductOnAction(ActionEvent actionEvent) throws IOException {
        setUi("ProductMainForm", false);
    }

    private void setUi(String url, boolean state) throws IOException {
        Stage stage = new Stage();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/devstack/pos/view/" + url + ".fxml"));
        loader.setControllerFactory(com.devstack.pos.PosApplication.getApplicationContext()::getBean);
        stage.setScene(new Scene(loader.load()));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(url);
        stage.centerOnScreen();
        stage.show();
    }

    private final ContextMenu customerPopup = new ContextMenu();
    private boolean isSelectingCustomerProgrammatically = false;

    private void setupCustomerAutocomplete() {
        customerPopup.setAutoHide(true);

        txtContact.textProperty().addListener((observable, oldValue, newValue) -> {
            if (isSelectingCustomerProgrammatically) {
                return;
            }
            if (newValue == null || newValue.trim().length() < 2) {
                customerPopup.hide();
                if (newValue == null || newValue.trim().isEmpty()) {
                    selectedCustomerId = null;
                    txtName.setText("Walk-in Customer");
                    availableLoyaltyPoints = 0.0;
                    txtCustomerLoyaltyPoints.setText("Aval: 0.0");
                }
                return;
            }

            String query = newValue.trim();
            List<Customer> matches = customerService.searchCustomers(query);
            if (matches == null || matches.isEmpty()) {
                customerPopup.hide();
                return;
            }

            customerPopup.getItems().clear();
            int count = 0;
            for (Customer c : matches) {
                if (count++ >= 6) break;

                String phone = (c.getContact() != null) ? c.getContact() : "";
                String name = (c.getName() != null) ? c.getName() : "Unknown";
                double pts = c.getLoyaltyPoints();

                Label itemLabel = new Label(String.format("📞 %s  —  %s  (%.1f Pts)", phone, name, pts));
                itemLabel.setStyle("-fx-font-size: 14px; -fx-padding: 8 16; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

                CustomMenuItem item = new CustomMenuItem(itemLabel, true);
                item.setOnAction(e -> selectCustomer(c));
                customerPopup.getItems().add(item);
            }

            if (!customerPopup.getItems().isEmpty() && txtContact.getScene() != null && txtContact.getScene().getWindow() != null) {
                Bounds bounds = txtContact.localToScreen(txtContact.getBoundsInLocal());
                if (bounds != null) {
                    customerPopup.show(txtContact, bounds.getMinX(), bounds.getMaxY());
                }
            } else {
                customerPopup.hide();
            }
        });

        txtContact.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                PauseTransition pause = new PauseTransition(Duration.millis(250));
                pause.setOnFinished(e -> customerPopup.hide());
                pause.play();
            }
        });
    }

    private void selectCustomer(Customer customer) {
        if (customer == null) return;
        isSelectingCustomerProgrammatically = true;
        try {
            selectedCustomerId = customer.getId();
            txtContact.setText(customer.getContact() != null ? customer.getContact() : "");
            txtName.setText(customer.getName() != null ? customer.getName() : "Customer");
            availableLoyaltyPoints = customer.getLoyaltyPoints();
            txtCustomerLoyaltyPoints.setText(String.format("Aval: %.1f", availableLoyaltyPoints));
            customerPopup.hide();
            txtBarcode.requestFocus();
        } finally {
            isSelectingCustomerProgrammatically = false;
        }
    }

    public void searchCustomer(ActionEvent actionEvent) {
        try {
            Customer customer = customerService.findByContact(txtContact.getText().trim());
            if (customer != null) {
                selectCustomer(customer);
            } else {
                new Alert(Alert.AlertType.WARNING, "Customer not found (Press Enter to search again or Add New)")
                        .show();
                selectedCustomerId = null;
                availableLoyaltyPoints = 0.0;
                txtCustomerLoyaltyPoints.setText("Aval: 0");
                txtName.setText("");
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Search Error: " + e.getMessage()).show();
        }
    }

    private void setupBarcodeScannerDetection() {
        txtBarcode.textProperty().addListener((observable, oldValue, newValue) -> {
            if (isUpdatingBarcodeProgrammatically)
                return;

            // Always filter the grid based on search/barcode input
            loadAllProducts(activeCategoryId, newValue);

            // Auto-load product if exact barcode match of standard length (scanner
            // behavior)
            if (newValue != null && (newValue.length() >= 13 || newValue.length() == 8)) {
                loadProduct(null);
            }
        });
    }

    public void loadProduct(ActionEvent actionEvent) {
        String barcode = txtBarcode.getText().trim();
        if (barcode.isEmpty())
            return;
        try {
            ProductDetail productDetail = productDetailService.findByCodeWithProduct(barcode);

            // Fallback: If not found by batch code/barcode, try searching by product code
            // (integer)
            if (productDetail == null && barcode.matches("\\d+")) {
                try {
                    int pCode = Integer.parseInt(barcode);
                    List<ProductDetail> activeBatches = productDetailService.findActiveBatchesByProductCode(pCode);
                    if (!activeBatches.isEmpty()) {
                        productDetail = activeBatches.get(0);
                    }
                } catch (NumberFormatException e) {
                    // Not a valid integer, ignore fallback
                }
            }

            if (productDetail != null) {
                if (productDetail.isExpired()) {
                    new Alert(Alert.AlertType.WARNING, "Cannot sell from expired batch: " + productDetail.getCode())
                            .show();
                    return;
                }
                Product product = productService.findProduct(productDetail.getProductCode());
                txtDescription.setText(product != null ? product.getDescription() : "N/A");
                txtSellingPrice.setText(String.valueOf(productDetail.getSellingPrice()));
                txtQtyOnHand.setText(String.valueOf(productDetail.getQtyOnHand()));
                txtDiscount.setText("0.0");
                currentShowPrice = productDetail.getShowPrice();

                if (productDetail.getImage() != null && !productDetail.getImage().isEmpty()) {
                    try {
                        String base64Image = productDetail.getImage();
                        if (base64Image.contains(",")) {
                            base64Image = base64Image.split(",")[1];
                        }
                        byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                        imgProduct.setImage(new Image(new ByteArrayInputStream(imageBytes)));
                    } catch (Exception e) {
                        System.err.println("Image loading failed: " + e.getMessage());
                        imgProduct.setImage(null);
                    }
                } else {
                    imgProduct.setImage(null);
                }

                txtQty.requestFocus();
            } else {
                imgProduct.setImage(null);
                // Try searching by description if barcode/code not found
                List<ProductDetail> allProducts = productDetailService.findAllProductDetails();
                for (ProductDetail detail : allProducts) {
                    Product p = productService.findProduct(detail.getProductCode());
                    if (p != null
                            && p.getDescription().toLowerCase().trim().equalsIgnoreCase(barcode.toLowerCase().trim())) {
                        if (detail.getQtyOnHand() > 0 && !detail.isExpired()) {
                            // Found a matching product description with an active batch!
                            isUpdatingBarcodeProgrammatically = true;
                            txtBarcode.setText(detail.getCode());
                            isUpdatingBarcodeProgrammatically = false;

                            txtDescription.setText(p.getDescription());
                            txtSellingPrice.setText(String.valueOf(detail.getSellingPrice()));
                            txtQtyOnHand.setText(String.valueOf(detail.getQtyOnHand()));
                            txtDiscount.setText("0.0");
                            currentShowPrice = detail.getShowPrice();

                            if (detail.getImage() != null && !detail.getImage().isEmpty()) {
                                try {
                                    String base64Image = detail.getImage();
                                    if (base64Image.contains(",")) {
                                        base64Image = base64Image.split(",")[1];
                                    }
                                    byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                                    imgProduct.setImage(new Image(new ByteArrayInputStream(imageBytes)));
                                } catch (Exception e) {
                                }
                            }

                            txtQty.requestFocus();
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Load error: " + e.getMessage()).show();
        }
    }

    private final ObservableList<CartTm> tms = FXCollections.observableArrayList();

    public void addToCart(ActionEvent actionEvent) {
        try {
            double qty = Double.parseDouble(txtQty.getText());
            double sellingPrice = Double.parseDouble(txtSellingPrice.getText());
            double qtyOnHand = Double.parseDouble(txtQtyOnHand.getText());
            double discount = txtDiscount.getText().isEmpty() ? 0.0 : Double.parseDouble(txtDiscount.getText());

            double currentCartQty = 0;
            int n = isExists(txtBarcode.getText());
            if (n != -1) {
                currentCartQty = tms.get(n).getQty();
            }

            if (qtyOnHand < (qty + currentCartQty)) {
                new Alert(Alert.AlertType.WARNING,
                        "Insufficient stock. Available: " + qtyOnHand + ", In Cart: " + currentCartQty).show();
                return;
            }

            addToCartInternal(txtBarcode.getText(), txtDescription.getText(), sellingPrice, qty, discount,
                    currentShowPrice);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Number format error").show();
        }
    }

    private void addToCartInternal(String barcode, String description, double sellingPrice, double qty, double discount,
            double showPrice) {
        // Disable print button when starting a new order (first item added to empty cart) - matches Kumara
        if (btnPrint != null && tms.isEmpty()) {
            btnPrint.setDisable(true);
            lastCompletedOrderId = null;
        }
        int n = isExists(barcode);
        if (n == -1) {
            Button btn = new Button("Remove");
            CartTm tm = new CartTm(barcode, description, discount, sellingPrice, showPrice, qty,
                    (sellingPrice - discount) * qty, btn);
            btn.setOnAction(e -> {
                tms.remove(tm);
                setTotal();
            });
            tms.add(tm);
        } else {
            CartTm tm = tms.get(n);
            double oldDiscountAmount = tm.getDiscount() * tm.getQty();
            double newDiscountAmount = discount * qty;
            double newQuantity = tm.getQty() + qty;
            tm.setDiscount((oldDiscountAmount + newDiscountAmount) / newQuantity);
            tm.setQty(newQuantity);
            tm.setTotalCost((tm.getSellingPrice() - tm.getDiscount()) * newQuantity);
        }
        tblCart.setItems(tms);
        tblCart.refresh();
        setTotal();
        clear();
        txtBarcode.requestFocus();
    }

    public void btnClearOnAction(ActionEvent actionEvent) {
        clear();
    }

    private void clear() {
        txtBarcode.clear();
        txtDescription.clear();
        txtSellingPrice.clear();
        txtQtyOnHand.clear();
        txtQty.clear();
        txtDiscount.clear();
        currentShowPrice = 0.0;
        imgProduct.setImage(null);
        txtBarcode.requestFocus();
    }

    private int isExists(String code) {
        for (int i = 0; i < tms.size(); i++) {
            if (tms.get(i).getCode().equals(code))
                return i;
        }
        return -1;
    }

    private void setTotal() {
        double total = 0;
        for (CartTm tm : tms)
            total += tm.getTotalCost();
        txtTotal.setText(String.format("%.2f /=", total));
        calculateBalance();
    }

    public void calculateBalance(javafx.scene.input.KeyEvent keyEvent) {
        calculateBalance();
    }

    private void calculateBalance() {
        try {
            double total = 0;
            for (CartTm tm : tms)
                total += tm.getTotalCost();
            double pointsToUse = txtPointsToUse.getText().isEmpty() ? 0.0
                    : Double.parseDouble(txtPointsToUse.getText());
            if (pointsToUse > availableLoyaltyPoints) {
                pointsToUse = availableLoyaltyPoints;
                txtPointsToUse.setText(String.valueOf(pointsToUse));
            }
            double discountPercentage = txtBillDiscount.getText().isEmpty() ? 0.0
                    : Double.parseDouble(txtBillDiscount.getText());

            double discountAmount = total * (discountPercentage / 100.0);
            double finalTotal = total - pointsToUse - discountAmount;
            if (finalTotal < 0) {
                finalTotal = 0;
            }
            txtTotal.setText(String.format("%.2f /=", finalTotal));

            // Calculate points to earn
            double rate = 100.0;
            try {
                SystemSettings settings = systemSettingsService.getSystemSettings();
                if (settings != null && settings.getLoyaltyPointsRate() != null
                        && settings.getLoyaltyPointsRate() > 0) {
                    rate = settings.getLoyaltyPointsRate();
                }
            } catch (Exception e) {
            }
            double pointsEarned = finalTotal / rate;
            txtPointsToEarn.setText(String.format("+%.1f", pointsEarned));

            double paid = txtCustomerPaid.getText().isEmpty() ? 0.0 : Double.parseDouble(txtCustomerPaid.getText());
            txtBalance.setText(String.format("%.2f /=", (paid - finalTotal)));
        } catch (Exception e) {
            txtBalance.setText("Error");
        }
    }

    public void btnCompleteOrder(ActionEvent actionEvent) {
        if (tms.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Cart is empty").show();
            return;
        }

        try {
            double totalCost = tms.stream().mapToDouble(CartTm::getTotalCost).sum();
            double pointsToUse = txtPointsToUse.getText().isEmpty() ? 0.0
                    : Double.parseDouble(txtPointsToUse.getText());

            if (pointsToUse > availableLoyaltyPoints) {
                new Alert(Alert.AlertType.WARNING, "Not enough loyalty points available.").show();
                return;
            }

            double discountPercentageValue = txtBillDiscount.getText().isEmpty() ? 0.0
                    : Double.parseDouble(txtBillDiscount.getText());

            double billDiscountAmount = totalCost * (discountPercentageValue / 100.0);
            double finalTotal = totalCost - pointsToUse - billDiscountAmount;
            if (finalTotal < 0)
                finalTotal = 0;

            String paymentMethod = cmbPaymentMethod.getValue();
            String paidText = txtCustomerPaid.getText().trim();

            // 1. Validate Payment Amount for Cash/Card
            if ("CASH".equalsIgnoreCase(paymentMethod) || "CARD".equalsIgnoreCase(paymentMethod)) {
                if (paidText.isEmpty()) {
                    new Alert(Alert.AlertType.WARNING, "Please enter the amount paid by the customer.").show();
                    txtCustomerPaid.requestFocus();
                    return;
                }
                try {
                    double paidAmount = Double.parseDouble(paidText);
                    if (paidAmount < finalTotal) {
                        new Alert(Alert.AlertType.WARNING,
                                "Insufficient payment. Amount must be at least " + String.format("%.2f", finalTotal))
                                .show();
                        txtCustomerPaid.requestFocus();
                        return;
                    }
                } catch (NumberFormatException e) {
                    new Alert(Alert.AlertType.ERROR, "Invalid payment amount entered.").show();
                    txtCustomerPaid.requestFocus();
                    return;
                }
            }

            double orderTotalDiscount = tms.stream().mapToDouble(tm -> tm.getDiscount() * tm.getQty()).sum()
                    + billDiscountAmount;

            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setBusinessId(TerminalConfigManager.getBusinessId());
            orderDetail.setTerminalId(TerminalConfigManager.getTerminalId());
            orderDetail.setIssuedDate(LocalDateTime.now());
            orderDetail.setDiscount(orderTotalDiscount);
            orderDetail.setTotalCost(finalTotal);
            orderDetail.setCustomerId(selectedCustomerId);
            orderDetail.setCustomerName(txtName.getText());
            orderDetail.setOperatorEmail(UserSessionData.email);
            orderDetail.setPaymentMethod(cmbPaymentMethod.getValue());
            if ("CREDIT".equalsIgnoreCase(cmbPaymentMethod.getValue())
                    || "CHEQUE".equalsIgnoreCase(cmbPaymentMethod.getValue())) {
                orderDetail.setPaymentStatus("PENDING");
            } else {
                orderDetail.setPaymentStatus("PAID");
            }
            orderDetail.setLoyaltyPointsUsed(pointsToUse);

            // Earn points based on configurable rate from admin side
            double rate = 100.0; // Default: 1 point per 100
            try {
                SystemSettings settings = systemSettingsService.getSystemSettings();
                if (settings != null && settings.getLoyaltyPointsRate() != null
                        && settings.getLoyaltyPointsRate() > 0) {
                    rate = settings.getLoyaltyPointsRate();
                }
            } catch (Exception e) {
                // Keep default
            }
            double pointsEarned = finalTotal / rate;
            orderDetail.setLoyaltyPointsEarned(pointsEarned);

            double totalPaid = txtCustomerPaid.getText().isEmpty() ? 0.0
                    : Double.parseDouble(txtCustomerPaid.getText());
            orderDetail.setCustomerPaid(totalPaid);
            orderDetail.setBalance(totalPaid - finalTotal);

            OrderDetail savedOrder = orderDetailService.saveOrderDetail(orderDetail);

            // Update customer loyalty points if customer is selected
            Double totalCustomerPoints = null;
            if (selectedCustomerId != null) {
                Customer customer = customerService.findCustomer(selectedCustomerId);
                if (customer != null) {
                    double newPoints = customer.getLoyaltyPoints() - pointsToUse + pointsEarned;
                    customer.setLoyaltyPoints(newPoints);
                    customerService.updateCustomer(customer);
                    totalCustomerPoints = newPoints;
                }
            }

            List<OrderItem> orderItems = new ArrayList<>();
            for (CartTm tm : tms) {
                ProductDetail productDetail = productDetailService.findProductDetailByCode(tm.getCode());
                OrderItem orderItem = OrderItem.builder()
                        .orderId(savedOrder.getCode())
                        .productCode(productDetail != null ? productDetail.getProductCode() : 0)
                        .productName(tm.getDescription())
                        .batchCode(tm.getCode())
                        .batchNumber(productDetail != null ? productDetail.getBatchNumber() : null)
                        .quantity(tm.getQty())
                        .unitPrice(tm.getSellingPrice())
                        .discountPerUnit(tm.getDiscount())
                        .totalDiscount(tm.getDiscount() * tm.getQty())
                        .lineTotal(tm.getTotalCost())
                        .build();
                orderItems.add(orderItem);
            }
            orderItemService.saveAllOrderItems(orderItems);

            for (CartTm tm : tms) {
                productDetailService.reduceStock(tm.getCode(), tm.getQty());
            }

            // Store the completed order ID so the Print button can use it - matches Kumara Enterprises
            lastCompletedOrderId = savedOrder.getCode();

            // Dispatch customer SMS receipt via Notify.lk asynchronously (non-blocking)
            String customerContact = null;
            String customerName = txtName.getText();
            if (selectedCustomerId != null) {
                Customer c = customerService.findCustomer(selectedCustomerId);
                if (c != null) {
                    customerContact = c.getContact();
                    if (c.getName() != null && !c.getName().isBlank()) {
                        customerName = c.getName();
                    }
                }
            }
            if ((customerContact == null || customerContact.isBlank()) && txtContact != null) {
                customerContact = txtContact.getText();
            }

            if (customerContact != null && !customerContact.isBlank()) {
                notifyLkSmsService.sendOrderReceiptSmsAsync(
                        customerContact,
                        customerName,
                        savedOrder.getCode(),
                        savedOrder.getTotalCost(),
                        pointsEarned,
                        totalCustomerPoints,
                        TerminalConfigManager.getBusinessName(),
                        TerminalConfigManager.getBusinessId()
                );
            }

            // Generate PDF for record keeping (no automatic printing - matches Kumara Enterprises)
            try {
                String receiptPath = pdfReportService.generateBillReceipt(savedOrder.getCode());
                new Alert(Alert.AlertType.CONFIRMATION,
                        "Order Completed Successfully!\nPDF saved to: " + receiptPath +
                                "\nClick Print button to print receipt.").show();
            } catch (Exception receiptEx) {
                receiptEx.printStackTrace();
                new Alert(Alert.AlertType.WARNING,
                        "Order completed but PDF generation failed: " + receiptEx.getMessage()).show();
            }

            // Enable print button after order is completed - matches Kumara Enterprises
            if (btnPrint != null) {
                btnPrint.setDisable(false);
            }

            // Clear cart and reset UI
            javafx.application.Platform.runLater(() -> {
                try {
                    clearFields();
                    tms.clear();
                    tblCart.setItems(tms);
                    tblCart.refresh();
                    setTotal();
                    tblCart.requestLayout();
                } catch (Exception uiEx) {
                    uiEx.printStackTrace();
                    try {
                        tms.clear();
                        if (tblCart != null) {
                            tblCart.setItems(tms);
                            tblCart.refresh();
                        }
                        if (txtTotal != null) txtTotal.setText("0.00 /=");
                        if (txtBalance != null) txtBalance.setText("0.00 /=");
                    } catch (Exception resetEx) {
                        resetEx.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            // Clear cart even on exception to prevent inconsistent state - matches Kumara Enterprises
            javafx.application.Platform.runLater(() -> {
                try {
                    tms.clear();
                    if (tblCart != null) {
                        tblCart.setItems(tms);
                        tblCart.refresh();
                        tblCart.requestLayout();
                    }
                    setTotal();
                } catch (Exception clearEx) {
                    clearEx.printStackTrace();
                    try {
                        if (txtTotal != null) txtTotal.setText("0.00 /=");
                        if (txtBalance != null) txtBalance.setText("0.00 /=");
                    } catch (Exception finalEx) {
                        finalEx.printStackTrace();
                    }
                }
            });
            new Alert(Alert.AlertType.WARNING, "Error completing order: " + e.getMessage()).show();
        }
    }

    public void btnPrintReceipt(ActionEvent actionEvent) {
        try {
            if (lastCompletedOrderId == null) {
                new Alert(Alert.AlertType.WARNING, "No order to print! Please complete an order first.").show();
                return;
            }

            // Generate plain text receipt for thermal printer
            String receiptText = pdfReportService.generatePlainTextReceipt(lastCompletedOrderId);

            // Print receipt directly to thermal printer using ESC/POS commands
            boolean printed = receiptPrinter.printRawText(receiptText);

            if (printed) {
                new Alert(Alert.AlertType.INFORMATION, "Receipt printed successfully!").show();
            } else {
                new Alert(Alert.AlertType.WARNING, "Printing failed. Please check printer connection.").show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error printing receipt: " + e.getMessage()).show();
        }
    }

    private void clearFields() {
        customerPopup.hide();
        txtContact.clear();
        txtName.setText("");
        txtBarcode.clear();
        txtDescription.clear();
        txtSellingPrice.clear();
        txtQtyOnHand.clear();
        txtQty.clear();
        txtDiscount.clear();
        txtCustomerPaid.clear();
        txtBalance.setText("0.00 /=");
        txtTotal.setText("0.00 /=");
        currentShowPrice = 0.0;
        selectedCustomerId = null;
        availableLoyaltyPoints = 0.0;
        txtPointsToUse.clear();
        txtBillDiscount.clear();
        txtPointsToEarn.setText("+0.0");
        txtCustomerLoyaltyPoints.setText("Aval: 0");
    }

}
