package com.ruoyi.web.controller.app;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.RepairTask;
import com.ruoyi.system.service.IRepairTaskService;

/**
 * 小程序修复任务接口
 */
@RestController
@RequestMapping("/app/repair/task")
public class AppRepairTaskController extends BaseController
{
    private static final int MAX_IMAGE_COUNT = 5;
    private static final String URL_SEPARATOR = ",";

    @Autowired
    private IRepairTaskService repairTaskService;

    @PostMapping("/submit")
    public AjaxResult submit(@RequestBody RepairTask body)
    {
        if (StringUtils.isBlank(body.getRepairMode()) || StringUtils.isBlank(body.getSourceType())
                || StringUtils.isBlank(body.getSourceUrls()))
        {
            return error("提交失败，参数不完整");
        }

        String normalizedSourceUrls = normalizeUrlList(body.getSourceUrls());
        int sourceCount = countUrlItems(normalizedSourceUrls);
        if (sourceCount <= 0)
        {
            return error("提交失败，未检测到有效图片");
        }
        if (sourceCount > MAX_IMAGE_COUNT)
        {
            return error("一次最多上传5张图片");
        }
        body.setSourceUrls(normalizedSourceUrls);

        Long taskId = repairTaskService.submitTask(body, getUserId(), getUsername());
        AjaxResult ajax = success("提交成功");
        ajax.put("taskId", taskId);
        return ajax;
    }

    @GetMapping("/list")
    public TableDataInfo list(RepairTask query)
    {
        query.setUserId(getUserId());
        startPage();
        List<RepairTask> rows = repairTaskService.selectRepairTaskList(query);
        return getDataTable(rows);
    }

    @GetMapping("/{taskId}")
    public AjaxResult detail(@PathVariable Long taskId)
    {
        RepairTask task = repairTaskService.selectRepairTaskById(taskId);
        if (task == null || !getUserId().equals(task.getUserId()))
        {
            return error("任务不存在");
        }
        return success(task);
    }

    private int countUrlItems(String urls)
    {
        String[] items = StringUtils.split(urls, URL_SEPARATOR);
        return items == null ? 0 : items.length;
    }

    private String normalizeUrlList(String urls)
    {
        String[] rawItems = StringUtils.split(urls, URL_SEPARATOR);
        if (rawItems == null || rawItems.length == 0)
        {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String item : rawItems)
        {
            String value = StringUtils.trimToEmpty(item);
            if (StringUtils.isBlank(value))
            {
                continue;
            }
            if (builder.length() > 0)
            {
                builder.append(URL_SEPARATOR);
            }
            builder.append(value);
        }
        return builder.toString();
    }
}
