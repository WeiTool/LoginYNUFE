package com.srun.campuslogin.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.srun.campuslogin.R;
import com.srun.campuslogin.core.App;
import com.srun.campuslogin.core.LoginBridge;
import com.srun.campuslogin.data.AppDatabase;
import com.srun.campuslogin.data.model.CardEntity;
import com.srun.campuslogin.databinding.ItemCardBinding;
import com.srun.campuslogin.ui.fragments.EditCardDialogFragment;
import com.srun.campuslogin.utils.DateUtils;
import com.srun.campuslogin.utils.NetworkUtils;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import android.os.Looper;


//===========================卡片适配器（RecyclerView）核心类=============================

/**
 * 功能：管理校园网认证卡片的列表展示和交互逻辑
 * 包含登录操作、删除功能、卡片编辑、IP显示更新等核心功能
 */
public class CardAdapter extends ListAdapter<CardEntity, CardAdapter.ViewHolder> {
    //===========================成员变量=============================
    private final Map<Integer, Handler> heartbeatHandlers = new HashMap<>();
    private final Set<Integer> disabledPositions = new HashSet<>();
    private final WeakReference<Context> contextRef; // 上下文弱引用，防止内存泄漏
    private static final DiffUtil.ItemCallback<CardEntity> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull CardEntity oldItem, @NonNull CardEntity newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull CardEntity oldItem, @NonNull CardEntity newItem) {
            return Objects.equals(oldItem.getStudentId(), newItem.getStudentId()) &&
                    Objects.equals(oldItem.getOperator(), newItem.getOperator()) &&
                    Objects.equals(oldItem.getLastIp(), newItem.getLastIp()) &&
                    oldItem.isHeartbeatActive() == newItem.isHeartbeatActive();
        }

        @Override
        public Object getChangePayload(@NonNull CardEntity oldItem, @NonNull CardEntity newItem) {
            Bundle payload = new Bundle();
            if (!Objects.equals(oldItem.getStudentId(), newItem.getStudentId())) {
                payload.putString("STUDENT_ID_CHANGED", newItem.getStudentId());
            }
            if (!Objects.equals(oldItem.getOperator(), newItem.getOperator())) {
                payload.putString("OPERATOR_CHANGED", newItem.getOperator());
            }
            if (!oldItem.getLastIp().equals(newItem.getLastIp())) {
                payload.putString("IP_CHANGED", newItem.getLastIp());
            }
            if (oldItem.isHeartbeatActive() != newItem.isHeartbeatActive()) {
                payload.putBoolean("HEARTBEAT_CHANGED", newItem.isHeartbeatActive());
            }
            return payload.isEmpty() ? null : payload;
        }
    };

    //===========================初始化与构造=============================
    public CardAdapter(Context context) {
        super(DIFF_CALLBACK);
        this.contextRef = new WeakReference<>(context);
    }
    public boolean isDestroyed() {
        // 通过检查 Context 是否已被回收来判断适配器状态
        return contextRef.get() == null;
    }

    //===========================视图容器管理=============================
    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemCardBinding binding;

        public ViewHolder(ItemCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            // 登录按钮事件
            binding.btnLogin.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                RecyclerView.Adapter<?> adapter = getBindingAdapter();
                if (position != RecyclerView.NO_POSITION && adapter instanceof CardAdapter) {
                    ((CardAdapter) adapter).handleLoginClick(position);
                }
            });

            // 心跳按钮事件
            binding.btnHeartbeat.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    // 获取当前适配器实例
                    RecyclerView.Adapter<?> adapter = getBindingAdapter();
                    if (adapter instanceof CardAdapter) {
                        CardEntity card = ((CardAdapter) adapter).getItem(position);
                        ((CardAdapter) adapter).toggleHeartbeatState(card, position);
                    }
                }
            });

            // 删除按钮事件
            binding.btnDelete.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                RecyclerView.Adapter<?> adapter = getBindingAdapter();
                if (position != RecyclerView.NO_POSITION && adapter instanceof CardAdapter) {
                    CardEntity card = ((CardAdapter) adapter).getItem(position);
                    ((CardAdapter) adapter).showDeleteConfirmationDialog(card);
                }
            });

            // 日志按钮事件
            binding.btnLogs.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                RecyclerView.Adapter<?> adapter = getBindingAdapter();
                if (position != RecyclerView.NO_POSITION && adapter instanceof CardAdapter) {
                    CardEntity card = ((CardAdapter) adapter).getItem(position);
                    // 正确传递 CardEntity 对象，而非 logs 队列
                    ((CardAdapter) adapter).showLogsDialog(card); // 修复此处参数
                }
            });

            // 卡片点击编辑事件
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                RecyclerView.Adapter<?> adapter = getBindingAdapter();
                if (position != RecyclerView.NO_POSITION && adapter instanceof CardAdapter) {
                    CardEntity card = ((CardAdapter) adapter).getItem(position);
                    ((CardAdapter) adapter).showEditDialog(card, position);
                }
            });
        }

        //========================= 视图绑定辅助方法 =========================

        /**
         * 绑定心跳按钮状态（供Payload局部刷新使用）
         *
         * @param isActive 是否激活心跳检测
         */
        public void bindHeartbeat(boolean isActive) {
            Context context = itemView.getContext();
            int colorRes = isActive ? R.color.red : R.color.blue;
            binding.btnHeartbeat.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))
            );
            binding.btnHeartbeat.setText(isActive ? "停止检测" : "断线重连");
        }

        /**
         * 绑定IP显示（供Payload局部刷新使用）
         *
         * @param ip 要显示的IP地址
         */
        public void bindIp(String ip) {
            Context context = itemView.getContext();
            String ipContent = TextUtils.isEmpty(ip) ?
                    context.getString(R.string.ip_unavailable) : ip;
            binding.tvIp.setText(
                    context.getString(R.string.ip_address, ipContent)
            );
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCardBinding binding = ItemCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    //===========================数据绑定核心方法=============================
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CardEntity card = getItem(position);

        //------------------ 基础数据绑定（始终执行） -----------------
        // 学生ID显示
        holder.binding.tvStudentId.setText(card.getStudentId() != null ? card.getStudentId() : "未知");

        // 运营商显示（本地化处理）
        String operatorDisplayName = getOperatorDisplayName(card.getOperator());
        holder.binding.tvOperator.setText(operatorDisplayName != null ? operatorDisplayName : "未知运营商");

        // IP地址动态显示（自动处理空值）
        updateIpDisplay(holder, card);

        //------------------ UI状态更新（依赖实时数据） -----------------
        // 登录按钮状态（禁用/启用）
        updateLoginButtonState(holder);

        // 心跳按钮状态（颜色/文本）
        holder.binding.btnHeartbeat.setText(
                card.isHeartbeatActive() ? "停止检测" : "断线重连"
        );
        holder.binding.btnHeartbeat.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(
                        holder.itemView.getContext(),
                        card.isHeartbeatActive() ? R.color.red : R.color.blue
                ))
        );
    }

    // 新增Payload局部刷新逻辑
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        } else {
            for (Object payload : payloads) {
                if (payload instanceof Bundle) {
                    Bundle bundle = (Bundle) payload;
                    // 处理 STUDENT_ID_CHANGED
                    if (bundle.containsKey("STUDENT_ID_CHANGED")) {
                        holder.binding.tvStudentId.setText(bundle.getString("STUDENT_ID_CHANGED"));
                    }
                    // 处理 OPERATOR_CHANGED
                    if (bundle.containsKey("OPERATOR_CHANGED")) {
                        String operator = bundle.getString("OPERATOR_CHANGED");
                        holder.binding.tvOperator.setText(getOperatorDisplayName(operator));
                    }
                    // 处理 IP_CHANGED
                    if (bundle.containsKey("IP_CHANGED")) {
                        holder.bindIp(bundle.getString("IP_CHANGED"));
                    }
                    // 处理 HEARTBEAT_CHANGED
                    if (bundle.containsKey("HEARTBEAT_CHANGED")) {
                        boolean isActive = bundle.getBoolean("HEARTBEAT_CHANGED");
                        holder.bindHeartbeat(isActive);
                    }
                }
            }
        }
    }

    //===========================登录功能处理=============================
    private void handleLoginClick(int position) {
        CardEntity card = getItem(position);
        if (!validateCardInfo(card)) return;

        LoginBridge.nativeLogin(
                card.getUsername() + card.getOperator(),
                card.getPassword(),
                true,
                new LoginBridge.LoginCallback() {
                    @Override
                    public void onSuccess() {
                        processLoginSuccess(card, position);
                    }

                    @Override
                    public void onFailure(String error) {
                        processLoginFailure(card, error);
                    }
                }
        );
    }

    //===========================心跳功能处理=============================
    private void toggleHeartbeatState(CardEntity card, int position) {
        boolean newState = !card.isHeartbeatActive();
        card.setHeartbeatActive(newState); // 直接修改当前列表对象状态

        // 强制更新数据库（同步操作）
        App.getDbExecutor().execute(() -> {
            try {
                // 创建新对象避免数据污染
                CardEntity newCard = new CardEntity();
                newCard.setId(card.getId());
                newCard.setHeartbeatActive(newState);
                newCard.setLastIp(card.getLastIp());
                newCard.setStudentId(card.getStudentId());
                newCard.setOperator(card.getOperator());
                newCard.setPassword(card.getPassword());
                newCard.setLogs(card.getLogs());
                newCard.setHeartbeatCounterValue(card.getHeartbeatCounterValue());

                // 直接更新数据库
                AppDatabase.getDatabase(contextRef.get()).cardDao().updateCard(newCard);

                // 主线程更新UI（关键修复：强制触发完整刷新）
                new Handler(Looper.getMainLooper()).post(() -> {
                    notifyItemChanged(position); // 移除手动Payload，强制完整刷新
                });

            } catch (Exception e) {
                Log.e("Heartbeat", "数据库更新失败: " + e.getMessage());
                new Handler(Looper.getMainLooper()).post(() ->
                        showToast("状态更新失败")
                );
            }
        });

        // 日志和心跳控制
        if (newState) {
            card.addLog("开启断线重连 - " + DateUtils.getCurrentTime());
            showToast("开启断线检测");
            startHeartbeatCheck(card);
        } else {
            card.addLog("关闭断线重连 - " + DateUtils.getCurrentTime());
            showToast("关闭断线重连");
            stopHeartbeatCheck(card.getId());
        }
    }

    private void startHeartbeatCheck(CardEntity card) {
        Log.d("Heartbeat", "启动心跳检测，卡片ID：" + card.getId());
        Handler handler = new Handler(Looper.getMainLooper());
        // 清理旧 Handler
        Handler oldHandler = heartbeatHandlers.get(card.getId());
        if (oldHandler != null) {
            oldHandler.removeCallbacksAndMessages(null);
        }
        // 启动新任务
        Runnable task = new HeartbeatTask(this, card, handler);
        handler.post(task);
        heartbeatHandlers.put(card.getId(), handler);
    }

    public void startHeartbeatForActiveCards() {
        for (CardEntity card : getCurrentList()) {
            if (card.isHeartbeatActive()) {
                startHeartbeatCheck(card);
            }
        }
    }

    public void stopAllHeartbeatChecks() {
        for (Integer cardId : heartbeatHandlers.keySet()) {
            stopHeartbeatCheck(cardId);
        }
        heartbeatHandlers.clear();
    }

    // =========================== 新增静态心跳任务类 ===========================
    private static class HeartbeatTask implements Runnable {
        private final WeakReference<CardAdapter> adapterRef;
        private final WeakReference<CardEntity> cardRef;
        private final Handler handler;

        HeartbeatTask(CardAdapter adapter, CardEntity card, Handler handler) {
            this.adapterRef = new WeakReference<>(adapter);
            this.cardRef = new WeakReference<>(card);
            this.handler = handler;
        }

        @Override
        public void run() {
            final CardAdapter adapter = adapterRef.get();
            final CardEntity card = cardRef.get();
            if (adapter == null || card == null) return;

            // 记录检测开始
            card.addLog("🕒 开始周期检测 - " + DateUtils.getCurrentTime());

            // 在后台线程执行网络检测
            new Thread(() -> {
                // 使用 Java 原生网络检测
                NetworkUtils.ReauthResult result = NetworkUtils.isReauthenticationRequired();

                // 主线程更新 UI 和日志
                new Handler(Looper.getMainLooper()).post(() -> {
                    // 动态获取卡片位置
                    int currentPosition = adapter.getCurrentList().indexOf(card);
                    if (currentPosition == RecyclerView.NO_POSITION) {
                        Log.w("Heartbeat", "卡片不存在于当前列表");
                        return;
                    }

                    // 更新检测计数
                    int count = card.getHeartbeatCounter().incrementAndGet();
                    card.syncHeartbeatCounter();
                    card.addLog("🔄 第 " + count + " 次检测 - " + DateUtils.getCurrentTime());

                    // 构建日志消息
                    StringBuilder log = new StringBuilder();
                    if (result.needReauth) {
                        log.append("⚠️ 需要重新认证");
                        if (result.error != null) {
                            log.append(" - 错误原因: ").append(result.error);
                        }
                    } else {
                        log.append("✅ 网络正常，无需自动登录");
                    }
                    log.append(" - ").append(DateUtils.getCurrentTime());
                    card.addLog(log.toString());

                    // 更新数据库和 UI
                    adapter.executeDatabaseUpdate(card);
                    adapter.notifyItemChanged(currentPosition);

                    // 触发重新认证
                    if (result.needReauth) {
                        adapter.autoRelogin(card, currentPosition);
                    }

                    // 调度下次检测（无论成功与否）
                    if (!adapter.isDestroyed()) {
                        handler.postDelayed(this, 60_000);
                        Log.d("Heartbeat", "已调度下次检测，卡片ID：" + card.getId());
                    }
                });
            }).start();
        }
    }

    //===========================辅助方法模块=============================
    private boolean validateCardInfo(CardEntity card) {
        // 检查所有关键字段是否为 null 或空字符串
        if (TextUtils.isEmpty(card.getUsername()) ||
                TextUtils.isEmpty(card.getOperator()) ||
                TextUtils.isEmpty(card.getPassword())) {
            showToast("账号信息不完整");
            return false;
        }

        // 检查学号是否为纯数字（可选）
        if (!card.getUsername().matches("\\d+")) {
            showToast("学号必须为纯数字");
            return false;
        }

        return true;
    }

    private void processLoginSuccess(CardEntity card, int position) {
        NetworkUtils.IpResult ipResult = NetworkUtils.getCurrentIPv4Address();
        card.setLastIp(ipResult.ip != null ? ipResult.ip : "未获取");
        if (ipResult.error != null) {
            card.addLog("⚠️ IP获取失败: " + ipResult.error);
        }
        card.addLog("✅ 登录成功 - " + DateUtils.getCurrentTime());
        showToast("登录成功");

        // 使用新的 Java 原生网络检测
        NetworkUtils.ReauthResult initialCheck = NetworkUtils.isReauthenticationRequired();
        card.addLog(buildNetworkLogMessage(initialCheck));

        executeDatabaseUpdate(card);
        updateUIAfterLogin(position, ipResult.ip);
    }

    private void updateUIAfterLogin(int position, String newIp) {
        Context context = contextRef.get();
        if (context instanceof AppCompatActivity) {
            ((AppCompatActivity) context).runOnUiThread(() -> {
                updateCardIp(position, newIp);
                disabledPositions.add(position);
                notifyItemChanged(position);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    disabledPositions.remove(position);
                    notifyItemChanged(position);
                }, 5000);
            });
        }
    }

    private void processLoginFailure(CardEntity card, String error) {
        card.addLog("登录失败 - " + error);
        executeDatabaseUpdate(card);
        showToast("失败：" + error);
    }

    private void executeDatabaseUpdate(CardEntity card) {
        App.getDbExecutor().execute(() -> {
            try {
                AppDatabase.getDatabase(contextRef.get()).cardDao().updateCard(card);
            } catch (Exception e) {
                showToast("更新失败");
            }
        });
    }

    //===========================UI状态更新模块=============================
    private void updateLoginButtonState(ViewHolder holder) {
        int position = holder.getBindingAdapterPosition();

        // 检查位置有效性
        if (position == RecyclerView.NO_POSITION) return;

        // 检查上下文是否有效
        Context context = contextRef.get();
        if (context == null) {
            Log.w("CardAdapter", "上下文不可用，无法更新登录按钮状态");
            return;
        }

        // 更新按钮状态
        boolean isDisabled = disabledPositions.contains(position);
        holder.binding.btnLogin.setEnabled(!isDisabled);

        // 动态设置按钮背景色
        int bgColor = isDisabled ? R.color.gray : R.color.green;
        try {
            holder.binding.btnLogin.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(context, bgColor))
            );
        } catch (Resources.NotFoundException e) {
            Log.e("CardAdapter", "颜色资源未找到: " + e.getMessage());
        }
    }

    //===========================网络检测模块=============================
    private String buildNetworkLogMessage(NetworkUtils.ReauthResult result) {
        StringBuilder log = new StringBuilder();
        if (result.needReauth) {
            log.append("⚠️ 需要重新认证");
            if (result.error != null) {
                log.append(" - 错误原因: ").append(result.error);
            }
        } else {
            log.append("✅ 网络正常，无需登录");
        }
        log.append(" - ").append(DateUtils.getCurrentTime());
        return log.toString();
    }

    //===========================自动重连模块=============================
    private void autoRelogin(CardEntity card, int position) {
        LoginBridge.nativeLogin(
                card.getUsername() + card.getOperator(),
                card.getPassword(),
                true,
                new LoginBridge.LoginCallback() {
                    @Override
                    public void onSuccess() {
                        updateAfterAutoLogin(card, position);
                    }

                    @Override
                    public void onFailure(String error) {
                        handleAutoLoginFailure(card, error);
                    }
                }
        );

        // 记录日志
        card.addLog("⏳ 发起自动重连 - " + DateUtils.getCurrentTime());
        executeDatabaseUpdate(card);
    }

    private void updateAfterAutoLogin(CardEntity card, int position) {
        NetworkUtils.IpResult ipResult = NetworkUtils.getCurrentIPv4Address();
        card.setLastIp((ipResult.ip == null || ipResult.ip.isEmpty()) ? "未获取" : ipResult.ip);
        card.addLog("✅ 自动登录成功 - " + DateUtils.getCurrentTime()); // 添加成功标记
        executeDatabaseUpdate(card);
        updateUIAfterAutoLogin(position, card.getLastIp());
    }

    private void updateUIAfterAutoLogin(int position, String ip) {
        Context context = contextRef.get();
        if (context instanceof AppCompatActivity) {
            ((AppCompatActivity) context).runOnUiThread(() -> {
                updateCardIp(position, ip);
                notifyItemChanged(position);
                showToast("自动登录成功");
            });
        }
    }

    private void handleAutoLoginFailure(CardEntity card, String error) {
        card.addLog("❌ 自动登录失败: " + error);
        executeDatabaseUpdate(card);
        showToast("自动登录失败：" + error);
    }

    //===========================其他核心方法=============================
    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        int position = holder.getBindingAdapterPosition();

        if (position != RecyclerView.NO_POSITION) {
            stopHeartbeatCheck(position);

        } else {
            Log.w("CardAdapter", "回收视图时获取到无效位置");
        }
    }

    private void stopHeartbeatCheck(int cardId) {
        Handler handler = heartbeatHandlers.get(cardId);
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            heartbeatHandlers.remove(cardId);
        }
    }

    private void showToast(String msg) {
        Context context = contextRef.get();
        if (context instanceof AppCompatActivity) {
            ((AppCompatActivity) context).runOnUiThread(() ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            );
        }
    }

    //===========================对话框模块=============================
    private void showDeleteConfirmationDialog(CardEntity card) {
        Context context = contextRef.get();
        if (context instanceof AppCompatActivity) {
            new AlertDialog.Builder(context)
                    .setTitle("删除账号")
                    .setMessage("确定要删除此账号信息吗？")
                    .setPositiveButton("确定", (dialog, which) -> deleteCard(card))
                    .setNegativeButton("取消", null)
                    .show();
        }
    }

    private void deleteCard(CardEntity card) {
        card.clearLogs();
        Context context = contextRef.get();
        if (context instanceof HistoryActivity) {
            ((HistoryActivity) context).getViewModel().deleteCard(card);
        }
    }

    private void showLogsDialog(CardEntity card) {
        Context context = contextRef.get();
        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_logs, null);

            TextView tvLogs = dialogView.findViewById(R.id.tv_logs);
            ScrollView scrollView = dialogView.findViewById(R.id.scrollView);
            Button btnClear = dialogView.findViewById(R.id.btn_clear_logs);

            // 初始化日志内容
            updateLogsText(tvLogs, card.getLogs());

            // 设置日志更新监听器
            card.setOnLogsUpdatedListener(newLogs ->
                    activity.runOnUiThread(() -> {
                        updateLogsText(tvLogs, newLogs);
                        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
                    })
            );

            // 清空日志按钮事件
            btnClear.setOnClickListener(v -> clearCardLogs(card, tvLogs));

            // 对话框关闭时移除监听器
            builder.setView(dialogView)
                    .setTitle("网络状态日志")
                    .setNegativeButton("关闭", null)
                    .show();
        }
    }

    private void clearCardLogs(CardEntity card, TextView logView) {
        App.getDbExecutor().execute(() -> {
            card.clearLogs();
            // 主线程更新UI
            ((AppCompatActivity) contextRef.get()).runOnUiThread(() ->
                    logView.setText("")
            );
            executeDatabaseUpdate(card);
        });
    }

    private void showEditDialog(CardEntity card, int position) {
        Bundle args = new Bundle();
        args.putParcelable("card", card);
        args.putInt("position", position);
        EditCardDialogFragment dialog = new EditCardDialogFragment();
        dialog.setArguments(args);

        Context context = contextRef.get();
        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            if (activity.getSupportFragmentManager().findFragmentByTag("dialog") == null) {
                dialog.show(activity.getSupportFragmentManager(), "dialog");
            }
        }
    }

    //===========================IP显示模块=============================
    private void updateCardIp(int position, String newIp) {
        CardEntity card = getCurrentList().get(position);
        card.setLastIp(TextUtils.isEmpty(newIp) ? "未获取" : newIp); // 使用 setLastIp()
        notifyItemChanged(position);
    }

    private void updateIpDisplay(ViewHolder holder, CardEntity card) {
        // 从 WeakReference 中获取 Context
        Context context = contextRef.get();
        if (context == null) {
            // 上下文不可用时提前返回
            return;
        }

        // 动态获取 IP 显示内容
        String ipContent = TextUtils.isEmpty(card.getLastIp()) ?
                context.getString(R.string.ip_unavailable) :
                card.getLastIp();

        // 格式化显示文本（例如 "IP: 192.168.1.1"）
        holder.binding.tvIp.setText(
                context.getString(R.string.ip_address, ipContent)
        );
    }

    //===========================运营商显示处理=============================
    private String getOperatorDisplayName(String operator) {
        Context context = contextRef.get();
        if (context == null) return operator;

        switch (operator) {
            case "@ctc":
                return context.getString(R.string.display_ctc);
            case "@ynufe":
                return context.getString(R.string.display_ynufe);
            default:
                return operator;
        }
    }

    // 辅助方法：更新日志文本
    private void updateLogsText(TextView tvLogs, LinkedBlockingQueue<String> logs) {
        tvLogs.setText(TextUtils.join("\n", new ArrayList<>(logs)));
    }
}