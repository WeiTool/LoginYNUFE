package com.srun.campuslogin.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.srun.campuslogin.data.model.CardEntity;
import java.util.List;

//=========================== 数据访问接口 =============================
/**
 * 卡片数据访问对象（DAO）
 * 功能：提供对 cards 表的 CRUD 操作接口
 * 特性：
 *   - 使用 LiveData 自动更新 UI
 *   - 支持异步数据库操作
 *   - 提供同步与异步查询方法
 *   - 自动管理数据库事务
 */
@Dao
public interface CardDao {
    //======================= 查询操作 =========================
    /**
     * 获取所有卡片数据（实时监测数据变化）
     * @return LiveData 包装的卡片列表，数据变化时自动通知观察者
     */
    @Query("SELECT * FROM cards ORDER BY id DESC")
    LiveData<List<CardEntity>> getAll();

    //======================= 写入操作 =========================
    /**
     * 插入新卡片数据
     * @param card 要插入的卡片对象，必须包含所有非空字段
     */
    @Insert
    void insert(CardEntity card);

    /**
     * 删除指定卡片数据
     * @param card 要删除的卡片对象（根据主键匹配）
     */
    @Delete
    void delete(CardEntity card);

    /**
     * 更新卡片信息
     * @param card 修改后的卡片对象（根据主键匹配更新字段）
     */
    @Update
    void updateCard(CardEntity card);

    //======================= 批量操作 =========================
    /**
     * 清空卡片表所有数据（危险操作）
     * 注意：将永久删除所有账号记录，谨慎调用
     */
    @Query("DELETE FROM cards")
    void deleteAll();

    //======================= 心跳计数器更新 =========================
    /**
     * 直接更新心跳计数器值（原子操作）
     * @param id    卡片主键
     * @param value 新的计数器值
     */
    @Query("UPDATE cards SET heartbeat_counter = :value WHERE id = :id")
    void updateHeartbeatCounter(int id, int value);
}