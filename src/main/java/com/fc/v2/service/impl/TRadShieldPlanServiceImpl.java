package com.fc.v2.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fc.v2.mapper.auto.TRadShieldPlanMapper;
import com.fc.v2.model.auto.TRadShieldPlan;
import com.fc.v2.service.ITRadShieldPlanService;

/**
 * 屏蔽改造方案 Service业务层处理（approval-chain 形状：多阶段签批）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TRadShieldPlanServiceImpl implements ITRadShieldPlanService {

    private static final int MAX_NODE = 2;
    private static final int MODE_OR = 0;
    private static final int MODE_AND = 1;
    private static final int STATUS_RUNNING = 0;
    private static final int STATUS_PASS = 1;
    private static final int STATUS_VETO = 2;

    @javax.annotation.Resource
    private TRadShieldPlanMapper radShieldPlanMapper;

    @Override
    public TRadShieldPlan selectTRadShieldPlanById(Long id) {
        return this.radShieldPlanMapper.selectById(id);
    }

    @Override
    public TRadShieldPlan approve(Long id, String approver, String comment) {
        TRadShieldPlan r = this.radShieldPlanMapper.selectById(id);
        if (r == null || approver == null || approver.trim().isEmpty()) {
            return null;
        }
        // 终态守卫：已通过/已否决的单据不得再签，原样返回（调用方据状态识别）
        if (isFinished(r)) {
            return r;
        }
        int node = r.getNodeNo() == null ? 0 : r.getNodeNo();
        int mode = r.getSignMode() == null ? MODE_OR : r.getSignMode();
        int signed = r.getSignCount() == null ? 0 : r.getSignCount();
        int need = r.getNeedCount() == null || r.getNeedCount() <= 0 ? 1 : r.getNeedCount();

        if (mode == MODE_AND) {
            // 会签：先累计票数，未满员不推进环节
            signed++;
            r.setSignCount(signed);
            if (signed < need) {
                r.setNodeNo(node);
                r.setStatus(STATUS_RUNNING);
                persistIfRunning(id, r);
                return r;
            }
        }
        // 或签一票即走；会签满员同样走到这里
        if (node >= MAX_NODE) {
            // 末环节签批完成：判通过，环节不动
            r.setStatus(STATUS_PASS);
        } else {
            // 进入下一环节，本环节票数清零
            r.setNodeNo(node + 1);
            r.setSignCount(0);
            r.setStatus(STATUS_RUNNING);
        }
        persistIfRunning(id, r);
        return r;
    }

    @Override
    public TRadShieldPlan reject(Long id, String approver, String comment) {
        TRadShieldPlan r = this.radShieldPlanMapper.selectById(id);
        if (r == null || approver == null || approver.trim().isEmpty()) {
            return null;
        }
        // 终态守卫：已通过/已否决不得重复落否决
        if (isFinished(r)) {
            return r;
        }
        // 否决即终止流转
        r.setStatus(STATUS_VETO);
        persistIfRunning(id, r);
        return r;
    }

    @Override
    public TRadShieldPlan rollback(Long id, String comment) {
        TRadShieldPlan r = this.radShieldPlanMapper.selectById(id);
        if (r == null) {
            return null;
        }
        // 终态守卫：已通过/已否决不允许退回
        if (isFinished(r)) {
            return r;
        }
        int node = r.getNodeNo() == null ? 0 : r.getNodeNo();
        if (node <= 0) {
            // 首环节无处可退
            return null;
        }
        // 退回上一环节，本环节已签票数清零，单据仍在批
        r.setNodeNo(node - 1);
        r.setSignCount(0);
        r.setStatus(STATUS_RUNNING);
        persistIfRunning(id, r);
        return r;
    }

    private boolean isFinished(TRadShieldPlan r) {
        return r.getStatus() != null && r.getStatus() != STATUS_RUNNING;
    }

    /** 条件落库：仅审批中的单据可写，挡住并发终态穿透 */
    private int persistIfRunning(Long id, TRadShieldPlan r) {
        return this.radShieldPlanMapper.update(r, new UpdateWrapper<TRadShieldPlan>()
                .eq("id", id)
                .eq("del_flag", 0)
                .eq("status", STATUS_RUNNING));
    }
}
