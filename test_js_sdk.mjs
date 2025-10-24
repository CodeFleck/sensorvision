/**
 * Test script for JavaScript SDK
 *
 * Run with: node test_js_sdk.mjs
 */

import { createRequire } from 'module';
const require = createRequire(import.meta.url);

// Import from the source files directly
const { SensorVisionClient } = require('./sensorvision-sdk-js/src/client.ts');

const API_URL = 'http://localhost:8080';
const API_KEY = '550e8400-e29b-41d4-a716-446655440000';

async function testJavaScriptSDK() {
    console.log('Testing JavaScript SDK - HTTP Client');
    console.log('='.repeat(50));

    const client = new SensorVisionClient({
        apiUrl: API_URL,
        apiKey: API_KEY,
        timeout: 10000
    });

    // Test 1: Send temperature and humidity
    console.log('\n1. Sending temperature and humidity data...');
    try {
        const response = await client.sendData('js-sdk-test-001', {
            temperature: 25.3,
            humidity: 62.8
        });
        console.log(`   [OK] Success: ${response.message}`);
    } catch (error) {
        console.log(`   [ERROR] ${error.message}`);
        return false;
    }

    // Test 2: Send multiple sensor readings
    console.log('\n2. Sending multiple sensor readings...');
    try {
        const response = await client.sendData('js-sdk-test-002', {
            temperature: 24.5,
            humidity: 70.2,
            pressure: 1015.5,
            co2_ppm: 425,
            light_level: 920
        });
        console.log(`   [OK] Success: ${response.message}`);
    } catch (error) {
        console.log(`   [ERROR] ${error.message}`);
        return false;
    }

    // Test 3: Send power monitoring data
    console.log('\n3. Sending power monitoring data...');
    try {
        const response = await client.sendData('js-sdk-test-003', {
            voltage: 230.2,
            current: 1.25,
            power_kw: 0.288
        });
        console.log(`   [OK] Success: ${response.message}`);
    } catch (error) {
        console.log(`   [ERROR] ${error.message}`);
        return false;
    }

    console.log('\n' + '='.repeat(50));
    console.log('[SUCCESS] All JavaScript SDK tests passed!');
    return true;
}

// Run tests
testJavaScriptSDK()
    .then(success => {
        process.exit(success ? 0 : 1);
    })
    .catch(error => {
        console.error('[FATAL]', error);
        process.exit(1);
    });
