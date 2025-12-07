#!/bin/bash

# =============================================================================
# SensorVision Pilot Program Deployment Script
# =============================================================================
# This script deploys the SensorVision platform for the pilot program with
# enhanced security, monitoring, and pilot-specific configurations.
# =============================================================================

set -e  # Exit on any error

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_FILE="${SCRIPT_DIR}/logs/pilot-deployment-$(date +%Y%m%d-%H%M%S).log"
BACKUP_DIR="${SCRIPT_DIR}/backups"
ENV_FILE="${SCRIPT_DIR}/.env.pilot"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging function
log() {
    echo -e "${1}" | tee -a "${LOG_FILE}"
}

log_info() {
    log "${BLUE}[INFO]${NC} ${1}"
}

log_success() {
    log "${GREEN}[SUCCESS]${NC} ${1}"
}

log_warning() {
    log "${YELLOW}[WARNING]${NC} ${1}"
}

log_error() {
    log "${RED}[ERROR]${NC} ${1}"
}

# Create necessary directories
create_directories() {
    log_info "Creating necessary directories..."
    
    mkdir -p "${SCRIPT_DIR}/logs"
    mkdir -p "${SCRIPT_DIR}/backups"
    mkdir -p "${SCRIPT_DIR}/certs"
    mkdir -p "${SCRIPT_DIR}/prometheus/rules"
    mkdir -p "${SCRIPT_DIR}/grafana/provisioning/datasources"
    mkdir -p "${SCRIPT_DIR}/grafana/provisioning/dashboards"
    mkdir -p "${SCRIPT_DIR}/grafana/dashboards"
    mkdir -p "${SCRIPT_DIR}/alertmanager"
    mkdir -p "${SCRIPT_DIR}/nginx/conf.d"
    mkdir -p "${SCRIPT_DIR}/mosquitto/config"
    mkdir -p "${SCRIPT_DIR}/mosquitto/data"
    mkdir -p "${SCRIPT_DIR}/mosquitto/log"
    mkdir -p "${SCRIPT_DIR}/fluentd"
    mkdir -p "${SCRIPT_DIR}/scripts"
    
    log_success "Directories created successfully"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check if Docker is installed and running
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        log_error "Docker is not running. Please start Docker first."
        exit 1
    fi
    
    # Check if Docker Compose is installed
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose is not installed. Please install Docker Compose first."
        exit 1
    fi
    
    # Check if environment file exists
    if [[ ! -f "${ENV_FILE}" ]]; then
        log_warning "Environment file ${ENV_FILE} not found. Creating from template..."
        cp "${SCRIPT_DIR}/.env.pilot.template" "${ENV_FILE}"
        log_warning "Please edit ${ENV_FILE} with your actual configuration values before continuing."
        read -p "Press Enter to continue after editing the environment file..."
    fi
    
    log_success "Prerequisites check completed"
}

# Generate SSL certificates for development/testing
generate_ssl_certs() {
    log_info "Generating SSL certificates..."
    
    CERT_DIR="${SCRIPT_DIR}/certs"
    
    if [[ ! -f "${CERT_DIR}/server.crt" ]]; then
        log_info "Generating self-signed SSL certificate for pilot.sensorvision.io..."
        
        # Generate private key
        openssl genrsa -out "${CERT_DIR}/server.key" 2048
        
        # Generate certificate signing request
        openssl req -new -key "${CERT_DIR}/server.key" -out "${CERT_DIR}/server.csr" \
            -subj "/C=US/ST=CA/L=San Francisco/O=SensorVision/OU=Pilot Program/CN=pilot.sensorvision.io"
        
        # Generate self-signed certificate
        openssl x509 -req -days 365 -in "${CERT_DIR}/server.csr" -signkey "${CERT_DIR}/server.key" \
            -out "${CERT_DIR}/server.crt" \
            -extensions v3_req -extfile <(cat <<EOF
[v3_req]
keyUsage = keyEncipherment, dataEncipherment
extendedKeyUsage = serverAuth
subjectAltName = @alt_names

[alt_names]
DNS.1 = pilot.sensorvision.io
DNS.2 = localhost
DNS.3 = *.pilot.sensorvision.io
IP.1 = 127.0.0.1
EOF
)
        
        # Set appropriate permissions
        chmod 600 "${CERT_DIR}/server.key"
        chmod 644 "${CERT_DIR}/server.crt"
        
        log_success "SSL certificates generated successfully"
    else
        log_info "SSL certificates already exist, skipping generation"
    fi
}

# Create configuration files
create_config_files() {
    log_info "Creating configuration files..."
    
    # Prometheus configuration
    cat > "${SCRIPT_DIR}/prometheus/prometheus.yml" <<EOF
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "rules/*.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

scrape_configs:
  - job_name: 'sensorvision-pilot'
    static_configs:
      - targets: ['backend:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
    
  - job_name: 'postgres-pilot'
    static_configs:
      - targets: ['postgres:5432']
    
  - job_name: 'redis-pilot'
    static_configs:
      - targets: ['redis:6379']
    
  - job_name: 'mosquitto-pilot'
    static_configs:
      - targets: ['mosquitto:1883']
EOF

    # Grafana datasource configuration
    cat > "${SCRIPT_DIR}/grafana/provisioning/datasources/prometheus.yml" <<EOF
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
EOF

    # AlertManager configuration
    cat > "${SCRIPT_DIR}/alertmanager/alertmanager.yml" <<EOF
global:
  smtp_smarthost: 'email-smtp.us-west-2.amazonaws.com:587'
  smtp_from: 'alerts@pilot.sensorvision.io'

route:
  group_by: ['alertname', 'pilot']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'pilot-team'

receivers:
  - name: 'pilot-team'
    email_configs:
      - to: 'pilot-team@sensorvision.io'
        subject: 'SensorVision Pilot Alert: {{ .GroupLabels.alertname }}'
        body: |
          {{ range .Alerts }}
          Alert: {{ .Annotations.summary }}
          Description: {{ .Annotations.description }}
          {{ end }}
EOF

    # Nginx configuration
    cat > "${SCRIPT_DIR}/nginx/nginx.conf" <<EOF
events {
    worker_connections 1024;
}

http {
    upstream backend {
        server backend:8080;
    }
    
    upstream grafana {
        server grafana:3000;
    }
    
    server {
        listen 80;
        server_name pilot.sensorvision.io localhost;
        return 301 https://\$server_name\$request_uri;
    }
    
    server {
        listen 443 ssl http2;
        server_name pilot.sensorvision.io localhost;
        
        ssl_certificate /etc/nginx/certs/server.crt;
        ssl_certificate_key /etc/nginx/certs/server.key;
        
        # Security headers
        add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
        add_header X-Frame-Options "DENY" always;
        add_header X-Content-Type-Options "nosniff" always;
        add_header X-XSS-Protection "1; mode=block" always;
        add_header Referrer-Policy "strict-origin-when-cross-origin" always;
        
        location / {
            proxy_pass http://backend;
            proxy_set_header Host \$host;
            proxy_set_header X-Real-IP \$remote_addr;
            proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto \$scheme;
        }
        
        location /grafana/ {
            proxy_pass http://grafana/;
            proxy_set_header Host \$host;
            proxy_set_header X-Real-IP \$remote_addr;
            proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto \$scheme;
        }
    }
}
EOF

    # Mosquitto configuration
    cat > "${SCRIPT_DIR}/mosquitto/config/mosquitto.conf" <<EOF
# Mosquitto configuration for SensorVision Pilot
listener 1883
protocol mqtt

listener 8883
protocol mqtt
cafile /mosquitto/certs/ca.crt
certfile /mosquitto/certs/server.crt
keyfile /mosquitto/certs/server.key

listener 9001
protocol websockets

allow_anonymous false
password_file /mosquitto/config/passwd

log_dest file /mosquitto/log/mosquitto.log
log_type all
connection_messages true
log_timestamp true

max_connections 1000
max_inflight_messages 100
max_queued_messages 1000
EOF

    # Backup script
    cat > "${SCRIPT_DIR}/scripts/backup.sh" <<'EOF'
#!/bin/bash

set -e

BACKUP_DIR="/backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/sensorvision_pilot_${TIMESTAMP}.sql"

echo "Starting database backup at $(date)"

# Create database backup
pg_dump -h postgres -U "${DB_USERNAME}" -d "${POSTGRES_DB}" > "${BACKUP_FILE}"

# Compress backup
gzip "${BACKUP_FILE}"

# Upload to S3 if configured
if [[ -n "${AWS_ACCESS_KEY_ID}" && -n "${BACKUP_S3_BUCKET}" ]]; then
    echo "Uploading backup to S3..."
    aws s3 cp "${BACKUP_FILE}.gz" "s3://${BACKUP_S3_BUCKET}/database/$(basename ${BACKUP_FILE}.gz)"
fi

# Clean up old backups (keep last 7 days)
find "${BACKUP_DIR}" -name "sensorvision_pilot_*.sql.gz" -mtime +7 -delete

echo "Backup completed successfully at $(date)"
EOF

    chmod +x "${SCRIPT_DIR}/scripts/backup.sh"
    
    log_success "Configuration files created successfully"
}

# Build and start services
deploy_services() {
    log_info "Building and starting pilot services..."
    
    # Load environment variables
    source "${ENV_FILE}"
    
    # Build the application
    log_info "Building SensorVision application..."
    ./gradlew clean build -x test
    
    # Stop existing services
    log_info "Stopping existing services..."
    docker-compose -f docker-compose.pilot.yml down --remove-orphans || true
    
    # Start services
    log_info "Starting pilot services..."
    docker-compose -f docker-compose.pilot.yml up -d --build
    
    log_success "Services started successfully"
}

# Wait for services to be healthy
wait_for_services() {
    log_info "Waiting for services to be healthy..."
    
    local max_attempts=60
    local attempt=1
    
    while [[ ${attempt} -le ${max_attempts} ]]; do
        log_info "Health check attempt ${attempt}/${max_attempts}..."
        
        # Check if backend is healthy
        if curl -f http://localhost:8080/actuator/health &> /dev/null; then
            log_success "Backend service is healthy"
            break
        fi
        
        if [[ ${attempt} -eq ${max_attempts} ]]; then
            log_error "Services failed to become healthy within expected time"
            log_error "Check logs: docker-compose -f docker-compose.pilot.yml logs"
            exit 1
        fi
        
        sleep 10
        ((attempt++))
    done
}

# Run database migrations
run_migrations() {
    log_info "Running database migrations..."
    
    # Wait for database to be ready
    sleep 10
    
    # Run migrations through the application
    docker-compose -f docker-compose.pilot.yml exec -T backend \
        java -jar /app/app.jar --spring.profiles.active=pilot --spring.flyway.migrate=true || true
    
    log_success "Database migrations completed"
}

# Setup monitoring
setup_monitoring() {
    log_info "Setting up monitoring and alerting..."
    
    # Wait for Prometheus to be ready
    sleep 15
    
    # Reload Prometheus configuration
    curl -X POST http://localhost:9090/-/reload || true
    
    log_success "Monitoring setup completed"
}

# Create initial pilot data
create_pilot_data() {
    log_info "Creating initial pilot program data..."
    
    # This would typically involve:
    # - Creating pilot organizations
    # - Setting up admin users
    # - Configuring initial dashboards
    # - Setting up sample devices
    
    log_info "Pilot data creation completed (manual setup required)"
}

# Display deployment summary
show_deployment_summary() {
    log_success "=========================================="
    log_success "  SensorVision Pilot Deployment Complete"
    log_success "=========================================="
    log_success ""
    log_success "Services Status:"
    docker-compose -f docker-compose.pilot.yml ps
    log_success ""
    log_success "Access URLs:"
    log_success "  Application:     https://localhost (or https://pilot.sensorvision.io)"
    log_success "  Grafana:         http://localhost:3000"
    log_success "  Prometheus:      http://localhost:9090"
    log_success "  AlertManager:    http://localhost:9093"
    log_success ""
    log_success "MQTT Endpoints:"
    log_success "  Standard:        mqtt://localhost:1883"
    log_success "  SSL/TLS:         mqtts://localhost:8883"
    log_success "  WebSocket:       ws://localhost:9001"
    log_success ""
    log_success "Management Commands:"
    log_success "  View logs:       docker-compose -f docker-compose.pilot.yml logs -f"
    log_success "  Stop services:   docker-compose -f docker-compose.pilot.yml down"
    log_success "  Restart:         docker-compose -f docker-compose.pilot.yml restart"
    log_success "  Backup DB:       docker-compose -f docker-compose.pilot.yml run --rm backup"
    log_success ""
    log_success "Next Steps:"
    log_success "  1. Configure DNS to point pilot.sensorvision.io to this server"
    log_success "  2. Set up proper SSL certificates (replace self-signed)"
    log_success "  3. Configure AWS SES for email notifications"
    log_success "  4. Set up Twilio for SMS notifications"
    log_success "  5. Create pilot organizations and users"
    log_success "  6. Test device integration"
    log_success ""
    log_success "Support:"
    log_success "  Logs location:   ${SCRIPT_DIR}/logs/"
    log_success "  Backup location: ${SCRIPT_DIR}/backups/"
    log_success "  Config location: ${SCRIPT_DIR}/"
    log_success ""
}

# Main deployment function
main() {
    log_info "Starting SensorVision Pilot Program deployment..."
    log_info "Deployment log: ${LOG_FILE}"
    
    create_directories
    check_prerequisites
    generate_ssl_certs
    create_config_files
    deploy_services
    wait_for_services
    run_migrations
    setup_monitoring
    create_pilot_data
    show_deployment_summary
    
    log_success "SensorVision Pilot Program deployment completed successfully!"
}

# Handle script interruption
trap 'log_error "Deployment interrupted"; exit 1' INT TERM

# Run main function
main "$@"