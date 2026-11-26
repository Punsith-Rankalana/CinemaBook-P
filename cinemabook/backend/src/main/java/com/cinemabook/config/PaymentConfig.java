package com.cinemabook.config;

import com.cinemabook.domain.payment.PaymentProvider;
import com.cinemabook.infrastructure.payment.StripePaymentProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dependency Inversion: TicketService depends on PaymentProvider (interface).
 * This config wires the concrete implementation.
 *
 * To swap payment providers in the future: change the @Bean method here only.
 * TicketService and all callers remain unchanged.
 */
@Configuration
public class PaymentConfig {

    @Bean
    public PaymentProvider paymentProvider(StripePaymentProvider stripe) {
        return stripe;
    }
}