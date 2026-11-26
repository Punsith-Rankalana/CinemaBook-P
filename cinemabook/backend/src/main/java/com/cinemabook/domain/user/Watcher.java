package com.cinemabook.domain.user;

/**
 * Interface Segregation Principle:
 * Watchers get their own capability interface.
 * Film owners don't implement this and vice versa.
 */
public interface Watcher extends UserAccount {

    /** Total clip seconds this watcher has used across all tickets. */
    int getClipSecondsUsed();

    /** True when the watcher account is allowed to buy and stream films. */
    default boolean canWatch() {
        return isActive();
    }
}
