package com.cinemabook.application.service;

import com.cinemabook.api.dto.SocialDtos.*;
import com.cinemabook.infrastructure.persistence.*;
import com.cinemabook.shared.exception.ResourceNotFoundException;
import com.cinemabook.shared.exception.UnauthorisedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Single Responsibility: film comment threads only.
 * Kept separate from SocialService (OCP — film comments can evolve independently).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FilmCommentService {

    private final FilmCommentRepository filmCommentRepository;
    private final LikeRepository        likeRepository;
    private final UserRepository        userRepository;
    private final FilmRepository        filmRepository;

    // ── Add comment ───────────────────────────────────────────────────────────

    @Transactional
    public CommentResponse addComment(UUID authorId, UUID filmId,
                                      CreateCommentRequest req) {
        UserEntity author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", authorId));
        FilmEntity film = filmRepository.findById(filmId)
                .orElseThrow(() -> new ResourceNotFoundException("Film", filmId));

        FilmCommentEntity comment = FilmCommentEntity.builder()
                .author(author)
                .film(film)
                .content(req.content())
                .build();

        comment = filmCommentRepository.save(comment);
        log.info("Film comment added: commentId={} filmId={} authorId={}",
                comment.getId(), filmId, authorId);

        return toResponse(comment, authorId);
    }

    // ── Delete comment ────────────────────────────────────────────────────────

    @Transactional
    public void deleteComment(UUID authorId, UUID commentId) {
        if (!filmCommentRepository.existsByIdAndAuthorId(commentId, authorId)) {
            throw new UnauthorisedException("You can only delete your own comments.");
        }
        filmCommentRepository.deleteById(commentId);
        log.info("Film comment deleted: commentId={} authorId={}", commentId, authorId);
    }

    // ── Get comments ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CommentPageResponse getFilmComments(UUID filmId, UUID requesterId,
                                               int page, int size) {
        Page<FilmCommentEntity> result = filmCommentRepository
                .findByFilmIdOrderByCreatedAtDesc(filmId, PageRequest.of(page, size));

        return new CommentPageResponse(
                result.getContent().stream()
                        .map(c -> toResponse(c, requesterId)).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    // ── Like comment ──────────────────────────────────────────────────────────

    @Transactional
    public LikeResponse toggleCommentLike(UUID userId, UUID commentId) {
        FilmCommentEntity comment = filmCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (likeRepository.existsByUserIdAndCommentId(userId, commentId)) {
            likeRepository.deleteByUserIdAndCommentId(userId, commentId);
            filmCommentRepository.adjustLikesCount(commentId, -1);
            return new LikeResponse(false, Math.max(0, comment.getLikesCount() - 1));
        } else {
            likeRepository.save(LikeEntity.builder()
                    .user(user).comment(comment).build());
            filmCommentRepository.adjustLikesCount(commentId, 1);
            return new LikeResponse(true, comment.getLikesCount() + 1);
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private CommentResponse toResponse(FilmCommentEntity c, UUID requesterId) {
        boolean likedByMe  = requesterId != null &&
                likeRepository.existsByUserIdAndCommentId(requesterId, c.getId());
        boolean ownedByMe  = requesterId != null &&
                c.getAuthor().getId().equals(requesterId);
        UserEntity author  = c.getAuthor();
        return new CommentResponse(
                c.getId().toString(),
                new AuthorDto(
                        author.getId().toString(),
                        author.getUsername(),
                        author.getDisplayName(),
                        author.getAvatarUrl()),
                c.getContent(),
                c.getLikesCount(),
                likedByMe,
                ownedByMe,
                c.getCreatedAt()
        );
    }
}