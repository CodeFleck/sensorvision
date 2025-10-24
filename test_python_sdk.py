"""
Test script for Python SDK
"""
import sys
sys.path.insert(0, 'sensorvision-sdk')

from sensorvision import SensorVisionClient

def test_sync_client():
    """Test synchronous client"""
    print("Testing Python SDK - Synchronous Client")
    print("=" * 50)

    # Using a properly formatted UUID token
    client = SensorVisionClient(
        api_url="http://localhost:8080",
        api_key="550e8400-e29b-41d4-a716-446655440000",  # Valid UUID format
        timeout=10
    )

    # Test 1: Send temperature and humidity data
    print("\n1. Sending temperature and humidity data...")
    try:
        response = client.send_data("python-sdk-test-001", {
            "temperature": 23.5,
            "humidity": 65.2
        })
        print(f"   [OK] Success: {response.message}")
        print(f"   Device ID: {response.device_id}")
        print(f"   Timestamp: {response.timestamp}")
    except Exception as e:
        print(f"   [ERROR] Error: {e}")
        return False

    # Test 2: Send multiple sensor readings
    print("\n2. Sending multiple sensor readings...")
    try:
        response = client.send_data("python-sdk-test-002", {
            "temperature": 22.8,
            "humidity": 68.5,
            "pressure": 1013.25,
            "co2_ppm": 450,
            "light_level": 850.0
        })
        print(f"   [OK] Success: {response.message}")
    except Exception as e:
        print(f"   [ERROR] Error: {e}")
        return False

    # Test 3: Test with context manager
    print("\n3. Testing with context manager...")
    try:
        with SensorVisionClient(
            api_url="http://localhost:8080",
            api_key="550e8400-e29b-41d4-a716-446655440000"
        ) as client:
            response = client.send_data("python-sdk-test-003", {
                "voltage": 220.5,
                "current": 0.85,
                "power_kw": 0.187
            })
            print(f"   [OK] Success: {response.message}")
    except Exception as e:
        print(f"   [ERROR] Error: {e}")
        return False

    print("\n" + "=" * 50)
    print("[SUCCESS] All Python SDK tests passed!")
    return True


if __name__ == "__main__":
    success = test_sync_client()
    sys.exit(0 if success else 1)
