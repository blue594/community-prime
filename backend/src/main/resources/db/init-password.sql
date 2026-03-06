-- ========================================================
-- 初始化用户密码
-- 将已有用户的密码设置为 123456（MD5加密）
-- ========================================================

-- 为没有密码的用户设置默认密码 123456
UPDATE tb_user 
SET password = 'e10adc3949ba59abbe56e057f20f883e'  -- MD5('123456')
WHERE password IS NULL OR password = '';

-- 验证更新结果
-- SELECT phone, password FROM tb_user;
