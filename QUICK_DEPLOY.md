# Quick Deploy Guide

## ✅ PowerShell Script Fixed

All syntax errors have been resolved. The script is ready to use.

## How to Run

### Option 1: Default SSH Key

If your SSH key is at `~/.ssh/id_rsa`:

```powershell
cd C:\sensorvision
.\deploy-production.ps1
```

### Option 2: Custom SSH Key Path

If you have a `.pem` file elsewhere:

```powershell
cd C:\sensorvision
.\deploy-production.ps1 -SSHKeyPath "C:\path\to\your-key.pem"
```

### Example with PEM File

```powershell
# If your key is in Downloads folder
.\deploy-production.ps1 -SSHKeyPath "$env:USERPROFILE\Downloads\sensorvision-prod.pem"

# Or specify full path
.\deploy-production.ps1 -SSHKeyPath "C:\Users\YourName\AWS\ec2-keypair.pem"
```

## Execution Policy Fix

If you get "cannot be loaded because running scripts is disabled":

```powershell
# Allow scripts for current session only
Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process

# Then run the script
.\deploy-production.ps1 -SSHKeyPath "path\to\key.pem"
```

## Common SSH Key Locations

Windows SSH keys are usually in one of these locations:

```
%USERPROFILE%\.ssh\id_rsa           (Default location)
%USERPROFILE%\Downloads\*.pem       (AWS downloaded keys)
C:\Users\YourName\.ssh\id_rsa
C:\AWS\keys\*.pem
```

## Test SSH Access First

Before running the deployment script, test your SSH access:

```powershell
# Replace with your actual key path
ssh -i "$env:USERPROFILE\.ssh\id_rsa" ec2-user@35.88.65.186

# If successful, you'll see a prompt like:
# [ec2-user@ip-172-31-x-x ~]$

# Type 'exit' to disconnect
```

## What Happens During Deployment

The script will:

1. ✅ Test SSH connection (5 seconds)
2. ✅ Check current git status
3. ✅ Pull latest code from main
4. ✅ Run deployment script (~3-5 minutes)
5. ✅ Check Docker containers
6. ✅ Wait for backend health check (~1-2 minutes)
7. ✅ Test external access
8. ✅ Show summary with URLs

**Total Time: ~5-8 minutes**

## After Successful Deployment

You'll see:

```
========================================
  Deployment Complete!
========================================

Backend URL:           http://35.88.65.186:8080
Frontend URL:          http://35.88.65.186:3000
Integration Wizard:    http://35.88.65.186:3000/integration-wizard
Health Check:          http://35.88.65.186:8080/actuator/health
```

Open the Integration Wizard URL in your browser to test!

## Troubleshooting

### Permission Denied (publickey)

**Fix PEM file permissions:**

```powershell
# Right-click file → Properties → Security → Advanced
# Remove all inherited permissions
# Add only your user account with Read permission

# Or use PowerShell:
$path = "C:\path\to\key.pem"
icacls $path /inheritance:r
icacls $path /grant:r "$($env:USERNAME):(R)"
```

### Script Execution Blocked

```powershell
# Temporary fix (current session only):
Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process

# Permanent fix (requires admin):
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### Health Check Timeout

If the health check times out but deployment completes:

1. Wait 2-3 more minutes
2. Check manually: `curl http://35.88.65.186:8080/actuator/health`
3. View logs: See troubleshooting commands in script output

### Port 8080 Not Accessible

Check AWS Security Group allows inbound traffic on port 8080:

```powershell
# Using AWS CLI
aws ec2 describe-security-groups --group-ids sg-0255cea554e401228 --region us-west-2
```

## Need Help?

1. **Check logs:** The script shows troubleshooting commands at the end
2. **Manual deployment:** See `DEPLOYMENT_GUIDE.md` for manual steps
3. **Batch file:** Try `deploy-production.bat` as alternative

---

**Ready?** Run the PowerShell script now:

```powershell
cd C:\sensorvision
.\deploy-production.ps1 -SSHKeyPath "your\key\path.pem"
```
