package com.acme.agentstudio.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.acme.agentstudio.common.response.ApiResponse;
import com.acme.agentstudio.config.JwtUtils;
import com.acme.agentstudio.config.LoginRateLimiter;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.application.security.UserSessionService;
import com.acme.agentstudio.infrastructure.persistence.entity.IamRoleMenuTemplateEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.*;
import com.acme.agentstudio.infrastructure.persistence.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 认证与会话管理 REST 控制器。
 * 负责处理用户账号登录认证、企业租户自主注册、JWT 访问令牌无感刷新、安全退出以及多设备活动会话管理等核心操作。
 */
@Tag(name = "认证与会话", description = "登录、租户注册、令牌和设备会话管理")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    /** 租户数据库 Mapper */
    private final TenantMapper tenantMapper;

    /** 系统用户数据库 Mapper */
    private final SysUserMapper sysUserMapper;

    /** 系统角色数据库 Mapper */
    private final SysRoleMapper sysRoleMapper;

    /** 用户与角色关联关系 Mapper */
    private final SysUserRoleMapper sysUserRoleMapper;

    /** IAM 高级角色绑定映射 Mapper */
    private final IamUserRoleBindingMapper roleBindingMapper;

    /** 系统菜单权限数据库 Mapper */
    private final SysMenuMapper sysMenuMapper;

    /** 角色与菜单绑定映射 Mapper */
    private final SysRoleMenuMapper sysRoleMenuMapper;

    /** 初始角色菜单权限模板 Mapper */
    private final IamRoleMenuTemplateMapper roleMenuTemplateMapper;

    /** 租户计费配额数据库 Mapper */
    private final TenantBillingQuotaMapper tenantBillingQuotaMapper;

    /** 密码加密工具（BCrypt 算法） */
    private final PasswordEncoder passwordEncoder;

    /** JWT 签名生成与解析校验工具 */
    private final JwtUtils jwtUtils;

    /** 登录防暴力破解限流服务 */
    private final LoginRateLimiter loginRateLimiter;

    /** 用户多设备活动会话追踪与下线服务 */
    private final UserSessionService userSessionService;

    /**
     * 构造函数注入身份认证相关的依赖组件。
     */
    public AuthController(
            TenantMapper tenantMapper,
            SysUserMapper sysUserMapper,
            SysRoleMapper sysRoleMapper,
            SysUserRoleMapper sysUserRoleMapper,
            IamUserRoleBindingMapper roleBindingMapper,
            SysMenuMapper sysMenuMapper,
            SysRoleMenuMapper sysRoleMenuMapper,
            IamRoleMenuTemplateMapper roleMenuTemplateMapper,
            TenantBillingQuotaMapper tenantBillingQuotaMapper,
            PasswordEncoder passwordEncoder,
            JwtUtils jwtUtils,
            LoginRateLimiter loginRateLimiter,
            UserSessionService userSessionService
    ) {
        this.tenantMapper = tenantMapper;
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.roleBindingMapper = roleBindingMapper;
        this.sysMenuMapper = sysMenuMapper;
        this.sysRoleMenuMapper = sysRoleMenuMapper;
        this.roleMenuTemplateMapper = roleMenuTemplateMapper;
        this.tenantBillingQuotaMapper = tenantBillingQuotaMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.loginRateLimiter = loginRateLimiter;
        this.userSessionService = userSessionService;
    }

    /**
     * 用户登录请求载荷数据结构。
     *
     * @param tenantCode 企业租户识别编码
     * @param username 用户登录账号名
     * @param password 登录原始密码
     */
    public record LoginRequest(String tenantCode, String username, String password) {}

    /**
     * 企业租户自助注册请求载荷数据结构。
     *
     * @param tenantCode 期望注册的租户唯一编码
     * @param tenantName 企业空间展示名称
     * @param adminUsername 管理员初始登录账号名
     * @param adminPassword 管理员初始登录密码
     * @param nickname 管理员显示昵称
     */
    public record RegisterTenantRequest(String tenantCode, String tenantName, String adminUsername, String adminPassword, String nickname) {}

    /**
     * 刷新 Access Token 访问令牌。
     * 读取存储在 HttpOnly Cookie 中的 Refresh Token 校验身份，轮转生成新的 Access Token 与 Refresh Token。
     *
     * @param refreshToken Cookie 中的 Refresh Token 字符串
     * @param response HTTP 响应对象
     * @return 包含新 Access Token 的响应对象
     */
    @Operation(summary = "刷新访问令牌", description = "使用 HttpOnly Cookie 中的 Refresh Token 换取新的 Access Token。")
    @PostMapping("/refresh")
    public ApiResponse<?> refresh(@CookieValue(value = "refresh_token", required = false) String refreshToken, HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ApiResponse.fail("缺少刷新令牌，请重新登录。");
        }
        Map<String, Object> claims = jwtUtils.validateAndParseToken(refreshToken);
        if (claims == null) {
            return ApiResponse.fail("刷新令牌无效或已过期，请重新登录。");
        }
        var session = userSessionService.requireActive(refreshToken);

        Long userId = ((Number) claims.get("userId")).longValue();
        Long tenantId = ((Number) claims.get("tenantId")).longValue();
        String username = String.valueOf(claims.get("username"));
        String role = String.valueOf(claims.get("role"));
        Collection<String> roles = readRoles(claims, role);

        // 生成新令牌并轮转会话
        String accessToken = jwtUtils.generateAccessToken(userId, username, tenantId, role, roles);
        String rotatedRefreshToken = jwtUtils.generateRefreshToken(userId, username, tenantId, role, roles);
        userSessionService.rotate(session, rotatedRefreshToken);

        // 写入 HttpOnly 安全 Cookie
        Cookie cookie = new Cookie("refresh_token", rotatedRefreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(cookie);

        return ApiResponse.ok("令牌刷新成功。", Map.of("token", accessToken));
    }

    /**
     * 用户注销退出登录。
     * 废弃当前设备对应的 Refresh Token，并清理浏览器 HttpOnly Cookie。
     *
     * @param refreshToken Refresh Token 凭证
     * @param response HTTP 响应对象
     * @return 退出成功响应
     */
    @Operation(summary = "用户退出登录", description = "注销设备会话并清理 Cookie 中的 Refresh Token。")
    @PostMapping("/logout")
    public ApiResponse<?> logout(@CookieValue(value = "refresh_token", required = false) String refreshToken, HttpServletResponse response) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            userSessionService.revoke(refreshToken);
        }
        Cookie cookie = new Cookie("refresh_token", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ApiResponse.ok("已退出登录。", null);
    }

    /**
     * 获取当前登录用户的所有在线设备会话列表。
     *
     * @param user 当前登录安全上下文用户
     * @return 设备登录会话数据列表
     */
    @Operation(summary = "获取当前用户活动设备会话", description = "查询当前用户在各个设备上的活动登录会话。")
    @GetMapping("/sessions")
    public ApiResponse<?> sessions(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(userSessionService.list(user.getTenantId(), user.getUserId()));
    }

    /**
     * 强制指定 ID 的设备会话下线。
     *
     * @param sessionId 目标下线的会话 ID
     * @param user 当前安全上下文用户
     * @return 操作成功结果
     */
    @Operation(summary = "强制下线指定设备会话", description = "下线并失效特定设备的 Refresh Token 会话。")
    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<?> revokeSession(@PathVariable Long sessionId, @AuthenticationPrincipal SecurityUser user) {
        userSessionService.revoke(user.getTenantId(), user.getUserId(), sessionId);
        return ApiResponse.ok("设备会话已下线。", null);
    }

    /**
     * 一键下线当前设备之外的所有其他在线会话。
     *
     * @param user 当前登录安全上下文用户
     * @return 被强制下线的会话条数
     */
    @Operation(summary = "下线其它所有设备会话", description = "保留当前活动设备，一键下线其它所有设备的登录会话。")
    @PostMapping("/sessions/revoke-all")
    public ApiResponse<?> revokeAllSessions(@AuthenticationPrincipal SecurityUser user) {
        int count = userSessionService.revokeAll(user.getTenantId(), user.getUserId());
        return ApiResponse.ok("已下线全部其他设备会话。", Map.of("revokedCount", count));
    }

    /**
     * 处理用户账号密码认证登录逻辑。
     * 依次校验租户状态、防暴破锁定、用户名密码以及账号禁用状态，成功后签发 JWT Token 并建立活动会话。
     *
     * @param request 包含租户编码、用户名与密码的请求体
     * @param response HTTP 响应体（写入 Cookie）
     * @param servletRequest HTTP 原生请求体（获取客户端 IP 与 User-Agent）
     * @return 登录结果与 Access Token
     */
    @Operation(summary = "用户登录认证", description = "验证租户编码、用户名与密码，颁发 JWT Token 并建立会话。")
    @PostMapping("/login")
    public ApiResponse<?> login(@RequestBody LoginRequest request, HttpServletResponse response, HttpServletRequest servletRequest) {
        String lockoutKey = request.username() + "@" + request.tenantCode();
        if (loginRateLimiter.isLocked(lockoutKey)) {
            return ApiResponse.fail("账户已被锁定，请 15 分钟后再试。");
        }

        // 1. 检索与校验租户空间
        TenantEntity tenant = tenantMapper.selectOne(
                new QueryWrapper<TenantEntity>().eq("tenant_code", request.tenantCode())
        );
        if (tenant == null) {
            return ApiResponse.fail("租户代码错误，该企业租户未注册。");
        }
        if (!"ACTIVE".equals(tenant.getStatus())) {
            return ApiResponse.fail("企业租户已被冻结，请联系平台管理员。");
        }

        // 2. 检索并校验用户凭证
        SysUserEntity user = sysUserMapper.selectOne(
                new QueryWrapper<SysUserEntity>()
                        .eq("tenant_id", tenant.getId())
                        .eq("username", request.username())
        );

        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            loginRateLimiter.loginFailed(lockoutKey);
            return ApiResponse.fail("用户名或密码错误。");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            return ApiResponse.fail("账号已被禁用。");
        }

        loginRateLimiter.loginSucceeded(lockoutKey);

        // 3. 加载角色与权限列表
        LinkedHashSet<String> roleCodes = loadActiveRoleCodes(tenant.getId(), user.getId());
        String roleCode = resolvePrimaryRole(roleCodes);
        roleCodes.add(roleCode);

        // 4. 签发 Access Token 与 Refresh Token
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername(), tenant.getId(), roleCode, roleCodes);
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername(), tenant.getId(), roleCode, roleCodes);
        userSessionService.create(tenant.getId(), user.getId(), refreshToken,
                servletRequest.getHeader("X-Device-Id"), servletRequest.getRemoteAddr(), servletRequest.getHeader("User-Agent"));

        // 5. 写入安全 HttpOnly Cookie
        Cookie cookie = new Cookie("refresh_token", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(cookie);

        Map<String, Object> data = new HashMap<>();
        data.put("token", accessToken);
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("role", roleCode);
        data.put("roles", roleCodes);
        data.put("tenantId", tenant.getId());
        data.put("tenantCode", tenant.getTenantCode());
        data.put("tenantName", tenant.getTenantName());

        return ApiResponse.ok("登录成功", data);
    }

    /**
     * 处理企业租户自主注册。
     * 初始化租户空间、管理员账号、内置预设角色、菜单权限以及默认资源计费配额。
     *
     * @param request 包含租户与管理员信息的注册参数
     * @return 注册处理结果
     */
    @Operation(summary = "企业租户注册", description = "注册新企业租户，初始化管理员账号、默认角色、菜单权限与计费配额。")
    @PostMapping("/register-tenant")
    public ApiResponse<?> registerTenant(@RequestBody RegisterTenantRequest request) {
        // 参数合法性校验
        if (request.tenantCode() == null || request.tenantCode().trim().isEmpty() ||
            request.tenantName() == null || request.tenantName().trim().isEmpty() ||
            request.adminUsername() == null || request.adminUsername().trim().isEmpty() ||
            request.adminPassword() == null || request.adminPassword().trim().isEmpty()) {
            return ApiResponse.fail("所有注册参数必填。");
        }

        if (request.adminPassword().length() < 8 ||
            !request.adminPassword().matches(".*[a-z].*") ||
            !request.adminPassword().matches(".*[A-Z].*") ||
            !request.adminPassword().matches(".*\\d.*") ||
            !request.adminPassword().matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\",./<>?].*")) {
            return ApiResponse.fail("密码强度太低：必须至少8位，且同时包含大小写字母、数字和特殊字符。");
        }

        // 校验租户编码全局唯一性
        Long count = tenantMapper.selectCount(
                new QueryWrapper<TenantEntity>().eq("tenant_code", request.tenantCode())
        );
        if (count > 0) {
            return ApiResponse.fail("该租户编码已被注册。");
        }

        // 1. 持久化新建租户实体
        TenantEntity tenant = new TenantEntity();
        tenant.setTenantCode(request.tenantCode());
        tenant.setTenantName(request.tenantName());
        tenant.setPlanCode("STARTER");
        tenant.setUserLimit(20);
        tenant.setStatus("ACTIVE");
        tenant.setCreatedAt(LocalDateTime.now());
        tenantMapper.insert(tenant);

        // 2. 为新租户创建系统内置角色
        SysRoleEntity adminRole = createRole(tenant.getId(), "ADMIN", "企业管理员");


        // 3. 从预设权限模板复制菜单路由分配
        bindRoleMenusFromTemplate(adminRole.getRoleCode(), adminRole.getId());


        // 4. 创建初始超级管理员账号
        SysUserEntity user = new SysUserEntity();
        user.setTenantId(tenant.getId());
        user.setUsername(request.adminUsername());
        user.setPassword(passwordEncoder.encode(request.adminPassword()));
        user.setNickname(request.nickname() == null || request.nickname().isBlank() ? "管理员" : request.nickname());
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.insert(user);

        // 5. 绑定管理员角色关系
        SysUserRoleEntity userRole = new SysUserRoleEntity();
        userRole.setUserId(user.getId());
        userRole.setRoleId(adminRole.getId());
        sysUserRoleMapper.insert(userRole);

        IamUserRoleBindingEntity binding = new IamUserRoleBindingEntity();
        binding.setTenantId(tenant.getId());
        binding.setUserId(user.getId());
        binding.setRoleId(adminRole.getId());
        binding.setSourceCode("SYSTEM");
        binding.setIsPrimary(true);
        binding.setValidFrom(LocalDateTime.now());
        binding.setStatus("ACTIVE");
        binding.setCreatedBy(user.getId());
        binding.setCreatedAt(LocalDateTime.now());
        binding.setUpdatedAt(LocalDateTime.now());
        roleBindingMapper.insert(binding);

        // 6. 初始化租户配额与用量计量表
        TenantBillingQuotaEntity quota = new TenantBillingQuotaEntity();
        quota.setTenantId(tenant.getId());
        quota.setMonthlyTokenLimit(1000000L);
        quota.setMonthlyTokenUsed(0L);
        quota.setMonthlyWorkflowLimit(1000);
        quota.setMonthlyWorkflowUsed(0);
        quota.setStorageLimitMb(100);
        quota.setUpdatedAt(LocalDateTime.now());
        tenantBillingQuotaMapper.insert(quota);

        return ApiResponse.ok("企业租户注册成功，管理员账号创建成功。");
    }

    /**
     * 获取当前上下文登录用户的完整扩展信息与权限菜单树。
     *
     * @return 包含用户属性、所属租户信息与动态菜单数据的响应
     */
    @Operation(summary = "获取当前登录用户信息", description = "获取当前上下文用户的基本信息、授权角色与菜单权限树。")
    @GetMapping("/user-info")
    public ApiResponse<?> userInfo() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof SecurityUser securityUser)) {
            return ApiResponse.fail("未登录。");
        }

        TenantEntity tenant = tenantMapper.selectById(securityUser.getTenantId());
        String tenantCode = tenant != null ? tenant.getTenantCode() : "unknown";
        String tenantName = tenant != null ? tenant.getTenantName() : "unknown";

        SysUserEntity user = sysUserMapper.selectById(securityUser.getUserId());
        String nickname = user != null ? user.getNickname() : securityUser.getUsername();

        List<SysMenuEntity> rawMenus = new ArrayList<>();
        List<String> perms = new ArrayList<>();
        LinkedHashSet<String> activeRoleCodes = loadActiveRoleCodes(securityUser.getTenantId(), securityUser.getUserId());
        List<Long> roleIds = sysUserRoleMapper.selectList(new QueryWrapper<SysUserRoleEntity>()
                        .eq("user_id", securityUser.getUserId()))
                .stream()
                .map(SysUserRoleEntity::getRoleId)
                .toList();

        if (!roleIds.isEmpty()) {
            List<Long> menuIds = sysRoleMenuMapper.selectList(new QueryWrapper<SysRoleMenuEntity>().in("role_id", roleIds))
                    .stream()
                    .map(SysRoleMenuEntity::getMenuId)
                    .distinct()
                    .toList();
            if (!menuIds.isEmpty()) {
                rawMenus = sysMenuMapper.selectList(
                        new QueryWrapper<SysMenuEntity>()
                                .in("id", menuIds)
                                .eq("status", "ACTIVE")
                                .orderByAsc("sort_order")
                );
                perms = rawMenus.stream()
                        .map(SysMenuEntity::getPerms)
                        .filter(Objects::nonNull)
                        .filter(p -> !p.isBlank())
                        .distinct()
                        .collect(Collectors.toList());
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", securityUser.getUserId());
        data.put("username", securityUser.getUsername());
        data.put("nickname", nickname);
        data.put("role", resolvePrimaryRole(activeRoleCodes));
        data.put("roles", activeRoleCodes);
        data.put("tenantId", securityUser.getTenantId());
        data.put("tenantCode", tenantCode);
        data.put("tenantName", tenantName);
        data.put("menus", rawMenus);
        data.put("permissions", perms);

        return ApiResponse.ok(data);
    }

    /** 辅助方法：快速创建具有特定编码的角色记录 */
    private SysRoleEntity createRole(Long tenantId, String roleCode, String roleName) {
        SysRoleEntity role = new SysRoleEntity();
        role.setTenantId(tenantId);
        role.setRoleCode(roleCode);
        role.setRoleName(roleName);
        role.setStatus("ACTIVE");
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        sysRoleMapper.insert(role);
        return role;
    }

    /** 辅助方法：批量绑定角色与菜单关系 */
    private void bindRoleMenus(Long roleId, List<Long> menuIds) {
        for (Long menuId : menuIds) {
            SysRoleMenuEntity roleMenu = new SysRoleMenuEntity();
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(menuId);
            sysRoleMenuMapper.insert(roleMenu);
        }
    }

    /** 辅助方法：从基线角色菜单模板复制代码路由关联 */
    private void bindRoleMenusFromTemplate(String roleCode, Long roleId) {
        List<Long> menuIds = roleMenuTemplateMapper.selectList(new QueryWrapper<IamRoleMenuTemplateEntity>()
                        .eq("role_code", roleCode)
                        .eq("status", "ACTIVE")
                        .orderByAsc("sort_order")
                        .orderByAsc("menu_id"))
                .stream()
                .map(IamRoleMenuTemplateEntity::getMenuId)
                .distinct()
                .toList();
        if (menuIds.isEmpty()) {
            throw new IllegalStateException("缺少角色菜单模板配置：" + roleCode);
        }
        bindRoleMenus(roleId, menuIds);
    }

    /** 辅助方法：查询用户在当前租户下激活生效的所有角色编码 */
    private LinkedHashSet<String> loadActiveRoleCodes(Long tenantId, Long userId) {
        LinkedHashSet<String> roleCodes = new LinkedHashSet<>();
        LocalDateTime now = LocalDateTime.now();
        roleBindingMapper.selectList(new QueryWrapper<IamUserRoleBindingEntity>()
                        .eq("tenant_id", tenantId).eq("user_id", userId).eq("status", "ACTIVE")
                        .le("valid_from", now).and(q -> q.isNull("valid_until").or().ge("valid_until", now)))
                .forEach(binding -> {
                    SysRoleEntity role = sysRoleMapper.selectOne(new QueryWrapper<SysRoleEntity>()
                            .eq("id", binding.getRoleId()).eq("tenant_id", tenantId).eq("status", "ACTIVE"));
                    if (role != null && role.getRoleCode() != null && !role.getRoleCode().isBlank()) {
                        roleCodes.add(role.getRoleCode());
                    }
                });
        if (!roleCodes.isEmpty()) {
            return roleCodes;
        }
        sysUserRoleMapper.selectList(new QueryWrapper<SysUserRoleEntity>().eq("user_id", userId))
                .forEach(userRole -> {
                    SysRoleEntity role = sysRoleMapper.selectOne(new QueryWrapper<SysRoleEntity>()
                            .eq("id", userRole.getRoleId())
                            .eq("tenant_id", tenantId)
                            .eq("status", "ACTIVE"));
                    if (role != null && role.getRoleCode() != null && !role.getRoleCode().isBlank()) {
                        roleCodes.add(role.getRoleCode());
                    }
                });
        return roleCodes;
    }

    /** 辅助方法：从多个角色中解析最高优先级的核心主角色 */
    private String resolvePrimaryRole(Collection<String> roleCodes) {
        if (roleCodes != null) {
            for (String candidate : List.of("SUPER_ADMIN", "ADMIN", "OPERATOR", "STAFF")) {
                if (roleCodes.contains(candidate)) {
                    return candidate;
                }
            }
            for (String roleCode : roleCodes) {
                if (roleCode != null && !roleCode.isBlank()) {
                    return roleCode;
                }
            }
        }
        return "STAFF";
    }

    /** 辅助方法：解析 JWT 签名声明中的角色集合，提供向前兼容性 */
    private Collection<String> readRoles(Map<String, Object> claims, String fallback) {
        LinkedHashSet<String> roles = new LinkedHashSet<>();
        Object value = claims.get("roles");
        if (value instanceof Iterable<?> items) {
            for (Object item : items) {
                if (item != null && !String.valueOf(item).isBlank()) {
                    roles.add(String.valueOf(item));
                }
            }
        }
        if (fallback != null && !fallback.isBlank()) {
            roles.add(fallback);
        }
        return roles;
    }
}

