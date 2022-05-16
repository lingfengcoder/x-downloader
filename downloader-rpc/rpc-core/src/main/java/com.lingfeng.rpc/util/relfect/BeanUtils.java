package com.lingfeng.rpc.util.relfect;


import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.lingfeng.rpc.util.relfect.Convert.objectToTarget;
import static com.lingfeng.rpc.util.relfect.ReflectUtils.getField;


/**
 * Bean 工具类
 *
 * @author freebook
 */
public class BeanUtils {


    /**
     * Bean方法名中属性名开始的下标
     */
    private static final int BEAN_METHOD_PROP_INDEX = 3;

    /**
     * 匹配getter方法的正则表达式
     */
    private static final Pattern GET_PATTERN = Pattern.compile("get(\\p{javaUpperCase}\\w*)");

    /**
     * 匹配setter方法的正则表达式
     */
    private static final Pattern SET_PATTERN = Pattern.compile("set(\\p{javaUpperCase}\\w*)");


    public static boolean fieldSameType(Field srcField, Field tarField) {
        //Assert.notNull(srcField, "Source must not be null");
        //Assert.notNull(tarField, "Source must not be null");
        //属性名字相同
        if (tarField.getType().equals(srcField.getType())) {
            return true;
            //基础数据类型和包装类
        } else if (BaseDataType.same(srcField.getClass(), tarField.getType())) {
            return true;
        }
        return false;
    }

    public static boolean fieldSameName(Field srcField, Field tarField) {
        //Assert.notNull(srcField, "Source must not be null");
        //Assert.notNull(tarField, "Source must not be null");
        //属性名字相同
        if (srcField.getName().equalsIgnoreCase(tarField.getName())) {
            return true;
        }
        return false;
    }

    public static boolean classSameType(Class srcClazz, Class tarClazz) {
        //Assert.notNull(srcClazz, "Source must not be null");
        //Assert.notNull(tarClazz, "Source must not be null");
        //属性名字相同
        if (srcClazz.equals(tarClazz)) {
            return true;
        } else if (srcClazz.getName().equalsIgnoreCase(tarClazz.getName())) {
            return true;
        } else if (BaseDataType.same(srcClazz, tarClazz)) {
            return true;
        }
        return false;
    }

    /**
     * 获取对象的setter方法。 (包括父类和接口)
     *
     * @param obj 对象
     * @return 对象的setter方法列表
     */
    public static List<Method> getSetterMethods(Object obj) {
        //Assert.notNull(obj, "Source must not be null");
        // setter方法列表
        List<Method> setterMethods = new ArrayList<>();
        // 获取所有方法
        Method[] methods = obj.getClass().getMethods();
        // 查找setter方法
        for (Method method : methods) {
            Matcher m = SET_PATTERN.matcher(method.getName());
            if (m.matches() && (method.getParameterTypes().length == 1)) {
                setterMethods.add(method);
            }
        }
        // 返回setter方法列表
        return setterMethods;
    }

    /**
     * 获取对象的getter方法。
     *
     * @param obj 对象
     * @return 对象的getter方法列表
     */

    public static List<Method> getGetterMethods(Object obj) {
        //Assert.notNull(obj, "Source must not be null");
        // getter方法列表
        List<Method> getterMethods = new ArrayList<Method>();
        // 获取所有方法
        Method[] methods = obj.getClass().getMethods();
        // 查找getter方法
        for (Method method : methods) {
            Matcher m = GET_PATTERN.matcher(method.getName());
            if (m.matches() && (method.getParameterTypes().length == 0)) {
                getterMethods.add(method);
            }
        }
        // 返回getter方法列表
        return getterMethods;
    }

    /**
     * 检查Bean方法名中的属性名是否相等。<br>
     * 如getName()和setName()属性名一样，getName()和setAge()属性名不一样。
     *
     * @param m1 方法名1
     * @param m2 方法名2
     * @return 属性名一样返回true，否则返回false
     */

    public static boolean isMethodPropEquals(String m1, String m2) {
        //Assert.notNull(m1, "Source must not be null");
        //Assert.notNull(m2, "Target must not be null");
        return m1.substring(BEAN_METHOD_PROP_INDEX).equals(m2.substring(BEAN_METHOD_PROP_INDEX));
    }


    /**
     * @Description
     * @param: 获取对象所有的fields
     * @return:
     * @author: wz
     * @date: 2020/3/30 15:19
     */
    public static Field[] getAllFields(Object src) {
        //Assert.notNull(src, "Source must not be null");
        Field[] fields = {};
        List<Field> fieldList = new ArrayList<>();
        for (Class<?> superClass = src.getClass(); superClass != Object.class; superClass = superClass.getSuperclass()) {
            try {
                Field[] temp = superClass.getDeclaredFields();
                fieldList.addAll(Arrays.asList(temp));
            } catch (Exception e) {
                continue;
            }
        }
        return fieldList.toArray(fields);
    }


    /**
     * @Description 根据对象的属性名获取属性值
     * @author: wz
     * @date: 2019/2/22 16:59
     */
    public static Object getValueByFieldName(Object srcObj, String fieldName) throws RuntimeException {
        //Assert.notNull(srcObj, "Source must not be null");
        //Assert.notNull(fieldName, "fieldName must not be null");
        try {
            PropertyDescriptor srcProp = new PropertyDescriptor(fieldName, srcObj.getClass());
            Method readMethod = srcProp.getReadMethod();//获得读方法
            if (readMethod != null) {
                if (!Modifier.isPublic(readMethod.getDeclaringClass().getModifiers()) || !readMethod.isAccessible()) {
                    return readMethod.invoke(srcObj);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(srcObj.getClass() + "的" + fieldName + "没有读取的方法");
        }
        return null;
    }


    /**
     * @Description 根据对象的属性名设置属性值
     * @author: wz
     * @date: 2019/2/22 17:05
     */
    public static void setValueByFieldName(Object srcObj, String fieldName, Object value) throws RuntimeException {
        //Assert.notNull(srcObj, "Source must not be null");
        //Assert.notNull(fieldName, "fieldName must not be null");
        try {
            PropertyDescriptor srcProp = new PropertyDescriptor(fieldName, srcObj.getClass());
            Method writeMethod = srcProp.getWriteMethod();//获得写方法
            if (writeMethod != null) {
                if (!Modifier.isPublic(writeMethod.getDeclaringClass().getModifiers()) || !writeMethod.isAccessible()) {
                    writeMethod.setAccessible(true);
                }
                writeMethod.invoke(srcObj, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(srcObj.getClass() + "的" + fieldName + "没有写入的方法或数据类型不一致");
        }
    }

    /**
     * @Description: 简单多层属性设置 ("user.name.firstname","wz",new User()) 只能设置存在的属性，中间不能有断层
     * user.name.firstname=wz
     * user:{
     * name:{
     * firstname:"wz"
     * }
     * }
     * @param: [propName, value, tarBean]
     * @return: void
     * @author: wz
     * @date: 2020/12/26 11:33
     */
    public static void setMultiLevelProp(String propName, Object value, Object tarBean) throws IllegalArgumentException {
        //Assert.notNull(propName, "Source must not be null");
        //Assert.notNull(tarBean, "Target must not be null");
        if (propName.contains(".")) {
            String[] split = propName.split("\\.");
            //如果只有一层直接设置
            if (split.length == 1) {
                setValueByFieldName(tarBean, split[0], value);
                // setValueByFieldNameWithTrans(tarBean,propName,value);
            } else {
                Object levelBean = tarBean;
                Object valueBean = null;
                int x = split.length;
                //寻找最后一层 即目标要设置的属性对象
                for (String levelName : split) {
                    valueBean = getValueByFieldName(levelBean, levelName);
                    --x;
                    if (x != 0) levelBean = valueBean;
                    if (valueBean == null) break;
                }
                if (x == 0) {
                    //已找到最后的属性，levelBean 就是属性所在的对象;对levelbean进行属性设置
                    try {
                        setValueByFieldNameWithTrans(levelBean, split[split.length - 1], value);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //setValueByFieldName(levelBean, split[split.length - 1], value);
                } else return;//说明有断层，不予处理
            }
        } else {
            try {
                setValueByFieldNameWithTrans(tarBean, propName, value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param srcObj,srcField,value
     * @description: 设置对象属性的值，并进行类型转换 例如int->string
     * @return: void
     * @author: wz
     * @date: 2020/4/24 14:55
     */
    public static void setValueByFieldNameWithTrans(Object srcObj, Field srcField, Object value) {
        //转换参数为目标参数类型
        Object newValue = objectToTarget(value, srcField.getType());
        setValueByFieldName(srcObj, srcField.getName(), newValue);
    }

    public static void setValueByFieldNameWithTrans(Object srcObj, String fieldName, Class<?> fieldClazz, Object value) {
        //转换参数为目标参数类型
        Object newValue = objectToTarget(value, fieldClazz);
        setValueByFieldName(srcObj, fieldName, newValue);
    }

    public static void setValueByFieldNameWithTrans(Object srcObj, String fieldName, Object value) throws Exception {
        Field field = null;
//            Field[] fields = getAllFields(srcObj);
//            for(Field item:fields){
//                if(item.getName().equalsIgnoreCase(fieldName)){
//                    field=item;
//                    break;
//                }
//            }
//            if(field==null){
//                throw new BizException("属性"+fieldName+"在"+srcObj.getClass()+"中不存在");
//            }
        field = getField(fieldName, srcObj);
        //转换参数为目标参数类型
        Object newValue = objectToTarget(value, field.getType());
        setValueByFieldName(srcObj, fieldName, newValue);
    }

    /**
     * @Description
     * @param: 通过access直接 获取指定属性的属性值 (不推荐 )
     * @return:
     * @author: wz
     * @date: 2020/3/30 14:28
     */
    public static Object getValueByFieldNameAccess(Object srcObj, String fieldName) throws RuntimeException, IllegalAccessException {
        //Assert.notNull(srcObj, "Source must not be null");
        //Assert.notNull(fieldName, "fieldName must not be null");
        Class<?> srcObjClass = srcObj.getClass();
        Field[] srcObjClassFields = srcObjClass.getDeclaredFields();
        for (Field field : srcObjClassFields) {
            if (Modifier.isFinal(field.getModifiers())) {
                continue;
            }//如果是final 类型则不修改 如要修改添加 modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);//fianl标志位置0

            //获取属性名
            String tempFieldName = field.getName();
            if (tempFieldName.equals(fieldName)) {
                if (!field.isAccessible()) {
                    //获取private权限
                    field.setAccessible(true);
                    Object o = field.get(srcObj);
                    // field.setAccessible(false);
                    return o;
                }
                return field.get(srcObj);
            }
        }
        return null;
    }

    /**
     * @Description
     * @param: 通过access直接 设置指定属性的属性值 (不推荐 )
     * @return:
     * @author: wz
     * @date: 2020/3/30 14:28
     */
    public static void setValueByFieldNameAccess(Object srcObj, String fieldName, Object value) throws RuntimeException, IllegalAccessException {
        //Assert.notNull(srcObj, "Source must not be null");
        //Assert.notNull(fieldName, "fieldName must not be null");
        Class<?> srcObjClass = srcObj.getClass();
        Field[] srcObjClassFields = srcObjClass.getDeclaredFields();
        for (Field field : srcObjClassFields) {
            if (Modifier.isFinal(field.getModifiers()))//如果是final 类型则不修改 如要修改添加 modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);//fianl标志位置0
                continue;
            //获取属性名
            String tempFieldName = field.getName();
            if (tempFieldName.equals(fieldName)) {
                if (!field.isAccessible()) {
                    //获取private权限
                    field.setAccessible(true);
                    field.set(srcObj, value);
                    // field.setAccessible(false);
                } else {
                    field.set(srcObj, value);
                }
            }
        }
    }

}
