package com.devstack.pos.util;

import javafx.scene.control.Alert;

public class AuthorizationUtil {

    /**
     * Check if current user has admin role
     * Supports: ROLE_ADMIN, ADMIN, ROLE_SUPER_ADMIN, SUPER_ADMIN
     */
    public static boolean isAdmin() {
        return UserSessionData.isAdmin();
    }

    /**
     * Check if current user has cashier role
     * Supports: ROLE_CASHIER, CASHIER
     */
    public static boolean isCashier() {
        return UserSessionData.isCashier();
    }

    /**
     * Check if user has specific permission
     */
    public static boolean hasPermission(String permission) {
        if (UserSessionData.isAdmin())
            return true; // Admin has all permissions
        return UserSessionData.permissions.contains(permission.toUpperCase());
    }

    /**
     * Check if user can access POS Orders (Place Orders)
     */
    public static boolean canAccessPOSOrders() {
        return hasPermission("MANAGE_ORDERS") || hasPermission("PLACE_ORDERS") || hasPermission("VIEW_ORDERS");
    }

    /**
     * Check if user can access Return Orders
     */
    public static boolean canAccessReturnOrders() {
        return hasPermission("MANAGE_RETURNS") || hasPermission("MANAGE_ORDERS");
    }

    /**
     * Check if user can access Purchase Orders
     */
    public static boolean canAccessPurchaseOrders() {
        return hasPermission("MANAGE_ORDERS") || hasPermission("MANAGE_INVENTORY");
    }

    /**
     * Check if user can access customers management
     */
    public static boolean canAccessCustomers() {
        return hasPermission("MANAGE_CUSTOMERS") || hasPermission("VIEW_CUSTOMERS");
    }

    /**
     * Check if user can access products management
     */
    public static boolean canAccessProducts() {
        return hasPermission("MANAGE_PRODUCTS") || hasPermission("VIEW_PRODUCTS");
    }

    /**
     * Check if user can access reports/analysis
     */
    public static boolean canAccessReports() {
        return hasPermission("VIEW_REPORTS");
    }

    /**
     * Check if user can access settings
     */
    public static boolean canAccessSettings() {
        return hasPermission("MANAGE_SETTINGS");
    }

    /**
     * Show unauthorized access alert
     */
    public static void showUnauthorizedAlert() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Access Denied");
        alert.setHeaderText("Unauthorized Access");
        alert.setContentText("You don't have permission to access this feature.\n" +
                "Current Roles: " + UserSessionData.userRoles);
        alert.showAndWait();
    }

    /**
     * Show admin only alert
     */
    public static void showAdminOnlyAlert() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Admin Access Required");
        alert.setHeaderText("Admin Only Feature");
        alert.setContentText("This feature is only accessible by administrators.\n" +
                "Your Roles: " + UserSessionData.userRoles);
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
