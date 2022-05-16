package com.lingfeng.rpc.util.relfect;



import com.lingfeng.rpc.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;


public class ReflectUtils {

    /**
     * 根据属性名获取属性
     */
    public static Field getField(String fieldName, Class<?> clazz) {
        Class<?> old = clazz;
        Field field = null;
        for (; clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                field = clazz.getDeclaredField(fieldName);
                if (field != null) {
                    break;
                }
            } catch (Exception e) {
            }
        }
        if (field == null) {
            throw new NullPointerException(old + "没有" + fieldName + "属性");
        }
        return field;
    }

    /**
     * 获取目标类的属性
     */
    public static Field getField(String fieldName, String className) {
        try {
            return getField(fieldName, Class.forName(className));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 获取目标对象的属性
     */
    public static Field getField(String fieldName, Object object) {
        return getField(fieldName, object.getClass());
    }


    /**
     * 获取当前类的属性 包括父类
     */
    public static List<Field> getFields(Class<?> clazz, Class<?> stopClass) {
        try {
            List<Field> fieldList = new ArrayList<>();
            while (clazz != null && clazz != stopClass) {//当父类为null的时候说明到达了最上层的父类(Object类).
                fieldList.addAll(Arrays.asList(clazz.getDeclaredFields()));
                clazz = clazz.getSuperclass(); //得到父类,然后赋给自己
            }
            return fieldList;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }


    /**
     * 获取当前类的属性 包括父类
     */
    @Deprecated
    public static List<Field> getFields(Class<?> clazz) {
        return getFields(clazz, Object.class);
    }

    private static List<Class<?>> getSuperClasses(Class<?> clazz, Class<?> stopClass) {
        List<Class<?>> classes = new ArrayList<>();
        while (clazz != null && clazz != stopClass) {//当父类为null的时候说明到达了最上层的父类(Object类).
            classes.add(clazz);
            clazz = clazz.getSuperclass(); //得到父类,然后赋给自己
        }
        return classes;
    }


    /**
     * 通过属性赋值
     */
    public static void setValueByField(String fieldName, Object object, Object value) {
        Field field = getField(fieldName, object.getClass());
        setValueByField(field, object, value);
    }

    /**
     * 通过属性赋值
     */
    public static void setValueByField(Field field, Object object, Object value) {
        try {
            if (!field.isAccessible()) {
                field.setAccessible(true);
                field.set(object, value);
                field.setAccessible(false);
            } else {
                field.set(object, value);
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }

    }

    /**
     * 获取属性的值
     */
    public static <T> T getValueByField(String fieldName, Object object) {
        Field field = getField(fieldName, object.getClass());
        return getValueByField(field, object);
    }

    /**
     * 获取属性的值
     */
    public static <T> T getValueByField(Field field, Object object) {
        try {
            Object value;
            if (!field.isAccessible()) {
                field.setAccessible(true);
                value = field.get(object);
                field.setAccessible(false);
            } else {
                value = field.get(object);
            }
            return (T) value;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * 通过set方法赋值
     */
    public static void setValueBySetMethod(String fieldName, Object object, Object value) {
        if (object == null) {
            throw new RuntimeException("实例对象不能为空");
        }
        if (value == null) {
            return;
        }
        try {
            String setMethodName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            Method setMethod = getMethod(setMethodName, object.getClass(), value.getClass());
            setMethod.invoke(object, value);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * 通过set方法赋值
     */
    public static void setValueBySetMethod(Field field, Object object, Object value) {
        if (object == null) {
            throw new RuntimeException("实例对象不能为空");
        }
        if (value == null) {
            return;
        }
        setValueBySetMethod(field.getName(), object, value);
    }

    /**
     * 通过get方法取值
     */
    public static <T> T getValueByGetMethod(String fieldName, Object object) {
        try {
            if (StringUtils.isNotBlank(fieldName)) {
                String getMethodName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                Method getMethod = getMethod(getMethodName, object.getClass());
                return (T) getMethod.invoke(object);
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * 通过get方法取值
     */
    public static <T> T getValueByGetMethod(Field field, Object object) {
        return getValueByGetMethod(field.getName(), object);
    }


    /**
     * 获取某个类的某个方法(当前类和父类)
     */
    public static HashSet<Method> getAllMethod(Class<?> clazz) {
        //本类方法
        Method[] methods = clazz.getDeclaredMethods();
        HashSet<Method> result = new LinkedHashSet<>(Arrays.asList(methods));
        //super方法
        for (; clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                Method[] superMethods = clazz.getMethods();
                result.addAll(Arrays.asList(superMethods));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (result.isEmpty()) {
            throw new NullPointerException(clazz.getName() + "没有任何方法");
        }
        return result;
    }

    /**
     * 获取某个类的某个方法(当前类和父类)
     */
    public static Method getMethod(String methodName, Class<?> clazz) {
        Method method = null;
        for (; clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                method = clazz.getDeclaredMethod(methodName);
                break;
            } catch (Exception e) {
            }
        }
        if (method == null) {
            throw new NullPointerException("没有" + methodName + "方法");
        }
        return method;
    }

    /**
     * 获取get方法
     *
     * @param fieldName 属性名
     * @return
     */
    public static String getMethodName(String fieldName) {
        String methodName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        return methodName;
    }

    /**
     * 获取某个类的某个方法(当前类和父类) 带一个参数
     */
    public static Method getMethod(String methodName, Class<?> clazz, Class<?> paramType) {
        Method method = null;
        for (; clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                method = clazz.getDeclaredMethod(methodName, paramType);
                if (method != null) {
                    return method;
                }
            } catch (Exception e) {
            }
        }
        if (method == null) {
            throw new NullPointerException(clazz + "没有" + methodName + "方法");
        }
        return method;
    }

    /**
     * 获取某个类的某个方法(当前类和父类)
     */
    public static Method getMethod(String methodName, Object obj) {
        return getMethod(methodName, obj.getClass());
    }

    /**
     * 获取某个类的某个方法(当前类和父类) 一个参数
     */
    public static Method getMethod(String methodName, Object obj, Class<?> paramType) {
        return getMethod(methodName, obj.getClass(), paramType);
    }

    /**
     * 获取某个类的某个方法(当前类和父类)
     */
    public static Method getMethod(String methodName, String clazz) {
        try {
            return getMethod(methodName, Class.forName(clazz));
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }

    }

    /**
     * 获取某个类的某个方法(当前类和父类) 一个参数
     */
    public static Method getMethod(String methodName, String clazz, Class<?> paramType) {
        try {
            return getMethod(methodName, Class.forName(clazz), paramType);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }

    }

    /**
     * 获取方法上的注解
     */
    public static Annotation getMethodAnnotation(Method method, Class targetAnnotationClass) {
        Annotation methodAnnotation = method.getAnnotation(targetAnnotationClass);
        return methodAnnotation;
    }

    /**
     * 获取属性上的注解
     */
    public static Annotation getFieldAnnotation(Field field, Class targetAnnotationClass) {
        Annotation methodAnnotation = field.getAnnotation(targetAnnotationClass);
        return methodAnnotation;
    }

    /**
     * 获取类上的注解
     *
     * @param targetAnnotationClass 目标注解
     * @param targetObjcetClass     目标类
     * @return 目标注解实例
     */
    public static Annotation getClassAnnotation(Class targetAnnotationClass, Class<?> targetObjcetClass) {
        Annotation methodAnnotation = targetObjcetClass.getAnnotation(targetAnnotationClass);
        return methodAnnotation;
    }

    /**
     * 获取类上的注解
     *
     * @return 目标注解实例
     */
    public static Annotation getClassAnnotation(Class targetAnnotationClass, Object obj) {
        return getClassAnnotation(targetAnnotationClass, obj.getClass());
    }

    /**
     * 获取类上的注解
     *
     * @return 目标注解实例
     */
    public static Annotation getClassAnnotation(Class targetAnnotationClass, String clazz) {
        try {
            return getClassAnnotation(targetAnnotationClass, Class.forName(clazz));
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }

    }

    /**
     * 获取注解某个属性的值
     *
     * @param methodName 属性名
     * @param annotation 目标注解
     * @param <T>        返回类型
     * @throws Exception
     */
    public static <T> T getAnnotationValue(String methodName, Annotation annotation) {
        try {
            Method method = annotation.annotationType().getMethod(methodName);
            Object object = method.invoke(annotation);
            return (T) object;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }

    }

    /**
     * 获取某个类的某个方法上的某个注解的属性
     *
     * @param methodName            注解属性的名字
     * @param targetAnnotationClass 目标注解
     * @param targetObjecMethodName 目标类的方法
     * @param targetObjectClass     目标类
     * @param <T>                   返回值类型
     */
    public static <T> T getMethodAnnotationValue(String methodName, Class targetAnnotationClass, String targetObjecMethodName, Class targetObjectClass) {
        Method method = getMethod(targetObjecMethodName, targetObjectClass);
        Annotation annotation = getMethodAnnotation(method, targetAnnotationClass);
        return getAnnotationValue(methodName, annotation);
    }

    /**
     * @param methodName            注解属性名
     * @param targetAnnotationClass 目标注解
     * @param targetObjecFieldName  目标属性名字
     * @param targetObjectClass     目标类
     * @param <T>                   返回值类型
     */
    public static <T> T getFieldAnnotationValue(String methodName, Class targetAnnotationClass, String targetObjecFieldName, Class targetObjectClass) {
        Field field = getField(targetObjecFieldName, targetObjectClass);
        Annotation annotation = getFieldAnnotation(field, targetAnnotationClass);
        return getAnnotationValue(methodName, annotation);
    }

    /**
     * 判断 clazz是否是target的子类型或者相等
     */
    public static boolean isSubClassOrEquesClass(Class<?> clazz, Class<?> target) {
        if (clazz == target) {
            return true;
        }
        while (clazz != Object.class) {
            if (clazz == target) {
                return true;
            }
            clazz = clazz.getSuperclass();
        }
        return false;
    }

}
