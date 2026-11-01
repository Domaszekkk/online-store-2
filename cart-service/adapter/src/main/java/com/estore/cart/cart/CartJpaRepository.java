package com.estore.cart.cart;

import com.estore.cart.CartStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface CartJpaRepository extends JpaRepository<CartEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cart FROM CartEntity cart WHERE cart.id = :id")
    Optional<CartEntity> findByIdForUpdate(@Param("id") UUID id);

    @Modifying
    @Query("""
            UPDATE CartEntity cart SET cart.status = :expired
            WHERE cart.status = :active AND cart.expiresAt <= :now
            """)
    int markExpired(@Param("now") Instant now,
                    @Param("active") CartStatus active,
                    @Param("expired") CartStatus expired);

    long deleteByStatusAndExpiresAtBefore(CartStatus status, Instant cutoff);
}
