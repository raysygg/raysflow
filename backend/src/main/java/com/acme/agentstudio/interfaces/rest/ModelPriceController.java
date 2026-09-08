package com.acme.agentstudio.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.acme.agentstudio.common.response.ApiResponse;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.common.BusinessStatus;
import com.acme.agentstudio.infrastructure.persistence.entity.ModelPriceEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SysModelConfigEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.ModelPriceMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysModelConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 大模型计费价格规则 REST 控制器。
 * 负责提供模型输入/输出 Token 价格版本查询、新增、修改、删除与模型有效性校验接口（仅限 SUPER_ADMIN 操作）。
 */
@Tag(name = "模型与价格", description = "模型配置、价格版本和路由规则管理")
@RestController
@RequestMapping("/api/system/model-prices")
public class ModelPriceController {

    /** 模型价格规则 Mapper */
    private final ModelPriceMapper mapper;

    /** 模型连接配置 Mapper */
    private final SysModelConfigMapper modelConfigMapper;

    /**
     * 构造函数注入 Mapper 依赖。
     */
    public ModelPriceController(ModelPriceMapper mapper, SysModelConfigMapper modelConfigMapper) {
        this.mapper = mapper;
        this.modelConfigMapper = modelConfigMapper;
    }

    /** 模型价格变更或创建请求载荷 */
    public record PriceRequest(String provider, String modelKey, BigDecimal inputPricePer1k,
                               BigDecimal outputPricePer1k, LocalDateTime effectiveFrom, String status) {}

    /**
     * 获取全平台大模型价格版本规则列表（仅超级管理员）。
     *
     * @param user 当前登录用户
     * @return 价格规则列表
     */
    @Operation(summary = "获取模型价格规则列表", description = "系统超管获取全部 AI 大模型的输入输出 Token 计费价格版本。")
    @GetMapping
    public ApiResponse<?> list(@AuthenticationPrincipal SecurityUser user) {
        if (!isSuperAdmin(user)) {
            return ApiResponse.fail("只有系统超管可以管理模型价格规则。");
        }
        return ApiResponse.ok(mapper.selectList(new LambdaQueryWrapper<ModelPriceEntity>()
                .orderByDesc(ModelPriceEntity::getEffectiveFrom)
                .orderByAsc(ModelPriceEntity::getProvider)));
    }

    /**
     * 新建大模型计费单价规则。
     *
     * @param user 当前登录用户
     * @param request 价格参数体
     * @return 创建后的实体数据
     */
    @Operation(summary = "创建模型价格规则", description = "录入特定供应商大模型的新计费单价与生效时间。")
    @PostMapping
    public ApiResponse<?> create(@AuthenticationPrincipal SecurityUser user, @RequestBody PriceRequest request) {
        if (!isSuperAdmin(user)) {
            return ApiResponse.fail("只有系统超管可以管理模型价格规则。");
        }
        validate(request);
        ModelPriceEntity entity = new ModelPriceEntity();
        apply(entity, request);
        mapper.insert(entity);
        return ApiResponse.ok("模型价格规则已创建。", entity);
    }

    /**
     * 修改已有模型的计费单价或状态。
     *
     * @param user 当前登录用户
     * @param id 价格规则 ID
     * @param request 包含价格参数的请求体
     * @return 更新后的实体数据
     */
    @Operation(summary = "更新模型价格规则", description = "修改已有模型计费单价或状态。")
    @PutMapping("/{id}")
    public ApiResponse<?> update(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id, @RequestBody PriceRequest request) {
        if (!isSuperAdmin(user)) {
            return ApiResponse.fail("只有系统超管可以管理模型价格规则。");
        }
        validate(request);
        ModelPriceEntity entity = mapper.selectById(id);
        if (entity == null) {
            return ApiResponse.fail("模型价格规则不存在。");
        }
        apply(entity, request);
        mapper.updateById(entity);
        return ApiResponse.ok("模型价格规则已更新。", entity);
    }

    /**
     * 删除特定的模型计费价格规则。
     *
     * @param user 当前登录用户
     * @param id 价格规则 ID
     * @return 删除成功响应
     */
    @Operation(summary = "删除模型价格规则", description = "删除指定的模型计费价格规则。")
    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id) {
        if (!isSuperAdmin(user)) {
            return ApiResponse.fail("只有系统超管可以管理模型价格规则。");
        }
        mapper.deleteById(id);
        return ApiResponse.ok("模型价格规则已删除。", null);
    }

    /**
     * 私有校验方法：验证模型价格规则参数有效性。
     */
    private void validate(PriceRequest request) {
        if (request == null || request.provider() == null || request.provider().isBlank() || request.modelKey() == null || request.modelKey().isBlank()) {
            throw new IllegalArgumentException("供应商和模型名称不能为空。");
        }
        if (request.inputPricePer1k() == null || request.inputPricePer1k().signum() < 0 || request.outputPricePer1k() == null || request.outputPricePer1k().signum() < 0) {
            throw new IllegalArgumentException("输入和输出 Token 价格不能为负数。");
        }
        if (modelConfigMapper.selectCount(new LambdaQueryWrapper<SysModelConfigEntity>()
                .eq(SysModelConfigEntity::getProvider, request.provider().trim())
                .eq(SysModelConfigEntity::getModelKey, request.modelKey().trim())
                .eq(SysModelConfigEntity::getStatus, BusinessStatus.ACTIVE)) == 0) {
            throw new IllegalArgumentException("价格规则必须关联模型中心中已启用的模型连接。");
        }
    }

    /**
     * 私有赋值方法：将请求体属性应用至模型价格实体。
     */
    private void apply(ModelPriceEntity entity, PriceRequest request) {
        entity.setProvider(request.provider().trim());
        entity.setModelKey(request.modelKey().trim());
        entity.setInputPricePer1k(request.inputPricePer1k());
        entity.setOutputPricePer1k(request.outputPricePer1k());
        entity.setEffectiveFrom(request.effectiveFrom() == null ? LocalDateTime.now() : request.effectiveFrom());
        entity.setStatus(request.status() == null || request.status().isBlank() ? BusinessStatus.ACTIVE : request.status());
    }

    /**
     * 私有校验方法：判断操作人是否为超级管理员。
     */
    private boolean isSuperAdmin(SecurityUser user) {
        return user != null && user.hasRole("SUPER_ADMIN");
    }
}

