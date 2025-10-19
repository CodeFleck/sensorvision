# SensorVision AWS Deployment Checklist

Print this checklist or keep it open while deploying. Check off each item as you complete it.

---

## Pre-Deployment Preparation

- [ ] Credit card ready for AWS
- [ ] Domain name purchased (optional)
- [ ] 2-3 hours of uninterrupted time available
- [ ] Windows computer ready
- [ ] SensorVision code repository on GitHub

---

## Part 1: AWS Account Setup (15 min)

- [ ] Created AWS account
- [ ] Verified email address
- [ ] Credit card added and verified
- [ ] Phone number verified
- [ ] **CRITICAL**: Enabled MFA on root account
- [ ] Created IAM user: `sensorvision-admin`
- [ ] Saved IAM user console login URL
- [ ] Created access keys for IAM user
- [ ] **SAVED**: Access key CSV file in safe location

**Saved Values:**
```
AWS Account ID: ________________
IAM Console URL: ________________
Access Key ID: ________________
Secret Access Key: (in CSV file)
```

---

## Part 2: Install Tools (20 min)

- [ ] Installed AWS CLI
- [ ] Verified: `aws --version` works
- [ ] Ran `aws configure` with access keys
- [ ] Verified: `aws sts get-caller-identity` works
- [ ] Installed Git (or verified existing)
- [ ] Installed Docker Desktop
- [ ] Verified: `docker --version` works

---

## Part 3: Create AWS Infrastructure (45 min)

### VPC & Networking
- [ ] Found default VPC ID
- [ ] Saved VPC ID: ________________

### Security Groups
- [ ] Found your public IP address
- [ ] Created EC2 security group
- [ ] Saved EC2 Security Group ID: ________________
- [ ] Added firewall rules to EC2 SG (SSH, HTTP, HTTPS, MQTT, 8080)
- [ ] Created RDS security group
- [ ] Saved RDS Security Group ID: ________________
- [ ] Added PostgreSQL rule to RDS SG

### Subnets
- [ ] Found two subnet IDs in different AZs
- [ ] Saved Subnet 1: ________________
- [ ] Saved Subnet 2: ________________
- [ ] Created RDS subnet group

### RDS Database
- [ ] Generated strong database password
- [ ] **SAVED** database password: ________________
- [ ] Created RDS instance
- [ ] Waited for RDS status: "available"
- [ ] Got RDS endpoint
- [ ] **SAVED** RDS endpoint: ________________

### ECR Repository
- [ ] Created ECR repository
- [ ] **SAVED** ECR URI: ________________

### IAM Role
- [ ] Created `ec2-trust-policy.json` file
- [ ] Created `ecr-policy.json` file
- [ ] Created IAM role: `sensorvision-ec2-role`
- [ ] Attached policy to role
- [ ] Created instance profile
- [ ] Added role to instance profile

### SSH Key
- [ ] Created SSH key pair
- [ ] **SAVED** `sensorvision-key.pem` file
- [ ] File location: ________________

### EC2 Instance
- [ ] Created `user-data.sh` file
- [ ] Launched EC2 instance
- [ ] Waited for instance to start
- [ ] Got EC2 public IP
- [ ] **SAVED** EC2 IP: ________________

---

## Part 4: Configure GitHub (20 min)

Added these GitHub Secrets:

- [ ] `AWS_ACCESS_KEY_ID`
- [ ] `AWS_SECRET_ACCESS_KEY`
- [ ] `AWS_REGION`
- [ ] `EC2_HOST`
- [ ] `EC2_USER`
- [ ] `EC2_SSH_PRIVATE_KEY`
- [ ] `DB_URL`
- [ ] `DB_USERNAME`
- [ ] `DB_PASSWORD`
- [ ] `JWT_SECRET` (generated new one)
- [ ] `JWT_EXPIRATION_MS`
- [ ] `JWT_ISSUER`
- [ ] `MQTT_USERNAME`
- [ ] `MQTT_PASSWORD`
- [ ] `PRODUCTION_DOMAIN`

**Optional secrets** (if using):
- [ ] `EMAIL_ENABLED` & email config
- [ ] `SMS_ENABLED` & Twilio config
- [ ] `SLACK_ENABLED` & webhook
- [ ] `TEAMS_ENABLED` & webhook

---

## Part 5: First Deployment (30 min)

### Local Build
- [ ] Opened Command Prompt
- [ ] Navigated to `C:\sensorvision`
- [ ] Logged into ECR
- [ ] Built Docker image (waited 5-10 min)
- [ ] Tagged image
- [ ] Pushed to ECR (waited 5-10 min)

### EC2 Setup
- [ ] Fixed SSH key permissions
- [ ] SSH'd to EC2 successfully
- [ ] Cloned/copied code to `/home/ubuntu/sensorvision`
- [ ] Created `.env.production` from template
- [ ] Updated all values in `.env.production`:
  - [ ] AWS_REGION
  - [ ] ECR_REGISTRY
  - [ ] DB_URL
  - [ ] DB_USERNAME
  - [ ] DB_PASSWORD
  - [ ] JWT_SECRET
  - [ ] JWT_ISSUER
  - [ ] MQTT credentials
- [ ] Set permissions: `chmod 600 .env.production`
- [ ] Logged into ECR from EC2
- [ ] Made deploy.sh executable
- [ ] Ran `./deploy.sh`
- [ ] Deployment completed successfully

### Testing
- [ ] Tested health endpoint from EC2: `curl http://localhost:8080/actuator/health`
- [ ] Tested from Windows: `curl http://EC2_IP:8080/actuator/health`
- [ ] Opened in browser: `http://EC2_IP:8080`
- [ ] Application loads successfully!

---

## Part 6: Domain & SSL (30 min) - OPTIONAL

- [ ] Logged into domain registrar
- [ ] Created A record pointing to EC2 IP
- [ ] Waited for DNS propagation (5-10 min)
- [ ] Verified DNS with `nslookup`
- [ ] SSH'd to EC2
- [ ] Updated `nginx.conf` with domain name
- [ ] Restarted nginx
- [ ] Ran certbot
- [ ] Entered email address
- [ ] Agreed to terms
- [ ] Chose to redirect HTTP to HTTPS
- [ ] Certificate installed successfully
- [ ] Tested HTTPS: `https://your-domain.com`
- [ ] Browser shows secure lock icon

---

## Part 7: Enable CI/CD (5 min)

- [ ] Committed all changes
- [ ] Pushed to GitHub: `git push origin main`
- [ ] Went to GitHub → Actions tab
- [ ] Saw "Deploy to Production" workflow running
- [ ] Workflow completed successfully
- [ ] Verified deployment worked

---

## Part 8: Final Verification

### Container Status
- [ ] SSH'd to EC2
- [ ] Ran: `docker-compose -f docker-compose.production.yml ps`
- [ ] All containers showing "Up"
  - [ ] sensorvision-backend
  - [ ] sensorvision-mosquitto
  - [ ] sensorvision-nginx

### Endpoint Testing
- [ ] Health check works: `curl https://your-domain.com/actuator/health`
- [ ] API responds: `curl https://your-domain.com/api/v1/devices`
- [ ] Frontend loads in browser
- [ ] MQTT connection works

### Monitoring Setup
- [ ] Checked CloudWatch logs exist
- [ ] Set up billing alarm
- [ ] (Optional) Created CloudWatch alarms for CPU, disk, etc.

---

## Post-Deployment Tasks

### Documentation
- [ ] Saved all important credentials in password manager:
  - [ ] AWS root password
  - [ ] AWS IAM user password
  - [ ] Database password
  - [ ] JWT secret
  - [ ] MQTT credentials
- [ ] Saved all AWS resource IDs:
  - [ ] VPC ID
  - [ ] Security Group IDs
  - [ ] RDS endpoint
  - [ ] EC2 instance ID
  - [ ] ECR repository URI
- [ ] Saved SSH key in safe location (backed up!)
- [ ] Documented custom configuration changes

### Security Hardening
- [ ] Reviewed security group rules
- [ ] Verified MFA is enabled
- [ ] Confirmed RDS is not publicly accessible
- [ ] Verified SSL certificate is valid
- [ ] Checked that `.env.production` is not in git
- [ ] Confirmed SSH key has proper permissions

### Monitoring & Alerts
- [ ] Set up AWS billing alerts
- [ ] Configured CloudWatch alarms (CPU, disk, memory)
- [ ] (Optional) Configured Slack/Teams notifications
- [ ] (Optional) Set up uptime monitoring (Pingdom, UptimeRobot)

### Backup Verification
- [ ] Verified RDS automated backups are enabled
- [ ] Confirmed backup retention period (7 days)
- [ ] Tested manual RDS snapshot creation
- [ ] Verified application data backup cron job (optional)

---

## Weekly Maintenance Checklist

- [ ] Check AWS billing dashboard
- [ ] Review CloudWatch logs for errors
- [ ] Verify RDS backups are running
- [ ] Check disk space on EC2
- [ ] Review application logs
- [ ] Update dependencies if needed

---

## Monthly Maintenance Checklist

- [ ] Review and optimize AWS costs
- [ ] Update system packages on EC2
- [ ] Rotate credentials (JWT secret, database password)
- [ ] Review CloudWatch metrics
- [ ] Test backup restoration
- [ ] Update SSL certificate (if not using auto-renewal)
- [ ] Review security group rules
- [ ] Check for AWS service updates

---

## Emergency Contacts & Resources

**AWS Support**: https://console.aws.amazon.com/support

**Important URLs**:
- AWS Console: https://console.aws.amazon.com/
- GitHub Repo: ________________
- Application URL: ________________
- CloudWatch Logs: https://console.aws.amazon.com/cloudwatch/

**Important Files**:
- SSH Key: ________________
- Access Keys CSV: ________________
- Passwords Document: ________________

**Documentation**:
- Beginner Guide: `DEPLOYMENT_WALKTHROUGH.md`
- Operations Guide: `DEPLOYMENT_OPERATIONS.md`
- Infrastructure Guide: `AWS_DEPLOYMENT.md`

---

## Troubleshooting Quick Reference

**Application won't start:**
1. Check logs: `docker-compose logs backend`
2. Verify .env.production
3. Check database connection
4. See DEPLOYMENT_OPERATIONS.md

**Can't SSH to EC2:**
1. Verify security group allows your IP
2. Check SSH key permissions
3. Verify EC2 instance is running

**Database connection failed:**
1. Check RDS endpoint in .env.production
2. Verify security groups
3. Test: `telnet RDS_ENDPOINT 5432`

**SSL certificate issues:**
1. Verify DNS points to EC2
2. Run: `sudo certbot renew`
3. Check nginx logs

**Deployment failed:**
1. Check GitHub Actions logs
2. SSH to EC2 and check deploy logs
3. Run: `./deploy.sh --rollback`

---

## Success Criteria ✅

You're done when all of these are true:

- [ ] Application loads at https://your-domain.com (or http://EC2_IP)
- [ ] Health endpoint returns `{"status":"UP"}`
- [ ] Can create an account and login
- [ ] GitHub Actions deploys automatically on push to main
- [ ] All containers are running on EC2
- [ ] Database is accessible and contains data
- [ ] MQTT broker is accepting connections
- [ ] SSL certificate is valid (if using domain)
- [ ] No errors in application logs
- [ ] AWS billing is under $100/month

---

**Congratulations! 🎉**

If all checkboxes are marked, your SensorVision application is successfully deployed to AWS!

**Next Steps:**
1. Share the URL with stakeholders
2. Monitor for issues over the next 24-48 hours
3. Set up proper monitoring and alerting
4. Plan for scaling if needed

**Remember:**
- Keep this checklist for future reference
- Document any custom changes you make
- Back up important credentials
- Monitor AWS costs regularly

Good luck with your production deployment! 🚀
