package cn.rbac.server.modules.system.service.job.impl;

import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.modules.system.dal.dataobject.job.SysJobLogDO;
import cn.rbac.server.modules.system.dal.mysql.job.SysJobLogMapper;
import cn.rbac.server.modules.system.service.job.SysJobLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
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
    public void clean() {
        remove(new LambdaQueryWrapper<>());
    }

    @Override
    public int cleanOlderThan(int days) {
        if (days <= 0) {
            return 0;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        return baseMapper.delete(new LambdaQueryWrapper<SysJobLogDO>()
                .lt(SysJobLogDO::getStartTime, cutoff));
    }
}
