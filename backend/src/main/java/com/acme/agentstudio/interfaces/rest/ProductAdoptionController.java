package com.acme.agentstudio.interfaces.rest;

import com.acme.agentstudio.application.runtime.ProductAdoptionCatalog;
import com.acme.agentstudio.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 产品功能采用度与黄金路径目录 REST 控制器。
 * 负责为应用创建向导、功能引导以及发布门禁评估提供 MVP 阶段划分、最佳实践黄金路径（Golden Paths）与能力验收指标目录。
 */
@Tag(name = "Runtime 产品目录", description = "MVP 分期、黄金路径和验收目标")
@RestController
@RequestMapping("/api/runtime/product")
public class ProductAdoptionController {

    /** 产品采用度元数据目录 */
    private final ProductAdoptionCatalog catalog;

    /**
     * 构造函数注入产品采用度目录。
     */
    public ProductAdoptionController(ProductAdoptionCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 获取 Runtime 产品 MVP 阶段拆解、功能落地路线图与黄金路径配置目录。
     *
     * @return 包含阶段定义与引导步步强的目录视图
     */
    @Operation(summary = "获取产品分期和黄金路径", description = "获取系统定义的 MVP 分期规范、推荐研发黄金路径与质量验收目标目录。")
    @GetMapping("/catalog")
    public ApiResponse<?> catalog() {
        return ApiResponse.ok(catalog.catalog());
    }
}

