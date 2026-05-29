package cn.rbac.server.modules.system.dal.mysql.file;

import cn.rbac.server.modules.system.dal.dataobject.file.SysFileGroupDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysFileGroupMapper extends BaseMapper<SysFileGroupDO> {

    @Select("SELECT g.*, (SELECT COUNT(*) FROM sys_file f WHERE f.group_id = g.id "
            + "AND f.file_path NOT LIKE 'images/chat/%') AS file_count "
            + "FROM sys_file_group g ORDER BY g.sort ASC, g.id ASC")
    List<SysFileGroupDO> selectListWithFileCount();

    @Select("SELECT COUNT(*) FROM sys_file WHERE group_id IS NULL AND file_path NOT LIKE 'images/chat/%'")
    Integer selectUngroupedFileCount();
}
