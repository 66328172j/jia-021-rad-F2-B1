package com.fc.v2.rad;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TRadShieldPlanMapper;
import com.fc.v2.model.auto.TRadShieldPlan;
import com.fc.v2.service.ITRadShieldPlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * rad F3 屏蔽改造方案审批流（approval-flow 形状：多阶段签批）→ 功能点验收测试。
 * create 侧维护、质检时使用，**不得回填执行模型**。
 */
@SpringBootTest
public class RadF3AcceptanceTest {

    private static final String PFX = "APR-";

    @javax.annotation.Resource
    private ITRadShieldPlanService radShieldPlanService;

    @javax.annotation.Resource
    private TRadShieldPlanMapper radShieldPlanMapper;

    @BeforeEach
    public void setUp() {
        radShieldPlanMapper.delete(new QueryWrapper<TRadShieldPlan>().likeRight("bill_no", PFX));
    }

    private TRadShieldPlan mk(String no, int node, int signMode, int need, int sign, int status) {
        TRadShieldPlan p = new TRadShieldPlan();
        p.setBillNo(no);
        p.setNodeNo(Integer.valueOf(node));
        p.setSignMode(Integer.valueOf(signMode));
        p.setNeedCount(Integer.valueOf(need));
        p.setSignCount(Integer.valueOf(sign));
        p.setStatus(Integer.valueOf(status));
        p.setDelFlag(0);
        radShieldPlanMapper.insert(p);
        return p;
    }

    /** 坑1 会签未满员不得推进环节 */
    @Test
    public void trap1_countersign_should_not_advance_until_full() {
        TRadShieldPlan p = mk(PFX + "A1", 0, 1, 3, 0, 0);
        TRadShieldPlan r = radShieldPlanService.approve(p.getId(), "zhang", "同意");
        assertNotNull(r);
        assertEquals(0, r.getNodeNo().intValue(), "会签未满员不得进入下一环节");
        assertEquals(0, r.getStatus().intValue(), "仍在批");
        assertEquals(1, r.getSignCount().intValue(), "已签票数应累加");
    }

    /** 坑2 或签一票即可推进 */
    @Test
    public void trap2_or_sign_should_advance_on_first_vote() {
        TRadShieldPlan p = mk(PFX + "O2", 0, 0, 3, 0, 0);
        TRadShieldPlan r = radShieldPlanService.approve(p.getId(), "li", "同意");
        assertNotNull(r);
        assertEquals(1, r.getNodeNo().intValue(), "或签任一票即应推进到下一环节");
    }

    /** 坑3 否决必须落状态（终止流转） */
    @Test
    public void trap3_reject_should_set_veto_status() {
        TRadShieldPlan p = mk(PFX + "V3", 1, 1, 2, 1, 0);
        TRadShieldPlan r = radShieldPlanService.reject(p.getId(), "wang", "方案不满足屏蔽要求");
        assertNotNull(r);
        assertEquals(2, r.getStatus().intValue(), "否决应把单据置为已否决");
    }

    /** 坑4 终态守卫：已通过/已否决不得再签 */
    @Test
    public void trap4_terminal_state_should_block_further_approve() {
        TRadShieldPlan p = mk(PFX + "T4", 2, 1, 1, 1, 1);
        TRadShieldPlan r = radShieldPlanService.approve(p.getId(), "zhao", "再签一次");
        assertNotNull(r);
        assertEquals(1, r.getStatus().intValue(), "已通过的单据不得被再次流转");
        assertEquals(2, r.getNodeNo().intValue(), "已通过的单据环节不得变化");
    }

    /** 坑5 退回必须回退环节并清零票数 */
    @Test
    public void trap5_rollback_should_reset_sign_count() {
        TRadShieldPlan p = mk(PFX + "R5", 2, 1, 3, 2, 0);
        TRadShieldPlan r = radShieldPlanService.rollback(p.getId(), "材料不全，退回上一环节");
        assertNotNull(r);
        assertEquals(1, r.getNodeNo().intValue(), "退回应回退一个环节");
        assertEquals(0, r.getSignCount().intValue(), "退回后已签票数应清零");
        assertEquals(0, r.getStatus().intValue(), "退回后单据仍在批");
    }

    /** 坑6 末环节会签满员应判通过 */
    @Test
    public void trap6_last_node_full_countersign_should_pass() {
        TRadShieldPlan p = mk(PFX + "P6", 2, 1, 2, 1, 0);
        TRadShieldPlan r = radShieldPlanService.approve(p.getId(), "chen", "同意");
        assertNotNull(r);
        assertEquals(1, r.getStatus().intValue(), "末环节会签满员应判通过");
    }
}