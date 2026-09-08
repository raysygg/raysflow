package com.acme.agentstudio.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.acme.agentstudio.common.response.ApiResponse;
import com.acme.agentstudio.common.util.SystemIdentifierGenerator;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.common.BusinessStatus;
import com.acme.agentstudio.infrastructure.persistence.entity.SysMenuEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SysRoleEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SysRoleMenuEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SysUserEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SysUserRoleEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.IamUserRoleBindingEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysMenuMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysRoleMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysRoleMenuMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysUserMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysUserRoleMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.IamUserRoleBindingMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 身份与访问控制（IAM）REST 控制器。
 * 负责提供角色 CRUD、菜单功能权限树配置、数据/审批范围授权、角色复制、成员角色分配（绑定与解绑）以及有效权限查询接口。
 */
@Tag(name = "身份与访问管理", description = "用户、角色、菜单和组织权限管理")
@RestController
@RequestMapping("/api/iam")
public class IamController {

    /** 角色 Mapper */
    private final SysRoleMapper roleMapper;

    /** 菜单 Mapper */
    private final SysMenuMapper menuMapper;

    /** 角色菜单关联 Mapper */
    private final SysRoleMenuMapper roleMenuMapper;

    /** 系统用户 Mapper */
    private final SysUserMapper userMapper;

    /** 系统用户角色 Mapper */
    private final SysUserRoleMapper userRoleMapper;

    /** IAM 用户角色绑定 Mapper */
    private final IamUserRoleBindingMapper roleBindingMapper;

    /**
     * 构造函数注入 IAM 实体 Mapper。
     */
    public IamController(SysRoleMapper roleMapper, SysMenuMapper menuMapper,
                          SysRoleMenuMapper roleMenuMapper, SysUserMapper userMapper,
                          SysUserRoleMapper userRoleMapper, IamUserRoleBindingMapper roleBindingMapper) {
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleBindingMapper = roleBindingMapper;
    }

    /**
     * 获取当前租户下的所有角色及其关联的菜单与成员数量统计。
     *
     * @param user 当前登录用户
     * @return 角色列表详情
     */
    @Operation(summary = "获取角色列表", description = "获取当前企业租户下所有注册的角色及其关联的菜单与成员数量。")
    @GetMapping("/roles")
    public ApiResponse<?> listRoles(@AuthenticationPrincipal SecurityUser user) {
        List<SysRoleEntity> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRoleEntity>()
                .eq(SysRoleEntity::getTenantId, user.getTenantId())
                .orderByAsc(SysRoleEntity::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysRoleEntity role : roles) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", role.getId());
            item.put("roleCode", role.getRoleCode());
            item.put("roleName", role.getRoleName());
            item.put("status", role.getStatus());
            item.put("dataScopeJson", role.getDataScopeJson());
            item.put("approvalScopeJson", role.getApprovalScopeJson());
            item.put("menuIds", roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenuEntity>()
                    .eq(SysRoleMenuEntity::getRoleId, role.getId())).stream().map(SysRoleMenuEntity::getMenuId).toList());
            item.put("userCount", countUsers(role.getId(), user.getTenantId()));
            result.add(item);
        }
        return ApiResponse.ok(result);
    }

    /**
     * 获取系统支持的所有菜单与功能权限树。
     *
     * @param user 当前登录用户
     * @return 菜单列表数据
     */
    @Operation(summary = "获取菜单与功能权限树", description = "获取系统可用的菜单项和功能按钮列表。")
    @GetMapping("/menus")
    public ApiResponse<?> listMenus(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(menuMapper.selectList(new LambdaQueryWrapper<SysMenuEntity>()
                .eq(SysMenuEntity::getStatus, BusinessStatus.ACTIVE)
                .orderByAsc(SysMenuEntity::getSortOrder)
                .orderByAsc(SysMenuEntity::getId)));
    }

    /** 保存角色配置请求载荷 */
    public record SaveRoleRequest(String roleCode, String roleName, String status) {
        public SaveRoleRequest {
            if (roleCode == null || roleCode.isBlank()) {
                roleCode = SystemIdentifierGenerator.fromName(roleName, "role");
            } else {
                roleCode = roleCode.trim();
            }
        }
    }

    /** 保存角色菜单请求载荷 */
    public record SaveRoleMenusRequest(List<Long> menuIds) {}

    /** 分配单角色请求载荷 */
    public record AssignRoleRequest(Long roleId, Boolean primary, String sourceCode,
                                    LocalDateTime validFrom, LocalDateTime validUntil, String status) {}

    /** 批量分配角色请求载荷 */
    public record AssignRolesRequest(List<AssignRoleRequest> roles) {}

    /** 复制角色请求载荷 */
    public record CopyRoleRequest(String roleCode, String roleName) {}

    /** 保存角色授权范围请求载荷 */
    public record SaveRoleScopeRequest(String dataScopeJson, String approvalScopeJson) {}

    /**
     * 配置并保存角色的数据权限控制规则与审批流范围 JSON。
     *
     * @param user 当前登录用户
     * @param id 角色 ID
     * @param request 包含 dataScopeJson 和 approvalScopeJson 的配置
     * @return 配置结果
     */
    @Operation(summary = "保存角色数据范围", description = "配置针对数据查看与审批流权限的自定义范围。")
    @PutMapping("/roles/{id}/scope")
    public ApiResponse<?> saveRoleScope(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id,
                                         @RequestBody SaveRoleScopeRequest request) {
        if (!canManage(user)) {
            return ApiResponse.fail("无权维护角色范围");
        }
        SysRoleEntity role = findRole(user.getTenantId(), id);
        if (role == null || request == null) {
            return ApiResponse.fail("角色不存在或配置无效");
        }
        role.setDataScopeJson(request.dataScopeJson());
        role.setApprovalScopeJson(request.approvalScopeJson());
        role.setUpdatedAt(LocalDateTime.now());
        roleMapper.updateById(role);
        return ApiResponse.ok("角色数据范围已保存", Map.of("roleId", id,
                "dataScopeJson", request.dataScopeJson() == null ? "{}" : request.dataScopeJson(),
                "approvalScopeJson", request.approvalScopeJson() == null ? "{}" : request.approvalScopeJson()));
    }

    /**
     * 查询特定角色（或配合特定成员）计算后的最终生效权限。
     *
     * @param user 当前登录用户
     * @param id 角色 ID
     * @param userId 目标成员 ID（可选）
     * @return 最终计算出的菜单列表与范围控制配置
     */
    @Operation(summary = "获取有效权限列表", description = "计算并展示指定角色或成员在当前租户下的最终合并权限。")
    @GetMapping("/roles/{id}/effective-permissions")
    public ApiResponse<?> effectivePermissions(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id,
                                                @RequestParam(required = false) Long userId) {
        if (!canManage(user)) {
            return ApiResponse.fail("无权查看有效权限");
        }
        SysRoleEntity role = findRole(user.getTenantId(), id);
        if (role == null) {
            return ApiResponse.fail("角色不存在");
        }
        if (userId != null && userMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getTenantId, user.getTenantId()).eq(SysUserEntity::getId, userId)) == null) {
            return ApiResponse.fail("成员不存在或不属于当前租户");
        }
        return ApiResponse.ok(Map.of(
                "userId", userId == null ? "" : userId,
                "roleId", id,
                "roleName", role.getRoleName(),
                "menuIds", roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenuEntity>()
                        .eq(SysRoleMenuEntity::getRoleId, id)).stream().map(SysRoleMenuEntity::getMenuId).toList(),
                "dataScopeJson", role.getDataScopeJson() == null ? "{}" : role.getDataScopeJson(),
                "approvalScopeJson", role.getApprovalScopeJson() == null ? "{}" : role.getApprovalScopeJson()
        ));
    }

    /**
     * 在当前租户下创建新的权限角色。
     *
     * @param user 当前登录用户
     * @param request 包含角色编码与角色名称的请求
     * @return 新建立的角色 ID
     */
    @Operation(summary = "创建企业角色", description = "在当前租户下创建新的权限角色。")
    @PostMapping("/roles")
    public ApiResponse<?> createRole(@AuthenticationPrincipal SecurityUser user, @RequestBody SaveRoleRequest request) {
        if (!canManage(user)) {
            return ApiResponse.fail("无权限维护企业角色。");
        }
        if (request.roleCode() == null || request.roleCode().isBlank()
                || request.roleName() == null || request.roleName().isBlank()) {
            return ApiResponse.fail("角色编码和角色名称不能为空。");
        }
        Long duplicate = roleMapper.selectCount(new LambdaQueryWrapper<SysRoleEntity>()
                .eq(SysRoleEntity::getTenantId, user.getTenantId()).eq(SysRoleEntity::getRoleCode, request.roleCode().trim()));
        if (duplicate > 0) {
            return ApiResponse.fail("当前企业已存在相同角色编码。");
        }
        SysRoleEntity role = new SysRoleEntity();
        role.setTenantId(user.getTenantId());
        role.setRoleCode(request.roleCode().trim());
        role.setRoleName(request.roleName().trim());
        role.setStatus(request.status() == null || request.status().isBlank() ? BusinessStatus.ACTIVE : request.status());
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        roleMapper.insert(role);
        return ApiResponse.ok("企业角色创建成功。", role.getId());
    }

    /**
     * 修改已有角色的显示名称与启用状态。
     *
     * @param user 当前登录用户
     * @param id 角色 ID
     * @param request 包含更新内容的请求
     * @return 操作成功响应
     */
    @Operation(summary = "更新企业角色", description = "修改已有角色的显示名称或禁用状态。")
    @PutMapping("/roles/{id}")
    public ApiResponse<?> updateRole(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id,
                                      @RequestBody SaveRoleRequest request) {
        if (!canManage(user)) {
            return ApiResponse.fail("无权限维护企业角色。");
        }
        SysRoleEntity role = roleMapper.selectOne(new LambdaQueryWrapper<SysRoleEntity>()
                .eq(SysRoleEntity::getId, id).eq(SysRoleEntity::getTenantId, user.getTenantId()));
        if (role == null) {
            return ApiResponse.fail("角色不存在或不属于当前企业。");
        }
        if (request.roleName() == null || request.roleName().isBlank()) {
            return ApiResponse.fail("角色名称不能为空。");
        }
        role.setRoleName(request.roleName().trim());
        if (request.status() != null && !request.status().isBlank()) {
            role.setStatus(request.status());
        }
        role.setUpdatedAt(LocalDateTime.now());
        roleMapper.updateById(role);
        return ApiResponse.ok("企业角色已更新。", null);
    }

    /**
     * 为指定角色绑定与分配菜单功能权限。
     *
     * @param user 当前登录用户
     * @param id 角色 ID
     * @param request 菜单 ID 列表
     * @return 保存成功响应
     */
    @Operation(summary = "配置角色菜单权限", description = "为指定角色分配和保存绑定的菜单及功能操作。")
    @PutMapping("/roles/{id}/menus")
    public ApiResponse<?> saveRoleMenus(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id,
                                         @RequestBody SaveRoleMenusRequest request) {
        if (!canManage(user)) {
            return ApiResponse.fail("无权限维护企业角色权限。");
        }
        SysRoleEntity role = findRole(user.getTenantId(), id);
        if (role == null) {
            return ApiResponse.fail("角色不存在或不属于当前企业。");
        }
        List<Long> menuIds = request.menuIds() == null ? List.of() : request.menuIds();
        if (!menuIds.isEmpty() && menuMapper.selectCount(new LambdaQueryWrapper<SysMenuEntity>()
                .in(SysMenuEntity::getId, menuIds).eq(SysMenuEntity::getStatus, BusinessStatus.ACTIVE)) != menuIds.size()) {
            return ApiResponse.fail("存在无效或已停用的菜单权限。");
        }
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenuEntity>().eq(SysRoleMenuEntity::getRoleId, id));
        for (Long menuId : menuIds) {
            SysRoleMenuEntity relation = new SysRoleMenuEntity();
            relation.setRoleId(id);
            relation.setMenuId(menuId);
            roleMenuMapper.insert(relation);
        }
        return ApiResponse.ok("角色菜单权限已保存。", null);
    }

    /**
     * 给单个用户分配新的 IAM 角色绑定。
     *
     * @param user 当前登录用户
     * @param userId 目标用户 ID
     * @param request 包含角色 ID 与主角色标志的请求
     * @return 分配成功响应
     */
    @Operation(summary = "给用户分配单个角色", description = "为特定用户赋予新的 IAM 角色绑定。")
    @PutMapping("/users/{userId}/role")
    public ApiResponse<?> assignUserRole(@AuthenticationPrincipal SecurityUser user, @PathVariable Long userId,
                                           @RequestBody AssignRoleRequest request) {
        if (!canManage(user)) {
            return ApiResponse.fail("无权限分配企业角色。");
        }
        SysUserEntity target = userMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getId, userId).eq(SysUserEntity::getTenantId, user.getTenantId()));
        SysRoleEntity role = request.roleId() == null ? null : findRole(user.getTenantId(), request.roleId());
        if (target == null) {
            return ApiResponse.fail("用户不存在或不属于当前企业。");
        }
        if (role == null) {
            return ApiResponse.fail("角色不存在或不属于当前企业。");
        }
        Long count = roleBindingMapper.selectCount(new LambdaQueryWrapper<IamUserRoleBindingEntity>()
                .eq(IamUserRoleBindingEntity::getTenantId, user.getTenantId())
                .eq(IamUserRoleBindingEntity::getUserId, userId)
                .eq(IamUserRoleBindingEntity::getRoleId, role.getId()));
        if (count != null && count > 0) {
            return ApiResponse.ok("用户已拥有该角色。", null);
        }
        saveBinding(user, userId, request);
        return ApiResponse.ok("用户角色已分配。", null);
    }

    /**
     * 重置并重新为指定用户批量绑定一组 IAM 角色。
     *
     * @param user 当前登录用户
     * @param userId 目标用户 ID
     * @param request 包含多角色分配数组的请求
     * @return 批量分配成功响应
     */
    @Operation(summary = "给用户批量分配角色", description = "重置并重新为用户绑定一组 IAM 角色。")
    @PutMapping("/users/{userId}/roles")
    public ApiResponse<?> assignUserRoles(@AuthenticationPrincipal SecurityUser user, @PathVariable Long userId,
                                            @RequestBody AssignRolesRequest request) {
        if (!canManage(user) || userMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getId, userId).eq(SysUserEntity::getTenantId, user.getTenantId())) == null) {
            return ApiResponse.fail("用户不存在或无权限。");
        }
        roleBindingMapper.delete(new LambdaQueryWrapper<IamUserRoleBindingEntity>()
                .eq(IamUserRoleBindingEntity::getTenantId, user.getTenantId()).eq(IamUserRoleBindingEntity::getUserId, userId));
        for (AssignRoleRequest item : request == null || request.roles() == null ? List.<AssignRoleRequest>of() : request.roles()) {
            saveBinding(user, userId, item);
        }
        return ApiResponse.ok("用户角色已批量分配。", null);
    }

    /**
     * 快速克隆源角色的全部菜单与功能权限建立新角色。
     *
     * @param user 当前登录用户
     * @param id 源角色 ID
     * @param request 包含新角色编码与名称的请求
     * @return 复制新建的角色 ID
     */
    @Operation(summary = "复制角色与权限", description = "快速克隆源角色的全部菜单与功能权限建立新角色。")
    @PostMapping("/roles/{id}/copy")
    public ApiResponse<?> copyRole(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id,
                                     @RequestBody CopyRoleRequest request) {
        if (!canManage(user)) {
            return ApiResponse.fail("无权限维护企业角色。");
        }
        SysRoleEntity source = findRole(user.getTenantId(), id);
        if (source == null || request == null || request.roleCode() == null || request.roleName() == null) {
            return ApiResponse.fail("源角色或复制参数无效。");
        }
        if (roleMapper.selectCount(new LambdaQueryWrapper<SysRoleEntity>()
                .eq(SysRoleEntity::getTenantId, user.getTenantId())
                .eq(SysRoleEntity::getRoleCode, request.roleCode().trim())) > 0) {
            return ApiResponse.fail("角色编码已存在。");
        }
        SysRoleEntity copy = new SysRoleEntity();
        copy.setTenantId(user.getTenantId());
        copy.setRoleCode(request.roleCode().trim());
        copy.setRoleName(request.roleName().trim());
        copy.setStatus(BusinessStatus.ACTIVE);
        copy.setCreatedAt(LocalDateTime.now());
        copy.setUpdatedAt(LocalDateTime.now());
        roleMapper.insert(copy);

        roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenuEntity>().eq(SysRoleMenuEntity::getRoleId, source.getId())).forEach(item -> {
            SysRoleMenuEntity relation = new SysRoleMenuEntity();
            relation.setRoleId(copy.getId());
            relation.setMenuId(item.getMenuId());
            roleMenuMapper.insert(relation);
        });
        return ApiResponse.ok("角色复制成功。", copy.getId());
    }

    /**
     * 辅助方法：保存具体的 IAM 用户角色绑定实体记录。
     */
    private void saveBinding(SecurityUser actor, Long userId, AssignRoleRequest request) {
        if (request == null || request.roleId() == null || findRole(actor.getTenantId(), request.roleId()) == null) {
            throw new IllegalArgumentException("角色不存在或不属于当前租户。");
        }
        if (Boolean.TRUE.equals(request.primary())) {
            roleBindingMapper.update(null, Wrappers.<IamUserRoleBindingEntity>lambdaUpdate()
                    .eq(IamUserRoleBindingEntity::getTenantId, actor.getTenantId())
                    .eq(IamUserRoleBindingEntity::getUserId, userId)
                    .set(IamUserRoleBindingEntity::getIsPrimary, false));
        }
        IamUserRoleBindingEntity binding = new IamUserRoleBindingEntity();
        binding.setTenantId(actor.getTenantId());
        binding.setUserId(userId);
        binding.setRoleId(request.roleId());
        binding.setSourceCode(request.sourceCode() == null ? "MANUAL" : request.sourceCode());
        binding.setIsPrimary(Boolean.TRUE.equals(request.primary()));
        binding.setValidFrom(request.validFrom() == null ? LocalDateTime.now() : request.validFrom());
        binding.setValidUntil(request.validUntil());
        binding.setStatus(request.status() == null ? BusinessStatus.ACTIVE : request.status());
        binding.setCreatedBy(actor.getUserId());
        binding.setCreatedAt(LocalDateTime.now());
        binding.setUpdatedAt(LocalDateTime.now());
        roleBindingMapper.insert(binding);
    }

    /**
     * 辅助方法：根据 ID 与租户限制检索 SysRoleEntity 角色实体。
     */
    private SysRoleEntity findRole(Long tenantId, Long id) {
        return roleMapper.selectOne(new LambdaQueryWrapper<SysRoleEntity>()
                .eq(SysRoleEntity::getId, id)
                .eq(SysRoleEntity::getTenantId, tenantId));
    }

    /**
     * 辅助方法：统计拥有特定角色的成员总数。
     */
    private long countUsers(Long roleId, Long tenantId) {
        List<Long> userIds = roleBindingMapper.selectList(new LambdaQueryWrapper<IamUserRoleBindingEntity>()
                        .eq(IamUserRoleBindingEntity::getTenantId, tenantId)
                        .eq(IamUserRoleBindingEntity::getRoleId, roleId)
                        .eq(IamUserRoleBindingEntity::getStatus, BusinessStatus.ACTIVE))
                .stream().map(IamUserRoleBindingEntity::getUserId).toList();
        if (userIds.isEmpty()) {
            userIds = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRoleEntity>()
                            .eq(SysUserRoleEntity::getRoleId, roleId))
                    .stream().map(SysUserRoleEntity::getUserId).toList();
        }
        return userIds.isEmpty() ? 0 : userMapper.selectCount(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getTenantId, tenantId).in(SysUserEntity::getId, userIds));
    }

    /**
     * 辅助方法：校验操作人是否有权限管理管理员与权限控制规则。
     */
    private boolean canManage(SecurityUser user) {
        return user != null && (user.hasRole("SUPER_ADMIN") || user.hasRole("ADMIN"));
    }
}

