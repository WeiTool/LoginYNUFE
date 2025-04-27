# 校园网登录客户端APP

📱 基于 [zu1k/srun](https://github.com/zu1k/srun) 的校园网多账号管理APP | Android平台 | 智能心跳保活

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Rust](https://img.shields.io/badge/Rust-1.65%2B-orange)](https://www.rust-lang.org)
[![Android API 24+](https://img.shields.io/badge/Android-API24%2B-brightgreen)](https://developer.android.com)

## 🌟 核心功能
- 🪄 **卡片式账号管理**
  - 可视化添加/删除校园网账号卡片
  - 支持安宁校区认证配置
  - 卡片排序管理
- ⚡ **智能连接引擎**
  - 基于Rust核心的跨平台认证模块
  - 自动检测网络连通性
  - 智能重连策略
- 📊 **连接状态可视化**
  - 历史连接记录
  - 网络连通性日志

## 🛠️ 技术架构
```bash
.
├── app/    # Android前端/后端 (Java)
├── srun/   # 定制化认证模块 (基于zu1k/srun)
```

## 📥 安装部署

### Android用户
1. 下载最新APK：[Releases页面](https://github.com/WeiTool/LoginYNUFE/releases)
2. 开启「未知来源」安装权限


## ✅ 使用指南

### 添加账号卡片
1. 点击右下角 ➕ 按钮
2. 输入以下信息：
   ```
   - 学号：XXXXXXXX
   - 密码：********
   - 选择区域
   ```
4. 点击「保存」生成新卡片

### 卡片操作

|图标|功能描述|
|------|-----------------------------|
| ▶️   | 手动连接               |
| ⚙️   | 编辑认证配置                 |
| 📊   | 查看日志                   |
| 🗑️   | 删除账号卡片                |


## 📜 开源协议
本项目基于 [GPL-3.0 License](LICENSE) 发布，核心认证模块继承自：  
[zu1k/srun](https://github.com/zu1k/srun) ([MIT License](https://github.com/zu1k/srun/blob/main/LICENSE))

## 🙌 致谢名单
- [zu1k/srun](https://github.com/zu1k/srun) 提供的优质认证库
- Android社区提供的JNI绑定方案
- Material Design提供的图标资源

> 🐞 问题/bug反馈：475288481@qq.com
