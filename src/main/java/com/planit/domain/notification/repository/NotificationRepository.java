package com.planit.domain.notification.repository;

import com.planit.domain.notification.entity.Notification;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
        select n from Notification n
        where n.userId = :userId
          and (:isRead is null or n.isRead = :isRead)
          and (:cursor is null or n.notificationId < :cursor)
        order by n.notificationId desc
        """)
    List<Notification> findPage(@Param("userId") Long userId,
                                @Param("isRead") Boolean isRead,
                                @Param("cursor") Long cursor,
                                Pageable pageable);

    long countByUserIdAndIsReadFalse(Long userId);
}
