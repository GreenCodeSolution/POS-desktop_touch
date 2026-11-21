package com.devstack.pos.service;

import com.devstack.pos.entity.AppUser;
import com.devstack.pos.entity.Permission;
import com.devstack.pos.entity.Role;
import com.devstack.pos.repository.PermissionRepository;
import com.devstack.pos.repository.RoleRepository;
import com.devstack.pos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@RequiredArgsConstructor
public class DatabaseBootstrapService implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final String[] SYSTEM_PERMISSIONS = {
            "ACCESS_ADMIN_DASHBOARD",
            "MANAGE_PRODUCTS", "VIEW_PRODUCTS",
            "MANAGE_CATEGORIES", "VIEW_CATEGORIES",
            "MANAGE_INVENTORY", "VIEW_INVENTORY",
            "MANAGE_ORDERS", "VIEW_ORDERS", "PLACE_ORDERS",
            "MANAGE_PENDING_PAYMENTS", "VIEW_PENDING_PAYMENTS",
            "VIEW_RETURNS", "MANAGE_RETURNS",
            "MANAGE_SUPPLIERS", "VIEW_SUPPLIERS",
            "MANAGE_CUSTOMERS", "VIEW_CUSTOMERS",
            "VIEW_REPORTS", "EXPORT_REPORTS",
            "MANAGE_SETTINGS",
            "MANAGE_USERS", "MANAGE_ROLES",
            "VIEW_FINANCIALS", "VIEW_CCTV"
    };

    private static final String[] ADMIN_PERMISSIONS = {
            "ACCESS_ADMIN_DASHBOARD",
            "MANAGE_PRODUCTS", "VIEW_PRODUCTS",
            "MANAGE_CATEGORIES", "VIEW_CATEGORIES",
            "MANAGE_INVENTORY", "VIEW_INVENTORY",
            "MANAGE_ORDERS", "VIEW_ORDERS", "PLACE_ORDERS",
            "MANAGE_PENDING_PAYMENTS", "VIEW_PENDING_PAYMENTS",
            "MANAGE_SUPPLIERS", "VIEW_SUPPLIERS",
            "MANAGE_CUSTOMERS", "VIEW_CUSTOMERS",
            "VIEW_REPORTS", "EXPORT_REPORTS",
            "MANAGE_SETTINGS",
            "VIEW_FINANCIALS", "MANAGE_RETURNS", "VIEW_RETURNS", "VIEW_CCTV"
    };

    private static final String[] CASHIER_PERMISSIONS = {
            "VIEW_PRODUCTS",
            "VIEW_CATEGORIES",
            "VIEW_INVENTORY",
            "PLACE_ORDERS", "VIEW_ORDERS",
            "MANAGE_PENDING_PAYMENTS", "VIEW_PENDING_PAYMENTS",
            "VIEW_CUSTOMERS",
            "MANAGE_RETURNS", "VIEW_RETURNS"
    };

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Seed Permissions & Roles
        seedPermissions();
        seedRoles();

        // 2. Ensure Platform Super Admin
        seedSuperAdmin();
    }

    private void seedPermissions() {
        for (String permName : SYSTEM_PERMISSIONS) {
            if (permissionRepository.findByName(permName).isEmpty()) {
                permissionRepository.save(Permission.builder().name(permName).build());
            }
        }
    }

    private void seedRoles() {
        // 1. ROLE_SUPER_ADMIN
        Role superAdminRole = roleRepository.findByName("ROLE_SUPER_ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_SUPER_ADMIN").build()));
        superAdminRole.setPermissions(permissionRepository.findAll());
        roleRepository.save(superAdminRole);

        // 2. ROLE_ADMIN
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ADMIN").build()));
        List<Permission> adminPerms = new ArrayList<>();
        for (String pName : ADMIN_PERMISSIONS) {
            permissionRepository.findByName(pName).ifPresent(adminPerms::add);
        }
        adminRole.setPermissions(adminPerms);
        roleRepository.save(adminRole);

        // 3. ROLE_CASHIER
        Role cashierRole = roleRepository.findByName("ROLE_CASHIER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_CASHIER").build()));
        List<Permission> cashierPerms = new ArrayList<>();
        for (String pName : CASHIER_PERMISSIONS) {
            permissionRepository.findByName(pName).ifPresent(cashierPerms::add);
        }
        cashierRole.setPermissions(cashierPerms);
        roleRepository.save(cashierRole);
    }

    private void seedSuperAdmin() {
        Role superAdminRole = roleRepository.findByName("ROLE_SUPER_ADMIN")
                .orElseThrow(() -> new IllegalStateException("ROLE_SUPER_ADMIN not found."));

        AppUser superAdmin = userRepository.findByEmail("superadmin")
                .orElseGet(() -> AppUser.builder().email("superadmin").build());

        superAdmin.setFirstName("Super Admin");
        superAdmin.setPassword(passwordEncoder.encode("ADMIN"));
        superAdmin.setBusinessId(null); // Platform-wide
        superAdmin.setStatus("ACTIVE");
        superAdmin.setRoles(new HashSet<>(Collections.singletonList(superAdminRole)));
        userRepository.save(superAdmin);
    }
}
