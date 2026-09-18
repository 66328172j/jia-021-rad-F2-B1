package com.fc.v2.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.model.auto.TRadAlarmBill;

import java.time.LocalDateTime;
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

    /**
     * 取一个时间窗内「计入剂量统计」的有效预警单：未删除(del_flag=0) 且
     * 已处理(1)/已办结(2)，create_time 落在 [start, end)。
     * 这是月度汇总、明细钻取、历史重算共用的唯一取数口径，任何入口不得另写一份。
     */
    List<TRadAlarmBill> selectEffectiveForDose(LocalDateTime start, LocalDateTime end);

    /** 新增 */
    int insertTRadAlarmBill(TRadAlarmBill record);

    /** 修改 */
    int updateTRadAlarmBill(TRadAlarmBill record);

    /** 批量删除 */
    int deleteTRadAlarmBillByIds(String ids);

    /** 按主键删除 */
    int deleteTRadAlarmBillById(Long id);
}
