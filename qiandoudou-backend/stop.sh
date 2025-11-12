#!/bin/bash

################################################################################
# 钱兜兜后端应用停止脚本
# 用途：优雅停止 Spring Boot 应用
# 使用方式：bash stop.sh 或 ./stop.sh
################################################################################

set -e

# ============================================================================
# 1. 环境变量配置
# ============================================================================

# 脚本所在目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
APP_HOME="${SCRIPT_DIR}"

# PID文件
PID_FILE="${APP_HOME}/qiandoudou.pid"

# ============================================================================
# 2. 函数定义
# ============================================================================

# 打印信息函数
print_info() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [INFO] $1"
}

# 打印错误函数
print_error() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [ERROR] $1" >&2
}

# 打印成功函数
print_success() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [SUCCESS] $1"
}

# ============================================================================
# 3. 获取应用PID
# ============================================================================

print_info "========== 钱兜兜后端应用停止脚本 =========="
print_info ""

# 方式1：从PID文件获取
if [ -f "$PID_FILE" ]; then
    APP_PID=$(cat "$PID_FILE")
    print_info "从PID文件获取: $APP_PID"
else
    # 方式2：从进程列表查找
    print_info "PID文件不存在，从进程列表查找..."
    APP_PID=$(pgrep -f "qiandoudou-backend.*\.jar" | head -1)

    if [ -z "$APP_PID" ]; then
        print_error "未找到运行中的应用"
        exit 1
    fi
    print_info "从进程列表找到: $APP_PID"
fi

# ============================================================================
# 4. 检查进程是否存在
# ============================================================================

if ! kill -0 $APP_PID 2>/dev/null; then
    print_error "应用进程不存在，PID: $APP_PID"
    rm -f "$PID_FILE"
    exit 1
fi

print_info "应用进程存在: $APP_PID"
print_info ""

# ============================================================================
# 5. 优雅停止应用
# ============================================================================

print_info "开始优雅停止应用..."
print_info "发送SIGTERM信号给应用进程"

# 发送SIGTERM信号（优雅停止）
kill -TERM $APP_PID 2>/dev/null || true

# 等待应用停止，最多等待30秒
WAIT_TIME=0
MAX_WAIT=30
WAIT_INTERVAL=1

while [ $WAIT_TIME -lt $MAX_WAIT ]; do
    if ! kill -0 $APP_PID 2>/dev/null; then
        print_success "应用已成功停止"
        rm -f "$PID_FILE"
        print_info ""
        print_success "停止脚本执行完成！"
        exit 0
    fi

    WAIT_TIME=$((WAIT_TIME + WAIT_INTERVAL))
    echo -n "."
    sleep $WAIT_INTERVAL
done

echo ""
print_error "应用未能在 ${MAX_WAIT} 秒内停止，强制杀死进程..."

# ============================================================================
# 6. 强制杀死（如果优雅停止失败）
# ============================================================================

# 发送SIGKILL信号（强制杀死）
kill -9 $APP_PID 2>/dev/null || true

# 确认进程已杀死
sleep 1
if ! kill -0 $APP_PID 2>/dev/null; then
    print_success "应用已强制停止"
    rm -f "$PID_FILE"
else
    print_error "应用仍在运行，可能需要手动处理"
    print_info "请执行: kill -9 $APP_PID"
    exit 1
fi

print_info ""
print_success "停止脚本执行完成！"
