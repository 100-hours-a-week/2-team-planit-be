package com.planit.domain.keywordalert.query.repository;

import com.planit.domain.keywordalert.entity.KeywordSubscription;
import com.planit.domain.keywordalert.query.projection.KeywordSubscriptionProjection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface KeywordSubscriptionQueryRepository extends Repository<KeywordSubscription, Long> {

    @Query(value = """
            select
                ks.subscription_id as subscriptionId,
                ks.keyword as keyword
            from keyword_subscriptions ks
            join users u on u.user_id = ks.user_id and u.is_deleted = 0
            where u.login_id = :loginId
            order by ks.created_at desc
            """, nativeQuery = true)
    List<KeywordSubscriptionProjection> findByLoginId(@Param("loginId") String loginId);
}
