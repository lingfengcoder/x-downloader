package com.lingfeng.rpc.invoke;


import com.lingfeng.rpc.ann.RpcComponent;
import com.lingfeng.rpc.ann.RpcHandler;
import com.lingfeng.rpc.data.AnnHandler;
import com.lingfeng.rpc.util.relfect.ReflectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Component
public class RpcBeanRegister implements ApplicationContextAware, SmartInitializingSingleton, DisposableBean {


    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        RpcBeanRegister.applicationContext = applicationContext;
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    // start
    @Override
    public void afterSingletonsInstantiated() {
        log.info("============= afterSingletonsInstantiated=============");
        annHandlerRegister(applicationContext);
    }

    // destroy
    @Override
    public void destroy() {
    }

    //存放所有带注解的bean
    private final static ConcurrentHashMap<String, AnnHandler> annHandlers = new ConcurrentHashMap<>();

    public static AnnHandler getHandler(String name) {
        return annHandlers.get(name);
    }

    private void annHandlerRegister(ApplicationContext applicationContext) {
        if (applicationContext == null) {
            return;
        }
        // 找到rpc的bean
        Map<String, Object> beansMap = applicationContext.getBeansWithAnnotation(RpcComponent.class);
        Collection<Object> beans = beansMap.values();
        //找到rpc的method 和name
        for (Object bean : beans) {
            //获取全部方法
            HashSet<Method> allMethod = ReflectUtils.getAllMethod(bean.getClass());
            for (Method method : allMethod) {
                //获取方法上的注解
                RpcHandler rpcHandler = method.getAnnotation(RpcHandler.class);
                if (rpcHandler != null) {
                    String name = rpcHandler.value();
                    //重名检测
                    if (annHandlers.containsKey(name)) {
                        throw new RuntimeException("name= " + name + " handler already exist! please check name");
                    }
                    AnnHandler annHandler = new AnnHandler();
                    annHandler.setBean(bean);
                    annHandler.setName(name);
                    annHandler.setMethod(method);
                    annHandlers.put(name, annHandler);
                }
            }
        }
    }


}
