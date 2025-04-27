package com.srun.campuslogin.core;

public class LoginBridge {

    //=========================== 自定义回调接口 =============================
    public interface LoginCallback {
        void onSuccess();
        void onFailure(String error);
    }

    //=========================== JNI 初始化 =============================
    static {
        System.loadLibrary("campuslogin");
    }

    //=========================== 原生登录接口（主实现）=============================
    /**
     * JNI 方式登录
     * @param username  校园网账号
     * @param password  校园网密码
     * @param detectIp 是否自动检测IP地址
     * @param callback  登录结果回调
     */
    public static native void nativeLogin(
            String username,
            String password,
            boolean detectIp,
            LoginCallback callback
    );
}