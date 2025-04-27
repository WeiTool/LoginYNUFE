package com.srun.campuslogin.data.model;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 校园网登录卡片数据实体类
 *
 * <p>
 * 功能说明：
 * 1. 存储用户账号信息及网络状态数据，支持通过 Room 数据库持久化。
 * 2. 实现 Parcelable 接口以支持跨组件（如 Activity/Fragment）传递。
 * 3. 内置操作日志记录功能，自动清理旧日志避免内存溢出。
 * </p>
 *
 * <p>
 * 核心字段说明：
 * - id          : 数据库自增主键，唯一标识卡片
 * - studentId   : 用户可见的学号标识
 * - username    : 运营商认证账号（如电信账号）
 * - operator    : 运营商类型标识（@ctc 表示电信，@ynufe 表示校园网）
 * - password    : 加密存储的登录密码
 * - lastIp      : 最近一次成功登录的 IP 地址
 * - isHeartbeatActive : 心跳检测是否激活
 * - logs        : 操作日志队列（最多保留 100 条）
 * - heartbeat_counter : 心跳检测计数器（数据库持久化字段）
 * </p>
 */
@Entity(tableName = "cards")
public class CardEntity implements Parcelable {
    //========================= 数据库字段定义 =========================
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private int id;

    @ColumnInfo(name = "student_id")
    private String studentId;

    @ColumnInfo(name = "username")
    private String username;

    @ColumnInfo(name = "operator")
    private String operator;

    @ColumnInfo(name = "password")
    private String password;

    @ColumnInfo(name = "last_ip")
    private String lastIp = "";

    @ColumnInfo(name = "is_heartbeat_active")
    private boolean isHeartbeatActive;

    @ColumnInfo(name = "logs")
    @TypeConverters(DatabaseConverters.class)
    private LinkedBlockingQueue<String> logs = new LinkedBlockingQueue<>(100);

    @ColumnInfo(name = "heartbeat_counter")
    private int heartbeatCounterValue;

    //========================= 内存字段（不持久化到数据库）=================
    @Ignore
    private final AtomicInteger heartbeatCounter = new AtomicInteger(0);

    //========================= 构造方法 =========================
    /**
     * Room 框架要求的默认构造方法
     * 初始化时同步数据库字段到内存计数器
     */
    public CardEntity() {
        heartbeatCounter.set(heartbeatCounterValue);
    }

    //========================= Parcelable 实现 ==================
    /**
     * Parcel 反序列化构造方法
     * @param in 包含序列化数据的 Parcel 对象
     */
    protected CardEntity(Parcel in) {
        id = in.readInt();
        studentId = in.readString();
        username = in.readString();
        operator = in.readString();
        password = in.readString();
        lastIp = in.readString();
        isHeartbeatActive = in.readByte() != 0;
        heartbeatCounterValue = in.readInt(); // 读取持久化计数器值

        List<String> tempLogs = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            in.readList(tempLogs, String.class.getClassLoader(), String.class);
        } else {
            //noinspection deprecation
            in.readList(tempLogs, String.class.getClassLoader());
        }
        logs.addAll(tempLogs);


        // 初始化内存计数器
        heartbeatCounter.set(heartbeatCounterValue);
    }

    /**
     * Parcelable 构造器，用于从 Parcel 中创建对象实例
     */
    public static final Creator<CardEntity> CREATOR = new Creator<>() {
        @Override
        public CardEntity createFromParcel(Parcel in) {
            return new CardEntity(in);
        }

        @Override
        public CardEntity[] newArray(int size) {
            return new CardEntity[size];
        }
    };

    //========================= Parcelable 方法 ==================
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * 将对象数据序列化到 Parcel 中
     * @param dest  目标 Parcel 对象
     * @param flags 序列化模式标志位
     */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(studentId);
        dest.writeString(username);
        dest.writeString(operator);
        dest.writeString(password);
        dest.writeString(lastIp);
        dest.writeByte((byte) (isHeartbeatActive ? 1 : 0));
        dest.writeInt(heartbeatCounterValue); // 写入持久化计数器值

        // 转换为 ArrayList 避免 Stream.toList() 的 API 问题
        dest.writeList(new ArrayList<>(logs));
    }

    //========================= Getter/Setter 方法 ==================
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLastIp() {
        return lastIp;
    }

    public void setLastIp(String lastIp) {
        this.lastIp = lastIp;
    }

    public boolean isHeartbeatActive() {
        return isHeartbeatActive;
    }

    public void setHeartbeatActive(boolean heartbeatActive) {
        isHeartbeatActive = heartbeatActive;
    }

    //========================= 心跳计数器管理 ==================
    /**
     * 设置数据库中的心跳计数器值（Room 自动调用）
     */
    public void setHeartbeatCounterValue(int value) {
        this.heartbeatCounterValue = value;
        heartbeatCounter.set(value); // 同步到内存中的 AtomicInteger
    }

    /**
     * 获取数据库中的心跳计数器值（Room 自动调用）
     */
    public int getHeartbeatCounterValue() {
        return heartbeatCounterValue;
    }

    /**
     * 获取内存中的原子计数器（用于线程安全操作）
     */
    public AtomicInteger getHeartbeatCounter() {
        return heartbeatCounter;
    }

    /**
     * 将内存中的计数器值同步到数据库字段
     */
    public void syncHeartbeatCounter() {
        setHeartbeatCounterValue(heartbeatCounter.get());
    }

    //========================= 日志管理方法 ==================
    // 新增日志更新监听器接口
    public interface OnLogsUpdatedListener {
        void onLogsUpdated(LinkedBlockingQueue<String> logs);
    }
    @Ignore
    private OnLogsUpdatedListener logsUpdatedListener;

    // 设置监听器的方法
    public void setOnLogsUpdatedListener(OnLogsUpdatedListener listener) {
        this.logsUpdatedListener = listener;
    }

    // 修改 addLog 方法以触发监听器
    public void addLog(String log) {
        if (logs.remainingCapacity() == 0) {
            logs.poll();
        }
        logs.offer(log);

        // 触发日志更新回调
        if (logsUpdatedListener != null) {
            logsUpdatedListener.onLogsUpdated(logs);
        }
    }

    /**
     * 清空所有操作日志
     */
    public void clearLogs() {
        logs.clear();
    }

    //========================= 其他方法 ==================
    public LinkedBlockingQueue<String> getLogs() {
        return new LinkedBlockingQueue<>(logs);
    }

    public void setLogs(LinkedBlockingQueue<String> logs) {
        this.logs = logs;
    }
}