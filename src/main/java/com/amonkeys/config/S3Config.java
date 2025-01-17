package com.amonkeys.config;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.net.URI;

@Configuration
public class S3Config {

    private static final Logger logger = LoggerFactory.getLogger(S3Config.class);

    @Value("${aws.s3.endpoint}")
    private String s3Endpoint;

    @Value("${aws.region}")
    private String awsRegion;

    @Value("${AWS_S3_ACCESS_KEY_ID:}")  // Default value is empty
    private String s3AccessKeyId;

    @Value("${AWS_S3_SECRET_ACCESS_KEY:}")  // Default value is empty
    private String s3SecretKey;

    @Autowired
    private Environment environment;

    // Helper method to get the credentials provider
    private StaticCredentialsProvider getCredentialsProvider() {
        String activeProfile = environment.getProperty("spring.profiles.active");


        if ("local".equals(activeProfile)) {
            // If local, we use configured credentials
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(s3AccessKeyId, s3SecretKey)
            );
        } else {
            // If production, we use the default credentials provider (IAM roles)
            return StaticCredentialsProvider.create(DefaultCredentialsProvider.create().resolveCredentials());
        }
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(s3Endpoint))
                .credentialsProvider(getCredentialsProvider())
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .region(Region.of(awsRegion))
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        try {
            return S3Presigner.builder()
                    .credentialsProvider(getCredentialsProvider())
                    .region(Region.of(awsRegion))
                    .endpointOverride(URI.create(s3Endpoint))
                    .build();
        } catch (SdkClientException e) {
            logger.error("Error creating S3Presigner: ", e);
            throw e;
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument: ", e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error: ", e);
            throw e;
        }
    }
}
