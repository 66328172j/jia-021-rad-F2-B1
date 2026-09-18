package com.fc.v2.rad;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TRadAlarmBillMapper;
import com.fc.v2.mapper.auto.TRadDoseSummaryMapper;
import com.fc.v2.model.auto.TRadAlarmBill;
import com.fc.v2.model.auto.TRadDoseSummary;
import com.fc.v2.service.ITRadDoseSummaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * rad F2 场所剂量按月汇总（multi-dim-summary 形状）→ 功能点验收测试。
 * create 侧维护、质检时使用，**不得回填执行模型**。
 */
@SpringBootTest
public class RadF2AcceptanceTest {

    private static final String PFX = "SUM-";

    @javax.annotation.Resource
    private ITRadDoseSummaryService radDoseSummaryService;

    @javax.annotation.Resource
    private TRadAlarmBillMapper radAlarmBillMapper;

    @javax.annotation.Resource
    private TRadDoseSummaryMapper radDoseSummaryMapper;

    @BeforeEach
    public void setUp() {
        radAlarmBillMapper.delete(new QueryWrapper<TRadAlarmBill>().likeRight("bill_no", PFX));
        radDoseSummaryMapper.delete(new QueryWrapper<TRadDoseSummary>().likeRight("period", "2026-"));
    }

    private static java.util.Date at(String s) {
        return java.sql.Timestamp.valueOf(s);
    }

    private TRadAlarmBill mk(String no, int siteId, String qty, int status, String createdAt, int del) {
        TRadAlarmBill b = new TRadAlarmBill();
        b.setBillNo(no);
        b.setSiteId(Integer.valueOf(siteId));
        b.setQty(qty == null ? null : new java.math.BigDecimal(qty));
        b.setStatus(Integer.valueOf(status));
        b.setDelFlag(Integer.valueOf(del));
        b.setCreateTime(at(createdAt));
        return b;
    }

    /** 坑1 统计期间与口径：其它月份、已删除、未处理的都不许计入 */
    @Test
    public void trap1_period_and_scope_should_be_filtered() {
        radAlarmBillMapper.insert(mk(PFX + "P1", 1, "10", 1, "2026-09-10 08:00:00", 0));
        radAlarmBillMapper.insert(mk(PFX + "P2", 1, "100", 1, "2026-08-10 08:00:00", 0));
        radAlarmBillMapper.insert(mk(PFX + "P3", 1, "1000", 1, "2026-09-11 08:00:00", 1));
        radAlarmBillMapper.insert(mk(PFX + "P4", 1, "1000", 0, "2026-09-12 08:00:00", 0));
        radDoseSummaryService.rebuild("2026-09");
        TRadDoseSummary s = radDoseSummaryService.pick("2026-09", Integer.valueOf(1));
        assertNotNull(s, "该月该场所应有汇总");
        assertEquals(0, s.getTotalQty().compareTo(new java.math.BigDecimal("10")),
                "只应计入本月、未删除、已处理的记录");
        assertEquals(1, s.getRowCount().intValue(), "参与汇总的行数应为 1");
    }

    /** 坑2 月份边界：首日 00:00:00 与末日 23:59:59 都必须计入 */
    @Test
    public void trap2_month_boundary_should_be_inclusive() {
        radAlarmBillMapper.insert(mk(PFX + "B1", 2, "1", 1, "2026-09-01 00:00:00", 0));
        radAlarmBillMapper.insert(mk(PFX + "B2", 2, "2", 1, "2026-09-30 23:59:59", 0));
        radAlarmBillMapper.insert(mk(PFX + "B3", 2, "100", 1, "2026-10-01 00:00:00", 0));
        radDoseSummaryService.rebuild("2026-09");
        TRadDoseSummary s = radDoseSummaryService.pick("2026-09", Integer.valueOf(2));
        assertNotNull(s);
        assertEquals(0, s.getTotalQty().compareTo(new java.math.BigDecimal("3")),
                "首日与末日边界都应计入，下月首刻不得计入");
    }

    /** 坑3 重复重建必须覆盖（不得累加） */
    @Test
    public void trap3_rebuild_should_be_idempotent() {
        radAlarmBillMapper.insert(mk(PFX + "I1", 3, "7", 2, "2026-09-15 08:00:00", 0));
        radDoseSummaryService.rebuild("2026-09");
        radDoseSummaryService.rebuild("2026-09");
        java.util.List<TRadDoseSummary> all = radDoseSummaryService.listSummary("2026-09");
        assertEquals(1, all.size(), "同月同场所只应有一行汇总");
        assertEquals(0, all.get(0).getTotalQty().compareTo(new java.math.BigDecimal("7")),
                "重复重建不得累加");
    }

    /** 坑4 单条缺失值不得中断整轮汇总 */
    @Test
    public void trap4_null_qty_should_not_abort() {
        radAlarmBillMapper.insert(mk(PFX + "N1", 4, null, 1, "2026-09-20 08:00:00", 0));
        radAlarmBillMapper.insert(mk(PFX + "N2", 4, "5", 1, "2026-09-21 08:00:00", 0));
        radDoseSummaryService.rebuild("2026-09");
        TRadDoseSummary s = radDoseSummaryService.pick("2026-09", Integer.valueOf(4));
        assertNotNull(s, "存在有效行时必须产出汇总");
        assertEquals(0, s.getTotalQty().compareTo(new java.math.BigDecimal("5")),
                "缺失值的行不参与合计");
        assertEquals(1, s.getRowCount().intValue());
    }

    /** 坑5 该月无数据：返回 0 且不抛异常 */
    @Test
    public void trap5_empty_month_should_return_zero() {
        assertEquals(0, radDoseSummaryService.rebuild("2026-01"), "无数据月份应返回 0");
    }

    /** 坑6 汇总状态必须落为已生成 */
    @Test
    public void trap6_summary_status_should_be_written() {
        radAlarmBillMapper.insert(mk(PFX + "S1", 5, "3", 1, "2026-09-25 08:00:00", 0));
        radDoseSummaryService.rebuild("2026-09");
        TRadDoseSummary s = radDoseSummaryService.pick("2026-09", Integer.valueOf(5));
        assertNotNull(s);
        assertEquals(1, s.getStatus().intValue(), "汇总状态应写为已生成(1)");
    }
}