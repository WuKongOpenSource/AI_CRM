-- ============================================
-- V41: Fix IM permission menu type.
-- V40 created the `im` permission with type=3 (菜单), but @RequirePermission
-- checks function-permission rows (type=5, 功能), like user:create / addressBook:list.
-- Correct the type so the `im` permission is actually recognized.
-- ============================================

UPDATE manager_menu SET type = 5 WHERE menu_id = 9001 AND realm = 'im';
