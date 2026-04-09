package com.example.project_backend04.exception;

/**
 * Exception thrown when a user does not have the trainer role
 */
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
