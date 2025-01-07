package com.amonkeys.service;

import com.amonkeys.entity.User;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;

@Service
public class S3Service {

    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);

    @Autowired
    private S3Client s3Client;

    @Autowired
    private S3Presigner s3Presigner;

    @Autowired
    private UserService userService;

    public String generatePresignedUrl(String bucketName, String key, @AuthenticationPrincipal OAuth2User principal) {
        try {
            // Check if user is authenticated
            if (principal != null) {
                String email = principal.getAttribute("email");
                Optional<User> userOptional = userService.findUserByEmail(email);

                //on minio (local) key = bucket + photo
                if (System.getProperty("spring.profiles.active").equals("local")) {
                    key = bucketName + "/" + key;
                }

                // Check if user is registered
                if (userOptional.isPresent()) {
                    GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build();

                    // Generate presigned URL
                    PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(r -> r
                            .signatureDuration(Duration.ofHours(1))
                            .getObjectRequest(getObjectRequest));

                    return presignedRequest.url().toString();
                } else {
                    throw new SecurityException("User not registered.");
                }
            } else {
                throw new SecurityException("User not authenticated.");
            }
        } catch (SdkClientException e) {
            logger.error("SDK Client error: ", e);
            throw e;
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument: ", e);
            throw e;
        } catch (SecurityException e) {
            logger.error("Security error: ", e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error: ", e);
            throw e;
        }
    }

    public void deletePhoto(String bucketName, String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build());
    }


}
