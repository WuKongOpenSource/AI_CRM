package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 修改账户状态BO
 */
@Data
@Schema(description = "修改账户状态BO")
public class UpdateAccountStatusBO {

    @Schema(description = "账户信息")
    private List<AccountInfo> accountInfoList;

    @Schema(description = "账户状态：1.启用，0、禁用")
    private Integer accountStatus;

    @Data
    @Schema(description = "账户信息")
    public static class AccountInfo {

        @Schema(description = "账户类型：1、企业，2、个人")
        private Integer type;

        @Schema(description = "关联ID")
        private Long relateId;

        @Schema(description = "企业ID")
        private Long companyId;

        @Schema(description = "用户ID")
        private Long userId;

        @Schema(description = "默认积分")
        private Long defaultToken = 0L;


    }

}
