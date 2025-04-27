package com.srun.campuslogin.ui;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.srun.campuslogin.R;
import com.srun.campuslogin.databinding.ActivityHistoryBinding;
import com.srun.campuslogin.ui.fragments.EditCardDialogFragment;
import com.srun.campuslogin.viewmodel.CardViewModel;
import java.util.Collections;

//===========================历史记录活动（主界面）核心类=============================
/**
 * 功能：校园网账号管理主界面
 * 核心职责：
 * 1. 展示所有已保存的校园网账号卡片
 * 2. 提供添加/编辑/删除账号功能
 * 3. 管理本地数据库与UI的同步
 * 4. 处理全局缓存清理操作
 */
public class HistoryActivity extends AppCompatActivity {
    //===========================成员变量=============================
    private ActivityHistoryBinding binding;    // 视图绑定实例
    private CardViewModel viewModel;          // 数据管理ViewModel
    private CardAdapter adapter;              // RecyclerView适配器

    //===========================生命周期方法=============================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //===========================视图初始化模块=============================
        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //===========================核心组件初始化模块=============================
        // 先初始化 RecyclerView 和 Adapter
        initRecyclerView();

        // 再初始化 ViewModel（依赖 Adapter）
        initViewModel();

        // 最后设置按钮监听
        setupButtonListeners();
    }

    //===========================ViewModel初始化模块=============================
    /**
     * 功能：初始化数据管理层并建立观察关系
     * 包含：
     * - 卡片列表数据观察
     * - 列表项更新位置观察
     * - Toast消息观察
     */
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(CardViewModel.class);

        // 观察卡片列表变化
        // 使用 Lambda 表达式替代方法引用，添加空检查
        viewModel.getAllCards().observe(this, cards -> {
            if (adapter != null) {
                adapter.submitList(cards);
            }
        });

        // 观察Toast消息
        viewModel.getToastMessage().observe(this, resId -> {
            if (resId != null) {
                Toast.makeText(this, getString(resId), Toast.LENGTH_SHORT).show();
            }
        });
    }

    //===========================列表视图初始化模块=============================
    /**
     * 功能：配置RecyclerView及其适配器
     * 包含：
     * - 布局管理器设置
     * - 适配器初始化
     */
    private void initRecyclerView() {
        adapter = new CardAdapter(this);
        binding.rvHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvHistory.setAdapter(adapter);

        //添加默认数据或占位符（如果列表可能为空）
        adapter.submitList(Collections.emptyList());
    }

    //===========================事件绑定模块=============================
    /**
     * 功能：集中管理所有按钮点击事件
     * 包含：
     * - 添加卡片按钮
     * - 清除缓存按钮
     */
    private void setupButtonListeners() {
        // 添加新卡片按钮
        binding.fabAdd.setOnClickListener(v ->
                new EditCardDialogFragment().show(getSupportFragmentManager(), "dialog")
        );

        // 清除全局缓存按钮
        binding.btnClearCache.setOnClickListener(v ->
                showClearCacheConfirmationDialog()
        );

        // 联系按钮
        binding.fabContact.setOnClickListener(v -> showContactDialog());
    }

    //===========================缓存管理模块=============================
    /**
     * 功能：显示二次确认弹窗防止误操作
     * 流程：
     * 1. 弹出警示对话框
     * 2. 确认后执行数据库清空操作
     * 3. 显示操作反馈Toast
     */
    private void showClearCacheConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("清除缓存")
                .setMessage("确定要删除所有账号数据吗？此操作不可恢复！")
                .setPositiveButton("确定", (dialog, which) -> {
                    viewModel.clearAllCards();
                    Toast.makeText(this, "缓存已清除", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    //===========================对外接口模块=============================
    /**
     * 功能：提供ViewModel实例供其他组件调用
     * @return CardViewModel 数据管理实例
     */
    public CardViewModel getViewModel() {
        return viewModel;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adapter != null) {
            adapter.stopAllHeartbeatChecks();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.startHeartbeatForActiveCards();
        }
    }
    // =========================新增显示联系方式的方法=============
    private void showContactDialog() {
        new AlertDialog.Builder(this)
                .setTitle("联系方式")
                .setMessage(getString(R.string.contact_info))
                .setPositiveButton("关闭", null)
                .show();
    }
}