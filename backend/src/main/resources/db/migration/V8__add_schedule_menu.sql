-- 补充日程安排模块的菜单数据（V6 创建了 crm_schedule 表但遗漏了菜单项）
-- 使用 2200 段，避免与现有菜单 ID 冲突

-- ========== 日程安排 (2200) ==========
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (2200, 0, 'schedule', '日程安排', 3);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (2201, 2200, 'schedule:create', '新建', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (2202, 2200, 'schedule:view', '查看', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (2203, 2200, 'schedule:edit', '编辑', 5);
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type) VALUES (2204, 2200, 'schedule:delete', '删除', 5);
