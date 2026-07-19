package com.se2030.dental.exception;

public class SelfModificationForbiddenException extends RuntimeException {
    public SelfModificationForbiddenException(String message) {
        super(message);
    }
}
