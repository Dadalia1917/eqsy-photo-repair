package com.ruoyi.system.domain;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 修复任务对象 repair_task
 */
public class RepairTask extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long taskId;

    /** 任务编号 */
    private String taskNo;

    /** 用户ID */
    private Long userId;

    /** 用户账号 */
    private String userName;

    /** 修复模式：AI/MANUAL */
    private String repairMode;

    /** 任务类型(BASIC/ANIMATE/HUG) */
    private String taskType;

    /** 源文件类型：IMAGE/VIDEO */
    private String sourceType;

    /** 上传原文件（逗号分隔） */
    private String sourceUrls;

    /** 修复后文件（逗号分隔） */
    private String resultUrls;

    /** 结果视频地址 */
    private String resultVideoUrl;

    /** 任务状态 */
    private String status;

    /** 进度 0-100 */
    private Integer progress;

    /** 分配学生ID */
    private Long studentId;

    /** 分配学生账号 */
    private String studentName;

    /** 错误信息 */
    private String errorMessage;

    /** 完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finishedTime;

    public Long getTaskId()
    {
        return taskId;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }

    public String getTaskNo()
    {
        return taskNo;
    }

    public void setTaskNo(String taskNo)
    {
        this.taskNo = taskNo;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public void setTaskType(String taskType) { this.taskType = taskType; }
    public String getTaskType() { return taskType; }

    public String getRepairMode()
    {
        return repairMode;
    }

    public void setRepairMode(String repairMode)
    {
        this.repairMode = repairMode;
    }

    public String getSourceType()
    {
        return sourceType;
    }

    public void setSourceType(String sourceType)
    {
        this.sourceType = sourceType;
    }

    public String getSourceUrls()
    {
        return sourceUrls;
    }

    public void setSourceUrls(String sourceUrls)
    {
        this.sourceUrls = sourceUrls;
    }

    public String getResultUrls()
    {
        return resultUrls;
    }

    public void setResultUrls(String resultUrls)
    {
        this.resultUrls = resultUrls;
    }

    public String getResultVideoUrl()
    {
        return resultVideoUrl;
    }

    public void setResultVideoUrl(String resultVideoUrl)
    {
        this.resultVideoUrl = resultVideoUrl;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Integer getProgress()
    {
        return progress;
    }

    public void setProgress(Integer progress)
    {
        this.progress = progress;
    }

    public Long getStudentId()
    {
        return studentId;
    }

    public void setStudentId(Long studentId)
    {
        this.studentId = studentId;
    }

    public String getStudentName()
    {
        return studentName;
    }

    public void setStudentName(String studentName)
    {
        this.studentName = studentName;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage)
    {
        this.errorMessage = errorMessage;
    }

    public Date getFinishedTime()
    {
        return finishedTime;
    }

    public void setFinishedTime(Date finishedTime)
    {
        this.finishedTime = finishedTime;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("taskId", getTaskId())
            .append("taskNo", getTaskNo())
            .append("userId", getUserId())
            .append("userName", getUserName())
            .append("repairMode", getRepairMode())
            .append("sourceType", getSourceType())
            .append("sourceUrls", getSourceUrls())
            .append("resultUrls", getResultUrls())
            .append("resultVideoUrl", getResultVideoUrl())
            .append("status", getStatus())
            .append("progress", getProgress())
            .append("studentId", getStudentId())
            .append("studentName", getStudentName())
            .append("errorMessage", getErrorMessage())
            .append("finishedTime", getFinishedTime())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}