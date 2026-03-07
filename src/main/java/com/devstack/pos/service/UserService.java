package com.devstack.pos.service;

import com.devstack.pos.entity.AppUser;
import com.devstack.pos.repository.UserRepository;
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

}