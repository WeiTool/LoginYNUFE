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
import com.srun.campuslogin.core.App;
import java.util.ArrayList;
import java.util.List;
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
 * - logs        : 操作日志列表（最多保留 100 条）
 * - heartbeatCounter : 心跳检测计数器（原子操作）
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
    @TypeConverters(DatabaseConverters.class) // 使用 DatabaseConverters 处理 List<String>
    private List<String> logs = new ArrayList<>();

    @ColumnInfo(name = "heartbeat_counter")
    @TypeConverters(AtomicIntegerConverter.class) // 使用 AtomicIntegerConverter
    private AtomicInteger heartbeatCounter = new AtomicInteger(0);

    //========================= 构造方法 =========================
    /**
     * Room 框架要求的默认构造方法
     */
    public CardEntity() {}

    //========================= Parcelable 实现 ==================
    protected CardEntity(Parcel in) {
        id = in.readInt();
        studentId = in.readString();
        username = in.readString();
        operator = in.readString();
        password = in.readString();
        lastIp = in.readString();
        isHeartbeatActive = in.readByte() != 0;
        heartbeatCounter.set(in.readInt());

        List<String> tempLogs = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            in.readList(tempLogs, String.class.getClassLoader(), String.class);
        }
        logs.addAll(tempLogs);
    }

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(studentId);
        dest.writeString(username);
        dest.writeString(operator);
        dest.writeString(password);
        dest.writeString(lastIp);
        dest.writeByte((byte) (isHeartbeatActive ? 1 : 0));
        dest.writeInt(heartbeatCounter.get());
        dest.writeList(logs);
    }

    //========================= Getter/Setter 方法 ==================
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getLastIp() { return lastIp; }
    public void setLastIp(String lastIp) { this.lastIp = lastIp; }

    public boolean isHeartbeatActive() { return isHeartbeatActive; }
    public void setHeartbeatActive(boolean heartbeatActive) { isHeartbeatActive = heartbeatActive; }

    public List<String> getLogs() { return new ArrayList<>(logs); }
    public void setLogs(List<String> logs) { this.logs = new ArrayList<>(logs); }

    public AtomicInteger getHeartbeatCounter() { return heartbeatCounter; }
    @SuppressWarnings("unused")
    public void setHeartbeatCounter(AtomicInteger value) { this.heartbeatCounter = value; }

    //========================= 日志管理方法 ==================
    public interface OnLogsUpdatedListener {
        void onLogsUpdated(List<String> logs);
    }

    @Ignore
    private OnLogsUpdatedListener logsUpdatedListener;

    public void setOnLogsUpdatedListener(OnLogsUpdatedListener listener) {
        this.logsUpdatedListener = listener;
    }

    public void addLog(String log) {
        if (logs.size() >= 100) {
            logs.remove(0); // 移除最旧的日志
        }
        logs.add(log);
        if (logsUpdatedListener != null) {
            logsUpdatedListener.onLogsUpdated(new ArrayList<>(logs));
        }
    }

    public void clearLogs() {
        logs.clear();
    }

    //========================= 心跳计数器同步 ==================
    public void syncHeartbeatCounter() {
        App.getDbExecutor().execute(() -> {
            // 直接调用 updateHeartbeatCounter，而非 updateCard
            int value = heartbeatCounter.get();
            App.getInstance().getDatabase().cardDao().updateHeartbeatCounter(id, value);
        });
    }
}