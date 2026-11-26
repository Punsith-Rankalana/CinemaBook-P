package com.cinemabook.api.controller;

import com.cinemabook.api.dto.SocialDtos.*;
import com.cinemabook.application.service.FilmCommentService;
import com.cinemabook.application.service.SocialService;
import com.cinemabook.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Social platform REST API — posts, likes, follows, hashtags, film comments.
 * Single Responsibility: HTTP mapping only. Logic in SocialService / FilmCommentService.
 */
@RestController
@RequiredArgsConstructor
public class SocialController {

    private final SocialService      socialService;
    private final FilmCommentService filmCommentService;

    // ═══════════════════════════════════════════════════════════════
    // POSTS
    // ═══════════════════════════════════════════════════════════════

    /** POST /social/posts — create a new post */
    @PostMapping("/social/posts")
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreatePostRequest req) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(socialService.createPost(userId, req)));
    }

    /** DELETE /social/posts/{postId} — delete own post */
    @DeleteMapping("/social/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID postId) {

        socialService.deletePost(userId, postId);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    /** GET /social/feed — posts from followed users */
    @GetMapping("/social/feed")
    public ResponseEntity<ApiResponse<PostPageResponse>> getFeed(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.ok(
                socialService.getFeed(userId, page, size)));
    }

    /** GET /social/users/{userId}/posts — posts by a specific user */
    @GetMapping("/social/users/{profileUserId}/posts")
    public ResponseEntity<ApiResponse<PostPageResponse>> getUserPosts(
            @AuthenticationPrincipal UUID requesterId,
            @PathVariable UUID profileUserId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.ok(
                socialService.getUserPosts(profileUserId, requesterId, page, size)));
    }

    /** GET /social/films/{filmId}/posts — posts tagged with a film */
    @GetMapping("/social/films/{filmId}/posts")
    public ResponseEntity<ApiResponse<PostPageResponse>> getFilmPosts(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID filmId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.ok(
                socialService.getFilmPosts(filmId, userId, page, size)));
    }

    /** GET /social/hashtags/{tag}/posts — posts tagged with a hashtag */
    @GetMapping("/social/hashtags/{tag}/posts")
    public ResponseEntity<ApiResponse<PostPageResponse>> getHashtagPosts(
            @AuthenticationPrincipal UUID userId,
            @PathVariable String tag,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.ok(
                socialService.getHashtagPosts(tag, userId, page, size)));
    }

    // ═══════════════════════════════════════════════════════════════
    // LIKES
    // ═══════════════════════════════════════════════════════════════

    /** POST /social/posts/{postId}/like — toggle like on a post */
    @PostMapping("/social/posts/{postId}/like")
    public ResponseEntity<ApiResponse<LikeResponse>> togglePostLike(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID postId) {

        return ResponseEntity.ok(ApiResponse.ok(
                socialService.togglePostLike(userId, postId)));
    }

    /** POST /social/comments/{commentId}/like — toggle like on a film comment */
    @PostMapping("/social/comments/{commentId}/like")
    public ResponseEntity<ApiResponse<LikeResponse>> toggleCommentLike(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID commentId) {

        return ResponseEntity.ok(ApiResponse.ok(
                filmCommentService.toggleCommentLike(userId, commentId)));
    }

    // ═══════════════════════════════════════════════════════════════
    // FOLLOWS
    // ═══════════════════════════════════════════════════════════════

    /** POST /social/users/{userId}/follow — follow or unfollow a user */
    @PostMapping("/social/users/{followeeId}/follow")
    public ResponseEntity<ApiResponse<FollowResponse>> toggleFollow(
            @AuthenticationPrincipal UUID followerId,
            @PathVariable UUID followeeId) {

        return ResponseEntity.ok(ApiResponse.ok(
                socialService.toggleFollow(followerId, followeeId)));
    }

    /** GET /social/users/{userId}/followers */
    @GetMapping("/social/users/{userId}/followers")
    public ResponseEntity<ApiResponse<UserPageResponse>> getFollowers(
            @AuthenticationPrincipal UUID requesterId,
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.ok(
                socialService.getFollowers(userId, requesterId, page, size)));
    }

    /** GET /social/users/{userId}/following */
    @GetMapping("/social/users/{userId}/following")
    public ResponseEntity<ApiResponse<UserPageResponse>> getFollowing(
            @AuthenticationPrincipal UUID requesterId,
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.ok(
                socialService.getFollowing(userId, requesterId, page, size)));
    }

    // ═══════════════════════════════════════════════════════════════
    // HASHTAGS
    // ═══════════════════════════════════════════════════════════════

    /** GET /social/hashtags/trending — top trending hashtags */
    @GetMapping("/social/hashtags/trending")
    public ResponseEntity<ApiResponse<List<HashtagResponse>>> getTrending(
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(ApiResponse.ok(
                socialService.getTrendingHashtags(limit)));
    }

    /** GET /social/hashtags/search?q={prefix} — autocomplete hashtags */
    @GetMapping("/social/hashtags/search")
    public ResponseEntity<ApiResponse<List<HashtagResponse>>> searchHashtags(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(ApiResponse.ok(
                socialService.searchHashtags(q, limit)));
    }

    // ═══════════════════════════════════════════════════════════════
    // FILM COMMENTS
    // ═══════════════════════════════════════════════════════════════

    /** GET /social/films/{filmId}/comments — get all comments for a film */
    @GetMapping("/social/films/{filmId}/comments")
    public ResponseEntity<ApiResponse<CommentPageResponse>> getFilmComments(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID filmId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.ok(
                filmCommentService.getFilmComments(filmId, userId, page, size)));
    }

    /** POST /social/films/{filmId}/comments — add a comment to a film */
    @PostMapping("/social/films/{filmId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> addFilmComment(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID filmId,
            @Valid @RequestBody CreateCommentRequest req) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        filmCommentService.addComment(userId, filmId, req)));
    }

    /** DELETE /social/comments/{commentId} — delete own film comment */
    @DeleteMapping("/social/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID commentId) {

        filmCommentService.deleteComment(userId, commentId);
        return ResponseEntity.ok(ApiResponse.noContent());
    }
}