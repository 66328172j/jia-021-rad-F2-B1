package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TRadDispose;
import com.fc.v2.service.ITRadDisposeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 分区剂量异常处置单 Controller（state-machine 形状：流转入口）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Api(value = "分区剂量异常处置单")
@Controller
@RequestMapping("/radDispose")
public class RadDisposeController extends BaseController {

    private final String prefix = "admin/radDispose";

    @Autowired
    private ITRadDisposeService radDisposeService;

    @ApiOperation(value = "流转台账跳转", notes = "流转台账跳转")
    @GetMapping("/view")
    @RequiresPermissions("radDispose:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "分区剂量异常处置单流转台账", action = "list")
    @ApiOperation(value = "流转台账", notes = "流转台账")
    @GetMapping("/list")
    @RequiresPermissions("radDispose:list")
    @ResponseBody
    public ResultTable list(TRadDispose record) {
        QueryWrapper<TRadDispose> queryWrapper = new QueryWrapper<TRadDispose>();
        startPage();
        com.github.pagehelper.PageInfo<TRadDispose> page =
                new com.github.pagehelper.PageInfo<TRadDispose>(radDisposeService.selectTRadDisposeList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "分区剂量异常处置单推进", action = "advance")
    @ApiOperation(value = "推进一档", notes = "推进一档")
    @PostMapping("/advance")
    @RequiresPermissions("radDispose:advance")
    @ResponseBody
    public AjaxResult advance(Long id, String remark) {
        return toAjax(radDisposeService.advance(id, remark) != null ? 1 : 0);
    }

    @Log(title = "分区剂量异常处置单回退", action = "rollback")
    @ApiOperation(value = "回退一档", notes = "回退一档")
    @PostMapping("/rollback")
    @RequiresPermissions("radDispose:rollback")
    @ResponseBody
    public AjaxResult rollback(Long id, String remark) {
        return toAjax(radDisposeService.rollback(id, remark) != null ? 1 : 0);
    }
}
