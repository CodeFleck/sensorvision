"""
Debug script to verify data ingestion and check backend status
"""
import requests
import json
from datetime import datetime

API_URL = "http://35.88.65.186.nip.io:8080"
DEVICE_TOKEN_001 = "80f6246c-9096-4d32-bbe2-44ede69702d7"
DEVICE_TOKEN_003 = "98d110c2-ff73-4db0-a4f6-422b885f2419"

def test_health():
    """Test backend health"""
    print("\n=== Testing Backend Health ===")
    try:
        response = requests.get(f"{API_URL}/actuator/health")
        print(f"Status: {response.status_code}")
        print(f"Response: {response.json()}")
        return response.status_code == 200
    except Exception as e:
        print(f"ERROR: {e}")
        return False

def test_ingestion(device_id, token):
    """Test data ingestion"""
    print(f"\n=== Testing Ingestion for {device_id} ===")

    test_data = {
        "kw_consumption": 50.0,
        "voltage": 220.0,
        "current": 0.23,
        "power_factor": 0.9,
        "frequency": 60.0
    }

    try:
        headers = {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json"
        }

        url = f"{API_URL}/api/v1/ingest/{device_id}"
        print(f"URL: {url}")
        print(f"Data: {test_data}")

        response = requests.post(url, json=test_data, headers=headers)
        print(f"Status: {response.status_code}")
        print(f"Response: {response.text}")

        return response.status_code in [200, 201]
    except Exception as e:
        print(f"ERROR: {e}")
        return False

def check_device_telemetry(device_id, token):
    """Try to fetch recent telemetry data"""
    print(f"\n=== Checking Telemetry for {device_id} ===")

    try:
        headers = {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json"
        }

        # Try different endpoints
        endpoints = [
            f"/api/v1/devices/{device_id}/telemetry",
            f"/api/v1/telemetry/{device_id}",
            f"/api/v1/devices/{device_id}/telemetry/latest"
        ]

        for endpoint in endpoints:
            url = f"{API_URL}{endpoint}"
            print(f"\nTrying: {url}")
            response = requests.get(url, headers=headers)
            print(f"Status: {response.status_code}")
            if response.status_code == 200:
                print(f"Response: {response.text[:500]}")  # First 500 chars
            else:
                print(f"Response: {response.text}")
    except Exception as e:
        print(f"ERROR: {e}")

def main():
    print("=" * 70)
    print("SensorVision Debug Script")
    print(f"API URL: {API_URL}")
    print(f"Time: {datetime.now()}")
    print("=" * 70)

    # Test backend health
    if not test_health():
        print("\nWARNING: Backend health check failed!")
        return

    # Test ingestion for both devices
    success_001 = test_ingestion("meter-001", DEVICE_TOKEN_001)
    success_003 = test_ingestion("meter-003", DEVICE_TOKEN_003)

    # Check telemetry data
    check_device_telemetry("meter-001", DEVICE_TOKEN_001)
    check_device_telemetry("meter-003", DEVICE_TOKEN_003)

    print("\n" + "=" * 70)
    print("Summary:")
    print(f"  meter-001 ingestion: {'SUCCESS' if success_001 else 'FAILED'}")
    print(f"  meter-003 ingestion: {'SUCCESS' if success_003 else 'FAILED'}")
    print("=" * 70)

    if success_001 and success_003:
        print("\nData ingestion is working! Check your dashboard:")
        print(f"  {API_URL}/dashboard")
        print("\nIf you don't see data in the UI:")
        print("  1. Check browser console for errors (F12)")
        print("  2. Verify WebSocket connection is established")
        print("  3. Check that frontend is connecting to correct API URL")
        print("  4. Verify time range filter in dashboard (data might be outside range)")

if __name__ == "__main__":
    main()