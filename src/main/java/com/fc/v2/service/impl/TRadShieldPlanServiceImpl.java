package com.fc.v2.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fc.v2.mapper.auto.TRadShieldPlanMapper;
import com.fc.v2.model.auto.TRadShieldPlan;
import com.fc.v2.service.ITRadShieldPlanService;

/**
 * 屏蔽改造方案 Service业务层处理（approval-chain 形状：多阶段签批）
 *
 * 环节链 node_no：0 -> 1 -> 2（到 2 即签批通过），每次签批只能走一格。
 * 签批/否决/退回共用同一份终态锁口径：已通过(1)/已否决(2)即终态，
 * 终态单据不得再签批、否决或退回重走，见类内守卫。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TRadShieldPlanServiceImpl implements ITRadShieldPlanService {

    private static final int MIN_NODE = 0;
    private static final int MAX_NODE = 2;
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
        TRadShieldPlan r = loadMutable(id, approver);
        if (r == null) {
            return null;
        }
        int node = nz(r.getNodeNo());
        if (node >= MAX_NODE) {
            return null;
        }
        int nextNode = node + 1;
        int nextStatus = nextNode >= MAX_NODE ? STATUS_PASS : STATUS_RUNNING;
        // 条件更新（CAS）：以当前环节为前置条件，重复提交/并发签批只有一票能推进，
        // 不会因双击连走两环或重复计票。
        int rows = this.radShieldPlanMapper.update(null, new UpdateWrapper<TRadShieldPlan>()
                .set("node_no", nextNode)
                .set("status", nextStatus)
                .set("remark", comment)
                .eq("id", id)
                .eq("node_no", node)
                .eq("status", STATUS_RUNNING)
                .eq("del_flag", 0));
        if (rows == 0) {
            return null;
        }
        return this.radShieldPlanMapper.selectById(id);
    }

    @Override
    public TRadShieldPlan reject(Long id, String approver, String comment) {
        TRadShieldPlan r = loadMutable(id, approver);
        if (r == null) {
            return null;
        }
        // 否决必须真正落终态；原实现只回写未改任何字段，否决不生效
        int rows = this.radShieldPlanMapper.update(null, new UpdateWrapper<TRadShieldPlan>()
                .set("status", STATUS_VETO)
                .set("remark", comment)
                .eq("id", id)
                .eq("status", STATUS_RUNNING)
                .eq("del_flag", 0));
        if (rows == 0) {
            return null;
        }
        return this.radShieldPlanMapper.selectById(id);
    }

    @Override
    public TRadShieldPlan rollback(Long id, String comment) {
        TRadShieldPlan r = loadMutable(id, null);
        if (r == null) {
            return null;
        }
        int node = nz(r.getNodeNo());
        if (node <= MIN_NODE) {
            return null;
        }
        // CAS 退回一环；终态单据在 loadMutable 已被拦下，不会退回重走
        int rows = this.radShieldPlanMapper.update(null, new UpdateWrapper<TRadShieldPlan>()
                .set("node_no", node - 1)
                .set("status", STATUS_RUNNING)
                .set("remark", comment)
                .eq("id", id)
                .eq("node_no", node)
                .eq("status", STATUS_RUNNING)
                .eq("del_flag", 0));
        if (rows == 0) {
            return null;
        }
        return this.radShieldPlanMapper.selectById(id);
    }

    /**
     * 统一终态锁口径（签批/否决/退回共用）：单据存在、未作废、仍在审批中，
     * 且签批类动作须带签批人，才允许写操作。
     */
    private TRadShieldPlan loadMutable(Long id, String approver) {
        if (id == null) {
            return null;
        }
        TRadShieldPlan r = this.radShieldPlanMapper.selectById(id);
        if (r == null) {
            return null;
        }
        if (r.getDelFlag() != null && r.getDelFlag() == 1) {
            return null;
        }
        if (r.getStatus() == null || r.getStatus() != STATUS_RUNNING) {
            return null;
        }
        if (approver != null && approver.trim().isEmpty()) {
            return null;
        }
        return r;
    }

    private int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
