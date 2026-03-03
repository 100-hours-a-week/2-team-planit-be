package com.planit.domain.post.repository;

import com.planit.domain.post.dto.MyPostSummaryProjection;
import com.planit.domain.post.dto.PostSummaryProjection;
import com.planit.domain.post.entity.Post;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository
        extends JpaRepository<Post, Long>, PostRepositoryCustom {

    @Query(
            value = """
                    select
                        p.post_id as postId,
                        p.title as title,
                        u.user_id as authorId,
                        u.nickname as authorNickname,
                        u.profile_image_key as authorProfileImageKey,
                        pi.image_id as representativeImageId,
                        i.s3_key as representativeImageKey,
                        pl.name as placeName,
                        pl.photo_url as placeImageUrl,
                        coalesce(c.comment_count, 0) as commentCount,
                        coalesce(c.comment_count_1year, 0) as commentCount1Year,
                        coalesce(l.like_count, 0) as likeCount,
                        coalesce(l.like_count_1year, 0) as likeCount1Year,
                        case when coalesce(l.liked_by_login_user, 0) > 0 then true else false end as likedByLoginUser,
                        p.created_at as createdAt
                    from posts p
                    join users u on u.user_id = p.user_id and u.is_deleted = 0
                    left join posted_images pi on pi.post_id = p.post_id and pi.is_main_image = 1
                    left join images i on i.id = pi.image_id
                    left join (
                        select pp.post_id, min(pp.posted_place_id) as representative_posted_place_id
                        from posted_places pp
                        group by pp.post_id
                    ) rep_place on rep_place.post_id = p.post_id
                    left join posted_places pp on pp.posted_place_id = rep_place.representative_posted_place_id
                    left join places pl on pl.place_id = pp.place_id
                    left join (
                        select post_id,
                               count(*) as comment_count,
                               sum(case when created_at >= date_sub(current_date(), interval 1 year) then 1 else 0 end) as comment_count_1year
                        from comments
                        where deleted_at is null
                        group by post_id
                    ) c on c.post_id = p.post_id
                    left join (
                        select post_id,
                               count(*) as like_count,
                               sum(case when created_at >= date_sub(current_date(), interval 1 year) then 1 else 0 end) as like_count_1year,
                               sum(case when author_id = :loginUserId then 1 else 0 end) as liked_by_login_user
                        from likes
                        group by post_id
                    ) l on l.post_id = p.post_id
                    where p.board_type = :boardType
                      and p.is_deleted = 0
                      and ( :search = ''
                        or p.title like concat('%', :search, '%')
                        or p.content like concat('%', :search, '%')
                        or exists (
                            select 1
                            from posted_places pp2
                            join places pl2 on pl2.place_id = pp2.place_id
                            where pp2.post_id = p.post_id
                              and pl2.name like concat('%', :search, '%')
                        )
                      )
                      and ( :filteredPostIdsSize = 0 or p.post_id in (:filteredPostIds) )
                    order by case
                        when :sortOption in ('COMMENTS', 'COMMENTS_1Y') then
                            case when :sortOption = 'COMMENTS_1Y' then coalesce(c.comment_count_1year, 0) else coalesce(c.comment_count, 0) end
                        when :sortOption in ('LIKES', 'LIKES_1Y') then
                            case when :sortOption = 'LIKES_1Y' then coalesce(l.like_count_1year, 0) else coalesce(l.like_count, 0) end
                        else p.created_at
                    end desc
                    """,
            countQuery =
                    """
                    select count(*)
                    from posts p
                    join users u on u.user_id = p.user_id and u.is_deleted = 0
                    where p.board_type = :boardType
                      and p.is_deleted = 0
                      and ( :search = ''
                        or p.title like concat('%', :search, '%')
                        or p.content like concat('%', :search, '%')
                        or exists (
                            select 1
                            from posted_places pp2
                            join places pl2 on pl2.place_id = pp2.place_id
                            where pp2.post_id = p.post_id
                              and pl2.name like concat('%', :search, '%')
                        )
                      )
                      and ( :filteredPostIdsSize = 0 or p.post_id in (:filteredPostIds) )
                    """,
            nativeQuery = true
    )
    Page<PostSummaryProjection> searchByBoardType(
            @Param("boardType") String boardType,
            @Param("search") String search,
            @Param("sortOption") String sortOption,
            @Param("filteredPostIds") List<Long> filteredPostIds,
            @Param("filteredPostIdsSize") int filteredPostIdsSize,
            @Param("loginUserId") Long loginUserId,
            Pageable pageable
    );

    @Query(value =
            "select p.post_id as postId, p.title as title, p.board_type as boardType, p.created_at as createdAt "
                    + "from posts p "
                    + "where p.user_id = :authorId "
                    + "and p.is_deleted = 0 "
                    + "order by p.created_at desc",
            countQuery =
                    "select count(*) from posts p where p.user_id = :authorId and p.is_deleted = 0",
            nativeQuery = true)
    Page<MyPostSummaryProjection> findMyPosts(
            @Param("authorId") Long authorId,
            Pageable pageable
    );
}