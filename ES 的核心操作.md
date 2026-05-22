# ES 核心操作
## 创建索引
- 索引类似表
- mapping，类似mysql的表结构
```json lines
put products
{
    "mappings": {
        "properties": {
            "title": {
                "type": "text"
            },
            "brand": {
                "type": "keyword"
            },
            "price": {
                "type": "double"
            },
            "stock": {
                "type": "integer"
            }
        }
    }
}
```
## 查看索引
```json lines
get products
```

## 插入文档
- 第一次插入
```json lines
post products/_doc/1
{
    "title": "华为 Mate60 Pro",
    "brand": "华为",
    "price": 6999,
    "stock": 100
}
```
- 再次插入
```json lines
post products/_doc/2
{
    "title": "小米14 Ultra",
    "brand": "小米",
    "price": 5999,
    "stock": 200
}
```
## 查询所有数据
```json lines
get products/_search
```

## match 查询
- title 分词：华为、Mate60、Pro
```json lines
get products/_search
{
    "query": {
    "match": {
        "title": "华为 Mate60 Pro"
        }
    }
}
```
## term 查询，不会分词，精确匹配
```json lines
get products/_search
{
    "query": {
    "term": {
        "brand": "华为"
        }
    }
}
```
## 范围查询
- price 范围查询：5000-6000
```json lines
get products/_search
{
    "query": {
        "range": {
            "price": {
                "gte": 5000,
                "lte": 6000
            }
        }
    }
}
```
## 组合查询
- 标题包含 Pro
- 并且价格 >= 6000
- must：相关性评分，搜索相关度，通常用在搜索词
- filter：过滤，不计算相关性评分，通常用在 分类/状态/价格 过滤条件
```json lines
get products/_search
{
    "query": {
        "bool": {
            "must": [
                {
                "title": "华为"
                }
            ],
            "filter": [
                {
                    "range": {
                    "price": {
                        "gte": 5000
                        }
                    }
                }
            ]
        }
    }
}
```
## 查看 ES 自动评分
```json lines
get products/_search
    {
    "query": {
    "match": {
        "title": "华为 Mate60 Pro"
        }
    }
}
```
- 会看到："_score"

## 删除索引
```json lines
delete products
```