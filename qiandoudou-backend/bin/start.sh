#!/bin/bash

################################################################################
# 钱兜兜后端应用启动脚本
# 用途：启动 Spring Boot 应用，配置JVM参数和运行环境
# 使用方式：bash start.sh 或 ./start.sh
################################################################################

set -e

# ============================================================================
# 1. 环境变量配置
# ============================================================================

# 脚本所在目录（应用根目录）
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
APP_HOME="${SCRIPT_DIR}"

# 应用相关路径
APP_JAR="${APP_HOME}/target/qiandoudou-backend-1.0.0.jar"
LOG_DIR="${APP_HOME}/logs"
PID_FILE="${APP_HOME}/qiandoudou.pid"

# 日志文件
LOG_FILE="${LOG_DIR}/qiandoudou-$(date +%Y%m%d-%H%M%S).log"

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

# 检查Java环境
check_java() {
    if ! command -v java &> /dev/null; then
        print_error "Java未安装或不在PATH中"
        exit 1
    fi

    JAVA_VERSION=$(java -version 2>&1 | grep "version" | awk -F'"' '{print $2}')
    print_info "检测到Java版本: $JAVA_VERSION"
}

# 检查JAR文件
check_jar() {
    if [ ! -f "$APP_JAR" ]; then
        print_error "JAR文件不存在: $APP_JAR"
        print_error "请先执行: mvn clean package -DskipTests"
        exit 1
    fi
    print_info "JAR文件检查正常: $APP_JAR"
}

# 创建日志目录
create_log_dir() {
    if [ ! -d "$LOG_DIR" ]; then
        mkdir -p "$LOG_DIR"
        print_info "创建日志目录: $LOG_DIR"
    fi
}

# 检查端口是否被占用
check_port() {
    local PORT=8080
    if command -v netstat &> /dev/null; then
        if netstat -tuln 2>/dev/null | grep -q ":$PORT "; then
            print_error "端口 $PORT 已被占用，请检查是否有其他应用运行"
            exit 1
        fi
    elif command -v ss &> /dev/null; then
        if ss -tuln 2>/dev/null | grep -q ":$PORT "; then
            print_error "端口 $PORT 已被占用，请检查是否有其他应用运行"
            exit 1
        fi
    fi
    print_info "端口检查正常: 8080 未被占用"
}

# 检查已运行的实例
check_existing_process() {
    if [ -f "$PID_FILE" ]; then
        local OLD_PID=$(cat "$PID_FILE")
        if kill -0 "$OLD_PID" 2>/dev/null; then
            print_error "应用已在运行，PID: $OLD_PID"
            print_info "如需重启，请先执行: bash stop.sh"
            exit 1
        else
            print_info "清理旧的PID文件"
            rm -f "$PID_FILE"
        fi
    fi
}

# ============================================================================
# 3. JVM参数配置
# ============================================================================

# JVM内存参数
JVM_MEMORY="-Xms512m -Xmx800m"

# JVM垃圾回收参数（G1GC）
JVM_GC="-XX:+UseG1GC \
-XX:MaxGCPauseMillis=200 \
-XX:+ParallelRefProcEnabled \
-XX:+UnlockDiagnosticVMOptions \
-XX:G1SummarizeRSetStatsPeriod=1"

# JVM其他参数
JVM_OTHER="-Dfile.encoding=UTF-8 \
-Dspring.profiles.active=prod \
-Duser.timezone=Asia/Shanghai"

# 合并所有JVM参数
JVM_OPTS="${JVM_MEMORY} ${JVM_GC} ${JVM_OTHER}"

# ============================================================================
# 4. 应用启动前检查
# ============================================================================

print_info "========== 钱兜兜后端应用启动脚本 =========="
print_info ""

print_info "正在进行启动前检查..."
check_java
check_jar
create_log_dir
check_port
check_existing_process

print_info "所有检查完成，准备启动应用"
print_info ""

# ============================================================================
# 5. 启动应用
# ============================================================================

print_info "启动参数配置如下："
print_info "  内存配置: $JVM_MEMORY"
print_info "  垃圾回收: G1GC (max pause 200ms)"
print_info "  文件编码: UTF-8"
print_info "  应用配置: prod"
print_info "  日志文件: $LOG_FILE"
print_info ""

print_info "正在启动应用..."
print_info "命令: java $JVM_OPTS -jar $APP_JAR"
print_info ""

# 使用nohup后台运行，并保存日志
nohup java $JVM_OPTS -jar "$APP_JAR" > "$LOG_FILE" 2>&1 &
APP_PID=$!

# 保存PID
echo $APP_PID > "$PID_FILE"

# 等待应用启动
print_info "应用启动中，PID: $APP_PID"
sleep 3

# 检查应用是否成功启动
if kill -0 $APP_PID 2>/dev/null; then
    print_success "应用启动成功！"
    print_info ""
    print_info "应用信息："
    print_info "  PID: $APP_PID"
    print_info "  日志: $LOG_FILE"
    print_info "  API地址: http://localhost:8080/api"
    print_info ""
    print_info "常用命令:"
    print_info "  查看日志: tail -f $LOG_FILE"
    print_info "  停止应用: bash stop.sh"
    print_info "  查看状态: ps aux | grep qiandoudou"
    print_info ""
else
    print_error "应用启动失败，请检查日志"
    print_error "日志位置: $LOG_FILE"
    print_error "最后10行日志:"
    tail -10 "$LOG_FILE" || true
    exit 1
fi

# ============================================================================
# 6. 启动后监控
# ============================================================================

print_info "开始监控应用..."
print_info ""

# 监控最多30秒，等待应用完全启动
WAIT_TIME=0
MAX_WAIT=30

while [ $WAIT_TIME -lt $MAX_WAIT ]; do
    if curl -s http://localhost:8080/api/auth/current-user >/dev/null 2>&1; then
        print_success "应用已完全启动，可以接收请求"
        break
    fi

    WAIT_TIME=$((WAIT_TIME + 2))
    sleep 2
done

if [ $WAIT_TIME -ge $MAX_WAIT ]; then
    print_info "应用仍在启动中，请稍候..."
    print_info "可以通过日志查看详细启动信息"
fi

print_info ""
print_success "启动脚本执行完成！"
