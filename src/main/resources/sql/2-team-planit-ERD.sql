CREATE TABLE `ai_chat_messages` (
	`id`	BIGINT	NOT NULL	COMMENT 'AI 챗봇 메시지를 식별하기 위한 기본 키',
	`room_id`	BIGINT	NOT NULL	COMMENT '메시지가 속한 채팅방 ID',
	`content`	TEXT	NOT NULL	COMMENT 'AI가 생성한 메시지 본문',
	`created_at`	DATETIME	NOT NULL	COMMENT 'AI 메시지가 생성된 시각'
);

CREATE TABLE `itinerary_items` (
	`id`	BIGINT	NOT NULL	COMMENT '일정 항목을 식별하기 위한 기본 키',
	`travel_id`	BIGINT	NOT NULL	COMMENT '해당 일정이 속한 여행',
	`day_index`	INT	NOT NULL	COMMENT '여행 내 몇 번째 날(N일차)인지 나타내는 값',
	`place_item_id`	BIGINT	NULL	COMMENT '해당 일정에 연결된 장소 항목'
);

CREATE TABLE `group_members` (
	`id`	BIGINT	NOT NULL	COMMENT '그룹원 식별자',
	`user_id`	INT	NOT NULL	COMMENT '사용자 ID',
	`group_id`	INT	NOT NULL	COMMENT '그룹 ID'
);

CREATE TABLE `trips` (
	`id`	BIGINT	NOT NULL	COMMENT '여행을 식별하는 기본 키',
	`group_id`	INT	NOT NULL	COMMENT '여행이 속한 그룹 ID',
	`title`	VARCHAR(15)	NOT NULL	COMMENT '여행 제목',
	`arrival_date`	DATE	NULL	COMMENT '항공편 도착 날짜',
	`departure_date`	DATE	NULL	COMMENT '항공편 출발 날짜',
	`arrival_time`	TIME	NULL	COMMENT '항공편 도착 시간',
	`departure_time`	TIME	NULL	COMMENT '항공편 출발 시간',
	`travel_city`	VARCHAR(50)	NULL	COMMENT '여행 도시  (추천 로직 기준 지역)',
	`members_number`	INT	NULL	COMMENT '여행 인원 수',
	`total_budget`	INT	NULL	COMMENT '여행 총 예산',
	`travel_theme`	VARCHAR(10)	NULL	COMMENT '여행 테마',
	`wanted_place`	TEXT	NULL	COMMENT '희망 방문 장소',
	`persona`	VARCHAR(100)	NULL	COMMENT '여행 성향 (페르소나)'
);

CREATE TABLE `chat_rooms` (
	`room_id`	BIGINT	NOT NULL	COMMENT '채팅방을 식별하기 위한 기본 키',
	`group_id`	BIGINT	NOT NULL	COMMENT '채팅방이 속한 여행 그룹 ID',
	`total_messages_number`	INT	NULL	COMMENT '채팅방 내 총 메시지 개수'
);

CREATE TABLE `itinerary_item_transports` (
	`transport_id`	BIGINT	NOT NULL	COMMENT '이동 정보 레코드를 식별하는 기본 키',
	`itinerary_item_id`	BIGINT	NOT NULL	COMMENT '해당 이동이 속한 일정 항목',
	`transport`	VARCHAR(20)	NOT NULL	COMMENT '이동 수단 (예 : WALK, CAR, BUS, SUBWAY 등)',
	`event_order`	INT	NOT NULL	COMMENT '일정 내 이동 이벤트의 순서',
	`start_time`	TIME	NOT NULL	COMMENT '이동 시작 시각',
	`duration_time`	TIME	NOT NULL	COMMENT '이동 소요 시간'
);

CREATE TABLE `posted_Images` (
	`id`	BIGINT	NOT NULL	COMMENT '게시물-이미지 관계를 식별하는 기본 키',
	`post_id`	BIGINT	NULL	COMMENT '이미지를 사용하는 게시물의 ID',
	`image_id`	BIGINT	NOT NULL	COMMENT '연결된 이미지 ID',
	`is_main_image`	BOOLEAN	NULL	COMMENT '게시물의 대표 이미지 첨부',
	`image_order`	INT	NULL	COMMENT '게시물 내 이미지 노출 순서'
);

CREATE TABLE `post_ranking_snapshots` (
	`snapshot_id`	BIGINT	NOT NULL AUTO_INCREMENT COMMENT '랭킹 스냅샷 식별자',
	`post_id`	BIGINT	NOT NULL COMMENT '랭킹 대상 게시물 ID',
	`snapshot_date`	VARCHAR(30)	NOT NULL COMMENT '랭킹 기준 날짜 또는 범위 식별값 (예 : 2026-01-08, 2026-W40)',
	`score`	INT	NOT NULL COMMENT '랭킹 계산 결과 점수',
	`likes`	INT	NOT NULL COMMENT '스냅샷 생성 시점의 좋아요 수',
	`comments`	INT	NOT NULL COMMENT '스냅샷 생성 시점의 댓글 수',
	`views`	INT	NULL COMMENT '스냅샷 생성 시점의 조회 수',
	`created_at`	DATETIME	NULL COMMENT '랭킹 스냅샷 생성 시각',
	PRIMARY KEY (`snapshot_id`)
);

CREATE TABLE `posts` (
	`post_id`	BIGINT	NOT NULL	COMMENT '게시물을 식별하는 기본 키',
	`user_id`	INT	NOT NULL	COMMENT '게시글 작성자',
	`board_type`	ENUM('FREE', 'PLAN_SHARE', 'PLACE_RECOMMEND')	NOT NULL	DEFAULT FREE	COMMENT '게시판 유형 구분',
	`title`	VARCHAR(100)	NOT NULL	COMMENT '게시물 제목',
	`content`	TEXT	NOT NULL	COMMENT '게시물 본문 내용',
	`author`	VARCHAR(50)	NOT NULL	COMMENT '작성자 표시용 이름 (닉네임 스냅샷)',
	`created_at`	DATETIME	NOT NULL	COMMENT '게시물 생성 시각',
	`updated_at`	DATETIME	NOT NULL	COMMENT '게시물 마지막 수정 시각',
	`deleted_at`	DATETIME	NULL	COMMENT '게시물 소프트 삭제 시각',
	`likes`	BIGINT	NULL	COMMENT '좋아요 수',
	`comments`	BIGINT	NULL	COMMENT '댓글 수',
	`views`	BIGINT	NULL	COMMENT '조회 수'
);

CREATE TABLE `comments` (
	`comment_id`	BIGINT	NOT NULL	COMMENT '댓글을 식별하는 기본 키',
	`post_id`	BIGINT	NOT NULL	COMMENT '댓글이 속한 게시물 ID',
	`author_id`	INT	NOT NULL	COMMENT '댓글 작성자 ID',
	`content`	TEXT	NOT NULL	COMMENT '댓글 본문 내용',
	`created_at`	DATETIME	NOT NULL	COMMENT '댓글 생성 시각',
	`deleted_at`	DATETIME	NULL	COMMENT '댓글 소프트 삭제 시각'
);

CREATE TABLE `groups` (
	`group_id`	BIGINT	NOT NULL	COMMENT '그룹을 식별하는 기본 키',
	`user_id`	INT	NOT NULL	COMMENT '그룹장 사용자 ID',
	`invite_code`	VARCHAR(20)	NOT NULL	COMMENT '그룹 초대 코드'
);

CREATE TABLE `Images` (
	`image_id`	BIGINT	NOT NULL	COMMENT '이미지를 식별하기 위한 기본 키',
	`image_url`	VARCHAR	NULL	COMMENT '실제 이미지가 저장된 경로(URL)',
	`created_at`	DATETIME	NULL	COMMENT '이미지 업로드 시각',
	`status`	VARCHAR(20)	NOT NULL	COMMENT 'ACTIVE / DELETED',
	`deleted_at`	DATETIME	NULL	COMMENT '삭제 시각',
	`s3_key`	VARCHAR(255)	NOT NULL	COMMENT 'S3 삭제용 key (배치처리)'
);

CREATE TABLE `chat_messages` (
	`message_id`	BIGINT	NOT NULL	COMMENT '채팅 메시지를 식별하기 위한 기본 키',
	`room_id`	BIGINT	NOT NULL	COMMENT '메시지가 속한 채팅방 ID',
	`participants_id`	BIGINT	NOT NULL	COMMENT '메시지를 보낸 채팅방 참여자 ID',
	`content`	TEXT	NOT NULL	COMMENT '채팅 메시지 본문',
	`created_at`	DATETIME	NOT NULL	COMMENT '메시지가 전송된 시각',
	`has_image`	TINYINT(1)	NOT NULL	COMMENT '이미지 포함 여부 (0: 없음, 1: 있음)'
);

CREATE TABLE `chat_images` (
	`id`	BIGINT	NOT NULL	COMMENT '채팅 이미지 식별을 위한 기본 키',
	`image_id`	BIGINT	NOT NULL	COMMENT '이미지 테이블(images)의 이미지 ID',
	`message_id`	BIGINT	NOT NULL	COMMENT '채팅 메시지 ID'
);

CREATE TABLE `itinerary_item_places` (
	`id`	BIGINT	NOT NULL	COMMENT '일정용 장소 고유 식별자',
	`itinerary_item_id`	BIGINT	NOT NULL	COMMENT '해당 장소가 속한 일정 항목 ID',
	`place_id`	BIGINT	NOT NULL	COMMENT '실제 장소 (관광지, 식당 등) ID',
	`event_order`	INT	NOT NULL	COMMENT '일정 내 방문 순서',
	`start_time`	TIME	NOT NULL	COMMENT '해당 장소 방문 시작 시각',
	`duration_time`	TIME	NOT NULL	COMMENT '해당 장소 체류 시간',
	`cost`	DECIMAL(10,2)	NOT NULL	COMMENT '해당 장소와 관련된 비용'
);

CREATE TABLE `keyword_subscriptions` (
	`subscription_id`	BIGINT	NOT NULL	COMMENT '키워드 알림 구독을 식별하기 위한 기본 키',
	`user_id`	BIGINT	NOT NULL	COMMENT '사용자를 식별하는 기본 키',
	`keyword`	VARCHAR(30)	NOT NULL	COMMENT '알림을 받을 키워드 문자열',
	`created_at`	DATETIME	NOT NULL	COMMENT '키워드 알림 등록 시각'
);

CREATE TABLE `posted_places` (
	`posted_place_id`	BIGINT	NOT NULL	COMMENT '게시물용 장소 식별자',
	`post_id`	BIGINT	NOT NULL	COMMENT '게시물 ID',
	`place_id`	BIGINT	NOT NULL	COMMENT '장소 ID',
	`rating`	INT	NOT NULL	COMMENT '게시물 기준 장소 별점 (예 : 4)'
);

CREATE TABLE `notifications` (
	`notification_id`	BIGINT	NOT NULL	COMMENT '알림을 식별하는 기본 키',
	`user_id`	BIGINT	NOT NULL	COMMENT '알림을 받는 사용자 ID',
	`post_id`	BIGINT	NOT NULL	COMMENT '알림과 연관된 게시물 ID',
	`type`	ENUM('COMMENT', 'LIKE', 'KEYWORD')	NOT NULL	COMMENT '알림 발생 유형',
	`actor_name`	VARCHAR(50)	NULL	COMMENT '알림을 발생시킨 사용자 이름',
	`preview_text`	VARCHAR(255)	NOT NULL	COMMENT '게시글 제목 또는 댓글 미리보기 텍스트',
	`is_read`	BOOLEAN	NOT NULL	DEFAULT false	COMMENT '알림 읽음 여부',
	`created_at`	DATETIME	NOT NULL	COMMENT '알림 생성 시각'
);

CREATE TABLE `user_image` (
	`id`	BIGINT	NOT NULL	COMMENT '유저 이미지 관계를 식별하는 기본 키',
	`user_id`	BIGINT	NOT NULL	COMMENT '이미지를 소유한 사용자 ID',
	`image_id`	BIGINT	NOT NULL	COMMENT '사용자와 연결된 이미지 ID'
);

CREATE TABLE `places` (
	`place_id`	BIGINT	NOT NULL	COMMENT '장소 식별자',
	`name`	VARCHAR(255)	NOT NULL	COMMENT '장소 이름',
	`address`	VARCHAR(255)	NULL	COMMENT '장소 주소',
	`city`	VARCHAR(100)	NULL	COMMENT '장소가 속한 도시',
	`country`	VARCHAR(255)	NULL,
	`google_map_url`	VARCHAR(500)	NULL	COMMENT '구글 지도 URL'
);

CREATE TABLE `chat_participants` (
	`participants_id`	BIGINT	NOT NULL	COMMENT '채팅방 참여자 레코드를 식별하기 위한 기본 키',
	`room_id`	BIGINT	NOT NULL	COMMENT '참여 중인 채팅방 ID',
	`id`	BIGINT	NOT NULL	COMMENT '그룹원 식별자',
	`last_read_message_id`	BIGINT	NULL
);

CREATE TABLE `posted_plans` (
	`posted_plan_id`	BIGINT	NOT NULL	COMMENT '게시물용 일정 매핑 식별자',
	`post_id`	BIGINT	NOT NULL	COMMENT '게시물 ID',
	`trip_id`	BIGINT	NOT NULL	COMMENT '여행 ID'
);

CREATE TABLE `likes` (
	`like_id`	BIGINT	NOT NULL	COMMENT '좋아요 레코드를 식별하는 기본 키',
	`user_id`	BIGINT	NOT NULL	COMMENT '좋아요를 누른 사용자 ID',
	`post_id`	BIGINT	NOT NULL	COMMENT '좋아요 대상 게시물 ID',
	`created_at`	DATETIME	NOT NULL	COMMENT '좋아요가 생성된 시각'
);

CREATE TABLE `users` (
	`user_id`	BIGINT	NOT NULL	COMMENT '사용자를 식별하는 기본 키',
	`login_id`	VARCHAR(50)	NOT NULL	COMMENT '로그인에 사용되는 사용자 아이디',
	`password`	VARCHAR(255)	NOT NULL	COMMENT '암호화된 사용자 비밀번호',
	`nickname`	VARCHAR(20)	NOT NULL	COMMENT '서비스 내에 노출되는 닉네임',
	`is_deleted`	TINYINT(1)	NOT NULL	DEFAULT 0	COMMENT '탈퇴 여부 (Soft Delete 용도)',
	`preferences`	JSON	NULL	COMMENT '사용자 개인 설정 (추천, 필터 등)',
	`created_at`	DATETIME	NOT NULL	COMMENT '사용자 계정 생성 시각',
	`updated_at`	DATETIME	NOT NULL	COMMENT '사용자 정보 마지막 수정 시각'
);

ALTER TABLE `ai_chat_messages` ADD CONSTRAINT `PK_AI_CHAT_MESSAGES` PRIMARY KEY (
	`id`
);

ALTER TABLE `itinerary_items` ADD CONSTRAINT `PK_ITINERARY_ITEMS` PRIMARY KEY (
	`id`
);

ALTER TABLE `group_members` ADD CONSTRAINT `PK_GROUP_MEMBERS` PRIMARY KEY (
	`id`
);

ALTER TABLE `trips` ADD CONSTRAINT `PK_TRIPS` PRIMARY KEY (
	`id`
);

ALTER TABLE `chat_rooms` ADD CONSTRAINT `PK_CHAT_ROOMS` PRIMARY KEY (
	`room_id`
);

ALTER TABLE `itinerary_item_transports` ADD CONSTRAINT `PK_ITINERARY_ITEM_TRANSPORTS` PRIMARY KEY (
	`transport_id`
);

ALTER TABLE `posted_Images` ADD CONSTRAINT `PK_POSTED_IMAGES` PRIMARY KEY (
	`id`
);

ALTER TABLE `post_ranking_snapshots` ADD CONSTRAINT `PK_POST_RANKING_SNAPSHOTS` PRIMARY KEY (
	`snapshot_id`
);

ALTER TABLE `posts` ADD CONSTRAINT `PK_POSTS` PRIMARY KEY (
	`post_id`
);

ALTER TABLE `comments` ADD CONSTRAINT `PK_COMMENTS` PRIMARY KEY (
	`comment_id`
);

ALTER TABLE `groups` ADD CONSTRAINT `PK_GROUPS` PRIMARY KEY (
	`group_id`
);

ALTER TABLE `Images` ADD CONSTRAINT `PK_IMAGES` PRIMARY KEY (
	`image_id`
);

ALTER TABLE `chat_messages` ADD CONSTRAINT `PK_CHAT_MESSAGES` PRIMARY KEY (
	`message_id`
);

ALTER TABLE `chat_images` ADD CONSTRAINT `PK_CHAT_IMAGES` PRIMARY KEY (
	`id`
);

ALTER TABLE `itinerary_item_places` ADD CONSTRAINT `PK_ITINERARY_ITEM_PLACES` PRIMARY KEY (
	`id`
);

ALTER TABLE `keyword_subscriptions` ADD CONSTRAINT `PK_KEYWORD_SUBSCRIPTIONS` PRIMARY KEY (
	`subscription_id`
);

ALTER TABLE `posted_places` ADD CONSTRAINT `PK_POSTED_PLACES` PRIMARY KEY (
	`posted_place_id`
);

ALTER TABLE `notifications` ADD CONSTRAINT `PK_NOTIFICATIONS` PRIMARY KEY (
	`notification_id`
);

ALTER TABLE `user_image` ADD CONSTRAINT `PK_USER_IMAGE` PRIMARY KEY (
	`id`
);

ALTER TABLE `places` ADD CONSTRAINT `PK_PLACES` PRIMARY KEY (
	`place_id`
);

ALTER TABLE `chat_participants` ADD CONSTRAINT `PK_CHAT_PARTICIPANTS` PRIMARY KEY (
	`participants_id`
);

ALTER TABLE `posted_plans` ADD CONSTRAINT `PK_POSTED_PLANS` PRIMARY KEY (
	`posted_plan_id`
);

ALTER TABLE `likes` ADD CONSTRAINT `PK_LIKES` PRIMARY KEY (
	`like_id`
);

ALTER TABLE `users` ADD CONSTRAINT `PK_USERS` PRIMARY KEY (
	`user_id`
);

