package com.admin.server.modules.system.service.job;

import com.admin.server.common.pojo.PageParam;
import com.admin.server.common.pojo.PageResult;
import com.admin.server.modules.system.api.monitor.vo.JobTemplateVO;
import com.admin.server.modules.system.api.monitor.vo.SysJobVO;
import com.admin.server.modules.system.dal.dataobject.job.SysJobDO;

import java.util.List;
import java.util.Map;

public interface SysJobService {
    Map<String, Object> overview();

    List<JobTemplateVO> templates();

    Map<String, Object> checkCron(String cronExpression);

    PageResult<SysJobVO> pageVo(Integer pageNo, Integer pageSize, String jobName, String jobGroup, Integer status);

    PageResult<SysJobDO> page(Integer pageNo, Integer pageSize, String jobName, String jobGroup, Integer status);

    SysJobDO getById(Long id);

    void create(SysJobDO job);

    void update(SysJobDO job);

    void delete(Long id);

    void changeStatus(Long id, Integer status);

    void run(Long id);

    PageResult<SysJobDO> recyclePage(PageParam pageParam, String jobName, String jobGroup);

    void restore(Long id);

    void deletePermanent(Long id);
}
