package com.cinemabook.shared.exception;

/** Thrown when authentication fails or credentials are wrong. */
public class UnauthorisedException extends RuntimeException {
    public UnauthorisedException(String message) { super(message); }
}
