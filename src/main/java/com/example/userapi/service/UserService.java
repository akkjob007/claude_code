package com.example.userapi.service;

import com.example.userapi.dto.CreateUserRequest;
import com.example.userapi.dto.UserResponse;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    UserResponse getUserById(Long id);
}
