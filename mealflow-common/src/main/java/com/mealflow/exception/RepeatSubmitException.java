package com.mealflow.exception;

/**
 * 重复提交异常
 */

public class RepeatSubmitException extends RuntimeException {
    public RepeatSubmitException(String message) {
        super(message);
    }
    public RepeatSubmitException() {

    }

}
