package com.smartticket.common;

public class BizException extends RuntimeException {
    private final int code;

    public BizException(int code, String msg) { super(msg); this.code = code; }
    public BizException(String msg) { this(500, msg); }

    public int getCode() { return code; }
}
