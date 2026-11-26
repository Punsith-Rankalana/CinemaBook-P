package com.cinemabook.application.service;

import com.cinemabook.api.dto.FilmDtos.BankLinkResponse;
import com.cinemabook.infrastructure.payment.StripePaymentProvider;
import com.cinemabook.infrastructure.persistence.FilmOwnerProfileEntity;
import com.cinemabook.infrastructure.persistence.FilmOwnerProfileRepository;
import com.cinemabook.infrastructure.persistence.UserRepository;
import com.cinemabook.shared.exception.BusinessRuleException;
import com.cinemabook.shared.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Single Responsibility: manages Stripe Connect bank account linking.
 * Open/Closed: swap StripePaymentProvider without touching this service.
 *
 * Fix 1: Replaced @RequiredArgsConstructor + @Value field with explicit
 *         constructor so Spring injects @Value correctly (non-final fields
 *         are not picked up by Lombok's constructor).
 *
 * Fix 2: Fetch owner email via UserRepository instead of calling
 *         profile.getUser().getEmail() which triggers a LazyInitializationException
 *         when the Hibernate session has already closed.
 */
@Service
@Slf4j
public class BankLinkService {

    private final FilmOwnerProfileRepository ownerProfileRepository;
    private final UserRepository             userRepository;
    private final StripePaymentProvider      stripeProvider;
    private final String                     connectReturnUrl;

    public BankLinkService(
            FilmOwnerProfileRepository ownerProfileRepository,
            UserRepository userRepository,
            StripePaymentProvider stripeProvider,
            @Value("${cinemabook.stripe.connect-return-url:http://localhost:3000/owner/bank/return}")
            String connectReturnUrl) {
        this.ownerProfileRepository = ownerProfileRepository;
        this.userRepository         = userRepository;
        this.stripeProvider         = stripeProvider;
        this.connectReturnUrl       = connectReturnUrl;
    }

    /**
     * Step 1: Create a Stripe Connect account (if not already created)
     *         and return the onboarding URL.
     */
    @Transactional
    public BankLinkResponse initiateBankLink(UUID ownerId) {
        FilmOwnerProfileEntity profile = ownerProfileRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("FilmOwnerProfile", ownerId));

        String stripeAccountId = profile.getStripeAccountId();

        if (stripeAccountId == null || stripeAccountId.isBlank()) {
            // Fix: fetch email via UserRepository — avoids lazy-load on profile.getUser()
            String email = userRepository.findById(ownerId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", ownerId))
                    .getEmail();

            stripeAccountId = stripeProvider.createConnectAccount(email);
            profile.setStripeAccountId(stripeAccountId);
            ownerProfileRepository.save(profile);
            log.info("Stripe account created: ownerId={} stripeAccount={}", ownerId, stripeAccountId);
        }

        String onboardingUrl = stripeProvider.createConnectOnboardingUrl(
                stripeAccountId, connectReturnUrl);

        return new BankLinkResponse(onboardingUrl, stripeAccountId);
    }

    /**
     * Step 2: Called when the owner returns from Stripe onboarding.
     * In production, also verify via Stripe webhook (account.updated event).
     */
    @Transactional
    public void completeBankLink(UUID ownerId) {
        FilmOwnerProfileEntity profile = ownerProfileRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("FilmOwnerProfile", ownerId));

        if (profile.getStripeAccountId() == null) {
            throw new BusinessRuleException(
                    "No Stripe account found. Please start the bank linking process first.");
        }

        profile.setBankAccountLinked(true);
        profile.setPayoutEnabled(true);
        ownerProfileRepository.save(profile);

        log.info("Bank account linked: ownerId={} stripeAccount={}",
                ownerId, profile.getStripeAccountId());
    }

    @Transactional(readOnly = true)
    public BankLinkStatusResponse getBankLinkStatus(UUID ownerId) {
        FilmOwnerProfileEntity profile = ownerProfileRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("FilmOwnerProfile", ownerId));

        return new BankLinkStatusResponse(
                profile.isBankAccountLinked(),
                profile.isPayoutEnabled(),
                profile.getStripeAccountId() != null
        );
    }

    public record BankLinkStatusResponse(
            boolean bankAccountLinked,
            boolean payoutEnabled,
            boolean stripeAccountCreated
    ) {}
}