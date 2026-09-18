package com.fc.v2.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.mapper.auto.TRadSiteMapper;
import com.fc.v2.model.auto.TRadSite;
import com.fc.v2.service.ITRadSiteService;

/**
 * 辐射工作场所档案 Service业务层处理。
 *
 * 「有效可引用场所」口径的唯一实现，预警单新增、月度汇总等所有涉及场所档案的
 * 入口都走 {@link #selectActiveSite(Long)}，避免一处一个说法。
 *
 * 存量兼容：历史档案 status 可能为空，空值按「在用」对待（与建表默认语义一致），
 * 该兼容口径只在此处生效一次，各入口复用、不得各写各的。
 *
 * @author fuce
 * @date 2026-09-18
 */
@Service
public class TRadSiteServiceImpl extends ServiceImpl<TRadSiteMapper, TRadSite> implements ITRadSiteService {

    /** 档案状态：在用 */
    private static final int STATUS_IN_USE = 0;
    /** 删除标记：正常 */
    private static final int DEL_FLAG_NORMAL = 0;

    @Override
    public TRadSite selectTRadSiteById(Long id) {
        if (id == null) {
            return null;
        }
        return this.baseMapper.selectById(id);
    }

    @Override
    public TRadSite selectActiveSite(Long id) {
        if (id == null) {
            return null;
        }
        // 唯一口径：未删除 且 (在用 或 历史空值)
        return this.baseMapper.selectOne(new QueryWrapper<TRadSite>()
                .eq("id", id)
                .eq("del_flag", DEL_FLAG_NORMAL)
                .and(w -> w.eq("status", STATUS_IN_USE).or().isNull("status"))
                .last("limit 1"));
    }

    @Override
    public List<TRadSite> selectTRadSiteList(Wrapper<TRadSite> queryWrapper) {
        return this.baseMapper.selectList(queryWrapper);
    }
}
