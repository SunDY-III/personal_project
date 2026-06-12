package com.smartticket.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public R<?> handleBiz(BizException e) { return R.fail(e.getCode(), e.getMessage()); }

    @ExceptionHandler(Exception.class)
    public R<?> handle(Exception e) { log.error("unexpected error", e); return R.fail("服务器内部错误"); }
}
