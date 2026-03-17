package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.RepairTask;
import com.ruoyi.system.domain.vo.RepairTrendVO;

public interface IRepairTaskService
{
    public RepairTask selectRepairTaskById(Long taskId);

    public List<RepairTask> selectRepairTaskList(RepairTask repairTask);

    public Long submitTask(RepairTask repairTask, Long userId, String userName);

    public boolean claimTask(Long taskId, Long studentId, String studentName);

    public int uploadManualResult(Long taskId, Long studentId, String resultUrls);

    public int finishManualTask(Long taskId, Long studentId);

    public int triggerAiTask(Long taskId);

    public List<RepairTrendVO> selectDailyTrend(Integer days);
    /**
     * 统计已完成任务数量
     */
    public Long selectTotalCompleted();
}

