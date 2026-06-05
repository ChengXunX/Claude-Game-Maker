# ============================================
# ChengXun Game Maker Dockerfile
# 多阶段构建：前端 + 后端
# ============================================

# 阶段1: 构建前端
FROM node:18-alpine AS frontend-builder
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci --legacy-peer-deps
COPY frontend/ ./
RUN npm run build

# 阶段2: 构建后端
FROM maven:3.9-eclipse-temurin-17 AS backend-builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src/ ./src/
RUN mvn clean package -DskipTests -B

# 阶段3: 运行时
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 安装必要工具
RUN apk add --no-cache curl tzdata

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 创建目录
RUN mkdir -p /app/data /app/logs /app/sql

# 复制后端 JAR
COPY --from=backend-builder /app/target/*.jar app.jar

# 复制前端
COPY --from=frontend-builder /app/frontend/dist/ /app/static/

# 复制 SQL 脚本
COPY src/main/resources/db/ /app/db/

# 暴露端口
EXPOSE 19922

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:19922/api/install/status || exit 1

# 启动
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar", "--spring.profiles.active=prod"]
