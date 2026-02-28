package com.devstack.pos.controller;

import com.devstack.pos.service.SessionManager;
import com.devstack.pos.util.AuthorizationUtil;
import com.devstack.pos.util.StageManager;
import com.devstack.pos.util.UserSessionData;
import com.jfoenix.controls.JFXButton;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;

/**
 * Base controller class with common navigation and UI functionality
 * All main screen controllers should extend this class for consistent behavior
 */
public abstract class BaseController {

    @FXML
    public AnchorPane context;

    @FXML
    protected Text txtUserEmail;

    @FXML
    protected Text txtUserRole;

    @FXML
    public JFXButton btnLogout;

    @Getter
    @Setter
    private SessionManager sessionManager;

    /**
     * Initialize user info in sidebar
     * Call this from your controller's initialize() method
     */
    protected void initializeSidebar() {
        if (txtUserEmail != null) {
            txtUserEmail.setText(UserSessionData.email);
        }
        if (txtUserRole != null) {
            String displayRoles = UserSessionData.userRoles.stream()
                    .map(role -> role.replace("ROLE_", ""))
                    .collect(java.util.stream.Collectors.joining(", "));
            txtUserRole.setText("ROLES: " + displayRoles);
        }

        // Show header logout button only for Cashier - Admin/Super Admin use sidebar
        if (btnLogout != null) {
            btnLogout.setVisible(AuthorizationUtil.isCashier());
            btnLogout.setManaged(AuthorizationUtil.isCashier());
        }

        // Setup activity tracking for mouse and keyboard events
        setupActivityTracking();
    }

    /**
     * Setup activity tracking for user interactions
     */
    private void setupActivityTracking() {
        if (context != null) {
            // Track mouse movements and clicks
            context.setOnMouseMoved(this::onUserActivity);
            context.setOnMouseClicked(this::onUserActivity);
            context.setOnMousePressed(this::onUserActivity);

            // Track keyboard events
            context.setOnKeyPressed(this::onUserActivity);
            context.setOnKeyTyped(this::onUserActivity);
        }
    }

    /**
     * Handle user activity - update last activity time
     */
    private void onUserActivity(javafx.event.Event event) {
        updateActivity();
    }

    /**
     * Update user activity timestamp
     * Can be called manually from child controllers if needed
     */
    protected void updateActivity() {
        UserSessionData.updateLastActivity();
        if (sessionManager != null) {
            sessionManager.updateActivity();
        }
    }

    /**
     * Get the name of the current page for highlighting active menu item
     * Override this in child controllers
     */
    protected abstract String getCurrentPageName();

    // ===== COMMON NAVIGATION METHODS =====

    @FXML
    public void btnHomeOnAction(ActionEvent event) {
        updateActivity();
        navigateTo("PlaceOrderForm", true);
    }

    @FXML
    public void btnCustomerOnAction(ActionEvent event) {
        updateActivity();
        if (AuthorizationUtil.canAccessCustomers()) {
            navigateTo("CustomerForm", true);
        } else {
            AuthorizationUtil.showUnauthorizedAlert();
        }
    }

    @FXML
    public void btnProductOnAction(ActionEvent event) {
        updateActivity();
        if (AuthorizationUtil.canAccessProducts()) {
            navigateTo("ProductMainForm", true);
        } else {
            AuthorizationUtil.showAdminOnlyAlert();
        }
    }

    @FXML
    public void btnPlaceOrderOnAction(ActionEvent event) {
        updateActivity();
        navigateTo("PlaceOrderForm", true);
    }

    @FXML
    public void btnPendingPaymentsOnAction(ActionEvent event) {
        updateActivity();
        navigateTo("PendingPaymentsForm", true);
    }

    @FXML
    public void btnOrderDetailsOnAction(ActionEvent event) {
        updateActivity();
        navigateTo("OrderDetailsForm", true);
    }

    @FXML
    public void btnReturnsOnAction(ActionEvent event) {
        updateActivity();
        navigateTo("ReturnOrdersForm", true);
    }

    @FXML
    public void btnHelpOnAction(ActionEvent event) {
        updateActivity();
        navigateTo("HelpPage", false);
    }

    @FXML
    public void btnLogoutOnAction(ActionEvent event) {
        try {
            // Stop session monitoring
            if (sessionManager != null) {
                sessionManager.stopSessionMonitoring();
            }

            // Clear session data
            UserSessionData.clear();
            System.out.println("User logged out");

            // Return to login screen with auth screen size
            Stage stage = (Stage) context.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/devstack/pos/view/LoginForm.fxml"));
            loader.setControllerFactory(com.devstack.pos.PosApplication.getApplicationContext()::getBean);

            Parent root = loader.load();
            StageManager.loadAuthScene(stage, root);
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to logout: " + e.getMessage()).show();
        }
    }

    // ===== HELPER METHODS =====

    /**
     * Navigate to another screen
     * 
     * @param viewName  FXML file name without extension
     * @param checkAuth Whether to check authorization
     */
    protected void navigateTo(String viewName, boolean checkAuth) {
        try {
            updateActivity(); // Track navigation as activity
            Stage stage = (Stage) context.getScene().getWindow();

            Parent currentRoot = stage.getScene().getRoot();
            boolean isInMainShell = currentRoot.getId() != null && currentRoot.getId().equals("mainContext");

            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/devstack/pos/view/" + viewName + ".fxml"));
            loader.setControllerFactory(com.devstack.pos.PosApplication.getApplicationContext()::getBean);

            Parent root = loader.load();

            if (isInMainShell) {
                // If we are in the main shell, load into the content pane
                javafx.scene.Node contentPaneNode = currentRoot.lookup("#contentPane");
                if (contentPaneNode instanceof javafx.scene.layout.StackPane) {
                    javafx.scene.layout.StackPane contentPane = (javafx.scene.layout.StackPane) contentPaneNode;
                    contentPane.getChildren().clear();
                    contentPane.getChildren().add(root);
                    return;
                }
            }

            // Otherwise load as a full scene (default behavior)
            StageManager.loadFullScreenScene(stage, root);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Navigation Error", "Failed to navigate to " + viewName + ": " + e.getMessage());
        }
    }

    /**
     * Show error alert
     */
    protected void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show success alert
     */
    protected void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show warning alert
     */
    protected void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
