-- V65: Seed pre-built device type templates
-- These are system templates available to all organizations

-- First, we need a system organization for system templates
-- Check if 'System' organization exists, if not create it
INSERT INTO organizations (name, description, created_at, updated_at)
SELECT 'System', 'System organization for global templates', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM organizations WHERE name = 'System');

-- Get the system organization ID for use in device types
-- We'll use a DO block to handle this properly

DO $$
DECLARE
    system_org_id BIGINT;
    smart_meter_id BIGINT;
    env_sensor_id BIGINT;
    industrial_pump_id BIGINT;
    hvac_id BIGINT;
    solar_inverter_id BIGINT;
BEGIN
    -- Get or create system organization
    SELECT id INTO system_org_id FROM organizations WHERE name = 'System' LIMIT 1;

    -- 1. Smart Meter Template
    INSERT INTO device_types (name, description, icon, organization_id, is_active, is_system_template, template_category, color, created_at, updated_at)
    VALUES (
        'Smart Meter',
        'Electric power meter with real-time consumption monitoring. Tracks kWh, voltage, current, power factor, and generates alerts for abnormal usage patterns.',
        'Zap',
        system_org_id,
        true,
        true,
        'ENERGY',
        '#F59E0B',
        NOW(),
        NOW()
    ) RETURNING id INTO smart_meter_id;

    -- Smart Meter Variables
    INSERT INTO device_type_variables (device_type_id, name, label, unit, data_type, min_value, max_value, required, description, display_order) VALUES
    (smart_meter_id, 'kw_consumption', 'Power Consumption', 'kW', 'NUMBER', 0, 1000, true, 'Real-time power consumption in kilowatts', 1),
    (smart_meter_id, 'voltage', 'Voltage', 'V', 'NUMBER', 0, 500, true, 'Line voltage in volts', 2),
    (smart_meter_id, 'current', 'Current', 'A', 'NUMBER', 0, 100, true, 'Current draw in amperes', 3),
    (smart_meter_id, 'power_factor', 'Power Factor', '', 'NUMBER', 0, 1, false, 'Power factor (0-1)', 4),
    (smart_meter_id, 'frequency', 'Frequency', 'Hz', 'NUMBER', 45, 65, false, 'AC frequency in hertz', 5),
    (smart_meter_id, 'total_kwh', 'Total Energy', 'kWh', 'NUMBER', 0, NULL, false, 'Cumulative energy consumption', 6);

    -- Smart Meter Rule Templates
    INSERT INTO device_type_rule_templates (device_type_id, name, description, variable_name, operator, threshold_value, severity, notification_message, enabled, display_order) VALUES
    (smart_meter_id, 'High Power Usage', 'Alerts when power consumption exceeds normal threshold', 'kw_consumption', 'GT', 500, 'WARNING', 'Power consumption exceeded 500 kW', true, 1),
    (smart_meter_id, 'Critical Power Usage', 'Alerts when power consumption reaches critical levels', 'kw_consumption', 'GT', 800, 'CRITICAL', 'CRITICAL: Power consumption exceeded 800 kW', true, 2),
    (smart_meter_id, 'Low Voltage', 'Alerts when voltage drops below safe operating range', 'voltage', 'LT', 200, 'WARNING', 'Low voltage detected', true, 3),
    (smart_meter_id, 'High Voltage', 'Alerts when voltage exceeds safe operating range', 'voltage', 'GT', 250, 'WARNING', 'High voltage detected', true, 4);

    -- Smart Meter Dashboard Templates
    INSERT INTO device_type_dashboard_templates (device_type_id, widget_type, title, variable_name, grid_x, grid_y, grid_width, grid_height, display_order) VALUES
    (smart_meter_id, 'LINE_CHART', 'Power Consumption', 'kw_consumption', 0, 0, 8, 4, 1),
    (smart_meter_id, 'GAUGE', 'Current Power', 'kw_consumption', 8, 0, 4, 4, 2),
    (smart_meter_id, 'METRIC', 'Voltage', 'voltage', 0, 4, 4, 2, 3),
    (smart_meter_id, 'METRIC', 'Current', 'current', 4, 4, 4, 2, 4),
    (smart_meter_id, 'METRIC', 'Total Energy', 'total_kwh', 8, 4, 4, 2, 5);

    -- 2. Environmental Sensor Template
    INSERT INTO device_types (name, description, icon, organization_id, is_active, is_system_template, template_category, color, created_at, updated_at)
    VALUES (
        'Environmental Sensor',
        'Multi-sensor device for environmental monitoring. Tracks temperature, humidity, air quality (CO2, PM2.5), pressure, and light levels.',
        'Thermometer',
        system_org_id,
        true,
        true,
        'ENVIRONMENTAL',
        '#10B981',
        NOW(),
        NOW()
    ) RETURNING id INTO env_sensor_id;

    -- Environmental Sensor Variables
    INSERT INTO device_type_variables (device_type_id, name, label, unit, data_type, min_value, max_value, required, description, display_order) VALUES
    (env_sensor_id, 'temperature', 'Temperature', '°C', 'NUMBER', -40, 80, true, 'Ambient temperature in Celsius', 1),
    (env_sensor_id, 'humidity', 'Humidity', '%', 'NUMBER', 0, 100, true, 'Relative humidity percentage', 2),
    (env_sensor_id, 'pressure', 'Atmospheric Pressure', 'hPa', 'NUMBER', 900, 1100, false, 'Atmospheric pressure in hectopascals', 3),
    (env_sensor_id, 'co2', 'CO2 Level', 'ppm', 'NUMBER', 0, 5000, false, 'Carbon dioxide concentration in parts per million', 4),
    (env_sensor_id, 'pm25', 'PM2.5', 'µg/m³', 'NUMBER', 0, 500, false, 'Fine particulate matter concentration', 5),
    (env_sensor_id, 'light', 'Light Level', 'lux', 'NUMBER', 0, 100000, false, 'Illumination in lux', 6);

    -- Environmental Sensor Rule Templates
    INSERT INTO device_type_rule_templates (device_type_id, name, description, variable_name, operator, threshold_value, severity, notification_message, enabled, display_order) VALUES
    (env_sensor_id, 'High Temperature', 'Alerts when temperature exceeds comfortable range', 'temperature', 'GT', 30, 'WARNING', 'High temperature detected', true, 1),
    (env_sensor_id, 'Low Temperature', 'Alerts when temperature drops below comfortable range', 'temperature', 'LT', 15, 'WARNING', 'Low temperature detected', true, 2),
    (env_sensor_id, 'High Humidity', 'Alerts when humidity exceeds comfortable range', 'humidity', 'GT', 70, 'WARNING', 'High humidity detected', true, 3),
    (env_sensor_id, 'Poor Air Quality', 'Alerts when CO2 levels exceed safe limits', 'co2', 'GT', 1000, 'WARNING', 'Poor air quality - high CO2 levels', true, 4);

    -- Environmental Sensor Dashboard Templates
    INSERT INTO device_type_dashboard_templates (device_type_id, widget_type, title, variable_name, grid_x, grid_y, grid_width, grid_height, display_order) VALUES
    (env_sensor_id, 'LINE_CHART', 'Temperature History', 'temperature', 0, 0, 6, 4, 1),
    (env_sensor_id, 'LINE_CHART', 'Humidity History', 'humidity', 6, 0, 6, 4, 2),
    (env_sensor_id, 'GAUGE', 'Current Temperature', 'temperature', 0, 4, 4, 3, 3),
    (env_sensor_id, 'GAUGE', 'Current Humidity', 'humidity', 4, 4, 4, 3, 4),
    (env_sensor_id, 'METRIC', 'CO2 Level', 'co2', 8, 4, 4, 3, 5);

    -- 3. Industrial Pump Template
    INSERT INTO device_types (name, description, icon, organization_id, is_active, is_system_template, template_category, color, created_at, updated_at)
    VALUES (
        'Industrial Pump',
        'Industrial pump monitoring with vibration analysis, flow rate, pressure, and motor temperature. Supports predictive maintenance alerts.',
        'Activity',
        system_org_id,
        true,
        true,
        'INDUSTRIAL',
        '#6366F1',
        NOW(),
        NOW()
    ) RETURNING id INTO industrial_pump_id;

    -- Industrial Pump Variables
    INSERT INTO device_type_variables (device_type_id, name, label, unit, data_type, min_value, max_value, required, description, display_order) VALUES
    (industrial_pump_id, 'flow_rate', 'Flow Rate', 'L/min', 'NUMBER', 0, 10000, true, 'Fluid flow rate in liters per minute', 1),
    (industrial_pump_id, 'pressure', 'Pressure', 'bar', 'NUMBER', 0, 100, true, 'Operating pressure in bar', 2),
    (industrial_pump_id, 'vibration', 'Vibration', 'mm/s', 'NUMBER', 0, 50, true, 'Vibration velocity in mm/s RMS', 3),
    (industrial_pump_id, 'motor_temp', 'Motor Temperature', '°C', 'NUMBER', 0, 150, true, 'Motor winding temperature', 4),
    (industrial_pump_id, 'rpm', 'Rotation Speed', 'RPM', 'NUMBER', 0, 5000, false, 'Motor rotation speed', 5),
    (industrial_pump_id, 'run_hours', 'Operating Hours', 'hrs', 'NUMBER', 0, NULL, false, 'Cumulative operating hours', 6);

    -- Industrial Pump Rule Templates
    INSERT INTO device_type_rule_templates (device_type_id, name, description, variable_name, operator, threshold_value, severity, notification_message, enabled, display_order) VALUES
    (industrial_pump_id, 'High Vibration', 'Alerts when vibration exceeds normal levels (bearing wear)', 'vibration', 'GT', 10, 'WARNING', 'High vibration detected - check bearings', true, 1),
    (industrial_pump_id, 'Critical Vibration', 'Alerts when vibration reaches critical levels', 'vibration', 'GT', 25, 'CRITICAL', 'CRITICAL: Dangerous vibration levels - shut down recommended', true, 2),
    (industrial_pump_id, 'Motor Overheating', 'Alerts when motor temperature exceeds safe limits', 'motor_temp', 'GT', 80, 'WARNING', 'Motor overheating detected', true, 3),
    (industrial_pump_id, 'Low Flow Rate', 'Alerts when flow rate drops below expected', 'flow_rate', 'LT', 100, 'WARNING', 'Low flow rate - check for blockage', true, 4);

    -- Industrial Pump Dashboard Templates
    INSERT INTO device_type_dashboard_templates (device_type_id, widget_type, title, variable_name, grid_x, grid_y, grid_width, grid_height, display_order) VALUES
    (industrial_pump_id, 'LINE_CHART', 'Vibration Trend', 'vibration', 0, 0, 6, 4, 1),
    (industrial_pump_id, 'LINE_CHART', 'Flow Rate', 'flow_rate', 6, 0, 6, 4, 2),
    (industrial_pump_id, 'GAUGE', 'Pressure', 'pressure', 0, 4, 4, 3, 3),
    (industrial_pump_id, 'GAUGE', 'Motor Temp', 'motor_temp', 4, 4, 4, 3, 4),
    (industrial_pump_id, 'METRIC', 'Operating Hours', 'run_hours', 8, 4, 4, 3, 5);

    -- 4. HVAC System Template
    INSERT INTO device_types (name, description, icon, organization_id, is_active, is_system_template, template_category, color, created_at, updated_at)
    VALUES (
        'HVAC System',
        'Heating, ventilation, and air conditioning system monitoring. Tracks temperature setpoints, actual values, energy consumption, and filter status.',
        'Wind',
        system_org_id,
        true,
        true,
        'SMART_HOME',
        '#06B6D4',
        NOW(),
        NOW()
    ) RETURNING id INTO hvac_id;

    -- HVAC Variables
    INSERT INTO device_type_variables (device_type_id, name, label, unit, data_type, min_value, max_value, required, description, display_order) VALUES
    (hvac_id, 'temperature', 'Room Temperature', '°C', 'NUMBER', 0, 50, true, 'Current room temperature', 1),
    (hvac_id, 'setpoint', 'Temperature Setpoint', '°C', 'NUMBER', 16, 30, true, 'Target temperature setting', 2),
    (hvac_id, 'humidity', 'Humidity', '%', 'NUMBER', 0, 100, false, 'Room humidity level', 3),
    (hvac_id, 'fan_speed', 'Fan Speed', '%', 'NUMBER', 0, 100, false, 'Fan speed percentage', 4),
    (hvac_id, 'power_consumption', 'Power Usage', 'kW', 'NUMBER', 0, 50, false, 'Current power consumption', 5),
    (hvac_id, 'mode', 'Operating Mode', '', 'STRING', NULL, NULL, false, 'Current mode: heating/cooling/fan/off', 6);

    -- HVAC Rule Templates
    INSERT INTO device_type_rule_templates (device_type_id, name, description, variable_name, operator, threshold_value, severity, notification_message, enabled, display_order) VALUES
    (hvac_id, 'Temperature Deviation', 'Alerts when room temp differs significantly from setpoint', 'temperature', 'GT', 28, 'WARNING', 'Temperature exceeds comfort range', true, 1),
    (hvac_id, 'High Energy Usage', 'Alerts when power consumption is unusually high', 'power_consumption', 'GT', 20, 'INFO', 'High HVAC energy consumption', true, 2);

    -- HVAC Dashboard Templates
    INSERT INTO device_type_dashboard_templates (device_type_id, widget_type, title, variable_name, grid_x, grid_y, grid_width, grid_height, display_order) VALUES
    (hvac_id, 'LINE_CHART', 'Temperature vs Setpoint', 'temperature', 0, 0, 8, 4, 1),
    (hvac_id, 'GAUGE', 'Current Temp', 'temperature', 8, 0, 4, 4, 2),
    (hvac_id, 'METRIC', 'Setpoint', 'setpoint', 0, 4, 4, 2, 3),
    (hvac_id, 'METRIC', 'Humidity', 'humidity', 4, 4, 4, 2, 4),
    (hvac_id, 'METRIC', 'Power Usage', 'power_consumption', 8, 4, 4, 2, 5);

    -- 5. Solar Inverter Template
    INSERT INTO device_types (name, description, icon, organization_id, is_active, is_system_template, template_category, color, created_at, updated_at)
    VALUES (
        'Solar Inverter',
        'Photovoltaic inverter monitoring. Tracks DC input, AC output, efficiency, temperature, and energy production. Supports grid-tie and off-grid configurations.',
        'Sun',
        system_org_id,
        true,
        true,
        'ENERGY',
        '#FBBF24',
        NOW(),
        NOW()
    ) RETURNING id INTO solar_inverter_id;

    -- Solar Inverter Variables
    INSERT INTO device_type_variables (device_type_id, name, label, unit, data_type, min_value, max_value, required, description, display_order) VALUES
    (solar_inverter_id, 'dc_power', 'DC Power Input', 'W', 'NUMBER', 0, 100000, true, 'DC power from solar panels', 1),
    (solar_inverter_id, 'ac_power', 'AC Power Output', 'W', 'NUMBER', 0, 100000, true, 'AC power output to grid/load', 2),
    (solar_inverter_id, 'efficiency', 'Efficiency', '%', 'NUMBER', 0, 100, false, 'Conversion efficiency', 3),
    (solar_inverter_id, 'dc_voltage', 'DC Voltage', 'V', 'NUMBER', 0, 1000, false, 'Input DC voltage from panels', 4),
    (solar_inverter_id, 'temperature', 'Inverter Temp', '°C', 'NUMBER', 0, 100, false, 'Inverter internal temperature', 5),
    (solar_inverter_id, 'daily_energy', 'Daily Energy', 'kWh', 'NUMBER', 0, NULL, false, 'Energy produced today', 6),
    (solar_inverter_id, 'total_energy', 'Total Energy', 'kWh', 'NUMBER', 0, NULL, false, 'Cumulative energy produced', 7);

    -- Solar Inverter Rule Templates
    INSERT INTO device_type_rule_templates (device_type_id, name, description, variable_name, operator, threshold_value, severity, notification_message, enabled, display_order) VALUES
    (solar_inverter_id, 'Low Efficiency', 'Alerts when inverter efficiency drops below expected', 'efficiency', 'LT', 90, 'WARNING', 'Low inverter efficiency - check for issues', true, 1),
    (solar_inverter_id, 'High Temperature', 'Alerts when inverter temperature exceeds safe limits', 'temperature', 'GT', 65, 'WARNING', 'Inverter temperature high - check ventilation', true, 2),
    (solar_inverter_id, 'Critical Temperature', 'Alerts when inverter reaches thermal shutdown threshold', 'temperature', 'GT', 80, 'CRITICAL', 'CRITICAL: Inverter overheating - shutdown imminent', true, 3);

    -- Solar Inverter Dashboard Templates
    INSERT INTO device_type_dashboard_templates (device_type_id, widget_type, title, variable_name, grid_x, grid_y, grid_width, grid_height, display_order) VALUES
    (solar_inverter_id, 'LINE_CHART', 'Power Output', 'ac_power', 0, 0, 8, 4, 1),
    (solar_inverter_id, 'GAUGE', 'Current Power', 'ac_power', 8, 0, 4, 4, 2),
    (solar_inverter_id, 'METRIC', 'Daily Energy', 'daily_energy', 0, 4, 4, 2, 3),
    (solar_inverter_id, 'METRIC', 'Total Energy', 'total_energy', 4, 4, 4, 2, 4),
    (solar_inverter_id, 'GAUGE', 'Efficiency', 'efficiency', 8, 4, 4, 2, 5);

END $$;

-- Add comments
COMMENT ON TABLE device_types IS 'Device type templates for auto-provisioning. System templates (is_system_template=true) are available to all organizations.';
