# SensorVision AWS Deployment - Complete Beginner's Walkthrough

This guide assumes you have **ZERO** prior AWS experience. We'll walk through every single step together.

**Estimated Total Time**: 2-3 hours
**Cost**: ~$50-80/month (AWS will charge your credit card)

---

## 📋 What You'll Need Before Starting

- [ ] A credit card (AWS requires it, even for free tier)
- [ ] A domain name (optional but recommended - costs ~$12/year from Namecheap or Google Domains)
- [ ] Windows computer (this guide is written for Windows, but includes Mac/Linux notes)
- [ ] Your SensorVision code repository on GitHub
- [ ] About 2-3 hours of uninterrupted time

---

## Part 1: AWS Account Setup (15 minutes)

### Step 1.1: Create AWS Account

1. Go to https://aws.amazon.com/
2. Click **"Create an AWS Account"** (orange button in top right)
3. Fill in:
   - **Email address**: Use your work or personal email
   - **Password**: Create a strong password (save it in a password manager!)
   - **AWS account name**: `sensorvision-production` (or your company name)
4. Click **Continue**

5. Choose **Personal** account type
6. Fill in your contact information (must be accurate - AWS may verify)
7. Enter your credit card information
   - ⚠️ **AWS will charge ~$1 to verify, then refund it**
   - ⚠️ **Monthly costs will be ~$50-80 for this deployment**
8. Verify your phone number (you'll receive a PIN code)
9. Choose **Basic Support - Free** plan
10. Click **Complete Sign Up**

✅ **Checkpoint**: You should receive an email saying "Welcome to Amazon Web Services"

### Step 1.2: Secure Your Root Account (CRITICAL!)

1. Go to https://console.aws.amazon.com/
2. Sign in with the email and password you just created
3. In the search bar at the top, type **IAM** and press Enter
4. In the left sidebar, click **Dashboard**
5. You'll see a security warning about MFA (Multi-Factor Authentication)

**Enable MFA (REQUIRED for security):**

1. Click on your account name (top right) → **Security credentials**
2. Scroll down to **Multi-factor authentication (MFA)**
3. Click **Assign MFA device**
4. Choose:
   - **Device name**: `root-account-mfa`
   - **MFA device**: **Authenticator app** (recommended)
5. Click **Next**
6. On your phone, install **Google Authenticator** or **Microsoft Authenticator**
7. Scan the QR code shown on screen
8. Enter two consecutive MFA codes from your phone app
9. Click **Add MFA**

✅ **Checkpoint**: Your root account now has MFA enabled (green checkmark)

### Step 1.3: Create an IAM User for Yourself (Do NOT use root account!)

1. Still in IAM console, click **Users** in the left sidebar
2. Click **Create user** (orange button)
3. **User name**: `sensorvision-admin`
4. Check ✅ **Provide user access to the AWS Management Console**
5. Choose **I want to create an IAM user**
6. **Custom password**: Create a strong password (save it!)
7. Uncheck **Users must create a new password at next sign-in**
8. Click **Next**

9. **Set permissions**:
   - Choose **Attach policies directly**
   - In the search box, type `AdministratorAccess`
   - Check ✅ **AdministratorAccess** policy
   - ⚠️ **Warning**: This gives full access - only use for yourself!
10. Click **Next**
11. Review and click **Create user**

12. **IMPORTANT**: On the success screen:
    - Copy the **Console sign-in URL** (looks like: https://123456789012.signin.aws.amazon.com/console)
    - Save this URL - you'll use it to login from now on
13. Click **Return to users list**

✅ **Checkpoint**: You now have an admin IAM user created

### Step 1.4: Create Access Keys for CLI/GitHub

1. In IAM → Users, click on **sensorvision-admin**
2. Click the **Security credentials** tab
3. Scroll down to **Access keys**
4. Click **Create access key**
5. Choose **Command Line Interface (CLI)**
6. Check ✅ **I understand the above recommendation...**
7. Click **Next**
8. Description tag: `SensorVision CLI Access`
9. Click **Create access key**

10. **CRITICAL - SAVE THESE NOW**:
    ```
    Access key ID: AKIAIOSFODNN7EXAMPLE
    Secret access key: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
    ```
    - Click **Download .csv file** - save this file securely!
    - ⚠️ **You can NEVER see the secret key again after closing this page!**

11. Click **Done**

✅ **Checkpoint**: You have access keys saved in a CSV file

---

## Part 2: Install Required Tools (20 minutes)

### Step 2.1: Install AWS CLI

**For Windows:**

1. Download: https://awscli.amazonaws.com/AWSCLIV2.msi
2. Double-click the downloaded file
3. Click through the installer (accept defaults)
4. Open **Command Prompt** (press Windows key, type `cmd`, press Enter)
5. Verify installation:
   ```cmd
   aws --version
   ```
   Should show: `aws-cli/2.x.x Python/3.x.x Windows/10 exe/AMD64 prompt/off`

**For Mac:**
```bash
brew install awscli
aws --version
```

**For Linux:**
```bash
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install
aws --version
```

### Step 2.2: Configure AWS CLI

Open Command Prompt (Windows) or Terminal (Mac/Linux):

```cmd
aws configure
```

It will prompt you for 4 things:

```
AWS Access Key ID [None]: PASTE_YOUR_ACCESS_KEY_ID_HERE
AWS Secret Access Key [None]: PASTE_YOUR_SECRET_ACCESS_KEY_HERE
Default region name [None]: us-east-1
Default output format [None]: json
```

**Region Explanation**:
- `us-east-1` = Northern Virginia (cheapest, most services)
- `us-west-2` = Oregon (good backup choice)
- `eu-west-1` = Ireland (if you're in Europe)

**Test it worked:**
```cmd
aws sts get-caller-identity
```

You should see output like:
```json
{
    "UserId": "AIDAXXXXXXXXX",
    "Account": "123456789012",
    "Arn": "arn:aws:iam::123456789012:user/sensorvision-admin"
}
```

✅ **Checkpoint**: AWS CLI is installed and configured

### Step 2.3: Install Git (if not already installed)

**Check if you have Git:**
```cmd
git --version
```

If you see a version number, skip to Step 2.4.

**If not installed:**
- Windows: Download from https://git-scm.com/download/win
- Mac: `brew install git`
- Linux: `sudo apt-get install git`

### Step 2.4: Install Docker Desktop (for local testing only)

1. Download Docker Desktop:
   - Windows/Mac: https://www.docker.com/products/docker-desktop
2. Install and start Docker Desktop
3. Verify:
   ```cmd
   docker --version
   docker-compose --version
   ```

✅ **Checkpoint**: All tools installed

---

## Part 3: Create AWS Infrastructure (45 minutes)

Now we'll create all the AWS resources. We'll go SLOWLY and explain everything.

### Step 3.1: Check Your Default VPC

```cmd
aws ec2 describe-vpcs --filters "Name=isDefault,Values=true"
```

**What is a VPC?** Think of it as your private network in AWS. Every AWS account gets a default one.

You should see output with a `VpcId` like `vpc-0123456789abcdef0`.

**Copy this VPC ID** - you'll need it soon.

### Step 3.2: Find Your Public IP Address

We need this to secure your EC2 instance so only YOU can SSH to it.

1. Go to https://www.whatismyip.com/
2. Copy the IP address shown (example: `203.0.113.45`)
3. Add `/32` to the end: `203.0.113.45/32`
4. Save this - we'll use it in security group rules

### Step 3.3: Create Security Groups

**What is a Security Group?** It's a firewall that controls what traffic can reach your servers.

#### 3.3a: Create EC2 Security Group

```cmd
aws ec2 create-security-group ^
  --group-name sensorvision-ec2-sg ^
  --description "Security group for SensorVision EC2 instance" ^
  --vpc-id vpc-PASTE_YOUR_VPC_ID_HERE
```

**Save the output** - you'll see something like:
```json
{
    "GroupId": "sg-0abc123def456789"
}
```

**Copy that GroupId** - let's call it `EC2_SG_ID` for reference.

Now let's add firewall rules:

```cmd
REM Allow SSH from YOUR IP only
aws ec2 authorize-security-group-ingress ^
  --group-id sg-0abc123def456789 ^
  --protocol tcp ^
  --port 22 ^
  --cidr YOUR_IP_ADDRESS/32

REM Allow HTTP (port 80)
aws ec2 authorize-security-group-ingress ^
  --group-id sg-0abc123def456789 ^
  --protocol tcp ^
  --port 80 ^
  --cidr 0.0.0.0/0

REM Allow HTTPS (port 443)
aws ec2 authorize-security-group-ingress ^
  --group-id sg-0abc123def456789 ^
  --protocol tcp ^
  --port 443 ^
  --cidr 0.0.0.0/0

REM Allow MQTT (port 1883)
aws ec2 authorize-security-group-ingress ^
  --group-id sg-0abc123def456789 ^
  --protocol tcp ^
  --port 1883 ^
  --cidr 0.0.0.0/0

REM Allow application health checks (port 8080)
aws ec2 authorize-security-group-ingress ^
  --group-id sg-0abc123def456789 ^
  --protocol tcp ^
  --port 8080 ^
  --cidr 0.0.0.0/0
```

**Replace**:
- `sg-0abc123def456789` with your actual EC2_SG_ID
- `YOUR_IP_ADDRESS/32` with your actual IP (e.g., `203.0.113.45/32`)

**For Mac/Linux**, replace `^` with `\` and remove `REM`:
```bash
aws ec2 authorize-security-group-ingress \
  --group-id sg-0abc123def456789 \
  --protocol tcp \
  --port 22 \
  --cidr 203.0.113.45/32
```

✅ **Checkpoint**: EC2 security group created with firewall rules

#### 3.3b: Create RDS Security Group

```cmd
aws ec2 create-security-group ^
  --group-name sensorvision-rds-sg ^
  --description "Security group for SensorVision RDS database" ^
  --vpc-id vpc-PASTE_YOUR_VPC_ID_HERE
```

**Save the GroupId** - let's call it `RDS_SG_ID`.

Now allow PostgreSQL (port 5432) from EC2 security group:

```cmd
aws ec2 authorize-security-group-ingress ^
  --group-id RDS_SG_ID ^
  --protocol tcp ^
  --port 5432 ^
  --source-group EC2_SG_ID
```

**Replace**:
- `RDS_SG_ID` with your RDS security group ID
- `EC2_SG_ID` with your EC2 security group ID

✅ **Checkpoint**: RDS security group created

### Step 3.4: Find Subnet IDs

RDS needs to know which subnets to use.

```cmd
aws ec2 describe-subnets --filters "Name=vpc-id,Values=vpc-YOUR_VPC_ID"
```

You'll see multiple subnets. **Copy TWO subnet IDs** that are in DIFFERENT availability zones.

Example output (look for these):
```
subnet-0aaa111 (us-east-1a)
subnet-0bbb222 (us-east-1b)
```

### Step 3.5: Create RDS Subnet Group

```cmd
aws rds create-db-subnet-group ^
  --db-subnet-group-name sensorvision-db-subnet-group ^
  --db-subnet-group-description "Subnet group for SensorVision RDS" ^
  --subnet-ids subnet-0aaa111 subnet-0bbb222
```

Replace `subnet-0aaa111 subnet-0bbb222` with your actual subnet IDs.

✅ **Checkpoint**: RDS subnet group created

### Step 3.6: Create RDS PostgreSQL Database

⚠️ **This will start costing money (~$15/month)**

First, **create a STRONG password** and save it somewhere safe:
- Example: `SensorVision2024!SecureDB#Pass`

```cmd
aws rds create-db-instance ^
  --db-instance-identifier sensorvision-db ^
  --db-instance-class db.t4g.micro ^
  --engine postgres ^
  --engine-version 15.4 ^
  --master-username sensorvision_admin ^
  --master-user-password "YOUR_STRONG_PASSWORD_HERE" ^
  --allocated-storage 20 ^
  --storage-type gp3 ^
  --vpc-security-group-ids RDS_SG_ID ^
  --db-subnet-group-name sensorvision-db-subnet-group ^
  --db-name sensorvision ^
  --backup-retention-period 7 ^
  --storage-encrypted ^
  --no-publicly-accessible
```

**Replace**:
- `YOUR_STRONG_PASSWORD_HERE` with your password
- `RDS_SG_ID` with your RDS security group ID

**This takes 5-10 minutes to create.** Let's check status:

```cmd
aws rds describe-db-instances --db-instance-identifier sensorvision-db --query "DBInstances[0].DBInstanceStatus"
```

Keep running this until you see `"available"`.

**Once available**, get the database endpoint:

```cmd
aws rds describe-db-instances ^
  --db-instance-identifier sensorvision-db ^
  --query "DBInstances[0].Endpoint.Address" ^
  --output text
```

**SAVE THIS ENDPOINT!** It looks like:
```
sensorvision-db.abc123def456.us-east-1.rds.amazonaws.com
```

✅ **Checkpoint**: RDS database created and running

### Step 3.7: Create ECR Repository (Docker Image Registry)

```cmd
aws ecr create-repository ^
  --repository-name sensorvision-backend ^
  --image-scanning-configuration scanOnPush=true
```

**Get the repository URI:**

```cmd
aws ecr describe-repositories ^
  --repository-names sensorvision-backend ^
  --query "repositories[0].repositoryUri" ^
  --output text
```

**SAVE THIS URI!** It looks like:
```
123456789012.dkr.ecr.us-east-1.amazonaws.com/sensorvision-backend
```

✅ **Checkpoint**: ECR repository created

### Step 3.8: Create IAM Role for EC2

This allows your EC2 instance to pull Docker images from ECR.

**Create trust policy file:**

Create a file called `ec2-trust-policy.json` in your `C:\sensorvision\` folder:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "ec2.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
```

**Create the IAM role:**

```cmd
cd C:\sensorvision
aws iam create-role ^
  --role-name sensorvision-ec2-role ^
  --assume-role-policy-document file://ec2-trust-policy.json
```

**Create permissions policy file** (`ecr-policy.json`):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*"
    }
  ]
}
```

**Attach policy to role:**

```cmd
aws iam put-role-policy ^
  --role-name sensorvision-ec2-role ^
  --policy-name sensorvision-ec2-policy ^
  --policy-document file://ecr-policy.json
```

**Create instance profile:**

```cmd
aws iam create-instance-profile ^
  --instance-profile-name sensorvision-ec2-instance-profile

aws iam add-role-to-instance-profile ^
  --instance-profile-name sensorvision-ec2-instance-profile ^
  --role-name sensorvision-ec2-role
```

✅ **Checkpoint**: IAM role created for EC2

### Step 3.9: Create SSH Key Pair

```cmd
aws ec2 create-key-pair ^
  --key-name sensorvision-key ^
  --query "KeyMaterial" ^
  --output text > sensorvision-key.pem
```

**CRITICAL**: This saves your SSH key to `sensorvision-key.pem` in your current directory.

**Keep this file SAFE** - you'll need it to access your server!

✅ **Checkpoint**: SSH key created and saved

### Step 3.10: Create User Data Script for EC2

Create a file called `user-data.sh` in `C:\sensorvision\`:

```bash
#!/bin/bash
set -e

# Update system
apt-get update -y
apt-get upgrade -y

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh

# Install Docker Compose
curl -L "https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# Install AWS CLI
apt-get install -y awscli

# Create application directory
mkdir -p /home/ubuntu/sensorvision
chown ubuntu:ubuntu /home/ubuntu/sensorvision

# Add ubuntu user to docker group
usermod -aG docker ubuntu

# Install certbot for SSL
apt-get install -y certbot python3-certbot-nginx

echo "EC2 initialization complete!"
```

✅ **Checkpoint**: User data script created

### Step 3.11: Launch EC2 Instance

⚠️ **This will start costing money (~$30/month)**

```cmd
aws ec2 run-instances ^
  --image-id ami-0c7217cdde317cfec ^
  --instance-type t3.medium ^
  --key-name sensorvision-key ^
  --security-group-ids EC2_SG_ID ^
  --iam-instance-profile Name=sensorvision-ec2-instance-profile ^
  --block-device-mappings "DeviceName=/dev/sda1,Ebs={VolumeSize=30,VolumeType=gp3}" ^
  --user-data file://user-data.sh ^
  --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=sensorvision-production}]"
```

**Replace** `EC2_SG_ID` with your EC2 security group ID.

**Wait 2-3 minutes** for instance to start.

**Get the public IP address:**

```cmd
aws ec2 describe-instances ^
  --filters "Name=tag:Name,Values=sensorvision-production" ^
  --query "Reservations[0].Instances[0].PublicIpAddress" ^
  --output text
```

**SAVE THIS IP ADDRESS!** Example: `54.123.45.67`

✅ **Checkpoint**: EC2 instance is running!

---

## Part 4: Configure GitHub (20 minutes)

### Step 4.1: Add GitHub Secrets

1. Go to your GitHub repository (example: https://github.com/your-username/sensorvision)
2. Click **Settings** (tab at the top)
3. In the left sidebar, click **Secrets and variables** → **Actions**
4. Click **New repository secret** (green button)

Add these secrets ONE BY ONE (click "New repository secret" for each):

| Secret Name | Value | Where to get it |
|-------------|-------|-----------------|
| `AWS_ACCESS_KEY_ID` | Your access key ID | From the CSV file you downloaded earlier |
| `AWS_SECRET_ACCESS_KEY` | Your secret access key | From the CSV file you downloaded earlier |
| `AWS_REGION` | `us-east-1` | Or whatever region you chose |
| `EC2_HOST` | `54.123.45.67` | Your EC2 public IP address |
| `EC2_USER` | `ubuntu` | Default user for Ubuntu EC2 instances |
| `EC2_SSH_PRIVATE_KEY` | Contents of `sensorvision-key.pem` | Open the file in Notepad, copy ALL text |
| `DB_URL` | `jdbc:postgresql://YOUR_RDS_ENDPOINT:5432/sensorvision` | Replace YOUR_RDS_ENDPOINT with RDS endpoint |
| `DB_USERNAME` | `sensorvision_admin` | What you set when creating RDS |
| `DB_PASSWORD` | Your RDS password | What you set when creating RDS |
| `JWT_SECRET` | Generate new one (see below) | |
| `JWT_EXPIRATION_MS` | `86400000` | 24 hours in milliseconds |
| `JWT_ISSUER` | `https://your-domain.com` | Or `http://YOUR_EC2_IP` if no domain |
| `MQTT_USERNAME` | `sensorvision_mqtt` | Create your own |
| `MQTT_PASSWORD` | Create strong password | Create your own |
| `PRODUCTION_DOMAIN` | `your-domain.com` | Or your EC2 IP |

**To generate JWT_SECRET** (on Windows):
```cmd
REM Method 1: Use PowerShell
powershell -Command "[guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')"

REM Method 2: Random string
echo %RANDOM%%RANDOM%%RANDOM%%RANDOM%%RANDOM%%RANDOM%%RANDOM%%RANDOM%
```

**Example EC2_SSH_PRIVATE_KEY** (must include the header/footer):
```
-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEAxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
... (many lines) ...
xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
-----END RSA PRIVATE KEY-----
```

✅ **Checkpoint**: All GitHub secrets added

---

## Part 5: First Deployment (30 minutes)

### Step 5.1: Build and Push Docker Image Locally

On your LOCAL Windows machine:

```cmd
cd C:\sensorvision

REM Login to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin YOUR_ECR_URI

REM Build the Docker image (this takes 5-10 minutes)
docker build -t sensorvision-backend:latest .

REM Tag the image
docker tag sensorvision-backend:latest YOUR_ECR_URI:latest

REM Push to ECR (this takes 5-10 minutes)
docker push YOUR_ECR_URI:latest
```

**Replace `YOUR_ECR_URI`** with your ECR repository URI (example: `123456789012.dkr.ecr.us-east-1.amazonaws.com/sensorvision-backend`)

✅ **Checkpoint**: Docker image is in ECR

### Step 5.2: SSH to EC2 and Set Up Application

**First, fix SSH key permissions on Windows:**

Right-click `sensorvision-key.pem` → Properties → Security → Advanced:
- Remove all permissions
- Add only your user account with Read permission

**SSH to EC2:**

```cmd
ssh -i sensorvision-key.pem ubuntu@YOUR_EC2_IP
```

If you see a warning about authenticity, type `yes` and press Enter.

**You should now be connected to your EC2 instance!** The prompt should change to `ubuntu@ip-xxx-xxx-xxx-xxx:~$`

**On the EC2 instance, run these commands:**

```bash
# Navigate to app directory
cd /home/ubuntu/sensorvision

# Clone your repository (or copy files)
# Replace with your actual GitHub repo URL
git clone https://github.com/your-username/sensorvision.git .

# Create .env.production from template
cp .env.production.template .env.production

# Edit the file
nano .env.production
```

**In nano editor**, update these values:

```bash
# Press arrow keys to navigate
# Update these values:

AWS_REGION=us-east-1
ECR_REGISTRY=YOUR_ECR_URI_WITHOUT_REPO_NAME
IMAGE_TAG=latest

DB_URL=jdbc:postgresql://YOUR_RDS_ENDPOINT:5432/sensorvision
DB_USERNAME=sensorvision_admin
DB_PASSWORD=YOUR_RDS_PASSWORD

JWT_SECRET=YOUR_JWT_SECRET_FROM_GITHUB_SECRETS
JWT_ISSUER=http://YOUR_EC2_IP

MQTT_USERNAME=sensorvision_mqtt
MQTT_PASSWORD=YOUR_MQTT_PASSWORD

# Save and exit:
# Press Ctrl+X
# Press Y
# Press Enter
```

**Set permissions:**

```bash
chmod 600 .env.production
```

**Login to ECR from EC2:**

```bash
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin YOUR_ECR_URI
```

**Run deployment script:**

```bash
chmod +x deploy.sh
./deploy.sh
```

**This will:**
1. Check prerequisites
2. Login to ECR
3. Pull Docker image
4. Check database connection
5. Start containers
6. Run health checks

**Wait 2-3 minutes** for containers to start.

✅ **Checkpoint**: Application is deployed!

### Step 5.3: Test the Deployment

**Still on EC2**, test locally:

```bash
curl http://localhost:8080/actuator/health
```

You should see:
```json
{"status":"UP"}
```

**From your Windows machine**, test remotely:

```cmd
curl http://YOUR_EC2_IP:8080/actuator/health
```

**Open in browser:**
```
http://YOUR_EC2_IP:8080
```

You should see the SensorVision frontend!

✅ **Checkpoint**: Application is accessible!

---

## Part 6: Set Up Domain and SSL (Optional, 30 minutes)

**Skip this if you don't have a domain yet.**

### Step 6.1: Point Domain to EC2

1. Log in to your domain registrar (Namecheap, GoDaddy, Google Domains, etc.)
2. Find DNS settings
3. Create an **A Record**:
   - **Name**: `@` (or leave blank)
   - **Type**: `A`
   - **Value**: `YOUR_EC2_IP`
   - **TTL**: `300`

4. Wait 5-10 minutes for DNS to propagate

**Test DNS:**
```cmd
nslookup your-domain.com
```

Should return your EC2 IP.

### Step 6.2: Get SSL Certificate

**SSH to EC2:**

```bash
ssh -i sensorvision-key.pem ubuntu@YOUR_EC2_IP
cd /home/ubuntu/sensorvision

# Update nginx.conf with your domain
nano nginx.conf
# Replace YOUR_DOMAIN_HERE with your actual domain
# Save with Ctrl+X, Y, Enter

# Restart nginx to load new config
docker-compose -f docker-compose.production.yml restart nginx

# Get SSL certificate
sudo certbot --nginx -d your-domain.com

# Follow the prompts:
# - Enter your email
# - Agree to terms
# - Choose whether to redirect HTTP to HTTPS (choose Yes)
```

**Test SSL:**
```
https://your-domain.com
```

Should show a secure connection!

✅ **Checkpoint**: SSL certificate installed!

---

## Part 7: Enable Automated Deployments (5 minutes)

This is already set up! Just commit and push to trigger deployment:

```cmd
cd C:\sensorvision

git add .
git commit -m "Initial production setup"
git push origin main
```

**Watch the deployment:**
1. Go to your GitHub repository
2. Click **Actions** tab
3. You'll see "Deploy to Production" workflow running

**It will:**
- Run tests
- Build Docker image
- Push to ECR
- Deploy to EC2
- Run health checks

✅ **Checkpoint**: CI/CD pipeline is active!

---

## Part 8: Monitoring and Verification

### Check if Everything is Running

**SSH to EC2:**
```bash
ssh -i sensorvision-key.pem ubuntu@YOUR_EC2_IP
cd /home/ubuntu/sensorvision

# Check container status
docker-compose -f docker-compose.production.yml ps

# Should show:
# - sensorvision-backend (running)
# - sensorvision-mosquitto (running)
# - sensorvision-nginx (running)

# View logs
docker-compose -f docker-compose.production.yml logs -f backend
```

### Test All Endpoints

```bash
# Health check
curl https://your-domain.com/actuator/health

# API test (should return 401 - not authenticated)
curl https://your-domain.com/api/v1/devices

# MQTT test
mosquitto_pub -h your-domain.com -p 1883 -t "test" -m "Hello"
```

---

## 🎉 Congratulations! You're Done!

Your SensorVision application is now running on AWS!

### What You've Accomplished:

✅ Created AWS account with proper security
✅ Created RDS PostgreSQL database
✅ Created EC2 instance with Docker
✅ Created ECR repository for Docker images
✅ Set up GitHub Actions CI/CD pipeline
✅ Deployed application to production
✅ (Optional) Set up SSL certificate

### Next Steps:

1. **Monitor your costs**: https://console.aws.amazon.com/billing/
2. **Set up CloudWatch alarms** (see DEPLOYMENT_OPERATIONS.md)
3. **Create database backups** (RDS does this automatically!)
4. **Invite users** to test your application

### Important Files to Keep Safe:

- `sensorvision-key.pem` - SSH key to access EC2
- AWS access keys CSV file
- `.env.production` on EC2 (already there)
- Your RDS password

### If Something Goes Wrong:

1. Check logs: `docker-compose logs -f backend`
2. Restart: `docker-compose restart`
3. Check database: `psql -h RDS_ENDPOINT -U sensorvision_admin -d sensorvision`
4. See DEPLOYMENT_OPERATIONS.md for troubleshooting

### Getting Help:

- AWS Support: https://console.aws.amazon.com/support
- Check logs in CloudWatch
- Review DEPLOYMENT_OPERATIONS.md

---

## 💰 Cost Breakdown (Monthly)

- EC2 t3.medium: ~$30
- RDS db.t4g.micro: ~$15
- EBS storage: ~$3
- ECR storage: ~$1
- Data transfer: ~$5-10
- **Total: ~$55-65/month**

**To reduce costs:**
- Stop EC2 when not in use: `aws ec2 stop-instances --instance-ids i-xxxxx`
- Take RDS snapshot and delete instance when testing
- Use Reserved Instances for 1-year commitment (40% discount)

---

## Quick Reference Commands

```bash
# SSH to EC2
ssh -i sensorvision-key.pem ubuntu@YOUR_EC2_IP

# Check application status
docker-compose -f docker-compose.production.yml ps

# View logs
docker-compose -f docker-compose.production.yml logs -f

# Restart application
docker-compose -f docker-compose.production.yml restart

# Deploy new version
./deploy.sh

# Check health
curl http://localhost:8080/actuator/health
```

---

**You did it!** 🚀

Save this document for future reference. Your application is now running in production on AWS!
