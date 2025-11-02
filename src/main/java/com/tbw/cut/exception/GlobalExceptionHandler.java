package com.tbw.cut.exception;

import com.tbw.cut.dto.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 处理所有异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseResult<String> handleException(Exception e) {
        log.error("系统异常", e);
        return ResponseResult.error("系统异常: " + e.getMessage());
    }
    
    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseResult<String> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("参数校验异常", e);
        return ResponseResult.error("参数校验异常: " + e.getMessage());
    }
}