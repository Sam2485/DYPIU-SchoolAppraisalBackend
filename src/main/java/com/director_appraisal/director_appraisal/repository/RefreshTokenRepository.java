package com.director_appraisal.director_appraisal.repository;

import com.director_appraisal.director_appraisal.model.RefreshToken;
import com.director_appraisal.director_appraisal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    int deleteByUser(User user);

    @Modifying
    int deleteByExpiryDateBefore(Instant now);
}
