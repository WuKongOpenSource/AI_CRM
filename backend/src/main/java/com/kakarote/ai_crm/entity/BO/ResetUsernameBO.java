package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Reset username request")
@Data
public class ResetUsernameBO {

    @Schema(description = "User ID")
    private Long userId;

    @Schema(description = "New username")
    private String username;

    @Schema(description = "Current login password, required when changing your own username")
    private String currentPassword;
}
