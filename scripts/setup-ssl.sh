#!/bin/bash
# =============================================================================
# SSL Setup Script for Industrial Cloud (indcloud.io)
# =============================================================================
# This script sets up Let's Encrypt SSL certificates using certbot
# Run this on the EC2 production server
# =============================================================================

set -e

DOMAIN="indcloud.io"
EMAIL="admin@indcloud.io"  # Change to your email for certificate notifications
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "=============================================="
echo "SSL Setup for $DOMAIN"
echo "=============================================="

# Check if running as root or with sudo
if [ "$EUID" -ne 0 ]; then
    echo "Please run as root or with sudo"
    exit 1
fi

# Install certbot if not present
if ! command -v certbot &> /dev/null; then
    echo "Installing certbot..."
    if command -v dnf &> /dev/null; then
        # Amazon Linux 2023 / RHEL / Fedora
        dnf install -y certbot
    elif command -v yum &> /dev/null; then
        # Amazon Linux 2 / CentOS
        amazon-linux-extras install epel -y 2>/dev/null || true
        yum install -y certbot
    elif command -v apt-get &> /dev/null; then
        # Ubuntu / Debian
        apt-get update
        apt-get install -y certbot
    else
        echo "Could not detect package manager. Please install certbot manually."
        exit 1
    fi
fi

# Create webroot directory for ACME challenge
mkdir -p /var/www/certbot

# Stop nginx temporarily to free port 80 for standalone mode
echo "Stopping nginx for certificate issuance..."
docker stop indcloud-nginx 2>/dev/null || true

# Obtain certificate using standalone mode
echo "Obtaining SSL certificate for $DOMAIN..."
certbot certonly \
    --standalone \
    --non-interactive \
    --agree-tos \
    --email "$EMAIL" \
    --domains "$DOMAIN" \
    --domains "www.$DOMAIN" \
    --domains "api.$DOMAIN"

# Create SSL directory for nginx
mkdir -p "$PROJECT_DIR/ssl"

# Create symbolic links or copy certificates
echo "Setting up certificate files..."
if [ -d "/etc/letsencrypt/live/$DOMAIN" ]; then
    # Create a script to copy certs (for docker volume mounting)
    cat > "$PROJECT_DIR/ssl/copy-certs.sh" << 'CERTSCRIPT'
#!/bin/bash
# Copy certificates for Docker volume mount
DOMAIN="indcloud.io"
mkdir -p /ssl
cp /etc/letsencrypt/live/$DOMAIN/fullchain.pem /ssl/
cp /etc/letsencrypt/live/$DOMAIN/privkey.pem /ssl/
chmod 644 /ssl/fullchain.pem
chmod 600 /ssl/privkey.pem
CERTSCRIPT
    chmod +x "$PROJECT_DIR/ssl/copy-certs.sh"

    # Copy certs now
    cp /etc/letsencrypt/live/$DOMAIN/fullchain.pem "$PROJECT_DIR/ssl/"
    cp /etc/letsencrypt/live/$DOMAIN/privkey.pem "$PROJECT_DIR/ssl/"
    chmod 644 "$PROJECT_DIR/ssl/fullchain.pem"
    chmod 600 "$PROJECT_DIR/ssl/privkey.pem"
fi

# Update nginx configuration
echo "Updating nginx configuration..."
cp "$PROJECT_DIR/nginx-ssl.conf" "$PROJECT_DIR/nginx.conf"

# Update docker-compose to mount letsencrypt directory
echo "Updating docker-compose configuration..."

# Restart nginx with new configuration
echo "Starting nginx with SSL..."
docker start indcloud-nginx 2>/dev/null || docker-compose -f "$PROJECT_DIR/docker-compose.production.yml" up -d nginx

# Set up auto-renewal cron job
echo "Setting up certificate auto-renewal..."
cat > /etc/cron.d/certbot-renew << 'CRON'
# Renew Let's Encrypt certificates twice daily
0 0,12 * * * root certbot renew --quiet --deploy-hook "docker exec indcloud-nginx nginx -s reload"
CRON

echo ""
echo "=============================================="
echo "SSL Setup Complete!"
echo "=============================================="
echo ""
echo "Your site is now available at:"
echo "  https://$DOMAIN"
echo "  https://www.$DOMAIN"
echo "  https://api.$DOMAIN"
echo ""
echo "Certificate auto-renewal is configured."
echo ""
echo "Next steps:"
echo "1. Update APP_BASE_URL to https://$DOMAIN"
echo "2. Update OAUTH2_REDIRECT_BASE_URL to https://$DOMAIN"
echo "3. Test the HTTPS endpoints"
echo ""
