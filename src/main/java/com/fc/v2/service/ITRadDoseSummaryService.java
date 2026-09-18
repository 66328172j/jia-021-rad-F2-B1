package com.fc.v2.service;

import java.util.List;

import com.fc.v2.model.auto.TRadDoseSummary;

/**
 * 场所剂量按月汇总 Service接口（multi-dim-summary 形状：多维汇总与钻取）
 *
 * @author fuce
 * @date 2026-09-17
 */
public interface ITRadDoseSummaryService {

    /** 按月份 + 场所取一条汇总（不存在返回 null） */
    TRadDoseSummary pick(String period, Integer siteId);

    /**
     * 重建某个月的汇总：按场所分组汇总剂量数据，返回**本次写出的汇总行数**。
     * 同月重复调用必须**覆盖**既有汇总（不得累加）；该月无数据返回 0。
     */
    int rebuild(String period);

    /** 该月全部汇总行（按场所升序） */
    List<TRadDoseSummary> listSummary(String period);
}