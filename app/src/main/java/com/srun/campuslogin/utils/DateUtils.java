package com.srun.campuslogin.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 日期时间工具类（线程安全优化版）
 * 功能：提供高性能的日期格式化方法
 */
public class DateUtils {
    // 使用 ThreadLocal 避免多线程竞争
    private static final ThreadLocal<SimpleDateFormat> THREAD_LOCAL_FORMATTER =
            new ThreadLocal<>() {
                @Override
                protected SimpleDateFormat initialValue() {
                    // 动态获取当前Locale
                    return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                }
            };

    /**
     * 获取当前时间的格式化字符串（线程安全）
     * @return 格式：yyyy-MM-dd HH:mm:ss
     */
    public static String getCurrentTime() {
        return THREAD_LOCAL_FORMATTER.get().format(new Date());
    }
}