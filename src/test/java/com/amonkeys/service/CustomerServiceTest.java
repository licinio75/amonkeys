package com.amonkeys.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amonkeys.entity.Customer;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;

class CustomerServiceTest {

    @Mock
    private DynamoDbEnhancedClient enhancedClient;

    @Mock
    private DynamoDbTable<Customer> customerTable;

    @InjectMocks
    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(enhancedClient.table("customers", TableSchema.fromBean(Customer.class))).thenReturn(customerTable);
    }

    @Test
    void testSaveCustomer() {
        Customer customer = Customer.builder()
                .id("1")
                .email("john.doe@example.com")
                .name("John")
                .surname("Doe")
                .build();
        
        // Mock the PutItemEnhancedRequest with the correct type
        PutItemEnhancedRequest<Customer> request = PutItemEnhancedRequest.builder(Customer.class) // Specify the type here
                .item(customer)  // Pass the customer you want to save
                .build();
        
        // Mock the putItem method
        doNothing().when(customerTable).putItem(eq(request));  // Make sure the request is correctly parameterized
        
        // Call the saveCustomer method
        Customer savedCustomer = customerService.saveCustomer(customer);
        
        // Verify that putItem was called with the correct request
        verify(customerTable, times(1)).putItem(eq(request));
        
        // Verify that the saved customer is the same as the one passed to the service
        assertEquals(customer, savedCustomer);
    }    

    @Test
    void testDeleteCustomerById() {
        // Create a Key to simulate the deletion
        Key mockKey = Key.builder().partitionValue("1").build();
    
        // Create a mock customer to return when getItem is called
        Customer mockCustomer = Customer.builder().id("1").email("john.doe@example.com").name("John").surname("Doe").build();
    
        // Set up the mock to return the customer when getItem is called
        when(customerTable.getItem(eq(mockKey))).thenReturn(mockCustomer);
    
        // Simulate the deletion without returning a value
        doAnswer(invocation -> null).when(customerTable).deleteItem(eq(mockKey)); // Here, the call does nothing and doesn't need a return value
    
        // Call the deleteCustomerById method from the service
        customerService.deleteCustomerById("1");
    
        // Verify that deleteItem was called with the correct key
        verify(customerTable, times(1)).deleteItem(eq(mockKey));
    }
}
