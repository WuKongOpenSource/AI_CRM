package com.kakarote.ai_crm.service;

import com.kakarote.ai_crm.common.enums.LoginTypeEnum;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.VO.LoginResponseVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthSessionService {

    LoginResponseVO createLoginResponse(ManagerUser user,
                                        LoginTypeEnum loginType,
                                        HttpServletRequest request,
                                        HttpServletResponse response);
}
