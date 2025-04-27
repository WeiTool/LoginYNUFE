package com.srun.campuslogin.ui.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import com.srun.campuslogin.R;
import com.srun.campuslogin.core.App;
import com.srun.campuslogin.data.model.CardEntity;
import com.srun.campuslogin.viewmodel.CardViewModel;

import java.lang.ref.WeakReference;


//===========================卡片编辑对话框核心类=============================
/**
 * 功能：处理校园网认证卡片的编辑和新增操作
 * 核心职责：
 * 1. 数据输入验证
 * 2. 卡片对象构建
 * 3. 数据库操作调度
 * 4. 数据持久化处理
 */
public class EditCardDialogFragment extends DialogFragment {
    //===========================成员变量=============================
    private CardEntity currentCard;    // 当前操作的卡片对象（编辑模式时存在）
    private final WeakReference<EditCardDialogFragment> fragmentRef = new WeakReference<>(this);

    //===========================对话框生命周期管理=============================
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_edit_card, null);

        //===========================UI组件初始化模块=============================
        RadioGroup rgOperator = view.findViewById(R.id.rg_operator);
        EditText etStudentId = view.findViewById(R.id.et_student_id);
        EditText etPassword = view.findViewById(R.id.et_password);

        // 运营商单选按钮本地化处理
        initOperatorRadioButtons(view);

        //===========================参数解析模块=============================
        parseIntentArguments();

        //===========================数据预填充模块=============================
        if (currentCard != null) {
            // 使用 Getter 方法访问字段
            etStudentId.setText(currentCard.getStudentId());
            etPassword.setText(currentCard.getPassword());

            // 使用 Getter 获取 operator 并设置单选按钮
            String operator = currentCard.getOperator();
            int radioButtonId = operator.equals("@ctc") ? R.id.rb_ctc : R.id.rb_ynufe;
            rgOperator.check(radioButtonId);
        }

        //===========================对话框构建模块=============================
        builder.setView(view)
                .setTitle(currentCard == null ? "添加新卡片" : "编辑卡片")
                .setPositiveButton("确认", (dialog, id) ->
                        handleSaveAction(rgOperator, etStudentId, etPassword))
                .setNegativeButton("取消", null);

        return builder.create();
    }

    //===========================运营商按钮初始化模块=============================
    /**
     * 功能：配置运营商单选按钮显示文本
     */
    private void initOperatorRadioButtons(View view) {
        RadioButton rbCtc = view.findViewById(R.id.rb_ctc);
        RadioButton rbYnufe = view.findViewById(R.id.rb_ynufe);
        rbCtc.setText(R.string.display_ctc);
        rbYnufe.setText(R.string.display_ynufe);
    }

    //===========================意图参数解析模块=============================
    /**
     * 功能：安全解析传入的卡片数据
     */
    private void parseIntentArguments() {
        Bundle args = getArguments();
        if (args != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                currentCard = args.getParcelable("card", CardEntity.class);
            } else {
                //noinspection deprecation
                currentCard = args.getParcelable("card");
            }
        }
    }

    //===========================数据保存处理模块=============================
    /**
     * 功能：处理保存操作核心逻辑
     */
    private void handleSaveAction(RadioGroup rgOperator, EditText etStudentId, EditText etPassword) {
        if (!validateInput(etStudentId, etPassword)) return;

        String operator = resolveOperatorSelection(rgOperator);
        CardEntity card = buildCardEntity(
                etStudentId.getText().toString().trim(),
                operator,
                etPassword.getText().toString().trim()
        );

        executeDatabaseOperation(card);
    }

    //===========================输入验证模块=============================
    /**
     * 功能：验证用户输入有效性
     */
    private boolean validateInput(EditText etStudentId, EditText etPassword) {
        String studentId = etStudentId.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (studentId.isEmpty() || password.isEmpty()) {
            showToast("学号和密码不能为空");
            return false;
        }

        if (!studentId.matches("\\d+")) {
            showToast("学号必须为纯数字");
            return false;
        }

        return true;
    }

    //===========================运营商解析方法=============================
    /**
     * 功能：解析选中的运营商类型
     */
    private String resolveOperatorSelection(RadioGroup rgOperator) {
        int selectedId = rgOperator.getCheckedRadioButtonId();
        return (selectedId == R.id.rb_ctc) ? "@ctc" : "@ynufe";
    }

    //===========================卡片对象构建模块=============================
    /**
     * @param studentId 用户学号（不可为空）
     * @param operator  运营商标识（如 @ctc）
     * @param password  加密后的密码
     * @return 构建完成的 CardEntity 对象
     * @throws IllegalArgumentException 如果参数不合法
     */
    private CardEntity buildCardEntity(String studentId, String operator, String password) {
        // 参数校验
        if (TextUtils.isEmpty(studentId)) {
            throw new IllegalArgumentException("学号不能为空");
        }
        if (TextUtils.isEmpty(operator)) {
            throw new IllegalArgumentException("运营商标识不能为空");
        }
        if (TextUtils.isEmpty(password)) {
            throw new IllegalArgumentException("密码不能为空");
        }

        CardEntity card = new CardEntity();

        // 如果存在当前卡片（例如编辑模式），继承部分字段
        if (currentCard != null) {
            card.setId(currentCard.getId());                 // 保留数据库ID
            card.setLastIp(currentCard.getLastIp());         // 继承最近IP
            card.setHeartbeatActive(currentCard.isHeartbeatActive()); // 保持心跳状态
        }

        // 设置新数据
        card.setStudentId(studentId);
        card.setUsername(studentId);  // 根据需求决定是否独立设置username
        card.setOperator(operator);
        card.setPassword(password);

        return card;
    }

    //===========================数据库操作模块=============================
    /**
     * 功能：执行数据库写入操作
     */
    private void executeDatabaseOperation(CardEntity card) {
        CardViewModel viewModel = new ViewModelProvider(requireActivity()).get(CardViewModel.class);
        EditCardDialogFragment fragment = fragmentRef.get();
        if (fragment == null || fragment.getActivity() == null) return;

        card.syncHeartbeatCounter();
        App.getDbExecutor().execute(() -> {
            try {
                if (currentCard != null) {
                    viewModel.updateCard(card);
                } else {
                    viewModel.insertCard(card);
                }
            } catch (Exception e) {
                // 检查Fragment是否仍附加到Activity
                if (fragment.isAdded()) {
                    fragment.requireActivity().runOnUiThread(() ->
                            Toast.makeText(fragment.requireContext(), "操作失败", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    //===========================工具方法模块=============================
    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}