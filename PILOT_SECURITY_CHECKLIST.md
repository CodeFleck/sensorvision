# SensorVision Pilot Security Checklist

## Authentication & Authorization
- [ ] Enable HTTPS/TLS for all endpoints
- [ ] Configure JWT token expiration (currently 24h - consider reducing to 8h for pilot)
- [ ] Implement password complexity requirements
- [ ] Enable account lockout after failed login attempts
- [ ] Set up OAuth2 with Google (already configured)
- [ ] Review and rotate all default passwords

## Database Security
- [ ] Enable PostgreSQL SSL connections
- [ ] Configure database user with minimal required permissions
- [ ] Enable database audit logging
- [ ] Set up automated database backups with encryption
- [ ] Configure connection pooling limits

## Network Security
- [ ] Configure AWS Security Groups to allow only necessary ports
- [ ] Set up VPC with private subnets for database
- [ ] Enable AWS CloudTrail for API logging
- [ ] Configure rate limiting on API endpoints
- [ ] Set up DDoS protection (AWS Shield)

## Application Security
- [ ] Enable CSRF protection
- [ ] Configure CORS properly for production domains
- [ ] Set up input validation and sanitization
- [ ] Enable SQL injection protection
- [ ] Configure XSS protection headers
- [ ] Set up security headers (HSTS, CSP, etc.)

## Secrets Management
- [ ] Move all secrets to AWS Secrets Manager or Parameter Store
- [ ] Remove any hardcoded credentials
- [ ] Set up secret rotation policies
- [ ] Configure environment-specific secrets

## Monitoring & Alerting
- [ ] Set up security event monitoring
- [ ] Configure failed login attempt alerts
- [ ] Enable suspicious activity detection
- [ ] Set up log aggregation and analysis