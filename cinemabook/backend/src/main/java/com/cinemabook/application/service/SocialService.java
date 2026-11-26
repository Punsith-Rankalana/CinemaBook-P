package com.cinemabook.application.service;

import com.cinemabook.api.dto.SocialDtos.*;
import com.cinemabook.infrastructure.persistence.*;
import com.cinemabook.shared.exception.BusinessRuleException;
import com.cinemabook.shared.exception.ResourceNotFoundException;
import com.cinemabook.shared.exception.UnauthorisedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Single Responsibility: social platform features (posts, likes, follows, hashtags).
 * Open/Closed: new post types or feed algorithms extend without modifying this class.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SocialService {

    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#([\\w]+)");

    private final PostRepository        postRepository;
    private final LikeRepository        likeRepository;
    private final FollowRepository      followRepository;
    private final HashtagRepository     hashtagRepository;
    private final UserRepository        userRepository;
    private final FilmRepository        filmRepository;

    // ── Create post ───────────────────────────────────────────────────────────

    @Transactional
    public PostResponse createPost(UUID authorId, CreatePostRequest req) {
        UserEntity author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", authorId));

        PostEntity.PostType postType = parsePostType(req.postType());

        PostEntity post = PostEntity.builder()
                .author(author)
                .postType(postType)
                .content(req.content())
                .mediaUrl(req.mediaUrl())
                .build();

        if (req.filmId() != null && !req.filmId().isBlank()) {
            filmRepository.findById(UUID.fromString(req.filmId()))
                    .ifPresent(post::setFilm);
        }

        Set<HashtagEntity> hashtags = extractAndSaveHashtags(req.content());
        post.setHashtags(hashtags);

        post = postRepository.save(post);
        log.info("Post created: postId={} authorId={}", post.getId(), authorId);

        return toPostResponse(post, authorId);
    }

    // ── Delete post ───────────────────────────────────────────────────────────

    @Transactional
    public void deletePost(UUID authorId, UUID postId) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));

        if (!post.getAuthor().getId().equals(authorId)) {
            throw new UnauthorisedException("You can only delete your own posts.");
        }

        post.getHashtags().forEach(h ->
                hashtagRepository.adjustPostCount(h.getId(), -1));

        postRepository.delete(post);
        log.info("Post deleted: postId={} authorId={}", postId, authorId);
    }

    // ── Feed ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PostPageResponse getFeed(UUID userId, int page, int size) {
        Page<PostEntity> result = postRepository.findFeedForUser(
                userId, Instant.now(),
                PageRequest.of(page, size));
        return toPostPage(result, userId);
    }

    @Transactional(readOnly = true)
    public PostPageResponse getUserPosts(UUID profileUserId, UUID requesterId,
                                         int page, int size) {
        Page<PostEntity> result = postRepository.findByAuthorNotExpired(
                profileUserId, Instant.now(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return toPostPage(result, requesterId);
    }

    @Transactional(readOnly = true)
    public PostPageResponse getFilmPosts(UUID filmId, UUID requesterId,
                                         int page, int size) {
        Page<PostEntity> result = postRepository.findByFilm(
                filmId, Instant.now(),
                PageRequest.of(page, size));
        return toPostPage(result, requesterId);
    }

    @Transactional(readOnly = true)
    public PostPageResponse getHashtagPosts(String tag, UUID requesterId,
                                            int page, int size) {
        Page<PostEntity> result = postRepository.findByHashtag(
                tag.toLowerCase(), Instant.now(),
                PageRequest.of(page, size));
        return toPostPage(result, requesterId);
    }

    // ── Like / Unlike post ────────────────────────────────────────────────────

    @Transactional
    public LikeResponse togglePostLike(UUID userId, UUID postId) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (likeRepository.existsByUserIdAndPostId(userId, postId)) {
            likeRepository.deleteByUserIdAndPostId(userId, postId);
            postRepository.adjustLikesCount(postId, -1);
            return new LikeResponse(false, Math.max(0, post.getLikesCount() - 1));
        } else {
            likeRepository.save(LikeEntity.builder().user(user).post(post).build());
            postRepository.adjustLikesCount(postId, 1);
            return new LikeResponse(true, post.getLikesCount() + 1);
        }
    }

    // ── Follow / Unfollow ─────────────────────────────────────────────────────

    @Transactional
    public FollowResponse toggleFollow(UUID followerId, UUID followeeId) {
        if (followerId.equals(followeeId)) {
            throw new BusinessRuleException("You cannot follow yourself.");
        }

        UserEntity follower = userRepository.findById(followerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", followerId));
        UserEntity followee = userRepository.findById(followeeId)
                .orElseThrow(() -> new ResourceNotFoundException("User", followeeId));

        if (followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            followRepository.deleteByFollowerIdAndFolloweeId(followerId, followeeId);
            long count = followRepository.countFollowers(followeeId);
            return new FollowResponse(false, count);
        } else {
            followRepository.save(
                    FollowEntity.builder().follower(follower).followee(followee).build());
            long count = followRepository.countFollowers(followeeId);
            return new FollowResponse(true, count);
        }
    }

    @Transactional(readOnly = true)
    public UserPageResponse getFollowers(UUID userId, UUID requesterId, int page, int size) {
        Page<UserEntity> result = followRepository.findFollowers(
                userId, PageRequest.of(page, size));
        return toUserPage(result, requesterId);
    }

    @Transactional(readOnly = true)
    public UserPageResponse getFollowing(UUID userId, UUID requesterId, int page, int size) {
        Page<UserEntity> result = followRepository.findFollowing(
                userId, PageRequest.of(page, size));
        return toUserPage(result, requesterId);
    }

    // ── Hashtags ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<HashtagResponse> getTrendingHashtags(int limit) {
        return hashtagRepository.findTrending(PageRequest.of(0, limit))
                .stream()
                .map(h -> new HashtagResponse(h.getId().toString(), h.getTag(), h.getPostCount()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HashtagResponse> searchHashtags(String prefix, int limit) {
        return hashtagRepository
                .findByTagStartingWithOrderByPostCountDesc(prefix.toLowerCase(),
                        PageRequest.of(0, limit))
                .stream()
                .map(h -> new HashtagResponse(h.getId().toString(), h.getTag(), h.getPostCount()))
                .toList();
    }

    // ── Hashtag extraction ────────────────────────────────────────────────────

    private Set<HashtagEntity> extractAndSaveHashtags(String content) {
        if (content == null) return new HashSet<>();

        Set<String> tags = new HashSet<>();
        Matcher m = HASHTAG_PATTERN.matcher(content);
        while (m.find()) {
            String tag = m.group(1).toLowerCase();
            if (tag.length() <= 100) tags.add(tag);
        }

        Set<HashtagEntity> entities = new HashSet<>();
        for (String tag : tags) {
            HashtagEntity hashtag = hashtagRepository.findByTag(tag)
                    .orElseGet(() -> hashtagRepository.save(
                            HashtagEntity.builder().tag(tag).postCount(0).build()));
            hashtagRepository.adjustPostCount(hashtag.getId(), 1);
            entities.add(hashtag);
        }
        return entities;
    }

    // ── Helpers — PUBLIC so UserController can call toUserSummary ─────────────

    private PostPageResponse toPostPage(Page<PostEntity> page, UUID requesterId) {
        return new PostPageResponse(
                page.getContent().stream()
                        .map(p -> toPostResponse(p, requesterId)).toList(),
                page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    public PostResponse toPostResponse(PostEntity p, UUID requesterId) {
        boolean likedByMe = requesterId != null &&
                likeRepository.existsByUserIdAndPostId(requesterId, p.getId());
        return new PostResponse(
                p.getId().toString(),
                toAuthorDto(p.getAuthor()),
                p.getPostType().name(),
                p.getContent(),
                p.getMediaUrl(),
                p.getFilm() != null ? p.getFilm().getId().toString() : null,
                p.getFilm() != null ? p.getFilm().getTitle() : null,
                p.getLikesCount(),
                p.getCommentsCount(),
                p.getHashtags().stream().map(HashtagEntity::getTag).toList(),
                likedByMe,
                p.getCreatedAt(),
                p.getAutoDeleteAt()
        );
    }

    private UserPageResponse toUserPage(Page<UserEntity> page, UUID requesterId) {
        return new UserPageResponse(
                page.getContent().stream()
                        .map(u -> toUserSummary(u, requesterId)).toList(),
                page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    /**
     * Public — called by UserController.searchUsers() from a different package.
     * Dependency Inversion: UserController depends on this service method,
     * not on repositories directly.
     */
    public UserSummaryResponse toUserSummary(UserEntity u, UUID requesterId) {
        boolean followedByMe = requesterId != null &&
                followRepository.existsByFollowerIdAndFolloweeId(requesterId, u.getId());
        return new UserSummaryResponse(
                u.getId().toString(),
                u.getUsername(),
                u.getDisplayName(),
                u.getAvatarUrl(),
                followRepository.countFollowers(u.getId()),
                followRepository.countFollowing(u.getId()),
                followedByMe
        );
    }

    /**
     * Public — used by FilmCommentService and other services in different packages.
     */
    public AuthorDto toAuthorDto(UserEntity u) {
        return new AuthorDto(
                u.getId().toString(),
                u.getUsername(),
                u.getDisplayName(),
                u.getAvatarUrl()
        );
    }

    private PostEntity.PostType parsePostType(String type) {
        if (type == null) return PostEntity.PostType.TEXT;
        try {
            return PostEntity.PostType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PostEntity.PostType.TEXT;
        }
    }
}