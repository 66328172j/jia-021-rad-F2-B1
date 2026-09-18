package com.fc.v2.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
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

    /** 末环节：推进到本环节后再推进即办结 */
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
        // 台账只展示未删除单据（逻辑删除的不进流转台账）
        queryWrapper.eq("del_flag", 0);
        return this.radDisposeMapper.selectList(queryWrapper);
    }

    @Override
    public TRadDispose advance(Long id, String remark) {
        TRadDispose r = requireActive(id);
        if (r == null) {
            return null;
        }
        // 重复提交识别：与上一次动作口径完全相同（含都为空，典型的双击重放）的提交按重放处理，不再推进
        if (java.util.Objects.equals(remark, r.getLastAction())) {
            return null;
        }
        int stage = r.getStage() == null ? 0 : r.getStage();
        if (stage >= MAX_STAGE) {
            // 末环节推进：办结
            r.setStatus(STATUS_TERMINAL);
        } else {
            // 每次只推进一个环节，不允许跨格
            r.setStage(stage + 1);
            r.setStatus(STATUS_ACTIVE);
        }
        r.setLastAction(remark);
        if (!casState(id, r)) {
            return null;
        }
        return r;
    }

    @Override
    public TRadDispose rollback(Long id, String remark) {
        TRadDispose r = requireActive(id);
        if (r == null) {
            return null;
        }
        int stage = r.getStage() == null ? 0 : r.getStage();
        if (stage <= 0) {
            // 首环节无可回退之处
            return null;
        }
        // 每次只回退一个环节，状态回到在办，不允许一把清回首环节
        r.setStage(stage - 1);
        r.setStatus(STATUS_ACTIVE);
        r.setLastAction(remark);
        if (!casState(id, r)) {
            return null;
        }
        return r;
    }

    @Override
    public boolean updateContent(Long id, String remark) {
        TRadDispose r = this.radDisposeMapper.selectById(id);
        if (r == null || isDeleted(r)) {
            return false;
        }
        // 终态锁：已办结的单据内容冻结，不得再保存
        if (isTerminal(r)) {
            return false;
        }
        r.setContent(remark);
        return this.radDisposeMapper.updateById(r) > 0;
    }

    @Override
    public boolean remove(Long id) {
        TRadDispose r = this.radDisposeMapper.selectById(id);
        if (r == null || isDeleted(r)) {
            return false;
        }
        // 终态锁：已办结单据保留留痕，不得删除（否则环节链断链）
        if (isTerminal(r)) {
            return false;
        }
        // 逻辑删除：台账与环节链仍可追溯，不做物理清除
        r.setDelFlag(1);
        return this.radDisposeMapper.updateById(r) > 0;
    }

    /** 取出可流转的单据：存在、未删除、未办结；否则返回 null（调用方按拒绝处理） */
    private TRadDispose requireActive(Long id) {
        if (id == null) {
            return null;
        }
        TRadDispose r = this.radDisposeMapper.selectById(id);
        if (r == null || isDeleted(r) || isTerminal(r)) {
            return null;
        }
        return r;
    }

    private boolean isTerminal(TRadDispose r) {
        return r.getStatus() != null && r.getStatus() == STATUS_TERMINAL;
    }

    private boolean isDeleted(TRadDispose r) {
        return r.getDelFlag() != null && r.getDelFlag() == 1;
    }

    /**
     * 条件落库：仅当单据仍未删除、未办结时才允许写入。
     * 兜住并发场景下"读出来还在办、写下去已办结"的终态穿透。
     */
    private boolean casState(Long id, TRadDispose r) {
        return this.radDisposeMapper.update(r, new UpdateWrapper<TRadDispose>()
                .eq("id", id)
                .eq("del_flag", 0)
                .ne("status", STATUS_TERMINAL)) > 0;
    }
}
