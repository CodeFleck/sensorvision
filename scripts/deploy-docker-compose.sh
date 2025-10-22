#!/bin/bash

# Deploy using Docker Compose
# This script builds and deploys the application using Docker Compose

print_info "Preparing Docker Compose deployment..."

# Check if Docker is running
if ! docker info &> /dev/null; then
    print_error "Docker is not running. Please start Docker first."
    exit 1
fi

print_success "Docker is running"

# Check if Docker Compose is installed
if ! docker-compose version &> /dev/null; then
    print_error "Docker Compose is not installed."
    exit 1
fi

print_success "Docker Compose is installed"

# Environment configuration
read -p "Environment file (.env) path [.env.prod]: " ENV_FILE
ENV_FILE=${ENV_FILE:-.env.prod}

if [ ! -f "$ENV_FILE" ]; then
    print_warning "Environment file not found. Creating template..."

    cat > "$ENV_FILE" <<EOF
# Database Configuration
DB_URL=jdbc:postgresql://postgres:5432/sensorvision
DB_USERNAME=sensoradmin
DB_PASSWORD=changeme_secure_password

# MQTT Configuration
MQTT_URL=tcp://mosquitto:1883
MQTT_CLIENT_ID=sensorvision-prod

# Application Configuration
SIMULATOR_ENABLED=false
SPRING_PROFILES_ACTIVE=prod

# Grafana Configuration (if using monitoring)
GRAFANA_USER=admin
GRAFANA_PASSWORD=changeme_secure_password
EOF

    print_info "Template environment file created at: $ENV_FILE"
    print_warning "Please edit $ENV_FILE with your production values before deploying!"
    read -p "Press Enter after updating the file..."
fi

# Load environment variables
export $(cat "$ENV_FILE" | xargs)

# Build the application
print_info "Building application..."
./gradlew clean build -x test

if [ $? -ne 0 ]; then
    print_error "Build failed"
    exit 1
fi

print_success "Application built successfully"

# Build frontend
print_info "Building frontend..."
cd frontend
npm install
npm run build

if [ $? -ne 0 ]; then
    print_error "Frontend build failed"
    exit 1
fi

cd ..
print_success "Frontend built successfully"

# Stop existing containers
print_info "Stopping existing containers..."
docker-compose -f docker-compose.prod.yml down

# Build Docker images
print_info "Building Docker images..."
docker-compose -f docker-compose.prod.yml build

if [ $? -ne 0 ]; then
    print_error "Docker build failed"
    exit 1
fi

print_success "Docker images built successfully"

# Start containers
print_info "Starting containers..."
docker-compose -f docker-compose.prod.yml up -d

if [ $? -ne 0 ]; then
    print_error "Failed to start containers"
    exit 1
fi

print_success "Containers started successfully"

# Wait for application to be healthy
print_info "Waiting for application to be healthy..."
sleep 10

for i in {1..30}; do
    if curl -f http://localhost:8080/actuator/health &> /dev/null; then
        print_success "Application is healthy!"
        break
    fi

    if [ $i -eq 30 ]; then
        print_error "Application failed to start"
        print_info "Showing logs:"
        docker-compose -f docker-compose.prod.yml logs app
        exit 1
    fi

    echo -n "."
    sleep 2
done

echo ""

# Show running containers
print_info "Running containers:"
docker-compose -f docker-compose.prod.yml ps

echo ""
print_success "Deployment completed successfully!"
echo ""
echo "Access the application at:"
echo "  - Web Interface: http://localhost"
echo "  - API: http://localhost:8080/api/v1"
echo "  - Health Check: http://localhost:8080/actuator/health"
echo "  - MQTT: mqtt://localhost:1883"
echo ""
echo "To view logs:"
echo "  docker-compose -f docker-compose.prod.yml logs -f"
echo ""
echo "To stop:"
echo "  docker-compose -f docker-compose.prod.yml down"
