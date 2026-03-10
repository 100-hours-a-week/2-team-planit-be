package com.planit.domain.keywordalert.repository;

import com.planit.domain.keywordalert.entity.KeywordSubscription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KeywordSubscriptionRepository extends JpaRepository<KeywordSubscription, Long> {

    boolean existsByUserIdAndKeyword(Long userId, String keyword);

    List<KeywordSubscription> findByUserId(Long userId);

    List<KeywordSubscription> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<KeywordSubscription> findByIdAndUserId(Long id, Long userId);

    @Query("""
        select ks
        from KeywordSubscription ks
        where locate(ks.keyword, :title) > 0
           or locate(ks.keyword, :content) > 0
    """)
    List<KeywordSubscription> findMatchingKeywords(@Param("title") String title, @Param("content") String content);
}
