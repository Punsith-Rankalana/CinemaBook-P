package com.cinemabook.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FilmUploadJobRepository extends JpaRepository<FilmUploadJobEntity, UUID> {

    Optional<FilmUploadJobEntity> findByFilmId(UUID filmId);

    List<FilmUploadJobEntity> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    @Modifying
    @Query("UPDATE FilmUploadJobEntity j SET j.jobStatus = :status, j.errorMessage = :error WHERE j.id = :id")
    void updateStatus(@Param("id") UUID id,
                      @Param("status") FilmUploadJobEntity.JobStatus status,
                      @Param("error") String error);
}