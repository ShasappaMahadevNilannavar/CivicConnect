package com.civicconnect.service;

import com.civicconnect.dto.ProfileUpdateRequest;
import com.civicconnect.dto.RegisterRequest;
import com.civicconnect.model.User;
import com.civicconnect.model.enums.Role;
import com.civicconnect.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(RegisterRequest req) {
        if (!req.isPasswordMatch()) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setPhone(req.getPhone());
        user.setAddress(req.getAddress());
        user.setRole(Role.CITIZEN);
        user.setEnabled(true);

        User savedUser = userRepository.save(user);
        logger.info("New user registered: {} ({})", savedUser.getName(), savedUser.getEmail());
        return savedUser;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public User updateProfile(Long userId, ProfileUpdateRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        user.setName(req.getName());
        user.setPhone(req.getPhone());
        user.setAddress(req.getAddress());

        User updatedUser = userRepository.save(user);
        logger.info("Profile updated for user: {}", updatedUser.getEmail());
        return updatedUser;
    }

    public User getCurrentUser(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        String email = principal.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
    }
}
