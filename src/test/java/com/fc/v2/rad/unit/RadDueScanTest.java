package com.fc.v2.rad.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fc.v2.mapper.auto.TRadAlarmItemMapper;
import com.fc.v2.model.auto.TRadAlarmItem;
import com.fc.v2.service.impl.TRadAlarmItemServiceImpl;

/** 到期扫描统一口径（窗口/端点/状态/容错）的纯单元验证。 */
@ExtendWith(MockitoExtension.class)
public class RadDueScanTest {

    @Mock
    private TRadAlarmItemMapper mapper;

    @InjectMocks
    private TRadAlarmItemServiceImpl service;

    private static Timestamp at(String s) {
        return Timestamp.valueOf(s);
    }

    private TRadAlarmItem item(String no, String dueAt, String amount, int status) {
        TRadAlarmItem r = new TRadAlarmItem();
        r.setItemNo(no);
        r.setDueAt(at(dueAt));
        r.setStatus(status);
        r.setDelFlag(0);
        r.setAmount(amount == null ? null : new BigDecimal(amount));
        return r;
    }

    @Test
    public void out_of_window_processes_nothing() {
        org.mockito.Mockito.lenient().when(mapper.selectList(any())).thenReturn(Arrays.asList(
                item("W1", "2026-09-14 03:00:00", "5", 0)));
        assertEquals(0, service.runOnce(at("2026-09-14 08:00:00")), "窗口外（8点）不处理");
        verify(mapper, times(0)).updateById(any());
    }

    @Test
    public void due_at_boundary_is_included() {
        TRadAlarmItem r = item("D2", "2026-09-14 03:00:00", "5", 0);
        when(mapper.selectList(any())).thenReturn(Arrays.asList(r));
        assertEquals(1, service.runOnce(at("2026-09-14 03:00:00")), "恰好到期必须计入");
        assertEquals(1, r.getStatus().intValue(), "处理完落已处理");
    }

    @Test
    public void already_processed_is_skipped() {
        TRadAlarmItem a = item("S3A", "2026-09-14 03:00:00", "5", 0);
        TRadAlarmItem b = item("S3B", "2026-09-14 03:00:00", "5", 1);
        when(mapper.selectList(any())).thenReturn(Arrays.asList(a, b));
        assertEquals(1, service.runOnce(at("2026-09-14 03:30:00")), "已处理的不重复处理");
        assertEquals(1, a.getStatus().intValue());
        assertEquals(1, b.getStatus().intValue());
    }

    @Test
    public void bad_amount_marked_fail_and_run_continues() {
        TRadAlarmItem a = item("E4A", "2026-09-14 03:00:00", "5", 0);
        TRadAlarmItem bad = item("E4B", "2026-09-14 03:00:00", null, 0);
        when(mapper.selectList(any())).thenReturn(Arrays.asList(a, bad));
        assertEquals(1, service.runOnce(at("2026-09-14 03:30:00")), "坏数据不中断整轮");
        assertEquals(1, a.getStatus().intValue());
        assertEquals(2, bad.getStatus().intValue(), "缺值置失败");
        verify(mapper, times(2)).updateById(any());
    }

    @Test
    public void nothing_due_returns_zero() {
        when(mapper.selectList(any())).thenReturn(Arrays.<TRadAlarmItem>asList());
        assertEquals(0, service.runOnce(at("2026-09-14 03:30:00")), "空列表返回 0 不抛异常");
    }

    @Test
    public void not_yet_due_is_skipped() {
        when(mapper.selectList(any())).thenReturn(Arrays.asList(
                item("F1", "2026-09-14 04:00:00", "5", 0)));
        assertEquals(0, service.runOnce(at("2026-09-14 03:30:00")), "未到期不处理");
    }
}
