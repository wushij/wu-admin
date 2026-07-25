package com.admin.server.modules.system.service.dict;

import com.admin.server.common.pojo.PageParam;
import com.admin.server.common.pojo.PageResult;
import com.admin.server.modules.system.dal.dataobject.dict.DictTypeDO;

import java.util.List;

public interface DictTypeService {

    PageResult<DictTypeDO> page(Integer pageNo, Integer pageSize, String dictName, String dictType, Integer status);

    List<DictTypeDO> listEnabled();

    DictTypeDO getById(Long id);

    void create(DictTypeDO dictType);

    void update(DictTypeDO dictType);

    void delete(Long id);

    /** 复制类型及其全部数据项 */
    void copy(Long id);

    PageResult<DictTypeDO> recyclePage(PageParam pageParam, String dictName, String dictType);

    void restore(Long id);

    void deletePermanent(Long id);
}
