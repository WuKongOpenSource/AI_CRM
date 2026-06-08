ALTER TABLE manager_user
    ADD COLUMN IF NOT EXISTS ui_preferences TEXT;

COMMENT ON COLUMN manager_user.ui_preferences IS '用户级 UI 偏好(JSON)';
