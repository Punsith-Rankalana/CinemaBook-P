package com.cinemabook.domain.payment;

/**
 * Shared status enum for all payment operation results.
 * Used by PaymentResult, PayoutResult, and RefundResult.
 */
public enum PaymentStatus {
    SUCCESS,
    FAILED,
    PENDING
}
