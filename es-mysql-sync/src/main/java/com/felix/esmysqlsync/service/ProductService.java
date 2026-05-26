package com.felix.esmysqlsync.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.felix.esmysqlsync.model.dto.ProductQueryDTO;
import com.felix.esmysqlsync.model.entity.ProductEntity;
import com.felix.esmysqlsync.model.vo.IndexMappingVO;
import com.felix.esmysqlsync.model.vo.ProductListVO;

import java.util.Map;

public interface ProductService extends IService<ProductEntity> {
    boolean switchIndexAliasAuto(String oldIndex, String newIndex, String alias);

    void createIndex();

    void initIndexData();

    boolean updateProduct(Long id, ProductEntity product);

    boolean deleteProduct(Long id);

    boolean addProduct(ProductEntity product);

    ProductListVO listProductPage(ProductQueryDTO queryDTO);

    Map<String, Object> testSegment(String text);

    IndexMappingVO getMapping(String index);

}
