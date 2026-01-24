package com.planit.domain.post.repository; // 게시글 도메인 저장소 패키지

import java.util.Optional; // PostDetail 커스텀 조회용 Optional 타입
import org.springframework.data.domain.Page; // 페이징 결과 타입
import org.springframework.data.domain.Pageable; // Pageable 파라미터
import org.springframework.data.jpa.repository.JpaRepository; // 기본 JPA 저장소
import org.springframework.data.jpa.repository.Query; // Native Query 정의
import org.springframework.data.repository.query.Param; // 쿼리 파라미터 바인딩

public interface PostRepository extends JpaRepository<com.planit.domain.post.entity.Post, Long>, PostRepositoryCustom {

    interface PostSummary {
        Long getPostId(); // 게시글 PK
        String getTitle(); // 제목
        Long getAuthorId(); // 작성자 PK
        String getAuthorNickname(); // 작성자 닉네임
        Long getAuthorProfileImageId(); // 프로필 이미지 ID
        java.time.LocalDateTime getCreatedAt(); // 작성 시간
        Long getLikeCount(); // 최근 1년 좋아요
        Long getCommentCount(); // 최근 1년 댓글
        Long getRepresentativeImageId(); // 대표 이미지 ID (null)
        Double getRankingScore(); // ranking snapshot 값
    }

    // 자유게시판 목록용 네이티브 쿼리: posts + users + user_image + likes/comments + ranking 스냅샷 결합
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
                + " order by pr.snapshot_date desc limit 1) as rankingScore "
                + "from posts p "
                + "join users u on u.user_id = p.user_id and u.is_deleted = 0 "
                + "left join user_image ui on ui.user_id = u.user_id "
                + "where p.board_type = :boardType "
                + "and (coalesce(:search,'') = '' "
                + " or lower(p.title) like lower(:pattern) "
                + " or lower(p.content) like lower(:pattern)) "
                + "order by p.created_at desc ",
        countQuery =
            "select count(*) from posts p "
                + "join users u on u.user_id = p.user_id and u.is_deleted = 0 "
                + "where p.board_type = :boardType "
                + "and (coalesce(:search,'') = '' "
                + " or lower(p.title) like lower(:pattern) "
                + " or lower(p.content) like lower(:pattern)) ",
        nativeQuery = true
    )
    Page<PostSummary> searchByBoardType(
        @Param("boardType") String boardType,
        @Param("search") String search,
        @Param("pattern") String pattern,
        Pageable pageable
    );
}
