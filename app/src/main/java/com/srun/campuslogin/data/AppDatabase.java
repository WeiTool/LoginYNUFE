package com.srun.campuslogin.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import com.srun.campuslogin.data.dao.CardDao;
import com.srun.campuslogin.data.model.AtomicIntegerConverter;
import com.srun.campuslogin.data.model.CardEntity;
import com.srun.campuslogin.data.model.Converters;
import com.srun.campuslogin.data.model.DatabaseConverters;

//=========================== 数据库核心类 =============================
/**
 * Room数据库抽象类，管理应用数据持久化
 * 核心功能：
 * 1. 定义数据库版本和实体类映射
 * 2. 提供数据访问对象(DAO)接口
 * 3. 实现线程安全的单例访问模式

 * 安全机制：
 * 1. 关闭schema导出防止敏感信息泄露（生产环境建议开启并妥善保管）
 * 2. 双重校验锁保证单例线程安全
 * 3. 数据库操作强制在子线程执行
 */
@Database(
        entities = {CardEntity.class}, // 实体类列表
        version = 2,                   // 数据库版本号
        exportSchema = false           // 关闭数据库结构导出
)
// 类型转换器注册
@TypeConverters({Converters.class, DatabaseConverters.class, AtomicIntegerConverter.class})
public abstract class AppDatabase extends RoomDatabase {

    //=========================== DAO接口声明 =============================
    /**
     * 功能：提供卡片数据的CRUD操作接口
     */
    public abstract CardDao cardDao();

    //=========================== 单例模式实现 =============================
    private static volatile AppDatabase INSTANCE; // 保证可见性的单例实例

    /**
     * 获取数据库实例（双重校验锁模式）
     * @param context 应用上下文，用于构建数据库
     * @return 全局唯一的数据库实例
     */
    public static AppDatabase getDatabase(final Context context) {
        // 第一次检查避免不必要的同步
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                // 第二次检查防止重复创建
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(), // 使用应用级上下文
                            AppDatabase.class,               // 数据库类
                            "campus-login-db"               // 数据库名称
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}