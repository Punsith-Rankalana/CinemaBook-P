package com.cinemabook.domain.payment;

/**
 * Result of a charge() call.
 * PayoutResult and RefundResult are separate top-level files.
 */
public record PaymentResult(
        PaymentStatus status,
        String providerTransactionId,
        String failureReason
) {
    public static PaymentResult success(String transactionId) {
        return new PaymentResult(PaymentStatus.SUCCESS, transactionId, null);
    }

    public static PaymentResult failed(String reason) {
        return new PaymentResult(PaymentStatus.FAILED, null, reason);
    }

    public boolean isSuccessful() {
        return PaymentStatus.SUCCESS.equals(status);
    }
}
