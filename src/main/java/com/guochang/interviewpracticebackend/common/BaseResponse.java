package com.guochang.interviewpracticebackend.common;

import com.guochang.interviewpracticebackend.common.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 通用返回类
 *
 * @param <T>
 *  
 *  
 */
@Data
public class BaseResponse<T> implements Serializable {

    private int code;

    private T data;

    private String message;

    // 构造方法
    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }


    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    // 失败
    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
