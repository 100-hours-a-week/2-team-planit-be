package com.planit.domain.post.repository; // 커스텀 리포지토리 구현 패키지

import com.planit.domain.post.dto.PostDetailResponse; // DTO
import com.planit.domain.post.dto.PostDetailResponse.CommentInfo;
import com.planit.domain.post.dto.PostDetailResponse.PostImage;
import com.planit.domain.post.entity.BoardType;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

@Repository // 커스텀 리포지토리 구현체
public class PostRepositoryCustomImpl implements PostRepositoryCustom {

    private static final int MAX_COMMENT_PAGE_SIZE = 20; // 20개 댓글씩 조회
    private static final Map<BoardType, String> BOARD_NAMES = Map.of(
        BoardType.FREE, "자유게시판",
        BoardType.PLAN_SHARE, "일정 공유",
        BoardType.PLACE_RECOMMEND, "장소 추천"
    ); // 게시판 이름 매핑
    private static final Map<BoardType, String> BOARD_DESCRIPTIONS = Map.of(
        BoardType.FREE, "자유롭게 이야기하는 공간",
        BoardType.PLAN_SHARE, "함께 일정 계획을 공유",
        BoardType.PLACE_RECOMMEND, "좋은 장소 추천하기"
    ); // 게시판 설명

    @PersistenceContext
    private EntityManager entityManager; // 직접 native query 실행

    @Override
    public Optional<PostDetailResponse> findDetailById(Long postId, Long requesterId) {
        Query baseQuery = entityManager.createNativeQuery("""
            select p.post_id,
                   p.title,
                   p.content,
                   p.created_at,
                   p.board_type,
            u.user_id,
            u.nickname,
            u.profile_image_url,
                   (select count(1) from likes l where l.post_id = p.post_id) as like_count,
                   (select count(1) from comments c where c.post_id = p.post_id) as comment_count,
                   case
                     when :requesterId < 0 then 0
                     when exists(select 1 from likes l2 where l2.post_id = p.post_id and l2.user_id = :requesterId) then 1
                     else 0
                   end as liked
            from posts p
            join users u on u.user_id = p.user_id and u.is_deleted = 0
            where p.post_id = :postId
            """); // posts+users 조합
        baseQuery.setParameter("postId", postId);
        baseQuery.setParameter("requesterId", requesterId == null ? -1L : requesterId);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = baseQuery.getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = rows.get(0);
        Long authorId = ((Number) row[5]).longValue();
        String profileImageUrl = (String) row[7];
        String boardTypeValue = (String) row[4];
        BoardType boardType = BoardType.FREE;
        if (boardTypeValue != null) {
            try {
                boardType = BoardType.valueOf(boardTypeValue);
            } catch (IllegalArgumentException ignored) {
            }
        }
        PostDetailResponse.AuthorInfo author = new PostDetailResponse.AuthorInfo(
            authorId,
            (String) row[6],
            profileImageUrl
        ); // 작성자 정보 구성
        Long likeCount = row[8] == null ? 0L : ((Number) row[8]).longValue();
        Long commentCount = row[9] == null ? 0L : ((Number) row[9]).longValue();
        Boolean liked = row[10] != null && ((Number) row[10]).longValue() == 1L;
        List<PostImage> images = fetchImages(postId); // 최대 5개 이미지 조회
        List<CommentInfo> comments = fetchComments(postId, requesterId); // 댓글 조회
        boolean editable = requesterId != null && requesterId.equals(authorId); // 수정/삭제 버튼 판단
        String boardName = BOARD_NAMES.getOrDefault(boardType, "자유게시판");
        String boardDescription = BOARD_DESCRIPTIONS.getOrDefault(boardType, "자유롭게 이야기하는 공간");
        Timestamp createdTimestamp = (Timestamp) row[3];
        LocalDateTime createdAt = createdTimestamp == null ? null : createdTimestamp.toLocalDateTime();
        PostDetailResponse detail = new PostDetailResponse(
            ((Number) row[0]).longValue(),
            boardName,
            boardDescription,
            (String) row[1],
            (String) row[2],
            createdAt,
            author,
            images,
            likeCount.intValue(),
            commentCount.intValue(),
            liked,
            comments,
            editable
        ); // DTO 구성하여 반환
        return Optional.of(detail);
    }

    private List<PostImage> fetchImages(Long postId) {
        Query imageQuery = entityManager.createNativeQuery("""
            select pi.image_id
            from posted_images pi
            where pi.post_id = :postId
            order by pi.id asc
            limit 5
            """); // posted_images에서 최대 5장 이미지 조회
        imageQuery.setParameter("postId", postId);
        @SuppressWarnings("unchecked")
        List<Number> results = imageQuery.getResultList();
        if (results.isEmpty()) {
            return Collections.emptyList();
        }
        List<PostImage> images = new ArrayList<>();
        for (Number value : results) {
            images.add(new PostImage(value.longValue()));
        }
        return images;
    }

    private List<CommentInfo> fetchComments(Long postId, Long requesterId) {
        Query commentQuery = entityManager.createNativeQuery("""
            select c.comment_id,
                   c.author_id,
                   u.nickname,
                   u.profile_image_url,
                   c.content,
                   c.created_at
            from comments c
            join users u on u.user_id = c.author_id and u.is_deleted = 0
            where c.post_id = :postId
              and c.deleted_at is null
            order by c.created_at asc
            limit :limit
            """); // 댓글 20개 오래된 순
        commentQuery.setParameter("postId", postId);
        commentQuery.setParameter("limit", MAX_COMMENT_PAGE_SIZE);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = commentQuery.getResultList();
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<CommentInfo> comments = new ArrayList<>();
        for (Object[] row : rows) {
            Long authorId = ((Number) row[1]).longValue();
            String profileImageUrl = (String) row[3];
            boolean deletable = requesterId != null && requesterId.equals(authorId);
            Timestamp commentTimestamp = (Timestamp) row[5];
            LocalDateTime commentCreatedAt = commentTimestamp == null ? null : commentTimestamp.toLocalDateTime();
            comments.add(new CommentInfo(
                ((Number) row[0]).longValue(),
                authorId,
                (String) row[2],
                profileImageUrl,
                (String) row[4],
                commentCreatedAt,
                deletable
            ));
        }
        return comments;
    }
}
