#!/usr/bin/env bash
# ============================================================
# 山川游记 — 自签名 HTTPS 证书生成
# 用法:
#   chmod +x ssl.sh
#   sudo ./ssl.sh
# ============================================================

set -euo pipefail

CERT_DIR="/etc/nginx/ssl"
CERT_KEY="$CERT_DIR/shanhescroll.key"
CERT_CRT="$CERT_DIR/shanhescroll.crt"
DAYS=3650

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
log_info()    { echo -e "${BLUE}[INFO]${NC}  $*"; }
log_success() { echo -e "${GREEN}[OK]${NC}    $*"; }
log_error()   { echo -e "${RED}[ERROR]${NC} $*"; }

if [ "$EUID" -ne 0 ]; then
    log_error "请使用 sudo 运行: sudo ./ssl.sh"
    exit 1
fi

log_info "生成自签名证书（有效期 ${DAYS} 天）..."
mkdir -p "$CERT_DIR"

openssl req -x509 -nodes -days "$DAYS" \
    -newkey rsa:2048 \
    -keyout "$CERT_KEY" \
    -out "$CERT_CRT" \
    -subj "/C=CN/ST=Zhejiang/L=Hangzhou/O=ShanHeScroll/CN=120.26.14.58"

log_success "证书已生成"
log_info "  私钥: $CERT_KEY"
log_info "  证书: $CERT_CRT"

log_info "重载 Nginx..."
nginx -t && systemctl reload nginx && log_success "完成" || log_error "Nginx 配置有误"

echo ""
log_info "访问: https://120.26.14.58"
log_warn "浏览器会警告不安全，点「高级」→「继续访问」即可"
log_warn "阿里云安全组别忘了放行 443 端口!"
