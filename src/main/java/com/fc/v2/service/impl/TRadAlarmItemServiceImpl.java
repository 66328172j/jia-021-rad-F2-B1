package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fc.v2.mapper.auto.TRadAlarmItemMapper;
import com.fc.v2.model.auto.TRadAlarmItem;
import com.fc.v2.service.ITRadAlarmItemService;

/**
 * 剂量预警条目 Service业务层处理（scheduling-job 形状：周期执行）。
 * 执行窗口/到期/状态/容错口径统一走 {@link AbstractDueScanService}，本类只做字段适配。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TRadAlarmItemServiceImpl extends AbstractDueScanService<TRadAlarmItem>
        implements ITRadAlarmItemService {

    @javax.annotation.Resource
    private TRadAlarmItemMapper radAlarmItemMapper;

    @Override
    public TRadAlarmItem selectTRadAlarmItemById(Long id) {
        return this.radAlarmItemMapper.selectById(id);
    }

    @Override
    public List<TRadAlarmItem> listDue(Date at) {
        return super.listDue(at);
    }

    @Override
    public int runOnce(Date at) {
        return super.runOnce(at);
    }

    @Override
    protected com.baomidou.mybatisplus.core.mapper.BaseMapper<TRadAlarmItem> mapper() {
        return radAlarmItemMapper;
    }

    @Override
    protected Date dueAtOf(TRadAlarmItem row) {
        return row.getDueAt();
    }

    @Override
    protected BigDecimal amountOf(TRadAlarmItem row) {
        return row.getAmount();
    }

    @Override
    protected Integer statusOf(TRadAlarmItem row) {
        return row.getStatus();
    }

    @Override
    protected Integer delFlagOf(TRadAlarmItem row) {
        return row.getDelFlag();
    }

    @Override
    protected void applyStatus(TRadAlarmItem row, int status) {
        row.setStatus(status);
    }
}
