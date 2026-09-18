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
        String no = rows.get(0).getBatchNo();
        java.util.List<TRadDoseRow> errors = new java.util.ArrayList<TRadDoseRow>();
        int seq = 0;
        for (TRadDoseRow r : rows) {
            if (r.getItemCode() == null || r.getItemCode().trim().isEmpty()
                    || r.getQty() == null
                    || r.getQty().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                seq++;
                r.setRowNo(Integer.valueOf(seq));
                r.setBatchNo(no);
                r.setStatus(STATUS_FAIL);
                this.radDoseRowMapper.insert(r);
                errors.add(r);
            }
        }
        if (!errors.isEmpty()) {
            return 0;
        }
        int ok = 0;
        for (TRadDoseRow r : rows) {
            r.setBatchNo(no);
            r.setStatus(STATUS_OK);
            this.radDoseRowMapper.insert(r);
            ok++;
        }
        return ok;
    }

    @Override
    public List<TRadDoseRow> listErrors(String batchNo) {
        return this.radDoseRowMapper.selectList(new QueryWrapper<TRadDoseRow>()
                .eq("batch_no", batchNo).eq("status", STATUS_FAIL));
    }
}
