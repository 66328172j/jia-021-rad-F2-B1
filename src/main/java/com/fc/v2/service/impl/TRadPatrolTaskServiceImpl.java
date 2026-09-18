package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fc.v2.mapper.auto.TRadPatrolTaskMapper;
import com.fc.v2.model.auto.TRadPatrolTask;
import com.fc.v2.service.ITRadPatrolTaskService;

/**
 * 个人剂量巡测条目 Service业务层处理（scheduling-job 形状：周期执行）。
 * 执行窗口/到期/状态/容错口径统一走 {@link AbstractDueScanService}，本类只做字段适配。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TRadPatrolTaskServiceImpl extends AbstractDueScanService<TRadPatrolTask>
        implements ITRadPatrolTaskService {

    @javax.annotation.Resource
    private TRadPatrolTaskMapper radPatrolTaskMapper;

    @Override
    public TRadPatrolTask selectTRadPatrolTaskById(Long id) {
        return this.radPatrolTaskMapper.selectById(id);
    }

    @Override
    public List<TRadPatrolTask> listDue(Date at) {
        return super.listDue(at);
    }

    @Override
    public int runOnce(Date at) {
        return super.runOnce(at);
    }

    @Override
    protected com.baomidou.mybatisplus.core.mapper.BaseMapper<TRadPatrolTask> mapper() {
        return radPatrolTaskMapper;
    }

    @Override
    protected Date dueAtOf(TRadPatrolTask row) {
        return row.getDueAt();
    }

    @Override
    protected BigDecimal amountOf(TRadPatrolTask row) {
        return row.getAmount();
    }

    @Override
    protected Integer statusOf(TRadPatrolTask row) {
        return row.getStatus();
    }

    @Override
    protected Integer delFlagOf(TRadPatrolTask row) {
        return row.getDelFlag();
    }

    @Override
    protected void applyStatus(TRadPatrolTask row, int status) {
        row.setStatus(status);
    }
}
