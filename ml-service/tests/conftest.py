"""
Pytest configuration and fixtures.
"""
import pytest
import numpy as np
import pandas as pd


@pytest.fixture(scope="session")
def sample_telemetry_data():
    """Generate sample telemetry data for testing."""
    np.random.seed(42)
    n_samples = 100

    return pd.DataFrame({
        "timestamp": pd.date_range(start="2024-01-01", periods=n_samples, freq="1min"),
        "temperature": np.random.normal(25, 2, n_samples),
        "pressure": np.random.normal(101.3, 1, n_samples),
        "vibration": np.random.normal(0.5, 0.1, n_samples),
        "humidity": np.random.normal(50, 5, n_samples),
        "current": np.random.normal(10, 1, n_samples),
    })


@pytest.fixture(scope="session")
def sample_training_data(sample_telemetry_data):
    """Training data subset."""
    return sample_telemetry_data.head(80)


@pytest.fixture(scope="session")
def sample_test_data(sample_telemetry_data):
    """Test data subset with anomalies."""
    test_data = sample_telemetry_data.tail(20).copy()
    # Inject anomalies
    test_data.iloc[0, test_data.columns.get_loc("temperature")] = 100.0
    test_data.iloc[1, test_data.columns.get_loc("pressure")] = 200.0
    return test_data
