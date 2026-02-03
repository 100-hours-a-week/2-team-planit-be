-- 좋아요(post_id, author_id) 조합이 아닌 단독 post_id로만 UNIQUE 제약이 걸려 있던
-- 기존 인덱스를 제거하고 (post_id, author_id) 조합으로 유니크 제약을 다시 걸어줍니다.
ALTER TABLE likes
  DROP INDEX uk_likes_post_author;

ALTER TABLE likes
  ADD CONSTRAINT uk_likes_post_author UNIQUE (post_id, author_id);
