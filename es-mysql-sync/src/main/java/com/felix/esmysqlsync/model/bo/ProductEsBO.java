package com.felix.esmysqlsync.model.bo;

import com.felix.esmysqlsync.model.enums.OperationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductEsBO {

    /**
     * 操作类型
     */
    private OperationType operationType;

    /**
     * 商品主键ID
     */
    private Long id;

    public void setInsert() {
        this.operationType = OperationType.INSERT;
    }

    public void setUpdate() {
        this.operationType = OperationType.UPDATE;
    }

    public void setDelete() {
        this.operationType = OperationType.DELETE;
    }
}
