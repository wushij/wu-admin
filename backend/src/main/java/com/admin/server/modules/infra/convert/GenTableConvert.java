package com.admin.server.modules.infra.convert;

import com.admin.server.common.core.PageResult;
import com.admin.server.common.util.BeanMappingUtils;
import com.admin.server.modules.infra.api.gen.vo.GenTableColumnRespVO;
import com.admin.server.modules.infra.api.gen.vo.GenTableRespVO;
import com.admin.server.modules.infra.dal.dataobject.gen.GenTableDO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 代码生成表配置转换器
 */
public final class GenTableConvert {

    private GenTableConvert() {
    }

    /**
     * 代码生成表拷贝，支持 columns 和 pkColumn 的级联转换
     */
    public static GenTableRespVO convertGenTable(GenTableDO table) {
        if (table == null) {
            return null;
        }
        GenTableRespVO vo = BeanMappingUtils.copyProperties(table, GenTableRespVO.class);
        if (table.getColumns() != null) {
            vo.setColumns(BeanMappingUtils.copyListProperties(table.getColumns(), GenTableColumnRespVO.class));
        }
        if (table.getPkColumn() != null) {
            vo.setPkColumn(BeanMappingUtils.copyProperties(table.getPkColumn(), GenTableColumnRespVO.class));
        }
        return vo;
    }

    public static PageResult<GenTableRespVO> convertGenTablePage(PageResult<GenTableDO> page) {
        if (page == null) {
            return PageResult.of(Collections.emptyList(), 0L);
        }
        List<GenTableRespVO> targetList = new ArrayList<>(page.getList().size());
        for (GenTableDO table : page.getList()) {
            targetList.add(convertGenTable(table));
        }
        return PageResult.of(targetList, page.getTotal());
    }
}
