package com.devstack.pos.util;

import javafx.scene.control.Alert;

public class AuthorizationUtil {

    /**
     * Check if current user has cashier role.
     * In the desktop app, all logged-in users must be cashiers.
     */
    public static boolean isCashier() {
        return UserSessionData.isCashier();
    }

    /**
     * Check if user has specific permission.
     * Since the desktop app is now only for the core cashier workflow,
     * we grant all permissions to the authenticated cashier.
     */
    public static boolean hasPermission(String permission) {
        return true;
    }

    /**
     * Check if user can access POS Orders
     */
    public static boolean canAccessPOSOrders() {
        return true;
    }

    /**
     * Check if user can access Return Orders
     */
    public static boolean canAccessReturnOrders() {
        return true;
    }

    /**
     * Check if user can access customers management
     */
    public static boolean canAccessCustomers() {
        return true;
    }

    /**
     * Check if user can access products management
     */
    public static boolean canAccessProducts() {
        return true;
    }

    /**
     * Show unauthorized access alert
     */
    public static void showUnauthorizedAlert() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Access Denied");
        alert.setHeaderText("Unauthorized Access");
        alert.setContentText("You don't have permission to access this feature.");
        alert.showAndWait();
    }

    /**
     * Check authorization and show alert if denied
     * 
     * @return true if authorized, false otherwise
     */
    public static boolean checkAndAlert(boolean isAuthorized) {
        if (!isAuthorized) {
            showUnauthorizedAlert();
        }
        return isAuthorized;
    }
}
