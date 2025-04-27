package com.srun.campuslogin.data.model;

import androidx.room.TypeConverter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class DatabaseConverters {

    @TypeConverter
    public static String queueToString(LinkedBlockingQueue<String> queue) {
        if (queue == null || queue.isEmpty()) {
            return "";
        }
        return String.join(",", queue);
    }

    @TypeConverter
    public static LinkedBlockingQueue<String> stringToQueue(String data) {
        LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>(100);
        if (data != null && !data.isEmpty()) {
            List<String> logs = Arrays.asList(data.split(","));
            // 只保留最后 100 条日志
            int startIndex = Math.max(0, logs.size() - 100);
            for (int i = startIndex; i < logs.size(); i++) {
                if (!queue.offer(logs.get(i))) {
                    // 队列已满，停止添加
                    break;
                }
            }
        }
        return queue;
    }
}