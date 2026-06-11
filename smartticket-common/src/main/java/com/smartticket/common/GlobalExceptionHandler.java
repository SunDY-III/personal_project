package com.smartticket.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BizException.class)
    public R<?> handleBiz(BizException e) { return R.fail(e.getCode(), e.getMessage()); }
    @ExceptionHandler(Exception.class)
    public R<?> handle(Exception e) { log.error("unexpected error", e); return R.fail("服务器内部错误"); }
}
