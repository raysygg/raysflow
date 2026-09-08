package com.acme.agentstudio.interfaces.rest;

import com.acme.agentstudio.application.saas.AdoptionAnalyticsService;
import com.acme.agentstudio.common.response.ApiResponse;
import com.acme.agentstudio.common.exception.AuthorizationDeniedException;
import com.acme.agentstudio.config.SecurityUser;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Set;

/**
 * 产品采用指标接口，只返回去标识化指标和安全事件摘要。
 */
@RestController
@RequestMapping("/api/saas/adoption")
public class SaasAdoptionController {
    private static final Set<String> ADOPTION_VIEW_ROLES = Set.of("SUPER_ADMIN", "ADMIN", "OPERATOR");
    private final AdoptionAnalyticsService service;

    public SaasAdoptionController(AdoptionAnalyticsService service) {
        this.service = service;
    }

    @GetMapping("/definitions")
    public ApiResponse<?> definitions() {
        return ApiResponse.ok(service.definitions());
    }

    @GetMapping("/report")
    public ApiResponse<?> report(@AuthenticationPrincipal SecurityUser user,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        requireViewer(user);
        return ApiResponse.ok(service.report(user, from, to));
    }

    @GetMapping("/quality")
    public ApiResponse<?> quality(@AuthenticationPrincipal SecurityUser user,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        requireViewer(user);
        return ApiResponse.ok(service.dataQuality(user, from, to));
    }

    @GetMapping("/benchmark")
    public ApiResponse<?> benchmark(@AuthenticationPrincipal SecurityUser user,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(service.benchmark(user, from, to));
    }

    /** 校验产品采用分析查看角色 */
    private void requireViewer(SecurityUser user) {
        if (user == null || user.getRoles().stream().noneMatch(ADOPTION_VIEW_ROLES::contains)) {
            throw new AuthorizationDeniedException("当前账号没有查看产品采用分析的权限。");
        }
    }
}
