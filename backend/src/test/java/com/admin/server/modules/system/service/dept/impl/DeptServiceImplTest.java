package com.admin.server.modules.system.service.dept.impl;

import com.admin.server.common.pojo.BusinessException;
import com.admin.server.modules.system.dal.dataobject.dept.DeptDO;
import com.admin.server.modules.system.dal.mysql.dept.DeptMapper;
import com.admin.server.modules.system.dal.mysql.user.UserMapper;
import com.admin.server.testsupport.MybatisLambdaTestBase;
import com.admin.server.testsupport.MybatisMockMatchers;
import com.admin.server.testsupport.ServiceTestFixtures;
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
@DisplayName("DeptServiceImpl 单元测试")
class DeptServiceImplTest extends MybatisLambdaTestBase {

    @Mock
    private DeptMapper deptMapper;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private DeptServiceImpl deptService;

    @BeforeEach
    void wireBaseMapper() {
        ReflectionTestUtils.setField(Objects.requireNonNull(deptService), "baseMapper", deptMapper);
    }

    @Test
    @DisplayName("getById：部门不存在抛异常")
    void getById_notFound() {
        when(deptMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> deptService.getById(99L));

        assertTrue(ex.getMessage().contains("不存在"));
    }

    @Test
    @DisplayName("create：根部门 ancestors 为 0")
    void create_rootDept() {
        DeptDO dept = new DeptDO();
        dept.setName("总部");
        dept.setParentId(0L);

        deptService.create(dept);

        assertEquals("0", dept.getAncestors());
        assertEquals(1, dept.getStatus());
        verify(deptMapper).insert(dept);
    }

    @Test
    @DisplayName("create：子部门 ancestors 拼接父级路径")
    void create_childDept() {
        DeptDO parent = ServiceTestFixtures.dept(1L, 0L, "总部");
        when(deptMapper.selectById(1L)).thenReturn(parent);

        DeptDO child = new DeptDO();
        child.setName("研发部");
        child.setParentId(1L);

        deptService.create(child);

        assertEquals("0,1", child.getAncestors());
    }

    @Test
    @DisplayName("delete：存在子部门时拒绝删除")
    void delete_rejectsWhenHasChildren() {
        when(deptMapper.selectCount(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(2L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> deptService.delete(1L));

        assertTrue(ex.getMessage().contains("子部门"));
        verify(deptMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("delete：部门下有用户时拒绝删除")
    void delete_rejectsWhenHasUsers() {
        when(deptMapper.selectCount(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(0L);
        when(userMapper.selectCount(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(3L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> deptService.delete(1L));

        assertTrue(ex.getMessage().contains("用户"));
    }

    @Test
    @DisplayName("update：上级部门不能选择自己")
    void update_rejectsSelfAsParent() {
        DeptDO exist = ServiceTestFixtures.dept(5L, 0L, "部门A");
        when(deptMapper.selectById(5L)).thenReturn(exist);

        DeptDO update = new DeptDO();
        update.setId(5L);
        update.setParentId(5L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> deptService.update(update));

        assertTrue(ex.getMessage().contains("不能选择自己"));
    }

    @Test
    @DisplayName("move：不能移动到子部门下")
    void move_rejectsMoveUnderChild() {
        DeptDO dept = ServiceTestFixtures.dept(1L, 0L, "总部");
        DeptDO child = ServiceTestFixtures.dept(5L, 1L, "研发部");
        child.setAncestors("0,1");
        when(deptMapper.selectById(1L)).thenReturn(dept);
        when(deptMapper.selectById(5L)).thenReturn(child);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> deptService.move(1L, 5L, null));

        assertTrue(ex.getMessage().contains("子部门"));
    }

    @Test
    @DisplayName("tree：构建父子层级")
    void tree_buildsHierarchy() {
        DeptDO root = ServiceTestFixtures.dept(1L, 0L, "总部");
        DeptDO child = ServiceTestFixtures.dept(2L, 1L, "研发部");
        when(deptMapper.selectList(MybatisMockMatchers.anyLambdaQueryWrapper()))
                .thenReturn(List.of(root, child));
        when(userMapper.selectList(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(List.of());

        List<DeptDO> tree = deptService.tree(null, null);

        assertEquals(1, tree.size());
        assertEquals(1L, tree.get(0).getId());
        assertNotNull(tree.get(0).getChildren());
        assertEquals(1, tree.get(0).getChildren().size());
        assertEquals(2L, tree.get(0).getChildren().get(0).getId());
    }
}
