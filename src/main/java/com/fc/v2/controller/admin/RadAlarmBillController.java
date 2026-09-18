package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TRadAlarmBill;
import com.fc.v2.service.ITRadAlarmBillService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 剂量预警单 Controller
 *
 * @author fuce
 * @date 2026-09-12
 */
@Api(value = "剂量预警单")
@Controller
@RequestMapping("/RadAlarmBillController")
public class RadAlarmBillController extends BaseController {

    private final String prefix = "admin/radAlarmBill";

    @Autowired
    private ITRadAlarmBillService radAlarmBillService;

    @ApiOperation(value = "分页跳转", notes = "分页跳转")
    @GetMapping("/view")
    @RequiresPermissions("rad:radAlarmBill:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "剂量预警单集合查询", action = "list")
    @ApiOperation(value = "分页查询", notes = "分页查询")
    @GetMapping("/list")
    @RequiresPermissions("rad:radAlarmBill:list")
    @ResponseBody
    public ResultTable list(TRadAlarmBill record) {
        // 以页面查询条件构造 wrapper（有效单据口径 del_flag=0 由 service 统一兜底）
        QueryWrapper<TRadAlarmBill> queryWrapper = new QueryWrapper<TRadAlarmBill>(record);
        startPage();
        com.github.pagehelper.PageInfo<TRadAlarmBill> page =
                new com.github.pagehelper.PageInfo<TRadAlarmBill>(radAlarmBillService.selectTRadAlarmBillList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "剂量预警单新增", action = "add")
    @ApiOperation(value = "新增", notes = "新增")
    @PostMapping("/add")
    @RequiresPermissions("rad:radAlarmBill:add")
    @ResponseBody
    public AjaxResult add(TRadAlarmBill record) {
        return toAjax(radAlarmBillService.insertTRadAlarmBill(record));
    }

    @Log(title = "剂量预警单修改", action = "edit")
    @ApiOperation(value = "修改保存", notes = "修改保存")
    @PostMapping("/edit")
    @RequiresPermissions("rad:radAlarmBill:edit")
    @ResponseBody
    public AjaxResult editSave(TRadAlarmBill record) {
        return toAjax(radAlarmBillService.updateTRadAlarmBill(record));
    }

    @Log(title = "剂量预警单删除", action = "remove")
    @ApiOperation(value = "删除", notes = "删除")
    @DeleteMapping("/remove")
    @RequiresPermissions("rad:radAlarmBill:remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        return toAjax(radAlarmBillService.deleteTRadAlarmBillByIds(ids));
    }
}
