package com.fintech.mco1.exception;

/**
 * Standardized error for when an email has already been used in registration.
 */
public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException() {
        super("An account with this email already exists");
    }
}
