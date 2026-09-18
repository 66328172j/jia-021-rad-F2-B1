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
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fc.v2.mapper.auto.TRadDoseSummaryMapper;
import com.fc.v2.model.auto.TRadAlarmBill;
import com.fc.v2.model.auto.TRadDoseSummary;
import com.fc.v2.service.ITRadAlarmBillService;
import com.fc.v2.service.ITRadDoseSummaryService;

/**
 * 场所剂量按月汇总 Service业务层处理（multi-dim-summary 形状：多维汇总与钻取）
 *
 * 取数口径与明细页/历史重算完全一致，统一由
 * {@link ITRadAlarmBillService#selectEffectiveForDose} 提供，本类不再另写过滤条件。
 *
 * @author fuce
 * @date 2026-09-17
 */
@Service
public class TRadDoseSummaryServiceImpl implements ITRadDoseSummaryService {

    /** 汇总行状态：已生成（页面按月份+场所直接取数时只认已生成行） */
    private static final int STATUS_GENERATED = 1;
    private static final int DEL_FLAG_NORMAL = 0;
    private static final int DEL_FLAG_DELETED = 1;

    @javax.annotation.Resource
    private TRadDoseSummaryMapper radDoseSummaryMapper;

    @javax.annotation.Resource
    private ITRadAlarmBillService radAlarmBillService;

    @Override
    public TRadDoseSummary pick(String period, Long siteId) {
        if (siteId == null) {
            return null;
        }
        // 与列表/重算同口径：只取本月、未作废、已生成的行
        return this.radDoseSummaryMapper.selectOne(new QueryWrapper<TRadDoseSummary>()
                .eq("period", period)
                .eq("site_id", siteId)
                .eq("status", STATUS_GENERATED)
                .eq("del_flag", DEL_FLAG_NORMAL)
                .last("limit 1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int rebuild(String period) {
        // 边界：本月一号零点(含) 到 下月一号零点(不含)
        YearMonth ym = YearMonth.parse(period);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();

        // 取数口径与明细页一致：唯一来自预警单 Service（有效、已处理/已办结、本月时间窗）
        List<TRadAlarmBill> rows = radAlarmBillService.selectEffectiveForDose(start, end);

        // 按场所分组累计；缺剂量值(qty 为空)的记录跳过本条、继续汇总其余记录
        Map<Long, BigDecimal> sum = new TreeMap<Long, BigDecimal>();
        Map<Long, Integer> cnt = new TreeMap<Long, Integer>();
        for (TRadAlarmBill r : rows) {
            if (r.getSiteId() == null || r.getQty() == null) {
                continue;
            }
            sum.merge(r.getSiteId(), r.getQty(), BigDecimal::add);
            cnt.merge(r.getSiteId(), 1, Integer::sum);
        }

        // 同月重复汇总必须覆盖上一次结果：先把该月既有「有效」汇总行逻辑作废（del_flag=1），
        // 再整体重写。不做物理删除，保留历史汇总链，审计可追溯。
        this.radDoseSummaryMapper.update(null, new UpdateWrapper<TRadDoseSummary>()
                .set("del_flag", DEL_FLAG_DELETED)
                .eq("period", period)
                .eq("del_flag", DEL_FLAG_NORMAL));

        int written = 0;
        for (Map.Entry<Long, BigDecimal> e : sum.entrySet()) {
            TRadDoseSummary s = new TRadDoseSummary();
            s.setPeriod(period);
            s.setSiteId(e.getKey());
            s.setTotalQty(e.getValue());
            s.setRowCount(cnt.get(e.getKey()));
            s.setStatus(STATUS_GENERATED);
            s.setDelFlag(DEL_FLAG_NORMAL);
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
        w.eq("status", STATUS_GENERATED);
        w.eq("del_flag", DEL_FLAG_NORMAL);
        w.orderByAsc("site_id");
        return this.radDoseSummaryMapper.selectList(w);
    }
}
