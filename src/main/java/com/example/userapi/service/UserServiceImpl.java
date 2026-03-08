package com.example.userapi.service;

import com.example.userapi.cache.LruCache;
import com.example.userapi.dto.CreateUserRequest;
import com.example.userapi.dto.UserResponse;
import com.example.userapi.exception.UserNotFoundException;
import com.example.userapi.model.User;
import com.example.userapi.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final LruCache<Long, UserResponse> userCache;

    public UserServiceImpl(UserRepository userRepository, LruCache<Long, UserResponse> userCache) {
        this.userRepository = userRepository;
        this.userCache = userCache;
    }

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        User user = new User(request.getName(), request.getEmail());
        User savedUser = userRepository.save(user);
        UserResponse response = toResponse(savedUser);
        userCache.put(savedUser.getId(), response);
        return response;
    }

    @Override
    public UserResponse getUserById(Long id) {
        UserResponse cached = userCache.get(id);
        if (cached != null) {
            return cached;
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        UserResponse response = toResponse(user);
        userCache.put(user.getId(), response);
        return response;
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
