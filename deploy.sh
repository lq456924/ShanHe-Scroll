#!/usr/bin/env bash
# ============================================================
# 山川游记（ShanHeScroll）— 一键重启脚本
# 用法:
#   chmod +x deploy.sh
#   ./deploy.sh              # 重启后端 + 重载 nginx
#   ./deploy.sh -y           # 跳过确认直接执行
# ============================================================

set -euo pipefail

# ============================================================
# 配置区 —— 按你的服务器实际情况修改
# ============================================================
PROJECT_DIR="/opt/shanhescroll"
JAR_FILE="shanhescroll-0.0.1-SNAPSHOT.jar"
CONFIG_FILE="application-prod.properties"
BACKEND_PORT=8080
MAX_WAIT=90
LOG_FILE="/var/log/shanhescroll.log"

# ============================================================
# 颜色输出
# ============================================================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()    { echo -e "${BLUE}[INFO]${NC}  $(date '+%H:%M:%S')  $*"; }
log_success() { echo -e "${GREEN}[OK]${NC}    $(date '+%H:%M:%S')  $*"; }
log_warn()    { echo -e "${YELLOW}[WARN]${NC}  $(date '+%H:%M:%S')  $*"; }
log_error()   { echo -e "${RED}[ERROR]${NC} $(date '+%H:%M:%S')  $*"; }

# ============================================================
# 解析参数
# ============================================================
AUTO_YES=false

for arg in "$@"; do
    case "$arg" in
        -y|--yes) AUTO_YES=true ;;
        -h|--help)
            echo "用法: $0 [-y]"
            echo "  -y, --yes  跳过确认，直接执行"
            exit 0
            ;;
        *) log_error "未知参数: $arg"; exit 1 ;;
    esac
done

# ============================================================
# 前置检查
# ============================================================
log_info "=============================================="
log_info "  山川游记（ShanHeScroll）一键重启"
log_info "=============================================="

cd "$PROJECT_DIR"

if [ ! -f "$JAR_FILE" ]; then
    log_error "JAR 文件不存在: $PROJECT_DIR/$JAR_FILE"
    exit 1
fi

# 确认
if [ "$AUTO_YES" = false ]; then
    echo ""
    log_warn "即将执行:"
    echo "  1. 停止后端进程（端口 $BACKEND_PORT）"
    echo "  2. 启动后端（$JAR_FILE）"
    echo "  3. 重载 Nginx"
    echo ""
    read -r -p "是否继续? [y/N] " yn
    case "$yn" in
        [Yy]*) ;;
        *)     log_info "已取消"; exit 0 ;;
    esac
fi

# ============================================================
# 1. 停止旧后端
# ============================================================
log_info "停止旧后端进程..."

KILLED_COUNT=0
for pid in $(lsof -ti :"$BACKEND_PORT" 2>/dev/null || true); do
    log_info "  正在停止 PID: $pid"
    kill "$pid" 2>/dev/null || true
    KILLED_COUNT=$((KILLED_COUNT + 1))
done

if [ "$KILLED_COUNT" -gt 0 ]; then
    sleep 2
    # 检查是否还有未终止的进程
    STILL_ALIVE=$(lsof -ti :"$BACKEND_PORT" 2>/dev/null || true)
    if [ -n "$STILL_ALIVE" ]; then
        log_warn "  进程未响应，强制终止..."
        for pid in $STILL_ALIVE; do
            kill -9 "$pid" 2>/dev/null || true
        done
        sleep 1
    fi
    log_success "  旧后端已停止（${KILLED_COUNT} 个进程）"
else
    log_info "  未找到运行中的后端进程"
fi

# ============================================================
# 2. 启动新后端
# ============================================================
log_info "启动后端..."

if [ ! -f "$CONFIG_FILE" ]; then
    log_error "配置文件不存在: $PROJECT_DIR/$CONFIG_FILE"
    exit 1
fi

nohup java -jar "$JAR_FILE" \
    --spring.profiles.active=prod \
    --spring.config.additional-location="file:$CONFIG_FILE" \
    > "$LOG_FILE" 2>&1 &

NEW_PID=$!
log_info "  新进程 PID: $NEW_PID"

# 等待启动
for i in $(seq 1 $MAX_WAIT); do
    if curl -s -o /dev/null "http://127.0.0.1:${BACKEND_PORT}/api/public/regions?level=2" 2>/dev/null; then
        log_success "后端启动成功（${i}s）"
        break
    fi
    if ! kill -0 "$NEW_PID" 2>/dev/null; then
        log_error "后端进程已退出，请检查日志:"
        echo ""
        tail -30 "$LOG_FILE"
        exit 1
    fi
    if [ "$i" -eq "$MAX_WAIT" ]; then
        log_error "后端 ${MAX_WAIT}s 内未就绪，请检查日志: tail -50 $LOG_FILE"
        exit 1
    fi
    sleep 1
done

# ============================================================
# 3. 重载 Nginx
# ============================================================
log_info "重载 Nginx..."

if command -v nginx &>/dev/null; then
    if sudo -n nginx -t 2>&1; then
        sudo -n systemctl reload nginx
        log_success "Nginx 重载完成"
    else
        log_error "Nginx 配置检查失败（可能需配置 NOPASSWD sudo）"
        exit 1
    fi
else
    log_warn "nginx 未安装，跳过"
fi

# ============================================================
# 完成
# ============================================================
echo ""
log_success "=============================================="
log_success "  重启完成!"
log_success "=============================================="
echo ""
log_info "验证: curl http://127.0.0.1:${BACKEND_PORT}/api/public/regions?level=2"
echo ""
