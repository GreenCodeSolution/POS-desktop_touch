package com.devstack.pos.controller;

import com.devstack.pos.util.AuthorizationUtil;
import com.devstack.pos.util.UserSessionData;
import com.jfoenix.controls.JFXButton;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
public class MainFormController implements Initializable {

    @FXML
    public AnchorPane mainContext;

    @FXML
    public StackPane contentPane;

    @FXML
    private Text txtUserEmail;

    @FXML
    private Text txtUserRole;

    @FXML
    private JFXButton btnCustomer;
    @FXML
    private JFXButton btnProduct;
    @FXML
    private JFXButton btnPlaceOrder;
    @FXML
    private JFXButton btnPendingPayments;
    @FXML
    private JFXButton btnReturns;
    @FXML
    private JFXButton btnOrderDetails;
    @FXML
    private JFXButton btnHelp;

    @FXML
    private ImageView imgLogo;
    @FXML
    private Label lblBusinessName;

    private final com.devstack.pos.service.SystemSettingsService systemSettingsService;

    private List<JFXButton> sidebarButtons;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        sidebarButtons = Arrays.asList(
                btnCustomer, btnProduct, btnPlaceOrder,
                btnPendingPayments, btnReturns, btnOrderDetails,
                btnHelp);
        setUserData();
        loadSystemSettings();
        systemSettingsService.addSettingsUpdateListener(this::loadSystemSettings);

        // Configure sidebar visibility based on role
        configureSidebarVisibility();

        // Load POS / Orders as default for everyone in desktop view
        navigateTo("PlaceOrderForm", btnPlaceOrder);
    }

    private void configureSidebarVisibility() {
        // All requested buttons are visible to all authenticated users in desktop view
        setButtonVisibility(btnCustomer, true);
        setButtonVisibility(btnProduct, true);
        setButtonVisibility(btnPlaceOrder, true);
        setButtonVisibility(btnPendingPayments, true);
        setButtonVisibility(btnReturns, true);
        setButtonVisibility(btnOrderDetails, true);
        setButtonVisibility(btnHelp, true);
    }

    private void setButtonVisibility(JFXButton button, boolean visible) {
        if (button != null) {
            button.setVisible(visible);
            button.setManaged(visible);
        }
    }

    private void loadSystemSettings() {
        try {
            com.devstack.pos.entity.SystemSettings settings = systemSettingsService.getSystemSettings();
            javafx.scene.image.Image logoImage = null;

            if (settings != null) {
                if (settings.getBusinessName() != null && !settings.getBusinessName().isEmpty()) {
                    if (lblBusinessName != null)
                        lblBusinessName.setText(settings.getBusinessName());
                }

                if (settings.getLogoPath() != null && !settings.getLogoPath().isEmpty()) {
                    try {
                        java.io.File file = new java.io.File(settings.getLogoPath());
                        if (file.exists()) {
                            logoImage = new javafx.scene.image.Image(file.toURI().toString());
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to load custom logo: " + e.getMessage());
                    }
                }
            }

            // Fallback to default logo if no custom logo loaded
            if (logoImage == null) {
                try {
                    // Load from resources
                    URL resource = getClass().getResource("/com/devstack/pos/view/assets/default_logo.png");
                    if (resource != null) {
                        logoImage = new javafx.scene.image.Image(resource.toExternalForm());
                    } else {
                        System.err.println("Default logo not found in resources");
                    }
                } catch (Exception e) {
                    System.err.println("Failed to load default logo: " + e.getMessage());
                }
            }

            if (imgLogo != null && logoImage != null) {
                imgLogo.setImage(logoImage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setUserData() {
        if (txtUserEmail != null) {
            txtUserEmail.setText(UserSessionData.email);
        }
        if (txtUserRole != null) {
            String displayRoles = UserSessionData.userRoles.stream()
                    .map(role -> role.replace("ROLE_", ""))
                    .collect(java.util.stream.Collectors.joining(", "));
            txtUserRole.setText("ROLES: " + displayRoles);
        }
    }

    @FXML
    public void btnCustomerOnAction(ActionEvent event) {
        if (AuthorizationUtil.canAccessCustomers()) {
            navigateTo("CustomerForm", btnCustomer);
        } else {
            AuthorizationUtil.showUnauthorizedAlert();
        }
    }

    @FXML
    public void btnProductOnAction(ActionEvent event) {
        if (AuthorizationUtil.canAccessProducts()) {
            navigateTo("ProductMainForm", btnProduct);
        } else {
            AuthorizationUtil.showAdminOnlyAlert();
        }
    }

    @FXML
    public void btnPlaceOrderOnAction(ActionEvent event) {
        if (AuthorizationUtil.canAccessPOSOrders()) {
            navigateTo("PlaceOrderForm", btnPlaceOrder);
        } else {
            AuthorizationUtil.showUnauthorizedAlert();
        }
    }

    @FXML
    public void btnPendingPaymentsOnAction(ActionEvent event) {
        if (AuthorizationUtil.canAccessPOSOrders()) {
            navigateTo("PendingPaymentsForm", btnPendingPayments);
        } else {
            AuthorizationUtil.showUnauthorizedAlert();
        }
    }

    @FXML
    public void btnOrderDetailsOnAction(ActionEvent event) {
        navigateTo("OrderDetailsForm", btnOrderDetails);
    }

    @FXML
    public void btnReturnsOnAction(ActionEvent event) {
        if (AuthorizationUtil.canAccessReturnOrders()) {
            navigateTo("ReturnOrdersForm", btnReturns);
        } else {
            AuthorizationUtil.showUnauthorizedAlert();
        }
    }

    @FXML
    public void btnHelpOnAction(ActionEvent event) {
        navigateTo("HelpPage", btnHelp);
    }

    private void navigateTo(String viewName, JFXButton activeBtn) {
        try {
            UserSessionData.updateLastActivity();
            updateSidebarStyles(activeBtn);

            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/devstack/pos/view/" + viewName + ".fxml"));
            loader.setControllerFactory(com.devstack.pos.PosApplication.getApplicationContext()::getBean);

            Parent root = loader.load();
            contentPane.getChildren().clear();
            contentPane.getChildren().add(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateSidebarStyles(JFXButton activeBtn) {
        if (sidebarButtons == null || activeBtn == null)
            return;

        for (JFXButton btn : sidebarButtons) {
            if (btn == null)
                continue;
            btn.getStyleClass().remove("sidebar-btn-active-light");
            btn.getStyleClass().remove("sidebar-btn-light");

            if (btn == activeBtn) {
                btn.getStyleClass().add("sidebar-btn-active-light");
            } else {
                btn.getStyleClass().add("sidebar-btn-light");
            }
        }
    }

}
