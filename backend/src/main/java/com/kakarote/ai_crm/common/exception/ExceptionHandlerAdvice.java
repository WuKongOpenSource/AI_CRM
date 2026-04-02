package com.kakarote.ai_crm.common.exception;

import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.common.result.ResultCode;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import io.jsonwebtoken.MalformedJwtException;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global exception handler.
 */
@RestControllerAdvice
@Slf4j
public class ExceptionHandlerAdvice {

    @ExceptionHandler(value = {AsyncRequestNotUsableException.class, ClientAbortException.class})
    public void clientDisconnectException(Exception ex) {
        log.warn("Client stream disconnected: {}", ex.getMessage());
    }

    @ExceptionHandler(value = Exception.class)
    public Result<String> defaultException(Exception ex) {
        log.error("Unhandled exception", ex);
        if (ex instanceof ResultCode) {
            return Result.error(((ResultCode) ex).getCode(), ((ResultCode) ex).getMsg());
        }
        return Result.error(SystemCodeEnum.SYSTEM_ERROR);
    }

    @ExceptionHandler(value = BusinessException.class)
    public Result<String> businessException(BusinessException ex) {
        log.error("Business exception", ex);
        return Result.error(((ResultCode) ex).getCode(), ((ResultCode) ex).getMsg());
    }

    @ExceptionHandler(value = NoResourceFoundException.class)
    public Result<String> noResourceFoundException(NoResourceFoundException ex) {
        log.error("Request url not found: {}", ex.getResourcePath());
        return Result.error(SystemCodeEnum.SYSTEM_NO_FOUND);
    }

    @ExceptionHandler(value = MalformedJwtException.class)
    public Result<String> jwtException() {
        return Result.error(SystemCodeEnum.TOKEN_VERIFICATION_EXCEPTION);
    }

    @ExceptionHandler(value = HttpRequestMethodNotSupportedException.class)
    public Result<String> methodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.error("Unsupported request method", e);
        return Result.error(SystemCodeEnum.SYSTEM_NO_VALID);
    }

    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public Result<String> messageNotReadableException(HttpMessageNotReadableException e) {
        log.error("Request body parse error", e);
        return Result.error(SystemCodeEnum.SYSTEM_NO_VALID, "请求参数格式错误");
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public Result<String> argumentNotValidException(MethodArgumentNotValidException e) {
        log.error("MethodArgumentNotValidException", e);
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
