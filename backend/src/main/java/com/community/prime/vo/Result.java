package com.community.prime.vo;

import lombok.Data;

/**
 * 统一响应结果封装
 * 
 * 技术说明：
 * 1. 所有接口统一返回此结构
 * 2. success表示业务是否成功
 * 3. code为业务状态码，非HTTP状态码
 * 4. msg为提示信息
 * 5. data为实际业务数据
 */
@Data
public class Result {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 错误码
     */
    private Integer code;

    /**
     * 错误信息
     */
    private String msg;

    /**
     * 数据
     */
    private Object data;

    public static Result ok() {
        return new Result() {{
            setSuccess(true);
            setCode(200);
            setMsg("success");
        }};
    }

    public static Result ok(Object data) {
        return new Result() {{
            setSuccess(true);
            setCode(200);
            setMsg("success");
            setData(data);
        }};
    }

    public static Result fail(String msg) {
        return new Result() {{
            setSuccess(false);
            setCode(500);
            setMsg(msg);
        }};
    }

    public static Result fail(Integer code, String msg) {
        return new Result() {{
            setSuccess(false);
            setCode(code);
            setMsg(msg);
        }};
    }

    public static Result fail(Integer code, String msg, Object data) {
        return new Result() {{
            setSuccess(false);
            setCode(code);
            setMsg(msg);
            setData(data);
        }};
    }
}
