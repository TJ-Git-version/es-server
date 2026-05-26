package com.felix.esmysqlsync.model.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class IndexMappingVO {
    private String indexName;
    private List<FieldMapping> properties;

    @Data
    public static class FieldMapping {
        private String fieldName;
        private String type;
        private String analyzer;
        private String searchAnalyzer;
        private String format;
        private Map<String, Object> fields;
    }
}