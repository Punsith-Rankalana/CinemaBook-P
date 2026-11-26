package com.cinemabook.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for FollowEntity (follower_id + followee_id).
 * Required by JPA @IdClass when using multiple @Id fields.
 */
public class FollowId implements Serializable {

    private UserEntity follower;
    private UserEntity followee;

    public FollowId() {}

    public FollowId(UserEntity follower, UserEntity followee) {
        this.follower = follower;
        this.followee = followee;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FollowId that)) return false;
        return Objects.equals(follower, that.follower)
                && Objects.equals(followee, that.followee);
    }

    @Override
    public int hashCode() {
        return Objects.hash(follower, followee);
    }
}