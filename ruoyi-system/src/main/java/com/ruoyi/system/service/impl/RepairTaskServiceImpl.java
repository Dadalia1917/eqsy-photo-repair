package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.RepairTask;
import com.ruoyi.system.domain.vo.RepairTrendVO;
import com.ruoyi.system.mapper.RepairTaskMapper;
import com.ruoyi.system.service.IRepairTaskService;

@Service
public class RepairTaskServiceImpl implements IRepairTaskService
{
    private static final String MODE_MANUAL = "MANUAL";
    private static final String STATUS_WAIT_STUDENT = "WAIT_STUDENT";
    private static final String STATUS_MANUAL_PROCESSING = "MANUAL_PROCESSING";
    private static final String STATUS_COMPLETED = "COMPLETED";

    @Autowired
    private RepairTaskMapper repairTaskMapper;

    @Override
    public RepairTask selectRepairTaskById(Long taskId)
    {
        return repairTaskMapper.selectRepairTaskById(taskId);
    }

    @Override
    public List<RepairTask> selectRepairTaskList(RepairTask repairTask)
    {
        return repairTaskMapper.selectRepairTaskList(repairTask);
    }

    @Override
    public Long submitTask(RepairTask repairTask, Long userId, String userName)
    {
        repairTask.setTaskNo(buildTaskNo());
        repairTask.setUserId(userId);
        repairTask.setUserName(userName);
        repairTask.setProgress(5);
        repairTask.setCreateBy(userName);
        repairTask.setCreateTime(new Date());

        repairTask.setRepairMode(MODE_MANUAL);
        repairTask.setTaskType(MODE_MANUAL);
        repairTask.setStatus(STATUS_WAIT_STUDENT);

        repairTaskMapper.insertRepairTask(repairTask);
        return repairTask.getTaskId();
    }

    @Override
    public boolean claimTask(Long taskId, Long studentId, String studentName)
    {
        return repairTaskMapper.claimTask(taskId, studentId, studentName, studentName) > 0;
    }

    @Override
    public int uploadManualResult(Long taskId, Long studentId, String resultUrls)
    {
        RepairTask db = repairTaskMapper.selectRepairTaskById(taskId);
        if (db == null || db.getStudentId() == null || !db.getStudentId().equals(studentId))
        {
            return 0;
        }
        db.setResultUrls(resultUrls);
        db.setStatus(STATUS_MANUAL_PROCESSING);
        db.setProgress(85);
        db.setUpdateBy(db.getStudentName());
        db.setUpdateTime(new Date());
        return repairTaskMapper.updateRepairTask(db);
    }

    @Override
    public int finishManualTask(Long taskId, Long studentId)
    {
        RepairTask db = repairTaskMapper.selectRepairTaskById(taskId);
        if (db == null || db.getStudentId() == null || !db.getStudentId().equals(studentId)
                || StringUtils.isBlank(db.getResultUrls()))
        {
            return 0;
        }
        db.setStatus(STATUS_COMPLETED);
        db.setProgress(100);
        db.setFinishedTime(new Date());
        db.setUpdateBy(db.getStudentName());
        db.setUpdateTime(new Date());
        return repairTaskMapper.updateRepairTask(db);
    }

    @Override
    public int triggerAiTask(Long taskId)
    {
        return 0;
    }

    @Override
    public List<RepairTrendVO> selectDailyTrend(Integer days)
    {
        int safeDays = (days == null || days < 1) ? 7 : Math.min(days, 60);
        List<RepairTrendVO> raw = repairTaskMapper.selectDailyTrend(safeDays);
        Map<String, RepairTrendVO> map = new HashMap<>();
        for (RepairTrendVO item : raw)
        {
            map.put(item.getDay(), item);
        }

        List<RepairTrendVO> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate start = LocalDate.now().minusDays(safeDays - 1L);
        for (int i = 0; i < safeDays; i++)
        {
            String day = start.plusDays(i).format(formatter);
            RepairTrendVO vo = map.get(day);
            if (vo == null)
            {
                vo = new RepairTrendVO();
                vo.setDay(day);
                vo.setTotalCount(0);
                vo.setCompletedCount(0);
            }
            result.add(vo);
        }
        return result;
    }

    private String buildTaskNo()
    {
        return "EQSY" + System.currentTimeMillis() + RandomStringUtils.randomNumeric(4);
    }

    @Override
    public Long selectTotalCompleted() {
        return repairTaskMapper.selectTotalCompleted();
    }
}

