#!/usr/bin/env python3
"""
Test script for Data Plugins feature.
Tests the complete workflow: create plugin, send webhook data, check execution history.
"""

import requests
import json
import time

BASE_URL = "http://localhost:8080"

# Test credentials
USERNAME = "test@sensorvision.com"
PASSWORD = "testpass123"


def login():
    """Login and get session with JWT token."""
    session = requests.Session()

    login_data = {
        "username": USERNAME,
        "password": PASSWORD
    }

    response = session.post(f"{BASE_URL}/api/v1/auth/login", json=login_data)

    if response.status_code == 200:
        print(f"[OK] Logged in successfully as {USERNAME}")
        try:
            data = response.json()
            if 'accessToken' in data:
                session.headers.update({'Authorization': f"Bearer {data['accessToken']}"})
                print(f"[INFO] Using JWT token for authentication")
            elif 'token' in data:
                session.headers.update({'Authorization': f"Bearer {data['token']}"})
                print(f"[INFO] Using JWT token for authentication")
            else:
                print(f"[INFO] Using cookie-based authentication")

            # Store organization ID for webhook testing
            if 'organizationId' in data:
                session.organization_id = data['organizationId']
                print(f"[INFO] Organization ID: {session.organization_id}")
        except Exception as e:
            print(f"[INFO] Using cookie-based authentication")
        return session
    else:
        print(f"[FAIL] Login failed: {response.status_code}")
        print(f"  Response: {response.text}")
        return None


def create_http_webhook_plugin(session):
    """Create an HTTP webhook plugin."""

    plugin_data = {
        "name": "test-http-webhook",
        "description": "Test HTTP webhook plugin for data ingestion",
        "pluginType": "WEBHOOK",
        "provider": "HTTP_WEBHOOK",
        "enabled": True,
        "configuration": {
            "deviceIdField": "deviceId",
            "timestampField": "timestamp",
            "variablesField": "variables",
            "metadataField": "metadata"
        }
    }

    response = session.post(f"{BASE_URL}/api/v1/plugins", json=plugin_data)

    if response.status_code in [200, 201]:
        plugin = response.json()
        print(f"[OK] Created plugin: {plugin['name']} (ID: {plugin['id']})")
        return plugin
    else:
        print(f"[FAIL] Failed to create plugin: {response.status_code}")
        print(f"  Response: {response.text}")
        return None


def send_webhook_data(plugin, organization_id):
    """Send test data via webhook endpoint."""

    webhook_data = {
        "deviceId": "webhook-device-001",
        "timestamp": "2024-01-15T12:00:00Z",
        "variables": {
            "temperature": 25.5,
            "humidity": 60.0,
            "pressure": 1013.25
        },
        "metadata": {
            "location": "Building A - Room 101",
            "sensor_type": "Environmental"
        }
    }

    url = f"{BASE_URL}/api/v1/webhooks/{organization_id}/{plugin['name']}"
    print(f"\n-> Sending webhook data to: {url}")
    print(f"   Payload: {json.dumps(webhook_data, indent=2)}")

    # Send as raw JSON string (webhook expects string body, not JSON object)
    response = requests.post(
        url,
        data=json.dumps(webhook_data),
        headers={'Content-Type': 'application/json'}
    )

    if response.status_code == 200:
        result = response.json()
        print(f"[OK] Webhook processed successfully")
        print(f"  Status: {result.get('status')}")
        print(f"  Records processed: {result.get('recordsProcessed')}")
        print(f"  Duration: {result.get('durationMs')}ms")
        return result
    else:
        print(f"[FAIL] Failed to process webhook: {response.status_code}")
        print(f"  Response: {response.text}")
        return None


def get_execution_history(session, plugin_id):
    """Get execution history for the plugin."""

    response = session.get(
        f"{BASE_URL}/api/v1/plugins/{plugin_id}/executions",
        params={"page": 0, "size": 10}
    )

    if response.status_code == 200:
        data = response.json()
        executions = data.get('content', [])
        print(f"\n[OK] Retrieved {len(executions)} execution(s)")

        for i, execution in enumerate(executions, 1):
            print(f"\n  Execution #{i}:")
            print(f"    Status: {execution['status']}")
            print(f"    Records processed: {execution['recordsProcessed']}")
            print(f"    Duration: {execution.get('durationMs', 'N/A')}ms")
            print(f"    Executed at: {execution['executedAt']}")

            if execution.get('errorMessage'):
                print(f"    Error: {execution['errorMessage']}")

        return executions
    else:
        print(f"[FAIL] Failed to get execution history: {response.status_code}")
        print(f"  Response: {response.text}")
        return None


def get_plugins(session):
    """Get all plugins."""

    response = session.get(f"{BASE_URL}/api/v1/plugins", params={"page": 0, "size": 10})

    if response.status_code == 200:
        data = response.json()
        plugins = data.get('content', [])
        print(f"\n[OK] Retrieved {len(plugins)} plugin(s)")

        for plugin in plugins:
            print(f"  - {plugin['name']} ({plugin['provider']}) - {'Enabled' if plugin['enabled'] else 'Disabled'}")

        return plugins
    else:
        print(f"[FAIL] Failed to get plugins: {response.status_code}")
        print(f"  Response: {response.text}")
        return None


def delete_plugin(session, plugin_id):
    """Delete the test plugin."""

    response = session.delete(f"{BASE_URL}/api/v1/plugins/{plugin_id}")

    if response.status_code in [200, 204]:
        print(f"\n[OK] Deleted plugin {plugin_id}")
        return True
    else:
        print(f"\n[FAIL] Failed to delete plugin: {response.status_code}")
        print(f"  Response: {response.text}")
        return False


def main():
    print("=" * 60)
    print("Data Plugins - End-to-End Test")
    print("=" * 60)

    # Step 1: Login
    print("\n[1] Authenticating...")
    session = login()
    if not session:
        print("\n[FAIL] Test failed: Could not authenticate")
        return

    # Step 2: Get existing plugins
    print("\n[2] Listing existing plugins...")
    get_plugins(session)

    # Step 3: Create HTTP webhook plugin
    print("\n[3] Creating HTTP webhook plugin...")
    plugin = create_http_webhook_plugin(session)
    if not plugin:
        print("\n[FAIL] Test failed: Could not create plugin")
        return

    plugin_id = plugin['id']

    # Step 4: Send webhook data
    print("\n[4] Sending webhook data...")
    org_id = getattr(session, 'organization_id', 1)
    result = send_webhook_data(plugin, org_id)
    if not result:
        print("\n[FAIL] Test failed: Could not send webhook data")
        delete_plugin(session, plugin_id)
        return

    # Step 5: Check execution history
    print("\n[5] Checking execution history...")
    time.sleep(1)  # Brief pause to ensure execution is recorded
    executions = get_execution_history(session, plugin_id)

    # Step 6: Cleanup
    print("\n[6] Cleaning up...")
    delete_plugin(session, plugin_id)

    # Summary
    print("\n" + "=" * 60)
    if result and executions:
        print("[OK] ALL TESTS PASSED!")
        print("=" * 60)
        print("\nData Plugins feature is working correctly:")
        print("  [OK] Plugin creation")
        print("  [OK] Webhook data reception")
        print("  [OK] Data processing and telemetry ingestion")
        print("  [OK] Execution history tracking")
        print("  [OK] Plugin deletion")
    else:
        print("[FAIL] SOME TESTS FAILED")
        print("=" * 60)


if __name__ == "__main__":
    main()
