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
 * 环节链 stage：0待发起 -> 1 -> 2 -> 3（到 3 即办结），每次只能走一格。
 * 所有流转入口（推进/回退/改内容/删除）共用同一份锁单口径，见类内守卫方法，
 * 不得在各入口各写各的判定。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TRadDisposeServiceImpl implements ITRadDisposeService {

    private static final int MIN_STAGE = 0;
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
        // 流转台账只出现有效单据；作废（逻辑删除）单据不影响台账口径，
        // 但其环节链仍保留在库（删除为逻辑删除，见 remove）。
        queryWrapper.eq("del_flag", 0);
        return this.radDisposeMapper.selectList(queryWrapper);
    }

    @Override
    public TRadDispose advance(Long id, String remark) {
        TRadDispose r = loadMutable(id);
        if (r == null) {
            return null;
        }
        int stage = nz(r.getStage());
        if (stage >= MAX_STAGE) {
            // 已在末环（办结），不能再推进
            return null;
        }
        int nextStage = stage + 1;
        int nextStatus = nextStage >= MAX_STAGE ? STATUS_TERMINAL : STATUS_ACTIVE;
        // 条件更新（CAS）：以当前环节为前置条件，重复提交/并发点击只有一个请求能推进成功，
        // 单子不会因双击多走一步。
        int rows = this.radDisposeMapper.update(null, new UpdateWrapper<TRadDispose>()
                .set("stage", nextStage)
                .set("status", nextStatus)
                .set("last_action", remark)
                .eq("id", id)
                .eq("stage", stage)
                .ne("status", STATUS_TERMINAL)
                .eq("del_flag", 0));
        if (rows == 0) {
            return null;
        }
        return this.radDisposeMapper.selectById(id);
    }

    @Override
    public TRadDispose rollback(Long id, String remark) {
        TRadDispose r = loadMutable(id);
        if (r == null) {
            return null;
        }
        int stage = nz(r.getStage());
        if (stage <= MIN_STAGE) {
            // 首环之前没有可退的环节
            return null;
        }
        // 同样以当前环节为前置条件做条件更新，重复回退不会连退多格
        int rows = this.radDisposeMapper.update(null, new UpdateWrapper<TRadDispose>()
                .set("stage", stage - 1)
                .set("status", STATUS_ACTIVE)
                .set("last_action", remark)
                .eq("id", id)
                .eq("stage", stage)
                .ne("status", STATUS_TERMINAL)
                .eq("del_flag", 0));
        if (rows == 0) {
            return null;
        }
        return this.radDisposeMapper.selectById(id);
    }

    @Override
    public boolean updateContent(Long id, String remark) {
        TRadDispose r = loadMutable(id);
        if (r == null) {
            return false;
        }
        // 条件里再次带终态/作废守卫，避免读与写之间状态被别的请求推进到办结
        return this.radDisposeMapper.update(null, new UpdateWrapper<TRadDispose>()
                .set("content", remark)
                .eq("id", id)
                .ne("status", STATUS_TERMINAL)
                .eq("del_flag", 0)) > 0;
    }

    @Override
    public boolean remove(Long id) {
        TRadDispose r = loadMutable(id);
        if (r == null) {
            return false;
        }
        // 只作逻辑删除：单据与已走过的环节链保留在库，台账/审计不缺段。
        // 办结单据受同一把锁保护，不允许清掉。
        return this.radDisposeMapper.update(null, new UpdateWrapper<TRadDispose>()
                .set("del_flag", 1)
                .eq("id", id)
                .ne("status", STATUS_TERMINAL)
                .eq("del_flag", 0)) > 0;
    }

    /**
     * 统一锁单口径（推进/回退/改内容/删除共用，禁止各入口另写一份）：
     * 单据存在、未逻辑删除、未办结，才允许流转类写操作。
     */
    private TRadDispose loadMutable(Long id) {
        if (id == null) {
            return null;
        }
        TRadDispose r = this.radDisposeMapper.selectById(id);
        if (r == null) {
            return null;
        }
        if (r.getDelFlag() != null && r.getDelFlag() == 1) {
            return null;
        }
        if (r.getStatus() != null && r.getStatus() == STATUS_TERMINAL) {
            return null;
        }
        return r;
    }

    private int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
