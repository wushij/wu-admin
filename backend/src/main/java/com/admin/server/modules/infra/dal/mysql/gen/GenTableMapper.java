package com.admin.server.modules.infra.dal.mysql.gen;

import com.admin.server.modules.infra.dal.dataobject.gen.DatabaseColumnVO;
import com.admin.server.modules.infra.dal.dataobject.gen.DatabaseTableVO;
import com.admin.server.modules.infra.dal.dataobject.gen.GenTableDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface GenTableMapper extends BaseMapper<GenTableDO> {

    @Select("""
        <script>
        SELECT table_name AS tableName, table_comment AS tableComment,
               create_time AS createTime, update_time AS updateTime
        FROM information_schema.tables
        WHERE table_schema = (SELECT DATABASE())
          AND table_type = 'BASE TABLE'
          AND table_name NOT LIKE 'gen_%'
          AND table_name NOT IN (SELECT table_name FROM gen_table WHERE IFNULL(deleted, 0) = 0)
          <if test="tableName != null and tableName != ''">
            AND table_name LIKE CONCAT('%', #{tableName}, '%')
          </if>
        ORDER BY create_time DESC
        LIMIT #{offset}, #{limit}
        </script>
        """)
    List<DatabaseTableVO> selectDbTableList(@Param("tableName") String tableName,
                                            @Param("offset") int offset,
                                            @Param("limit") int limit);

    @Select("""
        <script>
        SELECT COUNT(*)
        FROM information_schema.tables
        WHERE table_schema = (SELECT DATABASE())
          AND table_type = 'BASE TABLE'
          AND table_name NOT LIKE 'gen_%'
          AND table_name NOT IN (SELECT table_name FROM gen_table WHERE IFNULL(deleted, 0) = 0)
          <if test="tableName != null and tableName != ''">
            AND table_name LIKE CONCAT('%', #{tableName}, '%')
          </if>
        </script>
        """)
    long countDbTable(@Param("tableName") String tableName);

    @Select("""
        SELECT table_name AS tableName, table_comment AS tableComment,
               create_time AS createTime, update_time AS updateTime
        FROM information_schema.tables
        WHERE table_schema = (SELECT DATABASE())
          AND table_type = 'BASE TABLE'
          AND table_name = #{tableName}
        """)
    DatabaseTableVO selectDbTableByName(@Param("tableName") String tableName);

    @Select("""
        SELECT column_name AS columnName, column_comment AS columnComment,
               data_type AS dataType, column_type AS columnType,
               is_nullable AS isNullable, column_key AS columnKey,
               extra, ordinal_position AS ordinalPosition
        FROM information_schema.columns
        WHERE table_schema = (SELECT DATABASE())
          AND table_name = #{tableName}
        ORDER BY ordinal_position
        """)
    List<DatabaseColumnVO> selectDbColumnsByTableName(@Param("tableName") String tableName);

    @Select({
            "<script>",
            "SELECT * FROM gen_table",
            "WHERE deleted = 1",
            "<if test='tableName != null and tableName != \"\"'>",
            "  AND table_name LIKE CONCAT('%', #{tableName}, '%')",
            "</if>",
            "ORDER BY update_time DESC",
            "</script>"
    })
    IPage<GenTableDO> selectDeletedPage(Page<GenTableDO> page, @Param("tableName") String tableName);

    @Update("UPDATE gen_table SET deleted = 0, update_time = NOW() WHERE id = #{id} AND deleted = 1")
    int restoreById(@Param("id") Long id);

    @Update("UPDATE gen_table SET deleted = 1, update_time = NOW() WHERE id = #{id} AND IFNULL(deleted, 0) = 0")
    int softDeleteById(@Param("id") Long id);

    @Delete("DELETE FROM gen_table WHERE id = #{id} AND deleted = 1")
    int deletePhysicalById(@Param("id") Long id);
}
