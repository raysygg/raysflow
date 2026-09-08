package com.acme.agentstudio.application.workspace;

import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.common.BusinessStatus;
import com.acme.agentstudio.infrastructure.persistence.entity.SysMenuEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SysRoleEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SysRoleMenuEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysMenuMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysRoleMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysRoleMenuMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 客户端前端工作区功能能力包（Workspace Capability & Navigation）授权服务。
 * 负责依据用户的角色集合（Roles）在后端动态计算并输出菜单导航结构、功能代码开关（Capabilities）与工作台组件分布，确保前端不能仅凭隐藏链接越权访问。
 */
@Service
public class WorkspaceCapabilityApplicationService {

    /** 平台控制台类型 */
    private static final String PLATFORM = "PLATFORM";

    /** 租户控制台类型 */
    private static final String TENANT = "TENANT";

    /** 系统角色 Mapper */
    private final SysRoleMapper roleMapper;

    /** 角色-菜单关联 Mapper */
    private final SysRoleMenuMapper roleMenuMapper;

    /** 系统菜单 Mapper */
    private final SysMenuMapper menuMapper;

    /**
     * 构造函数注入角色与菜单 Mapper 依赖。
     */
    public WorkspaceCapabilityApplicationService(SysRoleMapper roleMapper,
                                                 SysRoleMenuMapper roleMenuMapper,
                                                 SysMenuMapper menuMapper) {
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.menuMapper = menuMapper;
    }

    /**
     * 根据当前登录用户的角色配置，动态算得包含按钮开关、动态菜单导航与组件分布的能力包字典。
     *
     * @param user 当前登录用户
     * @return 包含 consoleType, tenantId, capabilities, navigation, homeWidgets 的能力包对象
     */
    public Map<String, Object> getCapabilities(SecurityUser user) {
        if (user == null || user.getTenantId() == null) {
            throw new IllegalArgumentException("当前登录身份状态无效，无法计算工作区能力包。");
        }
        boolean superAdmin = user.hasRole("SUPER_ADMIN");
        boolean admin = superAdmin || user.hasRole("ADMIN");
        boolean designer = admin || user.hasRole("DESIGNER") || user.hasRole("OPERATOR");
        boolean approver = admin || user.hasRole("APPROVER") || user.hasRole("OPERATOR");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("consoleType", superAdmin ? PLATFORM : TENANT);
        result.put("tenantId", user.getTenantId());
        result.put("roleCodes", user.getRoles());
        result.put("capabilities", List.of(
                capability("APPLICATION_VIEW", true),
                capability("APPLICATION_DESIGN", designer),
                capability("APPLICATION_PUBLISH", admin || user.hasRole("PUBLISHER")),
                capability("RUN_VIEW", true),
                capability("TASK_APPROVE", approver),
                capability("ORGANIZATION_MANAGE", admin),
                capability("TENANT_MANAGE", superAdmin),
                capability("PLATFORM_RESOURCE_MANAGE", superAdmin),
                capability("PLATFORM_AUDIT", superAdmin)
        ));
        result.put("navigation", configuredNavigation(user));
        result.put("homeWidgets", superAdmin
                ? List.of("TENANT_HEALTH", "PLATFORM_RISK", "RESOURCE_AVAILABILITY", "PLATFORM_AUDIT")
                : List.of("AVAILABLE_APPLICATIONS", "MY_REQUESTS", "ASSIGNED_TASKS", "APPLICATION_HEALTH"));
        return result;
    }

    /**
     * 根据租户和角色的关联菜单导出实际可用的动态导航菜单。
     */
    private List<Map<String, Object>> configuredNavigation(SecurityUser user) {
        Set<String> roleCodes = user.getRoles();
        List<SysRoleEntity> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRoleEntity>()
                .eq(SysRoleEntity::getTenantId, user.getTenantId())
                .eq(SysRoleEntity::getStatus, BusinessStatus.ACTIVE)
                .in(!roleCodes.isEmpty(), SysRoleEntity::getRoleCode, roleCodes));
        if (roles.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = roles.stream().map(SysRoleEntity::getId).toList();
        List<Long> menuIds = roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenuEntity>()
                        .in(SysRoleMenuEntity::getRoleId, roleIds))
                .stream().map(SysRoleMenuEntity::getMenuId).distinct().toList();
        if (menuIds.isEmpty()) {
            return List.of();
        }
        return menuMapper.selectList(new LambdaQueryWrapper<SysMenuEntity>()
                        .in(SysMenuEntity::getId, menuIds)
                        .eq(SysMenuEntity::getStatus, BusinessStatus.ACTIVE)
                        .eq(SysMenuEntity::getMenuType, "C")
                        .isNotNull(SysMenuEntity::getPath)
                        .orderByAsc(SysMenuEntity::getSortOrder)
                        .orderByAsc(SysMenuEntity::getId))
                .stream().map(this::navigationItem).toList();
    }

    /**
     * 构建单个导航菜单项数据。
     */
    private Map<String, Object> navigationItem(SysMenuEntity menu) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("path", menu.getPath());
        item.put("label", menu.getMenuName());
        item.put("capability", menu.getPerms());
        item.put("icon", menu.getIcon());
        item.put("enabled", true);
        return item;
    }

    /**
     * 构建单个功能能力项。
     */
    private Map<String, Object> capability(String code, boolean enabled) {
        return Map.of("code", code, "enabled", enabled);
    }
}

