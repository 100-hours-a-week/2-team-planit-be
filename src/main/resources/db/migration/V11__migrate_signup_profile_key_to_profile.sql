-- 회원가입 시 저장된 프로필 이미지 key를 signup/ -> profile/ 로 통일
UPDATE users
SET profile_image_key = REPLACE(profile_image_key, 'signup/', 'profile/')
WHERE profile_image_key LIKE 'signup/%';
