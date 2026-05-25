# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

Use Maven wrapper in each module directory:
```bash
cd es-mysql-sync && ./mvnw clean compile
cd es-springboot && ./mvnw clean compile
```

Run tests:
```bash
cd es-mysql-sync && ./mvnw test
```

Package:
```bash
cd es-mysql-sync && ./mvnw package -DskipTests
```

## Architecture

### Multi-Module Maven Project
Two independent Spring Boot modules under the root:
- `es-mysql-sync/` - MySQL to Elasticsearch sync service
- `es-springboot/` - Basic ES Spring Boot integration

### es-mysql-sync
- **Purpose**: Syncs MySQL product data to Elasticsearch via RabbitMQ messaging
- **ES endpoint**: `localhost:9200`
- **Key components**:
  - `ProductEntity` - MyBatis-Plus entity mapped to `product` table
  - `ProductService.createIndex()` / `initIndexData()` - Creates ES index and bulk imports from MySQL
  - `RabbitMQConfig` + `ProductQueueConfig` - Message queue setup for async sync
  - `ElasticsearchConfig` - ES client bean with IO-thread pool (`esThreadPoolTaskExecutor`)

### es-springboot
- **Purpose**: Basic ES CRUD operations with product search
- **ES endpoint**: `100.64.0.8:9200`
- **Key components**:
  - `Product` - ES document model
  - `ProductController.query()` - Bool query with keyword match, brand filter, price range filter, and highlight
  - `ProductController.batchAdd()` - Bulk insert 100 products

### Shared Patterns
- Elasticsearch Java Client 8.13.4 with `RestClientTransport`
- Jackson JSON mapper for serialization
- Spring Boot 3.2.0, Java 21

### ES Index Conventions (from `ES 的核心操作.md`)
- `text` type fields are tokenized (e.g., `title`)
- `keyword` type fields are exact-match (e.g., `brand`, `brand.keyword`)
- Use `bool` + `must` for search relevance scoring
- Use `bool` + `filter` for exact-match conditions without scoring