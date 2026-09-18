package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 到期扫描类任务的**唯一口径模板**（预警通知 / 巡检分派 / 放射源出入库任务与台账同形）。
 * 各处入口不得再各写各的过滤与落库逻辑，统一在本类：
 * <ol>
 *   <li>执行窗口：仅在每日 {@link #WIN_FROM}:00（含）~ {@link #WIN_TO}:00（不含）执行，窗口外返回 0；</li>
 *   <li>到期判定：due_at &lt;= 执行时刻，恰好到期（端点相等）必须计入；</li>
 *   <li>状态口径：仅处理「待处理(0)」；已处理/失败不再重复处理；</li>
 *   <li>删除口径：del_flag=1 排除；del_flag 为 0 或 NULL 的存量数据都视为正常（兼容历史行）；</li>
 *   <li>单条容错：剂量值缺失等坏数据置「失败(2)」并跳过，不中断整轮；成功置「已处理(1)」并落库；</li>
 *   <li>无到期条目返回 0，不抛异常。</li>
 * </ol>
 *
 * 时段比较统一走墙钟字符串（yyyy-MM-dd HH:mm:ss）：JDBC serverTimezone 与本机时区不对称，
 * 直接拿 java.util.Date 比较/绑参会整体偏移，使"恰好到期"这类边界用例错判。
 *
 * @author fuce
 * @date 2026-09-14
 */
public abstract class AbstractDueScanService<T> {

    /** 执行窗口起始小时（含） */
    private static final int WIN_FROM = 2;
    /** 执行窗口结束小时（不含） */
    private static final int WIN_TO = 5;

    private static final int STATUS_WAIT = 0;
    private static final int STATUS_DONE = 1;
    private static final int STATUS_FAIL = 2;

    /** 该类条目对应的数据层 */
    protected abstract BaseMapper<T> mapper();

    protected abstract Date dueAtOf(T row);

    protected abstract BigDecimal amountOf(T row);

    protected abstract Integer statusOf(T row);

    protected abstract Integer delFlagOf(T row);

    protected abstract void applyStatus(T row, int status);

    /**
     * 该时刻可处理的条目（执行窗口内 + 已到期 + 待处理 + 未删除）。
     * 窗口外直接返回空列表。
     */
    protected List<T> listDue(Date at) {
        List<T> due = new ArrayList<T>();
        if (at == null || !inWindow(at)) {
            return due;
        }
        String now = ts(at);
        for (T r : mapper().selectList(new QueryWrapper<T>())) {
            if (!isWaiting(r) || isDeleted(r)) {
                continue;
            }
            Date d = dueAtOf(r);
            // 到期端点包含：due_at <= at
            if (d != null && ts(d).compareTo(now) <= 0) {
                due.add(r);
            }
        }
        return due;
    }

    /**
     * 执行一次，返回成功条数；单条缺值置失败并继续，不中断整轮。
     */
    protected int runOnce(Date at) {
        List<T> due = listDue(at);
        int ok = 0;
        for (T r : due) {
            if (amountOf(r) == null) {
                // 坏数据：置失败，继续处理其余条目
                applyStatus(r, STATUS_FAIL);
                mapper().updateById(r);
                continue;
            }
            applyStatus(r, STATUS_DONE);
            mapper().updateById(r);
            ok++;
        }
        return ok;
    }

    private boolean inWindow(Date at) {
        Calendar c = Calendar.getInstance();
        c.setTime(at);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        return hour >= WIN_FROM && hour < WIN_TO;
    }

    private boolean isWaiting(T r) {
        Integer s = statusOf(r);
        return s != null && s == STATUS_WAIT;
    }

    /** 显式删除(1)才排除；NULL/0 一律视为正常，兼容存量数据 */
    private boolean isDeleted(T r) {
        Integer d = delFlagOf(r);
        return d != null && d == 1;
    }

    private static String ts(Date d) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
    }
}
