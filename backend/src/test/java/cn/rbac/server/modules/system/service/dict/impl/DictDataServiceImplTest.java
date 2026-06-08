package cn.rbac.server.modules.system.service.dict.impl;

import cn.rbac.server.modules.system.dal.dataobject.dict.DictDataDO;
import cn.rbac.server.modules.system.dal.mysql.dict.DictDataMapper;
import cn.rbac.server.modules.system.framework.cache.DictCacheService;
import cn.rbac.server.testsupport.MybatisLambdaTestBase;
import cn.rbac.server.testsupport.MybatisMockMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DictDataServiceImpl 单元测试")
class DictDataServiceImplTest extends MybatisLambdaTestBase {

    @Mock
    private DictDataMapper dictDataMapper;
    @Mock
    private DictCacheService dictCacheService;

    @InjectMocks
    private DictDataServiceImpl dictDataService;

    @BeforeEach
    void wireDependencies() {
        ReflectionTestUtils.setField(Objects.requireNonNull(dictDataService), "baseMapper", dictDataMapper);
    }

    @Test
    @DisplayName("listByDictType：委托缓存服务")
    void listByDictType_usesCache() {
        DictDataDO row = dictRow(1L, "sys_user_sex", "1", "男");
        when(dictCacheService.listByDictType("sys_user_sex")).thenReturn(List.of(row));

        List<DictDataDO> result = dictDataService.listByDictType("sys_user_sex");

        assertEquals(1, result.size());
        assertEquals("男", result.get(0).getDictLabel());
        verify(dictCacheService).listByDictType("sys_user_sex");
    }

    @Test
    @DisplayName("getById：不存在时抛异常")
    void getById_notFound() {
        when(dictDataMapper.selectById(99L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> dictDataService.getById(99L));
    }

    @Test
    @DisplayName("create：键值重复时拒绝")
    void create_duplicateValue() {
        when(dictDataMapper.selectCount(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(1L);

        DictDataDO dictData = dictRow(null, "sys_user_sex", "1", "男");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> dictDataService.create(dictData));

        assertTrue(ex.getMessage().contains("已存在"));
        verify(dictCacheService, never()).refreshAll();
    }

    @Test
    @DisplayName("create：成功后刷新缓存")
    void create_refreshesCache() {
        when(dictDataMapper.selectCount(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(0L);
        when(dictDataMapper.insert(any(DictDataDO.class))).thenReturn(1);

        DictDataDO dictData = dictRow(null, "sys_user_sex", "2", "女");
        dictDataService.create(dictData);

        verify(dictDataMapper).insert(dictData);
        verify(dictCacheService).refreshAll();
    }

    @Test
    @DisplayName("delete：成功后刷新缓存")
    void delete_refreshesCache() {
        when(dictDataMapper.deleteById(1L)).thenReturn(1);

        dictDataService.delete(1L);

        verify(dictCacheService).refreshAll();
    }

    private static DictDataDO dictRow(Long id, String type, String value, String label) {
        DictDataDO row = new DictDataDO();
        row.setId(id);
        row.setDictType(type);
        row.setDictValue(value);
        row.setDictLabel(label);
        row.setStatus(1);
        return row;
    }
}
