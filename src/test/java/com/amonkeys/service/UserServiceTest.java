package com.amonkeys.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amonkeys.entity.User;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;

class UserServiceTest {

    @Mock
    private DynamoDbEnhancedClient enhancedClient;

    @Mock
    private DynamoDbTable<User> userTable;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(enhancedClient.table("users", TableSchema.fromBean(User.class))).thenReturn(userTable);
    }

    @Test
    void testSaveUser() {
        User user = User.builder()
                .id("1")
                .name("John Doe")
                .email("john.doe@example.com")
                .isAdmin(false)
                .build();

        userService.saveUser(user);

        verify(userTable, times(1)).putItem(user); // Verify that the putItem method was called once with the user
    }

    @Test
    void testFindUserByEmail() {
        List<User> mockUsers = new ArrayList<>();
        mockUsers.add(User.builder()
                .id("1")
                .name("John Doe")
                .email("john.doe@example.com")
                .isAdmin(false)
                .build());

        // Simulate the behavior of PageIterable and Page<User>
        SdkIterable<User> sdkIterable = new SdkIterable<>() {
            @Override
            public java.util.Iterator<User> iterator() {
                return mockUsers.iterator();
            }
        };

        @SuppressWarnings("unchecked")
        PageIterable<User> mockPageIterable = mock(PageIterable.class);
        when(mockPageIterable.items()).thenReturn(sdkIterable);

        // Configure the mock for scan()
        when(userTable.scan()).thenReturn(mockPageIterable);

        Optional<User> result = userService.findUserByEmail("john.doe@example.com");

        assertTrue(result.isPresent()); // Verify that the user is present in the result
        assertEquals("John Doe", result.get().getName()); // Verify that the name matches the expected value
    }

    @Test
    void testFindUserById() {
        User user = User.builder()
                .id("1")
                .name("John Doe")
                .build();

        // Create a mock for the key
        Key mockKey = Key.builder().partitionValue("1").build();

        // Configure the mock for userTable
        when(userTable.getItem(mockKey)).thenReturn(user);

        // Call the method we're testing
        Optional<User> result = userService.findUserById("1");

        // Validate the result
        assertTrue(result.isPresent());
        assertEquals("John Doe", result.get().getName());
    }

    @Test
    void testDeleteUser() {
        // Create a Key to simulate deletion
        Key mockKey = Key.builder().partitionValue("1").build();
    
        // Create a mock user to return when getItem is called
        User mockUser = User.builder().id("1").name("John Doe").build();
    
        // Configure the mock to return the user when getItem is called
        when(userTable.getItem(eq(mockKey))).thenReturn(mockUser);
    
        // Simulate deletion without the need for 'doNothing()'
        doAnswer(invocation -> null).when(userTable).deleteItem(eq(mockKey));  // Here, the call does nothing and doesn't need a return value.
    
        // Call the deleteUser method from the service
        userService.deleteUser("1");
    
        // Verify that deleteItem was called with the correct key
        verify(userTable, times(1)).deleteItem(eq(mockKey));
    }
}
