package com.cinemabook.shared.exception;

/** Thrown when a requested resource does not exist. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found: " + id);
    }
}
