package com.amonkeys.controller;

import com.amonkeys.entity.User;
import com.amonkeys.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "User Management API", description = "API for managing users")
@RestController
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Operation(summary = "Get all users", description = "Retrieve a list of all users.", security = {@SecurityRequirement(name = "oauth2")})
    @GetMapping("/api/users")
    public ResponseEntity<?> getAllUsers(@AuthenticationPrincipal OidcUser principal) {
        try {
            if (principal != null) {
                String email = principal.getEmail();
                Optional<User> userOptional = userService.findUserByEmail(email);
    
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    if (user.getIsAdmin()) {
                        // Fetch all users except those marked as deleted
                        return ResponseEntity.ok(userService.findAllUsers().stream()
                                          .filter(u -> Boolean.FALSE.equals(u.getIsDeleted())) 
                                          .collect(Collectors.toList()));
                    } else {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User not authorized.");
                    }
                } else {
                    // If user is not found
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User not authorized.");
                }
            } else {
                // If principal is null
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User not authorized.");
            }
        } catch (Exception ex) {
            logger.error("Unexpected error: ", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }
    

    @Operation(summary = "Create a new user", description = "Create a new user. Only administrators can create new users.", security = {@SecurityRequirement(name = "oauth2")})
    @PostMapping("/api/users")
    public ResponseEntity<?> createUser(@AuthenticationPrincipal OidcUser principal, @RequestBody User newUser) {
        try {
            if (principal != null) {
                String adminEmail = principal.getEmail();
                Optional<User> adminOptional = userService.findUserByEmail(adminEmail);
                if (adminOptional.isPresent()) {
                    User adminUser = adminOptional.get();
                    if (!adminUser.getIsAdmin()) {
                        throw new AccessDeniedException("Only administrators can create new users.");
                    }
                    
                    // Check if name and email are provided
                    if (newUser.getName() == null || newUser.getName().isEmpty()) {
                        return ResponseEntity.badRequest().body("The name field is required.");
                    }
                    if (newUser.getEmail() == null || newUser.getEmail().isEmpty()) {
                        return ResponseEntity.badRequest().body("The email field is required.");
                    }
    
                    // Check if the email already exists in the database
                    Optional<User> existingUserOptional = userService.findUserByEmail(newUser.getEmail());
                    if (existingUserOptional.isPresent()) {
                        return ResponseEntity.status(HttpStatus.CONFLICT).body("A user with this email already exists.");
                    }
    
                    // Generate a unique ID for the new user
                    newUser.setId(UUID.randomUUID().toString());
    
                    // Set default values
                    newUser.setIsDeleted(false);
                    newUser.setUpdatedAt(java.time.Instant.now().toString());
                    newUser.setUpdatedBy(adminUser.getId());
                    if (newUser.getIsAdmin() == null) {
                        newUser.setIsAdmin(false); // If not provided, set to false
                    }
                    return ResponseEntity.ok(userService.saveUser(newUser));
                }
            }
            throw new BadCredentialsException("User authentication failed.");
        } catch (BadCredentialsException ex) {
            logger.error("Authentication error: ", ex);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
        } catch (AccessDeniedException ex) {
            logger.error("Access denied: ", ex);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("Illegal argument: ", ex);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected error: ", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    @Operation(summary = "Update an existing user", description = "Update user information. Only administrators can update users.", security = {@SecurityRequirement(name = "oauth2")})
    @PutMapping("/api/users/{id}")
    public ResponseEntity<?> updateUser(@AuthenticationPrincipal OidcUser principal, @PathVariable String id, @RequestBody User updatedUser) {
        try {
            if (principal != null) {
                String adminEmail = principal.getEmail();
                Optional<User> adminOptional = userService.findUserByEmail(adminEmail);
                if (adminOptional.isPresent()) {
                    User adminUser = adminOptional.get();
                    if (!adminUser.getIsAdmin()) {
                        throw new AccessDeniedException("Only administrators can update users.");
                    }
    
                    // Fetch the existing user
                    Optional<User> userOptional = userService.findUserById(id);
                    if (userOptional.isPresent()) {
                        User existingUser = userOptional.get();
    
                        // Check if the email is provided and validate it
                        if (updatedUser.getEmail() != null && !updatedUser.getEmail().isEmpty()) {
                            // Check if the email already exists in the database and belongs to another user
                            Optional<User> existingEmailUser = userService.findUserByEmail(updatedUser.getEmail());
                            if (existingEmailUser.isPresent() && !existingEmailUser.get().getId().equals(id)) {
                                return ResponseEntity.status(HttpStatus.CONFLICT).body("A user with this email already exists.");
                            }
                            existingUser.setEmail(updatedUser.getEmail());
                        }
    
                        // Check if the name is provided and validate it
                        if (updatedUser.getName() != null && !updatedUser.getName().isEmpty()) {
                            existingUser.setName(updatedUser.getName());
                        }
    
                        // Update isAdmin if provided
                        if (updatedUser.getIsAdmin() != null) {
                            existingUser.setIsAdmin(updatedUser.getIsAdmin());
                        }
    
                        // Update isDeleted if provided
                        if (updatedUser.getIsDeleted() != null) {
                            existingUser.setIsDeleted(updatedUser.getIsDeleted());
                        }

                        // Update timestamps and the user who made the update
                        existingUser.setUpdatedAt(java.time.Instant.now().toString());
                        existingUser.setUpdatedBy(adminUser.getId());
    
                        return ResponseEntity.ok(userService.saveUser(existingUser));
                    } else {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
                    }
                }
            }
            throw new BadCredentialsException("User authentication failed.");
        } catch (BadCredentialsException ex) {
            logger.error("Authentication error: ", ex);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
        } catch (AccessDeniedException ex) {
            logger.error("Access denied: ", ex);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("Illegal argument: ", ex);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected error: ", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    @Operation(summary = "Delete a user", description = "Delete a user. Only administrators can delete users.", security = {@SecurityRequirement(name = "oauth2")})
    @DeleteMapping("/api/users")
    public ResponseEntity<?> deleteUser(@AuthenticationPrincipal OidcUser principal, @RequestParam String id, @RequestParam String email) {
        try {
            if (principal != null) {
                String adminEmail = principal.getEmail();
                Optional<User> adminOptional = userService.findUserByEmail(adminEmail);
                if (adminOptional.isPresent()) {
                    User adminUser = adminOptional.get();
                    if (!adminUser.getIsAdmin()) {
                        throw new AccessDeniedException("Only administrators can delete users.");
                    }

                    // Verify if user with the provided id and email exists
                    Optional<User> userOptional = userService.findUserByIdAndEmail(id, email);
                    if (userOptional.isPresent()) {
                        User userToDelete = userOptional.get();
                        userToDelete.setIsDeleted(true); // Soft delete by setting isDeleted to true
                        userService.saveUser(userToDelete);
                        return ResponseEntity.ok("User successfully deleted.");
                    } else {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with the given id and email.");
                    }
                }
            }
            throw new BadCredentialsException("User authentication failed.");
        } catch (BadCredentialsException ex) {
            logger.error("Authentication error: ", ex);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
        } catch (AccessDeniedException ex) {
            logger.error("Access denied: ", ex);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected error: ", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

}
