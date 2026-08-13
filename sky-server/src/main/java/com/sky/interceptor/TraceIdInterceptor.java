package com.sky.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * 全链路日志：请求入口生成 traceId 放入 MDC，日志自动带上
 * 透传：上游传了 X-Trace-Id 就复用，否则生成新的
 */
@Component
@Slf4j
public class TraceIdInterceptor implements HandlerInterceptor {

    public static final String TRACE_ID = "traceId";
    private static final String START_TIME = "startTime";

    @Override
    /**
     * preHandle方法是请求处理前的拦截器方法
     * @param request HttpServletRequest对象，包含请求信息
     * @param response HttpServletResponse对象，用于响应请求
     * @param handler 请求处理的方法处理器
     * @return 返回true表示继续流程，false表示终端流程
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从请求头中获取追踪ID
        String traceId = request.getHeader("X-Trace-Id");
        // 如果追踪ID为空或空白字符串，则生成一个新的追踪ID
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        // 将追踪ID存入MDC（Mapped Diagnostic Context）中，便于日志追踪
        MDC.put(TRACE_ID, traceId);
        // 记录请求开始时间，用于后续计算请求处理耗时
        request.setAttribute(START_TIME, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        long cost = System.currentTimeMillis() - (long) request.getAttribute(START_TIME);
        log.info("接口耗时：uri={}, method={}, cost={}ms, status={}",
                request.getRequestURI(), request.getMethod(), cost, response.getStatus());
        // 请求结束必须清理，否则线程池复用会串 traceId
        MDC.remove(TRACE_ID);
    }
}

