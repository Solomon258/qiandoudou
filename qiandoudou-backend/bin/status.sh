#!/bin/bash

################################################################################
# 钱兜兜后端应用状态检查脚本
# 用途：检查应用运行状态、内存使用、日志信息等
# 使用方式：bash status.sh 或 ./status.sh
################################################################################

# ============================================================================
# 1. 环境变量配置
# ============================================================================

# 脚本所在目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
APP_HOME="${SCRIPT_DIR}"

# 配置文件
PID_FILE="${APP_HOME}/qiandoudou.pid"
LOG_DIR="${APP_HOME}/logs"

# ============================================================================
# 2. 颜色定义
# ============================================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ============================================================================
# 3. 函数定义
# ============================================================================

# 打印信息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

# 打印成功
print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# 打印警告
print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# 打印错误
print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# ============================================================================
# 4. 检查应用状态
# ============================================================================

check_process_status() {
    echo ""
    print_info "========== 进程状态 =========="

    # 获取PID
    if [ -f "$PID_FILE" ]; then
        APP_PID=$(cat "$PID_FILE")
    else
        APP_PID=$(pgrep -f "qiandoudou-backend.*\.jar" | head -1)
    fi

    # 检查进程
    if [ -z "$APP_PID" ]; then
        print_error "应用未运行"
        return 1
    fi

    if ! kill -0 $APP_PID 2>/dev/null; then
        print_error "应用进程不存在，PID: $APP_PID"
        return 1
    fi

    print_success "应用正在运行"
    print_info "进程ID: $APP_PID"

    # 获取进程信息
    PROC_INFO=$(ps aux | grep $APP_PID | grep -v grep)
    echo "$PROC_INFO" | awk '{print "  用户: " $1 "\n  启动时间: " $9 " " $10 "\n  CPU占用: " $3 "%\n  内存占用: " $4 "%"}'

    return 0
}

# 检查内存使用
check_memory_usage() {
    echo ""
    print_info "========== 内存使用 =========="

    if [ -f "$PID_FILE" ]; then
        APP_PID=$(cat "$PID_FILE")
    else
        APP_PID=$(pgrep -f "qiandoudou-backend.*\.jar" | head -1)
    fi

    if [ -z "$APP_PID" ]; then
        print_warning "应用未运行，无法检查内存"
        return
    fi

    # 获取应用内存使用
    PROC_MEM=$(ps aux | grep $APP_PID | grep -v grep | awk '{print $6}')
    PROC_MEM_MB=$((PROC_MEM / 1024))

    # 获取系统内存信息
    if [ -f /proc/meminfo ]; then
        TOTAL_MEM=$(grep MemTotal /proc/meminfo | awk '{print int($2 / 1024)}')
        AVAIL_MEM=$(grep MemAvailable /proc/meminfo | awk '{print int($2 / 1024)}')
        USED_MEM=$((TOTAL_MEM - AVAIL_MEM))

        print_info "系统内存: ${USED_MEM}MB / ${TOTAL_MEM}MB (${AVAIL_MEM}MB 可用)"
        print_info "应用内存: ${PROC_MEM_MB}MB"

        # 计算内存占比
        MEM_PERCENT=$((PROC_MEM_MB * 100 / TOTAL_MEM))
        if [ $MEM_PERCENT -gt 80 ]; then
            print_error "应用内存占用过高: $MEM_PERCENT%"
        elif [ $MEM_PERCENT -gt 60 ]; then
            print_warning "应用内存占用较高: $MEM_PERCENT%"
        else
            print_success "应用内存占用正常: $MEM_PERCENT%"
        fi
    else
        print_info "应用内存: ${PROC_MEM_MB}MB"
    fi
}

# 检查端口状态
check_port_status() {
    echo ""
    print_info "========== 端口状态 =========="

    PORT=8080
    if command -v netstat &> /dev/null; then
        if netstat -tuln 2>/dev/null | grep -q ":$PORT "; then
            print_success "端口 $PORT 正在监听"
        else
            print_error "端口 $PORT 未监听"
        fi
    elif command -v ss &> /dev/null; then
        if ss -tuln 2>/dev/null | grep -q ":$PORT "; then
            print_success "端口 $PORT 正在监听"
        else
            print_error "端口 $PORT 未监听"
        fi
    else
        print_warning "无法检查端口状态（netstat/ss 不可用）"
    fi
}

# 检查API是否可用
check_api_status() {
    echo ""
    print_info "========== API状态 =========="

    if command -v curl &> /dev/null; then
        # 尝试连接API
        HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/auth/current-user 2>/dev/null || echo "000")

        case $HTTP_CODE in
            200)
                print_success "API 可用（返回 200）"
                ;;
            401)
                print_success "API 可用（返回 401 - 未认证，符合预期）"
                ;;
            502|503)
                print_error "API 不可用（返回 $HTTP_CODE - 网关错误）"
                ;;
            000)
                print_error "无法连接到API（可能应用未启动或端口错误）"
                ;;
            *)
                print_warning "API 返回: $HTTP_CODE"
                ;;
        esac
    else
        print_warning "curl 不可用，无法检查API状态"
    fi
}

# 检查日志
check_recent_logs() {
    echo ""
    print_info "========== 最近日志 =========="

    if [ ! -d "$LOG_DIR" ]; then
        print_warning "日志目录不存在: $LOG_DIR"
        return
    fi

    LATEST_LOG=$(ls -t "$LOG_DIR"/*.log 2>/dev/null | head -1)

    if [ -z "$LATEST_LOG" ]; then
        print_warning "日志文件不存在"
        return
    fi

    print_info "最新日志文件: $LATEST_LOG"
    print_info ""
    echo "最后 20 行日志:"
    echo "---"
    tail -20 "$LATEST_LOG"
    echo "---"
}

# ============================================================================
# 5. 主程序
# ============================================================================

echo ""
echo -e "${BLUE}========== 钱兜兜后端应用状态检查 ==========${NC}"
echo ""

check_process_status
PROCESS_STATUS=$?

check_memory_usage
check_port_status
check_api_status

# 只有应用运行时才显示日志
if [ $PROCESS_STATUS -eq 0 ]; then
    read -p "是否显示最近日志？(y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        check_recent_logs
    fi
fi

echo ""
echo -e "${BLUE}========== 检查完成 ==========${NC}"
echo ""
