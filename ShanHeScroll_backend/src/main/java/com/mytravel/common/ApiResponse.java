package com.mytravel.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 统一 API 响应体，所有接口返回此结构。
 * <pre>
 * {
 *   "code": 200,
 *   "message": "success",
 *   "data": { ... }
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;

    private ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ---- 成功 ----

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(200, "success", null);
    }

    // ---- 失败 ----

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(400, message, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message, null);
    }

    // ---- getters (Jackson 序列化需要) ----

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public T getData() { return data; }
}
