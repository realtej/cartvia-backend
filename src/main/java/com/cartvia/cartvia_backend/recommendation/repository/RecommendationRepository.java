package com.cartvia.cartvia_backend.recommendation.repository;

import com.cartvia.cartvia_backend.common.enums.RecommendationType;
import com.cartvia.cartvia_backend.recommendation.entity.RecommendationEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecommendationRepository extends JpaRepository<RecommendationEntry, UUID> {

    List<RecommendationEntry> findByUser_IdAndTypeOrderByScoreDesc(UUID userId, RecommendationType type);

    List<RecommendationEntry> findByProduct_IdAndTypeOrderByScoreDesc(UUID productId, RecommendationType type);

    List<RecommendationEntry> findByCategoryAndTypeOrderByScoreDesc(String category, RecommendationType type);

    List<RecommendationEntry> findByTypeOrderByScoreDesc(RecommendationType type);

    void deleteAll();
}
