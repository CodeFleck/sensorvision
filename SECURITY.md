# Security Policy

## Reporting Security Vulnerabilities

If you discover a security vulnerability in SensorVision, please report it responsibly:

1. **Do NOT** create a public GitHub issue for security vulnerabilities
2. Email the security concern to the project maintainers
3. Include detailed steps to reproduce the vulnerability
4. Allow reasonable time for the issue to be addressed before public disclosure

## Credential Management

### Never Commit Credentials

This project uses environment variables for all sensitive configuration. **Never commit actual credentials to the repository.**

#### Files That Should NEVER Contain Real Credentials
- `application-local.properties` (use `.example` template)
- `.env` files (use `.env.example` template)
- Any file not in `.gitignore`

#### Safe Templates Provided
| Template File | Purpose |
|---------------|---------|
| `.env.example` | Local development environment variables |
| `.env.production.template` | Production deployment template |
 | `src/main/resources/application-local.properties.example` | Spring Boot local profile |

### Setting Up Local Development

1. Copy template files:
   ```bash
   cp .env.example .env
   cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
   ```

2. Generate secure credentials:
   ```bash
   # Database password (32 chars, base64)
   node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"

   # JWT secret (128 chars, hex)
   node -e "console.log(require('crypto').randomBytes(64).toString('hex'))"
   ```

3. Update your local files with the generated credentials

4. **Verify files are gitignored:**
   ```bash
   git check-ignore .env
   git check-ignore src/main/resources/application-local.properties
   ```

### Production Deployment

Production deployments should:

1. **Use environment variables** - Never hardcode credentials
2. **Use secrets management** - AWS Secrets Manager, HashiCorp Vault, etc.
3. **Rotate credentials regularly** - Recommended every 90 days
4. **Use least privilege** - Each service gets only required permissions

#### Required Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `DB_PASSWORD` | PostgreSQL password | (secure random string) |
| `JWT_SECRET` | JWT signing secret | (64+ byte hex string) |
| `MQTT_PASSWORD` | MQTT broker password | (secure random string) |

#### Optional Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `EMAIL_ENABLED` | Enable email notifications | `false` |
| `AWS_ACCESS_KEY_ID` | AWS credentials for SES | - |
| `AWS_SECRET_ACCESS_KEY` | AWS credentials for SES | - |

## Security Controls

### Automated Secret Scanning

This repository uses multiple layers of secret detection:

1. **TruffleHog** - Scans for verified secrets in commits
2. **Gitleaks** - Additional secret pattern detection
3. **Custom patterns** - Project-specific credential patterns

See `.github/workflows/secret-scan.yml` for configuration.

### Pre-Commit Hooks (Recommended)

Install a local pre-commit hook to prevent accidental credential commits:

```bash
# Create pre-commit hook
cat > .git/hooks/pre-commit << 'EOF'
#!/bin/bash
# Prevent committing files with potential secrets

BLOCKED_PATTERNS=(
    "password.*=.*['\"][A-Za-z0-9+/=]{16,}"
    "secret.*=.*['\"][A-Za-z0-9+/=]{16,}"
    "AKIA[0-9A-Z]{16}"
)

for pattern in "${BLOCKED_PATTERNS[@]}"; do
    if git diff --cached --name-only | xargs grep -lEi "$pattern" 2>/dev/null; then
        echo "ERROR: Potential secret detected. Please review staged changes."
        exit 1
    fi
done
EOF

chmod +x .git/hooks/pre-commit
```

## Credential Rotation Schedule

| Credential | Rotation Frequency | Notes |
|------------|-------------------|-------|
| Database password | 90 days | Coordinate with backup schedule |
| JWT secret | 90 days | Invalidates all sessions |
| MQTT password | 90 days | Update all connected devices |
| API tokens | 30 days | Device tokens in production |

## Security Audit History

| Date | Type | Findings | Status |
|------|------|----------|--------|
| 2025-12-14 | Credential Audit | Default passwords in docker-compose.yml | Remediated |
| 2025-12-14 | Credential Audit | Added secret scanning CI/CD | Implemented |

## Dependencies

### Security Updates

- Monitor dependencies for security vulnerabilities
- Use `npm audit` for frontend dependencies
- Use Dependabot or similar for automated updates
- Review Spring Security advisories regularly

### Known Considerations

- MQTT anonymous access is enabled for local development only
- Production deployments must set `MQTT_DEVICE_AUTH_REQUIRED=true`
- JWT tokens expire after 24 hours by default

## Contact

For security-related inquiries, contact the project maintainers through the repository's security advisory feature or private channels.