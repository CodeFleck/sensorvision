# Setup Admin User - Interactive PowerShell Script
# This script helps you create an admin user in production safely

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "SensorVision Admin User Setup" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# Get admin email
$ADMIN_EMAIL = Read-Host "Enter admin email address"
if ([string]::IsNullOrWhiteSpace($ADMIN_EMAIL)) {
    Write-Host "Error: Email is required" -ForegroundColor Red
    exit 1
}

# Get admin password securely
Write-Host ""
$ADMIN_PASSWORD = Read-Host "Enter admin password (minimum 12 characters recommended)" -AsSecureString
$ADMIN_PASSWORD_CONFIRM = Read-Host "Confirm admin password" -AsSecureString

# Convert SecureString to plain text for comparison
$BSTR1 = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($ADMIN_PASSWORD)
$PlainPassword1 = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR1)
$BSTR2 = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($ADMIN_PASSWORD_CONFIRM)
$PlainPassword2 = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR2)

if ($PlainPassword1 -ne $PlainPassword2) {
    Write-Host "Error: Passwords don't match" -ForegroundColor Red
    [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($BSTR1)
    [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($BSTR2)
    exit 1
}

if ($PlainPassword1.Length -lt 12) {
    Write-Host "Warning: Password is less than 12 characters." -ForegroundColor Yellow
    $confirm = Read-Host "Continue anyway? (y/n)"
    if ($confirm -ne "y") {
        [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($BSTR1)
        [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($BSTR2)
        exit 1
    }
}

# Get optional details
Write-Host ""
$ADMIN_FIRST_NAME = Read-Host "Enter admin first name (default: Admin)"
if ([string]::IsNullOrWhiteSpace($ADMIN_FIRST_NAME)) {
    $ADMIN_FIRST_NAME = "Admin"
}

$ADMIN_LAST_NAME = Read-Host "Enter admin last name (default: User)"
if ([string]::IsNullOrWhiteSpace($ADMIN_LAST_NAME)) {
    $ADMIN_LAST_NAME = "User"
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Configuration Summary:" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Email: $ADMIN_EMAIL"
Write-Host "First Name: $ADMIN_FIRST_NAME"
Write-Host "Last Name: $ADMIN_LAST_NAME"
Write-Host "Password: ********** (hidden)"
Write-Host ""
$confirm = Read-Host "Proceed with these settings? (y/n)"
if ($confirm -ne "y") {
    Write-Host "Aborted." -ForegroundColor Yellow
    [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($BSTR1)
    [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($BSTR2)
    exit 1
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Deployment Method" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "1) Update production via SSH to EC2"
Write-Host "2) Deploy code first, then configure production"
Write-Host ""
$deploymentMethod = Read-Host "Select deployment method (1 or 2)"

if ($deploymentMethod -eq "1") {
    Write-Host ""
    Write-Host "You'll need to SSH to your EC2 server and run the following commands:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "# SSH to your server" -ForegroundColor Green
    Write-Host "ssh ec2-user@35.88.65.186" -ForegroundColor White
    Write-Host ""
    Write-Host "# Navigate to the application directory" -ForegroundColor Green
    Write-Host "cd /home/ec2-user/sensorvision" -ForegroundColor White
    Write-Host ""
    Write-Host "# Pull the latest code with admin initializer" -ForegroundColor Green
    Write-Host "git pull origin feature/admin-user-initializer" -ForegroundColor White
    Write-Host ""
    Write-Host "# Set environment variables" -ForegroundColor Green
    Write-Host "export ADMIN_EMAIL=""$ADMIN_EMAIL""" -ForegroundColor White
    Write-Host "export ADMIN_PASSWORD=""$PlainPassword1""" -ForegroundColor White
    Write-Host "export ADMIN_FIRST_NAME=""$ADMIN_FIRST_NAME""" -ForegroundColor White
    Write-Host "export ADMIN_LAST_NAME=""$ADMIN_LAST_NAME""" -ForegroundColor White
    Write-Host ""
    Write-Host "# Rebuild and restart the application" -ForegroundColor Green
    Write-Host "docker-compose -f docker-compose.production.yml up -d --build backend" -ForegroundColor White
    Write-Host ""
    Write-Host "# Check logs for admin creation" -ForegroundColor Green
    Write-Host "docker-compose -f docker-compose.production.yml logs -f backend | grep -A 5 'ADMIN USER'" -ForegroundColor White
    Write-Host ""
    Write-Host "Press Ctrl+C when you see the admin user created message, then:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "# Restart without env vars to remove them" -ForegroundColor Green
    Write-Host "unset ADMIN_EMAIL ADMIN_PASSWORD ADMIN_FIRST_NAME ADMIN_LAST_NAME" -ForegroundColor White
    Write-Host "docker-compose -f docker-compose.production.yml restart backend" -ForegroundColor White

} elseif ($deploymentMethod -eq "2") {
    Write-Host ""
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host "Step 1: Merge and Deploy Code" -ForegroundColor Cyan
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Run these commands locally:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "git checkout main" -ForegroundColor White
    Write-Host "git merge feature/admin-user-initializer" -ForegroundColor White
    Write-Host "git push origin main" -ForegroundColor White
    Write-Host ""
    Write-Host "Then deploy to production (push to server and restart)." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host "Step 2: SSH Configuration Commands" -ForegroundColor Cyan
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "After deployment, SSH to production and run:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "ssh ec2-user@35.88.65.186" -ForegroundColor White
    Write-Host ""
    Write-Host "# Create a one-time setup script" -ForegroundColor Green
    Write-Host @"
cat > /tmp/setup-admin.sh << 'SCRIPT_END'
export ADMIN_EMAIL="$ADMIN_EMAIL"
export ADMIN_PASSWORD="$PlainPassword1"
export ADMIN_FIRST_NAME="$ADMIN_FIRST_NAME"
export ADMIN_LAST_NAME="$ADMIN_LAST_NAME"

cd ~/sensorvision
docker-compose -f docker-compose.production.yml restart backend

echo "Waiting for admin user creation..."
sleep 15

docker-compose -f docker-compose.production.yml logs backend | grep -A 5 "ADMIN USER"

echo ""
echo "Admin user created! Restarting without environment variables..."

# Restart without env vars
docker-compose -f docker-compose.production.yml restart backend

echo "Setup complete! Removing this script..."
rm /tmp/setup-admin.sh
SCRIPT_END
"@ -ForegroundColor White
    Write-Host ""
    Write-Host "chmod +x /tmp/setup-admin.sh && /tmp/setup-admin.sh" -ForegroundColor White
    Write-Host ""

} else {
    Write-Host "Invalid option selected." -ForegroundColor Red
    exit 1
}

# Clear password from memory
[System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($BSTR1)
[System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($BSTR2)

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Next Steps After Setup:" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "1. Test login at: http://35.88.65.186.nip.io:8080" -ForegroundColor White
Write-Host "2. Log in with email: $ADMIN_EMAIL" -ForegroundColor White
Write-Host "3. Change your password immediately after first login!" -ForegroundColor Yellow
Write-Host ""
