#!/bin/bash
# EC2 Disk Cleanup Script for Industrial Cloud
# Run this script on the EC2 instance to free up disk space before deployment

set -e

echo "======================================"
echo "Industrial Cloud EC2 Disk Cleanup"
echo "======================================"

# Show current disk usage
echo ""
echo "Current disk usage:"
df -h /

echo ""
echo "Starting cleanup..."

# 1. Remove old Docker images (keep only currently used ones)
echo ""
echo "[1/6] Cleaning up unused Docker images..."
docker image prune -af
echo "Docker images cleaned!"

# 2. Remove unused Docker volumes
echo ""
echo "[2/6] Cleaning up unused Docker volumes..."
docker volume prune -f
echo "Docker volumes cleaned!"

# 3. Remove unused Docker networks
echo ""
echo "[3/6] Cleaning up unused Docker networks..."
docker network prune -f
echo "Docker networks cleaned!"

# 4. Remove Docker build cache
echo ""
echo "[4/6] Cleaning up Docker build cache..."
docker builder prune -af
echo "Docker build cache cleaned!"

# 5. Remove old backups (keep only last 3)
echo ""
echo "[5/6] Cleaning up old backups..."
BACKUP_DIR="$HOME/indcloud/backups"
if [ -d "$BACKUP_DIR" ]; then
    cd "$BACKUP_DIR"
    # List backups sorted by date, keep last 3
    BACKUP_COUNT=$(ls -d backup_* 2>/dev/null | wc -l)
    if [ "$BACKUP_COUNT" -gt 3 ]; then
        echo "Found $BACKUP_COUNT backups, removing oldest ones (keeping last 3)..."
        ls -dt backup_* | tail -n +4 | xargs rm -rf
        echo "Old backups removed!"
    else
        echo "Only $BACKUP_COUNT backups found, nothing to remove."
    fi
else
    echo "Backup directory not found, skipping."
fi

# 6. Clean up system logs (truncate large logs)
echo ""
echo "[6/6] Cleaning up system logs..."
sudo journalctl --vacuum-size=100M 2>/dev/null || echo "journalctl cleanup skipped"
sudo truncate -s 0 /var/log/*.log 2>/dev/null || echo "Log truncation skipped (may need sudo)"

# Final disk usage
echo ""
echo "======================================"
echo "Cleanup complete!"
echo "======================================"
echo ""
echo "Final disk usage:"
df -h /

echo ""
echo "You can now re-run the deployment."
