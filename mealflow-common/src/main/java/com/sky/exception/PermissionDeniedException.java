package com.sky.exception;

public class PermissionDeniedException extends BaseException {
    /**
     * 权限不足异常
     * @param message
     */
    public PermissionDeniedException(String message) {
        super(message);
    }
}
