package com.cinemabook.domain.payment;

public record RefundResult(
        PaymentStatus status,
        String providerRefundId,
        String failureReason
) {
    public static RefundResult success(String refundId) {
        return new RefundResult(PaymentStatus.SUCCESS, refundId, null);
    }

    public static RefundResult failed(String reason) {
        return new RefundResult(PaymentStatus.FAILED, null, reason);
    }
}
