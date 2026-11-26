package com.cinemabook.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface FilmRepository extends JpaRepository<FilmEntity, UUID> {

    Page<FilmEntity> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId, Pageable pageable);

    Page<FilmEntity> findByOwnerIdAndStatusOrderByCreatedAtDesc(
            UUID ownerId, FilmEntity.FilmStatus status, Pageable pageable);

    Page<FilmEntity> findByStatusOrderByCreatedAtDesc(
            FilmEntity.FilmStatus status, Pageable pageable);

    long countByOwnerId(UUID ownerId);

    long countByOwnerIdAndStatus(UUID ownerId, FilmEntity.FilmStatus status);

    @Query("SELECT COALESCE(SUM(f.totalRevenue), 0) FROM FilmEntity f WHERE f.owner.id = :ownerId")
    BigDecimal sumRevenueByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("SELECT COALESCE(SUM(f.totalTicketsSold), 0) FROM FilmEntity f WHERE f.owner.id = :ownerId")
    long sumTicketsSoldByOwnerId(@Param("ownerId") UUID ownerId);

    @Modifying
    @Query("UPDATE FilmEntity f SET f.status = :status WHERE f.id = :id")
    void updateStatus(@Param("id") UUID id,
                      @Param("status") FilmEntity.FilmStatus status);

    @Modifying
    @Query("UPDATE FilmEntity f SET f.ticketPrice = :price WHERE f.id = :id AND f.owner.id = :ownerId")
    int updatePrice(@Param("id") UUID id,
                    @Param("ownerId") UUID ownerId,
                    @Param("price") BigDecimal price);

    @Modifying
    @Query("""
        UPDATE FilmEntity f
        SET f.totalTicketsSold = f.totalTicketsSold + 1,
            f.totalRevenue     = f.totalRevenue + :amount
        WHERE f.id = :id
    """)
    void incrementTicketStats(@Param("id") UUID id,
                              @Param("amount") BigDecimal amount);
}