package cn.rbac.server.testsupport;

import cn.rbac.server.modules.system.dal.dataobject.approval.ApprovalFormDO;
import cn.rbac.server.modules.system.dal.dataobject.dept.DeptDO;
import cn.rbac.server.modules.system.dal.dataobject.permission.MenuDO;
import cn.rbac.server.modules.system.dal.dataobject.permission.RoleDO;
import cn.rbac.server.modules.system.dal.dataobject.permission.RoleMenuDO;
import cn.rbac.server.modules.system.dal.dataobject.permission.UserRoleDO;
import cn.rbac.server.modules.system.dal.dataobject.ticket.TicketDO;
import cn.rbac.server.modules.system.dal.dataobject.user.UserDO;

/**
 * 单元测试公共数据构造。
 */
public final class ServiceTestFixtures {

    private ServiceTestFixtures() {
    }

    public static UserDO user(long id, String username, int status) {
        UserDO user = new UserDO();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(username);
        user.setStatus(status);
        user.setPassword("$2a$10$encoded");
        return user;
    }

    public static UserRoleDO userRole(long userId, long roleId) {
        UserRoleDO link = new UserRoleDO();
        link.setUserId(userId);
        link.setRoleId(roleId);
        return link;
    }

    public static RoleMenuDO roleMenu(long roleId, long menuId) {
        RoleMenuDO link = new RoleMenuDO();
        link.setRoleId(roleId);
        link.setMenuId(menuId);
        return link;
    }

    public static RoleDO role(long id, String code) {
        RoleDO role = new RoleDO();
        role.setId(id);
        role.setCode(code);
        role.setStatus(1);
        return role;
    }

    public static MenuDO menu(long id, long parentId, int status, String permission) {
        MenuDO menu = new MenuDO();
        menu.setId(id);
        menu.setParentId(parentId);
        menu.setStatus(status);
        menu.setPermission(permission);
        menu.setSort((int) id);
        return menu;
    }

    public static DeptDO dept(long id, long parentId, String name) {
        DeptDO dept = new DeptDO();
        dept.setId(id);
        dept.setParentId(parentId);
        dept.setName(name);
        dept.setStatus(1);
        dept.setSort((int) id);
        dept.setAncestors(parentId == 0L ? "0" : "0," + parentId);
        return dept;
    }

    public static ApprovalFormDO approvalForm(long id, long applicantUserId, String status) {
        ApprovalFormDO form = new ApprovalFormDO();
        form.setId(id);
        form.setFormType("REGISTER");
        form.setApplicantUserId(applicantUserId);
        form.setStatus(status);
        form.setContent("{\"bizType\":\"USER_REGISTER\",\"userId\":" + applicantUserId + "}");
        return form;
    }

    public static TicketDO ticket(long id, long assigneeUserId, String status) {
        TicketDO ticket = new TicketDO();
        ticket.setId(id);
        ticket.setAssigneeUserId(assigneeUserId);
        ticket.setStatus(status);
        ticket.setTitle("test ticket");
        return ticket;
    }
}
