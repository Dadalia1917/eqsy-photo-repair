package com.ruoyi.system.domain.vo;

/**
 * 修复任务趋势统计
 */
public class RepairTrendVO
{
    private String day;

    private Integer totalCount;

    private Integer completedCount;

    public String getDay()
    {
        return day;
    }

    public void setDay(String day)
    {
        this.day = day;
    }

    public Integer getTotalCount()
    {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount)
    {
        this.totalCount = totalCount;
    }

    public Integer getCompletedCount()
    {
        return completedCount;
    }

    public void setCompletedCount(Integer completedCount)
    {
        this.completedCount = completedCount;
    }
}
