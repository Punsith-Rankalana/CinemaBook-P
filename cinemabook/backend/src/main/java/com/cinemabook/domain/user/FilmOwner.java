package com.cinemabook.domain.user;

/**
 * Interface Segregation Principle:
 * Film owners get their own capability interface separate from Watcher.
 * Nothing in the Watcher interface bleeds into this.
 *
 * Open/Closed: Add new film-owner capabilities (e.g. analytics, promotions)
 * by extending this interface or adding new interfaces — never modify existing.
 */
public interface FilmOwner extends UserAccount {

    String getCompanyName();
    String getStripeAccountId();
    boolean isBankAccountLinked();
    boolean isPayoutEnabled();

    /** True only when the owner can receive ticket earnings. */
    default boolean canReceivePayouts() {
        return isActive() && isBankAccountLinked() && isPayoutEnabled();
    }
}
