package cn.rbac.server.modules.system.service.gen;

import cn.rbac.server.modules.system.dal.dataobject.gen.DatabaseTableVO;
import cn.rbac.server.modules.system.dal.dataobject.gen.GenTableDO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

public interface GenTableService {

    Page<DatabaseTableVO> selectDbTableList(Integer pageNo, Integer pageSize, String tableName);

    void importTable(String[] tableNames);

    Page<GenTableDO> page(Integer pageNo, Integer pageSize, String tableName);

    GenTableDO getTableById(Long id);

    void updateTable(GenTableDO table);

    void deleteTable(Long[] ids);

    Map<String, String> previewCode(Long tableId);

    byte[] generateCode(Long[] tableIds);

    List<String> previewGenerateFiles(Long tableId);

    List<String> generateToProject(Long tableId, boolean overwrite);

    List<String> previewRemoveFiles(Long tableId);

    List<String> removeGeneratedCode(Long tableId);

    void syncTable(Long tableId);
}
