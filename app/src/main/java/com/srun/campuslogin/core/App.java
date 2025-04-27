package com.srun.campuslogin.core;

import android.app.Application;
import androidx.room.Room;
import com.srun.campuslogin.data.AppDatabase;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

//===========================应用核心类（单例管理）=============================
/**
 * 功能：提供全局应用上下文，管理Room数据库的单例实例
 * 包含数据库初始化、全局实例访问等核心功能
 */
public class App extends Application {
    //===========================静态成员=============================
    private static App instance;
    private static ExecutorService dbExecutor;
    private static ExecutorService networkExecutor;

    //===========================实例成员=============================
    private AppDatabase database;

    //===========================生命周期方法=============================
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        initThreadPools();
        initDatabase();
    }

    @Override
    public void onTerminate() {
        shutdownThreadPools();
        super.onTerminate();
    }

    //===========================初始化方法=============================
    private void initThreadPools() {
        // 数据库线程池配置（IO密集型）
        int dbCorePoolSize = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        dbExecutor = new ThreadPoolExecutor(
                dbCorePoolSize,
                dbCorePoolSize * 2,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // 网络线程池配置（CPU密集型）
        int networkCorePoolSize = Runtime.getRuntime().availableProcessors();
        networkExecutor = new ThreadPoolExecutor(
                networkCorePoolSize,
                networkCorePoolSize * 2,
                30L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(50),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    private void initDatabase() {
        database = Room.databaseBuilder(this, AppDatabase.class, "campus-login-db")
                .setQueryExecutor(dbExecutor) // 绑定数据库线程池
                .build();
    }

    //===========================实例获取方法=============================
    public static App getInstance() {
        return instance;
    }

    //===========================线程池访问方法=============================
    public static ExecutorService getDbExecutor() {
        return dbExecutor;
    }

    //===========================数据库访问方法=============================
    public AppDatabase getDatabase() {
        return database;
    }

    //===========================资源释放方法=============================
    private void shutdownThreadPools() {
        if (dbExecutor != null) {
            dbExecutor.shutdownNow();
        }
        if (networkExecutor != null) {
            networkExecutor.shutdownNow();
        }
    }
}