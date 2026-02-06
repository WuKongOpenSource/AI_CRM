package com.kakarote.ai_crm.common.exception;

import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.common.result.ResultCode;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import io.jsonwebtoken.MalformedJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;


/**
 * @author zhangzhiwei
 * 全局异常处理类
 */
@RestControllerAdvice
@Slf4j
public class ExceptionHandlerAdvice {


    @ExceptionHandler(value = Exception.class)
    public Result<String> defaultException(Exception ex) {
        //TODO 默认异常需要处理
        log.error("默认异常需要处理", ex);
        if (ex instanceof ResultCode) {
            return Result.error(((ResultCode) ex).getCode(), ((ResultCode) ex).getMsg());
        }
        return Result.error(SystemCodeEnum.SYSTEM_ERROR);
    }

    @ExceptionHandler(value = BusinessException.class)
    public Result<String> businessException(BusinessException ex) {
        log.error("业务异常需要处理", ex);
        return Result.error(((ResultCode) ex).getCode(), ((ResultCode) ex).getMsg());
    }

    @ExceptionHandler(value = NoResourceFoundException.class)
    public Result<String> noResourceFoundException(NoResourceFoundException ex) {
        log.error("请求url未找到:{}", ex.getResourcePath());
        return Result.error(SystemCodeEnum.SYSTEM_NO_FOUND);
    }

    @ExceptionHandler(value = MalformedJwtException.class)
    public Result<String> jwtException() {
        return Result.error(SystemCodeEnum.TOKEN_VERIFICATION_EXCEPTION);
    }

    @ExceptionHandler(value = HttpRequestMethodNotSupportedException.class)
    public Result<String> methodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.error("方法请求错误", e);
        return Result.error(SystemCodeEnum.SYSTEM_NO_VALID);
    }

    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public Result<String> messageNotReadableException(HttpMessageNotReadableException e) {
        log.error("请求参数解析错误", e);
        return Result.error(SystemCodeEnum.SYSTEM_NO_VALID, "请求参数格式错误");
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public Result<String> argumentNotValidException(MethodArgumentNotValidException e) {
        log.error("MethodArgumentNotValidException:", e);
        BindingResult bindingResult = e.getBindingResult();
        if (bindingResult.getGlobalError() != null) {
            return Result.error(SystemCodeEnum.SYSTEM_NO_VALID, bindingResult.getGlobalError().getDefaultMessage());
        } else if (bindingResult.getFieldError() != null) {
            return Result.error(SystemCodeEnum.SYSTEM_NO_VALID, bindingResult.getFieldError().getDefaultMessage());
        } else {
            return Result.error(SystemCodeEnum.SYSTEM_NO_VALID);
        }
    }


}
