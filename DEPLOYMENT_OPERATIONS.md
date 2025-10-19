# SensorVision Deployment Operations Guide

This guide covers common operational tasks for managing your SensorVision production deployment on AWS.

---

## Table of Contents

1. [Daily Operations](#daily-operations)
2. [Monitoring](#monitoring)
3. [Deployments](#deployments)
4. [Database Operations](#database-operations)
5. [Troubleshooting](#troubleshooting)
6. [Backup and Recovery](#backup-and-recovery)
7. [Security](#security)
8. [Cost Management](#cost-management)

---

## Daily Operations

### Check Application Health

```bash
# Via HTTP endpoint
curl https://your-domain.com/actuator/health

# Expected output:
# {"status":"UP","components":{"db":{"status":"UP"},"diskSpace":{"status":"UP"}}}

# SSH to EC2 and check containers
ssh -i sensorvision-key.pem ubuntu@your-ec2-ip
cd /home/ubuntu/sensorvision
docker-compose -f docker-compose.production.yml ps
```

### View Application Logs

```bash
# SSH to EC2
ssh -i sensorvision-key.pem ubuntu@your-ec2-ip
cd /home/ubuntu/sensorvision

# View all logs
docker-compose -f docker-compose.production.yml logs -f

# View specific service logs
docker-compose -f docker-compose.production.yml logs -f backend
docker-compose -f docker-compose.production.yml logs -f mosquitto
docker-compose -f docker-compose.production.yml logs -f nginx

# View last 100 lines
docker-compose -f docker-compose.production.yml logs --tail=100 backend

# Search logs for errors
docker-compose -f docker-compose.production.yml logs backend | grep ERROR
```

### Restart Services

```bash
# SSH to EC2
ssh -i sensorvision-key.pem ubuntu@your-ec2-ip
cd /home/ubuntu/sensorvision

# Restart all services
docker-compose -f docker-compose.production.yml restart

# Restart specific service
docker-compose -f docker-compose.production.yml restart backend
docker-compose -f docker-compose.production.yml restart mosquitto

# Stop and start (full restart)
docker-compose -f docker-compose.production.yml down
docker-compose -f docker-compose.production.yml up -d
```

---

## Monitoring

### CloudWatch Metrics

```bash
# View CPU utilization
aws cloudwatch get-metric-statistics \
  --namespace AWS/EC2 \
  --metric-name CPUUtilization \
  --dimensions Name=InstanceId,Value=i-xxxxx \
  --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 \
  --statistics Average

# View RDS connections
aws cloudwatch get-metric-statistics \
  --namespace AWS/RDS \
  --metric-name DatabaseConnections \
  --dimensions Name=DBInstanceIdentifier,Value=sensorvision-db \
  --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 \
  --statistics Average
```

### Application Metrics

```bash
# Access Prometheus metrics
curl https://your-domain.com/actuator/prometheus

# View JVM metrics
curl https://your-domain.com/actuator/metrics/jvm.memory.used

# View HTTP request metrics
curl https://your-domain.com/actuator/metrics/http.server.requests
```

### Set Up CloudWatch Alarms

```bash
# High CPU alarm
aws cloudwatch put-metric-alarm \
  --alarm-name sensorvision-high-cpu \
  --alarm-description "CPU above 80%" \
  --metric-name CPUUtilization \
  --namespace AWS/EC2 \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --dimensions Name=InstanceId,Value=i-xxxxx \
  --evaluation-periods 2 \
  --alarm-actions arn:aws:sns:us-east-1:123456789:sensorvision-alerts

# Disk space alarm
aws cloudwatch put-metric-alarm \
  --alarm-name sensorvision-low-disk \
  --alarm-description "Disk space below 20%" \
  --metric-name disk_used_percent \
  --namespace CWAgent \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2 \
  --alarm-actions arn:aws:sns:us-east-1:123456789:sensorvision-alerts

# RDS storage alarm
aws cloudwatch put-metric-alarm \
  --alarm-name sensorvision-rds-storage \
  --alarm-description "RDS storage below 2GB" \
  --metric-name FreeStorageSpace \
  --namespace AWS/RDS \
  --statistic Average \
  --period 300 \
  --threshold 2000000000 \
  --comparison-operator LessThanThreshold \
  --dimensions Name=DBInstanceIdentifier,Value=sensorvision-db \
  --evaluation-periods 2 \
  --alarm-actions arn:aws:sns:us-east-1:123456789:sensorvision-alerts
```

---

## Deployments

### Manual Deployment

```bash
# SSH to EC2
ssh -i sensorvision-key.pem ubuntu@your-ec2-ip
cd /home/ubuntu/sensorvision

# Pull latest code (if using git on EC2)
git pull origin main

# Or copy files from local machine
# scp -i sensorvision-key.pem -r ./your-files ubuntu@your-ec2-ip:/home/ubuntu/sensorvision/

# Run deployment script
./deploy.sh

# Monitor deployment
docker-compose -f docker-compose.production.yml logs -f backend
```

### GitHub Actions Deployment

```bash
# Push to main branch triggers automatic deployment
git add .
git commit -m "Update feature X"
git push origin main

# Monitor deployment at:
# https://github.com/your-username/sensorvision/actions

# Manual trigger via GitHub UI:
# Go to Actions → Deploy to Production → Run workflow
```

### Rollback to Previous Version

```bash
# SSH to EC2
ssh -i sensorvision-key.pem ubuntu@your-ec2-ip
cd /home/ubuntu/sensorvision

# Run deployment script with rollback flag
./deploy.sh --rollback

# Or manually rollback to specific image tag
# Update .env.production with specific IMAGE_TAG
nano .env.production  # Set IMAGE_TAG=<previous-commit-sha>

# Pull and restart
docker-compose -f docker-compose.production.yml pull
docker-compose -f docker-compose.production.yml up -d
```

### Update Environment Variables

```bash
# SSH to EC2
ssh -i sensorvision-key.pem ubuntu@your-ec2-ip
cd /home/ubuntu/sensorvision

# Edit .env.production
nano .env.production

# Restart services to apply changes
docker-compose -f docker-compose.production.yml restart

# For some changes (like database URL), you may need a full restart
docker-compose -f docker-compose.production.yml down
docker-compose -f docker-compose.production.yml up -d
```

---

## Database Operations

### Connect to RDS Database

```bash
# From your local machine (ensure security group allows your IP)
psql -h sensorvision-db.xxxxx.us-east-1.rds.amazonaws.com \
     -U sensorvision_admin \
     -d sensorvision

# Or from EC2 instance
ssh -i sensorvision-key.pem ubuntu@your-ec2-ip
docker-compose -f docker-compose.production.yml exec backend bash
apt-get update && apt-get install -y postgresql-client
psql -h $DB_HOST -U $DB_USERNAME -d sensorvision
```

### Run Database Queries

```sql
-- Check database size
SELECT pg_size_pretty(pg_database_size('sensorvision'));

-- Check table sizes
SELECT schemaname, tablename,
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Check active connections
SELECT count(*) FROM pg_stat_activity;

-- Check recent telemetry records
SELECT * FROM telemetry_records ORDER BY timestamp DESC LIMIT 10;

-- Check Flyway migration history
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

### Database Backup

```bash
# Create RDS snapshot
aws rds create-db-snapshot \
  --db-instance-identifier sensorvision-db \
  --db-snapshot-identifier sensorvision-manual-backup-$(date +%Y%m%d-%H%M%S)

# List snapshots
aws rds describe-db-snapshots \
  --db-instance-identifier sensorvision-db

# Export data using pg_dump (from EC2)
ssh -i sensorvision-key.pem ubuntu@your-ec2-ip
docker run --rm postgres:15-alpine pg_dump \
  -h sensorvision-db.xxxxx.us-east-1.rds.amazonaws.com \
  -U sensorvision_admin \
  -d sensorvision \
  -F c \
  -f /tmp/backup-$(date +%Y%m%d).dump
```

### Database Restore

```bash
# Restore from RDS snapshot
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier sensorvision-db-restored \
  --db-snapshot-identifier sensorvision-manual-backup-20240101-120000

# Restore from pg_dump file
docker run --rm -v /tmp:/backup postgres:15-alpine pg_restore \
  -h sensorvision-db.xxxxx.us-east-1.rds.amazonaws.com \
  -U sensorvision_admin \
  -d sensorvision \
  -c \
  /backup/backup-20240101.dump
```

### Database Maintenance

```sql
-- Vacuum analyze (reclaim space and update statistics)
VACUUM ANALYZE;

-- Reindex (if performance degrades)
REINDEX DATABASE sensorvision;

-- Check for long-running queries
SELECT pid, now() - query_start as duration, query
FROM pg_stat_activity
WHERE state = 'active'
ORDER BY duration DESC;

-- Kill long-running query
SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE pid = <pid>;
```

---

## Troubleshooting

### Application Won't Start

```bash
# Check logs
docker-compose -f docker-compose.production.yml logs backend

# Common issues:
# 1. Database connection failed
#    - Check RDS endpoint in .env.production
#    - Verify security group allows EC2 to connect to RDS
#    - Test: telnet <rds-endpoint> 5432

# 2. Missing environment variables
#    - Check .env.production has all required vars
#    - Compare with .env.production.template

# 3. Flyway migration failed
#    - Check migration files in src/main/resources/db/migration
#    - Manually connect to DB and check flyway_schema_history table

# Check container status
docker-compose -f docker-compose.production.yml ps

# Inspect specific container
docker inspect sensorvision-backend

# Execute command in container
docker-compose -f docker-compose.production.yml exec backend bash
```

### High CPU Usage

```bash
# Check which process is consuming CPU
ssh -i sensorvision-key.pem ubuntu@your-ec2-ip
top

# Check Docker container stats
docker stats

# Check application threads (inside container)
docker-compose -f docker-compose.production.yml exec backend bash
jstack 1  # PID 1 is typically the main process in container

# Scale EC2 instance if needed
# Stop instance, change instance type to t3.large, restart
aws ec2 stop-instances --instance-ids i-xxxxx
aws ec2 modify-instance-attribute --instance-id i-xxxxx --instance-type t3.large
aws ec2 start-instances --instance-ids i-xxxxx
```

### High Memory Usage

```bash
# Check memory usage
ssh -i sensorvision-key.pem ubuntu@your-ec2-ip
free -h

# Check Docker container memory
docker stats --no-stream

# Check JVM memory (inside container)
docker-compose -f docker-compose.production.yml exec backend bash
jmap -heap 1

# Adjust JVM heap size in docker-compose.production.yml
# Add to backend service environment:
# JAVA_OPTS: "-Xmx1g -Xms512m"
```

### Disk Space Full

```bash
# Check disk usage
ssh -i sensorvision-key.pem ubuntu@your-ec2-ip
df -h

# Find large files
du -sh /* | sort -h
du -sh /home/ubuntu/sensorvision/* | sort -h

# Clean up Docker
docker system prune -a --volumes  # WARNING: Removes unused images/volumes
docker image prune -a  # Remove unused images only

# Clean up old logs
cd /home/ubuntu/sensorvision/logs
find . -name "*.log" -mtime +30 -delete  # Delete logs older than 30 days

# Increase EBS volume size
aws ec2 modify-volume --volume-id vol-xxxxx --size 50
# Then resize filesystem on EC2:
sudo growpart /dev/nvme0n1 1
sudo resize2fs /dev/nvme0n1p1
```

### SSL Certificate Issues

```bash
# Check certificate expiry
ssh -i sensorvision-key.pem ubuntu@your-ec2-ip
sudo certbot certificates

# Renew certificate manually
sudo certbot renew

# Test renewal (dry run)
sudo certbot renew --dry-run

# Check nginx configuration
sudo nginx -t

# Reload nginx
docker-compose -f docker-compose.production.yml restart nginx
```

### MQTT Connection Issues

```bash
# Test MQTT broker locally
ssh -i sensorvision-key.pem ubuntu@your-ec2-ip
mosquitto_sub -h localhost -p 1883 -t '#' -v

# Test from remote
mosquitto_sub -h your-domain.com -p 1883 -t '#' -v

# Check mosquitto logs
docker-compose -f docker-compose.production.yml logs mosquitto

# Check mosquitto configuration
docker-compose -f docker-compose.production.yml exec mosquitto cat /mosquitto/config/mosquitto.conf
```

---

## Backup and Recovery

### Automated Backup Strategy

1. **RDS Automated Backups** (already configured)
   - Retention: 7 days
   - Backup window: 03:00-04:00 UTC
   - Point-in-time recovery available

2. **Application Data Backup**
```bash
# Create backup script
cat > /home/ubuntu/backup.sh << 'EOF'
#!/bin/bash
BACKUP_DIR="/home/ubuntu/backups"
DATE=$(date +%Y%m%d-%H%M%S)

mkdir -p $BACKUP_DIR

# Backup environment file
cp /home/ubuntu/sensorvision/.env.production $BACKUP_DIR/.env.production.$DATE

# Backup docker-compose file
cp /home/ubuntu/sensorvision/docker-compose.production.yml $BACKUP_DIR/docker-compose.$DATE.yml

# Backup MQTT data
tar -czf $BACKUP_DIR/mosquitto-data.$DATE.tar.gz /home/ubuntu/sensorvision/mosquitto/data

# Keep only last 30 days
find $BACKUP_DIR -mtime +30 -delete
EOF

chmod +x /home/ubuntu/backup.sh

# Add to crontab (daily at 2 AM)
crontab -e
# Add: 0 2 * * * /home/ubuntu/backup.sh
```

### Disaster Recovery Procedure

1. **Complete System Failure**
```bash
# 1. Restore RDS from snapshot
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier sensorvision-db-restored \
  --db-snapshot-identifier <latest-snapshot-id>

# 2. Launch new EC2 instance using same user data script

# 3. Update security groups to allow new EC2 to connect to RDS

# 4. Deploy application using GitHub Actions or manual deploy script

# 5. Update Route 53 DNS to point to new EC2 IP
```

2. **Database Corruption**
```bash
# 1. Stop application
docker-compose -f docker-compose.production.yml stop backend

# 2. Restore from RDS snapshot to new instance

# 3. Update DB_URL in .env.production

# 4. Restart application
docker-compose -f docker-compose.production.yml start backend
```

---

## Security

### Rotate Credentials

```bash
# 1. Generate new JWT secret
openssl rand -hex 64

# 2. Update GitHub Secrets
# Go to repository Settings → Secrets → Update JWT_SECRET

# 3. Update .env.production on EC2
ssh -i sensorvision-key.pem ubuntu@your-ec2-ip
nano /home/ubuntu/sensorvision/.env.production
# Update JWT_SECRET

# 4. Restart application
docker-compose -f docker-compose.production.yml restart backend

# 5. Rotate database password
aws rds modify-db-instance \
  --db-instance-identifier sensorvision-db \
  --master-user-password 'NEW_STRONG_PASSWORD' \
  --apply-immediately

# Update .env.production with new password and restart

# 6. Rotate MQTT credentials
# Update .env.production MQTT_USERNAME and MQTT_PASSWORD
# Restart mosquitto container
```

### Security Audit

```bash
# Check for security updates
ssh -i sensorvision-key.pem ubuntu@your-ec2-ip
sudo apt-get update
sudo apt-get upgrade

# Check Docker images for vulnerabilities
docker scan sensorvision-backend:latest

# Review security group rules
aws ec2 describe-security-groups --group-ids sg-xxxxx

# Review IAM policies
aws iam list-attached-role-policies --role-name sensorvision-ec2-role

# Check CloudTrail logs
aws cloudtrail lookup-events --max-results 50

# Review RDS security
aws rds describe-db-instances --db-instance-identifier sensorvision-db \
  --query 'DBInstances[0].{Encrypted:StorageEncrypted,PubliclyAccessible:PubliclyAccessible}'
```

---

## Cost Management

### Monitor Costs

```bash
# Get current month costs
aws ce get-cost-and-usage \
  --time-period Start=$(date -d "$(date +%Y-%m-01)" +%Y-%m-%d),End=$(date +%Y-%m-%d) \
  --granularity MONTHLY \
  --metrics BlendedCost \
  --group-by Type=SERVICE

# Set billing alert
aws cloudwatch put-metric-alarm \
  --alarm-name sensorvision-billing-alert \
  --alarm-description "Alert when estimated charges exceed $100" \
  --metric-name EstimatedCharges \
  --namespace AWS/Billing \
  --statistic Maximum \
  --period 21600 \
  --threshold 100 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 1
```

### Cost Optimization

```bash
# 1. Stop EC2 during non-business hours (dev/test only)
# Create stop schedule
aws ec2 stop-instances --instance-ids i-xxxxx

# Create start schedule
aws ec2 start-instances --instance-ids i-xxxxx

# 2. Use Savings Plans or Reserved Instances
# Purchase from AWS Console for 1-3 year commitment

# 3. Delete old RDS snapshots
aws rds describe-db-snapshots --db-instance-identifier sensorvision-db
aws rds delete-db-snapshot --db-snapshot-identifier old-snapshot-id

# 4. Delete old ECR images
aws ecr list-images --repository-name sensorvision-backend
aws ecr batch-delete-image --repository-name sensorvision-backend --image-ids imageTag=old-tag

# 5. Set CloudWatch log retention
aws logs put-retention-policy \
  --log-group-name /aws/ec2/sensorvision \
  --retention-in-days 7
```

---

## Maintenance Windows

### Planned Maintenance Checklist

- [ ] Notify users of maintenance window
- [ ] Create backup/snapshot before changes
- [ ] Test changes in staging environment (if available)
- [ ] Have rollback plan ready
- [ ] Monitor during and after maintenance
- [ ] Document changes made

### Example Maintenance Tasks

```bash
# Update Docker images
ssh -i sensorvision-key.pem ubuntu@your-ec2-ip
cd /home/ubuntu/sensorvision
docker-compose -f docker-compose.production.yml pull
docker-compose -f docker-compose.production.yml up -d

# Update system packages
sudo apt-get update
sudo apt-get upgrade -y
sudo reboot  # Schedule during off-hours

# RDS minor version upgrade
aws rds modify-db-instance \
  --db-instance-identifier sensorvision-db \
  --engine-version 15.5 \
  --apply-immediately

# Scale RDS instance type
aws rds modify-db-instance \
  --db-instance-identifier sensorvision-db \
  --db-instance-class db.t4g.small \
  --apply-immediately
```

---

## Support Contacts

- **AWS Support**: https://console.aws.amazon.com/support
- **GitHub Issues**: https://github.com/your-username/sensorvision/issues
- **On-call Engineer**: [Your contact info]
- **Database Admin**: [Your contact info]

---

## Quick Reference

### Essential Commands

```bash
# SSH to EC2
ssh -i sensorvision-key.pem ubuntu@EC2_IP

# View logs
docker-compose -f docker-compose.production.yml logs -f backend

# Restart application
docker-compose -f docker-compose.production.yml restart backend

# Check health
curl https://your-domain.com/actuator/health

# Deploy latest version
./deploy.sh

# Rollback
./deploy.sh --rollback
```

### Important File Locations

- Application: `/home/ubuntu/sensorvision/`
- Logs: `/home/ubuntu/sensorvision/logs/`
- Backups: `/home/ubuntu/backups/`
- Environment: `/home/ubuntu/sensorvision/.env.production`
- SSL Certificates: `/etc/letsencrypt/`

### Emergency Procedures

1. **Application Down**: Check logs → Restart services → Check RDS connection
2. **Database Down**: Check RDS status → Restore from snapshot if needed
3. **SSL Expired**: Run `sudo certbot renew`
4. **Disk Full**: Clean Docker → Delete old logs → Increase EBS volume
5. **High Load**: Check metrics → Scale instance → Add caching

---

**Remember**: Always test in a non-production environment first when possible. Keep this guide updated as your deployment evolves.
