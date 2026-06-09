package cn.rbac.server.testsupport;

import cn.rbac.server.modules.system.dal.dataobject.approval.ApprovalFormDO;
import cn.rbac.server.modules.system.dal.dataobject.approval.ApprovalRecordDO;
import cn.rbac.server.modules.system.dal.dataobject.dept.DeptDO;
import cn.rbac.server.modules.system.dal.dataobject.dict.DictDataDO;
import cn.rbac.server.modules.system.dal.dataobject.loginlog.LoginLogDO;
import cn.rbac.server.modules.system.dal.dataobject.message.ChatGroupDO;
import cn.rbac.server.modules.system.dal.dataobject.message.ChatGroupMemberDO;
import cn.rbac.server.modules.system.dal.dataobject.message.ChatGroupMessageDO;
import cn.rbac.server.modules.system.dal.dataobject.message.ChatMessageDO;
import cn.rbac.server.modules.system.dal.dataobject.message.UserBlacklistDO;
import cn.rbac.server.modules.system.dal.dataobject.permission.MenuDO;
import cn.rbac.server.modules.system.dal.dataobject.permission.RoleDO;
import cn.rbac.server.modules.system.dal.dataobject.notice.NoticeDO;
import cn.rbac.server.modules.system.dal.dataobject.user.UserDO;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;

/**
 * 纯单元测试中构造 {@link com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper}
 * 需要预先初始化实体元数据，否则会抛 MybatisPlusException。
 */
public abstract class MybatisLambdaTestBase {

    @BeforeAll
    static void initLambdaEntityCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, UserDO.class);
        TableInfoHelper.initTableInfo(assistant, MenuDO.class);
        TableInfoHelper.initTableInfo(assistant, UserBlacklistDO.class);
        TableInfoHelper.initTableInfo(assistant, ChatGroupMemberDO.class);
        TableInfoHelper.initTableInfo(assistant, ChatMessageDO.class);
        TableInfoHelper.initTableInfo(assistant, ChatGroupMessageDO.class);
        TableInfoHelper.initTableInfo(assistant, DeptDO.class);
        TableInfoHelper.initTableInfo(assistant, RoleDO.class);
        TableInfoHelper.initTableInfo(assistant, ApprovalFormDO.class);
        TableInfoHelper.initTableInfo(assistant, ApprovalRecordDO.class);
        TableInfoHelper.initTableInfo(assistant, NoticeDO.class);
        TableInfoHelper.initTableInfo(assistant, LoginLogDO.class);
        TableInfoHelper.initTableInfo(assistant, DictDataDO.class);
        TableInfoHelper.initTableInfo(assistant, ChatGroupDO.class);
    }
}
