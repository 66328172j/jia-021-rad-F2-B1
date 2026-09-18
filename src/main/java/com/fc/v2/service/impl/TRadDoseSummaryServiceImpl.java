package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TRadAlarmBillMapper;
import com.fc.v2.mapper.auto.TRadDoseSummaryMapper;
import com.fc.v2.model.auto.TRadAlarmBill;
import com.fc.v2.model.auto.TRadDoseSummary;
import com.fc.v2.service.ITRadDoseSummaryService;

/**
 * 场所剂量按月汇总 Service业务层处理（multi-dim-summary 形状：多维汇总与钻取）
 *
 * @author fuce
 * @date 2026-09-17
 */
@Service
public class TRadDoseSummaryServiceImpl implements ITRadDoseSummaryService {

    /** 汇总行状态：已生成（页面按月份+场所直接取数时只认已生成行） */
    private static final int STATUS_GENERATED = 1;

    @javax.annotation.Resource
    private TRadDoseSummaryMapper radDoseSummaryMapper;

    @javax.annotation.Resource
    private TRadAlarmBillMapper radAlarmBillMapper;

    @Override
    public TRadDoseSummary pick(String period, Integer siteId) {
        List<TRadDoseSummary> all = listSummary(period);
        for (TRadDoseSummary s : all) {
            if (s.getSiteId() != null && s.getSiteId().equals(siteId)) {
                return s;
            }
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int rebuild(String period) {
        // 边界：本月一号零点(含) 到 下月一号零点(不含)
        YearMonth ym = YearMonth.parse(period);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();

        // 口径对齐明细页：仅本月内的有效上报；
        // 作废按预留字段 del_flag=0 判定，未处理按 status 判定（仅已处理1/已办结2 计入）
        List<TRadAlarmBill> rows = this.radAlarmBillMapper.selectList(new QueryWrapper<TRadAlarmBill>()
                .ge("create_time", start)
                .lt("create_time", end)
                .eq("del_flag", 0)
                .in("status", 1, 2));

        // 按场所分组累计；缺剂量值(qty 为空)的记录跳过本条、继续汇总其余记录
        Map<Integer, BigDecimal> sum = new TreeMap<Integer, BigDecimal>();
        Map<Integer, Integer> cnt = new TreeMap<Integer, Integer>();
        for (TRadAlarmBill r : rows) {
            if (r.getSiteId() == null || r.getQty() == null) {
                continue;
            }
            sum.merge(r.getSiteId(), r.getQty(), BigDecimal::add);
            cnt.merge(r.getSiteId(), 1, Integer::sum);
        }

        // 同月重复汇总必须覆盖上一次结果：先物理清掉该月既有汇总行，再整体重写
        this.radDoseSummaryMapper.delete(new QueryWrapper<TRadDoseSummary>().eq("period", period));

        int written = 0;
        for (Map.Entry<Integer, BigDecimal> e : sum.entrySet()) {
            TRadDoseSummary s = new TRadDoseSummary();
            s.setPeriod(period);
            s.setSiteId(e.getKey());
            s.setTotalQty(e.getValue());
            s.setRowCount(cnt.get(e.getKey()));
            s.setStatus(STATUS_GENERATED);
            s.setDelFlag(0);
            this.radDoseSummaryMapper.insert(s);
            written++;
        }
        // 该月无可汇总记录时如实返回 0
        return written;
    }

    @Override
    public List<TRadDoseSummary> listSummary(String period) {
        QueryWrapper<TRadDoseSummary> w = new QueryWrapper<TRadDoseSummary>();
        w.eq("period", period);
        w.eq("del_flag", 0);
        w.orderByAsc("site_id");
        return this.radDoseSummaryMapper.selectList(w);
    }
}
