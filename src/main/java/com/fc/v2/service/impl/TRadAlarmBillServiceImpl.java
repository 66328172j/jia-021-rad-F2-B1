package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import com.fc.v2.service.ITRadAlarmBillService;
import com.fc.v2.service.ITRadSiteService;
import com.fc.v2.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 剂量预警单Service业务层处理
 *
 * 场所档案有效性统一走 {@link ITRadSiteService#selectActiveSite(Long)}，
 * 不在本类另写 status/del_flag 判定。
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TRadAlarmBillServiceImpl extends ServiceImpl<TRadAlarmBillMapper, TRadAlarmBill> implements ITRadAlarmBillService {

    /** 已办结（终态）：终态单据锁定，不可再改、不可删除 */
    private static final int STATUS_TERMINAL = 2;
    /** 已处理 */
    private static final int STATUS_HANDLED = 1;
    private static final int DEL_FLAG_NORMAL = 0;
    private static final int DEL_FLAG_DELETED = 1;

    @Autowired
    private TRadDoseRuleMapper radDoseRuleMapper;

    @Autowired
    private ITRadSiteService radSiteService;

    @Override
    public TRadAlarmBill selectTRadAlarmBillById(Long id) {
        return this.baseMapper.selectOne(new QueryWrapper<TRadAlarmBill>()
                .eq("id", id)
                .eq("del_flag", DEL_FLAG_NORMAL));
    }

    @Override
    public List<TRadAlarmBill> selectTRadAlarmBillList(Wrapper<TRadAlarmBill> queryWrapper) {
        // 以调用方条件为准，仅兜底统一「有效单据」口径(del_flag=0)；
        // 不再硬编码 status=0、不再在此强塞分页（分页由 Controller startPage 统一下发），
        // 否则台账口径与月度汇总口径对不上，且分页互相覆盖。
        QueryWrapper<TRadAlarmBill> wrapper = (queryWrapper instanceof QueryWrapper)
                ? (QueryWrapper<TRadAlarmBill>) queryWrapper
                : new QueryWrapper<TRadAlarmBill>();
        wrapper.eq("del_flag", DEL_FLAG_NORMAL);
        return this.baseMapper.selectList(wrapper);
    }

    @Override
    public List<TRadAlarmBill> selectEffectiveForDose(LocalDateTime start, LocalDateTime end) {
        // 唯一取数口径：本月时间窗 [start,end) 内，未作废、且已处理/已办结的上报。
        // 月度汇总 rebuild、按场所钻取、历史月份重算都必须经此方法，确保两处口径一致。
        return this.baseMapper.selectList(new QueryWrapper<TRadAlarmBill>()
                .ge("create_time", start)
                .lt("create_time", end)
                .eq("del_flag", DEL_FLAG_NORMAL)
                .in("status", STATUS_HANDLED, STATUS_TERMINAL));
    }

    @Override
    public int insertTRadAlarmBill(TRadAlarmBill record) {
        if (record == null) {
            return 0;
        }

        // 判档阈值来自「剂量率判定规则」（t_rad_dose_rule 的 th1_max/th2_max/th3_max），
        // 取启用且未删除、优先级最高的一条；无可用法则阈值缺失，直接判 0 档。
        TRadDoseRule bandArch = radDoseRuleMapper.selectOne(new QueryWrapper<TRadDoseRule>()
                .eq("status", 0).eq("del_flag", 0).orderByDesc("priority").last("limit 1"));
        if (bandArch == null) {
            return 0;
        }
        // 场所有效性：唯一口径来自场所档案 Service（存在、未删除、在用）
        if (radSiteService.selectActiveSite(record.getSiteId()) == null) {
            return 0;
        }
        if (StringUtils.isNotEmpty(record.getBillNo())) {
            Integer dupCnt = this.baseMapper.selectCount(new QueryWrapper<TRadAlarmBill>()
                    .eq("bill_no", record.getBillNo()).eq("del_flag", DEL_FLAG_NORMAL));
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

        record.setDelFlag(DEL_FLAG_NORMAL);
        return this.baseMapper.insert(record);
    }

    @Override
    public int updateTRadAlarmBill(TRadAlarmBill record) {
        if (record == null || record.getId() == null) {
            return 0;
        }

        TRadAlarmBill current = this.baseMapper.selectById(record.getId());
        if (current == null
                || (current.getDelFlag() != null && current.getDelFlag() == DEL_FLAG_DELETED)) {
            return 0;
        }
        // 已办结单据锁定，改内容不允许再保存
        if (current.getStatus() != null && current.getStatus() == STATUS_TERMINAL) {
            return 0;
        }

        if (StringUtils.isNotEmpty(record.getBillNo())) {
            Integer dupCnt = this.baseMapper.selectCount(new QueryWrapper<TRadAlarmBill>()
                    .eq("bill_no", record.getBillNo()).ne("id", record.getId())
                    .eq("del_flag", DEL_FLAG_NORMAL));
            if (dupCnt != null && dupCnt > 0) {
                return 0;
            }
        }

        record.setUpdateTime(new Date());
        return this.baseMapper.update(record, new UpdateWrapper<TRadAlarmBill>()
                .eq("id", record.getId())
                .ne("status", STATUS_TERMINAL)
                .eq("del_flag", DEL_FLAG_NORMAL));
    }

    @Override
    public int deleteTRadAlarmBillByIds(String ids) {
        Long[] idArr = ConvertUtil.toLongArray(ids);
        if (idArr == null || idArr.length == 0) {
            return 0;
        }
        // 逻辑删除保留台账，且终态(已办结)单据受锁保护，不允许清掉
        return this.baseMapper.update(null, new UpdateWrapper<TRadAlarmBill>()
                .set("del_flag", DEL_FLAG_DELETED)
                .in("id", Arrays.asList(idArr))
                .ne("status", STATUS_TERMINAL)
                .eq("del_flag", DEL_FLAG_NORMAL));
    }

    @Override
    public int deleteTRadAlarmBillById(Long id) {
        if (id == null) {
            return 0;
        }
        return this.baseMapper.update(null, new UpdateWrapper<TRadAlarmBill>()
                .set("del_flag", DEL_FLAG_DELETED)
                .eq("id", id)
                .ne("status", STATUS_TERMINAL)
                .eq("del_flag", DEL_FLAG_NORMAL));
    }
}
