package com.fc.v2.common.quartz.task;

import com.fc.v2.service.ITRadDoseSummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

/**
 * 场所剂量按月汇总 定时调度入口。
 * 在定时任务后台配置 invokeTarget 即可：
 *   每月跑上月汇总：radDoseSummaryTask.rebuildLastMonth()
 *   重算指定月份（含存量历史月份）：radDoseSummaryTask.rebuildMonth('2026-08')
 * 同月重复执行覆盖上次结果，不会累加。
 *
 * @author fuce
 * @date 2026-09-17
 */
@Component("radDoseSummaryTask")
public class RadDoseSummaryTask {

    @Autowired
    private ITRadDoseSummaryService radDoseSummaryService;

    /**
     * 每月汇总：默认重算上一个月（月初跑批时上月数据已齐）
     */
    public void rebuildLastMonth() {
        String period = YearMonth.now().minusMonths(1).toString();
        int rows = radDoseSummaryService.rebuild(period);
        System.out.println("场所剂量按月汇总完成 period=" + period + " 写出行数=" + rows);
    }

    /**
     * 重算指定月份
     * @param period 统计月份，格式 yyyy-MM（如 2026-08）
     */
    public void rebuildMonth(String period) {
        int rows = radDoseSummaryService.rebuild(period);
        System.out.println("场所剂量按月汇总完成 period=" + period + " 写出行数=" + rows);
    }
}
