package com.cinemabook.infrastructure.payment;

import com.cinemabook.domain.payment.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Stripe implementation of PaymentProvider.
 *
 * Open/Closed: PaymentProvider interface is never touched.
 * Swap this for PayPalPaymentProvider by injecting a different bean.
 *
 * For dev/Phase 2: returns mock responses so the app runs without
 * a real Stripe account. Set cinemabook.stripe.mock=false in prod.
 *
 * Production wiring: add stripe-java to pom.xml:
 * <dependency>
 *   <groupId>com.stripe</groupId>
 *   <artifactId>stripe-java</artifactId>
 *   <version>25.3.0</version>
 * </dependency>
 */
@Service
@Slf4j
public class StripePaymentProvider implements PaymentProvider {

    private final String secretKey;
    private final boolean mockMode;

    public StripePaymentProvider(
            @Value("${cinemabook.stripe.secret-key}") String secretKey,
            @Value("${cinemabook.stripe.mock:true}") boolean mockMode) {
        this.secretKey = secretKey;
        this.mockMode  = mockMode;
        if (mockMode) {
            log.warn("StripePaymentProvider running in MOCK mode — no real charges");
        }
    }

    @Override
    public PaymentResult charge(UUID watcherId, UUID filmId,
                                BigDecimal amount, String currency,
                                String paymentToken) {
        if (mockMode) {
            log.info("MOCK charge: watcherId={} filmId={} amount={} {}", watcherId, filmId, amount, currency);
            return PaymentResult.success("mock_pi_" + UUID.randomUUID());
        }

        // TODO prod: Stripe.apiKey = secretKey;
        // PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
        //     .setAmount(amount.multiply(BigDecimal.valueOf(100)).longValue())
        //     .setCurrency(currency.toLowerCase())
        //     .setPaymentMethod(paymentToken)
        //     .setConfirm(true)
        //     .build();
        // PaymentIntent intent = PaymentIntent.create(params);
        // return PaymentResult.success(intent.getId());

        throw new UnsupportedOperationException("Set cinemabook.stripe.mock=false and wire Stripe SDK");
    }

    @Override
    public PayoutResult payout(UUID filmOwnerId, String stripeAccountId,
                               BigDecimal amount, String currency) {
        if (mockMode) {
            log.info("MOCK payout: ownerId={} account={} amount={}", filmOwnerId, stripeAccountId, amount);
            return PayoutResult.success("mock_po_" + UUID.randomUUID());
        }

        // TODO prod: Stripe Connect payout to stripeAccountId
        throw new UnsupportedOperationException("Set cinemabook.stripe.mock=false and wire Stripe SDK");
    }

    @Override
    public RefundResult refund(String providerTransactionId, BigDecimal amount) {
        if (mockMode) {
            log.info("MOCK refund: transactionId={} amount={}", providerTransactionId, amount);
            return RefundResult.success("mock_re_" + UUID.randomUUID());
        }

        // TODO prod: Refund.create(RefundCreateParams)
        throw new UnsupportedOperationException("Set cinemabook.stripe.mock=false and wire Stripe SDK");
    }

    /**
     * Creates a Stripe Connect onboarding link for a film owner.
     * In mock mode returns a placeholder URL.
     */
    public String createConnectOnboardingUrl(String stripeAccountId, String returnUrl) {
        if (mockMode) {
            log.info("MOCK onboarding URL for account={}", stripeAccountId);
            return returnUrl + "?mock_onboarding=complete&account=" + stripeAccountId;
        }
        // TODO prod: AccountLink.create(AccountLinkCreateParams)
        throw new UnsupportedOperationException("Wire Stripe SDK for prod onboarding");
    }

    /**
     * Creates a new Stripe Connect account for a film owner.
     * In mock mode returns a fake account ID.
     */
    public String createConnectAccount(String email) {
        if (mockMode) {
            String mockId = "acct_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            log.info("MOCK Stripe account created: id={}", mockId);
            return mockId;
        }
        // TODO prod: Account.create(AccountCreateParams)
        throw new UnsupportedOperationException("Wire Stripe SDK for prod account creation");
    }
}
