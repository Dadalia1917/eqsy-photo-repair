package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.RepairTask;
import com.ruoyi.system.domain.vo.RepairTrendVO;

/**
 * 淇��浠诲姟Mapper
 */
public interface RepairTaskMapper
{
    public RepairTask selectRepairTaskById(Long taskId);

    public List<RepairTask> selectRepairTaskList(RepairTask repairTask);

    public int insertRepairTask(RepairTask repairTask);

    public int updateRepairTask(RepairTask repairTask);

    public int updateResultVideoUrl(@Param("taskId") Long taskId, @Param("resultVideoUrl") String resultVideoUrl);

    public List<RepairTrendVO> selectDailyTrend(@Param("days") Integer days);

    public List<RepairTrendVO> selectDailyTrendByUser(@Param("days") Integer days,
            @Param("userId") Long userId, @Param("studentId") Long studentId);

    public int claimTask(@Param("taskId") Long taskId, @Param("studentId") Long studentId,
            @Param("studentName") String studentName, @Param("updateBy") String updateBy);
    /**
     * 统计已完成任务数量
     * @return 数量
     */
    public Long selectTotalCompleted();
}

