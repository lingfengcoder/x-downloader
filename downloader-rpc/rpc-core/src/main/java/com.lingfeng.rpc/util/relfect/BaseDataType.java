package com.lingfeng.rpc.util.relfect;

 

/**
 * @author: wz
 * @Date: 2020/3/30 16:53
 * @Description: java基本数据类型与 包装类的映射枚举类
 */
public enum BaseDataType {
    Integer("int", java.lang.Integer.class.getName()),
    Double("double", java.lang.Double.class.getName()),
    Long("long", java.lang.Long.class.getName()),
    Short("short", java.lang.Short.class.getName()),
    Byte("byte", java.lang.Byte.class.getName()),
    Boolean("boolean", java.lang.Boolean.class.getName()),
    Char("char", Character.class.getName()),
    Float("float", java.lang.Float.class.getName());

    BaseDataType(String base, String pack) {
        this.base = base;
        this.pack = pack;
    }

    /**
     * @Description:判断是否是基本数据类型
     * @return:
     * @author: wz
     * @date: 2020/3/30 17:41
     */
    public static Boolean isBaseType(Class t) {
        for ( BaseDataType item : BaseDataType.values()) {
            String temp = item.base;
            if (temp.equals(t.getSimpleName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @Description:根据基本类型获取 包装类完整包名
     * @return:
     * @author: wz
     * @date: 2020/3/30 17:41
     */
    public static String getPackByBase(String base) {
        ////Assert.notNull(base, "param must not be null");
        for (BaseDataType item : BaseDataType.values()) {
            String temp = item.base;
            if (temp.equals(base)) {
                return item.pack;
            }
        }
        return null;
    }

    /**
     * @Description:根据基本类型 获取包装类直接名字
     * @author: wz
     * @date: 2020/3/30 17:46
     */
    public static String getSimplePackByBase(String base) {
        //Assert.notNull(base, "param must not be null");
        for (BaseDataType item : BaseDataType.values()) {
            String temp = item.base;
            if (temp.equals(base)) {
                return item.name();
            }
        }
        return null;
    }

    /**
     * @Description:判断两个类型 基本类和包装类是否相同
     * @author: wz
     * @date: 2020/3/30 17:46
     */
    public static Boolean same(Class clazz1, Class clazz2) {
        //Assert.notNull(clazz1, "param must not be null");
        //Assert.notNull(clazz2, "param must not be null");
        if (clazz1.equals(clazz2)) {
            return true;
        }
        String pk1 = clazz1.getSimpleName();
        String pk2 = clazz2.getSimpleName();
        String pack = getSimplePackByBase(pk1);
        if (pack != null && pack.equals(pk2)) {
            return true;
        }
        String pack2 = getSimplePackByBase(pk2);
        if (pack2 != null && pack2.equals(pk1)) {
            return true;
        }
        return false;
    }

    private String base;
    private String pack;

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public String getPack() {
        return pack;
    }

    public void setPack(String pack) {
        this.pack = pack;
    }
}
