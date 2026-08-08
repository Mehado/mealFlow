package com.sky.interceptor;

import com.sky.constant.JwtClaimsConstant;
import com.sky.context.BaseContext;
import com.sky.properties.JwtProperties;
import com.sky.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * jwt令牌校验的拦截器
 */
@Component
@Slf4j
public class JwtTokenAdminInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 校验jwt
     * 这是一个拦截器，用于验证JWT令牌的有效性

 *
     * @param request  HTTP请求对象，包含请求信息
     * @param response HTTP响应对象，用于返回响应
     * @param handler  拦截到的处理器对象，可能是Controller方法或其他资源
     * @return 返回boolean值，true表示放行，false表示拦截
     * @throws Exception 可能抛出的异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            //当前拦截到的不是动态方法，直接放行
            return true;
        }

        //1、从请求头中获取令牌
        String token = request.getHeader(jwtProperties.getAdminTokenName());

        //2、校验令牌
        try {
            log.info("收到token：{}", token != null ? "有" : "无");
        //解析JWT令牌，获取其中的声明信息
            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
        //从声明中获取员工ID，并设置为当前线程ID
            Long empId = Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());
            log.info("当前员工id：{}", empId);  //记录当前员工ID的日志信息
            BaseContext.setCurrentId(empId);    //将员工ID存入线程上下文

        //从声明中获取角色信息
            Object roleObject = claims.get(JwtClaimsConstant.ROLE);
            String role = roleObject != null ? roleObject.toString() : null;
            BaseContext.setRole(role);          //将角色信息存入线程上下文
            //3、通过，放行
            return true;
        } catch (Exception ex) {
            //4、不通过，响应401状态码
            response.setStatus(401);  //设置HTTP状态码为401未授权
            return false;             //拦截请求
        }
    }

    /**
     * 请求处理完成后清理 ThreadLocal，防止内存泄漏
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        BaseContext.removeCurrentId();
        BaseContext.removeRole();
    }
}
