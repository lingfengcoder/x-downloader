package com.lingfeng.rpc.ann;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;


@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Component
public @interface RpcComponent {
    String value() default "";
}
