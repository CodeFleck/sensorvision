# SensorVision Pilot Documentation and Training Plan

## Documentation Structure

### 1. Pilot-Specific Documentation
```
docs/pilot/
├── README.md                           # Pilot program overview
├── getting-started/
│   ├── account-setup.md               # Initial account configuration
│   ├── first-device.md                # Connecting your first device
│   ├── dashboard-basics.md            # Understanding dashboards
│   └── integration-wizard.md          # Using the integration wizard
├── user-guides/
│   ├── device-management.md           # Managing devices and groups
│   ├── dashboard-creation.md          # Creating custom dashboards
│   ├── alerting-setup.md             # Setting up alerts and notifications
│   ├── analytics-reporting.md         # Using analytics and reports
│   └── plugin-marketplace.md          # Installing and using plugins
├── integration-examples/
│   ├── esp32-arduino.md              # ESP32/Arduino integration
│   ├── raspberry-pi.md               # Raspberry Pi integration
│   ├── python-scripts.md             # Python device scripts
│   ├── lorawan-ttn.md                # LoRaWAN/TTN integration
│   └── modbus-industrial.md          # Modbus industrial sensors
├── api-reference/
│   ├── authentication.md             # API authentication
│   ├── device-api.md                 # Device management API
│   ├── telemetry-api.md              # Telemetry ingestion API
│   └── webhook-api.md                # Webhook configuration
├── troubleshooting/
│   ├── common-issues.md              # Common problems and solutions
│   ├── connectivity-issues.md        # Network and connectivity
│   ├── data-issues.md                # Data ingestion problems
│   └── performance-issues.md         # Performance troubleshooting
└── support/
    ├── contact-info.md               # Support contact information
    ├── feedback-process.md           # How to provide feedback
    └── escalation-procedures.md      # Issue escalation process
```

### 2. Quick Start Guide
```markdown
# SensorVision Pilot Quick Start Guide

## Welcome to the Pilot Program!

This guide will help you get up and running with SensorVision in under 30 minutes.

### Prerequisites
- Pilot program invitation email
- IoT device or sensor (ESP32, Raspberry Pi, or similar)
- Basic understanding of IoT concepts

### Step 1: Account Setup (5 minutes)
1. **Login to your account**
   - Visit: https://pilot.sensorvision.io
   - Use credentials from your invitation email
   - Complete profile setup

2. **Verify your organization**
   - Check organization name and settings
   - Update contact information if needed

### Step 2: Connect Your First Device (10 minutes)
1. **Use the Integration Wizard**
   - Navigate to "Integration Wizard" in the sidebar
   - Select your device type (ESP32, Raspberry Pi, etc.)
   - Follow the step-by-step instructions

2. **Copy the generated code**
   - The wizard generates device-specific code
   - Copy and paste into your device
   - Upload to your device

3. **Verify connection**
   - Check the "Devices" page for your new device
   - Confirm data is being received

### Step 3: Create Your First Dashboard (10 minutes)
1. **Navigate to Dashboards**
   - Click "Dashboards" in the sidebar
   - Click "Create New Dashboard"

2. **Add widgets**
   - Add a line chart for temperature data
   - Add a gauge for humidity
   - Add a statistics card for device status

3. **Customize and save**
   - Arrange widgets as desired
   - Set refresh intervals
   - Save your dashboard

### Step 4: Set Up Alerts (5 minutes)
1. **Create a rule**
   - Go to "Rules & Alerts"
   - Click "Create Rule"
   - Set condition: temperature > 30°C

2. **Configure notifications**
   - Add your email for notifications
   - Set alert severity
   - Enable the rule

### Next Steps
- Explore the Plugin Marketplace
- Try advanced analytics features
- Connect additional devices
- Join our weekly office hours

### Need Help?
- Email: pilot-support@sensorvision.io
- Slack: #sensorvision-pilot
- Documentation: https://docs.sensorvision.io/pilot
```

## Training Program Structure

### 1. Onboarding Webinar Series
```yaml
webinar_schedule:
  week_1:
    title: "SensorVision Platform Overview"
    duration: 60_minutes
    topics:
      - Platform capabilities and architecture
      - Pilot program goals and timeline
      - Account setup and navigation
      - Q&A session
    
  week_2:
    title: "Device Integration Workshop"
    duration: 90_minutes
    topics:
      - Integration wizard walkthrough
      - Device connection examples
      - Troubleshooting common issues
      - Hands-on exercises
    
  week_3:
    title: "Dashboards and Analytics"
    duration: 75_minutes
    topics:
      - Dashboard creation and customization
      - Widget types and configuration
      - Analytics and reporting features
      - Best practices
    
  week_4:
    title: "Advanced Features and Plugins"
    duration: 60_minutes
    topics:
      - Plugin marketplace exploration
      - Synthetic variables and expressions
      - Advanced alerting strategies
      - API integration examples
```

### 2. Interactive Tutorials
```typescript
// Interactive tutorial component
export const PilotTutorial: React.FC = () => {
  const [currentStep, setCurrentStep] = useState(0);
  const [completedSteps, setCompletedSteps] = useState<number[]>([]);

  const tutorialSteps = [
    {
      id: 'welcome',
      title: 'Welcome to SensorVision',
      content: 'Let\'s take a tour of the platform...',
      target: '.sidebar',
      action: 'highlight'
    },
    {
      id: 'devices',
      title: 'Device Management',
      content: 'This is where you manage your IoT devices...',
      target: '[href="/devices"]',
      action: 'click'
    },
    {
      id: 'integration-wizard',
      title: 'Integration Wizard',
      content: 'Use this wizard to connect your first device...',
      target: '[href="/integration-wizard"]',
      action: 'navigate'
    },
    {
      id: 'dashboard',
      title: 'Dashboards',
      content: 'Create custom dashboards to visualize your data...',
      target: '[href="/dashboards"]',
      action: 'navigate'
    }
  ];

  return (
    <TutorialProvider
      steps={tutorialSteps}
      currentStep={currentStep}
      onStepComplete={(stepId) => {
        setCompletedSteps([...completedSteps, currentStep]);
        setCurrentStep(currentStep + 1);
      }}
    >
      <TutorialOverlay />
    </TutorialProvider>
  );
};
```

### 3. Video Tutorial Library
```markdown
## Video Tutorial Topics

### Getting Started (5 videos, ~30 minutes total)
1. **Platform Overview** (5 min)
   - Interface walkthrough
   - Key concepts explanation
   - Navigation basics

2. **Account Setup** (3 min)
   - Profile configuration
   - Organization settings
   - Security settings

3. **First Device Connection** (8 min)
   - Integration wizard demo
   - ESP32 example
   - Troubleshooting tips

4. **Dashboard Creation** (10 min)
   - Widget types overview
   - Layout customization
   - Real-time data display

5. **Basic Alerting** (4 min)
   - Rule creation
   - Notification setup
   - Testing alerts

### Advanced Features (8 videos, ~60 minutes total)
1. **Plugin Marketplace** (8 min)
   - Plugin discovery
   - Installation process
   - Configuration examples

2. **Synthetic Variables** (10 min)
   - Expression syntax
   - Mathematical functions
   - Statistical analysis

3. **Advanced Analytics** (12 min)
   - Data aggregation
   - Time-series analysis
   - Export capabilities

4. **API Integration** (15 min)
   - Authentication setup
   - REST API examples
   - Webhook configuration

5. **Fleet Management** (8 min)
   - Device groups
   - Bulk operations
   - Global rules

6. **Custom Dashboards** (7 min)
   - Advanced widgets
   - Dashboard sharing
   - Template usage
```

## Support Documentation

### 1. FAQ Document
```markdown
# SensorVision Pilot FAQ

## General Questions

### Q: What is included in the pilot program?
A: The pilot includes:
- Full platform access for 90 days
- Up to 100 devices per organization
- Email and SMS notifications
- Plugin marketplace access
- Dedicated pilot support
- Weekly office hours

### Q: How do I get support during the pilot?
A: Multiple support channels are available:
- Email: pilot-support@sensorvision.io (24h response)
- Slack: #sensorvision-pilot (business hours)
- Weekly office hours: Tuesdays 2-3 PM PST
- Documentation: https://docs.sensorvision.io/pilot

### Q: Can I invite additional users to my organization?
A: Yes, you can invite up to 50 users per organization during the pilot.

## Technical Questions

### Q: What devices are supported?
A: SensorVision supports any device that can send HTTP or MQTT messages:
- ESP32/ESP8266 microcontrollers
- Raspberry Pi and similar SBCs
- Industrial sensors with Modbus
- LoRaWAN devices via TTN
- Custom devices via REST API

### Q: What data formats are accepted?
A: Multiple formats are supported:
- JSON (recommended)
- CSV
- Protocol-specific formats via plugins
- Custom formats via serverless functions

### Q: How is data stored and secured?
A: Data security features:
- Encrypted at rest and in transit
- Organization-level isolation
- Regular automated backups
- SOC 2 Type II compliance (in progress)

### Q: What are the rate limits?
A: Pilot program limits:
- 100,000 API calls per day
- 1,000,000 telemetry points per day
- 1,000 alerts per day
- 90 days data retention

## Integration Questions

### Q: How do I connect an ESP32 device?
A: Use the Integration Wizard:
1. Go to Integration Wizard
2. Select "ESP32/Arduino"
3. Copy the generated code
4. Upload to your device
5. Verify connection in Devices page

### Q: Can I use existing MQTT brokers?
A: Yes, you can:
- Use SensorVision's built-in MQTT broker
- Connect via HTTP REST API
- Use MQTT bridge plugin for external brokers

### Q: How do I set up LoRaWAN devices?
A: Install the LoRaWAN TTN plugin:
1. Go to Plugin Marketplace
2. Install "LoRaWAN TTN Integration"
3. Configure with your TTN application details
4. Devices will auto-register on first uplink
```

### 2. Troubleshooting Guide
```markdown
# SensorVision Pilot Troubleshooting Guide

## Device Connection Issues

### Device Not Appearing in Dashboard
**Symptoms:** Device code uploaded but not visible in Devices page

**Troubleshooting Steps:**
1. Check device token is correct
2. Verify network connectivity
3. Check MQTT/HTTP endpoint URLs
4. Review device logs for error messages
5. Test with curl command:
   ```bash
   curl -X POST https://pilot.sensorvision.io/api/v1/ingest/test-device \
     -H "X-API-Key: YOUR_DEVICE_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"temperature": 25.0}'
   ```

### Data Not Updating
**Symptoms:** Device appears but no recent data

**Troubleshooting Steps:**
1. Check device status (online/offline)
2. Verify data format matches expected schema
3. Check for rate limiting (too frequent updates)
4. Review telemetry logs in device details
5. Test with Integration Wizard code

### Connection Timeouts
**Symptoms:** Device connects but frequently disconnects

**Troubleshooting Steps:**
1. Check network stability
2. Increase keepalive interval
3. Implement retry logic with exponential backoff
4. Use persistent connections for MQTT
5. Monitor connection logs

## Dashboard Issues

### Widgets Not Loading Data
**Symptoms:** Dashboard widgets show "No data available"

**Troubleshooting Steps:**
1. Verify device is sending data
2. Check widget device/variable selection
3. Adjust time range (last 24 hours)
4. Refresh dashboard manually
5. Check browser console for errors

### Slow Dashboard Performance
**Symptoms:** Dashboard takes long time to load

**Troubleshooting Steps:**
1. Reduce number of widgets per dashboard
2. Increase widget refresh intervals
3. Use data aggregation for large datasets
4. Clear browser cache
5. Check network connection speed

## Alert Issues

### Alerts Not Triggering
**Symptoms:** Rule conditions met but no alerts generated

**Troubleshooting Steps:**
1. Verify rule is enabled
2. Check rule conditions and thresholds
3. Ensure device is sending required variables
4. Check alert cooldown period
5. Test rule with manual evaluation

### Missing Notifications
**Symptoms:** Alerts created but notifications not received

**Troubleshooting Steps:**
1. Verify notification preferences
2. Check email spam/junk folders
3. Confirm phone number for SMS
4. Test notification channels
5. Check notification logs

## Performance Issues

### Slow API Responses
**Symptoms:** API calls taking longer than expected

**Troubleshooting Steps:**
1. Check API endpoint status
2. Reduce query complexity
3. Use pagination for large datasets
4. Implement client-side caching
5. Contact support if persistent

### High Data Usage
**Symptoms:** Approaching pilot program limits

**Troubleshooting Steps:**
1. Review telemetry frequency
2. Implement data filtering
3. Use data aggregation
4. Archive old data
5. Optimize device code

## Getting Additional Help

### When to Contact Support
- Issues not resolved by troubleshooting guide
- Suspected platform bugs
- Feature requests or feedback
- Integration assistance needed

### Information to Include
- Organization name and user email
- Device ID and type
- Error messages or screenshots
- Steps to reproduce the issue
- Expected vs actual behavior

### Support Channels
- **Email:** pilot-support@sensorvision.io
- **Slack:** #sensorvision-pilot
- **Office Hours:** Tuesdays 2-3 PM PST
- **Emergency:** Use "Critical" tag in support email
```

## Documentation Maintenance

### 1. Content Review Process
```yaml
review_schedule:
  weekly:
    - Update FAQ with new questions
    - Review and respond to documentation feedback
    - Update troubleshooting guide with new issues
    
  bi_weekly:
    - Review tutorial completion rates
    - Update integration examples
    - Refresh video content if needed
    
  monthly:
    - Comprehensive documentation audit
    - User feedback analysis
    - Content gap identification
    - Performance metrics review
```

### 2. Feedback Collection
```typescript
// Documentation feedback component
export const DocumentationFeedback: React.FC<{page: string}> = ({page}) => {
  const [feedback, setFeedback] = useState({
    helpful: null as boolean | null,
    comment: '',
    email: ''
  });

  const submitFeedback = async () => {
    await api.post('/api/v1/pilot/documentation-feedback', {
      ...feedback,
      page,
      timestamp: new Date().toISOString()
    });
    
    toast.success('Thank you for your feedback!');
  };

  return (
    <div className="mt-8 p-4 bg-gray-50 rounded-lg">
      <h3 className="text-lg font-medium mb-4">Was this page helpful?</h3>
      
      <div className="flex gap-4 mb-4">
        <button
          onClick={() => setFeedback({...feedback, helpful: true})}
          className={`px-4 py-2 rounded ${feedback.helpful === true ? 'bg-green-500 text-white' : 'bg-gray-200'}`}
        >
          👍 Yes
        </button>
        <button
          onClick={() => setFeedback({...feedback, helpful: false})}
          className={`px-4 py-2 rounded ${feedback.helpful === false ? 'bg-red-500 text-white' : 'bg-gray-200'}`}
        >
          👎 No
        </button>
      </div>
      
      <textarea
        placeholder="Additional comments (optional)"
        value={feedback.comment}
        onChange={(e) => setFeedback({...feedback, comment: e.target.value})}
        className="w-full p-2 border rounded mb-4"
        rows={3}
      />
      
      <button
        onClick={submitFeedback}
        className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
      >
        Submit Feedback
      </button>
    </div>
  );
};
```

## Success Metrics

### Documentation Effectiveness
- Tutorial completion rates > 80%
- Support ticket reduction > 30%
- User onboarding time < 30 minutes
- Documentation satisfaction score > 4.0/5.0

### Training Program Success
- Webinar attendance > 70%
- Post-training quiz scores > 85%
- Feature adoption rate > 60%
- User confidence scores > 4.0/5.0