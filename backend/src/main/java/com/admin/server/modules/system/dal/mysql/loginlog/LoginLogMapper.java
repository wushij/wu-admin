package com.admin.server.modules.system.dal.mysql.loginlog;

import com.admin.server.modules.system.dal.dataobject.loginlog.LoginLogDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录日志 Mapper
 */
@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLogDO> {
}
