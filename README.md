# supOS Backend

## Project Overview
supOS Backend is a multi-module Java project based on Spring Boot 3.1.0. It aims to provide adaptation support for multiple backend services such as Grafana, PostgreSQL, TDengine, Kong, Minio, Elasticsearch, etc. The project is built with Java 17 and integrates MyBatis-Plus, Sa-Token, FastJSON, Flyway, HikariCP, and other technologies.

## Technology Stack
- **Spring Boot 3.1.0** - Core framework
- **Java 17** - Runtime environment
- **MyBatis-Plus** - ORM framework
- **Elasticsearch** - Search and analytics
- **FastJSON & FastJSON2** - JSON parsing
- **Sa-Token** - Authentication and authorization
- **Flyway** - Database version control
- **HikariCP** - Database connection pool
- **Forest** - HTTP client

## Project Modules
This project follows a multi-module architecture, including the following submodules:

| Module Name            | Description |
|------------------------|-------------|
| bootstrap             | Bootstrap module |
| common                | Common utility module |
| adpter-grafana        | Grafana adapter |
| adpter-postgresql     | PostgreSQL adapter |
| adpter-tdengine       | TDengine adapter |
| UnityNamespace        | Unified namespace |
| adapter-mqtt          | MQTT adapter |
| app-manager           | Application management module |
| adpter-kong           | Kong API gateway adapter |
| adpter-nodered        | Node-RED adapter |
| gateway               | Gateway service |
| adpter-elasticsearch  | Elasticsearch adapter |
| adpter-minio          | Minio storage adapter |
| webhook               | Webhook processing module |
| adpter-camunda        | Camunda workflow adapter |
| adpter-eventflow      | Event flow processing adapter |

## Environment Requirements
- **JDK 17** or later
- **Maven 3.8+**
- **Docker (optional)** for deploying dependent services

## Running Locally
### 1. Clone the Project
```bash
git clone <your-repo-url>
cd base
```

### 2. Configure the Environment
Ensure your `application.yml` is correctly configured with database and service addresses.

### 3. Build and Run
```bash
mvn clean install
mvn spring-boot:run
```

## Dependency Management
This project uses `dependencyManagement` for centralized dependency control. Some key dependencies include:
- **Hutool** (`cn.hutool:hutool-all:5.8.32`)
- **Guava** (`com.google.guava:guava:32.1.3-jre`)
- **MyBatis-Plus** (`com.baomidou:mybatis-plus:3.5.10.1`)
- **FastJSON** (`com.alibaba:fastjson:2.0.53`)
- **Elasticsearch Client** (`org.elasticsearch.client:elasticsearch-rest-high-level-client:7.10.2`)
- **Sa-Token** (`cn.dev33:sa-token-spring-boot3-starter:1.34.0`)
- **Flyway** (`org.flywaydb:flyway-core:9.19.4`)
- **Forest HTTP Client** (`com.dtflys.forest:forest-spring:1.5.32`)
- **Lombok & MapStruct** for code simplification

## Contribution Guide
1. Fork the repository
2. Create a new feature branch (`git checkout -b feature-xxx`)
3. Commit your changes (`git commit -m 'Add XXX feature'`)
4. Push to the branch (`git push origin feature-xxx`)
5. Create a Pull Request

## License
This project is licensed under the Apache License 2.0