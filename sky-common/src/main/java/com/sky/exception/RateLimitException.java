package com.sky.exception;


/**
 * 接口限流异常
 */
public class RateLimitException extends RuntimeException {
    public RateLimitException(String message) {
        super(message);
    }
    public RateLimitException(){

    }
}
