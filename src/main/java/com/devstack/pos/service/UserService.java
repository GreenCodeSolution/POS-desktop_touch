package com.devstack.pos.service;

import com.devstack.pos.entity.AppUser;
import com.devstack.pos.entity.Role;
import com.devstack.pos.repository.RoleRepository;
import com.devstack.pos.repository.UserRepository;
import com.devstack.pos.util.TerminalConfigManager;
import com.devstack.pos.util.JwtUtil;
import com.devstack.pos.view.tm.TokenRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtUtil jwtUtil;

    // Direct instantiation — no need for Spring Security bean infrastructure
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AppUser findUser(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public String checkPassword(String email, String password) {
        try {
            AppUser user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return null;
            }

            // Verify password using BCrypt
            if (!passwordEncoder.matches(password, user.getPassword())) {
                return null;
            }

            // Collect ALL roles and permissions for the JWT
            List<String> roleNames = user.getRoles().stream()
                    .map(com.devstack.pos.entity.Role::getName)
                    .collect(Collectors.toList());

            List<String> permissionNames = user.getRoles().stream()
                    .flatMap(role -> role.getPermissions().stream())
                    .map(com.devstack.pos.entity.Permission::getName)
                    .distinct()
                    .collect(Collectors.toList());

            String jwtToken = jwtUtil.createJwtToken(TokenRequest.builder()
                    .username(user.getEmail())
                    .roles(roleNames)
                    .permissions(permissionNames)
                    .build());
            return jwtToken;

        } catch (Exception e) {
            return null;
        }
    }

    public List<AppUser> findActiveCashiers(Long businessId) {
        System.out.println("DEBUG: findActiveCashiers() called for business: " + businessId);
        List<AppUser> allUsers = userRepository.findAll();
        
        List<AppUser> cashiers = allUsers.stream()
                .filter(user -> {
                    // Filter by businessId if specified
                    if (businessId != null && user.getBusinessId() != null && !user.getBusinessId().equals(businessId)) {
                        return false;
                    }
                    // Be lenient with status: ACTIVE or null (assuming active)
                    boolean isActive = user.getStatus() == null || "ACTIVE".equalsIgnoreCase(user.getStatus());
                    
                    // Check if any role matches 'CASHIER' or 'ROLE_CASHIER'
                    boolean isCashier = user.getRoles().stream()
                            .anyMatch(role -> role.getName().toUpperCase().contains("CASHIER"));
                    
                    return isActive && isCashier;
                })
                .collect(Collectors.toList());
        
        System.out.println("DEBUG: Total cashiers filtered: " + cashiers.size());
        return cashiers;
    }

    public List<AppUser> findActiveCashiers() {
        return findActiveCashiers(TerminalConfigManager.getBusinessId());
    }
}
