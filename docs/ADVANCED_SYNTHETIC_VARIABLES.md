# Advanced Synthetic Variables

Comprehensive guide to using advanced mathematical expressions, conditional logic, and statistical functions in SensorVision synthetic variables.

## Table of Contents
1. [Overview](#overview)
2. [Basic Arithmetic](#basic-arithmetic)
3. [Math Functions](#math-functions)
4. [Comparison Operators](#comparison-operators)
5. [Conditional Logic](#conditional-logic)
6. [Practical Examples](#practical-examples)
7. [Best Practices](#best-practices)

---

## Overview

Synthetic variables allow you to create derived metrics from raw telemetry data using mathematical expressions. As of Sprint 4, the expression engine supports:

- **Basic Arithmetic**: `+`, `-`, `*`, `/`, `()`
- **17 Math Functions**: `sqrt`, `pow`, `abs`, `log`, `sin`, `cos`, `round`, etc.
- **Comparison Operators**: `>`, `<`, `>=`, `<=`, `==`, `!=`
- **Conditional Logic**: `if`, `and`, `or`, `not`
- **Variable References**: Reference any telemetry variable by name

---

## Basic Arithmetic

### Supported Operators
- **Addition**: `voltage + current`
- **Subtraction**: `voltage - 220`
- **Multiplication**: `voltage * current`
- **Division**: `kwConsumption / voltage`
- **Parentheses**: `(voltage + current) * 2`

### Operator Precedence
1. Parentheses `()`
2. Multiplication `/` and Division `/`
3. Addition `+` and Subtraction `-`

### Example
```javascript
// Apparent Power (VA) = Voltage × Current
voltage * current

// Power Factor Calculation
kwConsumption / (voltage * current)

// Complex Expression
(voltage - 220) / 220 * 100
```

---

## Math Functions

### Algebraic Functions

#### `sqrt(x)` - Square Root
```javascript
// Root Mean Square Current
sqrt(pow(current, 2))

// Pythagorean Theorem
sqrt(pow(voltage, 2) + pow(current, 2))
```

#### `pow(base, exponent)` - Power
```javascript
// Square of voltage
pow(voltage, 2)

// Cube of current
pow(current, 3)
```

#### `abs(x)` - Absolute Value
```javascript
// Absolute difference from target
abs(voltage - 220)

// Deviation magnitude
abs(kwConsumption - 100)
```

### Logarithmic Functions

#### `log(x)` - Natural Logarithm
```javascript
// Natural log of consumption
log(kwConsumption)
```

#### `log10(x)` - Base-10 Logarithm
```javascript
// Decibel calculation
10 * log10(voltage / 1)
```

#### `exp(x)` - Exponential (e^x)
```javascript
// Exponential growth model
exp(kwConsumption / 100)
```

### Trigonometric Functions

#### `sin(x)`, `cos(x)`, `tan(x)` - Trigonometric (radians)
```javascript
// Phase shift calculation
sin(frequency * 3.14159 / 180)

// Cosine of phase angle
cos(voltage / 180 * 3.14159)
```

#### `asin(x)`, `acos(x)`, `atan(x)` - Inverse Trigonometric
```javascript
// Calculate phase angle
atan(current / voltage)
```

### Rounding Functions

#### `round(x)` - Round to Nearest Integer
```javascript
// Round voltage to whole number
round(voltage)

// Round power calculation
round(voltage * current)
```

#### `floor(x)` - Round Down
```javascript
// Voltage bucket (e.g., 220.7 -> 220)
floor(voltage)
```

#### `ceil(x)` - Round Up
```javascript
// Ceiling of consumption (e.g., 50.1 -> 51)
ceil(kwConsumption)
```

### Comparison Functions

#### `min(x, y, ...)` - Minimum Value
```javascript
// Minimum of three voltages
min(voltage, 220, 240)

// Clamp maximum
min(kwConsumption, 100)
```

#### `max(x, y, ...)` - Maximum Value
```javascript
// Maximum consumption
max(kwConsumption, 0)

// Choose higher value
max(voltage, 220)
```

---

## Comparison Operators

Comparison operators return `1` for true, `0` for false.

### Supported Operators
- **Greater Than**: `>`
- **Less Than**: `<`
- **Greater Than or Equal**: `>=`
- **Less Than or Equal**: `<=`
- **Equal**: `==`
- **Not Equal**: `!=`

### Examples
```javascript
// Is voltage greater than 230?
voltage > 230  // Returns 1 if true, 0 if false

// Is consumption within range?
kwConsumption >= 50  // Returns 1 if >= 50

// Is temperature exactly 75?
temperature == 75  // Returns 1 if equal

// Is voltage not nominal?
voltage != 220  // Returns 1 if not equal
```

---

## Conditional Logic

### `if(condition, trueValue, falseValue)`

Returns `trueValue` if condition is non-zero (true), otherwise returns `falseValue`.

#### Simple Conditional
```javascript
// Alert flag: 1 if over threshold, 0 otherwise
if(temperature > 80, 1, 0)
```

#### Conditional Calculation
```javascript
// Penalty if over limit
if(kwConsumption > 100, kwConsumption * 1.5, kwConsumption)
```

#### Nested Conditionals
```javascript
// Three-tier pricing
if(kwConsumption < 50, kwConsumption * 0.10,
  if(kwConsumption < 100, kwConsumption * 0.15,
    kwConsumption * 0.20))
```

### `and(x, y, ...)` - Logical AND

Returns `1` if all arguments are non-zero, otherwise `0`.

```javascript
// Both conditions must be true
and(voltage > 220, current > 5)

// Three conditions
and(voltage > 220, current > 5, temperature < 85)
```

### `or(x, y, ...)` - Logical OR

Returns `1` if any argument is non-zero, otherwise `0`.

```javascript
// Either condition triggers alert
or(voltage > 230, voltage < 210)

// Multiple alert conditions
or(temperature > 85, current > 10, voltage < 200)
```

### `not(x)` - Logical NOT

Returns `1` if argument is zero, `0` if non-zero.

```javascript
// Invert condition
not(voltage > 220)  // True if voltage <= 220

// Device offline flag
not(current)  // Returns 1 if current is 0
```

---

## Practical Examples

### 1. Equipment Health Score
```javascript
// Calculate health score (0-100)
if(and(temperature < 80, voltage > 210, voltage < 230, current < 10),
  100,  // Perfect health
  if(or(temperature > 90, voltage < 200, voltage > 240, current > 15),
    30,   // Poor health
    70))  // Moderate health
```

### 2. Energy Efficiency Ratio
```javascript
// Efficiency = Actual Output / Ideal Output * 100
round((voltage * current * powerFactor) / (220 * 5 * 1.0) * 100)
```

### 3. Spike Detection
```javascript
// Flag consumption spikes (1.5x baseline)
if(kwConsumption > 80 * 1.5, 1, 0)
```

### 4. Time-of-Use Cost Calculation
```javascript
// Off-peak (6am-5pm): $0.15/kWh, Peak (5pm-10pm): $0.35/kWh
// Note: hour variable would need to be added to telemetry
if(and(hour >= 17, hour < 22),
  kwConsumption * 0.35,  // Peak rate
  kwConsumption * 0.15)  // Off-peak rate
```

### 5. Voltage Deviation Percentage
```javascript
// How far is voltage from nominal (220V)?
round(abs(voltage - 220) / 220 * 100)
```

### 6. Power Quality Index
```javascript
// Combine multiple factors into single quality score
round(
  if(and(voltage >= 215, voltage <= 225), 40, 0) +  // Voltage stability (40 pts)
  if(powerFactor >= 0.95, 30, 0) +                 // Power factor (30 pts)
  if(frequency >= 59.5, frequency <= 60.5, 30, 0))  // Frequency stability (30 pts)
```

### 7. Load Balancing Metric
```javascript
// Compare current load to capacity
round(current / 10 * 100)  // Assuming 10A capacity
```

### 8. Overvoltage Protection Trigger
```javascript
// Trigger if voltage exceeds 240V or drops below 200V
or(voltage > 240, voltage < 200)
```

### 9. Energy Cost Prediction (Linear)
```javascript
// Predict daily cost based on hourly consumption
round(kwConsumption * 24 * 0.15)
```

### 10. Harmonic Distortion Estimation
```javascript
// Simplified THD calculation placeholder
// (Would require actual harmonic data in telemetry)
sqrt(pow(current, 2) - pow(current * powerFactor, 2)) / current * 100
```

---

## Best Practices

### 1. **Use Clear Variable Names**
```javascript
// Good
temperatureDeviation

// Better
abs(temperature - 75)
```

### 2. **Add Units in Description**
When creating synthetic variables, always specify the unit:
- Name: `powerEfficiencyRatio`
- Description: "Power efficiency as percentage"
- Unit: `%`
- Expression: `round(kwConsumption / (voltage * current) * 100)`

### 3. **Handle Edge Cases**
```javascript
// Avoid division by zero
if(voltage == 0, 0, kwConsumption / voltage)

// Clamp values to valid range
max(0, min(100, calculatedPercentage))
```

### 4. **Break Complex Expressions into Multiple Variables**
Instead of one huge expression, create intermediate synthetic variables:

1. **Variable 1**: `apparentPower = voltage * current`
2. **Variable 2**: `realPower = kwConsumption`
3. **Variable 3**: `powerFactor = realPower / apparentPower`

### 5. **Use Rounding Appropriately**
```javascript
// Round final results for readability
round(voltage * current)

// But keep precision for intermediate calculations
(round(voltage) * round(current))  // Less accurate
```

### 6. **Test Expressions Before Deploying**
- Use the expression preview feature in the UI
- Test with representative data samples
- Verify edge cases (zero values, negatives, extremes)

### 7. **Document Complex Logic**
Use the Description field to explain what the expression does:
```
Expression: if(and(temperature > 80, humidity > 60), 1, 0)
Description: Heat index warning flag - triggers when both temperature > 80°F and humidity > 60%
```

---

## Performance Considerations

### Efficient Expressions
- ✅ **Good**: `voltage * current`
- ✅ **Good**: `if(temperature > 80, 1, 0)`
- ❌ **Avoid**: Deeply nested conditionals (> 5 levels)
- ❌ **Avoid**: Redundant calculations

### Caching
Synthetic variables are calculated once per telemetry ingestion. Reuse the calculated values instead of duplicating expressions.

---

## Function Reference Quick Table

| Function | Description | Example |
|----------|-------------|---------|
| `sqrt(x)` | Square root | `sqrt(16)` → 4 |
| `pow(b,e)` | Power | `pow(2,3)` → 8 |
| `abs(x)` | Absolute value | `abs(-5)` → 5 |
| `log(x)` | Natural log | `log(2.718)` → 1 |
| `log10(x)` | Base-10 log | `log10(100)` → 2 |
| `exp(x)` | Exponential | `exp(1)` → 2.718 |
| `sin(x)` | Sine (radians) | `sin(0)` → 0 |
| `cos(x)` | Cosine (radians) | `cos(0)` → 1 |
| `tan(x)` | Tangent (radians) | `tan(0)` → 0 |
| `round(x)` | Round to integer | `round(3.7)` → 4 |
| `floor(x)` | Round down | `floor(3.9)` → 3 |
| `ceil(x)` | Round up | `ceil(3.1)` → 4 |
| `min(...)` | Minimum | `min(1,2,3)` → 1 |
| `max(...)` | Maximum | `max(1,2,3)` → 3 |
| `if(c,t,f)` | Conditional | `if(x>5,1,0)` |
| `and(...)` | Logical AND | `and(1,1,0)` → 0 |
| `or(...)` | Logical OR | `or(0,1,0)` → 1 |
| `not(x)` | Logical NOT | `not(0)` → 1 |

---

## Support

For questions or issues with synthetic variables:
- Create a support ticket in the platform
- Check the [Expression Function API](/api/v1/expression-functions) for the latest function list
- Review test cases in `ExpressionEvaluatorTest.java`

---

**Last Updated**: 2025-11-06 (Sprint 4 Phase 1)
**Version**: 1.0.0
