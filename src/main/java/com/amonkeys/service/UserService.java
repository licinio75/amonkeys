package com.amonkeys.service;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amonkeys.entity.User;

import jakarta.annotation.PostConstruct;

@Service
public class UserService {

    @Value("${aws.dynamodb.endpoint}")
    private String dynamoDbEndpoint;

    @Value("${aws.region}")
    private String awsRegion;

    @Value("${aws.accessKeyId}")
    private String awsAccessKeyId;

    @Value("${aws.secretKey}")
    private String awsSecretKey;

    private DynamoDbEnhancedClient enhancedClient;

    @PostConstruct
    public void init() {
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                .endpointOverride(URI.create(dynamoDbEndpoint))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(awsAccessKeyId, awsSecretKey)))
                .region(Region.of(awsRegion))
                .build();

        enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    public User saveUser(User user) {
        DynamoDbTable<User> table = enhancedClient.table("users", TableSchema.fromBean(User.class));
        table.putItem(user);
        return user;
    }

    public List<User> findAllUsers() {
        DynamoDbTable<User> table = enhancedClient.table("users", TableSchema.fromBean(User.class));
        Iterable<User> usersIterable = table.scan().items();
        List<User> users = new ArrayList<>();
        usersIterable.forEach(users::add);
        return users;
    }

    public Optional<User> findUserByEmail(String email) {
        DynamoDbTable<User> table = enhancedClient.table("users", TableSchema.fromBean(User.class));
        return table.scan().items().stream().filter(user -> user.getEmail().equals(email)).findAny();
    }

    public Optional<User> findUserById(String id) {
        DynamoDbTable<User> table = enhancedClient.table("users", TableSchema.fromBean(User.class));
        User user = table.getItem(Key.builder().partitionValue(id).build());
        return Optional.ofNullable(user);
    }

    public Optional<User> findUserByIdAndEmail(String id, String email) {
        DynamoDbTable<User> table = enhancedClient.table("users", TableSchema.fromBean(User.class));
        return table.scan().items().stream().filter(user -> user.getId().equals(id) && user.getEmail().equals(email)).findAny();
    }

    public void deleteUser(String id) {
        DynamoDbTable<User> table = enhancedClient.table("users", TableSchema.fromBean(User.class));
        User user = table.getItem(Key.builder().partitionValue(id).build());
        if (user != null) {
            table.deleteItem(Key.builder().partitionValue(id).build());
        }
    }
}
