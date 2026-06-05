#!/bin/bash
# ============================================
# ChengXun Game Maker 一键重装脚本
# ============================================
# 用法: ./scripts/reinstall.sh [--dev|--prod]
# 等同于: uninstall.sh --force && install.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=========================================="
echo "  ChengXun Game Maker 一键重装"
echo "=========================================="
echo ""

# 卸载（强制，不保留数据）
echo ">>> 开始卸载..."
"$SCRIPT_DIR/uninstall.sh" --force
echo ""

# 安装
echo ">>> 开始安装..."
"$SCRIPT_DIR/install.sh" "$@"
echo ""

echo "=========================================="
echo "  重装完成！"
echo "=========================================="
