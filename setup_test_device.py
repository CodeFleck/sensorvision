"""
Setup test device with token for SDK testing
"""
import requests
import json

BASE_URL = "http://localhost:8080"

def setup_test_device():
    """Create a test device and generate a token"""
    print("Setting up test device...")
    print("=" * 50)

    # Step 1: Create a device
    print("\n1. Creating test device...")
    device_data = {
        "externalId": "sdk-test-device",
        "name": "SDK Test Device",
        "description": "Device for testing SDKs",
        "organizationId": 1  # Assuming organization 1 exists
    }

    try:
        response = requests.post(
            f"{BASE_URL}/api/v1/devices",
            json=device_data,
            headers={"Content-Type": "application/json"}
        )
        print(f"   Status: {response.status_code}")

        if response.status_code in (200, 201):
            device = response.json()
            print(f"   [OK] Device created: ID={device.get('id')}, ExternalID={device.get('externalId')}")
            device_id = device.get('id')
        elif response.status_code == 400 and "already exists" in response.text:
            print("   [INFO] Device already exists, fetching it...")
            # Get existing device
            list_response = requests.get(f"{BASE_URL}/api/v1/devices")
            devices = list_response.json()
            device = next((d for d in devices if d.get('externalId') == 'sdk-test-device'), None)
            if device:
                device_id = device.get('id')
                print(f"   [OK] Found existing device: ID={device_id}")
            else:
                print("   [ERROR] Could not find device")
                return None
        else:
            print(f"   [ERROR] Failed to create device: {response.text}")
            return None

    except Exception as e:
        print(f"   [ERROR] {e}")
        return None

    # Step 2: Generate token for device
    print(f"\n2. Generating token for device {device_id}...")
    try:
        response = requests.post(
            f"{BASE_URL}/api/v1/devices/{device_id}/tokens/generate",
            headers={"Content-Type": "application/json"}
        )
        print(f"   Status: {response.status_code}")

        if response.status_code in (200, 201):
            token_data = response.json()
            token = token_data.get('token')
            print(f"   [OK] Token generated: {token}")
            print(f"\n   Use this token in your SDK tests:")
            print(f"   X-API-Key: {token}")
            return token
        else:
            print(f"   [ERROR] Failed to generate token: {response.text}")
            return None

    except Exception as e:
        print(f"   [ERROR] {e}")
        return None

if __name__ == "__main__":
    token = setup_test_device()
    if token:
        print("\n" + "=" * 50)
        print("[SUCCESS] Setup complete!")
        print(f"\nToken: {token}")
    else:
        print("\n" + "=" * 50)
        print("[FAILED] Setup failed!")
