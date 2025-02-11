package com.lashana.backend.service;

import com.lashana.backend.model.User;
import com.lashana.backend.repository.UserRepository;
import com.lashana.backend.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ActivityLogService activityLogService;

    public UserService(UserRepository userRepository, 
                      PasswordEncoder passwordEncoder, 
                      JwtUtil jwtUtil,
                      ActivityLogService activityLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.activityLogService = activityLogService;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User registerUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Nếu là user đầu tiên, gán quyền ADMIN, nếu không thì là USER
        user.setRole(userRepository.count() == 0 ? "ADMIN" : "USER");

        return userRepository.save(user);
    }

    public String loginUser(String username, String password) {
        System.out.println("Login attempt for username: " + username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    System.out.println("User not found for username: " + username);
                    return new RuntimeException("User not found");
                });

        // Check password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            System.out.println("Invalid password for username: " + username);
            throw new RuntimeException("Invalid password");
        }

        activityLogService.logActivity(username, "LOGIN"); // Ghi nhật ký đăng nhập

        // Generate JWT token
        return jwtUtil.generateToken(username);
    }

    public void setUserRole(Long userId, String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole(role);
        userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
