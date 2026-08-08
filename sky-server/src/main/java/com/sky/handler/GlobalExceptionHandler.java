package com.sky.handler;

import com.sky.constant.MessageConstant;
import com.sky.exception.BaseException;
import com.sky.result.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler
    public Result exceptionHandler(SQLIntegrityConstraintViolationException ex){
        // 处理唯一键约束冲突
        String message = ex.getMessage();
        if (message != null && message.contains("Duplicate entry")) {
            String[] parts = message.split(" ");
            String key = parts.length > 2 ? parts[2] : "";
            return Result.error(key + "已存在");
        }
        return Result.error("数据约束异常，请检查输入");
    }

    /**
     * 参数校验失败（@RequestBody / 绑定对象）
 * 这是一个异常处理方法，用于处理请求体参数校验失败的情况
 * 当使用@Valid注解进行参数校验时，如果校验失败，会抛出MethodArgumentNotValidException异常
 * 该方法会捕获这个异常并返回一个错误信息给客户端
     */
    @ExceptionHandler  // 标记此方法为异常处理器，可以处理指定类型的异常
    public Result exceptionHandler(MethodArgumentNotValidException ex) {
    // 获取校验失败的字段错误信息，并将第一个错误信息作为返回消息
    // 如果没有错误信息，则使用默认消息"参数校验失败"
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)  // 获取每个字段的错误信息
                .findFirst()  // 获取第一个错误信息
                .orElse("参数校验失败");  // 如果没有错误信息，则使用默认消息
    // 记录错误日志
        log.error("参数校验失败：{}", message);
    // 返回错误结果，包含错误信息
        return Result.error(message);
    }

    /**
     * 参数校验失败（方法参数/查询参数）的异常处理方法
 * 使用@ExceptionHandler注解标注，用于处理ConstraintViolationException类型的异常
 *
 * @param ex ConstraintViolationException异常对象，包含参数校验失败的详细信息
 * @return Result 返回一个包含错误信息的Result对象，用于前端统一处理
     */
    @ExceptionHandler  // 标记此方法为异常处理方法，处理ConstraintViolationException异常
    public Result exceptionHandler(ConstraintViolationException ex) {
    // 通过Stream API从异常对象中提取第一条错误信息
    // 如果没有获取到错误信息，则使用默认提示"参数校验失败"
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)  // 提取每条校验规则的错误信息
                .findFirst()                          // 获取第一条错误信息
                .orElse("参数校验失败");               // 如果没有错误信息，则使用默认提示
    // 记录错误日志，方便后续排查问题
        log.error("参数校验失败：{}", message);
    // 返回错误信息，前端可根据此信息进行相应处理
        return Result.error(message);
    }

    /**
     * 请求体格式错误或为空
 * 该方法是一个异常处理方法，用于处理请求体格式错误或为空的异常情况
 * 使用 @ExceptionHandler 注解标记，用于捕获 HttpMessageNotReadableException 异常
 *
 * @param ex HttpMessageNotReadableException 异常对象，包含请求体解析失败的详细信息
 * @return 返回一个 Result 对象，包含错误信息"请求体格式错误或为空"
     */
    @ExceptionHandler
    public Result exceptionHandler(HttpMessageNotReadableException ex) {
    // 记录错误日志，输出请求体解析失败的详细信息
        log.error("请求体解析失败：{}", ex.getMessage());
    // 返回错误结果，提示请求体格式错误或为空
        return Result.error("请求体格式错误或为空");
    }


    /**
     * 文件上传失败异常捕获
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(MaxUploadSizeExceededException ex) {
        log.error("文件上传失败：{}", ex.getMessage());
        return Result.error("文件大小超过限制,最大5MB");
    }
    @ExceptionHandler(Exception.class)
    public Result exceptionHandler(Exception ex) {
        log.error("未知错误:", ex);
        return Result.error("系统异常，请稍后再试");
    }


}
