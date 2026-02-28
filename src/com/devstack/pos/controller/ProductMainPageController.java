package com.devstack.pos.controller;

import com.devstack.pos.entity.Category;
import com.devstack.pos.entity.Product;
import com.devstack.pos.entity.ProductDetail;
import com.devstack.pos.service.CategoryService;
import com.devstack.pos.service.ProductDetailService;
import com.devstack.pos.service.ProductService;
import com.devstack.pos.service.SupplierService;
import com.devstack.pos.view.tm.ProductDetailTm;
import com.devstack.pos.view.tm.ProductTm;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductMainPageController extends BaseController {
    private final ProductService productService;
    private final ProductDetailService productDetailService;
    private final CategoryService categoryService;
    private final SupplierService supplierService;

    public TextArea txtProductDescription;
    public Button btnSaveUpdate;
    public ComboBox<String> cmbCategory;
    public TableView<ProductTm> tbl;
    public TableColumn colProductId;
    public TableColumn colProductDesc;
    public TableColumn colProductCategory;

    public TextField txtSelectedProdId;
    public TextArea txtSelectedProdDescription;
    public Button btnNewBatch;
    public ScrollPane scrollPaneBatchDetails;
    public TableView<ProductDetailTm> tblDetail;
    public TableColumn colPDId;
    public TableColumn colPDInitialQty;
    public TableColumn colPDQty;
    public TableColumn colPDSellingQty;
    public TableColumn colPDSellingPrice;
    public TableColumn colPDBuyingPrice;
    public TableColumn colPDSupplierName;

    public com.jfoenix.controls.JFXTextField txtSearchProducts;

    private final String searchText = "";
    private Integer currentProductCode = null;
    private FilteredList<ProductTm> filteredProducts;
    private ObservableList<ProductTm> allProducts;
    private boolean showLowStockOnly = false;
    private boolean isSettingCategoryProgrammatically = false;

    public void setShowLowStockOnly(boolean showLowStockOnly) {
        this.showLowStockOnly = showLowStockOnly;
        loadAllProducts(searchText);
    }

    public void initialize() {
        showLowStockOnly = false;
        initializeSidebar();

        tbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        if (tblDetail != null) {
            tblDetail.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        }

        if (scrollPaneBatchDetails != null) {
            scrollPaneBatchDetails.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPaneBatchDetails.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPaneBatchDetails.setStyle("-fx-background-color: transparent;");
        }

        colProductId.setCellValueFactory(new PropertyValueFactory<>("code"));
        colProductDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colProductCategory.setCellValueFactory(new PropertyValueFactory<>("category"));

        if (tblDetail != null) {
            colPDId.setCellValueFactory(new PropertyValueFactory<>("code"));
            if (colPDInitialQty != null) {
                colPDInitialQty.setCellValueFactory(new PropertyValueFactory<>("initialQty"));
                colPDInitialQty.setCellFactory(column -> new TableCell<ProductDetailTm, Double>() {
                    @Override
                    protected void updateItem(Double initialQty, boolean empty) {
                        super.updateItem(initialQty, empty);
                        if (empty || initialQty == null)
                            setText(null);
                        else
                            setText(initialQty == initialQty.intValue() ? String.valueOf(initialQty.intValue())
                                    : String.format("%.2f", initialQty));
                    }
                });
            }
            colPDQty.setCellValueFactory(new PropertyValueFactory<>("qty"));
            colPDQty.setCellFactory(column -> new TableCell<ProductDetailTm, Double>() {
                @Override
                protected void updateItem(Double qty, boolean empty) {
                    super.updateItem(qty, empty);
                    if (empty || qty == null)
                        setText(null);
                    else
                        setText(qty == qty.intValue() ? String.valueOf(qty.intValue()) : String.format("%.2f", qty));
                }
            });
            if (colPDSellingQty != null) {
                colPDSellingQty.setCellValueFactory(new PropertyValueFactory<>("sellingQty"));
                colPDSellingQty.setCellFactory(column -> new TableCell<ProductDetailTm, Double>() {
                    @Override
                    protected void updateItem(Double sellingQty, boolean empty) {
                        super.updateItem(sellingQty, empty);
                        if (empty || sellingQty == null)
                            setText(null);
                        else
                            setText(sellingQty == sellingQty.intValue() ? String.valueOf(sellingQty.intValue())
                                    : String.format("%.2f", sellingQty));
                    }
                });
            }
            colPDSellingPrice.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
            colPDBuyingPrice.setCellValueFactory(new PropertyValueFactory<>("buyingPrice"));
            if (colPDSupplierName != null) {
                colPDSupplierName.setCellValueFactory(new PropertyValueFactory<>("supplierName"));
            }
        }

        loadCategories();
        loadAllProducts(searchText);

        if (txtSearchProducts != null) {
            txtSearchProducts.textProperty().addListener((observable, oldValue, newValue) -> filterProducts(newValue));
        }

        if (cmbCategory != null) {
            cmbCategory.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (currentProductCode != null && newValue != null && !newValue.isEmpty()
                        && !isSettingCategoryProgrammatically) {
                    clearProductForm();
                }
            });
        }

        tbl.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null)
                setData(newValue);
        });

        if (tblDetail != null) {
            tblDetail.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                try {
                    if (newValue != null)
                        loadExternalUi(true, newValue);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        javafx.application.Platform.runLater(() -> {
            hideTableScrollBars(tbl);
            if (tblDetail != null)
                hideTableScrollBars(tblDetail);
            if (scrollPaneBatchDetails != null)
                hideScrollPaneScrollBars(scrollPaneBatchDetails);
        });
    }

    private void hideScrollPaneScrollBars(ScrollPane scrollPane) {
        if (scrollPane == null)
            return;
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                javafx.application.Platform.runLater(() -> {
                    hideScrollBarsInScrollPane(scrollPane);
                    newScene.addPostLayoutPulseListener(() -> hideScrollBarsInScrollPane(scrollPane));
                });
            }
        });
        if (scrollPane.getScene() != null)
            javafx.application.Platform.runLater(() -> hideScrollBarsInScrollPane(scrollPane));
    }

    private void hideScrollBarsInScrollPane(ScrollPane scrollPane) {
        if (scrollPane == null || scrollPane.getScene() == null)
            return;
        ScrollBar vScrollBar = (ScrollBar) scrollPane.lookup(".scroll-bar:vertical");
        ScrollBar hScrollBar = (ScrollBar) scrollPane.lookup(".scroll-bar:horizontal");
        if (vScrollBar != null) {
            vScrollBar.setVisible(false);
            vScrollBar.setManaged(false);
        }
        if (hScrollBar != null) {
            hScrollBar.setVisible(false);
            hScrollBar.setManaged(false);
        }
        hideScrollBarsRecursive(scrollPane);
    }

    private void hideTableScrollBars(TableView<?> tableView) {
        if (tableView == null)
            return;
        tableView.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                javafx.application.Platform.runLater(() -> {
                    hideScrollBarsInTable(tableView);
                    newScene.addPostLayoutPulseListener(() -> hideScrollBarsInTable(tableView));
                });
            }
        });
        if (tableView.getScene() != null)
            javafx.application.Platform.runLater(() -> hideScrollBarsInTable(tableView));
    }

    private void hideScrollBarsInTable(TableView<?> tableView) {
        if (tableView == null || tableView.getScene() == null)
            return;
        ScrollBar vScrollBar = (ScrollBar) tableView.lookup(".scroll-bar:vertical");
        ScrollBar hScrollBar = (ScrollBar) tableView.lookup(".scroll-bar:horizontal");
        if (vScrollBar != null) {
            vScrollBar.setVisible(false);
            vScrollBar.setManaged(false);
        }
        if (hScrollBar != null) {
            hScrollBar.setVisible(false);
            hScrollBar.setManaged(false);
        }
        hideScrollBarsRecursive(tableView);
    }

    private void hideScrollBarsRecursive(javafx.scene.Node node) {
        if (node instanceof ScrollBar) {
            ScrollBar scrollBar = (ScrollBar) node;
            scrollBar.setVisible(false);
            scrollBar.setManaged(false);
        }
        if (node instanceof Parent) {
            Parent parent = (Parent) node;
            for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
                hideScrollBarsRecursive(child);
            }
        }
    }

    private void loadCategories() {
        try {
            List<Category> categoryList = categoryService.findActiveCategories();
            ObservableList<String> categories = FXCollections.observableArrayList();
            for (Category category : categoryList)
                categories.add(category.getName());
            cmbCategory.setItems(categories);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected String getCurrentPageName() {
        return "Products";
    }

    private void setData(ProductTm newValue) {
        txtSelectedProdId.setText(String.valueOf(newValue.getCode()));
        txtSelectedProdDescription.setText(newValue.getDescription());
        btnNewBatch.setDisable(false);
        currentProductCode = newValue.getCode();
        try {
            Product product = productService.findProduct(newValue.getCode());
            if (product != null)
                loadExistingProduct(product);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            loadBatchData(newValue.getCode());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void btnBackToHomeOnAction(ActionEvent actionEvent) {
        btnHomeOnAction(actionEvent);
    }

    public void btnNewProductOnAction(ActionEvent actionEvent) {
        try {
            if (txtProductDescription.getText().trim().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Please enter description").show();
                return;
            }
            if (cmbCategory.getValue() == null) {
                new Alert(Alert.AlertType.WARNING, "Please select category").show();
                return;
            }

            Product product = new Product();
            product.setDescription(txtProductDescription.getText().trim());
            Category category = categoryService.findCategoryByName(cmbCategory.getValue());
            if (category != null)
                product.setCategory(category);

            if (btnSaveUpdate.getText().equals("Save Product")) {
                productService.saveProduct(product);
                new Alert(Alert.AlertType.CONFIRMATION, "Saved").show();
                clearFields();
                loadAllProducts(searchText);
            } else {
                if (currentProductCode != null) {
                    product.setCode(currentProductCode);
                    if (productService.updateProduct(product)) {
                        new Alert(Alert.AlertType.CONFIRMATION, "Updated").show();
                        clearFields();
                        loadAllProducts(searchText);
                        btnSaveUpdate.setText("Save Product");
                    }
                }
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    private void loadAllProducts(String searchText) {
        try {
            List<Product> products = productService.findAllProducts();
            if (showLowStockOnly) {
                products = products.stream().filter(product -> {
                    List<ProductDetail> batches = productDetailService.findByProductCode(product.getCode());
                    return batches.stream().anyMatch(pd -> pd.isLowStock() || pd.getQtyOnHand() <= 0);
                }).collect(java.util.stream.Collectors.toList());
            }
            ObservableList<ProductTm> tmsList = FXCollections.observableArrayList();
            for (Product product : products) {
                tmsList.add(new ProductTm(product.getCode(), product.getDescription(),
                        product.getCategory() != null ? product.getCategory().getName() : "Uncategorized"));
            }
            allProducts = tmsList;
            filteredProducts = new FilteredList<>(allProducts, p -> true);
            tbl.setItems(filteredProducts);
            if (txtSearchProducts != null && !txtSearchProducts.getText().trim().isEmpty())
                filterProducts(txtSearchProducts.getText());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearFields() {
        txtProductDescription.clear();
        cmbCategory.setValue(null);
        currentProductCode = null;
    }

    private void clearProductForm() {
        txtProductDescription.clear();
        currentProductCode = null;
        btnSaveUpdate.setText("Save Product");
        if (txtSelectedProdId != null)
            txtSelectedProdId.clear();
        if (txtSelectedProdDescription != null)
            txtSelectedProdDescription.clear();
        if (btnNewBatch != null)
            btnNewBatch.setDisable(true);
        if (tblDetail != null)
            tblDetail.setItems(FXCollections.observableArrayList());
        if (tbl != null)
            tbl.getSelectionModel().clearSelection();
    }

    public void btnAddNewOnAction(ActionEvent actionEvent) {
        clearFields();
        btnSaveUpdate.setText("Save Product");
    }

    public void newBatchOnAction(ActionEvent actionEvent) throws IOException {
        loadExternalUi(false, null);
    }

    private void loadExternalUi(boolean state, ProductDetailTm tm) throws IOException {
        if (!txtSelectedProdId.getText().isEmpty()) {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/devstack/pos/view/NewBatchForm.fxml"));
            NewBatchFormController controller = new NewBatchFormController(productDetailService, productService,
                    supplierService);
            loader.setController(controller);
            Parent parent = loader.load();
            stage.setScene(new Scene(parent));
            stage.setResizable(true);
            javafx.application.Platform.runLater(() -> {
                try {
                    controller.setDetails(Integer.parseInt(txtSelectedProdId.getText()),
                            txtSelectedProdDescription.getText(), stage, state, tm);
                    stage.setOnHidden(e -> {
                        if (currentProductCode != null)
                            loadBatchData(currentProductCode);
                    });
                    stage.show();
                    stage.centerOnScreen();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } else {
            new Alert(Alert.AlertType.WARNING, "Select one").show();
        }
    }

    private void loadBatchData(int code) {
        if (tblDetail == null)
            return;
        try {
            ObservableList<ProductDetailTm> obList = FXCollections.observableArrayList();
            List<ProductDetail> batches = productDetailService.findByProductCode(code);
            if (showLowStockOnly) {
                batches = batches.stream().filter(pd -> pd.isLowStock() || pd.getQtyOnHand() <= 0)
                        .collect(java.util.stream.Collectors.toList());
            }
            for (ProductDetail pd : batches) {
                double sellingQty = Math.max(0, pd.getInitialQty() - pd.getQtyOnHand());
                obList.add(new ProductDetailTm(pd.getCode(), pd.getQtyOnHand(), pd.getInitialQty(), sellingQty,
                        pd.getSellingPrice(), pd.getBuyingPrice(), pd.isDiscountAvailability(),
                        pd.getSupplierName() != null ? pd.getSupplierName() : "", null, null));
            }
            tblDetail.setItems(obList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void btnManageCategoriesOnAction(ActionEvent actionEvent) {
        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/devstack/pos/view/CategoryManagementForm.fxml"));
            loader.setControllerFactory(com.devstack.pos.PosApplication.getApplicationContext()::getBean);
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Category Management");
            stage.setOnHidden(e -> loadCategories());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadExistingProduct(Product product) {
        currentProductCode = product.getCode();
        txtProductDescription.setText(product.getDescription());
        if (product.getCategory() != null) {
            isSettingCategoryProgrammatically = true;
            cmbCategory.setValue(product.getCategory().getName());
            isSettingCategoryProgrammatically = false;
        }
        btnSaveUpdate.setText("Update Product");
    }

    private void filterProducts(String searchText) {
        if (filteredProducts == null)
            return;
        if (searchText == null || searchText.trim().isEmpty()) {
            filteredProducts.setPredicate(p -> true);
        } else {
            String lower = searchText.toLowerCase().trim();
            filteredProducts
                    .setPredicate(p -> (p.getDescription() != null && p.getDescription().toLowerCase().contains(lower))
                            || (p.getCategory() != null && p.getCategory().toLowerCase().contains(lower)));
        }
    }
}
