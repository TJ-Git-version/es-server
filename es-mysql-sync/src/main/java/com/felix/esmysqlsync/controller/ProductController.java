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

import java.util.Map;

@RestController
@RequestMapping("/product")
@AllArgsConstructor
@Tag(name = "商品管理", description = "商品增删改查接口")
public class ProductController {

    private final ProductService productService;

    @GetMapping("/test/segment")
    public Result<?> testSegment(@Parameter(description = "中文字符串", required = true, example = "中文字符串分词测试")
                                @RequestParam String text) {
        return Result.success(productService.testSegment(text));
    }

    @GetMapping("/es/mapping")
    public Result<?> getMapping(@Parameter(description = "索引名称", required = true, example = "product")
                                @RequestParam String index) {
        return Result.success(productService.getMapping(index));
    }

    @Operation(summary = "ES数据同步", description = "同步数据并返回结果")
    @GetMapping("/es/reindex")
    public Result<Map<String, Object>> reindexData(
            @Parameter(description = "源索引名称", required = true, example = "product_v1")
            @RequestParam String sourceIndex,
            @Parameter(description = "目标索引名称", required = true, example = "product_v2")
            @RequestParam String destIndex) {
        return Result.success(productService.reindexData(sourceIndex, destIndex));
    }

    @Operation(summary = "ES异步数据同步", description = "启动异步同步，返回任务ID")
    @GetMapping("/es/reindex/async")
    public Result<String> reindexDataAsync(
            @Parameter(description = "源索引名称", required = true, example = "product_v1")
            @RequestParam String sourceIndex,
            @Parameter(description = "目标索引名称", required = true, example = "product_v2")
            @RequestParam String destIndex) {
        return Result.success(productService.reindexDataAsync(sourceIndex, destIndex));
    }

    @Operation(summary = "查询同步进度", description = "通过任务ID查询Reindex进度")
    @GetMapping("/es/reindex/progress")
    public Result<Map<String, Object>> getReindexProgress(
            @Parameter(description = "任务ID", required = true, example = "xxxxxxx:12345")
            @RequestParam String taskId) {
        return Result.success(productService.getReindexProgress(taskId));
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

    @Operation(summary = "创建ES索引", description = "创建ES索引")
    @GetMapping("/es/create")
    public Result<?> createIndex() {
        productService.createIndex();
        return Result.success();
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
