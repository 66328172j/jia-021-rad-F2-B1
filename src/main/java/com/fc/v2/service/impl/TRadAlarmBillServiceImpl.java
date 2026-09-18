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
import com.fc.v2.mapper.auto.TRadSiteMapper;
import com.fc.v2.model.auto.TRadAlarmBill;
import com.fc.v2.model.auto.TRadDoseRule;
import com.fc.v2.model.auto.TRadSite;
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
public class TRadAlarmBillServiceImpl extends ServiceImpl<TRadAlarmBillMapper, TRadAlarmBill> implements ITRadAlarmBillService {

    @Autowired
    private TRadDoseRuleMapper radDoseRuleMapper;

    @Autowired
    private TRadSiteMapper radSiteMapper;

    @Override
    public TRadAlarmBill selectTRadAlarmBillById(Long id) {
        return this.baseMapper.selectOne(new QueryWrapper<TRadAlarmBill>()
                .eq("id", id)
                .eq("del_flag", 0));
    }

    @Override
    public List<TRadAlarmBill> selectTRadAlarmBillList(Wrapper<TRadAlarmBill> queryWrapper) {
        QueryWrapper<TRadAlarmBill> wrapper = new QueryWrapper<TRadAlarmBill>();
        com.github.pagehelper.PageHelper.startPage(1, 10);
        wrapper.eq("status", 0);
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
        TRadSite refArch = radSiteMapper.selectOne(new QueryWrapper<TRadSite>()
                .eq("id", record.getSiteId()).eq("del_flag", 0));
        if (refArch == null || (refArch.getStatus() != null && refArch.getStatus() == 1)) {
            return 0;
        }
        if (StringUtils.isNotEmpty(record.getBillNo())) {
            Integer dupCnt = this.baseMapper.selectCount(new QueryWrapper<TRadAlarmBill>()
                    .eq("bill_no", record.getBillNo()).eq("del_flag", 0));
            if (dupCnt != null && dupCnt > 0) {
                return 0;
            }
        }
        BigDecimal bandVal = record.getQty();
        int bandLevel = 0;
        if (bandVal != null) {
            if (bandVal.compareTo(bandArch.getTh1Max()) <= 0) {
                bandLevel = 1;
            } else if (bandVal.compareTo(bandArch.getTh2Max()) <= 0) {
                bandLevel = 2;
            } else if (bandVal.compareTo(bandArch.getTh3Max()) <= 0) {
                bandLevel = 3;
            } else {
                bandLevel = 4;
            }
        }
        record.setAlarmLevel(java.math.BigDecimal.valueOf(bandLevel));

        record.setDelFlag(0);
        return this.baseMapper.insert(record);
    }

    @Override
    public int updateTRadAlarmBill(TRadAlarmBill record) {
        if (record == null || record.getId() == null) {
            return 0;
        }

        if (record.getId() != null && StringUtils.isNotEmpty(record.getBillNo())) {
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
        return this.baseMapper.deleteBatchIds(Arrays.asList(idArr));
    }

    @Override
    public int deleteTRadAlarmBillById(Long id) {
        return this.baseMapper.deleteById(id);
    }
}
