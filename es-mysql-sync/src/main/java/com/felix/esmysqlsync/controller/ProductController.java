package com.felix.esmysqlsync.controller;

import com.felix.esmysqlsync.model.domain.result.Result;
import com.felix.esmysqlsync.model.dto.ProductQueryDTO;
import com.felix.esmysqlsync.model.entity.ProductEntity;
import com.felix.esmysqlsync.model.vo.ProductListVO;
import com.felix.esmysqlsync.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product")
@AllArgsConstructor
@Tag(name = "商品管理", description = "商品增删改查接口")
public class ProductController {

    private final ProductService productService;

    /**
     * 中文字符串分词测试
     */
    @GetMapping("/test/segment")
    public Result<?> testSegment(@Parameter(description = "中文字符串", required = true, example = "中文字符串分词测试")
                                @RequestParam String text) {
        return Result.success(productService.testSegment(text));
    }

    /**
     * 获取索引mapping表结构
     */
    @GetMapping("/es/mapping")
    public Result<?> getMapping(@Parameter(description = "索引名称", required = true, example = "product")
                                @RequestParam String index) {
        return Result.success(productService.getMapping(index));
    }

    @Operation(summary = "切换ES索引别名", description = "切换ES索引别名")
    @GetMapping("/es/switchAlias")
    public Result<?> switchIndexAliasAuto(
            @Parameter(description = "旧索引名称", example = "product_v0")
            @RequestParam(required = false) String oldIndex,
            @Parameter(description = "新索引名称", required = true, example = "product_v1")
            @RequestParam() String newIndex,
            @Parameter(description = "索引别名", required = true, example = "product")
            @RequestParam() String alias) {
        return Result.success(productService.switchIndexAliasAuto(oldIndex, newIndex, alias));
    }

    @Operation(summary = "初始化ES索引", description = "创建ES索引并初始化MySQL数据")
    @GetMapping("/es/init")
    public Result<?> init() {
        productService.createIndex();
        productService.initIndexData();
        return Result.success();
    }

    @Operation(summary = "新增商品")
    @PostMapping
    public Result<?> add(@RequestBody ProductEntity product) {
        return Result.success(productService.addProduct(product));
    }

    @Operation(summary = "修改商品")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "修改成功"),
            @ApiResponse(responseCode = "404", description = "商品不存在")
    })
    @PutMapping("/{id}")
    public Result<?> update(
            @Parameter(description = "商品ID", required = true, example = "1")
            @PathVariable Long id,
            @RequestBody ProductEntity product) {
        return Result.success(productService.updateProduct(id, product));
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    public Result<?> delete(
            @Parameter(description = "商品ID", required = true, example = "1")
            @PathVariable Long id) {
        return Result.success(productService.deleteProduct(id));
    }

    @Operation(summary = "查询商品列表")
    @GetMapping
    public Result<ProductListVO> listProductPage(
            @Parameter(description = "查询条件")
            @ModelAttribute ProductQueryDTO queryDTO) {
        return Result.success(productService.listProductPage(queryDTO));
    }
}
