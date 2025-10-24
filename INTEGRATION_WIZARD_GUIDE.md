# Integration Wizard - User Guide

**Phase 6: Frontend Integration Wizard** ✅ COMPLETE

## Overview

The Integration Wizard is a 5-step guided interface that helps users connect their devices to SensorVision in less than 5 minutes. It's accessible to all users (not admin-only) and provides platform-specific code generation.

## Features

### Step 1: Platform Selection
- **Supported Platforms:**
  - ESP32 / Arduino (IoT microcontrollers with WiFi)
  - Python (Raspberry Pi, servers, scripts)
  - Node.js / JavaScript (Web apps, Node-RED, servers)
  - Raspberry Pi (Single-board computer projects)
  - Arduino (Microcontroller projects)
  - cURL / HTTP (Command line, testing, scripts)

### Step 2: Device Setup
- **Create New Device** or **Use Existing Device**
- Automatically generates device API token
- Validates device ID uniqueness
- User-friendly error handling

### Step 3: Code Generation
- **Platform-specific code templates:**
  - ✅ ESP32/Arduino (.ino files)
  - ✅ Python (.py files)
  - ✅ Node.js (.js files)
  - ✅ Bash/cURL (.sh files)
- **Features:**
  - Copy to clipboard
  - Download as file
  - Syntax-highlighted code display
  - Pre-configured with API URL and token

### Step 4: Connection Testing
- Real-time connection verification
- Sends test telemetry data
- Visual success/error feedback
- Detailed error messages for troubleshooting

### Step 5: Success Confirmation
- Completion checklist
- Quick links to:
  - Dashboard (view device data)
  - Rules & Alerts (set up monitoring)
  - Analytics (historical data)
- Option to add another device

## Technical Implementation

### Components
- **Location:** `frontend/src/pages/IntegrationWizard.tsx`
- **Routing:** `/integration-wizard`
- **Navigation:** Accessible from main sidebar (Zap icon)
- **Dependencies:**
  - React Router for navigation
  - lucide-react for icons
  - clsx for conditional styling
  - apiService for backend integration

### API Integration
The wizard uses the following API endpoints:
- `POST /api/v1/devices` - Create new device
- `POST /api/v1/devices/{deviceId}/token/generate` - Generate device token
- `POST /api/v1/ingest/{deviceId}` - Test connection with sample data

### Code Templates

#### ESP32/Arduino Template
```cpp
#include <WiFi.h>
#include <HTTPClient.h>

const char* apiKey = "{GENERATED_TOKEN}";
const char* deviceId = "{DEVICE_ID}";

void sendData(float temperature, float humidity) {
  HTTPClient http;
  http.begin("http://localhost:8080/api/v1/ingest/" + String(deviceId));
  http.addHeader("X-API-Key", apiKey);
  http.addHeader("Content-Type", "application/json");

  String payload = "{\"temperature\":" + String(temperature) +
                   ",\"humidity\":" + String(humidity) + "}";
  http.POST(payload);
  http.end();
}
```

#### Python Template
```python
import requests

API_KEY = "{GENERATED_TOKEN}"
DEVICE_ID = "{DEVICE_ID}"

def send_data(temperature, humidity):
    response = requests.post(
        f"http://localhost:8080/api/v1/ingest/{DEVICE_ID}",
        headers={"X-API-Key": API_KEY},
        json={"temperature": temperature, "humidity": humidity}
    )
    return response.json()
```

#### Node.js Template
```javascript
const axios = require('axios');

const API_KEY = '{GENERATED_TOKEN}';
const DEVICE_ID = '{DEVICE_ID}';

async function sendData(temperature, humidity) {
  const response = await axios.post(
    `http://localhost:8080/api/v1/ingest/${DEVICE_ID}`,
    { temperature, humidity },
    { headers: { 'X-API-Key': API_KEY } }
  );
  return response.data;
}
```

## User Experience

### Navigation Flow
1. User clicks "Integration Wizard" in sidebar
2. Progress bar shows 5 steps with current position
3. Each step has clear instructions and visual feedback
4. Back button allows users to revisit previous steps
5. Final step provides clear next actions

### Visual Design
- **Color-coded platform cards** for easy identification
- **Progress indicator** with checkmarks for completed steps
- **Code editor styling** with dark theme and syntax highlighting
- **Success animations** with checkmarks and green highlighting
- **Responsive design** works on desktop and tablet

## Testing Checklist

### Manual Testing Steps
1. ✅ Navigate to `/integration-wizard`
2. ✅ Select each platform type (6 platforms total)
3. ✅ Create new device with unique ID
4. ✅ Verify token generation
5. ✅ Verify code generation for each platform
6. ✅ Test "Copy to Clipboard" button
7. ✅ Test "Download Code" button
8. ✅ Test connection with test data
9. ✅ Verify success page displays correctly
10. ✅ Test "Add Another Device" flow

### Integration Testing
- Backend must be running on port 8080
- PostgreSQL database must be available
- User must be logged in (wizard is protected route)
- Device IDs must be unique per organization

## Accessibility

- Keyboard navigation supported
- Semantic HTML with proper ARIA labels
- Clear focus indicators
- High contrast text and buttons
- Screen reader friendly

## Future Enhancements

### Potential Improvements (not in Phase 6 scope)
1. Video tutorials embedded in each step
2. Wiring diagrams for ESP32/Arduino
3. SDK library links (Python PyPI, Node.js NPM)
4. Hardware requirements per platform
5. Email code to user option
6. Multi-language support
7. Dark mode toggle
8. More platform templates (Go, Rust, Java)

## Success Metrics

- ✅ 5-step wizard flow implemented
- ✅ 6 platform templates with working code
- ✅ Real-time connection testing
- ✅ Copy/download functionality
- ✅ Responsive design
- ✅ Integration with existing backend APIs
- ✅ User-friendly error handling
- ✅ Accessible from main navigation

## Related Documentation

- **Backend API:** See `SimpleIngestionController.java` for `/api/v1/ingest/{deviceId}` endpoint
- **Python SDK:** See `SDK_TEST_RESULTS.md` for Python SDK testing
- **JavaScript SDK:** See `SDK_TEST_RESULTS.md` for JS SDK testing
- **Device Management:** See `DeviceController.java` for device token endpoints

## Developer Notes

### Adding New Platforms
To add support for a new platform:

1. Add platform to `platforms` array in `IntegrationWizard.tsx`:
```typescript
{
  id: 'new-platform',
  name: 'Platform Name',
  description: 'Description',
  icon: IconComponent,
  color: 'blue',
}
```

2. Add case to `generateCode()` function
3. Add file extension to `downloadCode()` extensions map
4. Test code generation and connection

### Customizing Code Templates
Code templates are in the `generateCode()` function. Each template:
- Uses string interpolation for deviceId, apiToken, apiUrl
- Includes basic error handling
- Provides example sensor reading structure
- Includes comments for customization

---

**Status:** ✅ Phase 6 Complete
**Frontend URL:** http://localhost:3001/integration-wizard
**Backend URL:** http://localhost:8080
**Created:** October 23, 2025
**Last Updated:** October 23, 2025
