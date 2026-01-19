-- Mock Data Seeder for AI Assistant Testing
-- This script creates test devices and telemetry data for a specific user's organization
--
-- Usage:
-- 1. Via psql: psql -h localhost -U indcloud -d indcloud -f seed_mock_data.sql
-- 2. Via docker: docker exec -i indcloud-postgres psql -U indcloud -d indcloud < seed_mock_data.sql

-- Configuration: Set the target user email here
\set target_email 'danielfleck268+01@gmail.com'

-- Start transaction
BEGIN;

-- Step 1: Get the user's organization ID
DO $$
DECLARE
    v_org_id BIGINT;
    v_user_id BIGINT;
    v_device_id UUID;
    v_variable_id BIGINT;
    v_timestamp TIMESTAMPTZ;
    v_device_names TEXT[] := ARRAY['Factory-Sensor-01', 'Factory-Sensor-02', 'Warehouse-Monitor-01', 'HVAC-Unit-Main', 'Production-Line-A'];
    v_device_locations TEXT[] := ARRAY['Building A - Floor 1', 'Building A - Floor 2', 'Warehouse B', 'Mechanical Room', 'Production Hall'];
    v_device_types TEXT[] := ARRAY['smart_meter', 'smart_meter', 'environmental', 'hvac', 'industrial'];
    i INT;
    j INT;
    v_kw NUMERIC;
    v_voltage NUMERIC;
    v_current NUMERIC;
    v_power_factor NUMERIC;
    v_frequency NUMERIC;
    v_temperature NUMERIC;
    v_humidity NUMERIC;
    v_base_hour INT;
BEGIN
    -- Find the user and their organization
    SELECT u.organization_id, u.id INTO v_org_id, v_user_id
    FROM users u
    WHERE u.email = 'danielfleck268+01@gmail.com';

    IF v_org_id IS NULL THEN
        RAISE EXCEPTION 'User with email danielfleck268+01@gmail.com not found';
    END IF;

    RAISE NOTICE 'Found user with organization_id: %, user_id: %', v_org_id, v_user_id;

    -- Step 2: Create test devices for the organization
    FOR i IN 1..array_length(v_device_names, 1) LOOP
        v_device_id := gen_random_uuid();

        -- Insert device (skip if external_id already exists)
        INSERT INTO devices (
            id, external_id, name, description, location, sensor_type, firmware_version,
            status, active, organization_id, health_score, created_at, updated_at
        )
        VALUES (
            v_device_id,
            'test-device-' || i || '-' || substr(md5(random()::text), 1, 8),
            v_device_names[i],
            'Test device for AI assistant demo - ' || v_device_types[i],
            v_device_locations[i],
            v_device_types[i],
            '2.1.' || (i % 5),
            'ONLINE',
            true,
            v_org_id,
            85 + (random() * 15)::INT,
            NOW() - INTERVAL '30 days',
            NOW()
        )
        ON CONFLICT (external_id) DO NOTHING;

        -- Get the device ID (in case it already existed)
        SELECT id INTO v_device_id FROM devices
        WHERE name = v_device_names[i] AND organization_id = v_org_id
        LIMIT 1;

        IF v_device_id IS NULL THEN
            RAISE NOTICE 'Skipping device % - already exists', v_device_names[i];
            CONTINUE;
        END IF;

        RAISE NOTICE 'Created/found device: % (id: %)', v_device_names[i], v_device_id;

        -- Step 3: Create variables for this device
        -- Variable 1: kw_consumption
        INSERT INTO variables (
            organization_id, device_id, name, display_name, description, unit,
            data_type, data_source, is_system_variable, decimal_places, created_at, updated_at
        )
        VALUES (
            v_org_id, v_device_id, 'kw_consumption', 'Power Consumption',
            'Real-time power consumption', 'kW', 'NUMBER', 'auto', false, 2, NOW(), NOW()
        )
        ON CONFLICT DO NOTHING;

        -- Variable 2: voltage
        INSERT INTO variables (
            organization_id, device_id, name, display_name, description, unit,
            data_type, data_source, is_system_variable, decimal_places, created_at, updated_at
        )
        VALUES (
            v_org_id, v_device_id, 'voltage', 'Voltage',
            'Supply voltage', 'V', 'NUMBER', 'auto', false, 1, NOW(), NOW()
        )
        ON CONFLICT DO NOTHING;

        -- Variable 3: current
        INSERT INTO variables (
            organization_id, device_id, name, display_name, description, unit,
            data_type, data_source, is_system_variable, decimal_places, created_at, updated_at
        )
        VALUES (
            v_org_id, v_device_id, 'current', 'Current',
            'Electrical current', 'A', 'NUMBER', 'auto', false, 2, NOW(), NOW()
        )
        ON CONFLICT DO NOTHING;

        -- Variable 4: temperature (for environmental/hvac sensors)
        IF v_device_types[i] IN ('environmental', 'hvac') THEN
            INSERT INTO variables (
                organization_id, device_id, name, display_name, description, unit,
                data_type, data_source, is_system_variable, decimal_places, created_at, updated_at
            )
            VALUES (
                v_org_id, v_device_id, 'temperature', 'Temperature',
                'Ambient temperature', '°C', 'NUMBER', 'auto', false, 1, NOW(), NOW()
            )
            ON CONFLICT DO NOTHING;

            INSERT INTO variables (
                organization_id, device_id, name, display_name, description, unit,
                data_type, data_source, is_system_variable, decimal_places, created_at, updated_at
            )
            VALUES (
                v_org_id, v_device_id, 'humidity', 'Humidity',
                'Relative humidity', '%', 'NUMBER', 'auto', false, 1, NOW(), NOW()
            )
            ON CONFLICT DO NOTHING;
        END IF;

        -- Step 4: Generate 7 days of historical data (every 5 minutes = 2016 data points per device)
        v_timestamp := NOW() - INTERVAL '7 days';

        WHILE v_timestamp < NOW() LOOP
            v_base_hour := EXTRACT(HOUR FROM v_timestamp)::INT;

            -- Generate realistic values based on time of day
            IF v_base_hour BETWEEN 6 AND 8 THEN
                v_kw := 110 + (random() * 20 - 10);  -- Morning ramp-up
            ELSIF v_base_hour BETWEEN 9 AND 16 THEN
                v_kw := 140 + (random() * 30 - 15);  -- Peak hours
            ELSIF v_base_hour BETWEEN 17 AND 21 THEN
                v_kw := 120 + (random() * 25 - 12);  -- Evening
            ELSE
                v_kw := 80 + (random() * 15 - 7);    -- Night (low usage)
            END IF;

            v_voltage := 220 + (random() * 10 - 5);
            v_current := v_kw / v_voltage * 1000;
            v_power_factor := 0.85 + (random() * 0.1 - 0.05);
            v_frequency := 50 + (random() * 0.2 - 0.1);
            v_temperature := 22 + (random() * 6 - 3);
            v_humidity := 45 + (random() * 20 - 10);

            -- Insert variable values for each variable of this device
            FOR v_variable_id IN
                SELECT id FROM variables WHERE device_id = v_device_id
            LOOP
                INSERT INTO variable_values (variable_id, timestamp, value, created_at)
                SELECT
                    v_variable_id,
                    v_timestamp,
                    CASE (SELECT name FROM variables WHERE id = v_variable_id)
                        WHEN 'kw_consumption' THEN v_kw
                        WHEN 'voltage' THEN v_voltage
                        WHEN 'current' THEN v_current
                        WHEN 'power_factor' THEN v_power_factor
                        WHEN 'frequency' THEN v_frequency
                        WHEN 'temperature' THEN v_temperature
                        WHEN 'humidity' THEN v_humidity
                        ELSE random() * 100
                    END,
                    v_timestamp;
            END LOOP;

            -- Also insert into telemetry_records for legacy compatibility
            INSERT INTO telemetry_records (
                id, device_id, measurement_timestamp,
                kw_consumption, voltage, current, power_factor, frequency,
                metadata, created_at, updated_at
            )
            VALUES (
                gen_random_uuid(),
                v_device_id,
                v_timestamp,
                v_kw, v_voltage, v_current, v_power_factor, v_frequency,
                jsonb_build_object(
                    'location', v_device_locations[i],
                    'sensor_type', v_device_types[i],
                    'data_source', 'mock_seeder'
                ),
                v_timestamp, v_timestamp
            );

            v_timestamp := v_timestamp + INTERVAL '5 minutes';
        END LOOP;

        -- Update last_value on variables
        UPDATE variables v
        SET
            last_value = (
                SELECT vv.value
                FROM variable_values vv
                WHERE vv.variable_id = v.id
                ORDER BY vv.timestamp DESC
                LIMIT 1
            ),
            last_value_at = (
                SELECT vv.timestamp
                FROM variable_values vv
                WHERE vv.variable_id = v.id
                ORDER BY vv.timestamp DESC
                LIMIT 1
            ),
            updated_at = NOW()
        WHERE v.device_id = v_device_id;

        -- Update device last_seen_at
        UPDATE devices SET last_seen_at = NOW(), updated_at = NOW() WHERE id = v_device_id;

        RAISE NOTICE 'Generated telemetry data for device: %', v_device_names[i];
    END LOOP;

    RAISE NOTICE 'Mock data seeding completed successfully!';
END $$;

COMMIT;

-- Show summary
SELECT
    'Devices' as entity,
    COUNT(*) as count
FROM devices d
JOIN users u ON d.organization_id = u.organization_id
WHERE u.email = 'danielfleck268+01@gmail.com'
UNION ALL
SELECT
    'Variables',
    COUNT(*)
FROM variables v
JOIN users u ON v.organization_id = u.organization_id
WHERE u.email = 'danielfleck268+01@gmail.com'
AND v.device_id IS NOT NULL
UNION ALL
SELECT
    'Variable Values (last 7 days)',
    COUNT(*)
FROM variable_values vv
JOIN variables v ON vv.variable_id = v.id
JOIN users u ON v.organization_id = u.organization_id
WHERE u.email = 'danielfleck268+01@gmail.com'
AND vv.timestamp > NOW() - INTERVAL '7 days';
