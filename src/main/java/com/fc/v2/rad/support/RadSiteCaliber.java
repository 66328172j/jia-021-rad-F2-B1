package com.fc.v2.rad.support;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TRadSiteMapper;
import com.fc.v2.model.auto.TRadSite;

/**
 * 辐射工作场所档案「有效口径」的**唯一定义处**。
 *
 * <pre>
 * 有效场所 = 档案存在
 *          AND del_flag 不是 1（0=正常；NULL 按正常兼容存量行）
 *          AND status   不是 1（0=在用；NULL 按在用兼容存量行）
 * </pre>
 *
 * 历史上新增预警单、修改预警单、按月汇总等入口对这份口径各写各的，
 * 导致停用/删除/NULL 三种情况各处说法不一。现在所有入口统一走本类：
 * <ul>
 *   <li>{@link #requireActive(Integer)}：写入类入口（新增/修改）严格校验；</li>
 *   <li>{@link #isBlockedSiteId(Integer)}：读取/汇总类入口只排除「档案存在且显式停用或删除」的场所，
 *       档案缺失（历史场所）不否决——这是存量兼容口径，两处的 NULL/缺失处理都以本类为准；</li>
 *   <li>{@link #blockedSiteIds()}：批量汇总时一次取齐，避免逐行查库。</li>
 * </ul>
 * 历史数据迁移脚本（doc/schema 下 rad_site 迁移 SQL）必须把 NULL 回填为 0，与本口径对齐。
 *
 * @author fuce
 * @date 2026-09-18
 */
@Component
public class RadSiteCaliber {

    /** 档案状态：0 在用 */
    public static final int STATUS_IN_USE = 0;
    /** 档案状态：1 停用 */
    public static final int STATUS_SUSPENDED = 1;
    /** 删除标记：0 正常 */
    public static final int DEL_FLAG_NORMAL = 0;
    /** 删除标记：1 删除 */
    public static final int DEL_FLAG_DELETED = 1;

    @javax.annotation.Resource
    private TRadSiteMapper radSiteMapper;

    /**
     * 写入类入口校验：返回场所本身当且仅当它是「在用」场所；
     * 不存在、已停用、已删除、id 为空一律返回 null（调用方按拒绝处理）。
     */
    public TRadSite requireActive(Integer siteId) {
        if (siteId == null) {
            return null;
        }
        TRadSite site = this.radSiteMapper.selectById(siteId.longValue());
        return isActive(site) ? site : null;
    }

    /**
     * 场所当前是否有效（在用、未删除）。NULL 状态/NULL 删除标记按存量数据兼容为在用/正常。
     */
    public boolean isActive(TRadSite site) {
        if (site == null) {
            return false;
        }
        if (site.getDelFlag() != null && site.getDelFlag() == DEL_FLAG_DELETED) {
            return false;
        }
        // 仅显式停用(1)才否决；NULL 视为未回填，按在用兼容
        return site.getStatus() == null || site.getStatus() == STATUS_IN_USE;
    }

    /**
     * 读取/汇总类口径：只有「档案存在且显式停用或删除」的场所才被排除；
     * 档案里查不到的场所（历史数据/迁移残留）不排除，保证存量数据两处口径一致。
     */
    public boolean isBlockedSiteId(Integer siteId) {
        if (siteId == null) {
            return false;
        }
        TRadSite site = this.radSiteMapper.selectById(siteId.longValue());
        return site != null && !isActive(site);
    }

    /**
     * 被显式停用/删除的场所 id 集合（档案缺失的历史场所不在集合内）。供批量汇总一次取齐。
     */
    public Set<Long> blockedSiteIds() {
        Set<Long> blocked = new HashSet<Long>();
        List<TRadSite> all = this.radSiteMapper.selectList(new QueryWrapper<TRadSite>());
        for (TRadSite s : all) {
            if (!isActive(s) && s.getId() != null) {
                blocked.add(s.getId());
            }
        }
        return blocked;
    }
}
