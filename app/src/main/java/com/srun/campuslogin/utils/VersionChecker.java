package com.srun.campuslogin.utils;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.srun.campuslogin.core.App;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class VersionChecker {
    private static final String TAG = "VersionChecker";
    private static final String GITEE_API_URL = "https://gitee.com/api/v5/repos/weitool/login-ynufe/releases";

    // 自定义数据结构
    private static class Release {
        String tag_name;
        String body;
        List<Asset> assets = Collections.emptyList(); // 确保非空初始化
    }

    private static class Asset {
        String browser_download_url;
    }

    public static void checkNewVersion(Context context) {
        ExecutorService networkExecutor = App.getDbExecutor();
        networkExecutor.execute(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .build();
                Request request = new Request.Builder().url(GITEE_API_URL).build();

                try (Response response = client.newCall(request).execute()) {
                    // 处理空响应体
                    if (!response.isSuccessful() || response.body() == null) {
                        Log.e(TAG, "API请求失败或响应体为空");
                        return;
                    }

                    String json = response.body().string();
                    Type listType = new TypeToken<List<Release>>(){}.getType();
                    List<Release> releases = new Gson().fromJson(json, listType);

                    // 空集合保护
                    if (releases == null || releases.isEmpty()) return;
                    Release latest = releases.get(0);
                    if (latest.assets == null || latest.assets.isEmpty()) return;

                    // 安全获取版本号
                    PackageInfo pInfo = context.getPackageManager().getPackageInfo(
                            context.getPackageName(), PackageManager.GET_ACTIVITIES);
                    String localVersion = pInfo.versionName != null ? pInfo.versionName : "0.0.0";

                    if (isNewVersion(latest.tag_name, localVersion)) {
                        showUpdateDialog(context, latest);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "版本检查异常: ", e);
            }
        });
    }

    private static boolean isNewVersion(String remote, @NonNull String local) {
        try {
            String cleanRemote = remote.replaceAll("[^\\d.]", "");
            String cleanLocal = local.replaceAll("[^\\d.]", "");

            String[] remoteParts = cleanRemote.split("\\.");
            String[] localParts = cleanLocal.split("\\.");

            for (int i = 0; i < Math.max(remoteParts.length, localParts.length); i++) {
                int r = (i < remoteParts.length) ? parsePart(remoteParts[i]) : 0;
                int l = (i < localParts.length) ? parsePart(localParts[i]) : 0;
                if (r > l) return true;
                if (r < l) return false;
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "版本号解析错误: " + e.getMessage());
        }
        return false;
    }

    private static int parsePart(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0; // 非数字部分视为0
        }
    }

    private static void showUpdateDialog(Context context, Release release) {
        new Handler(Looper.getMainLooper()).post(() -> {
            // 双重空检查
            if (release.assets == null || release.assets.isEmpty()) return;

            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle("新版本 " + release.tag_name)
                    .setMessage(release.body != null ? release.body : "新功能优化")
                    .setPositiveButton("下载", (d, w) ->
                            startDownload(context, release.assets.get(0).browser_download_url)
                    )
                    .setNegativeButton("取消", null)
                    .create();
            dialog.show();
        });
    }

    @SuppressLint({"UnspecifiedRegisterReceiverFlag", "ObsoleteSdkInt"})
    private static void startDownload(Context context, String url) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setTitle("应用更新")
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "campus_login_update.apk");

            DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm == null) {
                Log.e(TAG, "DownloadManager不可用");
                return;
            }

            long downloadId = dm.enqueue(request);

            // 广播接收器定义
            BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent intent) {
                    long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    if (id == downloadId) {
                        Uri uri = dm.getUriForDownloadedFile(id);
                        if (uri != null) installApk(ctx, uri);
                        try {
                            ctx.unregisterReceiver(this);
                        } catch (IllegalArgumentException e) {
                            Log.e(TAG, "接收器注销失败: ", e);
                        }
                    }
                }
            };

            // 动态注册（全版本兼容方案）
            IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                context.registerReceiver(downloadReceiver, filter);
            }
        } catch (Exception e) {
            Log.e(TAG, "下载初始化失败: ", e);
        }
    }

    private static void installApk(Context context, Uri apkUri) {
        try {
            // Android 8.0+安装权限检查
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    !context.getPackageManager().canRequestPackageInstalls()) {
                context.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .setData(Uri.parse("package:" + context.getPackageName())));
                return;
            }

            Intent install = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(apkUri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(install);
        } catch (Exception e) {
            Log.e(TAG, "安装APK失败: ", e);
        }
    }
}