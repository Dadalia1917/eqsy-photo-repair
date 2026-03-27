package com.ruoyi.system.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.RepairTaskMapper;

@Service
public class RepairVideoService
{
    @Autowired
    private RepairTaskMapper repairTaskMapper;

    public int updateResultVideoUrl(Long taskId, String resultVideoUrl)
    {
        if (taskId == null)
        {
            return 0;
        }
        return repairTaskMapper.updateResultVideoUrl(taskId, resultVideoUrl);
    }
}
