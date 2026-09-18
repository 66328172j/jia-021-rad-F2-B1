package com.fc.v2.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.model.auto.TRadAlarmBill;

import java.util.List;

/**
 * 剂量预警单 Service接口
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITRadAlarmBillService {

    /** 按主键查询 */
    TRadAlarmBill selectTRadAlarmBillById(Long id);

    /** 按条件查询列表（分页由调用方统一处理） */
    List<TRadAlarmBill> selectTRadAlarmBillList(Wrapper<TRadAlarmBill> queryWrapper);

    /** 新增 */
    int insertTRadAlarmBill(TRadAlarmBill record);

    /** 修改 */
    int updateTRadAlarmBill(TRadAlarmBill record);

    /** 批量删除 */
    int deleteTRadAlarmBillByIds(String ids);

    /** 按主键删除 */
    int deleteTRadAlarmBillById(Long id);
}
