-- 会话存档专用「真实 corpId」（明文）。
-- 第三方授权回写的 corp_id 在企业微信 ID 加密升级后可能是服务商级密文 open_corpid，
-- 而会话存档 WeWorkFinanceSDK 的 Init(corpid, secret) 需要企业真实明文 corpid
-- （企业管理端「我的企业-企业信息」，与会话存档后台一致）。
-- 留空则回退使用 corp_id。
ALTER TABLE crm_wecom_corp_config
    ADD COLUMN IF NOT EXISTS archive_corp_id VARCHAR(128);

COMMENT ON COLUMN crm_wecom_corp_config.archive_corp_id IS '会话存档专用真实corpid(明文)，为空则回退 corp_id';
