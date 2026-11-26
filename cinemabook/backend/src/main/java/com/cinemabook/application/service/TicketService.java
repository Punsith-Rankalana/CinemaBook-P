package com.cinemabook.application.service;

import com.cinemabook.api.dto.WatcherDtos.*;
import com.cinemabook.domain.payment.PaymentProvider;
import com.cinemabook.domain.payment.PaymentResult;
import com.cinemabook.infrastructure.persistence.*;
import com.cinemabook.shared.exception.BusinessRuleException;
import com.cinemabook.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Single Responsibility: ticket purchase and management.
 *
 * Open/Closed: PaymentProvider is injected — swap Stripe for another
 * provider without touching this class.
 *
 * Dependency Inversion: depends on PaymentProvider (interface),
 * not on StripePaymentProvider directly.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository              ticketRepository;
    private final FilmRepository                filmRepository;
    private final PaymentTransactionRepository  paymentRepository;
    private final UserRepository                userRepository;
    private final PaymentProvider               paymentProvider;

    // ── Purchase ticket ───────────────────────────────────────────────────────

    @Transactional
    public PaymentConfirmResponse purchaseTicket(UUID watcherId,
                                                 PurchaseTicketRequest req) {
        UUID filmId = UUID.fromString(req.filmId());

        FilmEntity film = filmRepository.findById(filmId)
                .orElseThrow(() -> new ResourceNotFoundException("Film", filmId));

        if (!FilmEntity.FilmStatus.PUBLISHED.equals(film.getStatus())) {
            throw new BusinessRuleException("This film is not available for purchase.");
        }

        // One ticket per watcher per film
        if (ticketRepository.existsByWatcherIdAndFilmId(watcherId, filmId)) {
            throw new BusinessRuleException(
                    "You already own a ticket for this film.");
        }

        UserEntity watcher = userRepository.findById(watcherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", watcherId));

        // Charge via payment provider (Stripe in prod, mock in dev)
        PaymentResult charge = paymentProvider.charge(
                watcherId, filmId,
                film.getTicketPrice(), film.getCurrency(),
                req.paymentToken());

        if (!charge.isSuccessful()) {
            throw new BusinessRuleException(
                    "Payment failed: " + charge.failureReason());
        }

        // Create ticket
        TicketEntity ticket = TicketEntity.builder()
                .watcher(watcher)
                .film(film)
                .status(TicketEntity.TicketStatus.ACTIVE)
                .pricePaid(film.getTicketPrice())
                .currency(film.getCurrency())
                .clipSecondsUsed(0)
                .clipSecondsLimit(300)
                .watchPositionSec(0)
                .build();
        ticket = ticketRepository.save(ticket);

        // Record payment transaction
        PaymentTransactionEntity payment = PaymentTransactionEntity.builder()
                .watcher(watcher)
                .film(film)
                .ticket(ticket)
                .stripePaymentId(charge.providerTransactionId())
                .amount(film.getTicketPrice())
                .currency(film.getCurrency())
                .status(PaymentTransactionEntity.PaymentTxStatus.COMPLETED)
                .build();
        paymentRepository.save(payment);

        // Update film stats
        filmRepository.incrementTicketStats(filmId, film.getTicketPrice());

        log.info("Ticket purchased: ticketId={} watcherId={} filmId={}",
                ticket.getId(), watcherId, filmId);

        return new PaymentConfirmResponse(
                ticket.getId().toString(),
                film.getTitle(),
                film.getTicketPrice(),
                film.getCurrency(),
                ticket.getPurchasedAt(),
                "Payment successful. Enjoy the film!"
        );
    }

    // ── Get watcher's tickets ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TicketPageResponse getMyTickets(UUID watcherId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "purchasedAt"));

        Page<TicketEntity> result =
                ticketRepository.findByWatcherIdOrderByPurchasedAtDesc(watcherId, pageable);

        return new TicketPageResponse(
                result.getContent().stream().map(this::toTicketResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicket(UUID watcherId, UUID ticketId) {
        TicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));

        if (!ticket.getWatcher().getId().equals(watcherId)) {
            throw new ResourceNotFoundException("Ticket", ticketId);
        }
        return toTicketResponse(ticket);
    }

    // ── Progress tracking ─────────────────────────────────────────────────────

    @Transactional
    public void updateProgress(UUID watcherId, UUID ticketId,
                               UpdateProgressRequest req) {
        TicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));

        if (!ticket.getWatcher().getId().equals(watcherId)) {
            throw new ResourceNotFoundException("Ticket", ticketId);
        }
        if (!TicketEntity.TicketStatus.ACTIVE.equals(ticket.getStatus())) {
            throw new BusinessRuleException("Ticket is not active.");
        }

        ticketRepository.updateWatchPosition(ticketId,
                req.positionSeconds(), Instant.now());

        log.debug("Progress updated: ticketId={} position={}s event={}",
                ticketId, req.positionSeconds(), req.eventType());
    }

    // ── Validate entitlement ──────────────────────────────────────────────────

    /**
     * Verifies a watcher holds a valid active ticket for a film.
     * Throws BusinessRuleException if not entitled.
     */
    @Transactional(readOnly = true)
    public TicketEntity validateEntitlement(UUID watcherId, UUID filmId) {
        return ticketRepository.findActiveTicket(watcherId, filmId)
                .orElseThrow(() -> new BusinessRuleException(
                        "You do not have an active ticket for this film. " +
                                "Please purchase a ticket to watch."));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TicketResponse toTicketResponse(TicketEntity t) {
        FilmEntity film = t.getFilm();
        return new TicketResponse(
                t.getId().toString(),
                film.getId().toString(),
                film.getTitle(),
                film.getThumbnailUrl(),
                t.getPricePaid(),
                t.getCurrency(),
                t.getStatus().name(),
                t.getClipSecondsUsed(),
                t.clipSecondsRemaining(),
                t.getWatchPositionSec(),
                t.getPurchasedAt(),
                t.getExpiresAt(),
                t.getLastWatchedAt()
        );
    }
}