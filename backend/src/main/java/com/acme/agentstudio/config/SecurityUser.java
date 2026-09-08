package com.acme.agentstudio.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Spring Security 认证用户主体包装类。
 * 实现 UserDetails 接口，封装当前登录用户的 UserID、TenantID、Username、主角色以及完整角色列表，供鉴权与控制器切面注入使用。
 */
public class SecurityUser implements UserDetails {

    /** 用户 ID */
    private final Long userId;

    /** 归属租户 ID */
    private final Long tenantId;

    /** 用户账号名 */
    private final String username;

    /** 主角色标识（如 TENANT_ADMIN, SYSTEM_ADMIN, REGULAR_USER） */
    private final String role;

    /** 持有的全量角色集合 */
    private final Set<String> roles;

    /** 单角色构造函数 */
    public SecurityUser(Long userId, Long tenantId, String username, String role) {
        this(userId, tenantId, username, role, Set.of(role));
    }

    /** 多角色构造函数 */
    public SecurityUser(Long userId, Long tenantId, String username, String role, Set<String> roles) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.username = username;
        this.role = role;
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (roles != null) {
            roles.stream().filter(item -> item != null && !item.isBlank()).forEach(normalized::add);
        }
        if (role != null && !role.isBlank()) {
            normalized.add(role);
        }
        this.roles = Set.copyOf(normalized);
    }

    /** 获取用户唯一标识 ID */
    public Long getUserId() {
        return userId;
    }

    /** 获取用户归属租户 ID */
    public Long getTenantId() {
        return tenantId;
    }

    /** 获取主角色标识 */
    public String getRole() {
        return role;
    }

    /** 获取全量角色集合 */
    public Set<String> getRoles() {
        return roles;
    }

    /** 判断当前用户是否具备指定的角色 */
    public boolean hasRole(String roleCode) {
        return roles.contains(roleCode);
    }

    /** 将角色列表映射转换为 Spring Security 的 GrantedAuthority 集合 */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(item -> new SimpleGrantedAuthority("ROLE_" + item)).toList();
    }

    /** 获取密码（JWT 认证模式下返回空串） */
    @Override
    public String getPassword() {
        return "";
    }

    /** 获取登录用户名 */
    @Override
    public String getUsername() {
        return username;
    }

    /** 账户未过期标识 */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /** 账户未锁定标识 */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /** 凭证未过期标识 */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** 账号启用标识 */
    @Override
    public boolean isEnabled() {
        return true;
    }
}

