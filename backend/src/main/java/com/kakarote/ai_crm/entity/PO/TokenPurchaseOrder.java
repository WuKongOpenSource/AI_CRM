package com.kakarote.ai_crm.entity.PO;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("crm_token_purchase_order")
public class TokenPurchaseOrder implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "order_id", type = IdType.ASSIGN_ID)
    private Long orderId;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    private Long userId;

    private String orderNo;

    private String planId;

    private String planName;

    private Long tokenAmount;

    private Integer amountFen;

    private String paymentChannel;

    private String status;

    private String paymentProviderOrderNo;

    private String paymentQrCode;

    private Date expireTime;

    private Date paidTime;

    private String notifyPayload;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
