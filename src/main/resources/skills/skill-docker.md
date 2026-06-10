---
name: Docker容器化
description: Docker容器化部署和管理
trigger: Docker,容器化,容器部署,Dockerfile,镜像构建
examples: Docker部署|容器管理|镜像构建|Docker Compose
---

# Docker容器化技能

## Dockerfile编写

### Java应用
```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Node.js应用
```dockerfile
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
EXPOSE 3000
CMD ["node", "src/index.js"]
```

### 前端应用
```dockerfile
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

## Docker Compose

### 基础配置
```yaml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
    depends_on:
      - db
      - redis

  db:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=root
      - MYSQL_DATABASE=game
    volumes:
      - mysql_data:/var/lib/mysql

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

volumes:
  mysql_data:
```

### 游戏服务配置
```yaml
version: '3.8'

services:
  game-server:
    build:
      context: .
      dockerfile: Dockerfile.server
    ports:
      - "8080:8080"
      - "8081:8081"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=db
      - REDIS_HOST=redis
    depends_on:
      - db
      - redis
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G

  game-frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    ports:
      - "80:80"
    depends_on:
      - game-server

  db:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=${DB_PASSWORD}
      - MYSQL_DATABASE=game
    volumes:
      - mysql_data:/var/lib/mysql
      - ./sql/init.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "3306:3306"

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

volumes:
  mysql_data:
  redis_data:
```

## 最佳实践

### 1. 镜像优化
```dockerfile
# 多阶段构建
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
# 减少层数
RUN apk add --no-cache curl
```

### 2. 安全配置
```dockerfile
# 使用非root用户
RUN addgroup -g 1001 -S appgroup
RUN adduser -u 1001 -S appuser -G appgroup
USER appuser

# 只读文件系统
VOLUME ["/tmp"]
```

### 3. 健康检查
```dockerfile
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1
```

## 部署流程

### 1. 构建镜像
```bash
# 构建镜像
docker build -t game-server:latest .

# 推送到仓库
docker tag game-server:latest registry.example.com/game-server:latest
docker push registry.example.com/game-server:latest
```

### 2. 部署服务
```bash
# 启动服务
docker-compose up -d

# 查看状态
docker-compose ps

# 查看日志
docker-compose logs -f app
```

### 3. 更新服务
```bash
# 拉取新镜像
docker-compose pull

# 更新服务
docker-compose up -d --no-deps app
```

## 常见错误

1. **镜像太大**：要使用多阶段构建
2. **安全问题**：要使用非root用户
3. **健康检查**：要配置健康检查
4. **日志管理**：要配置日志收集
5. **资源限制**：要设置资源限制
