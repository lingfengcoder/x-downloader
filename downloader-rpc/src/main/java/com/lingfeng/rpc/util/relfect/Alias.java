package com.lingfeng.rpc.util.relfect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author: wz
 * @Date: 2020/4/16 20:11
 * @Description: 复制不同名称对象的属性的标识符注解
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Alias {
    String[] value() default {""};
}
