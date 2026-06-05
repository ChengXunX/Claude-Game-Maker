#!/bin/bash
# 依赖检查脚本
# 检查项目运行所需的外部依赖

echo "=========================================="
echo "  ChengXun Game Maker 依赖检查"
echo "=========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查结果
PASSED=0
FAILED=0
WARNINGS=0

# 检查函数
check_pass() {
    echo -e "${GREEN}✅ $1${NC}"
    ((PASSED++))
}

check_fail() {
    echo -e "${RED}❌ $1${NC}"
    ((FAILED++))
}

check_warn() {
    echo -e "${YELLOW}⚠️  $1${NC}"
    ((WARNINGS++))
}

# 1. 检查Java
echo "1. 检查Java环境..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    check_pass "Java已安装: $JAVA_VERSION"
else
    check_fail "Java未安装"
fi

# 2. 检查Maven
echo ""
echo "2. 检查Maven..."
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version 2>&1 | head -n 1)
    check_pass "Maven已安装: $MVN_VERSION"
else
    check_fail "Maven未安装"
fi

# 3. 检查MySQL
echo ""
echo "3. 检查MySQL..."
if command -v mysql &> /dev/null; then
    check_pass "MySQL客户端已安装"
else
    check_warn "MySQL客户端未安装（可选）"
fi

# 检查MySQL连接
if [ -n "$MYSQL_HOST" ]; then
    MYSQL_HOST_VAL=$MYSQL_HOST
else
    MYSQL_HOST_VAL="localhost"
fi

if [ -n "$MYSQL_PORT" ]; then
    MYSQL_PORT_VAL=$MYSQL_PORT
else
    MYSQL_PORT_VAL="3306"
fi

if command -v nc &> /dev/null; then
    if nc -z -w5 $MYSQL_HOST_VAL $MYSQL_PORT_VAL 2>/dev/null; then
        check_pass "MySQL服务可连接: $MYSQL_HOST_VAL:$MYSQL_PORT_VAL"
    else
        check_fail "MySQL服务不可连接: $MYSQL_HOST_VAL:$MYSQL_PORT_VAL"
    fi
else
    check_warn "nc命令不可用，跳过MySQL连接检查"
fi

# 4. 检查Redis
echo ""
echo "4. 检查Redis..."
if command -v redis-cli &> /dev/null; then
    check_pass "Redis客户端已安装"
else
    check_warn "Redis客户端未安装（可选）"
fi

if [ -n "$REDIS_HOST" ]; then
    REDIS_HOST_VAL=$REDIS_HOST
else
    REDIS_HOST_VAL="localhost"
fi

if [ -n "$REDIS_PORT" ]; then
    REDIS_PORT_VAL=$REDIS_PORT
else
    REDIS_PORT_VAL="6379"
fi

if command -v nc &> /dev/null; then
    if nc -z -w5 $REDIS_HOST_VAL $REDIS_PORT_VAL 2>/dev/null; then
        check_pass "Redis服务可连接: $REDIS_HOST_VAL:$REDIS_PORT_VAL"
    else
        check_fail "Redis服务不可连接: $REDIS_HOST_VAL:$REDIS_PORT_VAL"
    fi
else
    check_warn "nc命令不可用，跳过Redis连接检查"
fi

# 5. 检查端口
echo ""
echo "5. 检查应用端口..."
if command -v nc &> /dev/null; then
    if nc -z -w5 localhost 9922 2>/dev/null; then
        check_warn "端口9922已被占用"
    else
        check_pass "端口9922可用"
    fi
else
    check_warn "nc命令不可用，跳过端口检查"
fi

# 6. 检查环境变量
echo ""
echo "6. 检查环境变量..."

if [ -n "$CLAUDE_API_KEY" ]; then
    check_pass "CLAUDE_API_KEY已设置"
else
    check_warn "CLAUDE_API_KEY未设置（Agent功能需要）"
fi

if [ -n "$MYSQL_DB" ]; then
    check_pass "MYSQL_DB已设置: $MYSQL_DB"
else
    check_warn "MYSQL_DB未设置，将使用默认值: game_maker"
fi

# 7. 检查数据目录
echo ""
echo "7. 检查数据目录..."
DATA_DIR="data"
if [ -d "$DATA_DIR" ]; then
    check_pass "数据目录存在: $DATA_DIR"
else
    check_warn "数据目录不存在，将自动创建: $DATA_DIR"
fi

# 汇总
echo ""
echo "=========================================="
echo "  检查汇总"
echo "=========================================="
echo -e "${GREEN}通过: $PASSED${NC}"
echo -e "${YELLOW}警告: $WARNINGS${NC}"
echo -e "${RED}失败: $FAILED${NC}"
echo ""

if [ $FAILED -gt 0 ]; then
    echo -e "${RED}存在必须解决的问题，请先修复后再启动${NC}"
    exit 1
elif [ $WARNINGS -gt 0 ]; then
    echo -e "${YELLOW}存在警告，但不影响启动${NC}"
    exit 0
else
    echo -e "${GREEN}所有检查通过，可以启动${NC}"
    exit 0
fi
