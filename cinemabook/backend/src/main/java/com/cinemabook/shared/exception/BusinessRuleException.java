package com.cinemabook.shared.exception;

/** Thrown when an action violates a business rule (e.g. clip limit exceeded). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) { super(message); }
}
