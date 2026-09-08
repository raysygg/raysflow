package com.acme.agentstudio.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.acme.agentstudio.common.response.ApiResponse;
import com.acme.agentstudio.common.util.SystemIdentifierGenerator;
import com.acme.agentstudio.application.audit.AuditApplicationService;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.common.BusinessStatus;
import com.acme.agentstudio.infrastructure.persistence.entity.SysOrgUnitEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SysUserEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SysUserOrgEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysOrgUnitMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysUserMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysUserOrgMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 组织架构与部门人员关系 REST 控制器。
 * 负责提供部门树形结构单元 CRUD、成员所属部门分配与解绑、批量调整成员部门、直属主管设置与上下级循环防环校验接口。
 */
@Tag(name = "系统设置", description = "系统配置、模型接入与组织架构")
@RestController
@RequestMapping("/api/organization")
public class OrganizationController {

    /** 组织单元 Mapper */
    private final SysOrgUnitMapper orgUnitMapper;

    /** 成员组织关系 Mapper */
    private final SysUserOrgMapper userOrgMapper;

    /** 系统用户 Mapper */
    private final SysUserMapper userMapper;

    /** 审计日志应用服务 */
    private final AuditApplicationService auditApplicationService;

    /**
     * 构造函数注入组织架构所需的依赖。
     */
    public OrganizationController(SysOrgUnitMapper orgUnitMapper, SysUserOrgMapper userOrgMapper, SysUserMapper userMapper,
                                   AuditApplicationService auditApplicationService) {
        this.orgUnitMapper = orgUnitMapper;
        this.userOrgMapper = userOrgMapper;
        this.userMapper = userMapper;
        this.auditApplicationService = auditApplicationService;
    }

    /**
     * 获取租户下的全量组织单元（部门/团队）列表及成员数量统计。
     *
     * @param user 当前登录用户
     * @return 组织单元树结构列表
     */
    @Operation(summary = "获取组织单元列表", description = "按租户获取组织架构树形结构单元及各部门成员数量。")
    @GetMapping("/units")
    public ApiResponse<?> listUnits(@AuthenticationPrincipal SecurityUser user) {
        List<SysOrgUnitEntity> units = orgUnitMapper.selectList(
                new LambdaQueryWrapper<SysOrgUnitEntity>()
                        .eq(SysOrgUnitEntity::getTenantId, user.getTenantId())
                        .orderByAsc(SysOrgUnitEntity::getSortOrder)
                        .orderByAsc(SysOrgUnitEntity::getId)
        );
        List<SysUserEntity> users = userMapper.selectList(
                new LambdaQueryWrapper<SysUserEntity>().eq(SysUserEntity::getTenantId, user.getTenantId())
        );
        Map<Long, String> userNames = new HashMap<>();
        users.forEach(item -> userNames.put(item.getId(), item.getNickname() == null ? item.getUsername() : item.getNickname()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysOrgUnitEntity unit : units) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", unit.getId());
            item.put("parentId", unit.getParentId());
            item.put("orgCode", unit.getOrgCode());
            item.put("orgName", unit.getOrgName());
            item.put("orgType", unit.getOrgType());
            item.put("leaderUserId", unit.getLeaderUserId());
            item.put("leaderName", userNames.get(unit.getLeaderUserId()));
            item.put("status", unit.getStatus());
            item.put("sortOrder", unit.getSortOrder());
            item.put("memberCount", userOrgMapper.selectCount(new LambdaQueryWrapper<SysUserOrgEntity>()
                    .eq(SysUserOrgEntity::getTenantId, user.getTenantId())
                    .eq(SysUserOrgEntity::getOrgUnitId, unit.getId())));
            result.add(item);
        }
        return ApiResponse.ok(result);
    }

    /**
     * 获取当前租户下所有成员列表，及其主所属部门与直属主管名字。
     *
     * @param user 当前登录用户
     * @return 组织成员及其属性列表
     */
    @Operation(summary = "获取组织成员列表", description = "获取当前租户下所有成员及其主所属部门与直属主管。")
    @GetMapping("/users")
    public ApiResponse<?> listUsers(@AuthenticationPrincipal SecurityUser user) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<SysUserEntity> tenantUsers = userMapper.selectList(
                new LambdaQueryWrapper<SysUserEntity>().eq(SysUserEntity::getTenantId, user.getTenantId())
        );
        Map<Long, String> userNames = new HashMap<>();
        tenantUsers.forEach(item -> userNames.put(item.getId(), item.getNickname() == null ? item.getUsername() : item.getNickname()));
        List<SysOrgUnitEntity> tenantUnits = orgUnitMapper.selectList(
                new LambdaQueryWrapper<SysOrgUnitEntity>().eq(SysOrgUnitEntity::getTenantId, user.getTenantId())
        );
        Map<Long, String> unitNames = new HashMap<>();
        tenantUnits.forEach(item -> unitNames.put(item.getId(), item.getOrgName()));
        tenantUsers.forEach(item -> {
            Map<String, Object> safeUser = new LinkedHashMap<>();
            safeUser.put("id", item.getId());
            safeUser.put("username", item.getUsername());
            safeUser.put("nickname", item.getNickname());
            safeUser.put("managerUserId", item.getManagerUserId());
            safeUser.put("managerName", userNames.get(item.getManagerUserId()));
            SysUserOrgEntity primaryOrg = userOrgMapper.selectOne(new LambdaQueryWrapper<SysUserOrgEntity>()
                    .eq(SysUserOrgEntity::getTenantId, user.getTenantId())
                    .eq(SysUserOrgEntity::getUserId, item.getId())
                    .eq(SysUserOrgEntity::getIsPrimary, 1));
            safeUser.put("orgUnitId", primaryOrg == null ? null : primaryOrg.getOrgUnitId());
            safeUser.put("orgUnitName", primaryOrg == null ? null : unitNames.get(primaryOrg.getOrgUnitId()));
            safeUser.put("status", item.getStatus());
            result.add(safeUser);
        });
        return ApiResponse.ok(result);
    }

    /**
     * 按主管用户 ID 查询其直属下属成员列表。
     *
     * @param user 当前登录用户
     * @param id 主管用户 ID
     * @return 直属下属成员列表
     */
    @Operation(summary = "获取直属下属列表", description = "按指定主管的用户 ID 查询其直接下属成员列表。")
    @GetMapping("/users/{id}/subordinates")
    public ApiResponse<?> listSubordinates(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id) {
        Long ownerCount = userMapper.selectCount(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getId, id).eq(SysUserEntity::getTenantId, user.getTenantId()));
        if (ownerCount == 0) {
            return ApiResponse.fail("直属上级不属于当前租户。");
        }
        return ApiResponse.ok(userMapper.selectList(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getTenantId, user.getTenantId()).eq(SysUserEntity::getManagerUserId, id)
                .orderByAsc(SysUserEntity::getId)));
    }

    /** 保存组织单元请求载荷 */
    public record SaveOrgUnitRequest(Long parentId, String orgCode, String orgName, String orgType, Long leaderUserId) {
        public SaveOrgUnitRequest {
            if (orgCode == null || orgCode.isBlank()) {
                orgCode = SystemIdentifierGenerator.fromName(orgName, "department");
            } else {
                orgCode = orgCode.trim();
            }
        }
    }

    /**
     * 在当前租户下创建新的部门或团队组织单元。
     *
     * @param user 当前登录用户
     * @param request 组织单元参数
     * @return 新建的组织单元对象
     */
    @Operation(summary = "创建组织单元", description = "在当前租户下创建新的部门或团队组织单元。")
    @PostMapping("/units")
    public ApiResponse<?> createUnit(@AuthenticationPrincipal SecurityUser user, @RequestBody SaveOrgUnitRequest request) {
        if (!canManage(user)) {
            return ApiResponse.fail("无权限维护企业组织架构。");
        }
        if (request.orgCode() == null || request.orgCode().isBlank() || request.orgName() == null || request.orgName().isBlank()) {
            return ApiResponse.fail("组织编码和组织名称不能为空。");
        }
        if (!isValidParent(user.getTenantId(), request.parentId(), null)
                || !isValidLeader(user.getTenantId(), request.leaderUserId())) {
            return ApiResponse.fail("上级组织或负责人不属于当前租户。");
        }
        Long count = orgUnitMapper.selectCount(new LambdaQueryWrapper<SysOrgUnitEntity>()
                .eq(SysOrgUnitEntity::getTenantId, user.getTenantId())
                .eq(SysOrgUnitEntity::getOrgCode, request.orgCode().trim()));
        if (count > 0) {
            return ApiResponse.fail("当前租户已存在相同组织编码。");
        }
        SysOrgUnitEntity unit = new SysOrgUnitEntity();
        unit.setTenantId(user.getTenantId());
        unit.setParentId(request.parentId() == null ? 0L : request.parentId());
        unit.setOrgCode(request.orgCode().trim());
        unit.setOrgName(request.orgName().trim());
        unit.setOrgType(request.orgType() == null || request.orgType().isBlank() ? "DEPARTMENT" : request.orgType());
        unit.setLeaderUserId(request.leaderUserId());
        unit.setStatus(BusinessStatus.ACTIVE);
        unit.setSortOrder(0);
        unit.setCreatedAt(LocalDateTime.now());
        unit.setUpdatedAt(LocalDateTime.now());
        orgUnitMapper.insert(unit);
        return ApiResponse.ok("组织单元创建成功。", unit);
    }

    /**
     * 修改指定组织单元的名称、类型、父层级或负责人。
     *
     * @param user 当前登录用户
     * @param id 组织单元 ID
     * @param request 包含修改项的请求
     * @return 修改后的组织单元对象
     */
    @Operation(summary = "更新组织单元", description = "修改指定组织单元的名称、上级归属或负责人。")
    @PutMapping("/units/{id}")
    public ApiResponse<?> updateUnit(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id, @RequestBody SaveOrgUnitRequest request) {
        if (!canManage(user)) {
            return ApiResponse.fail("无权限维护企业组织架构。");
        }
        SysOrgUnitEntity unit = orgUnitMapper.selectOne(new LambdaQueryWrapper<SysOrgUnitEntity>()
                .eq(SysOrgUnitEntity::getId, id).eq(SysOrgUnitEntity::getTenantId, user.getTenantId()));
        if (unit == null) {
            return ApiResponse.fail("组织单元不存在。");
        }
        if (request.orgName() != null && request.orgName().isBlank()) {
            return ApiResponse.fail("组织名称不能为空。");
        }
        if (!isValidParent(user.getTenantId(), request.parentId(), id)
                || !isValidLeader(user.getTenantId(), request.leaderUserId())) {
            return ApiResponse.fail("上级组织、负责人或组织层级不合法。");
        }
        unit.setParentId(request.parentId() == null ? unit.getParentId() : request.parentId());
        unit.setOrgName(request.orgName() == null ? unit.getOrgName() : request.orgName().trim());
        unit.setOrgType(request.orgType() == null ? unit.getOrgType() : request.orgType());
        unit.setLeaderUserId(request.leaderUserId());
        unit.setUpdatedAt(LocalDateTime.now());
        orgUnitMapper.updateById(unit);
        return ApiResponse.ok("组织单元更新成功。", unit);
    }

    /** 绑定成员与组织结构载荷 */
    public record AssignUserRequest(Long userId, Long orgUnitId, Boolean primary, Long managerUserId) {}

    /** 修改成员状态请求载荷 */
    public record UpdateUserStatusRequest(String status) {}

    /** 批量调整成员组织请求载荷 */
    public record BatchAssignRequest(List<Long> userIds, Long orgUnitId, Boolean primary, Long managerUserId) {}

    /**
     * 更新成员的启用/禁用/离职/冻结在职状态。
     *
     * @param user 当前登录用户
     * @param id 目标成员用户 ID
     * @param request 包含新状态的请求
     * @return 状态变更响应
     */
    @PostMapping("/users/{id}/status")
    public ApiResponse<?> updateUserStatus(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id,
                                           @RequestBody UpdateUserStatusRequest request) {
        if (!canManage(user)) {
            return ApiResponse.fail("无权维护成员状态");
        }
        if (request == null || !Set.of(BusinessStatus.ACTIVE, "DISABLED", "LEAVE", "FROZEN").contains(request.status())) {
            return ApiResponse.fail("成员状态不合法");
        }
        SysUserEntity target = userMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getTenantId, user.getTenantId()).eq(SysUserEntity::getId, id));
        if (target == null) {
            return ApiResponse.fail("成员不存在");
        }
        String previous = target.getStatus();
        target.setStatus(request.status());
        target.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(target);
        auditApplicationService.recordWorkflowAction(user.getTenantId(), user.getUsername(), "MEMBER_STATUS_CHANGE", id,
                Map.of("from", String.valueOf(previous), "to", request.status()));
        return ApiResponse.ok("成员状态已更新", Map.of("id", id, "status", request.status()));
    }

    /**
     * 批量调整多个成员的所属部门及直属主管。
     *
     * @param user 当前登录用户
     * @param request 批量调整配置体
     * @return 批量更新成功响应
     */
    @Transactional
    @PostMapping("/users/batch-assignment")
    public ApiResponse<?> batchAssign(@AuthenticationPrincipal SecurityUser user, @RequestBody BatchAssignRequest request) {
        if (!canManage(user)) {
            return ApiResponse.fail("无权批量调整成员");
        }
        if (request == null || request.userIds() == null || request.userIds().isEmpty()) {
            return ApiResponse.fail("请选择成员");
        }
        List<Long> updated = new ArrayList<>();
        for (Long userId : request.userIds()) {
            ApiResponse<?> response = assignUser(user, new AssignUserRequest(userId, request.orgUnitId(), request.primary(), request.managerUserId()));
            if (!response.success()) {
                return response;
            }
            updated.add(userId);
        }
        auditApplicationService.recordWorkflowAction(user.getTenantId(), user.getUsername(), "MEMBER_BATCH_ASSIGNMENT", user.getUserId(),
                Map.of("userIds", updated, "orgUnitId", request.orgUnitId()));
        return ApiResponse.ok("成员批量调整已保存", updated);
    }

    /**
     * 设置或更新指定用户在组织中的归属部门、主从部门关系及直属主管。
     *
     * @param user 当前登录用户
     * @param request 成员关系分配载荷
     * @return 保存成功响应
     */
    @Operation(summary = "关联成员与组织单元", description = "设置用户在组织中的归属部门、主次关系及直属主管。")
    @PostMapping("/assignments")
    public ApiResponse<?> assignUser(@AuthenticationPrincipal SecurityUser user, @RequestBody AssignUserRequest request) {
        if (!canManage(user)) {
            return ApiResponse.fail("无权限维护组织成员关系。");
        }
        if (request.userId() == null || request.orgUnitId() == null) {
            return ApiResponse.fail("用户和组织不能为空。");
        }
        Long userCount = userMapper.selectCount(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getId, request.userId()).eq(SysUserEntity::getTenantId, user.getTenantId()));
        Long orgCount = orgUnitMapper.selectCount(new LambdaQueryWrapper<SysOrgUnitEntity>()
                .eq(SysOrgUnitEntity::getId, request.orgUnitId()).eq(SysOrgUnitEntity::getTenantId, user.getTenantId()));
        if (userCount == 0 || orgCount == 0) {
            return ApiResponse.fail("用户或组织不属于当前租户。");
        }
        if (!isValidManager(user.getTenantId(), request.userId(), request.managerUserId())) {
            return ApiResponse.fail("直属上级必须属于当前租户，且不能造成上下级循环。");
        }
        SysUserOrgEntity relation = userOrgMapper.selectOne(new LambdaQueryWrapper<SysUserOrgEntity>()
                .eq(SysUserOrgEntity::getTenantId, user.getTenantId())
                .eq(SysUserOrgEntity::getUserId, request.userId())
                .eq(SysUserOrgEntity::getOrgUnitId, request.orgUnitId()));
        if (Boolean.TRUE.equals(request.primary())) {
            userOrgMapper.update(null, Wrappers.<SysUserOrgEntity>lambdaUpdate()
                    .eq(SysUserOrgEntity::getTenantId, user.getTenantId())
                    .eq(SysUserOrgEntity::getUserId, request.userId())
                    .set(SysUserOrgEntity::getIsPrimary, 0));
        }
        if (relation == null) {
            relation = new SysUserOrgEntity();
            relation.setTenantId(user.getTenantId());
            relation.setUserId(request.userId());
            relation.setOrgUnitId(request.orgUnitId());
            relation.setCreatedAt(LocalDateTime.now());
        }
        relation.setIsPrimary(Boolean.TRUE.equals(request.primary()) ? 1 : 0);
        if (relation.getId() == null) {
            userOrgMapper.insert(relation);
        } else {
            userOrgMapper.updateById(relation);
        }
        SysUserEntity targetUser = userMapper.selectById(request.userId());
        targetUser.setManagerUserId(request.managerUserId());
        targetUser.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(targetUser);
        return ApiResponse.ok("组织成员关系已保存。", relation);
    }

    /**
     * 解绑成员与特定组织单元的部门归属关系。
     *
     * @param user 当前登录用户
     * @param userId 目标成员 ID
     * @param orgUnitId 部门 ID
     * @return 移除成功响应
     */
    @Operation(summary = "移除成员组织关系", description = "解绑指定成员与组织单元的关联绑定。")
    @DeleteMapping("/assignments/{userId}/{orgUnitId}")
    public ApiResponse<?> removeAssignment(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long userId,
            @PathVariable Long orgUnitId
    ) {
        if (!canManage(user)) {
            return ApiResponse.fail("无权限维护组织成员关系。");
        }
        SysUserOrgEntity relation = userOrgMapper.selectOne(new LambdaQueryWrapper<SysUserOrgEntity>()
                .eq(SysUserOrgEntity::getTenantId, user.getTenantId())
                .eq(SysUserOrgEntity::getUserId, userId)
                .eq(SysUserOrgEntity::getOrgUnitId, orgUnitId));
        if (relation == null) {
            return ApiResponse.fail("组织成员关系不存在。");
        }
        userOrgMapper.deleteById(relation.getId());
        return ApiResponse.ok("组织成员关系已移除。", null);
    }

    /**
     * 删除为空（无子部门且无关联成员）的组织单元。
     *
     * @param user 当前登录用户
     * @param id 组织单元 ID
     * @return 删除成功响应
     */
    @Operation(summary = "删除组织单元", description = "仅允许删除没有子组织和成员的组织单元，避免破坏组织层级关系。")
    @DeleteMapping("/units/{id}")
    public ApiResponse<?> deleteUnit(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id) {
        if (!canManage(user)) {
            return ApiResponse.fail("无权限维护企业组织架构。");
        }
        SysOrgUnitEntity unit = orgUnitMapper.selectOne(new LambdaQueryWrapper<SysOrgUnitEntity>()
                .eq(SysOrgUnitEntity::getId, id).eq(SysOrgUnitEntity::getTenantId, user.getTenantId()));
        if (unit == null) {
            return ApiResponse.fail("组织单元不存在。");
        }
        Long childCount = orgUnitMapper.selectCount(new LambdaQueryWrapper<SysOrgUnitEntity>()
                .eq(SysOrgUnitEntity::getTenantId, user.getTenantId()).eq(SysOrgUnitEntity::getParentId, id));
        Long memberCount = userOrgMapper.selectCount(new LambdaQueryWrapper<SysUserOrgEntity>()
                .eq(SysUserOrgEntity::getTenantId, user.getTenantId()).eq(SysUserOrgEntity::getOrgUnitId, id));
        if (childCount > 0 || memberCount > 0) {
            return ApiResponse.fail("请先移除子组织和成员后再删除。");
        }
        orgUnitMapper.deleteById(id);
        return ApiResponse.ok("组织单元已删除。", null);
    }

    /**
     * 辅助校验：验证主管 ID 的合法性，并进行链式追踪防止形成 A -> B -> A 循环主管树。
     */
    private boolean isValidManager(Long tenantId, Long userId, Long managerUserId) {
        if (managerUserId == null) {
            return true;
        }
        if (userId.equals(managerUserId)) {
            return false;
        }
        SysUserEntity manager = userMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getId, managerUserId).eq(SysUserEntity::getTenantId, tenantId));
        if (manager == null) {
            return false;
        }
        Long cursor = manager.getManagerUserId();
        int depth = 0;
        while (cursor != null && depth++ < 64) {
            if (userId.equals(cursor)) {
                return false;
            }
            SysUserEntity parent = userMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
                    .eq(SysUserEntity::getId, cursor).eq(SysUserEntity::getTenantId, tenantId));
            cursor = parent == null ? null : parent.getManagerUserId();
        }
        return true;
    }

    /**
     * 辅助校验：验证父组织单元 ID 的合法性，并向上溯源防止自循环父子节点结构。
     */
    private boolean isValidParent(Long tenantId, Long parentId, Long currentId) {
        if (parentId == null || parentId == 0L) {
            return true;
        }
        if (currentId != null && parentId.equals(currentId)) {
            return false;
        }
        SysOrgUnitEntity parent = orgUnitMapper.selectOne(new LambdaQueryWrapper<SysOrgUnitEntity>()
                .eq(SysOrgUnitEntity::getId, parentId).eq(SysOrgUnitEntity::getTenantId, tenantId));
        if (parent == null) {
            return false;
        }
        Long cursor = parent.getParentId();
        int depth = 0;
        while (currentId != null && cursor != null && cursor != 0L && depth++ < 32) {
            if (currentId.equals(cursor)) {
                return false;
            }
            SysOrgUnitEntity ancestor = orgUnitMapper.selectOne(new LambdaQueryWrapper<SysOrgUnitEntity>()
                    .eq(SysOrgUnitEntity::getId, cursor).eq(SysOrgUnitEntity::getTenantId, tenantId));
            cursor = ancestor == null ? 0L : ancestor.getParentId();
        }
        return true;
    }

    /**
     * 辅助校验：验证部门负责人用户 ID 是否属于当前租户。
     */
    private boolean isValidLeader(Long tenantId, Long leaderUserId) {
        return leaderUserId == null || userMapper.selectCount(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getId, leaderUserId).eq(SysUserEntity::getTenantId, tenantId)) > 0;
    }

    /**
     * 辅助校验：判断操作人是否具备管理员相关角色权限。
     */
    private boolean canManage(SecurityUser user) {
        return user != null && (user.hasRole("SUPER_ADMIN") || user.hasRole("ADMIN"));
    }
}

