package com.susu.dfs.common.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

/**
 * <p>Description: Date processing class</p>
 * <p>时间处理工具类</p>
 * @author sujay
 * @version 22:58 2022/1/22
 * @see Date
 * @since JDK1.8
 */
public class DateUtils {

    public static final String PATTERN_WHOLE = "yyyy-MM-dd HH:mm:ss";

    public static final String PATTERN_YMD = "yyyy-MM-dd";

    /**
     *  <p>Description: Gets the current time and converts it to a date format string</p>
     *  <p>获取当前时间，转换为日期格式字符串</p>
     */
    public static String getTime() {
        return getDateTimeFormatter().format(getLocalDateTime());
    }

    public static String getTime(Date date) {
        return  getDateTimeFormatter().format(getLocalDateTime(date));
    }

    public static String getTime(Date date,String pattern) {
        return  getDateTimeFormatter(pattern).format(getLocalDateTime(date));
    }

    /**
     *  <p>Description: Get the year of the time</p>
     *  <p>获取时间所在年份</p>
     *
     */
    public static int getYear(Date date) {
        Calendar cd = getCalender(date);
        return cd.get(Calendar.YEAR);
    }

    /**
     *  <p>Description: Get the month of the time</p>
     *  <p>获取时间所在月份</p>
     *  从 0 开始 到 1
     */
    public static int getMonth(Date date) {
        Calendar cd = getCalender(date);
        return cd.get(Calendar.MONTH);
    }

    /**
     *  <p>Description: Get the month of the time</p>
     *  <p>获取时间所在周数</p>
     * @return 返回1是星期日、2是星期一、3是星期二、4是星期三、5是星期四、6是星期五、7是星期六
     */
    public static int getWeek(Date date) {
        Calendar cd = getCalender(date);
        return cd.get(Calendar.DAY_OF_WEEK);
    }

    /**
     *  <p>Description: Gets the current time in milliseconds</p>
     *  <p>获取当前时间毫秒数</p>
     */
    public static long getTimeMillis() {
        return System.currentTimeMillis();
    }

    public static long getTimeMillis(Date date) {
        return date.getTime();
    }

    /**
     *  <p>Description: Get date class</p>
     *  <p>获取日期类</p>
     */
    public static LocalDateTime getLocalDateTime() {
        return  LocalDateTime.now();
    }

    public static LocalDateTime getLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        Instant instant = date.toInstant();
        return  instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     *  <p>Description: Get date formatting class</p>
     *  <p>获取日期格式化类</p>
     */
    public static DateTimeFormatter getDateTimeFormatter(String pattern){
        return DateTimeFormatter.ofPattern(pattern);
    }

    public static DateTimeFormatter getDateTimeFormatter(){
        return DateTimeFormatter.ofPattern(PATTERN_WHOLE);
    }

    public static Calendar getCalender(Date date) {
        Calendar cd = getCalender();
        cd.setTime(date);
        return cd;
    }

    public static Calendar getCalender() {
        return Calendar.getInstance();
    }

    public static void main(String[] args) {
        System.out.println("当前系统时间：" + getTime());
        System.out.println("时间格式化：" + getTime(new Date(),PATTERN_YMD));
        System.out.println("当前时间毫秒数：" + getTimeMillis());
        System.out.println("时间转毫秒数：" + getTimeMillis(new Date()));
        System.out.println("获取时间所在年份：" + getYear(new Date()));
        System.out.println("获取时间所在月份：" + getMonth(new Date()));
        System.out.println("获取时间所在周几：" + getWeek(new Date()));
    }

}
