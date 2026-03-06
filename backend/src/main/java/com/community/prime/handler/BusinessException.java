package com.community.prime.handler;

import lombok.Getter;

/**
 * 业务异常
 * 
 * 用于区分业务逻辑错误和系统异常
 * 业务异常会返回给前端具体的错误信息
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }
}
