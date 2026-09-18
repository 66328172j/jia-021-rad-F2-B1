package com.fc.v2.rad.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fc.v2.mapper.auto.TRadShieldPlanMapper;
import com.fc.v2.model.auto.TRadShieldPlan;
import com.fc.v2.service.impl.TRadShieldPlanServiceImpl;

/** 屏蔽方案多阶段签批修复的纯单元验证（不依赖 Spring/数据库）。 */
@ExtendWith(MockitoExtension.class)
public class RadShieldPlanFlowTest {

    @Mock
    private TRadShieldPlanMapper mapper;

    @InjectMocks
    private TRadShieldPlanServiceImpl service;

    @BeforeEach
    public void wire() {
        org.mockito.Mockito.lenient().when(mapper.update(any(TRadShieldPlan.class), any())).thenReturn(1);
    }

    private TRadShieldPlan plan(int node, int mode, int need, int signed, int status) {
        TRadShieldPlan p = new TRadShieldPlan();
        p.setId(200L);
        p.setNodeNo(node);
        p.setSignMode(mode);
        p.setNeedCount(need);
        p.setSignCount(signed);
        p.setStatus(status);
        p.setDelFlag(0);
        return p;
    }

    @Test
    public void countersign_not_full_should_not_advance() {
        when(mapper.selectById(200L)).thenReturn(plan(0, 1, 3, 0, 0));
        TRadShieldPlan r = service.approve(200L, "zhang", "同意");
        assertEquals(0, r.getNodeNo().intValue(), "会签未满员不得进入下一环节");
        assertEquals(0, r.getStatus().intValue(), "仍在批");
        assertEquals(1, r.getSignCount().intValue(), "已签票数累加");
    }

    @Test
    public void or_sign_advances_on_first_vote() {
        when(mapper.selectById(200L)).thenReturn(plan(0, 0, 3, 0, 0));
        TRadShieldPlan r = service.approve(200L, "li", "同意");
        assertEquals(1, r.getNodeNo().intValue(), "或签一票即推进");
        assertEquals(0, r.getSignCount().intValue(), "进入下一环节票数清零");
    }

    @Test
    public void reject_sets_veto() {
        when(mapper.selectById(200L)).thenReturn(plan(1, 1, 2, 1, 0));
        TRadShieldPlan r = service.reject(200L, "wang", "不满足屏蔽要求");
        assertEquals(2, r.getStatus().intValue(), "否决落已否决状态");
    }

    @Test
    public void terminal_plan_cannot_be_signed_again() {
        when(mapper.selectById(200L)).thenReturn(plan(2, 1, 1, 1, 1));
        TRadShieldPlan r = service.approve(200L, "zhao", "再签一次");
        assertEquals(1, r.getStatus().intValue(), "已通过不得再流转");
        assertEquals(2, r.getNodeNo().intValue(), "环节不变");
        verify(mapper, never()).update(any(), any());
    }

    @Test
    public void rollback_resets_votes_and_steps_back() {
        when(mapper.selectById(200L)).thenReturn(plan(2, 1, 3, 2, 0));
        TRadShieldPlan r = service.rollback(200L, "材料不全");
        assertEquals(1, r.getNodeNo().intValue(), "退回上一环节");
        assertEquals(0, r.getSignCount().intValue(), "退回后票数清零");
        assertEquals(0, r.getStatus().intValue(), "仍在批");
    }

    @Test
    public void last_node_full_countersign_passes() {
        when(mapper.selectById(200L)).thenReturn(plan(2, 1, 2, 1, 0));
        TRadShieldPlan r = service.approve(200L, "chen", "同意");
        assertEquals(1, r.getStatus().intValue(), "末环节会签满员判通过");
        assertEquals(2, r.getNodeNo().intValue(), "末环节通过后环节不动");
    }

    @Test
    public void vetoed_plan_cannot_be_rejected_again() {
        when(mapper.selectById(200L)).thenReturn(plan(1, 0, 1, 0, 2));
        TRadShieldPlan r = service.reject(200L, "sun", "再来一次否决");
        assertEquals(2, r.getStatus().intValue());
        verify(mapper, never()).update(any(), any());
    }

    @Test
    public void missing_approver_rejected() {
        when(mapper.selectById(200L)).thenReturn(plan(0, 0, 1, 0, 0));
        assertNull(service.approve(200L, "  ", "x"));
        verify(mapper, never()).update(any(), any());
    }
}
