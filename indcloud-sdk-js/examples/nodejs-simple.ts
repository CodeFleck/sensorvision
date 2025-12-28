/**
 * Simple Node.js example - Send telemetry data
 *
 * Run with: ts-node examples/nodejs-simple.ts
 * Or compile and run: npm run build && node dist/examples/nodejs-simple.js
 */

import { IndCloudClient } from '../src';

const client = new IndCloudClient({
  apiUrl: 'http://localhost:8080',
  apiKey: 'your-device-token',  // Replace with your actual API key
});

async function main() {
  try {
    console.log('Sending telemetry data...');

    const response = await client.sendData('sensor-001', {
      temperature: 23.5,
      humidity: 65.2,
      pressure: 1013.25,
    });

    console.log('Success!', response);
  } catch (error) {
    console.error('Error:', error);
    process.exit(1);
  }
}

main();
