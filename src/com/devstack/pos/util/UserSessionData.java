package com.devstack.pos.util;

import java.time.LocalDateTime;

public class UserSessionData {
    public static String email = "";
    public static String jwtToken = "";
    public static java.util.Set<String> userRoles = new java.util.HashSet<>();
    public static java.util.Set<String> permissions = new java.util.HashSet<>();
    public static LocalDateTime lastActivityTime = null;

    public static boolean isAdmin() {
        return userRoles.stream().anyMatch(role -> role.equalsIgnoreCase("ROLE_SUPER_ADMIN") ||
                role.equalsIgnoreCase("SUPER_ADMIN") ||
                role.equalsIgnoreCase("ROLE_ADMIN") ||
                role.equalsIgnoreCase("ADMIN"));
    }

    public static boolean isAdminRole() {
        return userRoles.stream().anyMatch(role -> role.equalsIgnoreCase("ROLE_ADMIN") ||
                role.equalsIgnoreCase("ADMIN"));
    }

    public static boolean isCashier() {
        return userRoles.stream().anyMatch(role -> role.equalsIgnoreCase("ROLE_CASHIER") ||
                role.equalsIgnoreCase("CASHIER"));
    }

    public static boolean isManager() {
        return userRoles.stream().anyMatch(role -> role.equalsIgnoreCase("ROLE_MANAGER") ||
                role.equalsIgnoreCase("MANAGER"));
    }

    public static boolean isSuperAdmin() {
        return userRoles.stream().anyMatch(role -> role.equalsIgnoreCase("ROLE_SUPER_ADMIN") ||
                role.equalsIgnoreCase("SUPER_ADMIN"));
    }

    /**
     * Update the last activity time to current time
     */
    public static void updateLastActivity() {
        lastActivityTime = LocalDateTime.now();
    }

    /**
     * Get the last activity time
     * 
     * @return Last activity time, or null if never set
     */
    public static LocalDateTime getLastActivityTime() {
        return lastActivityTime;
    }

    /**
     * Check if user has been inactive for more than specified hours
     * 
     * @param hours Number of hours to check
     * @return true if inactive for more than specified hours
     */
    public static boolean isInactiveForHours(long hours) {
        if (lastActivityTime == null) {
            return true; // No activity recorded, consider inactive
        }
        LocalDateTime now = LocalDateTime.now();
        return lastActivityTime.plusHours(hours).isBefore(now);
    }

    public static void clear() {
        email = "";
        jwtToken = "";
        userRoles.clear();
        permissions.clear();
        lastActivityTime = null;
    }

}
