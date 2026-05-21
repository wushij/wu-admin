package cn.rbac.server.modules.system.service.dict;

import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.modules.system.dal.dataobject.dict.DictDataDO;

import java.util.List;
import java.util.Map;

public interface DictDataService {

    PageResult<DictDataDO> page(Integer pageNo, Integer pageSize, String dictType, String dictLabel, Integer status);

    List<DictDataDO> listByDictType(String dictType);

    List<DictDataDO> listByDictTypeForManage(String dictType);

    DictDataDO getById(Long id);

    void create(DictDataDO dictData);

    void update(DictDataDO dictData);

    void delete(Long id);

    long countByDictType(String dictType);

    /** 批量按类型编码拉取（供表单下拉） */
    Map<String, List<DictDataDO>> batchByTypes(List<String> dictTypes);
}
