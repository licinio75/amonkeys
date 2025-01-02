package com.amonkeys.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CustomerTest {

    @Test
    void testCustomerCreation() {
        // Create a Customer object using the builder
        Customer customer = Customer.builder()
                .id("1")
                .email("john.doe@example.com")
                .name("John")
                .surname("Doe")
                .build();

        // Verify that the customer's id, email, and name match the expected values
        assertEquals("1", customer.getId());
        assertEquals("john.doe@example.com", customer.getEmail());
        assertEquals("John", customer.getName());
        assertEquals("Doe", customer.getSurname());
    }
}
