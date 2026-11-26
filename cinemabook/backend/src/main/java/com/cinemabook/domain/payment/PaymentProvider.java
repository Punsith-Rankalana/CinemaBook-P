package com.cinemabook.domain.payment;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Open/Closed Principle core example.
 *
 * This interface is CLOSED for modification.
 * To add a new payment provider (PayPal, local bank, etc.),
 * create a new class implementing this interface.
 * Zero existing code changes required.
 *
 * Dependency Inversion: all services depend on THIS abstraction,
 * never on StripePaymentProvider directly.
 */
public interface PaymentProvider {

    /**
     * Charges a watcher for a film ticket.
     *
     * @param watcherId   the watcher's UUID
     * @param filmId      the film's UUID
     * @param amount      ticket price
     * @param currency    ISO 4217 currency code (e.g. "USD")
     * @param paymentToken provider-specific token (e.g. Stripe PaymentMethod ID)
     * @return PaymentResult with status and provider transaction ID
     */
    PaymentResult charge(UUID watcherId, UUID filmId,
                         BigDecimal amount, String currency,
                         String paymentToken);

    /**
     * Sends a payout to a film owner for their earnings.
     *
     * @param filmOwnerId          the owner's UUID
     * @param stripeAccountId      the owner's connected account ID
     * @param amount               amount to pay out
     * @param currency             ISO 4217 currency code
     * @return PayoutResult with status and provider payout ID
     */
    PayoutResult payout(UUID filmOwnerId, String stripeAccountId,
                        BigDecimal amount, String currency);

    /**
     * Refunds a previous charge.
     *
     * @param providerTransactionId  the provider's original transaction ID
     * @param amount                 amount to refund (may be partial)
     * @return RefundResult with status
     */
    RefundResult refund(String providerTransactionId, BigDecimal amount);
}
