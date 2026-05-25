package com.felix.esmysqlsync.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.felix.esmysqlsync.model.entity.ProductEntity;

public interface ProductService extends IService<ProductEntity> {
    void createIndex();

    void initIndexData();

    boolean updateProduct(Long id, ProductEntity product);

    boolean deleteProduct(Long id);

    boolean addProduct(ProductEntity product);
}
