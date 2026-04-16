package com.ruoyi.quartz.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.RepairFileCleanupService;

/**
 * 修复任务附件清理任务
 */
@Component("repairFileCleanupTask")
public class RepairFileCleanupTask
{
    private static final Logger log = LoggerFactory.getLogger(RepairFileCleanupTask.class);

    @Autowired
    private RepairFileCleanupService repairFileCleanupService;

    /**
     * 清理upload/temp目录中过期且未被任务引用的文件
     * @param expireHours 过期小时数，空值默认48
     */
    public void cleanUploadTempFiles(String expireHours)
    {
        Integer hours = null;
        if (StringUtils.isNotBlank(expireHours))
        {
            try
            {
                hours = Integer.valueOf(expireHours);
            }
            catch (Exception e)
            {
                log.warn("临时文件清理任务参数非法，使用默认值48小时: {}", expireHours);
            }
        }
        int deleted = repairFileCleanupService.cleanUploadTempFiles(hours);
        log.info("上传临时文件清理完成，删除文件数: {}", deleted);
    }

    /**
     * 清理repair_task中指向本地不存在文件的URL
     */
    public void cleanBrokenTaskResourceUrls()
    {
        int changed = repairFileCleanupService.cleanBrokenTaskResourceUrls();
        log.info("repair_task失效URL清理任务完成，更新任务数: {}", changed);
    }
}
