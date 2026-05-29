package com.kakarote.ai_crm.service;

import com.kakarote.ai_crm.entity.BO.ExternalAuthRegisterBO;
import com.kakarote.ai_crm.entity.BO.ExternalAuthTicketLoginBO;
import com.kakarote.ai_crm.entity.VO.ExternalAuthAuthorizeVO;
import com.kakarote.ai_crm.entity.VO.ExternalAuthBindingVO;
import com.kakarote.ai_crm.entity.VO.ExternalAuthProviderVO;
import com.kakarote.ai_crm.entity.VO.LoginResponseVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface ExternalAuthService {

    List<ExternalAuthProviderVO> listProviders();

    ExternalAuthAuthorizeVO createAuthorizeUrl(String provider, String redirect, HttpServletRequest request);

    ExternalAuthAuthorizeVO createBindAuthorizeUrl(String provider, String redirect, HttpServletRequest request);

    String handleCallback(String provider,
                          String code,
                          String state,
                          String error,
                          HttpServletRequest request);

    LoginResponseVO loginByTicket(ExternalAuthTicketLoginBO loginBO,
                                  HttpServletRequest request,
                                  HttpServletResponse response);

    LoginResponseVO registerByTicket(ExternalAuthRegisterBO registerBO,
                                     HttpServletRequest request,
                                     HttpServletResponse response);

    List<ExternalAuthBindingVO> listBindings();

    void unbind(String provider);
}
