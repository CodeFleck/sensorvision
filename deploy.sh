#!/bin/bash
# =============================================================================
# SensorVision Production Deployment Script
# =============================================================================
# This script handles deployment to EC2 instance
# Can be run manually or triggered by GitHub Actions
# =============================================================================

set -e  # Exit on error
set -u  # Exit on undefined variable

# =============================================================================
# Configuration
# =============================================================================
APP_NAME="sensorvision"
COMPOSE_FILE="docker-compose.production.yml"
ENV_FILE=".env.production"
BACKUP_DIR="./backups"
LOG_FILE="./logs/deployment.log"

# Create logs and backups directories early to avoid permission errors
# Remove if they exist with wrong permissions, then recreate
# Use || true to prevent script exit if directories can't be fully deleted
rm -rf logs backups || true
mkdir -p logs backups
touch logs/deployment.log

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# =============================================================================
# Logging Functions
# =============================================================================
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "$LOG_FILE"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR:${NC} $1" | tee -a "$LOG_FILE"
}

warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARNING:${NC} $1" | tee -a "$LOG_FILE"
}

# =============================================================================
# Pre-deployment Checks
# =============================================================================
pre_deployment_checks() {
    log "Running pre-deployment checks..."

    # Check if running as correct user
    if [ "$EUID" -eq 0 ]; then
        warn "Running as root. Consider using a non-root user with docker group membership."
    fi

    # Check if .env.production exists
    if [ ! -f "$ENV_FILE" ]; then
        error "$ENV_FILE not found! Create it from .env.production.template"
        exit 1
    fi

    # Check if docker is installed and running
    if ! command -v docker &> /dev/null; then
        error "Docker is not installed!"
        exit 1
    fi

    if ! docker info &> /dev/null; then
        error "Docker daemon is not running!"
        exit 1
    fi

    # Check if docker-compose is installed
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        error "Docker Compose is not installed!"
        exit 1
    fi

    # Create necessary directories
    mkdir -p "$BACKUP_DIR"
    mkdir -p logs

    log "Pre-deployment checks passed!"
}

# =============================================================================
# ECR Login
# =============================================================================
ecr_login() {
    log "Logging into AWS ECR..."

    # Check if AWS CLI is installed
    if ! command -v aws &> /dev/null; then
        error "AWS CLI is not installed!"
        exit 1
    fi

    # Get AWS region and account ID from environment or .env file
    source "$ENV_FILE"

    if [ -z "${AWS_REGION:-}" ]; then
        error "AWS_REGION not set in $ENV_FILE"
        exit 1
    fi

    if [ -z "${ECR_REGISTRY:-}" ]; then
        error "ECR_REGISTRY not set in $ENV_FILE"
        exit 1
    fi

    # Login to ECR
    aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$ECR_REGISTRY"

    log "ECR login successful!"
}

# =============================================================================
# Backup Current State
# =============================================================================
backup_current_state() {
    log "Backing up current deployment state..."

    BACKUP_TIMESTAMP=$(date +'%Y%m%d_%H%M%S')
    BACKUP_PATH="$BACKUP_DIR/backup_$BACKUP_TIMESTAMP"

    mkdir -p "$BACKUP_PATH"

    # Backup environment file
    cp "$ENV_FILE" "$BACKUP_PATH/"

    # Backup docker-compose file
    cp "$COMPOSE_FILE" "$BACKUP_PATH/"

    # Export current container states
    docker-compose -f "$COMPOSE_FILE" ps > "$BACKUP_PATH/container_states.txt" 2>&1 || true

    # Save current image tags
    docker images | grep "$APP_NAME" > "$BACKUP_PATH/image_tags.txt" 2>&1 || true

    log "Backup created at $BACKUP_PATH"

    # Keep only last 10 backups
    ls -dt $BACKUP_DIR/backup_* | tail -n +11 | xargs rm -rf 2>/dev/null || true
}

# =============================================================================
# Pull Latest Images
# =============================================================================
pull_images() {
    log "Pulling latest Docker images from ECR..."

    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" pull

    log "Images pulled successfully!"
}

# =============================================================================
# Database Migration Check
# =============================================================================
check_database() {
    log "Checking database connectivity..."

    # The backend container will run Flyway migrations automatically on startup
    # This is just a connectivity check

    source "$ENV_FILE"

    # Simple postgres connection test using docker
    docker run --rm -e PGPASSWORD="$DB_PASSWORD" postgres:15-alpine \
        psql -h "$(echo $DB_URL | sed -n 's/.*\/\/\([^:]*\).*/\1/p')" \
        -U "$DB_USERNAME" \
        -d sensorvision \
        -c "SELECT 1;" &> /dev/null

    if [ $? -eq 0 ]; then
        log "Database connection successful!"
    else
        error "Database connection failed! Check RDS endpoint and credentials."
        exit 1
    fi
}

# =============================================================================
# Deploy Application
# =============================================================================
deploy() {
    log "Starting deployment..."

    # Stop and remove old containers
    log "Stopping existing containers..."
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" down

    # Remove old images to free up space (keep last 2 versions)
    log "Cleaning up old images..."
    docker images | grep "$APP_NAME" | awk '{print $3}' | tail -n +3 | xargs -r docker rmi -f 2>/dev/null || true

    # Start new containers
    log "Starting new containers..."
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d

    log "Containers started!"
}

# =============================================================================
# Health Check
# =============================================================================
health_check() {
    log "Running health checks..."

    MAX_RETRIES=30
    RETRY_INTERVAL=5

    for i in $(seq 1 $MAX_RETRIES); do
        if curl -f http://localhost:8080/actuator/health &> /dev/null; then
            log "Health check passed!"
            return 0
        fi

        warn "Health check attempt $i/$MAX_RETRIES failed. Retrying in $RETRY_INTERVAL seconds..."
        sleep $RETRY_INTERVAL
    done

    error "Health check failed after $MAX_RETRIES attempts!"
    return 1
}

# =============================================================================
# Rollback
# =============================================================================
rollback() {
    error "Deployment failed! Rolling back to previous version..."

    # Find the most recent backup
    LATEST_BACKUP=$(ls -dt $BACKUP_DIR/backup_* | head -n 1)

    if [ -z "$LATEST_BACKUP" ]; then
        error "No backup found for rollback!"
        exit 1
    fi

    log "Rolling back to $LATEST_BACKUP"

    # Restore files
    cp "$LATEST_BACKUP/$ENV_FILE" .
    cp "$LATEST_BACKUP/$COMPOSE_FILE" .

    # Restart with old configuration
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" down
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d

    log "Rollback completed!"
}

# =============================================================================
# Display Deployment Info
# =============================================================================
display_info() {
    log "======================================"
    log "Deployment completed successfully!"
    log "======================================"
    log ""
    log "Container Status:"
    docker-compose -f "$COMPOSE_FILE" ps
    log ""
    log "Application URL: http://localhost:8080"
    log "Health Endpoint: http://localhost:8080/actuator/health"
    log "Metrics Endpoint: http://localhost:8080/actuator/prometheus"
    log ""
    log "View logs with: docker-compose -f $COMPOSE_FILE logs -f"
    log "======================================"
}

# =============================================================================
# Main Deployment Flow
# =============================================================================
main() {
    log "======================================"
    log "SensorVision Deployment Started"
    log "======================================"

    # Run pre-deployment checks
    pre_deployment_checks

    # Login to ECR
    ecr_login

    # Backup current state
    backup_current_state

    # Check database connectivity
    check_database

    # Pull latest images
    pull_images

    # Deploy application
    deploy

    # Run health checks
    if ! health_check; then
        rollback
        exit 1
    fi

    # Display deployment info
    display_info

    log "Deployment completed successfully at $(date)"
}

# =============================================================================
# Script Entry Point
# =============================================================================
# Handle script arguments
case "${1:-}" in
    --rollback)
        rollback
        ;;
    --health-check)
        health_check
        ;;
    *)
        main
        ;;
esac
