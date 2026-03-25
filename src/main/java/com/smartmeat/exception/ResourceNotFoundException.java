package com.smartmeat.exception;

//── Custom exceptions ─────────────────────────────────────────────────────────
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}