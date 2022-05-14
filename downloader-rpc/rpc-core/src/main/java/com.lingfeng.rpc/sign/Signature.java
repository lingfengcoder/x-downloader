package com.lingfeng.rpc.sign;

import com.lingfeng.rpc.frame.SafeFrame;
import com.lingfeng.rpc.util.dae.md5.MD5Util;
import com.lingfeng.rpc.util.relfect.BeanClone;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @Author: wz
 * @Date: 2022/5/11 11:21
 * @Description:
 */
@Slf4j
public class Signature {
    //不需要参与签名的属性
    private final static String[] NO_JOIN_SIGN = new String[]{"sign"};

    public static String sign(SafeFrame safeFrame) {
        HashMap<String, Object> map = new HashMap<>();
        BeanClone.javaToMap(safeFrame, map);
        return signMap(map);
    }


    private static boolean noNeedSign(String str) {
        for (String s : NO_JOIN_SIGN) {
            if (s.equalsIgnoreCase(str)) {
                return true;
            }
        }
        return false;
    }

    public static <T> String signMap(Map<? extends String, ? extends T> data) {
        Set<? extends String> keySet = data.keySet();
        String[] keyArray = keySet.toArray(new String[keySet.size()]);
        T val;
        String sval;
        Arrays.sort(keyArray);
        StringBuilder builder = new StringBuilder();
        for (String k : keyArray) {
            //去掉不需要参与签名的字段
            if (noNeedSign(k)) continue;
            val = data.get(k);
            if (val != null) {
                sval = val.toString().trim();
                if (!StringUtil.isNullOrEmpty(sval)) {
                    builder.append(k).append("=").append(sval).append("&");
                }
            }
        }
        if (builder.length() > 0) {
            sval = builder.toString();
            if (sval.endsWith("&")) {
                sval = sval.substring(0, sval.length() - 1);
            }
//            log.info("签名前数据：{}", sval);
            return MD5Util.md5_32(sval);
        }
        return null;
    }
}
