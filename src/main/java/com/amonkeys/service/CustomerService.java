package com.amonkeys.service;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;

import java.net.URI;import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amonkeys.entity.Customer;

import jakarta.annotation.PostConstruct;

@Service
public class CustomerService {

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

    public Customer saveCustomer(Customer customer) {
        DynamoDbTable<Customer> table = enhancedClient.table("customers", TableSchema.fromBean(Customer.class));

        PutItemEnhancedRequest<Customer> request = PutItemEnhancedRequest.builder(Customer.class)
                .item(customer)
                .build();

        table.putItem(request);
        return customer;
    }

    public List<Customer> findAllCustomers() {
        DynamoDbTable<Customer> table = enhancedClient.table("customers", TableSchema.fromBean(Customer.class));
        Iterable<Customer> customersIterable = table.scan().items();
        List<Customer> customers = new ArrayList<>();
        customersIterable.forEach(customers::add);
        return customers;
    }

    public Optional<Customer> findCustomerById(String id) {
        DynamoDbTable<Customer> table = enhancedClient.table("customers", TableSchema.fromBean(Customer.class));
        Customer customer = table.getItem(Key.builder().partitionValue(id).build());
        return Optional.ofNullable(customer);
    }

    public void deleteCustomerById(String id) {
        DynamoDbTable<Customer> table = enhancedClient.table("customers", TableSchema.fromBean(Customer.class));
        Customer customer = table.getItem(Key.builder().partitionValue(id).build());
        if (customer != null) {
            table.deleteItem(Key.builder().partitionValue(id).build());
        }
    }

}
