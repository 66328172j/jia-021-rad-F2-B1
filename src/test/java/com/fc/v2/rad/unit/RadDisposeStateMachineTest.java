package com.fc.v2.rad.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fc.v2.mapper.auto.TRadDisposeMapper;
import com.fc.v2.model.auto.TRadDispose;
import com.fc.v2.service.impl.TRadDisposeServiceImpl;

/** 处置单状态机修复的纯单元验证（不依赖 Spring/数据库）。 */
@ExtendWith(MockitoExtension.class)
public class RadDisposeStateMachineTest {

    @Mock
    private TRadDisposeMapper mapper;

    @InjectMocks
    private TRadDisposeServiceImpl service;

    @BeforeEach
    public void wire() {
        // 条件落库默认成功（部分用例不会触达，按 lenient 处理）
        org.mockito.Mockito.lenient().when(mapper.update(any(TRadDispose.class), any())).thenReturn(1);
        org.mockito.Mockito.lenient().when(mapper.updateById(any(TRadDispose.class))).thenReturn(1);
    }

    private TRadDispose bill(int stage, int status) {
        TRadDispose r = new TRadDispose();
        r.setId(100L);
        r.setStage(stage);
        r.setStatus(status);
        r.setDelFlag(0);
        return r;
    }

    @Test
    public void advance_should_move_exactly_one_stage() {
        when(mapper.selectById(100L)).thenReturn(bill(1, 1));
        TRadDispose r = service.advance(100L, "现场处置");
        assertEquals(2, r.getStage().intValue(), "一次只许走一格，不得跨格");
        assertEquals(1, r.getStatus().intValue());
    }

    @Test
    public void advance_at_last_stage_should_finish_not_overshoot() {
        when(mapper.selectById(100L)).thenReturn(bill(3, 1));
        TRadDispose r = service.advance(100L, "办结");
        assertEquals(3, r.getStage().intValue(), "末环节不再加环节");
        assertEquals(2, r.getStatus().intValue(), "末环节推进即办结");
    }

    @Test
    public void duplicate_submit_same_action_should_be_rejected() {
        TRadDispose b = bill(1, 1);
        b.setLastAction("现场处置");
        when(mapper.selectById(100L)).thenReturn(b);
        assertNull(service.advance(100L, "现场处置"), "同口径重复提交不得再多走一步");
        assertEquals(1, b.getStage().intValue());
        verify(mapper, never()).update(any(TRadDispose.class), any());
    }

    @Test
    public void finished_bill_should_be_locked_from_advance_and_rollback() {
        when(mapper.selectById(100L)).thenReturn(bill(2, 2));
        assertNull(service.advance(100L, "再推一次"), "已办结不得推进");
        assertNull(service.rollback(100L, "退回"), "已办结不得退回重走");
        verify(mapper, never()).update(any(TRadDispose.class), any());
    }

    @Test
    public void rollback_should_step_back_one_stage_only() {
        when(mapper.selectById(100L)).thenReturn(bill(2, 1));
        TRadDispose r = service.rollback(100L, "材料不全");
        assertEquals(1, r.getStage().intValue(), "一次只回退一格，不得清回首环节");
        assertEquals(1, r.getStatus().intValue(), "退回后仍在办");
    }

    @Test
    public void rollback_at_first_stage_should_be_rejected() {
        when(mapper.selectById(100L)).thenReturn(bill(0, 1));
        assertNull(service.rollback(100L, "退回"));
    }

    @Test
    public void finished_bill_content_should_be_frozen() {
        when(mapper.selectById(100L)).thenReturn(bill(3, 2));
        assertFalse(service.updateContent(100L, "偷偷改内容"), "已办结的单子内容不得再保存");
        verify(mapper, never()).updateById(any(TRadDispose.class));
    }

    @Test
    public void active_bill_content_can_be_saved() {
        when(mapper.selectById(100L)).thenReturn(bill(1, 1));
        assertTrue(service.updateContent(100L, "补充说明"));
    }

    @Test
    public void finished_bill_should_not_be_physically_removed() {
        when(mapper.selectById(100L)).thenReturn(bill(3, 2));
        assertFalse(service.remove(100L), "已办结不得删除，环节链不能断");
        verify(mapper, never()).deleteById(100L);
        verify(mapper, never()).updateById(any(TRadDispose.class));
    }

    @Test
    public void active_bill_removal_is_logical_only() {
        TRadDispose b = bill(1, 1);
        when(mapper.selectById(100L)).thenReturn(b);
        assertTrue(service.remove(100L));
        assertEquals(1, b.getDelFlag().intValue(), "在办单删除只置删除标记，不物理清除");
        verify(mapper, never()).deleteById(100L);
    }
}
