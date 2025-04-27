package com.srun.campuslogin.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

//===========================日期时间工具类=============================
/**
 * 功能：提供日期时间相关的工具方法
 * 包含时间格式化、当前时间获取等核心功能
 */
public class DateUtils {
    //===========================核心功能方法=============================
    /**
     * 功能：获取当前时间的格式化字符串
     * 格式：yyyy-MM-dd HH:mm:ss
     * 注意：使用同步锁保证线程安全，并动态获取当前Locale
     * @return 格式化后的当前时间字符串
     */
    public static synchronized String getCurrentTime() {
        // 每次调用时动态获取当前Locale
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
}