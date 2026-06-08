package cn.rbac.server.modules.system.service.gen.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.rbac.server.common.pojo.BusinessException;
import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.modules.system.dal.dataobject.gen.DatabaseColumnVO;
import cn.rbac.server.modules.system.dal.dataobject.gen.DatabaseTableVO;
import cn.rbac.server.modules.system.dal.dataobject.gen.GenTableColumnDO;
import cn.rbac.server.modules.system.dal.dataobject.gen.GenTableDO;
import cn.rbac.server.modules.system.dal.dataobject.permission.MenuDO;
import cn.rbac.server.modules.system.dal.dataobject.permission.RoleDO;
import cn.rbac.server.modules.system.dal.dataobject.permission.RoleMenuDO;
import cn.rbac.server.modules.system.dal.mysql.gen.GenTableColumnMapper;
import cn.rbac.server.modules.system.dal.mysql.gen.GenTableMapper;
import cn.rbac.server.modules.system.dal.mysql.permission.MenuMapper;
import cn.rbac.server.modules.system.dal.mysql.permission.RoleMapper;
import cn.rbac.server.modules.system.dal.mysql.permission.RoleMenuMapper;
import cn.rbac.server.modules.system.service.gen.GenTableService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.StringWriter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenTableServiceImpl implements GenTableService {

    private static final String DEFAULT_PACKAGE = "cn.rbac.server.modules.system";
    private static final Long DEFAULT_PARENT_MENU_ID = 1L;
    private static final Set<String> BASE_ENTITY_FIELDS = Set.of(
            "id", "createTime", "updateTime", "creator", "updater", "deleted");
    private static final Set<String> DELETE_MARK_FIELDS = Set.of(
            "deleted", "delFlag", "isDeleted", "del_flag", "is_deleted");
    private static final Set<String> TABLE_PREFIXES = Set.of(
            "sys", "system", "t", "tb", "biz", "app");
    private static final Map<String, String> TYPE_MAP = new HashMap<>();

    static {
        TYPE_MAP.put("tinyint", "Integer");
        TYPE_MAP.put("smallint", "Integer");
        TYPE_MAP.put("mediumint", "Integer");
        TYPE_MAP.put("int", "Integer");
        TYPE_MAP.put("integer", "Integer");
        TYPE_MAP.put("bigint", "Long");
        TYPE_MAP.put("float", "Double");
        TYPE_MAP.put("double", "Double");
        TYPE_MAP.put("decimal", "BigDecimal");
        TYPE_MAP.put("bit", "Boolean");
        TYPE_MAP.put("char", "String");
        TYPE_MAP.put("varchar", "String");
        TYPE_MAP.put("tinytext", "String");
        TYPE_MAP.put("text", "String");
        TYPE_MAP.put("mediumtext", "String");
        TYPE_MAP.put("longtext", "String");
        TYPE_MAP.put("date", "LocalDate");
        TYPE_MAP.put("datetime", "LocalDateTime");
        TYPE_MAP.put("timestamp", "LocalDateTime");
        TYPE_MAP.put("time", "LocalTime");
        TYPE_MAP.put("json", "String");

        Properties props = new Properties();
        props.put("resource.loader", "class");
        props.put("resource.loader.class.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        props.put("input.encoding", "UTF-8");
        props.put("output.encoding", "UTF-8");
        Velocity.init(props);
    }

    private final GenTableMapper genTableMapper;
    private final GenTableColumnMapper genTableColumnMapper;
    private final MenuMapper menuMapper;
    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final @NonNull PlatformTransactionManager transactionManager;

    @Override
    public Page<DatabaseTableVO> selectDbTableList(Integer pageNo, Integer pageSize, String tableName) {
        int offset = (pageNo - 1) * pageSize;
        List<DatabaseTableVO> list = genTableMapper.selectDbTableList(tableName, offset, pageSize);
        long total = genTableMapper.countDbTable(tableName);
        Page<DatabaseTableVO> result = new Page<>(pageNo, pageSize);
        result.setRecords(list);
        result.setTotal(total);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importTable(String[] tableNames) {
        for (String tableName : tableNames) {
            if (StrUtil.isBlank(tableName)) {
                continue;
            }
            if (existsImportedTable(tableName)) {
                throw new BusinessException("表 " + tableName + " 已导入，请勿重复导入");
            }
            DatabaseTableVO dbTable = genTableMapper.selectDbTableByName(tableName);
            if (dbTable == null) {
                continue;
            }
            GenTableDO table = new GenTableDO();
            table.setTableName(tableName);
            table.setTableComment(dbTable.getTableComment());
            table.setClassName(toClassName(tableName));
            table.setPackageName(DEFAULT_PACKAGE);
            table.setModuleName("system");
            table.setBusinessName(toBusinessName(tableName));
            table.setFormLayout("vertical");
            table.setFunctionName(StrUtil.isNotBlank(dbTable.getTableComment())
                    ? dbTable.getTableComment() : toClassName(tableName));
            table.setAuthor("wu-admin");
            table.setGenType("crud");
            table.setFrontType("element-plus");
            table.setParentMenuId(DEFAULT_PARENT_MENU_ID);
            table.setDeleted(0);
            genTableMapper.insert(table);

            List<DatabaseColumnVO> columns = genTableMapper.selectDbColumnsByTableName(tableName);
            for (DatabaseColumnVO col : columns) {
                GenTableColumnDO column = buildColumnFromDb(table.getId(), col);
                genTableColumnMapper.insert(column);
            }
        }
    }

    private boolean existsImportedTable(String tableName) {
        return genTableMapper.selectCount(new LambdaQueryWrapper<GenTableDO>()
                .eq(GenTableDO::getTableName, tableName)
                .and(w -> w.eq(GenTableDO::getDeleted, 0).or().isNull(GenTableDO::getDeleted))) > 0;
    }

    private boolean isDeleted(GenTableDO table) {
        return table.getDeleted() != null && table.getDeleted() == 1;
    }

    private GenTableColumnDO buildColumnFromDb(Long tableId, DatabaseColumnVO col) {
        GenTableColumnDO column = new GenTableColumnDO();
        column.setTableId(tableId);
        column.setColumnName(col.getColumnName());
        column.setColumnComment(StrUtil.isNotBlank(col.getColumnComment())
                ? col.getColumnComment() : col.getColumnName());
        column.setColumnType(col.getColumnType());
        column.setJavaType(toJavaType(col.getDataType()));
        column.setJavaField(toCamelCase(col.getColumnName()));
        column.setIsPk("PRI".equals(col.getColumnKey()) ? 1 : 0);
        column.setIsIncrement("auto_increment".equalsIgnoreCase(col.getExtra()) ? 1 : 0);
        column.setIsRequired("NO".equals(col.getIsNullable()) ? 1 : 0);
        column.setSort(col.getOrdinalPosition());

        String javaField = column.getJavaField();
        String columnNameLower = column.getColumnName().toLowerCase();
        boolean isBaseField = BASE_ENTITY_FIELDS.contains(javaField);
        boolean isDeleteField = DELETE_MARK_FIELDS.contains(javaField)
                || columnNameLower.contains("deleted") || columnNameLower.contains("del_flag");

        column.setIsInsert(column.getIsPk() == 0 && !isBaseField && !isDeleteField ? 1 : 0);
        column.setIsEdit(column.getIsPk() == 0 && !isBaseField && !isDeleteField ? 1 : 0);
        column.setIsList(isDeleteField ? 0 : 1);
        column.setIsQuery(column.getIsPk() == 1 || "name".equals(javaField) || "status".equals(javaField) ? 1 : 0);
        column.setQueryType("EQ");
        column.setHtmlType(resolveHtmlType(column));
        column.setDictType(resolveDictType(column));
        return column;
    }

    @Override
    public Page<GenTableDO> page(Integer pageNo, Integer pageSize, String tableName) {
        Page<GenTableDO> pageParam = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<GenTableDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(GenTableDO::getDeleted, 0).or().isNull(GenTableDO::getDeleted));
        if (StrUtil.isNotBlank(tableName)) {
            wrapper.like(GenTableDO::getTableName, tableName);
        }
        wrapper.orderByDesc(GenTableDO::getCreateTime);
        return genTableMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public GenTableDO getTableById(Long id) {
        GenTableDO table = genTableMapper.selectById(id);
        if (table != null && isDeleted(table)) {
            return null;
        }
        if (table != null) {
            List<GenTableColumnDO> columns = genTableColumnMapper.selectByTableId(id);
            table.setColumns(columns);
            columns.stream().filter(c -> c.getIsPk() == 1).findFirst().ifPresent(table::setPkColumn);
        }
        return table;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTable(GenTableDO table) {
        genTableMapper.updateById(table);
        if (table.getColumns() != null) {
            for (GenTableColumnDO column : table.getColumns()) {
                genTableColumnMapper.updateById(column);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTable(Long[] ids) {
        for (Long id : ids) {
            int rows = genTableMapper.softDeleteById(id);
            if (rows == 0) {
                throw new BusinessException("表配置不存在或已删除");
            }
        }
    }

    @Override
    public PageResult<GenTableDO> recyclePage(PageParam pageParam, String tableName) {
        Page<GenTableDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        Page<GenTableDO> deletedPage = (Page<GenTableDO>) genTableMapper.selectDeletedPage(page, tableName);
        return PageResult.of(deletedPage.getRecords(), deletedPage.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restore(Long id) {
        GenTableDO deleted = genTableMapper.selectById(id);
        if (deleted == null || !isDeleted(deleted)) {
            throw new BusinessException(404, "回收站表配置不存在");
        }
        if (existsImportedTable(deleted.getTableName())) {
            throw new BusinessException("表 " + deleted.getTableName() + " 已存在有效配置，无法恢复");
        }
        int rows = genTableMapper.restoreById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站表配置不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermanent(Long id) {
        int rows = genTableMapper.deletePhysicalById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站表配置不存在");
        }
        genTableColumnMapper.deleteByTableId(id);
    }

    @Override
    public Map<String, String> previewCode(Long tableId) {
        GenTableDO table = requireTable(tableId);
        VelocityContext context = prepareContext(table);
        Map<String, String> codeMap = new LinkedHashMap<>();
        for (String[] tpl : templateList()) {
            codeMap.put(tpl[1], renderTemplate(tpl[0], context));
        }
        return codeMap;
    }

    @Override
    public byte[] generateCode(Long[] tableIds) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Long tableId : tableIds) {
                GenTableDO table = requireTable(tableId);
                VelocityContext context = prepareContext(table);
                for (String[] tpl : templateList()) {
                    String code = renderTemplate(tpl[0], context);
                    String filePath = zipEntryPath(table, tpl[1]);
                    zos.putNextEntry(new ZipEntry(filePath));
                    IoUtil.writeUtf8(zos, false, code);
                    zos.closeEntry();
                }
            }
        } catch (Exception e) {
            log.error("生成代码失败", e);
            throw new RuntimeException("生成代码失败");
        }
        return baos.toByteArray();
    }

    @Override
    public List<String> previewGenerateFiles(Long tableId) {
        GenTableDO table = requireTable(tableId);
        String projectRoot = resolveProjectRoot();
        VelocityContext context = prepareContext(table);
        List<String> files = new ArrayList<>();
        for (String[] tpl : templateList()) {
            String path = projectFilePath(table, tpl[1], projectRoot);
            if (path == null) {
                continue;
            }
            String rel = relativePath(projectRoot, path);
            File file = new File(path);
            if (!file.exists()) {
                files.add("[新建] " + rel);
                continue;
            }
            String code = renderTemplate(tpl[0], context);
            String existing = FileUtil.readUtf8String(file);
            if (existing.equals(code)) {
                files.add("[覆盖·无变化] " + rel);
            } else {
                files.add("[覆盖·已修改] " + rel);
            }
        }
        String menuPath = menuPath(table);
        if (menuMapper.selectCount(new LambdaQueryWrapper<MenuDO>()
                .eq(MenuDO::getPath, menuPath)) == 0) {
            files.add("[数据库] 将创建菜单: " + table.getFunctionName());
        } else {
            files.add("[数据库] 菜单已存在，将跳过创建");
        }
        return files;
    }

    @Override
    public List<String> generateToProject(Long tableId, boolean overwrite) {
        GenTableDO table = requireTable(tableId);
        String projectRoot = resolveProjectRoot();
        VelocityContext context = prepareContext(table);
        List<PendingFile> pendingFiles = new ArrayList<>();
        for (String[] tpl : templateList()) {
            String path = projectFilePath(table, tpl[1], projectRoot);
            if (path == null) {
                continue;
            }
            String code = renderTemplate(tpl[0], context);
            File file = new File(path);
            if (file.exists() && !overwrite) {
                String existing = FileUtil.readUtf8String(file);
                if (!existing.equals(code)) {
                    throw new IllegalStateException(
                            "文件已存在且内容已修改，请确认后覆盖: " + relativePath(projectRoot, path));
                }
            }
            pendingFiles.add(new PendingFile(path, code));
        }

        List<String> generated = new ArrayList<>();
        for (PendingFile pending : pendingFiles) {
            File file = new File(pending.path);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            FileUtil.writeUtf8String(pending.code, file);
            generated.add(relativePath(projectRoot, pending.path));
            log.info("生成文件: {}", pending.path);
        }

        runInTransaction(() -> createMenus(table));
        generated.add("[数据库] 菜单数据已自动创建");
        return generated;
    }

    @Override
    public List<String> previewRemoveFiles(Long tableId) {
        GenTableDO table = requireTable(tableId);
        String projectRoot = resolveProjectRoot();
        List<String> files = new ArrayList<>();
        for (String[] tpl : templateList()) {
            String path = projectFilePath(table, tpl[1], projectRoot);
            if (path != null && new File(path).exists()) {
                files.add(relativePath(projectRoot, path));
            }
        }
        String menuPath = menuPath(table);
        if (menuMapper.selectCount(new LambdaQueryWrapper<MenuDO>()
                .eq(MenuDO::getPath, menuPath)) > 0) {
            files.add("[数据库] 将删除菜单: " + table.getFunctionName());
        }
        if (files.isEmpty()) {
            files.add("没有找到需要删除的文件");
        }
        return files;
    }

    @Override
    public List<String> removeGeneratedCode(Long tableId) {
        GenTableDO table = requireTable(tableId);
        String projectRoot = resolveProjectRoot();
        List<String> paths = new ArrayList<>();
        for (String[] tpl : templateList()) {
            String path = projectFilePath(table, tpl[1], projectRoot);
            if (path != null) {
                paths.add(path);
            }
        }

        List<String> removed = new ArrayList<>();
        for (String path : paths) {
            File file = new File(path);
            if (file.exists() && file.delete()) {
                removed.add(relativePath(projectRoot, path));
                if (path.endsWith(".vue")) {
                    File parent = file.getParentFile();
                    if (parent != null && parent.isDirectory() && parent.list() != null && parent.list().length == 0) {
                        parent.delete();
                    }
                }
            }
        }

        runInTransaction(() -> removeMenus(table));
        removed.add("[数据库] 菜单数据已删除");
        return removed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncTable(Long tableId) {
        GenTableDO table = genTableMapper.selectById(tableId);
        if (table == null) {
            throw new RuntimeException("表不存在");
        }
        List<DatabaseColumnVO> dbColumns = genTableMapper.selectDbColumnsByTableName(table.getTableName());
        List<GenTableColumnDO> existColumns = genTableColumnMapper.selectByTableId(tableId);
        Map<String, GenTableColumnDO> existMap = new HashMap<>();
        for (GenTableColumnDO col : existColumns) {
            existMap.put(col.getColumnName(), col);
        }
        for (DatabaseColumnVO dbCol : dbColumns) {
            GenTableColumnDO exist = existMap.remove(dbCol.getColumnName());
            if (exist != null) {
                exist.setColumnType(dbCol.getColumnType());
                exist.setJavaType(toJavaType(dbCol.getDataType()));
                applyDeleteFieldRules(exist);
                genTableColumnMapper.updateById(exist);
            } else {
                genTableColumnMapper.insert(buildColumnFromDb(tableId, dbCol));
            }
        }
        for (GenTableColumnDO orphan : existMap.values()) {
            genTableColumnMapper.deleteById(orphan.getId());
        }
    }

    private void applyDeleteFieldRules(GenTableColumnDO col) {
        String javaField = col.getJavaField();
        String columnNameLower = col.getColumnName().toLowerCase();
        boolean isDeleteField = DELETE_MARK_FIELDS.contains(javaField)
                || columnNameLower.contains("deleted") || columnNameLower.contains("del_flag");
        if (isDeleteField) {
            col.setIsInsert(0);
            col.setIsEdit(0);
            col.setIsList(0);
            col.setIsQuery(0);
        }
    }

    private void createMenus(GenTableDO table) {
        String businessName = table.getBusinessName();
        String functionName = table.getFunctionName();
        String moduleName = resolveModuleName(table);
        String permPrefix = permissionPrefix(table);
        String menuPath = menuPath(table);
        Long parentId = table.getParentMenuId() != null ? table.getParentMenuId() : DEFAULT_PARENT_MENU_ID;

        if (menuMapper.selectCount(new LambdaQueryWrapper<MenuDO>().eq(MenuDO::getPath, menuPath)) > 0) {
            log.info("菜单已存在，跳过: {}", menuPath);
            return;
        }

        MenuDO mainMenu = new MenuDO();
        mainMenu.setParentId(parentId);
        mainMenu.setName(functionName);
        mainMenu.setType(2);
        mainMenu.setPath(menuPath);
        mainMenu.setComponent(moduleName + "/" + businessName + "/index");
        mainMenu.setPermission(permPrefix + ":list");
        mainMenu.setIcon("List");
        mainMenu.setSort(99);
        mainMenu.setStatus(1);
        menuMapper.insert(mainMenu);
        authorizeSuperAdmin(mainMenu.getId());

        String[][] buttons = {
                {functionName + "查询", permPrefix + ":query", "1"},
                {functionName + "新增", permPrefix + ":create", "2"},
                {functionName + "修改", permPrefix + ":update", "3"},
                {functionName + "删除", permPrefix + ":delete", "4"},
        };
        for (String[] btn : buttons) {
            MenuDO btnMenu = new MenuDO();
            btnMenu.setParentId(mainMenu.getId());
            btnMenu.setName(btn[0]);
            btnMenu.setType(3);
            btnMenu.setPermission(btn[1]);
            btnMenu.setSort(Integer.parseInt(btn[2]));
            btnMenu.setStatus(1);
            menuMapper.insert(btnMenu);
            authorizeSuperAdmin(btnMenu.getId());
        }
    }

    private void removeMenus(GenTableDO table) {
        String menuPath = menuPath(table);
        MenuDO mainMenu = menuMapper.selectOne(new LambdaQueryWrapper<MenuDO>()
                .eq(MenuDO::getPath, menuPath).last("LIMIT 1"));
        if (mainMenu == null) {
            return;
        }
        menuMapper.delete(new LambdaQueryWrapper<MenuDO>().eq(MenuDO::getParentId, mainMenu.getId()));
        menuMapper.deleteById(mainMenu.getId());
    }

    private void authorizeSuperAdmin(Long menuId) {
        RoleDO role = roleMapper.selectOne(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getCode, "super_admin").last("LIMIT 1"));
        if (role == null) {
            return;
        }
        long count = roleMenuMapper.selectCount(new LambdaQueryWrapper<RoleMenuDO>()
                .eq(RoleMenuDO::getRoleId, role.getId())
                .eq(RoleMenuDO::getMenuId, menuId));
        if (count > 0) {
            return;
        }
        RoleMenuDO rm = new RoleMenuDO();
        rm.setRoleId(role.getId());
        rm.setMenuId(menuId);
        roleMenuMapper.insert(rm);
    }

    private GenTableDO requireTable(Long tableId) {
        GenTableDO table = getTableById(tableId);
        if (table == null || table.getPkColumn() == null) {
            throw new RuntimeException("表不存在或未配置主键");
        }
        return table;
    }

    private VelocityContext prepareContext(GenTableDO table) {
        VelocityContext context = new VelocityContext();
        context.put("tableName", table.getTableName());
        context.put("tableComment", table.getTableComment());
        context.put("className", table.getClassName());
        context.put("classname", StrUtil.lowerFirst(table.getClassName()));
        context.put("packageName", table.getPackageName());
        context.put("moduleName", table.getModuleName());
        context.put("businessName", table.getBusinessName());
        context.put("functionName", table.getFunctionName());
        context.put("author", table.getAuthor());
        context.put("datetime", java.time.LocalDate.now().toString());
        context.put("columns", table.getColumns());
        context.put("pkColumn", table.getPkColumn());
        context.put("table", table);
        context.put("formLayout", table.getFormLayout() != null ? table.getFormLayout() : "vertical");
        context.put("permPrefix", table.getModuleName() + ":" + table.getBusinessName());
        context.put("baseFields", BASE_ENTITY_FIELDS);

        boolean hasBigDecimal = false;
        boolean hasLocalDateTime = false;
        boolean hasLocalDate = false;
        for (GenTableColumnDO col : table.getColumns()) {
            if ("BigDecimal".equals(col.getJavaType())) hasBigDecimal = true;
            if ("LocalDateTime".equals(col.getJavaType())) hasLocalDateTime = true;
            if ("LocalDate".equals(col.getJavaType())) hasLocalDate = true;
        }
        context.put("hasBigDecimal", hasBigDecimal);
        context.put("hasLocalDateTime", hasLocalDateTime);
        context.put("hasLocalDate", hasLocalDate);
        return context;
    }

    private List<String[]> templateList() {
        return List.of(
                new String[]{"templates/gen/java/do.java.vm", "DO.java"},
                new String[]{"templates/gen/java/mapper.java.vm", "Mapper.java"},
                new String[]{"templates/gen/java/service.java.vm", "Service.java"},
                new String[]{"templates/gen/java/serviceImpl.java.vm", "ServiceImpl.java"},
                new String[]{"templates/gen/java/controller.java.vm", "Controller.java"},
                new String[]{"templates/gen/vue/api.ts.vm", "api.ts"},
                new String[]{"templates/gen/vue/index.vue.vm", "index.vue"}
        );
    }

    private String renderTemplate(String templatePath, VelocityContext context) {
        try {
            Template template = Velocity.getTemplate(templatePath, "UTF-8");
            StringWriter writer = new StringWriter();
            template.merge(context, writer);
            return writer.toString();
        } catch (Exception e) {
            log.error("渲染模板失败: {}", templatePath, e);
            return "// 模板渲染失败: " + e.getMessage();
        }
    }

    private String resolveProjectRoot() {
        String root = System.getProperty("user.dir");
        if (root.endsWith("backend")) {
            return new File(root).getParent();
        }
        return root;
    }

    private String projectFilePath(GenTableDO table, String fileName, String projectRoot) {
        String className = table.getClassName();
        String businessName = table.getBusinessName();
        String moduleName = resolveModuleName(table);
        String javaRoot = projectRoot + "/backend/src/main/java/cn/rbac/server/modules/system";
        String feRoot = projectRoot + "/frontend/src";
        return switch (fileName) {
            case "DO.java" -> javaRoot + "/dal/dataobject/" + businessName + "/" + className + "DO.java";
            case "Mapper.java" -> javaRoot + "/dal/mysql/" + businessName + "/" + className + "Mapper.java";
            case "Service.java" -> javaRoot + "/service/" + businessName + "/" + className + "Service.java";
            case "ServiceImpl.java" -> javaRoot + "/service/" + businessName + "/impl/" + className + "ServiceImpl.java";
            case "Controller.java" -> javaRoot + "/api/" + businessName + "/" + className + "Controller.java";
            case "api.ts" -> feRoot + "/api/" + moduleName + "/" + businessName + ".ts";
            case "index.vue" -> feRoot + "/views/" + moduleName + "/" + businessName + "/index.vue";
            default -> null;
        };
    }

    private String zipEntryPath(GenTableDO table, String fileName) {
        String businessName = table.getBusinessName();
        String className = table.getClassName();
        String moduleName = resolveModuleName(table);
        return switch (fileName) {
            case "DO.java" -> "backend/dal/dataobject/" + businessName + "/" + className + "DO.java";
            case "Mapper.java" -> "backend/dal/mysql/" + businessName + "/" + className + "Mapper.java";
            case "Service.java" -> "backend/service/" + businessName + "/" + className + "Service.java";
            case "ServiceImpl.java" -> "backend/service/" + businessName + "/impl/" + className + "ServiceImpl.java";
            case "Controller.java" -> "backend/api/" + businessName + "/" + className + "Controller.java";
            case "api.ts" -> "frontend/api/" + moduleName + "/" + businessName + ".ts";
            case "index.vue" -> "frontend/views/" + moduleName + "/" + businessName + "/index.vue";
            default -> fileName;
        };
    }

    private String relativePath(String projectRoot, String absolutePath) {
        return absolutePath.replace(projectRoot, "").replace("\\", "/");
    }

    private String toClassName(String tableName) {
        String name = tableName;
        while (name.contains("_")) {
            String prefix = name.substring(0, name.indexOf('_'));
            if (TABLE_PREFIXES.contains(prefix.toLowerCase())) {
                name = name.substring(name.indexOf('_') + 1);
            } else {
                break;
            }
        }
        return toPascalCase(name);
    }

    private void runInTransaction(Runnable action) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.executeWithoutResult(status -> action.run());
    }

    private String resolveModuleName(GenTableDO table) {
        return StrUtil.isNotBlank(table.getModuleName()) ? table.getModuleName() : "system";
    }

    private String permissionPrefix(GenTableDO table) {
        return resolveModuleName(table) + ":" + table.getBusinessName();
    }

    private String menuPath(GenTableDO table) {
        return "/" + resolveModuleName(table) + "/" + table.getBusinessName();
    }

    private record PendingFile(String path, String code) {}

    private String toBusinessName(String tableName) {
        return StrUtil.lowerFirst(toClassName(tableName));
    }

    private String toCamelCase(String name) {
        if (name == null || name.isEmpty()) return name;
        StringBuilder result = new StringBuilder();
        boolean upperNext = false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '_') {
                upperNext = true;
            } else if (upperNext) {
                result.append(Character.toUpperCase(c));
                upperNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }

    private String toPascalCase(String name) {
        String camel = toCamelCase(name);
        if (camel == null || camel.isEmpty()) return camel;
        return Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
    }

    private String toJavaType(String dbType) {
        return TYPE_MAP.getOrDefault(dbType.toLowerCase(), "String");
    }

    private String resolveHtmlType(GenTableColumnDO column) {
        String javaType = column.getJavaType();
        String columnName = column.getColumnName().toLowerCase();
        if (columnName.contains("status") || columnName.contains("type") || columnName.contains("sex")) {
            return "select";
        }
        if (columnName.contains("image") || columnName.contains("avatar")) {
            return "imageUpload";
        }
        if (columnName.contains("content") || columnName.contains("remark") || columnName.contains("desc")) {
            return "textarea";
        }
        if ("LocalDateTime".equals(javaType) || "LocalDate".equals(javaType)) {
            return "datetime";
        }
        return "input";
    }

    private String resolveDictType(GenTableColumnDO column) {
        if (!"select".equals(column.getHtmlType())) {
            return "";
        }
        String columnName = column.getColumnName().toLowerCase();
        if (columnName.contains("status")) {
            return "sys_status";
        }
        if (columnName.contains("sex") || columnName.contains("gender")) {
            return "sys_user_sex";
        }
        return "";
    }
}
