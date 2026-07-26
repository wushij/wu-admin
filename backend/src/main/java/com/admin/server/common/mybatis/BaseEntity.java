package com.admin.server.common.mybatis;

import com.admin.server.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 继承框架层 BaseDO 基类，保持向下兼容
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BaseEntity extends BaseDO {
}
