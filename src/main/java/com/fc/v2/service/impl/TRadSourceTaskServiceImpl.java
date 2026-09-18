package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fc.v2.mapper.auto.TRadSourceTaskMapper;
import com.fc.v2.model.auto.TRadSourceTask;
import com.fc.v2.service.ITRadSourceTaskService;

/**
 * 放射源出入库任务条目 Service业务层处理（scheduling-job 形状：周期执行）。
 * 执行窗口/到期/状态/容错口径统一走 {@link AbstractDueScanService}，本类只做字段适配。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TRadSourceTaskServiceImpl extends AbstractDueScanService<TRadSourceTask>
        implements ITRadSourceTaskService {

    @javax.annotation.Resource
    private TRadSourceTaskMapper radSourceTaskMapper;

    @Override
    public TRadSourceTask selectTRadSourceTaskById(Long id) {
        return this.radSourceTaskMapper.selectById(id);
    }

    @Override
    public List<TRadSourceTask> listDue(Date at) {
        return super.listDue(at);
    }

    @Override
    public int runOnce(Date at) {
        return super.runOnce(at);
    }

    @Override
    protected com.baomidou.mybatisplus.core.mapper.BaseMapper<TRadSourceTask> mapper() {
        return radSourceTaskMapper;
    }

    @Override
    protected Date dueAtOf(TRadSourceTask row) {
        return row.getDueAt();
    }

    @Override
    protected BigDecimal amountOf(TRadSourceTask row) {
        return row.getAmount();
    }

    @Override
    protected Integer statusOf(TRadSourceTask row) {
        return row.getStatus();
    }

    @Override
    protected Integer delFlagOf(TRadSourceTask row) {
        return row.getDelFlag();
    }

    @Override
    protected void applyStatus(TRadSourceTask row, int status) {
        row.setStatus(status);
    }
}
