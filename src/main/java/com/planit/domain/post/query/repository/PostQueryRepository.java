package com.planit.domain.post.query.repository;

import com.planit.domain.post.entity.Post;
import com.planit.domain.post.query.projection.PostDetailProjection;
import com.planit.domain.post.query.projection.PostSummaryProjection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface PostQueryRepository extends Repository<Post, Long> {

    @Query(
            value =
                    "select "
                            + "p.post_id as postId, "
                            + "p.title as title, "
                            + "u.user_id as authorId, "
                            + "u.nickname as authorNickname, "
                            + "u.profile_image_key as authorProfileImageKey, "
                            + "p.created_at as createdAt, "
                            + "coalesce(plc.like_count, 0) as likeCount, "
                            + "coalesce(pcc.comment_count, 0) as commentCount, "
                            + "coalesce(pvc.view_count, 0) as viewCount, "
                            + "(select pi.image_id from posted_images pi "
                            + " where pi.post_id = p.post_id "
                            + "   and pi.is_main_image = 1 "
                            + " order by pi.created_at asc limit 1) as representativeImageId, "
                            + "(select i.s3_key from posted_images pi "
                            + " join images i on i.id = pi.image_id "
                            + " where pi.post_id = p.post_id "
                            + "   and pi.is_main_image = 1 "
                            + " order by pi.created_at asc limit 1) as representativeImageKey, "
                            + "null as rankingScore, "
                            + "(select pl.photo_url from posted_places pp "
                            + " join places pl on pl.place_id = pp.place_id "
                            + " where pp.post_id = p.post_id "
                            + " limit 1) as placeImageUrl, "
                            + "(select pl.name from posted_places pp "
                            + " join places pl on pl.place_id = pp.place_id "
                            + " where pp.post_id = p.post_id "
                            + " limit 1) as placeName, "
                            + "p.google_place_id as googlePlaceId, "
                            + "null as tripTitle, "
                            + "p.board_type as boardType "
                            + "from posts p "
                            + "join users u on u.user_id = p.user_id "
                            + " and u.is_deleted = 0 "
                            + "left join post_like_count plc on plc.post_id = p.post_id "
                            + "left join post_comment_count pcc on pcc.post_id = p.post_id "
                            + "left join post_view_count pvc on pvc.post_id = p.post_id "
                            + "where p.board_type = :boardType "
                            + "and p.is_deleted = 0 "
                            + "and ( :search = '' "
                            + "   or p.title like concat('%', :search, '%') "
                            + "   or p.content like concat('%', :search, '%') "
                            + "   or exists ("
                            + "       select 1 from posted_places pp "
                            + "       join places pl on pl.place_id = pp.place_id "
                            + "       where pp.post_id = p.post_id "
                            + "         and pl.name like concat('%', :search, '%')"
                            + "   )"
                            + ")",
            countQuery =
                    "select count(*) "
                            + "from posts p "
                            + "join users u on u.user_id = p.user_id "
                            + " and u.is_deleted = 0 "
                            + "where p.board_type = :boardType "
                            + "and p.is_deleted = 0 "
                            + "and ( :search = '' "
                            + "   or p.title like concat('%', :search, '%') "
                            + "   or p.content like concat('%', :search, '%') "
                            + "   or exists ("
                            + "       select 1 from posted_places pp "
                            + "       join places pl on pl.place_id = pp.place_id "
                            + "       where pp.post_id = p.post_id "
                            + "         and pl.name like concat('%', :search, '%')"
                            + "   )"
                            + ")",
            nativeQuery = true
    )
    Page<PostSummaryProjection> findPostSummaries(
            @Param("boardType") String boardType,
            @Param("search") String search,
            Pageable pageable
    );

    @Query(
            value = """
                    select p.post_id as postId,
                           p.title as title,
                           p.content as content,
                           p.board_type as boardType,
                           p.created_at as createdAt,
                           u.user_id as authorId,
                           u.nickname as authorNickname,
                           u.profile_image_key as authorProfileImageKey,
                           coalesce(plc.like_count, 0) as likeCount,
                           coalesce(pcc.comment_count, 0) as commentCount,
                           coalesce(pvc.view_count, 0) as viewCount,
                           case
                             when :requesterId < 0 then 0
                             when exists(select 1 from likes l2 where l2.post_id = p.post_id and l2.author_id = :requesterId) then 1
                             else 0
                           end as likedByRequester,
                           post_plan.trip_id as planTripId,
                           t.title as tripTitle,
                           (select i.s3_key
                            from posted_images pi
                            left join images i on i.id = pi.image_id
                            where pi.post_id = p.post_id
                              and pi.is_main_image = 1
                            order by pi.id asc
                            limit 1) as planThumbnailKey,
                           p.place_name as placeName,
                           p.google_place_id as googlePlaceId,
                           pl.city as placeCity,
                           pl.country as placeCountry,
                           pp.rating as placeRating
                    from posts p
                    join users u on u.user_id = p.user_id and u.is_deleted = 0
                    left join post_like_count plc on plc.post_id = p.post_id
                    left join post_comment_count pcc on pcc.post_id = p.post_id
                    left join post_view_count pvc on pvc.post_id = p.post_id
                    left join posted_plans post_plan on post_plan.post_id = p.post_id
                    left join trips t on t.id = post_plan.trip_id
                    left join posted_places pp on pp.post_id = p.post_id
                    left join places pl on pl.place_id = pp.place_id
                    where p.post_id = :postId
                      and p.is_deleted = 0
                    """,
            nativeQuery = true
    )
    Optional<PostDetailProjection> findPostDetail(
            @Param("postId") Long postId,
            @Param("requesterId") Long requesterId
    );

    @Query(
            value = """
                    select pi.image_id as imageId, i.s3_key as s3Key
                    from posted_images pi
                    left join images i on i.id = pi.image_id
                    where pi.post_id = :postId
                    order by pi.id asc
                    limit 5
                    """,
            nativeQuery = true
    )
    List<PostImageProjection> findPostImages(@Param("postId") Long postId);

    @Query(
            value = """
                    select c.comment_id as commentId,
                           c.author_id as authorId,
                           u.nickname as authorNickname,
                           u.profile_image_key as authorProfileImageKey,
                           c.content as content,
                           c.created_at as createdAt
                    from comments c
                    join users u on u.user_id = c.author_id and u.is_deleted = 0
                    where c.post_id = :postId
                      and c.deleted_at is null
                    order by c.created_at asc
                    limit :limit
                    """,
            nativeQuery = true
    )
    List<PostCommentProjection> findPostComments(@Param("postId") Long postId, @Param("limit") int limit);

    @Query(
            value = """
                    select pt.post_id as postId, pt.trip_id as tripId
                    from posted_plans pt
                    where pt.post_id in (:postIds)
                    """,
            nativeQuery = true
    )
    List<PostTripIdProjection> findTripIdsByPostIds(@Param("postIds") List<Long> postIds);

    @Query(
            value = """
                    select d.trip_id as tripId, p.place_id as placeId
                    from itinerary_days d
                    join itinerary_item_places p on p.itinerary_day_id = d.id
                    where d.trip_id in (:tripIds)
                    order by d.trip_id, d.day_index asc, p.event_order asc
                    """,
            nativeQuery = true
    )
    List<TripPlaceProjection> findFirstPlacesByTripIds(@Param("tripIds") List<Long> tripIds);

    @Query(
            value = """
                    select u.user_id
                    from users u
                    where u.login_id = :loginId
                      and u.is_deleted = 0
                    limit 1
                    """,
            nativeQuery = true
    )
    Optional<Long> findActiveUserIdByLoginId(@Param("loginId") String loginId);

    interface PostImageProjection {
        Long getImageId();
        String getS3Key();
    }

    interface PostCommentProjection {
        Long getCommentId();
        Long getAuthorId();
        String getAuthorNickname();
        String getAuthorProfileImageKey();
        String getContent();
        LocalDateTime getCreatedAt();
    }

    interface PostTripIdProjection {
        Long getPostId();
        Long getTripId();
    }

    interface TripPlaceProjection {
        Long getTripId();
        Long getPlaceId();
    }
}
