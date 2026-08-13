package com.sky.exception;

/**
 * 员工操作异常（如老板账号保护等）
 */
public class EmployeeOperationException extends BaseException {

    public EmployeeOperationException(String msg) {
        super(msg);
    }
}
