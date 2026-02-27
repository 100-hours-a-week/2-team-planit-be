CREATE TABLE IF NOT EXISTS chat_room (
    id BIGINT NOT NULL AUTO_INCREMENT,
    trip_id BIGINT NOT NULL,
    total_message_count BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_chat_room_trip (trip_id),
    CONSTRAINT fk_chat_room_trip FOREIGN KEY (trip_id) REFERENCES trips (id)
);

CREATE TABLE IF NOT EXISTS chat_room_participant (
    id BIGINT NOT NULL AUTO_INCREMENT,
    chat_room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    last_read_count BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_chat_room_participant (chat_room_id, user_id),
    KEY idx_chat_room_participant_user (user_id),
    CONSTRAINT fk_chat_room_participant_room FOREIGN KEY (chat_room_id) REFERENCES chat_room (id),
    CONSTRAINT fk_chat_room_participant_user FOREIGN KEY (user_id) REFERENCES users (user_id)
);
