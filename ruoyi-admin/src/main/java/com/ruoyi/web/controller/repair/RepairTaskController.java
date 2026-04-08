package com.ruoyi.web.controller.repair;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.RepairTask;
import com.ruoyi.system.domain.vo.RepairTrendVO;
import com.ruoyi.system.service.IRepairTaskService;
import com.ruoyi.system.service.RepairVideoService;

/**
 * 后台修复任务管理（管理员/学生）
 */
@RestController
@RequestMapping("/repair/task")
public class RepairTaskController extends BaseController
{
    @Autowired
    private IRepairTaskService repairTaskService;

    @Autowired
    private RepairVideoService repairVideoService;

    @PreAuthorize("@ss.hasAnyPermi('repair:task:list,repair:task:claim')")
    @GetMapping("/list")
    public TableDataInfo list(RepairTask repairTask)
    {
        startPage();
        List<RepairTask> list = repairTaskService.selectRepairTaskList(repairTask);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('repair:task:query')")
    @GetMapping("/{taskId:\\d+}")
    public AjaxResult getInfo(@PathVariable Long taskId)
    {
        return success(repairTaskService.selectRepairTaskById(taskId));
    }

    @PreAuthorize("@ss.hasAnyPermi('repair:task:list,repair:task:claim')")
    @GetMapping("/trend")
    public AjaxResult trend(Integer days)
    {
        List<RepairTrendVO> rows = repairTaskService.selectDailyTrend(days);
        return success(rows);
    }

    /**
     * 获取累计修复完成数 (允许前台免登录或基础登录调用)
     */
    @GetMapping("/stats/total")
    public AjaxResult totalCompleted()
    {
        return success(repairTaskService.selectTotalCompleted());
    }

    @PreAuthorize("@ss.hasPermi('repair:task:claim')")
    @Log(title = "修复任务", businessType = BusinessType.UPDATE)
    @PostMapping("/claim/{taskId}")
    public AjaxResult claim(@PathVariable Long taskId)
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        boolean ok = repairTaskService.claimTask(taskId, loginUser.getUserId(), loginUser.getUsername());
        return ok ? success("认领成功") : error("认领失败，任务可能已被其他同学认领");
    }

    @PreAuthorize("@ss.hasPermi('repair:task:edit')")
    @Log(title = "修复任务", businessType = BusinessType.UPDATE)
    @PutMapping("/manual/result")
    public AjaxResult uploadResult(@RequestBody RepairTask body)
    {
        if (body.getTaskId() == null || StringUtils.isBlank(body.getResultUrls()))
        {
            return error("参数不完整");
        }
        LoginUser loginUser = SecurityUtils.getLoginUser();
        int rows = repairTaskService.uploadManualResult(body.getTaskId(), loginUser.getUserId(), body.getResultUrls());
        return toAjax(rows);
    }

    @PreAuthorize("@ss.hasPermi('repair:task:edit')")
    @Log(title = "修复任务", businessType = BusinessType.UPDATE)
    @PutMapping("/manual/video")
    public AjaxResult uploadVideo(@RequestBody java.util.Map<String, Object> body)
    {
        Object taskIdObj = body.get("taskId");
        Object videoUrlObj = body.get("resultVideoUrl");
        if (taskIdObj == null)
        {
            return error("taskId 不能为空");
        }
        Long taskId = Long.valueOf(taskIdObj.toString());
        String resultVideoUrl = videoUrlObj != null ? videoUrlObj.toString() : null;
        int rows = repairVideoService.updateResultVideoUrl(taskId, resultVideoUrl);
        return toAjax(rows);
    }

    @PreAuthorize("@ss.hasPermi('repair:task:edit')")
    @Log(title = "修复任务", businessType = BusinessType.UPDATE)
    @PutMapping("/manual/finish/{taskId}")
    public AjaxResult finish(@PathVariable Long taskId)
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        int rows = repairTaskService.finishManualTask(taskId, loginUser.getUserId());
        return rows > 0 ? success("已完成并回传社区端") : error("完成失败，请先上传修复结果");
    }

    @PreAuthorize("@ss.hasPermi('repair:task:ai')")
    @Log(title = "修复任务", businessType = BusinessType.UPDATE)
    @PostMapping("/ai/trigger/{taskId}")
    public AjaxResult triggerAi(@PathVariable Long taskId)
    {
        return error("AI通道已下线，请使用志愿者处理流程");
    }
}
