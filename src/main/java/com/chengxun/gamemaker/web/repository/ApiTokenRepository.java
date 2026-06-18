package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.ApiToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiTokenRepository extends JpaRepository<ApiToken, Long> {

    List<ApiToken> findByStatus(ApiToken.TokenStatus status);

    long countByStatus(ApiToken.TokenStatus status);

    List<ApiToken> findByStatusOrderByCreatedAtDesc(ApiToken.TokenStatus status);
}
