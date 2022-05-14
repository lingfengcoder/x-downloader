package com.lingfeng.rpc.util;

import org.apache.commons.lang3.time.DateFormatUtils;

import java.lang.management.ManagementFactory;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TimeUtil extends org.apache.commons.lang3.time.DateUtils {

    public enum DateEMP {
        SECOND, MINUTE, HOUR, YEAR, MONTH, DAY
    }

    public final static String StandardFormat = "yyyy-MM-dd HH:mm:ss";
    public final static String UnderlineFormat = "yyyy_MM_dd_HH_mm_ss";
    public final static String HourFormat = "HH_mm_ss";
    public final static String YYYY_MM_DD_HH_MM_SS = "YYYY_MM_dd_HH_mm_ss";
    private static String[] parsePatterns = {
            "yyyyMMddHHmmss",
            "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM",
            "yyyy/MM/dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm", "yyyy/MM",
            "yyyy.MM.dd", "yyyy.MM.dd HH:mm:ss", "yyyy.MM.dd HH:mm", "yyyy.MM"};

    private final static long timeThreshold = 60000;//60000;//1分钟
    private static long jvmStartTime = 0L;

    public static Calendar addTime(Calendar calendar, int time, DateEMP dateEMP) {
        calendar = calendar == null ? Calendar.getInstance() : calendar;
        switch (dateEMP) {
            case YEAR:
                calendar.add(Calendar.YEAR, time);
                break;
            case MONTH:
                calendar.add(Calendar.MONTH, time);
                break;
            case DAY:
                calendar.add(Calendar.DAY_OF_YEAR, time);
                break;
            case HOUR:
                calendar.add(Calendar.HOUR, time);
                break;
            case MINUTE:
                calendar.add(Calendar.MINUTE, time);
                break;
            case SECOND:
                calendar.add(Calendar.SECOND, time);
                break;
        }
        return calendar;
    }

    /**
     * 比较时间戳
     *
     * @param timeStamp
     * @param date
     * @return 小于 等于true 大于false
     */
    public static boolean compareTimeWithThreshold(long timeStamp, Date date) {
        if (timeStamp + timeThreshold >= date.getTime() && timeStamp - timeThreshold <= date.getTime()) {

            return true;
        }
        return false;
    }

    public static Date getDate(String time, String format) throws ParseException {
        if (StringUtils.isEmpty(time)) {
            return null;
        }

        return new SimpleDateFormat(format).parse(time);
    }

    public static Date getDate(String time) throws ParseException {
        if (StringUtils.isEmpty(time)) {
            return null;
        }
        return new SimpleDateFormat("yyyyMMddHHmmss").parse(time);
    }

    public static Boolean isSameTime(Long time1, Long time2, String format) {
        return formatDate(time1, format).equals(formatDate(time2, format));
    }

    public static Boolean isSameTime(Date time1, Long time2, String format) {
        return formatDate(time1, format).equals(formatDate(time2, format));
    }

    public static Boolean isSameTime(Date time1, Date time2, String format) {
        return formatDate(time1, format).equals(formatDate(time2, format));
    }

    public static int compareTime(Date time1, Date time2) {
        return time1.compareTo(time2);
        //return (int) (time1.getTime() - time2.getTime());
    }

    public static boolean isTimeBigger(Date time1, Date time2) {
        return time1.compareTo(time2) > 0;
    }

    public static String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat("yyyyMMdd_HHmmss").format(date);
    }

    public static String formatDate(Date date, String format) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat(format).format(date);
    }

    /**
     * 日期型字符串转化为日期 格式
     */
    public static Date parseDate(Object str) {
        if (str == null) {
            return null;
        }
        try {
            return parseDate(str.toString(), parsePatterns);
        } catch (ParseException e) {
            return null;
        }
    }

    public static Date formatDateToDate(Date date, String format) throws ParseException {
        if (date == null) {
            return null;
        }
        SimpleDateFormat format1 = new SimpleDateFormat(format);
        String s = format1.format(date);
        return format1.parse(s);
    }

    public static String formatDate(Long timestamp) {
        return new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date(timestamp));
    }

    public static String formatDate(Long timestamp, String format) {
        return new SimpleDateFormat(format).format(new Date(timestamp));
    }

    /**
     * 时间转换为秒
     */
    public static Integer transTimeToSecond(Date date) {
        return date.getHours() * 60 * 60 + date.getMinutes() * 60 + date.getSeconds();
    }

    public static Date transSecondToTime(Integer second) throws ParseException {
        Integer hours = second / 3600;
        second = second % 3600;
        Integer minutes = second / 60;
        second = second % 60;
        Date now = new Date();
        now.setHours(hours);
        now.setMinutes(minutes);
        now.setSeconds(second);
        // SimpleDateFormat format=new SimpleDateFormat("HH:mm:ss");
        // return format.parse(hours+":"+minutes+":"+second);
        return now;
    }


    public static Date transToTime(Long millisecond) throws ParseException {
        Integer second = (int) (millisecond / 1000);
        Integer hours = second / 3600;
        second = second % 3600;
        Integer minutes = second / 60;
        second = second % 60;
        Date now = new Date();
        now.setHours(hours);
        now.setMinutes(minutes);
        now.setSeconds(second);
        // SimpleDateFormat format=new SimpleDateFormat("HH:mm:ss");
        // return format.parse(hours+":"+minutes+":"+second);
        return now;
    }


    /**
     * @Description: 获取定时任务表达式
     * @author: wz
     * @date: 2019/7/3 15:59
     */
    public static String getCron(Date date) {
        String dateFormat = "ss mm HH dd MM ? yyyy";
        return new SimpleDateFormat(dateFormat).format(date);
    }


    public static Long getDiffTime(Date now, Date last, DateEMP dateEMP) {
        long temp = now.getTime() - last.getTime();
        if (dateEMP.equals(DateEMP.SECOND)) {
            return temp / 1000;
        }
        if (dateEMP.equals(DateEMP.MINUTE)) {
            return temp / 1000 / 60;
        }
        if (dateEMP.equals(DateEMP.HOUR)) {
            return temp / 1000 / 60 / 60;
        }
        if (dateEMP.equals(DateEMP.DAY)) {
            return temp / 1000 / 60 / 60 / 24;
        }
        return 0L;
    }

    public static String getDiffTime(Date endDate, Date nowDate)
    {
        long nd = 1000 * 24 * 60 * 60;
        long nh = 1000 * 60 * 60;
        long nm = 1000 * 60;
        // long ns = 1000;
        // 获得两个时间的毫秒时间差异
        long diff = endDate.getTime() - nowDate.getTime();
        // 计算差多少天
        long day = diff / nd;
        // 计算差多少小时
        long hour = diff % nd / nh;
        // 计算差多少分钟
        long min = diff % nd % nh / nm;
        // 计算差多少秒//输出结果
        // long sec = diff % nd % nh % nm / ns;
        return day + "天" + hour + "小时" + min + "分钟";
    }

    public static final String datePath() {
        Date now = new Date();
        return DateFormatUtils.format(now, "yyyy/MM/dd");
    }

    /**
     * 获取服务器启动时间
     */
    public static Date getServerStartDate() {
        if (jvmStartTime == 0)
            jvmStartTime = ManagementFactory.getRuntimeMXBean().getStartTime();
        return new Date(jvmStartTime);
    }

    public static Long getServerStartDateL() {
        return jvmStartTime;
    }
}
