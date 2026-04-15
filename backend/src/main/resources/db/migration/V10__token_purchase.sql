ALTER TABLE crm_tenant
    ADD COLUMN IF NOT EXISTS purchased_token_total BIGINT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS purchased_token_used BIGINT DEFAULT 0;

COMMENT ON COLUMN crm_tenant.purchased_token_total IS '已购买 token 总量';
COMMENT ON COLUMN crm_tenant.purchased_token_used IS '已购买 token 已使用量';

CREATE TABLE IF NOT EXISTS crm_token_purchase_order (
    order_id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    plan_id VARCHAR(64) NOT NULL,
    plan_name VARCHAR(100) NOT NULL,
    token_amount BIGINT NOT NULL,
    amount_fen INTEGER NOT NULL,
    payment_channel VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_provider_order_no VARCHAR(100),
    payment_qr_code TEXT,
    expire_time TIMESTAMP,
    paid_time TIMESTAMP,
    notify_payload TEXT,
    remark VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE crm_token_purchase_order IS 'Token 购买订单';
COMMENT ON COLUMN crm_token_purchase_order.order_no IS '业务订单号';
COMMENT ON COLUMN crm_token_purchase_order.plan_id IS '套餐ID';
COMMENT ON COLUMN crm_token_purchase_order.plan_name IS '套餐名称';
COMMENT ON COLUMN crm_token_purchase_order.token_amount IS '购买 token 数量';
COMMENT ON COLUMN crm_token_purchase_order.amount_fen IS '订单金额，单位分';
COMMENT ON COLUMN crm_token_purchase_order.payment_channel IS '支付渠道：wechat/alipay';
COMMENT ON COLUMN crm_token_purchase_order.status IS '订单状态：PENDING/PAID/FAILED/CLOSED/EXPIRED';
COMMENT ON COLUMN crm_token_purchase_order.payment_provider_order_no IS '支付平台交易号';
COMMENT ON COLUMN crm_token_purchase_order.payment_qr_code IS '二维码原始内容';
COMMENT ON COLUMN crm_token_purchase_order.notify_payload IS '支付回调原始报文';

CREATE UNIQUE INDEX IF NOT EXISTS uk_token_purchase_order_no
    ON crm_token_purchase_order (order_no);

CREATE INDEX IF NOT EXISTS idx_token_purchase_order_tenant_time
    ON crm_token_purchase_order (tenant_id, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_token_purchase_order_tenant_status
    ON crm_token_purchase_order (tenant_id, status);
