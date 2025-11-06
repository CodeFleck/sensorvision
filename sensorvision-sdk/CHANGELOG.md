# Changelog

All notable changes to the SensorVision Python SDK will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.1] - 2025-01-15

### Fixed
- **BREAKING**: Fixed exception inconsistency in validation functions. `validate_device_id()` and `validate_telemetry_data()` now raise `ValidationError` instead of `ValueError` to match the documented API contract in `SensorVisionClient.send_data()`.

### Migration Guide

If you were catching `ValueError` for validation errors:

```python
# Before (v0.1.0)
try:
    client.send_data(device_id, data)
except ValueError as e:  # Caught validation errors
    handle_error(e)

# After (v0.1.1)
from sensorvision.exceptions import ValidationError

try:
    client.send_data(device_id, data)
except ValidationError as e:  # Now catches ValidationError as documented
    handle_error(e)
```

### Why This Change?

The docstring for `send_data()` documented that `ValidationError` would be raised for invalid inputs, but the implementation raised `ValueError`. This change aligns the implementation with the documented API, making the SDK more predictable and allowing consumers to catch validation errors specifically without catching all `ValueError` exceptions.

## [0.1.0] - 2025-01-15

### Added
- Initial release of SensorVision Python SDK
- `SensorVisionClient` for synchronous telemetry ingestion
- Device token authentication
- Configurable retry logic with exponential backoff
- Comprehensive error handling (AuthenticationError, ValidationError, NetworkError, RateLimitError, ServerError)
- Type hints throughout
- Complete documentation and examples
- Published to PyPI: https://pypi.org/project/sensorvision-sdk/

### Features
- Simple API: `client.send_data(device_id, data)`
- Automatic validation of device IDs and telemetry data
- Configurable timeout and retry settings
- Production-ready with >90% test coverage
- Support for Python 3.8+

[0.1.1]: https://github.com/CodeFleck/sensorvision/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/CodeFleck/sensorvision/releases/tag/v0.1.0
