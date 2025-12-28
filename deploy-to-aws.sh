#!/bin/bash

#########################################################
# Industrial Cloud AWS Deployment Script
# This script automates the deployment to AWS EC2
#########################################################

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
APP_NAME="indcloud"
AWS_REGION="us-east-1"
INSTANCE_TYPE="t3.medium"
KEY_NAME="indcloud-key"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Industrial Cloud AWS Deployment Script${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Function to print colored messages
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Check if AWS CLI is installed
if ! command -v aws &> /dev/null; then
    print_error "AWS CLI is not installed. Please install it first."
    echo "Visit: https://aws.amazon.com/cli/"
    exit 1
fi

print_success "AWS CLI is installed"

# Check if AWS credentials are configured
if ! aws sts get-caller-identity &> /dev/null; then
    print_error "AWS credentials are not configured."
    echo "Run: aws configure"
    exit 1
fi

print_success "AWS credentials are configured"

# Get AWS account ID
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
print_info "AWS Account ID: $ACCOUNT_ID"

# Menu for deployment options
echo ""
echo "Select deployment option:"
echo "1) Deploy to new EC2 instance (recommended for first-time)"
echo "2) Update existing EC2 instance"
echo "3) Deploy using Docker Compose only"
echo "4) Create RDS database"
echo "5) Full AWS infrastructure setup"
echo "6) Exit"
echo ""
read -p "Enter option (1-6): " DEPLOY_OPTION

case $DEPLOY_OPTION in
    1)
        print_info "Deploying to new EC2 instance..."
        source ./scripts/deploy-ec2-new.sh
        ;;
    2)
        print_info "Updating existing EC2 instance..."
        read -p "Enter EC2 instance IP: " EC2_IP
        read -p "Enter SSH key path: " KEY_PATH
        source ./scripts/deploy-ec2-update.sh
        ;;
    3)
        print_info "Deploying with Docker Compose..."
        source ./scripts/deploy-docker-compose.sh
        ;;
    4)
        print_info "Creating RDS database..."
        source ./scripts/create-rds.sh
        ;;
    5)
        print_info "Setting up full AWS infrastructure..."
        source ./scripts/deploy-full-aws.sh
        ;;
    6)
        print_info "Exiting..."
        exit 0
        ;;
    *)
        print_error "Invalid option"
        exit 1
        ;;
esac

echo ""
print_success "Deployment process completed!"
echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Next Steps:${NC}"
echo -e "${GREEN}========================================${NC}"
echo "1. Test the application by visiting the public IP"
echo "2. Configure DNS (optional)"
echo "3. Set up SSL certificate (recommended)"
echo "4. Configure CloudWatch alarms"
echo "5. Set up automated backups"
echo ""
print_info "See AWS_DEPLOYMENT_GUIDE.md for detailed instructions"
