# supOS Backend

## 项目简介
supOS Backend 是一个基于 Spring Boot 3.1.0 的多模块 Java 项目，旨在提供对多个后端服务（如 Grafana、PostgreSQL、TDengine、Kong、Minio、Elasticsearch 等）的适配支持。项目采用 Java 17，集成 MyBatis-Plus、Sa-Token、FastJSON、Flyway、HikariCP 等技术栈。

## 技术栈
- **Spring Boot 3.1.0** - 核心框架
- **Java 17** - 运行环境
- **MyBatis-Plus** - ORM 框架
- **Elasticsearch** - 搜索与数据分析
- **FastJSON & FastJSON2** - JSON 解析
- **Sa-Token** - 认证与授权
- **Flyway** - 数据库版本管理
- **HikariCP** - 数据库连接池
- **Forest** - HTTP 客户端

## 项目模块
本项目采用多模块架构，包含以下子模块：

| 模块名称             | 说明 |
|----------------------|------|
| bootstrap           | 启动模块 |
| common              | 公共工具模块 |
| adpter-grafana      | Grafana 适配器 |
| adpter-postgresql   | PostgreSQL 适配器 |
| adpter-tdengine     | TDengine 适配器 |
| UnityNamespace      | 统一命名空间 |
| adapter-mqtt        | MQTT 适配器 |
| app-manager         | 应用管理模块 |
| adpter-kong         | Kong API 网关适配器 |
| adpter-nodered      | Node-RED 适配器 |
| gateway             | 网关服务 |
| adpter-elasticsearch| Elasticsearch 适配器 |
| adpter-minio        | Minio 存储适配器 |
| webhook            | Webhook 处理模块 |
| adpter-camunda      | Camunda 工作流适配器 |
| adpter-eventflow    | 事件流处理适配器 |

## 运行环境要求
- **JDK 17** 及以上
- **Maven 3.8+**
- **Docker（可选）** 用于部署相关依赖服务

## 本地运行
### 1. 克隆项目
```bash
git clone <your-repo-url>
cd base
```

### 2. 配置环境
确保你的 `application.yml` 配置了正确的数据库及服务地址。

### 3. 编译与运行
```bash
mvn clean install
mvn spring-boot:run
```

## 依赖管理
本项目使用 `dependencyManagement` 进行统一依赖管理，部分主要依赖如下：
- **Hutool** (`cn.hutool:hutool-all:5.8.32`)
- **Guava** (`com.google.guava:guava:32.1.3-jre`)
- **MyBatis-Plus** (`com.baomidou:mybatis-plus:3.5.10.1`)
- **FastJSON** (`com.alibaba:fastjson:2.0.53`)
- **Elasticsearch Client** (`org.elasticsearch.client:elasticsearch-rest-high-level-client:7.10.2`)
- **Sa-Token** (`cn.dev33:sa-token-spring-boot3-starter:1.34.0`)
- **Flyway** (`org.flywaydb:flyway-core:9.19.4`)
- **Forest HTTP Client** (`com.dtflys.forest:forest-spring:1.5.32`)
- **Lombok & MapStruct** 用于代码简化

## 贡献指南
1. Fork 代码库
2. 创建新的 feature 分支 (`git checkout -b feature-xxx`)
3. 提交代码 (`git commit -m '新增 XXX 功能'`)
4. 推送分支 (`git push origin feature-xxx`)
5. 创建 Pull Request

## 许可证
本项目采用 Apache License 2.0 许可证。

