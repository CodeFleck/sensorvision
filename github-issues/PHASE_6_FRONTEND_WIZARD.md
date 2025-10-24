# Phase 6: Frontend Integration Wizard

**Labels:** `enhancement`, `frontend`, `ux`, `wizard`
**Milestone:** Integration Simplification
**Estimated Duration:** 1 week
**Priority:** High (Best UX improvement)
**Depends on:** Phase 0 (Quick Wins) ✅ COMPLETE

## 🎯 Objectives

Create an interactive step-by-step wizard in the SensorVision frontend that:
- Guides users through device integration
- Generates platform-specific code automatically
- Tests connections in real-time
- Provides copy-paste ready code snippets
- Reduces integration time to < 5 minutes

## ✅ Tasks

### UI/UX Design
- [ ] Design wizard flow (5 steps max)
- [ ] Create mockups for each step
- [ ] Design platform selection interface
- [ ] Create code snippet display component
- [ ] Design real-time test results UI

### Core Wizard Development
- [ ] Implement wizard component (React/TypeScript)
- [ ] Step 1: Select platform (ESP32, Python, Node.js, etc.)
- [ ] Step 2: Get/generate device token
- [ ] Step 3: Generate platform-specific code
- [ ] Step 4: Test connection in real-time
- [ ] Step 5: Success confirmation & next steps

### Code Generation
- [ ] ESP32/Arduino code generator
- [ ] Python code generator
- [ ] Node.js/JavaScript code generator
- [ ] Raspberry Pi code generator
- [ ] cURL command generator
- [ ] Template system for code snippets

### Real-time Testing
- [ ] "Test Connection" button functionality
- [ ] Live connection status indicator
- [ ] Real-time data display
- [ ] Error diagnosis and suggestions
- [ ] Connection troubleshooting guide

### Features
- [ ] Copy to clipboard for all code snippets
- [ ] Download code as file (.ino, .py, .js)
- [ ] Email code to user
- [ ] Platform-specific hardware requirements
- [ ] Wiring diagrams (where applicable)
- [ ] Video tutorials embedded

### Integration Points
- [ ] Device management page integration
- [ ] Dashboard quick-start link
- [ ] First-time user onboarding flow
- [ ] Help menu integration

## 🎨 Wizard Flow

```
┌─────────────────────────────────────────┐
│  Step 1: Choose Your Platform           │
│  [ ESP32 ] [ Python ] [ Node.js ]       │
│  [ Raspberry Pi ] [ Arduino ] [ cURL ]  │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│  Step 2: Device Setup                   │
│  Device ID: [sensor-001]                │
│  API Token: [Generate] [Use Existing]   │
│  Token: 550e8400-e29b-41d4...  [Copy]   │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│  Step 3: Copy Your Code                 │
│  ┌───────────────────────────────────┐  │
│  │ #include <WiFi.h>                 │  │
│  │ #include <HTTPClient.h>           │  │
│  │ ...                               │  │
│  └───────────────────────────────────┘  │
│  [Copy Code] [Download .ino]            │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│  Step 4: Test Connection                │
│  [Test Now]  Status: 🟢 Connected       │
│  Last data received: 2 seconds ago      │
│  Temperature: 23.5°C                    │
│  Humidity: 65.2%                        │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│  Step 5: You're All Set! 🎉            │
│  ✅ Device connected successfully       │
│  📊 View your dashboard                 │
│  📚 Read advanced tutorials             │
│  🔔 Set up alerts (optional)            │
└─────────────────────────────────────────┘
```

## 💡 Code Generation Examples

### ESP32/Arduino
```cpp
#include <WiFi.h>
#include <HTTPClient.h>

const char* ssid = "YOUR_WIFI_SSID";
const char* password = "YOUR_WIFI_PASSWORD";
const char* apiKey = "550e8400-e29b-41d4-a716-446655440000";
const char* deviceId = "sensor-001";

void sendData(float temperature, float humidity) {
  HTTPClient http;
  http.begin("http://localhost:8080/api/v1/ingest/" + String(deviceId));
  http.addHeader("X-API-Key", apiKey);
  http.addHeader("Content-Type", "application/json");

  String payload = "{\"temperature\":" + String(temperature) +
                   ",\"humidity\":" + String(humidity) + "}";
  int httpCode = http.POST(payload);
  // ... error handling
}
```

### Python
```python
import requests

API_URL = "http://localhost:8080"
API_KEY = "550e8400-e29b-41d4-a716-446655440000"
DEVICE_ID = "sensor-001"

def send_data(temperature, humidity):
    response = requests.post(
        f"{API_URL}/api/v1/ingest/{DEVICE_ID}",
        headers={"X-API-Key": API_KEY},
        json={"temperature": temperature, "humidity": humidity}
    )
    return response.json()
```

## 🎯 Success Criteria

- [ ] Wizard accessible from dashboard
- [ ] All 6 platforms have code generators
- [ ] Real-time connection testing works
- [ ] Code copy/download functionality
- [ ] First-time user onboarding complete
- [ ] Video tutorials embedded
- [ ] User testing shows < 5 min integration time
- [ ] Mobile responsive design

## 📚 References

- Backend API: `/api/v1/ingest/{deviceId}`
- Integration templates: `integration-templates/`
- Frontend: `frontend/src/`

## 🔗 Related

- **Depends on:** Phase 0 (Quick Wins) ✅ COMPLETE
- **Enhances:** Phase 3 (ESP32 SDK), Phase 4 (Python SDK), Phase 5 (JS SDK)
- **Blocks:** Phase 7 (Documentation & Guides)

## 📹 Additional Assets

- [ ] Create 30-second demo video
- [ ] Create platform-specific tutorial videos (< 3 min each)
- [ ] Create troubleshooting video
- [ ] Add wizard walkthrough to docs
