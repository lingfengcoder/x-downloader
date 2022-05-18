package com.lingfeng.biz.downloader.model.resp;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @Author: wz
 * @Date: 2021/11/11 11:04
 * @Description:
 */
@Setter
@Getter
@Accessors(chain = true)
@ToString
public class R<T> implements Serializable {

    enum Resp {
        SUCCESS(200, "成功"),
        FAIL(4000, "失败");
        public Integer code;
        public String msg;

        Resp(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }
    }

    private Integer code;
    private String message;
    private T data;


    public static <T> R<T> success(T data) {
        return resp(Resp.SUCCESS, data);
    }

    public static R<?> fail(String msg) {
        return resp(Resp.FAIL, null).setMessage(msg);
    }

    public static <T> R<T> fail(String msg, T data) {
        return resp(Resp.FAIL, data).setMessage(msg);
    }

    public static R<?> fail(int code, String msg) {
        return resp(Resp.FAIL, null).setCode(code).setMessage(msg);
    }

    public static <T> R<T> fail(T data, int code, String msg) {
        return resp(Resp.FAIL, data).setCode(code).setMessage(msg);
    }

    private static <T> R<T> resp(Resp resp, T t) {
        return new R<T>().setCode(resp.code).setMessage(resp.msg).setData(t);
    }
}
