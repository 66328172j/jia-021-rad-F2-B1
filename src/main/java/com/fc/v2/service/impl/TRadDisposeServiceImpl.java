package com.fc.v2.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TRadDisposeMapper;
import com.fc.v2.model.auto.TRadDispose;
import com.fc.v2.service.ITRadDisposeService;

/**
 * 分区剂量异常处置单 Service业务层处理（state-machine 形状：单据流转）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TRadDisposeServiceImpl implements ITRadDisposeService {

    private static final int MAX_STAGE = 3;
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_TERMINAL = 2;

    @javax.annotation.Resource
    private TRadDisposeMapper radDisposeMapper;

    @Override
    public TRadDispose selectTRadDisposeById(Long id) {
        return this.radDisposeMapper.selectById(id);
    }

    @Override
    public List<TRadDispose> selectTRadDisposeList(QueryWrapper<TRadDispose> queryWrapper) {
        return this.radDisposeMapper.selectList(queryWrapper);
    }

    @Override
    public TRadDispose advance(Long id, String remark) {
        TRadDispose r = this.radDisposeMapper.selectById(id);
        if (r == null) {
            return null;
        }
        int st = r.getStage() == null ? 0 : r.getStage();
        r.setStage(Math.min(st + 2, MAX_STAGE));
        r.setStatus(STATUS_ACTIVE);
        r.setLastAction(remark);
        this.radDisposeMapper.updateById(r);
        return r;
    }

    @Override
    public TRadDispose rollback(Long id, String remark) {
        TRadDispose r = this.radDisposeMapper.selectById(id);
        if (r == null) {
            return null;
        }
        r.setStage(0);
        r.setStatus(STATUS_ACTIVE);
        r.setLastAction(remark);
        this.radDisposeMapper.updateById(r);
        return r;
    }

    @Override
    public boolean updateContent(Long id, String remark) {
        TRadDispose r = this.radDisposeMapper.selectById(id);
        if (r == null) {
            return false;
        }
        r.setContent(remark);
        return this.radDisposeMapper.updateById(r) > 0;
    }

    @Override
    public boolean remove(Long id) {
        TRadDispose r = this.radDisposeMapper.selectById(id);
        if (r == null) {
            return false;
        }
        return this.radDisposeMapper.deleteById(id) > 0;
    }

}
