package com.kakarote.ai_crm.config.security.filter;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import com.kakarote.ai_crm.common.log.AccessLogAttributes;
import com.kakarote.ai_crm.common.log.AccessLogPayloadSanitizer;
import com.kakarote.ai_crm.common.log.AccessLogSnapshot;
import com.kakarote.ai_crm.config.tenant.TenantContextHolder;
import com.kakarote.ai_crm.entity.BO.LoginUser;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.service.AccessLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class AccessLogFilter extends OncePerRequestFilter {

    private static final int STACK_TRACE_LIMIT_CHARS = 64 * 1024;
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Autowired
    private AccessLogService accessLogService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || isStaticOrDocumentationPath(uri);
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startNanos = System.nanoTime();
        String traceId = resolveTraceId(request);
        response.setHeader(TRACE_ID_HEADER, traceId);
        MDC.put("traceId", traceId);

        ContentCachingRequestWrapper requestWrapper = shouldCacheRequestBody(request)
                ? new ContentCachingRequestWrapper(request, AccessLogPayloadSanitizer.REQUEST_BODY_LIMIT_BYTES)
                : null;
        ContentCachingResponseWrapper responseWrapper = shouldCacheResponseBody(request)
                ? new ContentCachingResponseWrapper(response)
                : null;
        HttpServletRequest requestToUse = requestWrapper != null ? requestWrapper : request;
        HttpServletResponse responseToUse = responseWrapper != null ? responseWrapper : response;

        Throwable thrown = null;
        try {
            filterChain.doFilter(requestToUse, responseToUse);
        } catch (Exception exception) {
            thrown = exception;
            throw exception;
        } finally {
            try {
                AccessLogSnapshot snapshot = buildSnapshot(
                        requestToUse, responseToUse, requestWrapper, responseWrapper,
                        traceId, startNanos, thrown);
                accessLogService.recordAsync(snapshot);
            } catch (Exception e) {
                log.warn("Build access log failed: traceId={}, uri={}, error={}",
                        traceId, request.getRequestURI(), e.getMessage(), e);
            } finally {
                if (responseWrapper != null) {
                    responseWrapper.copyBodyToResponse();
                }
                MDC.remove("traceId");
            }
        }
    }

    private AccessLogSnapshot buildSnapshot(HttpServletRequest request,
                                            HttpServletResponse response,
                                            ContentCachingRequestWrapper requestWrapper,
                                            ContentCachingResponseWrapper responseWrapper,
                                            String traceId,
                                            long startNanos,
                                            Throwable thrown) {
        CurrentUser currentUser = resolveCurrentUser();
        AccessLogPayloadSanitizer.SanitizedText queryString =
                AccessLogPayloadSanitizer.sanitizeQueryString(request.getQueryString());
        AccessLogPayloadSanitizer.SanitizedText requestBody =
                resolveRequestBody(request, requestWrapper);
        AccessLogPayloadSanitizer.ResultResponseSummary resultSummary =
                resolveResponseSummary(responseWrapper);
        Throwable systemError = resolveSystemError(request, thrown);
        int statusCode = response.getStatus();
        boolean success = systemError == null
                && statusCode < 400
                && (resultSummary.businessCode() == null || resultSummary.businessCode() == 0);

        return new AccessLogSnapshot(
                currentUser.tenantId(),
                currentUser.userId(),
                currentUser.username(),
                request.getMethod(),
                request.getRequestURI(),
                queryString.value(),
                AccessLogPayloadSanitizer.sanitizeHeaders(request),
                requestBody.value(),
                resultSummary.responseBody(),
                statusCode,
                resultSummary.businessCode(),
                success,
                JakartaServletUtil.getClientIP(request),
                StrUtil.maxLength(request.getHeader(HttpHeaders.USER_AGENT), 500),
                traceId,
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos),
                requestBody.truncated(),
                resultSummary.truncated(),
                resultSummary.resultResponse(),
                toErrorSnapshot(systemError)
        );
    }

    private AccessLogPayloadSanitizer.SanitizedText resolveRequestBody(HttpServletRequest request,
                                                                       ContentCachingRequestWrapper wrapper) {
        if (wrapper == null) {
            String omitted = AccessLogPayloadSanitizer.buildOmittedBodyMessage(
                    request.getContentType(), request.getContentLengthLong());
            return new AccessLogPayloadSanitizer.SanitizedText(omitted, false);
        }
        byte[] content = wrapper.getContentAsByteArray();
        if (content.length == 0) {
            return new AccessLogPayloadSanitizer.SanitizedText(null, false);
        }
        String body = AccessLogPayloadSanitizer.decodeBody(content, request.getCharacterEncoding());
        AccessLogPayloadSanitizer.SanitizedText sanitized =
                AccessLogPayloadSanitizer.sanitizeRequestBody(body, request.getContentType());
        boolean truncatedByCache = content.length >= AccessLogPayloadSanitizer.REQUEST_BODY_LIMIT_BYTES
                && request.getContentLengthLong() > AccessLogPayloadSanitizer.REQUEST_BODY_LIMIT_BYTES;
        return new AccessLogPayloadSanitizer.SanitizedText(
                sanitized.value(),
                sanitized.truncated() || truncatedByCache
        );
    }

    private AccessLogPayloadSanitizer.ResultResponseSummary resolveResponseSummary(
            ContentCachingResponseWrapper responseWrapper) {
        if (responseWrapper == null) {
            return new AccessLogPayloadSanitizer.ResultResponseSummary(null, null, false, false);
        }
        byte[] body = responseWrapper.getContentAsByteArray();
        if (body.length == 0) {
            return new AccessLogPayloadSanitizer.ResultResponseSummary(null, null, false, false);
        }
        String responseBody = AccessLogPayloadSanitizer.decodeBody(body, responseWrapper.getCharacterEncoding());
        return AccessLogPayloadSanitizer.summarizeResultResponse(responseBody);
    }

    private Throwable resolveSystemError(HttpServletRequest request, Throwable thrown) {
        if (thrown != null) {
            return thrown;
        }
        Object attribute = request.getAttribute(AccessLogAttributes.SYSTEM_EXCEPTION);
        return attribute instanceof Throwable throwable ? throwable : null;
    }

    private AccessLogSnapshot.ErrorSnapshot toErrorSnapshot(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        AccessLogPayloadSanitizer.SanitizedText stackTrace =
                AccessLogPayloadSanitizer.truncateToChars(writer.toString(), STACK_TRACE_LIMIT_CHARS);
        return new AccessLogSnapshot.ErrorSnapshot(
                throwable.getClass().getName(),
                StrUtil.maxLength(throwable.getMessage(), 4000),
                stackTrace.value()
        );
    }

    private CurrentUser resolveCurrentUser() {
        Long tenantId = TenantContextHolder.getTenantId();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            ManagerUser user = loginUser.getUser();
            if (user != null) {
                return new CurrentUser(
                        tenantId != null ? tenantId : user.getTenantId(),
                        user.getUserId(),
                        user.getUsername()
                );
            }
        }
        return new CurrentUser(tenantId, null, null);
    }

    private String resolveTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (StrUtil.isNotBlank(traceId)) {
            return StrUtil.maxLength(traceId, 64);
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    private boolean shouldCacheRequestBody(HttpServletRequest request) {
        long contentLength = request.getContentLengthLong();
        if (contentLength == 0) {
            return false;
        }
        return isTextualContentType(request.getContentType());
    }

    private boolean shouldCacheResponseBody(HttpServletRequest request) {
        return !isLikelyStreamingOrBinaryRequest(request);
    }

    private boolean isTextualContentType(String contentType) {
        if (StrUtil.isBlank(contentType)) {
            return true;
        }
        String normalized = contentType.toLowerCase(Locale.ROOT);
        return normalized.contains("json")
                || normalized.contains("xml")
                || normalized.startsWith(MediaType.TEXT_PLAIN_VALUE)
                || normalized.startsWith(MediaType.TEXT_HTML_VALUE)
                || normalized.startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
    }

    private boolean isLikelyStreamingOrBinaryRequest(HttpServletRequest request) {
        String uri = request.getRequestURI().toLowerCase(Locale.ROOT);
        String accept = StrUtil.nullToEmpty(request.getHeader(HttpHeaders.ACCEPT)).toLowerCase(Locale.ROOT);
        return accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE)
                || uri.contains("/stream")
                || uri.contains("/download")
                || uri.contains("/preview-range")
                || uri.contains("/import/template")
                || uri.contains("/export");
    }

    private boolean isStaticOrDocumentationPath(String uri) {
        if (StrUtil.isBlank(uri)) {
            return false;
        }
        String path = uri.toLowerCase(Locale.ROOT);
        return "/".equals(path)
                || "/index".equals(path)
                || "/index.html".equals(path)
                || "/favicon.ico".equals(path)
                || "/doc.html".equals(path)
                || path.startsWith("/assets/")
                || path.startsWith("/static/")
                || path.startsWith("/webjars/")
                || path.startsWith("/swagger-resources/")
                || path.startsWith("/v3/api-docs/")
                || path.endsWith(".js")
                || path.endsWith(".css")
                || path.endsWith(".map")
                || path.endsWith(".png")
                || path.endsWith(".jpg")
                || path.endsWith(".jpeg")
                || path.endsWith(".gif")
                || path.endsWith(".svg")
                || path.endsWith(".ico")
                || path.endsWith(".woff")
                || path.endsWith(".woff2");
    }

    private record CurrentUser(Long tenantId, Long userId, String username) {
    }
}
