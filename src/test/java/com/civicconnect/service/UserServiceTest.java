package com.civicconnect.service;

import com.civicconnect.dto.RegisterRequest;
import com.civicconnect.model.User;
import com.civicconnect.model.enums.Role;
import com.civicconnect.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private BCryptPasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void testRegisterUser_Successful() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Alice Citizen");
        req.setEmail("alice@example.com");
        req.setPassword("Password@123");
        req.setConfirmPassword("Password@123");
        req.setPhone("+1-555-1234");
        req.setAddress("Ward 5");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(100L);
            return u;
        });

        User registered = userService.registerUser(req);

        assertNotNull(registered);
        assertEquals("alice@example.com", registered.getEmail());
        assertTrue(passwordEncoder.matches("Password@123", registered.getPassword()));
        assertEquals(Role.CITIZEN, registered.getRole());
        assertTrue(registered.isEnabled());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterUser_PasswordMismatch_ThrowsException() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Alice");
        req.setEmail("alice@example.com");
        req.setPassword("Password@123");
        req.setConfirmPassword("Different@123");

        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(req));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegisterUser_DuplicateEmail_ThrowsException() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Bob");
        req.setEmail("bob@example.com");
        req.setPassword("Secret@123");
        req.setConfirmPassword("Secret@123");

        when(userRepository.existsByEmail("bob@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(req));
        verify(userRepository, never()).save(any(User.class));
    }
}
