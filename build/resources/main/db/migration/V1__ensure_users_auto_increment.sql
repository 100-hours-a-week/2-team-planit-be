SET @pk_exists := (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND CONSTRAINT_TYPE = 'PRIMARY KEY'
);

SET @drop_pk_sql := IF(@pk_exists > 0, 'ALTER TABLE users DROP PRIMARY KEY', 'SELECT 1');

PREPARE drop_pk_stmt FROM @drop_pk_sql;
EXECUTE drop_pk_stmt;
DEALLOCATE PREPARE drop_pk_stmt;

ALTER TABLE users
    MODIFY COLUMN user_id BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE users
    ADD PRIMARY KEY (user_id);
