package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 场所剂量按月汇总对象 t_rad_dose_summary
 *
 * @author fuce
 * @date 2026-09-17
 */
@TableName("t_rad_dose_summary")
@ApiModel(value = "TRadDoseSummary", description = "场所剂量按月汇总")
public class TRadDoseSummary implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 统计月份 yyyy-MM */
    @TableField("period")
    @ApiModelProperty(value = "统计月份 yyyy-MM")
    private String period;

    /** 场所/机房（对齐 t_rad_site.id：bigint） */
    @TableField("site_id")
    @ApiModelProperty(value = "场所/机房")
    private Long siteId;

    /** 剂量合计(mSv) */
    @TableField("total_qty")
    @ApiModelProperty(value = "剂量合计(mSv)")
    private BigDecimal totalQty;

    /** 参与汇总的明细行数 */
    @TableField("row_count")
    @ApiModelProperty(value = "参与汇总的明细行数")
    private Integer rowCount;

    /** 汇总状态 0待生成 1已生成 */
    @TableField("status")
    @ApiModelProperty(value = "汇总状态 0待生成 1已生成")
    private Integer status;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建人 */
    @TableField("create_by")
    @ApiModelProperty(value = "创建人")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新人 */
    @TableField("update_by")
    @ApiModelProperty(value = "更新人")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public Long getSiteId() { return siteId; }
    public void setSiteId(Long siteId) { this.siteId = siteId; }
    public BigDecimal getTotalQty() { return totalQty; }
    public void setTotalQty(BigDecimal totalQty) { this.totalQty = totalQty; }
    public Integer getRowCount() { return rowCount; }
    public void setRowCount(Integer rowCount) { this.rowCount = rowCount; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getDelFlag() { return delFlag; }
    public void setDelFlag(Integer delFlag) { this.delFlag = delFlag; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
}