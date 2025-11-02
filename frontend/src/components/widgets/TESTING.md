# Tremor Chart Widgets - Manual Testing Guide

## Overview

This guide provides a comprehensive manual testing checklist for the Tremor chart widgets that replaced the Chart.js implementation.

## Services Setup

Ensure both services are running:

```bash
# Terminal 1: Backend
./gradlew bootRun

# Terminal 2: Frontend
cd frontend
npm run dev
```

Access the dashboard at: **http://localhost:3001/dashboards**

---

## Test Scenarios

### 1. LineChart Widget Tests

#### Basic Functionality
- [ ] Create a LINE_CHART widget with a temperature variable
- [ ] Verify data loads and displays correctly
- [ ] Check that time axis shows proper timestamps
- [ ] Verify values are displayed with 2 decimal precision
- [ ] Confirm smooth line rendering

#### Configuration Options
- [ ] Toggle legend visibility (should show/hide)
- [ ] Toggle grid lines (should show/hide)
- [ ] Set custom color (e.g., red, blue, green)
- [ ] Set min/max Y-axis values
- [ ] Change time range (30min, 1hr, 2hr, 4hr)
- [ ] Verify all settings persist after page refresh

#### Real-time Updates
- [ ] Send MQTT telemetry data
- [ ] Verify chart updates in real-time via WebSocket
- [ ] Confirm new data points appear on the chart
- [ ] Check that chart auto-scrolls with new data

#### Error Handling
- [ ] Create widget without device ID - should show "No data available"
- [ ] Create widget with invalid variable name - should handle gracefully
- [ ] Disconnect backend - verify error message appears
- [ ] Reconnect backend - verify chart recovers

---

### 2. BarChart Widget Tests

#### Basic Functionality
- [ ] Create a BAR_CHART widget with a power variable
- [ ] Verify bars display correctly
- [ ] Check that values are properly labeled
- [ ] Confirm bar spacing is appropriate
- [ ] Verify hover tooltips show correct values

#### Configuration Options
- [ ] Toggle legend visibility
- [ ] Toggle grid lines
- [ ] Set custom bar color
- [ ] Set min/max Y-axis values
- [ ] Change time range
- [ ] Verify refresh interval works (30s default)

#### Real-time Updates
- [ ] Send MQTT data and verify bars update
- [ ] Check that old bars are replaced with new data
- [ ] Verify smooth transitions between updates

#### Error Handling
- [ ] Test with no device ID
- [ ] Test with missing data
- [ ] Test with API errors

---

### 3. AreaChart Widget Tests

#### Basic Functionality
- [ ] Create an AREA_CHART widget with a humidity variable
- [ ] Verify filled area displays correctly
- [ ] Check gradient/color fill
- [ ] Confirm line on top of area
- [ ] Verify smooth curve rendering

#### Configuration Options
- [ ] Toggle legend visibility
- [ ] Toggle grid lines
- [ ] Set custom fill color
- [ ] Set min/max Y-axis values
- [ ] Test beginAtZero option
- [ ] Change time range

#### Real-time Updates
- [ ] Send MQTT data and verify area updates
- [ ] Check that fill animates smoothly
- [ ] Verify data integrity through multiple refreshes

#### Error Handling
- [ ] Test with no device ID
- [ ] Test with empty data
- [ ] Test with API failures

---

## Regression Testing (vs Chart.js)

### Data Accuracy
- [ ] Compare data values between old Chart.js and new Tremor charts
- [ ] Verify same number of data points are displayed
- [ ] Confirm timestamps match
- [ ] Check that calculations are identical

### Visual Comparison
- [ ] Charts render at similar sizes
- [ ] Colors are consistent (or configurable)
- [ ] Grid lines align properly
- [ ] Legends position correctly
- [ ] Tooltips show same information

### Performance
- [ ] Charts load at similar speeds
- [ ] Real-time updates are smooth
- [ ] No memory leaks during auto-refresh
- [ ] Browser performance is acceptable

### Configuration Compatibility
- [ ] All Chart.js config options work with Tremor
- [ ] Colors config translates correctly
- [ ] showLegend works
- [ ] showGrid works
- [ ] min/max values work
- [ ] timeRangeMinutes works
- [ ] refreshInterval works

---

## Integration Testing

### Dashboard Context Device
- [ ] Create widget with "Use Context Device" enabled
- [ ] Switch device in dashboard selector
- [ ] Verify chart updates to show new device's data
- [ ] Test with multiple widgets using context device

### Multiple Widgets
- [ ] Add 3+ chart widgets to dashboard
- [ ] Verify all update independently
- [ ] Check that WebSocket updates all charts
- [ ] Confirm no performance degradation

### Kiosk Mode
- [ ] Enable kiosk mode (?kiosk=true)
- [ ] Verify charts display full screen
- [ ] Confirm auto-refresh works in kiosk mode
- [ ] Test dashboard switching

### Drag & Drop
- [ ] Drag chart widgets to new positions
- [ ] Resize chart widgets
- [ ] Verify charts re-render correctly after resize
- [ ] Confirm positions persist after refresh

---

## Browser Compatibility

Test in multiple browsers:
- [ ] Chrome (latest)
- [ ] Firefox (latest)
- [ ] Edge (latest)
- [ ] Safari (if available)

Check for:
- Rendering differences
- Performance issues
- Console errors
- Responsive behavior

---

## MQTT Testing

### Send Test Data

```bash
# Line chart data (temperature)
mosquitto_pub -h localhost -p 1883 -t "sensorvision/devices/test-001/telemetry" -m '{
  "deviceId": "test-001",
  "timestamp": "2024-01-01T12:00:00Z",
  "variables": {
    "temperature": 23.5
  }
}'

# Bar chart data (power)
mosquitto_pub -h localhost -p 1883 -t "sensorvision/devices/test-001/telemetry" -m '{
  "deviceId": "test-001",
  "timestamp": "2024-01-01T12:00:00Z",
  "variables": {
    "power": 150.0
  }
}'

# Area chart data (humidity)
mosquitto_pub -h localhost -p 1883 -t "sensorvision/devices/test-001/telemetry" -m '{
  "deviceId": "test-001",
  "timestamp": "2024-01-01T12:00:00Z",
  "variables": {
    "humidity": 65.0
  }
}'
```

### Stress Testing
- [ ] Send rapid MQTT messages (10/second)
- [ ] Verify charts handle high frequency updates
- [ ] Check for UI lag or freezing
- [ ] Confirm memory usage stays stable

---

## Edge Cases

### Data Scenarios
- [ ] No data available (empty array)
- [ ] Single data point
- [ ] Very large values (1000000+)
- [ ] Very small values (0.0001)
- [ ] Negative values
- [ ] Zero values
- [ ] Missing values (null/undefined)

### Time Scenarios
- [ ] Time range > available data
- [ ] Time range < available data
- [ ] Data with gaps in timestamps
- [ ] Future timestamps
- [ ] Past timestamps (years ago)

### Configuration Scenarios
- [ ] No config object
- [ ] Empty config object {}
- [ ] Invalid color names
- [ ] Min > Max values
- [ ] Negative time ranges
- [ ] Zero refresh interval

---

## Acceptance Criteria

All chart widgets must:
- ✅ Display data correctly from backend API
- ✅ Update in real-time via WebSocket
- ✅ Respect all configuration options
- ✅ Handle errors gracefully
- ✅ Maintain performance with auto-refresh
- ✅ Work in kiosk mode
- ✅ Support drag & drop repositioning
- ✅ Support device context switching
- ✅ Match or exceed Chart.js functionality
- ✅ Provide better visual design than Chart.js

---

## Reporting Issues

If you find any issues during testing:

1. **Reproduce** - Verify the issue is reproducible
2. **Document** - Note the exact steps to reproduce
3. **Screenshot** - Capture any visual issues
4. **Console** - Check browser console for errors
5. **Network** - Check network tab for API failures
6. **Report** - Create GitHub issue with all above info

---

## Notes

- Tremor uses a simpler API than Chart.js
- Bundle size increased from 1.4MB to 2.3MB (acceptable trade-off)
- Charts now match the Tailwind design system
- Better accessibility and responsiveness out-of-the-box

---

## Resources

- [Tremor Documentation](https://www.tremor.so/docs/visualizations/line-chart)
- [SensorVision Dashboard Guide](../../README.md)
- [MQTT Testing Guide](../../../CLAUDE.md)
