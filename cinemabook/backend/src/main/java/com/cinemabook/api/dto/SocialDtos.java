package com.cinemabook.api.dto;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

public final class SocialDtos {

    private SocialDtos() {}

    // ── Requests ─────────────────────────────────────────────────────────────

    public record CreatePostRequest(
            @NotBlank(message = "Post content is required")
            @Size(max = 2000, message = "Post must not exceed 2000 characters")
            String content,

            String postType,        // TEXT | IMAGE | CLIP | AVATAR_CARD  (default TEXT)
            String mediaUrl,        // optional — URL of uploaded image or clip
            String filmId           // optional — tag a film in the post
    ) {}

    public record CreateCommentRequest(
            @NotBlank(message = "Comment content is required")
            @Size(max = 2000, message = "Comment must not exceed 2000 characters")
            String content
    ) {}

    // ── Responses ────────────────────────────────────────────────────────────

    public record PostResponse(
            String      id,
            AuthorDto   author,
            String      postType,
            String      content,
            String      mediaUrl,
            String      filmId,
            String      filmTitle,
            long        likesCount,
            long        commentsCount,
            List<String> hashtags,
            boolean     likedByMe,
            Instant     createdAt,
            Instant     autoDeleteAt
    ) {}

    public record PostPageResponse(
            List<PostResponse> posts,
            int    page,
            int    size,
            long   totalElements,
            int    totalPages
    ) {}

    public record CommentResponse(
            String    id,
            AuthorDto author,
            String    content,
            long      likesCount,
            boolean   likedByMe,
            boolean   ownedByMe,
            Instant   createdAt
    ) {}

    public record CommentPageResponse(
            List<CommentResponse> comments,
            int    page,
            int    size,
            long   totalElements,
            int    totalPages
    ) {}

    public record AuthorDto(
            String id,
            String username,
            String displayName,
            String avatarUrl
    ) {}

    public record UserSummaryResponse(
            String  id,
            String  username,
            String  displayName,
            String  avatarUrl,
            long    followersCount,
            long    followingCount,
            boolean followedByMe
    ) {}

    public record UserPageResponse(
            List<UserSummaryResponse> users,
            int  page,
            int  size,
            long totalElements,
            int  totalPages
    ) {}

    public record HashtagResponse(
            String id,
            String tag,
            long   postCount
    ) {}

    public record LikeResponse(
            boolean liked,
            long    newCount
    ) {}

    public record FollowResponse(
            boolean following,
            long    followersCount
    ) {}
}