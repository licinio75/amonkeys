package com.amonkeys.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.regions.Region;

import java.net.URI;

@Configuration
public class DynamoDbConfig {

    @Value("${aws.dynamodb.endpoint}")
    private String dynamoDbEndpoint;

    @Value("${aws.region}")
    private String region;

    @Value("${AWS_ACCESS_KEY_ID:}")  // Default value empty
    private String accessKeyId;

    @Value("${AWS_SECRET_ACCESS_KEY:}")  // Default value empty
    private String secretKey;

    // Helper method to get the credentials provider
    private StaticCredentialsProvider getCredentialsProvider() {
        if (!accessKeyId.isEmpty() && !secretKey.isEmpty()) {
            // If credentials are configured (local), we use them
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretKey)
            );
        } else {
            // If no credentials (production), we use the default credentials provider (IAM roles)
            return StaticCredentialsProvider.create(DefaultCredentialsProvider.create().resolveCredentials());
        }
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient() {
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(getCredentialsProvider()) // Use the helper method here
                .endpointOverride(URI.create(dynamoDbEndpoint))
                .build();

        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}
