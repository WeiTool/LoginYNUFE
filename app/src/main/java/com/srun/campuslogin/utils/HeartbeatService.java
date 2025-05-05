package com.srun.campuslogin.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import com.srun.campuslogin.R;
import com.srun.campuslogin.ui.CardAdapter;

public class HeartbeatService extends Service {
    private static final String CHANNEL_ID = "HeartbeatChannel";
    private CardAdapter.HeartbeatTask currentTask;

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化通知渠道（Android O+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }
        updateNotification(-1); // 默认显示通用信息
    }

    //=========================== 通知管理 ===========================
    public void updateNotification(int cardId) {
        String contentTitle = cardId == -1 ?
                "校园网断线检测" : "检测卡片 #" + cardId;

        Notification notification = buildBaseNotification()
                .setContentTitle(contentTitle)
                .setContentText("最后检测时间：" + System.currentTimeMillis())
                .build();

        startForeground(1, notification);
    }

    private NotificationCompat.Builder buildBaseNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "断线检测服务",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("网络连接状态监控服务");
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    //=========================== 任务管理 ===========================
    public void setCurrentTask(CardAdapter.HeartbeatTask task) {
        if (this.currentTask != null && this.currentTask.isRunning()) {
            this.currentTask.stop();
        }
        this.currentTask = task;
        if (task != null) {
            updateNotification(task.getCardId());
        }
    }

    @Override
    public void onDestroy() {
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.stop();
        }
        super.onDestroy();
    }

    //=========================== 服务生命周期 ========================
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}