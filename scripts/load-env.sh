#!/bin/bash
# =============================================================================
# Load Environment Variables from .env file (Linux/Mac)
# =============================================================================
# Usage:
#   source ./scripts/load-env.sh
#
# Or:
#   . ./scripts/load-env.sh
#
# Note: Must be sourced (not executed) to set variables in current shell
# =============================================================================

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
GRAY='\033[0;37m'
NC='\033[0m' # No Color

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"
ENV_FILE="$PROJECT_ROOT/.env"

if [ ! -f "$ENV_FILE" ]; then
    echo -e "${RED}ERROR: .env file not found at $ENV_FILE${NC}"
    echo -e "${YELLOW}Copy .env.example to .env and configure your settings${NC}"
    return 1
fi

echo -e "${GREEN}Loading environment variables from .env file...${NC}"
echo ""

loaded_vars=0
skipped_vars=0

# Read .env file
while IFS= read -r line || [ -n "$line" ]; do
    # Remove leading/trailing whitespace
    line=$(echo "$line" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')

    # Skip empty lines and comments
    if [[ -z "$line" ]] || [[ "$line" =~ ^# ]]; then
        continue
    fi

    # Parse KEY=VALUE
    if [[ "$line" =~ ^([^=]+)=(.*)$ ]]; then
        key="${BASH_REMATCH[1]}"
        value="${BASH_REMATCH[2]}"

        # Remove quotes if present
        value=$(echo "$value" | sed -e 's/^"//' -e 's/"$//' -e "s/^'//" -e "s/'$//")

        # Export variable
        export "$key=$value"

        echo -e "  ${GRAY}✓ $key${NC}"
        ((loaded_vars++))
    else
        echo -e "  ${YELLOW}⚠ Skipped invalid line: $line${NC}"
        ((skipped_vars++))
    fi
done < "$ENV_FILE"

echo ""
echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}Environment variables loaded:${NC}"
echo -e "${GREEN}  Loaded: $loaded_vars${NC}"
if [ $skipped_vars -gt 0 ]; then
    echo -e "${YELLOW}  Skipped: $skipped_vars${NC}"
fi
echo -e "${GREEN}================================${NC}"
echo ""

# Verify critical variables
echo -e "${CYAN}Verifying critical variables...${NC}"

critical_vars=(
    "DB_USERNAME"
    "DB_PASSWORD"
    "JWT_SECRET"
    "MQTT_USERNAME"
    "MQTT_PASSWORD"
)

missing_critical=()

for var in "${critical_vars[@]}"; do
    value="${!var}"
    if [ -z "$value" ]; then
        echo -e "  ${RED}✗ $var - NOT SET${NC}"
        missing_critical+=("$var")
    else
        # Display first 10 chars + ...
        display_value="${value:0:10}"
        if [ ${#value} -gt 20 ]; then
            display_value="${display_value}..."
        else
            display_value="$value"
        fi
        echo -e "  ${GREEN}✓ $var - SET ($display_value)${NC}"
    fi
done

echo ""

if [ ${#missing_critical[@]} -gt 0 ]; then
    echo -e "${RED}WARNING: Missing critical environment variables!${NC}"
    echo -e "${YELLOW}Please set the following in your .env file:${NC}"
    for var in "${missing_critical[@]}"; do
        echo -e "  ${YELLOW}- $var${NC}"
    done
    echo ""
    return 1
fi

echo -e "${GREEN}All critical variables are set!${NC}"
echo ""
echo -e "${CYAN}You can now run:${NC}"
echo -e "  ${NC}./gradlew bootRun${NC}"
echo ""

# Generate JWT secret if needed
if [ -z "$JWT_SECRET" ] || [ "$JWT_SECRET" == "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970" ]; then
    echo -e "${YELLOW}SECURITY WARNING: Using default or missing JWT_SECRET${NC}"
    echo -e "${YELLOW}Generate a secure secret:${NC}"
    echo -e "  ${NC}openssl rand -hex 32${NC}"
    echo ""
fi
