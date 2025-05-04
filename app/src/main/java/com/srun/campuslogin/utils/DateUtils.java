package com.srun.campuslogin.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/**
 * 日期时间工具类（线程安全优化版）
 * 功能：提供高性能的日期格式化方法
 */
public class DateUtils {
    private static final ThreadLocal<SimpleDateFormat> THREAD_LOCAL_FORMATTER =
            ThreadLocal.withInitial(
                    () -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            );

    /**
     * 获取当前时间的格式化字符串（线程安全）
     * @return 格式：yyyy-MM-dd HH:mm:ss
     */
    public static String getCurrentTime() {
        SimpleDateFormat formatter = Objects.requireNonNull(
                THREAD_LOCAL_FORMATTER.get(),
                "SimpleDateFormat 实例未正确初始化"
        );
        return formatter.format(new Date());
    }
}