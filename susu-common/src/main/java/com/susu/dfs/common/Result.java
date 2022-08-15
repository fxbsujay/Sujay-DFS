package com.susu.dfs.common;


import com.susu.dfs.common.eum.ErrorEnum;
import lombok.Data;

/**
 * <p>Description: 结果返回类</p>
 * @author sujay
 * @version 14:36 2022/7/1
 */
@Data
public class Result<T> {

    /**
     * 编码
     **/
    private int code = 200;

    /**
     * 消息内容
     **/
    private String msg = "操作成功";

    /**
     * 数据
     **/
    private T data;

    private Result() {
    }

    private Result(T data) {
        this.data = data;
    }

    private Result(Integer code) {
        this.code = code;
    }

    private Result(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    private Result(String msg) {
        this.msg = msg;
    }


    private Result(T data, String msg) {
        this.data = data;
        this.msg = msg;
    }

    public static Result<String> ok() {
        return new Result<>();
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(data);
    }

    public static <T> Result<T> ok(T data,String msg) {
        return new Result<>(data,msg);
    }


    public static <T> Result<T> error() {
        return new Result<>(ErrorEnum.ERROR_500.getCode(),ErrorEnum.ERROR_500.getMessage());
    }

    public static <T> Result<T> error(String msg) {
        return new Result<>(ErrorEnum.ERROR_500.getCode(),msg);
    }

    public static <T> Result<T> error(Integer code,String msg) {
        return new Result<>(code,msg);
    }

    public static <T> Result<T> error(ErrorEnum e) {
        return new Result<>(e.getCode(),e.getMessage());
    }

}
