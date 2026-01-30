-- Flyway migration to switch board_type column from MySQL enum to varchar(255)
ALTER TABLE posts
  MODIFY COLUMN board_type VARCHAR(255) NOT NULL;
