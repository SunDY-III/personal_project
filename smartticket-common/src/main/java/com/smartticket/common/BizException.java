package com.smartticket.common;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    private final int code;
    public BizException(int code, String msg) { super(msg); this.code = code; }
    public BizException(String msg) { this(500, msg); }
}
