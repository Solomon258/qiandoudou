# 钱兜兜后端应用部署指南

## 📋 目录

1. [系统要求](#系统要求)
2. [安装步骤](#安装步骤)
3. [启动应用](#启动应用)
4. [停止应用](#停止应用)
5. [查看状态](#查看状态)
6. [日志管理](#日志管理)
7. [常见问题](#常见问题)
8. [性能优化](#性能优化)

---

## 系统要求

- **操作系统**: Linux (CentOS/RHEL/Ubuntu 等)
- **Java**: JDK 1.8+ 或 OpenJDK 8+
- **内存**: 最少 1.8GB (建议 2GB+)
- **磁盘**: 最少 500MB 可用空间
- **网络**: 访问外部 API (火山引擎 TTS、阿里云 OSS 等)

### 检查环境

```bash
# 检查 Java 版本
java -version

# 检查内存
free -h

# 检查磁盘空间
df -h
```

---

## 安装步骤

### 1. 准备应用

```bash
# 进入项目后端目录
cd /path/to/qiandoudou-backend

# 编译项目
mvn clean package -DskipTests

# 输出示例:
# [INFO] --- maven-jar-plugin:3.2.0:jar (default-jar) @ qiandoudou-backend ---
# [INFO] Building jar: /path/to/target/qiandoudou-backend-1.0.0.jar
# [INFO] BUILD SUCCESS
```

### 2. 准备启动脚本

```bash
# 给启动脚本添加执行权限
chmod +x start.sh stop.sh status.sh

# 验证脚本
ls -la *.sh
# 应该显示：
# -rwxr-xr-x  1 root root  xxxx start.sh
# -rwxr-xr-x  1 root root  xxxx stop.sh
# -rwxr-xr-x  1 root root  xxxx status.sh
```

### 3. 检查配置文件

确保 `src/main/resources/application.yml` 中的以下配置正确：

```yaml
# 数据库配置
spring:
  datasource:
    url: jdbc:mysql://8.148.206.18:3306/qiandoudou
    username: root
    password: cys1234

# 阿里云 OSS 配置
aliyun:
  oss:
    endpoint: https://oss-cn-guangzhou.aliyuncs.com
    access-key-id: your-key-id
    access-key-secret: your-key-secret
    bucket-name: qiandoudou
```

---

## 启动应用

### 基本启动

```bash
# 进入项目后端目录
cd /path/to/qiandoudou-backend

# 启动应用
bash start.sh
```

**输出示例：**
```
[2025-11-06 10:00:00] [INFO] ========== 钱兜兜后端应用启动脚本 ==========
[2025-11-06 10:00:00] [INFO]
[2025-11-06 10:00:00] [INFO] 正在进行启动前检查...
[2025-11-06 10:00:00] [INFO] 检测到Java版本: 1.8.0_291
[2025-11-06 10:00:00] [INFO] JAR文件检查正常: /path/to/target/qiandoudou-backend-1.0.0.jar
[2025-11-06 10:00:00] [INFO] 创建日志目录: /path/to/logs
[2025-11-06 10:00:00] [INFO] 端口检查正常: 8080 未被占用
[2025-11-06 10:00:01] [SUCCESS] 应用启动成功！
[2025-11-06 10:00:01] [INFO]
[2025-11-06 10:00:01] [INFO] 应用信息：
[2025-11-06 10:00:01] [INFO]   PID: 12345
[2025-11-06 10:00:01] [INFO]   日志: /path/to/logs/qiandoudou-20251106-100000.log
[2025-11-06 10:00:01] [INFO]   API地址: http://localhost:8080/api
```

### 后台启动并离开

```bash
# 使用 nohup 后台启动（即使断开 SSH 连接也继续运行）
nohup bash start.sh > startup.log 2>&1 &

# 查看启动日志
tail -f startup.log
```

### 使用 systemd 启动（推荐用于生产环境）

```bash
# 1. 创建 systemd 服务文件
sudo vim /etc/systemd/system/qiandoudou.service
```

**文件内容：**
```ini
[Unit]
Description=QianDouDou Backend Service
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/path/to/qiandoudou-backend
ExecStart=/bin/bash /path/to/qiandoudou-backend/start.sh
ExecStop=/bin/bash /path/to/qiandoudou-backend/stop.sh
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

```bash
# 2. 重载 systemd
sudo systemctl daemon-reload

# 3. 启动服务
sudo systemctl start qiandoudou

# 4. 设置开机自启
sudo systemctl enable qiandoudou

# 5. 查看服务状态
sudo systemctl status qiandoudou

# 6. 查看日志
sudo journalctl -u qiandoudou -f
```

---

## 停止应用

### 优雅停止（推荐）

```bash
bash stop.sh
```

**输出示例：**
```
[2025-11-06 10:05:00] [INFO] ========== 钱兜兜后端应用停止脚本 ==========
[2025-11-06 10:05:00] [INFO]
[2025-11-06 10:05:00] [INFO] 从PID文件获取: 12345
[2025-11-06 10:05:00] [INFO] 应用进程存在: 12345
[2025-11-06 10:05:00] [INFO]
[2025-11-06 10:05:00] [INFO] 开始优雅停止应用...
[2025-11-06 10:05:00] [INFO] 发送SIGTERM信号给应用进程
....
[2025-11-06 10:05:03] [SUCCESS] 应用已成功停止
[2025-11-06 10:05:03] [SUCCESS] 停止脚本执行完成！
```

### 强制停止

```bash
# 如果优雅停止不工作，使用强制杀死
kill -9 <PID>

# 或使用脚本的强制停止功能（会自动尝试）
bash stop.sh
```

### 使用 systemd 停止

```bash
# 停止服务
sudo systemctl stop qiandoudou

# 重启服务
sudo systemctl restart qiandoudou
```

---

## 查看状态

### 快速状态检查

```bash
bash status.sh
```

**输出示例：**
```
========== 钱兜兜后端应用状态检查 ==========

[INFO] ========== 进程状态 ==========
[SUCCESS] 应用正在运行
[INFO] 进程ID: 12345
  用户: root
  启动时间: 10:00:00
  CPU占用: 0.5%
  内存占用: 18.7%

[INFO] ========== 内存使用 ==========
[INFO] 系统内存: 1100MB / 1800MB (700MB 可用)
[INFO] 应用内存: 350MB
[SUCCESS] 应用内存占用正常: 19%

[INFO] ========== 端口状态 ==========
[SUCCESS] 端口 8080 正在监听

[INFO] ========== API状态 ==========
[SUCCESS] API 可用（返回 401 - 未认证，符合预期）
```

### 手动查看进程

```bash
# 查看 Java 进程
ps aux | grep qiandoudou

# 查看端口监听
netstat -tuln | grep 8080
# 或
ss -tuln | grep 8080

# 查看内存使用
free -h

# 查看进程详细信息
top -p <PID>
```

### 使用 systemd 查看状态

```bash
# 查看服务状态
sudo systemctl status qiandoudou

# 查看实时日志
sudo journalctl -u qiandoudou -f

# 查看最近 100 行日志
sudo journalctl -u qiandoudou -n 100
```

---

## 日志管理

### 日志位置

```bash
# 应用日志目录
/path/to/qiandoudou-backend/logs/

# 最新日志
ls -lt logs/ | head -1

# 查看最新日志
tail -f logs/qiandoudou-*.log
```

### 日志轮转

```bash
# 创建日志轮转配置
sudo vim /etc/logrotate.d/qiandoudou
```

**配置内容：**
```
/path/to/qiandoudou-backend/logs/*.log {
    daily
    rotate 10
    compress
    delaycompress
    notifempty
    create 0600 root root
    sharedscripts
    postrotate
        systemctl reload qiandoudou > /dev/null 2>&1 || true
    endscript
}
```

### 查看特定错误

```bash
# 查看最后 50 行日志
tail -50 logs/qiandoudou-*.log

# 搜索错误日志
grep "ERROR" logs/qiandoudou-*.log

# 搜索特定模块的日志
grep "OssServiceImpl" logs/qiandoudou-*.log

# 实时查看日志（类似 tail -f）
tail -f logs/qiandoudou-*.log
```

---

## 常见问题

### Q1: 启动失败，提示 "JAR文件不存在"

**原因**: 没有编译项目

**解决**:
```bash
# 在后端目录执行编译
cd qiandoudou-backend
mvn clean package -DskipTests
```

### Q2: 启动失败，提示 "端口 8080 已被占用"

**原因**: 端口 8080 被其他应用占用

**解决**:
```bash
# 查看占用端口的进程
lsof -i :8080
# 或
netstat -tuln | grep 8080

# 停止占用该端口的应用，或修改应用配置文件中的端口
```

### Q3: 应用启动后立即退出

**原因**: 可能是内存不足或配置错误

**解决**:
```bash
# 查看启动日志
tail -100 logs/qiandoudou-*.log

# 检查内存
free -h

# 检查数据库连接
# 确保 application.yml 中的数据库配置正确
```

### Q4: 应用运行一段时间后挂掉

**原因**: 可能是线程泄漏或内存溢出

**解决**:
```bash
# 查看应用内存占用
bash status.sh | grep 内存

# 查看日志中的错误
grep "ERROR\|Exception" logs/qiandoudou-*.log | tail -20

# 查看系统日志
dmesg | tail -50
```

### Q5: 无法连接到 API

**原因**: 应用未启动或端口配置错误

**解决**:
```bash
# 检查应用是否运行
bash status.sh

# 检查端口是否监听
netstat -tuln | grep 8080

# 尝试本地连接
curl http://localhost:8080/api/auth/current-user

# 查看防火墙设置
sudo firewall-cmd --list-all
```

---

## 性能优化

### JVM 参数说明

启动脚本中使用的 JVM 参数：

```bash
-Xms512m          # 初始堆大小（推荐用总内存的 30%）
-Xmx800m          # 最大堆大小（推荐用总内存的 44%）
-XX:+UseG1GC      # 使用 G1 垃圾回收器（适合堆大小 1GB+）
-XX:MaxGCPauseMillis=200  # 目标 GC 停顿时间（ms）
```

### 根据系统调整

**如果系统内存 < 2GB：**
```bash
-Xms256m -Xmx512m
```

**如果系统内存 2-4GB：**
```bash
-Xms512m -Xmx1024m
```

**如果系统内存 > 4GB：**
```bash
-Xms1024m -Xmx2048m
```

### 线程池配置

在 `application.yml` 中修改：

```yaml
spring:
  task:
    execution:
      pool:
        core-size: 10        # 核心线程数（根据 CPU 核数调整）
        max-size: 20         # 最大线程数
        queue-capacity: 200  # 队列大小
```

### 数据库连接池配置

在 `application.yml` 中修改：

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 15      # 最大连接数
      minimum-idle: 5            # 最小空闲连接数
```

---

## 监控和维护

### 定期检查

```bash
# 每天检查日志中的错误
grep "ERROR" logs/qiandoudou-*.log | wc -l

# 检查内存使用趋势
bash status.sh

# 检查磁盘空间（日志占用）
du -sh logs/
```

### 定期清理

```bash
# 删除超过 30 天的日志
find logs/ -name "*.log" -mtime +30 -delete

# 压缩日志（如果没有配置 logrotate）
gzip logs/qiandoudou-*.log
```

### 备份

```bash
# 备份数据库
mysqldump -h 8.148.206.18 -u root -p qiandoudou > backup-$(date +%Y%m%d).sql

# 备份应用
tar -czf qiandoudou-backup-$(date +%Y%m%d).tar.gz qiandoudou-backend/
```

---

## 联系支持

如遇问题，请：

1. 查看 `/logs/` 目录下的最新日志
2. 运行 `bash status.sh` 检查应用状态
3. 执行 `dmesg | tail -100` 查看系统日志
4. 收集这些信息后联系开发团队

---

**最后更新**: 2025-11-06
**版本**: 1.0.0
