package com.lingfeng.rpc.invoke;

import com.lingfeng.rpc.data.AnnHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;

@Slf4j
public class RpcInvokeProxy {

    public static Object invoke(Consumer<Object> postHandler, String name, Object... param) {
        //从方法集中找到bean和method
        AnnHandler handler = RpcBeanRegister.getHandler(name);
        if (handler != null) {
            //执行方法
            Object bean = handler.getBean();
            Method method = handler.getMethod();
            Object result = null;
            try {
                result = method.invoke(bean, param);
            } catch (IllegalAccessException | InvocationTargetException e) {
                log.error(e.getMessage(), e);
            }
            if (postHandler != null) {
                postHandler.accept(result);
            }
        } else {
            log.error("没有找到 @RpcHandler(\"" + name + "\")的处理器");
        }
        return null;
    }

    public static Object invoke(String name, Object... param) {
        //从方法集中找到bean和method
        AnnHandler handler = RpcBeanRegister.getHandler(name);
        if (handler != null) {
            //执行方法
            Object bean = handler.getBean();
            Method method = handler.getMethod();
            Object result = null;
            try {
                result = method.invoke(bean, param);
            } catch (IllegalAccessException | InvocationTargetException e) {
                log.error(e.getMessage(), e);
            }
            return result;
        } else {
            log.error("没有找到 @RpcHandler(\"" + name + "\")的处理器");
        }
        return null;
    }
}
