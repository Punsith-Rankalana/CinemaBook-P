package com.cinemabook.domain.user;

import java.util.UUID;

/**
 * Core contract for every account in the system.
 *
 * Open/Closed: This interface is CLOSED for modification.
 * New account behaviour (e.g. admin) must be added by implementing
 * this interface, NOT by editing it.
 *
 * Liskov Substitution: FilmOwner and Watcher both honour this contract.
 */
public interface UserAccount {

    UUID getId();
    String getEmail();
    String getUsername();
    UserType getUserType();
    AccountStatus getAccountStatus();

    /** Returns true only when the account can log in and use the platform. */
    default boolean isActive() {
        return AccountStatus.ACTIVE.equals(getAccountStatus());
    }
}
