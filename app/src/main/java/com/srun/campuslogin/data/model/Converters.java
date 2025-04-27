package com.srun.campuslogin.data.model;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;

//===========================数据库类型转换核心类=============================
/**
 * 功能：提供Room数据库类型转换支持，处理List<String>与JSON字符串之间的相互转换
 * 包含两个方向的数据转换方法，确保复杂数据类型能够被Room数据库正确存储和读取
 */
public class Converters {
    //===========================类型定义=============================
    private static final Type LIST_STRING_TYPE = new TypeToken<List<String>>() {}.getType();

    //===========================JSON转列表类型转换器=============================
    /**
     * 将数据库中的JSON字符串转换为List<String>集合
     * @param value JSON格式的字符串，当值为null时直接返回null
     * @return 转换后的字符串集合，可能为null
     */
    @TypeConverter
    public static List<String> fromString(String value) {
        if (value == null) return null;
        return new Gson().fromJson(value, LIST_STRING_TYPE);
    }

    //===========================列表转JSON类型转换器=============================
    /**
     * 将List<String>集合序列化为JSON字符串以便存入数据库
     * @param list 待转换的字符串集合，值为null时直接返回null
     * @return JSON格式的字符串，可能为null
     */
    @TypeConverter
    public static String fromList(List<String> list) {
        if (list == null) return null;
        return new Gson().toJson(list, LIST_STRING_TYPE);
    }
}