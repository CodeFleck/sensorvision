#!/bin/bash

# Setup Admin User - Interactive Script
# This script helps you create an admin user in production safely

set -e

echo "=========================================="
echo "SensorVision Admin User Setup"
echo "=========================================="
echo ""

# Get admin email
read -p "Enter admin email address: " ADMIN_EMAIL
if [[ -z "$ADMIN_EMAIL" ]]; then
    echo "Error: Email is required"
    exit 1
fi

# Get admin password
echo ""
echo "Enter admin password (minimum 12 characters recommended):"
read -s ADMIN_PASSWORD
echo ""
echo "Confirm admin password:"
read -s ADMIN_PASSWORD_CONFIRM
echo ""

if [[ "$ADMIN_PASSWORD" != "$ADMIN_PASSWORD_CONFIRM" ]]; then
    echo "Error: Passwords don't match"
    exit 1
fi

if [[ ${#ADMIN_PASSWORD} -lt 12 ]]; then
    echo "Warning: Password is less than 12 characters. Continue anyway? (y/n)"
    read -r confirm
    if [[ "$confirm" != "y" ]]; then
        exit 1
    fi
fi

# Get optional details
read -p "Enter admin first name (default: Admin): " ADMIN_FIRST_NAME
ADMIN_FIRST_NAME=${ADMIN_FIRST_NAME:-Admin}

read -p "Enter admin last name (default: User): " ADMIN_LAST_NAME
ADMIN_LAST_NAME=${ADMIN_LAST_NAME:-User}

echo ""
echo "=========================================="
echo "Configuration Summary:"
echo "=========================================="
echo "Email: $ADMIN_EMAIL"
echo "First Name: $ADMIN_FIRST_NAME"
echo "Last Name: $ADMIN_LAST_NAME"
echo "Password: ********** (hidden)"
echo ""
read -p "Proceed with these settings? (y/n): " confirm
if [[ "$confirm" != "y" ]]; then
    echo "Aborted."
    exit 1
fi

echo ""
echo "=========================================="
echo "Deployment Method"
echo "=========================================="
echo "1) Docker Compose on EC2 (SSH to server)"
echo "2) Local deployment (for testing)"
echo ""
read -p "Select deployment method (1 or 2): " deployment_method

if [[ "$deployment_method" == "1" ]]; then
    echo ""
    read -p "Enter EC2 server address (e.g., ec2-user@35.88.65.186): " SSH_HOST

    echo ""
    echo "Creating temporary .env file with admin credentials..."

    # Create temporary .env file
    cat > /tmp/admin-setup.env <<EOF
ADMIN_EMAIL=$ADMIN_EMAIL
ADMIN_PASSWORD=$ADMIN_PASSWORD
ADMIN_FIRST_NAME=$ADMIN_FIRST_NAME
ADMIN_LAST_NAME=$ADMIN_LAST_NAME
EOF

    echo "Uploading configuration to server..."
    scp /tmp/admin-setup.env "$SSH_HOST:/tmp/admin-setup.env"

    echo "Deploying with admin user creation..."
    ssh "$SSH_HOST" << 'ENDSSH'
        cd /home/ec2-user/sensorvision || cd ~/sensorvision

        # Load admin env vars temporarily
        export $(cat /tmp/admin-setup.env | xargs)

        # Restart the backend service
        docker-compose -f docker-compose.production.yml restart backend

        # Wait for initialization
        echo "Waiting for application to start..."
        sleep 10

        # Check logs for success message
        echo ""
        echo "Checking logs for admin user creation..."
        docker-compose -f docker-compose.production.yml logs backend | grep -A 5 "ADMIN USER CREATED"

        # Clean up
        rm /tmp/admin-setup.env
        echo ""
        echo "Admin user setup complete!"
        echo "IMPORTANT: The environment variables will be removed on next restart."
ENDSSH

    # Clean up local temp file
    rm /tmp/admin-setup.env

    echo ""
    echo "=========================================="
    echo "Next Steps:"
    echo "=========================================="
    echo "1. Verify the admin user was created (check logs above)"
    echo "2. Test login at: http://35.88.65.186.nip.io:8080"
    echo "3. The admin credentials are now active"
    echo "4. Change your password after first login!"
    echo ""

elif [[ "$deployment_method" == "2" ]]; then
    echo ""
    echo "Setting environment variables for local deployment..."

    export ADMIN_EMAIL="$ADMIN_EMAIL"
    export ADMIN_PASSWORD="$ADMIN_PASSWORD"
    export ADMIN_FIRST_NAME="$ADMIN_FIRST_NAME"
    export ADMIN_LAST_NAME="$ADMIN_LAST_NAME"

    echo "Starting application..."
    ./gradlew bootRun

else
    echo "Invalid option selected."
    exit 1
fi
