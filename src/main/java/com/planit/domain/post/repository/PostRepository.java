package com.planit.domain.post.repository;

import com.planit.domain.post.entity.Post;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
        Long getAuthorProfileImageId();
        LocalDateTime getCreatedAt();
        Long getLikeCount();
        Long getCommentCount();
        Long getRepresentativeImageId();
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
                            + "ui.image_id as authorProfileImageId, "
                            + "p.created_at as createdAt, "
                            + "(select count(1) from likes l "
                            + " where l.post_id = p.post_id "
                            + "   and l.created_at >= date_sub(current_date(), interval 1 year)) as likeCount, "
                            + "(select count(1) from comments c "
                            + " where c.post_id = p.post_id "
                            + "   and c.created_at >= date_sub(current_date(), interval 1 year)) as commentCount, "
                            + "null as representativeImageId, "
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
                            + "from posts p "
                            + "join users u on u.user_id = p.user_id "
                            + " and u.is_deleted = 0 "
                            + "left join user_image ui on ui.user_id = u.user_id "
                            + "where p.board_type = :boardType "
                            + "and p.is_deleted = 0 "
                            + "and ( :pattern = '%' "
                            + "   or lower(p.title) like lower(:pattern) "
                            + "   or lower(p.content) like lower(:pattern) ) "
                            + "order by p.created_at desc",
            countQuery =
                    "select count(*) "
                            + "from posts p "
                            + "join users u on u.user_id = p.user_id "
                            + " and u.is_deleted = 0 "
                            + "where p.board_type = :boardType "
                            + "and p.is_deleted = 0 "
                            + "and ( :pattern = '%' "
                            + "   or lower(p.title) like lower(:pattern) "
                            + "   or lower(p.content) like lower(:pattern) )",
            nativeQuery = true
    )
    Page<PostSummary> searchByBoardType(
            @Param("boardType") String boardType,
            @Param("pattern") String pattern,
            Pageable pageable
    );

    /**
     * 내 게시글 조회
     */
    List<Post> findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(
            Long authorId,
            Pageable pageable
    );
}

