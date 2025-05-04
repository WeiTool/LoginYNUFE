package com.srun.campuslogin.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

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

//===========================应用版本检测工具类=============================
/**
 * 功能：实现应用版本检测与更新引导功能
 * 核心职责：
 * 1. 从Gitee API获取最新版本信息
 * 2. 对比本地版本号识别可用更新
 * 3. 通过系统浏览器引导用户下载新版本
 * 4. 处理版本号解析与比较逻辑
 */
public class VersionChecker {
    //===========================常量定义=============================
    private static final String TAG = "VersionChecker";
    private static final String GITEE_API_URL = "https://gitee.com/api/v5/repos/weitool/login-ynufe/releases";
    private static final String TARGET_APK_NAME = "LoginYNUFE.apk";

    //===========================嵌套数据结构=============================
    /**
     * Gitee发行版数据结构
     */
    private static class Release {
        String tag_name;    // 版本标签（如V1.1.0）
        String body;        // 版本描述
        List<Asset> assets = Collections.emptyList(); // 资源文件列表
    }

    /**
     * 资源文件元数据
     */
    private static class Asset {
        String browser_download_url; // APK下载地址
    }

    //===========================公开接口模块=============================
    /**
     * 功能：启动版本检查流程
     * @param context 上下文对象（需为Activity上下文）
     */
    public static void checkNewVersion(Context context) {
        ExecutorService networkExecutor = App.getNetworkExecutor();
        networkExecutor.execute(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .build();
                Request request = new Request.Builder().url(GITEE_API_URL).build();

                try (Response response = client.newCall(request).execute()) {
                    processApiResponse(context, response);
                }
            } catch (Exception e) {
                Log.e(TAG, "版本检查异常: ", e);
            }
        });
    }

    //===========================数据处理模块=============================
    /**
     * 功能：处理API响应数据
     * @param context 上下文对象
     * @param response 网络响应对象
     */
    private static void processApiResponse(Context context, Response response) throws Exception {
        if (!response.isSuccessful() || response.body() == null) {
            Log.e(TAG, "API请求失败或响应体为空");
            return;
        }

        String json = response.body().string();
        Log.d(TAG, "原始响应数据：" + json);
        Type listType = new TypeToken<List<Release>>(){}.getType();
        List<Release> releases = new Gson().fromJson(json, listType);

        if (releases == null || releases.isEmpty()) return;
        Collections.reverse(releases);
        findValidRelease(context, releases);
    }

    /**
     * 功能：遍历处理所有发行版数据
     * @param context 上下文对象
     * @param releases 发行版列表
     */
    private static void findValidRelease(Context context, List<Release> releases) throws PackageManager.NameNotFoundException {
        for (Release release : releases) {
            if (release.assets == null || release.assets.isEmpty()) continue;

            String downloadUrl = findTargetApkUrl(release.assets);
            if (downloadUrl != null) {
                PackageInfo pInfo = context.getPackageManager().getPackageInfo(
                        context.getPackageName(), PackageManager.GET_ACTIVITIES);
                String localVersion = pInfo.versionName != null ? pInfo.versionName : "0.0.0";

                if (isNewVersion(release.tag_name, localVersion)) {
                    showUpdateDialog(context, release, downloadUrl);
                }
                break; // 找到最新有效版本后终止循环
            }
        }
    }

    //===========================核心算法模块=============================
    /**
     * 功能：在资源列表中查找目标APK下载地址
     * @param assets 资源列表
     * @return 匹配的APK下载地址，未找到返回null
     */
    private static String findTargetApkUrl(List<Asset> assets) {
        for (Asset asset : assets) {
            if (asset.browser_download_url != null &&
                    asset.browser_download_url.endsWith(TARGET_APK_NAME)) {
                return asset.browser_download_url;
            }
        }
        return null;
    }

    /**
     * 功能：版本号比较逻辑
     * @param remote 远程版本号（如V1.1.0）
     * @param local 本地版本号（如1.0.0）
     * @return 远程版本是否较新
     */
    private static boolean isNewVersion(String remote, @NonNull String local) {
        try {
            String cleanRemote = remote.toLowerCase().replaceAll("[^\\d.]", "");
            String cleanLocal = local.toLowerCase().replaceAll("[^\\d.]", "");

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

    /**
     * 功能：解析版本号分段
     * @param part 版本号分段字符串
     * @return 转换为整数的分段值，转换失败返回0
     */
    private static int parsePart(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    //===========================用户交互模块=============================
    /**
     * 功能：显示版本更新对话框
     * @param context 上下文对象
     * @param release 发行版数据
     * @param downloadUrl 下载地址
     */
    private static void showUpdateDialog(Context context, Release release, String downloadUrl) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (!(context instanceof Activity) || ((Activity) context).isFinishing()) {
                Log.w(TAG, "无效的上下文环境");
                return;
            }

            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle("新版本 " + release.tag_name)
                    .setMessage(release.body != null ? release.body : "新功能优化")
                    .setPositiveButton("下载", (d, w) -> openBrowserForDownload(context, downloadUrl))
                    .setNegativeButton("取消", null)
                    .create();
            dialog.show();
        });
    }
//===========================浏览器操作模块=============================
    /**
     * 功能：通过浏览器选择器打开下载链接
     * @param context 上下文对象
     * @param downloadUrl APK下载地址
     */
    private static void openBrowserForDownload(Context context, String downloadUrl) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(downloadUrl));

            // 创建选择器标题
            String chooserTitle = "选择浏览器";
            Intent chooserIntent = Intent.createChooser(intent, chooserTitle);

            // 验证是否有应用可处理该Intent
            if (chooserIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(chooserIntent);
            } else {
                Toast.makeText(context, "未找到可用的浏览器应用", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "浏览器打开失败: ", e);
            Toast.makeText(context, "无法打开下载链接", Toast.LENGTH_LONG).show();
        }
    }
}