package com.srun.campuslogin.data.model;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库类型转换器：处理 List<String> 与 JSON 字符串的转换
 * 核心功能：
 * - 将日志列表序列化为 JSON 字符串以便存储
 * - 从 JSON 字符串反序列化回日志列表
 */
public class DatabaseConverters {
    private static final Type LIST_STRING_TYPE = new TypeToken<List<String>>() {}.getType();
    @SuppressWarnings("unused")
    @TypeConverter
    public static String listToString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return new Gson().toJson(list, LIST_STRING_TYPE);
    }
    @SuppressWarnings("unused")
    @TypeConverter
    public static List<String> stringToList(String data) {
        if (data == null || data.isEmpty()) {
            return new ArrayList<>();
        }
        return new Gson().fromJson(data, LIST_STRING_TYPE);
    }
}