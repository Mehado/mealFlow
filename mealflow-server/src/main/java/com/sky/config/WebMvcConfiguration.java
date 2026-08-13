package com.sky.config;

import com.sky.interceptor.JwtTokenAdminInterceptor;
import com.sky.interceptor.JwtTokenUserInterceptor;
import com.sky.interceptor.TraceIdInterceptor;
import com.sky.json.JacksonObjectMapper;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class WebMvcConfiguration implements WebMvcConfigurer {


    private final JwtTokenAdminInterceptor jwtTokenAdminInterceptor;
    private final JwtTokenUserInterceptor jwtTokenUserInterceptor;
    private final TraceIdInterceptor traceIdInterceptor;

/**
 * 注册自定义拦截器方法
 * 该方法用于配置系统中需要使用的拦截器，并设置它们的拦截路径和排除路径
 * @param registry 拦截器注册器，用于注册和管理拦截器
 */
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");
    // 注册管理员JWT令牌拦截器
        registry.addInterceptor(jwtTokenAdminInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/employee/login");

    // 注册用户JWT令牌拦截器
        registry.addInterceptor(jwtTokenUserInterceptor)
                .addPathPatterns("/user/**")
                .excludePathPatterns("/user/user/login")
                .excludePathPatterns("/user/shop/status");
        // 注册全链路日志拦截器
        registry.addInterceptor(traceIdInterceptor).addPathPatterns("/**");
    }

/**
 * 添加资源处理器，用于处理静态资源请求
 * @param registry 资源处理器注册表，用于注册静态资源处理器
 */
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // 注册资源处理器，处理 "/doc.html" 路径的请求
    // 将请求映射到 classpath:/META-INF/resources/ 目录下的资源
        registry.addResourceHandler("/doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
    }

/**
 * 配置JsonMapper Bean
 * 用于将对象转换为JSON字符串或从JSON字符串解析为对象
 *
 * @return 返回一个配置好的JacksonObjectMapper实例
 *         JacksonObjectMapper是JsonMapper的实现类，提供了更灵活的JSON处理功能
 */
    @Bean
    public JsonMapper jsonMapper() {
        return new JacksonObjectMapper();
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("苍穹外卖项目接口文档")
                        .version("2.0")
                        .description("苍穹外卖项目接口文档")
                        .contact(new Contact()
                                .name("开发团队")
                                .email("dev@sky.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")))
                .addSecurityItem(new SecurityRequirement().addList("JWT"))
                .components(new Components()
                        .addSecuritySchemes("JWT", new SecurityScheme()
                                .name("JWT")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT 认证令牌，登录后获取")));
    }
}




