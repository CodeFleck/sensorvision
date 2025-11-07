# Statistical Functions for Synthetic Variables

## Overview

Statistical functions enable time-series analytics and historical data aggregation in synthetic variable expressions. These functions operate on historical telemetry data within configurable time windows.

## Time Windows

Supported time window specifications:
- `5m` - 5 minutes
- `15m` - 15 minutes
- `1h` - 1 hour
- `6h` - 6 hours
- `12h` - 12 hours
- `24h` - 24 hours
- `7d` - 7 days
- `30d` - 30 days

## Available Statistical Functions

### `avg(variable, timeWindow)`
Calculate the average (mean) value over a time window.

**Example:**
```javascript
// Average consumption in last hour
avg("kwConsumption", "1h")

// Spike detection: current > 1.5x average
if(kwConsumption > avg("kwConsumption", "1h") * 1.5, 1, 0)
```

### `stddev(variable, timeWindow)`
Calculate standard deviation to measure variability.

**Example:**
```javascript
// Temperature variability in last 24 hours
stddev("temperature", "24h")

// High variability alert
if(stddev("voltage", "1h") > 10, 1, 0)
```

### `percentile(variable, percentile, timeWindow)`
Calculate the Nth percentile value.

**Example:**
```javascript
// 95th percentile consumption (last 7 days)
percentile("kwConsumption", 95, "7d")

// Alert if above 95th percentile
if(kwConsumption > percentile("kwConsumption", 95, "30d"), 1, 0)
```

### `sum(variable, timeWindow)`
Calculate total sum over a time window.

**Example:**
```javascript
// Total consumption in last 24 hours
sum("kwConsumption", "24h")

// Daily cost calculation
sum("kwConsumption", "24h") * 0.15
```

### `count(variable, timeWindow)`
Count number of data points in a time window.

**Example:**
```javascript
// Number of readings in last hour
count("kwConsumption", "1h")

// Data quality check (should be ~60 for 1-minute intervals)
if(count("kwConsumption", "1h") < 30, 1, 0)
```

### `minWindow(variable, timeWindow)`
Get minimum value in a time window.

**Example:**
```javascript
// Minimum voltage in last 24 hours
minWindow("voltage", "24h")

// Voltage sag detection
if(minWindow("voltage", "1h") < 200, 1, 0)
```

### `maxWindow(variable, timeWindow)`
Get maximum value in a time window.

**Example:**
```javascript
// Maximum temperature in last 7 days
maxWindow("temperature", "7d")

// Peak detection
if(temperature >= maxWindow("temperature", "24h"), 1, 0)
```

### `rateOfChange(variable, timeWindow)`
Calculate rate of change (per second) over a time window.

**Example:**
```javascript
// Temperature change rate (°F per second)
rateOfChange("temperature", "15m")

// Rapid temperature increase alert
if(rateOfChange("temperature", "5m") > 0.1, 1, 0)
```

### `delta(variable)`
Get difference from previous reading.

**Example:**
```javascript
// Change in consumption from last reading
delta("kwConsumption")

// Sudden jump detection
if(abs(delta("kwConsumption")) > 50, 1, 0)
```

## Practical Examples

### 1. Adaptive Spike Detection
```javascript
// Alert if current > 1.5x rolling average
if(kwConsumption > avg("kwConsumption", "1h") * 1.5, 1, 0)
```

### 2. Anomaly Detection (Z-Score)
```javascript
// Z-score calculation: (current - mean) / stddev
abs(temperature - avg("temperature", "24h")) / stddev("temperature", "24h")

// Alert if > 2 standard deviations
if(abs(temperature - avg("temperature", "24h")) / stddev("temperature", "24h") > 2, 1, 0)
```

### 3. Daily Energy Cost Prediction
```javascript
// Predict daily cost based on hourly average
avg("kwConsumption", "1h") * 24 * 0.15
```

### 4. Equipment Degradation Monitoring
```javascript
// Compare current efficiency to 30-day baseline
round((voltage * current) / avg("kwConsumption", "30d") * 100)
```

### 5. Peak Demand Tracking
```javascript
// Current load as % of 7-day peak
round(kwConsumption / maxWindow("kwConsumption", "7d") * 100)
```

### 6. Temperature Trend Analysis
```javascript
// Rising temperature alert (>0.05°F per second over 15 min)
if(rateOfChange("temperature", "15m") > 0.05, 1, 0)
```

### 7. Data Quality Score
```javascript
// Percentage of expected readings received (1-min intervals)
round(count("kwConsumption", "1h") / 60 * 100)
```

### 8. Voltage Stability Index
```javascript
// Lower stddev = more stable
100 - stddev("voltage", "24h")
```

### 9. Consumption Percentile Rank
```javascript
// Is current consumption in top 5%?
if(kwConsumption >= percentile("kwConsumption", 95, "30d"), 1, 0)
```

### 10. Load Factor Calculation
```javascript
// Load factor = avg / peak
round(avg("kwConsumption", "24h") / maxWindow("kwConsumption", "24h") * 100)
```

## Implementation Status

**Phase 2 - Sprint 4 (In Progress)**:
- ✅ TimeWindow enum (5m, 15m, 1h, 6h, 12h, 24h, 7d, 30d)
- ✅ TelemetryRepository time-based queries
- ✅ StatisticalFunctions class with 9 functions
- ✅ ExpressionContext for device/timestamp injection
- ⏳ Function registry integration (pending)
- ⏳ Expression evaluator context support (pending)
- ⏳ Unit tests (pending)

**Note**: Statistical functions require additional context (deviceId, timestamp) and are not yet fully integrated into the expression evaluator. Full integration will be completed in Sprint 4 Phase 2 continuation.

## Performance Considerations

### Query Optimization
- Statistical functions query historical data, which can be expensive
- Use appropriate time windows (shorter = faster)
- Consider caching for frequently-used calculations

### Best Practices
1. **Choose appropriate time windows**:
   - Recent trends: 5m, 15m, 1h
   - Daily patterns: 24h
   - Long-term baselines: 7d, 30d

2. **Limit nested statistical calls**:
   - ✅ Good: `avg("kwConsumption", "1h")`
   - ❌ Avoid: `avg(avg("kwConsumption", "1h"), "24h")` (nested not supported)

3. **Combine with simple functions**:
   ```javascript
   // Efficient: statistical + math
   round(avg("kwConsumption", "1h") * 24)
   ```

4. **Data quality checks**:
   ```javascript
   // Ensure enough data before calculating statistics
   if(count("temperature", "1h") > 10,
     avg("temperature", "1h"),
     temperature)  // Fall back to current value
   ```

## Coming Soon

### Advanced Statistical Functions
- `median(variable, timeWindow)` - Median value
- `mode(variable, timeWindow)` - Most frequent value
- `variance(variable, timeWindow)` - Variance
- `range(variable, timeWindow)` - Max - Min
- `movingAvg(variable, points)` - Simple moving average over N points
- `ewma(variable, alpha, timeWindow)` - Exponentially weighted moving average

### Correlation & Comparison
- `corr(var1, var2, timeWindow)` - Correlation coefficient between two variables
- `covariance(var1, var2, timeWindow)` - Covariance

### Time-Series Specific
- `slope(variable, timeWindow)` - Linear regression slope
- `forecast(variable, timeWindow, steps)` - Simple linear forecast

---

**Last Updated**: 2025-11-06 (Sprint 4 Phase 2 - In Progress)
**Status**: Infrastructure Complete, Integration Pending
