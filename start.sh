#!/bin/bash
# ChengXun Game Maker 启动脚本

echo "=========================================="
echo "  ChengXun Game Maker 启动"
echo "=========================================="

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 检查依赖
echo ""
echo "检查依赖..."
bash check-dependencies.sh
CHECK_RESULT=$?

if [ $CHECK_RESULT -ne 0 ]; then
    echo -e "${RED}依赖检查失败，请先解决上述问题${NC}"
    exit 1
fi

# 编译项目
echo ""
echo "编译项目..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo -e "${RED}编译失败${NC}"
    exit 1
fi

echo -e "${GREEN}编译成功${NC}"

# 启动应用
echo ""
echo "启动应用..."
echo "访问地址: http://localhost:9922"
echo "默认账号: admin / admin123"
echo ""

# 设置JVM参数
JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# 启动
java $JAVA_OPTS -jar target/game-maker-1.0-SNAPSHOT.jar
