package com.felix.esmysqlsync.controller;

import com.felix.esmysqlsync.model.domain.result.Result;
import com.felix.esmysqlsync.model.entity.ProductEntity;
import com.felix.esmysqlsync.service.ProductService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product")
@AllArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 初始化es索引，并初始化数据
     */
    @GetMapping("/es/init")
    public Result<?> init() {
        // 创建索引
        productService.createIndex();
        // 初始化数据
        productService.initIndexData();
        return Result.success();
    }

    /**
     * 新增商品
     */
    @PostMapping
    public Result<?> add(@RequestBody ProductEntity product) {
        return Result.success(productService.addProduct(product));
    }

    /**
     * 修改商品
     */
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody ProductEntity product) {
        return Result.success(productService.updateProduct(id, product));
    }

    /**
     * 删除商品
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        return Result.success(productService.deleteProduct(id));
    }
}
