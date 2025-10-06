package com.guochang.interviewpracticebackend.exception;


import com.guochang.interviewpracticebackend.common.BaseResponse;
import com.guochang.interviewpracticebackend.common.ErrorCode;
import com.guochang.interviewpracticebackend.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return Result.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }
}
