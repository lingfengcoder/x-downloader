package com.lingfeng.rpc.coder;

import com.lingfeng.rpc.coder.biz.SimpleCoder;
import com.lingfeng.rpc.coder.safe.SafeCoder;

/**
 * @Author: wz
 * @Date: 2022/5/11 15:14
 * @Description:
 */
public class CoderFactory {
    private final static CoderFactory instance = new CoderFactory();

    public static CoderFactory getInstance() {
        return instance;
    }

    public <T extends Coder> Coder generate(Class<T> clazz) {
        if (SimpleCoder.class.equals(clazz)) {
            return new SimpleCoder();
        }
        if (SafeCoder.class.equals(clazz)) {
            return new SafeCoder();
        }
        return null;
    }
}
