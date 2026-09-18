package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fc.v2.mapper.auto.TRadSourceStockMapper;
import com.fc.v2.model.auto.TRadSourceStock;
import com.fc.v2.service.ITRadSourceStockService;

/**
 * 放射源出入库条目 Service业务层处理（scheduling-job 形状：周期执行）。
 * 执行窗口/到期/状态/容错口径统一走 {@link AbstractDueScanService}，本类只做字段适配。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TRadSourceStockServiceImpl extends AbstractDueScanService<TRadSourceStock>
        implements ITRadSourceStockService {

    @javax.annotation.Resource
    private TRadSourceStockMapper radSourceStockMapper;

    @Override
    public TRadSourceStock selectTRadSourceStockById(Long id) {
        return this.radSourceStockMapper.selectById(id);
    }

    @Override
    public List<TRadSourceStock> listDue(Date at) {
        return super.listDue(at);
    }

    @Override
    public int runOnce(Date at) {
        return super.runOnce(at);
    }

    @Override
    protected com.baomidou.mybatisplus.core.mapper.BaseMapper<TRadSourceStock> mapper() {
        return radSourceStockMapper;
    }

    @Override
    protected Date dueAtOf(TRadSourceStock row) {
        return row.getDueAt();
    }

    @Override
    protected BigDecimal amountOf(TRadSourceStock row) {
        return row.getAmount();
    }

    @Override
    protected Integer statusOf(TRadSourceStock row) {
        return row.getStatus();
    }

    @Override
    protected Integer delFlagOf(TRadSourceStock row) {
        return row.getDelFlag();
    }

    @Override
    protected void applyStatus(TRadSourceStock row, int status) {
        row.setStatus(status);
    }
}
