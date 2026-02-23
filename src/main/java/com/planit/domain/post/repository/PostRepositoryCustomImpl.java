package com.planit.domain.post.repository; // 커스텀 리포지토리 구현 패키지

import com.planit.domain.post.dto.PostDetailResponse; // DTO
import com.planit.domain.post.dto.PostDetailResponse.CommentInfo;
import com.planit.domain.post.dto.PostDetailResponse.PostImage;
import com.planit.domain.post.entity.BoardType;
import com.planit.infrastructure.storage.S3ImageUrlResolver;
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

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
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

    private static final Logger log = LoggerFactory.getLogger(PostRepositoryCustomImpl.class);

    @PersistenceContext
    private EntityManager entityManager; // 직접 native query 실행

    private final S3ImageUrlResolver imageUrlResolver;

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
                   u.profile_image_key,
                   (select count(1) from likes l where l.post_id = p.post_id) as like_count,
                   (select count(1) from comments c where c.post_id = p.post_id) as comment_count,
                   case
                     when :requesterId < 0 then 0
                     when exists(select 1 from likes l2 where l2.post_id = p.post_id and l2.author_id = :requesterId) then 1
                     else 0
                   end as liked,
                   post_plan.trip_id,
                   t.title as plan_title,
                   t.arrival_date,
                   t.departure_date,
                   (select i.s3_key
                    from posted_images pi
                    left join images i on i.id = pi.image_id
                    where pi.post_id = p.post_id
                      and pi.is_main_image = 1
                    order by pi.id asc
                    limit 1) as plan_thumbnail_key,
                   pp.place_id,
                   pl.name as place_name,
                   pp.google_place_id as place_google_place_id,
                   null as place_city,
                   null as place_country,
                   pp.rating as place_rating
            from posts p
            join users u on u.user_id = p.user_id and u.is_deleted = 0
            left join posted_plans post_plan on post_plan.post_id = p.post_id
            left join trips t on t.id = post_plan.trip_id
            left join posted_places pp on pp.post_id = p.post_id
            left join places pl on pl.place_id = pp.place_id
            where p.post_id = :postId
            """); // posts+users 조합
        baseQuery.setParameter("postId", postId);
        baseQuery.setParameter("requesterId", requesterId == null ? -1L : requesterId);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = baseQuery.getResultList();
        log.debug("findDetailById postId={} returned rows={}", postId, rows.size());
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = rows.get(0);
        Long authorId = ((Number) row[5]).longValue();
        String profileImageKey = (String) row[7];
        String profileImageUrl = imageUrlResolver.resolveOrNull(profileImageKey);
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
        String placeName = (String) row[17];
        String placeGooglePlaceId = (String) row[18];
        String placeImageUrl = null;
        String placeCity = (String) row[19];
        String placeCountry = (String) row[20];
        Integer placeRating = row[21] == null ? null : ((Number) row[21]).intValue();
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
                editable,
                placeName,
                placeGooglePlaceId,
                placeImageUrl,
                placeCity,
                placeCountry,
                placeRating
        ); // DTO 구성하여 반환
        return Optional.of(detail);
    }

    private List<PostImage> fetchImages(Long postId) {
        Query imageQuery = entityManager.createNativeQuery("""
            select pi.image_id, i.s3_key
            from posted_images pi
            left join images i on i.id = pi.image_id
            where pi.post_id = :postId
            order by pi.id asc
            limit 5
            """);
        imageQuery.setParameter("postId", postId);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = imageQuery.getResultList();
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<PostImage> images = new ArrayList<>();
        for (Object[] row : rows) {
            Long imageId = ((Number) row[0]).longValue();
            String s3Key = row[1] != null ? (String) row[1] : null;
            String url = imageUrlResolver.resolveOrNull(s3Key);
            images.add(new PostImage(imageId, s3Key, url));
        }
        return images;
    }

    private List<CommentInfo> fetchComments(Long postId, Long requesterId) {
        Query commentQuery = entityManager.createNativeQuery("""
            select c.comment_id,
                   c.author_id,
                   u.nickname,
                   u.profile_image_key,
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
            String profileImageKey = (String) row[3];
            String profileImageUrl = imageUrlResolver.resolve(profileImageKey);
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
