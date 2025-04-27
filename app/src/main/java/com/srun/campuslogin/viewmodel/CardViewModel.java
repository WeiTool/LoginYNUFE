package com.srun.campuslogin.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.srun.campuslogin.R;
import com.srun.campuslogin.core.App;
import com.srun.campuslogin.data.dao.CardDao;
import com.srun.campuslogin.data.model.CardEntity;
import java.util.List;

//===========================校园网账号卡片视图模型核心类=============================
/**
 * 功能：管理校园网账号卡片的数据访问和业务逻辑
 * 核心作用：作为UI层和数据层之间的桥梁，实现数据持久化操作的封装
 * 设计要点：
 * 1. 遵循Android架构组件规范
 * 2. 自动管理生命周期
 * 3. 异步线程处理数据库操作
 * 4. 使用LiveData实现数据观察
 */
public class CardViewModel extends AndroidViewModel {
    //===========================成员变量=============================
    private final CardDao cardDao; // 数据库访问对象
    private final MutableLiveData<Integer> toastMessage = new MutableLiveData<>(); // Toast消息通知

    //===========================初始化方法=============================
    public CardViewModel(Application application) {
        super(application);
        this.cardDao = App.getInstance().getDatabase().cardDao(); // 从应用实例获取数据库
    }

    //===========================数据操作接口=============================
    //-------------------------查询操作-------------------------
    /**
     * 获取所有卡片数据（实时更新）
     * @return LiveData包装的卡片列表，数据变化时自动通知观察者
     */
    public LiveData<List<CardEntity>> getAllCards() {
        return cardDao.getAll(); // 直接返回Room管理的LiveData
    }

    //-------------------------新增操作-------------------------
    /**
     * 异步插入新卡片
     * @param card 要插入的卡片实体
     * 执行流程：
     * 1. 在IO线程执行数据库写入
     * 2. 操作成功发送成功Toast消息
     * 3. 操作失败发送错误Toast消息
     */
    public void insertCard(CardEntity card) {
        App.getDbExecutor().execute(() -> {
            try {
                cardDao.insert(card);
                toastMessage.postValue(R.string.save_success);
            } catch (Exception e) {
                Log.e("CardViewModel", "插入失败", e);
                toastMessage.postValue(R.string.save_failure);
            }
        });
    }

    //-------------------------更新操作-------------------------
    /**
     * 异步更新卡片数据
     * @param card 要更新的卡片实体
     * 设计规范：
     * - 通过主键匹配更新记录
     * - 自动触发LiveData更新
     */
    public void updateCard(CardEntity card) {
        App.getDbExecutor().execute(() -> {
            try {
                cardDao.updateCard(card);
                toastMessage.postValue(R.string.update_success);
            } catch (Exception e) {
                toastMessage.postValue(R.string.update_failure);
            }
        });
    }

    //-------------------------删除操作-------------------------
    /**
     * 异步删除指定卡片
     * @param card 要删除的卡片实体（根据主键匹配）
     */
    public void deleteCard(CardEntity card) {
        App.getDbExecutor().execute(() -> {
            try {
                cardDao.delete(card);
                toastMessage.postValue(R.string.delete_success);
            } catch (Exception e) {
                toastMessage.postValue(R.string.delete_failure);
            }
        });
    }

    /**
     * 清空所有卡片数据（高危操作）
     * 安全机制：
     * - 需在前端进行二次确认
     * - 在IO线程执行批量删除
     */
    public void clearAllCards() {
        App.getDbExecutor().execute(() ->{
            try {
                cardDao.deleteAll();
                toastMessage.postValue(R.string.clear_success);
            } catch (Exception e) {
                toastMessage.postValue(R.string.clear_failure);
            }
        });
    }

    //===========================事件通知接口=============================
    /**
     * 获取Toast消息观察对象
     * @return 包含资源ID的LiveData对象
     */
    public LiveData<Integer> getToastMessage() {
        return toastMessage;
    }
}