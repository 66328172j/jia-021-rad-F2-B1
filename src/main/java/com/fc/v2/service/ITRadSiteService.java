package com.fc.v2.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.model.auto.TRadSite;

/**
 * 辐射工作场所档案 Service接口。
 *
 * 凡涉及「场所档案」的口径（是否有效、能否被业务单据引用）统一收敛到本接口，
 * 各业务入口（预警单、月度汇总等）不得各自另写一份 status/del_flag 判定。
 *
 * @author fuce
 * @date 2026-09-18
 */
public interface ITRadSiteService {

    /** 按主键查询档案 */
    TRadSite selectTRadSiteById(Long id);

    /**
     * 取一处「可被业务引用」的场所档案：存在且 del_flag=0（未删除）、status=0（在用）。
     * 停用/删除/不存在均返回 null。这是场所档案有效性的唯一口径。
     */
    TRadSite selectActiveSite(Long id);

    /** 列表查询 */
    List<TRadSite> selectTRadSiteList(Wrapper<TRadSite> queryWrapper);
}
