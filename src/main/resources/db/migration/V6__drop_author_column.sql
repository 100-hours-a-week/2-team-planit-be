SET @sql = (
  SELECT IF(
    COUNT(*) > 0,
    'ALTER TABLE posts DROP COLUMN author',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'posts'
    AND column_name = 'author'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
