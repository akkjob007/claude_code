package com.example.userapi.service;

import com.example.userapi.cache.LruCache;
import com.example.userapi.dto.CreateUserRequest;
import com.example.userapi.dto.UserResponse;
import com.example.userapi.exception.UserNotFoundException;
import com.example.userapi.model.User;
import com.example.userapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private LruCache<Long, UserResponse> userCache;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userCache = new LruCache<>(100);
        userService = new UserServiceImpl(userRepository, userCache);
    }

    @Test
    void createUser_shouldSaveToDatabaseAndPopulateCache() {
        CreateUserRequest request = new CreateUserRequest("John Doe", "john@example.com");
        User savedUser = new User("John Doe", "john@example.com");
        savedUser.setId(1L);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = userService.createUser(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("John Doe", response.getName());
        assertEquals("john@example.com", response.getEmail());

        verify(userRepository).save(any(User.class));

        // Verify the user response is now in the cache
        UserResponse cachedResponse = userCache.get(1L);
        assertNotNull(cachedResponse);
        assertEquals("John Doe", cachedResponse.getName());
    }

    @Test
    void getUserById_shouldReturnFromCacheOnHit() {
        UserResponse cachedResponse = new UserResponse(1L, "Jane Doe", "jane@example.com");
        userCache.put(1L, cachedResponse);

        UserResponse response = userService.getUserById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Jane Doe", response.getName());
        assertEquals("jane@example.com", response.getEmail());

        // Repository should NOT be called when cache hit
        verify(userRepository, never()).findById(any());
    }

    @Test
    void getUserById_shouldQueryDatabaseOnCacheMissAndPopulateCache() {
        User dbUser = new User("Bob Smith", "bob@example.com");
        dbUser.setId(2L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(dbUser));

        UserResponse response = userService.getUserById(2L);

        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertEquals("Bob Smith", response.getName());
        assertEquals("bob@example.com", response.getEmail());

        verify(userRepository).findById(2L);

        // Verify the user response is now in the cache
        UserResponse cachedResponse = userCache.get(2L);
        assertNotNull(cachedResponse);
        assertEquals("Bob Smith", cachedResponse.getName());
    }

    @Test
    void getUserById_shouldThrowUserNotFoundWhenNotInCacheOrDatabase() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.getUserById(99L)
        );

        assertEquals("User not found with id: 99", exception.getMessage());
        verify(userRepository).findById(99L);
    }
}
