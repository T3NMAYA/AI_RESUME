package com.resume.backend.controller;

import com.resume.backend.entity.User;
import com.resume.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

// DTO to exclude password from response
class UserDTO {
    private String email;
    private String fullName;

    public UserDTO(User user) {
        this.email = user.getEmail();
        this.fullName = user.getFullName();
    }

    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
}

// DTO for password change request
class PasswordChangeRequest {
    private String email;
    private String oldPassword;
    private String newPassword;

    // Default constructor required for Jackson deserialization
    public PasswordChangeRequest() {}

    public PasswordChangeRequest(String email, String oldPassword, String newPassword) {
        this.email = email;
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}

@Controller
public class UserController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @GetMapping("/register")
    public String showRegisterForm() {
        return "register";
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @GetMapping("/resumeForm")
    public String showResumeForm() {
        return "resumeForm";
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody User user) {
        logger.info("Registering user: {}", user.getEmail());
        if (userRepository.findByEmail(user.getEmail()) != null) {
            return ResponseEntity.status(400).body("Email already registered!");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword())); // Hash the password
        userRepository.save(user);
        return ResponseEntity.ok("User successfully registered");
    }

    @PostMapping("/login")
    public ResponseEntity<UserDTO> login(@RequestBody User user) {
        logger.info("Login attempt for email: {}", user.getEmail());
        User existingUser = userRepository.findByEmail(user.getEmail());
        if (existingUser != null && passwordEncoder.matches(user.getPassword(), existingUser.getPassword())) {
            UserDTO userDTO = new UserDTO(existingUser);
            return ResponseEntity.ok(userDTO);
        } else {
            return ResponseEntity.status(401).body(null); // Unauthorized
        }
    }

    @GetMapping("/changePassword")
    public String showChangePasswordForm() {
        return "changePassword";
    }

    @PostMapping("/changePassword")
    public ResponseEntity<String> changePassword(@RequestBody PasswordChangeRequest request) {
        logger.info("Change password request for email: {}", request.getEmail());
        User existingUser = userRepository.findByEmail(request.getEmail());
        if (existingUser != null && passwordEncoder.matches(request.getOldPassword(), existingUser.getPassword())) {
            existingUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(existingUser);
            return ResponseEntity.ok("Password changed successfully!");
        } else {
            return ResponseEntity.status(400).body("Invalid email or old password");
        }
    }

    // Endpoint to get all registered users
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        logger.info("Fetching all registered users");
        List<User> users = userRepository.findAll();
        List<UserDTO> userDTOs = users.stream()
                .map(UserDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userDTOs);
    }

    // New endpoint to delete a user account
    @DeleteMapping("/users/{email}")
    public ResponseEntity<String> deleteUser(@PathVariable String email) {
        logger.info("Delete request for email: {}", email);
        if (userRepository.existsById(email)) {
            userRepository.deleteById(email);
            return ResponseEntity.ok("User account deleted successfully");
        } else {
            return ResponseEntity.status(404).body("User not found");
        }
    }
}