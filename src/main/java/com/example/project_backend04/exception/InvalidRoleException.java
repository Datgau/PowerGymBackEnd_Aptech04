package com.example.project_backend04.exception;

public class InvalidRoleException extends RuntimeException {
    
    private final Long userId;
    
    public InvalidRoleException(Long userId) {
        super("User with id " + userId + " is not a trainer");
        this.userId = userId;
    }
    
    public Long getUserId() {
        return userId;
    }
}
