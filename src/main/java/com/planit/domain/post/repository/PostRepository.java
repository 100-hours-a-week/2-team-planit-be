package com.planit.domain.post.repository;

import com.planit.domain.post.entity.Post;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository
        extends JpaRepository<Post, Long>, PostRepositoryCustom {

    /**
     * 게시글 목록 Projection
     */
    interface PostSummary {
        Long getPostId();
        String getTitle();
        Long getAuthorId();
        String getAuthorNickname();
        String getAuthorProfileImageKey();
        LocalDateTime getCreatedAt();
        Long getLikeCount();
        Long getCommentCount();
        Long getRepresentativeImageId();
        String getRepresentativeImageKey();
        Double getRankingScore();
        String getPlaceName();
        String getTripTitle();
    }

    /**
     * 자유게시판 목록 조회
     * - Soft Delete 제외
     * - 제목/본문 검색
     * - 장소/여행 대표 정보 포함
     */
    @Query(
            value =
                    "select "
                            + "p.post_id as postId, "
                            + "p.title as title, "
                            + "u.user_id as authorId, "
                            + "u.nickname as authorNickname, "
                            + "u.profile_image_key as authorProfileImageKey, "
                            + "p.created_at as createdAt, "
                            + "(select count(1) from likes l "
                            + " where l.post_id = p.post_id "
                            + "   and l.created_at >= date_sub(current_date(), interval 1 year)) as likeCount, "
                            + "(select count(1) from comments c "
                            + " where c.post_id = p.post_id "
                            + "   and c.created_at >= date_sub(current_date(), interval 1 year)) as commentCount, "
                            + "(select pi.image_id from posted_images pi "
                            + " where pi.post_id = p.post_id "
                            + "   and pi.is_main_image = 1 "
                            + " order by pi.created_at asc limit 1) as representativeImageId, "
                            + "(select i.s3_key from posted_images pi "
                            + " join images i on i.id = pi.image_id "
                            + " where pi.post_id = p.post_id "
                            + "   and pi.is_main_image = 1 "
                            + " order by pi.created_at asc limit 1) as representativeImageKey, "
                            //없는 테이블 참조 문제 해결
                            + "null as rankingScore, "
                            + "(select pl.name from posted_places pp "
                            + " join places pl on pl.place_id = pp.place_id "
                            + " where pp.post_id = p.post_id "
                            + " limit 1) as placeName, "
                            + "null as tripTitle "
                            /*
                            + "(select pr.score from post_ranking_snapshots pr "
                            + " where pr.post_id = p.post_id "
                            + " order by pr.snapshot_date desc limit 1) as rankingScore, "
                            + "(select pl.name from posted_places pp "
                            + " join places pl on pl.place_id = pp.place_id "
                            + " where pp.post_id = p.post_id "
                            + " limit 1) as placeName, "
                            + "(select tr.title from posted_plans pt "
                            + " join trips tr on tr.id = pt.trip_id "
                            + " where pt.post_id = p.post_id "
                            + " limit 1) as tripTitle "
                             */
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
                    + ") "
                    + "order by case\n"
                            + " when :sortOption in ('COMMENTS', 'COMMENTS_1Y') then commentCount\n"
                            + " when :sortOption in ('LIKES', 'LIKES_1Y') then likeCount\n"
                            + " else p.created_at\n"
                            + " end desc",
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
    Page<PostSummary> searchByBoardType(
        @Param("boardType") String boardType,
        @Param("search") String search,
        @Param("sortOption") String sortOption,
        Pageable pageable
    );

    /**
     * 내 게시글 조회
     */
    List<Post> findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(
            Long authorId,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + 1 WHERE p.id = :postId")
    void incrementCommentCount(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Post p
        SET p.commentCount = CASE
            WHEN p.commentCount > 0 THEN p.commentCount - 1
            ELSE 0
        END
        WHERE p.id = :postId
        """)
    void decrementCommentCount(@Param("postId") Long postId);
}
