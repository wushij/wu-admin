package com.admin.server.modules.infra.service.recycle.impl;

import com.admin.server.modules.infra.api.recycle.vo.RecycleSummaryVO;
import com.admin.server.modules.infra.dal.mysql.recycle.RecycleCenterMapper;
import com.admin.server.modules.infra.service.recycle.RecycleCenterService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class RecycleCenterServiceImpl implements RecycleCenterService {

    @Resource
    private RecycleCenterMapper recycleCenterMapper;

    @Override
    public RecycleSummaryVO summary() {
        RecycleSummaryVO vo = new RecycleSummaryVO();
        vo.setUser(recycleCenterMapper.countDeletedUsers());
        vo.setRole(recycleCenterMapper.countDeletedRoles());
        vo.setMenu(recycleCenterMapper.countDeletedMenus());
        vo.setDept(recycleCenterMapper.countDeletedDepts());
        vo.setPost(recycleCenterMapper.countDeletedPosts());
        vo.setTicket(recycleCenterMapper.countDeletedTickets());
        vo.setApproval(recycleCenterMapper.countDeletedApprovals());
        vo.setDict(recycleCenterMapper.countDeletedDictTypes());
        vo.setDictData(recycleCenterMapper.countDeletedDictData());
        vo.setAnnounce(recycleCenterMapper.countDeletedAnnounces());
        vo.setJob(recycleCenterMapper.countDeletedJobs());
        vo.setJobLog(recycleCenterMapper.countDeletedJobLogs());
        vo.setFile(recycleCenterMapper.countDeletedFiles());
        vo.setGen(recycleCenterMapper.countDeletedGenTables());
        vo.setTotal(vo.getUser() + vo.getRole() + vo.getMenu() + vo.getDept()
                + vo.getPost() + vo.getTicket() + vo.getApproval() + vo.getDict()
                + vo.getDictData() + vo.getAnnounce() + vo.getJob() + vo.getJobLog()
                + vo.getFile() + vo.getGen());
        return vo;
    }
}
