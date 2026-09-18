package com.fc.v2.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TRadDoseRowMapper;
import com.fc.v2.model.auto.TRadDoseRow;
import com.fc.v2.service.ITRadDoseRowService;

/**
 * 个人剂量巡测明细 Service业务层处理（batch-process 形状：整批提交）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TRadDoseRowServiceImpl implements ITRadDoseRowService {

    private static final int MAX_ROWS = 500;
    private static final int STATUS_OK = 1;
    private static final int STATUS_FAIL = 2;

    @javax.annotation.Resource
    private TRadDoseRowMapper radDoseRowMapper;

    @Override
    public TRadDoseRow selectTRadDoseRowById(Long id) {
        return this.radDoseRowMapper.selectById(id);
    }

    @Override
    public int submitBatch(String batchNo, List<TRadDoseRow> rows) {
        if (rows == null || rows.isEmpty()) {
            // 空批次：0 行入库
            return 0;
        }
        if (rows.size() > MAX_ROWS) {
            // 超单批上限：整批不入库
            return -1;
        }
        // 重复提交识别：同批次号已有入库记录（成功或失败明细）即视为重放，
        // 不再重复入库，直接回既有成功行数，保证重复提交结果一致
        Integer existed = this.radDoseRowMapper.selectCount(new QueryWrapper<TRadDoseRow>()
                .eq("batch_no", batchNo));
        if (existed != null && existed > 0) {
            Integer okExisted = this.radDoseRowMapper.selectCount(new QueryWrapper<TRadDoseRow>()
                    .eq("batch_no", batchNo).eq("status", STATUS_OK));
            return okExisted == null ? 0 : okExisted;
        }

        int ok = 0;
        for (int i = 0; i < rows.size(); i++) {
            TRadDoseRow r = rows.get(i);
            // 保留原始行号（批次内从 1 起）
            r.setRowNo(Integer.valueOf(i + 1));
            r.setBatchNo(batchNo);
            if (r.getItemCode() == null || r.getItemCode().trim().isEmpty()
                    || r.getQty() == null
                    || r.getQty().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                // 非法行：记失败明细，不中断整批
                r.setStatus(STATUS_FAIL);
            } else {
                r.setStatus(STATUS_OK);
                ok++;
            }
            this.radDoseRowMapper.insert(r);
        }
        return ok;
    }

    @Override
    public List<TRadDoseRow> listErrors(String batchNo) {
        return this.radDoseRowMapper.selectList(new QueryWrapper<TRadDoseRow>()
                .eq("batch_no", batchNo).eq("status", STATUS_FAIL).orderByAsc("row_no"));
    }
}
