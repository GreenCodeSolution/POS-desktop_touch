package com.devstack.pos.controller;

import com.jfoenix.controls.JFXButton;
import javafx.fxml.FXML;
import javafx.scene.text.Text;
import org.springframework.stereotype.Component;

@Component
public class HelpPageController extends BaseController {

    @FXML
    private Text txtUserEmail;

    // Menu buttons listed for visibility management
    @FXML
    private JFXButton btnCustomer;

    @FXML
    private JFXButton btnProduct;

    @FXML
    private JFXButton btnPlaceOrder;

    @FXML
    private JFXButton btnReturns;

    @FXML
    private JFXButton btnOrderDetails;

    @FXML
    private JFXButton btnHelp;

    @FXML
    public void initialize() {
        // Initialize sidebar with user info
        initializeSidebar();

        // Configure menu visibility - all remaining items are visible to cashiers
        configureMenuVisibility();
    }

    /**
     * Configure menu visibility. In the desktop app, all remaining items
     * are part of the core cashier workflow and are visible to any authorized user.
     */
    private void configureMenuVisibility() {
        setMenuItemVisible(btnCustomer, true);
        setMenuItemVisible(btnProduct, true);
        setMenuItemVisible(btnPlaceOrder, true);
        setMenuItemVisible(btnReturns, true);
        setMenuItemVisible(btnOrderDetails, true);
        setMenuItemVisible(btnHelp, true);
    }

    private void setMenuItemVisible(JFXButton button, boolean visible) {
        if (button != null) {
            button.setVisible(visible);
            button.setManaged(visible);
        }
    }

    @Override
    protected String getCurrentPageName() {
        return "Help";
    }
}
