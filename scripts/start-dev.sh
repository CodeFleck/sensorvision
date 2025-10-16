#!/bin/bash
# =============================================================================
# Start Development Environment (Linux/Mac)
# =============================================================================
# This script:
# 1. Stops any existing containers
# 2. Kills processes on port 8080
# 3. Loads environment variables
# 4. Starts Docker Compose services
# =============================================================================

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
GRAY='\033[0;37m'
NC='\033[0m' # No Color

# Parse arguments
SKIP_PORT_CHECK=false
BUILD_IMAGES=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-port-check)
            SKIP_PORT_CHECK=true
            shift
            ;;
        --build)
            BUILD_IMAGES=true
            shift
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

echo -e "${CYAN}================================${NC}"
echo -e "${CYAN}Starting Development Environment${NC}"
echo -e "${CYAN}================================${NC}"
echo ""

# Step 1: Stop existing containers
echo -e "${YELLOW}Step 1: Stopping existing containers...${NC}"
if docker-compose down 2>/dev/null; then
    echo -e "  ${GREEN}✓ Containers stopped${NC}"
else
    echo -e "  ${GRAY}⚠ No containers to stop${NC}"
fi
echo ""

# Step 2: Check and kill processes on port 8080
if [ "$SKIP_PORT_CHECK" = false ]; then
    echo -e "${YELLOW}Step 2: Checking port 8080...${NC}"

    # Find processes using port 8080
    if command -v lsof &> /dev/null; then
        # Linux/Mac with lsof
        PIDS=$(lsof -ti:8080 2>/dev/null || true)
    elif command -v netstat &> /dev/null; then
        # Fallback to netstat
        PIDS=$(netstat -tlnp 2>/dev/null | grep :8080 | awk '{print $7}' | cut -d'/' -f1 || true)
    else
        echo -e "  ${YELLOW}⚠ Cannot check port (lsof/netstat not found)${NC}"
        PIDS=""
    fi

    if [ -n "$PIDS" ]; then
        echo -e "  ${YELLOW}⚠ Port 8080 is in use${NC}"
        for pid in $PIDS; do
            echo -e "    ${YELLOW}Killing process $pid...${NC}"
            if kill -9 $pid 2>/dev/null; then
                echo -e "    ${GREEN}✓ Process $pid terminated${NC}"
            else
                echo -e "    ${RED}⚠ Could not kill process $pid${NC}"
            fi
        done

        # Wait for port to be released
        sleep 2

        # Verify port is free
        if lsof -ti:8080 &>/dev/null; then
            echo -e "  ${RED}✗ Port 8080 is still in use. Please stop the application manually.${NC}"
            exit 1
        fi
    fi

    echo -e "  ${GREEN}✓ Port 8080 is available${NC}"
else
    echo -e "${GRAY}Step 2: Skipping port check (--skip-port-check)${NC}"
fi
echo ""

# Step 3: Load environment variables
echo -e "${YELLOW}Step 3: Loading environment variables...${NC}"

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ENV_FILE="$SCRIPT_DIR/../.env"

if [ ! -f "$ENV_FILE" ]; then
    echo -e "  ${RED}✗ .env file not found!${NC}"
    echo -e "  ${YELLOW}Copy .env.example to .env and configure your settings${NC}"
    exit 1
fi

loaded_vars=0
while IFS= read -r line || [ -n "$line" ]; do
    line=$(echo "$line" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')

    if [[ -z "$line" ]] || [[ "$line" =~ ^# ]]; then
        continue
    fi

    if [[ "$line" =~ ^([^=]+)=(.*)$ ]]; then
        key="${BASH_REMATCH[1]}"
        value="${BASH_REMATCH[2]}"
        value=$(echo "$value" | sed -e 's/^"//' -e 's/"$//' -e "s/^'//" -e "s/'$//")
        export "$key=$value"
        ((loaded_vars++))
    fi
done < "$ENV_FILE"

echo -e "  ${GREEN}✓ Loaded $loaded_vars environment variables${NC}"
echo ""

# Step 4: Start Docker Compose
echo -e "${YELLOW}Step 4: Starting Docker services...${NC}"

COMPOSE_CMD="docker-compose up -d"
if [ "$BUILD_IMAGES" = true ]; then
    echo -e "  ${CYAN}Building images...${NC}"
    COMPOSE_CMD="docker-compose up -d --build"
fi

if $COMPOSE_CMD; then
    echo ""
    echo -e "  ${GREEN}✓ Services started!${NC}"
    echo ""

    # Step 5: Show status
    echo -e "${YELLOW}Step 5: Checking service health...${NC}"
    sleep 3

    docker-compose ps

    echo ""
    echo -e "${GREEN}================================${NC}"
    echo -e "${GREEN}Development Environment Ready!${NC}"
    echo -e "${GREEN}================================${NC}"
    echo ""

    echo -e "${CYAN}Services available at:${NC}"
    echo "  • Backend API:  http://localhost:8080"
    echo "  • Frontend:     http://localhost:3001"
    echo "  • API Docs:     http://localhost:8080/swagger-ui.html"
    echo "  • Health Check: http://localhost:8080/actuator/health"
    echo ""

    echo -e "${CYAN}Useful commands:${NC}"
    echo -e "  ${GRAY}• View logs:      docker-compose logs -f${NC}"
    echo -e "  ${GRAY}• View backend:   docker-compose logs -f backend${NC}"
    echo -e "  ${GRAY}• Stop services:  docker-compose down${NC}"
    echo -e "  ${GRAY}• Restart:        ./scripts/start-dev.sh${NC}"
    echo ""

else
    echo -e "  ${RED}✗ Failed to start services!${NC}"
    echo ""
    echo -e "${YELLOW}Troubleshooting:${NC}"
    echo -e "  ${GRAY}1. Check Docker is running: docker ps${NC}"
    echo -e "  ${GRAY}2. View logs: docker-compose logs${NC}"
    echo -e "  ${GRAY}3. Check .env file exists and is configured${NC}"
    echo ""
    exit 1
fi
