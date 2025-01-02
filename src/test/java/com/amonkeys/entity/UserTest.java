package com.amonkeys.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void testUserBuilderAndGetters() {
        User user = User.builder()
                .id("1")
                .name("John Doe")
                .email("john.doe@example.com")
                .isAdmin(true)
                .updatedAt("2025-01-01T10:00:00Z")
                .updatedBy("admin")
                .build();

        assertEquals("1", user.getId());
        assertEquals("John Doe", user.getName());
        assertEquals("john.doe@example.com", user.getEmail());
        assertTrue(user.getIsAdmin());
        assertEquals("2025-01-01T10:00:00Z", user.getUpdatedAt());
        assertEquals("admin", user.getUpdatedBy());
    }

    @Test
    void testUserNoArgsConstructor() {
        User user = new User();
        user.setId("2");
        user.setName("Jane Doe");
        user.setEmail("jane.doe@example.com");
        user.setIsAdmin(false);

        assertEquals("2", user.getId());
        assertEquals("Jane Doe", user.getName());
        assertEquals("jane.doe@example.com", user.getEmail());
        assertFalse(user.getIsAdmin());
    }
}
