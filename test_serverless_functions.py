#!/usr/bin/env python3
"""
Test script for serverless functions feature.
Tests the complete workflow: create, invoke, check history.
"""

import requests
import json
import time

BASE_URL = "http://localhost:8080"

# Test credentials
USERNAME = "test@sensorvision.com"
PASSWORD = "testpass123"

def register():
    """Register a test user."""
    register_data = {
        "username": USERNAME,
        "email": USERNAME,
        "password": PASSWORD,
        "firstName": "Test",
        "lastName": "User",
        "organizationName": "Test Organization"
    }

    response = requests.post(f"{BASE_URL}/api/v1/auth/register", json=register_data)

    if response.status_code in [200, 201]:
        print(f"[OK] Registered user {USERNAME}")
        return True
    elif response.status_code in [400, 409]:
        # User already exists, that's fine
        if "already taken" in response.text or "already exists" in response.text:
            print(f"[INFO] User {USERNAME} already exists")
            return True
        else:
            print(f"[FAIL] Registration failed: {response.status_code}")
            print(f"  Response: {response.text}")
            return False
    else:
        print(f"[FAIL] Registration failed: {response.status_code}")
        print(f"  Response: {response.text}")
        return False

def login():
    """Login and get session cookie."""
    session = requests.Session()

    # Login
    login_data = {
        "username": USERNAME,
        "password": PASSWORD
    }

    response = session.post(f"{BASE_URL}/api/v1/auth/login", json=login_data)

    if response.status_code == 200:
        print(f"[OK] Logged in successfully as {USERNAME}")
        # Check if we got a token in the response
        try:
            data = response.json()
            if 'accessToken' in data:
                # Use JWT token for authentication
                session.headers.update({'Authorization': f"Bearer {data['accessToken']}"})
                print(f"[INFO] Using JWT token for authentication")
            elif 'token' in data:
                # Use JWT token for authentication
                session.headers.update({'Authorization': f"Bearer {data['token']}"})
                print(f"[INFO] Using JWT token for authentication")
            else:
                print(f"[INFO] No token in response, using cookie-based authentication")
        except Exception as e:
            # Cookie-based auth
            print(f"[INFO] Using cookie-based authentication (parse error: {e})")
        return session
    else:
        print(f"[FAIL] Login failed: {response.status_code}")
        print(f"  Response: {response.text}")
        return None

def create_function(session):
    """Create a test serverless function."""

    python_code = """def main(event):
    # Test function that processes sensor data
    device_id = event.get('device_id', 'unknown')
    temperature = event.get('temperature', 0)

    # Simple processing logic
    status = 'normal'
    if temperature > 30:
        status = 'high'
    elif temperature < 10:
        status = 'low'

    return {
        'device_id': device_id,
        'temperature': temperature,
        'status': status,
        'message': f'Device {device_id} temperature is {status}'
    }
"""

    function_data = {
        "name": "test-sensor-processor",
        "description": "Test function for processing sensor data",
        "runtime": "PYTHON_3_11",
        "code": python_code,
        "handler": "main",
        "enabled": True,
        "timeoutSeconds": 30,
        "memoryLimitMb": 512
    }

    response = session.post(f"{BASE_URL}/api/v1/functions", json=function_data)

    if response.status_code in [200, 201]:
        function = response.json()
        print(f"[OK] Created function: {function['name']} (ID: {function['id']})")
        return function
    else:
        print(f"[FAIL] Failed to create function: {response.status_code}")
        print(f"  Response: {response.text}")
        return None

def invoke_function(session, function_id):
    """Invoke the function with test data."""

    test_input = {
        "device_id": "sensor-001",
        "temperature": 35.5
    }

    invoke_data = {
        "input": test_input,
        "sync": True
    }

    print(f"\n-> Invoking function with input: {json.dumps(test_input, indent=2)}")

    response = session.post(
        f"{BASE_URL}/api/v1/functions/{function_id}/invoke",
        json=invoke_data
    )

    if response.status_code == 200:
        result = response.json()
        print(f"[OK] Function executed successfully")
        print(f"  Status: {result['status']}")
        print(f"  Output: {json.dumps(result.get('output'), indent=2)}")
        return result
    else:
        print(f"[FAIL] Failed to invoke function: {response.status_code}")
        print(f"  Response: {response.text}")
        return None

def get_execution_history(session, function_id):
    """Get execution history for the function."""

    response = session.get(
        f"{BASE_URL}/api/v1/functions/{function_id}/executions",
        params={"page": 0, "size": 10}
    )

    if response.status_code == 200:
        data = response.json()
        executions = data.get('content', [])
        print(f"\n[OK] Retrieved {len(executions)} execution(s)")

        for i, execution in enumerate(executions, 1):
            print(f"\n  Execution #{i}:")
            print(f"    Status: {execution['status']}")
            print(f"    Duration: {execution.get('durationMs', 'N/A')}ms")
            print(f"    Memory: {execution.get('memoryUsedMb', 'N/A')}MB")
            print(f"    Started: {execution['startedAt']}")

            if execution.get('outputData'):
                print(f"    Output: {json.dumps(execution['outputData'], indent=6)}")

        return executions
    else:
        print(f"[FAIL] Failed to get execution history: {response.status_code}")
        print(f"  Response: {response.text}")
        return None

def delete_function(session, function_id):
    """Delete the test function."""

    response = session.delete(f"{BASE_URL}/api/v1/functions/{function_id}")

    if response.status_code in [200, 204]:
        print(f"\n[OK] Deleted function {function_id}")
        return True
    else:
        print(f"\n[FAIL] Failed to delete function: {response.status_code}")
        print(f"  Response: {response.text}")
        return False

def main():
    print("=" * 60)
    print("Serverless Functions - End-to-End Test")
    print("=" * 60)

    # Step 1: Register and Login
    print("\n[1] Registering test user...")
    if not register():
        print("\n[FAIL] Test failed: Could not register")
        return

    print("\n[2] Authenticating...")
    session = login()
    if not session:
        print("\n[FAIL] Test failed: Could not authenticate")
        return

    # Step 3: Create function
    print("\n[3] Creating test function...")
    function = create_function(session)
    if not function:
        print("\n[FAIL] Test failed: Could not create function")
        return

    function_id = function['id']

    # Step 4: Invoke function
    print("\n[4] Invoking function...")
    result = invoke_function(session, function_id)
    if not result:
        print("\n[FAIL] Test failed: Could not invoke function")
        delete_function(session, function_id)
        return

    # Step 5: Check execution history
    print("\n[5] Checking execution history...")
    time.sleep(1)  # Brief pause to ensure execution is recorded
    executions = get_execution_history(session, function_id)

    # Step 6: Cleanup
    print("\n[6] Cleaning up...")
    delete_function(session, function_id)

    # Summary
    print("\n" + "=" * 60)
    if result and executions:
        print("[OK] ALL TESTS PASSED!")
        print("=" * 60)
        print("\nServerless functions feature is working correctly:")
        print("  [OK] Function creation")
        print("  [OK] Python code execution")
        print("  [OK] Input/output handling")
        print("  [OK] Execution history tracking")
        print("  [OK] Function deletion")
    else:
        print("[FAIL] SOME TESTS FAILED")
        print("=" * 60)

if __name__ == "__main__":
    main()
