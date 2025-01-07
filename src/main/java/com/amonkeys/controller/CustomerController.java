package com.amonkeys.controller;

import com.amonkeys.entity.Customer;
import com.amonkeys.entity.User;
import com.amonkeys.service.CustomerService;
import com.amonkeys.service.S3Service;
import com.amonkeys.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Tag(name = "Customer Management API", description = "API for managing customers")
@RestController
public class CustomerController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);

    @Autowired
    private CustomerService customerService;

    @Autowired
    private UserService userService;

    @Autowired
    private S3Client s3Client;

    @Autowired
    private S3Service s3Service;

    @Value("${photo.extensions}")
    private String[] allowedExtensions;

    @Value("${photo.maxSize}")
    private long maxPhotoSize;

    @Value("${aws.s3.bucket-name}")
    private String s3BucketName;

    @Value("${aws.s3.endpoint}")
    private String s3Endpoint;

    @Operation(summary = "Create a new customer", description = "Create a new customer. Only logged-in users can create customers.", security = { @SecurityRequirement(name = "oauth2") })
    @PostMapping("/api/customers")
    public ResponseEntity<?> createCustomer(@AuthenticationPrincipal OidcUser principal,
                                            @RequestParam("name") String name,
                                            @RequestParam("surname") String surname,
                                            @RequestParam(value="email", required=false) String email,
                                            @RequestParam(value="photo", required=false) MultipartFile photo) {
        try {

            System.out.println("Create a new customer");

            if (principal != null) {
                String userEmail = principal.getEmail();
                System.out.println("userEmail:"+userEmail);
                Optional<User> userOptional = userService.findUserByEmail(userEmail);
    
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    System.out.println("name:"+name);
                    // Check required fields
                    if (name == null || name.isEmpty() || surname == null || surname.isEmpty()) {
                        return ResponseEntity.badRequest().body("Name and surname fields are required");
                    }
                    System.out.println("email:"+email);
                    // Check if the email is already taken
                    if (email != null && !email.isEmpty()) {
                        System.out.println("before findCustomerByEmail");
                        Optional<Customer> existingCustomer = customerService.findCustomerByEmail(email);
                        System.out.println("existingCustomer:"+existingCustomer);
                        if (existingCustomer.isPresent()) {
                            return ResponseEntity.badRequest().body("A customer with this email already exists.");
                        }
                    }
    
                    // Validate photo
                    System.out.println("photo1:"+photo);
                    if (photo != null && !photo.isEmpty()) {
                        System.out.println("photo2:"+photo);
                        String fileName = photo.getOriginalFilename();
                        String fileExtension = getFileExtension(fileName);
    
                        if (!Arrays.asList(allowedExtensions).contains(fileExtension.toLowerCase())) {
                            return ResponseEntity.badRequest().body("Invalid file type. Only " + String.join(", ", allowedExtensions) + " are allowed.");
                        }
    
                        if (photo.getSize() > maxPhotoSize) {
                            return ResponseEntity.badRequest().body("File too large. Maximum size is " + maxPhotoSize + " bytes.");
                        }
                    }
                    System.out.println("before Customer.builder");
                    // Create customer
                    Customer customer = Customer.builder()
                            .id(UUID.randomUUID().toString())
                            .name(name)
                            .surname(surname)
                            .email(email)
                            .photo((photo==null || photo.isEmpty()) ? null : uploadPhotoToS3(photo))
                            .createdAt(java.time.Instant.now().toString())
                            .createdBy(user.getId())
                            .updatedAt(null)
                            .updatedBy(null)
                            .build();
    
                    return ResponseEntity.ok(customerService.saveCustomer(customer));
                }
            }
            throw new BadCredentialsException("User authentication failed.");
        } catch (BadCredentialsException ex) {
            logger.error("Authentication failed: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
        } catch (IOException ex) {
            logger.error("Failed to upload photo: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to upload photo.");
        } catch (Exception ex) {
            logger.error("An unexpected error occurred: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }
    

    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        }
        return null;
    }

    private String uploadPhotoToS3(MultipartFile photo) throws IOException {
        String bucketName = s3BucketName;
        String key =  UUID.randomUUID().toString() + "-" + cleanFileName(photo.getOriginalFilename());
        System.out.println("bucketName"+bucketName);
        // Check if bucket exists, if not, create it
        System.out.println("s3Endpoint"+s3Endpoint);
        
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
        } catch (NoSuchBucketException e) {
            logger.warn("Bucket {} does not exist. Creating bucket.", bucketName, e);
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        }

        // Upload the file to S3
        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build(),
                RequestBody.fromBytes(photo.getBytes()));

        logger.info("Photo uploaded to S3 with key: {}", key);
        return key;
    }

    private String cleanFileName(String fileName) {
        // Normalize and remove special characters
        String normalized = Normalizer.normalize(fileName, Normalizer.Form.NFD);
        return normalized.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
    }


    @Operation(
        summary = "View Customer Photo",
        description = "View customer photo by photo name.",
        security = { @SecurityRequirement(name = "oauth2") }
    )
    @GetMapping("/api/customers/photo")
    public ResponseEntity<?> viewPhoto(
        @RequestParam String photo, 
        @AuthenticationPrincipal OidcUser principal
    ) {
        try {
            String key = s3BucketName + "/" + photo;
            String presignedUrl = s3Service.generatePresignedUrl(s3BucketName, key, principal);
            return ResponseEntity.ok(presignedUrl);
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected error: ", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }
    

    @Operation(summary = "List all customers", description = "Fetches a list of all customers.", security = {@SecurityRequirement(name = "oauth2")})
    @GetMapping("/api/customers")
    public ResponseEntity<?> listAllCustomers(@AuthenticationPrincipal OidcUser principal) {
        try {
            if (principal != null) {
                String userEmail = principal.getEmail();
                Optional<User> userOptional = userService.findUserByEmail(userEmail);

                if (userOptional.isPresent()) {
                    List<Customer> customers = customerService.findAllCustomers();
                    return ResponseEntity.ok(customers);
                } else {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User not authorized.");
                }
            } else {
                throw new BadCredentialsException("User authentication failed.");
            }
        } catch (BadCredentialsException ex) {
            logger.error("Authentication failed: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
        } catch (Exception ex) {
            logger.error("Failed to list customers: ", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to list customers.");
        }
    }


    @Operation(summary = "Get customer details", description = "Fetch detailed information of a customer, including the public URL of their photo.", security = {@SecurityRequirement(name = "oauth2")})
    @GetMapping("/api/customers/{id}")
    public ResponseEntity<?> getCustomerDetails(@PathVariable String id, @AuthenticationPrincipal OidcUser principal) {
        try {
            if (principal != null) {
                String userEmail = principal.getEmail();
                Optional<User> userOptional = userService.findUserByEmail(userEmail);

                if (userOptional.isPresent()) {
                    Optional<Customer> customerOptional = customerService.findCustomerById(id);
                    if (customerOptional.isPresent()) {
                        Customer customer = customerOptional.get();
                        if (customer.getPhoto() != null) {
                            String photoKey = customer.getPhoto();
                            String presignedUrl = s3Service.generatePresignedUrl(s3BucketName, s3BucketName+"/"+photoKey, principal);
                            customer.setPhoto(presignedUrl);
                        }
                        return ResponseEntity.ok(customer);
                    } else {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Customer not found.");
                    }
                } else {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User not authorized.");
                }
            } else {
                throw new BadCredentialsException("User authentication failed.");
            }
        } catch (BadCredentialsException ex) {
            logger.error("Authentication failed: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
        } catch (Exception ex) {
            logger.error("Failed to get customer details: ", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to get customer details.");
        }
    }

    @Operation(summary = "Delete a customer", description = "Delete a customer and their photo from S3 if available.", security = {@SecurityRequirement(name = "oauth2")})
    @DeleteMapping("/api/customers/{id}")
    public ResponseEntity<?> deleteCustomer(@PathVariable String id, @AuthenticationPrincipal OidcUser principal) {
        try {
            if (principal != null) {
                String userEmail = principal.getEmail();
                Optional<User> userOptional = userService.findUserByEmail(userEmail);

                if (userOptional.isPresent()) {
                    Optional<Customer> customerOptional = customerService.findCustomerById(id);
                    if (customerOptional.isPresent()) {
                        Customer customer = customerOptional.get();
                        if (customer.getPhoto() != null) {
                            String photoKey = customer.getPhoto().substring(customer.getPhoto().lastIndexOf("/") + 1);
                            s3Service.deletePhoto(s3BucketName, photoKey);
                        }
                        customerService.deleteCustomerById(id);
                        return ResponseEntity.ok().body("Customer and their photo deleted successfully.");
                    } else {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Customer not found.");
                    }
                } else {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User not authorized.");
                }
            } else {
                throw new BadCredentialsException("User authentication failed.");
            }
        } catch (BadCredentialsException ex) {
            logger.error("Authentication failed: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
        } catch (Exception ex) {
            logger.error("Failed to delete customer: ", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete customer.");
        }
    }


    @Operation(summary = "Update a customer", description = "Update a customer’s details. Only non-null parameters will be updated. New photos will replace the old ones in S3.", security = {@SecurityRequirement(name = "oauth2")})
    @PatchMapping("/api/customers/{id}")
    public ResponseEntity<?> updateCustomer(@PathVariable String id,
                                            @AuthenticationPrincipal OidcUser principal,
                                            @RequestParam(required = false) String name,
                                            @RequestParam(required = false) String surname,
                                            @RequestParam(required = false) String email,
                                            @RequestParam(required = false) MultipartFile photo) {
        try {
            if (principal != null) {
                String userEmail = principal.getEmail();
                Optional<User> userOptional = userService.findUserByEmail(userEmail);
    
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    Optional<Customer> customerOptional = customerService.findCustomerById(id);
    
                    if (customerOptional.isPresent()) {
                        Customer customer = customerOptional.get();
    
                        // Check if email is being updated
                        if (email != null && !email.equals(customer.getEmail())) {
                            // Check if there's already a customer with the new email (excluding the current customer)
                            Optional<Customer> existingCustomer = customerService.findCustomerByEmail(email);
                            if (existingCustomer.isPresent()) {
                                return ResponseEntity.badRequest().body("A customer with this email already exists.");
                            }
                            // Update email if it's not already taken
                            customer.setEmail(email);
                        }
    
                        // Update other fields
                        if (name != null) customer.setName(name);
                        if (surname != null) customer.setSurname(surname);
    
                        // Handle photo update
                        if (photo != null && !photo.isEmpty()) {
                            String fileExtension = getFileExtension(photo.getOriginalFilename());
    
                            if (!Arrays.asList(allowedExtensions).contains(fileExtension.toLowerCase())) {
                                return ResponseEntity.badRequest().body("Invalid file type. Only " + String.join(", ", allowedExtensions) + " are allowed.");
                            }
    
                            if (photo.getSize() > maxPhotoSize) {
                                return ResponseEntity.badRequest().body("File too large. Maximum size is " + maxPhotoSize + " bytes.");
                            }
    
                            String oldPhotoKey = customer.getPhoto() != null ? customer.getPhoto().substring(customer.getPhoto().lastIndexOf("/") + 1) : null;
    
                            String newPhotoKey = uploadPhotoToS3(photo);
                            customer.setPhoto(newPhotoKey);
    
                            // Delete old photo if it exists
                            if (oldPhotoKey != null) {
                                s3Service.deletePhoto(s3BucketName, oldPhotoKey);
                            }
                        }
    
                        // Update metadata
                        customer.setUpdatedAt(java.time.Instant.now().toString());
                        customer.setUpdatedBy(user.getId());
    
                        customerService.saveCustomer(customer);
                        return ResponseEntity.ok(customer);
    
                    } else {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Customer not found.");
                    }
                } else {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User not authorized.");
                }
            } else {
                throw new BadCredentialsException("User authentication failed.");
            }
        } catch (BadCredentialsException ex) {
            logger.error("Authentication failed: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
        } catch (IOException ex) {
            logger.error("Failed to upload photo: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to upload photo.");
        } catch (Exception ex) {
            logger.error("Failed to update customer: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update customer.");
        }
    }
    
    


}
