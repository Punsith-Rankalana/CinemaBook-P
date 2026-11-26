package com.cinemabook.domain.payment;

public record PayoutResult(
        PaymentStatus status,
        String providerPayoutId,
        String failureReason
) {
    public static PayoutResult success(String payoutId) {
        return new PayoutResult(PaymentStatus.SUCCESS, payoutId, null);
    }

    public static PayoutResult failed(String reason) {
        return new PayoutResult(PaymentStatus.FAILED, null, reason);
    }
}
