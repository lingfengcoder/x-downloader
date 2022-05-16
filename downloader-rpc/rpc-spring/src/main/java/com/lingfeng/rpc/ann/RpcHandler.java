package com.lingfeng.rpc.ann;

import java.lang.annotation.*;


@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface RpcHandler {

    /**
     * jobhandler name
     */
    String value();

    /**
     * init handler, invoked when JobThread init
     */
    String init() default "";

    /**
     * destroy handler, invoked when JobThread destroy
     */
    String destroy() default "";

}
