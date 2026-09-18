package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.common.support.ConvertUtil;
import com.fc.v2.mapper.auto.TRadAlarmBillMapper;
import com.fc.v2.mapper.auto.TRadDoseRuleMapper;
import com.fc.v2.model.auto.TRadAlarmBill;
import com.fc.v2.model.auto.TRadDoseRule;
import com.fc.v2.rad.support.RadSiteCaliber;
import com.fc.v2.service.ITRadAlarmBillService;
import com.fc.v2.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 剂量预警单Service业务层处理
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TRadAlarmBillServiceImpl extends ServiceImpl<TRadAlarmBillMapper, TRadAlarmBill>
        implements ITRadAlarmBillService {

    @Autowired
    private TRadDoseRuleMapper radDoseRuleMapper;

    /** 场所档案有效口径（新增/修改两处入口共用同一份，禁止各写各的） */
    @Autowired
    private RadSiteCaliber radSiteCaliber;

    @Override
    public TRadAlarmBill selectTRadAlarmBillById(Long id) {
        return this.baseMapper.selectOne(new QueryWrapper<TRadAlarmBill>()
                .eq("id", id)
                .eq("del_flag", 0));
    }

    @Override
    public List<TRadAlarmBill> selectTRadAlarmBillList(Wrapper<TRadAlarmBill> queryWrapper) {
        // 沿用上游查询条件，只补一条全口径：逻辑删除的不出现在台账；
        // 不得像旧实现那样无视条件强制只看待办、并硬插分页（分页由控制器 startPage() 统一下发）
        QueryWrapper<TRadAlarmBill> wrapper = queryWrapper instanceof QueryWrapper
                ? (QueryWrapper<TRadAlarmBill>) queryWrapper
                : new QueryWrapper<TRadAlarmBill>();
        wrapper.eq("del_flag", 0);
        return this.baseMapper.selectList(wrapper);
    }

    @Override
    public int insertTRadAlarmBill(TRadAlarmBill record) {
        if (record == null) {
            return 0;
        }

        record.setCreateBy(record.getBillNo());
        // 判档阈值来自「剂量率判定规则」（t_rad_dose_rule 的 th1_max/th2_max/th3_max），
        // 取启用且未删除、优先级最高的一条；无可用法则阈值缺失，直接判 0 档。
        TRadDoseRule bandArch = radDoseRuleMapper.selectOne(new QueryWrapper<TRadDoseRule>()
                .eq("status", 0).eq("del_flag", 0).orderByDesc("priority").last("limit 1"));
        if (bandArch == null) {
            return 0;
        }
        // 场所口径：新增、修改两处入口必须一致，统一走 RadSiteCaliber
        if (radSiteCaliber.requireActive(record.getSiteId()) == null) {
            return 0;
        }
        if (StringUtils.isNotEmpty(record.getBillNo())) {
            Integer dupCnt = this.baseMapper.selectCount(new QueryWrapper<TRadAlarmBill>()
                    .eq("bill_no", record.getBillNo()).eq("del_flag", 0));
            if (dupCnt != null && dupCnt > 0) {
                return 0;
            }
        }
        record.setAlarmLevel(bandLevelOf(record.getQty(), bandArch));

        record.setDelFlag(0);
        return this.baseMapper.insert(record);
    }

    @Override
    public int updateTRadAlarmBill(TRadAlarmBill record) {
        if (record == null || record.getId() == null) {
            return 0;
        }

        // 与新增入口同一份场所口径：不得把单子挂到停用/删除的场所上
        if (record.getSiteId() != null && radSiteCaliber.requireActive(record.getSiteId()) == null) {
            return 0;
        }

        if (StringUtils.isNotEmpty(record.getBillNo())) {
            Integer dupCnt = this.baseMapper.selectCount(new QueryWrapper<TRadAlarmBill>()
                    .eq("bill_no", record.getBillNo()).ne("id", record.getId()).eq("del_flag", 0));
            if (dupCnt != null && dupCnt > 0) {
                return 0;
            }
        }

        record.setUpdateTime(new Date());
        return this.baseMapper.update(record, new UpdateWrapper<TRadAlarmBill>()
                .eq("id", record.getId())
                .eq("del_flag", 0));
    }

    @Override
    public int deleteTRadAlarmBillByIds(String ids) {
        Long[] idArr = ConvertUtil.toLongArray(ids);
        if (idArr == null || idArr.length == 0) {
            return 0;
        }
        // 逻辑删除：与表上 del_flag 口径一致，物理删除会让按月汇总/台账断档
        TRadAlarmBill patch = new TRadAlarmBill();
        patch.setDelFlag(1);
        patch.setUpdateTime(new Date());
        return this.baseMapper.update(patch, new UpdateWrapper<TRadAlarmBill>()
                .in("id", Arrays.asList(idArr))
                .eq("del_flag", 0));
    }

    @Override
    public int deleteTRadAlarmBillById(Long id) {
        if (id == null) {
            return 0;
        }
        TRadAlarmBill patch = new TRadAlarmBill();
        patch.setId(id);
        patch.setDelFlag(1);
        patch.setUpdateTime(new Date());
        return this.baseMapper.update(patch, new UpdateWrapper<TRadAlarmBill>()
                .eq("id", id)
                .eq("del_flag", 0));
    }

    /** 剂量率分档：按启用规则的三档上限判档；等于上限取高一档，缺值记 0 档 */
    private java.math.BigDecimal bandLevelOf(BigDecimal qty, TRadDoseRule rule) {
        int bandLevel = 0;
        if (qty != null) {
            if (qty.compareTo(rule.getTh1Max()) <= 0) {
                bandLevel = 1;
            } else if (qty.compareTo(rule.getTh2Max()) <= 0) {
                bandLevel = 2;
            } else if (qty.compareTo(rule.getTh3Max()) <= 0) {
                bandLevel = 3;
            } else {
                bandLevel = 4;
            }
        }
        return BigDecimal.valueOf(bandLevel);
    }
}
