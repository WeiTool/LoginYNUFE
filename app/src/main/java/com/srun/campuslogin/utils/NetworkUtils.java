package com.srun.campuslogin.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Enumeration;

public class NetworkUtils {

    //=========================== 网络检测结果封装类 =============================
    public static class ReauthResult {
        public final boolean needReauth;
        public final String error;

        public ReauthResult(boolean needReauth, String error) {
            this.needReauth = needReauth;
            this.error = error;
        }
    }

    //=========================== 新的网络检测逻辑 =============================
    /**
     * 检测是否需要重新认证（使用 HttpURLConnection）
     */
    public static ReauthResult isReauthenticationRequired() {
        HttpURLConnection connection = null;
        try {
            HttpURLConnection.setFollowRedirects(false); // 全局禁用自动重定向
            URL url = new URL("http://connectivitycheck.gstatic.com/generate_204");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setInstanceFollowRedirects(false); // 双重确保

            int responseCode = connection.getResponseCode();
            String location = connection.getHeaderField("Location");

            // 逻辑1：直接检查 302 或其他非 204 状态码
            if (responseCode != 204) {
                return new ReauthResult(true, "响应码异常: " + responseCode);
            }

            // 逻辑2：检查重定向头（即使状态码是 204）
            if (location != null && location.contains("login")) {
                return new ReauthResult(true, "检测到登录重定向: " + location);
            }

            // 逻辑3：检查响应内容
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())
            )) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
                boolean needReauth = content.toString().matches(".*(用户登录|上网认证平台|portal|login).*");
                return new ReauthResult(needReauth, null);
            }

        } catch (IOException e) {
            return new ReauthResult(true, "网络检测异常: " + e.getMessage());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    //=========================== IP地址获取模块（保持不变）=============================
    public static class IpResult {
        public final String ip;
        public final String error;

        public IpResult(String ip, String error) {
            this.ip = ip;
            this.error = error;
        }
    }

    public static IpResult getCurrentIPv4Address() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address) {
                        return new IpResult(addr.getHostAddress(), null);
                    }
                }
            }
            return new IpResult("", "未找到有效IPv4地址");
        } catch (Exception e) {
            return new IpResult("", "获取IPv4地址失败: " + e.getMessage());
        }
    }
}