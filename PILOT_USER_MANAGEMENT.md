# SensorVision Pilot User Management

## Pilot Program Structure

### User Tiers
1. **Pilot Administrators** (2-3 users)
   - Full system access
   - User management capabilities
   - System configuration access
   - Support ticket management

2. **Pilot Organizations** (5-10 organizations)
   - Isolated tenant environments
   - Organization-specific data and devices
   - Custom branding (if needed)
   - Usage analytics and reporting

3. **End Users** (20-50 users per organization)
   - Dashboard access
   - Device management
   - Alert configuration
   - Basic reporting

## User Onboarding Process

### 1. Organization Setup Script
```sql
-- Create pilot organizations
INSERT INTO organizations (id, name, domain, created_at, updated_at) VALUES
('pilot-org-001', 'Manufacturing Corp', 'manufacturing.pilot', NOW(), NOW()),
('pilot-org-002', 'Smart Building Solutions', 'smartbuilding.pilot', NOW(), NOW()),
('pilot-org-003', 'Energy Management Inc', 'energy.pilot', NOW(), NOW()),
('pilot-org-004', 'Agricultural IoT', 'agri.pilot', NOW(), NOW()),
('pilot-org-005', 'Healthcare Monitoring', 'health.pilot', NOW(), NOW());

-- Create admin users for each organization
INSERT INTO users (id, username, email, password_hash, first_name, last_name, organization_id, role, created_at, updated_at) VALUES
('admin-001', 'admin.manufacturing', 'admin@manufacturing.pilot', '$2a$10$...', 'John', 'Smith', 'pilot-org-001', 'ADMIN', NOW(), NOW()),
('admin-002', 'admin.smartbuilding', 'admin@smartbuilding.pilot', '$2a$10$...', 'Jane', 'Doe', 'pilot-org-002', 'ADMIN', NOW(), NOW()),
('admin-003', 'admin.energy', 'admin@energy.pilot', '$2a$10$...', 'Mike', 'Johnson', 'pilot-org-003', 'ADMIN', NOW(), NOW()),
('admin-004', 'admin.agri', 'admin@agri.pilot', '$2a$10$...', 'Sarah', 'Wilson', 'pilot-org-004', 'ADMIN', NOW(), NOW()),
('admin-005', 'admin.health', 'admin@health.pilot', '$2a$10$...', 'David', 'Brown', 'pilot-org-005', 'ADMIN', NOW(), NOW());
```

### 2. Welcome Email Template
```html
<!DOCTYPE html>
<html>
<head>
    <title>Welcome to SensorVision Pilot Program</title>
</head>
<body>
    <h1>Welcome to the SensorVision Pilot Program!</h1>
    
    <p>Dear {{firstName}},</p>
    
    <p>You've been invited to participate in the SensorVision IoT Platform pilot program. Your organization <strong>{{organizationName}}</strong> has been set up with the following details:</p>
    
    <div style="background: #f5f5f5; padding: 20px; margin: 20px 0;">
        <h3>Your Account Details</h3>
        <ul>
            <li><strong>Platform URL:</strong> https://pilot.sensorvision.io</li>
            <li><strong>Username:</strong> {{username}}</li>
            <li><strong>Organization:</strong> {{organizationName}}</li>
            <li><strong>Role:</strong> {{role}}</li>
        </ul>
    </div>
    
    <h3>Getting Started</h3>
    <ol>
        <li><a href="https://pilot.sensorvision.io/login">Login to your account</a></li>
        <li>Complete the <a href="https://pilot.sensorvision.io/integration-wizard">Integration Wizard</a></li>
        <li>Connect your first device</li>
        <li>Explore the dashboard and analytics</li>
    </ol>
    
    <h3>Pilot Program Resources</h3>
    <ul>
        <li><a href="https://docs.sensorvision.io/pilot">Pilot Program Guide</a></li>
        <li><a href="https://docs.sensorvision.io/quick-start">Quick Start Tutorial</a></li>
        <li><a href="https://docs.sensorvision.io/api">API Documentation</a></li>
        <li><a href="mailto:pilot-support@sensorvision.io">Support Email</a></li>
    </ul>
    
    <p>If you have any questions, please don't hesitate to reach out to our pilot support team.</p>
    
    <p>Best regards,<br>The SensorVision Team</p>
</body>
</html>
```

## Usage Limits and Quotas

### 1. Pilot Quotas Configuration
```yaml
# Pilot program limits
pilot:
  quotas:
    devices_per_organization: 100
    users_per_organization: 50
    api_calls_per_day: 100000
    telemetry_points_per_day: 1000000
    dashboards_per_organization: 20
    rules_per_organization: 50
    alerts_per_day: 1000
    data_retention_days: 90
    
  features:
    enabled:
      - basic_dashboards
      - device_management
      - real_time_monitoring
      - basic_analytics
      - email_notifications
      - sms_notifications
      - plugin_marketplace
      - integration_wizard
    
    disabled:
      - white_labeling
      - advanced_ml
      - custom_branding
      - api_rate_limiting_bypass
```

### 2. Quota Enforcement Service
```java
@Service
public class PilotQuotaService {
    
    public void validateDeviceCreation(String organizationId) {
        long deviceCount = deviceRepository.countByOrganizationId(organizationId);
        if (deviceCount >= pilotConfig.getMaxDevicesPerOrganization()) {
            throw new QuotaExceededException("Device limit reached for pilot organization");
        }
    }
    
    public void validateApiCall(String organizationId) {
        String key = "api_calls:" + organizationId + ":" + LocalDate.now();
        long callCount = redisTemplate.opsForValue().increment(key);
        
        if (callCount == 1) {
            redisTemplate.expire(key, Duration.ofDays(1));
        }
        
        if (callCount > pilotConfig.getMaxApiCallsPerDay()) {
            throw new QuotaExceededException("Daily API call limit exceeded");
        }
    }
}
```

## Pilot Feedback Collection

### 1. Feedback Form Integration
```typescript
// Feedback widget component
export const PilotFeedbackWidget: React.FC = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [feedback, setFeedback] = useState({
    rating: 0,
    category: '',
    message: '',
    email: ''
  });

  const submitFeedback = async () => {
    await api.post('/api/v1/pilot/feedback', feedback);
    setIsOpen(false);
    toast.success('Thank you for your feedback!');
  };

  return (
    <div className="fixed bottom-4 right-4 z-50">
      <button
        onClick={() => setIsOpen(true)}
        className="bg-blue-600 text-white px-4 py-2 rounded-lg shadow-lg hover:bg-blue-700"
      >
        💬 Pilot Feedback
      </button>
      
      {isOpen && (
        <FeedbackModal
          feedback={feedback}
          onChange={setFeedback}
          onSubmit={submitFeedback}
          onClose={() => setIsOpen(false)}
        />
      )}
    </div>
  );
};
```

### 2. Usage Analytics Dashboard
```sql
-- Pilot analytics queries
CREATE VIEW pilot_usage_analytics AS
SELECT 
    o.name as organization_name,
    COUNT(DISTINCT d.id) as device_count,
    COUNT(DISTINCT u.id) as user_count,
    COUNT(DISTINCT tr.id) as telemetry_points_today,
    COUNT(DISTINCT a.id) as alerts_today,
    AVG(CASE WHEN u.last_login > NOW() - INTERVAL '7 days' THEN 1 ELSE 0 END) as weekly_active_users
FROM organizations o
LEFT JOIN devices d ON o.id = d.organization_id
LEFT JOIN users u ON o.id = u.organization_id
LEFT JOIN telemetry_records tr ON d.id = tr.device_id AND tr.timestamp > CURRENT_DATE
LEFT JOIN alerts a ON d.id = a.device_id AND a.created_at > CURRENT_DATE
WHERE o.name LIKE '%pilot%'
GROUP BY o.id, o.name;
```

## Support and Training

### 1. Pilot Support Channels
- **Email:** pilot-support@sensorvision.io
- **Slack Channel:** #sensorvision-pilot
- **Weekly Office Hours:** Tuesdays 2-3 PM PST
- **Documentation:** https://docs.sensorvision.io/pilot

### 2. Training Schedule
```markdown
## Week 1: Platform Introduction
- Platform overview and capabilities
- Account setup and navigation
- Basic device connection

## Week 2: Device Management
- Device registration and configuration
- Data visualization and dashboards
- Basic alerting setup

## Week 3: Advanced Features
- Synthetic variables and expressions
- Plugin marketplace exploration
- Advanced analytics and reporting

## Week 4: Integration and Automation
- API integration examples
- Webhook configuration
- Serverless functions introduction
```

## Success Metrics

### 1. Pilot KPIs
```yaml
success_metrics:
  adoption:
    target_organizations: 10
    target_users: 200
    target_devices: 500
    
  engagement:
    weekly_active_users: 80%
    devices_sending_data: 90%
    dashboards_created: 50
    
  satisfaction:
    nps_score: 50+
    support_ticket_resolution: 24h
    feature_completion_rate: 75%
    
  technical:
    uptime: 99.5%
    api_response_time_p95: 500ms
    data_ingestion_success_rate: 99.9%
```

### 2. Feedback Collection Points
- Weekly usage surveys
- Feature-specific feedback forms
- Exit interviews for churned users
- Monthly pilot review meetings
- End-of-pilot comprehensive survey