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

### 前端应用
```dockerfile
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
```

## Docker Compose

```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=mysql
    depends_on:
      - mysql
  
  mysql:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=root123456
      - MYSQL_DATABASE=gamemaker
    volumes:
      - mysql-data:/var/lib/mysql
    ports:
      - "3306:3306"

volumes:
  mysql-data:
```

## 常用命令

```bash
# 构建镜像
docker build -t myapp:1.0 .

# 运行容器
docker run -d -p 8080:8080 --name myapp myapp:1.0

# 查看容器
docker ps
docker ps -a

# 查看日志
docker logs -f myapp

# 进入容器
docker exec -it myapp /bin/sh

# 停止/启动
docker stop myapp
docker start myapp

# 清理
docker system prune -a
```

## 最佳实践

### 镜像优化
- 使用多阶段构建
- 使用Alpine基础镜像
- 合并RUN指令
- 使用.dockerignore

### 安全实践
- 不使用root用户
- 扫描漏洞
- 固定版本号
- 最小化安装
