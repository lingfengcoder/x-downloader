package com.lingfeng.rpc.util.relfect;


import com.lingfeng.rpc.util.StringUtils;
import lombok.extern.slf4j.Slf4j;


import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.hutool.core.bean.BeanUtil.copyProperties;
import static com.lingfeng.rpc.util.relfect.BaseDataType.isBaseType;
import static com.lingfeng.rpc.util.relfect.BeanUtils.*;
import static com.lingfeng.rpc.util.relfect.Convert.strToObject;
import static com.lingfeng.rpc.util.relfect.ReflectUtils.getValueByField;


/**
 * @author: wz
 * @Date: 2020/3/30 11:35
 * @Description:
 */
@Slf4j
public class BeanClone {


    /**
     * @Description 指定属性 重写 bean属性 支持多级属性 "user.name.firstname"
     * @param: [srcBean, tarBean]
     * @return: void
     * @author: wz
     * @date: 2020/12/26 12:09
     */
    public static void overwriteBeanProp(Map<String, Object> srcBean, Object tarBean) {

        Set<Map.Entry<String, Object>> array = srcBean.entrySet();
        for (Map.Entry<String, Object> item : array) {
            try {
                setMultiLevelProp(item.getKey(), item.getValue(), tarBean);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Bean属性复制工具方法。
     *
     * @param tar 目标对象
     * @param src 源对象
     */
    public static void copyBeanProp(Object src, Object tar) {
        try {
            copyProperties(src, tar);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static <T> T copyByFields(Object src, Class<T> tar, boolean deepCopy) throws RuntimeException {
        try {
            T t = tar.getDeclaredConstructor().newInstance();
            if (t != null) {
                copyByFields(src, t, deepCopy);
                return t;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @Description: 通过获取元对象与目标对象的Fields属性去 复制对象类似spring的 copybean
     * @author: wz
     * @date: 2020/3/30 15:46
     */
    public static void copyByFields(Object src, Object tar, boolean deepCopy) throws RuntimeException {
//        Assert.notNull(src, "Source must not be null");
//        Assert.notNull(tar, "Target must not be null");
        //递归获取本类及其父类的属性
        Field[] srcFields = getAllFields(src);
        Field[] tarFields = getAllFields(tar);
        for (Field tarField : tarFields) {
            if (Modifier.isFinal(tarField.getModifiers()))//如果是final 类型则不修改 如要修改添加 modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);//fianl标志位置0
                continue;
            String tarFieldName = tarField.getName();
            for (Field srcField : srcFields) {
                String srcFieldName = srcField.getName();
                //名字相同且类型相同
                if (fieldSameName(srcField, tarField) && fieldSameType(srcField, tarField)) {
                    try {
                        //获取元对象的属性值
                        Object value = getValueByFieldName(src, srcFieldName);
                        if (deepCopy) {
                            value = deepCopyByStream(value);
                        }
                        //设置给目标对象对应的属性值
                        setValueByFieldName(tar, tarFieldName, value);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    public static void deepCopyIgnoreTypeWithOtherNames(Object src, Object tar) {
        copyIgnoreTypeWithOtherNames(src, tar, true);
    }


    /**
     * @param src,srcField,tar,tarField,deepCopy
     * @description: 复制属性并转换类型 例如int->string
     * @return: void
     * @author: wz
     * @date: 2020/4/24 14:48
     */
    private static void copyFieldTransType(Object src, Field srcField, Object tar, Field tarField, boolean deepCopy) {
        try {//拷贝属性
            //获取元对象的属性值
            Object value = getValueByFieldName(src, srcField.getName());
            copyFieldTransType(value, tar, tarField, deepCopy);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param value,tar,tarField,deepCopy
     * @description: 复制属性并转换类型 例如int->string
     * @return: void
     * @author: wz
     * @date: 2020/4/24 14:52
     */
    private static void copyFieldTransType(Object value, Object tar, Field tarField, boolean deepCopy) {
        try {//拷贝属性
            //是否开启深度复制,通过流复制新对象
            if (deepCopy) value = deepCopyByStream(value);
            //设置给目标对象对应的属性值
            setValueByFieldNameWithTrans(tar, tarField, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void copyIgnoreTypeWithOtherNames(Object src, Object tar, boolean deepCopy) throws RuntimeException {
        //递归获取本类及其父类的属性
        Field[] srcFields = getAllFields(src);
        Field[] tarFields = getAllFields(tar);
        for (Field tarField : tarFields) {
            if (Modifier.isFinal(tarField.getModifiers()))//如果是final 类型则不修改 如要修改添加 modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);//fianl标志位置0
                continue;
            //String tarFieldName = tarField.getName();
            Alias tarAnnotation = tarField.getAnnotation(Alias.class);
            //目标对象的属性上有注解
            if (tarAnnotation != null) {
                String[] tarOthernames = tarAnnotation.value();
                boolean getOne = false;
                for (String tarName : tarOthernames) {
                    if (getOne) break;
                    //注解不为空的情况下,看命中率,越靠命中率越高
                    if (!StringUtils.isEmpty(tarName)) {
                        for (Field srcField : srcFields) {
                            //如果元数据属性名字直接命中
                            if (srcField.getName().equalsIgnoreCase(tarName)) {//命中
                                //带类型转换的复制
                                copyFieldTransType(src, srcField, tar, tarField, deepCopy);
                                getOne = true;
                                break;
                            } else {//查看元数据属性上的注解是否有命中
                                Alias srcAlias = srcField.getAnnotation(Alias.class);
                                if (srcAlias != null) {
                                    String[] srcNames = srcAlias.value();
                                    for (String srcName : srcNames) {
                                        if (srcName.equals(tarName)) {//命中
                                            //带类型转换的复制
                                            copyFieldTransType(src, srcField, tar, tarField, deepCopy);
                                            getOne = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                //目标对象的属性上没有注解,寻找名字相同的进行复制
                for (Field srcField : srcFields) {
                    if (fieldSameName(srcField, tarField)) {//命中
                        //带类型转换的复制
                        copyFieldTransType(src, srcField, tar, tarField, deepCopy);
                        break;
                    }
                }
            }
        }
    }

    //=============================================stream流操作===========================================================================
    public static <T> List<T> deepCopyListByStream(List<T> src) throws IOException, ClassNotFoundException {
        return (List<T>) deepCopyByStream(src);
    }

    public static <K, V> Map<K, V> deepCopyMapByStream(Map<K, V> src) throws IOException, ClassNotFoundException {
        return (Map<K, V>) deepCopyByStream(src);
    }

    /**
     * @Description 使用stream流进行深度copy
     * @author: wz
     * @date: 2019/3/25 16:32
     */
    public static Object deepCopyByStream(Object src) {
        if (src == null) return null;
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream out = null;
        ByteArrayInputStream byteIn = null;
        ObjectInputStream in = null;
        try {
            out = new ObjectOutputStream(byteOut);
            out.writeObject(src);
            out.flush();
            out.close();
            byteOut.close();
            byteIn = new ByteArrayInputStream(byteOut.toByteArray());
            in = new ObjectInputStream(byteIn);
            @SuppressWarnings("unchecked")
            Object dest = in.readObject();
            in.close();
            byteIn.close();
            return dest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (byteOut != null) {
                try {
                    byteOut.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (byteIn != null) {
                try {
                    byteIn.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }


//=========================================================JSON操作==============================================================

    /**
     * @Description 利用json 对象深拷贝 新旧对象没有关联
     * @author: wz
     * @date: 2019/2/22 15:12
     */
    public static <T> T deepCopyClassByJson(Object srcObj, Class<T> targetClass) {
        return null;
//        return JSON.toJavaObject(JSON.parseObject(JSON.toJSONString(srcObj)), targetClass);
    }

    public static <T> T toJavaBeanByJson(String str, Class<T> targetClass) {
        if (str == null) return null;
        T t = null;
        if (targetClass != null)
            t = strToObject(str, targetClass);
        if (t != null) return t;
        return null;
        //note json  return JSON.toJavaObject(JSONObject.parseObject(str), targetClass);
    }

    /**
     * @Description 利用json 对象深拷贝 新旧对象没有关联
     * @author: wz
     * @date: 2019/2/22 15:12
     */
    public static <T> List<T> deepCopyListByJson(Object srcObj, Class<T> targetClass) {
        List<T> resultList = new ArrayList<>();
        //JSONArray jsonArray = JSONArray.parseArray(JSON.toJSONString(srcObj));
//        for (Object o : jsonArray) {
//            resultList.add(deepCopyClassByJson(o, targetClass));
//        }
        return resultList;
    }


    /**
     * @Description 将对象中 json字符串 转换为对象
     * @author: wz
     * @date: 2019/5/20 10:31
     */
    public static void objFieldToJson(Object obj) {
        Class<?> c = obj.getClass();
        c.getDeclaredFields();
        Field[] fields = c.getDeclaredFields();
        for (Field field : fields) {
            if (field.getType().equals(String.class)) {//String 类型
                Object val = getValueByFieldName(obj, field.getName());
                if (val != null && !val.equals("")) {
                    Object json = val;
                    try {
                        //note json json = JSON.parse(val.toString());
                    } catch (Exception e) {
                        json = val;
                    }
                    try {
                        setValueByFieldName(obj, field.getName(), json);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    public static <T> T copyMapToObject(Map<?, ?> map, Class<T> tar, boolean deepCopy) throws RuntimeException {
        try {
            T t = tar.getDeclaredConstructor().newInstance();
            if (t != null) {
                copyMapToObject(map, t, deepCopy);
            }
            return t;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param map
     * @param tar
     * @param deepCopy 是否深拷贝
     * @description: 将map复制成对象的属性
     * @return: void
     * @author: wz
     * @date: 2020/4/24 15:21
     */
    public static void copyMapToObject(Map<?, ?> map, Object tar, boolean deepCopy) throws RuntimeException {
//        Assert.notNull(map, "Source must not be null");
//        Assert.notNull(tar, "Target must not be null");
        try {
            //获取所有的fields
            Field[] tarFields = getAllFields(tar);
            for (Field targetField : tarFields) {
                if (Modifier.isFinal(targetField.getModifiers()))//如果是final 类型则不修改 如要修改添加 modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);//fianl标志位置0
                    continue;
                //获取属性名
                String tarFieldName = targetField.getName();
                //获取value
                Object value = map.get(tarFieldName);
                try {
                    if (value != null) {
                        copyFieldTransType(value, tar, targetField, deepCopy);
                    } else {
                        //如果不是基础数据类型则是null就是null
                        if (!isBaseType(targetField.getType())) {
                            setValueByFieldName(tar, tarFieldName, null);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }


    /**
     * @Description java 对象转换成map
     * @author wz
     * @date 2022/5/11 13:39
     */
    public static void javaToMap(Object src, Map<String, Object> map) {
        try {
            //获取所有的fields
            Field[] tarFields = getAllFields(src);
            for (Field targetField : tarFields) {
                if (Modifier.isFinal(targetField.getModifiers()))//如果是final 类型则不修改 如要修改添加 modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);//fianl标志位置0
                    continue;
                //获取属性名
                String name = targetField.getName();
                map.put(name, getValueByField(name, src));
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
}
