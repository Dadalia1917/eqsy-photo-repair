package com.ruoyi.system.service;

import java.net.URI;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.RepairTask;
import com.ruoyi.system.mapper.RepairTaskMapper;

@Service
public class RepairFileCleanupService
{
    private static final Logger log = LoggerFactory.getLogger(RepairFileCleanupService.class);
    private static final String CLEANUP_OPERATOR = "system-cleanup";
    private static final int DEFAULT_EXPIRE_HOURS = 48;
    private static final int MAX_EXPIRE_HOURS = 24 * 30;

    @Autowired
    private RepairTaskMapper repairTaskMapper;

    public void deleteTaskAttachments(RepairTask task)
    {
        if (task == null)
        {
            return;
        }
        deleteResourceList(task.getSourceUrls());
        deleteResourceList(task.getResultUrls());
        deleteResourceList(task.getResultVideoUrl());
    }

    public void deleteResourceList(String resources)
    {
        Set<String> set = parseResourceSet(resources);
        for (String resource : set)
        {
            deleteSingleResource(resource);
        }
    }

    public int cleanUploadTempFiles(Integer expireHours)
    {
        int safeHours = normalizeExpireHours(expireHours);
        Path tempRoot = Paths.get(RuoYiConfig.getUploadPath(), "temp").toAbsolutePath().normalize();
        if (!Files.exists(tempRoot))
        {
            return 0;
        }

        Set<String> activeFileSet = getActiveResourceAbsolutePaths();
        long expireBefore = System.currentTimeMillis() - safeHours * 3600_000L;
        int deletedCount = 0;

        try (Stream<Path> stream = Files.walk(tempRoot))
        {
            List<Path> files = stream.filter(Files::isRegularFile).collect(Collectors.toList());
            for (Path file : files)
            {
                Path normalized = file.toAbsolutePath().normalize();
                if (!normalized.startsWith(tempRoot))
                {
                    continue;
                }
                if (activeFileSet.contains(normalized.toString()))
                {
                    continue;
                }
                long lastModified = Files.getLastModifiedTime(normalized).toMillis();
                if (lastModified >= expireBefore)
                {
                    continue;
                }
                if (Files.deleteIfExists(normalized))
                {
                    deletedCount++;
                }
            }
        }
        catch (Exception e)
        {
            log.warn("清理上传临时文件失败", e);
        }

        clearEmptyDirectories(tempRoot);
        return deletedCount;
    }

    /**
     * 清理repair_task中指向本地不存在文件的URL
     * @return 更新的任务数量
     */
    public int cleanBrokenTaskResourceUrls()
    {
        List<RepairTask> tasks = repairTaskMapper.selectRepairTaskList(new RepairTask());
        int changedTasks = 0;
        int removedUrls = 0;

        for (RepairTask task : tasks)
        {
            NormalizeResult source = normalizeResourceUrls(task.getSourceUrls(), false);
            NormalizeResult result = normalizeResourceUrls(task.getResultUrls(), true);
            NormalizeResult video = normalizeResourceUrls(task.getResultVideoUrl(), true);

            String newSourceUrls = source.value;
            String newResultUrls = result.value;
            String newResultVideoUrl = video.value;

            boolean changed = !Objects.equals(StringUtils.trimToEmpty(task.getSourceUrls()), StringUtils.trimToEmpty(newSourceUrls))
                    || !Objects.equals(StringUtils.trimToEmpty(task.getResultUrls()), StringUtils.trimToEmpty(newResultUrls))
                    || !Objects.equals(StringUtils.trimToEmpty(task.getResultVideoUrl()), StringUtils.trimToEmpty(newResultVideoUrl));
            if (!changed)
            {
                continue;
            }

            int rows = repairTaskMapper.updateTaskResourceUrls(task.getTaskId(), newSourceUrls, newResultUrls,
                    newResultVideoUrl, CLEANUP_OPERATOR);
            if (rows > 0)
            {
                changedTasks++;
                removedUrls += source.removedCount + result.removedCount + video.removedCount;
            }
        }

        log.info("repair_task失效URL清理完成，更新任务数: {}, 删除URL数: {}", changedTasks, removedUrls);
        return changedTasks;
    }

    private int normalizeExpireHours(Integer expireHours)
    {
        if (expireHours == null || expireHours < 1)
        {
            return DEFAULT_EXPIRE_HOURS;
        }
        return Math.min(expireHours, MAX_EXPIRE_HOURS);
    }

    private NormalizeResult normalizeResourceUrls(String resources, boolean nullableWhenEmpty)
    {
        if (StringUtils.isBlank(resources))
        {
            return new NormalizeResult(nullableWhenEmpty ? null : "", 0);
        }
        String[] arr = resources.split(",");
        List<String> validUrls = new ArrayList<>();
        int removedCount = 0;
        for (String item : arr)
        {
            String url = StringUtils.trimToEmpty(item);
            if (StringUtils.isBlank(url))
            {
                continue;
            }
            if (shouldKeepResourceUrl(url))
            {
                validUrls.add(url);
            }
            else
            {
                removedCount++;
            }
        }

        if (validUrls.isEmpty())
        {
            return new NormalizeResult(nullableWhenEmpty ? null : "", removedCount);
        }
        return new NormalizeResult(StringUtils.join(validUrls, ","), removedCount);
    }

    private boolean shouldKeepResourceUrl(String resource)
    {
        String raw = StringUtils.trimToEmpty(resource);
        if (StringUtils.isBlank(raw) || raw.startsWith("wxfile://") || raw.startsWith("http://tmp/"))
        {
            return false;
        }

        String absolutePath = resolveLocalAbsolutePath(raw);
        if (StringUtils.isBlank(absolutePath))
        {
            // 非本地/profile资源不做删除，避免误清理外链
            return true;
        }
        return Files.exists(Paths.get(absolutePath));
    }

    private Set<String> getActiveResourceAbsolutePaths()
    {
        Set<String> active = new LinkedHashSet<>();
        List<String> resourceRows = repairTaskMapper.selectAllResourceUrls();
        for (String row : resourceRows)
        {
            for (String resource : parseResourceSet(row))
            {
                String absolutePath = resolveLocalAbsolutePath(resource);
                if (StringUtils.isNotBlank(absolutePath))
                {
                    active.add(Paths.get(absolutePath).toAbsolutePath().normalize().toString());
                }
            }
        }
        return active;
    }

    private void clearEmptyDirectories(Path root)
    {
        try (Stream<Path> stream = Files.walk(root))
        {
            List<Path> dirs = stream.filter(Files::isDirectory)
                    .sorted(Comparator.reverseOrder())
                    .collect(Collectors.toList());
            for (Path dir : dirs)
            {
                if (dir.equals(root))
                {
                    continue;
                }
                try (Stream<Path> children = Files.list(dir))
                {
                    if (!children.findAny().isPresent())
                    {
                        Files.deleteIfExists(dir);
                    }
                }
            }
        }
        catch (Exception ignored)
        {
        }
    }

    private Set<String> parseResourceSet(String resources)
    {
        Set<String> set = new LinkedHashSet<>();
        if (StringUtils.isBlank(resources))
        {
            return set;
        }
        String[] arr = resources.split(",");
        for (String item : arr)
        {
            String value = StringUtils.trimToEmpty(item);
            if (StringUtils.isNotBlank(value))
            {
                set.add(value);
            }
        }
        return set;
    }

    private void deleteSingleResource(String resource)
    {
        String absolutePath = resolveLocalAbsolutePath(resource);
        if (StringUtils.isBlank(absolutePath))
        {
            return;
        }
        try
        {
            Files.deleteIfExists(Paths.get(absolutePath));
        }
        catch (Exception e)
        {
            log.warn("删除任务关联文件失败: {}", absolutePath, e);
        }
    }

    private String resolveLocalAbsolutePath(String resource)
    {
        String raw = StringUtils.trimToEmpty(resource);
        if (StringUtils.isBlank(raw) || raw.startsWith("wxfile://") || raw.startsWith("http://tmp/"))
        {
            return null;
        }

        String path = raw;
        if (raw.startsWith(Constants.HTTP) || raw.startsWith(Constants.HTTPS))
        {
            try
            {
                path = URI.create(raw).getPath();
            }
            catch (Exception e)
            {
                return null;
            }
        }

        if (StringUtils.isBlank(path))
        {
            return null;
        }

        int prefixIndex = path.indexOf(Constants.RESOURCE_PREFIX + "/");
        if (prefixIndex >= 0)
        {
            path = path.substring(prefixIndex + Constants.RESOURCE_PREFIX.length());
        }
        else if (path.startsWith(Constants.RESOURCE_PREFIX))
        {
            path = path.substring(Constants.RESOURCE_PREFIX.length());
        }
        else
        {
            return null;
        }

        String profile = RuoYiConfig.getProfile();
        if (StringUtils.isBlank(profile))
        {
            return null;
        }

        Path profilePath = Paths.get(profile).toAbsolutePath().normalize();
        String relative = path.replace('\\', '/').replaceAll("^/+", "");
        Path target = profilePath.resolve(relative).normalize();
        if (!target.startsWith(profilePath))
        {
            return null;
        }
        return target.toString();
    }

    private static class NormalizeResult
    {
        private final String value;
        private final int removedCount;

        private NormalizeResult(String value, int removedCount)
        {
            this.value = value;
            this.removedCount = removedCount;
        }
    }
}
