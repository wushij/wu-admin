package cn.rbac.server.modules.system.dal.dataobject.job;

import cn.rbac.server.common.mybatis.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_job")
public class SysJobDO extends BaseEntity {
    private String jobName;
    private String jobGroup;
    private String invokeTarget;
    private String cronExpression;
    /** 1 立即执行 / 2 执行一次 / 3 放弃执行 */
    private Integer misfirePolicy;
    /** 0 允许并发 / 1 禁止 */
    private Integer concurrent;
    /** 0 暂停 / 1 正常 */
    private Integer status;
    private String remark;
}
