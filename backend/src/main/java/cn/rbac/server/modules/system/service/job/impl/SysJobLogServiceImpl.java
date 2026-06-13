package cn.rbac.server.modules.system.service.job.impl;

import cn.rbac.server.common.pojo.BusinessException;
import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.modules.system.dal.dataobject.job.SysJobLogDO;
import cn.rbac.server.modules.system.dal.mysql.job.SysJobLogMapper;
import cn.rbac.server.modules.system.service.job.SysJobLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SysJobLogServiceImpl extends ServiceImpl<SysJobLogMapper, SysJobLogDO> implements SysJobLogService {

    @Override
    public void recordLog(SysJobLogDO log) {
        super.save(log);
    }

    @Override
    public PageResult<SysJobLogDO> page(Integer pageNo, Integer pageSize, String jobName, String jobGroup, Integer status) {
        Page<SysJobLogDO> pageParam = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<SysJobLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(jobName), SysJobLogDO::getJobName, jobName)
                .eq(StringUtils.hasText(jobGroup), SysJobLogDO::getJobGroup, jobGroup)
                .eq(status != null, SysJobLogDO::getStatus, status)
                .orderByDesc(SysJobLogDO::getStartTime);
        Page<SysJobLogDO> result = page(pageParam, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    @Override
    public Map<String, Object> statistics() {
        Map<String, Object> result = new HashMap<>();
        long total = count();
        long successCount = count(new LambdaQueryWrapper<SysJobLogDO>().eq(SysJobLogDO::getStatus, 0));
        long failCount = count(new LambdaQueryWrapper<SysJobLogDO>().eq(SysJobLogDO::getStatus, 1));
        List<Map<String, Object>> dailyStats = baseMapper.selectDailyStats();
        result.put("totalCount", total);
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("dailyStats", dailyStats);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanAll() {
        remove(new LambdaQueryWrapper<>());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanScope(String jobName, String jobGroup) {
        if (!StringUtils.hasText(jobName) && !StringUtils.hasText(jobGroup)) {
            cleanAll();
            return;
        }
        LambdaQueryWrapper<SysJobLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(jobName), SysJobLogDO::getJobName, jobName)
                .eq(StringUtils.hasText(jobGroup), SysJobLogDO::getJobGroup, jobGroup);
        remove(wrapper);
    }

    @Override
    public PageResult<SysJobLogDO> recyclePage(PageParam pageParam, String jobName, String jobGroup) {
        Page<SysJobLogDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        Page<SysJobLogDO> deletedPage = (Page<SysJobLogDO>) baseMapper.selectDeletedPage(page, jobName, jobGroup);
        return PageResult.of(deletedPage.getRecords(), deletedPage.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restore(Long id) {
        int rows = baseMapper.restoreById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站日志不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermanent(Long id) {
        int rows = baseMapper.deletePhysicalById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站日志不存在");
        }
    }

    @Override
    public int cleanOlderThan(int days) {
        if (days <= 0) {
            return 0;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        return baseMapper.deletePhysicalOlderThan(cutoff);
    }
}
