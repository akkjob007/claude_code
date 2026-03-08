package com.example.userapi.controller;

import com.example.userapi.dto.HelloWorldResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorldController {

    @GetMapping("/helloworld")
    public ResponseEntity<HelloWorldResponse> helloWorld() {
        return ResponseEntity.ok(new HelloWorldResponse("Hello, World!"));
    }
}
