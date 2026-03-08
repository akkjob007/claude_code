package com.example.userapi.dto;

public class HelloWorldResponse {

    private String message;

    public HelloWorldResponse() {
    }

    public HelloWorldResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
