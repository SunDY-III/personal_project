package com.smartticket.common;


public class R<T> {
    private int code;
    private String msg;
    private T data;

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public static <T> R<T> ok(T data) { R<T> r = new R<>(); r.code = 200; r.msg = "success"; r.data = data; return r; }
    public static <T> R<T> ok() { return ok(null); }
    public static <T> R<T> fail(int code, String msg) { R<T> r = new R<>(); r.code = code; r.msg = msg; return r; }
    public static <T> R<T> fail(String msg) { return fail(500, msg); }
}
