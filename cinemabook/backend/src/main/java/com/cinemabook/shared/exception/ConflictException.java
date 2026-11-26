package com.cinemabook.shared.exception;

/** Thrown when a duplicate resource would be created (e.g. email already registered). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}
